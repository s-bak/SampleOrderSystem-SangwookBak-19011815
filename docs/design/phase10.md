# Phase 10 설계 문서 — UI 핸들러 단위 테스트

**대상 Phase:** Phase 10  
**기준 문서:** `docs/PRD.md` §4.1 ~ §4.7, 기존 핸들러 구현

---

## 1. 목표

Phase 9에서 구현된 6개 UI 핸들러(`SampleMenuHandler`, `OrderMenuHandler`, `ApprovalMenuHandler`, `MonitoringMenuHandler`, `ReleaseMenuHandler`, `ProductionMenuHandler`)에 대한 단위 테스트를 작성한다.  
콘솔 I/O는 `ByteArrayInputStream` / `ByteArrayOutputStream`으로 대체하고, `JsonDataStore`는 `@TempDir`로 격리된 임시 파일에 연결한다.

---

## 2. 파일 구조

```
src/test/java/org/example/
└── ui/
    ├── SampleMenuHandlerTest.java     (11개)
    ├── OrderMenuHandlerTest.java      ( 6개)
    ├── ApprovalMenuHandlerTest.java   ( 8개)
    ├── MonitoringMenuHandlerTest.java ( 4개)
    ├── ReleaseMenuHandlerTest.java    ( 4개)
    └── ProductionMenuHandlerTest.java ( 4개)
```

---

## 3. 공통 테스트 인프라

### 3.1 I/O 시뮬레이션 헬퍼

각 테스트 클래스 내부에서 아래 패턴으로 `ConsoleIO`를 구성한다.

```java
private ConsoleIO io(String inputs) {
    return new ConsoleIO(
        new ByteArrayInputStream(inputs.getBytes(StandardCharsets.UTF_8)),
        new PrintStream(out, true, StandardCharsets.UTF_8)
    );
}

private ByteArrayOutputStream out;

@BeforeEach
void initOut() { out = new ByteArrayOutputStream(); }

private String output() { return out.toString(StandardCharsets.UTF_8); }
```

### 3.2 JsonDataStore 격리

```java
@TempDir Path tempDir;

private JsonDataStore dataStore(SampleRepository sr, OrderRepository or, ProductionQueue q) {
    return new JsonDataStore(tempDir.resolve("db.json"), sr, or, q);
}
```

### 3.3 handle() 호출 패턴

모든 핸들러 테스트는 `handle()`을 통해 서브메뉴까지 내려가 테스트한다.  
입력 문자열 형식: `"{서브메뉴번호}\n{필요입력...}\n0\n"`

- 서브메뉴 번호: 해당 기능을 선택
- 필요 입력: 각 기능에 필요한 데이터
- 마지막 `0`: `handle()` while 루프 탈출

---

## 4. 테스트 명세

### 4.1 `SampleMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `SampleService`, `JsonDataStore` 인스턴스 생성

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_listAll_empty_showsMessage` | `"2\n0\n"` | 출력에 `"등록된 시료가 없습니다"` 포함 |
| 2 | `handle_listAll_withSamples_showsTable` | `"2\n0\n"` (S-001 사전 등록) | 출력에 `"S-001"`, `"AlphaChip"` 포함 |
| 3 | `handle_register_success` | `"1\nS-001\nAlphaChip\n30\n0.85\n0\n"` | 출력에 `"등록 완료"` 포함, `sampleService.findById("S-001")` 반환값 존재 |
| 4 | `handle_register_emptyId_returnsToMenu` | `"1\n\n0\n"` | 출력에 `"[안내]"` 포함 |
| 5 | `handle_register_duplicateId_retries` | `"1\nS-001\nS-002\nBetaChip\n20.0\n0.90\n0\n"` (S-001 사전 등록) | 출력에 `"이미 존재하는"` 포함, S-002 등록 완료 메시지 포함 |
| 6 | `handle_register_emptyName_returnsToMenu` | `"1\nS-001\n\n0\n"` | 출력에 `"[안내]"` 포함 |
| 7 | `handle_register_zeroAvgTime_showsError` | `"1\nS-001\nAlphaChip\n0\n0\n"` | 출력에 `"[오류]"` 포함, S-001 미등록 확인 |
| 8 | `handle_register_invalidYield_showsError` | `"1\nS-001\nAlphaChip\n30\n0\n0\n"` | 출력에 `"[오류]"` 포함, S-001 미등록 확인 |
| 9 | `handle_search_byId_found` | `"3\n1\n-001\n0\n"` (S-001 사전 등록) | 출력에 `"S-001"` 포함 |
| 10 | `handle_search_noResult_showsMessage` | `"3\n1\nXXX\n0\n"` (S-001 사전 등록) | 출력에 `"검색 결과가 없습니다"` 포함 |
| 11 | `handle_search_emptyKeyword_returnsToMenu` | `"3\n1\n\n0\n"` | 출력에 `"[안내]"` 포함 |

---

### 4.2 `OrderMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `OrderRepository`, `SampleService`, `OrderService`, `JsonDataStore`

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_placeOrder_noSamples_showsError` | `"1\n0\n"` (시료 미등록) | 출력에 `"[오류]"` 포함 |
| 2 | `handle_placeOrder_success` | `"1\nS-001\n고객A\n5\n0\n"` (S-001 사전 등록) | 출력에 `"주문 등록 완료"`, `"O-001"` 포함 |
| 3 | `handle_placeOrder_emptyInput_returnsToMenu` | `"1\n\n0\n"` (S-001 사전 등록) | 출력에 `"[안내]"` 포함 |
| 4 | `handle_placeOrder_invalidQuantity_showsError` | `"1\nS-001\n고객A\nabc\n0\n"` (S-001 사전 등록) | 출력에 `"[오류]"` 포함 |
| 5 | `handle_listAll_empty_showsMessage` | `"2\n0\n"` | 출력에 `"주문 내역이 없습니다"` 포함 |
| 6 | `handle_listAll_withOrders_showsTable` | `"2\n0\n"` (O-001 사전 등록) | 출력에 `"O-001"`, `"고객A"` 포함 |

---

### 4.3 `ApprovalMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `OrderRepository`, `ProductionQueue`, `ApprovalService`, `JsonDataStore`

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_listReserved_empty_showsMessage` | `"1\n0\n"` | 출력에 `"대기 중인 주문이 없습니다"` 포함 |
| 2 | `handle_listRejected_empty_showsMessage` | `"2\n0\n"` | 출력에 `"거절된 주문이 없습니다"` 포함 |
| 3 | `handle_approve_noReserved_showsError` | `"3\n0\n"` | 출력에 `"[오류]"` 포함 |
| 4 | `handle_approve_emptyInput_returnsToMenu` | `"3\n\n0\n"` (O-001 RESERVED 사전 등록) | 출력에 `"[안내]"` 포함 |
| 5 | `handle_approve_stockSufficient_confirmed` | `"3\nO-001\n0\n"` (재고 10, 주문량 5) | 출력에 `"CONFIRMED"` 포함 |
| 6 | `handle_approve_stockInsufficient_producing` | `"3\nO-001\n0\n"` (재고 0, 주문량 5) | 출력에 `"PRODUCING"` 포함, `productionQueue.size() == 1` |
| 7 | `handle_reject_success` | `"4\nO-001\n0\n"` (O-001 RESERVED 사전 등록) | 출력에 `"거절 완료"` 포함 |
| 8 | `handle_reject_emptyInput_returnsToMenu` | `"4\n\n0\n"` (O-001 RESERVED 사전 등록) | 출력에 `"[안내]"` 포함 |

---

### 4.4 `MonitoringMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `OrderRepository`, `MonitoringService`

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_showOrdersByStatus_empty_showsZeroCounts` | `"1\n0\n"` | 출력에 `"RESERVED"`, `"0건"` 포함 |
| 2 | `handle_showOrdersByStatus_withOrders_showsData` | `"1\n0\n"` (RESERVED 1건 사전 등록) | 출력에 `"1건"` 포함 |
| 3 | `handle_showStockStatus_empty_showsMessage` | `"2\n0\n"` | 출력에 `"등록된 시료가 없습니다"` 포함 |
| 4 | `handle_showStockStatus_withEntries_showsTable` | `"2\n0\n"` (S-001 재고 10, 대기 없음) | 출력에 `"S-001"`, `"여유"` 포함 |

---

### 4.5 `ReleaseMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `OrderRepository`, `ReleaseService`, `JsonDataStore`

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_listConfirmed_empty_showsMessage` | `"1\n0\n"` | 출력에 `"출고 대기 중인 주문이 없습니다"` 포함 |
| 2 | `handle_release_noConfirmed_showsError` | `"2\n0\n"` | 출력에 `"[오류]"` 포함 |
| 3 | `handle_release_emptyInput_returnsToMenu` | `"2\n\n0\n"` (O-001 CONFIRMED 사전 등록) | 출력에 `"[안내]"` 포함 |
| 4 | `handle_release_success` | `"2\nO-001\n0\n"` (O-001 CONFIRMED 사전 등록) | 출력에 `"출고 완료"` 포함 |

> **CONFIRMED 사전 등록 방법:** `Order.restore(...)` + `orderRepository.restoreFromDb()`로 CONFIRMED 상태 주문을 직접 주입한다.

---

### 4.6 `ProductionMenuHandlerTest`

**공통 setUp:** `SampleRepository`, `OrderRepository`, `ProductionQueue`

| # | 테스트명 | 입력 | 검증 내용 |
|---|---|---|---|
| 1 | `handle_showCurrent_noActiveJob_showsMessage` | `"1\n0\n"` | 출력에 `"생산 중인 작업이 없습니다"` 포함 |
| 2 | `handle_showCurrent_withActiveJob_showsDetails` | `"1\n0\n"` (`queue.enqueue(order, 5)` 사전 호출) | 출력에 주문 ID, 시료명, `"진행률"` 관련 컬럼 포함 |
| 3 | `handle_showWaiting_empty_showsMessage` | `"2\n0\n"` | 출력에 `"대기 중인 작업이 없습니다"` 포함 |
| 4 | `handle_showWaiting_withJobs_showsTable` | `"2\n0\n"` (큐에 2건 등록, 1번은 active, 2번은 대기) | 출력에 대기 주문 ID 포함 |

> **활성 작업 생성:** `queue.enqueue(order, shortfall)` 호출 시 큐가 비어 있으면 즉시 `job.start()`가 호출되므로 `peek().get().isActive() == true`가 보장된다.  
> **대기 작업 생성:** 두 번째 `queue.enqueue(order2, shortfall2)` 호출 시 `getWaiting()` 에 1건이 존재한다.

---

## 5. 완료 조건

- [ ] `SampleMenuHandlerTest` 11개 통과
- [ ] `OrderMenuHandlerTest` 6개 통과
- [ ] `ApprovalMenuHandlerTest` 8개 통과
- [ ] `MonitoringMenuHandlerTest` 4개 통과
- [ ] `ReleaseMenuHandlerTest` 4개 통과
- [ ] `ProductionMenuHandlerTest` 4개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (기존 90개 + 신규 37개 = 127개 이상)
