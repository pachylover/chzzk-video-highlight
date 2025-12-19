package com.pacny.highlight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacny.highlight.dto.CreateHighlightRequest;
import com.pacny.highlight.dto.HighlightResponse;
import com.pacny.highlight.service.HighlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HighlightController.class)
class HighlightControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private HighlightService service;

    @Test
    void postCreatesAccepted() throws Exception {
        CreateHighlightRequest req = new CreateHighlightRequest();
        req.setUrl("https://example.com/watch?v=abc123");
        when(service.createHighlight(any(), any())).thenReturn(UUID.randomUUID());

        mvc.perform(post("/api/v1/highlights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    void postReturnsBadRequestOnInvalidUrl() throws Exception {
        CreateHighlightRequest req = new CreateHighlightRequest();
        req.setUrl("");

        mvc.perform(post("/api/v1/highlights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReturnsBadRequestOnInvalidCallbackUrl() throws Exception {
        CreateHighlightRequest req = new CreateHighlightRequest();
        req.setUrl("https://example.com/watch?v=abc123");
        req.setCallbackUrl("notaurl");

        mvc.perform(post("/api/v1/highlights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreatesAcceptedWithCallback() throws Exception {
        CreateHighlightRequest req = new CreateHighlightRequest();
        req.setUrl("https://example.com/watch?v=abc123");
        req.setCallbackUrl("https://example.com/hook");
        UUID fake = UUID.randomUUID();
        when(service.createHighlight(any(), any())).thenReturn(fake);

        mvc.perform(post("/api/v1/highlights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());

        // verify service called
        verify(service).createHighlight("https://example.com/watch?v=abc123", "https://example.com/hook");
    }

    @Test
    void getReturnsOkWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        HighlightResponse resp = HighlightResponse.builder()
                .taskId(id.toString())
                .status("done")
                .videoId("abc123")
                .minute(Instant.now())
                .build();
        when(service.getHighlight(id)).thenReturn(resp);

        mvc.perform(get("/api/v1/highlights/" + id))
                .andExpect(status().isOk());
    }
}
