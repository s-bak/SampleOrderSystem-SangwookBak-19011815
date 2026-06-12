# Phase 7 설계 문서 — ProductionLine (생산 라인)

**대상 Phase:** Phase 7  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.6

---

## 1. 목표

생산량·생산시간 계산 로직을 담는 `ProductionJob`을 도입하고,  
Phase 6에서 최소 구조로 만들었던 `ProductionQueue`를 `Queue<ProductionJob>` 기반으로 확장한다.  
`ProductionService`를 구현하여 생산 완료 처리(재고 반영 + CONFIRMED 전이)를 지원한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   ├── domain/
│   │   ├── ProductionJob.java          # 신규
│   │   └── ProductionQueue.java        # 확장 (Queue<Order> → Queue<ProductionJob>)
│   └── service/
│       └── ProductionService.java      # 신규
└── test/java/org/example/
    ├── domain/
    │   └── ProductionJobTest.java      # 신규
    └── service/
        └── ProductionServiceTest.java  # 신규
```

---

## 3. 구현 명세

### 3.1 `ProductionJob`

**패키지:** `org.example.domain`

**생성자**

```
ProductionJob(Order order, int shortfall)
```

- `shortfall`: 승인 시점의 `order.getQuantity() - sample.getStock()` (부족분, 1 이상)
- `shortfall < 1` → `IllegalArgumentException("부족분은 1 이상이어야 합니다.")`

**계산 필드** (생성자에서 산출, 이후 불변)

```
actualProductionCount = (int) Math.ceil(shortfall / (sample.getYield() * 0.9))
totalProductionTime   = sample.getAvgProductionTime() * actualProductionCount
```

**메서드**

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `getOrder()` | `Order` | 주문 참조 |
| `getActualProductionCount()` | `int` | 실 생산량 |
| `getTotalProductionTime()` | `int` | 총 생산 시간 (min) |

---

### 3.2 `ProductionQueue` 확장

**패키지:** `org.example.domain`

내부 자료구조를 `Queue<Order>` → `Queue<ProductionJob>`으로 교체한다.  
`ApprovalServiceTest`에서 사용하는 `contains(orderId)` 시그니처는 유지한다.

**변경되는 메서드**

| 메서드 | 변경 내용 |
|---|---|
| `enqueue(Order order)` | 승인 시점의 shortfall을 계산하여 `ProductionJob` 생성 후 큐에 추가 |
| `contains(String orderId)` | `job.getOrder().getOrderId()`로 탐색하도록 수정 |

**추가되는 메서드**

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `peek()` | `Optional<ProductionJob>` | 큐의 첫 번째 작업 (현재 생산 중) 반환. 비어있으면 `Optional.empty()` |
| `getWaiting()` | `List<ProductionJob>` | 첫 번째를 제외한 나머지 대기 작업 목록 |
| `remove(String orderId)` | `boolean` | 해당 주문 ID의 작업을 큐에서 제거. 존재하지 않으면 `false` |

> `enqueue`의 shortfall 계산: `order.getQuantity() - order.getSample().getStock()`  
> 이 값은 승인 시점(`PRODUCING` 전이 직전)에 측정된 부족분이다.

---

### 3.3 `ProductionService`

**패키지:** `org.example.service`

**생성자**

```
ProductionService(OrderRepository orderRepository, ProductionQueue productionQueue)
```

---

#### `complete`

```
Order complete(String orderId)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `productionQueue`에서 orderId로 `ProductionJob` 탐색 → 없으면 `IllegalArgumentException("생산 큐에 존재하지 않는 주문 ID입니다: " + orderId)` |
| 2 | `order.getStatus() != PRODUCING` → `order.transitionTo(CONFIRMED)` 내부에서 `InvalidOrderStateTransitionException` |
| 3 | `productionQueue.remove(orderId)` |
| 4 | `order.getSample().increaseStock(job.getActualProductionCount())` |
| 5 | `order.transitionTo(OrderStatus.CONFIRMED)` |
| 6 | 변경된 `Order` 반환 |

> 탐색을 위해 `ProductionQueue`에 `findJobByOrderId(String orderId)` 내부 헬퍼 또는 `peek()` + `getWaiting()`을 활용한다.  
> 구현 편의를 위해 `ProductionQueue`에 `Optional<ProductionJob> findByOrderId(String orderId)` 메서드를 추가한다.

---

## 4. 테스트 명세

### 4.1 `ProductionJobTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `calculate_actualCount` | shortfall=5, yield=0.8 → `ceil(5 / (0.8×0.9)) = ceil(6.94) = 7` |
| 2 | `calculate_actualCount_exact` | shortfall=9, yield=1.0 → `ceil(9 / 0.9) = 10` |
| 3 | `calculate_totalTime` | actualCount=7, avgTime=30 → totalTime=210 |
| 4 | `shortfall_zero_throws` | shortfall=0 → `IllegalArgumentException` |
| 5 | `shortfall_negative_throws` | shortfall=-1 → `IllegalArgumentException` |

### 4.2 `ProductionServiceTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `complete_success` | PRODUCING 주문 완료 → 상태 `CONFIRMED`, 재고 `actualProductionCount`만큼 증가 |
| 2 | `complete_stockIncreased` | stock=2, shortfall=8, yield=1.0 → actualCount=9 → 완료 후 stock=11 |
| 3 | `complete_removedFromQueue` | 완료 후 큐에서 제거됨 확인 (`productionQueue.size() == 0`) |
| 4 | `complete_notInQueue_throws` | 큐에 없는 주문 ID → `IllegalArgumentException` |
| 5 | `complete_notProducing_throws` | RESERVED 상태 주문 직접 완료 시도 → `InvalidOrderStateTransitionException` |

---

## 5. 완료 조건

- [ ] `ProductionJob` 구현 및 `ProductionJobTest` 5개 통과
- [ ] `ProductionQueue` 확장 및 기존 `ApprovalServiceTest` 69개 전원 통과 유지
- [ ] `ProductionService` 구현 및 `ProductionServiceTest` 5개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 79개 이상)
