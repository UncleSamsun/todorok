# JPA·Flyway·PostgreSQL 기반 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** planner·activity·notification 서비스에 schema별 JPA·Flyway 기반, migration 전용 jar, PostgreSQL 통합 테스트와 Compose 기동 순서를 구현한다.

**Architecture:** 각 서비스가 자기 DataSource 설정과 Flyway migration을 소유하며 일반 실행에서는 Flyway를 끄고 Hibernate validate만 수행한다. 같은 image에 일반 `app.jar`와 one-shot `migration.jar`를 넣고, PostgreSQL Testcontainers가 migration·repository·schema 격리를 실제 DB에서 검증한다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Data JPA, Hibernate, Flyway 12.4.0, PostgreSQL 17.11, Testcontainers 2.0.5, Gradle 9.7.1 Kotlin DSL, Docker Compose v2.

**Spec:** `docs/superpowers/specs/2026-09-02-persistence-foundation-design.md`

## Global Constraints

- planner·activity·notification은 각각 `planner`, `activity`, `notification` schema만 사용한다.
- 일반 서비스 실행은 `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`다.
- migration 실행은 웹·JPA·Kafka를 기동하지 않고 Flyway 성공 시 0, 실패 시 non-zero로 종료한다.
- schema와 role은 PostgreSQL provisioning이 만들며 운영 migration은 `create-schemas=false`다.
- migration 파일은 적용 후 수정하지 않고 새 version을 추가한다.
- repository 통합 테스트는 PostgreSQL 17.11 container를 사용하며 H2와 container reuse를 금지한다.
- 실제 domain entity, outbox·inbox, Debezium은 이 계획에 포함하지 않는다.
- 생성 계약 파일은 직접 수정하지 않는다.
- 저장소와 Git 기록에 금지된 작성 주체·도구 이름을 남기지 않는다.

---

### Task 1: Planner persistence 수직 경로

**Files:**
- Modify: `services/planner-service/build.gradle.kts`
- Modify: `services/planner-service/src/main/resources/application.yml`
- Create: `services/planner-service/src/main/resources/db/migration/V1__initialize_planner_schema.sql`
- Delete: `services/planner-service/src/test/java/io/todorok/planner/PlannerApplicationTest.java`
- Create: `services/planner-service/src/test/java/io/todorok/planner/persistence/PersistenceSample.java`
- Create: `services/planner-service/src/test/java/io/todorok/planner/persistence/PersistenceSampleRepository.java`
- Create: `services/planner-service/src/test/java/io/todorok/planner/persistence/PlannerPersistenceIntegrationTest.java`
- Create: `services/planner-service/src/test/resources/db/test-migration/V9000__create_persistence_sample.sql`

**Interfaces:**
- Consumes: PostgreSQL 17.11 image, `PlannerApplication`, `planner` schema.
- Produces: planner Flyway history, `service_metadata`, JPA repository 통합 테스트 기반.

- [ ] **Step 1: planner PostgreSQL 실패 테스트 작성**

```java
package io.todorok.planner.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class PlannerPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;

    @Test
    void migratesOnlyPlannerSchema() {
        assertThat(jdbc.queryForObject(
                "select service_name from planner.service_metadata",
                String.class)).isEqualTo("planner-service");
        assertThat(jdbc.queryForObject(
                "select count(*) from planner.flyway_schema_history where success",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList(
                "select schema_name from information_schema.schemata "
                        + "where schema_name in ('planner','activity','notification') "
                        + "order by schema_name",
                String.class)).isEqualTo(List.of("planner"));
    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from planner.flyway_schema_history where success",
                Integer.class)).isEqualTo(2);
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("planner-value"));
        assertThat(saved.version()).isZero();
        saved.rename("planner-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
        assertThat(repository.findById(saved.id())).get()
                .extracting(PersistenceSample::name)
                .isEqualTo("planner-renamed");
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:planner-service:test --tests '*PlannerPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, Testcontainers·JPA dependency와 `PersistenceSampleRepository`가 없다.

- [ ] **Step 3: planner 의존성과 설정 추가**

`build.gradle.kts`에 다음을 추가한다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-flyway")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:testcontainers-postgresql")
testImplementation("org.testcontainers:testcontainers-junit-jupiter")
```

`application.yml`에 다음을 병합한다.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/todorok?currentSchema=planner}
    username: ${SPRING_DATASOURCE_USERNAME:planner_app}
    password: ${SPRING_DATASOURCE_PASSWORD:replace-with-planner-password}
  flyway:
    enabled: false
    default-schema: planner
    schemas: planner
    create-schemas: false
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: planner
```

- [ ] **Step 4: planner 운영·테스트 migration 작성**

`V1__initialize_planner_schema.sql`:

```sql
create table service_metadata (
    service_name varchar(40) primary key,
    schema_version integer not null,
    installed_at timestamptz not null default now()
);

insert into service_metadata(service_name, schema_version)
values ('planner-service', 1);
```

`V9000__create_persistence_sample.sql`:

```sql
create table persistence_sample (
    id uuid primary key,
    name varchar(100) not null,
    version bigint not null
);
```

- [ ] **Step 5: test entity와 repository 작성**

`PersistenceSample`은 `@Entity(name = "PlannerPersistenceSample")`, `@Table(name = "persistence_sample", schema = "planner")`, UUID id, 길이 100의 name, `@Version long version`을 가진다. 생성자는 `UUID.randomUUID()`와 입력 name을 저장하고, `rename(String)`은 name을 변경한다. 접근자는 `id()`, `name()`, `version()`으로 고정한다.

```java
interface PersistenceSampleRepository extends JpaRepository<PersistenceSample, UUID> {}
```

- [ ] **Step 6: planner 통합 테스트 통과**

Run: `gradlew.bat :services:planner-service:test --tests '*PlannerPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, migration 2개와 repository optimistic version이 검증된다.

- [ ] **Step 7: 커밋**

```bash
git add services/planner-service
git commit -m "feat(persistence): planner PostgreSQL 기반 추가"
```

---

### Task 2: Activity persistence 수직 경로

**Files:**
- Modify: `services/activity-service/build.gradle.kts`
- Modify: `services/activity-service/src/main/resources/application.yml`
- Create: `services/activity-service/src/main/resources/db/migration/V1__initialize_activity_schema.sql`
- Delete: `services/activity-service/src/test/java/io/todorok/activity/ActivityApplicationTest.java`
- Create: `services/activity-service/src/test/java/io/todorok/activity/persistence/PersistenceSample.java`
- Create: `services/activity-service/src/test/java/io/todorok/activity/persistence/PersistenceSampleRepository.java`
- Create: `services/activity-service/src/test/java/io/todorok/activity/persistence/ActivityPersistenceIntegrationTest.java`
- Create: `services/activity-service/src/test/resources/db/test-migration/V9000__create_persistence_sample.sql`

**Interfaces:**
- Consumes: PostgreSQL 17.11 image, `ActivityApplication`, `activity` schema.
- Produces: activity Flyway history, `service_metadata`, JPA repository 통합 테스트 기반.

- [ ] **Step 1: activity PostgreSQL 실패 테스트 작성**

`ActivityPersistenceIntegrationTest`를 다음 public boundary로 작성한다. import는 planner test와 동일한 Spring·JUnit·Testcontainers type을 activity package에서 사용한다.

```java
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class ActivityPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;

    @Test
    void migratesOnlyActivitySchema() {
        assertThat(jdbc.queryForObject(
                "select service_name from activity.service_metadata",
                String.class)).isEqualTo("activity-service");
        assertThat(jdbc.queryForObject(
                "select count(*) from activity.flyway_schema_history where success",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList(
                "select schema_name from information_schema.schemata "
                        + "where schema_name in ('planner','activity','notification') "
                        + "order by schema_name",
                String.class)).isEqualTo(List.of("activity"));
    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from activity.flyway_schema_history where success",
                Integer.class)).isEqualTo(2);
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("activity-value"));
        assertThat(saved.version()).isZero();
        saved.rename("activity-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:activity-service:test --tests '*ActivityPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, activity JPA·Flyway와 test repository가 없다.

- [ ] **Step 3: activity 의존성과 설정 작성**

activity build에 다음 dependency를 추가한다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-flyway")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:testcontainers-postgresql")
testImplementation("org.testcontainers:testcontainers-junit-jupiter")
```

DataSource 기본 URL은 `jdbc:postgresql://localhost:5432/todorok?currentSchema=activity`, username은 `activity_app`, password는 `replace-with-activity-password`를 사용한다. Flyway의 `default-schema`·`schemas`와 Hibernate `default_schema`는 모두 `activity`로 고정한다. Flyway는 false, OSIV는 false, ddl-auto는 validate다.

- [ ] **Step 4: activity migration과 test repository 작성**

운영 V1은 `service_metadata(service_name varchar(40) primary key, schema_version integer not null, installed_at timestamptz not null default now())`를 만들고 `('activity-service', 1)`을 넣는다. test V9000은 `persistence_sample(id uuid primary key, name varchar(100) not null, version bigint not null)`을 만든다. test entity는 `@Entity(name = "ActivityPersistenceSample")`, `@Table(name = "persistence_sample", schema = "activity")`, UUID id, name, `@Version long version`, `rename`, `id`, `name`, `version`을 제공한다. repository signature는 `interface PersistenceSampleRepository extends JpaRepository<PersistenceSample, UUID> {}`다.

- [ ] **Step 5: activity 통합 테스트 통과**

Run: `gradlew.bat :services:activity-service:test --tests '*ActivityPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, activity schema만 존재하고 optimistic version이 증가한다.

- [ ] **Step 6: 커밋**

```bash
git add services/activity-service
git commit -m "feat(persistence): activity PostgreSQL 기반 추가"
```

---

### Task 3: Notification persistence 수직 경로

**Files:**
- Modify: `services/notification-service/build.gradle.kts`
- Modify: `services/notification-service/src/main/resources/application.yml`
- Create: `services/notification-service/src/main/resources/db/migration/V1__initialize_notification_schema.sql`
- Delete: `services/notification-service/src/test/java/io/todorok/notification/NotificationApplicationTest.java`
- Create: `services/notification-service/src/test/java/io/todorok/notification/persistence/PersistenceSample.java`
- Create: `services/notification-service/src/test/java/io/todorok/notification/persistence/PersistenceSampleRepository.java`
- Create: `services/notification-service/src/test/java/io/todorok/notification/persistence/NotificationPersistenceIntegrationTest.java`
- Create: `services/notification-service/src/test/resources/db/test-migration/V9000__create_persistence_sample.sql`

**Interfaces:**
- Consumes: PostgreSQL 17.11 image, `NotificationApplication`, `notification` schema.
- Produces: notification Flyway history, `service_metadata`, JPA repository 통합 테스트 기반.

- [ ] **Step 1: notification PostgreSQL 실패 테스트 작성**

`NotificationPersistenceIntegrationTest`는 PostgreSQLContainer와 ServiceConnection을 선언한다. `migratesOnlyNotificationSchema`는 다음 결과를 검증한다.

```java
@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class NotificationPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;

    @Test
    void migratesOnlyNotificationSchema() {
assertThat(jdbc.queryForObject(
        "select service_name from notification.service_metadata",
        String.class)).isEqualTo("notification-service");
assertThat(jdbc.queryForObject(
        "select count(*) from notification.flyway_schema_history where success",
        Integer.class)).isEqualTo(2);
assertThat(jdbc.queryForList(
        "select schema_name from information_schema.schemata "
                + "where schema_name in ('planner','activity','notification') "
                + "order by schema_name",
        String.class)).isEqualTo(List.of("notification"));

    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from notification.flyway_schema_history where success",
                Integer.class)).isEqualTo(2);
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("notification-value"));
        assertThat(saved.version()).isZero();
        saved.rename("notification-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:notification-service:test --tests '*NotificationPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, notification JPA·Flyway와 test repository가 없다.

- [ ] **Step 3: notification 의존성과 설정 작성**

notification build에 다음 dependency를 추가한다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-flyway")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:testcontainers-postgresql")
testImplementation("org.testcontainers:testcontainers-junit-jupiter")
```

DataSource 기본 URL은 `jdbc:postgresql://localhost:5432/todorok?currentSchema=notification`, username은 `notification_app`, password는 `replace-with-notification-password`를 사용한다. Flyway의 `default-schema`·`schemas`와 Hibernate `default_schema`는 모두 `notification`으로 고정한다. Flyway는 false, OSIV는 false, ddl-auto는 validate다.

- [ ] **Step 4: notification migration과 test repository 작성**

운영 V1은 `service_metadata(service_name varchar(40) primary key, schema_version integer not null, installed_at timestamptz not null default now())`를 만들고 `('notification-service', 1)`을 넣는다. test V9000은 `persistence_sample(id uuid primary key, name varchar(100) not null, version bigint not null)`을 만든다. test entity는 `@Entity(name = "NotificationPersistenceSample")`, `@Table(name = "persistence_sample", schema = "notification")`, UUID id, name, `@Version long version`, `rename`, `id`, `name`, `version`을 제공한다. repository signature는 `interface PersistenceSampleRepository extends JpaRepository<PersistenceSample, UUID> {}`다.

- [ ] **Step 5: notification 통합 테스트 통과**

Run: `gradlew.bat :services:notification-service:test --tests '*NotificationPersistenceIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, notification schema만 존재하고 optimistic version이 증가한다.

- [ ] **Step 6: 커밋**

```bash
git add services/notification-service
git commit -m "feat(persistence): notification PostgreSQL 기반 추가"
```

---

### Task 4: Migration 전용 실행 파일과 jar

**Files:**
- Create: `services/planner-service/src/main/java/io/todorok/migration/planner/PlannerMigrationApplication.java`
- Create: `services/activity-service/src/main/java/io/todorok/migration/activity/ActivityMigrationApplication.java`
- Create: `services/notification-service/src/main/java/io/todorok/migration/notification/NotificationMigrationApplication.java`
- Create: `services/planner-service/src/main/resources/application-migration.yml`
- Create: `services/activity-service/src/main/resources/application-migration.yml`
- Create: `services/notification-service/src/main/resources/application-migration.yml`
- Create: `services/planner-service/src/test/java/io/todorok/migration/planner/PlannerMigrationApplicationTest.java`
- Create: `services/activity-service/src/test/java/io/todorok/migration/activity/ActivityMigrationApplicationTest.java`
- Create: `services/notification-service/src/test/java/io/todorok/migration/notification/NotificationMigrationApplicationTest.java`
- Modify: `services/planner-service/build.gradle.kts`
- Modify: `services/activity-service/build.gradle.kts`
- Modify: `services/notification-service/build.gradle.kts`

**Interfaces:**
- Consumes: 서비스 DataSource 환경 변수와 `application-migration.yml`.
- Produces: `migrationBootJar`, `{service}-service-migration.jar`, non-web one-shot migration main.

- [ ] **Step 1: planner migration-only 실패 테스트 작성**

```java
@Testcontainers
class PlannerMigrationApplicationTest {
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Test
    void runsFlywayWithoutJpaOrKafka() {
        createSchema(POSTGRES, "planner");
        try (var context = PlannerMigrationApplication.run(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword())) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
            assertThat(context.getBeansOfType(KafkaAdmin.class)).isEmpty();
            assertThat(context.getEnvironment().getProperty("spring.main.web-application-type"))
                    .isEqualTo("none");
        }
    }

    private static void createSchema(PostgreSQLContainer postgres, String schema) {
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:planner-service:test --tests '*PlannerMigrationApplicationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, `PlannerMigrationApplication`이 없다.

- [ ] **Step 3: 세 migration application 작성**

세 class는 서비스 base package 밖의 `io.todorok.migration` 아래에 둔다. planner class는 다음과 같다.

```java
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        HibernateJpaAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class PlannerMigrationApplication {
    public static ConfigurableApplicationContext run(String... args) {
        return new SpringApplicationBuilder(PlannerMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static void main(String[] args) {
        try (var ignored = new SpringApplicationBuilder(PlannerMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args)) {
        }
    }
}
```

activity class:

```java
package io.todorok.migration.activity;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        HibernateJpaAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class ActivityMigrationApplication {
    public static ConfigurableApplicationContext run(String... args) {
        return new SpringApplicationBuilder(ActivityMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static void main(String[] args) {
        try (var ignored = run(args)) {
        }
    }
}
```

notification class:

```java
package io.todorok.migration.notification;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        HibernateJpaAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class NotificationMigrationApplication {
    public static ConfigurableApplicationContext run(String... args) {
        return new SpringApplicationBuilder(NotificationMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static void main(String[] args) {
        try (var ignored = run(args)) {
        }
    }
}
```

- [ ] **Step 4: migration profile 작성**

planner profile:

```yaml
spring:
  main:
    web-application-type: none
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: planner
    schemas: planner
    create-schemas: false
```

activity와 notification은 `default-schema`와 `schemas`를 각각 자기 schema로 지정한다.

- [ ] **Step 5: activity·notification migration test 작성**

`ActivityMigrationApplicationTest`는 JDBC로 `activity` schema를 만든 뒤 `ActivityMigrationApplication.run`에 container의 URL·username·password command-line argument를 전달한다. `NotificationMigrationApplicationTest`는 `notification` schema와 `NotificationMigrationApplication.run`을 사용한다. 두 test 모두 Flyway bean 1개, EntityManagerFactory 0개, KafkaAdmin 0개, web application type `none`을 검증한다.

- [ ] **Step 6: migration jar task 작성**

세 build file에 다음 import와 task를 서비스별 main class로 추가한다.

```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootJar

springBoot {
    mainClass.set("io.todorok.planner.PlannerApplication")
}

tasks.register<BootJar>("migrationBootJar") {
    group = "build"
    archiveClassifier.set("migration")
    mainClass.set("io.todorok.migration.planner.PlannerMigrationApplication")
    classpath(sourceSets.main.get().runtimeClasspath)
}

tasks.named("assemble") {
    dependsOn("migrationBootJar")
}
```

- [ ] **Step 7: migration test와 jar 검증**

Run: `gradlew.bat :services:planner-service:test :services:activity-service:test :services:notification-service:test :services:planner-service:migrationBootJar :services:activity-service:migrationBootJar :services:notification-service:migrationBootJar --no-daemon --no-configuration-cache`

Expected: PASS, 세 `build/libs/*-migration.jar`가 생성된다.

- [ ] **Step 8: 커밋**

```bash
git add services
git commit -m "feat(persistence): 서비스별 migration 실행 파일 추가"
```

---

### Task 5: Hibernate validate 실패 회귀

**Files:**
- Create: `services/planner-service/src/test/java/io/todorok/planner/persistence/HibernateValidationFailureTest.java`

**Interfaces:**
- Consumes: planner V1·V9000 migration과 `PlannerApplication`.
- Produces: `ddl-auto=validate`가 mapping 불일치를 거부한다는 회귀 검사.

- [ ] **Step 1: validate 실패 테스트 작성**

```java
@Testcontainers
class HibernateValidationFailureTest {
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Test
    void rejectsMissingSchema() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(PlannerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of(
                        "spring.datasource.url", POSTGRES.getJdbcUrl(),
                        "spring.datasource.username", POSTGRES.getUsername(),
                        "spring.datasource.password", POSTGRES.getPassword(),
                        "spring.flyway.enabled", "true",
                        "spring.flyway.create-schemas", "true",
                        "spring.flyway.locations", "classpath:db/migration,classpath:db/test-migration",
                        "spring.jpa.properties.hibernate.default_schema", "missing_schema",
                        "spring.jpa.hibernate.ddl-auto", "validate"))
                .run())
                .hasRootCauseInstanceOf(SchemaManagementException.class)
                .hasStackTraceContaining("Schema-validation");
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:planner-service:test --tests '*HibernateValidationFailureTest' --no-daemon --no-configuration-cache`

Expected before assertion refinement: FAIL if the root exception differs; inspect the actual Hibernate 7 validation exception and keep the assertion on `SchemaManagementException` plus `Schema-validation`.

- [ ] **Step 3: test context 격리 보정**

Kafka 연결 시도가 validation 오류보다 먼저 발생하면 test property에 다음을 추가해 listener 기동을 막는다.

```java
"spring.kafka.listener.auto-startup", "false"
```

context가 생성되면 실패해야 하므로 test를 변경하지 않고 `application.yml`의 `ddl-auto: validate`와 planner test entity scan을 확인한다.

- [ ] **Step 4: validate 회귀 통과**

Run: `gradlew.bat :services:planner-service:test --tests '*HibernateValidationFailureTest' --no-daemon --no-configuration-cache`

Expected: PASS, 존재하지 않는 Hibernate default schema가 context 시작을 거부한다.

- [ ] **Step 5: 커밋**

```bash
git add services/planner-service/src/test
git commit -m "test(persistence): Hibernate validate 실패 회귀 추가"
```

---

### Task 6: Docker image와 Compose migration 순서

**Files:**
- Modify: `services/planner-service/Dockerfile`
- Modify: `services/activity-service/Dockerfile`
- Modify: `services/notification-service/Dockerfile`
- Modify: `infra/docker/compose.yml`
- Create: `scripts/persistence-compose.test.mjs`
- Modify: `scripts/verify-all.mjs`
- Modify: `README.md`

**Interfaces:**
- Consumes: 세 서비스 `app.jar`, `migration.jar`, PostgreSQL 환경 변수.
- Produces: migration one-shot 서비스 3개와 `service_completed_successfully` 기동 gate.

- [ ] **Step 1: Compose 동작 실패 테스트 작성**

```js
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('각 서비스는 자기 migration 성공 뒤 시작한다', () => {
  const result = spawnSync('docker', [
    'compose', '--env-file', '.env.example',
    '-f', 'infra/docker/compose.yml', 'config', '--format', 'json',
  ], { encoding: 'utf8', shell: process.platform === 'win32' })
  assert.equal(result.status, 0, result.stderr)
  const services = JSON.parse(result.stdout).services
  for (const name of ['planner', 'activity', 'notification']) {
    const migration = services[`${name}-migration`]
    const application = services[`${name}-service`]
    assert.deepEqual(migration.entrypoint, [
      'java', '-jar', '/app/migration.jar', '--spring.profiles.active=migration',
    ])
    assert.equal(
      application.depends_on[`${name}-migration`].condition,
      'service_completed_successfully',
    )
    assert.equal(application.environment.SPRING_FLYWAY_ENABLED, 'false')
  }
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/persistence-compose.test.mjs`

Expected: FAIL, `planner-migration` service가 없다.

- [ ] **Step 3: Dockerfile 두 jar 복사**

각 builder는 `bootJar migrationBootJar`를 실행한다. runtime stage는 이름이 `-migration.jar`로 끝나는 파일을 `/app/migration.jar`로 복사하고, classifier 없는 jar를 `/app/app.jar`로 복사한다. wildcard가 두 파일을 같은 대상으로 복사하지 않도록 builder stage에서 다음처럼 명시적으로 이름을 정규화한다.

```dockerfile
RUN cp services/planner-service/build/libs/*-migration.jar /tmp/migration.jar \
    && find services/planner-service/build/libs -maxdepth 1 -name '*.jar' \
       ! -name '*-migration.jar' -exec cp {} /tmp/app.jar \;
```

runtime stage는 `/tmp/app.jar`와 `/tmp/migration.jar`를 각각 복사한다.

- [ ] **Step 4: Compose migration 서비스 추가**

서비스 image 이름을 `todorok/planner-service:local`, `todorok/activity-service:local`, `todorok/notification-service:local`로 고정한다. migration 서비스는 같은 image, `restart: "no"`, PostgreSQL health dependency, 위 test의 entrypoint와 자기 DataSource URL·계정·비밀번호를 사용한다.

일반 서비스에도 같은 DataSource 환경 변수와 `SPRING_FLYWAY_ENABLED: "false"`를 넣고 자기 migration dependency를 추가한다.

- [ ] **Step 5: Compose test 통과**

Run: `node --test scripts/persistence-compose.test.mjs`

Run: `docker compose --env-file .env.example -f infra/docker/compose.yml config --quiet`

Expected: 모두 PASS.

- [ ] **Step 6: 전체 검증 연결과 문서화**

`scripts/verify-all.mjs`의 계약 검사 뒤에 `node --test scripts/persistence-compose.test.mjs`를 추가한다. README에 다음 명령과 순서를 기록한다.

```text
docker compose --env-file .env -f infra/docker/compose.yml up --build
postgres → migration 3개 → application 3개 → nginx
```

- [ ] **Step 7: 커밋**

```bash
git add services/*/Dockerfile infra/docker/compose.yml scripts/persistence-compose.test.mjs scripts/verify-all.mjs README.md
git commit -m "feat(persistence): Compose migration 기동 순서 연결"
```

---

### Task 7: 전체 수용 기준 검증

**Files:**
- Modify: `docs/plans/2026-09-01-mvp-implementation-plan.md`
- Modify: `docs/ISSUE_ROADMAP.md`

**Interfaces:**
- Consumes: Task 1~6의 service persistence와 Compose gate.
- Produces: #19 수용 기준 완료 근거와 전체 CI 결과.

- [ ] **Step 1: 구현 계획 상태 갱신**

MVP 계획의 T3를 완료 체크하고 #19 로드맵 행에 구현 결과 문서 링크를 추가한다. PRD 요구사항은 변경하지 않는다.

- [ ] **Step 2: 빠른 persistence 검증**

Run: `gradlew.bat :services:planner-service:test :services:activity-service:test :services:notification-service:test --no-daemon --no-configuration-cache --max-workers=1`

Expected: 세 PostgreSQL migration·repository·migration-only·validate test PASS.

- [ ] **Step 3: jar와 Compose 검증**

Run: `gradlew.bat :services:planner-service:bootJar :services:planner-service:migrationBootJar :services:activity-service:bootJar :services:activity-service:migrationBootJar :services:notification-service:bootJar :services:notification-service:migrationBootJar --no-daemon --no-configuration-cache --max-workers=1`

Run: `node --test scripts/persistence-compose.test.mjs`

Expected: jar 6개 생성, Compose migration gate PASS.

- [ ] **Step 4: 전체 검증**

Run: `node scripts/verify-all.mjs`

Expected: 계약 drift, Java·PostgreSQL, TypeScript, React, Compose, 기록 정책 검사가 모두 PASS.

- [ ] **Step 5: 변경과 정책 검증**

Run: `git diff --check`

Run: 저장소와 `develop..HEAD` 커밋 기록에 대한 금지 문자열 검사.

Expected: whitespace 오류와 금지 문자열 0개.

- [ ] **Step 6: 문서 커밋**

```bash
git add docs/plans/2026-09-01-mvp-implementation-plan.md docs/ISSUE_ROADMAP.md
git commit -m "docs(persistence): PostgreSQL 기반 완료 상태 반영"
```

- [ ] **Step 7: 이슈 수용 기준 대조**

`#19`의 세 수용 기준과 네 테스트 항목을 다음 근거에 대조한다.

```text
schema migration 소유권 → 세 PersistenceIntegrationTest
서비스와 migration 분리 → 세 MigrationApplicationTest + migrationBootJar
실제 PostgreSQL repository → 세 PersistenceSampleRepository test
빈 DB·기존 schema → 최초 migrate + Flyway 재실행 검증
Hibernate validate 실패 → HibernateValidationFailureTest
데이터 격리 → 서비스별 container + transaction rollback
```
