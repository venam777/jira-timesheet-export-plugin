/*AJS.$(document).ready (
    function() {
        $('#worklog-tabpanel').live('click', function(){
            $.ajax({
                url: $(this).attr('href'),
                success: function(){
                    $('div[id^="worklog-"]').hover(function () {
                        if ($(this).find('.panelHeader').text() == 'Не просмотрено') {
                            $(this).find('.edit-worklog-trigger').css('display', 'none');
                            $(this).find('.delete-worklog-trigger').css('display', 'none');
                        }
                    })
                }
            })
        })
    }
);*/

oldfunc = window.onload;
window.onload = function(event) {
    if (oldfunc) {
        oldfunc(event);
    }
   window.setInterval(function() {
       disableButtons($('div[id^="worklog-"]'));
   }, 1000);
};


/*function enableMutationObserver() {
    var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;
    var observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.addedNodes.length > 0) {
                for (var i = 0; i < mutation.addedNodes.length; ++i) {
                    var node = mutation.addedNodes[i];
                    if ($(node) != undefined && $(node).attr('id') != undefined && $(node).attr('id').search(/worklog-\d{1,}/) != -1) {
                        disableButtons(node);
                    }
                }
            }
        });
    });
    observer.observe(document.body, {childList: true,  subtree: true});
}*/

function disableButtons(node) {
    $(node).hover(function () {
        if ($(this).find('.panelHeader').text().search(/^Утверждено/) != -1) {
            $(this).find('.edit-worklog-trigger').css('display', 'none');
            $(this).find('.delete-worklog-trigger').css('display', 'none');
        }
    });
}