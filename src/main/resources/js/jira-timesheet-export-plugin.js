function setExportMode(isAutoMode) {
    document.getElementById("startDate").disabled = isAutoMode;
    document.getElementById("endDate").disabled = isAutoMode;
    document.getElementById("startAuto").disabled = !isAutoMode || document.getElementById("runningMode").getAttribute('auto') == 'true';
    document.getElementById("stopAuto").disabled = !isAutoMode || document.getElementById("runningMode").getAttribute('auto') != 'true';
    document.getElementById("executeManualy").disabled = isAutoMode;
    document.getElementById("exportPeriod").disabled = !isAutoMode;
    document.getElementById("importPeriod").disabled = !isAutoMode;
    document.getElementById("startDate").required = !isAutoMode;
    document.getElementById("endDate").required = !isAutoMode;
}
window.onload = function () {
    setExportMode(true)
};

function setIncludeAllProjects(includeAllProjects) {
   document.getElementById("projects").disabled = includeAllProjects;
}