package com.craftedbyk.puzzle.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** With App Check enabled, an {@code /api/**} call lacking an attestation token is rejected. */
@SpringBootTest(properties = "cbk.security.app-check-enabled=true")
class AppCheckFilterTest {

  @Autowired private WebApplicationContext context;
  @Autowired private AppCheckFilter appCheckFilter;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(appCheckFilter).build();
  }

  @Test
  void rejectsApiCallWithoutAppCheckToken() throws Exception {
    mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
  }

  @Test
  void allowsHealthProbeWithoutToken() throws Exception {
    mvc.perform(get("/api/puzzle/health")).andExpect(status().isOk());
  }
}
