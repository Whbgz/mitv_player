package mitv.player;

import android.content.Context;
import android.view.Surface;

import java.lang.reflect.Method;

final class EmbeddedTvEngine {
    private static final String[] TV_VIEW_MANAGERS = {
            "mitv.tv.TvViewManager",
            "com.xiaomi.mitv.tv.TvViewManager"
    };

    private static final String[] PLAYER_MANAGERS = {
            "mitv.tv.PlayerManager",
            "com.xiaomi.mitv.tv.PlayerManager"
    };

    private static final String[] SOURCE_MANAGERS = {
            "mitv.tv.SourceManager",
            "com.xiaomi.mitv.tv.SourceManager"
    };

    private EmbeddedTvEngine() {
    }

    static String start(Context context, Surface surface, String sourceName, int sourceId) {
        StringBuilder errors = new StringBuilder();

        Object sourceManager = createAny(context, SOURCE_MANAGERS, errors);
        if (sourceManager != null) {
            invokeBestEffort(sourceManager, "switchInputSource", sourceName, errors);
            invokeBestEffort(sourceManager, "switchInputSource", sourceId, errors);
            invokeBestEffort(sourceManager, "setInputSource", sourceName, errors);
            invokeBestEffort(sourceManager, "setInputSource", sourceId, errors);
        }

        Object tvViewManager = createAny(context, TV_VIEW_MANAGERS, errors);
        if (tvViewManager != null && bindSurface(tvViewManager, surface, errors)) {
            invokeBestEffort(tvViewManager, "start", errors);
            invokeBestEffort(tvViewManager, "play", errors);
            return "已尝试绑定 TvViewManager Surface";
        }

        Object playerManager = createAny(context, PLAYER_MANAGERS, errors);
        if (playerManager != null && bindSurface(playerManager, surface, errors)) {
            invokeBestEffort(playerManager, "start", errors);
            invokeBestEffort(playerManager, "play", errors);
            return "已尝试绑定 PlayerManager Surface";
        }

        return "内置播放失败：" + (errors.length() == 0 ? "未找到可用 mitv.tv 私有接口" : errors.toString());
    }

    static void stop(Context context) {
        Object manager = createAny(context, TV_VIEW_MANAGERS, null);
        if (manager != null) {
            invokeBestEffort(manager, "stop", null);
            invokeBestEffort(manager, "release", null);
        }
        manager = createAny(context, PLAYER_MANAGERS, null);
        if (manager != null) {
            invokeBestEffort(manager, "stop", null);
            invokeBestEffort(manager, "release", null);
        }
    }

    private static Object createAny(Context context, String[] classNames, StringBuilder errors) {
        for (String className : classNames) {
            Object instance = create(context, className, errors);
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    private static Object create(Context context, String className, StringBuilder errors) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = tryFactory(clazz, "getInstance", context);
            if (instance != null) {
                return instance;
            }
            instance = tryFactory(clazz, "getInstance", null);
            if (instance != null) {
                return instance;
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            append(errors, className + ":" + throwable.getClass().getSimpleName());
            return null;
        }
    }

    private static Object tryFactory(Class<?> clazz, String methodName, Context context) {
        try {
            Method method = context == null ? clazz.getMethod(methodName) : clazz.getMethod(methodName, Context.class);
            method.setAccessible(true);
            return context == null ? method.invoke(null) : method.invoke(null, context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean bindSurface(Object manager, Surface surface, StringBuilder errors) {
        return invokeBestEffort(manager, "setSurface", surface, errors)
                || invokeBestEffort(manager, "setDisplay", surface, errors)
                || invokeBestEffort(manager, "setVideoSurface", surface, errors)
                || invokeBestEffort(manager, "setPreviewSurface", surface, errors);
    }

    private static boolean invokeBestEffort(Object target, String methodName, String value, StringBuilder errors) {
        try {
            Method method = target.getClass().getMethod(methodName, String.class);
            method.setAccessible(true);
            method.invoke(target, value);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "(String):" + throwable.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean invokeBestEffort(Object target, String methodName, int value, StringBuilder errors) {
        try {
            Method method = target.getClass().getMethod(methodName, int.class);
            method.setAccessible(true);
            method.invoke(target, value);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "(int):" + throwable.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean invokeBestEffort(Object target, String methodName, Surface surface, StringBuilder errors) {
        try {
            Method method = target.getClass().getMethod(methodName, Surface.class);
            method.setAccessible(true);
            method.invoke(target, surface);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "(Surface):" + throwable.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean invokeBestEffort(Object target, String methodName, StringBuilder errors) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "():" + throwable.getClass().getSimpleName());
            return false;
        }
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
}
