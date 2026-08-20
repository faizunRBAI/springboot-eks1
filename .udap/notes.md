# springboot-eks1 — Build Notes

## Project
- Blueprint: spring-boot-eks@1.0.2
- Cloud: AWS us-east-1, Target: EKS
- Stack: Java 21 / Spring Boot 3 / Maven / PostgreSQL (RDS db.t4g.micro) / EKS 1.33

## Status
- Meta approved ✅
- Design approved ✅
- Plan approved ✅
- Template applied (39 files) ✅
- Custom domain layer written (Item CRUD: domain, repository, service, controller, exception handler) ✅
- Tests written (HealthEndpointsTest + ItemServiceTest) ✅
- README updated with CRUD endpoints ✅
- versions.tf Blueprint tag fixed (was nodejs-eks → spring-boot-eks) ✅
- validate_project: PASS ✅
- test_project: PASSED ✅

## Architecture decisions
- No Ansible (EKS target — cluster configured via kubectl/terraform, not SSH)
- Flyway runs as a Kubernetes Job (db-migrate) before Deployment — not at app startup
- JDBC (JdbcClient) not JPA/Hibernate — simpler, no ORM overhead for a REST CRUD
- ItemRepository uses Spring 6 JdbcClient (named params, fluent API)
- All error handling centralised in ApiExceptionHandler (@RestControllerAdvice)

## Known issues / gotchas
- versions.tf had wrong Blueprint tag (nodejs-eks) from the template — fixed
- RDS PostgreSQL 16 in template; fine for this project
- NLB takes a few minutes to resolve in DNS after first deploy — expected

## Next
- create_repo_and_push
- set_pipeline_secret DB_PASSWORD
- deploy
- wait_for_run
