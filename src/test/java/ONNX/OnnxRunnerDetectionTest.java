package ONNX;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import utils.TestingPaths;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OnnxRunnerDetectionTest {

    private OnnxRunner onnxRunner;

    @Mock
    private Yolo mockInferenceSession;

    @Mock
    private LogQueue logQueue;

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
            dummySettings.setModelPath(TestingPaths.DUMMY_MODEL_PATH);
            dummySettings.setLabelPath(TestingPaths.DUMMY_LABEL_PATH);
            ProgramSettings.setCurrentSettings(dummySettings);
        }

        // Construct a real instance of OnnxRunner and wrap it as a spy.
        logQueue = mock(LogQueue.class);
        mockInferenceSession = mock(Yolo.class);
        onnxRunner = spy(new OnnxRunner(logQueue));

        // Inject mock dependencies using our helper method.
        setPrivateField(onnxRunner, "inferenceSession", mockInferenceSession);
        setPrivateField(onnxRunner, "logQueue", logQueue);
        setPrivateField(onnxRunner, "bufferThreshold", 3);
    }

    @Test
    public void testProcessDetectionsDoesNotAddDetectionBelowThreshold() {
        Detection detection = new Detection("test", new float[]{10, 10, 50, 50}, 0.9f);
        onnxRunner.processDetections(Collections.singletonList(detection));
        assertThat(onnxRunner.getActiveDetections()).doesNotContainKey("test");
    }

    @Test
    public void testProcessDetectionsAddsDetectionAfterThreshold() {
        onnxRunner.setBufferThreshold(3);
        Detection detection = new Detection("test", new float[]{10, 10, 50, 50}, 0.9f);
        onnxRunner.processDetections(Collections.singletonList(detection));
        onnxRunner.processDetections(Collections.singletonList(detection));
        onnxRunner.processDetections(Collections.singletonList(detection));
        assertThat(onnxRunner.getActiveDetections()).containsKey("test");
    }

    @Test
    public void testProcessDetectionsRemovesDetectionAfterDisappearance() {
        Detection detection = new Detection("test", new float[]{10, 10, 50, 50}, 0.9f);
        onnxRunner.processDetections(Collections.singletonList(detection));
        onnxRunner.processDetections(Collections.singletonList(detection));
        onnxRunner.processDetections(Collections.singletonList(detection));
        assertThat(onnxRunner.getActiveDetections()).containsKey("test");

        // Simulate disappearance.
        onnxRunner.processDetections(Collections.emptyList());
        onnxRunner.processDetections(Collections.emptyList());
        onnxRunner.processDetections(Collections.emptyList());
        assertThat(onnxRunner.getActiveDetections()).doesNotContainKey("test");
    }

}
