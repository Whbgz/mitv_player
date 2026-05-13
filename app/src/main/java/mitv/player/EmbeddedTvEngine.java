package mitv.player;

import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

final class EmbeddedTvEngine {
    private EmbeddedTvEngine() {
    }

    static Session createSession(Context context, FrameLayout container, String sourceName, int sourceId) {
        return new Session(context, container, sourceName, sourceId);
    }

    static String runNextStep(Session session) {
        if (session == null || session.isComplete()) {
            return "诊断已完成";
        }
        switch (session.step) {
            case 0:
                session.tvInputManager = (TvInputManager) session.context.getSystemService(Context.TV_INPUT_SERVICE);
                session.step++;
                return session.report("已完成：获取系统 TvInputManager");
            case 1:
                if (session.tvInputManager != null) {
                    session.inputs = session.tvInputManager.getTvInputList();
                    session.selectedInput = chooseInput(session);
                } else {
                    append(session.errors, "TvInputManager=null");
                }
                session.step++;
                return session.report("已完成：扫描系统 TV 输入\n" + describeInputs(session));
            case 2:
                session.tvView = new TvView(session.context);
                session.container.addView(session.tvView, 0, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
                session.step++;
                return session.report("已完成：创建 Android TvView");
            case 3:
                if (session.tvView != null && session.selectedInput != null) {
                    Uri uri = TvContract.buildChannelUriForPassthroughInput(session.selectedInput.getId());
                    session.tvView.tune(session.selectedInput.getId(), uri);
                    session.tuned = true;
                } else {
                    append(session.errors, "tune:TvView/input=null");
                }
                session.step++;
                if (session.tuned) {
                    return "已调用系统 TvView 播放：" + inputTitle(session.context, session.selectedInput);
                }
                return session.report("系统 TvView 播放失败");
            default:
                session.step = Session.STEP_COUNT;
                return "诊断已完成";
        }
    }

    static void stop(Session session) {
        if (session != null && session.tvView != null) {
            session.tvView.reset();
        }
    }

    private static TvInputInfo chooseInput(Session session) {
        if (session.inputs == null || session.inputs.isEmpty()) {
            append(session.errors, "系统没有返回 TV 输入列表");
            return null;
        }
        TvInputInfo firstHdmi = null;
        String wantedNumber = String.valueOf(session.sourceId);
        for (TvInputInfo input : session.inputs) {
            if (input == null || input.getType() != TvInputInfo.TYPE_HDMI) {
                continue;
            }
            if (firstHdmi == null) {
                firstHdmi = input;
            }
            String text = inputTitle(session.context, input).toLowerCase();
            if (text.contains(wantedNumber) || text.contains(session.sourceName.toLowerCase())) {
                return input;
            }
        }
        if (firstHdmi != null) {
            append(session.errors, "未精确匹配 " + session.sourceName + "，先使用第一个 HDMI 输入");
            return firstHdmi;
        }
        append(session.errors, "系统 TV 输入列表里没有 HDMI");
        return session.inputs.get(0);
    }

    private static String describeInputs(Session session) {
        if (session.inputs == null || session.inputs.isEmpty()) {
            return "未发现输入";
        }
        StringBuilder builder = new StringBuilder();
        for (TvInputInfo input : session.inputs) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(inputTitle(session.context, input))
                    .append(" / type=")
                    .append(input.getType());
        }
        if (session.selectedInput != null) {
            builder.append("\n选中：").append(inputTitle(session.context, session.selectedInput));
        }
        return builder.toString();
    }

    private static String inputTitle(Context context, TvInputInfo input) {
        if (input == null) {
            return "null";
        }
        CharSequence label = input.loadLabel(context);
        String value = label == null ? "" : label.toString();
        if (value.length() == 0) {
            value = input.getId();
        }
        return value;
    }

    private static void append(StringBuilder errors, String message) {
        if (errors == null) {
            return;
        }
        if (errors.length() > 0) {
            errors.append("; ");
        }
        errors.append(message);
    }

    static final class Session {
        static final int STEP_COUNT = 4;

        final Context context;
        final FrameLayout container;
        final String sourceName;
        final int sourceId;
        final StringBuilder errors = new StringBuilder();

        int step;
        TvInputManager tvInputManager;
        List<TvInputInfo> inputs;
        TvInputInfo selectedInput;
        TvView tvView;
        boolean tuned;

        Session(Context context, FrameLayout container, String sourceName, int sourceId) {
            this.context = context;
            this.container = container;
            this.sourceName = sourceName;
            this.sourceId = sourceId;
        }

        boolean isComplete() {
            return step >= STEP_COUNT;
        }

        String nextStepLabel() {
            switch (step) {
                case 0:
                    return "下一步：获取系统 TvInputManager";
                case 1:
                    return "下一步：扫描系统 TV 输入";
                case 2:
                    return "下一步：创建 Android TvView";
                case 3:
                    return "下一步：tune 到系统 HDMI 输入";
                default:
                    return "诊断已完成";
            }
        }

        String report(String prefix) {
            if (errors.length() == 0) {
                return prefix + "\n当前没有 Java 错误";
            }
            String value = errors.toString();
            if (value.length() > 900) {
                value = value.substring(0, 900) + "...";
            }
            return prefix + "\n" + value;
        }
    }
}
