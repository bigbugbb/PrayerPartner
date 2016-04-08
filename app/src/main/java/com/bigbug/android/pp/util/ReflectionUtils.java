package com.bigbug.android.pp.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {
    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private ReflectionUtils()
    {
        throw new UnsupportedOperationException("This class is non-instantiable");
    }

    /**
     * Use reflection to invoke a static method for a class object and method name
     *
     * @param <T>         Type that the method should return
     * @param classObject Class on which to invoke {@code methodName}. Cannot be null.
     * @param methodName  Name of the method to invoke. Cannot be null.
     * @param types       explicit types for the objects. This is useful if the types are primitives, rather than objects.
     * @param args        arguments for the method. May be null if the method takes no arguments.
     * @return The result of invoking the named method on the given class for the args
     * @throws RuntimeException if the class or method doesn't exist
     */
    @SuppressWarnings("unchecked")
    static public <T> T tryInvokeStatic(final Class<?> classObject, final String methodName, final Class<?>[] types, final Object[] args)
    {
        return (T) helper(null, classObject, null, methodName, types, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T helper(final Object target, final Class<?> classObject, final String className, final String methodName, final Class<?>[] argTypes, final Object[] args)
    {
        try
        {
            Class<?> cls;
            if (classObject != null)
            {
                cls = classObject;
            }
            else if (target != null)
            {
                cls = target.getClass();
            }
            else
            {
                cls = Class.forName(className);
            }

            return (T) cls.getMethod(methodName, argTypes).invoke(target, args);
        }
        catch (final NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (final IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (final InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        catch (final ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use reflection to invoke a static method for a class object and method name
     *
     * @param <T>        Type that the method should return
     * @param className  Name of the class on which to invoke {@code methodName}. Cannot be null.
     * @param methodName Name of the method to invoke. Cannot be null.
     * @param types      explicit types for the objects. This is useful if the types are primitives, rather than objects.
     * @param args       arguments for the method. May be null if the method takes no arguments.
     * @return The result of invoking the named method on the given class for the args
     * @throws RuntimeException if the class or method doesn't exist
     */
    @SuppressWarnings("unchecked")
    static public <T> T tryInvokeStatic(final String className, final String methodName, final Class<?>[] types, final Object[] args)
    {
        return (T) helper(null, null, className, methodName, types, args);
    }

    /**
     * Use reflection to invoke a static method for a class object and method name
     *
     * @param <T>        Type that the method should return
     * @param target     Object instance on which to invoke {@code methodName}. Cannot be null.
     * @param methodName Name of the method to invoke. Cannot be null.
     * @param types      explicit types for the objects. This is useful if the types are primitives, rather than objects.
     * @param args       arguments for the method. May be null if the method takes no arguments.
     * @return The result of invoking the named method on the given class for the args
     * @throws RuntimeException if the class or method doesn't exist
     */
    @SuppressWarnings("unchecked")
    static <T> T tryInvokeInstance(final Object target, final String methodName, final Class<?>[] types, final Object[] args)
    {
        return (T) helper(target, null, null, methodName, types, args);
    }

    /**
     * Use reflection to invoke a constructor for a class name
     *
     * @param <T>       Type of object that the constructor should return
     * @param className Name of the class to construct. Cannot be null.
     * @param types     explicit types for the constructor. This is useful if the types are primitives, rather than objects.
     * @param args      arguments for the constructor. May be null if the method takes no arguments.
     * @return New instance of the given class with args
     * @throws RuntimeException if the class or method doesn't exist
     */
    @SuppressWarnings("unchecked")
    static <T> T tryInvokeConstructor(final String className, final Class<?>[] types, final Object[] args)
    {
        try
        {
            return (T) Class.forName(className).getDeclaredConstructor(types).newInstance(args);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use reflection to get a static field for a class name
     *
     * @param className Name of the class to construct. Cannot be null.
     * @param fieldName Name of the field to get. Cannot be null.
     * @return The result of getting the the named field on the given class
     * @throws RuntimeException if the class or field doesn't exists or if the field is private
     */
    public static Object tryGetStaticField(final String className, final String fieldName)
    {
        try
        {
            Class<?> cls = Class.forName(className);
            Field field = cls.getField(fieldName);
            return field.get(null);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Object tryInvoke(Object target, String methodName, Object... args) {
        Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i].getClass();
        }

        return tryInvoke(target, methodName, argTypes, args);
    }

    public static Object tryInvoke(Object target, String methodName, Class<?>[] argTypes,
                                   Object... args) {
        try {
            return target.getClass().getMethod(methodName, argTypes).invoke(target, args);
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (InvocationTargetException ignored) {
        }

        return null;
    }
}