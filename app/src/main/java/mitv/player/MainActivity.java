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

import java.util.HashMap;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final String TAG = "SourceSwitcher";
    private static final int BACKGROUND = Color.rgb(23, 25, 31);
    private static final int PANEL = Color.rgb(36, 39, 47);
    private static final int FOCUSED = Color.rgb(47, 125, 246);
    private static final int TEXT_PRIMARY = Color.rgb(242, 244, 248);
    private static final int TEXT_SECONDARY = Color.rgb(156, 163, 175);
    private static final int ONLINE = Color.rgb(34, 197, 94);
    private static final int INDICATOR = Color.rgb(105, 111, 120);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, SourceRow> rows = new HashMap<>();
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(48), dp(64), dp(48), dp(36));
        root.setBackgroundColor(BACKGROUND);

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
        watchHeaderParams.topMargin = dp(32);
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
        sourceHeaderParams.topMargin = dp(22);
        root.addView(sourceHeader, sourceHeaderParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(8));
        scrollView.addView(list);

        for (SourceItem item : SourceCatalog.ALL) {
            SourceRow row = new SourceRow(item);
            rows.put(item.key, row);
            list.addView(row.container);
        }

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(dp(760), 0, 1f);
        scrollParams.topMargin = dp(8);
        root.addView(scrollView, scrollParams);
        return root;
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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(dp(10), dp(10), dp(10), dp(8));
        card.setBackgroundColor(PANEL);

        TextView icon = new TextView(this);
        icon.setText(iconForWatch(target.key));
        icon.setTextColor(TEXT_PRIMARY);
        icon.setTextSize(28);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        TextView label = new TextView(this);
        label.setText(target.title);
        label.setTextColor(TEXT_PRIMARY);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        card.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(178);
        params.height = dp(92);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(params);

        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                view.setBackgroundColor(hasFocus ? ONLINE : PANEL);
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchWatch(target);
            }
        });
        card.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    launchWatch(target);
                    return true;
                }
                return false;
            }
        });
        return card;
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
            updateRows();
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
        updateRows();
    }

    private void updateRows() {
        for (SourceRow row : rows.values()) {
            row.bind(row.item == currentSource);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private final class SourceRow {
        final SourceItem item;
        final LinearLayout container;
        final TextView signalDot;
        final TextView indicator;

        SourceRow(final SourceItem item) {
            this.item = item;
            container = new LinearLayout(MainActivity.this);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER_VERTICAL);
            container.setFocusable(true);
            container.setClickable(true);
            container.setPadding(dp(28), 0, dp(28), 0);
            container.setBackgroundColor(PANEL);
            container.setMinimumHeight(dp(74));

            TextView icon = new TextView(MainActivity.this);
            icon.setText(iconFor(item.kind));
            icon.setTextColor(TEXT_PRIMARY);
            icon.setTextSize(28);
            icon.setGravity(Gravity.CENTER);
            container.addView(icon, new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.MATCH_PARENT));

            signalDot = new TextView(MainActivity.this);
            signalDot.setText("•");
            signalDot.setTextColor(ONLINE);
            signalDot.setTextSize(28);
            signalDot.setGravity(Gravity.CENTER);
            container.addView(signalDot, new LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.MATCH_PARENT));

            TextView label = new TextView(MainActivity.this);
            label.setText(item.title);
            label.setTextColor(TEXT_PRIMARY);
            label.setTextSize(27);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER_VERTICAL);
            container.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            indicator = new TextView(MainActivity.this);
            indicator.setText("●");
            indicator.setTextColor(INDICATOR);
            indicator.setTextSize(26);
            indicator.setGravity(Gravity.CENTER);
            container.addView(indicator, new LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(74)
            );
            params.bottomMargin = dp(4);
            container.setLayoutParams(params);

            container.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    container.setBackgroundColor(hasFocus ? FOCUSED : PANEL);
                }
            });
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchTo(item);
                }
            });
            container.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP
                            && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                        switchTo(item);
                        return true;
                    }
                    return false;
                }
            });
        }

        void bind(boolean selected) {
            indicator.setTextColor(selected ? TEXT_PRIMARY : INDICATOR);
            signalDot.setVisibility(selected || item.kind == SourceKind.HDMI || item.isDeviceAlias()
                    ? View.VISIBLE : View.INVISIBLE);
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

