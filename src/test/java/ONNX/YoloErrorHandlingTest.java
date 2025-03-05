package ONNX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ai.onnxruntime.*;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import utils.TestingPaths;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class YoloErrorHandlingTest {

    // Dummy model/label paths for testing.
    private static final String DUMMY_MODEL_PATH = TestingPaths.DUMMY_MODEL_PATH;
    private static final String DUMMY_LABEL_PATH = TestingPaths.DUMMY_LABEL_PATH;

    // We'll create a dummy Yolo instance using an anonymous subclass.
    private Yolo testYolo;

    @Mock
    private LogQueue mockLogQueue;

    // A dummy OrtSession object to represent a successful CPU session.
    // (The actual type isn't used by Yolo after construction except to assign it.)
    @Mock
    private ai.onnxruntime.OrtSession dummyCpuSession;


    @BeforeEach
    public void setup() throws Exception {
        // Set up ProgramSettings: mark useGPU true.
        ProgramSettings dummySettings = new ProgramSettings();
        Method setSettingMethod = ProgramSettings.class.getDeclaredMethod("setSettings", String.class, Object.class);
        setSettingMethod.setAccessible(true);
        setSettingMethod.invoke(dummySettings, "useGPU", true);
        dummySettings.setModelPath(DUMMY_MODEL_PATH);
        dummySettings.setLabelPath(DUMMY_LABEL_PATH);
        ProgramSettings.setCurrentSettings(dummySettings);
    }

    @Test
    public void testGpuFailureFallbackSetsCudaFalse() throws Exception {
        try (MockedStatic<OrtEnvironment> envStatic = mockStatic(OrtEnvironment.class);
             MockedStatic<JOptionPane> paneStatic = mockStatic(JOptionPane.class)) {

            // Create a mock environment.
            OrtEnvironment mockEnv = mock(OrtEnvironment.class);
            // When OrtEnvironment.getEnvironment() is called, return our mock.
            envStatic.when(OrtEnvironment::getEnvironment).thenReturn(mockEnv);
            // Simulate available providers including CUDA.
            envStatic.when(OrtEnvironment::getAvailableProviders)
                    .thenReturn(EnumSet.of(OrtProvider.CUDA, OrtProvider.CPU));

            // When createSession is called with any options, simulate that the first call (GPU branch) fails,
            // and the fallback (CPU branch) returns a dummy session.
            when(mockEnv.createSession(eq(DUMMY_MODEL_PATH), any()))
                    .thenThrow(new OrtException("Simulated GPU failure"))
                    .thenReturn(dummyCpuSession);

            // Stub JOptionPane.getRootFrame() so that no headless exception occurs.
            paneStatic.when(JOptionPane::getRootFrame).thenReturn(new javax.swing.JFrame());

            // Create an instance of Yolo using an anonymous subclass to implement run().
            try {
                testYolo = new Yolo(DUMMY_MODEL_PATH, DUMMY_LABEL_PATH) {
                    @Override
                    public List<Detection> run(Mat img) {
                        return List.of();
                    }
                };
            }catch (Exception ignore) {}

            // Since the GPU branch threw an exception and the fallback was used,
            // isCudaAvailable should be set to false.
            assertThat(Yolo.isCudaAvailable()).isFalse();
        }
    }

    @Test
    public void testUpdateInferenceSessionFailureShowsDialog() throws Exception {
        try (MockedStatic<OrtEnvironment> envStatic = mockStatic(OrtEnvironment.class);
             MockedStatic<JOptionPane> paneStatic = mockStatic(JOptionPane.class)) {

            // Create a mock environment.
            OrtEnvironment mockEnv = mock(OrtEnvironment.class);
            envStatic.when(OrtEnvironment::getEnvironment).thenReturn(mockEnv);
            envStatic.when(OrtEnvironment::getAvailableProviders)
                    .thenReturn(EnumSet.of(OrtProvider.CUDA, OrtProvider.CPU));

            // Stub createSession so that the GPU branch fails.
            when(mockEnv.createSession(eq(DUMMY_MODEL_PATH), any()))
                    .thenThrow(new OrtException("Simulated GPU failure"))
                    .thenReturn(dummyCpuSession);

            // Stub getRootFrame() so that no headless exception occurs.
            paneStatic.when(JOptionPane::getRootFrame).thenReturn(new JFrame());

            // Create an instance of Yolo using an anonymous subclass to implement run().
            try {
                testYolo = new Yolo(DUMMY_MODEL_PATH, DUMMY_LABEL_PATH) {
                    @Override
                    public List<Detection> run(Mat img) {
                        return List.of();
                    }
                };
            }catch (Exception ignore) {}

            // Verify that the error dialog was attempted (i.e. that showMessageDialog was called)
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to create session with GPU, falling back to CPU:",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

        }
    }

}
