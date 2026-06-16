package com.craftedbyk.puzzle.shop;

import com.craftedbyk.puzzle.api.PuzzleRequest;
import com.craftedbyk.puzzle.config.AuthFilter;
import com.craftedbyk.puzzle.generator.PuzzleSpec;
import com.craftedbyk.puzzle.service.GeneratedPuzzle;
import com.craftedbyk.puzzle.service.PricingService;
import com.craftedbyk.puzzle.service.PuzzleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ShopController {

  private final ProductRepository products;
  private final OrderRepository orders;
  private final PuzzleService puzzleService;
  private final PricingService pricingService;
  private final ObjectMapper objectMapper;

  public ShopController(
      ProductRepository products,
      OrderRepository orders,
      PuzzleService puzzleService,
      PricingService pricingService,
      ObjectMapper objectMapper) {
    this.products = products;
    this.orders = orders;
    this.puzzleService = puzzleService;
    this.pricingService = pricingService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/products")
  public List<Product> listProducts() {
    return products.findAll();
  }

  @GetMapping("/products/{id}")
  public Product getProduct(@PathVariable Long id) {
    return products
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
  }

  // ---- orders (mock checkout: no payment provider; prices recomputed
  // server-side) ----

  public record OrderItemRequest(
      Long productId, PuzzleRequest customSpec, String material, @Min(1) @Max(50) int quantity) {}

  public record OrderRequest(
      @NotBlank String customerName,
      @NotBlank @Email String email,
      @NotBlank String addressLine,
      @NotBlank String city,
      @NotBlank String postalCode,
      @NotBlank String country,
      @NotEmpty @Size(max = 50) List<@Valid @NotNull OrderItemRequest> items) {}

  public record OrderConfirmation(String orderRef, String status, long totalCents, int itemCount) {}

  @PostMapping("/orders")
  @Transactional
  public OrderConfirmation createOrder(
      @Valid @RequestBody OrderRequest request,
      @RequestAttribute(name = AuthFilter.UID_ATTRIBUTE, required = false) String uid) {
    CustomerOrder order =
        new CustomerOrder(
            request.customerName(),
            request.email(),
            request.addressLine(),
            request.city(),
            request.postalCode(),
            request.country());
    order.setUid(uid);
    long total = 0;
    for (OrderItemRequest item : request.items()) {
      OrderItem entity;
      if (item.productId() != null) {
        Product product =
            products
                .findById(item.productId())
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "unknown product " + item.productId()));
        entity =
            new OrderItem(
                product.getId(),
                product.getName(),
                product.getMaterial().name(),
                item.quantity(),
                product.getPriceCents(),
                null);
      } else if (item.customSpec() != null) {
        Material material = parseMaterial(item.material());
        PuzzleSpec spec = item.customSpec().toSpec();
        if (spec.nonDeterministic) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "custom orders must reference the seed shown after generation");
        }
        GeneratedPuzzle puzzle = puzzleService.generate(spec);
        long unitPrice = pricingService.priceCents(spec, puzzle.jigsaw().npieces(), material);
        String specJson = writeSpecJson(item.customSpec());
        String name =
            "Custom fractal puzzle "
                + Math.round(spec.widthMm())
                + "x"
                + Math.round(spec.heightMm())
                + "mm, "
                + puzzle.jigsaw().npieces()
                + " pieces";
        entity = new OrderItem(null, name, material.name(), item.quantity(), unitPrice, specJson);
      } else {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "order item needs a productId or a customSpec");
      }
      order.addItem(entity);
      total += entity.getUnitPriceCents() * entity.getQuantity();
    }
    order.setTotalCents(total);
    CustomerOrder saved = orders.save(order);
    return new OrderConfirmation(
        saved.getPublicRef(), saved.getStatus(), saved.getTotalCents(), saved.getItems().size());
  }

  // Lookup by the unguessable publicRef (capability URL), never the sequential
  // id — prevents PII enumeration. Add auth + ownership checks before prod.
  @GetMapping("/orders/{ref}")
  public CustomerOrder getOrder(@PathVariable String ref) {
    return orders
        .findByPublicRef(ref)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
  }

  private Material parseMaterial(String material) {
    try {
      return Material.valueOf(material);
    } catch (Exception e) {
      // Don't reflect the raw user-supplied value back in the response.
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown material");
    }
  }

  private String writeSpecJson(PuzzleRequest spec) {
    try {
      return objectMapper.writeValueAsString(spec);
    } catch (Exception e) {
      throw new IllegalStateException("could not serialize spec", e);
    }
  }
}
