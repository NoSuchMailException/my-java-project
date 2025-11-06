package com.example;

import com.example.config.SdkMode;
import com.example.exception.WeatherApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherSdkTest {

    private static final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        // Clean up registry before each test
        cleanupRegistry();
    }

    @AfterEach
    void tearDown() {
        // Clean up registry after each test
        cleanupRegistry();
    }

    private void cleanupRegistry() {
        // Remove all instances from registry
        String[] keys = {"test-api-key", "test-api-key-1", "test-api-key-2", "test-api-key-3"};
        for (String key : keys) {
            try {
                WeatherSdk.delete(key);
            } catch (Exception e) {
                // Ignore errors during deletion
            }
        }
    }

    @Test
    void testCreate_WithValidApiKey() throws WeatherApiException {
        WeatherSdk sdk = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        assertNotNull(sdk);
        assertEquals(SdkMode.ON_DEMAND, sdk.getMode());
    }

    @Test
    void testCreate_WithNullApiKey() {
        assertThrows(WeatherApiException.class, () -> {
            WeatherSdk.create(null, SdkMode.ON_DEMAND);
        });
    }

    @Test
    void testCreate_WithEmptyApiKey() {
        assertThrows(WeatherApiException.class, () -> {
            WeatherSdk.create("", SdkMode.ON_DEMAND);
        });

        assertThrows(WeatherApiException.class, () -> {
            WeatherSdk.create("   ", SdkMode.ON_DEMAND);
        });
    }

    @Test
    void testCreate_WithNullMode() {
        assertThrows(WeatherApiException.class, () -> {
            WeatherSdk.create(TEST_API_KEY, null);
        });
    }

    @Test
    void testCreate_WithDifferentModes() throws WeatherApiException {
        WeatherSdk sdk1 = WeatherSdk.create("test-api-key-1", SdkMode.ON_DEMAND);
        WeatherSdk sdk2 = WeatherSdk.create("test-api-key-2", SdkMode.POLLING);

        assertNotNull(sdk1);
        assertNotNull(sdk2);
        assertEquals(SdkMode.ON_DEMAND, sdk1.getMode());
        assertEquals(SdkMode.POLLING, sdk2.getMode());
    }

    @Test
    void testCreate_ReturnsSameInstance_ForSameApiKey() throws WeatherApiException {
        WeatherSdk sdk1 = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);
        WeatherSdk sdk2 = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        assertSame(sdk1, sdk2);
    }

    @Test
    void testCreate_ThrowsException_WhenModeDiffers() throws WeatherApiException {
        WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        assertThrows(WeatherApiException.class, () -> {
            WeatherSdk.create(TEST_API_KEY, SdkMode.POLLING);
        });
    }

    @Test
    void testCreate_TrimsApiKey() throws WeatherApiException {
        WeatherSdk sdk1 = WeatherSdk.create("  " + TEST_API_KEY + "  ", SdkMode.ON_DEMAND);
        WeatherSdk sdk2 = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        assertSame(sdk1, sdk2);
    }

    @Test
    void testGet_ReturnsExistingInstance() throws WeatherApiException {
        WeatherSdk created = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);
        WeatherSdk retrieved = WeatherSdk.get(TEST_API_KEY);

        assertSame(created, retrieved);
    }

    @Test
    void testGet_ReturnsNull_WhenNotExists() {
        WeatherSdk result = WeatherSdk.get(TEST_API_KEY);
        assertNull(result);
    }

    @Test
    void testGet_WithNullApiKey() {
        WeatherSdk result = WeatherSdk.get(null);
        assertNull(result);
    }

    @Test
    void testGet_WithEmptyApiKey() {
        WeatherSdk result = WeatherSdk.get("");
        assertNull(result);
    }

    @Test
    void testGet_TrimsApiKey() throws WeatherApiException {
        WeatherSdk created = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);
        WeatherSdk retrieved = WeatherSdk.get("  " + TEST_API_KEY + "  ");

        assertSame(created, retrieved);
    }

    @Test
    void testDelete_ReturnsTrue_WhenExists() throws WeatherApiException {
        WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        boolean deleted = WeatherSdk.delete(TEST_API_KEY);

        assertTrue(deleted);
        assertNull(WeatherSdk.get(TEST_API_KEY));
    }

    @Test
    void testDelete_ReturnsFalse_WhenNotExists() {
        boolean deleted = WeatherSdk.delete(TEST_API_KEY);
        assertFalse(deleted);
    }

    @Test
    void testDelete_WithNullApiKey() {
        boolean deleted = WeatherSdk.delete(null);
        assertFalse(deleted);
    }

    @Test
    void testDelete_WithEmptyApiKey() {
        boolean deleted = WeatherSdk.delete("");
        assertFalse(deleted);
    }

    @Test
    void testDelete_TrimsApiKey() throws WeatherApiException {
        WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        boolean deleted = WeatherSdk.delete("  " + TEST_API_KEY + "  ");

        assertTrue(deleted);
    }

    @Test
    void testGetMode() throws WeatherApiException {
        WeatherSdk sdkOnDemand = WeatherSdk.create("test-api-key-1", SdkMode.ON_DEMAND);
        WeatherSdk sdkPolling = WeatherSdk.create("test-api-key-2", SdkMode.POLLING);

        assertEquals(SdkMode.ON_DEMAND, sdkOnDemand.getMode());
        assertEquals(SdkMode.POLLING, sdkPolling.getMode());
    }

    @Test
    void testGetCacheSize_InitiallyZero() throws WeatherApiException {
        WeatherSdk sdk = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);

        assertEquals(0, sdk.getCacheSize());
    }

    @Test
    void testGetCurrentWeather_WithNullOrEmptyCityName() throws WeatherApiException {
        WeatherSdk sdk = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);
        assertThrows(WeatherApiException.class, () -> sdk.getCurrentWeather(null));
        assertThrows(WeatherApiException.class, () -> sdk.getCurrentWeather(""));
        assertThrows(WeatherApiException.class, () -> sdk.getCurrentWeather("   "));
    }

    @Test
    void testShutdownPollingMode_DoesNotThrow() throws WeatherApiException {
        WeatherSdk sdk = WeatherSdk.create(TEST_API_KEY, SdkMode.POLLING);
        assertDoesNotThrow(sdk::shutdown);
    }

    @Test
    void testShutdownOnDemandMode_DoesNotThrow() throws WeatherApiException {
        WeatherSdk sdk = WeatherSdk.create(TEST_API_KEY, SdkMode.ON_DEMAND);
        assertDoesNotThrow(sdk::shutdown);
    }

    @Test
    void testDelete_CallsShutdownAndRemovesInstance() throws WeatherApiException {
        WeatherSdk.create(TEST_API_KEY, SdkMode.POLLING);
        boolean deleted = WeatherSdk.delete(TEST_API_KEY);
        assertTrue(deleted);
        assertNull(WeatherSdk.get(TEST_API_KEY));
    }

    @Test
    void testMultipleInstances_WithDifferentApiKeysAndModes() throws WeatherApiException {
        WeatherSdk sdk1 = WeatherSdk.create("test-api-key-1", SdkMode.ON_DEMAND);
        WeatherSdk sdk2 = WeatherSdk.create("test-api-key-2", SdkMode.ON_DEMAND);
        WeatherSdk sdk3 = WeatherSdk.create("test-api-key-3", SdkMode.POLLING);
        assertNotNull(sdk1);
        assertNotNull(sdk2);
        assertNotNull(sdk3);
        assertNotSame(sdk1, sdk2);
        assertNotSame(sdk1, sdk3);
        assertNotSame(sdk2, sdk3);
    }

    // Additional tests for cache logic, LRU, TTL, and POLLING mode can be added here
}

