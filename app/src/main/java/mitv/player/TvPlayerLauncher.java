package mitv.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

final class TvPlayerLauncher {
    private TvPlayerLauncher() {
    }

    static LaunchResult launch(Context context, WatchTarget target) {
        List<Intent> candidates = new ArrayList<>();
        candidates.add(buildIntent(target, true));
        candidates.add(buildIntent(target, false));

        if (target.activityClassName != null) {
            Intent componentIntent = buildIntent(target, false);
            componentIntent.setComponent(new ComponentName(SourceCatalog.TVPLAYER_PACKAGE, target.activityClassName));
            candidates.add(componentIntent);
        }

        StringBuilder errors = new StringBuilder();
        for (Intent intent : candidates) {
            LaunchResult result = startIfResolvable(context, intent);
            if (result.success) {
                return result;
            }
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(result.detail);
        }
        return LaunchResult.fail(errors.toString());
    }

    private static Intent buildIntent(WatchTarget target, boolean packageOnly) {
        Intent intent = new Intent(target.action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (packageOnly) {
            intent.setPackage(SourceCatalog.TVPLAYER_PACKAGE);
        }

        Bundle extras = new Bundle();
        extras.putBoolean("from_mitv_player", true);
        extras.putString("entrance", "mitv.player");
        if (target.sourceName != null) {
            extras.putString("sourceName", target.sourceName);
            extras.putString("source_name", target.sourceName);
            extras.putString("source", target.sourceName);
            extras.putString("sourceMode", target.sourceName);
        }
        if (target.sourceId >= 0) {
            extras.putInt("sourceId", target.sourceId);
            extras.putInt("source_id", target.sourceId);
            extras.putInt("tv_source_id", target.sourceId);
            extras.putInt("inputId", target.sourceId);
        }
        intent.putExtras(extras);
        return intent;
    }

    private static LaunchResult startIfResolvable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null && intent.getComponent() == null) {
            return LaunchResult.fail("unresolved:" + intent.getAction());
        }
        try {
            context.startActivity(intent);
            return LaunchResult.ok("intent:" + describe(intent));
        } catch (RuntimeException exception) {
            return LaunchResult.fail("start failed:" + exception.getClass().getSimpleName());
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

