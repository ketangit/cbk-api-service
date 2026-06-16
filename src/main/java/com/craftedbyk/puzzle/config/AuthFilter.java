package com.craftedbyk.puzzle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a valid Firebase Auth ID token (Bearer) on mutating endpoints that must belong to a
 * known user — currently {@code POST /api/orders}. The verified Firebase {@code uid} is exposed as
 * the {@link #UID_ATTRIBUTE} request attribute so the controller can own the order to that user.
 *
 * <p>App Check (see {@link AppCheckFilter}) answers "is this our app?"; this answers "who is the
 * user?". Read-only catalog and the capability-URL order lookup stay public; they are not gated
 * here.
 */
@Component
@Order(3)
public class AuthFilter extends OncePerRequestFilter {

  public static final String UID_ATTRIBUTE = "firebaseUid";
  private static final String JWKS_URL =
      "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

  private final FirebaseSecurityProperties props;
  private volatile JwtVerifier verifier;

  public AuthFilter(FirebaseSecurityProperties props) {
    this.props = props;
  }

  /** Endpoints that require an authenticated user. */
  private static boolean requiresAuth(HttpServletRequest request, String path) {
    return HttpMethod.POST.matches(request.getMethod()) && "/api/orders".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (!props.isAuthEnabled()
        || HttpMethod.OPTIONS.matches(request.getMethod())
        || path == null
        || !requiresAuth(request, path)) {
      chain.doFilter(request, response);
      return;
    }

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      reject(response, "missing bearer token");
      return;
    }
    try {
      String uid = verifier().verify(header.substring(7)).getSubject();
      request.setAttribute(UID_ATTRIBUTE, uid);
    } catch (Exception e) {
      reject(response, "invalid ID token");
      return;
    }
    chain.doFilter(request, response);
  }

  private JwtVerifier verifier() {
    JwtVerifier v = verifier;
    if (v == null) {
      synchronized (this) {
        v = verifier;
        if (v == null) {
          String projectId = props.getProjectId();
          verifier =
              v =
                  new JwtVerifier(
                      JWKS_URL,
                      "https://securetoken.google.com/" + projectId,
                      java.util.Set.of(projectId));
        }
      }
    }
    return v;
  }

  private void reject(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
