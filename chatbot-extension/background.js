'use strict';
let serviceDomain = "http://ec2-35-160-232-7.us-west-2.compute.amazonaws.com:8080";
let registerURL = "/api/register";
let coffeeWantURL = "/api/request";
let causeAlreadyExists = "Already registered request with id ";

let coffeeCheckTimer;

chrome.runtime.onInstalled.addListener(function () {

    chrome.storage.sync.set({popup: "", coffeeRequestId: "", status: ""}, function () {
    });

    $.post("https://rumskapp227.open.ru:8743/auth/realms/cofeebot/protocol/openid-connect/token",
            {"client_id": "cofeebot-extension", "grant_type": "password", "username": "extension", "password": "qwerty-3"})
            .done(function (data) {
                let accessToken = data["access_token"];
                chrome.storage.sync.set({accessToken: accessToken}, function () {
                });
            });

    chrome.declarativeContent.onPageChanged.removeRules(undefined, function () {
        chrome.declarativeContent.onPageChanged.addRules([{
                conditions: [new chrome.declarativeContent.PageStateMatcher({
                        pageUrl: {hostEquals: 'net.open.ru'},
                    })],
                actions: [new chrome.declarativeContent.ShowPageAction()]
            }]);
    });

    chrome.extension.onMessage.addListener(function (request, sender, callback) {
        callback('backMsg');
        if (request.cmd == 'registerUser') {
            registerUser();
        }
        if (request.cmd == 'wantCoffee') {
            wantCoffee(request.form);
        }
        if (request.cmd == 'paired') {
            pairDecision(request.paired);
        }
        if (request.cmd == 'cancel') {
            cancelRequest();
        }
    });

    chrome.storage.onChanged.addListener(function (changes, namespace) {
        for (var key in changes) {
            var storageChange = changes[key];
            console.log(key + '=' + storageChange.newValue);
        }
    });
});

function registerUser() {
    chrome.storage.sync.get(['accessToken', 'fio', 'account', 'lastname', 'firstname', 'middlename', 'telegram'], function (stData) {
        $.get("https://portalfc/SearchEmps.aspx",
                {"term": stData.fio, "r": "0.3128895135561418"})
                .done(function (data) {
                    let empls = jQuery.parseJSON(data)["data"];
                    $.each(empls, function (key, value) {
                        let empl = empls[key];
                        if (empl["data"]["Login"].replace("\\\\", "\\").toLowerCase() === stData.account) {
                            $.ajax({
                                type: 'POST',
                                cache: false,
                                url: serviceDomain + registerURL,
                                dataType: "json",
                                crossDomain: true,
                                contentType: "application/json; charset=utf-8",
                                data: JSON.stringify({login: stData.account,
                                    "firstname": stData.firstname,
                                    "lastname": stData.lastname,
                                    "middlename": stData.middlename,
                                    "location": empl["data"]["Address"],
                                    "workspace": "Комната: " + empl["data"]["Room"]
                                            + (empl["data"]["WorkPlace"] !== '' ?
                                                    " (раб. место " + empl["data"]["WorkPlace"] + ")" : ""),
                                    "department": empl["data"]["DepName"],
                                    "position": empl["data"]["Position"],
                                    "telegramAccount": stData.telegram
                                }),
                                beforeSend: function (xhr) {
                                    xhr.setRequestHeader("Authorization", "Bearer " + stData.accessToken);
                                    xhr.setRequestHeader("Content-Type", "application/json");
                                }
                            }).done(function (data2) {
                                let userToken = data2["result"];
                                chrome.storage.sync.set({userToken: userToken}, function () {
                                });
                            });
                        }
                    });
                });
    });
}

function wantCoffee(formData) {
    chrome.storage.sync.get(['accessToken', 'userToken'], function (stData) {
        $.ajax({
            type: 'POST',
            cache: false,
            url: serviceDomain + coffeeWantURL,
            dataType: "json",
            crossDomain: true,
            contentType: "application/json; charset=utf-8",
            data: formData,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "Bearer " + stData.accessToken);
                xhr.setRequestHeader("Content-Type", "application/json");
            }
        }).done(function (data) {
            let result = data["result"];
            chrome.storage.sync.set({coffeeRequestId: result}, function () {
                startCoffeeCheck();
            });
        }).fail(function (jqXHR, textStatus) {
            try {
                let cause = jqXHR.getResponseHeader('Cause');
                if (cause.includes(causeAlreadyExists)) {
                    let reqId = cause.replace(causeAlreadyExists, '');
                    chrome.storage.sync.set({coffeeRequestId: reqId}, function () {
                        startCoffeeCheck();
                    });
                } else {
                    console.log(cause);
                }
            } catch (ex) {
            }
        }
        );
    });
}

function startCoffeeCheck() {
    chrome.storage.sync.get(['accessToken', 'userToken', 'coffeeRequestId'], function (stData) {
        try {
            chrome.browserAction.setBadgeBackgroundColor({color: 'yellow'});
            chrome.browserAction.setBadgeText({text: "?"});
        } catch (ex) {
        }
        coffeeCheckTimer = setInterval(function () {
            $.ajax({
                type: 'GET',
                cache: false,
                url: serviceDomain + coffeeWantURL + "/" + stData.coffeeRequestId + "/status",
                dataType: "json",
                crossDomain: true,
                contentType: "application/json; charset=utf-8",
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("Authorization", "Bearer " + stData.accessToken);
                    xhr.setRequestHeader("Content-Type", "application/json");
                }
            }).done(function (data) {
                let status = data.request.requestStatusType;
                let paired = data.paired;
                let pairStatus = data.pairStatus;
                let requestId = data.request.id;
                let statusText = statusTextHtml(data);
                chrome.storage.sync.set({coffeeRequestId: requestId, status: statusText}, function () {
                });
                console.log(data);
                switch (status) {
                    case 'CREATED':
                    {
                        //next one, vozmozhnost otmeny zayavki
                        break;
                    }
                    case 'PAIRED':
                    {
                        //Nuzhno pokazat vybor Da Net CherneySpisok Otmena
                        try {
                            chrome.storage.sync.set({popup: "paired", buddyDescription: JSON.stringify(data.buddyDescription)}, function () {
                                chrome.browserAction.setBadgeBackgroundColor({color: 'red'});
                                chrome.browserAction.setBadgeText({text: "!!!!"});
                            });
                        } catch (ex) {
                        }
                        break;
                    }
                    case 'CANCELLED':
                    {
                        stopCoffeeCheck();
                        chrome.storage.sync.set({coffeeRequestId: '', status: ''}, function () {
                        });
                        //Otmenena , reset state
                        break;
                    }
                    case 'REJECTED':
                    {
                        stopCoffeeCheck();
                        chrome.storage.sync.set({coffeeRequestId: '', status: ''}, function () {
                        });
                        //Ya skazal net, sozdalas novaya zayvka s redirect
                        break;
                    }
                    case 'REJECTED_BLACKLIST':
                    {
                        stopCoffeeCheck();
                        chrome.storage.sync.set({coffeeRequestId: '', status: ''}, function () {
                        });
                        //ya vnes v blacklist, sozdalas novaya zayvka s redirect 
                        break;
                    }
                    case 'ACCEPTED':
                    {
                        switch (pairStatus) {
                            case 'ACCEPTED':
                            {
                                chrome.storage.sync.set({popup: "pairedOk", buddyDescription: JSON.stringify(data.buddyDescription)}, function () {
                                    chrome.browserAction.setBadgeText({text: ""});
                                });
                                stopCoffeeCheck();
                                break;
                            }
                            default:
                            {

                                break;
                            }

                        }

                        break;
                    }
                    case 'ACCEPT_TIMED_OUT':
                    {
                        //redirect
                        break;
                    }
                    case 'SKIPPED':
                    {
                        //redirect
                        break;
                    }
                    default:
                    {
                        stopCoffeeCheck();
                        chrome.storage.sync.set({coffeeRequestId: '', status: ''}, function () {
                        });
                        break;
                    }
                }
            });
        }, 5000);
    });
}

function stopCoffeeCheck() {
    try {
        chrome.browserAction.setBadgeText({text: ""});
    } catch (ex) {
    }
    clearInterval(coffeeCheckTimer);
}

function pairDecision(dec) {
    chrome.storage.sync.get(['accessToken', 'userToken', 'coffeeRequestId'], function (stData) {
        $.ajax({
            type: 'POST',
            cache: false,
            url: serviceDomain + coffeeWantURL + "/" + stData.coffeeRequestId + "/" + dec,
            dataType: "json",
            crossDomain: true,
            contentType: "application/json; charset=utf-8",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "Bearer " + stData.accessToken);
                xhr.setRequestHeader("Content-Type", "application/json");
            }
        }).done(function (data) {
            console.log(data);
//            let result = data["result"];
//            chrome.storage.sync.set({coffeeRequestId: result}, function () {
//                startCoffeeCheck();
//            });
        }).fail(function (jqXHR, textStatus) {
//            let cause = jqXHR.getResponseHeader('Cause');
//            if (cause.includes(causeAlreadyExists)) {
//                let reqId = cause.replace(causeAlreadyExists, '');
//                chrome.storage.sync.set({coffeeRequestId: reqId}, function () {
//                    startCoffeeCheck();
//                });
//            } else {
//                console.log(cause);
//            }
        });
    });
}

function statusTextHtml(data) {
    return "<table>" +
            (data.request.currentPersonCount != undefined ? ("<tr><td>Количество людей в очереди</td><td>" + data.request.currentPersonCount + "</td></tr>") : "")
            + "<tr><td>Время создания вашей заявки</td><td>" + data.request.originalCreated + "</td></tr>"
            + "<tr><td>Место кофепития</td><td>" + data.request.place + "</td></tr>"
            + "<tr><td>Готовность угостить</td><td>" + (data.request.canPay == "true" ? "Да" : "Нет") + "</td></tr>"
            + "</table><br><button id='cancelButton'>Отменить заявку</button>";
}

function cancelRequest() {
    chrome.storage.sync.get(['accessToken', 'userToken', 'coffeeRequestId'], function (stData) {
        $.ajax({
            type: 'POST',
            cache: false,
            url: serviceDomain + coffeeWantURL + "/" + stData.coffeeRequestId + "/cancel",
            dataType: "json",
            crossDomain: true,
            contentType: "application/json; charset=utf-8",
            beforeSend: function (xhr) {
                xhr.setRequestHeader("Authorization", "Bearer " + stData.accessToken);
                xhr.setRequestHeader("Content-Type", "application/json");
            }
        }).done(function (data) {
            stopCoffeeCheck();
            try {
                chrome.browserAction.setBadgeText({text: ""});
            } catch (ex) {
            }
        });
    });
}