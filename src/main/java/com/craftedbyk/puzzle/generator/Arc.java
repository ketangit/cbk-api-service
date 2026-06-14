package com.craftedbyk.puzzle.generator;

/**
 * Quarter-circle arc segment around a tile centre. Port of the reference Arc. Coordinates are
 * physical millimetres.
 */
public final class Arc {

  private static final double TAN225 = 0.4142135623730950488016887242097;

  private final double cpx;
  private final double cpy;
  private final int quad;
  private final double rad;
  private final int sign;
  private final double spx, spy;
  private final double epx, epy;

  public Arc(int gcpX, int gcpY, double rad, double offs, int quad, int sign) {
    this.cpx = gcpX * 2 * rad + rad + offs;
    this.cpy = gcpY * 2 * rad + rad + offs;
    this.quad = quad;
    this.rad = rad;
    this.sign = sign;
    double pax, pay, pbx, pby;
    switch (quad) {
      case 0:
        pax = cpx + rad;
        pay = cpy;
        pbx = cpx;
        pby = cpy - rad;
        break;
      case 1:
        pax = cpx;
        pay = cpy - rad;
        pbx = cpx - rad;
        pby = cpy;
        break;
      case 2:
        pax = cpx - rad;
        pay = cpy;
        pbx = cpx;
        pby = cpy + rad;
        break;
      case 3:
        pax = cpx;
        pay = cpy + rad;
        pbx = cpx + rad;
        pby = cpy;
        break;
      default:
        throw new IllegalArgumentException("quad " + quad);
    }
    if (sign == 0) {
      spx = pax;
      spy = pay;
      epx = pbx;
      epy = pby;
    } else {
      spx = pbx;
      spy = pby;
      epx = pax;
      epy = pay;
    }
  }

  /** SVG path fragment for the given tile shape; format matches reference. */
  public String svg(TileShape shape) {
    switch (shape) {
      case CIRCULAR:
        return "A "
            + Svg.fmt(rad)
            + " "
            + Svg.fmt(rad)
            + " 0 0,"
            + sign
            + " "
            + Svg.fmt(epx)
            + " "
            + Svg.fmt(epy)
            + " ";
      case SQUARE:
        return "L " + Svg.fmt(epx) + " " + Svg.fmt(epy) + " ";
      case OCTAGONAL:
        double hlen = rad * TAN225;
        double sx = spx, sy = spy, ex = epx, ey = epy;
        if (sign == 1) {
          sx = epx;
          sy = epy;
          ex = spx;
          ey = spy;
        }
        double m1x, m1y, m2x, m2y;
        switch (quad) {
          case 0:
            m1x = sx;
            m1y = sy - hlen;
            m2x = ex + hlen;
            m2y = ey;
            break;
          case 1:
            m1x = sx - hlen;
            m1y = sy;
            m2x = ex;
            m2y = ey - hlen;
            break;
          case 2:
            m1x = sx;
            m1y = sy + hlen;
            m2x = ex - hlen;
            m2y = ey;
            break;
          default:
            m1x = sx + hlen;
            m1y = sy;
            m2x = ex;
            m2y = ey + hlen;
            break;
        }
        if (sign == 1) {
          return "L "
              + Svg.fmt(m2x)
              + " "
              + Svg.fmt(m2y)
              + " L "
              + Svg.fmt(m1x)
              + " "
              + Svg.fmt(m1y)
              + " L "
              + Svg.fmt(epx)
              + " "
              + Svg.fmt(epy)
              + " ";
        }
        return "L "
            + Svg.fmt(m1x)
            + " "
            + Svg.fmt(m1y)
            + " L "
            + Svg.fmt(m2x)
            + " "
            + Svg.fmt(m2y)
            + " L "
            + Svg.fmt(epx)
            + " "
            + Svg.fmt(epy)
            + " ";
      default:
        throw new IllegalArgumentException("shape " + shape);
    }
  }

  /** Reference equality: same centre point and quadrant (direction ignored). */
  public boolean eq(Arc a) {
    return quad == a.quad && cpx == a.cpx && cpy == a.cpy;
  }

  /** Hashable identity key matching {@link #eq}. */
  public String key() {
    return quad + "|" + cpx + "|" + cpy;
  }

  public double spx() {
    return spx;
  }

  public double spy() {
    return spy;
  }

  public double epx() {
    return epx;
  }

  public double epy() {
    return epy;
  }

  public boolean spEquals(double x, double y) {
    return spx == x && spy == y;
  }
}
