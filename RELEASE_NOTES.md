Heat Pump Display Release Notes
===============================

- Statistics: support DTA files with different value names.
- Statistics: support DTA files containing enums.

## 17.1.0 (2023-01-07)

- Correct heat quantity values order of magnitude.
- Add actual total heat quantity.
- Display heat quantity since date.
- Detect if photovoltaics instead of swimming pool is configured.

## 17.0.1 (2023-01-06)

- Add button to download DTA file.

## 17.0.0 (2023-01-04)

- Updates only for Android 5 or newer.
- Add statistics screen to display some values of the DTA logging file of the controller.
- Add heat quantities.
- Add compressor frequency.
- Add info about software updates, the new web interface and the repo of this app.

## 16.0.1 (2022-03-11)

- Add mixing circuit 1+2 outgoing and outgoing target temperatures.

## 15 (2020-10-22)

- Request black navigation bar when using dark theme.
- Add secondary heater active time (1 + 2), operating hours (1 - 3).
- Add controller date and time.
- Add compressor 2 operating hours, impulses and average runtime.
- Fix crash on Android 4.4 or older due to theme issue.

14 (2020-03-07)
--------------

- By default set dark mode based on system setting (Android 10 or newer) or by battery saver (Android 9 or older).
- Add various operating hours values.
- Add compressor impulse count and average runtime.

13 (2019-05-12)
--------------

- Add external energy source temperature.

12 (2019-05-01)
--------------

- Add return external temperature.

11 (2019-03-30)
--------------

- Fix text color in dark mode.

10 (2019-03-29)
--------------

- Fix crash when trying to display values.
- Updated style of message bar.

9 (2018-11-16)
--------------

- Add dark color scheme.
- Setting to switch to dark scheme at specified hours.

8 (2018-10-31)
--------------

- Small design tweaks.

7 (2018-07-27)
--------------

- Setting to choose which items to display.
- Tap to copy item text to clipboard.
- Add solar collector and tank temperature.

6 (2017-10-13)
--------------

- Add average and hot gas temperature.
- Use vector icons.
- Add adaptive launcher icon for Android Oreo.

5 (2017-03-15)
--------------

- Add more operating states.
- Tweak some colors to be less offensive.

4 (2016-06-22)
--------------

- Remove navigation drawer, use toolbar settings button instead.
- Text is now only selectable if paused. Fixes scroll position resetting on value updates.

3.2.0 (2015-11-08)
------------------

- Display operating state and firmware version.

3.1.1 (2015-11-05)
------------------

- Add notice about different default port.
- Support "going back" from Settings.
- Reduce APK size by shrinking with ProGuard.