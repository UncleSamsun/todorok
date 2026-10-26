# Company server test deployment — 2026-10-14

## Scope and design

Deploy the already implemented MVP into its own `todorok-test` K3s namespace. Existing company applications and their data remain independent. Use the existing trusted IP HTTPS entry point at `/todorok/`; keep the three unique root `/api/{planner,activity,notification}/` prefixes. No new host ports, shared DB credentials, host mounts or sudo are needed.

Build application images from a reviewed source snapshot on `codex/company-server-deploy` through repository-owner GitHub Actions. Record GHCR digests and apply phased manifests: namespace/config/network → PostgreSQL/Kafka → role provisioning → migrations → topics/publication/slot → Connect → connector/bootstrap → runtime → HTTPS route.

Store newly generated authentication, template-service, push and DB secrets only in ignored `.local/company-server` and the dedicated Kubernetes Secret. Keep the initial login credentials in an ACL-restricted local file. The existing registry Secret has empty auths; the user selected Public visibility for the five images of this public repository. Pull the immutable digests anonymously without copying other applications' credentials.

## Verification

- [x] SSH identity, K3s health, existing workload baseline, available namespace/resource permissions.
- [x] Root and `/todorok/` frontend routing, manifest/SW and scoped push RED→GREEN; web133 tests.
- [x] Manifest invariants: immutable application digests, namespace-local storage/services, no host ports/secrets in rendered public files.
- [x] Server-side schema validation and final read-only review of the deployment; fixed Nginx DNS FQDN and web memory request/limit.
- [x] Build/publish five immutable application image references.
- [x] Provision only the new namespace and complete each migration job before starting application replicas.
- [x] Connector RUNNING, application readiness, HTTPS login/session, Task/activity/Kafka/reminder API checks.
- [x] Preserve a baseline comparison for SyncDoc/VODA, update vault and handoff records.

This is an authenticated test deployment, not final release acceptance. Actual iPhone push/timer/Wake Lock, source-catalog permission and sustained operations/backup recovery remain separately evidenced checks.

## Live result

2026-10-14: https://211.45.127.56/todorok/ is available. Image source cd3d48b, Actions run 38513852547 (five successful jobs); manifest release cd3d48b9ed53-r1 plus the manifest MIME fix. All five deployments and both StatefulSets are Ready. PostgreSQL histories are planner12/activity11/notification6. Connector todorok-postgres-outbox and task0 are RUNNING.

The live smoke passed 11 checks: HTTPS/scoped PWA/assets, API protection, login and Secure HttpOnly refresh rotation, authenticated notification/VAPID API, Task completion, reminder creation and pending-delivery CDC, activity→planner Kafka round trip, cleanup/cancellation, original preferences restoration and logout. The browser login screen renders without console errors. Nginx integration2 and manifest/push4 tests passed.

Initial real server DB and dedicated app secrets were encrypted into ACL-restricted off-host files on this PC. Decryption equality and an isolated network-none PostgreSQL restore preserved schema histories, the initial account, planner table owner and Debezium grants; plaintext was removed. This does not prove full restored application login/CDC or automated backup scheduling. Backup key remains separate from the encrypted backup directory.

Existing deployments stayed 1/1. HTTP baselines before/after match: SyncDoc200, Grafana302, VODA root404 (pre-existing; unchanged by this deployment). No existing app was modified. The original feature working tree remains uncommitted and intact; deployment source is in its separate managed worktree/branch.

## Account and persistent login follow-up

User explicitly requested a seven-character password policy. Source399a4a4 aligns bootstrap and OpenAPI/generated login validation at seven characters, keeps BCrypt cost12 and rate limits, and passes12 session/security tests (including seven-character acceptance and six-character rejection). Actions38517372582 succeeded. Only planner was updated to its new immutable digest; all workloads remain Ready. The original account ID/data were preserved, requested credentials returned200, and the previous password returned401. Credential values stay out of Git and vault.

Automatic login was verified against the server with a fresh SessionClient using only a persisted cookie, then in the actual browser after login, refresh and reopening a tab. The browser returned directly to /todorok/today with no credentials and console errors0. The rolling cookie expires after30 days of inactivity; logout/deleted cookies/revoked sessions require login. An encrypted account-update DB/secret snapshot and isolated owner/ACL-preserving DB restore succeeded. A local Docker interruption during the first restore attempt was recovered; temporary plaintext/container files were cleaned.

## Domain and mobile audit

Canonical URL is https://ai.dfocus.net/todorok/; the original IP URL remains supported. DNS and existing SNI TLS certificate verified; planner allowlist and the dedicated IngressRoute were updated without copying TLS keys or changing shared applications. Domain smoke11 and IP fallback login200/logout204 passed. Renderer supports explicit HTTPS aliases and preserves the server-IP egress exclusion;2 manifest tests passed.

Mobile audit390×844/360×800 covered8 functional flows with29 inspected screenshots. Actual server data matches UI input and timer partial status. Four test Tasks were deleted, activity records voided, category archived and original preferences restored; cancellation history is retained. Console errors0. Mobile design needs corrections, led by the climbing toolbar overflow. See docs/reports/2026-10-14-mobile-audit.md and ignored .local/mobile-audit/report.html.

## 2026-10-15 UI/program/login release

User authorized deployment after live local refinement, including the subsequent saved-ID/automatic-login options. Source `daeafaff5ea85f5ddf797638c3e231422160c37d`, Actions `38688131565`: five image builds succeeded. Test-only follow-up `58b15c2` aligns old full-form tests to inline autosave and the session restoration entry behavior. The primary working tree remains intact and uncommitted.

Encrypted current database/app-secret backup and decryption equality check completed before updates. Planner/activity migration jobs `*-migration-daeafaf-ui-20261015` both completed; activity schema V12 adds selected program session/epoch state. Updated only planner/activity/web by immutable digests. Notification, Connect, PostgreSQL, Kafka and all unrelated applications retained their existing deployments/configuration.

All five deployments and both StatefulSets are Ready; connector and task0 are RUNNING. Actual domain HTTPS smoke passed11 checks including login/refresh, reminder CDC, activity→planner completion, cleanup and preferences restoration. Additional checks confirmed four builtin catalog keys, authenticated enrollment query and deployed login-option labels/storage keys. Existing authenticated in-app browser restored automatically; after the PWA update and one refresh it showed the new header and actual pushup week/session popup with no console errors. No production program enrollment was created for verification.

Local verification: API client5/typecheck, contract drift, production/PWA build, manifest2, targeted auth/program/inline/floating19; full web run155/160 then corrected5 obsolete UI expectations and reran their two files26/26. Planner Task HTTP regression passed; activity/program/date25 had passed before release. Actual iPhone Safari autofocusing/push behavior remains a device check.

## 2026-10-26 program progress release

Source `820bd3f`, Actions `40249625206`: five image builds succeeded. Applied activity V13, then replaced activity-service and web only. Fresh service96/web169 tests, encrypted backup plus isolated restore, live HTTPS smoke11, deployed program context and browser state-editor/delete/repeat controls passed. All seven dedicated workloads are Ready, connector/task0 RUNNING, other images/readiness unchanged. See [the release evidence](../reports/2026-10-26-program-release.md). Existing user program progress was preserved; original feature checkout changes remain uncommitted.
