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
@XmlRootElement(name = "users")
@XmlAccessorType(XmlAccessType.NONE) //only annotated
public class UserDataObject{
    @XmlAttribute
    private String userName;
    @XmlAttribute
    private String userKey;
    @XmlAttribute
    private String avatar;
    private Long sumTimeSpent = 0L;
    private Map<String, Long> emptySpentList;
    private Map<String, Long> spentListByObject;
    private Map<Long, ProjectOfUserDataObject> projectOfUserDataObjectList = new TreeMap<Long, ProjectOfUserDataObject>();
    public UserDataObject(){}
    public UserDataObject(String userKey, String userName, String avatar, Map<String, Long> emptySpentList){
        this.userName = userName;
        this.avatar = avatar;
        this.userKey = userKey;
        this.emptySpentList = emptySpentList;
        this.spentListByObject = new TreeMap<String, Long>(emptySpentList);
    }

    public Void addIssueToProject(Long projectId, String projectKey, String ProjectName, String projectAvatar, Long issueId, String issueKey, String issueName, String issueType, String issueAvatar){
        ProjectOfUserDataObject project;
        if (this.projectOfUserDataObjectList.containsKey(projectId)){
            project = this.projectOfUserDataObjectList.get(projectId);
        } else {
            project = new ProjectOfUserDataObject(projectId, projectKey, ProjectName, projectAvatar, this.emptySpentList);
        }
        project.addIssue(issueId, issueKey, issueName, issueType, issueAvatar);
        this.projectOfUserDataObjectList.put(projectId, project);
        return null;
    }

    public Void spentTime(String objectId, Long spentTime, Long projectId, Long issueId){
        //System.out.println( "expect crash userObject.spent " + issueId );
        Long tmp_timespent = this.spentListByObject.get(objectId);
        this.spentListByObject.put(objectId, tmp_timespent + spentTime); 
        if (projectId > 0){
            projectSpent(projectId, objectId, spentTime, issueId);
        }
        return null;
    }

    public Void spentTime(Long spentTime){
        this.sumTimeSpent = this.sumTimeSpent + spentTime;
        return null;
    }

    public Void projectSpent(Long projectId, String objectId, Long spentTime, Long issueId){
        if (this.projectOfUserDataObjectList.containsKey(projectId)){
            ProjectOfUserDataObject project = this.projectOfUserDataObjectList.get(projectId);
            project.spentTime(objectId, spentTime, issueId);
            this.projectOfUserDataObjectList.put(projectId, project);
        } 
        return null;
    }

    public String getAvatar(){
        return this.avatar;
    }

    public String getUserName(){
        return this.userName;
    }

    public String getUserKey(){
        return this.userKey;
    }
    @XmlElement(name="projects")
    public List<ProjectOfUserDataObject> getAllProjects(){
        return new ArrayList<>(this.projectOfUserDataObjectList.values());
    }
    @XmlElement(name="spentList")
    public List<String> getSpentList(){
        List<String> spentList = new ArrayList<String>();
        for (Long spent : this.spentListByObject.values()){
            spentList.add(TimeUtil.getReadableTimeFromSeconds(spent));
        }
        return spentList;
    }

    @XmlAttribute(name="taskCount")
    public String getTaskCount(){
        Integer taskCount = 0;
        for (ProjectOfUserDataObject project : this.projectOfUserDataObjectList.values()){
            taskCount = taskCount + Integer.parseInt(project.getTaskCount());
        }
        return String.valueOf(taskCount);
    }

    @XmlAttribute(name="totalSpent")
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
}