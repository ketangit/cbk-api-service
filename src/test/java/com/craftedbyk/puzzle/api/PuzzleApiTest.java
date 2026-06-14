package com.craftedbyk.puzzle.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PuzzleApiTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static final String VALID =
      """
			{"seed":42,"nonDeterministic":false,"ncols":8,"nrows":8,
			 "tileRadius":6.0,"frame":6.0,"frameCorner":4.0,
			 "minPieceSize":4,"maxPieceSize":12,"shape":"CIRCULAR"}
			""";

  @Test
  void generateReturnsGeometryAndQuotes() throws Exception {
    mvc.perform(post("/api/puzzle/generate").contentType(MediaType.APPLICATION_JSON).content(VALID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seedUsed").value(42.0))
        .andExpect(jsonPath("$.pieceCount").value(5))
        .andExpect(jsonPath("$.widthMm").value(108.0))
        .andExpect(jsonPath("$.heightMm").value(108.0))
        .andExpect(jsonPath("$.piecePaths", hasSize(5)))
        .andExpect(jsonPath("$.framePath").isString())
        .andExpect(jsonPath("$.priceCentsByMaterial.BIRCH_PLY").value(greaterThan(0)));
  }

  @Test
  void nonDeterministicModeDrawsAFreshSeed() throws Exception {
    String nd = VALID.replace("\"nonDeterministic\":false", "\"nonDeterministic\":true");
    mvc.perform(post("/api/puzzle/generate").contentType(MediaType.APPLICATION_JSON).content(nd))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seedUsed").exists());
  }

  @Test
  void exportReturnsSvgAttachment() throws Exception {
    mvc.perform(
            post("/api/puzzle/export?mode=NON_OVERLAP")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString("jigsaw-non_overlap-seed42.svg")));
  }

  @Test
  void overlapAndNonOverlapExportsDiffer() throws Exception {
    String overlap =
        mvc.perform(
                post("/api/puzzle/export?mode=OVERLAP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String nonOverlap =
        mvc.perform(
                post("/api/puzzle/export?mode=NON_OVERLAP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.junit.jupiter.api.Assertions.assertNotEquals(overlap, nonOverlap);
    org.junit.jupiter.api.Assertions.assertTrue(
        overlap.length() > nonOverlap.length(),
        "overlapping export duplicates shared edges and must be larger");
  }

  @Test
  void rejectsInvalidGridSize() throws Exception {
    String invalid = VALID.replace("\"ncols\":8", "\"ncols\":1");
    mvc.perform(
            post("/api/puzzle/generate").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
  }
}
