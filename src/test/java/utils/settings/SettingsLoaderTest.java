package utils.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tkjonesy.utils.Paths;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.github.tkjonesy.utils.settings.SettingsLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for verifying the functionality of the {@link SettingsLoader} class.
 * <p>
 * These tests ensure that:
 * <ul>
 *   <li>If no settings file exists, a new one is created with the default settings loaded from the resource.</li>
 *   <li>If the settings file is corrupt, the default settings are loaded and saved.</li>
 *   <li>If the model or label files specified in the settings do not exist, they are extracted from resources and the settings are updated accordingly.</li>
 *   <li>If the required AIMs directories do not exist, they are created by {@code initializeAIMsDirectories()}.</li>
 * </ul>
 * <p>
 * Each test method uses the Given-When-Then style to clearly outline the preconditions, the action under test, and the expected outcome.
 * </p>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class SettingsLoaderTest {
    private static Path tempSettingsFile;

    @BeforeAll
    public static void setupPaths() throws Exception {
        Path aimsDir = java.nio.file.Paths.get(System.getProperty("user.home") + "/AIMs");
        Files.createDirectories(aimsDir);
        Files.createDirectories(aimsDir.resolve("ai_models"));
        Files.createDirectories(aimsDir.resolve("sessions"));

        tempSettingsFile = aimsDir.resolve("settings.json");
    }

    /**
     * <b>Given</b> that no settings file exists,
     * <b>when</b> {@link SettingsLoader#loadSettings()} is invoked,
     * <b>then</b> a new settings file is created with the default settings loaded from the resource,
     * and the fileDirectory, modelPath, and labelPath are updated to match the test environment.
     *
     * <p>
     * <b>Pass Condition:</b> The settings file exists and its contents (when adjusted for test paths) match the default settings.
     * <br>
     * <b>Fail Condition:</b> No settings file is created or the loaded settings do not match the expected test values.
     * </p>
     *
     * @throws Exception if an unexpected error occurs during the test
     */
    @Test
    public void givenNoSettingsFile_whenLoadSettings_thenDefaultSettingsCreatedAndSaved() throws Exception {
        // Given: Ensure the settings file does not exist.
        File settingsFile = new File(Paths.AIMS_SETTINGS_FILE_PATH);
        if (settingsFile.exists()) {
            settingsFile.delete();
        }

        // When: loadSettings() is invoked.
        ProgramSettings settings = SettingsLoader.loadSettings();
        settings.setFileDirectory(null);

        // Then: The settings file should now exist.
        assertThat(settingsFile.exists()).isTrue();

        // And: The returned settings should match the default settings loaded from the resource,
        // but with fields updated to match the test-specific paths.
        ObjectMapper mapper = new ObjectMapper();
        ProgramSettings defaultSettings = mapper.readValue(
                SettingsLoader.class.getResourceAsStream(Paths.RESOURCE_DEFAULT_SETTINGS_PATH),
                ProgramSettings.class);
        defaultSettings.setModelPath(Paths.AIMS_MODELS_DIRECTORY+"/yolo11m.onnx");
        defaultSettings.setLabelPath(Paths.AIMS_MODELS_DIRECTORY+"/yolo11m.names");

        // Compare recursively.
        assertThat(settings).usingRecursiveComparison().isEqualTo(defaultSettings);
    }

    /**
     * <b>Given</b> that the settings file is corrupt (e.g. contains an unknown field),
     * <b>when</b> {@link SettingsLoader#loadSettings()} is invoked,
     * <b>then</b> the default settings are loaded from the resource and saved to the settings file.
     *
     * <p>
     * <b>Pass Condition:</b> The returned settings match the default settings.
     * <br>
     * <b>Fail Condition:</b> The corrupt file is used or an exception is thrown.
     * </p>
     *
     * @throws Exception if an unexpected error occurs during the test
     */
    @Test
    public void givenCorruptSettingsFile_whenLoadSettings_thenDefaultSettingsLoadedAndSaved() throws Exception {
        // Given: Create a corrupt settings file.
        tempSettingsFile.toFile();
        Files.writeString(tempSettingsFile, "{ \"unknownField\": \"invalid\" }");

        // When: loadSettings() is invoked.
        ProgramSettings settings = SettingsLoader.loadSettings();
        settings.setFileDirectory(null);

        // Then: The returned settings should match the default settings.
        ObjectMapper mapper = new ObjectMapper();
        ProgramSettings defaultSettings = mapper.readValue(
                SettingsLoader.class.getResourceAsStream(Paths.RESOURCE_DEFAULT_SETTINGS_PATH),
                ProgramSettings.class);
        defaultSettings.setModelPath(Paths.AIMS_MODELS_DIRECTORY+"/yolo11m.onnx");
        defaultSettings.setLabelPath(Paths.AIMS_MODELS_DIRECTORY+"/yolo11m.names");

        assertThat(settings).usingRecursiveComparison().isEqualTo(defaultSettings);
    }

    /**
     * <b>Given</b> that the model and label paths specified in the settings point to non-existent files,
     * <b>when</b> {@link SettingsLoader#loadSettings()} is invoked,
     * <b>then</b> the default model and label resources are extracted and the settings are updated with the new paths.
     *
     * <p>
     * <b>Pass Condition:</b> The files at the model and label paths exist after loading settings.
     * <br>
     * <b>Fail Condition:</b> The files are not created or the settings remain unchanged.
     * </p>
     */
    @Test
    public void givenMissingModelAndLabelFiles_whenLoadSettings_thenResourcesExtractedAndSettingsUpdated() {
        // Given: Create a settings object with non-existent model and label file paths.
        ProgramSettings settings = new ProgramSettings();
        settings.setModelPath("nonexistent.onnx");
        settings.setLabelPath("nonexistent.names");
        SettingsLoader.saveSettings(settings);

        // When: loadSettings() is invoked.
        ProgramSettings updatedSettings = SettingsLoader.loadSettings();

        // Then: The model and label files specified in the settings should exist.
        File modelFile = new File(updatedSettings.getModelPath());
        File labelFile = new File(updatedSettings.getLabelPath());
        assertThat(modelFile.exists()).isTrue();
        assertThat(labelFile.exists()).isTrue();
    }

    /**
     * <b>Given</b> that the required AIMs directories do not exist,
     * <b>when</b> {@link SettingsLoader#initializeAIMsDirectories()} is invoked,
     * <b>then</b> the directories for AIMs, models, and sessions are created.
     *
     * <p>
     * <b>Pass Condition:</b> The AIMs directory, models directory, and sessions directory exist.
     * <br>
     * <b>Fail Condition:</b> Any of the required directories are not created.
     * </p>
     */
    @Test
    public void givenMissingAIMsDirectories_whenInitializeAIMsDirectories_thenDirectoriesAreCreated() {
        // Given: Assume the directories do not exist. Here we simulate by deleting them if they exist.
        File aimsDir = new File(Paths.AIMS_DIRECTORY);
        File modelsDir = new File(Paths.AIMS_MODELS_DIRECTORY);
        File sessionsDir = new File(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
        if (aimsDir.exists()) { aimsDir.delete(); }
        if (modelsDir.exists()) { modelsDir.delete(); }
        if (sessionsDir.exists()) { sessionsDir.delete(); }

        // When: initializeAIMsDirectories() is invoked.
        SettingsLoader.initializeAIMsDirectories();

        // Then: All required directories should exist.
        assertThat(aimsDir.exists()).isTrue();
        assertThat(modelsDir.exists()).isTrue();
        assertThat(sessionsDir.exists()).isTrue();
    }
}
