package com.craftedbyk.puzzle.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class CircleFractalJigsawTest {

  private CircleFractalJigsaw generate(double seed, int ncols, int nrows, int minP, int maxP) {
    CircleFractalJigsaw jig =
        new CircleFractalJigsaw(ncols, nrows, minP, maxP, new SinRandom(seed));
    jig.generateAll();
    return jig;
  }

  @Test
  void matchesReferenceGoldenOverlapSvg() throws Exception {
    CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
    String svg = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4);
    String golden =
        new String(
                getClass().getResourceAsStream("/golden-overlap-seed42.svg").readAllBytes(),
                StandardCharsets.UTF_8)
            .trim();
    assertEquals(golden, svg, "overlap export must match the reference generator byte-for-byte");
  }

  @Test
  void matchesReferenceGoldenNonOverlapSvg() throws Exception {
    CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
    String svg = jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4);
    String golden =
        new String(
                getClass().getResourceAsStream("/golden-nooverlap-seed42.svg").readAllBytes(),
                StandardCharsets.UTF_8)
            .trim();
    assertEquals(
        golden, svg, "non-overlap export must match the reference generator byte-for-byte");
  }

  @Test
  void sameSeedIsDeterministic() {
    String a = generate(1234, 12, 10, 4, 20).exportSvg(8, 5, TileShape.CIRCULAR, 3);
    String b = generate(1234, 12, 10, 4, 20).exportSvg(8, 5, TileShape.CIRCULAR, 3);
    assertEquals(a, b);
  }

  @Test
  void differentSeedsProduceDifferentPuzzles() {
    String a = generate(1, 10, 10, 4, 25).exportSvg(6, 6, TileShape.CIRCULAR, 4);
    String b = generate(2, 10, 10, 4, 25).exportSvg(6, 6, TileShape.CIRCULAR, 4);
    assertNotEquals(a, b);
  }

  @Test
  void everyPieceRespectsMinimumSize() {
    CircleFractalJigsaw jig = generate(99, 12, 12, 4, 30);
    assertTrue(jig.npieces() > 1);
    for (int tileCount : jig.pieceTileCounts()) {
      assertTrue(tileCount >= 4, "piece below minimum size: " + tileCount);
    }
    for (String path : jig.multipaths(6, 6, TileShape.CIRCULAR)) {
      assertTrue(path.startsWith("M") && path.endsWith("Z"), "piece path must be closed");
    }
  }

  @Test
  void allFourExportsAreWellFormedXml() throws Exception {
    CircleFractalJigsaw jig = generate(7, 10, 10, 4, 25);
    double coloringSeed = 7 + 1000; // arbitrary valid coloring seed
    String[] svgs = {
      jig.exportSvg(6, 6, TileShape.OCTAGONAL, 4),
      jig.exportSvgNoOverlap(6, 6, TileShape.SQUARE, 4),
      jig.exportSvgNoOverlapSinglePath(6, 6, TileShape.CIRCULAR, 4),
      jig.exportSvgColored(6, 6, TileShape.CIRCULAR, 4, coloringSeed),
    };
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    for (String svg : svgs) {
      Document doc =
          factory
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
      assertEquals("svg", doc.getDocumentElement().getLocalName());
    }
  }

  @Test
  void nonOverlapHasFewerArcsThanOverlap() {
    CircleFractalJigsaw jig = generate(31337, 10, 14, 4, 50);
    String overlap = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4);
    String noOverlap = jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4);
    assertTrue(
        countArcs(noOverlap) < countArcs(overlap),
        "deduplicated export must contain fewer arc segments");
  }

  @Test
  void singlePathExportContainsExactlyTwoPathElements() {
    CircleFractalJigsaw jig = generate(555, 9, 9, 4, 18);
    String svg = jig.exportSvgNoOverlapSinglePath(6, 6, TileShape.CIRCULAR, 4);
    assertEquals(2, count(svg, "<path"), "puzzle path + frame path");
  }

  @Test
  void coloredExportEmitsValidSixDigitHexColors() {
    CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
    String svg = jig.exportSvgColored(6, 6, TileShape.CIRCULAR, 4, 4242);
    Matcher m = Pattern.compile("fill=\"#([0-9a-fA-F]+)\"").matcher(svg);
    int colored = 0;
    while (m.find()) {
      assertEquals(6, m.group(1).length(), "hex colors must be zero-padded");
      colored++;
    }
    assertEquals(jig.npieces(), colored);
  }

  @Test
  void pieceCountMatchesReferenceForKnownSeeds() {
    assertEquals(5, generate(42, 8, 8, 4, 12).npieces());
  }

  @Test
  void singlePieceModeProducesOnePiece() {
    // min = max = half the tile count => "lampshade" single-piece mode
    CircleFractalJigsaw jig = generate(50, 30, 20, 300, 300);
    assertEquals(1, jig.npieces());
  }

  private static int countArcs(String svg) {
    return count(svg, "A ");
  }

  private static int count(String haystack, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = haystack.indexOf(needle, idx)) >= 0) {
      count++;
      idx += needle.length();
    }
    return count;
  }
}
