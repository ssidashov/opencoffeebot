'use strict';


$(document).ready(function () {
    chrome.storage.sync.get(['telegram'], function (stData) {
        $("#telegramInput").val(stData.telegram);
    });
    $("#saveButton").click(function () {
        let telegram = $('#telegramInput').val();
        chrome.storage.sync.set({telegram: telegram}, function () {
        });
    });
});
