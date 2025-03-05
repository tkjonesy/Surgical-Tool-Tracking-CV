package utils;

import java.lang.reflect.Field;

public class HelperMethods {

    // Helper method to inject private fields using reflection
    public static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
