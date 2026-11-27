package comp3011.assignment1.service;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import comp3011.assignment1.dto.SubtitleResponse;
import comp3011.assignment1.dto.SubtitleResponse.SubtitleSegment;
import comp3011.assignment1.dto.SubtitleResponse.SubtitleWord;

@Service
public class SubtitleService {

    private final RestClient openAiRestClient;
    private final GlobalStatsService globalStatsService;

    public SubtitleService(RestClient openAiRestClient, GlobalStatsService globalStatsService) {
        this.openAiRestClient = openAiRestClient;
        this.globalStatsService = globalStatsService;
    }

    public SubtitleResponse transcribe(MultipartFile audioFile) {

        LinkedMultiValueMap<String, Object> request =
                new LinkedMultiValueMap<>();

        request.add("file", audioFile.getResource());
        request.add("model", "whisper-1");
        request.add("response_format", "verbose_json");
        request.add("timestamp_granularities[]", "word");
        request.add("timestamp_granularities[]", "segment");

        OpenAiSubtitleResult result = openAiRestClient.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(request)
                .retrieve()
                .body(OpenAiSubtitleResult.class);

        List<SubtitleSegment> segments = result.segments().stream()
                .filter(segment ->
                        !(segment.noSpeechProb() > 0.6
                        && segment.avgLogProb() < -1.0))
                .map(segment -> new SubtitleSegment(
                        segment.id(),
                        segment.start(),
                        segment.end(),
                        segment.text(),
                        segment.avgLogProb(),
                        segment.noSpeechProb()))
                .toList();

        List<SubtitleWord> words = result.words().stream()
                .map(word -> new SubtitleWord(
                        word.word(),
                        word.start(),
                        word.end()))
                .toList();
        
        globalStatsService.recordUsageSeconds(result.duration());

        return new SubtitleResponse(
                result.text(),
                result.duration(),
                segments,
                words);
    }

    private record OpenAiSubtitleResult(
            String text,
            double duration,
            List<OpenAiSegment> segments,
            List<OpenAiWord> words) {
    }

    private record OpenAiSegment(
            int id,
            double start,
            double end,
            String text,
            @JsonProperty("avg_logprob") double avgLogProb,
            @JsonProperty("no_speech_prob") double noSpeechProb) {
    }

    private record OpenAiWord(
            String word,
            double start,
            double end) {
    }
}