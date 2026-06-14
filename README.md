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
| Runtime       | Cloud Run (linux/amd64)                                      |
| Registry      | Artifact Registry (`us-east4`)                               |

## Local development

```bash
./mvnw spring-boot:run                 # run on http://localhost:8080
curl localhost:8080/api/puzzle/health  # -> {"status":"ok"}
./mvnw spotless:apply                  # auto-format
./mvnw spotless:check                  # verify formatting (CI gate)
./mvnw test                            # unit tests
```

> The Maven wrapper jar is **not** committed; `./mvnw` downloads it on first run
> from the `wrapperUrl` in `.mvn/wrapper/maven-wrapper.properties`.

## Building the native image locally

Requires a running Docker daemon (the buildpack does the GraalVM compilation
inside a container, so you do **not** need GraalVM installed locally):

```bash
./mvnw -Pnative spring-boot:build-image \
  -Dspring-boot.build-image.imageName=us-east4-docker.pkg.dev/<your-gcp-project-id>/cbk-api-service/api:local
```

## CORS

`config/CorsConfig.java` registers a global `CorsFilter` allowing exactly
`https://craftedbyk.com` and `http://localhost:3000` (credentials enabled, no
wildcards). Add new front-end origins there.

## CI/CD — `.github/workflows/deploy.yml`

Default-deny token permissions; all actions pinned to commit SHAs.

- **verify** (push + PR): `spotless:check` and tests on the JVM (fast — no native
  build on PRs).
- **deploy** (push to `main` / manual): authenticates to GCP via WIF, builds the
  native image on `ubuntu-latest` (so it is `linux/amd64`, matching Cloud Run),
  pushes to Artifact Registry tagged with the commit SHA, and deploys to Cloud
  Run behind the `production` GitHub Environment.

### Pinned actions

| Action                              | Version | Commit SHA                                 |
| ----------------------------------- | ------- | ------------------------------------------ |
| actions/checkout                    | v4.2.2  | `11bd71901bbe5b1630ceea73d27597364c9af683` |
| actions/setup-java                  | v4.7.1  | `c5195efecf7bdfc987ee8bae7a71cb8b11521c00` |
| google-github-actions/auth          | v2.1.13 | `c200f3691d83b41bf9bbd8638997a462592937ed` |
| google-github-actions/deploy-cloudrun | v2.7.6 | `251330ba9a8a34bfbc1622895f42e1d53fd14522` |

### GCP configuration

Sensitive identifiers are stored as **GitHub Secrets** (Settings → Secrets and
variables → Actions), not committed to the workflow:

| Secret                | Holds                                           |
| --------------------- | ----------------------------------------------- |
| `GCP_PROJECT_ID`      | GCP project id                                  |
| `GCP_WIF_PROVIDER`    | Full Workload Identity Federation provider path |
| `GCP_SERVICE_ACCOUNT` | Deploy service-account email                    |

Non-sensitive values stay inline in `deploy.yml`:

| Setting           | Value                                                          |
| ----------------- | ------------------------------------------------------------- |
| Region            | `us-east4`                                                    |
| Cloud Run service | `cbk-api-service`                                             |
| Artifact Registry | `us-east4-docker.pkg.dev/<GCP_PROJECT_ID>/cbk-api-service/api` |

### One-time GCP setup (outside this repo)

1. **Artifact Registry** Docker repo `cbk-api-service` in `us-east4`.
2. **Workload Identity Federation** pool `github-actions-pool` + provider
   `github-provider`, with an attribute condition restricting to **this** repo,
   e.g. `assertion.repository == 'YOUR_ORG/cbk-api-service'`.
3. **Service account** `github-actions-sa` with the minimum roles:
   `roles/run.admin`, `roles/artifactregistry.writer`,
   `roles/iam.serviceAccountUser` (to act as the Cloud Run runtime SA), and bind
   the WIF principal with `roles/iam.workloadIdentityUser`.
4. Create a `production` GitHub Environment; require reviewers / restrict to
   `main` as desired.

Auth is keyless — no service-account JSON key is ever stored. The three GitHub
Secrets above hold only non-credential GCP identifiers (project id, WIF provider
path, SA email), kept out of source. The runtime `PORT` is provided by Cloud
Run; the app reads it via `server.port=${PORT:8080}`.

## Project structure

```
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
src/
  main/
    java/com/fractalforge/puzzle/api/
    resources/application.yml
  test/java/com/fractalforge/puzzle/api/
.github/workflows/deploy.yml
```
