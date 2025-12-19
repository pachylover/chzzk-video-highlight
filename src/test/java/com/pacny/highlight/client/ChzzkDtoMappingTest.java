package com.pacny.highlight.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacny.highlight.client.dto.ChzzkResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ChzzkDtoMappingTest {

    @Test
    void parseSampleResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = this.getClass().getResourceAsStream("/samples/chzzk_sample.json");
        assertNotNull(is);
        ChzzkResponse r = mapper.readValue(is, ChzzkResponse.class);
        assertEquals(200, r.getCode());
        assertNotNull(r.getContent());
        assertEquals(2, r.getContent().getVideoChats().size());
        assertEquals("구미당기지", mapper.readTree(r.getContent().getVideoChats().get(0).getProfile()).get("nickname").asText());
    }
}
