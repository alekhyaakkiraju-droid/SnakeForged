package com.snakeforged.api.highscore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snakeforged.persistence.entity.HighscoreEntry;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HighscoreController.class)
class HighscoreControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HighscoreRepository highscoreRepository;

    @MockBean
    private HighscoreService highscoreService;

    // ── GET tests ─────────────────────────────────────────────────────────────

    private HighscoreEntry entry(String nickname, int score, String difficulty) {
        HighscoreEntry e = new HighscoreEntry();
        e.setNickname(nickname);
        e.setScore(score);
        e.setDifficulty(difficulty);
        e.setCreatedAt(LocalDateTime.of(2026, 5, 12, 10, 0, 0));
        return e;
    }

    @Test
    void getHighscoresEasyReturns200() throws Exception {
        when(highscoreRepository.findTop10ByDifficultyOrderByScoreDesc("EASY"))
                .thenReturn(List.of(entry("Alice", 500, "EASY"), entry("Bob", 300, "EASY")));

        mvc.perform(get("/api/v1/highscores").param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void responseContainsCorrectFields() throws Exception {
        when(highscoreRepository.findTop10ByDifficultyOrderByScoreDesc("MEDIUM"))
                .thenReturn(List.of(entry("Carol", 750, "MEDIUM")));

        mvc.perform(get("/api/v1/highscores").param("difficulty", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname", is("Carol")))
                .andExpect(jsonPath("$[0].score", is(750)))
                .andExpect(jsonPath("$[0].difficulty", is("MEDIUM")))
                .andExpect(jsonPath("$[0].createdAt", is("2026-05-12T10:00:00")));
    }

    @Test
    void caseInsensitiveDifficultyParam() throws Exception {
        when(highscoreRepository.findTop10ByDifficultyOrderByScoreDesc("HARD"))
                .thenReturn(List.of());

        mvc.perform(get("/api/v1/highscores").param("difficulty", "hard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void emptyLeaderboardReturnsEmptyArray() throws Exception {
        when(highscoreRepository.findTop10ByDifficultyOrderByScoreDesc("EASY"))
                .thenReturn(List.of());

        mvc.perform(get("/api/v1/highscores").param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidDifficultyReturns400WithMessage() throws Exception {
        mvc.perform(get("/api/v1/highscores").param("difficulty", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingDifficultyParamReturns400WithMessage() throws Exception {
        mvc.perform(get("/api/v1/highscores"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── POST tests ────────────────────────────────────────────────────────────

    @Test
    void validPostReturns201WithBody() throws Exception {
        HighscoreEntry saved = entry("Alice", 100, "EASY");
        when(highscoreService.submitScore(any(), any())).thenReturn(saved);

        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice", 100, "EASY"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname", is("Alice")))
                .andExpect(jsonPath("$.score", is(100)))
                .andExpect(jsonPath("$.difficulty", is("EASY")));
    }

    @Test
    void emptyNicknameReturns400() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("", 100, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void nicknameWithSpecialCharsReturns400() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice!@#", 100, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void negativeScoreReturns400() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice", -1, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void invalidDifficultyOnPostReturns400() throws Exception {
        when(highscoreService.submitScore(any(), any()))
                .thenThrow(new IllegalArgumentException("Unknown difficulty: 'ULTRA'"));

        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice", 100, "ULTRA"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void implausibleScoreReturns400() throws Exception {
        when(highscoreService.submitScore(any(), any()))
                .thenThrow(new IllegalArgumentException("Score 99999 is implausible for difficulty EASY"));

        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice", 99999, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
