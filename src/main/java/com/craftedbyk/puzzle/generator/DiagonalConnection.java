package com.craftedbyk.puzzle.generator;

/**
 * A diagonal join between two tiles. Port of the reference DiagonalConnection. Quadrants (direction
 * of p2 relative to p1): 0 = up-right, 1 = up-left, 2 = down-left, 3 = down-right.
 */
public final class DiagonalConnection {

  private final TileNode p1;
  private final TileNode p2;
  private final boolean p2Taken;
  private final int slope;
  private final int quad;
  private final int cellX;
  private final int cellY;

  public DiagonalConnection(TileNode p1, TileNode p2, boolean p2Taken) {
    this.p1 = p1;
    this.p2 = p2;
    this.p2Taken = p2Taken;
    this.slope = (p2.y - p1.y) / (p2.x - p1.x);
    this.cellX = Math.min(p2.x, p1.x);
    this.cellY = Math.min(p2.y, p1.y);
    if (this.slope > 0) {
      this.quad = (p2.y > p1.y) ? 3 : 1;
    } else {
      this.quad = (p2.y > p1.y) ? 2 : 0;
    }
  }

  public boolean eq(DiagonalConnection other) {
    return (this == other)
        || (this.cellX == other.cellX
            && this.cellY == other.cellY
            && this.slope == other.slope
            && this.p2Taken == other.p2Taken);
  }

  public static DiagonalConnection fromPointAndQuad(TileNode p1, int quadrant, boolean p2Taken) {
    TileNode p2;
    switch (quadrant) {
      case 0:
        p2 = new TileNode(p1.x + 1, p1.y - 1);
        break;
      case 1:
        p2 = new TileNode(p1.x - 1, p1.y - 1);
        break;
      case 2:
        p2 = new TileNode(p1.x - 1, p1.y + 1);
        break;
      case 3:
        p2 = new TileNode(p1.x + 1, p1.y + 1);
        break;
      default:
        throw new IllegalArgumentException("quadrant " + quadrant);
    }
    return new DiagonalConnection(p1, p2, p2Taken);
  }

  public int slope() {
    return slope;
  }

  public int quad() {
    return quad;
  }

  public TileNode p1() {
    return p1;
  }

  public TileNode p2() {
    return p2;
  }

  public int cellX() {
    return cellX;
  }

  public int cellY() {
    return cellY;
  }

  public boolean p2Taken() {
    return p2Taken;
  }
}
