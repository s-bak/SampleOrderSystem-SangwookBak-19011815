# Phase 1 설계 문서 — 프로젝트 골격 및 패키지 구조

**대상 Phase:** Phase 1  
**기준 문서:** `docs/PLAN.md`, `docs/PRD.md`

---

## 1. 목표

이후 모든 Phase의 기반이 되는 패키지 레이아웃, 공통 예외 클래스, 애플리케이션 진입점을 확립한다.  
비즈니스 로직은 구현하지 않으며, 빌드와 기본 실행이 가능한 골격만 만든다.

---

## 2. 디렉터리 및 파일 구조

```
src/
└── main/
│   └── java/org/example/
│       ├── Application.java              # main 진입점
│       ├── domain/                       # 엔티티 (Phase 2~3에서 채움)
│       ├── repository/                   # 인메모리 저장소 (Phase 2~3에서 채움)
│       ├── service/                      # 비즈니스 로직 (Phase 4~8에서 채움)
│       ├── exception/
│       │   └── InvalidOrderStateTransitionException.java
│       └── ui/                           # 콘솔 I/O (Phase 9에서 채움)
└── test/
    └── java/org/example/
        └── exception/
            └── InvalidOrderStateTransitionExceptionTest.java
```

---

## 3. 구현 명세

### 3.1 `InvalidOrderStateTransitionException`

**패키지:** `org.example.exception`

```
RuntimeException 상속
생성자: (String message)
  → super(message) 호출
```

사용 예시 (참고용, 실제 호출은 Phase 3에서):
```
throw new InvalidOrderStateTransitionException(
    "RELEASE 상태에서 CONFIRMED로 전이할 수 없습니다."
);
```

---

### 3.2 `Application`

**패키지:** `org.example`

```
main(String[] args)
  → "SampleOrderSystem 시작" 콘솔 출력 후 종료 (stub)
```

Phase 9에서 MainMenu 호출로 교체된다.

---

## 4. 테스트 명세

### `InvalidOrderStateTransitionExceptionTest`

| # | 테스트명 | 검증 내용 |
|---|---|---|
| 1 | `exception_hasMessage` | 생성자에 전달한 메시지가 `getMessage()`로 반환된다 |
| 2 | `exception_isRuntimeException` | `RuntimeException`의 인스턴스임을 확인 |

---

## 5. 빌드 검증

```bash
./gradlew build   # 컴파일 오류 없이 성공
./gradlew test    # 위 2개 테스트 통과
./gradlew run     # "SampleOrderSystem 시작" 출력 후 정상 종료
```

> `./gradlew run` 실행을 위해 `build.gradle`에 `mainClass` 지정이 필요하다. (아래 참고)

### build.gradle 추가 설정

```groovy
application {
    mainClass = 'org.example.Application'
}
```

또는 기존 `plugins` 블록에 `id 'application'` 추가:

```groovy
plugins {
    id 'java'
    id 'application'
}
```

---

## 6. 완료 조건

- [ ] 패키지 디렉터리 6개 생성 (`domain`, `repository`, `service`, `exception`, `ui`, 루트)
- [ ] `InvalidOrderStateTransitionException` 구현 및 테스트 2개 통과
- [ ] `Application.java` stub 구현 및 `./gradlew run` 정상 실행
- [ ] `./gradlew build` 성공
