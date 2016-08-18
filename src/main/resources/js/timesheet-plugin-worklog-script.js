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
   customizeWorklogsStyle();
   window.setInterval(function() {
       customizeWorklogsStyle();
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

function customizeWorklogsStyle() {
    $('div[id^="worklog-"]').each(function(){
        disableButtons($(this));
        colorizeWorklogs($(this));
    });
}

function disableButtons(node) {
    $(node).hover(function () {
        if ($(this).find('p').text().search(/.*\| Статус: Утверждено/) != -1) {
            $(this).find('.edit-worklog-trigger').css('display', 'none');
            $(this).find('.delete-worklog-trigger').css('display', 'none');
        }
    });
}

function colorizeWorklogs(node) {
    var text = $(node).find('p').text();
    if (text.search(/.*\| Статус: Утверждено/) != -1) {
        $(node).find('.item-details').css('background-color', '#BADBAD');
    } else if (text.search(/.*\| Статус: Не просмотрено/) != -1) {
        $(node).find('.item-details').css('background-color', '#E0DDDD');
    } else if (text.search(/.*\| Статус: Отклонено.*/) != -1) {
        $(node).find('.item-details').css('background-color', '#F36D6D');
    }
}