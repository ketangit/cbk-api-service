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
 * <p>Allows the production site and local Next.js dev server to call the API. Origins are matched
 * exactly (no wildcards) so credentials can be supported safely. Adjust {@code ALLOWED_ORIGINS}
 * when adding new front-end origins.
 */
@Configuration
public class CorsConfig {

  private static final List<String> ALLOWED_ORIGINS =
      List.of("https://craftedbyk.com", "http://localhost:3000");

  private static final List<String> ALLOWED_METHODS =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(ALLOWED_ORIGINS);
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
