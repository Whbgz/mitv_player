package mitv.player;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public final class QuickLaunchActivity extends Activity {
    private static final String TAG = "MitvPlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WatchTarget target = WatchCatalog.byQuickAction(getIntent() == null ? null : getIntent().getAction());
        LaunchResult result = TvPlayerLauncher.launch(this, target);
        if (!result.success) {
            Log.w(TAG, "Failed to quick launch " + target.title + ": " + result.detail);
            Toast.makeText(this, "无法打开 " + target.title, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
