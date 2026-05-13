package mitv.player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class SourceCatalog {
    static final String TVPLAYER_PACKAGE = "com.xiaomi.mitv.tvplayer";
    static final String ACTION_EXTSRC_PLAY = TVPLAYER_PACKAGE + ".EXTSRC_PLAY";
    static final String ACTION_ATV_PLAY = TVPLAYER_PACKAGE + ".ATV_PLAY";
    static final String ACTION_AUX_PLAY = TVPLAYER_PACKAGE + ".AUX_PLAY";
    static final String ACTION_DTMB_PLAY = TVPLAYER_PACKAGE + ".DTMB_PLAY";

    static final List<SourceItem> ALL = Collections.unmodifiableList(Arrays.asList(
            new SourceItem("hdmi1", "HDMI 1", SourceKind.HDMI, 1, "HDMI1", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "source_hdmi1", "input_source_name_hdmi1"),
            new SourceItem("hdmi2", "HDMI 2", SourceKind.HDMI, 2, "HDMI2", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "source_hdmi2", "input_source_name_hdmi2"),
            new SourceItem("hdmi3", "HDMI 3", SourceKind.HDMI, 3, "HDMI3", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "source_hdmi3", "input_source_name_hdmi3"),
            new SourceItem("tv", "TV", SourceKind.ATV, 10, "ATV", ACTION_ATV_PLAY, TVPLAYER_PACKAGE + ".AtvActivity", "source_tv", "input_source_name_tv", "Analog TV"),
            new SourceItem("av", "AV", SourceKind.AV, 20, "AUX", ACTION_AUX_PLAY, TVPLAYER_PACKAGE + ".AuxActivity", "AUXIN", "source_av", "input_source_name_av"),
            new SourceItem("dtmb", "DTMB", SourceKind.DTMB, 30, "DTMB", ACTION_DTMB_PLAY, TVPLAYER_PACKAGE + ".dtmb.DTMBActivity", "source_dtmb", "Digital TV"),
            new SourceItem("external", "当前外部设备", SourceKind.EXTERNAL, 3, "HDMI", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "EXTSRC", "external", "TV external source", "Input source"),
            new SourceItem("router", "小米路由器", SourceKind.ROUTER, 40, "ROUTER", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "Xiaomi router", "Mi Router", "ch_router", "ch_router_str", "pic_signal_router", "icon_inputsource_router"),
            new SourceItem("mibox", "小米盒子", SourceKind.MI_BOX, 41, "MIBOX", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "Mi Box", "tvbox", "ch_xiaomi"),
            new SourceItem("miport", "Mi Port", SourceKind.MI_PORT, 42, "MIPORT", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "MiPort", "Mi Port", "output_miport"),
            new SourceItem("bluray", "蓝光播放器", SourceKind.BLU_RAY, 43, "BLURAY", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "Blu-ray player", "Blu ray player"),
            new SourceItem("soundbar", "Soundbar", SourceKind.SOUNDBAR, 44, "SOUNDBAR", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "Mi soundbar", "Soundbar", "output_mibar"),
            new SourceItem("homecinema", "小米放映室", SourceKind.HOME_CINEMA, 45, "HOME_CINEMA", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "Xiaomi Home Cinema"),
            new SourceItem("usb", "USB", SourceKind.USB, 50, "USB", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "ch_usb", "pic_signal_usb", "icon_inputsource_usb"),
            new SourceItem("vga", "VGA", SourceKind.VGA, 60, "VGA", ACTION_EXTSRC_PLAY, TVPLAYER_PACKAGE + ".ExternalSourceActivity", "ch_vga", "input_source_name_vga")
    ));

    private SourceCatalog() {
    }

    static SourceItem findCurrent(String value) {
        for (SourceItem item : ALL) {
            if (item.matches(value)) {
                return item;
            }
        }
        return null;
    }
}

