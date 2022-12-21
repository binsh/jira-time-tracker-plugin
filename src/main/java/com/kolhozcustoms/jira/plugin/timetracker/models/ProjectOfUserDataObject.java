package com.kolhozcustoms.jira.plugin.timetracker.models;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap; // sorted Map
import java.util.ArrayList;
import java.util.Arrays;
import com.kolhozcustoms.jira.plugin.timetracker.utils.TimeUtil;
import javax.xml.bind.annotation.*;
@XmlRootElement(name = "projects")
@XmlAccessorType(XmlAccessType.NONE)
public class ProjectOfUserDataObject{
    private Long projectID;
    @XmlAttribute
    private String projectKey;
    @XmlAttribute
    private String projectName;
    @XmlAttribute
    private String avatar;
    private Long sumTimeSpent = 0L;
    private Map<String, Long> emptySpentList;
    private Map<String, Long> spentListByObject;
    private Map<Long, IssueOfProjectDataObject> issueOfProjectDataObjectList;// = new TreeMap<Long, IssueOfProjectDataObject>();
    public ProjectOfUserDataObject(){}
    public ProjectOfUserDataObject(Long projectID, String projectKey, String projectName, String avatar, Map<String, Long> emptySpentList){
        this.projectID = projectID;
        this.projectKey = projectKey;
        this.projectName = projectName;
        this.avatar = avatar;
        this.emptySpentList = emptySpentList;
        this.spentListByObject = new TreeMap<String, Long>(emptySpentList);
        this.issueOfProjectDataObjectList = new TreeMap<Long, IssueOfProjectDataObject>();
    }

    public Void addIssue(Long issueId, String issueKey, String issueName, String issueType, String issueAvatar){
        if (this.issueOfProjectDataObjectList.containsKey(issueId)){
            //true
        } else {
            this.issueOfProjectDataObjectList.put(issueId, new IssueOfProjectDataObject(issueId, issueKey, issueName, issueType, issueAvatar, this.emptySpentList));
        }
        return null;
    }

    public Void spentTime(String objectId, Long spentTime, Long issueId){
        if (this.spentListByObject.containsKey(objectId)){
            Long tmp_timespent = this.spentListByObject.get(objectId);
            this.spentListByObject.put(objectId, tmp_timespent + spentTime);
        } 
        issueSpent(issueId, objectId, spentTime);
        return null;
    }

    public Void issueSpent(Long issueId, String objectId, Long spentTime){
        if (this.issueOfProjectDataObjectList.containsKey(issueId)){
            IssueOfProjectDataObject issue = this.issueOfProjectDataObjectList.get(issueId);
            if(!objectId.equals("")){
                issue.spentTime(objectId, spentTime);
            }
            this.issueOfProjectDataObjectList.put(issueId, issue);
        } 
        return null;
    }

    public String getAvatar(){
        return this.avatar;
    }

    public String getProjectName(){
        return this.projectName;
    }

    public String getProjectKey(){
        return this.projectKey;
    }

    @XmlAttribute(name="taskCount")
    public String getTaskCount(){
        return String.valueOf(issueOfProjectDataObjectList.size());
    }
    @XmlAttribute(name = "totalSpent")
    public String getTotalSpent(){
        if (this.sumTimeSpent == 0){
            Long sumTimeSpent = 0L;
            for (Long spent : this.spentListByObject.values()){
                sumTimeSpent = sumTimeSpent + spent;
            }
            return TimeUtil.getReadableTimeFromSeconds(sumTimeSpent);
        } else {
            return TimeUtil.getReadableTimeFromSeconds(this.sumTimeSpent);
        }
    }
    @XmlElement(name="issues")
    public List<IssueOfProjectDataObject> getAllIssues(){
        return new ArrayList<>(this.issueOfProjectDataObjectList.values());
    }

    @XmlElement(name="spentList")
    public List<String> getSpentList(){
        List<String> spentList = new ArrayList<String>();
        for (Long spent : this.spentListByObject.values()){
            spentList.add(TimeUtil.getReadableTimeFromSeconds(spent));
        }
        return spentList;
    }
}
