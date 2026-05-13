package mitv.player;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class EmbeddedTvEngine {
    private static final String TVPLAYER_PACKAGE = "com.xiaomi.mitv.tvplayer";
    private static final String TV_CONTEXT = "mitv.tv.TvContext";
    private static final String TV_SURFACE_VIEW_PARENT = "com.xiaomi.mitv.tvplayer.widget.views.TVSurfaceViewParent";

    private static Object activeTvView;
    private static Object activeTvViewManager;

    private EmbeddedTvEngine() {
    }

    static String start(Context context, FrameLayout container, String sourceName, int sourceId) {
        StringBuilder errors = new StringBuilder();
        ClassLoader classLoader = resolveClassLoader(context, errors);

        Object tvContext = getTvContext(classLoader, errors);
        Object sourceManager = invokeObject(tvContext, "getSourceManager", errors);
        Object tvViewManager = invokeObject(tvContext, "getTvViewManager", errors);
        Object playerManager = invokeObject(tvContext, "getPlayerManager", errors);
        Object tvPlayer = invokeObject(playerManager, "createTvPlayer", errors);
        Object tvView = createTvSurfaceView(context, classLoader, errors);

        if (tvView instanceof View) {
            View view = (View) tvView;
            container.addView(view, 0, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            invokeNoArg(tvView, "init", errors);
            invokeBoolean(tvView, "enableBlackCover", false, errors);
        }

        boolean registered = false;
        if (tvViewManager != null && tvView instanceof View) {
            registered = invokeViewObject(tvViewManager, "registerMainTvView", (View) tvView, tvPlayer, errors);
            if (!registered) {
                registered = invokeViewObject(tvViewManager, "registerMainTvView", (View) tvView, null, errors);
            }
        }

        boolean switched = false;
        if (sourceManager != null) {
            switched = invokeInt(sourceManager, "setCurrentSource", sourceId, errors);
        }

        activeTvView = tvView;
        activeTvViewManager = tvViewManager;

        if (registered && switched) {
            return "已调用内置播放：" + sourceName + " / TvPlayer / setCurrentSource(" + sourceId + ")";
        }
        if (needsSecureSettingsGrant(errors)) {
            return "缺少 WRITE_SECURE_SETTINGS 权限，请执行：adb shell pm grant mitv.player android.permission.WRITE_SECURE_SETTINGS，然后重启应用";
        }
        if (registered) {
            return "已注册画面，但切源失败：" + compact(errors.toString());
        }
        if (switched) {
            return "已切换信号源，但注册画面失败：" + compact(errors.toString());
        }
        return "内置播放失败：" + compact(errors.toString());
    }

    static void stop(Context context) {
        if (activeTvViewManager != null && activeTvView instanceof View) {
            invokeView(activeTvViewManager, "unregisterTvView", (View) activeTvView, null);
        }
        if (activeTvView != null) {
            invokeNoArg(activeTvView, "clear", null);
        }
        activeTvView = null;
        activeTvViewManager = null;
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
            append(errors, "加载模拟电视失败:" + describe(throwable));
            return EmbeddedTvEngine.class.getClassLoader();
        }
    }

    private static Object getTvContext(ClassLoader classLoader, StringBuilder errors) {
        try {
            Class<?> clazz = Class.forName(TV_CONTEXT, true, classLoader);
            Method method = clazz.getMethod("getInstance");
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Throwable throwable) {
            append(errors, "TvContext.getInstance:" + describe(throwable));
            return null;
        }
    }

    private static Object createTvSurfaceView(Context context, ClassLoader classLoader, StringBuilder errors) {
        try {
            Class<?> clazz = Class.forName(TV_SURFACE_VIEW_PARENT, true, classLoader);
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context);
        } catch (Throwable throwable) {
            append(errors, "TVSurfaceViewParent:" + describe(throwable));
            return null;
        }
    }

    private static Object invokeObject(Object target, String methodName, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable throwable) {
            append(errors, methodName + "():" + describe(throwable));
            return null;
        }
    }

    private static boolean invokeInt(Object target, String methodName, int value, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, int.class);
            method.setAccessible(true);
            Object result = method.invoke(target, value);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable throwable) {
            append(errors, methodName + "(int):" + describe(throwable));
            return false;
        }
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean value, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.setAccessible(true);
            method.invoke(target, value);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "(boolean):" + describe(throwable));
            return false;
        }
    }

    private static boolean invokeViewObject(Object target, String methodName, View view, Object value, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, View.class, Object.class);
            method.setAccessible(true);
            Object result = method.invoke(target, view, value);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable throwable) {
            append(errors, methodName + "(View,Object):" + describe(throwable));
            return false;
        }
    }

    private static boolean invokeView(Object target, String methodName, View view, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, View.class);
            method.setAccessible(true);
            Object result = method.invoke(target, view);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable throwable) {
            append(errors, methodName + "(View):" + describe(throwable));
            return false;
        }
    }

    private static boolean invokeNoArg(Object target, String methodName, StringBuilder errors) {
        if (target == null) {
            append(errors, methodName + ":target=null");
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
            return true;
        } catch (Throwable throwable) {
            append(errors, methodName + "():" + describe(throwable));
            return false;
        }
    }

    private static String describe(Throwable throwable) {
        Throwable real = throwable;
        if (throwable instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) throwable).getCause();
            if (cause != null) {
                real = cause;
            }
        }
        String message = real.getMessage();
        if (message == null || message.length() == 0) {
            return real.getClass().getSimpleName();
        }
        return real.getClass().getSimpleName() + "(" + message + ")";
    }

    private static String compact(String value) {
        if (value == null || value.length() == 0) {
            return "未返回错误信息";
        }
        return value.length() > 900 ? value.substring(0, 900) + "..." : value;
    }

    private static boolean needsSecureSettingsGrant(StringBuilder errors) {
        return errors != null && errors.toString().contains("WRITE_SECURE_SETTINGS");
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
