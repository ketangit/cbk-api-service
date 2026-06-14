package com.craftedbyk.puzzle.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Global CORS policy.
 *
 * <p>Allows the production site, Firebase Hosting preview channels, and the local Next.js dev
 * server to call the API. Patterns are used (not exact origins) so the dynamic per-PR preview
 * subdomains are covered; {@code allowedOriginPatterns} supports credentials safely. Adjust {@code
 * ALLOWED_ORIGIN_PATTERNS} when adding new front-end origins.
 */
@Configuration
public class CorsConfig {

  private static final List<String> ALLOWED_ORIGIN_PATTERNS =
      List.of(
          "https://craftedbyk.com",
          // Firebase default live domain
          "https://craftedbyk-prod.firebaseapp.com",
          // Live (craftedbyk-prod.web.app) + preview channels
          // (craftedbyk-prod--<channel>-<hash>.web.app)
          "https://craftedbyk-prod*.web.app",
          "http://localhost:3000");

  private static final List<String> ALLOWED_METHODS =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
    config.setAllowedMethods(ALLOWED_METHODS);
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
