# Проект кофе-бота для поиска пары среди сотрудников

#### Возможности:
* Регистрация пользователей с помощью интеграции с внутренним порталом net.open.ru (через расширение Google Chrome)
* 2 интерфейса - web-интерфейс (через расширение браузера Chrome), и командный интерфейс Telegram бота
* Подбор пары среди сотрудников банка, в настощий момент находящихся в одной локации
* Заявка создается через интерфейс расширения или через команду чат-боту /iwantcoffee
* Расширение или чат-бот предлагает пару среди сотрудников той же локации
* Есть возможность согласиться с парой (Да в расширении или в чат боте) или Нет (пропустить предложенную пару)
* Возможность отменить заявку командой /cancel
* При пропуске будут предлагаться другие сокофейники
* Ранжирование пользователей и поиск наиболее "честного" и разнообразного сопоставления пар, основанный на предыдущем поведении пользователя
* Возможность переписываться с найденой согласованной парой через чат-бота

#### Структура проекта
* Расширение для браузера Google Chrome. Реализует веб-интерфейс чат-бота
Не требует сборки, может быть установлен через меню "Расширения" со включенным режимом разработчика Chrome или, при регистрации расширения, в Google Chrome Market.
[Инструкция по использованию расширения](инструкция-расширение.docx)
* Серверная часть на Spring boot. Реализует командный интерфейс Telegram-бота, а также логику распределения пар, а также REST интерфейс используемый расширением Chrome.

##### Сборка
Серверная часть представляет собой spring boot проект.
Собрать проект с помощью Apache Maven
------------------
    mvn clean package

В target видим cofeebot-server.jar - исполняемый файл проекта. Также формируется RPM пакет (для установки в RedHat-подобных Linux дистрибутивов)

##### Развертывание
Для запуска необходима виртуальная машина Java версии не ниже 11. Запуск осуществляется командой
------------------
    java -jar -Dcofeebot.botToken=Токен бота -Dcofeebot.botName=openmdm_coffee_bot cofeebot-server.jar
Обратите внимание, для авторизации используется OAuth2 сервер, собранный проект уже настроен на тот же сервер что и расширение для Chrome
Указывается имя (-Dcofeebot.botName=) и токен (-Dcofeebot.botToken=) зарегистрированного Telegram-бота. Если их не указать - то старт не удастся. Нужно зарегистрировать бота в Telegram, либо обратиться к команде за этими данными уже существующего бота
