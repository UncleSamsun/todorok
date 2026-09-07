# API·이벤트 계약 기반 구현 계획

> 각 Task는 체크박스 순서대로 진행한다. 실패 테스트 확인, 최소 구현, 전체 검증, 파일을 지정한 커밋을 한 단위로 유지한다.

**목표:** planner·activity 핵심 REST와 네 이벤트의 단일 계약을 만들고 Java·TypeScript 생성물과 CI drift 검사를 제공한다.

**아키텍처:** `contracts/`의 OpenAPI·JSON Schema를 유일한 원본으로 사용한다. OpenAPI Generator 7.24.0이 Spring Boot 4 interface·DTO와 TypeScript Fetch client를 만들고, 저장소에 포함된 생성물을 CI에서 재생성해 비교한다. 이벤트 Java record와 fixture는 JSON Schema Validator 3.0.6으로 Draft 2020-12 계약을 검증한다.

**기술:** Java 25, Spring Boot 4.1.1, Jackson 3, Gradle Kotlin DSL, OpenAPI 3.1, OpenAPI Generator 7.24.0, TypeScript 7, JSON Schema Draft 2020-12, Node.js 24.

**설계:** `docs/plans/2026-09-02-contract-foundation-design.md`

## 전체 제약

- 실제 인증·Task·Activity 업무 로직과 DB 구현은 포함하지 않는다.
- OpenAPI 외부 경로는 `/api/planner/v1/**`, `/api/activity/v1/**`로 고정한다.
- 생성 Spring 코드는 `useSpringBoot4=true`, `useJackson3=true`, `useJakartaEe=true`를 사용한다.
- 생성 TypeScript client는 `typescript-fetch`와 `dateLibrary=string`을 사용한다.
- 생성 시각과 문서·예제 테스트 출력을 끄고 Windows·Linux에서 같은 결과를 만든다.
- 오류는 RFC 9457 기본 필드와 `code`, `traceId`, `retryable`, `fieldErrors`를 사용한다.
- 이벤트 envelope는 `eventId`, `type`, `version`, `aggregateVersion`, `occurredAt`, `userId`, `payload`를 가진다.
- 생성 파일을 직접 수정하지 않는다.
- 저장소와 Git 기록에 금지된 작성 주체·도구 이름을 남기지 않는다.

---

### Task 1: 결정적 계약 생성 기반

**Files:**
- Create: `contracts/openapi/common-v1.yaml`
- Create: `contracts/openapi/planner-v1.yaml`
- Create: `contracts/openapi/activity-v1.yaml`
- Create: `contracts/generator/planner-spring.yaml`
- Create: `contracts/generator/activity-spring.yaml`
- Create: `contracts/generator/planner-typescript.yaml`
- Create: `contracts/generator/activity-typescript.yaml`
- Create: `scripts/generate-contracts.mjs`
- Create: `scripts/contracts-generation.test.mjs`
- Modify: `build.gradle.kts`
- Modify: `package.json`

**Interfaces:**
- Consumes: Gradle wrapper, OpenAPI Generator 7.24.0.
- Produces: `pnpm contracts:generate`, `node scripts/generate-contracts.mjs --output-root .tmp/contracts-generated`, Gradle task `generateContracts`.

- [ ] **Step 1: 생성 진입점 실패 테스트 작성**

```js
import assert from 'node:assert/strict'
import { mkdtemp, stat } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('네 계약 생성기가 결정된 출력 루트를 만든다', async () => {
  const outputRoot = await mkdtemp(path.join(os.tmpdir(), 'todorok-contracts-'))
  const result = spawnSync(
    process.execPath,
    ['scripts/generate-contracts.mjs', '--output-root', outputRoot],
    { cwd: path.resolve('.'), encoding: 'utf8' },
  )

  assert.equal(result.status, 0, result.stderr)
  await stat(path.join(outputRoot, 'services/planner-service/src/generated/java'))
  await stat(path.join(outputRoot, 'services/activity-service/src/generated/java'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/planner'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/activity'))
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/contracts-generation.test.mjs`

Expected: FAIL, `scripts/generate-contracts.mjs` 파일이 없어 exit code가 0이 아니다.

- [ ] **Step 3: 생성 설정 작성**

Spring 설정 네 가지 핵심 값은 다음과 같이 고정한다.

```yaml
generatorName: spring
additionalProperties:
  interfaceOnly: true
  skipDefaultInterface: true
  useSpringBoot4: true
  useJackson3: true
  useJakartaEe: true
  useTags: true
  useSwaggerUI: false
  documentationProvider: none
  annotationLibrary: none
  hideGenerationTimestamp: true
  openApiNullable: false
```

planner 설정은 `apiPackage: io.todorok.planner.api`, `modelPackage: io.todorok.planner.api.model`, `sourceFolder: java`를 사용하고 출력 루트는 `services/planner-service/src/generated`로 고정한다. activity 설정은 `io.todorok.activity.api`, `io.todorok.activity.api.model`, 같은 source folder와 `services/activity-service/src/generated` 출력 루트를 사용한다. 각 출력 루트는 생성 전에 정리하고 생성기의 `.gitignore`는 제외해 삭제된 모델과 환경별 파일이 남지 않게 한다.

TypeScript 설정은 다음을 공통으로 사용한다.

```yaml
generatorName: typescript-fetch
additionalProperties:
  dateLibrary: string
  supportsES6: true
  stringEnums: true
  enumUnknownDefaultCase: true
  hideGenerationTimestamp: true
  npmName: '@todorok/api-client'
```

- [ ] **Step 4: Gradle 생성 task 작성**

`build.gradle.kts`에 생성기 plugin과 네 task를 등록한다. 출력 기준은 `contractsOutputRoot` property가 있으면 해당 경로, 없으면 저장소 루트다.

```kotlin
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    id("org.openapi.generator") version "7.24.0"
}

val contractsOutputRoot = providers.gradleProperty("contractsOutputRoot")
    .orElse(layout.projectDirectory.asFile.absolutePath)

fun registerContractTask(
    taskName: String,
    generator: String,
    spec: String,
    config: String,
    output: String,
) = tasks.register<GenerateTask>(taskName) {
    generatorName.set(generator)
    inputSpec.set(layout.projectDirectory.file(spec).asFile.absolutePath)
    configFile.set(layout.projectDirectory.file(config).asFile.absolutePath)
    outputDir.set(contractsOutputRoot.map { root -> "$root/$output" })
}

tasks.register("generateContracts") {
    dependsOn(
        "generatePlannerSpring",
        "generateActivitySpring",
        "generatePlannerTypeScript",
        "generateActivityTypeScript",
    )
}
```

- [ ] **Step 5: 운영체제 독립 wrapper 작성**

```js
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import process from 'node:process'

const outputIndex = process.argv.indexOf('--output-root')
const outputRoot = outputIndex >= 0
  ? path.resolve(process.argv[outputIndex + 1])
  : path.resolve('.')
const wrapper = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
const result = spawnSync(
  wrapper,
  [`-PcontractsOutputRoot=${outputRoot}`, 'generateContracts', '--no-daemon'],
  { cwd: path.resolve('.'), encoding: 'utf8', shell: process.platform === 'win32' },
)
process.stdout.write(result.stdout ?? '')
process.stderr.write(result.stderr ?? '')
process.exit(result.status ?? 1)
```

- [ ] **Step 6: 최소 OpenAPI 원본으로 생성 테스트 통과**

세 OpenAPI 문서는 `openapi: 3.1.0`, `info`, `servers`, 빈 `paths`, 필요한 `components`로 시작한다. `common-v1.yaml`에는 `ProblemDetails`와 `FieldError`를 먼저 정의한다.

Run: `node --test scripts/contracts-generation.test.mjs`

Expected: PASS.

- [ ] **Step 7: 커밋**

```bash
git add build.gradle.kts package.json contracts/openapi contracts/generator scripts/generate-contracts.mjs scripts/contracts-generation.test.mjs
git commit -m "build(contracts): 결정적 계약 생성 기반 추가"
```

---

### Task 2: 공통 오류와 planner 핵심 API 계약

**Files:**
- Modify: `contracts/openapi/common-v1.yaml`
- Modify: `contracts/openapi/planner-v1.yaml`
- Create: `scripts/openapi-contract.test.mjs`
- Regenerate: `services/planner-service/src/generated/java/**`
- Regenerate: `packages/api-client/src/generated/planner/**`

**Interfaces:**
- Consumes: `pnpm contracts:generate`.
- Produces: `AuthApi`, `CalendarApi`, `TaskApi`, planner TypeScript client, `ProblemDetails`.

- [ ] **Step 1: planner 계약 실패 테스트 작성**

```js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('planner 계약은 핵심 operationId와 공통 오류 참조를 제공한다', async () => {
  const yaml = await readFile('contracts/openapi/planner-v1.yaml', 'utf8')
  for (const operationId of [
    'login', 'refreshSession', 'logout', 'getCalendarSummary',
    'getDayDetail', 'createTask', 'updateTask', 'deleteTask',
    'completeTask', 'reopenTask',
  ]) {
    assert.match(yaml, new RegExp(`operationId: ${operationId}\\b`))
  }
  assert.match(yaml, /common-v1\.yaml#\/components\/schemas\/ProblemDetails/)
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/openapi-contract.test.mjs`

Expected: FAIL, `operationId: login`이 없다.

- [ ] **Step 3: 공통 오류 schema 완성**

`ProblemDetails` 필수 확장 필드를 다음처럼 고정한다.

```yaml
ProblemDetails:
  type: object
  additionalProperties: false
  required: [type, title, status, code, traceId, retryable]
  properties:
    type: { type: string, format: uri-reference }
    title: { type: string }
    status: { type: integer, minimum: 400, maximum: 599 }
    detail: { type: string }
    instance: { type: string, format: uri-reference }
    code: { type: string, pattern: '^[A-Z][A-Z0-9_]+$' }
    traceId: { type: string, minLength: 1 }
    retryable: { type: boolean }
    fieldErrors:
      type: array
      items: { $ref: '#/components/schemas/FieldError' }
```

- [ ] **Step 4: planner operation과 DTO 작성**

정확한 operationId는 실패 테스트 목록을 사용한다. Task DTO는 `taskId`, `userId`, `title`, `taskType`, `scheduledDate`, `status`, `version`을 포함하고 `version`은 update·complete·reopen command에서 필수다. 달력 범위는 `from`, `to`를 필수 query로 받고 최대 42일 제한을 description과 400 응답에 명시한다.

- [ ] **Step 5: 생성·검증**

Run: `pnpm contracts:generate`

Run: `node --test scripts/openapi-contract.test.mjs scripts/contracts-generation.test.mjs`

Run: `gradlew.bat :services:planner-service:compileJava --no-daemon`

Expected: 모두 PASS.

- [ ] **Step 6: 커밋**

```bash
git add contracts/openapi/common-v1.yaml contracts/openapi/planner-v1.yaml scripts/openapi-contract.test.mjs services/planner-service/src/generated packages/api-client/src/generated/planner
git commit -m "feat(contracts): planner 핵심 API와 공통 오류 계약 추가"
```

---

### Task 3: activity 핵심 API 계약

**Files:**
- Modify: `contracts/openapi/activity-v1.yaml`
- Modify: `scripts/openapi-contract.test.mjs`
- Regenerate: `services/activity-service/src/generated/java/**`
- Regenerate: `packages/api-client/src/generated/activity/**`

**Interfaces:**
- Consumes: `ProblemDetails`, UUID·LocalDate·Instant 공통 schema.
- Produces: `ActivityApi`, `CreateActivityRequest`, `ActivityResponse`, `VoidActivityRequest`, activity TypeScript client.

- [ ] **Step 1: activity 계약 실패 테스트 추가**

```js
test('activity 계약은 생성·조회·목록·무효화 operation을 제공한다', async () => {
  const yaml = await readFile('contracts/openapi/activity-v1.yaml', 'utf8')
  for (const operationId of [
    'createActivity', 'getActivity', 'listActivities', 'voidActivity',
  ]) {
    assert.match(yaml, new RegExp(`operationId: ${operationId}\\b`))
  }
  assert.match(yaml, /activityType:/)
  assert.match(yaml, /additionalProperties: true/)
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/openapi-contract.test.mjs`

Expected: FAIL, `operationId: createActivity`가 없다.

- [ ] **Step 3: activity 계약 작성**

`CreateActivityRequest`는 `taskId`, `activityType`, `performedAt`, `detail`을 필수로 한다. `activityType`은 `WORKOUT`, `STUDY`, `CLIMBING` enum이고 `detail`은 `additionalProperties: true`인 JSON object다. 초기 TypeScript client에서 지원이 제한된 `oneOf`은 사용하지 않는다. 목록은 `date`, `cursor`, `limit`을 받고 `nextCursor`를 반환한다. 무효화 요청은 `reason`과 optimistic `version`을 필수로 한다.

- [ ] **Step 4: 생성·컴파일·타입 검사**

Run: `pnpm contracts:generate`

Run: `node --test scripts/openapi-contract.test.mjs`

Run: `gradlew.bat :services:activity-service:compileJava --no-daemon`

Run: `pnpm --dir packages/api-client test`

Expected: 모두 PASS.

- [ ] **Step 5: 커밋**

```bash
git add contracts/openapi/activity-v1.yaml scripts/openapi-contract.test.mjs services/activity-service/src/generated packages/api-client/src/generated/activity
git commit -m "feat(contracts): activity 핵심 API 계약 추가"
```

---

### Task 4: 생성 Java·TypeScript 소비 경계 연결

**Files:**
- Modify: `services/planner-service/build.gradle.kts`
- Modify: `services/activity-service/build.gradle.kts`
- Modify: `packages/api-client/src/index.ts`
- Modify: `packages/api-client/package.json`
- Create: `packages/api-client/src/index.test-d.ts`

**Interfaces:**
- Consumes: 생성 Java source와 planner·activity TypeScript namespace.
- Produces: 서비스 compile classpath와 `@todorok/api-client` 공개 export.

- [ ] **Step 1: TypeScript 공개 export 실패 검사 작성**

```ts
import type { Configuration as PlannerConfiguration } from './generated/planner'
import type { Configuration as ActivityConfiguration } from './generated/activity'

const plannerConfiguration: PlannerConfiguration = { basePath: '/api/planner/v1' }
const activityConfiguration: ActivityConfiguration = { basePath: '/api/activity/v1' }

void plannerConfiguration
void activityConfiguration
```

- [ ] **Step 2: 실패 확인**

Run: `pnpm --dir packages/api-client test`

Expected: FAIL, generated namespace가 export되지 않는다.

- [ ] **Step 3: Java source set 연결**

두 서비스에 다음 source directory와 생성 선행 관계를 각각 추가한다.

```kotlin
sourceSets {
    main {
        java.srcDir("src/generated/java")
    }
}

tasks.named("compileJava") {
    dependsOn(rootProject.tasks.named("generateContracts"))
}
```

- [ ] **Step 4: TypeScript barrel export 연결**

```ts
export * as planner from './generated/planner'
export * as activity from './generated/activity'
export type { ApiRequest, ApiTransport } from './transport'
```

기존 transport interface는 `src/transport.ts`로 이동하고 `src/index.ts`는 export만 담당한다.

- [ ] **Step 5: 전체 생성물 컴파일 확인**

Run: `gradlew.bat :services:planner-service:compileJava :services:activity-service:compileJava --no-daemon`

Run: `pnpm --dir packages/api-client test`

Expected: 모두 PASS.

- [ ] **Step 6: 커밋**

```bash
git add services/planner-service/build.gradle.kts services/activity-service/build.gradle.kts packages/api-client
git commit -m "build(contracts): 생성 계약 소비 경계 연결"
```

---

### Task 5: 이벤트 JSON Schema·fixture·Java record

**Files:**
- Create: `contracts/events/envelope/v1.schema.json`
- Create: `contracts/events/task-scheduled/v1.schema.json`
- Create: `contracts/events/task-changed/v1.schema.json`
- Create: `contracts/events/activity-completed/v1.schema.json`
- Create: `contracts/events/activity-voided/v1.schema.json`
- Create: `contracts/fixtures/events/task-scheduled/v1-valid.json`
- Create: `contracts/fixtures/events/task-scheduled/v1-invalid.json`
- Create: `contracts/fixtures/events/task-changed/v1-valid.json`
- Create: `contracts/fixtures/events/task-changed/v1-invalid.json`
- Create: `contracts/fixtures/events/activity-completed/v1-valid.json`
- Create: `contracts/fixtures/events/activity-completed/v1-invalid.json`
- Create: `contracts/fixtures/events/activity-voided/v1-valid.json`
- Create: `contracts/fixtures/events/activity-voided/v1-invalid.json`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/events/TaskScheduled.java`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/events/TaskChanged.java`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/events/ActivityCompleted.java`
- Create: `libs/event-contracts/src/main/java/io/todorok/contracts/events/ActivityVoided.java`
- Create: `libs/event-contracts/src/test/java/io/todorok/contracts/EventSchemaContractTest.java`
- Modify: `libs/event-contracts/src/main/java/io/todorok/contracts/EventType.java`
- Modify: `libs/event-contracts/src/main/java/io/todorok/contracts/EventEnvelope.java`
- Modify: `libs/event-contracts/build.gradle.kts`

**Interfaces:**
- Consumes: `EventEnvelope<T>`.
- Produces: 네 payload record, schema·fixture 계약 검사.

- [ ] **Step 1: 정상·오류 fixture 실패 테스트 작성**

```java
@ParameterizedTest
@CsvSource({
        "task-scheduled,TASK_SCHEDULED",
        "task-changed,TASK_CHANGED",
        "activity-completed,ACTIVITY_COMPLETED",
        "activity-voided,ACTIVITY_VOIDED"
})
void validatesValidAndInvalidFixtures(String directory, String eventType) throws Exception {
    var schema = schema(directory);
    var valid = mapper.readTree(fixture(directory, "v1-valid.json"));
    var invalid = mapper.readTree(fixture(directory, "v1-invalid.json"));

    assertThat(schema.validate(valid)).isEmpty();
    assertThat(schema.validate(invalid)).isNotEmpty();
    assertThat(valid.path("type").asText()).isEqualTo(eventType);
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :libs:event-contracts:test --tests '*EventSchemaContractTest' --no-daemon`

Expected: FAIL, schema와 fixture resource가 없다.

- [ ] **Step 3: validator 의존성과 Draft 2020-12 설정 추가**

```kotlin
testImplementation("com.networknt:json-schema-validator:3.0.6")
```

Jackson 3 dependency와 `ObjectMapper` package를 사용하도록 기존 envelope test를 함께 전환한다. `sourceSets.test.resources`에 root `contracts/`를 추가해 schema와 fixture를 classpath resource로 읽는다. Schema registry 설정에서 Draft 2020-12와 format assertion을 활성화한다. 모든 schema는 `$schema`, `$id`, `type`, `required`, `additionalProperties: false`를 가진다.

- [ ] **Step 4: schema·fixture·record 작성**

record signature를 다음으로 고정한다.

```java
public record TaskScheduled(UUID taskId, String taskType, LocalDate scheduledDate, String status) {}
public record TaskChanged(UUID taskId, String taskType, LocalDate scheduledDate, String status, String changeType) {}
public record ActivityCompleted(UUID activityId, UUID taskId, String activityType, Instant completedAt, String outcome) {}
public record ActivityVoided(UUID activityId, UUID taskId, Instant voidedAt, String reason) {}
```

- [ ] **Step 5: Java 직렬화 계약 검사 추가**

각 record를 고정 UUID·시각으로 감싼 `EventEnvelope`를 Jackson 3으로 직렬화하고 대응 schema가 오류 없이 검증하는 parameterized test를 작성한다.

- [ ] **Step 6: 테스트 통과 확인**

Run: `gradlew.bat :libs:event-contracts:test --no-daemon`

Expected: 정상 fixture·record 직렬화 PASS, 오류 fixture는 의도한 validation message로 실패.

- [ ] **Step 7: 커밋**

```bash
git add contracts/events contracts/fixtures libs/event-contracts
git commit -m "feat(contracts): 핵심 이벤트 스키마와 직렬화 계약 추가"
```

---

### Task 6: 생성 drift와 전체 검증 연결

**Files:**
- Create: `scripts/check-contract-drift.mjs`
- Create: `scripts/check-contract-drift.test.mjs`
- Modify: `scripts/verify-all.mjs`
- Modify: `package.json`
- Modify: `.github/workflows/ci.yml`
- Modify: `README.md`

**Interfaces:**
- Consumes: `node scripts/generate-contracts.mjs --output-root .tmp/contracts-generated`.
- Produces: `pnpm contracts:check`, 전체 검증의 계약 gate.

- [ ] **Step 1: drift 검출 실패 테스트 작성**

```js
import assert from 'node:assert/strict'
import { mkdtemp, mkdir, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { compareTrees } from './check-contract-drift.mjs'

test('생성 파일 내용이 다르면 경로를 반환한다', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'todorok-drift-'))
  const expected = path.join(root, 'expected')
  const actual = path.join(root, 'actual')
  await mkdir(expected)
  await mkdir(actual)
  await writeFile(path.join(expected, 'Api.java'), 'version-one')
  await writeFile(path.join(actual, 'Api.java'), 'version-two')

  assert.deepEqual(await compareTrees(expected, actual), ['Api.java'])
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/check-contract-drift.test.mjs`

Expected: FAIL, `compareTrees` export가 없다.

- [ ] **Step 3: 결정적 tree 비교 구현**

`compareTrees(expected, actual)`은 재귀 파일 목록을 `/` 구분자로 정렬하고 SHA-256을 비교한다. 생성 시각 파일과 임시 cache는 생성 설정에서 제외하며 비교 함수에서는 예외 목록을 두지 않는다. 차이가 있으면 경로를 한 줄씩 출력하고 exit code 1로 종료한다.

- [ ] **Step 4: package script와 전체 검증 연결**

```json
{
  "scripts": {
    "contracts:generate": "node scripts/generate-contracts.mjs",
    "contracts:check": "node scripts/check-contract-drift.mjs",
    "test:contracts": "node --test scripts/contracts-generation.test.mjs scripts/openapi-contract.test.mjs scripts/check-contract-drift.test.mjs"
  }
}
```

`scripts/verify-all.mjs` 순서는 계약 단위 테스트, drift 검사, 기존 Java·TypeScript·web·Compose·기록 정책 검증으로 구성한다. CI workflow는 계속 `node scripts/verify-all.mjs` 하나만 호출한다.

- [ ] **Step 5: drift red-green 확인**

Run: `pnpm contracts:check`

Expected: PASS.

생성 파일 하나에 공백을 추가하고 다시 실행한다.

Expected: FAIL, 수정한 상대 경로가 출력된다.

생성 파일을 `pnpm contracts:generate`로 복원한 뒤 다시 실행한다.

Expected: PASS.

- [ ] **Step 6: 전체 검증**

Run: `node scripts/verify-all.mjs`

Expected: Java test·생성 코드 compile·TypeScript test·web test·build·Compose·계약 drift·기록 정책 검사가 모두 PASS.

- [ ] **Step 7: 문서와 커밋**

README의 개발 명령에 `pnpm contracts:generate`, `pnpm contracts:check`와 생성 파일 직접 수정 금지를 추가한다.

```bash
git add scripts/check-contract-drift.mjs scripts/check-contract-drift.test.mjs scripts/verify-all.mjs package.json .github/workflows/ci.yml README.md
git commit -m "ci(contracts): 계약 drift 검사를 전체 검증에 연결"
```

---

## 최종 검증

- [ ] `git diff --check`
- [ ] 저장소 파일과 커밋 기록 금지 문자열 검사
- [ ] `pnpm test:contracts`
- [ ] `pnpm contracts:check`
- [ ] `gradlew.bat test --no-daemon --max-workers=1`
- [ ] `pnpm test:packages`
- [ ] `pnpm test:web`
- [ ] `pnpm build:packages`
- [ ] `pnpm build:web`
- [ ] `docker compose --env-file .env.example -f infra/docker/compose.yml config --quiet`
- [ ] `node scripts/verify-all.mjs`
- [ ] issue #18 수용 기준과 생성 결과 파일 목록 대조
