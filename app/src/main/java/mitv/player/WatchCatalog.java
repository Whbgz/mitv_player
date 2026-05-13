package mitv.player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class WatchCatalog {
    static final String ACTION_PLAY = SourceCatalog.TVPLAYER_PACKAGE + ".PLAY";

    static final List<WatchTarget> ALL = Collections.unmodifiableList(Arrays.asList(
            new WatchTarget("play", "上次观看", ACTION_PLAY, null, -1, null),
            new WatchTarget("dtmb", "数字电视", SourceCatalog.ACTION_DTMB_PLAY, "DTMB", 30, SourceCatalog.TVPLAYER_PACKAGE + ".dtmb.DTMBActivity"),
            new WatchTarget("atv", "模拟电视", SourceCatalog.ACTION_ATV_PLAY, "ATV", 10, SourceCatalog.TVPLAYER_PACKAGE + ".AtvActivity"),
            new WatchTarget("av", "AV视频", SourceCatalog.ACTION_AUX_PLAY, "AUX", 20, SourceCatalog.TVPLAYER_PACKAGE + ".AuxActivity"),
            new WatchTarget("external", "当前外部设备", SourceCatalog.ACTION_EXTSRC_PLAY, "HDMI", 3, SourceCatalog.TVPLAYER_PACKAGE + ".ExternalSourceActivity"),
            new WatchTarget("router", "小米路由器", SourceCatalog.ACTION_EXTSRC_PLAY, "ROUTER", 40, SourceCatalog.TVPLAYER_PACKAGE + ".ExternalSourceActivity")
    ));

    private WatchCatalog() {
    }

    static WatchTarget byKey(String key) {
        if (key == null) {
            return ALL.get(0);
        }
        for (WatchTarget target : ALL) {
            if (target.key.equalsIgnoreCase(key)) {
                return target;
            }
        }
        return ALL.get(0);
    }

    static WatchTarget byQuickAction(String action) {
        if ("mitv.player.DTMB".equals(action)) {
            return byKey("dtmb");
        }
        if ("mitv.player.ATV".equals(action)) {
            return byKey("atv");
        }
        if ("mitv.player.AV".equals(action)) {
            return byKey("av");
        }
        if ("mitv.player.EXTERNAL".equals(action)) {
            return byKey("external");
        }
        if ("mitv.player.ROUTER".equals(action)) {
            return byKey("router");
        }
        return byKey("play");
    }
}

