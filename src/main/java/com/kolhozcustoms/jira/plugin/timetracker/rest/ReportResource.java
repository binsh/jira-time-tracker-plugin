package com.kolhozcustoms.jira.plugin.timetracker.rest;

//import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.FormParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap; // sorted Map

import com.kolhozcustoms.jira.plugin.timetracker.models.*;
import com.kolhozcustoms.jira.plugin.timetracker.utils.*;
import com.kolhozcustoms.jira.plugin.timetracker.service.PluginSettingsService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.security.JiraAuthenticationContext;
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
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.query.Query;
import org.apache.lucene.document.Document;


@Named 
@Path("/report")  
public class ReportResource {
    private JiraAuthenticationContext jiraAuthenticationContext;
    private UserSearchService userSearchService; 
    private JqlQueryParser jqlQueryParser;
    private SearchProvider searchProvider;
    private WorklogManager worklogManager;
    private IssueManager issueManager;
    private AvatarService avatarService;
    private ApplicationUser аpplicationLoggedInUser;
    private Long timeZoneOffset;
    private static final String PLUGIN_STORAGE_KEY = "com.kolhozcustoms.timeTracker.settings";

    private List<String> executerusers;
    private List<String> worklogprojects;
    private String start_date = "";
    private String end_date = "";
    private String exception = "";
    private boolean show_empty = false;
    private TableHeaderObject tableHeaderObject;

    public ReportResource() {}
    @Inject 
    public ReportResource(UserSearchService userSearchService, JqlQueryParser jqlQueryParser, SearchProvider searchProvider, WorklogManager worklogManager, IssueManager issueManager, AvatarService avatarService, PluginSettingsService pluginSettingsService, JiraAuthenticationContext jiraAuthenticationContext, TimeZoneManager timeZoneManager) {
        this.userSearchService = userSearchService;
        this.jqlQueryParser = jqlQueryParser;
        this.searchProvider = searchProvider;
        this.worklogManager = worklogManager;
        this.issueManager = issueManager;
        this.avatarService = avatarService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;

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

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response getReportData(@FormParam("executerusers") final List<String> executerusers,
                                @FormParam("worklogprojects") final List<String> worklogprojects,
                                @DefaultValue("") @FormParam("start_date") final String start_date,
                                @DefaultValue("") @FormParam("end_date") final String end_date,
                                @DefaultValue("user") @FormParam("group_by") final String group_by,
                                @DefaultValue("false") @FormParam("show_empty") final boolean show_empty/*,
                                @Context HttpServletRequest request*/) throws Exception{
        this.аpplicationLoggedInUser = this.jiraAuthenticationContext.getLoggedInUser();
        System.out.println( "currentuser: " + аpplicationLoggedInUser.getDisplayName() + "\n" );
        this.show_empty = show_empty;
        this.start_date = start_date.equals("") ? TimeUtil.fromTimeStamp(TimeUtil.nowTimeStamp()-(82800000*14),"yyyy-MM-dd"): start_date;
        this.end_date = end_date.equals("") ? TimeUtil.now("yyyy-MM-dd") : end_date;
        List<ApplicationUser> аpplicationUsers = getApplicationUsers(executerusers);
        List<?> dataObjectList = new ArrayList<>();
        if (group_by.equals("user")){
             dataObjectList = searchByUser(аpplicationUsers, worklogprojects, executerusers);
        } else if(group_by.equals("project")){
             dataObjectList = searchByProject(аpplicationUsers, worklogprojects, executerusers);
        }
        ResponseDataObject responseData = new ResponseDataObject(group_by, this.tableHeaderObject, dataObjectList);
        //System.out.println( "this.dataObjectList.size: " + dataObjectList.size() + "\n" );
        return Response.ok(responseData).build(); 
    }

    private String getWorklogAutorQuery(List<String> executerusers){ //depricated?
        String worklogAuthor = "";
        if (!executerusers.isEmpty()){
            worklogAuthor = "worklogAuthor in (";
            for(String user : executerusers){
                worklogAuthor = worklogAuthor + "\"" + user + "\",";
            }
            worklogAuthor = worklogAuthor.substring(0, worklogAuthor.length() - 1) + ") AND";
        } else {
            System.out.println("getWorklogAutorQuery: no values for user; ");
        }
        return worklogAuthor;
    }

    private String getWorklogProjectQuery(List<String> worklogprojects){ // project in (HR, IMOS) AND 
        String worklogQuery = "";
        if (worklogprojects != null && !worklogprojects.isEmpty()){
            worklogQuery = "project in (";
            for(String project : worklogprojects){
                worklogQuery = worklogQuery + project + ",";
            }
            worklogQuery = worklogQuery.substring(0, worklogQuery.length() - 1) + ") AND ";
        }
        return worklogQuery;
    }

    private List<ApplicationUser> getApplicationUsers(List<String> executerusers){
        List<ApplicationUser> аpplicationUsers = new ArrayList<>();
        UserSearchParams searchParams = UserSearchParams.builder().includeActive(true).build();
        try {
            if (executerusers != null && !executerusers.isEmpty()){
                for(String user : executerusers){
                    аpplicationUsers.addAll(this.userSearchService.findUsers(user, searchParams));
                }
            }
            if (аpplicationUsers.isEmpty()){
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

    private String getUserAvatar(ApplicationUser аpplicationUser){
        String avatar = "";
        try {
            avatar = this.avatarService.getAvatarURL(аpplicationUser, аpplicationUser).toString(); // Avatar.Size size
        } catch (NullPointerException e){ 
            avatar = "";
            System.out.println(" avatarService exception: " +  e.toString() + "; ");
        }
        return avatar;
    }

    private Map<String, String> getUserKeyNameMap(List<ApplicationUser> аpplicationUsers){
        Map<String, String> userKeyNameMap = new TreeMap<String, String>();
        if (!аpplicationUsers.isEmpty()) {
            for (ApplicationUser аpplicationUser : аpplicationUsers) {
                userKeyNameMap.put(аpplicationUser.getKey(), аpplicationUser.getDisplayName());
            }
        }
        return userKeyNameMap;
    }

    private List<Issue> getIssuesList(List<String> worklogprojects, List<String> executerusers) throws SearchException{
        Collection<Long> issue_id_collection = new ArrayList<Long>();
        Query query = null;
        SearchResults<DocumentWithId> searchResults = null;
        Long end_date_timestamp = TimeUtil.toTimeStamp(this.end_date, "yyyy-MM-dd");
        Long dateOffset = 4838400000L;
        String new_end_date = (end_date_timestamp + dateOffset) < TimeUtil.nowTimeStamp() ? TimeUtil.fromTimeStamp(end_date_timestamp + dateOffset, "yyyy/MM/dd") : TimeUtil.now("yyyy/MM/dd");
        List<Issue> issueList = new ArrayList<Issue>();
        String worklogProjectQuery = getWorklogProjectQuery(worklogprojects);
        String worklogAuthorQuery = getWorklogAutorQuery(executerusers);
        try {
            System.out.println(worklogProjectQuery + worklogAuthorQuery + " worklogDate >= \"" + TimeUtil.dateTimeFormat(this.start_date, "yyyy-MM-dd", "yyyy/MM/dd") + "\" and worklogDate <= \"" + new_end_date + "\"");
            query = this.jqlQueryParser.parseQuery(worklogProjectQuery + worklogAuthorQuery + " worklogDate >= \"" + TimeUtil.dateTimeFormat(this.start_date, "yyyy-MM-dd", "yyyy/MM/dd") + "\" and worklogDate <= \"" + new_end_date + "\"");
        } catch (JqlParseException e){ System.out.println("jqlQueryParser exception:" + e.getParseErrorMessage().toString());
        } catch (NullPointerException e){ System.out.println("jqlQueryParser exception: " +  e.toString() + "; ");}
        try {
            searchResults = this.searchProvider.search(SearchQuery.create(query, this.аpplicationLoggedInUser), PagerFilter.getUnlimitedFilter());
        } catch (SearchException | NullPointerException e){System.out.println("searchProvider exception: " +  e.toString() + "; ");}
        for(DocumentWithId document : searchResults.getResults()){              
            try { 
                Long issue_id = Long.parseLong(document.getDocument().get("issue_id")); //fieldnames: task[key, summary] project[projid, projkey]
                if (issue_id != null){
                    issue_id_collection.add(issue_id);
                }
            } catch (NullPointerException e){System.out.println("Collection exception for:" + ": " + e.toString() + "; ");}
        }
        try {
            issueList = this.issueManager.getIssueObjects(issue_id_collection);
        } catch (NullPointerException e){System.out.println(" issueList exception:" + e.toString() + "; ");}
        return issueList;
    }

    private List<UserDataObject> searchByUser(List<ApplicationUser> аpplicationUsers, List<String> worklogprojects, List<String> executerusers) throws SearchException{
        Map<String, UserDataObject> dataObjectList = new TreeMap<String, UserDataObject>();
        Long end_date_timestamp = TimeUtil.toTimeStamp(this.end_date, "yyyy-MM-dd");
        Long start_date_timestamp = TimeUtil.toTimeStamp(this.start_date, "yyyy-MM-dd");
        List<Issue> issueList = getIssuesList(worklogprojects, executerusers);
        this.tableHeaderObject = new TableHeaderObject(start_date_timestamp, end_date_timestamp);
        if (this.show_empty){
            System.out.println(" show_empty: true (" + this.show_empty + ")");
            for (ApplicationUser аpplicationUser : аpplicationUsers){
                dataObjectList.put(аpplicationUser.getKey(), new UserDataObject(аpplicationUser.getKey(), аpplicationUser.getDisplayName(), getUserAvatar(аpplicationUser), this.tableHeaderObject.getEmptySpentList()));                
            }
        }    
        for(Issue issue : issueList){
            Project project = issue.getProjectObject();               
            for(Worklog worklog : this.worklogManager.getByIssue(issue)){
                Long check_start_date = worklog.getStartDate().getTime() + this.timeZoneOffset;
                ApplicationUser аpplicationUser = worklog.getAuthorObject();
                if (check_start_date >= start_date_timestamp && check_start_date <= end_date_timestamp + 82800000 && (аpplicationUsers.contains(аpplicationUser))){ // + 23hours
                    if (!dataObjectList.containsKey(worklog.getAuthorKey())){
                        dataObjectList.put(worklog.getAuthorKey(), new UserDataObject(аpplicationUser.getKey(), аpplicationUser.getDisplayName(), getUserAvatar(аpplicationUser), this.tableHeaderObject.getEmptySpentList()));
                    }
                    UserDataObject userDataObject = dataObjectList.get(worklog.getAuthorKey());
                    userDataObject.addIssueToProject(project.getId(), project.getKey(), project.getName(), this.avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL).toString(), issue.getId(), issue.getKey(), issue.getSummary(), issue.getIssueTypeId(), "Avatar relocate to front"); //String.valueOf(issueAvatarId)
                    userDataObject.spentTime(TimeUtil.fromTimeStamp(check_start_date, "yyyy")+TimeUtil.fromTimeStamp(check_start_date, "MM")+TimeUtil.fromTimeStamp(check_start_date, "dd"), worklog.getTimeSpent(), project.getId(), issue.getId());
                }
            }
        }
        return new ArrayList<UserDataObject>(dataObjectList.values());
    }

    private List<ProjectOfUserDataObject> searchByProject(List<ApplicationUser> аpplicationUsers, List<String> worklogprojects, List<String> executerusers) throws SearchException{
        Map<Long, ProjectOfUserDataObject> dataObjectList = new TreeMap<Long, ProjectOfUserDataObject>();
        Long end_date_timestamp = TimeUtil.toTimeStamp(this.end_date, "yyyy-MM-dd");
        Long start_date_timestamp = TimeUtil.toTimeStamp(this.start_date, "yyyy-MM-dd");
        List<Issue> issueList = getIssuesList(worklogprojects, executerusers);
        this.tableHeaderObject = new TableHeaderObject(getUserKeyNameMap(аpplicationUsers)); // NEED аpplicationUsers to List<String>
        //if (this.show_empty){            /*this TODO issue list of every project*/        }
        for(Issue issue : issueList){
            Project project = issue.getProjectObject();
            if (!dataObjectList.containsKey(issue.getProjectId())){
                dataObjectList.put(issue.getProjectId(), new ProjectOfUserDataObject(project.getId(), project.getKey(), project.getName(), this.avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL).toString(), this.tableHeaderObject.getEmptySpentList()));
            }
            for(Worklog worklog : this.worklogManager.getByIssue(issue)){
                Long check_start_date = worklog.getStartDate().getTime() + this.timeZoneOffset;
                ApplicationUser аpplicationUser = worklog.getAuthorObject();
                if (check_start_date >= start_date_timestamp && check_start_date <= end_date_timestamp + 82800000 && (аpplicationUsers.contains(аpplicationUser))){ // + 23hours
                    ProjectOfUserDataObject projectDataObject = dataObjectList.get(issue.getProjectId());
                    projectDataObject.addIssue(issue.getId(), issue.getKey(), issue.getSummary(), issue.getIssueTypeId(), "Avatar relocate to front"); //String.valueOf(issueAvatarId)
                    projectDataObject.spentTime(аpplicationUser.getKey(), worklog.getTimeSpent(), issue.getId());
                }
            }
        }
        return new ArrayList<ProjectOfUserDataObject>(dataObjectList.values());
    }

/*
    private List<UserDataObject> searchByUser2(List<ApplicationUser> аpplicationUsers, List<String> worklogprojects, List<String> executerusers) throws SearchException{
        List<UserDataObject> dataObjectList = new ArrayList<UserDataObject>();
        Collection<Long> issue_id_collection = new ArrayList<Long>();
        Query query = null;
        SearchResults<DocumentWithId> searchResults = null;
        Long issue_id = null;
        Long check_start_date = null;
        Long end_date_timestamp = TimeUtil.toTimeStamp(this.end_date, "yyyy-MM-dd");
        Long start_date_timestamp = TimeUtil.toTimeStamp(this.start_date, "yyyy-MM-dd");
        Long dateOffset = 4838400000L;
        Long issueAvatarId = 0L;
        String new_end_date = (end_date_timestamp + dateOffset) < TimeUtil.nowTimeStamp() ? TimeUtil.fromTimeStamp(end_date_timestamp + dateOffset, "yyyy/MM/dd") : TimeUtil.now("yyyy/MM/dd");
        List<Issue> issueList = new ArrayList<Issue>();
        String worklogProjectQuery = getWorklogProjectQuery(worklogprojects);
        this.tableHeaderObject = new TableHeaderObject(start_date_timestamp, end_date_timestamp);
        for(ApplicationUser аpplicationUser : аpplicationUsers){
                query = this.jqlQueryParser.parseQuery(worklogProjectQuery + "worklogAuthor = \"" + аpplicationUser.getUsername() + "\"" + " and worklogDate >= \"" + TimeUtil.dateTimeFormat(this.start_date, "yyyy-MM-dd", "yyyy/MM/dd") + "\" and worklogDate <= \"" + new_end_date + "\"");
            } catch (JqlParseException e){ System.out.println("jqlQueryParser exception:" + e.getParseErrorMessage().toString());
            } catch (NullPointerException e){ System.out.println("jqlQueryParser exception: " +  e.toString() + "; ");}
            try {
                searchResults = this.searchProvider.search(SearchQuery.create(query, this.аpplicationLoggedInUser), PagerFilter.getUnlimitedFilter());
            } catch (SearchException | NullPointerException e){System.out.println("searchProvider exception: " +  e.toString() + "; ");}
            UserDataObject userDataObject = new UserDataObject(аpplicationUser.getKey(), аpplicationUser.getDisplayName(), getUserAvatar(аpplicationUser), this.tableHeaderObject.getEmptySpentList());
            for(DocumentWithId document : searchResults.getResults()){
                System.out.println(document.getDocument().toString());
                try { 
                    issue_id = Long.parseLong(document.getDocument().get("issue_id")); //fieldnames: task[key, summary] project[projid, projkey]
                    if (issue_id != null){
                        issue_id_collection.add(issue_id);
                    }
                } catch (NullPointerException e){System.out.println("Collection exception for:" + issue_id.getClass() + ": " + e.toString() + "; ");}
            
            }
            try {
                issueList = this.issueManager.getIssueObjects(issue_id_collection);
                issue_id_collection.clear();
            } catch (NullPointerException e){System.out.println(" issueList exception:" + e.toString() + "; ");}
            
            for(Issue issue : issueList){
                Project project = issue.getProjectObject();
                
                try {
                    issueAvatarId = issue.getIssueType().getAvatar().getId();
                } catch (NullPointerException e){
                    issueAvatarId = 10320L; //exception on Epic and Store
                }
                userDataObject.addIssueToProject(project.getId(), project.getKey(), project.getName(), this.avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL).toString(), issue.getId(), issue.getKey(), issue.getSummary(), issue.getIssueTypeId(), String.valueOf(issueAvatarId));
                for(Worklog worklog : this.worklogManager.getByIssue(issue)){
                    if(!аpplicationUser.getKey().equals(worklog.getAuthorKey())) { continue; } // костыль - надо избавить этот этого итератора и сделать dataObject из Documents
                    check_start_date = worklog.getStartDate().getTime() + this.timeZoneOffset;
                    if (check_start_date >= start_date_timestamp && check_start_date <= end_date_timestamp + 82800000){ // + 23hours
                        userDataObject.spentTime(Integer.parseInt(TimeUtil.fromTimeStamp(check_start_date, "MM")+TimeUtil.fromTimeStamp(check_start_date, "dd")), worklog.getTimeSpent(), project.getId(), issue.getId());
                        //this.exception = this.exception + " spent for: " + аpplicationUser.getKey()+ "(" + chek_user + "), issue: " + issue.getKey() + " = " + worklog.getTimeSpent() + "; ";
                    }
                }
            }
            dataObjectList.add(userDataObject);
        }
        return dataObjectList;
    }
    */
}