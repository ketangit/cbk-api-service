package com.craftedbyk.puzzle.shop;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class CustomerOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Unguessable public handle used as the order-lookup key. Sequential {@code id} is never exposed
   * as the access key, preventing PII enumeration (IDOR).
   */
  @Column(unique = true, nullable = false, updatable = false)
  private String publicRef;

  /**
   * Firebase Auth uid of the user who placed the order, when present. Nullable for legacy/anonymous
   * paths; never serialized to clients.
   */
  private String uid;

  private Instant createdAt;
  private String customerName;
  private String email;
  private String addressLine;
  private String city;
  private String postalCode;
  private String country;
  private String status;
  private long totalCents;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  protected CustomerOrder() {}

  public CustomerOrder(
      String customerName,
      String email,
      String addressLine,
      String city,
      String postalCode,
      String country) {
    this.publicRef = UUID.randomUUID().toString();
    this.createdAt = Instant.now();
    this.customerName = customerName;
    this.email = email;
    this.addressLine = addressLine;
    this.city = city;
    this.postalCode = postalCode;
    this.country = country;
    // No payment provider wired yet: an order starts unpaid. It must NOT be
    // auto-fulfilled — a real payment integration calls markPaid() only after
    // confirming payment. Default-safe so a prod deploy can't ship free goods.
    this.status = "PENDING_PAYMENT";
  }

  /** Transition to PAID. Call only after a payment provider confirms payment. */
  public void markPaid() {
    this.status = "PAID";
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getUid() {
    return uid;
  }

  public void addItem(OrderItem item) {
    items.add(item);
  }

  public void setTotalCents(long totalCents) {
    this.totalCents = totalCents;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public Long getId() {
    return id;
  }

  public String getPublicRef() {
    return publicRef;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getEmail() {
    return email;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getCity() {
    return city;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public String getStatus() {
    return status;
  }

  public long getTotalCents() {
    return totalCents;
  }

  public List<OrderItem> getItems() {
    return items;
  }
}
