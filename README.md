# cbk-api-service

API backend for **craftedbyk.com** — Spring Boot 4.1 on Java 25, compiled to a
**GraalVM native image** and deployed to **Google Cloud Run** from Artifact
Registry. Authentication to GCP is keyless via **Workload Identity Federation**.

## Stack

| Concern       | Choice                                                       |
| ------------- | ------------------------------------------------------------ |
| Language      | Java 25                                                      |
| Framework     | Spring Boot 4.1 (Spring Web, Actuator)                       |
| Build         | Maven (wrapper) + `native-maven-plugin` (GraalVM AOT)        |
| Image build   | `spring-boot:build-image` (Paketo buildpacks, `BP_NATIVE_IMAGE`) |
| Formatting    | Spotless (google-java-format, GOOGLE style)                  |
| Runtime       | Google Cloud Run (linux/amd64)                                      |

## Building the native image locally

Requires a running Docker daemon (the buildpack does the GraalVM compilation
inside a container, so you do **not** need GraalVM installed locally):

```bash
./mvnw -Pnative spring-boot:build-image \
  -Ddocker.image.name=us-east4-docker.pkg.dev/<your-gcp-project-id>/cbk-api-service/api:local
```

> Override the image ref with **`-Ddocker.image.name`**, not
> `-Dspring-boot.build-image.imageName`: the pom binds `<image><name>` to the
> `docker.image.name` property, and an explicit `<name>` ignores the plugin's
> own user-property.

## Scripts

Common commands (all via the Maven wrapper — no local Maven/GraalVM needed):

| Command                                   | Does                                              |
| ----------------------------------------- | ------------------------------------------------- |
| `./mvnw spring-boot:run`                  | Run the app on `http://localhost:8080`            |
| `./mvnw test`                             | Run unit tests                                    |
| `./mvnw spotless:apply`                   | Auto-format (google-java-format)                  |
| `./mvnw spotless:check`                   | Verify formatting — the CI gate                   |
| `./mvnw -Pnative spring-boot:build-image -Ddocker.image.name=<ref>` | Build the GraalVM native OCI image (needs Docker) |
| `gh workflow run "Deploy API"`            | Manually trigger the deploy workflow              |
| `git push origin main`                    | Push to `main` → CI builds + deploys to Cloud Run |

Deploys are automated: any push to `main` runs verify, then the native
build/push/deploy job. No manual `gcloud` steps in the normal flow.

## CORS

`config/CorsConfig.java` registers a global `CorsFilter` allowing exactly
`https://craftedbyk.com` and `http://localhost:3000` (credentials enabled, no
wildcards). Add new front-end origins there.

## Security — Firebase App Check + Auth

Two servlet filters in `config/` gate `/api/**`, verifying Firebase tokens as
plain RS256 JWTs (via `nimbus-jose-jwt` / `JwtVerifier` against Google's public
JWKS — **not** the firebase-admin SDK, which fights the GraalVM native build):

| Filter | Order | Guards | Requires |
| ------ | ----- | ------ | -------- |
| `AppCheckFilter` | 2 | every `/api/**` except `/api/puzzle/health` + OPTIONS | `X-Firebase-AppCheck` token |
| `AuthFilter`     | 3 | `POST /api/orders` | `Authorization: Bearer <Firebase ID token>` |

App Check answers *"is this our app?"* (blocks raw `*.run.app` hits from
curl/scrapers); Auth answers *"who is the user?"* (exposes the verified uid as
the `firebaseUid` request attribute). Config lives in `FirebaseSecurityProperties`
(`cbk.security.*`), driven by env vars:

| Env var | Maps to | Default |
| ------- | ------- | ------- |
| `FIREBASE_PROJECT_ID`     | ID-token audience/issuer | _(empty)_ |
| `FIREBASE_PROJECT_NUMBER` | App Check audience/issuer | _(empty)_ |
| `APPCHECK_ENABLED`        | toggle App Check gate | `true` |
| `AUTH_ENABLED`            | toggle Auth gate | `true` |

For local runs that don't go through Firebase, set `APPCHECK_ENABLED=false` and
`AUTH_ENABLED=false` (the test config already does). In production both project
values are injected by `deploy.yml` from GitHub Secrets — see GCP configuration.

## CI/CD — `.github/workflows/deploy.yml`

Default-deny token permissions; all actions pinned to commit SHAs.

- **verify** (push + PR): `spotless:check` and tests on the JVM (fast — no native
  build on PRs).
- **deploy** (push to `main` / manual): authenticates to GCP via WIF, builds the
  native image on `ubuntu-latest` (so it is `linux/amd64`, matching Cloud Run),
  pushes to Artifact Registry tagged with the commit SHA, and deploys to Cloud
  Run behind the `production` GitHub Environment. Skipped on pull requests.

The image ref is composed at runtime from the `GCP_PROJECT_ID` secret + commit
SHA and passed to the build via `-Ddocker.image.name` (the property the pom's
`<image><name>` is bound to). The deploy step sets `--allow-unauthenticated`, so
the public `allUsers → roles/run.invoker` binding is reasserted on every deploy
and can't drift — the service fronts both the public site and the API.

## Pinned actions

| Action                              | Version | Commit SHA                                 |
| ----------------------------------- | ------- | ------------------------------------------ |
| actions/checkout                    | v6.0.3  | `df4cb1c069e1874edd31b4311f1884172cec0e10` |
| actions/setup-java                  | v5.2.0  | `be666c2fcd27ec809703dec50e508c2fdc7f6654` |
| google-github-actions/auth          | v3      | `7c6bc770dae815cd3e89ee6cdf493a5fab2cc093` |
| google-github-actions/deploy-cloudrun | v3    | `2028e2d7d30a78c6910e0632e48dd561b064884d` |

## GCP configuration

Sensitive identifiers are stored as **GitHub Secrets** (Settings → Secrets and
variables → Actions), not committed to the workflow:

| Secret                | Holds                                           |
| --------------------- | ----------------------------------------------- |
| `GCP_PROJECT_ID`      | GCP project id (also used as `FIREBASE_PROJECT_ID`) |
| `GCP_PROJECT_NUMBER`  | GCP project number (used as `FIREBASE_PROJECT_NUMBER`) |
| `GCP_WIF_PROVIDER`    | Full Workload Identity Federation provider path |
| `GCP_SERVICE_ACCOUNT` | Deploy service-account email                    |

`deploy.yml` injects `FIREBASE_PROJECT_ID` (= `GCP_PROJECT_ID`) and
`FIREBASE_PROJECT_NUMBER` (= `GCP_PROJECT_NUMBER`) into the Cloud Run service via
the `deploy-cloudrun` `env_vars` input (`env_vars_update_strategy: merge`) — a
Firebase project *is* a GCP project, so the id is shared.

Non-sensitive values stay inline in `deploy.yml`:

| Setting           | Value                                                          |
| ----------------- | ------------------------------------------------------------- |
| Region            | `us-east4`                                                    |
| Cloud Run service | `cbk-api-service`                                             |
| Artifact Registry | `us-east4-docker.pkg.dev/<GCP_PROJECT_ID>/cbk-api-service/api` |


## Project structure

```
pom.xml                         # Spring Boot 4, Java 25, spotless, native profile
mvnw, mvnw.cmd                  # Maven wrapper (3.9.16); jar downloaded on first run
.mvn/
  jvm.config                    # --enable-native-access=ALL-UNNAMED (quiets JDK warning)
  wrapper/maven-wrapper.properties
.github/workflows/deploy.yml    # verify + native build/deploy to Cloud Run
src/
  main/
    java/com/craftedbyk/puzzle/
      FractalPuzzleApplication.java   # entry point
      api/        # PuzzleController, request/response records, exception handler
      config/     # CorsConfig, RateLimitFilter, Firebase App Check + Auth filters
                  #   (AppCheckFilter, AuthFilter, JwtVerifier, FirebaseSecurityProperties)
      generator/  # fractal jigsaw geometry + RNG (SinRandom / SecureRandomSource)
      service/    # PuzzleService, PricingService, ExportMode, GeneratedPuzzle
      shop/       # catalog + orders (controller, JPA entities, repositories)
    resources/application.yml   # server, actuator, compression config
  test/
    java/com/craftedbyk/puzzle/ # api, generator, shop tests
    resources/                  # golden-*.svg fixtures
```
