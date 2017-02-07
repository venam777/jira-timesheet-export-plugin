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

function setIncludeAllUsers(includeAllUsers) {
    document.getElementById("users").disabled = includeAllUsers;
}

function deleteBudget() {
    var nameField = $('#finance-project-name');
    var onlyDuplicatesFields = $('#only-duplicates');
    var financeProjectName = $(nameField).val();
    financeProjectName = financeProjectName.replace("#", "%23");
    if (financeProjectName) {
        $.ajax({
            url: AJS.params.baseURL + "/rest/timesheet/latest/admin/financeproject?name=" + financeProjectName + "&onlyDuplicates=" + $(onlyDuplicatesFields).prop('checked'),
            type: 'DELETE',
            success: function (response) {
                $(nameField).val('');
                $(onlyDuplicatesFields).prop('checked', false);
                alert("Было удалено " + response + " бюджетов");
            }
        });
    }
}