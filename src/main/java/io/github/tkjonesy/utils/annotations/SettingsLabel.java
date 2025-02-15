package io.github.tkjonesy.utils.annotations;

import java.lang.annotation.Retention;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SettingsLabel {
    String value();
    Class<?> type();
}
