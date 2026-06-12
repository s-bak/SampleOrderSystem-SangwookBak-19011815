# Phase 9 설계 문서 — 콘솔 UI 구현

**대상 Phase:** Phase 9  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md` §4.1 ~ §4.7

---

## 1. 목표

서비스 레이어를 콘솔 메뉴 시스템에 연결한다.  
`ConsoleIO`로 입출력을 추상화하고, 핸들러별로 책임을 분리한다.  
`Application.main()`에서 모든 의존성을 직접 생성하여 주입한다.  
검증은 `./gradlew run` 수동 실행으로 수행한다.

---

## 2. 파일 구조

```
src/main/java/org/example/
├── Application.java              # 의존성 조립 + MainMenu 실행 (기존 stub 교체)
├── ui/
│   ├── ConsoleIO.java            # Scanner/PrintStream 래퍼
│   ├── MainMenu.java             # 메인 메뉴 루프
│   ├── SampleMenuHandler.java    # 시료 관리 (등록 / 목록 / 검색)
│   ├── OrderMenuHandler.java     # 주문 접수 (등록 / 목록)
│   ├── ApprovalMenuHandler.java  # 주문 승인/거절
│   ├── MonitoringMenuHandler.java# 모니터링 (주문량 / 재고량)
│   ├── ReleaseMenuHandler.java   # 출고 처리
│   └── ProductionMenuHandler.java# 생산 라인
```

---

## 3. 구현 명세

### 3.1 `ConsoleIO`

**패키지:** `org.example.ui`

```
ConsoleIO(InputStream in, PrintStream out)
```

| 메서드 | 설명 |
|---|---|
| `print(String msg)` | 개행 없이 출력 |
| `println(String msg)` | 개행 포함 출력 |
| `println()` | 빈 줄 출력 |
| `readLine()` | 한 줄 입력 받아 trim 후 반환 |
| `readInt(String prompt)` | prompt 출력 후 정수 입력. 파싱 실패 시 -1 반환 |

---

### 3.2 `Application` 업데이트

`main()`에서 모든 객체를 생성하고 `MainMenu`를 실행한다.

```
SampleRepository   sampleRepo   = new SampleRepository()
OrderRepository    orderRepo    = new OrderRepository()
ProductionQueue    queue        = new ProductionQueue()

SampleService      sampleSvc    = new SampleService(sampleRepo)
OrderService       orderSvc     = new OrderService(sampleRepo, orderRepo)
ApprovalService    approvalSvc  = new ApprovalService(orderRepo, queue)
ProductionService  productionSvc= new ProductionService(orderRepo, queue)
ReleaseService     releaseSvc   = new ReleaseService(orderRepo)
MonitoringService  monitorSvc   = new MonitoringService(sampleRepo, orderRepo)

ConsoleIO io = new ConsoleIO(System.in, System.out)
new MainMenu(io, sampleSvc, orderSvc, approvalSvc, productionSvc, releaseSvc, monitorSvc).run()
```

---

### 3.3 `MainMenu`

메인 메뉴를 반복 출력하고 선택에 따라 핸들러를 호출한다.  
상단에 등록된 전체 시료 요약(시료 수, 총 재고)을 표시한다.

**화면 형식**

```
==============================
  S-Semi 시료 생산주문관리 시스템
  등록 시료: N개 | 총 재고: M개
==============================
1. 시료 관리
2. 주문 접수
3. 주문 승인 / 거절
4. 모니터링
5. 출고 처리
6. 생산 라인
0. 종료
선택> 
```

- `0` 입력 시 "시스템을 종료합니다." 출력 후 종료
- 그 외 숫자 입력 시 해당 핸들러 호출
- 유효하지 않은 입력 시 "[오류] 올바른 메뉴 번호를 입력해주세요." 출력 후 재표시

---

### 3.4 서브메뉴 핸들러 공통 규칙

- 서비스 예외(`IllegalArgumentException`, `InvalidOrderStateTransitionException`)는 핸들러에서 catch하여 `[오류] {message}` 형식으로 출력
- 각 핸들러는 작업 완료 또는 "0. 뒤로" 선택 후 메인 메뉴로 복귀
- 빈 목록 조회 시 PRD 명시 안내 메시지 출력 후 복귀

---

### 3.5 `SampleMenuHandler`

**서브메뉴**

```
1. 시료 등록
2. 시료 목록 조회
3. 시료 검색
0. 뒤로
```

**시료 등록** 입력 순서: 시료 ID → 시료명 → 평균 생산시간 → 수율 (초기 재고는 0 고정)  
**시료 목록** 출력 컬럼: `시료ID | 시료명 | 평균생산시간(min) | 수율 | 재고`  
**시료 검색** 입력: 검색 키워드 → 결과 목록 출력 (목록과 동일 형식)

---

### 3.6 `OrderMenuHandler`

**서브메뉴**

```
1. 주문 등록
2. 주문 목록 조회
0. 뒤로
```

**주문 등록** 입력 순서: 시료 ID → 고객명 → 주문 수량  
**주문 목록** 출력 컬럼: `주문ID | 고객명 | 시료명 | 수량 | 상태 | 등록일시`

---

### 3.7 `ApprovalMenuHandler`

**서브메뉴**

```
1. 접수 주문 목록 조회 (RESERVED)
2. 주문 승인
3. 주문 거절
0. 뒤로
```

**접수 주문 목록** 출력 컬럼: `주문ID | 고객명 | 시료명 | 수량 | 등록일시`  
**주문 승인/거절** 입력: 주문 ID → 처리 결과 메시지 출력

---

### 3.8 `MonitoringMenuHandler`

**서브메뉴**

```
1. 주문량 확인 (상태별)
2. 재고량 확인
0. 뒤로
```

**주문량 확인** 출력: RESERVED / PRODUCING / CONFIRMED / RELEASE 상태별 주문 목록  
출력 컬럼: `주문ID | 고객명 | 시료명 | 수량 | 상태`

**재고량 확인** 출력 컬럼: `시료ID | 시료명 | 재고 | 대기수량 | 상태(여유/부족/고갈)`

---

### 3.9 `ReleaseMenuHandler`

**서브메뉴**

```
1. 출고 대기 주문 목록 조회 (CONFIRMED)
2. 출고 실행
0. 뒤로
```

**출고 대기 목록** 출력 컬럼: `주문ID | 고객명 | 시료명 | 수량`  
**출고 실행** 입력: 주문 ID → "출고 완료: [주문ID]" 출력

---

### 3.10 `ProductionMenuHandler`

**서브메뉴**

```
1. 생산 현황 조회 (현재 생산 중)
2. 대기 주문 확인
3. 생산 완료 처리
0. 뒤로
```

**생산 현황** 출력 컬럼: `주문ID | 시료명 | 실생산량 | 총생산시간(min)`  
**대기 주문** 출력 컬럼: `주문ID | 시료명 | 실생산량 | 예상생산시간(min)`  
**생산 완료 처리** 입력: 주문 ID → "생산 완료: [주문ID], 재고 [N]개 추가" 출력

---

## 4. 검증 시나리오 (수동)

`./gradlew run` 실행 후 아래 순서로 확인한다.

| 순서 | 행동 | 기대 결과 |
|---|---|---|
| 1 | 메인 메뉴 진입 | 헤더 + 6개 메뉴 출력 |
| 2 | 시료 등록 (`S-001`, `AlphaChip`, 30, 0.85, 10) | 등록 완료 메시지 |
| 3 | 시료 목록 조회 | S-001 표시 |
| 4 | 주문 등록 (`S-001`, `고객A`, 5) | O-001 발급 |
| 5 | 주문 승인 (`O-001`) | 재고 충분 → CONFIRMED |
| 6 | 출고 실행 (`O-001`) | RELEASE |
| 7 | 주문 등록 (`S-001`, `고객B`, 20) | O-002 발급 |
| 8 | 주문 승인 (`O-002`) | 재고 부족 → PRODUCING, 큐 등록 |
| 9 | 생산 현황 조회 | O-002 표시 |
| 10 | 생산 완료 처리 (`O-002`) | CONFIRMED, 재고 증가 |
| 11 | 잘못된 메뉴 번호 입력 | 오류 메시지 후 재표시 |
| 12 | `0` 입력 | 종료 |

---

## 5. 완료 조건

- [ ] `ConsoleIO` 구현
- [ ] `Application` 의존성 조립 업데이트
- [ ] `MainMenu` + 6개 핸들러 구현
- [ ] `./gradlew run` 으로 위 12개 시나리오 수동 검증 통과
- [ ] `./gradlew test` BUILD SUCCESSFUL (기존 90개 유지)
