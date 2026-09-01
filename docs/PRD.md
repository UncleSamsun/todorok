---
title: TODOROK MVP PRD
product: TODOROK
status: approved
version: 1.4.0
created: 2026-08-31
updated: 2026-09-01
owner: 김민준
---

# TODOROK MVP PRD

## 1. 문서 목적

이 문서는 토도록(TODOROK)의 MVP 범위, 사용자 경험, 핵심 규칙, 시스템 경계, 비기능 요구사항과 완료 기준을 정의한다. 구현 계획과 코드 작성은 이 문서가 승인된 뒤 진행한다.

## 2. 제품 요약

토도록은 TodoMate처럼 간결한 달력 기반 할 일 관리에 운동·공부·클라이밍 기록을 연결한 개인 루틴 플래너다.

사용자는 아침에 달력과 오늘 할 일을 확인하고, 일반 할 일은 바로 체크한다. 운동·공부·클라이밍 할 일은 완료 과정에서 수행 기록을 남긴다. 일정, 실제 수행량, 그날의 메모가 하나의 날짜 안에서 연결되는 것이 핵심 가치다.

- 한글명: 토도록
- 영문명: TODOROK
- 코드명·저장소 접두사: `todorok`
- 출시 전 상표와 도메인 가용성을 별도로 확인한다.

## 3. 문제 정의

기존 TodoMate는 간결한 할 일 관리에는 적합하지만 다음 요구를 충족하지 못한다.

- 운동 완료 시 종목, 세트, 횟수와 시간을 함께 기록하기 어렵다.
- 검증 또는 출처가 있는 운동 커리큘럼을 오늘의 운동량으로 자동 편성하지 못한다.
- 공부 종류별로 서로 다른 측정 항목과 단위를 기록하기 어렵다.
- 할 일 설명, 수행 회고, 날짜별 자유 메모를 목적에 따라 나눠 남기기 어렵다.
- 클라이밍 전용 인터벌 타이머와 그립 순서를 실행 기록에 연결할 수 없다.

## 4. 목표 사용자

### 4.1 MVP 사용자

- 주 사용자: 김민준 본인
- 주 사용 기기: iPhone
- 보조 사용 기기: 노트북
- 사용 목적: 할 일, 운동, 공부, 클라이밍과 일일 메모를 한 앱에서 간결하게 운영

### 4.2 확장 사용자

개인 일정과 자기계발 기록을 하나로 관리하고 싶지만 범용 생산성 앱의 복잡한 설정은 원하지 않는 사용자다. MVP에서는 다중 사용자 성장보다 1인 사용 경험을 우선한다.

## 5. 제품 원칙

1. 달력과 체크형 할 일이 제품의 중심이다.
2. 운동·공부·클라이밍은 할 일에 연결되는 기록형 활동이다.
3. 입력을 강제하지 않는다. 사용자가 남긴 값만 저장한다.
4. 계획과 실제 기록을 구분하되 화면에서는 자연스럽게 연결한다.
5. 미수행 이력은 쌓지 않고 활성 할 일을 오늘로 이월한다.
6. 명시적으로 건너뛴 기록은 과거 날짜에 남기고 이월하지 않는다.
7. 출처 기반 운동 프로그램은 출처·조건·주의사항을 함께 제공한다.
8. MSA는 트래픽 요구가 아니라 학습 목표다. 서비스 경계는 유지하되 인프라는 작게 운영한다.

## 6. MVP 목표

- 달력에서 오늘 할 일을 10초 안에 확인하고 추가할 수 있다.
- 일반 할 일, 운동, 공부, 클라이밍 할 일을 같은 목록에서 관리할 수 있다.
- 기록형 할 일을 완료하면 도메인별 수행 기록이 저장된다.
- 미완료 할 일의 자동 이월과 지난 날짜 기록이 중복 없이 동작한다.
- 푸시업·풀업 커리큘럼이 주차와 수행 결과에 따라 다음 운동량을 편성한다.
- 클라이밍 크림프 루틴을 10초 운동·50초 휴식 자동 타이머로 실행할 수 있다.
- iPhone 홈 화면에 설치해 독립 앱처럼 사용할 수 있다.
- 노트북에서도 동일한 일정과 기록을 조회·편집할 수 있다.
- Spring MSA, Kafka, PostgreSQL을 AWS의 단일 Docker Compose 환경에서 운영한다.

## 7. MVP 비목표

- 소셜 피드, 친구, 그룹 챌린지
- 유료 결제와 구독
- AI가 임의로 생성하는 운동 처방
- Apple Health·Google Fit 연동
- 네이티브 iOS 앱
- Kubernetes, 멀티노드 Kafka, 다중 AZ 고가용성
- 서비스별 물리 DB 인스턴스
- 대규모 사용자와 트래픽 최적화
- 실시간 공동 편집
- 오프라인 조회·수정·재전송과 기기 간 충돌 병합
- 사용자별 시간대와 앱 실행 전 자정 이월

## 8. 정보 구조

### 8.1 모바일 주요 탭

1. **오늘**: 달력, 오늘 할 일, 날짜별 스티키노트
2. **운동**: 일반 운동 기록, 내장 커리큘럼, 진행 현황
3. **공부**: 카테고리, 기록 템플릿, 수행 이력
4. **클라이밍**: 크림프 루틴, 자동 타이머, 수행 이력
5. **설정**: 계정, 알림, PWA 설치 안내, 데이터 관리

### 8.2 노트북 화면

- 월간·주간 달력과 날짜별 할 일 편집을 우선한다.
- 운동·공부·클라이밍 상세 기록을 넓은 화면에서 조회할 수 있다.
- 모바일과 별도 제품이 아니라 동일 PWA의 반응형 레이아웃이다.

### 8.3 화면과 디자인 기준

- 핵심 내비게이션은 모바일 하단과 데스크톱 사이드바 모두 `오늘 · 운동 · 공부 · 클라이밍 · 설정`의 다섯 항목을 같은 순서로 유지한다.
- 앱을 새로 열면 항상 오늘 탭의 실제 오늘 날짜에서 시작하고, 마지막으로 선택한 주간·월간 보기만 기억한다.
- 오늘 화면에서 주간 스트립과 월간 달력을 즉시 전환할 수 있다.
- 데스크톱 오늘 화면은 왼쪽 달력과 오른쪽 선택 날짜의 할 일·스티키노트로 구성한 2열 레이아웃을 사용한다.
- 화면은 흰색·차콜 중심의 `Monochrome + Pastel` 방향을 따르며 라이트·다크 테마를 모두 제공한다.
- 기본 화면은 평평한 배경과 얇은 구분선을 사용하고, 카드는 타이머나 단일 기록 요약처럼 의미상 묶이는 경우에만 사용한다.
- 카테고리 색상은 대비를 검증한 파스텔 8색에서 선택하며 색만으로 상태나 분류를 표현하지 않는다.
- 타이머 숫자를 제외한 제품 글꼴은 Pretendard, 타이머 숫자는 Inter 700과 고정폭 숫자 기능을 사용한다.
- 상세 색상·타이포그래피·아이콘·반응형·상태 표현은 루트 `DESIGN.md`를 구현 기준으로 사용한다.

## 9. 핵심 도메인 모델

### 9.1 Task

- 유형: `GENERAL`, `WORKOUT`, `STUDY`, `CLIMBING`
- 상태: `PLANNED`, `COMPLETED`, `SKIPPED`
- `task_series`는 반복 제목, 유형, 메모와 반복 규칙을 보관한다.
- `task`는 달력에 표시되는 개별 회차이며 일회성 Task는 series 없이 저장한다.
- 시리즈별 활성 `PLANNED` Task는 DB partial unique index로 하나만 허용한다.
- 일반 반복은 `DAILY`, `WEEKLY`, `MONTHLY`와 간격·요일 또는 일자·시작·종료 조건만 지원한다.
- 운동 프로그램 세션 생성은 일반 반복 규칙과 별도 엔진에서 처리한다.
- `MISSED`는 영구 이력이 아니라 날짜 이월 처리 중 사용하는 전이 상태다.
- 계획 Task 삭제는 soft delete, series·category·template 삭제는 archive로 처리한다.

### 9.2 Activity Record

- 공통 header에 사용자, 원본 Task, 유형, 실제 수행 날짜, 생성 시각, 상태, 완료 기록 메모와 command ID를 저장한다.
- 상태는 `COMPLETED`, `PARTIAL`, `VOIDED`를 사용한다.
- 운동·공부·클라이밍 detail table을 공통 header와 같은 ID로 연결한다.
- 운동 세트와 클라이밍 라운드는 관계형으로, 공부 자유 값과 snapshot만 JSONB로 저장한다.
- `PARTIAL`은 Task를 완료하지 않고 `VOIDED`는 기존 완료를 되돌리는 이벤트를 발생시킨다.
- 건너뜀은 Activity를 만들지 않고 Task occurrence의 `SKIPPED` 상태로 보존한다.
- 계획 Task를 Activity Record로 덮어쓰지 않는다.

### 9.3 Daily Sticky Note

- 사용자·날짜별 한 개의 자유 텍스트 메모다.
- 할 일 또는 수행 기록에 종속되지 않는다.
- 자동 저장하고 과거 날짜에서도 조회·수정할 수 있다.

## 10. 할 일과 일정

### 10.1 기본 생성

- 달력에서 생성하는 기본 항목은 체크형 할 일이다.
- 오늘 화면의 `+`는 제목 한 줄만 먼저 받는 빠른 추가를 연다.
- 날짜·반복·분류·시간·개수는 필요할 때만 펼친다.
- 시간 블록은 선택 사항이다.
- 일반 할 일은 체크 시 즉시 완료된다.
- 기록형 할 일은 체크 시 해당 기록 화면을 연다.

### 10.2 완료·건너뜀·취소

- 기록형 할 일은 Activity Record 저장 시에만 완료된다.
- `건너뜀`은 사용자가 명시적으로 선택한다.
- 기록 화면을 닫으면 당일에는 `PLANNED` 상태를 유지한다.
- 건너뜀은 해당 날짜에 남으며 다음 날로 이월하지 않는다.
- 일반 완료와 건너뜀은 명시적 reopen command로 되돌릴 수 있다.
- 기록형 완료 취소는 Activity를 삭제하지 않고 `VOIDED`로 바꾸며 Task와 프로그램 진행을 재계산한다.
- Activity 값 수정은 revision을 증가시키고 correction 이벤트를 발행한다.

### 10.3 자동 이월

- 예정일이 지났는데 완료·건너뜀 처리하지 않은 활성 Task는 오늘 날짜로 이동한다.
- 이월 시 새 Task를 매일 복제하지 않는다.
- 이전 날짜의 미수행 기록은 삭제하며 활성 Task 하나만 유지한다.
- 이월 연산은 멱등해야 하며 여러 번 실행해도 중복 Task가 생기지 않아야 한다.
- 날짜 기준은 `Asia/Seoul`로 고정한다.
- 오늘 화면 조회 시에만 멱등 이월 command를 실행한다.
- 앱 실행 전 자정 이월과 사용자별 시간대는 지원하지 않는다.

### 10.4 지난 기록

- 운동·공부·클라이밍 탭 상단에 `지난 기록 추가`를 제공한다.
- 사용자는 과거 수행 날짜와 해당 날짜의 할 일 또는 루틴을 선택한 뒤 기존 기록 화면으로 진입한다.
- 저장 시 선택한 과거 날짜에 Activity Record를 생성하고 해당 Task를 완료한다.
- 반복 일정이면 오늘 날짜의 새 활성 Task를 즉시 생성한다.
- 사용자가 오늘 일정을 다시 등록할 필요가 없어야 한다.
- 저장 후 오늘 탭으로 이동해 방금 기록한 과거 날짜를 선택한 상태로 보여주고, 날짜 헤더의 `오늘` 버튼으로 실제 오늘에 복귀한다.

## 11. 메모

### 11.1 할 일 메모

- 수행 전 참고하는 설명, 준비물, 링크를 저장한다.
- 반복 Task에서는 다음 회차에도 유지할 수 있다.

### 11.2 완료 기록 메모

- 특정 수행 결과에 붙는 상태, 회고와 다음 행동을 저장한다.
- 해당 날짜의 Activity Record에 종속된다.

### 11.3 오늘의 스티키노트

- 날짜별 줄글 메모다.
- 선택 날짜의 할 일 목록 아래에서 한 줄 미리보기로 보이고 선택 시 같은 화면의 편집 영역이 펼쳐진다.
- 별도 저장 버튼 없이 자동 저장한다.
- 입력이 멈춘 뒤 500ms debounce로 PATCH하며 초기 MVP에서는 저장 요청 직렬화 queue를 두지 않는다.
- 저장 성공·실패 상태와 수동 재시도를 표시한다.
- 최대 길이는 20,000자다.

## 12. 공부

### 12.1 공부 카테고리

- 사용자는 `알고리즘`, `독서`, `강의` 등 카테고리를 생성한다.
- 카테고리마다 원하는 기록 항목, 입력 형식과 단위를 정의한다.
- 입력 형식 MVP 범위: 숫자, 시간, 짧은 텍스트, 체크, 긴 메모.
- 기록 항목 개수에는 상한을 두지 않는다.
- 모든 사용자 정의 항목은 선택 입력이다.

### 12.2 기록 템플릿 버전

- 카테고리 설정 변경은 기존 version을 수정하지 않고 새 template version을 생성해 이후 기록부터 적용한다.
- field definition은 관계형으로 이름·형식·단위·순서를 저장한다.
- 과거 Activity Record에는 template version과 당시 항목명·형식·단위·순서 snapshot을 JSONB로 보존한다.
- 값 JSONB에는 사용자가 실제 입력한 field ID만 저장하고 서버가 template version에 따라 타입을 검증한다.
- 입력하지 않은 항목은 없는 값으로 저장한다.

## 13. 운동 커리큘럼

### 13.1 공통 흐름

1. 프로그램을 선택한다.
2. 초기 최대 반복 테스트 또는 요구 조건을 입력한다.
3. 앱이 권장 시작 주차·난이도를 결정한다.
4. 사용자는 시작 주차를 수정할 수 있다.
5. 앱이 운동일을 운동형 Task로 편성한다.
6. 세트별 목표와 실제 수행량을 기록한다.
7. 성공하면 진급하고 실패하면 같은 주차를 반복한다.

### 13.2 푸시업 100개 6주 루틴

- 주 3회, 회차 사이 휴식일을 둔다.
- 초기 최대 반복 횟수로 난이도 열과 시작 주차를 결정한다.
- 목표 달성이 어려우면 다음 주차로 진급하지 않는다.
- 심한 통증이 있으면 중단·회복 후 재개하도록 안내한다.
- 출처: https://m.blog.naver.com/ordinaryfit/222956705720

### 13.3 Recon Ron Pull-up Program

- 38주, 회차당 5세트로 구성한다.
- 1주차 `6-5-5-4-3`, 총 23회부터 시작한다.
- 38주차 `31-18-17-16-16`, 총 98회까지 진행한다.
- 98은 연속 횟수가 아니라 5세트 합계다.
- 주 3~5회, 세트 간 휴식 60~90초를 기본 안내한다.
- 정자세 풀업 최소 10회 가능한 사용자를 전제로 표시한다.
- 해당 주차 목표를 채우지 못하면 같은 주차를 반복한다.
- 출처: https://m.blog.naver.com/hyunwoongg/221837315019

### 13.4 출처와 안전

- 프로그램 이름, 원문 링크, 적용 조건, 버전과 주의사항을 저장한다.
- `의학적으로 검증됨` 대신 `출처 기반 프로그램`으로 표시한다.
- 앱은 임의로 훈련 강도를 올리지 않는다.
- 프로그램은 불변 catalog version과 content checksum으로 관리하고 enrollment가 시작 version을 고정 참조한다.
- catalog는 JSON Schema로 구조·주차·세트 합계를 검증하고 같은 key·version·checksum import는 한 번만 적용한다.
- 이용 범위 확인 전 실제 전체 운동표는 공개 저장소 밖 private catalog로 주입하고 공개 테스트는 합성 fixture를 사용한다.

## 14. 클라이밍 크림프 트레이닝

### 14.1 기본 일정

- 주 7일, 하루 1회 10분 세션을 기본 커리큘럼으로 제공한다.
- 원형의 하루 2회·6시간 간격 정보는 출처 참고로만 보존한다.
- 미수행 시 일반 이월 규칙을 적용한다.
- 현재 루틴 근거는 사용자가 제공한 이미지이며, 공개 배포 전 원본 출처와 이용 범위를 확인한다.

### 14.2 10라운드 순서

모든 라운드는 10초 운동 후 50초 휴식이다.

1. 하프 크림프, 15~20mm
2. 하프 크림프, 15~20mm
3. 하프 크림프, 15~20mm
4. 세 손가락 오픈 그립, 30~40mm
5. 세 손가락 오픈 그립, 30~40mm
6. 세 손가락 오픈 그립, 30~40mm
7. 두 손가락 오픈 그립, 검지+중지, 30~40mm
8. 두 손가락 오픈 그립, 중지+약지, 30~40mm
9. 두 손가락 크림프, 중지+약지, 15~20mm
10. 두 손가락 크림프, 검지+중지, 15~20mm

목표 부하는 체중의 70~80%로 안내한다.

### 14.3 타이머

- 시작 전 준비 카운트다운을 제공한다.
- 현재 그립, 손가락 조합, 엣지, 라운드와 다음 단계를 크게 표시한다.
- 운동·휴식 단계를 자동 전환한다.
- 단계 종료 전 효과음과 음성 카운트다운을 제공한다.
- 일시정지, 재개, 라운드 건너뛰기, 즉시 중단을 제공한다.
- 중단 시 수행한 라운드까지만 부분 기록으로 저장한다.
- 단순 interval 누적이 아니라 단계 종료 시각을 기준으로 남은 시간을 계산한다.
- 지원 환경에서는 Screen Wake Lock으로 화면 꺼짐을 방지한다.
- `client-domain`에 timestamp와 phase 배열만 사용하는 순수 상태 기계를 둔다.
- 상태는 `IDLE`, `PREPARING`, `WORK`, `REST`, `PAUSED`, `FINISHED`, `ABORTED`를 사용한다.
- 여러 phase가 지난 뒤 복귀하면 현재 phase로 즉시 이동하고 지나간 알림을 연속 재생하지 않는다.
- React는 표시·음향·진동·Wake Lock·visibility adapter만 담당한다.

### 14.4 안전

- 시작 전 워밍업 안내를 제공한다.
- 손가락·건·풀리 통증 또는 기존 부상이 있으면 시작하지 않도록 안내한다.
- 진행 중 통증을 느끼면 즉시 중단하도록 고정 동작을 제공한다.
- 실제 부하 적용 방식은 보조 발판, 밴드, 추가 중량 등 사용자가 기록한다.

## 15. PWA 요구사항

- iPhone Safari에서 홈 화면에 추가할 수 있어야 한다.
- standalone 모드, 앱 아이콘, 시작 URL과 고유 manifest ID를 제공한다.
- iPhone 화면을 우선 설계하고 노트북에서 반응형으로 확장한다.
- 네트워크 연결을 전제로 하며 오프라인 캐시·변경 queue·Background Sync·충돌 병합을 제공하지 않는다.
- 네트워크 저장 실패는 명확한 오류와 수동 재시도를 제공한다.
- 웹 푸시는 홈 화면 설치와 사용자 동작 기반 권한 요청을 전제로 한다.
- 타이머의 정확성은 브라우저 이벤트 지연과 무관하게 종료 시각 기준으로 복구되어야 한다.
- React Router와 TanStack Query를 사용하고 별도 전역 상태 store는 두지 않는다.
- 오늘 화면만 초기 bundle에 포함하고 운동·공부·클라이밍·설정은 route lazy loading한다.
- 초기 JavaScript gzip 150KB, 개별 lazy chunk 100KB를 CI 예산으로 둔다.

## 16. 시스템 아키텍처

### 16.1 선택안

Spring 기반 최소 MSA를 AWS Lightsail 단일 VM에서 Docker Compose로 운영한다.

기반 기술 버전은 다음으로 고정한다.

- Java 25 LTS
- Spring Boot 4.1.1
- Gradle 9.7.1 Kotlin DSL
- React 19.2 + TypeScript + Vite 8
- Node.js 24 LTS + pnpm 11.24.0
- PostgreSQL 17.11
- Apache Kafka 4.3.1 KRaft
- Docker Compose v2

```text
Internet
  |
Nginx
  |-- /                 -> todorok-web
  |-- /api/planner/**   -> planner-service
  |-- /api/activity/**  -> activity-service

Kafka KRaft
  |-- planner-service
  |-- activity-service
  `-- notification-service

PostgreSQL
  |-- planner schema / planner DB user
  |-- activity schema / activity DB user
  `-- notification schema / notification DB user
```

### 16.2 서비스 책임

#### planner-service

- 사용자 인증과 기본 프로필
- Task, 반복 규칙, 자동 이월과 지난 기록 오케스트레이션
- 할 일 메모와 날짜별 스티키노트
- 활동 완료 이벤트를 반영한 Task 상태 변경
- 비대칭 키 access JWT 발급, refresh session과 최초 사용자 bootstrap

#### activity-service

- 운동·공부·클라이밍 Activity Record
- 공부 카테고리와 기록 템플릿
- 운동 프로그램, 사용자 등록, 세션, 진급
- 클라이밍 세션과 부분 기록
- Task 이벤트의 최소 reference projection과 기록 대상 검증

#### notification-service

- Kafka 이벤트 구독
- 웹 푸시 구독 정보
- 알림 예약, 발송, 재시도와 실패 이력
- HTTP는 헬스체크와 최소 관리 API만 제공

### 16.3 클라이언트 확장 구조

- MVP 웹은 React DOM 기반 PWA로 구현한다.
- 향후 iPhone 네이티브 앱은 Expo 기반 React Native로 구현한다.
- 웹 컴포넌트와 React Native 화면 컴포넌트는 직접 공유하지 않는다.
- `client-domain`: Task·Activity·Routine의 클라이언트 도메인 타입과 순수 계산 로직을 공유한다.
- `api-client`: OpenAPI 기반 요청·응답 타입과 API 호출 계층을 공유한다.
- `validation`: 입력 스키마와 오류 매핑을 공유한다.
- `design-tokens`: 색상, 간격, 타이포그래피 토큰을 공유한다.
- 공유 패키지는 DOM, 브라우저 전역 객체와 네이티브 모듈에 직접 의존하지 않는다.
- 웹 전용 PWA·Service Worker·Wake Lock 구현은 `apps/web`에 둔다.
- 네이티브 앱을 시작할 때 `apps/mobile`을 추가하고 공유 패키지를 소비한다.

### 16.4 이벤트 후보

- `TaskScheduled`
- `TaskRolledOver`
- `ActivityCompleted`
- `ActivitySkipped`
- `RoutineAdvanced`
- `NotificationRequested`
- `TaskChanged`, `TaskDeleted`
- `ActivityVoided`, `ActivityCorrected`

### 16.5 데이터 일관성

- 서비스는 다른 서비스의 DB 스키마를 직접 조회하지 않는다.
- planner·activity의 동일 구조 outbox table을 단일 Kafka Connect·Debezium PostgreSQL connector가 감시한다.
- Debezium은 `pgoutput`과 persistent replication slot 하나를 사용한다.
- Consumer는 `processed_event(event_id)` unique inbox를 로컬 결과와 같은 트랜잭션에 저장한다.
- activity-service는 Task reference projection으로 소유자·유형·상태를 검증하고 projection 지연 시 재시도 가능한 409 오류를 반환한다.
- Activity 저장 직후 클라이언트는 `동기화 중`을 표시하고 planner projection 반영 후 확정한다.
- 분산 트랜잭션을 사용하지 않는다.

### 16.6 인증·API·계약

- 공개 회원가입은 제공하지 않고 최초 사용자는 일회성 bootstrap command로 생성한다.
- access JWT는 10분 동안 유효하며 React 메모리에만 보관한다.
- refresh token은 30일 회전형 opaque 값으로 발급하고 해시만 DB에 저장하며 HttpOnly·Secure·SameSite=Lax cookie로 전달한다.
- refresh token 재사용을 탐지하면 해당 session chain을 폐기한다.
- planner가 비대칭 키로 서명하고 activity·notification은 Spring Security Resource Server로 공개키를 독립 검증한다.
- REST의 기준 파일은 `contracts/openapi/*-v1.yaml`이며 Spring Boot 4 API interface·DTO와 TypeScript Fetch client를 생성한다.
- 외부 경로는 `/api/planner/v1/**`, `/api/activity/v1/**`를 사용한다.
- 이벤트 기준 파일은 버전별 JSON Schema이며 fixture 계약 테스트로 Java record 직렬화를 검증한다.
- Schema Registry는 사용하지 않는다.
- 오류는 RFC 9457 `ProblemDetail`과 `code`, `traceId`, `retryable`, `fieldErrors` 확장 필드를 사용한다.

### 16.7 Persistence

- 일반 aggregate 저장과 CRUD는 Spring Data JPA·Hibernate를 사용한다.
- Flyway만 스키마를 변경하고 Hibernate는 `ddl-auto=validate`를 사용한다.
- Open Session in View를 비활성화하고 양방향 entity 관계와 API entity 직접 반환을 금지한다.
- mutable aggregate는 optimistic version을 가진다.
- 달력 range DTO projection과 JSONB·partial index·locking 등 PostgreSQL 특화 기능은 명시적 JPQL 또는 native query로 구현한다.
- query 수와 N+1 회귀를 통합 테스트에서 검증한다.

## 17. 배포와 운영

### 17.1 초기 인프라

- AWS Lightsail Linux 4GB, 2 vCPU, SSD 80GB
- Nginx
- Docker Compose
- PostgreSQL
- Kafka KRaft 단일 브로커
- Kafka Connect·Debezium PostgreSQL connector 단일 worker
- S3 일일 암호화 백업
- ECR 컨테이너 이미지
- GitHub Actions OIDC 배포
- 최소 CloudWatch 로그·알람

### 17.2 초기 메모리 예산

- planner-service: 384~512MB
- activity-service: 512~640MB
- notification-service: 256MB
- Kafka: 512~768MB
- Kafka Connect: heap 512MB, 컨테이너 768MB
- PostgreSQL: 384~512MB
- Nginx: 64MB 이하
- 2GB swap을 보조로 두되 정상 메모리 대체 수단으로 사용하지 않는다.

### 17.3 DB migration과 배포 순서

- 각 서비스가 자기 스키마의 Flyway migration을 소유한다.
- 운영에서는 서비스 기동 전에 동일 이미지의 일회성 migration job을 순서대로 실행한다.
- migration이 하나라도 실패하면 새 서비스 기동을 중단하고 기존 버전을 유지한다.
- 운영 서비스에서는 Flyway 자동 실행을 끄고 Hibernate는 `validate`만 사용한다.
- 파괴적 변경은 추가·호환 코드·데이터 이관·제거의 expand/contract 단계로 나눈다.
- 적용한 migration 파일은 수정하지 않고 새 version을 추가한다.

### 17.4 8GB 승급 조건

다음 중 하나가 재현되면 Lightsail 8GB로 확장한다.

- Container OOM 또는 반복 재시작
- 지속 메모리 사용률 85% 이상
- 과도한 swap 사용과 지연 증가
- 정상 사용에서 API 응답 지연 급증
- Kafka consumer lag 지속 누적

## 18. 보안 요구사항

- 비밀번호는 단방향 강한 해시로 저장한다.
- 서비스 간 요청은 외부에서 직접 접근할 수 없게 Docker 내부 네트워크로 제한한다.
- 외부 공개 포트는 80·443으로 제한한다.
- 관리 접속은 제한된 SSH 또는 AWS 관리 접속 경로를 사용한다.
- 서버에 장기 AWS Access Key를 저장하지 않는다.
- GitHub Actions는 OIDC 역할 위임을 사용한다.
- 사용자별 데이터 접근은 인증된 사용자 ID로 검증한다.
- 백업은 암호화하고 복구 절차를 문서화·검증한다.

## 19. 비기능 요구사항

### 19.1 정확성

- 같은 이월·지난 기록 요청이 반복되어도 중복 Task와 Activity Record가 생기지 않는다.
- Kafka 중복 전달에도 상태가 한 번만 변경된다.
- 타이머는 포그라운드에서 단계 전환 오차 500ms 이내를 목표로 한다.
- 앱이 잠시 비활성화된 후 돌아오면 실제 경과 시간을 기준으로 올바른 단계로 복구한다.

### 19.2 성능

- 1인 정상 사용에서 주요 읽기 API p95 500ms 이내를 목표로 한다.
- 오늘 화면의 첫 사용 가능 상태는 일반 네트워크에서 2초 이내를 목표로 한다.
- 메모 입력은 500ms debounce 자동 저장을 사용한다.
- 달력 range summary는 최대 42일이며 선택 날짜 detail과 분리한다.
- Activity 상세는 사용자가 열 때 지연 조회하고 긴 이력은 keyset pagination을 사용한다.
- Spring Boot의 Hikari·Tomcat·Kafka consumer 기본 동시성 값으로 시작하고 metric에서 포화가 확인될 때 조정한다.
- PostgreSQL replication slot WAL은 2GB로 제한하고 slot 비활성·safe WAL·디스크 사용률을 감시한다.
- Kafka domain topic은 7일 또는 partition당 1GB, dead-letter는 30일 또는 1GB를 보존한다.
- 발행 완료 outbox는 7일, consumer inbox는 30일 후 정리한다.

### 19.3 신뢰성

- PostgreSQL은 매일 S3에 백업한다.
- 월 1회 복구 리허설로 백업 유효성을 확인한다.
- 서비스 재시작 후 outbox 미발행 이벤트 처리를 재개한다.
- Kafka 장애 시 DB 트랜잭션은 보존하고 outbox 재처리로 발행을 복구한다.
- Activity Record는 저장됐지만 planner 반영이 지연되면 UI에 `동기화 중` 상태를 표시한다.
- 알림 발송 실패는 Task·Activity 완료를 취소하지 않으며 notification-service에서 재시도·실패 이력으로 관리한다.
- 클라이밍 타이머는 이미 열린 화면에서 로컬로 계속 실행하지만 네트워크 저장 실패는 현재 화면에서 수동 재시도한다.

### 19.4 접근성

- 색상만으로 완료·건너뜀·운동·휴식을 구분하지 않는다.
- 타이머는 큰 시각 변화, 짧은 소리, 지원 기기의 진동을 함께 제공하고 각각 끌 수 있게 한다.
- 동작 감소 설정에서는 큰 애니메이션을 제거하고 색·문구·소리 중심으로 구간 전환을 알린다.
- 주요 터치 영역은 모바일에서 최소 44px을 확보한다.
- 동작 감소 설정을 존중한다.

## 20. 테스트 전략

- Task·반복·이월·Activity·진급·타이머·refresh 보안 규칙은 branch 100% 단위 테스트를 요구한다.
- 전체 기준은 line 85%, branch 80% 이상을 PR·release gate에서 적용한다.
- repository는 PostgreSQL, messaging은 Kafka, outbox 왕복은 PostgreSQL·Kafka·Debezium Testcontainers로 분리한다.
- H2와 embedded Kafka는 사용하지 않는다.
- 서비스 API는 OpenAPI 계약과 Consumer Contract Test로 고정한다.
- 이월, 지난 기록, 반복 재생성, outbox, Consumer 멱등성을 핵심 회귀 테스트로 둔다.
- 일반 PR은 영향받은 Chromium 흐름, 플랫폼 기능 PR과 develop은 WebKit을 추가한다.
- release는 macOS Safari와 실제 iPhone에서 설치·로그인·푸시·타이머·Wake Lock을 검증한다.
- 타이머는 실제 시간 대기 대신 가상 시계를 주입해 단계 전환·복귀를 테스트한다.
- Docker Compose 전체 기동 후 4GB 메모리 예산을 부하 테스트한다.
- Kafka·Connect·DB·consumer 중단은 결정론적 fault-injection과 Toxiproxy로 검증한다.
- 개발 중 60초, 커밋 전 2분, 기능 PR 10분, develop 15분, release 30분, 배포 smoke 5분을 목표로 단계별 gate를 적용한다.

## 21. 핵심 수용 기준

### AC-1 오늘 화면

- 날짜 선택 시 해당 날짜의 할 일과 스티키노트가 표시된다.
- 주간·월간 보기를 전환할 수 있고 마지막 보기 선택이 유지된다.
- 앱을 새로 열면 실제 오늘 날짜에서 시작한다.
- 일반 할 일은 한 번의 체크로 완료된다.

### AC-2 기록형 완료

- 운동·공부·클라이밍 할 일을 체크하면 전용 기록 흐름이 열린다.
- 기록 저장 전에는 완료되지 않는다.
- 기록에 시간이 있으면 기존 할 일이 시간 블록으로, 시간이 없으면 기록 요약이 붙은 완료 행으로 표시되며 별도 중복 항목을 만들지 않는다.

### AC-3 이월과 지난 기록

- 미처리 Task는 다음 사용일에 하나만 노출된다.
- 지난 날짜로 완료하면 해당 날짜에 기록되고 오늘 반복 Task가 자동 생성된다.
- 같은 요청을 재시도해도 중복이 없다.

### AC-4 공부 템플릿

- 사용자가 필드를 제한 없이 추가·정렬할 수 있다.
- 입력한 필드만 기록에 저장된다.
- 카테고리 설정 변경 후에도 과거 기록 표시가 깨지지 않는다.

### AC-5 운동 프로그램

- 초기 테스트 결과로 권장 시작 단계를 제시한다.
- 사용자가 시작 단계를 수정할 수 있다.
- 회차 실패 시 자동 진급하지 않는다.

### AC-6 클라이밍 타이머

- 10개 라운드가 정확한 그립·손가락 조합 순서로 진행된다.
- 각 라운드는 10초 운동과 50초 휴식이다.
- 중단 시 부분 기록이 저장되고 완료로 집계되지 않는다.

### AC-7 PWA

- iPhone 홈 화면에서 독립 앱처럼 실행된다.
- 네트워크 연결 상태에서 오늘 일정과 기록 기능이 동작한다.
- 네트워크 저장 실패 시 오류와 수동 재시도가 표시된다.

### AC-8 알림 격리

- notification-service가 중단되어도 할 일과 활동 기록 생성·완료가 가능하다.
- 서비스 복구 후 미발송 알림이 중복 없이 재시도된다.
- 발송 실패 원인과 최종 상태를 조회할 수 있다.

### AC-9 알림 정책

- 알림은 기본 비활성화하고 홈 화면 설치 후 설정의 사용자 동작으로만 권한을 요청한다.
- 활성화 시 기본 오전 8시 오늘 요약을 제공하며 시간을 수정할 수 있다.
- 개별 Task 알림은 사용자가 시간과 reminder를 명시한 경우에만 예약한다.
- 기본 방해 금지는 22시~07시이며 해당 시간의 개별 알림은 다음 날로 미루지 않고 만료 처리한다.
- 오늘 요약은 오전 10시, 개별 알림은 예정 시각 30분 이후까지 실패하면 만료 처리한다.

## 22. 성공 지표

- 4주 동안 주 5일 이상 직접 사용한다.
- 기존 TodoMate를 열지 않고 일정·할 일·메모를 처리한 날이 80% 이상이다.
- 기록형 활동 완료 중 90% 이상이 도메인 기록과 정상 연결된다.
- 이월·지난 기록으로 생성된 중복 Task가 0건이다.
- 클라이밍 타이머 세션 중 잘못된 그립 순서가 0건이다.
- 4GB 환경에서 OOM 없이 7일 연속 운영된다.

## 23. 위험과 대응

| 위험 | 영향 | 대응 |
|---|---|---|
| 기능 범위가 넓음 | MVP 지연 | 오늘 화면부터 세로로 완성하고 활동 기능을 단계적으로 연결 |
| 학습 목적 MSA | 개발·운영 복잡도 증가 | 3개 서비스만 유지하고 물리 인프라는 공유 |
| 단일 VM | 전체 장애 | 외부 백업, Compose 재기동, 복구 절차 문서화 |
| 4GB 메모리 | OOM·지연 | JVM 제한, 서버 빌드 금지, 부하 테스트, 8GB 승급 기준 |
| Kafka·DB 이중 쓰기 | 상태 불일치 | outbox와 멱등 Consumer |
| PWA 백그라운드 제약 | 타이머·알림 오작동 | 종료 시각 기반 복구, Wake Lock, 실제 iPhone 검증 |
| 손가락 훈련 부상 | 사용자 피해 | 워밍업·부하 경고·통증 중단·부분 기록 |
| 운동표 저작권·출처 | 공개 배포 제한 | 출처·링크 보존, 데이터 사용 범위 출시 전 검토 |
| 크림프 루틴 원본 출처 미확정 | 공개 배포·안전 설명 부족 | 원본 영상·작성자·사용 조건 확인 전 외부 공개 보류 |

## 24. 구현 순서

1. 프로젝트·CI·Compose·PostgreSQL·Kafka 기반
2. 인증과 planner-service의 오늘 화면·Task·반복·메모
3. activity-service의 공통 Activity Record와 공부 템플릿
4. 운동 프로그램 엔진과 푸시업·풀업 데이터
5. 클라이밍 10분 타이머와 부분 기록
6. notification-service와 PWA 웹 푸시
7. iPhone 설치 경험과 온라인 저장 오류 처리
8. AWS Lightsail 배포, 백업 복구, 4GB 부하 테스트

## 25. 대안 검토

### A. Supabase 중심 MVP

- 장점: 빠른 구현, 낮은 운영 부담
- 단점: Spring MSA·Kafka 학습 목표를 달성하지 못함
- 결정: 미채택

### B. Spring 모듈형 모놀리스

- 장점: 가장 단순하고 4GB 운영에 유리
- 단점: 서비스 간 계약·Kafka·독립 배포 경험이 제한됨
- 결정: 미채택

### C. Spring 최소 MSA + 단일 VM

- 장점: 서비스 경계와 이벤트 기반 구조를 학습하면서 비용을 통제할 수 있음
- 단점: 단일 장애점이며 MSA의 운영 이점은 제한적
- 결정: 채택

## 26. 전제 재검토

- 이 제품은 MSA가 필요해서 MSA를 선택한 것이 아니다.
- MSA는 사용자의 명시적인 학습 목표이며, 추가 복잡도와 비용을 수용한 결정이다.
- 서비스별 클라우드 인프라를 분리하지 않는다.
- 4GB에서 실패하면 기능을 삭제하기보다 측정 결과에 따라 8GB로 확장한다.

## 27. 열린 결정

- React Native 앱 착수 시점과 Expo SDK 버전
- 도메인과 상표 최종 확인
- 운동 프로그램 원문 데이터의 공개 배포·저작권 범위
- 크림프 루틴 원본 영상·작성자와 이용 범위

## 28. 다음 단계

1. 기능 단위 이슈를 발행하고 각 이슈에 PRD 요구사항과 수용 기준을 연결한다.
2. 엔지니어링 계획에서 기술 버전, 저장소 구조, API·이벤트 계약과 마이그레이션을 확정한다.
3. 승인된 `DESIGN.md`를 기준으로 오늘 화면부터 세로 기능 단위로 구현한다.

## 29. 변경 관리와 Git 운영

### 29.1 기준 문서 우선

- 본 PRD를 제품 요구사항의 단일 기준 문서로 사용한다.
- 구현 중 요구사항, 상태 전이, 사용자 흐름, 서비스 경계 또는 수용 기준이 바뀌면 코드보다 PRD를 먼저 수정한다.
- 기능 변경 이슈는 PRD 변경 내용과 영향을 받는 수용 기준을 명시한다.
- 코드와 PRD가 충돌하면 승인된 최신 PRD를 우선하고 코드를 맞춘다.

### 29.2 이슈 단위 개발

- 기능 하나를 이슈 하나로 관리한다.
- 이슈에는 문제, 범위, 비범위, 수용 기준, 테스트, 문서 변경과 의존성을 작성한다.
- 하나의 Pull Request는 원칙적으로 하나의 기능 이슈만 닫는다.
- Pull Request 본문에는 항상 `Closes #<issue-number>`를 포함해 이슈와 연결한다.
- 이슈 제목·본문과 Pull Request 제목·본문은 원칙적으로 한글로 작성한다.
- 범위가 커지면 기존 이슈에 계속 추가하지 않고 하위 이슈로 분리한다.

### 29.3 Git Flow와 GitHub Flow 혼합

- `main`: 배포 가능한 안정 버전과 릴리스 태그만 유지한다.
- `develop`: 다음 릴리스의 통합 브랜치다.
- `feature/<issue-number>-<slug>`: `develop`에서 분기하고 Pull Request로 `develop`에 병합한다.
- `fix/<issue-number>-<slug>`: 일반 버그 수정 브랜치다.
- `release/<version>`: `develop`에서 분기해 릴리스 검증 후 `main`에 병합하고 태그를 생성한다.
- `hotfix/<issue-number>-<slug>`: `main`에서 분기해 긴급 수정 후 `main`과 `develop` 양쪽에 반영한다.
- 브랜치의 `<slug>`는 기능을 알아볼 수 있는 간결한 영어로 작성한다.
- `main`과 `develop`에는 직접 커밋하거나 직접 푸시하지 않는다.
- 1인 프로젝트라도 Pull Request, CI, 자체 diff review와 수용 기준 확인을 생략하지 않는다.

### 29.4 커밋

- 커밋은 되돌릴 수 있는 기능 단위로 작게 유지한다.
- Conventional Commits 형식은 유지하되 설명은 원칙적으로 한글로 작성한다. 예: `feat(planner): 할 일 자동 이월 정책 추가`.
- 테스트가 실패하거나 문서와 코드가 불일치한 상태는 커밋하지 않는다.
- 스펙 변경이 필요한 기능은 PRD 수정 커밋을 구현 커밋보다 먼저 둔다.
- squash·merge 커밋 제목과 릴리스 로그도 원칙적으로 한글로 작성한다.

### 29.5 기록 정책

- 프로젝트 산출물과 Git·GitHub 기록에는 AI 도구, 모델 또는 자동 작성 주체의 이름을 남기지 않는다.
- 금지 범위에는 커밋 메시지, author·co-author trailer, 브랜치명, 이슈, Pull Request, 댓글, 문서, changelog와 릴리스 노트가 포함된다.
- 프로젝트 기록의 작성자와 책임자는 실제 프로젝트 소유자와 기여자만 사용한다.

### 29.6 공개 저장소

- GitHub 공개 저장소는 `UncleSamsun/todorok`을 사용한다.
- 공개 저장소에 비밀값, 개인 일정·운동·공부 데이터와 운영 백업을 커밋하지 않는다.
- 실제 사용자 데이터가 필요한 테스트는 합성 fixture만 사용한다.
