# SampleOrderSystem

S-Semi 반도체 시료 생산주문관리 시스템 — Java 17+ / Gradle / JUnit 5 기반 콘솔 애플리케이션.

---

## 개요

반도체 시료(Sample)의 등록부터 주문 접수·승인·생산·출고까지 전 과정을 관리하는 CLI 시스템입니다.
주문 담당자와 생산 담당자의 역할을 구분하여 업무 흐름을 시뮬레이션하며, 백그라운드 스케줄러가 생산 진행 상태를 자동으로 업데이트합니다.
데이터는 `data/db.json`에 저장되어 재시작 후에도 유지됩니다.

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| 언어 | Java 17+ |
| 빌드 | Gradle |
| 테스트 | JUnit 5 (227개 테스트) |
| 직렬화 | Jackson 2.17.1 (`jackson-databind`, `jackson-datatype-jsr310`) |
| 실행 방식 | 콘솔(CLI) |

---

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew run
```

---

## 메인 메뉴 구조

```
1. 시료 관리      — 시료 등록 / 목록 조회 / 항목별 검색 (ID·이름·생산시간·수율)
2. 주문 접수      — 주문 등록 (RESERVED) / 주문 목록 조회
3. 주문 승인/거절 — 접수·거절 주문 목록 조회 / 승인 / 거절
4. 모니터링       — 상태별 주문 수(ANSI 색상) + 시료별 재고 현황(ANSI 색상)
5. 생산 라인      — 생산 현황(진행률 실시간) / 대기 큐
6. 출고 처리      — CONFIRMED 주문 선택 후 출고 실행
```

---

## 주문 상태 흐름

```
[주문 등록]
     │
     ▼
  RESERVED
  /       \
[거절]    [승인]
  ▼            ▼
REJECTED    재고 확인
            /       \
  [재고 부족]         [재고 충분]
        ▼                  ▼
    PRODUCING ──────► CONFIRMED
    [생산 완료 자동]        │
                      [출고 처리]
                           ▼
                        RELEASE
```

| 현재 상태 | 전이 가능 상태 | 조건 |
|---|---|---|
| `RESERVED` | `REJECTED` | 생산 담당자 거절 |
| `RESERVED` | `PRODUCING` | 승인 + 가용 재고 부족 |
| `RESERVED` | `CONFIRMED` | 승인 + 가용 재고 충분 |
| `PRODUCING` | `CONFIRMED` | 백그라운드 스케줄러 자동 완료 |
| `CONFIRMED` | `RELEASE` | 출고 처리 |

---

## 핵심 비즈니스 로직

### 가용 재고 계산

```
가용 재고 = stock − confirmedQty − producingStockAdded
```

- `confirmedQty`: 동일 시료의 CONFIRMED 주문 수량 합계
- `producingStockAdded`: 생산 중 작업에서 재고에 이미 추가된 수량 합계

### 생산량 산정

```
부족분       = 주문 수량 − max(0, 가용 재고)
실 생산량    = ceil(부족분 / (수율 × 0.9))
총 생산 시간 = 평균 생산시간 × 실 생산량   [단위: min]
```

수율에 0.9를 곱하는 것은 공정 오차를 고려한 안전 마진입니다.

### 재고 단위별 점진 업데이트

백그라운드 스케줄러(1초 주기)가 경과 시간을 기반으로 완성된 단위 수를 계산하여 1개씩 재고를 추가합니다.

```
msPerUnit = totalProductionTime × 60,000 / shortfall
```

`stockAdded >= shortfall` 도달 시 자동으로 `PRODUCING → CONFIRMED` 전이 및 다음 대기 작업 시작.

### 재고 상태

| 상태 | 조건 | 색상 |
|---|---|---|
| 여유 | `stock > 0` AND 대기 수량 = 0 | 연두색 |
| 부족 | `stock > 0` AND 대기 수량 > 0 | 주황색 |
| 고갈 | `stock == 0` | 빨강색 |

---

## 패키지 구조

```
src/main/java/org/example/
├── Application.java          # 진입점
├── domain/                   # 엔티티
│   ├── Sample.java
│   ├── Order.java
│   ├── OrderStatus.java
│   ├── ProductionJob.java
│   └── ProductionQueue.java
├── repository/               # 인메모리 저장소 + JSON 영속성
│   ├── SampleRepository.java
│   ├── OrderRepository.java
│   └── JsonDataStore.java
├── service/                  # 비즈니스 로직
│   ├── SampleService.java
│   ├── OrderService.java
│   ├── ApprovalService.java
│   ├── ProductionService.java
│   ├── ProductionScheduler.java
│   ├── ReleaseService.java
│   └── MonitoringService.java
├── ui/                       # 콘솔 I/O
│   ├── ConsoleIO.java
│   ├── MainMenu.java
│   ├── SampleMenuHandler.java
│   ├── OrderMenuHandler.java
│   ├── ApprovalMenuHandler.java
│   ├── ProductionMenuHandler.java
│   ├── ReleaseMenuHandler.java
│   └── MonitoringMenuHandler.java
└── exception/
    └── InvalidOrderStateTransitionException.java
```

---

## 데이터 영속성

- 저장 위치: `data/db.json` (`.gitignore`로 제외; `data/.gitkeep`만 추적)
- 저장 시점: 시료 등록·주문 등록·승인·거절·출고·생산 완료 성공 직후 즉시 저장
- 복원 시 `OrderRepository.restoreFromDb()`를 사용하여 주문 ID 시퀀스가 중복 증가하지 않도록 처리

---

## 예외 처리

| 상황 | 처리 |
|---|---|
| 허용되지 않는 상태 전이 | `InvalidOrderStateTransitionException` |
| 중복 시료 ID 등록 | 오류 메시지 출력 후 ID 재입력 요청 |
| 빈 값 입력 | `[안내] 입력이 없어 이전 메뉴로 돌아갑니다.` 출력 후 메뉴 복귀 |
| 잘못된 수율·생산시간 입력 | 오류 메시지 출력 후 중단 |
| 존재하지 않는 ID 참조 | 오류 메시지 출력 |
| 목록이 비어 있을 때 | 안내 메시지 출력 후 메뉴 복귀 |
