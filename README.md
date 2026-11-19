<p align="center">
  <img src="logo.png" width="128" height="128" alt="LenovoMotorolovichY70 Logo">
</p>

<h1 align="center">LenovoMotorolovichY70</h1>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Platform-Android%2012%2B-green.svg" alt="Platform"></a>
  <a href="#"><img src="https://img.shields.io/badge/Device-Lenovo%20Legion%20Y70%20%7C%20Moto%20Edge%2070%20Max-blue.svg" alt="Device"></a>
  <a href="#"><img src="https://img.shields.io/badge/SoC-Snapdragon%208%2B%20Gen%201-orange.svg" alt="SoC"></a>
  <a href="#"><img src="https://img.shields.io/badge/Access-Shizuku%20(No%20Root)%20%7C%20Root-red.svg" alt="Access"></a>
  <a href="#"><img src="https://img.shields.io/badge/APK%20Size-~800%20KB-brightgreen.svg" alt="APK Size"></a>
  <a href="#"><img src="https://img.shields.io/badge/Telemetry-None%20%2F%20No%20Ads-success.svg" alt="Telemetry"></a>
</p>

<p align="center">
  Набор системных твиков и утилит для смартфонов <b>Lenovo Legion Y70</b> и <b>Motorola Edge 70 Max</b> (Android 12+).<br>
  Работает через <b>Shizuku</b> (без Root) или с правами <b>Root</b> (Magisk / KernelSU / APatch). Без рекламы и аналитики.
</p>

---

## Возможности

### 1. Дисплей и DPI
* **Разрешение:** переключение между QHD+ (1440p), FHD+ (1080p) и HD+ (720p).
* **Плотность (DPI):** ручная регулировка ползунком или готовые пресеты под каждое разрешение.
* **Частота обновления:** фиксация 144 / 120 / 90 / 60 Гц или авто.
* **Защита от сбоев:** автоматический откат разрешения через 15 секунд при отсутствии подтверждения.
* **Плитки шторки:** переключение герцовки и разрешения из быстрых настроек.

### 2. Отклик сенсора и анимации
* Регулировка скорости указателя (`pointer_speed`) для уменьшения задержки реакции на касания.
* Переключение скорости системных анимаций (0.5x / 1.0x).

### 3. Экран и защита глаз
* **DC Dimming:** переключение аппаратного режима без мерцания через узел драйвера панели (`/sys/class/drm/card0-DSI-1/panelDC`).
* **Анти-шлейф OLED (Black Floor Lift):** программное смещение чисто чёрного (`#000000 → #040404`), предотвращающее отключение субпикселей и смазывание текста при прокрутке на низкой яркости.
* Быстрый доступ к ночному свету и режиму Extra Dim.

### 4. Звук
* Отключение абсолютной громкости Bluetooth (`Disable Absolute Volume`) при тихом звуке в беспроводных наушниках.
* Отключение системного предупреждения о небезопасной громкости (`Safe Media Bypass`).
* Быстрый вызов Motorola Multi-Volume и Dolby Atmos.

### 5. Питание и зарядка
* Переключение контроллера питания в режим обхода батареи (Bypass Charging) через системные ноды PMIC (если поддерживается сборкой ядра).
* Отображение температуры аккумулятора и источника питания.

### 6. Скорость сети в статус-баре
* Компактный индикатор скорости (приём / отдача) справа у батареи (не смещается при уведомлениях).
* Автоматическая центровка и адаптация под текущее разрешение и DPI.
* Остановка таймера опроса при выключении экрана для экономии заряда.

### 7. Отключение приложений (Debloater)
* Отключение предустановленных пакетов по сценариям (Safe, Balance, Clean, Y70 Edition).
* Изоляция приложений через AppOps без Root.
* Возможность восстановления всех пакетов в один клик.

### 8. Фикс NFC
* Графический интерфейс для переключения маршрутов NFC на процессор хоста (DH 0x00).
* Интегрирован фикс от автора **stepchansky** (4PDA).
* Поддержка экспорта готового архива для Magisk / KernelSU / APatch и возврат к стоковым маршрутам.

---

## Сборка

Проект компилируется автономным скриптом на Python (требуется установленный JDK 17+):

```bash
python build_apk.py
```

Готовый файл: `LenovoMotorolovichY70_v2.9.apk`.

---

## Требования
* Смартфон: Lenovo Legion Y70 или Motorola Edge 70 Max.
* Доступ: Shizuku (через беспроводную отладку) или Root.

Лицензия: MIT.
