package comp3011;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE;

public class SubtitleClient {

    private static final String SUBTITLE_URL =
            "http://localhost:8080/api/v1/subtitles";

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public SubtitleApiResponse getSubtitles(File mediaFile)
            throws Exception {

        File audioFile = extractAudio(mediaFile);

        try {
            String boundary = "SubtitleBoundary";

            HttpRequest.BodyPublisher body =
                    HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofString(
                            "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; "
                            + "name=\"file\"; filename=\""
                            + audioFile.getName() + "\"\r\n"
                            + "Content-Type: audio/wav\r\n\r\n"),

                        HttpRequest.BodyPublishers.ofFile(
                                audioFile.toPath()),

                        HttpRequest.BodyPublishers.ofString(
                            "\r\n--" + boundary + "--\r\n")
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(SUBTITLE_URL))
                            .header(
                                    "Content-Type",
                                    "multipart/form-data; boundary="
                                            + boundary)
                            .POST(body)
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Subtitle server returned status "
                        + response.statusCode());
            }

            return objectMapper.readValue(
                    response.body(),
                    SubtitleApiResponse.class);

        } finally {
            Files.deleteIfExists(audioFile.toPath());
        }
    }

    private File extractAudio(File mediaFile)
            throws Exception {

        File audioFile =
                Files.createTempFile(
                        "subtitle-audio-", ".wav")
                        .toFile();

        FFmpegFrameGrabber grabber =
                new FFmpegFrameGrabber(mediaFile);
        grabber.setAudioStream(0);

        FFmpegFrameRecorder recorder = null;

        try {
            grabber.start();

            Frame audioFrame;

            while ((audioFrame =
                    grabber.grabSamples()) != null) {

                if (audioFrame.samples == null
                        || audioFrame.audioChannels <= 0) {
                    continue;
                }

                if (recorder == null) {
                    recorder =
                            new FFmpegFrameRecorder(
                                    audioFile,
                                    audioFrame.audioChannels);

                    recorder.setFormat("wav");

                    int sampleRate =
                            audioFrame.sampleRate > 0
                                    ? audioFrame.sampleRate
                                    : 48000;

                    recorder.setSampleRate(sampleRate);
                    recorder.setAudioCodec(
                            AV_CODEC_ID_PCM_S16LE);

                    recorder.start();
                }

                recorder.recordSamples(
                        audioFrame.sampleRate,
                        audioFrame.audioChannels,
                        audioFrame.samples);
            }

            if (recorder == null) {
                throw new IOException(
                        "The media file does not contain readable audio.");
            }

        } finally {

            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }

            grabber.stop();
            grabber.release();
        }

        return audioFile;
    }
    }