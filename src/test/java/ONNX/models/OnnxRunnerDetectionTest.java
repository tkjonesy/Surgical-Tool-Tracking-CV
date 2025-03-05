package ONNX.models;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import testingUtils.TestingPaths;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static testingUtils.HelperMethods.setPrivateField;

/**
 * Test class for verifying the detection handling logic in the {@link OnnxRunner} class.
 * <p>
 * This class validates that:
 * <ul>
 *   <li>Detections are not added to the active detections map until they have been observed in a number of consecutive frames equal to the buffer threshold.</li>
 *   <li>Once the threshold is met, the detection becomes active.</li>
 *   <li>If a detection stops appearing for a series of frames, it is removed from the active detections map.</li>
 * </ul>
 * Naming convention for tests follows the "Given-When-Then" pattern.
 * A passing test indicates that the corresponding error handling behavior is working correctly.
 * A failing test indicates a breakdown in the expected error handling logic..
 */
public class OnnxRunnerDetectionTest {

    private OnnxRunner onnxRunner;

    @Mock
    private Yolo mockInferenceSession;

    @Mock
    private LogQueue logQueue;

    private final Detection dummyDetection = new Detection("test", new float[]{10, 10, 50, 50}, 0.9f);

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
     * <b>Given</b> a detection that has been processed fewer times than the buffer threshold,
     * <b>when</b> processDetections is invoked,
     * <b>then</b> the detection should not be added to the active detections.
     * <p>
     * <b>Pass Condition:</b> The active detections map does not contain the key "test".
     * <br>
     * <b>Fail Condition:</b> The active detections map contains the key "test" despite the threshold not being met.
     */
    @Test
    public void givenDetectionProcessedBelowThreshold_whenProcessDetections_thenDetectionNotAdded() {
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        assertThat(onnxRunner.getActiveDetections()).doesNotContainKey("test");
    }

    /**
     * <b>Given</b> a detection that is processed in consecutive frames for a number equal to the buffer threshold,
     * <b>when</b> processDetections is invoked repeatedly,
     * <b>then</b> the detection should be added to the active detections.
     * <p>
     * <b>Pass Condition:</b> After processing the detection three times, the active detections map contains the key "test".
     * <br>
     * <b>Fail Condition:</b> The active detections map does not contain the key "test" even though the threshold has been met.
     */
    @Test
    public void givenDetectionProcessedConsecutively_whenProcessDetections_thenDetectionIsActive() {
        // Simulate detection presence across three consecutive frames.
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        assertThat(onnxRunner.getActiveDetections()).containsKey("test");
    }

    /**
     * <b>Given</b> an active detection already present in the active detections map,
     * <b>when</b> no detection is processed for a sufficient number of consecutive frames,
     * <b>then</b> the detection should be removed from the active detections.
     * <p>
     * <b>Pass Condition:</b> After simulating absence by processing empty detections for three frames, the active detections map does not contain the key "test".
     * <br>
     * <b>Fail Condition:</b> The active detections map still contains the key "test" despite the detection no longer being present.
     */
    @Test
    public void givenActiveDetection_whenNoDetectionProcessed_thenDetectionIsRemoved() {
        // Simulate detection presence.
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        onnxRunner.processDetections(Collections.singletonList(dummyDetection));
        assertThat(onnxRunner.getActiveDetections()).containsKey("test");

        // Simulate disappearance by processing empty detections across three consecutive frames.
        onnxRunner.processDetections(Collections.emptyList());
        onnxRunner.processDetections(Collections.emptyList());
        onnxRunner.processDetections(Collections.emptyList());
        assertThat(onnxRunner.getActiveDetections()).doesNotContainKey("test");
    }
}
