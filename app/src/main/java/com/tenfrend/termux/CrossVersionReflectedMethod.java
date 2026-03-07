package com.tenfrend.termux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class CrossVersionReflectedMethod {

    private final Class<?> mClass;
    private Method mMethod = null;
    private Object[] mDefaultArgs;
    private HashMap<String, Integer> mArgNamesToIndexes;

    public CrossVersionReflectedMethod(Class<?> aClass) {
        mClass = aClass;
    }

    public CrossVersionReflectedMethod tryMethodVariant(String methodName, Object... typesNamesAndDefaults) {
        if (mMethod != null) return this;
        try {
            int argCount = typesNamesAndDefaults.length / 3;
            Class<?>[] refArguments = new Class<?>[argCount];
            for (int i = 0; i < argCount; i++) {
                Object refArgument = typesNamesAndDefaults[i * 3];
                if (refArgument instanceof Class) {
                    refArguments[i] = (Class<?>) refArgument;
                } else {
                    refArguments[i] = Class.forName((String) refArgument);
                }
            }
            mMethod = mClass.getMethod(methodName, (Class<?>[]) refArguments);
            mArgNamesToIndexes = new HashMap<>();
            mDefaultArgs = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                mArgNamesToIndexes.put((String) typesNamesAndDefaults[i * 3 + 1], i);
                mDefaultArgs[i] = typesNamesAndDefaults[i * 3 + 2];
            }
        } catch (NoSuchMethodException | ClassNotFoundException ignored) {}
        return this;
    }

    public CrossVersionReflectedMethod tryMethodVariantInexact(String methodName, Object... typesNamesAndDefaults) {
        if (mMethod != null) return this;

        int expectedArgCount = typesNamesAndDefaults.length / 3;

        for (Method method : mClass.getMethods()) {
            if (!methodName.equals(method.getName())) continue;

            try {
                Class<?> expectedArgumentClass = null;
                int expectedArgumentI = 0;
                int actualArgumentI = 0;
                HashMap<String, Integer> argNamesToIndexes = new HashMap<>();
                Object[] defaultArgs = new Object[method.getParameterTypes().length];

                for (Class<?> methodParam : method.getParameterTypes()) {
                    if (expectedArgumentClass == null && expectedArgumentI < expectedArgCount) {
                        Object refArgument = typesNamesAndDefaults[expectedArgumentI * 3];
                        if (refArgument instanceof Class) {
                            expectedArgumentClass = (Class<?>) refArgument;
                        } else {
                            expectedArgumentClass = Class.forName((String) refArgument);
                        }
                    }

                    if (methodParam == expectedArgumentClass) {
                        argNamesToIndexes.put((String) typesNamesAndDefaults[expectedArgumentI * 3 + 1], actualArgumentI);
                        defaultArgs[actualArgumentI] = typesNamesAndDefaults[expectedArgumentI * 3 + 2];
                        expectedArgumentI++;
                        expectedArgumentClass = null;
                    } else {
                        defaultArgs[actualArgumentI] = getDefaultValueForPrimitiveClass(methodParam);
                    }
                    actualArgumentI++;
                }

                if (expectedArgumentI != expectedArgCount) continue;

                mMethod = method;
                mDefaultArgs = defaultArgs;
                mArgNamesToIndexes = argNamesToIndexes;
            } catch (ClassNotFoundException ignored) {}
        }
        return this;
    }

    public Object invoke(Object receiver, Object... namesAndValues) throws InvocationTargetException {
        if (mMethod == null) {
            throw new RuntimeException("Couldn't find method with matching signature");
        }
        Object[] args = mDefaultArgs.clone();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Integer namedArgIndex = mArgNamesToIndexes.get(namesAndValues[i]);
            if (namedArgIndex != null) {
                args[namedArgIndex] = namesAndValues[i + 1];
            }
        }
        try {
            return mMethod.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getDefaultValueForPrimitiveClass(Class<?> aClass) {
        if (aClass == Boolean.TYPE) return false;
        if (aClass == Byte.TYPE) return (byte) 0;
        if (aClass == Character.TYPE) return '\0';
        if (aClass == Short.TYPE) return (short) 0;
        if (aClass == Integer.TYPE) return 0;
        if (aClass == Long.TYPE) return 0L;
        if (aClass == Float.TYPE) return 0f;
        if (aClass == Double.TYPE) return 0.0;
        return null;
    }

    public boolean isFound() {
        return mMethod != null;
    }
}