# Phase 2 설계 문서 — Sample 도메인 구현

**대상 Phase:** Phase 2  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.2

---

## 1. 목표

시료(Sample) 엔티티와 인메모리 저장소를 구현한다.  
유효성 검사 규칙을 엔티티 생성 시점에 적용하여 잘못된 상태의 객체가 만들어지지 않도록 한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   ├── domain/
│   │   └── Sample.java
│   └── repository/
│       └── SampleRepository.java
└── test/java/org/example/
    ├── domain/
    │   └── SampleTest.java
    └── repository/
        └── SampleRepositoryTest.java
```

---

## 3. 구현 명세

### 3.1 `Sample`

**패키지:** `org.example.domain`

**필드**

| 필드명 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 시료 ID (`S-XXX`, 외부에서 주입) |
| `name` | `String` | 시료명 |
| `avgProductionTime` | `int` | 평균 생산시간 (min/ea, 1 이상) |
| `yield` | `double` | 수율 (0 초과 1 이하) |
| `stock` | `int` | 현재 재고 수량 (0 이상) |

**생성자**

```
Sample(String id, String name, int avgProductionTime, double yield, int stock)
```

생성 시점 유효성 검사:

| 조건 위반 | 예외 |
|---|---|
| `yield <= 0` 또는 `yield > 1` | `IllegalArgumentException("수율은 0 초과 1 이하여야 합니다.")` |
| `stock < 0` | `IllegalArgumentException("재고 수량은 0 이상이어야 합니다.")` |
| `avgProductionTime < 1` | `IllegalArgumentException("평균 생산시간은 1 이상이어야 합니다.")` |

**메서드**

| 메서드 | 설명 |
|---|---|
| `decreaseStock(int amount)` | 재고 차감. `amount > stock` 이면 `IllegalStateException` |
| `increaseStock(int amount)` | 재고 증가. `amount < 1` 이면 `IllegalArgumentException` |
| getter 전체 | 모든 필드에 대한 getter (setter 없음 — 불변 필드는 생성자로만 설정) |

---

### 3.2 `SampleRepository`

**패키지:** `org.example.repository`

내부적으로 `Map<String, Sample>`을 사용하는 인메모리 저장소.

**메서드**

| 메서드 | 반환 타입 | 설명 |
|---|---|---|
| `save(Sample sample)` | `void` | 저장. 동일 ID가 이미 존재하면 `IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id)` |
| `findById(String id)` | `Optional<Sample>` | ID로 단건 조회 |
| `findAll()` | `List<Sample>` | 전체 목록 반환 (등록 순서 유지) |
| `findByNameContaining(String keyword)` | `List<Sample>` | 시료명에 keyword가 포함된 목록 반환 |

> `findAll()`, `findByNameContaining()`은 수정 불가능한 리스트(`List.copyOf`) 또는 새 리스트로 반환하여 내부 상태를 보호한다.

---

## 4. 테스트 명세

### 4.1 `SampleTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `create_success` | 정상 입력으로 Sample 생성 → 각 필드 getter 반환값 확인 |
| 2 | `yield_zero_throws` | yield = 0.0 → `IllegalArgumentException` |
| 3 | `yield_negative_throws` | yield = -0.1 → `IllegalArgumentException` |
| 4 | `yield_over_one_throws` | yield = 1.01 → `IllegalArgumentException` |
| 5 | `yield_boundary_min` | yield = 0.01 → 정상 생성 |
| 6 | `yield_boundary_max` | yield = 1.0 → 정상 생성 |
| 7 | `negative_stock_throws` | stock = -1 → `IllegalArgumentException` |
| 8 | `zero_stock_success` | stock = 0 → 정상 생성 |
| 9 | `avgProductionTime_zero_throws` | avgProductionTime = 0 → `IllegalArgumentException` |
| 10 | `decreaseStock_success` | stock 10 → decreaseStock(3) → stock 7 |
| 11 | `decreaseStock_insufficient_throws` | stock 2 → decreaseStock(3) → `IllegalStateException` |
| 12 | `increaseStock_success` | stock 5 → increaseStock(4) → stock 9 |

### 4.2 `SampleRepositoryTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `save_and_findById` | 저장 후 동일 ID로 조회 → 동일 객체 반환 |
| 2 | `findById_notFound` | 없는 ID → `Optional.empty()` |
| 3 | `save_duplicateId_throws` | 동일 ID 두 번 저장 → `IllegalArgumentException` |
| 4 | `findAll_empty` | 저장 전 → 빈 리스트 |
| 5 | `findAll_returnsAll` | 3개 저장 → 3개 반환, 등록 순서 유지 |
| 6 | `findByNameContaining_match` | "Alpha" 저장 후 keyword "lph" → 반환 |
| 7 | `findByNameContaining_noMatch` | keyword 없음 → 빈 리스트 |
| 8 | `findByNameContaining_multiMatch` | "AlphaX", "AlphaY" 저장 후 keyword "Alpha" → 2개 반환 |

---

## 5. 완료 조건

- [ ] `Sample` 구현 및 `SampleTest` 12개 통과
- [ ] `SampleRepository` 구현 및 `SampleRepositoryTest` 8개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL
