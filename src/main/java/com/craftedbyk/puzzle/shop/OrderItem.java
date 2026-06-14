package com.craftedbyk.puzzle.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long productId; // null for custom puzzles
  private String name;
  private String material;
  private int quantity;
  private long unitPriceCents;

  @Column(length = 2000)
  private String customSpecJson; // generator spec for custom puzzles

  protected OrderItem() {}

  public OrderItem(
      Long productId,
      String name,
      String material,
      int quantity,
      long unitPriceCents,
      String customSpecJson) {
    this.productId = productId;
    this.name = name;
    this.material = material;
    this.quantity = quantity;
    this.unitPriceCents = unitPriceCents;
    this.customSpecJson = customSpecJson;
  }

  public Long getId() {
    return id;
  }

  public Long getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public String getMaterial() {
    return material;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getUnitPriceCents() {
    return unitPriceCents;
  }

  public String getCustomSpecJson() {
    return customSpecJson;
  }
}
