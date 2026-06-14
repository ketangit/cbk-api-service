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
  Secrets** (`GCP_PROJECT_ID`, `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT`) — never hardcode them in
  the workflow, `pom.xml`, or README. The project id is genericized to `your-gcp-project-id` in
  source on purpose; keep it that way.

## Git workflow

- Feature branches prefixed `feat/`, `fix/`, `chore/`, `ci/`. Conventional Commits. Squash-merge
  into `main` via PR. End commit messages with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.

## Code layout / security

- Java package root is `com.craftedbyk.puzzle` (the entry class is still named
  `FractalPuzzleApplication` for historical reasons).
- **Known open security finding — do not widen it:** `GET /api/orders/{ref}` (`shop/ShopController`)
  returns full customer PII with no auth, guarded only by an unguessable UUID capability ref. Auth +
  ownership checks are still owed before treating this as production-safe.
