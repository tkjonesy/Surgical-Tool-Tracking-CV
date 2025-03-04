package ONNX;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import utils.TestingPaths;

import javax.swing.JOptionPane;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
public class OnnxRunnerErrorHandlingTest {

    private OnnxRunner onnxRunner;

    @Mock
    private Yolo mockInferenceSession;

    @Mock
    private LogQueue logQueue;

    private Mat dummyFrame;

    // Helper method to inject private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    public void setup() throws Exception {
        // Setup ProgramSettings if not already set.
        if (ProgramSettings.getCurrentSettings() == null) {
            ProgramSettings dummySettings = new ProgramSettings();
            Method setSettingMethod = ProgramSettings.class.getDeclaredMethod("setSettings", String.class, Object.class);
            setSettingMethod.setAccessible(true);
            dummySettings.setModelPath(TestingPaths.DUMMY_MODEL_PATH);
            dummySettings.setLabelPath(TestingPaths.DUMMY_LABEL_PATH);
            ProgramSettings.setCurrentSettings(dummySettings);
        }

        logQueue = mock(LogQueue.class);
        mockInferenceSession = mock(Yolo.class);

        // Construct a real instance of OnnxRunner and wrap it as a spy.
        onnxRunner = spy(new OnnxRunner(logQueue));

        // Initialize a dummy frame (10x10 Mat for testing).
        dummyFrame = new Mat(10, 10, 0);

        // Inject mock dependencies using our helper method.
        setPrivateField(onnxRunner, "inferenceSession", mockInferenceSession);
        setPrivateField(onnxRunner, "logQueue", logQueue);
        setPrivateField(onnxRunner, "bufferThreshold", 3);
    }

    @Test
    public void testRunInferenceHandlesOrtExceptionGracefully() throws Exception {
        // Stub run method to throw an OrtException.
        when(mockInferenceSession.run(any(Mat.class)))
                .thenThrow(new OrtException("Simulated inference failure"));

        // Intercept and suppress JOptionPane calls.
        try (MockedStatic<JOptionPane> mockedJOptionPane = mockStatic(JOptionPane.class)) {
            OnnxOutput output = onnxRunner.runInference(dummyFrame);
            verify(logQueue, atLeastOnce()).addRedLog(contains("Error running inference"));
            assertThat(output.getDetectionList()).isEmpty();
        }
    }

    @Test
    public void testUpdateInferenceSessionResetsTheSession() throws Exception {
        Field inferenceSessionField = OnnxRunner.class.getDeclaredField("inferenceSession");
        inferenceSessionField.setAccessible(true);
        Yolo oldSession = (Yolo) inferenceSessionField.get(onnxRunner);

        onnxRunner.updateInferenceSession(TestingPaths.DUMMY_MODEL_PATH, TestingPaths.DUMMY_LABEL_PATH);

        Yolo newSession = (Yolo) inferenceSessionField.get(onnxRunner);
        assertThat(newSession).isNotSameAs(oldSession);
    }

    @Test
    public void testUpdateInferenceSessionFailureShowsDialog() {
        // Use static mocking to intercept calls to JOptionPane.
        try (MockedStatic<JOptionPane> mockedJOptionPane = mockStatic(JOptionPane.class)) {
            mockedJOptionPane.when(JOptionPane::getRootFrame).thenReturn(new javax.swing.JFrame());
            mockedJOptionPane.when(() -> JOptionPane.showMessageDialog(any(), any(), any(), anyInt()))
                    .thenAnswer(invocation -> null);

            onnxRunner.updateInferenceSession("invalidModel", "invalidLabels");

            // Verify that the error dialog was shown at least once with an error message containing the expected text.
            mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(),
                    contains("Load model from invalidModel failed"),
                    anyString(),
                    eq(JOptionPane.ERROR_MESSAGE)
            ), atLeast(1));
        }
    }
}
