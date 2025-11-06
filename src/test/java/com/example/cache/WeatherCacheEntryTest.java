package com.example.cache;

import com.example.model.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WeatherCacheEntryTest {

    private WeatherData weatherData;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        weatherData = new WeatherData(); // Name will be null (no setter)
        timestamp = Instant.now();
    }

    @Test
    void testConstructor() {
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, timestamp);

        assertNotNull(entry);
        assertEquals(weatherData, entry.getWeatherData());
        assertEquals(timestamp, entry.getTimestamp());
    }

    @Test
    void testGetWeatherData() {
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, timestamp);
        WeatherData result = entry.getWeatherData();
        assertNotNull(result);
        // Name is null by default
        assertNull(result.getName());
    }

    @Test
    void testGetTimestamp() {
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, timestamp);

        Instant result = entry.getTimestamp();
        assertEquals(timestamp, result);
    }

    @Test
    void testIsUpToDate_WhenDataIsFresh() {
        // Create entry with current timestamp
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, Instant.now());
        // Data should be fresh (TTL = 600 seconds)
        assertTrue(entry.isUpToDate(600));
    }

    @Test
    void testIsUpToDate_WhenDataIsExpired() {
        // Create entry with timestamp 15 minutes ago (900 seconds)
        Instant oldTimestamp = Instant.now().minusSeconds(900);
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, oldTimestamp);
        // Data should be expired (TTL = 600 seconds)
        assertFalse(entry.isUpToDate(600));
    }

    @Test
    void testIsUpToDate_WhenDataIsExactlyOnTTLBoundary() {
        // Create entry with timestamp just under 10 minutes ago (599 seconds)
        // to account for possible delay between timestamp creation and check
        Instant boundaryTimestamp = Instant.now().minusSeconds(599);
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, boundaryTimestamp);
        // Data should be fresh (just under TTL boundary)
        assertTrue(entry.isUpToDate(600));
    }

    @Test
    void testIsUpToDate_WithDifferentTTL() {
        // Create entry with timestamp 5 minutes ago
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        WeatherCacheEntry entry = new WeatherCacheEntry(weatherData, fiveMinutesAgo);
        // With TTL 10 minutes, data should be fresh
        assertTrue(entry.isUpToDate(600));
        // With TTL 1 minute, data should be expired
        assertFalse(entry.isUpToDate(60));
    }
}

