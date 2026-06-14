package com.craftedbyk.puzzle.generator;

/** Number formatting helpers for SVG path data. */
final class Svg {

  private Svg() {}

  /**
   * Formats a coordinate the way JavaScript string concatenation does for integral values ("30",
   * not "30.0"); non-integral values are limited to 6 decimals (sub-micrometre at mm units) with
   * trailing zeros stripped.
   */
  static String fmt(double v) {
    if (v == Math.rint(v) && !Double.isInfinite(v)) {
      return Long.toString((long) v);
    }
    String s = String.format(java.util.Locale.ROOT, "%.6f", v);
    int end = s.length();
    while (end > 0 && s.charAt(end - 1) == '0') end--;
    if (end > 0 && s.charAt(end - 1) == '.') end--;
    return s.substring(0, end);
  }
}
