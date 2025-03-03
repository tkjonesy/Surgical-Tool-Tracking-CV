package io.github.tkjonesy.utils;

public class Paths {

    public static final String AIMS_DIRECTORY = System.getProperty("user.home") + "/AIMs";
    public static final String AIMS_MODELS_DIRECTORY = AIMS_DIRECTORY + "/ai_models";
    public static final String DEFAULT_AIMS_SESSIONS_DIRECTORY = AIMS_DIRECTORY + "/sessions";
    public static String AIMS_SESSIONS_DIRECTORY = AIMS_DIRECTORY + "/sessions";
    public static final String AIMS_SETTINGS_FILE_PATH = AIMS_DIRECTORY + "/settings.json";

    public static final String RESOURCE_DEFAULT_SETTINGS_PATH = "/defaultSettings.json";
    public static final String RESOURCE_DEFAULT_MODEL_PATH = "/ai_models/yolo11m.onnx";
    public static final String RESOURCE_DEFAULT_LABELS_PATH = "/ai_models/yolo11m.names";

    public static final String LOGO_PATH = "/images/logo.png";
    public static final String LOGO16_PATH = "/images/logo16.png";
    public static final String LOGO32_PATH = "/images/logo32.png";
    public static final String LOGO64_PATH = "/images/logo64.png";
}
