package com.craftedbyk.puzzle.shop;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A curated, ready-to-order puzzle. Carries its generator spec so the storefront can render live
 * 2D/3D previews of the exact product.
 */
@Entity
@Table(name = "products")
public class Product {

  @Id private Long id;
  private String slug;
  private String name;
  private String description;

  @Enumerated(EnumType.STRING)
  private Material material;

  private long priceCents;

  // generator spec
  private double seed;
  private int ncols;
  private int nrows;
  private double tileRadius;
  private double frame;
  private double frameCorner;
  private int minPieceSize;
  private int maxPieceSize;
  private String shape;

  protected Product() {}

  public Product(
      Long id,
      String slug,
      String name,
      String description,
      Material material,
      long priceCents,
      double seed,
      int ncols,
      int nrows,
      double tileRadius,
      double frame,
      double frameCorner,
      int minPieceSize,
      int maxPieceSize,
      String shape) {
    this.id = id;
    this.slug = slug;
    this.name = name;
    this.description = description;
    this.material = material;
    this.priceCents = priceCents;
    this.seed = seed;
    this.ncols = ncols;
    this.nrows = nrows;
    this.tileRadius = tileRadius;
    this.frame = frame;
    this.frameCorner = frameCorner;
    this.minPieceSize = minPieceSize;
    this.maxPieceSize = maxPieceSize;
    this.shape = shape;
  }

  public Long getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Material getMaterial() {
    return material;
  }

  public long getPriceCents() {
    return priceCents;
  }

  public double getSeed() {
    return seed;
  }

  public int getNcols() {
    return ncols;
  }

  public int getNrows() {
    return nrows;
  }

  public double getTileRadius() {
    return tileRadius;
  }

  public double getFrame() {
    return frame;
  }

  public double getFrameCorner() {
    return frameCorner;
  }

  public int getMinPieceSize() {
    return minPieceSize;
  }

  public int getMaxPieceSize() {
    return maxPieceSize;
  }

  public String getShape() {
    return shape;
  }
}
