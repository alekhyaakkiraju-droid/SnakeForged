package com.snakeforged.api.difficulty;

import com.snakeforged.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DifficultyController.class)
@Import(SecurityConfig.class)
class DifficultyControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void getDifficultiesReturns200() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(status().isOk());
    }

    @Test
    void responseIsJson() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void returnsExactlyThreeDifficulties() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void easyHasCorrectFields() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(jsonPath("$[0].name", is("EASY")))
                .andExpect(jsonPath("$[0].displayName", is("Easy")))
                .andExpect(jsonPath("$[0].tickIntervalMs", is(100)));
    }

    @Test
    void mediumHasCorrectFields() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(jsonPath("$[1].name", is("MEDIUM")))
                .andExpect(jsonPath("$[1].displayName", is("Medium")))
                .andExpect(jsonPath("$[1].tickIntervalMs", is(70)));
    }

    @Test
    void hardHasCorrectFields() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(jsonPath("$[2].name", is("HARD")))
                .andExpect(jsonPath("$[2].displayName", is("Hard")))
                .andExpect(jsonPath("$[2].tickIntervalMs", is(40)));
    }

    @Test
    void responseIncludesCacheControlMaxAge300() throws Exception {
        mvc.perform(get("/api/v1/difficulties"))
                .andExpect(header().string("Cache-Control", "max-age=300"));
    }
}
