# Phase 4 설계 문서 — SampleService

**대상 Phase:** Phase 4  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.2

---

## 1. 목표

시료 관리 비즈니스 로직을 담당하는 서비스 레이어를 구현한다.  
`SampleRepository`를 주입받아 등록·조회·검색 기능을 제공하며,  
유효성 검사와 중복 ID 확인을 서비스 레이어에서 한 번 더 명시적으로 처리한다.

---

## 2. 파일 구조

```
src/
├── main/java/org/example/
│   └── service/
│       └── SampleService.java
└── test/java/org/example/
    └── service/
        └── SampleServiceTest.java
```

---

## 3. 구현 명세

### 3.1 `SampleService`

**패키지:** `org.example.service`

**생성자**

```
SampleService(SampleRepository sampleRepository)
```

**메서드**

#### `register`

```
Sample register(String id, String name, int avgProductionTime, double yield, int initialStock)
```

| 단계 | 처리 내용 |
|---|---|
| 1 | `sampleRepository.findById(id)`로 중복 확인 → 존재하면 `IllegalArgumentException("이미 존재하는 시료 ID입니다: " + id)` |
| 2 | `new Sample(id, name, avgProductionTime, yield, initialStock)` 생성 (내부 유효성 검사 위임) |
| 3 | `sampleRepository.save(sample)` 저장 |
| 4 | 생성된 `Sample` 반환 |

---

#### `findAll`

```
List<Sample> findAll()
```

- `sampleRepository.findAll()` 결과를 그대로 반환

---

#### `search`

```
List<Sample> search(String keyword)
```

- `sampleRepository.findByNameContaining(keyword)` 결과를 그대로 반환

---

#### `findById`

```
Sample findById(String id)
```

- `sampleRepository.findById(id)` → `Optional` 이 비어있으면 `IllegalArgumentException("존재하지 않는 시료 ID입니다: " + id)`
- 존재하면 `Sample` 반환

---

## 4. 테스트 명세

### `SampleServiceTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `register_success` | 정상 등록 → 반환된 Sample의 ID·이름·수율·재고 확인 |
| 2 | `register_duplicateId_throws` | 동일 ID 두 번 등록 → `IllegalArgumentException` |
| 3 | `register_invalidYield_throws` | yield = 0 → `IllegalArgumentException` (Sample 생성자 위임 확인) |
| 4 | `register_negativeStock_throws` | stock = -1 → `IllegalArgumentException` (Sample 생성자 위임 확인) |
| 5 | `findAll_empty` | 등록 없음 → 빈 리스트 |
| 6 | `findAll_returnsRegistered` | 2개 등록 후 `findAll()` → 2개 반환 |
| 7 | `search_match` | "Alpha" 등록 후 keyword "lph" → 1개 반환 |
| 8 | `search_noMatch` | keyword 없음 → 빈 리스트 |
| 9 | `findById_success` | 등록 후 동일 ID 조회 → Sample 반환 |
| 10 | `findById_notFound_throws` | 없는 ID 조회 → `IllegalArgumentException` |

---

## 5. 완료 조건

- [ ] `SampleService` 구현 및 `SampleServiceTest` 10개 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (누적 51개 이상)
