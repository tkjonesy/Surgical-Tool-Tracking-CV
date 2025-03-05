package ONNX.models;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static _testingUtils.HelperMethods.setPrivateField;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.utils.ErrorDialogManager;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import _testingUtils.TestingPaths;

import java.lang.reflect.Field;

/**
 * Test class for verifying error handling in the {@link OnnxRunner} class.
 * <p>
 * These tests ensure that the OnnxRunner:
 * <ul>
 *   <li>Handles inference failures (OrtException) gracefully by logging the error and returning an empty output.</li>
 *   <li>Resets its inference session when valid model paths are provided.</li>
 *   <li>Displays an error dialog when updating the inference session fails due to invalid model paths.</li>
 * </ul>
 * <p>
 * Naming convention for tests follows the "Given-When-Then" pattern.
 * A passing test indicates that the corresponding error handling behavior is working correctly.
 * A failing test indicates a breakdown in the expected error handling logic.
 */
@ExtendWith(MockitoExtension.class)
public class OnnxRunnerErrorHandlingTest {

    private OnnxRunner onnxRunner;

    @Mock
    private Yolo mockInferenceSession;

    @Mock
    private LogQueue logQueue;

    private final Mat dummyFrame = new Mat(10, 10, 0);

    @BeforeAll
    public static void setupProgramSettings() {
        ProgramSettings dummySettings = new ProgramSettings();
        dummySettings.setModelPath(TestingPaths.DUMMY_MODEL_PATH);
        dummySettings.setLabelPath(TestingPaths.DUMMY_LABEL_PATH);
        ProgramSettings.setCurrentSettings(dummySettings);
    }

    @BeforeEach
    public void setup() throws Exception {
        logQueue = mock(LogQueue.class);
        mockInferenceSession = mock(Yolo.class);
        onnxRunner = spy(new OnnxRunner(logQueue));

        setPrivateField(onnxRunner, "inferenceSession", mockInferenceSession);
        setPrivateField(onnxRunner, "logQueue", logQueue);
        onnxRunner.setBufferThreshold(3);
    }

    /**
     * Given that an OrtException occurs during inference,
     * when runInference is invoked,
     * then the error is logged and an empty OnnxOutput is returned.
     *
     * <p>
     * <b>Pass Condition:</b> logQueue records an error containing "Error running inference"
     * and the returned OnnxOutput has an empty detection list.
     * <br>
     * <b>Fail Condition:</b> The error is not logged or the output is not empty.
     * </p>
     *
     * @throws Exception if the inference simulation fails unexpectedly
     */
    @Test
    public void givenOrtExceptionDuringInference_whenRunInference_thenErrorLoggedAndEmptyOutputReturned() throws Exception {
        // Given: A mock inference session that throws an OrtException.
        when(mockInferenceSession.run(any(Mat.class)))
                .thenThrow(new OrtException("Simulated inference failure"));

        try (MockedStatic<ErrorDialogManager> ignored = mockStatic(ErrorDialogManager.class)) {
            // When: runInference is called.
            OnnxOutput output = onnxRunner.runInference(dummyFrame);
            // Then: The error should be logged and the output should be empty.
            verify(logQueue, atLeastOnce()).addRedLog(contains("Error running inference"));
            assertThat(output.getDetectionList()).isEmpty();
        }
    }

    /**
     * Given valid model and label paths,
     * when updateInferenceSession is invoked,
     * then the inference session is reset (i.e., a new Yolo session is created).
     *
     * <p>
     * <b>Pass Condition:</b> The new inference session is not the same as the old one.
     * <br>
     * <b>Fail Condition:</b> The inference session remains unchanged.
     * </p>
     *
     * @throws Exception if reflection fails to access the inference session field
     */
    @Test
    public void givenValidModelPaths_whenUpdateInferenceSession_thenInferenceSessionIsReset() throws Exception {
        Field inferenceSessionField = OnnxRunner.class.getDeclaredField("inferenceSession");
        inferenceSessionField.setAccessible(true);
        Yolo oldSession = (Yolo) inferenceSessionField.get(onnxRunner);

        // Given: Valid model paths. When: Updating the inference session.
        onnxRunner.updateInferenceSession(TestingPaths.DUMMY_MODEL_PATH, TestingPaths.DUMMY_LABEL_PATH);
        // Then: The inference session should be reset.
        Yolo newSession = (Yolo) inferenceSessionField.get(onnxRunner);
        assertThat(newSession).isNotSameAs(oldSession);
    }

    /**
     * Given invalid model and label paths,
     * when updateInferenceSession is invoked,
     * then an error dialog is displayed via the ErrorDialogManager.
     *
     * <p>
     * <b>Pass Condition:</b> ErrorDialogManager.displayErrorDialog is called at least once with a message
     * containing "Load model from invalidModel failed".
     * <br>
     * <b>Fail Condition:</b> No error dialog is displayed or the message does not contain the expected text.
     * </p>
     */
    @Test
    public void givenInvalidModelPaths_whenUpdateInferenceSession_thenErrorDialogIsDisplayed() {
        try (MockedStatic<ErrorDialogManager> mockedErrorDialog = mockStatic(ErrorDialogManager.class)) {
            mockedErrorDialog.when(() -> ErrorDialogManager.displayErrorDialog(any()))
                    .thenAnswer(invocation -> null);

            // Given: Invalid model paths. When: Updating the inference session.
            onnxRunner.updateInferenceSession("invalidModel", "invalidLabels");

            // Then: ErrorDialogManager should be called with the expected message.
            mockedErrorDialog.verify(() -> ErrorDialogManager.displayErrorDialog(
                    argThat(message -> message != null && message.contains("Load model from invalidModel failed"))
            ), atLeast(1));
        }
    }
}
