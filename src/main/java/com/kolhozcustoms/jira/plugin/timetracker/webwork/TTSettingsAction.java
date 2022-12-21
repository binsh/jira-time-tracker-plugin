package com.kolhozcustoms.jira.plugin.timetracker.webwork;

import lombok.Data; // аннотации для авто-создания геттеров и сеттеров
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kolhozcustoms.jira.plugin.timetracker.service.PluginSettingsService;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.sal.api.user.UserManager;
//import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Data
public class TTSettingsAction extends JiraWebActionSupport
{
    //private static final Logger log = LoggerFactory.getLogger(TTSettingsAction.class);
    private String tz_radio;
    private String time_offset;
    private String defaultTimeZone;
    private String userTimeZone;
    private Boolean currentUserIsAdmin;
    private PluginSettingsService pluginSettingsService;


    @Inject
    public TTSettingsAction(@ComponentImport UserManager userManager, PluginSettingsService pluginSettingsService, @ComponentImport TimeZoneManager timeZoneManager){
        if (getLoggedInUser() != null && userManager.isAdmin(getLoggedInUser().getUsername())) {
            this.currentUserIsAdmin = true;
        }
        this.pluginSettingsService = pluginSettingsService;
        this.tz_radio = this.pluginSettingsService.getParameter("tz_radio", "0");
        this.time_offset = this.pluginSettingsService.getParameter("time_offset", "0");
        this.defaultTimeZone = timeZoneManager.getDefaultTimezone().getID() + (timeZoneManager.getDefaultTimezone().getRawOffset() > 0 ? " +" : " ") + (timeZoneManager.getDefaultTimezone().getRawOffset()/3600000);
        this.userTimeZone = timeZoneManager.getLoggedInUserTimeZone().getID() + (timeZoneManager.getLoggedInUserTimeZone().getRawOffset() > 0 ? " +" : " ") + (timeZoneManager.getLoggedInUserTimeZone().getRawOffset()/3600000);
    }

    @Override
    public String execute() throws Exception {
        if (this.currentUserIsAdmin) {
            return super.execute(); //returns SUCCESS
        }
        return ERROR;
        //return super.execute(); //returns SUCCESS
    }
    
    public String doSave(){
        this.pluginSettingsService.setParameter("tz_radio", this.tz_radio);
        if (tz_radio.equals("3")) {
            this.pluginSettingsService.setParameter("time_offset", this.time_offset);
        }
        return SUCCESS;
    }
}
