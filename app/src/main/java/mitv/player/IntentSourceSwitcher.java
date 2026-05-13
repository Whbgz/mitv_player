package mitv.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

final class IntentSourceSwitcher {
    private IntentSourceSwitcher() {
    }

    static SwitchResult switchTo(Context context, SourceItem item) {
        List<Intent> candidates = new ArrayList<>();
        candidates.add(buildActionIntent(item, true));
        candidates.add(buildActionIntent(item, false));

        if (item.activityClassName != null) {
            Intent componentIntent = buildActionIntent(item, false);
            componentIntent.setComponent(new ComponentName(SourceCatalog.TVPLAYER_PACKAGE, item.activityClassName));
            candidates.add(componentIntent);
        }

        StringBuilder errors = new StringBuilder();
        for (Intent intent : candidates) {
            SwitchResult result = startIfResolvable(context, intent);
            if (result.success) {
                return result;
            }
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(result.detail);
        }
        return SwitchResult.fail(errors.toString());
    }

    private static Intent buildActionIntent(SourceItem item, boolean packageOnly) {
        Intent intent = new Intent(item.action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (packageOnly) {
            intent.setPackage(SourceCatalog.TVPLAYER_PACKAGE);
        }

        Bundle extras = new Bundle();
        extras.putString("sourceName", item.romValue);
        extras.putString("source_name", item.romValue);
        extras.putString("source", item.romValue);
        extras.putString("source_path", item.key);
        extras.putString("sourceMode", item.romValue);
        extras.putString("sourceTitle", item.title);
        extras.putString("source_name_text", item.title);
        extras.putString("device_name", item.title);
        extras.putString("deviceType", item.key);
        extras.putString("sourceType", item.kind.name());
        extras.putBoolean("is_external_device", item.isDeviceAlias());
        extras.putInt("sourceId", item.numericId);
        extras.putInt("source_id", item.numericId);
        extras.putInt("tv_source_id", item.numericId);
        extras.putInt("inputId", item.numericId);
        extras.putBoolean("from_source_switcher", true);
        intent.putExtras(extras);
        return intent;
    }

    private static SwitchResult startIfResolvable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null && intent.getComponent() == null) {
            return SwitchResult.fail("unresolved:" + intent.getAction());
        }
        try {
            context.startActivity(intent);
            return SwitchResult.ok("intent:" + describe(intent));
        } catch (RuntimeException exception) {
            return SwitchResult.fail("start failed:" + exception.getClass().getSimpleName());
        }
    }

    private static String describe(Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            return componentName.flattenToShortString();
        }
        return intent.getAction();
    }
}

