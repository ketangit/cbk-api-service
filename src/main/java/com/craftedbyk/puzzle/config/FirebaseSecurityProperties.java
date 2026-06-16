package com.craftedbyk.puzzle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase identifiers + on/off switches for the App Check and Auth token filters.
 *
 * <p>The GCP project id/number are intentionally NOT hardcoded (kept out of source per repo
 * policy); supply them at deploy time via {@code FIREBASE_PROJECT_ID} / {@code
 * FIREBASE_PROJECT_NUMBER}. Both filters default to enabled in production and are switched off for
 * tests (see {@code src/test/resources/application.yml}) and local runs that don't go through
 * Firebase.
 */
@ConfigurationProperties(prefix = "cbk.security")
public class FirebaseSecurityProperties {

  /** Firebase project id, e.g. {@code craftedbyk-prod} (ID-token audience + issuer suffix). */
  private String projectId = "";

  /** GCP project number (App Check audience {@code projects/<number>} + issuer suffix). */
  private String projectNumber = "";

  /** Require a valid App Check token on {@code /api/**} (except health). */
  private boolean appCheckEnabled = true;

  /** Require a valid Firebase ID token on auth-gated endpoints (e.g. POST /api/orders). */
  private boolean authEnabled = true;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getProjectNumber() {
    return projectNumber;
  }

  public void setProjectNumber(String projectNumber) {
    this.projectNumber = projectNumber;
  }

  public boolean isAppCheckEnabled() {
    return appCheckEnabled;
  }

  public void setAppCheckEnabled(boolean appCheckEnabled) {
    this.appCheckEnabled = appCheckEnabled;
  }

  public boolean isAuthEnabled() {
    return authEnabled;
  }

  public void setAuthEnabled(boolean authEnabled) {
    this.authEnabled = authEnabled;
  }
}
