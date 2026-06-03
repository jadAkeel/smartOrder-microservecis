package com.jadakeel.common.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public final class KafkaEventUtils {

    private KafkaEventUtils() {
    }

    public static Object payload(Object event) {
        return event instanceof ConsumerRecord<?, ?> record ? record.value() : event;
    }

    public static boolean hasField(Object event, String fieldName) {
        Object payload = payload(event);
        if (payload instanceof Map<?, ?> fields) {
            return fields.containsKey(fieldName);
        }
        if (payload == null) {
            return false;
        }

        return hasAccessor(payload.getClass(), fieldName) || hasDeclaredField(payload.getClass(), fieldName);
    }

    private static boolean hasAccessor(Class<?> type, String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String getterName = "get" + suffix;
        String booleanGetterName = "is" + suffix;

        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0
                    && (method.getName().equals(getterName) || method.getName().equals(booleanGetterName))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasDeclaredField(Class<?> type, String fieldName) {
        Class<?> currentType = type;
        while (currentType != null && currentType != Object.class) {
            for (Field field : currentType.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    return true;
                }
            }
            currentType = currentType.getSuperclass();
        }
        return false;
    }
}
