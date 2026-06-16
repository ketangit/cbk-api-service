package com.craftedbyk.puzzle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal in-memory, per-client token-bucket rate limiter for {@code /api/**}. CPU-bound endpoints
 * (puzzle generate/export, order creation — each runs the generator) get a tighter bucket than
 * read-only catalog/order lookups. This blunts unauthenticated resource-exhaustion DoS until a real
 * limiter (gateway, bucket4j, Redis) is in place.
 *
 * <p>Keyed on {@link HttpServletRequest#getRemoteAddr()}. Behind a proxy/load balancer this is the
 * proxy IP — derive the client IP from a trusted X-Forwarded-For header before relying on this in
 * production. The bucket maps are unbounded in distinct-IP count; acceptable for a single-instance
 * deploy, revisit if exposed to large client populations.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_NANOS = 60_000_000_000L; // 1 minute
  private static final int HEAVY_PER_MIN = 30;
  private static final int GENERAL_PER_MIN = 120;

  private final ConcurrentHashMap<String, Bucket> heavyBuckets = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (path == null || !path.startsWith("/api/")) {
      chain.doFilter(request, response);
      return;
    }

    boolean heavy =
        path.startsWith("/api/puzzle/")
            || ("POST".equalsIgnoreCase(request.getMethod()) && path.equals("/api/orders"));
    int capacity = heavy ? HEAVY_PER_MIN : GENERAL_PER_MIN;
    ConcurrentHashMap<String, Bucket> buckets = heavy ? heavyBuckets : generalBuckets;
    Bucket bucket = buckets.computeIfAbsent(clientIp(request), k -> new Bucket(capacity));

    if (!bucket.tryConsume(capacity)) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"rate limit exceeded, slow down\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  /**
   * Best-effort client IP for keying buckets. Behind Cloud Run the originating client is the first
   * entry of {@code X-Forwarded-For}; falls back to the socket address (the Google front-end proxy,
   * which collapses everyone into one bucket). Note XFF is client-supplied and spoofable without a
   * trusted-proxy hop count — acceptable for this blunt limiter; spoof-resistant per-IP enforcement
   * belongs at the edge (Cloud Armor), out of scope on the free tier.
   */
  private static String clientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int comma = xff.indexOf(',');
      String first = (comma >= 0 ? xff.substring(0, comma) : xff).trim();
      if (!first.isEmpty()) {
        return first;
      }
    }
    return request.getRemoteAddr();
  }

  /** Lazily-refilled token bucket; one token per request, refilled over the window. */
  private static final class Bucket {
    private double tokens;
    private long lastRefillNanos;

    Bucket(int capacity) {
      this.tokens = capacity;
      this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryConsume(int capacity) {
      long now = System.nanoTime();
      double refill = (now - lastRefillNanos) / (double) WINDOW_NANOS * capacity;
      if (refill > 0) {
        tokens = Math.min(capacity, tokens + refill);
        lastRefillNanos = now;
      }
      if (tokens >= 1.0) {
        tokens -= 1.0;
        return true;
      }
      return false;
    }
  }
}
