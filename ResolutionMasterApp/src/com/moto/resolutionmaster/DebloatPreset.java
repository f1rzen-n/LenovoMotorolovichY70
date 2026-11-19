package com.moto.resolutionmaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DebloatPreset {

    public static final int TYPE_MINIMAL = 0;
    public static final int TYPE_BALANCED = 1;
    public static final int TYPE_AOSP = 2;
    public static final int TYPE_AUTHOR = 3;

    public static class PresetItem {
        public final String packageName;
        public final String title;
        public final String description;
        public final String category;

        public PresetItem(String packageName, String title, String description, String category) {
            this.packageName = packageName;
            this.title = title;
            this.description = description;
            this.category = category;
        }
    }

    public static String getPresetTitle(int type) {
        switch (type) {
            case TYPE_MINIMAL: return "1. Минимальный (Safe)";
            case TYPE_BALANCED: return "2. Баланс";
            case TYPE_AOSP: return "3. Чистый";
            case TYPE_AUTHOR: return "4. Авторский (Y70 Edition)";
            default: return "Сценарий";
        }
    }

    public static String getPresetDescription(int type) {
        switch (type) {
            case TYPE_MINIMAL:
                return "100% безопасная очистка без риска для любых функций: кредитные блокировщики (PAKS, PayJoy), мусорные иностранные операторы и рекламные установщики.";
            case TYPE_BALANCED:
                return "Оптимальная автономность и тишина в фоне. Вся телеметрия Moto, сбор дампов, трекеры Meta и Google. Фирменные жесты Moto, камера и десктоп Ready For полностью сохраняются!";
            case TYPE_AOSP:
                return "Превращение в чистую систему без лишних надстроек: отключение фирменных надстроек Moto и тяжелых приложений Google, оставляя только Google Play Store. Родной лаунчер Launcher3 остаётся активным!";
            case TYPE_AUTHOR:
                return "Эксклюзивный авторский профиль (111 пакетов), настроенный и проверенный на реальном Lenovo Legion Y70 / Moto Edge 70 Max. Идеальный баланс тишины, скорости и батареи.";
            default:
                return "";
        }
    }

    public static String getPresetTarget(int type) {
        switch (type) {
            case TYPE_MINIMAL: return "Для всех, 100% без рисков";
            case TYPE_BALANCED: return "Максимум автономности без потери фишек Moto";
            case TYPE_AOSP: return "Для фанатов чистого Android / Google Pixel";
            case TYPE_AUTHOR: return "Проверено сообществом (эталон Y70)";
            default: return "";
        }
    }

    public static String getPresetWhatInside(int type) {
        switch (type) {
            case TYPE_MINIMAL:
                return "Рекламные загрузчики, кредитные хвосты (PayJoy, PAKS AppOps-freeze), иностранные операторы.";
            case TYPE_BALANCED:
                return "Минимальный + вся телеметрия Moto/Google, предустановленный мусор, отключение Doze-трекеров, ненужные сервисы Google. Жесты и камера сохраняются!";
            case TYPE_AOSP:
                return "Рекомендуемый + отключение надстроек Moto (Smart Connect, Freeform, Moto App, Launcher-оверлеи) с переходом на чистый AOSP/Google стиль. Launcher3 защищен!";
            case TYPE_AUTHOR:
                return "Эталонный список из 111 пакетов для сообщества 4PDA, настроенный и проверенный на Legion Y70.";
            default:
                return "";
        }
    }

    public static List<PresetItem> getPresetList() {
        return getPresetList(TYPE_AUTHOR);
    }

    public static List<PresetItem> getPresetList(int type) {
        Map<String, PresetItem> map = new LinkedHashMap<>();

        // === 1. МИНИМАЛЬНЫЙ НАБОР (Всегда безопасный) ===
        addMinimal(map);

        if (type == TYPE_MINIMAL) {
            return new ArrayList<>(map.values());
        }

        // === 2. РЕКОМЕНДУЕМЫЙ ИИ / БАЛАНС ===
        addBalanced(map);

        if (type == TYPE_BALANCED) {
            return new ArrayList<>(map.values());
        }

        // === 3. ЧИСТЫЙ AOSP ===
        if (type == TYPE_AOSP) {
            addAosp(map);
            return new ArrayList<>(map.values());
        }

        // === 4. АВТОРСКИЙ Y70 EDITION ===
        if (type == TYPE_AUTHOR) {
            addAuthor(map);
            return new ArrayList<>(map.values());
        }

        return new ArrayList<>(map.values());
    }

    public static Set<String> getAllKnownPackages() {
        Set<String> set = new HashSet<>();
        for (int t = 0; t <= 3; t++) {
            for (PresetItem it : getPresetList(t)) {
                set.add(it.packageName);
            }
        }
        return set;
    }

    private static void addMinimal(Map<String, PresetItem> map) {
        // Кредит и операторы
        put(map, "com.motorola.paks", "PAKS Finance", "Фоновый кредитный модуль (глушится через AppOps)", "Кредит и операторы");
        put(map, "com.motorola.paks.notification", "PAKS Notification", "Уведомления кредита PAKS (блокируются через AppOps)", "Кредит и операторы");
        put(map, "com.motorola.android.overlay.payjoy", "PayJoy Overlay", "Остатки кредитного ПО PayJoy", "Кредит и операторы");
        put(map, "com.payjoy.access", "PayJoy Access", "Служба контроля доступа PayJoy", "Кредит и операторы");
        put(map, "com.motorola.vzw.pco.extensions.pcoreceiver", "Verizon PCO Extensions", "Хвосты оператора Verizon", "Кредит и операторы");
        put(map, "com.motorola.omadm.service", "DMService (OMA-DM)", "Операторский сервис удаленного управления OMA-DM", "Кредит и операторы");
        put(map, "com.motorola.omadm.vzw", "Verizon OMA-DM", "Операторский агент Verizon", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp", "EuiccPartnerApp", "Партнерский операторский сервис eSIM", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.vzw", "eUICC Verizon Overlay", "Операторский оверлей Verizon", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.att", "eUICC AT&T Overlay", "Операторский оверлей AT&T", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.tmo", "eUICC T-Mobile Overlay", "Операторский оверлей T-Mobile", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.comcast", "eUICC Comcast Overlay", "Операторский оверлей Comcast", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.br", "eUICC Brazil Overlay", "Операторский оверлей Brazil", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.cc", "eUICC CC Overlay", "Операторский оверлей Consumer Cellular", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.na", "eUICC NA Overlay", "Операторский оверлей North America", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.retca", "eUICC Retail Canada", "Операторский оверлей Canada", "Кредит и операторы");
        put(map, "com.oem.euiccpartnerapp.overlay.switchable.avr", "eUICC Switchable AVR", "Операторский оверлей Switchable", "Кредит и операторы");

        // Реклама и партнерский софт
        put(map, "com.aura.oobe.motorola", "Aura OOBE Motorola", "Рекламный установщик партнерских игр и программ", "Реклама и партнерский софт");
        put(map, "com.aura.oobe.vodafone", "Aura Vodafone", "Рекламный установщик Vodafone", "Реклама и партнерский софт");
        put(map, "com.dti.motorola", "DTI Motorola (Digital Turbine)", "Фоновая подгрузка рекламного софта", "Реклама и партнерский софт");
        put(map, "com.glance.lockscreenM", "Glance Lockscreen", "Реклама и новости на экране блокировки", "Реклама и партнерский софт");
        put(map, "com.motorola.playautoinstallext", "PlayAutoInstallExt", "Тихая фоновая автоустановка спонсорского ПО", "Реклама и партнерский софт");
        put(map, "com.amazon.appmanager", "Amazon App Manager", "Партнерский установщик Amazon", "Реклама и партнерский софт");
        put(map, "com.indus.appstore", "Indus Appstore", "Индийский магазин приложений Indus", "Реклама и партнерский софт");
        put(map, "com.handmark.expressweather", "ExpressWeather", "Сторонняя рекламно-погодная служба", "Реклама и партнерский софт");
        put(map, "com.inmobi.weather", "InMobi Weather", "Рекламный трекер погоды InMobi", "Реклама и партнерский софт");
    }

    private static void addBalanced(Map<String, PresetItem> map) {
        // Телеметрия и слежка
        put(map, "com.motorola.motocare", "MotoCare", "Диагностика и отправка телеметрии Motorola", "Телеметрия и слежка");
        put(map, "com.motorola.lifetimedata", "LifetimeData", "Постоянный фоновый сбор статистики устройства", "Телеметрия и слежка");
        put(map, "com.motorola.help.extlog", "MotoFeedbackAssistant", "Сбор и отправка системных логов", "Телеметрия и слежка");
        put(map, "com.motorola.wifi.motowifimetrics", "MotoWifiMetrics", "Сбор телеметрии подключений Wi-Fi", "Телеметрия и слежка");
        put(map, "com.motorola.proxy_server", "MotoProxyServer", "Прокси-сервер фоновой телеметрии", "Телеметрия и слежка");
        put(map, "com.motorola.ccc.devicemanagement", "Moto Device Management", "Фоновый менеджмент и телеметрия (глушится через AppOps)", "Телеметрия и слежка");
        put(map, "com.motorola.bug2go", "Moto Bug2Go", "Постоянный сборщик дампов и баг-репортов Moto в фоне", "Телеметрия и слежка");
        put(map, "com.motorola.bach.modemstats", "Moto ModemStats", "Сбор статистики радиомодема", "Телеметрия и слежка");
        put(map, "com.motorola.android.nativedropboxagent", "NativeDropBoxAgent", "Сборщик краш-логов DropBox", "Телеметрия и слежка");
        put(map, "com.motorola.cqawizard", "CQA Wizard", "Инженерное заводское тестирование CQATest", "Телеметрия и слежка");
        put(map, "com.google.mainline.telemetry", "Mainline Telemetry", "Фоновая телеметрия компонентов Google Mainline", "Телеметрия и слежка");
        put(map, "com.google.mainline.adservices", "Mainline AdServices", "Рекламные сервисы Google Mainline", "Телеметрия и слежка");

        // Meta / Facebook
        put(map, "com.facebook.appmanager", "Facebook App Manager", "Фоновый автоапдейтер и трекер Meta", "Meta / Facebook");
        put(map, "com.facebook.services", "Facebook Services", "Службы фоновой аналитики Meta", "Meta / Facebook");
        put(map, "com.facebook.system", "Facebook System", "Системные компоненты Meta", "Meta / Facebook");

        // Google службы
        put(map, "com.google.android.apps.wellbeing", "Digital Wellbeing", "Цифровое благополучие (постоянный трекер активности)", "Google службы");
        put(map, "com.google.android.feedback", "Google Feedback", "Отправка отчетов об ошибках Google", "Google службы");
        put(map, "com.google.android.partnersetup", "Google Partner Setup", "Настройка рекламных партнеров Google", "Google службы");
        put(map, "com.google.android.apps.carrier.carrierwifi", "Carrier Wi-Fi", "Автоподключение к открытым операторским сетям", "Google службы");
        put(map, "com.google.android.apps.subscriptions.red", "Google One", "Служба платной подписки Google One", "Google службы");

        // Лишние службы
        put(map, "com.motorola.stylus", "MotoStylus", "Служба стилуса (не нужна без физического пера)", "Лишние службы");
        put(map, "com.motorola.smartfeed", "SmartFeed", "Лента новостей в системном лаунчере", "Лишние службы");
        put(map, "com.motorola.migrateassist", "Moto Migrate Assist", "Помощник переноса со старого телефона", "Лишние службы");
        put(map, "com.motorola.demo", "Moto Demo Mode", "Демо-режим для витрин магазинов", "Лишние службы");
        put(map, "com.motorola.attvowifi", "AT&T VoWiFi Service", "Служба VoWiFi оператора AT&T (бесполезна в РФ)", "Лишние службы");
        put(map, "com.motorola.android.fmradio", "FM Radio", "FM-радио (в телефоне нет FM-приемника)", "Лишние службы");
        put(map, "com.motorola.appforecast", "AppForecast", "Фоновый трекер прогнозирования запуска приложений", "Лишние службы");
        put(map, "com.motorola.aiservices", "Moto AI Services", "Китайские облачные службы Moto AI", "Лишние службы");
        put(map, "com.motorola.aitranslation", "Moto AI Translation", "Китайские службы перевода Moto", "Лишние службы");
    }

    private static void addAosp(Map<String, PresetItem> map) {
        // Тяжелый софт Google (кроме Google Play Store и Play Services)
        put(map, "com.google.android.apps.youtube.music", "YouTube Music", "Музыкальный сервис YouTube", "Google софт");
        put(map, "com.google.android.videos", "Google TV / Фильмы", "Сервис видео Google TV", "Google софт");
        put(map, "com.google.android.apps.tachyon", "Google Meet (Duo)", "Видеозвонки Google Meet", "Google софт");
        put(map, "com.google.android.apps.bard", "Google Gemini (Bard)", "ИИ-ассистент Google Gemini", "Google софт");
        put(map, "com.google.android.apps.maps", "Google Карты", "Навигация и карты Google", "Google софт");
        put(map, "com.google.android.apps.photos", "Google Фото", "Облачная галерея Google Фото", "Google софт");
        put(map, "com.google.android.apps.docs", "Google Документы", "Офисный пакет Google Диск/Docs", "Google софт");
        put(map, "com.google.android.gm", "Gmail", "Почтовый клиент Gmail", "Google софт");
        put(map, "com.google.android.googlequicksearchbox", "Google Поиск / Ассистент", "Строка поиска и Google Assistant", "Google софт");
        put(map, "com.google.android.apps.turbo", "Device Health Services", "Служба расчёта батареи Google Turbo", "Google софт");
        put(map, "com.google.android.marvin.talkback", "Android Accessibility TalkBack", "Озвучивание экрана для незрячих", "Google софт");

        // Moto оболочка (отключение надстроек Moto, НО рабочий стол Launcher3 НЕ отключается!)
        put(map, "com.motorola.moto", "Moto App", "Основное приложение Moto (настройки фишек)", "Оболочка Moto");
        put(map, "com.motorola.actioncore", "Moto ActionCore", "Движок фирменных жестов Moto", "Оболочка Moto");
        put(map, "com.motorola.gamemode", "Moto Gametime", "Игровая панель Game Mode", "Оболочка Moto");
        put(map, "com.motorola.mobiledesktop", "Moto Ready For Desktop", "Десктопный режим подключения к монитору", "Оболочка Moto");
        put(map, "com.motorola.freeform", "Moto Freeform Windows", "Плавающие окна Moto", "Оболочка Moto");
        put(map, "com.motorola.smart5g", "Moto Smart 5G", "Автоматический переключатель 5G", "Оболочка Moto");
        put(map, "com.motorola.touchshare", "Touch to Share", "Беспроводной обмен Touch to Share", "Оболочка Moto");
        put(map, "com.motorola.livewallpaper3", "Moto Live Wallpaper", "Живые обои Motorola", "Оболочка Moto");
        put(map, "com.motorola.aicore", "Moto AI Core", "Ядро искусственного интеллекта Moto", "Оболочка Moto");
        put(map, "com.motorola.securityhub", "Moto Secure (ThinkShield)", "Центр безопасности Moto Secure", "Оболочка Moto");
        put(map, "com.motorola.securityhubext", "Moto Secure Extensions", "Расширения безопасности Moto Secure", "Оболочка Moto");
        put(map, "com.motorola.account", "Moto ID Account", "Учетная запись Motorola ID", "Оболочка Moto");
        put(map, "com.motorola.hce", "Moto HCE Pointer Service", "Служба мыши/курсора для Ready For", "Оболочка Moto");
        put(map, "com.lenovo.qira", "Lenovo Qira", "Голосовой помощник Qira", "Оболочка Moto");
        put(map, "com.lenovo.qiracore", "Lenovo Qira Core", "Ядро помощника Qira", "Оболочка Moto");
    }

    private static void addAuthor(Map<String, PresetItem> map) {
        // Включает весь AOSP набор
        addAosp(map);

        // Плюс все специфические оверлеи и компоненты, отключенные автором на телефоне
        put(map, "com.android.dialer", "AOSP Телефон", "Старый телефон AOSP (активен Google Dialer)", "Системные оверлеи");
        put(map, "com.android.contacts", "AOSP Контакты", "Старые контакты AOSP (активны Google Контакты)", "Системные оверлеи");
        put(map, "com.android.dialer.overlay.imei", "Dialer IMEI Overlay", "Оверлей номеронабирателя", "Системные оверлеи");
        put(map, "com.android.virtualization.terminal", "Virtualization Terminal", "Системный терминал виртуализации", "Системные службы");
        put(map, "com.google.android.gms.supervision", "Family Link Supervision", "Родительский контроль Google Supervision", "Google службы");
        put(map, "com.google.android.healthconnect.controller", "Health Connect", "Служба обмена данными о здоровье Health Connect", "Google службы");
        put(map, "com.motorola.android.launcher.overlay.amx", "Launcher AMX Overlay", "Региональный оверлей лаунчера AMX", "Региональные оверлеи");
        put(map, "com.motorola.android.launcher.overlay.applabeltwolines", "Launcher Two Lines Overlay", "Двухстрочный оверлей подписей лаунчера", "Региональные оверлеи");
        put(map, "com.motorola.android.launcher.overlay.koodo", "Launcher Koodo Overlay", "Региональный оверлей лаунчера Koodo", "Региональные оверлеи");
        put(map, "com.motorola.android.launcher.overlay.telus", "Launcher Telus Overlay", "Региональный оверлей лаунчера Telus", "Региональные оверлеи");
        put(map, "com.motorola.android.launcher.overlay.vfeu", "Launcher VF EU Overlay", "Региональный оверлей лаунчера Vodafone EU", "Региональные оверлеи");
        put(map, "com.motorola.android.launcher.overlay.vivo", "Launcher Vivo Overlay", "Региональный оверлей лаунчера Vivo", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.amxcl", "LauncherConfig AMX CL", "Региональный конфиг лаунчера AMX CL", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.amxco", "LauncherConfig AMX CO", "Региональный конфиг лаунчера AMX CO", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.amxla", "LauncherConfig AMX LA", "Региональный конфиг лаунчера AMX LA", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.amxmx", "LauncherConfig AMX MX", "Региональный конфиг лаунчера AMX MX", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.amxpe", "LauncherConfig AMX PE", "Региональный конфиг лаунчера AMX PE", "Региональные оверлеи");
        put(map, "com.motorola.launcherconfig.overlay.retin", "LauncherConfig Retail IN", "Региональный конфиг лаунчера Retail India", "Региональные оверлеи");
        put(map, "com.motorola.systemui.overlay.dcm", "SystemUI DCM Overlay", "Региональный оверлей SystemUI Docomo", "Региональные оверлеи");
        put(map, "com.motorola.systemui.overlay.jp", "SystemUI Japan Overlay", "Региональный оверлей SystemUI Japan", "Региональные оверлеи");
        put(map, "com.motorola.contacts.preloadcontacts.overlay.vzw", "Verizon Contacts Overlay", "Оверлей предустановленных контактов Verizon", "Региональные оверлеи");
        put(map, "com.motorola.msimsettings.overlay", "MSimSettings Overlay", "Оверлей настроек мульти-SIM", "Региональные оверлеи");
        put(map, "com.motorola.setup.overlay.dlcdisclaimer", "Setup DLC Disclaimer", "Оверлей мастера настройки DLC", "Региональные оверлеи");
        put(map, "com.motorola.setup.overlay.optinphone", "Setup OptIn Phone", "Оверлей согласий мастера настройки", "Региональные оверлеи");
        put(map, "com.motorola.setup.overlay.optinphone", "Setup OptIn Phone", "Оверлей согласий мастера настройки", "Региональные оверлеи");
        put(map, "com.motorola.setup.overlay.pai", "Setup PAI Overlay", "Оверлей PAI мастера настройки", "Региональные оверлеи");
        put(map, "com.motorola.spectrum.setup.extensions", "Spectrum Setup Extensions", "Расширения настройки оператора Spectrum", "Региональные оверлеи");
        put(map, "com.motorola.att.phone.extensions", "AT&T Phone Extensions", "Расширения телефона AT&T", "Региональные оверлеи");
        put(map, "com.motorola.brapps", "BRApps Service", "Бразильские региональные службы", "Региональные оверлеи");
        put(map, "com.motorola.easyprefix", "Moto EasyPrefix", "Префиксы операторских номеров", "Операторские службы");
        put(map, "com.motorola.enterprise.service", "Moto Enterprise Service", "Корпоративные службы Enterprise MDM", "Корпоративные службы");
        put(map, "com.motorola.genie", "Moto Genie", "Интерактивные советы и подсказки Genie", "Лишние службы");
        put(map, "com.motorola.help", "Moto Help", "Приложение справки Motorola", "Лишние службы");
        put(map, "com.motorola.iotservice", "Moto IoT Service", "Служба умных устройств Moto IoT", "Лишние службы");
        put(map, "com.motorola.motosignature.app", "Moto Signature", "Подписи продуктов Motorola", "Системные службы");
        put(map, "com.motorola.motosignature2.app", "Moto Signature 2", "Подписи продуктов Motorola 2", "Системные службы");
        put(map, "com.motorola.om", "Moto OM Service", "Операторский модуль Moto OM", "Операторские службы");
        put(map, "com.motorola.rcsConfigService", "RCS Config Service", "Служба конфигурации операторского RCS", "Операторские службы");
        put(map, "com.motorola.smartmode", "Moto SmartMode", "Автоматические умные режимы Moto", "Лишние службы");
        put(map, "com.motorola.carrierconfig", "Moto CarrierConfig", "Оверлей операторских настроек Moto", "Операторские службы");
        put(map, "com.motorola.ccc.notification", "Moto CCC Notification", "Сервисные уведомления службы поддержки Moto", "Лишние службы");
    }

    private static void put(Map<String, PresetItem> map, String pkg, String title, String desc, String cat) {
        map.put(pkg, new PresetItem(pkg, title, desc, cat));
    }
}
