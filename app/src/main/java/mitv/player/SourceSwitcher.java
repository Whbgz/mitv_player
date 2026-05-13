package mitv.player;

import android.content.Context;

final class SourceSwitcher {
    private SourceSwitcher() {
    }

    static SwitchResult switchTo(Context context, SourceItem item) {
        if (item.requiresPreciseSwitch()) {
            SwitchResult reflectionResult = ReflectionSourceSwitcher.switchTo(context, item);
            if (reflectionResult.success) {
                return reflectionResult;
            }
            return SwitchResult.fail("public EXTSRC_PLAY opens the current external source; precise "
                    + item.title + " switching needs ROM/system API permission. " + reflectionResult.detail);
        }

        SwitchResult intentResult = IntentSourceSwitcher.switchTo(context, item);
        if (intentResult.success) {
            return intentResult;
        }

        SwitchResult reflectionResult = ReflectionSourceSwitcher.switchTo(context, item);
        if (reflectionResult.success) {
            return reflectionResult;
        }

        SwitchResult shellResult = ShellSourceSwitcher.switchTo(item);
        if (shellResult.success) {
            return shellResult;
        }

        return SwitchResult.fail("intent[" + intentResult.detail + "], reflection["
                + reflectionResult.detail + "], shell[" + shellResult.detail + "]");
    }
}

