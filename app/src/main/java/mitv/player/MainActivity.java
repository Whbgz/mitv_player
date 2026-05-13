package mitv.player;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String TAG = "SourceSwitcher";
    private static final int BACKGROUND = Color.rgb(23, 25, 31);
    private static final int PANEL = Color.rgb(36, 39, 47);
    private static final int FOCUSED = Color.rgb(47, 125, 246);
    private static final int TEXT_PRIMARY = Color.rgb(242, 244, 248);
    private static final int TEXT_SECONDARY = Color.rgb(156, 163, 175);
    private static final int ONLINE = Color.rgb(34, 197, 94);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScrollView scrollView;
    private TextView statusView;
    private SourceItem currentSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
        refreshCurrentSource();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentSource();
    }

    private View createContentView() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(48), dp(46), dp(48), dp(42));
        root.setBackgroundColor(BACKGROUND);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText(R.string.title);
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        statusView = new TextView(this);
        statusView.setText(R.string.status_ready);
        statusView.setTextColor(TEXT_SECONDARY);
        statusView.setTextSize(16);
        statusView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(12);
        root.addView(statusView, statusParams);

        TextView watchHeader = createSectionHeader(R.string.section_watch);
        LinearLayout.LayoutParams watchHeaderParams = new LinearLayout.LayoutParams(dp(760), LinearLayout.LayoutParams.WRAP_CONTENT);
        watchHeaderParams.topMargin = dp(24);
        root.addView(watchHeader, watchHeaderParams);

        GridLayout watchGrid = new GridLayout(this);
        watchGrid.setColumnCount(4);
        for (WatchTarget target : WatchCatalog.ALL) {
            watchGrid.addView(createWatchCard(target));
        }
        LinearLayout.LayoutParams watchParams = new LinearLayout.LayoutParams(dp(760), LinearLayout.LayoutParams.WRAP_CONTENT);
        watchParams.topMargin = dp(8);
        root.addView(watchGrid, watchParams);

        TextView sourceHeader = createSectionHeader(R.string.section_source);
        LinearLayout.LayoutParams sourceHeaderParams = new LinearLayout.LayoutParams(dp(760), LinearLayout.LayoutParams.WRAP_CONTENT);
        sourceHeaderParams.topMargin = dp(18);
        root.addView(sourceHeader, sourceHeaderParams);

        GridLayout sourceGrid = new GridLayout(this);
        sourceGrid.setColumnCount(4);
        for (SourceItem item : SourceCatalog.ALL) {
            if (shouldShowSourceCard(item)) {
                sourceGrid.addView(createSourceCard(item));
            }
        }
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(dp(760), LinearLayout.LayoutParams.WRAP_CONTENT);
        sourceParams.topMargin = dp(8);
        root.addView(sourceGrid, sourceParams);
        return scrollView;
    }

    private TextView createSectionHeader(int resId) {
        TextView header = new TextView(this);
        header.setText(resId);
        header.setTextColor(TEXT_SECONDARY);
        header.setTextSize(18);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        return header;
    }

    private View createWatchCard(final WatchTarget target) {
        return createCard(target.title, iconForWatch(target.key), new Runnable() {
            @Override
            public void run() {
                launchWatch(target);
            }
        });
    }

    private View createSourceCard(final SourceItem item) {
        return createCard(item.title, iconFor(item.kind), new Runnable() {
            @Override
            public void run() {
                switchTo(item);
            }
        });
    }

    private View createCard(String title, String iconText, final Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(dp(10), dp(10), dp(10), dp(8));
        card.setBackgroundColor(PANEL);

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextColor(TEXT_PRIMARY);
        icon.setTextSize(28);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(TEXT_PRIMARY);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        card.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(178);
        params.height = dp(88);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(params);

        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                view.setBackgroundColor(hasFocus ? ONLINE : PANEL);
                if (hasFocus) {
                    ensureVisible(view);
                }
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.run();
            }
        });
        card.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    action.run();
                    return true;
                }
                return false;
            }
        });
        return card;
    }

    private void ensureVisible(final View view) {
        if (scrollView == null) {
            return;
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                int[] scrollLocation = new int[2];
                int[] viewLocation = new int[2];
                scrollView.getLocationOnScreen(scrollLocation);
                view.getLocationOnScreen(viewLocation);

                int top = viewLocation[1] - scrollLocation[1];
                int bottom = top + view.getHeight();
                int viewport = scrollView.getHeight();
                int padding = dp(36);

                if (bottom > viewport - padding) {
                    scrollView.smoothScrollBy(0, bottom - viewport + padding);
                } else if (top < padding) {
                    scrollView.smoothScrollBy(0, top - padding);
                }
            }
        });
    }

    private boolean shouldShowSourceCard(SourceItem item) {
        return "external".equals(item.key)
                || "router".equals(item.key)
                || "av".equals(item.key)
                || "tv".equals(item.key)
                || "dtmb".equals(item.key);
    }

    private void launchWatch(WatchTarget target) {
        statusView.setText(getString(R.string.status_launching, target.title));
        LaunchResult result = TvPlayerLauncher.launch(this, target);
        if (result.success) {
            statusView.setText(getString(R.string.status_launch_success, target.title));
        } else {
            Log.w(TAG, "Failed to launch " + target.title + ": " + result.detail);
            statusView.setText(getString(R.string.status_launch_failed, target.title));
        }
    }

    private void switchTo(SourceItem item) {
        statusView.setText(getString(R.string.status_switching, item.title));
        SwitchResult result = SourceSwitcher.switchTo(this, item);
        if (result.success) {
            currentSource = item;
            statusView.setText(getString(R.string.status_success, item.title));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshCurrentSource();
                }
            }, 1500L);
        } else {
            Log.w(TAG, "Failed to switch to " + item.title + ": " + result.detail);
            statusView.setText(getString(R.string.status_failed, item.title));
        }
    }

    private void refreshCurrentSource() {
        SourceItem detected = SourceStatusReader.readCurrent(this);
        if (detected != null) {
            currentSource = detected;
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private String iconFor(SourceKind kind) {
        switch (kind) {
            case HDMI:
                return "▭";
            case ATV:
                return "TV";
            case AV:
                return "◉";
            case DTMB:
                return "▣";
            case EXTERNAL:
                return "▭";
            case ROUTER:
                return "⌂";
            case MI_BOX:
                return "▣";
            case MI_PORT:
                return "◇";
            case BLU_RAY:
                return "BD";
            case SOUNDBAR:
                return "▰";
            case HOME_CINEMA:
                return "▤";
            case USB:
                return "USB";
            case VGA:
                return "VGA";
            default:
                return "•";
        }
    }

    private String iconForWatch(String key) {
        if ("play".equals(key)) {
            return "▶";
        }
        if ("dtmb".equals(key)) {
            return "▣";
        }
        if ("atv".equals(key)) {
            return "TV";
        }
        if ("av".equals(key)) {
            return "◉";
        }
        if ("router".equals(key)) {
            return "⌂";
        }
        return "▭";
    }
}

