package com.craftedbyk.puzzle.generator;

/**
 * A grid tile taking part in piece construction. Mirrors the reference Tile class including its
 * mutable "haspossibleconnections" flag, whose state intentionally survives between
 * possibleconnections() calls.
 */
public final class TileNode {

  public final int x;
  public final int y;
  public boolean hasConnections = true;

  public TileNode(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public boolean eq(TileNode p) {
    return this.x == p.x && this.y == p.y;
  }
}
