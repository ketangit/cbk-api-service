# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Full command reference and architecture live in @README.md. This file covers only what is
non-obvious or easy to get wrong.

## Build / format gotchas

- **Spotless is a CI gate.** CI runs `./mvnw spotless:check` and fails on any drift. Always run
  `./mvnw spotless:apply` before committing Java or `pom.xml` changes. Spotless also sorts `pom.xml`.
- **JDK 25 is the target; CI pins JDK 25.** `java.version=25` in `pom.xml`; the deploy workflow's
  `setup-java` uses 25. A local machine may run JDK 26 — do **not** put JDK-26-only flags in
  `.mvn/jvm.config` (e.g. `--enable-final-field-mutation`); they abort the Maven JVM on CI's JDK 25.
  Keep flags valid on JDK 21+.
- **Native image override uses `-Ddocker.image.name=…`**, NOT `-Dspring-boot.build-image.imageName`.
  The pom binds `<image><name>` to `${docker.image.name}` explicitly, so the plugin's own
  user-property is ignored (explicit mojo config wins).
- Native image builds (`./mvnw -Pnative spring-boot:build-image`) need a running Docker daemon;
  buildpacks do the GraalVM compilation in a container — no local GraalVM required.

## Deploy / GCP

- **Push to `main` auto-deploys to Cloud Run** (build native image → Artifact Registry → deploy).
  This is outward-facing and live — treat merges to `main` as production deploys. PRs skip deploy.
- Auth is keyless via Workload Identity Federation. Sensitive GCP identifiers live in **GitHub
  Secrets** (`GCP_PROJECT_ID`, `GCP_PROJECT_NUMBER`, `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT`) —
  never hardcode them in the workflow, `pom.xml`, or README. The project id is genericized to
  `your-gcp-project-id` in source on purpose; keep it that way.
- **Firebase env vars are injected at deploy time**, not hardcoded. `deploy.yml` sets
  `FIREBASE_PROJECT_ID` (= the `GCP_PROJECT_ID` secret — a Firebase project *is* a GCP project) and
  `FIREBASE_PROJECT_NUMBER` (= `GCP_PROJECT_NUMBER` secret) via the `deploy-cloudrun` `env_vars`
  input with `env_vars_update_strategy: merge`. Both default empty in `application.yml`, so a
  deploy that forgets them silently disables nothing (gates stay on but have no audience) — keep
  the secret + wiring in sync.

## Git workflow

- Feature branches prefixed `feat/`, `fix/`, `chore/`, `ci/`. Conventional Commits. Squash-merge
  into `main` via PR. End commit messages with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.

## Code layout / security

- Java package root is `com.craftedbyk.puzzle` (the entry class is still named
  `FractalPuzzleApplication` for historical reasons).
- **Firebase token gates live in `config/`.** Two `OncePerRequestFilter`s, ordered after
  `RateLimitFilter`: `AppCheckFilter` (`@Order(2)`) requires an `X-Firebase-AppCheck` token on every
  `/api/**` (except `/api/puzzle/health` + OPTIONS); `AuthFilter` (`@Order(3)`) requires a Bearer
  Firebase ID token on `POST /api/orders` and stashes the uid in the `firebaseUid` request
  attribute. Both verify tokens as plain RS256 JWTs via `JwtVerifier` (nimbus-jose-jwt against
  Google's public JWKS) — **deliberately not firebase-admin**, which fights the GraalVM native
  build. Verifiers are built lazily so disabled gates / tests never open a JWKS client.
- Gates are toggled by `APPCHECK_ENABLED` / `AUTH_ENABLED` (default `true`). `src/test/resources/
  application.yml` sets both `false` so the test slice never reaches Firebase.
- **Known open security finding — do not widen it:** `GET /api/orders/{ref}` (`shop/ShopController`)
  returns full customer PII guarded only by an unguessable UUID capability ref. App Check now fronts
  it (no raw non-browser access), but there is still **no per-user ownership check** — any caller
  with the ref and a valid App Check token reads the PII. Ownership is still owed before treating
  this as production-safe.
