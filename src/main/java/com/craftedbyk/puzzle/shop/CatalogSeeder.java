package com.craftedbyk.puzzle.shop;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds the in-memory catalog on startup. */
@Configuration
public class CatalogSeeder {

  @Bean
  CommandLineRunner seedCatalog(ProductRepository products) {
    return args -> {
      if (products.count() > 0) {
        return;
      }
      products.saveAll(
          List.of(
              new Product(
                  1L,
                  "dragon-classic",
                  "Dragon Classic",
                  "Our signature 240x240mm birch puzzle. Dragon-curve texture, around 30 winding pieces.",
                  Material.BIRCH_PLY,
                  4900,
                  4242,
                  19,
                  19,
                  6.0,
                  6.0,
                  4.0,
                  4,
                  20,
                  "CIRCULAR"),
              new Product(
                  2L,
                  "walnut-labyrinth",
                  "Walnut Labyrinth",
                  "300x180mm walnut hardwood with large meandering pieces. A slow, satisfying solve.",
                  Material.WALNUT,
                  8900,
                  777,
                  24,
                  14,
                  6.0,
                  6.0,
                  4.0,
                  8,
                  40,
                  "CIRCULAR"),
              new Product(
                  3L,
                  "acrylic-aurora",
                  "Acrylic Aurora",
                  "216x216mm clear acrylic, octagonal tile geometry. Catches the light beautifully.",
                  Material.ACRYLIC_CLEAR,
                  6500,
                  1337,
                  17,
                  17,
                  6.0,
                  6.0,
                  4.0,
                  4,
                  16,
                  "OCTAGONAL"),
              new Product(
                  4L,
                  "pocket-fractal",
                  "Pocket Fractal",
                  "132x132mm birch mini-puzzle. Small grid, fiendish pieces — a desk toy with teeth.",
                  Material.BIRCH_PLY,
                  2900,
                  99,
                  10,
                  10,
                  6.0,
                  6.0,
                  4.0,
                  4,
                  12,
                  "CIRCULAR")));
    };
  }
}
