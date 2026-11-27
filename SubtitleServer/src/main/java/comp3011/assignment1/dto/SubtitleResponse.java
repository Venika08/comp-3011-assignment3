package comp3011.assignment1.dto;

import java.util.List;

public record SubtitleResponse(
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