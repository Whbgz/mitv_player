param(
    [string]$Adb = "adb"
)

$ErrorActionPreference = "Stop"

$tests = @(
    @{
        Name = "Current external source via HDMI1 extras"
        Args = @("shell", "am", "start", "-a", "com.xiaomi.mitv.tvplayer.EXTSRC_PLAY", "--es", "sourceName", "HDMI1", "--es", "source_name", "HDMI1", "--ei", "sourceId", "1", "--ei", "source_id", "1")
    },
    @{
        Name = "Current external source via router extras"
        Args = @("shell", "am", "start", "-a", "com.xiaomi.mitv.tvplayer.EXTSRC_PLAY", "--es", "sourceName", "ROUTER", "--es", "source_name", "ROUTER", "--es", "sourceTitle", "小米路由器", "--es", "device_name", "小米路由器", "--ei", "sourceId", "40", "--ei", "source_id", "40")
    },
    @{
        Name = "AV"
        Args = @("shell", "am", "start", "-a", "com.xiaomi.mitv.tvplayer.AUX_PLAY")
    },
    @{
        Name = "TV"
        Args = @("shell", "am", "start", "-a", "com.xiaomi.mitv.tvplayer.ATV_PLAY")
    },
    @{
        Name = "DTMB"
        Args = @("shell", "am", "start", "-a", "com.xiaomi.mitv.tvplayer.DTMB_PLAY")
    }
)

foreach ($test in $tests) {
    Write-Host "Testing $($test.Name)..." -ForegroundColor Cyan
    & $Adb @($test.Args)
    Start-Sleep -Seconds 2
}
