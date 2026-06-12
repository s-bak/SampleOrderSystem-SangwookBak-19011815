package org.example.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidOrderStateTransitionExceptionTest {

    @Test
    void exception_hasMessage() {
        String message = "RELEASE 상태에서 CONFIRMED로 전이할 수 없습니다.";
        InvalidOrderStateTransitionException ex = new InvalidOrderStateTransitionException(message);
        assertEquals(message, ex.getMessage());
    }

    @Test
    void exception_isRuntimeException() {
        InvalidOrderStateTransitionException ex = new InvalidOrderStateTransitionException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
