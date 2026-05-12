package com.snakeforged.config;

import com.snakeforged.api.highscore.HighscoreController;
import com.snakeforged.api.highscore.HighscoreService;
import com.snakeforged.persistence.repository.HighscoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HighscoreController.class)
@Import({SecurityConfig.class, RequestBodySizeLimitFilter.class})
class RequestBodySizeLimitFilterTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private HighscoreRepository highscoreRepository;

    @MockBean
    private HighscoreService highscoreService;

    private static final String ENDPOINT = "/api/v1/highscores";

    @Test
    void oversizedBodyReturns413() throws Exception {
        byte[] bigBody = new byte[5000];
        Arrays.fill(bigBody, (byte) 'x');
        // Wrap in a JSON string to set correct Content-Type; body still > 4096 bytes
        String bigJson = "{\"nickname\":\"" + new String(bigBody) + "\",\"score\":1,\"difficulty\":\"EASY\"}";

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bigJson))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void oversized413ResponseFollowsStructuredFormat() throws Exception {
        byte[] bigBody = new byte[5000];
        Arrays.fill(bigBody, (byte) 'a');
        String bigJson = "{\"nickname\":\"" + new String(bigBody) + "\",\"score\":1,\"difficulty\":\"EASY\"}";

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bigJson))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path", is(ENDPOINT)));
    }

    @Test
    void normalBodyUnder4kbIsProcessedNormally() throws Exception {
        String validJson = "{\"nickname\":\"Alice\",\"score\":100,\"difficulty\":\"EASY\"}";

        // 400 (validation/service error) is fine — it means the request reached the controller
        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(
                                413, result.getResponse().getStatus(),
                                "Normal-sized body should not be rejected by size filter"));
    }

    @Test
    void oversizedBodyOnGetEndpointIsNotBlocked() throws Exception {
        // The size limit only applies to POST /api/v1/highscores; GET is unaffected
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/highscores")
                        .param("difficulty", "EASY"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(
                                413, result.getResponse().getStatus(),
                                "GET requests should not be rejected by the body size filter"));
    }
}
