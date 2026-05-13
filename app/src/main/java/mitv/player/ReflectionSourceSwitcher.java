package mitv.player;

import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ReflectionSourceSwitcher {
    private static final String[] MANAGER_CLASSES = {
            "mitv.tv.TvManager",
            "com.mitv.tv.TvManager",
            "com.xiaomi.mitv.tv.TvManager",
            "com.xiaomi.mitv.tvplayer.InputSourceListManager"
    };

    private static final String[] SWITCH_METHODS = {
            "switchInputSource",
            "setInputSource",
            "setCurrentSource",
            "setSource"
    };

    private static final String[] READ_METHODS = {
            "getCurrentSource",
            "getInputSource",
            "getSource",
            "getSourceName"
    };

    private ReflectionSourceSwitcher() {
    }

    static SwitchResult switchTo(Context context, SourceItem item) {
        StringBuilder errors = new StringBuilder();
        for (String className : MANAGER_CLASSES) {
            Object manager = createManager(context, className, errors);
            if (manager == null) {
                continue;
            }
            for (String methodName : SWITCH_METHODS) {
                if (invokeSwitch(manager, methodName, item.romValue, errors)
                        || invokeSwitch(manager, methodName, item.numericId, errors)) {
                    return SwitchResult.ok("reflection:" + className + "#" + methodName);
                }
            }
        }
        return SwitchResult.fail(errors.length() == 0 ? "no reflection API matched" : errors.toString());
    }

    static SourceItem readCurrent(Context context) {
        for (String className : MANAGER_CLASSES) {
            Object manager = createManager(context, className, null);
            if (manager == null) {
                continue;
            }
            for (String methodName : READ_METHODS) {
                Object value = invokeRead(manager, methodName);
                SourceItem item = SourceCatalog.findCurrent(value == null ? null : String.valueOf(value));
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    private static Object createManager(Context context, String className, StringBuilder errors) {
        try {
            Class<?> clazz = Class.forName(className);
            Object manager = tryStaticFactory(clazz, "getInstance", context);
            if (manager != null) {
                return manager;
            }
            manager = tryStaticFactory(clazz, "getInstance", null);
            if (manager != null) {
                return manager;
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            appendError(errors, className, throwable);
            return null;
        }
    }

    private static Object tryStaticFactory(Class<?> clazz, String methodName, Context context) {
        try {
            Method method = context == null
                    ? clazz.getMethod(methodName)
                    : clazz.getMethod(methodName, Context.class);
            method.setAccessible(true);
            return context == null ? method.invoke(null) : method.invoke(null, context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeSwitch(Object manager, String methodName, String value, StringBuilder errors) {
        try {
            Method method = manager.getClass().getMethod(methodName, String.class);
            method.setAccessible(true);
            method.invoke(manager, value);
            return true;
        } catch (Throwable throwable) {
            appendError(errors, methodName + "(String)", unwrap(throwable));
            return false;
        }
    }

    private static boolean invokeSwitch(Object manager, String methodName, int value, StringBuilder errors) {
        try {
            Method method = manager.getClass().getMethod(methodName, int.class);
            method.setAccessible(true);
            method.invoke(manager, value);
            return true;
        } catch (Throwable throwable) {
            appendError(errors, methodName + "(int)", unwrap(throwable));
            return false;
        }
    }

    private static Object invokeRead(Object manager, String methodName) {
        try {
            Method method = manager.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(manager);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) throwable).getCause();
            return cause == null ? throwable : cause;
        }
        return throwable;
    }

    private static void appendError(StringBuilder errors, String location, Throwable throwable) {
        if (errors == null) {
            return;
        }
        if (errors.length() > 0) {
            errors.append("; ");
        }
        errors.append(location).append(": ").append(throwable.getClass().getSimpleName());
    }
}

