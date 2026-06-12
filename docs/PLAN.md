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

### Phase 9-10 — 시료명 · 고객명 공백 입력 차단

**목표:** 시료 등록 시 시료명, 주문 등록 시 고객명이 공백이면 오류 메시지를 출력하고 중단한다.

**작업 목록**
- `SampleMenuHandler.register()`: 시료명 입력 루프 — `isBlank()` 시 오류 출력 후 재입력
- `OrderMenuHandler.placeOrder()`: 고객명 입력 루프 — `isBlank()` 시 오류 출력 후 재입력

---

### Phase 9-11 — 모니터링 ANSI 색상 표시

**목표:** 모니터링 화면에서 주문 상태 및 재고 상태를 색상으로 직관적으로 구분한다.

**작업 목록**
- `MonitoringMenuHandler`에 ANSI 색상 상수 추가 (`char ESC = 0x1B` 기반)
  - `CYAN` (`[96m`) — RESERVED 하늘색
  - `GREEN` (`[92m`) — CONFIRMED 연두색 / 재고 여유 연두색
  - `ORANGE` (`[38;5;214m`) — PRODUCING 주황색 / 재고 부족 주황색
  - `PURPLE` (`[95m`) — RELEASE 연보라색
  - `RED` (`[91m`) — 재고 고갈 빨강색
- `colorOf(OrderStatus)` 헬퍼: 상태 → ANSI 색상 문자열 반환
- `stockColorOf(String)` 헬퍼: 재고 상태 문자열(`여유`/`부족`/`고갈`) → ANSI 색상 반환
- `showOrdersByStatus()`: 상태 헤더·테이블 헤더·구분선·각 행에 색상 적용 후 `RESET`
- `showStockStatus()`: 각 시료 행에 재고 상태별 색상 적용 후 `RESET`

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-12 — 메인 메뉴 5·6번 순서 변경

**목표:** 생산 라인(5번)과 출고 처리(6번)의 메뉴 순서를 업무 흐름에 맞게 조정한다.

**작업 목록**
- `MainMenu.printHeader()`: 5번 ↔ 6번 메뉴 텍스트 교체
- `MainMenu.run()` switch: `case "5"` → `productionHandler`, `case "6"` → `releaseHandler`

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-13 — 모니터링 주문량 확인에 시료 ID 표시

**목표:** 주문량 확인 화면에서 시료명 옆에 시료 ID를 `시료명 (S-XXX)` 형태로 함께 표시한다.

**작업 목록**
- `MonitoringMenuHandler.showOrdersByStatus()`: 시료 컬럼 너비 `%-15s` → `%-22s`로 확장
- 헤더 컬럼명 `시료명` → `시료명 (ID)`
- 데이터 행: `o.getSample().getName() + " (" + o.getSample().getId() + ")"` 조합 출력
- 구분선 길이 44 → 51로 조정

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-15 — 생산 현황 조회 상세 출력 + 진행률

**목표:** 생산 현황 조회 화면에 주문·시료 전체 정보와 실시간 진행률을 표시한다.

**작업 목록**
- `ProductionJob`
  - `startedAt(LocalDateTime)` 필드 추가
  - 생성자 시그니처를 `(Order, int shortfall, LocalDateTime startedAt)`으로 변경 — `now()`를 생성자 내부에서 호출하지 않음
- `ProductionQueue.enqueue(Order)`: `LocalDateTime.now()`를 캡처해 `ProductionJob` 생성 — **승인 시점**이 `startedAt`으로 기록됨
- `JsonDataStore`
  - `QueueItemDto`에 `startedAt(String)` 필드 추가
  - save/load 시 ISO-8601 문자열로 직렬화/역직렬화 (기존 DB에 값 없으면 현재 시각 fallback)
- `ProductionMenuHandler.showCurrent()`: 출력 컬럼 변경
  - 기존: 주문ID, 시료명, 실생산수, 총생산시간
  - 변경: 주문번호, 시료명, 시료ID, 주문량, 재고수량, 부족수량, 실생산량, 소요시간(min), 진행률(%)
- `printCurrentJobRow()` 신규 추가 (대기 큐용 `printJobRow()`와 분리)
  - 진행률 = `min(100.0, Duration.between(startedAt, now).toMinutes() / totalProductionTime * 100)`

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-16 — 진행률 정밀 계산 + 100% 도달 시 재고 자동 완료

**목표:** 진행률을 밀리초 단위로 정확하게 계산하고, 100% 도달 시 생산 완료를 자동 처리해 재고를 즉시 반영한다.

**작업 목록**
- `ProductionMenuHandler.calcProgress(ProductionJob)` 신규 추출
  - 기존: `Duration.toMinutes()` (long, 1분 미만 → 0% 버그)
  - 변경: `Duration.toMillis() / (totalProductionTime × 60_000)` — 밀리초 기반으로 소수점 단위까지 정확히 계산
- `ProductionMenuHandler.showCurrent()` 재구성
  - 진행률 계산 → 행 출력 → 100% 여부 확인 순으로 분리
  - 100% 도달 시 `productionService.complete()` + `dataStore.save()` 자동 호출 → 재고 업데이트 메시지 출력
- `printCurrentJobRow(ProductionJob, double progressPct)` 시그니처 변경 — 외부에서 계산된 값을 수신

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-17 — 생산 큐 순차 처리 (enqueuedAt / startedAt 분리)

**목표:** 한 번에 한 작업만 생산하고, 큐에 삽입된 시각(`enqueuedAt`)과 실제 생산 시작 시각(`startedAt`)을 구분하여 FIFO 순차 처리를 보장한다.

**작업 목록**
- `ProductionJob`
  - `enqueuedAt(LocalDateTime)`: 큐 삽입 시각 (승인 시점), 항상 설정
  - `startedAt(LocalDateTime, nullable)`: 실제 생산 시작 시각; `null`이면 대기 중
  - `start()`: 대기 → 생산 중 전환 (`startedAt = now()`)
  - `isActive()`: `startedAt != null`
  - `restore(order, shortfall, enqueuedAt, startedAt)` 정적 팩터리 (JSON 복원용)
- `ProductionQueue`
  - `enqueue()`: `enqueuedAt=now()` 기록, 큐가 비어 있으면 즉시 `job.start()`, 아니면 `startedAt=null`로 대기
  - `startNext()`: 헤드 제거 후 다음 대기 작업에 `start()` 호출
- `ProductionService.complete()`: 기존 완료 처리 후 `productionQueue.startNext()` 호출
- `JsonDataStore`
  - `QueueItemDto`에 `enqueuedAt`, `startedAt`(nullable) 추가
  - 구 DB 호환: `enqueuedAt` 없으면 기존 `startedAt`을 `enqueuedAt`으로 사용하고 활성 상태로 복원
- `ProductionMenuHandler`
  - `showCurrent()`: `isActive()` 검사 추가 — 비활성 헤드면 "생산 중인 작업이 없습니다" 출력
  - `showWaiting()`: 접수시각(`enqueuedAt`) 컬럼 추가 (`MM/dd HH:mm` 형식)

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-18 — 재고 차감 시점 출고로 이동 + shortfall에 CONFIRMED 수량 반영

**목표:** 재고 차감을 CONFIRMED 시점에서 RELEASE 시점으로 이동하고, 승인 시 가용 재고를 `stock − CONFIRMED 수량`으로 계산하여 shortfall을 정확히 산정한다.

**작업 목록**
- `ApprovalService.approve()`:
  - CONFIRMED 시 `decreaseStock()` 제거
  - 가용 재고 = `sample.stock − sum(CONFIRMED 주문 수량, 동일 시료 기준)`
  - 재고 충분 시 → `CONFIRMED` (재고 불변)
  - 재고 부족 시 → `shortfall = 주문 수량 − max(0, 가용 재고)`, `enqueue(order, shortfall)`, `PRODUCING`
- `ProductionQueue.enqueue(Order, int shortfall)`: shortfall을 외부에서 받도록 시그니처 변경 (내부 계산 제거)
- `ReleaseService.release()`: `decreaseStock(quantity)` 추가 — 출고 시 재고 차감
- 관련 단위 테스트 일체 갱신

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-19 — 재고 상태 판단 기준 단순화

**목표:** 재고 상태(여유/부족/고갈) 판단을 물리 재고와 대기 수량 기준으로 단순화한다.

**작업 목록**
- `MonitoringService.StockStatusEntry.determineStatus()` 수정
  - `고갈`: `stock == 0`
  - `부족`: `stock > 0` AND `pendingQuantity > 0` (RESERVED + PRODUCING 대기 존재)
  - `여유`: `stock > 0` AND `pendingQuantity == 0`
- `MonitoringServiceTest` 갱신: 재고 상태 3가지 기준에 맞춰 테스트 케이스 재구성

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-20 — 생산 완료 백그라운드 자동 처리

**목표:** 생산 현황 조회 없이도 진행률 100% 도달 시 즉시 생산 완료·재고 업데이트가 이루어지도록 백그라운드 스케줄러를 도입한다.

**작업 목록**
- `ProductionScheduler` 신규
  - `ScheduledExecutorService` (데몬 스레드, 1초 주기) 로 `checkAndComplete()` 반복 실행
  - 현재 활성 작업의 진행률 ≥ 100% → `productionService.complete()` + `dataStore.save()` 자동 호출
  - 중복 호출 시 예외 무시
- `Application`: `scheduler.start()` (DB 로드 후), `scheduler.stop()` (메인 루프 종료 후)
- `ProductionMenuHandler`: 자동완료 코드 제거, 불필요해진 `ProductionService` · `JsonDataStore` 의존성 제거

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-21 — 대기 주문 확인에 시료 ID · 주문량 컬럼 추가

**목표:** 대기 큐 조회 화면에 시료 ID와 주문량을 함께 표시한다.

**작업 목록**
- `ProductionMenuHandler.showWaiting()`: 헤더에 `시료ID`, `주문량` 컬럼 추가 (구분선 67 → 85)
- `ProductionMenuHandler.printJobRow()`: `order.getSample().getId()`, `order.getQuantity()` 출력 추가

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-22 — 생산 완료 시 재고 단위별 점진 업데이트

**목표:** 진행률 100% 시 일괄 재고 반영 대신, 생산 진행에 따라 1개씩 재고를 추가한다.

**작업 목록**
- `ProductionJob`: `stockAdded(int)` 필드 추가; `addStock(int)`, `getStockAdded()`, `getRemainingShortfall()` 제공
  - `restore()` 시그니처에 `stockAdded` 파라미터 추가
- `ProductionScheduler.checkAndComplete()` 재구성
  - `msPerUnit = totalProductionTime × 60,000 / shortfall`
  - `expectedUnits = min(shortfall, floor(elapsedMs / msPerUnit))`
  - `newUnits = expectedUnits − stockAdded` 만큼 `increaseStock` + `addStock` + `dataStore.save()`
  - `stockAdded >= shortfall` 시 `productionService.complete()` 호출
- `ProductionService.complete()`: `increaseStock` 제거 — 재고는 스케줄러가 모두 추가한 뒤 호출됨
- `JsonDataStore.QueueItemDto`: `stockAdded` 필드 추가, save/load 반영
- `ProductionMenuHandler.printCurrentJobRow()`: 부족수량 컬럼을 `getRemainingShortfall()`로 표시

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-23 — 가용 재고 계산 시 생산 중 기추가 재고 차감

**목표:** 부분 생산 중인 작업이 이미 재고에 추가한 수량(`stockAdded`)을 가용 재고에서 차감하여 신규 주문의 shortfall을 정확히 산정한다.

**작업 목록**
- `ApprovalService.approve()`: 가용 재고 계산식 확장
  - `producingStockAdded = sum(job.getStockAdded())` (동일 시료의 생산 중 작업 전체)
  - `available = stock − confirmedQty − producingStockAdded`

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-24 — orderSequence 중복 증가 버그 수정

**목표:** 시스템 재시작 후 주문 ID가 실제 주문 수보다 크게 시작되는 버그를 수정한다.

**작업 목록**
- `OrderRepository.restoreFromDb(Order)` 추가 — `save()`와 달리 `sequence`를 변경하지 않음
- `JsonDataStore.load()`: `orderRepository.save(order)` → `orderRepository.restoreFromDb(order)` 교체
  - 기존 `save()` 호출은 DB 로드 시마다 `sequence`를 증가시켜 재시작 후 주문 ID가 잘못 발급되는 문제 야기

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-25 — 전체 메뉴 테이블 `|` 구분자 형식 통일

**목표:** 모든 메뉴의 테이블 출력을 `SampleMenuHandler`와 동일한 `|` 컬럼 구분자 + `+---+` 경계선 형식으로 통일한다.

**작업 목록**
- `OrderMenuHandler`: `ORDER_SEP`, `ORDER_HEADER_FMT`, `ORDER_ROW_FMT` 상수 정의; `printHeader()`·`printRow()` 수정 (각 행 하단 구분선 포함)
- `ApprovalMenuHandler`: `APPROVAL_SEP`, `APPROVAL_HEADER_FMT`, `APPROVAL_ROW_FMT` 상수 정의; `listReserved()`·`listRejected()` 수정
- `MonitoringMenuHandler`: `OS_SEP`/`OS_HEADER_FMT`/`OS_ROW_FMT` (주문량 확인), `STOCK_SEP`/`STOCK_HEADER_FMT`/`STOCK_ROW_FMT` (재고량 확인) 추가; ANSI 색상 유지
- `ReleaseMenuHandler`: `RELEASE_SEP`, `RELEASE_HEADER_FMT`, `RELEASE_ROW_FMT` 상수 정의; `listConfirmed()` 수정
- `ProductionMenuHandler`: `CURRENT_SEP`/`CURRENT_HEADER_FMT`/`CURRENT_ROW_FMT` (현황), `WAITING_SEP`/`WAITING_HEADER_FMT`/`WAITING_ROW_FMT` (대기 큐) 추가

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-26 — 승인·거절·출고 실행 전 관련 주문 목록 먼저 출력

**목표:** 주문 ID 입력 전에 처리 대상 주문 목록을 먼저 출력하여 담당자가 주문 ID를 확인한 후 입력할 수 있도록 한다.

**작업 목록**
- `ApprovalMenuHandler.approve()`: `RESERVED` 주문 목록 테이블 출력 후 주문 ID 입력 요청
- `ApprovalMenuHandler.reject()`: `RESERVED` 주문 목록 테이블 출력 후 주문 ID 입력 요청
- `ReleaseMenuHandler.release()`: `CONFIRMED` 주문 목록 테이블 출력 후 주문 ID 입력 요청; 출고 대기 주문 없으면 오류 출력 후 즉시 반환

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-27 — 주문 등록 전 시료 목록 먼저 출력

**목표:** 주문 등록 시 등록된 시료가 없으면 즉시 오류 출력 후 메뉴 복귀, 있으면 시료 목록을 출력한 후 시료 ID 입력을 받는다.

**작업 목록**
- `OrderMenuHandler`: `Sample` import 추가
- `OrderMenuHandler.placeOrder()`: `SampleService.findAll()` 호출 → 비어 있으면 오류 출력 후 반환; 있으면 `SAMPLE_SEP`·`SAMPLE_HEADER_FMT`·`SAMPLE_ROW_FMT` 상수 정의 후 시료 목록 테이블 출력 → 이후 기존 입력 흐름

**검증 기준**
- `./gradlew build` 성공

---

### Phase 9-29 — Corner Case 단위 테스트 보강 (90 → 157개)

**목표:** 기존 단위 테스트에서 커버하지 않은 경계 조건·복원 경로·예외 흐름을 추가하여 테스트 커버리지를 높인다.

**작업 목록**
- `ProductionQueueTest` 신규 작성 (22개): 빈 큐 즉시 시작, 순차 처리, `startNext()`, `getWaiting()` 등
- `ProductionJobTest` 보완: `restore()`, `isActive()`, `addStock()`, `getRemainingShortfall()` 경계값
- `OrderTest` 보완: 잘못된 상태 전이 예외, `createdAt` 불변성 등
- `SampleTest` 보완: 재고·수율 경계값 추가
- `OrderRepositoryTest` 보완: `restoreFromDb()` 시퀀스 미증가 검증
- `ApprovalServiceTest` 보완: 가용 재고 계산 시나리오(CONFIRMED + PRODUCING 복합)
- `ProductionServiceTest` 보완: 비PRODUCING 상태 완료 시도 예외
- `SampleServiceTest` 보완: 중복 등록·존재하지 않는 ID 예외
- `MonitoringServiceTest` 보완: RELEASE·REJECTED 주문 제외 검증 등

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL (157개)

---

### Phase 9-30 — 테스트 메서드명 한글 → 영어 변환

**목표:** 테스트 메서드명에 포함된 한글을 영어로 통일하여 코드베이스 일관성을 확보한다.

**작업 목록**
- `MonitoringServiceTest`: 한글 포함 메서드명 5개 영어로 변환
  - `getStockStatus_재고있고대기있으면_부족` → `getStockStatus_withStockAndPendingOrders_returnsShortage`
  - `getStockStatus_부족` → `getStockStatus_shortage`
  - `getStockStatus_고갈` → `getStockStatus_depleted`
  - `getStockStatus_noPending_여유` → `getStockStatus_noPending_sufficient`
  - `getStockStatus_noPending_고갈` → `getStockStatus_noPending_depleted`

**검증 기준**
- `./gradlew test` BUILD SUCCESSFUL

---

### Phase 9-28 — 모든 입력에서 빈 값 입력 시 이전 메뉴 복귀

**목표:** 어떤 입력 단계에서든 빈 값(Enter)을 입력하면 `[안내] 입력이 없어 이전 메뉴로 돌아갑니다.` 메시지를 출력하고 해당 메서드를 즉시 반환한다.

**작업 목록**
- `SampleMenuHandler.register()`:
  - 시료 ID, 시료명 입력 루프에 `isEmpty()` 즉시 반환 추가
  - `readDouble()` 호출 → `readLine()` + 직접 파싱으로 교체하여 빈 값 구분 처리 (평균 생산시간, 수율)
- `SampleMenuHandler.search()`:
  - 항목 선택, ID/명 검색어, 평균 생산시간/수율 입력에 `isEmpty()` 즉시 반환 추가
  - `readDouble()` 호출 → `readLine()` + 직접 파싱으로 교체
- `OrderMenuHandler.placeOrder()`:
  - 시료 ID, 고객명 입력 루프에 `isEmpty()` 즉시 반환 추가
  - `readInt()` 호출 → `readLine()` + `Integer.parseInt()` 파싱으로 교체하여 빈 값 구분 처리
- `ApprovalMenuHandler.approve()` / `reject()`: 주문 ID `isEmpty()` 즉시 반환 추가
- `ReleaseMenuHandler.release()`: 주문 ID `isEmpty()` 즉시 반환 추가

**검증 기준**
- `./gradlew build` 성공

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
| 9-10 | 시료명·고객명 공백 차단 | `SampleMenuHandler`, `OrderMenuHandler` `isBlank()` 검증 |
| 9-11 | 모니터링 ANSI 색상 표시 | `MonitoringMenuHandler` 상태별·재고별 ANSI 글자색 |
| 9-12 | 메인 메뉴 순서 변경 | 5↔6 스왑: 생산 라인(5) → 출고 처리(6) |
| 9-13 | 모니터링 주문량 시료 ID 표시 | `showOrdersByStatus()` 시료명 옆 `(S-XXX)` 추가 |
| 9-15 | 생산 현황 상세 출력 + 진행률 | `ProductionJob.startedAt` 승인 시점 기록, 현황 9컬럼 출력, 진행률(%) 실시간 계산 |
| 9-16 | 진행률 정밀 계산 + 재고 자동 완료 | 밀리초 기반 진행률 수정, 100% 도달 시 생산 자동 완료·재고 즉시 반영 |
| 9-17 | 생산 큐 순차 처리 | `enqueuedAt`/`startedAt` 분리, 빈 큐 즉시 시작, 대기 후 순차 처리, `startNext()` |
| 9-18 | 재고 차감 시점 출고로 이동 | 승인 시 재고 불변, 가용=stock−CONFIRMED, 출고 시 `decreaseStock` |
| 9-19 | 재고 상태 기준 단순화 | 고갈=stock==0, 부족=stock>0&대기>0, 여유=stock>0&대기없음 |
| 9-20 | 생산 완료 백그라운드 자동 처리 | `ProductionScheduler` 1초 주기 데몬 스레드, UI 독립 자동 완료 |
| 9-21 | 대기 주문 시료ID·주문량 표시 | `printJobRow()` 시료ID·주문량 컬럼 추가 |
| 9-22 | 재고 단위별 점진 업데이트 | `ProductionJob.stockAdded`, 스케줄러 msPerUnit 기반 1개씩 증가, `complete()`에서 `increaseStock` 제거 |
| 9-23 | 가용 재고 계산 버그 수정 | `available = stock − confirmedQty − producingStockAdded` |
| 9-24 | orderSequence 중복 증가 버그 수정 | `OrderRepository.restoreFromDb()`, `JsonDataStore.load()` 교체 |
| 9-25 | 전체 테이블 `\|` 구분자 형식 통일 | 모든 `*MenuHandler` 테이블을 `+---+`/`\| col \|` 형식으로 통일 |
| 9-26 | 승인·거절·출고 전 목록 먼저 출력 | `approve()`, `reject()`, `release()` 각각 관련 주문 목록 출력 후 ID 입력 |
| 9-27 | 주문 등록 전 시료 목록 먼저 출력 | `placeOrder()` 시료 없으면 오류, 있으면 목록 출력 후 ID 입력 |
| 9-28 | 빈 입력 시 이전 메뉴 복귀 | 모든 입력에서 빈 값 → `[안내]` 출력 후 즉시 반환 |
| 9-29 | Corner Case 단위 테스트 보강 | 90 → 157개; `ProductionQueueTest` 신규, 각 레이어 경계 조건 추가 |
| 9-30 | 테스트 메서드명 영어화 | `MonitoringServiceTest` 한글 메서드명 5개 → 영어 변환 |
| 10 | 통합 검증 | `IntegrationTest`, 전체 `./gradlew test` |
