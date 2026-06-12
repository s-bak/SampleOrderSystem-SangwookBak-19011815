# Phase 5 설계 문서 — OrderService (주문 등록 · 조회)

**대상 Phase:** Phase 5  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.3

---

## 1. 목표

주문 접수(RESERVED 생성) 및 목록·단건 조회 서비스를 구현한다.  
`SampleRepository`로 시료 존재를 확인하고, `OrderRepository`로 주문을 저장한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   └── service/
│       └── OrderService.java
└── test/java/org/example/
    └── service/
        └── OrderServiceTest.java
```

---

## 3. 구현 명세

### 3.1 `OrderService`

**패키지:** `org.example.service`

**생성자**

```
OrderService(SampleRepository sampleRepository, OrderRepository orderRepository)
```

---

#### `placeOrder`

```
Order placeOrder(String sampleId, String customerName, int quantity)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `sampleRepository.findById(sampleId)` → 없으면 `IllegalArgumentException("존재하지 않는 시료 ID입니다: " + sampleId)` |
| 2 | `quantity < 1` → `IllegalArgumentException("주문 수량은 1 이상이어야 합니다.")` |
| 3 | `orderRepository.generateNextId()`로 주문 ID 발급 |
| 4 | `new Order(orderId, customerName, sample, quantity)` 생성 |
| 5 | `orderRepository.save(order)` 저장 |
| 6 | 생성된 `Order` 반환 |

---

#### `findAll`

```
List<Order> findAll()
```

- `orderRepository.findAll()` 결과를 그대로 반환

---

#### `findById`

```
Order findById(String orderId)
```

- `orderRepository.findById(orderId)` → `Optional`이 비어있으면 `IllegalArgumentException("존재하지 않는 주문 ID입니다: " + orderId)`
- 존재하면 `Order` 반환

---

## 4. 테스트 명세

### `OrderServiceTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `placeOrder_success` | 정상 주문 → 반환된 Order의 ID(`O-001`), 상태(`RESERVED`), 고객명, 시료, 수량 확인 |
| 2 | `placeOrder_idSequence` | 2번 주문 → 각각 `O-001`, `O-002` 발급 확인 |
| 3 | `placeOrder_sampleNotFound_throws` | 없는 시료 ID → `IllegalArgumentException` |
| 4 | `placeOrder_quantityZero_throws` | quantity = 0 → `IllegalArgumentException` |
| 5 | `placeOrder_quantityNegative_throws` | quantity = -1 → `IllegalArgumentException` |
| 6 | `findAll_empty` | 주문 없음 → 빈 리스트 |
| 7 | `findAll_returnsAll` | 2건 주문 후 `findAll()` → 2개 반환 |
| 8 | `findById_success` | 주문 후 동일 ID 조회 → Order 반환 |
| 9 | `findById_notFound_throws` | 없는 주문 ID → `IllegalArgumentException` |

---

## 5. 완료 조건

- [ ] `OrderService` 구현 및 `OrderServiceTest` 9개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 60개 이상)
