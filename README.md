# MiTV Player

这是从 `模拟电视_1.5.28.apk` 中提取“观看电视”和“切换信号源”入口后整理出的独立 Android TV 小程序。APK 包名：

```text
mitv.player
```

## 功能

同一个 APK 内包含两个区域：

- `观看电视`：内置 HDMI 1/2/3 实验、上次观看、数字电视、模拟电视、AV 视频、当前外部设备、小米路由器。
- `切换信号源`：HDMI 1、HDMI 2、HDMI 3、当前外部设备、小米路由器、AV、TV、DTMB。

已从原 APK 的 manifest 和 dex 字符串中确认这些入口或线索：

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

## HDMI 说明

实测 `com.xiaomi.mitv.tvplayer.EXTSRC_PLAY` 不按 `sourceName/sourceId` 精确切换 HDMI1/2/3，它会打开电视当前识别到的外部设备。例如设备实际插在 HDMI3 且被识别为“小米路由器”时，即使传 `HDMI1` 或 `ROUTER`，实际也会进入 HDMI3。

所以当前版本里：

- `HDMI 1/2/3` 卡片已经恢复显示，但只在系统 ROM 反射 API 可用时才可能精确切换。
- `当前外部设备` 和 `小米路由器` 仍走原系统模拟电视暴露的 `EXTSRC_PLAY` 入口。
- `内置 HDMI 1/2/3` 是实验播放入口，会尝试在本应用内创建 `SurfaceView` 并反射调用 `mitv.tv.*` 私有接口。

## 构建

在 GitHub Actions 里会自动构建 debug APK。构建成功后进入：

`Actions` -> 最新的 `build` -> 页面底部 `Artifacts` -> 下载 `mitv-player-debug-apk`

本地命令行也可以执行：

```powershell
gradle :app:assembleDebug
```

生成位置通常是：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 权限边界

小米电视的底层切源和 HDMI/ATV/DTMB 播放通常依赖系统签名权限、TV HAL/native 库和 ROM 私有服务。普通侧载 APK 不一定能直接拿到视频流。

本应用当前采用多策略尝试：

1. 优先启动系统模拟电视暴露的 action 和 Activity。
2. 对 HDMI1/2/3 尝试反射 ROM 内常见的 `switchInputSource` / `setInputSource`。
3. `内置 HDMI 1/2/3` 尝试在本应用内绑定 `SurfaceView` 并调用 `mitv.tv.TvViewManager` / `mitv.tv.PlayerManager` / `mitv.tv.SourceManager`。

如果内置播放没有画面，请看屏幕底部状态文字，它会显示当前找不到哪个私有类或方法。后续可以根据这个错误继续补 ROM 适配。

## ADB 调试

```powershell
adb shell am start -a com.xiaomi.mitv.tvplayer.EXTSRC_PLAY --es sourceName HDMI1 --es source_name HDMI1 --ei sourceId 1 --ei source_id 1
adb shell am start -a com.xiaomi.mitv.tvplayer.EXTSRC_PLAY --es sourceName ROUTER --es source_name ROUTER --es sourceTitle 小米路由器 --es device_name 小米路由器 --ei sourceId 40 --ei source_id 40
adb shell am start -a com.xiaomi.mitv.tvplayer.AUX_PLAY
adb shell am start -a com.xiaomi.mitv.tvplayer.ATV_PLAY
adb shell am start -a com.xiaomi.mitv.tvplayer.DTMB_PLAY
```

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

当前主工程是本目录，构建后生成 `mitv.player`。之前拆出来的 `tv-watch-launcher` 只是过渡工程，功能已经合并进本应用，不再作为最终程序使用。
