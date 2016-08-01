function setExportMode(isAutoMode) {
    document.getElementById("exportPeriod").disabled = isAutoMode;
    document.getElementById("importPeriod").disabled = isAutoMode;
    document.getElementById("startDate").disabled = isAutoMode;
    document.getElementById("endDate").disabled = isAutoMode;
    document.getElementById("saveButton").value = isAutoMode ? "Сохранить" : "Выполнить";
}
window.onload = function(){
    setExportMode(true)
};