# TODOROK Project Foundation Implementation Plan

**Goal:** 세 개의 Spring 서비스, Vue PWA 셸, PostgreSQL, Kafka와 CI가 함께 동작하는 최소 모노레포 기반을 만든다.

**Architecture:** 하나의 저장소에서 서비스별 독립 Spring Boot 애플리케이션과 공용 이벤트 계약 모듈을 관리한다. 로컬·초기 운영은 Docker Compose의 PostgreSQL 17.11과 Kafka 4.3.1 KRaft를 공유하되 각 서비스는 별도 스키마와 계정을 사용한다.

**Tech Stack:** Java 25 LTS, Spring Boot 4.1.1, Gradle 9.7.1 Kotlin DSL, Vue 3, TypeScript, Vite 8, Node.js 24 LTS, pnpm 10, PostgreSQL 17.11, Apache Kafka 4.3.1, Docker Compose v2.

**Spec:** `docs/PRD.md` §16~20, `docs/ISSUE_ROADMAP.md` F-001.

## Global Constraints

- 요구사항과 아키텍처 변경은 `docs/PRD.md`를 먼저 수정한다.
- `main`과 `develop` 직접 커밋을 금지하고 기능 이슈 브랜치와 Pull Request를 사용한다.
- 프로젝트 기록 정책을 모든 파일과 Git·GitHub 메타데이터에 적용한다.
- 서비스는 서로의 DB 스키마를 직접 조회하지 않는다.
- DB 변경과 이벤트 발행은 이후 기능에서 outbox로 연결할 수 있게 기반을 분리한다.
- 로컬 기반은 Windows와 macOS에서 같은 명령으로 실행되어야 한다.
- 4GB 운영 목표 때문에 각 JVM의 컨테이너 메모리 상한을 명시한다.

---

## File Map

```text
apps/web/                         Vue PWA
libs/event-contracts/             서비스 간 이벤트 DTO와 직렬화 계약
services/planner-service/         인증·Task·일정·메모 경계
services/activity-service/        운동·공부·클라이밍 경계
services/notification-service/    푸시·재시도 경계
infra/docker/                     PostgreSQL·Kafka·서비스 Compose
infra/nginx/                      정적 파일과 API reverse proxy
scripts/                          로컬 검증 진입점
.github/workflows/                CI
```

### Task 1: Gradle 모노레포와 버전 고정

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`

**Interfaces:**
- Produces: Gradle 프로젝트 `:libs:event-contracts`, `:services:planner-service`, `:services:activity-service`, `:services:notification-service`.

- [ ] **Step 1: 멀티프로젝트 구성을 작성한다**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "todorok"
include(
    "libs:event-contracts",
    "services:planner-service",
    "services:activity-service",
    "services:notification-service",
)
```

- [ ] **Step 2: 버전 카탈로그를 작성한다**

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "4.1.1"
dependency-management = "1.1.7"

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
dependency-management = { id = "io.spring.dependency-management", version.ref = "dependency-management" }
```

- [ ] **Step 3: Java 25와 공통 테스트 정책을 루트 빌드에 고정한다**

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
```

- [ ] **Step 4: Gradle Wrapper를 9.7.1로 생성한다**

Run: `gradle wrapper --gradle-version 9.7.1 --distribution-type bin`

Expected: `./gradlew --version` 출력에 Gradle 9.7.1과 Java 25가 표시된다.

- [ ] **Step 5: 설정 검증을 실행한다**

Run: `./gradlew projects`

Expected: 네 개 하위 프로젝트가 모두 표시되고 BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋한다**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat
git commit -m "chore(build): establish java monorepo toolchain"
```

### Task 2: 공용 이벤트 계약 모듈

**Files:**
- Create: `libs/event-contracts/build.gradle.kts`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/EventEnvelope.java`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/EventType.java`
- Create: `libs/event-contracts/src/test/java/io/todorok/contracts/EventEnvelopeTest.java`

**Interfaces:**
- Produces: `EventEnvelope<T>(UUID eventId, EventType type, int version, Instant occurredAt, UUID userId, T payload)`.

- [ ] **Step 1: 직렬화 계약 실패 테스트를 작성한다**

```kotlin
// libs/event-contracts/build.gradle.kts
plugins { `java-library` }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api("com.fasterxml.jackson.core:jackson-annotations")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
```

```java
package io.todorok.contracts;

class EventEnvelopeTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preserves_identity_and_version_during_json_round_trip() throws Exception {
        var envelope = new EventEnvelope<>(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                EventType.TASK_SCHEDULED,
                1,
                Instant.parse("2026-08-31T00:00:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Map.of("taskId", "00000000-0000-0000-0000-000000000003")
        );

        var json = mapper.writeValueAsString(envelope);
        var restored = mapper.readTree(json);

        assertThat(restored.path("eventId").asText()).isEqualTo(envelope.eventId().toString());
        assertThat(restored.path("version").asInt()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: 테스트가 컴파일 실패하는지 확인한다**

Run: `./gradlew :libs:event-contracts:test`

Expected: `EventEnvelope`와 `EventType`이 없어 컴파일 실패.

- [ ] **Step 3: 최소 계약 타입을 구현한다**

```java
package io.todorok.contracts;

public record EventEnvelope<T>(
        UUID eventId,
        EventType type,
        int version,
        Instant occurredAt,
        UUID userId,
        T payload
) {}
```

```java
package io.todorok.contracts;

public enum EventType {
    TASK_SCHEDULED,
    TASK_ROLLED_OVER,
    ACTIVITY_COMPLETED,
    ACTIVITY_SKIPPED,
    ROUTINE_ADVANCED,
    NOTIFICATION_REQUESTED
}
```

- [ ] **Step 4: 계약 테스트를 실행한다**

Run: `./gradlew :libs:event-contracts:test`

Expected: PASS.

- [ ] **Step 5: 커밋한다**

```bash
git add libs/event-contracts
git commit -m "feat(contracts): define versioned domain event envelope"
```

### Task 3: planner-service 기동과 헬스 계약

**Files:**
- Create: `services/planner-service/build.gradle.kts`
- Create: `services/planner-service/src/main/java/io/todorok/planner/PlannerApplication.java`
- Create: `services/planner-service/src/main/resources/application.yml`
- Create: `services/planner-service/src/test/java/io/todorok/planner/PlannerApplicationTest.java`

**Interfaces:**
- Produces: HTTP `GET /actuator/health` on port `8081`.

- [ ] **Step 1: 애플리케이션 컨텍스트 테스트를 작성한다**

```kotlin
// services/planner-service/build.gradle.kts
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":libs:event-contracts"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

```java
package io.todorok.planner;

@SpringBootTest
class PlannerApplicationTest {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: 테스트 실패를 확인한다**

Run: `./gradlew :services:planner-service:test`

Expected: 애플리케이션과 빌드 파일이 없어 실패.

- [ ] **Step 3: Spring Boot 애플리케이션과 최소 설정을 구현한다**

```java
package io.todorok.planner;

@SpringBootApplication
public class PlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }
}
```

```yaml
server:
  port: 8081
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 4: 테스트를 실행한다**

Run: `./gradlew :services:planner-service:test`

Expected: PASS.

- [ ] **Step 5: 커밋한다**

```bash
git add services/planner-service
git commit -m "feat(planner): add service application foundation"
```

### Task 4: activity-service 기동과 헬스 계약

**Files:**
- Create: `services/activity-service/build.gradle.kts`
- Create: `services/activity-service/src/main/java/io/todorok/activity/ActivityApplication.java`
- Create: `services/activity-service/src/main/resources/application.yml`
- Create: `services/activity-service/src/test/java/io/todorok/activity/ActivityApplicationTest.java`

**Interfaces:**
- Produces: HTTP `GET /actuator/health` on port `8082`.

- [ ] **Step 1: 빌드와 컨텍스트 테스트를 작성한다**

```kotlin
// services/activity-service/build.gradle.kts
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":libs:event-contracts"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

```java
package io.todorok.activity;

@SpringBootTest
class ActivityApplicationTest {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: `./gradlew :services:activity-service:test`가 애플리케이션 부재로 실패하는지 확인한다**
- [ ] **Step 3: 애플리케이션과 설정을 작성한다**

```java
package io.todorok.activity;

@SpringBootApplication
public class ActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}
```

```yaml
server:
  port: 8082
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 4: `./gradlew :services:activity-service:test`가 PASS인지 확인한다**
- [ ] **Step 5: 커밋한다**

```bash
git add services/activity-service
git commit -m "feat(activity): add service application foundation"
```

### Task 5: notification-service 기동과 헬스 계약

**Files:**
- Create: `services/notification-service/build.gradle.kts`
- Create: `services/notification-service/src/main/java/io/todorok/notification/NotificationApplication.java`
- Create: `services/notification-service/src/main/resources/application.yml`
- Create: `services/notification-service/src/test/java/io/todorok/notification/NotificationApplicationTest.java`

**Interfaces:**
- Produces: HTTP `GET /actuator/health` on port `8083`.

- [ ] **Step 1: 빌드와 컨텍스트 테스트를 작성한다**

```kotlin
// services/notification-service/build.gradle.kts
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    implementation(project(":libs:event-contracts"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

```java
package io.todorok.notification;

@SpringBootTest
class NotificationApplicationTest {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: `./gradlew :services:notification-service:test`가 애플리케이션 부재로 실패하는지 확인한다**
- [ ] **Step 3: 애플리케이션과 설정을 작성한다**

```java
package io.todorok.notification;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
```

```yaml
server:
  port: 8083
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- [ ] **Step 4: `./gradlew :services:notification-service:test`가 PASS인지 확인한다**
- [ ] **Step 5: 커밋한다**

```bash
git add services/notification-service
git commit -m "feat(notification): add service application foundation"
```

### Task 6: PostgreSQL·Kafka 로컬 인프라

**Files:**
- Create: `infra/docker/compose.yml`
- Create: `infra/docker/postgres/init/001-create-service-roles.sh`
- Create: `.env.example`

**Interfaces:**
- Produces: PostgreSQL `localhost:5432`, Kafka `localhost:9092`.
- Produces: `planner`, `activity`, `notification` 스키마와 동일 이름 DB 계정.

- [ ] **Step 1: 스키마·권한 SQL을 작성한다**

```bash
#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=planner_password="$PLANNER_DB_PASSWORD" \
  --set=activity_password="$ACTIVITY_DB_PASSWORD" \
  --set=notification_password="$NOTIFICATION_DB_PASSWORD" <<'EOSQL'
create role planner_app login password :'planner_password';
create role activity_app login password :'activity_password';
create role notification_app login password :'notification_password';
create schema planner authorization planner_app;
create schema activity authorization activity_app;
create schema notification authorization notification_app;
revoke create on schema public from public;
EOSQL
```

- [ ] **Step 2: Compose에 PostgreSQL 17.11과 Kafka 4.3.1 KRaft를 작성한다**

```yaml
services:
  postgres:
    image: postgres:17.11-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 20
  kafka:
    image: apache/kafka:4.3.1
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 10s
      timeout: 5s
      retries: 20
```

- [ ] **Step 3: `.env.example`에 세 DB 비밀번호와 PostgreSQL 관리자 비밀번호 키를 작성한다**
- [ ] **Step 4: 인프라를 기동한다**

Run: `docker compose --env-file .env -f infra/docker/compose.yml up -d`

Expected: `postgres`와 `kafka`가 healthy.

- [ ] **Step 5: 스키마와 Kafka 연결을 확인한다**

Run: `docker compose -f infra/docker/compose.yml exec postgres psql -U postgres -c "select schema_name from information_schema.schemata where schema_name in ('planner','activity','notification');"`

Expected: 세 스키마가 모두 출력된다.

- [ ] **Step 6: 커밋한다**

```bash
git add infra/docker .env.example scripts
git commit -m "chore(infra): add local postgres and kafka stack"
```

### Task 7: Vue PWA 셸

**Files:**
- Create: `apps/web/package.json`
- Create: `apps/web/vite.config.ts`
- Create: `apps/web/src/main.ts`
- Create: `apps/web/src/App.vue`
- Create: `apps/web/src/App.test.ts`
- Create: `apps/web/public/manifest.webmanifest`

**Interfaces:**
- Produces: PWA route `/` and manifest `/manifest.webmanifest`.

- [ ] **Step 1: 앱 이름 렌더링 테스트를 작성한다**

```ts
it('renders the approved product name', () => {
  render(App)
  expect(screen.getByRole('heading', { name: '토도록' })).toBeVisible()
})
```

- [ ] **Step 2: `pnpm --dir apps/web test`가 App 부재로 실패하는지 확인한다**
- [ ] **Step 3: Vue 3 앱 셸과 `토도록` 제목을 구현한다**
- [ ] **Step 4: manifest에 `name`, `short_name`, `id`, `start_url`, `display: standalone`을 작성한다**
- [ ] **Step 5: `pnpm --dir apps/web test`와 `pnpm --dir apps/web build`가 PASS인지 확인한다**
- [ ] **Step 6: 커밋한다**

```bash
git add apps/web
git commit -m "feat(web): add installable pwa shell"
```

### Task 8: Nginx와 전체 서비스 Compose

**Files:**
- Create: `infra/nginx/nginx.conf`
- Create: `services/planner-service/Dockerfile`
- Create: `services/activity-service/Dockerfile`
- Create: `services/notification-service/Dockerfile`
- Modify: `infra/docker/compose.yml`

**Interfaces:**
- Produces: `/api/planner/actuator/health`, `/api/activity/actuator/health`, 내부 notification health.

- [ ] **Step 1: Nginx smoke test 스크립트를 작성한다**
- [ ] **Step 2: 세 서비스 멀티스테이지 Dockerfile을 작성하고 JVM 메모리 상한을 설정한다**
- [ ] **Step 3: Nginx가 정적 웹과 planner/activity 경로를 reverse proxy하도록 작성한다**
- [ ] **Step 4: Compose에 세 서비스와 Nginx를 추가한다**
- [ ] **Step 5: `docker compose up -d --build` 후 두 공개 health endpoint가 HTTP 200인지 확인한다**
- [ ] **Step 6: `docker stats --no-stream`에서 총 메모리가 3.5GB 미만인지 확인한다**
- [ ] **Step 7: 커밋한다**

```bash
git add infra/nginx infra/docker services/*/Dockerfile scripts
git commit -m "chore(runtime): compose services behind nginx"
```

### Task 9: CI와 기록 정책 검사

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `scripts/check-record-policy.mjs`
- Create: `scripts/verify-all.mjs`

**Interfaces:**
- Produces: PR마다 Java 테스트, 웹 테스트·빌드, Compose 검증, 기록 정책 검사를 수행하는 CI.

- [ ] **Step 1: 금지 문자열이 있으면 실패하는 검사 스크립트를 작성한다**

```js
import { spawnSync } from 'node:child_process'

const raw = process.env.RECORD_POLICY_FORBIDDEN_TERMS
if (!raw?.trim()) throw new Error('RECORD_POLICY_FORBIDDEN_TERMS is required')
const escaped = raw.split(',').map((value) => value.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
const result = spawnSync('rg', ['-n', '-i', escaped.join('|'), '.'], { stdio: 'inherit' })
process.exit(result.status === 0 ? 1 : 0)
```

- [ ] **Step 2: 로컬 통합 검증 스크립트를 작성한다**

```js
import { spawnSync } from 'node:child_process'

const commands = [
  ['./gradlew', ['test']],
  ['pnpm', ['--dir', 'apps/web', 'test']],
  ['pnpm', ['--dir', 'apps/web', 'build']],
  ['node', ['scripts/check-record-policy.mjs']],
]
for (const [command, args] of commands) {
  const result = spawnSync(command, args, { stdio: 'inherit', shell: process.platform === 'win32' })
  if (result.status !== 0) process.exit(result.status ?? 1)
}
```

- [ ] **Step 3: GitHub Actions에 Java 25·Node 24·pnpm 10 환경을 구성한다**
- [ ] **Step 4: `node scripts/verify-all.mjs`가 Windows와 macOS에서 PASS인지 확인한다**
- [ ] **Step 5: 커밋한다**

```bash
git add .github/workflows scripts
git commit -m "ci: verify services web build and repository policy"
```

## Final Verification

- [ ] `./gradlew test` PASS
- [ ] `pnpm --dir apps/web test` PASS
- [ ] `pnpm --dir apps/web build` PASS
- [ ] `docker compose --env-file .env -f infra/docker/compose.yml up -d --build` 성공
- [ ] planner·activity health endpoint HTTP 200
- [ ] Kafka와 PostgreSQL healthy
- [ ] 전체 컨테이너 메모리 3.5GB 미만
- [ ] 기록 정책 검사 PASS
- [ ] F-001 수용 기준을 PR 본문에서 모두 체크
