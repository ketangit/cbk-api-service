package com.craftedbyk.puzzle.generator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Procedural "fractal" jigsaw generator. Faithful port of the reference CircleFractalJigsaw: tiles
 * on a square grid are joined diagonally to form pieces, then rendered as chains of quarter-circle
 * arcs (or octagonal / square approximations).
 */
public final class CircleFractalJigsaw {

  private final int ncols;
  private final int nrows;
  private final int minPieceLen;
  private final int maxPieceLen;
  private final CellGrid grid;
  private final RandomSource rng;
  private final List<List<DiagonalConnection>> pieces = new ArrayList<List<DiagonalConnection>>();

  public CircleFractalJigsaw(
      int ncols, int nrows, int minPieceLen, int maxPieceLen, RandomSource rng) {
    this.ncols = ncols;
    this.nrows = nrows;
    this.minPieceLen = minPieceLen;
    this.maxPieceLen = maxPieceLen;
    this.grid = new CellGrid(ncols, nrows);
    this.rng = rng;
  }

  /** Runs the full reference pipeline: generate, then fill holes. */
  public void generateAll() {
    while (grid.nUnvisited() > 0) {
      createPiece();
    }
    regenerateGrid();
    while (fillHoles(false)) {
      // repeat until no more holes can be filled strictly
    }
    fillHoles(true);
  }

  private List<DiagonalConnection> possibleConnections(
      List<TileNode> myTiles, boolean allowPartials) {
    List<DiagonalConnection> pcs = new ArrayList<DiagonalConnection>();
    int[][] neighbors = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    for (TileNode v : myTiles) {
      if (v.hasConnections || allowPartials) {
        v.hasConnections = false;
        for (int[] n : neighbors) {
          int cx = v.x + n[0];
          int cy = v.y + n[1];
          if (grid.isTileValid(cx, cy) && findTile(myTiles, cx, cy) == null) {
            TileNode cpt = new TileNode(cx, cy);
            boolean visited = grid.isTileVisited(cpt);
            DiagonalConnection dc = new DiagonalConnection(v, cpt, !visited);
            if (grid.isCellEmpty(dc.cellX(), dc.cellY())) {
              if (allowPartials || !visited) {
                pcs.add(dc);
                v.hasConnections = true;
              }
            }
          }
        }
      }
    }
    return pcs;
  }

  private static TileNode findTile(List<TileNode> tiles, int x, int y) {
    for (TileNode t : tiles) {
      if (t.x == x && t.y == y) return t;
    }
    return null;
  }

  private void createPiece() {
    List<TileNode> myTiles = new ArrayList<TileNode>();
    List<DiagonalConnection> myConnections = new ArrayList<DiagonalConnection>();
    int targetPieceLen = (int) Math.round(rng.uniform(minPieceLen, maxPieceLen));

    TileNode vi = grid.randomEmptyTile(rng);
    myTiles.add(vi);
    grid.visitTile(vi);

    while (grid.nUnvisited() > 0 && myTiles.size() < targetPieceLen) {
      List<DiagonalConnection> pcs = possibleConnections(myTiles, false);
      if (pcs.isEmpty()) {
        break;
      }
      DiagonalConnection chosen = pcs.get((int) Math.floor(rng.uniform(0, pcs.size())));
      myConnections.add(chosen);
      myTiles.add(chosen.p2());
      grid.occupyCell(chosen.cellX(), chosen.cellY());
      grid.visitTile(chosen.p2());
    }

    if (myTiles.size() >= minPieceLen) {
      pieces.add(myConnections);
    } else {
      for (DiagonalConnection c : myConnections) {
        grid.liberateCell(c.cellX(), c.cellY());
      }
    }
  }

  private boolean fillHoles(boolean allowPartials) {
    boolean filled = false;
    pieces.sort(
        new java.util.Comparator<List<DiagonalConnection>>() {
          @Override
          public int compare(List<DiagonalConnection> a, List<DiagonalConnection> b) {
            return a.size() - b.size();
          }
        });
    for (List<DiagonalConnection> p : pieces) {
      List<TileNode> tiles = new ArrayList<TileNode>();
      tiles.add(p.get(0).p1());
      for (DiagonalConnection con : p) {
        tiles.add(con.p2());
      }
      // Reference used Array.forEach, which does not visit elements
      // appended during iteration: iterate over the initial range only.
      int initialCount = tiles.size();
      for (int i = 0; i < initialCount; i++) {
        TileNode v = tiles.get(i);
        List<DiagonalConnection> pcs =
            possibleConnections(java.util.Collections.singletonList(v), allowPartials);
        if (!pcs.isEmpty()) {
          for (DiagonalConnection pc : pcs) {
            if (findTile(tiles, pc.p2().x, pc.p2().y) != null) {
              continue;
            }
            p.add(pc);
            tiles.add(pc.p2());
            filled = true;
            grid.occupyCell(pc.cellX(), pc.cellY());
            grid.visitTile(pc.p2());
          }
        }
      }
    }
    return filled;
  }

  private void regenerateGrid() {
    grid.reset();
    for (List<DiagonalConnection> p : pieces) {
      for (DiagonalConnection c : p) {
        if (!grid.isTileVisited(c.p1())) {
          grid.visitTile(c.p1());
        }
        if (c.p2Taken() && !grid.isTileVisited(c.p2())) {
          grid.visitTile(c.p2());
        }
        grid.occupyCell(c.cellX(), c.cellY());
      }
    }
  }

  static void addArcs(
      DiagonalConnection con,
      List<DiagonalConnection> connections,
      List<Arc> arcs,
      double rad,
      double frame,
      boolean first) {
    Arc newArc;
    switch (con.quad()) {
      case 0:
        newArc = new Arc(con.p1().x + 1, con.p1().y, rad, frame, 1, 1);
        break;
      case 1:
        newArc = new Arc(con.p1().x, con.p1().y - 1, rad, frame, 2, 1);
        break;
      case 2:
        newArc = new Arc(con.p1().x - 1, con.p1().y, rad, frame, 3, 1);
        break;
      default:
        newArc = new Arc(con.p1().x, con.p1().y + 1, rad, frame, 0, 1);
        break;
    }
    arcs.add(newArc);

    if (con.p2Taken()) {
      int[] p2quads = {(con.quad() + 3) % 4, (con.quad() + 4) % 4, (con.quad() + 5) % 4};
      for (int q : p2quads) {
        DiagonalConnection pct = DiagonalConnection.fromPointAndQuad(con.p2(), q, true);
        DiagonalConnection pcnt = DiagonalConnection.fromPointAndQuad(con.p2(), q, false);
        DiagonalConnection foundTaken = findConnection(connections, pct);
        if (foundTaken != null) {
          addArcs(foundTaken, connections, arcs, rad, frame, false);
        } else {
          DiagonalConnection foundNotTaken = findConnection(connections, pcnt);
          if (foundNotTaken != null) {
            addArcs(foundNotTaken, connections, arcs, rad, frame, false);
          } else {
            arcs.add(new Arc(con.p2().x, con.p2().y, rad, frame, q, 0));
          }
        }
      }
    } else {
      arcs.add(new Arc(con.p2().x, con.p2().y, rad, frame, (con.quad() + 2) % 4, 1));
    }

    switch (con.quad()) {
      case 0:
        newArc = new Arc(con.p1().x, con.p1().y - 1, rad, frame, 3, 1);
        break;
      case 1:
        newArc = new Arc(con.p1().x - 1, con.p1().y, rad, frame, 0, 1);
        break;
      case 2:
        newArc = new Arc(con.p1().x, con.p1().y + 1, rad, frame, 1, 1);
        break;
      default:
        newArc = new Arc(con.p1().x + 1, con.p1().y, rad, frame, 2, 1);
        break;
    }
    arcs.add(newArc);

    if (first) {
      int[] p1quads = {(con.quad() + 1) % 4, (con.quad() + 2) % 4, (con.quad() + 3) % 4};
      for (int q : p1quads) {
        DiagonalConnection pct = DiagonalConnection.fromPointAndQuad(con.p1(), q, true);
        DiagonalConnection pcnt = DiagonalConnection.fromPointAndQuad(con.p1(), q, false);
        DiagonalConnection foundTaken = findConnection(connections, pct);
        if (foundTaken != null) {
          addArcs(foundTaken, connections, arcs, rad, frame, false);
        } else {
          DiagonalConnection foundNotTaken = findConnection(connections, pcnt);
          if (foundNotTaken != null) {
            addArcs(foundNotTaken, connections, arcs, rad, frame, false);
          } else {
            arcs.add(new Arc(con.p1().x, con.p1().y, rad, frame, q, 0));
          }
        }
      }
    }
  }

  private static DiagonalConnection findConnection(
      List<DiagonalConnection> connections, DiagonalConnection target) {
    for (DiagonalConnection c : connections) {
      if (c.eq(target)) return c;
    }
    return null;
  }

  public int npieces() {
    return pieces.size();
  }

  /** Distinct tile count per piece (for stats, pricing and tests). */
  public List<Integer> pieceTileCounts() {
    List<Integer> counts = new ArrayList<Integer>();
    for (List<DiagonalConnection> p : pieces) {
      Set<String> tiles = new LinkedHashSet<String>();
      for (DiagonalConnection c : p) {
        tiles.add(c.p1().x + "," + c.p1().y);
        tiles.add(c.p2().x + "," + c.p2().y);
      }
      counts.add(tiles.size());
    }
    return counts;
  }

  /** Closed outline path ("d" attribute) for every piece. */
  public List<String> multipaths(double frame, double rad, TileShape shape) {
    List<String> paths = new ArrayList<String>();
    for (List<DiagonalConnection> p : pieces) {
      List<Arc> arcs = new ArrayList<Arc>();
      addArcs(p.get(0), p, arcs, rad, frame, true);
      StringBuilder d = new StringBuilder();
      d.append("M")
          .append(Svg.fmt(arcs.get(0).spx()))
          .append(",")
          .append(Svg.fmt(arcs.get(0).spy()))
          .append(" ");
      for (Arc a : arcs) {
        d.append(a.svg(shape));
      }
      d.append("Z");
      paths.add(d.toString());
    }
    return paths;
  }

  private String svgHeader(double frame, double rad) {
    double width = ncols * 2 * rad + 2 * frame;
    double height = nrows * 2 * rad + 2 * frame;
    return "<?xml version=\"1.0\" encoding=\"utf-8\" ?><svg baseProfile=\"full\" height=\""
        + Svg.fmt(height)
        + "mm\" version=\"1.1\" viewBox=\"0 0 "
        + Svg.fmt(width)
        + " "
        + Svg.fmt(height)
        + "\" width=\""
        + Svg.fmt(width)
        + "mm\" xmlns=\"http://www.w3.org/2000/svg\""
        + " xmlns:ev=\"http://www.w3.org/2001/xml-events\""
        + " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><defs />";
  }

  private static String framePathElement(String frameD) {
    return "<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"" + frameD + "\"></path>";
  }

  /** Rounded-rectangle frame path. Port of reference createframe(). */
  public String createFrame(double frame, double rad, double frameCorner) {
    double width = ncols * 2 * rad + 2 * frame;
    double height = nrows * 2 * rad + 2 * frame;
    StringBuilder d = new StringBuilder();
    d.append("M").append(Svg.fmt(frameCorner)).append(",0 ");
    d.append("H ").append(Svg.fmt(width - frameCorner));
    if (frameCorner > 0) {
      d.append("A ")
          .append(Svg.fmt(frameCorner))
          .append(" ")
          .append(Svg.fmt(frameCorner))
          .append(" 0 0,1 ")
          .append(Svg.fmt(width))
          .append(" ")
          .append(Svg.fmt(frameCorner))
          .append(" ");
    }
    d.append("V ").append(Svg.fmt(height - frameCorner));
    if (frameCorner > 0) {
      d.append("A ")
          .append(Svg.fmt(frameCorner))
          .append(" ")
          .append(Svg.fmt(frameCorner))
          .append(" 0 0,1 ")
          .append(Svg.fmt(width - frameCorner))
          .append(" ")
          .append(Svg.fmt(height))
          .append(" ");
    }
    d.append("H ").append(Svg.fmt(frameCorner));
    if (frameCorner > 0) {
      d.append("A ")
          .append(Svg.fmt(frameCorner))
          .append(" ")
          .append(Svg.fmt(frameCorner))
          .append(" 0 0,1 0 ")
          .append(Svg.fmt(height - frameCorner))
          .append(" ");
    }
    d.append("V ").append(Svg.fmt(frameCorner));
    if (frameCorner > 0) {
      d.append("A ")
          .append(Svg.fmt(frameCorner))
          .append(" ")
          .append(Svg.fmt(frameCorner))
          .append(" 0 0,1 ")
          .append(Svg.fmt(frameCorner))
          .append(" 0 ");
    }
    d.append("Z");
    return d.toString();
  }

  /**
   * Overlapping export: every piece is an individually contoured closed path (shared edges drawn
   * twice). Good for CNC milling and for cutting replacement pieces.
   */
  public String exportSvg(double frame, double rad, TileShape shape, double frameCorner) {
    StringBuilder data = new StringBuilder(svgHeader(frame, rad));
    for (List<DiagonalConnection> p : pieces) {
      List<Arc> arcs = new ArrayList<Arc>();
      addArcs(p.get(0), p, arcs, rad, frame, true);
      data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
          .append(Svg.fmt(arcs.get(0).spx()))
          .append(",")
          .append(Svg.fmt(arcs.get(0).spy()))
          .append(" ");
      for (Arc a : arcs) {
        data.append(a.svg(shape));
      }
      data.append("Z\"></path>");
    }
    data.append(framePathElement(createFrame(frame, rad, frameCorner)));
    data.append("</svg>");
    return data.toString();
  }

  /**
   * Non-overlapping export: each shared edge appears exactly once, so the file can be laser-cut
   * directly without double-cutting.
   */
  public String exportSvgNoOverlap(double frame, double rad, TileShape shape, double frameCorner) {
    StringBuilder data = new StringBuilder(svgHeader(frame, rad));
    Set<String> allArcs = new LinkedHashSet<String>();
    for (List<DiagonalConnection> p : pieces) {
      boolean inPath = false;
      StringBuilder path = new StringBuilder();
      List<Arc> arcs = new ArrayList<Arc>();
      addArcs(p.get(0), p, arcs, rad, frame, true);
      for (Arc a : arcs) {
        if (allArcs.contains(a.key())) {
          if (inPath) {
            path.append("\"></path>");
            data.append(path);
            inPath = false;
          }
        } else {
          allArcs.add(a.key());
          if (!inPath) {
            path =
                new StringBuilder("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
                    .append(Svg.fmt(a.spx()))
                    .append(",")
                    .append(Svg.fmt(a.spy()))
                    .append(" ");
            inPath = true;
          }
          path.append(a.svg(shape));
        }
      }
      if (inPath) {
        path.append("\"></path>");
        data.append(path);
      }
    }
    data.append(framePathElement(createFrame(frame, rad, frameCorner)));
    data.append("</svg>");
    return data.toString();
  }

  /**
   * Non-overlapping export as one single SVG path element (works better with some laser
   * controllers, e.g. Trotec).
   */
  public String exportSvgNoOverlapSinglePath(
      double frame, double rad, TileShape shape, double frameCorner) {
    StringBuilder data = new StringBuilder(svgHeader(frame, rad));
    Set<String> allArcs = new LinkedHashSet<String>();
    data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"");
    double curX = -1, curY = -1;
    for (List<DiagonalConnection> p : pieces) {
      List<Arc> arcs = new ArrayList<Arc>();
      addArcs(p.get(0), p, arcs, rad, frame, true);
      for (Arc a : arcs) {
        if (!allArcs.contains(a.key())) {
          allArcs.add(a.key());
          if (!a.spEquals(curX, curY)) {
            data.append("M")
                .append(Svg.fmt(a.spx()))
                .append(",")
                .append(Svg.fmt(a.spy()))
                .append(" ");
          }
          data.append(a.svg(shape));
          curX = a.epx();
          curY = a.epy();
        }
      }
    }
    data.append("\"></path>");
    data.append(framePathElement(createFrame(frame, rad, frameCorner)));
    data.append("</svg>");
    return data.toString();
  }

  /**
   * Colored export: filled piece shapes, e.g. for the box cover / solution sheet. Colors are
   * derived from the coloring seed (reference parity), but hex values are zero-padded — the
   * reference generator could emit invalid 5-digit colors.
   */
  public String exportSvgColored(
      double frame, double rad, TileShape shape, double frameCorner, double coloringSeed) {
    StringBuilder data = new StringBuilder(svgHeader(frame, rad));
    RandomSource colorRng = new SinRandom(coloringSeed);
    for (String p : multipaths(frame, rad, shape)) {
      int color = (int) Math.floor(colorRng.uniform(0, 16777216));
      data.append("<path fill=\"#")
          .append(String.format("%06x", color))
          .append("\" stroke=\"black\" stroke-width=\"")
          .append(Svg.fmt(rad / 20.0))
          .append("\" d=\"")
          .append(p)
          .append("\"></path>");
    }
    data.append(framePathElement(createFrame(frame, rad, frameCorner)));
    data.append("</svg>");
    return data.toString();
  }
}
