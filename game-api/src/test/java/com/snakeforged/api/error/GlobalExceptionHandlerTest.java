package com.snakeforged.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snakeforged.api.highscore.HighscoreController;
import com.snakeforged.api.highscore.HighscoreRequestDTO;
import com.snakeforged.api.highscore.HighscoreService;
import com.snakeforged.config.SecurityConfig;
import com.snakeforged.observability.MetricsService;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies GlobalExceptionHandler behaviour through the HighscoreController slice.
 * @WebMvcTest auto-detects @ControllerAdvice beans so GlobalExceptionHandler
 * is loaded without any explicit @Import.
 */
@WebMvcTest(HighscoreController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HighscoreRepository highscoreRepository;

    @MockBean
    private HighscoreService highscoreService;

    @MockBean
    private MetricsService metricsService;

    // ── 400 structured error ──────────────────────────────────────────────────

    @Test
    void illegalArgumentReturns400WithStructuredBody() throws Exception {
        mvc.perform(get("/api/v1/highscores").param("difficulty", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path", is("/api/v1/highscores")));
    }

    // ── Validation 400 with fieldErrors ──────────────────────────────────────

    @Test
    void validationErrorContainsFieldErrorsArray() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("", -1, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void fieldErrorsContainFieldNameAndMessage() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("bad nickname!", 100, "EASY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    // ── 500 hides internal details ────────────────────────────────────────────

    @Test
    void unexpectedExceptionReturns500WithGenericMessage() throws Exception {
        when(highscoreService.submitScore(any(), any()))
                .thenThrow(new RuntimeException("database exploded"));

        String body = mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HighscoreRequestDTO("Alice", 100, "EASY"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("Internal server error")))
                .andReturn().getResponse().getContentAsString();

        // Must not leak internal exception details
        assert !body.contains("database exploded");
        assert !body.contains("RuntimeException");
    }

    // ── Missing request body ──────────────────────────────────────────────────

    @Test
    void missingRequestBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/highscores")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── Content-Type is always JSON ───────────────────────────────────────────

    @Test
    void allErrorResponsesHaveJsonContentType() throws Exception {
        mvc.perform(get("/api/v1/highscores").param("difficulty", "BOGUS"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
