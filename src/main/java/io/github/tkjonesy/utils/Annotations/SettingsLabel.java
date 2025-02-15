package io.github.tkjonesy.utils.Annotations;

import java.lang.annotation.Retention;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SettingsLabel {
    String value();
    Class<?> type();
}
