package com.kolhozcustoms.jira.plugin.timetracker.service;

import javax.inject.Inject;
import javax.inject.Named;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

@Named
public class PluginSettingsService {
    private final PluginSettings pluginSettings;
    private static final String PLUGIN_STORAGE_KEY = "com.kolhozcustoms.timeTracker.settings";
    
    @Inject
    public PluginSettingsService(@ComponentImport PluginSettingsFactory pluginSettingsFactory ){
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public String getParameter(String parametrName, String defaulValue){
        return pluginSettings.get(PLUGIN_STORAGE_KEY + parametrName) == null ? defaulValue : pluginSettings.get(PLUGIN_STORAGE_KEY + parametrName).toString();
    }

    public void setParameter(String parametrName, String parameterValue){
        this.pluginSettings.put(PLUGIN_STORAGE_KEY + parametrName, parameterValue);
    }
}
