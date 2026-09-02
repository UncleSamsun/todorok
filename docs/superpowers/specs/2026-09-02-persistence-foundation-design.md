# JPA·Flyway·PostgreSQL 기반 설계

## 목적

이슈 #19는 planner·activity·notification 서비스가 각자 소유한 PostgreSQL schema를 Spring Data JPA로 사용하고, Flyway migration과 서비스 실행을 분리하며, 실제 PostgreSQL 통합 테스트를 공통 기준으로 제공한다.

이 기반은 후속 #20 outbox·inbox, #2 인증, #3 Task 구현이 동일한 저장·migration 규칙을 따르게 하는 선행 작업이다.

## 범위

- 세 서비스에 Spring Data JPA·Hibernate와 PostgreSQL Flyway 지원 추가
- 서비스별 migration 위치와 schema history 분리
- 일반 서비스 실행과 migration 전용 실행 파일 분리
- `ddl-auto=validate`, Open Session in View 비활성화
- PostgreSQL 17.11 Testcontainers 2.0.5 통합 테스트 기반
- 빈 DB migration, 재실행, schema 소유권, JPA repository, validate 실패 검사
- Docker Compose에서 migration 성공 후 서비스 기동

## 비범위

- Task·Activity·사용자·알림 등 실제 도메인 entity와 repository
- outbox·inbox table과 Debezium connector
- 운영 데이터 backfill과 파괴적 migration
- H2 또는 embedded database
- 여러 서비스가 하나의 JPA persistence unit이나 repository를 공유하는 구조

## 선택한 방식

각 서비스는 독립된 persistence 설정과 migration을 소유한다. 공통 library에 entity나 repository를 두지 않고, Spring Boot가 관리하는 같은 의존성 조합과 같은 테스트 규칙만 반복한다. 세 서비스뿐인 현재 범위에서는 별도의 build convention plugin보다 서비스 경계를 명확히 유지하는 편이 단순하다.

서비스 Docker image에는 일반 실행용 `app.jar`와 migration 전용 `migration.jar`를 함께 넣는다. Compose의 one-shot migration 서비스가 `migration.jar`를 실행하고 성공한 경우에만 일반 서비스를 시작한다. migration 실행 파일은 웹 서버·JPA·Kafka를 올리지 않고 DataSource와 Flyway만 자동 구성한다.

## 의존성

Spring Boot 4.1.1 dependency management가 다음 버전을 고정한다.

- Flyway 12.4.0
- Testcontainers 2.0.5
- PostgreSQL 17.11 image

각 서비스의 runtime 의존성은 다음으로 통일한다.

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-flyway`
- `org.flywaydb:flyway-database-postgresql`
- `org.postgresql:postgresql`

통합 테스트에는 다음을 추가한다.

- `org.springframework.boot:spring-boot-testcontainers`
- `org.testcontainers:testcontainers-postgresql`
- `org.testcontainers:testcontainers-junit-jupiter`

## 서비스별 파일 구조

각 서비스는 같은 구조를 사용한다. `{service}`는 `planner`, `activity`, `notification` 중 하나다.

```text
services/{service}-service/
  src/main/java/io/todorok/{service}/
    {Service}Application.java
    {Service}MigrationApplication.java
  src/main/resources/
    application.yml
    application-migration.yml
    db/migration/
      V1__initialize_{service}_schema.sql
  src/test/java/io/todorok/{service}/persistence/
    {Service}PersistenceIntegrationTest.java
    PersistenceSample.java
    PersistenceSampleRepository.java
    HibernateValidationFailureTest.java
  src/test/resources/
    db/test-migration/V9000__create_persistence_sample.sql
```

`V1`은 해당 schema 안에 `service_metadata` table을 만든다. 이 table은 migration 기반이 준비된 schema와 서비스 버전을 확인하는 최소 운영 table이며 실제 도메인 모델 역할을 하지 않는다.

```sql
create table service_metadata (
    service_name varchar(40) primary key,
    schema_version integer not null,
    installed_at timestamptz not null default now()
);

insert into service_metadata(service_name, schema_version)
values ('planner-service', 1);
```

서비스마다 문자열만 해당 이름으로 바꾼다. 적용된 migration 파일은 수정하지 않고 이후 변경은 새 version으로 추가한다.

## 설정

일반 `application.yml`은 환경 변수로 DataSource를 구성한다.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/todorok?currentSchema=planner}
    username: ${SPRING_DATASOURCE_USERNAME:planner_app}
    password: ${SPRING_DATASOURCE_PASSWORD:planner_local}
  flyway:
    enabled: false
    default-schema: planner
    schemas: planner
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: planner
```

activity와 notification은 schema·계정 기본값만 자기 이름으로 바꾼다. 비밀번호 기본값은 `.env.example`과 일치하는 로컬 개발값만 사용하며 운영 비밀번호는 환경 변수로 주입한다.

`application-migration.yml`은 Flyway만 활성화하고 location과 schema를 고정한다.

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: planner
    schemas: planner
    create-schemas: false
```

`create-schemas=false`로 두어 서비스 계정이 다른 schema를 만들지 못하게 한다. schema와 role은 PostgreSQL 초기화 또는 운영 provisioning이 먼저 만든다.

## migration 전용 실행 파일

각 `{Service}MigrationApplication`은 `@SpringBootConfiguration`과 제한된 `@EnableAutoConfiguration`을 사용한다. `WebApplicationType.NONE`으로 실행하고 JPA·Kafka 자동 구성을 제외한다. context 초기화 중 Flyway가 완료되면 context를 닫고 exit code 0으로 끝난다. migration 또는 연결 실패는 예외를 그대로 전파해 non-zero로 종료한다.

Gradle은 기본 `bootJar`의 main class를 기존 `{Service}Application`으로 고정하고, classifier가 `migration`인 별도 `BootJar` task를 등록한다. 서비스 Dockerfile은 두 jar를 모두 복사한다.

```text
/app/app.jar
/app/migration.jar
```

별도 image를 만들지 않으므로 애플리케이션과 migration의 코드·driver·SQL version이 항상 같다.

## Docker Compose 실행 순서

세 migration 서비스는 PostgreSQL health check 이후 병렬 실행할 수 있다. 각 일반 서비스는 자기 migration만 `service_completed_successfully` 조건으로 기다린다.

```text
postgres healthy
  ├─ planner-migration complete ────── planner-service start
  ├─ activity-migration complete ───── activity-service start
  └─ notification-migration complete ─ notification-service start
```

migration 서비스는 일반 서비스와 같은 image를 사용하고 entrypoint만 `java -jar /app/migration.jar --spring.profiles.active=migration`으로 바꾼다. `restart: no`이며 PostgreSQL 연결 환경 변수만 받는다. 일반 서비스에는 `SPRING_FLYWAY_ENABLED=false`를 명시한다.

하나라도 실패하면 연결된 일반 서비스는 시작하지 않는다. 이미 실행 중인 이전 배포를 migration 실패만으로 종료하는 배포 동작은 #14에서 구현한다.

## 통합 테스트

테스트는 PostgreSQL 17.11 container를 실제로 시작하며 `@ServiceConnection`으로 연결한다. 서비스별 테스트는 독립 container를 사용해 데이터와 schema history가 섞이지 않게 한다.

### Migration 검사

1. 관리자 연결로 자기 schema와 service role을 만든다.
2. service role로 Flyway를 실행한다.
3. `service_metadata`와 `flyway_schema_history`가 자기 schema에만 생성됐는지 확인한다.
4. 같은 migration을 다시 실행해 pending migration 0개와 기존 row 불변을 확인한다.
5. 다른 두 서비스 schema에 table을 만들거나 migration history를 기록하지 않았는지 확인한다.

### Repository 검사

운영 migration을 오염시키지 않도록 `src/test/resources/db/test-migration`에만 `persistence_sample` table을 만든다. test source의 entity와 repository로 저장·조회·optimistic `@Version` 증가·테스트 transaction rollback을 확인한다.

### Hibernate validate 실패 검사

test source의 entity가 존재하지 않는 column을 요구하도록 별도 application context를 구성한다. Flyway 완료 후 `ddl-auto=validate`가 context 시작을 거부하고 schema validation 오류를 남기는지 확인한다. 이 검사는 운영 설정이 실수로 `none`, `update`, `create`로 바뀌는 회귀를 막는다.

## 테스트 격리와 실행 조건

- H2와 Testcontainers JDBC shortcut URL을 사용하지 않는다.
- Docker를 사용할 수 없는 환경에서는 통합 테스트를 성공으로 건너뛰지 않고 명시적으로 실패한다.
- container reuse를 사용하지 않는다.
- 테스트는 schema 이름·table 수·migration version을 literal 값으로 검증한다.
- 각 서비스 persistence 통합 테스트는 별도 Gradle test class로 실행할 수 있다.
- 전체 `gradlew test`와 `scripts/verify-all.mjs`에 통합한다.

## 실패 처리

- migration checksum 불일치: 실패하고 서비스 시작 차단
- schema 또는 role 없음: `create-schemas=false`에 의해 실패
- 다른 schema 접근: PostgreSQL 권한 오류로 실패
- Hibernate mapping 불일치: runtime context 시작 실패
- PostgreSQL container 기동 실패: 통합 테스트 실패
- 두 migration의 같은 version 사용: 서비스별 schema history가 분리되어 충돌하지 않음

오류를 자동 repair하거나 baseline 처리하지 않는다. 이미 적용한 migration을 수정하지 않고 새 migration으로만 복구한다.

## 수용 기준 대응

- 세 서비스가 자기 schema migration만 실행: schema·계정·location 고정과 격리 테스트로 검증
- 운영 mode에서 서비스와 migration 분리: `app.jar`·`migration.jar` 및 Compose one-shot 서비스로 검증
- 실제 PostgreSQL repository·migration 테스트: PostgreSQL 17.11 Testcontainers와 test 전용 entity·repository로 검증
- 빈 DB와 기존 schema: 최초 migrate와 두 번째 migrate 결과를 함께 검증
- Hibernate validate 실패: 존재하지 않는 column을 요구하는 test context로 검증
- 데이터 격리: 서비스별 container와 transaction rollback로 검증
