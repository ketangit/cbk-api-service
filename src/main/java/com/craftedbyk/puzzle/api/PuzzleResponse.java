package com.craftedbyk.puzzle.api;

import java.util.List;
import java.util.Map;

/** Generation result: geometry for previews plus per-material price quotes. */
public record PuzzleResponse(
    double seedUsed,
    double widthMm,
    double heightMm,
    int pieceCount,
    List<String> piecePaths,
    String framePath,
    Map<String, Long> priceCentsByMaterial) {}
