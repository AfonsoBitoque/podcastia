package com.jep.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldReturnApiDocsAsJson() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void shouldReturnSwaggerUi() throws Exception {
    // Pode retornar 302 Found (redirecionamento) ou 200 OK
    // O importante é não retornar 401 (Unauthorized) ou 403 (Forbidden)
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().is2xxSuccessful());
  }
}
