package comp3011;

import java.util.List;

public record SubtitleApiResponse(
        String text,
        double duration,
        List<SubtitleSegment> segments,
        List<SubtitleWord> words) {

    public record SubtitleSegment(
            int id,
            double start,
            double end,
            String text,
            double avgLogProb,
            double noSpeechProb) {
    }

    public record SubtitleWord(
            String word,
            double start,
            double end) {
    }
}