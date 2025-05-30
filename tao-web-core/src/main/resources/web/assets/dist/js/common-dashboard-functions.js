function updateRservice(task){

    // add icon
    var tasklogs = '';
    if(task.data.Payload === "DONE" || task.data.Payload === "FAILED"){
        tasklogs = ' <button id="task_log_info_'+task.source+'" class="fa fa-info-circle log-info" value="'+task.source+'"></button>';
        tasklogs +='<div id="task_log_modal'+task.source+'" class="log-modal"><div class="log-modal-content"><div class="log-title-button"><div class="log-title"><h4 class="title">Task Log</h4></div><button type="button" class="log-close">&times;</button></div><p class="log_text">'+'</p></div></div>';
    
        if($("#task_log_info_" + task.source).length === 0){
            $("#"+ task.source +".one-task strong:first").after(tasklogs);
        }
    }

    // fill collapsible
	var time =  'Started: [' + niceIsoTime(task.taskStart) +  ']' + (typeof task.taskEnd === "undefined" ? ', updated [' + niceIsoTime(task.lastUpdated) : ', ended ['+ niceIsoTime(task.taskEnd)) + ']';
    var htmlCollapse = 
        '<div style="margin-left: 20px;">' +
                '<span>' + time + '</span><br>' +
                ((task.data.host && task.data.host !== null)? ('<span>@host: <strong>' + task.data.host + '</strong></span>') : '') +
        '</div>';

    $("#"+ task.source + ".one-task .collapsible").html(htmlCollapse);

    var jd = $("#"+ task.source).parents().eq(2).data("jobData");
    if(typeof task.lastUpdated !== 'undefined'){
        jd.taskSummaries.find(x => x.taskId === parseInt(task.source)).lastUpdated = task.lastUpdated;
    }
    if(typeof jd.taskSummaries.find(x => x.taskId === parseInt(task.source)).host === 'undefined'){
        jd.taskSummaries.find(x => x.taskId === parseInt(task.source)).host = task.data.host;
    }
}

function updateTask(task){

    var time;
    var taskStatus = task.taskStatus;
    var statusIcon = ui_getTaskStatusIcon(taskStatus);
    
    if (taskStatus === "RUNNING") {
        var percent = Number.isInteger(task.percentComplete) ? (task.percentComplete < 0 ? 0 : task.percentComplete) : Math.round(task.percentComplete*100)/100;
        htmlContent = "<div class='progress history'>" + "<div class='progress-bar progress-bar-info' role='progressbar' data-toggle='tooltip' data-placement='bottom' onmouseover='$(this).tooltip();' style='width:" + percent +"%'>" + percent + " %</div></div>";    
        if($("#"+ task.taskId + ".one-task .progress").length === 0){
            $("#"+ task.taskId + ".one-task strong").after(htmlContent);
        }else{
            $("#"+ task.taskId + ".one-task .progress").replaceWith(htmlContent);
        }
       	time = ('Started: [' + niceIsoTime(task.taskStart)+ ']' + ((typeof task.taskEnd === "undefined") ? (', updated [' + niceIsoTime(task.lastUpdated) + ']') : ' ,ended ['+ niceIsoTime(task.taskEnd) + ']')) ;
    }else {
		time =  'Started: [' + niceIsoTime(task.taskStart) +  ']' + (typeof task.taskEnd === "undefined" ? '], updated [' + niceIsoTime(task.lastUpdated) + ']' : ', ended ['+ niceIsoTime(task.taskEnd)) + ']';
    }

    $($("#"+ task.taskId +".one-task .icon")[0]).replaceWith(statusIcon);
    $("#"+ task.taskId + ".one-task strong").html(task.componentName);

    var htmlCollapse = 
        '<div style="margin-left: 20px;">' +
                '<span>' + time + '</span><br>' +
                ((task.host && task.host !== null)? ('<span>@host: <strong>' + task.host + '</strong></span>') : '') +
        '</div>';

    $("#"+ task.taskId + ".one-task .collapsible").html(htmlCollapse);

    var jd = $("#"+ task.taskId).parents().eq(2).data("jobData");
    jd.taskSummaries.find(x => x.taskId === parseInt(task.taskId)).taskStatus = task.taskStatus;

    if(typeof task.lastUpdated !== 'undefined'){
        jd.taskSummaries.find(x => x.taskId === parseInt(task.taskId)).lastUpdated = task.lastUpdated;
    }
}

var ui_getTaskStatusIcon = function(taskStatus){
    var html = '<i class="icon fa fa-ban fa-fw" aria-hidden="true"></i>';
    if (taskStatus === "DONE") html = '<i class="icon fa fa-check fa-fw" aria-hidden="true" style="color: green;"></i>';
    if (taskStatus === "PENDING_FINALISATION") html = '<i class="icon fa fa-spinner fa-fw fa-spin" aria-hidden="true" style="color: orange;"></i>';
    if (taskStatus === "RUNNING") html = '<i class="icon fa fa-cog fa-fw fa-spin" aria-hidden="true" style="color: orange;"></i>';
    if (taskStatus === "UNDETERMINED") html = '<i class="icon fa fa-question fa-fw" aria-hidden="true" style="color: lightgrey;"></i>';
    if (taskStatus === "QUEUED_ACTIVE") html = '<i class="icon fa fa-forward fa-fw fa-fade" aria-hidden="true" style="color: grey;"></i>';
    return html;
}

var ui_getJobStatusClass = function(jobStatus){
    var className = ''; //UNDETERMINED
    if(jobStatus === "DONE") className = ' success';
    if(jobStatus === "FAILED") className = ' fail';
    if(jobStatus === "QUEUED_ACTIVE") className = ' queued-active';
    if(jobStatus === "SUSPENDED") className = ' suspended';
    if(jobStatus === "CANCELLED") className = ' cancelled';
    if(jobStatus === "RUNNING") className = ' running';
    return className;
};

function getTime(time){
        	
    var str = '';
    var seconds = parseInt(time, 10);
    var days = Math.floor(seconds / (3600*24));
    seconds  -= days*3600*24;
    var hrs   = Math.floor(seconds / 3600);
    seconds  -= hrs*3600;
    var mnts = Math.floor(seconds / 60);
    seconds  -= mnts*60;

    str = ( ((days != 0)? (days+'d '): '') + ((hrs != 0)? (hrs+'h '): '') + ((mnts != 0)? (mnts+'m'): ''));
    return str;
}