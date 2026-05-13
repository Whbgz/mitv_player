package mitv.player;

import android.content.Context;
import android.provider.Settings;

final class SourceStatusReader {
    private static final String[] SETTINGS_KEYS = {
            "tv_current_source",
            "current_source",
            "input_source",
            "source_name",
            "sourceName",
            "source_id",
            "sourceId",
            "tv_source_id"
    };

    private SourceStatusReader() {
    }

    static SourceItem readCurrent(Context context) {
        for (String key : SETTINGS_KEYS) {
            SourceItem item = SourceCatalog.findCurrent(readSystem(context, key));
            if (item != null) {
                return item;
            }
            item = SourceCatalog.findCurrent(readGlobal(context, key));
            if (item != null) {
                return item;
            }
            item = SourceCatalog.findCurrent(readSecure(context, key));
            if (item != null) {
                return item;
            }
        }
        return ReflectionSourceSwitcher.readCurrent(context);
    }

    private static String readSystem(Context context, String key) {
        try {
            return Settings.System.getString(context.getContentResolver(), key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String readGlobal(Context context, String key) {
        try {
            return Settings.Global.getString(context.getContentResolver(), key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String readSecure(Context context, String key) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}

