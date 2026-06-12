# CLAUDE.md — SampleOrderSystem (SBak)

## 프로젝트 개요

**S-Semi 반도체 시료 생산주문관리 시스템**  
Java 17+ / Gradle / JUnit 5 기반 콘솔 애플리케이션.  
상세 요구사항은 `docs/PRD.md` 참조.

---

## 기술 스택

- **언어:** Java 17+
- **빌드:** Gradle (`./gradlew build`)
- **테스트:** JUnit 5 (`./gradlew test`)
- **실행:** 콘솔(CLI) 기반

---

## 도메인 개념

### 주요 엔티티

| 엔티티 | 설명 |
|---|---|
| `Sample` | 시료. ID(`S-XXX`), 시료명, 평균 생산시간(`double`, 0 초과), 수율(0 < yield ≤ 1), 재고 수량 |
| `Order` | 주문. 주문 ID, 고객명, 시료 참조, 주문 수량, 상태 |
| `ProductionJob` | 생산 작업. Order, shortfall, 실생산량, 소요시간, `enqueuedAt`, `startedAt`(null=대기), `stockAdded`(지금까지 재고에 추가된 수량) 보유 |
| `ProductionQueue` | 생산 라인. FIFO 큐; `enqueue(order, shortfall)`로 삽입; 빈 큐 삽입 시 즉시 `start()`, 선행 작업 있으면 대기; 완료 시 `startNext()`로 순차 시작 |
| `ProductionScheduler` | 백그라운드 데몬 스레드(1초 주기). `msPerUnit` 기반으로 완성 단위 수 계산, 신규 단위마다 `increaseStock(1)` + `addStock(1)`; `stockAdded >= shortfall` 시 `complete()` 호출 |

### 주문 상태 (`OrderStatus`)

```
RESERVED → REJECTED
RESERVED → PRODUCING  (승인 + 재고 부족)
RESERVED → CONFIRMED  (승인 + 재고 충분)
PRODUCING → CONFIRMED (생산 완료)
CONFIRMED → RELEASE   (출고 처리)
```

- 매트릭스에 없는 전이 시도 → `InvalidOrderStateTransitionException` 발생
- `REJECTED` 상태 주문은 모니터링에서 제외

### 재고 모델

- **가용 재고** = `stock − confirmedQty − producingStockAdded`
  - `confirmedQty`: 동일 시료의 CONFIRMED 주문 수량 합계
  - `producingStockAdded`: 동일 시료의 생산 중 작업에서 이미 재고에 추가된 수량(`stockAdded`) 합계
- 승인 시 가용 재고 ≥ 주문 수량 → 재고 불변 → `CONFIRMED`
- 승인 시 가용 재고 부족 → shortfall = 주문 수량 − max(0, 가용 재고) → 생산 큐 등록 → `PRODUCING`
- 출고(`RELEASE`) 시점에만 `decreaseStock(quantity)` 호출

### 재고 상태 (모니터링)

| 상태 | 조건 |
|---|---|
| 여유 | `stock > 0` AND `RESERVED + PRODUCING` 대기 수량 = 0 |
| 부족 | `stock > 0` AND `RESERVED + PRODUCING` 대기 수량 > 0 |
| 고갈 | `stock == 0` |

### 생산량 산정 공식

```
부족분       = 주문 수량 − max(0, 가용 재고)
실 생산량    = ceil(부족분 / (수율 × 0.9))
총 생산 시간 = 평균 생산시간 × 실 생산량  [단위: min]
```

재고는 단위별로 점진 추가: `msPerUnit = totalProductionTime × 60,000 / shortfall`, 매 완성 단위마다 `increaseStock(1)`.
`complete()`는 재고 변경 없이 상태 전이(`PRODUCING → CONFIRMED`)와 다음 작업 시작만 처리한다.

---

## 메인 메뉴 구조

1. 시료 관리 — 등록(초기 재고=0 고정) / 목록 조회 / 항목별 검색(ID·이름·평균생산시간·수율)
2. 주문 접수 — 주문 등록(등록된 시료 목록 먼저 출력, `RESERVED`) / 주문 목록 조회
3. 주문 승인/거절 — 접수 주문 목록 / 거절 주문 목록 / 승인(접수 주문 목록 출력 후 ID 입력) / 거절(동일); 대기 주문 없으면 즉시 차단
4. 모니터링 — 상태별 주문 수(RESERVED=하늘색·PRODUCING=주황색·CONFIRMED=연두색·RELEASE=연보라색, 시료명 옆 시료 ID 표시) + 시료별 재고 현황(여유=연두색·부족=주황색·고갈=빨강색)
5. 생산 라인 — 생산 현황(주문번호·시료명·시료ID·주문량·부족수량(잔여)·실생산량·소요시간·진행률) / 대기 큐(시료ID·주문량·접수시각 포함); 스케줄러가 단위별 재고 추가·자동 완료 처리
6. 출고 처리 — `CONFIRMED` 주문 목록 출력 후 ID 입력 → 재고 차감 → `RELEASE`; 대기 주문 없으면 즉시 차단

---

## 예외 처리 규칙

- 잘못된 상태 전이: `InvalidOrderStateTransitionException`
- 중복 시료 ID 등록: 오류 메시지 출력 후 ID 재입력 요청 (다른 필드는 묻지 않음)
- 잘못된 수율·재고 입력: 오류 메시지 출력 후 중단
- 존재하지 않는 ID 참조: 오류 메시지 출력
- 목록이 비었을 때: 안내 메시지 출력 후 메뉴 복귀
- **빈 값 입력**: `[안내] 입력이 없어 이전 메뉴로 돌아갑니다.` 출력 후 즉시 메뉴 복귀 (모든 입력 공통)
- **시료명·고객명 공백 입력**: `ConsoleIO.readLine()`이 `trim()`을 적용하므로 공백 전용 입력은 빈 문자열로 변환되어 위 빈 값 입력 규칙으로 처리됨 (별도 `isBlank()` 재입력 루프 없음)

---

## 빌드 및 테스트 명령

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행 (main 클래스 지정 후)
./gradlew run
```

---

## 데이터 영속성

- 실행 종료 후에도 데이터가 유지되도록 `data/db.json`에 JSON으로 저장
- `JsonDataStore`가 시작 시 load, 각 데이터 변경(시료 등록·주문 등록·승인·거절·출고·생산 완료) 성공 직후 즉시 save
- `data/db.json`은 `.gitignore`로 제외, `data/.gitkeep`만 추적
- Jackson 2.17.1(`jackson-databind`, `jackson-datatype-jsr310`) 사용
- DB 복원 시 `OrderRepository.restoreFromDb(order)` 사용 — `save()`와 달리 `sequence`를 증가시키지 않음

---

## 콘솔 출력 테이블 형식

모든 테이블은 `SampleMenuHandler`를 기준으로 통일된 형식을 따른다:

```
+--------+-----------------+-----------------+--------+--------+
| 시료ID  | 시료명           |   생산시간(min) |   수율 |   재고 |
+--------+-----------------+-----------------+--------+--------+
| S-001  | AlphaChip       |           30.0 |   0.85 |      0 |
+--------+-----------------+-----------------+--------+--------+
```

- 헤더 위·아래, 각 데이터 행 아래에 `+---+` 형식의 구분선 출력
- 컬럼 구분자는 `|`, 구분선은 컬럼 너비 + 2 길이의 `-` 사용
- 각 핸들러는 `SEP`, `HEADER_FMT`, `ROW_FMT` 3개 상수를 `private static final`로 정의

---

## 코딩 컨벤션

- 패키지 루트: `org.example`
- 엔티티, 서비스, 예외, UI 레이어를 분리하여 구성
- 콘솔 I/O는 별도 클래스로 분리 (테스트 용이성)
- 주석은 WHY가 비명백한 경우에만 한 줄로 작성
- **테스트 메서드명은 영어로 작성** — 한글 포함 금지; `method_condition_expectedResult` 패턴 사용

---

## 개발 워크플로우

각 Phase는 아래 순서로 진행한다.

1. **설계 문서 작성** — `docs/design/phaseN.md` 작성 후 사용자 검토 요청
2. **승인 후 구현** — 사용자 승인이 확인된 이후에 코드 작성 시작
3. **테스트 통과 확인** — `./gradlew test` BUILD SUCCESSFUL 확인
4. **승인 후 push** — 구현 완료 후 사용자 승인을 받은 뒤 `git push` 실행

> `git push`는 반드시 사용자 승인 후에만 실행한다.
