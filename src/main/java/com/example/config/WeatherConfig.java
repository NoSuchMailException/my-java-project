package com.example.config;

/**
 * Configuration class for OpenWeather API.
 * After obtaining your API key, add it to the API_KEY variable.
 */
public class WeatherConfig {
    // TODO: Replace with your API key after registering at openweathermap.org
    public static final String API_KEY = "YOUR_API_KEY_HERE";
    
    public static final String BASE_URL = "https://api.openweathermap.org/data/3.0";
    
    /**
     * Constants for request parameters
     */
    public static final String UNITS_METRIC = "metric";
    public static final String LANG_RU = "ru";
    
    /**
     * Checks if the API key is set
     */
    public static boolean isApiKeySet() {
        return API_KEY != null && !API_KEY.equals("YOUR_API_KEY_HERE") && !API_KEY.isEmpty();
    }
}