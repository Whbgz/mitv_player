package mitv.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

final class EmbeddedTvEngine {
    interface StatusListener {
        void onStatus(String message, boolean videoMaybeVisible);
    }

    private EmbeddedTvEngine() {
    }

    static Session createSession(
            Context context,
            FrameLayout container,
            String sourceName,
            int sourceId,
            StatusListener listener
    ) {
        return new Session(context, container, sourceName, sourceId, listener);
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
                session.tvView.setVisibility(View.VISIBLE);
                session.tvView.setStreamVolume(1.0f);
                session.tvView.setCallback(new TvView.TvInputCallback() {
                    @Override
                    public void onConnectionFailed(String inputId) {
                        session.notifyStatus("TvView 回调：连接失败\n" + inputId, false);
                    }

                    @Override
                    public void onDisconnected(String inputId) {
                        session.notifyStatus("TvView 回调：已断开\n" + inputId, false);
                    }

                    @Override
                    public void onChannelRetuned(String inputId, Uri channelUri) {
                        session.notifyStatus("TvView 回调：已调谐\n" + inputId + "\n" + channelUri, true);
                    }

                    @Override
                    public void onTracksChanged(String inputId, List<TvTrackInfo> tracks) {
                        session.audioTrackCount = countTracks(tracks, TvTrackInfo.TYPE_AUDIO);
                        session.videoTrackCount = countTracks(tracks, TvTrackInfo.TYPE_VIDEO);
                        session.notifyStatus(
                                "TvView 回调：音频轨道 " + session.audioTrackCount
                                        + "，视频轨道 " + session.videoTrackCount,
                                session.videoTrackCount > 0
                        );
                    }

                    @Override
                    public void onTrackSelected(String inputId, int type, String trackId) {
                        session.notifyStatus("TvView 回调：选择轨道 type=" + type + " id=" + trackId, true);
                    }

                    @Override
                    public void onVideoAvailable(String inputId) {
                        session.notifyStatus("TvView 回调：视频可用\n" + inputId, true);
                    }

                    @Override
                    public void onVideoUnavailable(String inputId, int reason) {
                        session.notifyStatus("TvView 回调：视频不可用 reason=" + reason + "\n" + inputId, false);
                    }

                    @Override
                    public void onContentAllowed(String inputId) {
                        session.notifyStatus("TvView 回调：内容允许\n" + inputId, true);
                    }

                    @Override
                    public void onContentBlocked(String inputId, TvContentRating rating) {
                        session.notifyStatus("TvView 回调：内容被阻止\n" + inputId + "\n" + rating, false);
                    }
                });
                session.container.addView(session.tvView, 0, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
                session.step++;
                return session.report("已完成：创建 Android TvView");
            case 3:
                requestAudioFocus(session);
                if (session.tvView != null && session.selectedInput != null) {
                    Uri uri = TvContract.buildChannelUriForPassthroughInput(session.selectedInput.getId());
                    session.tvView.setStreamVolume(1.0f);
                    session.tvView.tune(session.selectedInput.getId(), uri);
                    session.tuned = true;
                } else {
                    append(session.errors, "tune:TvView/input=null");
                }
                session.step++;
                if (session.tuned) {
                    return "已调用系统 TvView 播放：" + inputTitle(session.context, session.selectedInput)
                            + "\n已请求音频焦点，TvView 音量=100%\n等待回调";
                }
                return session.report("系统 TvView 播放失败");
            default:
                session.step = Session.STEP_COUNT;
                return "诊断已完成";
        }
    }

    static void stop(Session session) {
        if (session != null) {
            if (session.tvView != null) {
                session.tvView.reset();
            }
            abandonAudioFocus(session);
        }
    }

    private static void requestAudioFocus(Session session) {
        session.audioManager = (AudioManager) session.context.getSystemService(Context.AUDIO_SERVICE);
        if (session.audioManager == null) {
            append(session.errors, "AudioManager=null");
            return;
        }
        try {
            int result = session.audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
            append(session.errors, "requestAudioFocus=" + result);
        } catch (Throwable throwable) {
            append(session.errors, "requestAudioFocus:" + throwable.getClass().getSimpleName());
        }
    }

    private static void abandonAudioFocus(Session session) {
        if (session.audioManager == null) {
            return;
        }
        session.audioManager.abandonAudioFocus(null);
    }

    private static int countTracks(List<TvTrackInfo> tracks, int type) {
        if (tracks == null) {
            return 0;
        }
        int count = 0;
        for (TvTrackInfo track : tracks) {
            if (track != null && track.getType() == type) {
                count++;
            }
        }
        return count;
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
        final StatusListener listener;
        final StringBuilder errors = new StringBuilder();

        int step;
        TvInputManager tvInputManager;
        List<TvInputInfo> inputs;
        TvInputInfo selectedInput;
        TvView tvView;
        AudioManager audioManager;
        int audioTrackCount;
        int videoTrackCount;
        boolean tuned;

        Session(
                Context context,
                FrameLayout container,
                String sourceName,
                int sourceId,
                StatusListener listener
        ) {
            this.context = context;
            this.container = container;
            this.sourceName = sourceName;
            this.sourceId = sourceId;
            this.listener = listener;
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

        void notifyStatus(String message, boolean videoMaybeVisible) {
            if (listener != null) {
                listener.onStatus(message, videoMaybeVisible);
            }
        }
    }
}
