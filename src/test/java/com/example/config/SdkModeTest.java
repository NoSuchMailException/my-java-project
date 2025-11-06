package com.example.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdkModeTest {

    @Test
    void testEnumValues() {
        // Check that enum contains expected values
        SdkMode[] values = SdkMode.values();
        assertEquals(2, values.length);
        assertTrue(Enum.valueOf(SdkMode.class, "ON_DEMAND") == SdkMode.ON_DEMAND);
        assertTrue(Enum.valueOf(SdkMode.class, "POLLING") == SdkMode.POLLING);
    }

    @Test
    void testValueOf() {
        assertEquals(SdkMode.ON_DEMAND, SdkMode.valueOf("ON_DEMAND"));
        assertEquals(SdkMode.POLLING, SdkMode.valueOf("POLLING"));
    }

    @Test
    void testValueOf_InvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            SdkMode.valueOf("INVALID_MODE");
        });
    }

    @Test
    void testEnumComparison() {
        assertNotEquals(SdkMode.ON_DEMAND, SdkMode.POLLING);
        assertEquals(SdkMode.ON_DEMAND, SdkMode.ON_DEMAND);
        assertEquals(SdkMode.POLLING, SdkMode.POLLING);
    }
}

