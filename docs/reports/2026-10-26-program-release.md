# Program progress release — 2026-10-26

Deployed source `820bd3fdf86d94c8cc006d6bc905ec7cda73b92e` from `codex/company-server-deploy` to [the company server](https://ai.dfocus.net/todorok/). [Actions 40249625206](https://github.com/UncleSamsun/todorok/actions/runs/40249625206) built all five images successfully. The primary feature checkout remains uncommitted and preserved.

The release fixes completion without optional repetitions, adds task-menu deletion, allows editing an enrollment's week/session, and offers an explicit week repeat when saving the current final session. Only activity-service and web were replaced:

- Activity: `ghcr.io/unclesamsun/todorok-activity@sha256:4a6ac3a3b41073b94853e9eaeef4e30d6ac88286701f1beda9faac8ad92141f4`
- Web: `ghcr.io/unclesamsun/todorok-web@sha256:e9b84f81b941bc3336ca695dede7fbf3c30596b773d9b3f64993d471ec7336ed`

`activity-migration-820bd3f-program-20261026` applied V13 successfully before runtime replacement. Planner V12 and notification V6 remain unchanged. All five dedicated Deployments and both StatefulSets are Ready; the connector and task0 are RUNNING. Other workload images and readiness match the predeployment baseline.

The current database and live application Secret were encrypted off-host. Decryption equality and an isolated, network-none PostgreSQL restore verified schema versions, record counts, owners and replication ACLs. Temporary plaintext was removed. A transient SSH timeout interrupted smoke cleanup verification; cancellation was confirmed after reconnecting, and the exact original preferences were recovered from the encrypted predeployment backup and restored. No test reminder delivery remains pending.

Verification: fresh activity-service 96 tests and web 169 tests passed. All five CI image builds succeeded. Live HTTPS/PWA/authentication/reminder/HTTP–Kafka round-trip/cleanup/logout smoke passed 11 checks. The deployed entry is `/todorok/assets/index-BCp2kpbO.js`; eight script assets and the new owner-scoped program-session context were checked. Existing account/enrollment counts and program positions were retained; no test enrollment was created.

The actual authenticated browser showed week/session selectors, task-menu deletion, and the repeat button on the current final workout session, with zero console errors. No real workout was saved or program position changed during browser verification. The existing PWA initially displayed cached assets; reloading loaded the new entry and controls. Installed iPhone behavior remains a separate device check.

Local evidence is under ignored `.local/company-server/program-release-20261026/`: `result.json`, `verification.json`, `smoke-report.json`, `feature-check.json`, `browser-verification.json`, and `deployed-status-editor.png`. Encrypted backup material remains in the ACL-restricted private directory and is not committed.
