package comp3011;

import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;

public class FrameSubtitler extends FrameProcessor {

    private volatile boolean enabled;
    private volatile List<SubtitleCue> subtitles = List.of();
    
    private final OpenCVFrameConverter.ToMat converter =
            new OpenCVFrameConverter.ToMat();

    private InfoVideo videoInfo;

    public FrameSubtitler(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggleEnabled() {
        enabled = !enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSubtitles(List<SubtitleCue> subtitles) {
        this.subtitles = List.copyOf(subtitles);
    }
    
    private List<String> splitSubtitle(String text) {
        text = text.trim();

        if (text.length() <= 42) {
            return List.of(text);
        }

        int splitPosition = -1;

        // Prefer breaking after punctuation.
        for (int i = Math.min(41, text.length() - 1); i >= 0; i--) {
            char c = text.charAt(i);

            if (c == '.' || c == ',' || c == '!'
                    || c == '?' || c == ';' || c == ':') {

                splitPosition = i + 1;
                break;
            }
        }

        // Otherwise break at the nearest space.
        if (splitPosition == -1) {
            splitPosition = text.lastIndexOf(' ', 42);
        }

        if (splitPosition <= 0) {
            splitPosition = 42;
        }

        String firstLine =
                text.substring(0, splitPosition).trim();

        String secondLine =
                text.substring(splitPosition).trim();

        if (secondLine.length() > 42) {
            secondLine = secondLine.substring(0, 39) + "...";
        }

        return List.of(firstLine, secondLine);
    }
    
    @Override
    public void initialise(InfoVideo videoInfo) throws Exception {
        this.videoInfo = videoInfo;
    }

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {
        if (!enabled || subtitles.isEmpty()) {
            return;
        }

        double currentSeconds = info.timestampUs() / 1_000_000.0;

        SubtitleCue currentSubtitle = null;

        for (SubtitleCue subtitle : subtitles) {
            if (currentSeconds >= subtitle.startSeconds()
                    && currentSeconds <= subtitle.endSeconds()) {

                currentSubtitle = subtitle;
                break;
            }
        }

        if (currentSubtitle == null) {
            return;
        }
        
        Mat image = converter.convert(frame);

        if (image == null) {
            return;
        }

        List<String> lines =
                splitSubtitle(currentSubtitle.text());

        int y = videoInfo.imageHeight() - 60;

        for (String line : lines) {
            int x = Math.max(
                    20,
                    (videoInfo.imageWidth() - line.length() * 14) / 2);

            // Black outline
            putText(
                    image,
                    line,
                    new Point(x, y),
                    FONT_HERSHEY_SIMPLEX,
                    0.8,
                    new Scalar(0, 0, 0, 0),
                    5,
                    LINE_AA,
                    false);

            // White text
            putText(
                    image,
                    line,
                    new Point(x, y),
                    FONT_HERSHEY_SIMPLEX,
                    0.8,
                    new Scalar(255, 255, 255, 0),
                    2,
                    LINE_AA,
                    false);

            y += 35;
        }
    }
}
