package com.example.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherApiExceptionTest {

    @Test
    void testConstructor_WithMessage() {
        String message = "Test error message";
        WeatherApiException exception = new WeatherApiException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor_WithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        WeatherApiException exception = new WeatherApiException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        WeatherApiException exception = new WeatherApiException("Test");
        assertTrue(exception instanceof Exception);
    }

    @Test
    void testCanBeThrown() {
        assertThrows(WeatherApiException.class, () -> {
            throw new WeatherApiException("Test exception");
        });
    }

    @Test
    void testCanBeThrown_WithCause() {
        RuntimeException cause = new RuntimeException("Cause");
        assertThrows(WeatherApiException.class, () -> {
            throw new WeatherApiException("Test exception", cause);
        });
    }
}

