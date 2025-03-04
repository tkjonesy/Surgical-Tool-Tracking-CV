package io.github.tkjonesy.ONNX;

import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;

import static org.bytedeco.opencv.global.opencv_core.copyMakeBorder;
import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;

import java.util.List;

/**
 * The {@code ImageUtil} class provides utility functions for image processing tasks,
 * including resizing images with padding, reordering color channels, and drawing predictions on images.
 */
public class ImageUtil {

    private static final ProgramSettings settings = ProgramSettings.getCurrentSettings();

    /**
     * Resizes the source image to fit within the specified dimensions, adding padding
     * as necessary to maintain the original aspect ratio.
     *
     * @param src The source image to resize.
     * @param dst The destination image after resizing and padding.
     * @param width The target width for the resized image.
     * @param height The target height for the resized image.
     */
    public static void resizeWithPadding(Mat src, Mat dst, int width, int height) {
        int oldW = src.cols();
        int oldH = src.rows();

        double r = Math.min((double) width / oldW, (double) height / oldH);

        int newUnpadW = (int) Math.round(oldW * r);
        int newUnpadH = (int) Math.round(oldH * r);

        int dw = (width - newUnpadW) / 2;
        int dh = (height - newUnpadH) / 2;

        int top = (int) Math.round(dh - 0.1);
        int bottom = (int) Math.round(dh + 0.1);
        int left = (int) Math.round(dw - 0.1);
        int right = (int) Math.round(dw + 0.1);

        resize(src, dst, new Size(newUnpadW, newUnpadH));
        copyMakeBorder(dst, dst, top, bottom, left, right, BORDER_CONSTANT);
    }

    /**
     * Reorders a float array from WHC (width-height-channels) format to CWH (channels-width-height) format.
     *
     * @param src The source array in WHC format.
     * @return A new array in CWH format.
     */
    public static float[] whc2cwh(float[] src) {
        float[] chw = new float[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ++ch) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }

    /**
     * Reorders a byte array from WHC (width-height-channels) format to CWH (channels-width-height) format.
     *
     * @param src The source array in WHC format.
     * @return A new array in CWH format.
     */
    public static byte[] whc2cwh(byte[] src) {
        byte[] chw = new byte[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ++ch) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }

    /**
     * Draws bounding boxes and labels on an image based on a list of detections.
     *
     * @param img The image on which to draw the predictions.
     * @param detectionList The list of detections to draw, where each detection includes
     *                      bounding box coordinates and a label.
     */
    public static void drawPredictions(Mat img, List<Detection> detectionList) {

        if(!settings.isShowBoundingBoxes()){return;}

        for (Detection detection : detectionList) {
            float[] bbox = detection.bbox();
            Scalar color = new Scalar(249.0, 218.0, 60.0, 0.0);
            rectangle(img,                    // Matrix object of the image
                    new Point((int) bbox[0], (int) bbox[1]),      // Top-left point
                    new Point((int) bbox[2], (int) bbox[3]),      // Bottom-right point
                    color,                            // Color of the rectangle
                    2,                                 // Line thickness
                    LINE_8,                           // Type of line
                    0                                  // Shift
            );


            if(settings.isShowLabels()){
                putText(
                        img,
                        detection.label(),
                        new Point((int) bbox[0] - 1, (int) bbox[1] - 5),
                        FONT_HERSHEY_SIMPLEX,
                        0.5,
                        color,
                        1,       // thickness
                        LINE_8,  // lineType
                        false    // bottomLeftOrigin
                );
            }

            if(settings.isShowConfidences()){
                putText(
                        img,
                        String.format("%.2f", detection.confidence()),
                        new Point((int) bbox[0] - 1, (int) bbox[1] - 20),
                        FONT_HERSHEY_SIMPLEX,
                        0.5,
                        color,
                        1,       // thickness
                        LINE_8,  // lineType
                        false    // bottomLeftOrigin
                );
            }

        }
    }
}
