package com.kolhozcustoms.jira.plugin.timetracker.webwork;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap; // sorted Map
import java.util.ArrayList;
import java.util.Arrays;
import lombok.Data; // аннотации для авто-создания геттеров и сеттеров
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kolhozcustoms.jira.plugin.timetracker.models.*;
import com.kolhozcustoms.jira.plugin.timetracker.service.PluginSettingsService;
import com.kolhozcustoms.jira.plugin.timetracker.utils.TimeUtil;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.security.JiraAuthenticationContext; // need import here for REST
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.issue.search.SearchQuery;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.SearchException; 
import com.atlassian.jira.issue.search.DocumentWithId;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.query.Query;
import org.apache.lucene.document.Document;

@Slf4j
@Data
public class TimeTrackerReportAction extends JiraWebActionSupport
{
    //private static final Logger log = LoggerFactory.getLogger(TimeTrackerReportAction.class);
    private final UserSearchService userSearchService;
    private final JqlQueryParser jqlQueryParser;
    private final SearchProvider searchProvider;
    private final WorklogManager worklogManager;
    private final IssueManager issueManager;
    private final AvatarService avatarService;

    private final WebResourceManager webResourceManager;
    private final ProjectManager projectManager;
    private final Long timeZoneOffset;

    private List<ApplicationUser> аpplicationUsers = new ArrayList<>();
    private ApplicationUser аpplicationLoggedInUser;

    private List<String> executerusers;
    private String[] worklogprojects;
    private String start_date = "";
    private String end_date = "";
    private String exception = "";
    private String group_by = "";
    
    private List<String> currentUser = new ArrayList<String>();
    private ArrayList<List<String>> userList = new ArrayList<List<String>>();
    private ArrayList<List<String>> projectCheckedList;
    private ArrayList<UserDataObject> dataObjectList = new ArrayList<UserDataObject>();
    private TableHeaderObject tableHeaderObject;

    @Inject
    public TimeTrackerReportAction(@ComponentImport UserSearchService userSearchService, @ComponentImport JqlQueryParser jqlQueryParser, @ComponentImport SearchProvider searchProvider, @ComponentImport WorklogManager worklogManager, @ComponentImport IssueManager issueManager, @ComponentImport AvatarService avatarService, PluginSettingsService pluginSettingsService, @ComponentImport WebResourceManager webResourceManager, @ComponentImport JiraAuthenticationContext jiraAuthenticationContext, @ComponentImport ProjectManager projectManager, @ComponentImport TimeZoneManager timeZoneManager) { 
        this.userSearchService = userSearchService;
        this.jqlQueryParser = jqlQueryParser;
        this.searchProvider = searchProvider;
        this.worklogManager = worklogManager;
        this.issueManager = issueManager;
        this.avatarService = avatarService;
        this.projectManager = projectManager;
        this.аpplicationLoggedInUser = getLoggedInUser(); //jiraAuthenticationContext.getLoggedInUser();
        this.currentUser.addAll(Arrays.asList(new String[] {this.аpplicationLoggedInUser.getUsername(), this.avatarService.getAvatarURL(this.аpplicationLoggedInUser, this.аpplicationLoggedInUser).toString(), this.аpplicationLoggedInUser.getDisplayName(), this.аpplicationLoggedInUser.getEmailAddress()}));
        this.webResourceManager = webResourceManager; // for include css and js in html
        this.start_date = this.start_date == "" ? TimeUtil.fromTimeStamp(TimeUtil.nowTimeStamp()-(82800000*14),"yyyy-MM-dd"): this.start_date;
        this.end_date = this.end_date == "" ? TimeUtil.now("yyyy-MM-dd") : this.end_date;  
        
        Integer tzSetting = Integer.parseInt(pluginSettingsService.getParameter("tz_radio", "0"));
        if (tzSetting == 1) {
            this.timeZoneOffset = Long.valueOf(timeZoneManager.getDefaultTimezone().getRawOffset());
        } else if (tzSetting == 2) {
            this.timeZoneOffset = Long.valueOf(timeZoneManager.getLoggedInUserTimeZone().getRawOffset());
        } else if (tzSetting == 3) {
            Integer customTimeOffset = Integer.parseInt(pluginSettingsService.getParameter("time_offset", "0"));
            this.timeZoneOffset = Long.valueOf(customTimeOffset * 3600000);
        } else {
            this.timeZoneOffset = 0L;
        }

    }

    @Override
    public String execute() throws Exception {
        this.projectCheckedList = getCheckedProjectList(getFormParametrsArray("worklogprojects"));
        this.executerusers = getFormParametrsArray("executerusers");
        this.аpplicationUsers = getApplicationUsers(this.executerusers);
        getFilter();
        //searchByUser(this.аpplicationUsers);
        return super.execute(); //returns SUCCESS
    }

    private String getWorklogAutorQuery(){ //depricated?
        String worklogAuthor = "";
        List<String> executerusers = getFormParametrsArray("executerusers");
        if (executerusers != null && executerusers.size() > 0){ // !executerusers.isEmpty()
            worklogAuthor = "worklogAuthor in (";
            for(String user : executerusers){
                worklogAuthor = worklogAuthor + user + ",";
            }
            worklogAuthor = worklogAuthor.substring(0, worklogAuthor.length() - 1) + ")";
        } else {
            this.exception = this.exception + "no values for user; ";
        }
        return worklogAuthor;
    }

    private String getWorklogProjectQuery(){ // project in (HR, IMOS) AND 
        String worklogQuery = "";
        List<String> projects = getFormParametrsArray( "worklogprojects");
            if (projects != null && projects.size() > 0){
                worklogQuery = "project in (";
                for(String project : projects){
                    worklogQuery = worklogQuery + project + ",";
                }
                worklogQuery = worklogQuery.substring(0, worklogQuery.length() - 1) + ") AND ";
            }
        return worklogQuery;
    }

    private List<String> getFormParametrsArray(String paramName){
        String[] parametr = getHttpRequest().getParameterValues(paramName);
        if(parametr != null) {
            return Arrays.asList(parametr);
        } else {
            return new ArrayList<String>();
        }
    }

    private ArrayList<List<String>> getCheckedProjectList(List<String> worklogprojects){
        ArrayList<List<String>> projectList = new ArrayList<List<String>>();
        if (worklogprojects.size() > 0){
            for(String projectKey : worklogprojects){
                try {
                Project project = this.projectManager.getProjectObjByKeyIgnoreCase(projectKey);
                projectList.add(Arrays.asList(new String[] {project.getKey(), this.avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL).toString(), project.getName(), "checked=\"checked\""}));
                } catch (NullPointerException e){this.exception = this.exception + " projectManager.getProjectObjByKeyIgnoreCase exception: " +  e.toString() + "; ";}
            }
        }
        return projectList;
    }
/*
    private Void getApplicationUsers(){ //List<ApplicationUser>
        List<ApplicationUser> аpplicationUsers = new ArrayList<>();
        try {
            if (this.executerusers != null && this.executerusers.length > 0){
                for(String user : this.executerusers){
                    this.аpplicationUsers.addAll(this.userSearchService.findUsersAllowEmptyQuery(this.getJiraServiceContext(), user));
                }
            }
            if (this.current_user != ""){ // навести порядок с current_user и currentUser
                this.currentUser.add("checked=\"checked\"");
                this.аpplicationUsers.addAll(this.userSearchService.findUsersAllowEmptyQuery(this.getJiraServiceContext(), this.current_user));
            } else {
                this.currentUser.add("");
            }
            if (this.аpplicationUsers.size() == 0){
                this.аpplicationUsers.addAll(this.userSearchService.findUsersAllowEmptyQuery(this.getJiraServiceContext(), ""));
            }
        } catch (NullPointerException e){this.exception = this.exception + " this.executerusers.length exception: " +  e.toString() + "; ";}
        return null;
    }
*/
    private List<ApplicationUser> getApplicationUsers(List<String> executerusers){
        List<ApplicationUser> аpplicationUsers = new ArrayList<>();
        UserSearchParams searchParams = UserSearchParams.builder().includeActive(true).build();
        try {
            if (executerusers != null && executerusers.size() > 0){
                for(String user : executerusers){
                    аpplicationUsers.addAll(this.userSearchService.findUsers(user, searchParams));
                }
            }
            if (аpplicationUsers.size() == 0){
                searchParams = UserSearchParams.builder()
                .allowEmptyQuery(true)
                .includeActive(true)
                .sorted(true)
                .build();
                аpplicationUsers.addAll(this.userSearchService.findUsers("", searchParams));
            }
        } catch (NullPointerException e){System.out.println("this.executerusers.length exception: " +  e.toString() + "; ");}
        return аpplicationUsers;
    }

    private Void getFilter(){ 
        String avatar = "";
        if (this.executerusers.contains(this.аpplicationLoggedInUser.getUsername())){
            this.currentUser.add("checked=\"checked\"");
        } else {
            this.currentUser.add("");
        }
        if (this.executerusers != null && this.executerusers.size() > 0){
            for(ApplicationUser аpplicationUser : this.аpplicationUsers){
                if (this.executerusers.contains(аpplicationUser.getUsername()) && !аpplicationUser.getUsername().equals(this.currentUser.get(0))){
                    try {
                        avatar = this.avatarService.getAvatarURL(аpplicationUser, аpplicationUser).toString(); // Avatar.Size size
                    } catch (NullPointerException e){this.exception = this.exception + " avatarService exception: " +  e.toString() + "; ";}
                    this.userList.add(Arrays.asList(new String[] {аpplicationUser.getUsername(), avatar, аpplicationUser.getDisplayName(), аpplicationUser.getEmailAddress(), аpplicationUser.getKey()}));
                }
            }
        }
        return null;
    }

    private Void searchByUser(List<ApplicationUser> аpplicationUsers) throws SearchException{ //noy use? replaced to REST
        Query query = null;
        SearchResults<DocumentWithId> searchResults = null;
        Collection<Long> issue_id_collection = new ArrayList<Long>();
        Long issue_id = null;
        Long check_start_date = null;
        Long end_date_timestamp = TimeUtil.toTimeStamp(this.end_date, "yyyy-MM-dd");
        Long start_date_timestamp = TimeUtil.toTimeStamp(this.start_date, "yyyy-MM-dd");
        Long dateOffset = 4838400000L;
        Long issueAvatarId = 0L;
        String avatar = "";
        String new_end_date = (end_date_timestamp + dateOffset) < TimeUtil.nowTimeStamp() ? TimeUtil.fromTimeStamp(end_date_timestamp + dateOffset, "yyyy/MM/dd") : TimeUtil.now("yyyy/MM/dd");
        List<Issue> issueList = new ArrayList<Issue>();
        String worklogProjectQuery = getWorklogProjectQuery();
        this.tableHeaderObject = new TableHeaderObject(start_date_timestamp, end_date_timestamp); //for (Map.Entry<String, Long> entry : emptySpentListByDay.entrySet()) { entry.getKey(); entry.getValue(); } 
        
        for(ApplicationUser аpplicationUser : аpplicationUsers){
            try {
                avatar = this.avatarService.getAvatarURL(аpplicationUser, аpplicationUser).toString(); // Avatar.Size size
            } catch (NullPointerException e){this.exception = this.exception + " avatarService exception: " +  e.toString() + "; ";}
            try {
                query = this.jqlQueryParser.parseQuery(worklogProjectQuery + "worklogAuthor = \"" + аpplicationUser.getUsername() + "\"" + " and worklogDate >= \"" + TimeUtil.dateTimeFormat(this.start_date, "yyyy-MM-dd", "yyyy/MM/dd") + "\" and worklogDate <= \"" + new_end_date + "\"");
            } catch (JqlParseException e){ this.exception = this.exception + " jqlQueryParser exception:" + e.getParseErrorMessage().toString();
            } catch (NullPointerException e){ this.exception = this.exception + " jqlQueryParser exception: " +  e.toString() + "; ";}
            try {
                searchResults = this.searchProvider.search(SearchQuery.create(query, аpplicationUser), PagerFilter.getUnlimitedFilter());
            } catch (SearchException | NullPointerException e){this.exception = this.exception + " searchProvider exception: " +  e.toString() + "; ";}
            UserDataObject userDataObject = new UserDataObject(аpplicationUser.getKey(), аpplicationUser.getDisplayName(), avatar, this.tableHeaderObject.getEmptySpentList());
            Map<Long, String> projectKeyById = new TreeMap<Long, String>();
            for(DocumentWithId document : searchResults.getResults()){
                try { 
                    issue_id = Long.parseLong(document.getDocument().get("issue_id")); //fieldnames: task[key, summary] project[projid, projkey]
                    if (issue_id != null){
                        issue_id_collection.add(issue_id);
                        projectKeyById.put(issue_id, document.getDocument().get("projkey"));
                    }
                } catch (NullPointerException e){this.exception = this.exception + " Collection exception for:" + issue_id.getClass() + ": " + e.toString() + "; ";}
            }
            try {
                issueList = this.issueManager.getIssueObjects(issue_id_collection);
            } catch (NullPointerException e){this.exception = this.exception + " issueList exception:" + e.toString() + "; ";}
            
            for(Issue issue : issueList){
                Project project = issue.getProjectObject();
                try {
                    issueAvatarId = issue.getIssueType().getAvatar().getId();
                } catch (NullPointerException e){
                    issueAvatarId = 10320L; //exception on Epic and Store
                }
                userDataObject.addIssueToProject(project.getId(), project.getKey(), project.getName(), this.avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL).toString(), issue.getId(), issue.getKey(), issue.getSummary(), issue.getIssueTypeId(), String.valueOf(issueAvatarId));
                for(Worklog worklog : this.worklogManager.getByIssue(issue)){
                    if(!аpplicationUser.getKey().equals(worklog.getAuthorKey())) { continue; }
                    check_start_date = worklog.getStartDate().getTime() + this.timeZoneOffset;
                    if (check_start_date >= start_date_timestamp && check_start_date <= end_date_timestamp + 82800000){ // + 23hours
                        userDataObject.spentTime(TimeUtil.fromTimeStamp(check_start_date, "MM")+TimeUtil.fromTimeStamp(check_start_date, "dd"), worklog.getTimeSpent(), project.getId(), issue.getId());
                    }
                }
            }
            issue_id_collection.clear();
            projectKeyById.clear();
            this.dataObjectList.add(userDataObject);
        }
        //worklogAuthor = " + аpplicationUser.getName() + "  and worklogDate >= "2019/01/10" and worklogDate <= "2023/01/12"  AND issueFunction in  aggregateExpression("Total Estimate for all Issues", "originalEstimate.sum()", "Remaining work", "remainingEstimate.sum()", "Total Time Spent", "timeSpent.sum()") 
        return null;
    }
}