package me.sunstorm.showmanager.injection;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.sunstorm.showmanager.terminable.Terminables;
import me.sunstorm.showmanager.terminable.statics.StaticTerminable;
import me.sunstorm.showmanager.terminable.statics.Termination;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Slf4j
public class DependencyInjection implements StaticTerminable {
    private static final Map<Class<?>, Supplier<?>> providerMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<InjectRecipient>> injectMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    public static <T> void registerProvider(Class<T> type, Supplier<T> provider) {
        log.debug("Registering provider for {}", type.getSimpleName());
        providerMap.put(type, provider);
    }

    public static <T> void updateProvider(Class<T> type, Supplier<T> provider) {
        providerMap.put(type, provider);
        injectMap.get(type).forEach(recipient -> injectSpecific(type, recipient));
    }

    protected static void performInjection(InjectRecipient recipient) {
        val clazz = recipient.getClass();
        if (clazz.isAnnotationPresent(Inject.class)) {
            for (Field field : getCached(clazz)) {
                if (!providerMap.containsKey(field.getType()))
                    continue;
                injectField(field, recipient);
            }
        } else {
            Arrays.stream(getCached(clazz)).filter(f -> f.isAnnotationPresent(Inject.class)).forEach(f -> {
                if (!providerMap.containsKey(f.getType())) {
                    log.error("Found @Inject annotated field ({}#{}) without known provider type {}", clazz.getSimpleName(), f.getName(), f.getType().getSimpleName());
                    return;
                }
                injectField(f, recipient);
            });
        }
    }

    private static void injectSpecific(Class<?> type, InjectRecipient recipient) {
        Arrays.stream(getCached(recipient.getClass())).filter(f -> f.getType().equals(type) && (f.isAnnotationPresent(Inject.class) || recipient.getClass().isAnnotationPresent(Inject.class))).forEach(f -> injectField(f, recipient));
    }

    private static void injectField(Field f, InjectRecipient recipient) {
        try {
            if (providerMap.get(f.getType()) == null) return;
            f.setAccessible(true);
            f.set(recipient, providerMap.get(f.getType()).get());
            injectMap.computeIfAbsent(f.getType(), __ -> new CopyOnWriteArrayList<>()).add(recipient);
        } catch (IllegalAccessException e) {
            log.error("Failed to inject value to field (" + f.getName() + " - " + f.getType().getSimpleName() + ")", e);
        }
    }

    private static Field[] getCached(Class<?> type) {
        if (fieldCache.containsKey(type)) {
            return fieldCache.get(type);
        } else {
            val fields = type.getDeclaredFields();
            fieldCache.put(type, fields);
            return fields;
        }
    }

    @Termination
    public static void shutdownStatic() {
        providerMap.clear();
        injectMap.clear();
    }

    static {
        Terminables.addTerminable(DependencyInjection.class);
    }
}
