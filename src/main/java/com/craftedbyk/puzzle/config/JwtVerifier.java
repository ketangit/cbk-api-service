package com.craftedbyk.puzzle.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Set;

/**
 * Verifies an RS256 JWT against a remote JWKS, enforcing signature, issuer, audience and expiry.
 *
 * <p>Used for both Firebase App Check tokens and Firebase Auth ID tokens — both are standard RS256
 * JWTs published at well-known JWKS endpoints, so we verify them directly instead of pulling the
 * heavy firebase-admin SDK (which fights the GraalVM native build). The underlying {@link
 * JWKSourceBuilder} caches keys and refreshes on rotation.
 */
final class JwtVerifier {

  private final DefaultJWTProcessor<SecurityContext> processor;

  JwtVerifier(String jwksUrl, String issuer, Set<String> acceptedAudiences) {
    JWKSource<SecurityContext> keySource;
    try {
      keySource = JWKSourceBuilder.<SecurityContext>create(URI.create(jwksUrl).toURL()).build();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("invalid JWKS url: " + jwksUrl, e);
    }
    this.processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
    processor.setJWTClaimsSetVerifier(
        new DefaultJWTClaimsVerifier<>(
            acceptedAudiences,
            new JWTClaimsSet.Builder().issuer(issuer).build(),
            Set.of("sub", "exp", "iat"),
            null));
  }

  /**
   * @return the verified claims
   * @throws Exception if the token's signature, issuer, audience, or expiry is invalid
   */
  JWTClaimsSet verify(String token) throws Exception {
    return processor.process(token, null);
  }
}
