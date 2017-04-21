function setExportMode(isAutoMode) {
    $("#startDate").attr('disabled', isAutoMode);
    $("#endDate").attr('disabled', isAutoMode);
    $("#startAuto").attr('disabled', !isAutoMode || $("#runningMode").attr('auto') == 'true');
    $("#stopAuto").attr('disabled',  !isAutoMode || $("#runningMode").attr('auto') != 'true');
    $("#executeManualy").attr('disabled', isAutoMode);
    $("#exportPeriod").attr('disabled', !isAutoMode);
    $("#importPeriod").attr('disabled', !isAutoMode);
    $('#worklogStartDate').attr('disabled', isAutoMode);
    $('#worklogEndDate').attr('disabled', isAutoMode);
    $('#issueKeys').attr('disabled', isAutoMode);
    //$("#startDate").required = !isAutoMode;
    //$("#endDate").required = !isAutoMode;
}
window.onload = function () {
    setExportMode(true)
};

function setIncludeAllProjects(includeAllProjects) {
   $("#projects").attr('disabled', includeAllProjects);
}

function setIncludeAllUsers(includeAllUsers) {
    $("#users").attr('disabled', includeAllUsers);
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