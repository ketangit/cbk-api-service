package com.craftedbyk.puzzle.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the statically-exported Next.js frontend from the classpath and adds SPA-style fallbacks:
 * "/designer" resolves to designer.html, unknown paths fall back to index.html. API routes are
 * untouched.
 *
 * <p>CORS is owned solely by {@link CorsConfig}'s global {@code CorsFilter} (prod origin +
 * localhost dev). No CORS mapping is declared here to avoid two competing policies.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requested = location.createRelative(resourcePath);
                if (requested.exists() && requested.isReadable()) {
                  return requested;
                }
                Resource html = location.createRelative(resourcePath + ".html");
                if (html.exists() && html.isReadable()) {
                  return html;
                }
                Resource index = location.createRelative("index.html");
                return index.exists() && index.isReadable() ? index : null;
              }
            });
  }
}
