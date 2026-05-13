package mitv.player;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class EmbeddedPlayerActivity extends Activity {
    static final String EXTRA_TITLE = "mitv.player.extra.TITLE";
    static final String EXTRA_SOURCE_NAME = "mitv.player.extra.SOURCE_NAME";
    static final String EXTRA_SOURCE_ID = "mitv.player.extra.SOURCE_ID";

    private TextView statusView;
    private String title;
    private String sourceName;
    private int sourceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        sourceName = getIntent().getStringExtra(EXTRA_SOURCE_NAME);
        sourceId = getIntent().getIntExtra(EXTRA_SOURCE_ID, 3);
        if (title == null || title.length() == 0) {
            title = "内置 HDMI " + sourceId;
        }
        if (sourceName == null || sourceName.length() == 0) {
            sourceName = "HDMI" + sourceId;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        statusView = new TextView(this);
        statusView.setText(title + "：正在初始化...");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(20);
        statusView.setGravity(Gravity.CENTER);
        statusView.setBackgroundColor(Color.argb(150, 0, 0, 0));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        statusParams.setMargins(24, 24, 24, 24);
        root.addView(statusView, statusParams);

        setContentView(root);
        String result = EmbeddedTvEngine.start(this, root, sourceName, sourceId);
        statusView.setText(result);
        statusView.bringToFront();
    }

    @Override
    protected void onDestroy() {
        EmbeddedTvEngine.stop(this);
        super.onDestroy();
    }
}
