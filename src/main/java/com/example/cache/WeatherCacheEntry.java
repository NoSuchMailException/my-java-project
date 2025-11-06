package com.example.cache;

import com.example.model.WeatherData;
import java.time.Instant;

/**
 * Cache entry for storing weather data with a timestamp.
 *
 * <p>Used internally by {@link com.example.WeatherSdk} for caching weather data.
 * Each entry contains weather data and the time it was received, allowing
 * determination of data freshness.</p>
 *
 * @author Weather SDK Team
 * @see com.example.WeatherSdk
 */
public class WeatherCacheEntry {
    private final WeatherData weatherData;
    private final Instant timestamp;
    
    /**
     * Creates a new cache entry.
     *
     * @param weatherData weather data
     * @param timestamp time the data was received
     */
    public WeatherCacheEntry(WeatherData weatherData, Instant timestamp) {
        this.weatherData = weatherData;
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the weather data.
     *
     * @return WeatherData object containing weather information
     */
    public WeatherData getWeatherData() {
        return weatherData;
    }
    
    /**
     * Gets the time the data was received.
     *
     * @return time the data was received as Instant
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Checks if the data is up-to-date (less than TTL seconds old).
     *
     * <p>Data is considered up-to-date if it was received less than the specified TTL ago.
     * This matches the cache's Time To Live (TTL) requirements.</p>
     *
     * @param ttlSeconds cache time-to-live in seconds
     * @return true if the data is up-to-date (less than TTL), false otherwise
     */
    public boolean isUpToDate(long ttlSeconds) {
        return Instant.now().minusSeconds(ttlSeconds).isBefore(timestamp);
    }
}

