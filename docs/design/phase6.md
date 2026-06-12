# Phase 6 설계 문서 — ApprovalService (주문 승인 / 거절)

**대상 Phase:** Phase 6  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.4

---

## 1. 목표

생산 담당자의 주문 승인·거절 로직과 재고 차감을 구현한다.  
승인 시 재고 충분 여부에 따라 `CONFIRMED` 또는 `PRODUCING`으로 분기하며,  
`PRODUCING` 전이 시 `ProductionQueue`에 주문을 등록한다.  
`ProductionQueue`는 이 Phase에서 최소 구조만 도입하고, Phase 7에서 완성한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   ├── service/
│   │   └── ApprovalService.java
│   └── domain/
│       └── ProductionQueue.java        # 최소 구조 도입 (Phase 7에서 확장)
└── test/java/org/example/
    └── service/
        └── ApprovalServiceTest.java
```

---

## 3. 구현 명세

### 3.1 `ProductionQueue` (최소 구조)

**패키지:** `org.example.domain`

Phase 7에서 `ProductionJob` 및 조회 기능이 추가될 예정이므로,  
이 Phase에서는 승인 처리에 필요한 최소 기능만 구현한다.

**내부 구조:** `Queue<Order>` (LinkedList)

**메서드**

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `enqueue(Order order)` | `void` | 주문을 큐 끝에 추가 |
| `size()` | `int` | 현재 큐에 대기 중인 주문 수 |
| `contains(String orderId)` | `boolean` | 해당 주문 ID가 큐에 존재하는지 확인 |

---

### 3.2 `ApprovalService`

**패키지:** `org.example.service`

**생성자**

```
ApprovalService(OrderRepository orderRepository, ProductionQueue productionQueue)
```

---

#### `approve`

```
Order approve(String orderId)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `orderRepository.findById(orderId)` → 없으면 `IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId)` |
| 2 | `order.getStatus() != RESERVED` → `InvalidOrderStateTransitionException` (`transitionTo` 위임) |
| 3 | `sample.getStock() >= order.getQuantity()` (재고 충분) → `sample.decreaseStock(quantity)` → `order.transitionTo(CONFIRMED)` |
| 4 | 재고 부족 → `productionQueue.enqueue(order)` → `order.transitionTo(PRODUCING)` |
| 5 | 변경된 `Order` 반환 |

> 재고 충분 조건: `sample.getStock() >= order.getQuantity()`  
> 재고 부족 조건: `sample.getStock() < order.getQuantity()`

---

#### `reject`

```
Order reject(String orderId)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `orderRepository.findById(orderId)` → 없으면 `IllegalArgumentException` |
| 2 | `order.transitionTo(REJECTED)` (RESERVED가 아니면 내부에서 `InvalidOrderStateTransitionException`) |
| 3 | 변경된 `Order` 반환 |

---

## 4. 테스트 명세

### `ApprovalServiceTest`

**공통 픽스처**
- `Sample`: id=`S-001`, avgProductionTime=30, yield=0.85, stock=10
- `Order`: sampleId=`S-001`, quantity=5 (재고 충분 케이스)
- `Order`: sampleId=`S-001`, quantity=15 (재고 부족 케이스)

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `approve_stockSufficient_confirmed` | stock=10, quantity=5 → 상태 `CONFIRMED`, 재고 5로 차감 |
| 2 | `approve_stockExact_confirmed` | stock=5, quantity=5 → 상태 `CONFIRMED`, 재고 0으로 차감 |
| 3 | `approve_stockInsufficient_producing` | stock=10, quantity=15 → 상태 `PRODUCING`, 재고 차감 없음, 큐에 등록 |
| 4 | `approve_stockZero_producing` | stock=0, quantity=5 → 상태 `PRODUCING`, 큐에 등록 |
| 5 | `approve_notReserved_throws` | 이미 `CONFIRMED` 상태 주문 승인 시도 → `InvalidOrderStateTransitionException` |
| 6 | `approve_orderNotFound_throws` | 없는 주문 ID → `IllegalArgumentException` |
| 7 | `reject_success` | `RESERVED` → 상태 `REJECTED` |
| 8 | `reject_notReserved_throws` | `CONFIRMED` 상태 주문 거절 시도 → `InvalidOrderStateTransitionException` |
| 9 | `reject_orderNotFound_throws` | 없는 주문 ID → `IllegalArgumentException` |

---

## 5. 완료 조건

- [ ] `ProductionQueue` 최소 구조 구현 (`enqueue`, `size`, `contains`)
- [ ] `ApprovalService` 구현 및 `ApprovalServiceTest` 9개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 69개 이상)
