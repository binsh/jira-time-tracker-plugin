var issueTypes;
var getIssueTypes = function() {
    var issueTypes = {};
    AJS.$.ajax({
        url: AJS.contextPath() + '/rest/api/latest/issuetype/',
        dataType: 'json',
        success: function(response) {
            if (response.length > 0) {
                response.forEach(function(issue) {
                    console.log('response:' + AJS.contextPath() + '/rest/api/latest/issuetype/');
                    issueTypes[issue.id] = {'name': issue.name, 'iconUrl': issue.iconUrl};                   
                });
            }
            issueTypes['0'] = {'name': "Неизвестный тип", 'iconUrl': AJS.contextPath() + "/secure/viewavatar?size=xsmall&avatarId=10320&avatarType=issuetype"};
        },
        error: function() {
            console.log('Failed to load issue types');
        }
    });
    console.log(issueTypes);
    return issueTypes;
}

var initProjectLoad = function(url, target){
    console.log("checked " + project_checked);
        AJS.$.ajax({
            url: AJS.contextPath() + url,
            dataType: 'json',
            success: function(response) {
                console.log('response:' + AJS.contextPath() + url);
                console.log(response);
                var projectlisthtml = "";
                var checked = "";
                AJS.$('#' + target).html("");
                if (response.length > 0) { 
                    console.log("Есть проекты для: " + target);
                    response.forEach(function(project) {
                        if($("#project_" + project.key).length){
                            console.log("project_" + project.key + " exists");
                        }else{
                            if (project_checked.includes("project_" + project.key)){checked = "checked=\"checked\"";} else {checked = "";}
                            projectlisthtml = projectlisthtml + "<li class=\"check-list-item\" role=\"option\" id=\"project_" + project.key + "\"><label class=\"item-label checkbox\" resolved=\"\"><input type=\"checkbox\" tabindex=\"-1\" name=\"worklogprojects\" value=\"" + project.key + "\" " + checked + "><span class=\"aui-form-glyph\"></span><img height=\"16\" width=\"16\" class=\"icon\" align=\"absmiddle\" src=\"" + project.avatarUrls["16x16"] + "\">" + project.name + " (" + project.key + ")</label></li>";
                            console.log("project_" + project.key + " does not exists");
                        }
                    });
                    AJS.$('#' + target).html(projectlisthtml).removeClass("hidden");

                } else {
                    console.log("Нет проектов " + target);
                }
            },
            error: function() {
                console.log('Failed to serch user for query:' + url);
                AJS.$('#' + target).html("<li class=\"no-suggestions\">Ошибка загрузки проектов</li>").removeClass("hidden");
            }
        });
};

var initProjectSearch = function(query, target) { 
    if (query.length > 2) {
        AJS.$('#' + target).html("<li class=\"no-suggestions\">Поиск не работает, зайдите позже</li>").removeClass("hidden");
    } else {
        AJS.$('#' + target).html("").addClass("hidden");
    }
}

var initUserSearch = function(query, target) { 
    AJS.$('#' + target).find("input[type=checkbox]:checked").each(function () {
        $(this).parent().parent().detach().appendTo("#assignee-checked");
    });
    if($.trim($("#assignee-checked").html())!=''){
        $("#assignee-checked").removeClass("hidden");
    }

    if (query.length > 2) { 
        AJS.$.ajax({
            url: AJS.contextPath() + '/rest/api/latest/groupuserpicker?showAvatar=true&query=' + query + "&_=0",
            dataType: 'json',
            success: function(response) {
                console.log('response:' + AJS.contextPath() + '/rest/api/latest/groupuserpicker?showAvatar=true&query=' + query + "&_=0");
                console.log(response);
                var userlisthtml = "";
                AJS.$('#' + target).html("");
                if (response.users.users.length > 0) { 
                    console.log("Есть совпадения пользователей");
                    response.users.users.forEach(function(user) {
                        if($("#user_" + user.key).length){
                            console.log("user_" + user.key + " exists");
                        }else{
                            userlisthtml = userlisthtml + "<li class=\"check-list-item\" role=\"option\" id=\"user_" + user.key + "\"><label class=\"item-label checkbox\" resolved=\"\"><input type=\"checkbox\" tabindex=\"-1\" name=\"executerusers\" value=\"" + user.name + "\"><span class=\"aui-form-glyph\"></span><img height=\"16\" width=\"16\" class=\"icon rounded\" align=\"absmiddle\" src=\"" + user.avatarUrl + "\">" + user.html + "</label></li>";
                            console.log("user_" + user.key + " does not exists");
                        }                        
                    });
                    if (userlisthtml == ""){
                        userlisthtml = "<li class=\"no-suggestions\">Нет новых совпадений</li>";
                    }
                }
                if (response.groups.groups.length > 0) { 
                    console.log("Есть совпадения групп");
                    response.groups.groups.forEach(function(user) {
                        userlisthtml = userlisthtml + "<li class=\"check-list-item\" role=\"option\" id=\"group_" + user.name + "\"><label class=\"item-label checkbox\" resolved=\"\"><input type=\"checkbox\" tabindex=\"-1\" name=\"groups[]\" value=\"" + user.name + "\"><span class=\"aui-form-glyph\"></span><img height=\"16\" width=\"16\" class=\"icon rounded\" align=\"absmiddle\" src=\"" + AJS.contextPath() + "/images/icons/icon_groups_16.png\">" + user.html + "</label></li>";
                    });
                }
                if (response.users.users.length == 0 && response.groups.groups.length == 0) {
                    AJS.$('#' + target).html("<li class=\"no-suggestions\">Нет совпадений</li>").removeClass("hidden");
                    console.log("Нет совпадений");
                } else {
                    AJS.$('#' + target).html(userlisthtml).removeClass("hidden");

                }
            },
            error: function() {
                console.log('Failed to serch user for query:' + query);
                AJS.$('#' + target).html("<li class=\"no-suggestions\">Ошибка поиска</li>").removeClass("hidden");
            }
        });
    } else {
        AJS.$('#' + target).html("").addClass("hidden");
    }
};

var dataLoad = function(source, target, data){
    $.ajax({   
        type: "POST",
        data : data,
        url: AJS.contextPath() + source,
        dataType: 'json',
        success: function(response){
            console.log('data: ' + response + ';');
            $('#headerLevel1, #headerLevel2, #headerLevel3').hide();
            $('#' + target).children('tbody[class*=tbody_level]:not(.template)').remove();
            if(response.hasOwnProperty('tableHeader')){
                for (var key in response.tableHeader) {
                    let header = response.tableHeader[key];
                    if (header.length > 0) { 
                        let th = $('#' + key).children('.template').first().clone(true).removeClass('template')
                        $('#' + key).children('.data_column:not(.template)').remove();
                        header.forEach(function(headerLevel) {
                            $('#' + key).append(th.clone().attr("colspan", headerLevel[2]).html(headerLevel[1]));
                        });
                        $('#' + key).show();
                    }
                }
            }
            if (response.groupBy == "user"){      
  
                if (response.tableData.length > 0) {
                    response.tableData.forEach(function(user) {
                        let tbody_level1 = $('#' + target).children('.tbody_level1.template').first().clone(true).removeClass('template');
                        tbody_level1.children('tr').children('.project-expander').attr('aria-controls', "tr_" + user.userKey).append(' ' + user.userName);
                        tbody_level1.children('tr').children('.project-expander').children('.icon').attr('src', user.avatar);
                        tbody_level1.children('tr').children('.taskcount').html(user.taskCount);
                        tbody_level1.children('tr').children('.totalspent').html(user.totalSpent);
                        let td = tbody_level1.children('tr').children('.data_column.template').first().clone(true).removeClass('template');
                        user.spentList.forEach(function(spent) {
                            tbody_level1.children('tr').append(td.clone().html(spent));
                        });
                        $('#' + target).append(tbody_level1.clone());
                        let tbody_level2 = $('#' + target).children('.tbody_level2.template').first().clone(true).removeClass('template');
                        tbody_level2.attr('id', "tr_" + user.userKey);
                        
                        user.projects.forEach(function(project) {
                            let tr_level2 = tbody_level2.children('.tr_level2.template').first().clone(true).removeClass('template');
                            tr_level2.children('.issue-expander').attr('aria-controls', "tr_" + project.projectKey).append(' ' + project.projectName);
                            tr_level2.children('.issue-expander').children('.icon').attr('src', project.avatar);
                            tr_level2.children('.taskcount').html(project.taskCount);
                            tr_level2.children('.totalspent').html(project.totalSpent);
                            let td2 = tr_level2.children('.data_column.template').first().clone(true).removeClass('template');

                            project.spentList.forEach(function(spent) {
                                tr_level2.append(td2.clone().html(spent));
                            });
                            tbody_level2.append(tr_level2.clone());

                            project.issues.forEach(function(issue) {
                                let tr_level3 = tbody_level2.children('.tr_level3.template').first().clone(true).removeClass('template').addClass("inner_tr_" + project.projectKey);
                                if(!issueTypes.hasOwnProperty(issue.issueType)){ 
                                    issue.issueType = '0';
                                }
                                tr_level3.children('.issue').children('.icon').attr('src', issueTypes[issue.issueType]['iconUrl']).attr('title', issueTypes[issue.issueType]['name']);
                                tr_level3.children('.issue').children('a').attr('href', AJS.contextPath() + "/browse/" + issue.issueKey).html(issue.issueName + " (" + issue.issueKey + ")");
                                tr_level3.children('.taskcount').html("(" + issue.spentCount + ")");
                                tr_level3.children('.totalspent').html(issue.totalSpent);

                                let td3 = tr_level3.children('.data_column.template').first().clone(true).removeClass('template');
                                issue.spentList.forEach(function(spent) {
                                    tr_level3.append(td3.clone().html(spent));
                                });
                                tbody_level2.append(tr_level3.clone());
                            });
                        });
                        $('#' + target).append(tbody_level2.clone());
                    });
                }
            } else if(response.groupBy == "project"){
                if (response.tableData.length > 0) {
                    response.tableData.forEach(function(project) {
                        let tbody_level1 = $('#' + target).children('.tbody_level1.template').first().clone(true).removeClass('template');
                        tbody_level1.children('tr').children('.project-expander').attr('aria-controls', "tr_" + project.projectKey).append(' ' + project.projectName);
                        tbody_level1.children('tr').children('.project-expander').children('.icon').attr('src', project.avatar);
                        tbody_level1.children('tr').children('.taskcount').html(project.taskCount);
                        tbody_level1.children('tr').children('.totalspent').html(project.totalSpent);
                        let td = tbody_level1.children('tr').children('.data_column.template').first().clone(true).removeClass('template');
                        project.spentList.forEach(function(spent) {
                            tbody_level1.children('tr').append(td.clone().html(spent));
                        });
                        $('#' + target).append(tbody_level1.clone());
                        let tbody_level2 = $('#' + target).children('.tbody_level2.template').first().clone(true).removeClass('template').attr('id', "tr_" + project.projectKey );
                        project.issues.forEach(function(issue) {
                            let tr_level3 = tbody_level2.children('.tr_level3.template').first().clone(true).removeClass('template').show(); //.addClass("inner_tr_" + project.projectKey).show();
                            if(!issueTypes.hasOwnProperty(issue.issueType)){ 
                                issue.issueType = '0';
                            }
                            tr_level3.children('.issue').children('.icon').attr('src', issueTypes[issue.issueType]['iconUrl']).attr('title', issueTypes[issue.issueType]['name']);
                            tr_level3.children('.issue').children('a').attr('href', AJS.contextPath() + "/browse/" + issue.issueKey).html(issue.issueName + " (" + issue.issueKey + ")");
                            tr_level3.children('.taskcount').html("(" + issue.spentCount + ")");
                            tr_level3.children('.totalspent').html(issue.totalSpent);
                            let td3 = tr_level3.children('.data_column.template').first().clone(true).removeClass('template');
                            issue.spentList.forEach(function(spent) {
                                tr_level3.append(td3.clone().html(spent));
                            });
                            tbody_level2.append(tr_level3.clone());
                        });
                        $('#' + target).append(tbody_level2.clone());
                    });
                }
            }
        }
    });
};

AJS.toInit(function(){
    AJS.$('#assignee-input').on("keyup", function(a) {
        var query = $(this).val(); 
        var target = $(this).attr("aria-controls");
        initUserSearch(query, target);
        return false;
    }).on("keydown", function(a) {
        if(a.keyCode == 13){  a.preventDefault(); return false; }
    });

    AJS.$('#project-input').on("keyup", function(a) {
        var query = $(this).val(); 
        var target = $(this).attr("aria-controls");
        initProjectSearch(query, target);
        return false;
    }).on("keydown", function(a) {
        if(a.keyCode == 13){  a.preventDefault(); return false; }
    });

    AJS.$('div.aui-list').on("click", 'label.checkbox', function() {
        var checkbox = $(this).children("input");
        if ($(checkbox).prop("checked")){
            $(checkbox).prop("checked", false);
            console.log('uncheck ' + $(checkbox).prop("checked") + " " + $(checkbox).val());
        } else {
            $(checkbox).prop("checked", true);
            console.log('check ' + $(checkbox).prop("checked") + " " + $(checkbox).val());
        }
        return false;
    });

    AJS.$('#time-report-table, #time-report-table-by-project').on("click", ".project-expander", function(){
        var target = $(this).attr("aria-controls");
        AJS.$('#' + target).slideToggle('fast');
    });
    AJS.$('#time-report-table').on("click", ".issue-expander", function(){
        var target = $(this).attr("aria-controls");
        $(this).parent().siblings(".inner_" + target).slideToggle('fast');
    });
    
    AJS.$('#filter-form').on("submit", function(a){
        //var source = $(this).attr("action"); 
        var target = $(this).attr("aria-controls");
        var data = $(this).serialize();
        dataLoad("/rest/timetracker/latest/report/", target, data); //source , target, data
        a.preventDefault(); return false;
    });
});


AJS.$(document).ready(function() {
    issueTypes = getIssueTypes();
    var username_dropdown = document.querySelector('#username-dropdown');
    var observer = new MutationObserver(function(mutations) {
        AJS.$('#assignee-suggestions').find("input[type=checkbox]:checked").each(function () {
            $(this).parent().parent().detach().appendTo("#assignee-checked");
        });
        AJS.$('#assignee-checked').find("input[type=checkbox]:not(:checked)").each(function () {
            $(this).parent().parent().detach();
        });
        if($.trim($("#assignee-checked").html())==''){
            $("#assignee-checked").addClass("hidden");
        }else{
            $("#assignee-checked").removeClass("hidden");
        }
        console.log('open changed');
    });
    observer.observe(username_dropdown, { 
        attributes: true, 
        attributeFilter: ['open'] }
    );

    initProjectLoad("/rest/api/latest/project?recent=1", "project-recent");
    initProjectLoad("/rest/api/latest/project", "project-all");
    if($.trim($("#assignee-checked").html())==''){
        $("#assignee-checked").addClass("hidden");
    }
    var data = $('#filter-form').serialize();
    dataLoad("/rest/timetracker/latest/report/", $('#filter-form').attr("aria-controls"), data);

        //AJS.$('#start-date').datePicker({'overrideBrowserDefault': true, 'firstDay': -1, 'languageCode':'ru', 'dateFormat':'dd.mm.yy'});
        //AJS.$('#end-date').datePicker({'overrideBrowserDefault': true, 'firstDay': -1, 'languageCode':'ru', 'dateFormat':'dd.mm.yy'});
});
