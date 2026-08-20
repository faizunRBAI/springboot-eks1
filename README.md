# springboot-eks1

A Spring Boot REST API running on Amazon EKS, provisioned by Terraform and shipped
by a security-gated GitHub Actions pipeline.

> Built from the UDAP `spring-boot-eks` production blueprint. The long-form
> description of the platform lives in [.udap/docs/README.md](.udap/docs/README.md).

## Layout

| Path | What it holds |
|------|---------------|
| `src/main/java/com/example/app/web/` | HTTP controllers and the exception handler |
| `src/main/java/com/example/app/domain/` | `Item` record, repository, service, `ItemNotFoundException` |
| `src/main/resources/` | `application.yaml`, static welcome page, Flyway DDL in `db/migration/{vendor}/` |
| `src/test/java/` | JUnit 5 tests (`HealthEndpointsTest`, `ItemServiceTest`) |
| `config/checkstyle/` | Coding-standards rule set |
| `bin/migrate` | Migration entrypoint the deploy calls |
| `infra/` | All Terraform: VPC, EKS, managed node group, ECR, KMS, RDS PostgreSQL |
| `k8s/` | Deployment, Service (NLB), HPA, PodDisruptionBudget, ServiceAccount, Flyway migration Job |
| `.udap/architecture.d2` | Architecture diagram in the UDAP D2 profile |
| `.udap/pipeline.yaml` | Pipeline spec — CI workflow files are rendered from it |
| `pom.xml` | Maven build descriptor |

## Endpoints

| Route | Method | Purpose |
|-------|--------|---------|
| `GET /` | — | Welcome page; status read live from `/health` |
| `GET /health` | — | Liveness. Never touches the database |
| `GET /ready` | — | Readiness. Checks the database when one is configured |
| `GET /api/info` | — | Runtime and build facts |
| `GET /api/echo` | — | Echoes query parameters |
| `GET /api/items` | — | List all items, newest first |
| `GET /api/items/{id}` | — | Fetch one item |
| `POST /api/items` | `{"name":"…"}` | Create an item — returns 201 + Location |
| `PUT /api/items/{id}` | `{"name":"…"}` | Rename an item |
| `DELETE /api/items/{id}` | — | Delete an item — returns 204 |
| `GET /actuator/**` | — | Spring Actuator: health, info, metrics, prometheus |

## Running it locally

```bash
# Without a database (stateless mode — /ready reports "not configured")
mvn spring-boot:run

# With a local PostgreSQL
SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/appdb' \
SPRING_DATASOURCE_USERNAME=appuser \
SPRING_DATASOURCE_PASSWORD=secret \
  mvn spring-boot:run
```

Open <http://localhost:8080> in a browser.

The quality gates the pipeline runs, locally:

```bash
mvn -B checkstyle:check   # coding standards
mvn -B test               # unit tests
mvn -B verify             # everything + the packaged jar
docker build -t app .     # the image the pipeline builds
```

## Domain model

The API manages **items** — a simple named resource with a server-assigned id and
creation timestamp. The `items` table is created by Flyway migration `V1__init.sql`
and the application talks to it through a Spring JDBC repository (`ItemRepository`)
and a service layer (`ItemService`) that validates input and maps repository
outcomes to domain exceptions.

```bash
# Create
curl -s -X POST http://localhost:8080/api/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"hello"}' | jq .

# List
curl -s http://localhost:8080/api/items | jq .

# Update
curl -s -X PUT http://localhost:8080/api/items/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"world"}' | jq .

# Delete
curl -s -X DELETE http://localhost:8080/api/items/1 -o /dev/null -w "%{http_code}\n"
```

## Database migrations

The deploy runs `sh bin/migrate` as a Kubernetes Job before the new Deployment is
applied. A failure stops the deploy, with the migration log printed to CI.

Add numbered Flyway files under the PostgreSQL directory:

```
src/main/resources/db/migration/postgresql/V1__init.sql   # items table
src/main/resources/db/migration/postgresql/V2__orders.sql # yours
```

Flyway is **disabled in the running application** — every replica would otherwise
race to migrate on startup. `bin/migrate` runs the same jar under the `migrate`
profile, which enables Flyway, disables the web server and exits.

Because a rolling update runs old and new pods simultaneously, **a migration must
be backward-compatible with the code already running**. Add a nullable column and
ship the code that writes it; drop the old column in a later release.

## How a deploy runs

Seven gates run in parallel — Checkstyle, unit tests, Semgrep SAST, Gitleaks
secret scanning, licence compliance, SBOM generation and Terraform IaC security
scanning. All must pass before a single AWS resource is touched.

Then: Terraform applies `infra/` → image built + pushed to ECR → Trivy image
scan → Flyway migrations Job → `k8s/` manifests applied → rollout watched →
NLB health-checked before the deploy is declared green.

The pipeline is defined once in `.udap/pipeline.yaml`. Edit it there; the platform
re-renders `.github/workflows/deploy.yml` and `.github/workflows/destroy.yml`
automatically — never edit those files by hand.

## Configuration

The platform sets these repository secrets at deploy time — nothing needs to be
configured manually:

| Secret | Used for |
|--------|----------|
| `PROJECT_NAME` | Resource prefix, Kubernetes namespace, ECR repo name, Terraform state key |
| `TF_STATE_BUCKET` | Terraform remote state bucket |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Provisioning and cluster access |

The database password is generated by Terraform, lives in the state file, and
reaches pods only as the `app-database` Kubernetes Secret
(`SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD`).

## Finding the application URL

The deploy prints it during **Wait for the load balancer**, **Health check** and
in the workflow run's **Summary** panel.

At any time afterwards:

```bash
aws eks update-kubeconfig --name "$PROJECT_NAME-eks" --region us-east-1
kubectl get svc api -n "$PROJECT_NAME"    # EXTERNAL-IP column
```

A brand-new NLB takes a few minutes to propagate in DNS outside AWS — the health
check passes from the CI runner before a browser can resolve it from elsewhere.

## Operating it

```bash
aws eks update-kubeconfig --name "$PROJECT_NAME-eks" --region us-east-1

kubectl get pods -n "$PROJECT_NAME"
kubectl logs -n "$PROJECT_NAME" -l app.kubernetes.io/name=api --tail=100
kubectl rollout undo deployment/api -n "$PROJECT_NAME"   # roll back one release
kubectl get svc api -n "$PROJECT_NAME"                   # public hostname
```

## Accepted security findings

`.trivyignore` records every infrastructure finding this project accepts, with the
reason inline. A finding that is not listed is a real one — fix it rather than
adding a line.
