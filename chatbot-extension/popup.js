'use strict';

let checkTimer;

$(document).ready(function () {
    $("#dialogPaired").dialog({
        autoOpen: false,
        resizable: false,
        height: 490,
        width: 490,
        modal: true,
        buttons: {
            "Да": function () {
                chrome.extension.sendMessage({cmd: 'paired', paired: "accept"}, function (backMessage) {
                    $("#dialogPaired").dialog("close");
                });
            },
            "Пропустить": function () {
                chrome.extension.sendMessage({cmd: 'paired', paired: "reject"}, function (backMessage) {
                    $("#dialogPaired").dialog("close");
                });
            },
            "Отмена заявки": function () {
                chrome.extension.sendMessage({cmd: 'paired', paired: "cancel"}, function (backMessage) {
                    $("#dialogPaired").dialog("close");
                });
            }
        }
    });

    startStatus();

});

function startStatus() {
    checkStatus();
    checkTimer = setInterval(function () {
        checkStatus();
    }, 1000);
}

function checkStatus() {
    chrome.storage.sync.get(['popup', 'buddyDescription', 'status'], function (stData) {
        switch (stData.popup) {
            case 'paired':
            {
                let buddyDescription = jQuery.parseJSON(stData.buddyDescription);
                $("#status").html("");
                chrome.storage.sync.set({popup: ""}, function () {
                    chrome.browserAction.setBadgeBackgroundColor({color: 'red'});
                    chrome.browserAction.setBadgeText({text: ""});
                });
                $("#buddyDescription").html(
                        buddyDescriptionHtml(buddyDescription)
                        );
                $("#dialogPaired").dialog('open');
                break;
            }
            case 'pairedOk':
            {
                $("#dialogPaired").dialog('close');
                let buddyDescription = jQuery.parseJSON(stData.buddyDescription);
                stopStatus();
                $("#status").html(
                        "<p>Ваша пара:</p><br>" + buddyDescriptionHtml(buddyDescription));
                dropRequestData();
                break;
            }
            default:
            {
                $("#status").html(
                        "<img src='images/coffee_wait.gif' style='width:100px;height:100px;'><br>" +
                        (stData.status !== undefined ? stData.status : "")
                        );
                $("#cancelButton").click(function () {
                    chrome.extension.sendMessage({cmd: 'cancel'}, function (backMessage) {
                        window.close();
                    });
                });
            }
        }
    });
}

function stopStatus() {
    clearInterval(checkTimer);
}

function dropRequestData() {
    chrome.storage.sync.set({popup: "", coffeeRequestId: "", status: ""}, function () {
    });
}

function buddyDescriptionHtml(buddyDescription) {
    return "<table><tr><td>ФИО</td><td>" +
            buddyDescription.lastname + " " +
            buddyDescription.firstname + " " +
            buddyDescription.middlename +
            "</td></tr><tr><td>Подразделение</td><td>" +
            buddyDescription.department +
            "</td></tr><tr><td>Место кофепития</td><td>" +
            buddyDescription.place +
            "</td></tr><tr><td>Должность</td><td>" +
            buddyDescription.position +
            "</td></tr><tr><td>Возможность угостить</td><td>" +
            (buddyDescription.canPay == "true" ? "Да" : "Нет") +
            "</td></tr><tr><td>На своем рабочем месте</td><td>" +
            (buddyDescription.myPlace == "true" ? "Да" : "Нет") +
            "</td></tr></table>";
}