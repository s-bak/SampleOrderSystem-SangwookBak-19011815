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
| `ProductionLine` | 생산 라인. FIFO 큐로 작업을 순차 처리 |

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

### 생산량 산정 공식

```
실 생산량    = ceil(부족분 / (수율 × 0.9))
총 생산 시간 = 평균 생산시간 × 실 생산량  [단위: min]
```

---

## 메인 메뉴 구조

1. 시료 관리 — 등록(초기 재고=0 고정) / 목록 조회 / 항목별 검색(ID·이름·평균생산시간·수율)
2. 주문 접수 — 주문 등록 (`RESERVED`) / 주문 목록 조회
3. 주문 승인/거절 — 접수 주문 목록 / 거절 주문 목록 / 승인(재고 확인) / 거절; 대기 주문 없으면 승인·거절 즉시 차단
4. 모니터링 — 상태별 주문 수(RESERVED=하늘색·PRODUCING=주황색·CONFIRMED=연두색·RELEASE=연보라색, 시료명 옆 시료 ID 표시) + 시료별 재고 현황(여유=연두색·부족=주황색·고갈=빨강색)
5. 생산 라인 — 생산 현황 / 대기 큐 / 생산 완료 처리
6. 출고 처리 — `CONFIRMED` 주문 출고 → `RELEASE`

---

## 예외 처리 규칙

- 잘못된 상태 전이: `InvalidOrderStateTransitionException`
- 중복 시료 ID 등록: 오류 메시지 출력 후 ID 재입력 요청 (다른 필드는 묻지 않음)
- 시료명·고객명 공백 입력: 오류 메시지 출력 후 재입력 요청
- 잘못된 수율·재고 입력: 오류 메시지 출력 후 중단
- 존재하지 않는 ID 참조: 오류 메시지 출력
- 목록이 비었을 때: 안내 메시지 출력 후 메뉴 복귀

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

---

## 코딩 컨벤션

- 패키지 루트: `org.example`
- 엔티티, 서비스, 예외, UI 레이어를 분리하여 구성
- 콘솔 I/O는 별도 클래스로 분리 (테스트 용이성)
- 주석은 WHY가 비명백한 경우에만 한 줄로 작성

---

## 개발 워크플로우

각 Phase는 아래 순서로 진행한다.

1. **설계 문서 작성** — `docs/design/phaseN.md` 작성 후 사용자 검토 요청
2. **승인 후 구현** — 사용자 승인이 확인된 이후에 코드 작성 시작
3. **테스트 통과 확인** — `./gradlew test` BUILD SUCCESSFUL 확인
4. **승인 후 push** — 구현 완료 후 사용자 승인을 받은 뒤 `git push` 실행

> `git push`는 반드시 사용자 승인 후에만 실행한다.
