package com.craftedbyk.puzzle.service;

import com.craftedbyk.puzzle.generator.PuzzleSpec;
import com.craftedbyk.puzzle.shop.Material;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Server-side pricing for custom puzzles (cents, USD). */
@Service
public class PricingService {

  private static final long BASE_CENTS = 1200; // setup / handling
  private static final long PER_PIECE_CENTS = 35; // cutting complexity
  private static final double PER_CM2_CENTS = 5.0; // material area

  public long priceCents(PuzzleSpec spec, int pieceCount, Material material) {
    double areaCm2 = (spec.widthMm() / 10.0) * (spec.heightMm() / 10.0);
    double raw = BASE_CENTS + PER_PIECE_CENTS * (long) pieceCount + PER_CM2_CENTS * areaCm2;
    long cents = Math.round(raw * material.priceFactor());
    return Math.max(cents, 500);
  }

  public Map<Material, Long> quoteAll(PuzzleSpec spec, int pieceCount) {
    Map<Material, Long> quotes = new EnumMap<>(Material.class);
    for (Material m : Material.values()) {
      quotes.put(m, priceCents(spec, pieceCount, m));
    }
    return quotes;
  }
}
