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
                session.classLoader = resolveClassLoader(session.context, session.errors);
                session.step++;
                return session.report("已完成：加载模拟电视代码");
            case 1:
                session.tvContext = getTvContext(session.classLoader, session.errors);
                session.step++;
                return session.report("已完成：TvContext.getInstance");
            case 2:
                session.sourceManager = invokeObject(session.tvContext, "getSourceManager", session.errors);
                session.step++;
                return session.report("已完成：getSourceManager");
            case 3:
                session.tvViewManager = invokeObject(session.tvContext, "getTvViewManager", session.errors);
                session.step++;
                return session.report("已完成：getTvViewManager");
            case 4:
                session.playerManager = invokeObject(session.tvContext, "getPlayerManager", session.errors);
                session.step++;
                return session.report("已完成：getPlayerManager");
            case 5:
                session.tvPlayer = invokeObject(session.playerManager, "createTvPlayer", session.errors);
                session.step++;
                return session.report("已完成：createTvPlayer");
            case 6:
                session.tvView = createTvSurfaceView(session.context, session.classLoader, session.errors);
                if (session.tvView instanceof View) {
                    View view = (View) session.tvView;
                    session.container.addView(view, 0, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    invokeNoArg(session.tvView, "init", session.errors);
                    invokeBoolean(session.tvView, "enableBlackCover", false, session.errors);
                }
                session.step++;
                return session.report("已完成：创建 TVSurfaceViewParent");
            case 7:
                if (session.tvViewManager != null && session.tvView instanceof View) {
                    session.registered = invokeViewObject(
                            session.tvViewManager,
                            "registerMainTvView",
                            (View) session.tvView,
                            session.tvPlayer,
                            session.errors
                    );
                    if (!session.registered) {
                        session.registered = invokeViewObject(
                                session.tvViewManager,
                                "registerMainTvView",
                                (View) session.tvView,
                                null,
                                session.errors
                        );
                    }
                } else {
                    append(session.errors, "registerMainTvView:manager/view=null");
                }
                session.step++;
                return session.report("已完成：registerMainTvView");
            case 8:
                if (session.sourceManager != null) {
                    session.switched = invokeInt(session.sourceManager, "setCurrentSource", session.sourceId, session.errors);
                } else {
                    append(session.errors, "setCurrentSource:sourceManager=null");
                }
                session.step++;
                if (session.registered && session.switched) {
                    return "已调用内置播放：" + session.sourceName + " / registerMainTvView / setCurrentSource(" + session.sourceId + ")";
                }
                if (needsSecureSettingsGrant(session.errors)) {
                    return "缺少 WRITE_SECURE_SETTINGS 权限，请执行：adb shell pm grant mitv.player android.permission.WRITE_SECURE_SETTINGS";
                }
                return session.report("诊断完成，但未全部成功");
            default:
                session.step = Session.STEP_COUNT;
                return "诊断已完成";
        }
    }

    static void stop(Session session) {
        if (session == null) {
            return;
        }
        if (session.tvViewManager != null && session.tvView instanceof View) {
            invokeView(session.tvViewManager, "unregisterTvView", (View) session.tvView, null);
        }
        if (session.tvView != null) {
            invokeNoArg(session.tvView, "clear", null);
        }
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

    static final class Session {
        static final int STEP_COUNT = 9;

        final Context context;
        final FrameLayout container;
        final String sourceName;
        final int sourceId;
        final StringBuilder errors = new StringBuilder();

        int step;
        ClassLoader classLoader;
        Object tvContext;
        Object sourceManager;
        Object tvViewManager;
        Object playerManager;
        Object tvPlayer;
        Object tvView;
        boolean registered;
        boolean switched;

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
                    return "下一步：加载模拟电视代码";
                case 1:
                    return "下一步：TvContext.getInstance";
                case 2:
                    return "下一步：getSourceManager";
                case 3:
                    return "下一步：getTvViewManager";
                case 4:
                    return "下一步：getPlayerManager";
                case 5:
                    return "下一步：createTvPlayer";
                case 6:
                    return "下一步：创建 TVSurfaceViewParent";
                case 7:
                    return "下一步：registerMainTvView";
                case 8:
                    return "下一步：setCurrentSource(" + sourceId + ")";
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
