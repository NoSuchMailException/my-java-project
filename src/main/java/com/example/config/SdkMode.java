package com.example.config;

/**
 * SDK operation modes for weather data updates.
 * 
 * <p>The SDK supports two modes that determine the strategy
 * for updating cached weather data:</p>
 * 
 * @author Weather SDK Team
 * @see com.example.WeatherSdk
 */
public enum SdkMode {
    /**
     * ON_DEMAND mode - updates data only on client request.
     * 
     * <p>In this mode, weather data is updated only when the client
     * explicitly requests information for a city. If data is already cached and valid
     * (less than 10 minutes old), no API request is made.</p>
     * 
     * <p><b>Recommended:</b> when request frequency is low
     * or minimizing API calls is important.</p>
     */
    ON_DEMAND,
    
    /**
     * POLLING mode - automatic update for all cached locations.
     * 
     * <p>In this mode, the SDK automatically updates weather data for all cached cities
     * every 5 minutes. This ensures zero response delay for client requests, as data is always fresh.</p>
     * 
     * <p><b>Recommended:</b> when maximum response speed and high request frequency are required.</p>
     * 
     * <p><b>Important:</b> In POLLING mode, you must call {@link com.example.WeatherSdk#shutdown()}
     * or {@link com.example.WeatherSdk#delete(String)} to properly terminate background update threads.</p>
     */
    POLLING
}

