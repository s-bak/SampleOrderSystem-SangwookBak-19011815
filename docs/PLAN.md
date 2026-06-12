# PLAN.md — SampleOrderSystem 단계별 개발 계획

**기준 문서:** `docs/PRD.md`  
**기술 스택:** Java 17+, Gradle, JUnit 5  
**개발 원칙:** 각 Phase는 독립적으로 빌드·테스트 가능하도록 구성한다.

**진행 절차 (매 Phase 공통)**
1. `docs/design/phaseN.md` 설계 문서 작성 → 사용자 검토 요청
2. 사용자 승인 후 구현 시작
3. `./gradlew test` BUILD SUCCESSFUL 확인
4. 사용자 승인 후 `git push` 실행

---

## Phase 1 — 프로젝트 골격 및 패키지 구조 설정

**목표:** 이후 모든 Phase의 기반이 되는 패키지 레이아웃과 공통 예외 클래스를 확립한다.

**작업 목록**
- 패키지 구조 생성
  ```
  org.example
  ├── domain          # 엔티티 (Sample, Order, ProductionJob)
  ├── repository      # 인메모리 저장소
  ├── service         # 비즈니스 로직
  ├── exception       # 커스텀 예외
  └── ui              # 콘솔 I/O
  ```
- `InvalidOrderStateTransitionException` 작성 (RuntimeException 상속)
- `Application.java` main 진입점 stub 작성

**검증 기준**
- `./gradlew build` 성공

---

## Phase 2 — Sample 도메인 구현

**목표:** 시료 엔티티와 유효성 검사 규칙을 구현한다.

**작업 목록**
- `Sample` 클래스
  - 필드: `id(String)`, `name(String)`, `avgProductionTime(double)`, `yield(double)`, `stock(int)`
  - ID 형식: `S-XXX` (S-000부터 시작)
  - 생성자에서 유효성 검사: 평균 생산시간 `> 0`, 수율 `(0, 1]`, 재고 `≥ 0`
- `SampleRepository` — 인메모리 `Map<String, Sample>` 저장소
  - `save`, `findById`, `findAll`, `findByNameContaining`
  - 중복 ID 저장 시 예외

**검증 기준**
- `SampleTest`: 정상 생성, 수율 경계값(0, 0.01, 1.0, 1.01), 음수 재고, 중복 ID 등록 → 단위 테스트 통과

---

## Phase 3 — Order 도메인 및 상태 정의

**목표:** 주문 엔티티와 상태 전이 규칙을 구현한다.

**작업 목록**
- `OrderStatus` 열거형: `RESERVED`, `REJECTED`, PRODUCING`, `CONFIRMED`, `RELEASE`
- `Order` 클래스
  - 필드: `orderId(String)`, `customerName(String)`, `sample(Sample)`, `quantity(int)`, `status(OrderStatus)`, `createdAt(LocalDateTime)`
  - ID 발급 규칙: `O-XXX` (O-001부터 순차 발급)
  - `transitionTo(OrderStatus next)`: 허용 전이 매트릭스 검증, 불허 시 `InvalidOrderStateTransitionException`
- `OrderRepository` — 인메모리 저장소
  - `save`, `findById`, `findAll`, `findByStatus`

**검증 기준**
- `OrderStatusTransitionTest`: 허용된 전이 6가지 + 불허 전이 대표 케이스 → 단위 테스트 통과

---

## Phase 4 — SampleService (시료 관리 비즈니스 로직)

**목표:** 시료 등록·조회·검색 서비스 레이어를 구현한다.

**작업 목록**
- `SampleService`
  - `register(String id, String name, double avgTime, double yield, int initialStock)` — ID 중복 검사 포함
  - `findAll()` — 전체 목록 반환
  - `search(String keyword)` — 시료명 부분 일치 검색
  - `searchById(String keyword)` — ID 부분 일치 검색
  - `searchByAvgProductionTime(double time)` — 수학적 동치 검색
  - `searchByYield(double yield)` — 수학적 동치 검색
  - `findById(String id)` — 없으면 예외
  - `existsById(String id)` — boolean 반환

**검증 기준**
- `SampleServiceTest`: 등록 성공, 중복 등록 실패, 검색 결과 있음/없음 → 단위 테스트 통과

---

## Phase 5 — OrderService (주문 등록 · 조회)

**목표:** 주문 접수(RESERVED 생성) 및 목록 조회 서비스를 구현한다.

**작업 목록**
- `OrderService`
  - `placeOrder(String sampleId, String customerName, int quantity)` — Sample 존재 확인, 수량 ≥ 1 검사, RESERVED 상태로 생성
  - `findAll()` — 전체 주문 목록
  - `findById(String orderId)` — 없으면 예외

**검증 기준**
- `OrderServiceTest`: 정상 주문 등록, 없는 시료 ID, 수량 0·음수 → 단위 테스트 통과

---

## Phase 6 — ApprovalService (주문 승인 / 거절 + 재고 차감)

**목표:** 생산 담당자의 승인·거절 로직과 재고 차감을 구현한다.

**작업 목록**
- `ApprovalService`
  - `approve(String orderId)`:
    1. 상태가 `RESERVED` 확인
    2. `sample.stock >= order.quantity` → 재고 차감 → `CONFIRMED`
    3. 재고 부족 → `ProductionQueue`에 작업 등록 → `PRODUCING`
  - `reject(String orderId)`: 상태 `RESERVED` → `REJECTED`

**검증 기준**
- `ApprovalServiceTest`: 재고 충분 시 CONFIRMED, 재고 부족 시 PRODUCING, RESERVED 아닌 상태 거절 시 예외 → 단위 테스트 통과

---

## Phase 7 — ProductionLine (생산 라인)

**목표:** FIFO 생산 큐와 생산 완료 처리를 구현한다.

**작업 목록**
- `ProductionJob` 클래스: `order`, `actualProductionCount(int)`, `totalProductionTime(double)` 계산 포함
  - `actualProductionCount = ceil(부족분 / (yield × 0.9))`
  - `totalProductionTime = avgProductionTime(double) × actualProductionCount`
- `ProductionQueue`: `Queue<ProductionJob>` FIFO
  - `enqueue(Order)`, `peek()` (현재 생산 중), `getWaiting()` (대기 목록)
- `ProductionService`
  - `complete(String orderId)`: `PRODUCING` 상태 확인 → 재고 반영 → `CONFIRMED`

**검증 기준**
- `ProductionJobTest`: 생산량·생산시간 공식 계산 정확성 (수율 0.8, 부족분 5 등 경계값) → 단위 테스트 통과
- `ProductionServiceTest`: 완료 처리 성공, 비PRODUCING 상태에서 완료 시도 → 예외 → 통과

---

## Phase 8 — ReleaseService + MonitoringService

**목표:** 출고 처리와 모니터링 서비스를 구현한다.

**작업 목록**
- `ReleaseService`
  - `release(String orderId)`: `CONFIRMED` 확인 → `RELEASE` 전이
- `MonitoringService`
  - `getOrdersByStatus()`: RESERVED / PRODUCING / CONFIRMED / RELEASE 상태별 주문 반환 (REJECTED 제외)
  - `getStockStatus()`: 시료별 재고 상태 계산
    - `여유`: `stock ≥ (RESERVED + PRODUCING 요청 수량 합계)`
    - `부족`: `stock > 0` but 요청 수량에 미달
    - `고갈`: `stock == 0`

**검증 기준**
- `ReleaseServiceTest`: 정상 출고, CONFIRMED 아닌 상태 → 예외 → 통과
- `MonitoringServiceTest`: 재고 상태 3가지 경계 조건 → 단위 테스트 통과

---

## Phase 9 — 콘솔 UI 구현

**목표:** 전체 메뉴 시스템과 콘솔 I/O를 연결한다.

### Phase 9 기본 (phase9-01) — UI 뼈대

**작업 목록**
- `ConsoleIO`: `Scanner` 래퍼, `readInt()`, `readDouble()` 제공
- `MainMenu`: 메뉴 출력 및 선택 라우팅
- 각 서브메뉴 핸들러
  - `SampleMenuHandler` — 등록(초기 재고=0 고정) / 목록 / 검색
  - `OrderMenuHandler` — 주문 등록 / 목록
  - `ApprovalMenuHandler` — 접수 목록 / 승인 / 거절
  - `MonitoringMenuHandler` — 주문량 / 재고량
  - `ReleaseMenuHandler` — 출고 대기 목록 / 출고 실행
  - `ProductionMenuHandler` — 현황 / 대기 큐 / 완료 처리
- 메인 메뉴에 전체 시료 요약 정보(등록 시료 수, 총 재고) 표시

**검증 기준**
- `./gradlew run`으로 콘솔 실행 → 메뉴 정상 출력 확인 (수동 검증)

---

### Phase 9-02 — JSON 데이터 영속성

**목표:** 실행 간 데이터를 유지하기 위해 `data/db.json`에 저장·불러오기를 구현한다.

**작업 목록**
- `JsonDataStore`: Jackson으로 시료·주문·생산 큐·주문 ID 시퀀스 직렬화/역직렬화
- `Order.restore()` 정적 팩터리 — JSON 복원 전용 생성자
- `ProductionQueue.enqueueJob()`, `getAll()` 추가 — 큐 상태 복원
- `OrderRepository.getSequence()`, `setSequence()` 추가 — ID 시퀀스 영속
- `MainMenu`: 메뉴 1·2·3·5·6 처리 후 `dataStore.save()` 호출

---

### Phase 9-03 — 시료 등록 중복 ID 즉시 검증

**목표:** 시료 등록 시 ID 중복이면 다른 필드를 묻지 않고 ID만 재입력받는다.

**작업 목록**
- `SampleService.existsById(String id)` 추가
- `SampleMenuHandler.register()`: ID 입력 루프 — 중복 시 오류 출력 후 ID 재입력

---

### Phase 9-04 — 평균 생산시간 타입 변경 (double, 0 초과)

**목표:** 평균 생산시간을 소수로 입력받을 수 있도록 타입을 `int`에서 `double`로 변경한다.

**작업 목록**
- `Sample.avgProductionTime`: `int` → `double`, 검증 `< 1` → `<= 0`
- `ProductionJob.totalProductionTime`: `int` → `double`
- `SampleService.register()` 파라미터 변경
- `JsonDataStore.SampleDto.avgProductionTime`: `int` → `double`
- `ConsoleIO.readDouble()` 추가
- `SampleMenuHandler`, `ProductionMenuHandler` 출력 포맷 수정

---

### Phase 9-05 — 시료 검색 항목 선택 기능

**목표:** 시료 검색 시 시료 ID·이름·평균 생산시간·수율 중 항목을 먼저 선택하게 한다.

**작업 목록**
- `SampleRepository`: `findByIdContaining()`, `findByAvgProductionTime()`, `findByYield()` 추가
  - 수치 비교: `Double.compare()` 사용 (0.9 == 0.90 보장)
- `SampleService`: `searchById()`, `searchByAvgProductionTime()`, `searchByYield()` 추가
- `SampleMenuHandler.search()`: 항목 선택 서브메뉴로 교체

**검증 기준**
- `./gradlew test` 전체 통과 (103개)

---

### Phase 9-06 — 시료 목록 테이블 정렬 개선

**목표:** 시료 목록 조회 시 컬럼 구분자(`|`)와 정렬이 맞도록 출력 형식을 개선한다.

**작업 목록**
- `SampleMenuHandler`: `printSeparator()`, `printHeader()`, `printRow()` 추가
  - `+---+` 형식의 테두리 구분선 사용
  - 헤더·데이터 행 모두 동일한 `String.format` 패턴 사용 (Java 문자 수 기준 정렬)

---

### Phase 9-07 — 주문 등록 시료 ID 검증 및 주문 목록 시료 ID 표시

**목표:** 주문 등록 시 존재하지 않는 시료 ID 즉시 검증, 주문 목록에서 시료명 옆에 시료 ID 표시.

**작업 목록**
- `OrderMenuHandler`: `SampleService` 주입, `placeOrder()` ID 입력 루프 추가
- `MainMenu`: `OrderMenuHandler` 생성자에 `sampleService` 전달
- `OrderMenuHandler.printRow()`: `시료명 (시료ID)` 형태로 출력
- `OrderMenuHandler.printHeader()`: 헤더도 `시료명 (ID)` 컬럼으로 수정

---

### Phase 9-08 — 변경 작업 직후 즉시 JSON 저장

**목표:** 각 데이터 변경 성공 직후 즉시 `data/db.json`에 저장한다.

**작업 목록**
- `SampleMenuHandler`, `OrderMenuHandler`, `ApprovalMenuHandler`, `ReleaseMenuHandler`, `ProductionMenuHandler` 각각에 `JsonDataStore` 주입
- 시료 등록·주문 등록·승인·거절·출고·생산 완료 성공 직후 `dataStore.save()` 호출
- `MainMenu`: 핸들러 종료 후 일괄 `save()` 호출 제거

---

### Phase 9-09 — 승인/거절 대기 주문 없음 즉시 차단 + 거절 주문 목록 조회

**목표:** 대기 주문이 없을 때 승인·거절 진입을 즉시 차단하고, 거절된 주문 내역 조회 기능을 추가한다.

**작업 목록**
- `ApprovalMenuHandler.approve()`: `RESERVED` 주문 목록이 비어 있으면 오류 메시지 출력 후 즉시 반환 (주문 ID 입력 프롬프트 생략)
- `ApprovalMenuHandler.reject()`: 동일하게 대기 주문 없음 시 즉시 반환
- `ApprovalMenuHandler`: 메뉴에 **2. 거절 주문 목록 조회** 추가 → 기존 승인/거절은 3·4번으로 재번호
- `ApprovalMenuHandler.listRejected()`: `REJECTED` 상태 주문 테이블 출력; 없으면 "거절된 주문이 없습니다" 안내

---

## Phase 10 — 통합 시나리오 검증 및 마무리

**목표:** 전체 비즈니스 흐름을 end-to-end 시나리오로 검증하고 코드를 정리한다.

**작업 목록**
- 통합 테스트 시나리오 작성
  1. 시료 등록 → 주문 → 재고 충분 → 승인(CONFIRMED) → 출고(RELEASE)
  2. 시료 등록 → 주문 → 재고 부족 → 승인(PRODUCING) → 생산 완료(CONFIRMED) → 출고(RELEASE)
  3. 주문 거절 시나리오 (RESERVED → REJECTED)
  4. 불허 상태 전이 시도 → 예외 발생 확인
- 재고 상태(여유/부족/고갈) 표시 검증
- 예외 메시지 출력 포맷 통일
- `./gradlew test` 전체 통과 확인

**검증 기준**
- `IntegrationTest`: 위 4개 시나리오 모두 통과
- `./gradlew build` 경고 없이 성공

---

## Phase 요약표

| Phase | 내용 | 주요 산출물 |
|:---:|---|---|
| 1 | 프로젝트 골격 | 패키지 구조, 공통 예외, main stub |
| 2 | Sample 도메인 | `Sample`(avgTime: double, >0), `SampleRepository` |
| 3 | Order 도메인 + 상태 전이 | `Order`, `OrderStatus`, `OrderRepository` |
| 4 | SampleService | `SampleService` + 단위 테스트 |
| 5 | OrderService (주문 등록) | `OrderService` + 단위 테스트 |
| 6 | ApprovalService (승인/거절) | `ApprovalService` + 단위 테스트 |
| 7 | ProductionLine | `ProductionJob`(totalTime: double), `ProductionQueue`, `ProductionService` |
| 8 | ReleaseService + Monitoring | `ReleaseService`, `MonitoringService` |
| 9-01 | 콘솔 UI 뼈대 | `MainMenu`, 각 `*MenuHandler`, 초기 재고=0 고정 |
| 9-02 | JSON 영속성 | `JsonDataStore`, `data/db.json` |
| 9-03 | 중복 ID 즉시 검증 | `SampleMenuHandler` ID 루프 재입력 |
| 9-04 | 평균 생산시간 double | `Sample`, `ProductionJob`, `ConsoleIO.readDouble()` |
| 9-05 | 시료 검색 항목 선택 | ID·이름·생산시간·수율별 검색 |
| 9-06 | 시료 목록 테이블 정렬 | `+---+` 구분선, `String.format` 기준 컬럼 정렬 |
| 9-07 | 주문 시료 ID 검증 + 목록 표시 | 주문 등록 ID 루프, 주문 목록 `시료명 (ID)` 출력 |
| 9-08 | 변경 직후 즉시 저장 | 각 핸들러에 `JsonDataStore` 주입, 성공 직후 `save()` |
| 9-09 | 승인/거절 대기 없음 차단 + 거절 목록 조회 | `ApprovalMenuHandler` 즉시 차단, `listRejected()` 추가 |
| 10 | 통합 검증 | `IntegrationTest`, 전체 `./gradlew test` |
