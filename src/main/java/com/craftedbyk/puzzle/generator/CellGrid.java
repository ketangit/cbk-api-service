package com.craftedbyk.puzzle.generator;

/**
 * Tile/cell occupancy grid. Port of the reference CellGrid. The reference constructor was called as
 * new CellGrid(ncols, nrows); here width = ncols and height = nrows to avoid the reference naming
 * confusion. Indexing is y * width + x, exactly as in the original.
 */
public final class CellGrid {

  private final int width;
  private final int height;
  private final boolean[] visited;
  private final boolean[] cellmap;
  private int nUnvisited;

  public CellGrid(int width, int height) {
    this.width = width;
    this.height = height;
    this.visited = new boolean[width * height];
    // Reference allocated (w-1)*(h-1) but relied on JS auto-growth when
    // indexing with y*width+x; allocate the full range here.
    this.cellmap = new boolean[width * height];
    this.nUnvisited = width * height;
  }

  /** Picks a random unvisited tile, identical selection order to reference. */
  public TileNode randomEmptyTile(RandomSource rng) {
    int n = 0;
    for (int i = 0; i < visited.length; i++) {
      if (!visited[i]) n++;
    }
    int[] empty = new int[n];
    int k = 0;
    for (int i = 0; i < visited.length; i++) {
      if (!visited[i]) empty[k++] = i;
    }
    int index = empty[(int) Math.floor(rng.uniform(0, n))];
    int y = index / width;
    int x = index % width;
    return new TileNode(x, y);
  }

  public void reset() {
    java.util.Arrays.fill(visited, false);
    java.util.Arrays.fill(cellmap, false);
    nUnvisited = width * height;
  }

  public boolean isTileValid(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  public boolean isTileVisited(TileNode v) {
    return visited[v.y * width + v.x];
  }

  public boolean isCellEmpty(int cx, int cy) {
    return !cellmap[cy * width + cx];
  }

  public void visitTile(TileNode v) {
    int i = v.y * width + v.x;
    if (!visited[i]) {
      visited[i] = true;
      nUnvisited--;
    }
  }

  public void occupyCell(int cx, int cy) {
    cellmap[cy * width + cx] = true;
  }

  public void liberateCell(int cx, int cy) {
    cellmap[cy * width + cx] = false;
  }

  public int nUnvisited() {
    return nUnvisited;
  }
}
