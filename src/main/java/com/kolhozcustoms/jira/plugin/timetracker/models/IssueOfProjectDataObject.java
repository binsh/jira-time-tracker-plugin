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
@XmlRootElement(name = "issues")
@XmlAccessorType(XmlAccessType.NONE)
public class IssueOfProjectDataObject{
    private Long issueID;
    @XmlAttribute
    private String issueKey;
    @XmlAttribute
    private String issueName;
    @XmlAttribute
    private String issueType;
    @XmlAttribute
    private String avatar;
    @XmlAttribute
    private Integer spentCount;
    private Long sumTimeSpent = 0L;
    private Map<String, Long> spentListByObject;
    Map<String, UserDataObject> userDataObjectList = new TreeMap<String, UserDataObject>();
    public IssueOfProjectDataObject(){}
    public IssueOfProjectDataObject(Long issueID, String issueKey, String issueName, String issueType, String avatar, Map<String, Long> emptySpentList){
        this.issueID = issueID;
        this.issueKey = issueKey;
        this.issueName = issueName;
        this.issueType = issueType;
        this.avatar = avatar;
        this.spentListByObject = new TreeMap<String, Long>(emptySpentList);
        this.spentCount = 0;
    }

    public Void spentTime(String objectId, Long spentTime){
        Long tmp_timespent = this.spentListByObject.get(objectId);
        this.spentListByObject.put(objectId, tmp_timespent + spentTime); 
        this.spentCount = this.spentCount + 1;
        return null;
    }

    public Void userSpent(String userKey, Long spentTime){
        if (this.userDataObjectList.containsKey(userKey)){
            UserDataObject user = this.userDataObjectList.get(userKey);
            user.spentTime(spentTime);
            this.userDataObjectList.put(userKey, user);
        } 
        return null;
    }

    public String getAvatar(){
        return this.avatar;
    }

    public String getIssueName(){
        return this.issueName;
    }

    public String getIssueType(){
        return this.issueType;
    }

    public String getIssueKey(){
        return this.issueKey;
    }

    public String getSpentCount(){
        return String.valueOf(this.spentCount);
    }
    @XmlElement(name="spentList")
    public List<String> getSpentList(){
        List<String> spentList = new ArrayList<String>();
        for (Long spent : this.spentListByObject.values()){
            spentList.add(TimeUtil.getReadableTimeFromSeconds(spent));
        }
        return spentList;
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

    @XmlElement(name="users")
    public List<UserDataObject> getAllUsers(){
        if (this.userDataObjectList.size() > 0){
            return new ArrayList<>(this.userDataObjectList.values());
        } else {
            return null;
        }
    }
}