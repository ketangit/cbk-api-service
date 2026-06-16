package com.craftedbyk.puzzle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a valid Firebase App Check token on every {@code /api/**} request (except the health
 * probe and CORS preflight). This is what makes a direct hit to the raw Cloud Run {@code *.run.app}
 * URL — or any curl/Postman/scraper — useless: without an attestation token minted by our real web
 * app, the request is rejected with 401. It cannot prove the caller is our exact JS (a browser is
 * attacker-controllable), but it blocks non-browser clients and raises the cost of abuse.
 *
 * <p>Runs after {@link RateLimitFilter} (cheap throttling first) and before {@link AuthFilter}.
 */
@Component
@Order(2)
public class AppCheckFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Firebase-AppCheck";
  private static final String JWKS_URL = "https://firebaseappcheck.googleapis.com/v1/jwks";

  private final FirebaseSecurityProperties props;
  private volatile JwtVerifier verifier;

  public AppCheckFilter(FirebaseSecurityProperties props) {
    this.props = props;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (!props.isAppCheckEnabled()
        || HttpMethod.OPTIONS.matches(request.getMethod())
        || path == null
        || !path.startsWith("/api/")
        || path.equals("/api/puzzle/health")) {
      chain.doFilter(request, response);
      return;
    }

    String token = request.getHeader(HEADER);
    if (token == null || token.isBlank()) {
      reject(response, "missing App Check token");
      return;
    }
    try {
      verifier().verify(token);
    } catch (Exception e) {
      reject(response, "invalid App Check token");
      return;
    }
    chain.doFilter(request, response);
  }

  /** Built lazily so tests/local runs with App Check disabled never construct a JWKS client. */
  private JwtVerifier verifier() {
    JwtVerifier v = verifier;
    if (v == null) {
      synchronized (this) {
        v = verifier;
        if (v == null) {
          String number = props.getProjectNumber();
          Set<String> audiences = new LinkedHashSet<>();
          audiences.add("projects/" + number);
          if (!props.getProjectId().isBlank()) {
            audiences.add("projects/" + props.getProjectId());
          }
          verifier =
              v =
                  new JwtVerifier(
                      JWKS_URL, "https://firebaseappcheck.googleapis.com/" + number, audiences);
        }
      }
    }
    return v;
  }

  private void reject(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "AppCheck");
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
