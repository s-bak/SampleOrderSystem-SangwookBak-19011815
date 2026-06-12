# Phase 3 설계 문서 — Order 도메인 및 상태 전이

**대상 Phase:** Phase 3  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §3, §4.3

---

## 1. 목표

주문 엔티티, 주문 상태 열거형, 상태 전이 규칙, 인메모리 저장소를 구현한다.  
허용되지 않는 상태 전이를 시도하면 `InvalidOrderStateTransitionException`을 발생시킨다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   ├── domain/
│   │   ├── Order.java
│   │   └── OrderStatus.java
│   └── repository/
│       └── OrderRepository.java
└── test/java/org/example/
    ├── domain/
    │   └── OrderTest.java
    └── repository/
        └── OrderRepositoryTest.java
```

---

## 3. 구현 명세

### 3.1 `OrderStatus`

**패키지:** `org.example.domain`

```
enum OrderStatus {
    RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASE
}
```

---

### 3.2 `Order`

**패키지:** `org.example.domain`

**필드**

| 필드명 | 타입 | 설명 |
|---|---|---|
| `orderId` | `String` | 주문 ID (`O-XXX`, 외부에서 주입) |
| `customerName` | `String` | 고객명 |
| `sample` | `Sample` | 주문 대상 시료 참조 |
| `quantity` | `int` | 주문 수량 (1 이상) |
| `status` | `OrderStatus` | 현재 주문 상태 |
| `createdAt` | `LocalDateTime` | 등록 일시 (생성 시 자동 설정) |

**생성자**

```
Order(String orderId, String customerName, Sample sample, int quantity)
```

- 초기 상태: `RESERVED`
- `quantity < 1` → `IllegalArgumentException("주문 수량은 1 이상이어야 합니다.")`

**메서드**

| 메서드 | 설명 |
|---|---|
| `transitionTo(OrderStatus next)` | 허용 전이 검증 후 상태 변경. 불허 시 `InvalidOrderStateTransitionException` |
| getter 전체 | 모든 필드에 대한 getter (status 포함) |

**허용 전이 매트릭스**

| 현재 \ 다음 | REJECTED | PRODUCING | CONFIRMED | RELEASE |
|:---:|:---:|:---:|:---:|:---:|
| `RESERVED` | O | O | O | - |
| `PRODUCING` | - | - | O | - |
| `CONFIRMED` | - | - | - | O |
| `REJECTED` | - | - | - | - |
| `RELEASE` | - | - | - | - |

예외 메시지 형식:
```
"[현재상태] 상태에서 [다음상태] 상태로 전이할 수 없습니다."
```

---

### 3.3 `OrderRepository`

**패키지:** `org.example.repository`

내부적으로 `LinkedHashMap<String, Order>`을 사용하는 인메모리 저장소.  
주문 ID(`O-XXX`)는 저장소가 순번을 관리하여 `save` 시점에 발급한다.

**메서드**

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `save(Order order)` | `void` | 저장. 동일 ID 존재 시 `IllegalArgumentException` |
| `findById(String orderId)` | `Optional<Order>` | ID로 단건 조회 |
| `findAll()` | `List<Order>` | 전체 목록 (등록 순서 유지) |
| `findByStatus(OrderStatus status)` | `List<Order>` | 상태별 필터링된 목록 |
| `generateNextId()` | `String` | 다음 주문 ID 발급 (`O-001`, `O-002`, ...) |

> ID 발급 규칙: 저장된 주문 수 + 1을 세 자리 숫자로 포맷 (`String.format("O-%03d", ...)`)

---

## 4. 테스트 명세

### 4.1 `OrderTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `create_success` | 정상 생성 → 초기 상태 `RESERVED`, 각 필드 확인 |
| 2 | `create_invalidQuantity_throws` | quantity = 0 → `IllegalArgumentException` |
| 3 | `create_negativeQuantity_throws` | quantity = -1 → `IllegalArgumentException` |
| 4 | `transition_reserved_to_rejected` | `RESERVED` → `REJECTED` 정상 전이 |
| 5 | `transition_reserved_to_producing` | `RESERVED` → `PRODUCING` 정상 전이 |
| 6 | `transition_reserved_to_confirmed` | `RESERVED` → `CONFIRMED` 정상 전이 |
| 7 | `transition_producing_to_confirmed` | `PRODUCING` → `CONFIRMED` 정상 전이 |
| 8 | `transition_confirmed_to_release` | `CONFIRMED` → `RELEASE` 정상 전이 |
| 9 | `transition_reserved_to_release_throws` | `RESERVED` → `RELEASE` → `InvalidOrderStateTransitionException` |
| 10 | `transition_rejected_throws` | `REJECTED` → 어떤 상태로든 전이 → `InvalidOrderStateTransitionException` |
| 11 | `transition_release_throws` | `RELEASE` → 어떤 상태로든 전이 → `InvalidOrderStateTransitionException` |
| 12 | `transition_producing_to_rejected_throws` | `PRODUCING` → `REJECTED` → `InvalidOrderStateTransitionException` |

### 4.2 `OrderRepositoryTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `save_and_findById` | 저장 후 동일 ID로 조회 → 동일 객체 반환 |
| 2 | `findById_notFound` | 없는 ID → `Optional.empty()` |
| 3 | `findAll_empty` | 저장 전 → 빈 리스트 |
| 4 | `findAll_returnsAll` | 3개 저장 → 3개, 등록 순서 유지 |
| 5 | `findByStatus_reserved` | RESERVED 2개, CONFIRMED 1개 저장 → RESERVED 조회 시 2개 반환 |
| 6 | `findByStatus_noMatch` | 해당 상태 주문 없음 → 빈 리스트 |
| 7 | `generateNextId_sequence` | 첫 번째 `O-001`, 저장 후 두 번째 `O-002` |

---

## 5. 완료 조건

- [ ] `OrderStatus` 열거형 구현
- [ ] `Order` 구현 및 `OrderTest` 12개 통과
- [ ] `OrderRepository` 구현 및 `OrderRepositoryTest` 7개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 31개 이상)
