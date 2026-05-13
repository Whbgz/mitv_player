package mitv.player;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class EmbeddedPlayerActivity extends Activity {
    static final String EXTRA_TITLE = "mitv.player.extra.TITLE";
    static final String EXTRA_SOURCE_NAME = "mitv.player.extra.SOURCE_NAME";
    static final String EXTRA_SOURCE_ID = "mitv.player.extra.SOURCE_ID";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private TextView statusView;
    private EmbeddedTvEngine.Session session;
    private boolean runningStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String sourceName = getIntent().getStringExtra(EXTRA_SOURCE_NAME);
        int sourceId = getIntent().getIntExtra(EXTRA_SOURCE_ID, 3);
        if (title == null || title.length() == 0) {
            title = "内置 HDMI " + sourceId;
        }
        if (sourceName == null || sourceName.length() == 0) {
            sourceName = "HDMI" + sourceId;
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(20);
        statusView.setGravity(Gravity.CENTER);
        statusView.setBackgroundColor(Color.argb(170, 0, 0, 0));
        statusView.setPadding(28, 22, 28, 22);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        statusParams.setMargins(24, 24, 24, 24);
        root.addView(statusView, statusParams);

        setContentView(root);
        session = EmbeddedTvEngine.createSession(this, root, sourceName, sourceId);
        statusView.setText(title + "\n诊断模式：按确认键执行下一步\n" + session.nextStepLabel());
        statusView.bringToFront();
        root.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            runNextStep();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void runNextStep() {
        if (runningStep || session == null || session.isComplete()) {
            return;
        }
        runningStep = true;
        statusView.setText("正在执行，若闪退请记住这一步：\n" + session.nextStepLabel());
        statusView.bringToFront();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String result = EmbeddedTvEngine.runNextStep(session);
                runningStep = false;
                statusView.setText(result + "\n\n" + session.nextStepLabel());
                statusView.bringToFront();
            }
        }, 350L);
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            EmbeddedTvEngine.stop(session);
        }
        super.onDestroy();
    }
}
