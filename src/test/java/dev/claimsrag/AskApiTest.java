package dev.claimsrag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "assistant.provider=fake")
@AutoConfigureMockMvc
class AskApiTest {

    @Autowired MockMvc mvc;

    @Test
    void answersWithCitationAndUsage() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How many days do I have to file a first-level appeal?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.citations[0]").value(org.hamcrest.Matchers.startsWith("appeals#")))
                .andExpect(jsonPath("$.usage.inputTokens").isNumber());
    }

    @Test
    void abstainsWhenNothingRelevant() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"best pizza toppings\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(false));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usageEndpointResponds() throws Exception {
        mvc.perform(get("/api/usage")).andExpect(status().isOk());
    }
}
