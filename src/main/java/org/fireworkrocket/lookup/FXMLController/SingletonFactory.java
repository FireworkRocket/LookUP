package org.fireworkrocket.lookup.FXMLController;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;

/**
 * SingletonFactory 类，用于管理单例实例。
 */
public class SingletonFactory {

    private static final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private static final Map<Class<?>, WeakReference<Object>> weakReferenceInstances = new ConcurrentHashMap<>();

    /**
     * 获取指定类的单例实例。
     *
     * @param className 类对象
     * @param <E>       类的类型
     * @return 单例实例
     */
    public static <E> E getInstance(Class<E> className) {
        return getInstance(className, false);
    }

    /**
     * 获取指定类的弱引用单例实例。
     *
     * @param className 类对象
     * @param <E>       类的类型
     * @return 弱引用单例实例
     */
    public static <E> E getWeakInstance(Class<E> className) {
        return getInstance(className, true);
    }

    /**
     * 获取指定类的单例实例。
     *
     * @param className        类对象
     * @param useWeakReference 是否使用弱引用
     * @param <E>              类的类型
     * @return 单例实例
     */
    private static <E> E getInstance(Class<E> className, boolean useWeakReference) {
        Object instance;
        if (useWeakReference) {
            WeakReference<Object> reference = weakReferenceInstances.get(className);
            instance = reference == null ? null : reference.get();
        } else {
            instance = instances.get(className);
        }

        if (instance == null) {
            synchronized (SingletonFactory.class) {
                if (useWeakReference) {
                    WeakReference<Object> reference = weakReferenceInstances.get(className);
                    instance = reference == null ? null : reference.get();
                } else {
                    instance = instances.get(className);
                }

                if (instance == null) {
                    try {
                        instance = className.getDeclaredConstructor().newInstance();
                        if (useWeakReference) {
                            weakReferenceInstances.put(className, new WeakReference<>(instance));
                        } else {
                            instances.put(className, instance);
                        }
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        handleException(e);
                    }
                }
            }
        }
        return className.cast(instance);
    }

    /**
     * 清除指定类的单例实例。
     *
     * @param className 类对象
     */
    public static void clearInstance(Class<?> className) {
        instances.remove(className);
        weakReferenceInstances.remove(className);
    }

    /**
     * 清除所有单例实例。
     */
    public static void clearAllInstances() {
        instances.clear();
        weakReferenceInstances.clear();
    }

    /**
     * 获取所有单例实例。
     *
     * @return 单例实例的映射
     */
    public static Map<Class<?>, Object> getAllInstances() {
        return new ConcurrentHashMap<>(instances);
    }

    /**
     * 获取所有弱引用单例实例。
     *
     * @return 弱引用单例实例的映射
     */
    public static Map<Class<?>, WeakReference<Object>> getAllWeakInstances() {
        return new ConcurrentHashMap<>(weakReferenceInstances);
    }
}