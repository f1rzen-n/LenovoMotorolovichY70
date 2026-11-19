package com.moto.resolutionmaster;

import java.util.HashMap;
import java.util.Map;

public class PackageDescriptions {

    public static final int SAFETY_UNKNOWN = 0;
    public static final int SAFETY_SAFE = 1;       // Safe to disable (bloat, telemetry, ads, trackers)
    public static final int SAFETY_CAUTION = 2;    // Optional / convenience feature (launcher, camera, dolby, notes)
    public static final int SAFETY_CRITICAL = 3;   // Core system, phone, sim, wifi, bluetooth, systemui, biometrics

    public static class PackageMeta {
        public final String description;
        public final int safetyLevel;

        public PackageMeta(String description, int safetyLevel) {
            this.description = description;
            this.safetyLevel = safetyLevel;
        }

        public String getSafetyLabel() {
            switch (safetyLevel) {
                case SAFETY_SAFE:
                    return "Безопасно";
                case SAFETY_CAUTION:
                    return "По выбору";
                case SAFETY_CRITICAL:
                    return "Системное";
                default:
                    return "";
            }
        }

        public int getSafetyColor() {
            switch (safetyLevel) {
                case SAFETY_SAFE:
                    return 0xFF10B981; // green
                case SAFETY_CAUTION:
                    return 0xFFF59E0B; // yellow / amber
                case SAFETY_CRITICAL:
                    return 0xFFEF4444; // red
                default:
                    return 0xFF6B7280; // muted
            }
        }
    }

    private static final Map<String, PackageMeta> DB = new HashMap<>();

    static {
        // ==================== КАТЕГОРИЯ: БЛОАТВАРЬ, КРЕДИТЫ, ТЕЛЕМЕТРИЯ (БЕЗОПАСНО) ====================
        put("com.motorola.paks", "Фоновый модуль проверки лизинга/кредита устройства", SAFETY_SAFE);
        put("com.motorola.paks.notification", "Уведомления кредитного модуля PAKS", SAFETY_SAFE);
        put("com.motorola.android.overlay.payjoy", "Остатки кредитного ПО PayJoy", SAFETY_SAFE);
        put("com.payjoy.access", "Сервис блокировки экрана PayJoy", SAFETY_SAFE);
        put("com.motorola.vzw.pco.extensions.pcoreceiver", "Хвосты американского оператора Verizon", SAFETY_SAFE);
        put("com.motorola.omadm.service", "Операторский сервис удаленного управления OMA-DM", SAFETY_SAFE);
        put("com.motorola.omadm.vzw", "Сервис настройки сети оператора Verizon", SAFETY_SAFE);
        put("com.oem.euiccpartnerapp", "Партнерский операторский сервис профилей eSIM", SAFETY_SAFE);

        put("com.motorola.motocare", "Диагностика и отправка телеметрии Motorola в облако", SAFETY_SAFE);
        put("com.motorola.lifetimedata", "Постоянный сбор аналитики и времени работы компонентов", SAFETY_SAFE);
        put("com.motorola.help.extlog", "Служба сбора и отправки расширенных системных логов", SAFETY_SAFE);
        put("com.motorola.wifi.motowifimetrics", "Сбор телеметрии Wi-Fi сетей и трафика", SAFETY_SAFE);
        put("com.motorola.proxy_server", "Прокси-сервер фоновой телеметрии и статистики", SAFETY_SAFE);
        put("com.motorola.ccc.devicemanagement", "Фоновое удаленное управление и телеметрия", SAFETY_SAFE);
        put("com.motorola.bug2go", "Служба отправки дампов ошибок разработчикам", SAFETY_SAFE);
        put("com.motorola.bach.modemstats", "Сбор статистики работы модема и радиомодуля", SAFETY_SAFE);

        put("com.taboola.mip", "Новостной и рекламный виджет Taboola", SAFETY_SAFE);
        put("com.aura.oobe.motorola", "Рекламный установщик партнерских игр и программ", SAFETY_SAFE);
        put("com.aura.oobe.vodafone", "Рекламный установщик оператора Vodafone", SAFETY_SAFE);
        put("com.motorola.playautoinstallext", "Тихая фоновая автоустановка спонсорского ПО", SAFETY_SAFE);
        put("com.dti.motorola", "Digital Turbine — фоновая подгрузка рекламных приложений", SAFETY_SAFE);
        put("com.glance.lockscreenM", "Glance — реклама и новости на экране блокировки", SAFETY_SAFE);
        put("com.amazon.appmanager", "Служба установки и обновления приложений Amazon", SAFETY_SAFE);
        put("com.inmobi.weather", "Сторонняя рекламная служба погоды InMobi", SAFETY_SAFE);
        put("com.handmark.expressweather", "Рекламный погодный модуль", SAFETY_SAFE);
        put("com.indus.appstore", "Партнерский магазин приложений Indus Appstore", SAFETY_SAFE);

        put("com.motorola.stylus", "Служба стилуса (не требуется без физического пера)", SAFETY_SAFE);
        put("com.motorola.smartfeed", "Лента новостей в системном лаунчере", SAFETY_SAFE);
        put("com.motorola.migrateassist", "Помощник переноса данных со старого телефона", SAFETY_SAFE);
        put("com.motorola.brapps", "Бразильские партнерские приложения", SAFETY_SAFE);
        put("com.motorola.demo", "Демо-режим для магазинов и витрин", SAFETY_SAFE);

        put("com.facebook.appmanager", "Служба автообновления и трекер Meta (Facebook)", SAFETY_SAFE);
        put("com.facebook.services", "Фоновые службы аналитики Meta (Facebook)", SAFETY_SAFE);
        put("com.facebook.system", "Системные компоненты интеграции Meta", SAFETY_SAFE);

        put("com.google.android.apps.wellbeing", "Digital Wellbeing (Цифровое благополучие — постоянный фоновый трекер активности)", SAFETY_SAFE);
        put("com.google.android.feedback", "Отправка отчетов об ошибках и сбоях в Google", SAFETY_SAFE);
        put("com.google.android.partnersetup", "Настройка рекламы и партнеров Google", SAFETY_SAFE);
        put("com.google.android.apps.carrier.carrierwifi", "Автоподключение к открытым сетям операторов связи", SAFETY_SAFE);
        put("com.google.android.gms.supervision", "Родительский контроль Family Link", SAFETY_SAFE);

        // ==================== КАТЕГОРИЯ: MOTOROLA ФУНКЦИИ (ПО ВЫБОРУ) ====================
        put("com.motorola.camera5", "Штатная камера Motorola (фото, 8K/4K видео, Pro-режим)", SAFETY_CAUTION);
        put("com.motorola.camera.ai.colortone", "AI-обработка цветов и сцен в камере", SAFETY_CAUTION);
        put("com.motorola.deptheffect", "Эффект размытия фона и глубины резкости для портретов", SAFETY_CAUTION);
        put("com.motorola.actions", "Фирменные Moto-жесты (включение фонарика, быстрый запуск камеры)", SAFETY_CAUTION);
        put("com.motorola.gesture", "Служба обработки жестов и движений смартфона", SAFETY_CAUTION);
        put("com.motorola.actioncore", "Ядро обработки действий и автоматизации Motorola", SAFETY_CAUTION);
        put("com.motorola.faceunlock", "Разблокировка по лицу Face Unlock", SAFETY_CAUTION);
        put("com.motorola.gamemode", "Игровой режим Moto Game Mode (настройка FPS и блокировка уведомлений)", SAFETY_CAUTION);
        put("com.dolby.daxservice", "Фоновый аудио-сервис Dolby Atmos DAX", SAFETY_CAUTION);
        put("com.motorola.dolby.dolbyui", "Эквалайзер и звуковые профили Dolby Atmos", SAFETY_CAUTION);
        put("com.motorola.dolbyvisionservice", "Служба поддержки HDR контента Dolby Vision", SAFETY_CAUTION);
        put("com.motorola.dynamicvolume", "Интеллектуальная подстройка громкости звука под шум окружения", SAFETY_CAUTION);
        put("com.motorola.mobiledesktop", "Moto Ready For (десктопный режим при подключении к ТВ/монитору)", SAFETY_CAUTION);
        put("com.motorola.mobiledesktop.core", "Ядро сервиса трансляции Moto Ready For", SAFETY_CAUTION);
        put("com.motorola.timeweatherwidget", "Фирменный круглый виджет часов и погоды Motorola", SAFETY_CAUTION);
        put("com.motorola.audiorecorder", "Системный диктофон Motorola с шумоподавлением", SAFETY_CAUTION);
        put("com.motorola.screenshoteditor", "Редактор скриншотов с поддержкой длинных снимков экрана", SAFETY_CAUTION);
        put("com.motorola.personalize", "Меню персонализации Moto (темы, акцентные цвета, шрифты)", SAFETY_CAUTION);
        put("com.motorola.launcher3", "Системный рабочий стол (лаунчер) Motorola", SAFETY_CAUTION);
        put("com.motorola.batterycare", "Адаптивная зарядка (защита батареи от износа при зарядке ночью)", SAFETY_CAUTION);
        put("com.motorola.thermalservice", "Служба термоконтроля и троттлинга процессора", SAFETY_CAUTION);
        put("com.motorola.motocit", "Инженерное меню тестирования датчиков, экрана и кнопок (CQATest)", SAFETY_CAUTION);
        put("com.motorola.cqawizard", "Мастер калибровки и заводского тестирования аппарата", SAFETY_CAUTION);
        put("com.motorola.securevault", "Защищенная папка Moto Secure для скрытия фото и файлов", SAFETY_CAUTION);
        put("com.motorola.motosecurecore", "Ядро безопасности Moto Secure и защита PIN-кода", SAFETY_CAUTION);
        put("com.motorola.securityhub", "Центр безопасности и проверки уязвимостей системы", SAFETY_CAUTION);
        put("com.motorola.aicore", "Нейросетевые службы искусственного интеллекта Moto AI", SAFETY_CAUTION);
        put("com.motorola.aiservices", "Сервисы обработки текста и голоса через Moto AI", SAFETY_CAUTION);
        put("com.motorola.aitranslation", "Живой переводчик Moto AI в реальном времени", SAFETY_CAUTION);
        put("com.motorola.journal", "Фирменное приложение Заметки / Дневник", SAFETY_CAUTION);
        put("com.motorola.smart5g", "Умное переключение 5G/4G для экономии заряда батареи", SAFETY_CAUTION);
        put("com.motorola.ccc.ota", "Служба проверки и установки официальных обновлений системы по воздуху", SAFETY_CAUTION);
        put("com.motorola.ccc.notification", "Сервисные уведомления и напоминания Motorola", SAFETY_CAUTION);
        put("com.motorola.hiddenmenuapp", "Инженерное меню специальных сервисных кодов (##...)", SAFETY_CAUTION);
        put("com.motorola.mykey", "Настройка действий физической клавиши и двойных касаний", SAFETY_CAUTION);
        put("com.motorola.touchshare", "Быстрый обмен файлами с близлежащими устройствами", SAFETY_CAUTION);
        put("com.motorola.livewallpaper3", "Фирменные интерактивные живые обои Motorola", SAFETY_CAUTION);
        put("com.motorola.genie", "Интерактивный помощник и подсказки по функциям телефона", SAFETY_CAUTION);
        put("com.motorola.moto", "Главное приложение «Moto» со справочником всех функций", SAFETY_CAUTION);
        put("com.motorola.help", "Справка и руководство пользователя Motorola", SAFETY_CAUTION);
        put("com.motorola.account", "Вход в аккаунт Motorola (для синхронизации и резервных копий)", SAFETY_CAUTION);
        put("com.motorola.android.fmradio", "FM-радио (требует проводных наушников)", SAFETY_CAUTION);
        put("com.motorola.smartmode", "Автоматические профили (Дома, На работе, Сон)", SAFETY_CAUTION);
        put("com.motorola.dciservice", "Калибровка цветопередачи экрана DCI-P3", SAFETY_CAUTION);
        put("com.motorola.appdirectedsmsproxy", "Системный SMS-прокси Motorola", SAFETY_CAUTION);
        put("com.motorola.motosignature2.app", "Проверка цифровых подписей системных пакетов Motorola", SAFETY_CAUTION);
        put("com.motorola.easyprefix", "Автоматический префикс набора телефонных номеров", SAFETY_CAUTION);
        put("com.motorola.msimsettings", "Управление двумя SIM-картами (Dual SIM)", SAFETY_CAUTION);
        put("com.motorola.carriersettingsext", "Расширенные профили сотовых операторов", SAFETY_CAUTION);
        put("com.motorola.iotservice", "Служба интеграции с умными устройствами Moto IoT", SAFETY_CAUTION);
        put("com.motorola.entitlement", "Проверка права оператора на точку доступа и раздачу интернета", SAFETY_CAUTION);
        put("com.motorola.securityhubext", "Расширение центра безопасности Motorola", SAFETY_CAUTION);

        // ==================== КАТЕГОРИЯ: GOOGLE СЕРВИСЫ (ПО ВЫБОРУ / КРИТИЧНО) ====================
        put("com.google.android.gms", "Сервисы Google Play (основа работы аккаунта, пушей и карт)", SAFETY_CRITICAL);
        put("com.google.android.gsf", "Google Services Framework (базовый системный фреймворк Google)", SAFETY_CRITICAL);
        put("com.android.vending", "Google Play Маркет (магазин приложений)", SAFETY_CAUTION);
        put("com.google.android.googlequicksearchbox", "Приложение Google, голосовой поиск и поисковая строка", SAFETY_CAUTION);
        put("com.google.android.inputmethod.latin", "Клавиатура Gboard (не отключать, если нет другой клавиатуры!)", SAFETY_CAUTION);
        put("com.google.android.dialer", "Телефон Google (звонилка, определитель номеров и автоответчик)", SAFETY_CAUTION);
        put("com.android.dialer", "Системная телефонная книга и приложение набора номера", SAFETY_CAUTION);
        put("com.google.android.apps.messaging", "Google Сообщения (SMS, MMS и RCS-чат)", SAFETY_CAUTION);
        put("com.google.android.contacts", "Контакты Google (телефонная книга и синхронизация)", SAFETY_CAUTION);
        put("com.android.contacts", "Системное хранилище и менеджер контактов", SAFETY_CAUTION);
        put("com.google.android.apps.maps", "Google Карты и навигация", SAFETY_CAUTION);
        put("com.google.android.apps.photos", "Google Фото (галерея и облачная синхронизация фото)", SAFETY_CAUTION);
        put("com.google.android.gm", "Почтовый клиент Gmail", SAFETY_CAUTION);
        put("com.google.android.youtube", "Видеохостинг YouTube", SAFETY_CAUTION);
        put("com.google.android.apps.youtube.music", "Музыкальный стриминг YouTube Music", SAFETY_CAUTION);
        put("com.google.android.calculator", "Калькулятор", SAFETY_CAUTION);
        put("com.google.android.calendar", "Календарь Google", SAFETY_CAUTION);
        put("com.google.android.deskclock", "Часы, будильник, секундомер и таймер", SAFETY_CAUTION);
        put("com.google.android.webview", "Системный движок отображения веб-страниц внутри приложений", SAFETY_CRITICAL);
        put("com.google.android.apps.nbu.files", "Файловый менеджер Google Files", SAFETY_CAUTION);
        put("com.google.android.tts", "Синтезатор речи Google Text-to-Speech", SAFETY_CAUTION);
        put("com.google.android.projection.gearhead", "Android Auto (интеграция с экраном автомобиля)", SAFETY_CAUTION);
        put("com.google.android.apps.turbo", "Device Health Services (прогноз расхода и калибровка батареи)", SAFETY_CAUTION);
        put("com.google.android.as", "Private Compute Services (офлайн-обработка распознавания речи)", SAFETY_CAUTION);
        put("com.google.android.apps.restore", "Служба восстановления данных и бэкапов из облака Google", SAFETY_CAUTION);
        put("com.google.android.apps.wallpaper", "Обои Google", SAFETY_CAUTION);
        put("com.google.android.marvin.talkback", "TalkBack — голосовой помощник для людей с нарушением зрения", SAFETY_CAUTION);
        put("com.google.android.apps.docs", "Google Документы и Диск", SAFETY_CAUTION);
        put("com.google.ar.core", "Сервисы дополненной реальности Google Play для AR", SAFETY_CAUTION);
        put("com.google.android.apps.bard", "Google Gemini (интеллектуальный AI-помощник)", SAFETY_CAUTION);
        put("com.google.android.apps.tachyon", "Google Meet (видеосвязь и видеозвонки)", SAFETY_CAUTION);
        put("com.google.android.apps.authenticator2", "Google Authenticator (двухфакторная аутентификация)", SAFETY_CAUTION);
        put("com.google.android.play.games", "Google Play Игры (достижения и облачные сохранения в играх)", SAFETY_CAUTION);
        put("com.google.android.videos", "Google TV (фильмы и сериалы)", SAFETY_CAUTION);
        put("com.google.android.apps.safetyhub", "Google Личная безопасность (SOS, экстренная помощь)", SAFETY_CAUTION);
        put("com.google.android.apps.work.clouddpc", "Корпоративный профиль Google Cloud DPC", SAFETY_CAUTION);
        put("com.google.android.safetycore", "Системное ядро безопасности Google Safety Core", SAFETY_CAUTION);
        put("com.google.ambient.streaming", "Служба беспроводной трансляции аудио и экрана Google", SAFETY_CAUTION);
        put("com.google.android.apps.subscriptions.red", "Менеджер подписок Google One / YouTube", SAFETY_CAUTION);

        // ==================== КАТЕГОРИЯ: БАЗОВАЯ СИСТЕМА ANDROID (КРИТИЧНО) ====================
        put("com.android.systemui", "Системный интерфейс: шторка уведомлений, статус-бар, навигация", SAFETY_CRITICAL);
        put("com.android.settings", "Главное системное приложение «Настройки» телефона", SAFETY_CRITICAL);
        put("com.android.phone", "Базовая служба телефонии, голосовых вызовов и связи", SAFETY_CRITICAL);
        put("com.android.server.telecom", "Диспетчер вызовов и маршрутизации звонков", SAFETY_CRITICAL);
        put("com.android.bluetooth", "Служба беспроводной связи Bluetooth", SAFETY_CRITICAL);
        put("com.android.nfc", "Бесконтактная оплата и обмен данными по NFC", SAFETY_CRITICAL);
        put("com.android.providers.media.module", "Хранилище мультимедиа (доступ приложений к файлам, фото и видео)", SAFETY_CRITICAL);
        put("com.android.providers.downloads", "Менеджер фоновых загрузок файлов из сети", SAFETY_CRITICAL);
        put("com.android.permissioncontroller", "Управление разрешениями и доступом приложений к камере/микрофону", SAFETY_CRITICAL);
        put("com.android.keychain", "Системное защищенное хранилище ключей и сертификатов безопасности", SAFETY_CRITICAL);
        put("com.android.carrierconfig", "Конфигурация параметров операторов связи (APN, VoLTE, VoWiFi)", SAFETY_CRITICAL);
        put("com.android.packageinstaller", "Системный установщик и средство проверки APK файлов", SAFETY_CRITICAL);
        put("com.android.emergency", "Экстренные вызовы SOS и медицинская карта владельца", SAFETY_CAUTION);
        put("com.android.printspooler", "Диспетчер печати документов на принтерах", SAFETY_CAUTION);
        put("com.android.cellbroadcastreceiver", "Прием экстренных оповещений МЧС и экстренных служб", SAFETY_CAUTION);
        put("com.android.documentsui", "Системный проводник и диалог выбора файлов", SAFETY_CRITICAL);
        put("com.android.chrome", "Браузер Google Chrome", SAFETY_CAUTION);
    }

    private static void put(String pkg, String desc, int safety) {
        DB.put(pkg, new PackageMeta(desc, safety));
    }

    public static PackageMeta getMeta(String packageName) {
        if (packageName == null) {
            return new PackageMeta("Приложение Android", SAFETY_UNKNOWN);
        }
        PackageMeta meta = DB.get(packageName);
        if (meta != null) {
            return meta;
        }

        // Heuristics for unknown packages
        if (packageName.startsWith("com.google.android.overlay.") ||
            packageName.startsWith("com.motorola.android.systemui.overlay.") ||
            packageName.contains(".overlay.")) {
            return new PackageMeta("Системный оверлей конфигурации интерфейса", SAFETY_CAUTION);
        }
        if (packageName.startsWith("com.qualcomm.") || packageName.startsWith("vendor.qti.")) {
            return new PackageMeta("Низкоуровневый драйвер / сервис чипсета Qualcomm Snapdragon", SAFETY_CRITICAL);
        }
        if (packageName.startsWith("com.motorola.")) {
            return new PackageMeta("Системный модуль или сервис Motorola", SAFETY_CAUTION);
        }
        if (packageName.startsWith("com.google.android.")) {
            return new PackageMeta("Модуль экосистемы Google", SAFETY_CAUTION);
        }
        if (packageName.startsWith("com.android.")) {
            return new PackageMeta("Системный компонент платформы Android", SAFETY_CRITICAL);
        }

        return new PackageMeta("Стороннее приложение", SAFETY_UNKNOWN);
    }
}
