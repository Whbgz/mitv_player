# MiTV Player

这是从 `模拟电视_1.5.28.apk` 中提取“观看电视”和“切换信号源”入口后整理出的独立 Android TV 小程序。最终 APK 包名为：

```text
mitv.player
```

原 APK 的 manifest 和 dex 字符串里能看到这些关键入口：

- `com.xiaomi.mitv.tvplayer.EXTSRC_PLAY`
- `com.xiaomi.mitv.tvplayer.ATV_PLAY`
- `com.xiaomi.mitv.tvplayer.AUX_PLAY`
- `com.xiaomi.mitv.tvplayer.DTMB_PLAY`
- `com.xiaomi.mitv.tvplayer.PLAY`
- `com.xiaomi.mitv.tvplayer.ExternalSourceActivity`
- `switchInputSource`
- `source_hdmi1`、`source_hdmi2`、`source_hdmi3`、`source_av`、`source_tv`、`source_dtmb`
- `icon_inputsource_router`、`pic_signal_router`、`ch_router`、`ch_router_str`
- `Mi Box`、`Mi Port`、`Blu-ray player`、`Soundbar`、`Xiaomi Home Cinema`、`USB`、`VGA`

## 功能

同一个 APK 内包含两个区域：

- `观看电视`：打开上次观看、数字电视、模拟电视、AV 视频、当前外部设备/小米路由器入口。
- `切换信号源`：打开固定物理源和设备型外部源。

切换信号源支持的固定物理源：

- `HDMI 1`
- `HDMI 2`
- `HDMI 3`
- `TV`
- `AV`
- `DTMB`
- `当前外部设备`
- `USB`
- `VGA`

设备型外部源：

- `小米路由器`
- `小米盒子`
- `Mi Port`
- `蓝光播放器`
- `Soundbar`
- `小米放映室`

注意：实测 `com.xiaomi.mitv.tvplayer.EXTSRC_PLAY` 不按 `sourceName/sourceId` 精确切换 HDMI1/2/3，它会打开电视当前识别到的外部设备。例如设备实际插在 HDMI3 且被识别为“小米路由器”时，即使传 `HDMI1` 或 `ROUTER`，实际也会进入 HDMI3。因此本应用把它标成 `当前外部设备` / `小米路由器` 入口；固定 `HDMI 1/2/3` 只有系统 ROM 反射 API 成功时才会算精确切换成功。

## 构建

用 Android Studio 打开本目录，执行 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。

命令行环境已安装 Android SDK 和 Gradle 时，也可以执行：

```powershell
gradle :app:assembleDebug
```

## 权限说明

小米电视的底层切源能力通常需要系统签名权限。这个应用采用多策略尝试：

1. 优先启动原 `模拟电视` 应用暴露的 action 和 Activity。
2. 再尝试设备 ROM 中常见的反射 API，例如 `switchInputSource` / `setInputSource`。
3. 最后通过 `am start` 命令兜底发送同样的 action。

普通侧载安装时，如果系统拒绝第三方应用切换信号源，需要把 APK 做成系统应用或使用电视固件对应的平台证书签名。

## 调试命令

可以先用 ADB 验证电视是否接受原 action：

```powershell
adb shell am start -a com.xiaomi.mitv.tvplayer.EXTSRC_PLAY --es sourceName HDMI1 --es source_name HDMI1 --ei sourceId 1 --ei source_id 1
adb shell am start -a com.xiaomi.mitv.tvplayer.EXTSRC_PLAY --es sourceName ROUTER --es source_name ROUTER --es sourceTitle 小米路由器 --es device_name 小米路由器 --ei sourceId 40 --ei source_id 40
adb shell am start -a com.xiaomi.mitv.tvplayer.AUX_PLAY
adb shell am start -a com.xiaomi.mitv.tvplayer.ATV_PLAY
adb shell am start -a com.xiaomi.mitv.tvplayer.DTMB_PLAY
```

如果需要精确切换 HDMI1/2/3，需要继续确认电视 ROM 是否开放 `switchInputSource` / `setInputSource` 等系统 API，普通 `EXTSRC_PLAY` action 不足以完成。

安装本应用后，也可以直接调用 `mitv.player` 的快捷入口：

```powershell
adb shell am start -a mitv.player.PLAY
adb shell am start -a mitv.player.DTMB
adb shell am start -a mitv.player.ATV
adb shell am start -a mitv.player.AV
adb shell am start -a mitv.player.EXTERNAL
adb shell am start -a mitv.player.ROUTER
```

## 工程说明

当前主工程是本目录，构建后生成 `mitv.player`。此前拆出来的 `tv-watch-launcher` 只是过渡工程，功能已经合并进本应用，不再作为最终程序使用。
