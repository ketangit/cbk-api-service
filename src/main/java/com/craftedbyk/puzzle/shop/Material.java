package com.craftedbyk.puzzle.shop;

/** Available cutting materials and their price multipliers. */
public enum Material {
  BIRCH_PLY("Birch plywood 3mm", 1.00),
  WALNUT("Walnut hardwood 4mm", 1.60),
  ACRYLIC_CLEAR("Clear acrylic 3mm", 1.35),
  ACRYLIC_BLACK("Black acrylic 3mm", 1.35);

  private final String label;
  private final double priceFactor;

  Material(String label, double priceFactor) {
    this.label = label;
    this.priceFactor = priceFactor;
  }

  public String label() {
    return label;
  }

  public double priceFactor() {
    return priceFactor;
  }
}
