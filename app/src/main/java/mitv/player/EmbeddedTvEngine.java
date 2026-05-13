package mitv.player;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

final class EmbeddedTvEngine {
    private static final String TVPLAYER_PACKAGE = "com.xiaomi.mitv.tvplayer";

    private static final String[] TV_VIEW_MANAGERS = {
            "mitv.tv.TvViewManager",
            "mitv.internal.TvViewManagerDefaultImpl"
    };

    private static final String[] PLAYER_MANAGERS = {
            "mitv.tv.PlayerManager",
            "mitv.internal.PlayerManagerDefaultImpl",
            "mitv.tv.Player",
            "mitv.tv.TvPlayer"
    };

    private static final String[] SOURCE_MANAGERS = {
            "mitv.tv.SourceManager",
            "mitv.internal.SourceManagerDefaultImpl"
    };

    private static final String[] CONTEXT_MANAGERS = {
            "mitv.tv.TvContext",
            "mitv.internal.TvContextDefaultImpl"
    };

    private EmbeddedTvEngine() {
    }

    static String start(Context context, Surface surface, String sourceName, int sourceId) {
        StringBuilder errors = new StringBuilder();
        ClassLoader classLoader = resolveClassLoader(context, errors);
        Object tvContext = createAny(context, classLoader, CONTEXT_MANAGERS, errors);

        Object sourceManager = createAny(context, classLoader, SOURCE_MANAGERS, errors);
        if (sourceManager != null) {
            invokeBestEffort(sourceManager, "switchInputSource", sourceName, errors);
            invokeBestEffort(sourceManager, "switchInputSource", sourceId, errors);
            invokeBestEffort(sourceManager, "setInputSource", sourceName, errors);
            invokeBestEffort(sourceManager, "setInputSource", sourceId, errors);
            invokeBestEffort(sourceManager, "setSource", sourceName, errors);
            invokeBestEffort(sourceManager, "setSource", sourceId, errors);
        }

        Object tvViewManager = createAny(context, classLoader, TV_VIEW_MANAGERS, errors);
        if (tvViewManager != null && preparePlayback(tvViewManager, tvContext, surface, sourceName, sourceId, errors)) {
            return "已尝试内置播放：" + shortName(tvViewManager.getClass().getName());
        }

        Object playerManager = createAny(context, classLoader, PLAYER_MANAGERS, errors);
        if (playerManager != null && preparePlayback(playerManager, tvContext, surface, sourceName, sourceId, errors)) {
            return "已尝试内置播放：" + shortName(playerManager.getClass().getName());
        }

        return "内置播放失败：" + compact(errors.toString());
    }

    static void stop(Context context) {
        ClassLoader classLoader = resolveClassLoader(context, null);
        stopAny(context, classLoader, TV_VIEW_MANAGERS);
        stopAny(context, classLoader, PLAYER_MANAGERS);
    }

    private static ClassLoader resolveClassLoader(Context context, StringBuilder errors) {
        try {
            Context tvPlayerContext = context.createPackageContext(
                    TVPLAYER_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            append(errors, "已加载模拟电视代码");
            return tvPlayerContext.getClassLoader();
        } catch (PackageManager.NameNotFoundException exception) {
            append(errors, "未找到模拟电视包");
            return EmbeddedTvEngine.class.getClassLoader();
        } catch (Throwable throwable) {
            append(errors, "加载模拟电视失败:" + throwable.getClass().getSimpleName());
            return EmbeddedTvEngine.class.getClassLoader();
        }
    }

    private static boolean preparePlayback(
            Object manager,
            Object tvContext,
            Surface surface,
            String sourceName,
            int sourceId,
            StringBuilder errors
    ) {
        boolean bound = bindSurface(manager, surface, errors);
        boolean invoked = false;
        if (tvContext != null) {
            invoked |= invokeAnyContextMethod(manager, tvContext, errors);
        }
        invoked |= invokeBestEffort(manager, "setInputSource", sourceName, errors);
        invoked |= invokeBestEffort(manager, "setInputSource", sourceId, errors);
        invoked |= invokeBestEffort(manager, "setSource", sourceName, errors);
        invoked |= invokeBestEffort(manager, "setSource", sourceId, errors);
        invoked |= invokeBestEffort(manager, "start", errors);
        invoked |= invokeBestEffort(manager, "play", errors);
        invoked |= invokeBestEffort(manager, "resume", errors);
        invoked |= invokeBestEffort(manager, "open", errors);
        invoked |= invokeBestEffort(manager, "show", errors);
        return bound && invoked;
    }

    private static void stopAny(Context context, ClassLoader classLoader, String[] classNames) {
        Object manager = createAny(context, classLoader, classNames, null);
        if (manager != null) {
            invokeBestEffort(manager, "stop", null);
            invokeBestEffort(manager, "pause", null);
            invokeBestEffort(manager, "release", null);
        }
    }

    private static Object createAny(Context context, ClassLoader classLoader, String[] classNames, StringBuilder errors) {
        for (String className : classNames) {
            Object instance = create(context, classLoader, className, errors);
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    private static Object create(Context context, ClassLoader classLoader, String className, StringBuilder errors) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            Object instance = tryFactory(clazz, "getInstance", context);
            if (instance != null) {
                return instance;
            }
            instance = tryFactory(clazz, "getInstance", null);
            if (instance != null) {
                return instance;
            }
            instance = tryConstructor(clazz, context);
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

    private static Object tryConstructor(Class<?> clazz, Context context) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean bindSurface(Object manager, Surface surface, StringBuilder errors) {
        return invokeBestEffort(manager, "setSurface", surface, errors)
                || invokeBestEffort(manager, "setDisplay", surface, errors)
                || invokeBestEffort(manager, "setVideoSurface", surface, errors)
                || invokeBestEffort(manager, "setPreviewSurface", surface, errors)
                || invokeBestEffort(manager, "setMainSurface", surface, errors);
    }

    private static boolean invokeAnyContextMethod(Object target, Object tvContext, StringBuilder errors) {
        boolean invoked = false;
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].isInstance(tvContext)) {
                String name = method.getName().toLowerCase();
                if (name.contains("context") || name.contains("init") || name.contains("attach")) {
                    try {
                        method.setAccessible(true);
                        method.invoke(target, tvContext);
                        invoked = true;
                    } catch (Throwable throwable) {
                        append(errors, method.getName() + "(TvContext):" + throwable.getClass().getSimpleName());
                    }
                }
            }
        }
        return invoked;
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

    private static String shortName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static String compact(String value) {
        if (value == null || value.length() == 0) {
            return "未找到可用播放接口";
        }
        return value.length() > 900 ? value.substring(0, 900) + "..." : value;
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
