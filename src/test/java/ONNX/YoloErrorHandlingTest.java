package ONNX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtProvider;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.utils.ErrorDialogManager;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import testingUtils.TestingPaths;

import java.util.*;

/**
 * Test class for verifying error handling in the {@link Yolo} class' constructor.
 * <p>
 * This test suite validates that the Yolo class handles errors as expected:
 * <ul>
 *   <li>When a GPU failure occurs during construction (simulated by a thrown OrtException),
 *       the CUDA availability flag is set to false.</li>
 *   <li>When updating the inference session with invalid model paths causes a failure,
 *       an error dialog is displayed via the {@link ErrorDialogManager}.</li>
 * </ul>
 * Naming convention for tests follows the "Given-When-Then" pattern.
 * A passing test indicates that the corresponding error handling behavior is working correctly.
 * A failing test indicates a breakdown in the expected error handling logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class YoloErrorHandlingTest {

    private static final String DUMMY_MODEL_PATH = TestingPaths.DUMMY_MODEL_PATH;
    private static final String DUMMY_LABEL_PATH = TestingPaths.DUMMY_LABEL_PATH;

    private static ProgramSettings dummySettings;

    @BeforeAll
    public static void setupProgramSettings() throws ClassNotFoundException {
        dummySettings = new ProgramSettings();
        dummySettings.setModelPath(DUMMY_MODEL_PATH);
        dummySettings.setLabelPath(DUMMY_LABEL_PATH);
        ProgramSettings.setCurrentSettings(dummySettings);

        Class.forName("io.github.tkjonesy.ONNX.Yolo");
    }

    /**
     * <b>Given</b> that a GPU failure is simulated during Yolo construction (i.e. createSession throws an OrtException),
     * <b>when</b> a Yolo instance is created,
     * <b>then</b> the CUDA availability flag is set to false and an error dialog is displayed.
     *
     * <p>
     * <b>Pass Condition:</b> {@code Yolo.isCudaAvailable()} returns false and
     * {@code ErrorDialogManager.displayErrorDialog(...)} is invoked at least once with a message containing "Simulated GPU failure".
     * <br>
     * <b>Fail Condition:</b> {@code Yolo.isCudaAvailable()} returns true or no error dialog is displayed.
     * </p>
     *
     * @throws Exception if Yolo creation fails unexpectedly
     */
    @Test
    public void givenGpuFailure_whenCreatingYolo_thenCudaFlagIsFalseAndErrorDialogDisplayed() throws Exception {
        OrtEnvironment mockEnv = mock(OrtEnvironment.class);
        dummySettings.setUseGPU(true);

        try (MockedStatic<OrtEnvironment> envStatic = mockStatic(OrtEnvironment.class);
             MockedStatic<ErrorDialogManager> errorDialogStatic = mockStatic(ErrorDialogManager.class)) {

            // Given: OrtEnvironment is mocked to simulate GPU failure.
            envStatic.when(OrtEnvironment::getEnvironment).thenReturn(mockEnv);
            envStatic.when(OrtEnvironment::getAvailableProviders)
                    .thenReturn(EnumSet.of(OrtProvider.CUDA, OrtProvider.CPU));

            when(mockEnv.createSession(eq(DUMMY_MODEL_PATH), any()))
                    .thenThrow(new OrtException("Simulated GPU failure"));

            // When: Creating a Yolo instance
            try {
                new Yolo(DUMMY_MODEL_PATH, DUMMY_LABEL_PATH) {
                    @Override
                    public List<Detection> run(Mat img) {
                        return List.of();
                    }
                };
            } catch (Exception ignore) { }

            // Then: The CUDA flag should be false and an error dialog should be displayed.
            assertThat(Yolo.isCudaAvailable()).isFalse();
            errorDialogStatic.verify(() -> ErrorDialogManager.displayErrorDialog(
                    argThat(message -> message != null && message.contains("Simulated GPU failure"))
            ), times(1));
        }
    }


    /**
     * <b>Given</b> that invalid model and label paths cause the session creation to fail.
     * <b>when</b> createSession is invoked,
     * <b>then</b> an error dialog is displayed via the ErrorDialogManager.
     *
     * <p>
     * <b>Pass Condition:</b> {@code ErrorDialogManager.displayErrorDialog(...)} is called at least once
     * with a message containing "ORT_NO_SUCHFILE".
     * <br>
     * <b>Fail Condition:</b> No error dialog is displayed or the message does not contain the expected text.
     * </p>
     */
    @Test
    public void givenInvalidModelPaths_whenUpdatingInferenceSession_thenErrorDialogIsDisplayed() {
        try (MockedStatic<ErrorDialogManager> errorDialogStatic = mockStatic(ErrorDialogManager.class)) {
            dummySettings.setUseGPU(false);

            // Given: Invalid model and label paths.
            String invalidModelPath = "invalidModel.onnx";
            String invalidLabelPath = "invalidModel.names";

            // When: Creating a Yolo instance (via updateInferenceSession) with invalid model paths.
            try {
                new Yolo(invalidModelPath, invalidLabelPath) {
                    @Override
                    public List<Detection> run(Mat img) {
                        return List.of();
                    }
                };
            } catch (Exception ignore) { }

            // Then: The error dialog should be displayed with a message indicating ORT_NO_SUCHFILE.
            errorDialogStatic.verify(() -> ErrorDialogManager.displayErrorDialog(
                    argThat(message -> message != null && message.contains("ORT_NO_SUCHFILE"))
            ), times(1));
        }
    }
}
