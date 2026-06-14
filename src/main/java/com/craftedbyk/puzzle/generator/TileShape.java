package com.craftedbyk.puzzle.generator;

/** Arc rendering shape; codes match the reference arc_shape values. */
public enum TileShape {
  CIRCULAR(0),
  SQUARE(1),
  OCTAGONAL(2);

  private final int code;

  TileShape(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }
}
