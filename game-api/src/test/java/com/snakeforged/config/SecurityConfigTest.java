package com.snakeforged.config;

import com.snakeforged.api.difficulty.DifficultyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DifficultyController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void responseIncludesContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/difficulties"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("style-src 'self'")));
    }

    @Test
    void responseIncludesXContentTypeOptionsNosniff() throws Exception {
        mockMvc.perform(get("/api/v1/difficulties"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void responseIncludesXFrameOptionsDeny() throws Exception {
        mockMvc.perform(get("/api/v1/difficulties"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void responseIncludesReferrerPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/difficulties"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void crossOriginPreflightLacksAccessControlAllowOrigin() throws Exception {
        // Cross-origin preflight must not receive Access-Control-Allow-Origin,
        // which causes the browser to block the request.
        mockMvc.perform(options("/api/v1/highscores")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void requestWithoutOriginHeaderSucceeds() throws Exception {
        // Same-origin browser requests never include the Origin header for GET requests;
        // they bypass CORS entirely and reach the controller normally.
        mockMvc.perform(get("/api/v1/difficulties"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
