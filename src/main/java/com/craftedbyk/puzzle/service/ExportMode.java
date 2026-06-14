package com.craftedbyk.puzzle.service;

/** SVG export variants. */
public enum ExportMode {
  /** Pieces individually contoured (overlapping cuts) — for CNC milling. */
  OVERLAP,
  /** Every edge once — laser-cut ready. */
  NON_OVERLAP,
  /** Every edge once, single path element — for picky controllers (Trotec). */
  NON_OVERLAP_SINGLE_PATH,
  /** Filled colored pieces — box cover / solution sheet. */
  COLORED
}
