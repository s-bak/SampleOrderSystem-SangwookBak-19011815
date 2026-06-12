# Phase 8 설계 문서 — ReleaseService + MonitoringService

**대상 Phase:** Phase 8  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.5, §4.7

---

## 1. 목표

출고 처리 서비스(`ReleaseService`)와 현황 조회 서비스(`MonitoringService`)를 구현한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   └── service/
│       ├── ReleaseService.java
│       └── MonitoringService.java
└── test/java/org/example/
    └── service/
        ├── ReleaseServiceTest.java
        └── MonitoringServiceTest.java
```

---

## 3. 구현 명세

### 3.1 `ReleaseService`

**패키지:** `org.example.service`

**생성자**

```
ReleaseService(OrderRepository orderRepository)
```

---

#### `release`

```
Order release(String orderId)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `orderRepository.findById(orderId)` → 없으면 `IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId)` |
| 2 | `order.transitionTo(RELEASE)` (CONFIRMED가 아니면 내부에서 `InvalidOrderStateTransitionException`) |
| 3 | 변경된 `Order` 반환 |

---

### 3.2 `MonitoringService`

**패키지:** `org.example.service`

**생성자**

```
MonitoringService(SampleRepository sampleRepository, OrderRepository orderRepository)
```

---

#### `getOrdersByStatus`

```
Map<OrderStatus, List<Order>> getOrdersByStatus()
```

- `RESERVED`, `PRODUCING`, `CONFIRMED`, `RELEASE` 4개 상태에 대해 주문 목록을 상태별로 묶어 반환
- `REJECTED` 상태는 제외
- 해당 상태의 주문이 없는 경우 빈 리스트로 포함 (키 누락 없음)

---

#### `getStockStatus`

```
List<StockStatusEntry> getStockStatus()
```

시료별로 현재 재고 상태를 계산하여 반환한다.

**`StockStatusEntry`** — `MonitoringService` 내부 정적 클래스 (또는 별도 파일)

| 필드 | 타입 | 설명 |
|---|---|---|
| `sample` | `Sample` | 시료 참조 |
| `status` | `String` | `"여유"` / `"부족"` / `"고갈"` |
| `pendingQuantity` | `int` | RESERVED + PRODUCING 상태 주문의 총 요청 수량 합계 |

**재고 상태 판정 기준**

| 상태 | 조건 |
|---|---|
| `여유` | `sample.getStock() >= pendingQuantity` |
| `부족` | `sample.getStock() > 0` && `sample.getStock() < pendingQuantity` |
| `고갈` | `sample.getStock() == 0` |

> `pendingQuantity = 0`이고 `stock > 0`이면 `여유`로 판정한다.  
> `pendingQuantity = 0`이고 `stock == 0`이면 `고갈`로 판정한다.

---

## 4. 테스트 명세

### 4.1 `ReleaseServiceTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `release_success` | CONFIRMED → 상태 `RELEASE` |
| 2 | `release_notConfirmed_throws` | RESERVED 상태 주문 출고 시도 → `InvalidOrderStateTransitionException` |
| 3 | `release_orderNotFound_throws` | 없는 주문 ID → `IllegalArgumentException` |

### 4.2 `MonitoringServiceTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `getOrdersByStatus_allEmpty` | 주문 없음 → 4개 키 모두 빈 리스트 |
| 2 | `getOrdersByStatus_groupsCorrectly` | RESERVED 2건, CONFIRMED 1건 → 각 키에 올바른 수 |
| 3 | `getOrdersByStatus_excludesRejected` | REJECTED 주문 → 결과에 포함되지 않음 |
| 4 | `getStockStatus_여유` | stock=10, pendingQty=5 → `"여유"` |
| 5 | `getStockStatus_부족` | stock=3, pendingQty=5 → `"부족"` |
| 6 | `getStockStatus_고갈` | stock=0 → `"고갈"` |
| 7 | `getStockStatus_noPending_여유` | stock=5, 주문 없음 → `"여유"` |
| 8 | `getStockStatus_noPending_고갈` | stock=0, 주문 없음 → `"고갈"` |

---

## 5. 완료 조건

- [ ] `ReleaseService` 구현 및 `ReleaseServiceTest` 3개 통과
- [ ] `MonitoringService` + `StockStatusEntry` 구현 및 `MonitoringServiceTest` 8개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 90개 이상)
