# Company server test deployment — 2026-10-14

## Scope and design

Deploy the already implemented MVP into its own `todorok-test` K3s namespace. Existing company applications and their data remain independent. Use the existing trusted IP HTTPS entry point at `/todorok/`; keep the three unique root `/api/{planner,activity,notification}/` prefixes. No new host ports, shared DB credentials, host mounts or sudo are needed.

Build application images from a reviewed source snapshot on `codex/company-server-deploy` through repository-owner GitHub Actions. Record GHCR digests and apply phased manifests: namespace/config/network → PostgreSQL/Kafka → role provisioning → migrations → topics/publication/slot → Connect → connector/bootstrap → runtime → HTTPS route.

Store newly generated authentication, template-service, push and DB secrets only in ignored `.local/company-server` and the dedicated Kubernetes Secret. Keep the initial login credentials in an ACL-restricted local file. Copy only the established repository-owner GHCR image-pull credential into the new namespace without printing it. This credential is for kubelet image pulls and is not mounted into applications.

## Verification

- [x] SSH identity, K3s health, existing workload baseline, available namespace/resource permissions.
- [x] Root and `/todorok/` frontend routing, manifest/SW and scoped push RED→GREEN; web133 tests.
- [x] Manifest invariants: immutable application digests, namespace-local storage/services, no host ports/secrets in rendered public files.
- [ ] Server-side schema validation and final read-only review of the deployment.
- [ ] Build/publish five immutable application image references.
- [ ] Provision only the new namespace and complete each migration job before starting application replicas.
- [ ] Connector RUNNING, application readiness, HTTPS login/session, Task/activity/Kafka/reminder API checks.
- [ ] Preserve a baseline comparison for SyncDoc/VODA, update vault and handoff records.

This is an authenticated test deployment, not final release acceptance. Actual iPhone push/timer/Wake Lock, source-catalog permission and sustained operations/backup recovery remain separately evidenced checks.
