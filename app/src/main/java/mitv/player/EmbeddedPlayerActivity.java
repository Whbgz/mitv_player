package mitv.player;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class EmbeddedPlayerActivity extends Activity implements SurfaceHolder.Callback {
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

        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        statusView = new TextView(this);
        statusView.setText(title + "：等待 Surface...");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(20);
        statusView.setGravity(Gravity.CENTER);
        statusView.setBackgroundColor(Color.argb(140, 0, 0, 0));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        statusParams.setMargins(24, 24, 24, 24);
        root.addView(statusView, statusParams);

        setContentView(root);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        String result = EmbeddedTvEngine.start(this, holder.getSurface(), sourceName, sourceId);
        statusView.setText(result);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        EmbeddedTvEngine.stop(this);
    }

    @Override
    protected void onDestroy() {
        EmbeddedTvEngine.stop(this);
        super.onDestroy();
    }
}
