package com.craftedbyk.puzzle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds baseline security response headers to every response (frontend HTML and API alike). There is
 * no Spring Security on the classpath, so these are set directly.
 *
 * <p>The CSP allows {@code 'unsafe-inline'} for scripts and styles because the frontend is a
 * Next.js static export ({@code output: "export"}): static exports emit inline bootstrap scripts
 * and cannot use per-request nonces, and React renders inline {@code style} attributes. This is the
 * documented tightening debt — once the app moves to a server-rendered/nonce-capable setup, drop
 * {@code 'unsafe-inline'} and switch to nonces. Everything else is locked to {@code 'self'}.
 */
@Component
@Order(0)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String CONTENT_SECURITY_POLICY =
      String.join(
          "; ",
          "default-src 'self'",
          "base-uri 'self'",
          "object-src 'none'",
          "frame-ancestors 'none'",
          "script-src 'self' 'unsafe-inline'",
          "style-src 'self' 'unsafe-inline'",
          "img-src 'self' data: blob:",
          "font-src 'self'",
          "connect-src 'self'");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
    // Honoured only over HTTPS; harmless over plain HTTP. Assumes TLS is
    // terminated at a proxy in front of the container.
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    chain.doFilter(request, response);
  }
}
