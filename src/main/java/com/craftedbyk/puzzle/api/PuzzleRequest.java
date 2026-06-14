package com.craftedbyk.puzzle.api;

import com.craftedbyk.puzzle.generator.PuzzleSpec;
import com.craftedbyk.puzzle.generator.TileShape;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Generation request; dimensions in millimetres. Double-typed fields are range-checked by the
 * PuzzleSpec constructor (Bean Validation does not support @DecimalMin on primitive doubles).
 */
public record PuzzleRequest(
    double seed,
    boolean nonDeterministic,
    @Min(2) @Max(250) int ncols,
    @Min(2) @Max(250) int nrows,
    double tileRadius,
    double frame,
    double frameCorner,
    @Min(2) @Max(62500) int minPieceSize,
    @Min(2) @Max(62500) int maxPieceSize,
    @NotNull TileShape shape) {

  public PuzzleSpec toSpec() {
    return new PuzzleSpec(
        seed,
        nonDeterministic,
        ncols,
        nrows,
        tileRadius,
        frame,
        frameCorner,
        minPieceSize,
        maxPieceSize,
        shape);
  }
}
