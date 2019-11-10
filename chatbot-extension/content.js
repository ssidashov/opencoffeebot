
$(document).ready(function () {
    let fioPs = $("body").find("a.url[href='/ru/services/personal_user_page/']");
    let lastname = fioPs.find(".userLastName").text();
    let firstname = fioPs.find(".userFirstName").text();
    let middlename = fioPs.find(".userMiddleName").text();
    let fio = lastname + " " + firstname + " " + middlename;
    let account = $("body").find("p.user-account").data("account").toLowerCase();
    chrome.storage.sync.set({fio: fio, account: account,
        lastname: lastname, firstname: firstname, middlename: middlename}, function () {
    });
    let staticURL = chrome.extension.getURL('images/coffee32b.png');
    let moveURL = chrome.extension.getURL('images/coffee32.png');
    $("<div style='display: block;width: 32px;height: 32px;position: relative;box-sizing: border-box;margin-right: 14px;'>\n\
 		<img id='openCoffee' src='" + staticURL + "' style='cursor: pointer;'></div>").insertAfter(".secondary-menu .row-items-wrap .row-items ul");
    $("<div id='dialogOpenCoffee' style='display:none;'>\n\
    <form id='dialogOpenCoffeeForm'>\n\
        <input type='hidden' id='userToken' name='userId'>\n\
        <table>\n\
            <tr><td><input type='checkbox' name='canPay' value='true'><label>Готов угостить</label></td></tr>\n\
            <tr><td><input type='checkbox' name='myPlace' value='true'><label>У меня на месте</label></td></tr>\n\
            <tr><td><select name='maxWaitTime'>\n\
                        <option>5</option>\n\
                        <option>15</option>\n\
                        <option>30</option>\n\
                    </select><label>Максимальное время ожидания</label></td></tr>\n\
        </table>\n\
    </form>\n\
    </div>").insertAfter(".secondary-menu .row-items-wrap .row-items ul");
    $("<div id='dialogPaired' style='display:none;'>\n\
        <form id='dialogPairedForm'>\n\
            <p id='buddyDescription'></p>\n\
        </form>\n\
       </div>");
    $('#openCoffee').hover(function () {
        $(this).attr('src', moveURL);
    }, function () {
        $(this).attr('src', staticURL);
    });
    $("#dialogOpenCoffee").dialog({
        autoOpen: false,
        resizable: false,
        height: "auto",
        width: 400,
        modal: true,
        buttons: {
            "Хочу кофе!": function () {
                chrome.storage.sync.get(['userToken'], function (stData) {
                    $('#userToken').val(stData.userToken);
                    var config = {};
                    jQuery("#dialogOpenCoffeeForm").serializeArray().map(function (item) {
                        if (config[item.name]) {
                            if (typeof (config[item.name]) === "string") {
                                config[item.name] = [config[item.name]];
                            }
                            config[item.name].push(item.value);
                        } else {
                            config[item.name] = item.value;
                        }
                    });
                    chrome.extension.sendMessage({cmd: 'wantCoffee', form: JSON.stringify(config)}, function (backMessage) {
                        $("#dialogOpenCoffee").dialog("close");
                    });
                });
            }
        }
    });
    register();
    $('#openCoffee').click(function () {
        $("#dialogOpenCoffee").dialog("open");
    });
});

function register() {
    chrome.extension.sendMessage({cmd: 'registerUser'}, function (backMessage) {
    });
}