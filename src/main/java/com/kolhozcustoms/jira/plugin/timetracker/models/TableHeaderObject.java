package com.kolhozcustoms.jira.plugin.timetracker.models;

import java.util.List;
import java.util.Map;
import java.util.TreeMap; // sorted Map
import java.util.ArrayList;
import java.util.Arrays;
import com.kolhozcustoms.jira.plugin.timetracker.utils.TimeUtil;

import javax.xml.bind.annotation.*;
@XmlRootElement(name = "tableHeader")
@XmlAccessorType(XmlAccessType.NONE) //only annotated
public class TableHeaderObject{
    private List<String> spentList;
    private final Map<String, Long> emptySpentList = new TreeMap<String, Long>();
    private List<List<String>> dayList = new ArrayList<List<String>>();
    private List<List<String>> monthList = new ArrayList<List<String>>();
    @XmlElement
    private List<List<String>> headerLevel1 = new ArrayList<List<String>>();
    @XmlElement
    private List<List<String>> headerLevel2 = new ArrayList<List<String>>();
    @XmlElement
    private List<List<String>> headerLevel3 = new ArrayList<List<String>>();
    public TableHeaderObject(){}
    public TableHeaderObject(Long start_date_timestamp, Long end_date_timestamp) {
        Integer current_months = Integer.parseInt(TimeUtil.fromTimeStamp(start_date_timestamp, "MM"));
        Integer day_in_month = 0;
        this.monthList.add(Arrays.asList(new String[] {TimeUtil.fromTimeStamp(start_date_timestamp, "MM"), TimeUtil.fromTimeStamp(start_date_timestamp, "MMMMM"), "0"}));
        for (Long day = start_date_timestamp; day <= end_date_timestamp; day = day + 86400000) {
            this.dayList.add(Arrays.asList(new String[] {TimeUtil.fromTimeStamp(day, "MM"), TimeUtil.fromTimeStamp(day, "dd"), "1"}));
            if (current_months != Integer.parseInt(TimeUtil.fromTimeStamp(day, "MM"))){ 
                current_months = Integer.parseInt(TimeUtil.fromTimeStamp(day, "MM"));
                //List<String> tmp = 
                this.monthList.get(this.monthList.size()-1).set(2, String.valueOf(day_in_month)); //get last monyh from list and change day count in month
                //tmp.set(2, String.valueOf(day_in_month));
                //this.monthList.set(this.monthList.size()-1, tmp);
                day_in_month = 0;
                this.monthList.add(Arrays.asList(new String[] {TimeUtil.fromTimeStamp(day, "MM"), TimeUtil.fromTimeStamp(day, "MMMMM"), "0"}));
            }
            day_in_month++;
            this.emptySpentList.put(TimeUtil.fromTimeStamp(day, "MM")+TimeUtil.fromTimeStamp(day, "dd"), 0L);
        }
        this.monthList.get(this.monthList.size()-1).set(2, String.valueOf(day_in_month));
        //this.headerLevel1.addAll(Arrays.asList(Arrays.asList("0", i18n.getText("time-tracker-report.timetracker.table.name"),"1"), Arrays.asList("0", "Задачи","1"), Arrays.asList("0", "Затрачено времени","1")));
        this.headerLevel1.addAll(this.monthList);
        //this.headerLevel2.addAll(Arrays.asList(Arrays.asList("0", "0", "Проект"), Arrays.asList("0", "0", "(Списания)"), Arrays.asList("0", "0", "Всего")));
        this.headerLevel2.addAll(this.dayList);
    }
    public TableHeaderObject(Map<String, String> userKeysNameMap) {
        //this.headerLevel1.addAll(Arrays.asList(Arrays.asList("0", i18n.getText("time-tracker-report.timetracker.table.name"),"1"), Arrays.asList("0", "Задачи","1"), Arrays.asList("0", "Всего","1")));
        for (Map.Entry<String, String> entry : userKeysNameMap.entrySet()) {
            this.headerLevel3.add(Arrays.asList(new String[] {entry.getKey(), entry.getValue(), "1"}));
            this.emptySpentList.put(entry.getKey(), 0L);
        }
    }
    public List<List<String>> getHeaderLevel1(){
        return this.headerLevel1;
    }

    public List<List<String>> getHeaderLevel2(){
        return this.headerLevel2;
    }

    public Map<String, Long> getEmptySpentList(){
        return this.emptySpentList;
    }
}