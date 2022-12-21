package com.kolhozcustoms.jira.plugin.timetracker.models;

import java.util.List;
import java.util.Map;
import java.util.TreeMap; // sorted Map
import java.util.ArrayList;
import java.util.Arrays;
import com.kolhozcustoms.jira.plugin.timetracker.utils.TimeUtil;

import javax.xml.bind.annotation.*;
@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.NONE) //only annotated
public class ResponseDataObject{
    @XmlAttribute
    private String groupBy;
    private List<UserDataObject> userDataObjects = new ArrayList<UserDataObject>();
    private List<ProjectOfUserDataObject> projectDataObjects = new ArrayList<ProjectOfUserDataObject>();
    private List<?> dataObjects = new ArrayList<>();
    @XmlElement(name="tableHeader")
    private TableHeaderObject tableHeaderObject;
    public ResponseDataObject(){}
    public ResponseDataObject(String groupBy) {
        this.groupBy = groupBy;
    }

    public ResponseDataObject(String groupBy, TableHeaderObject tableHeaderObject, List<?> dataObjects) {
        this.groupBy = groupBy;
        this.tableHeaderObject = tableHeaderObject;
        this.dataObjects = dataObjects;
    }

    public String getGroupBy() {
        return this.groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }
    @XmlElement(name="tableData")
    public List<?> geDataObjects() {
        return this.dataObjects;
    }

    public void setDataObjects(List<?> dataObjects) {
        this.dataObjects = dataObjects;
    }

    public TableHeaderObject getTableHeaderObject() {
        return this.tableHeaderObject;
    }

    public void setTableHeaderObject(TableHeaderObject tableHeaderObject) {
        this.tableHeaderObject = tableHeaderObject;
    }
}