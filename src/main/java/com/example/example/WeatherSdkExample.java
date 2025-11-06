package com.example.example;

import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;

/**
 * Example usage of Weather SDK.
 * 
 * <p>This class demonstrates the main SDK features:
 * instance creation, weather data retrieval, cache operations.</p>
 * 
 * <p><b>To run:</b></p>
 * <pre>{@code
 * java -cp target/classes:target/dependency/* com.example.example.WeatherSdkExample
 * }</pre>
 * 
 * <p><b>Or via Maven:</b></p>
 * <pre>{@code
 * mvn exec:java -Dexec.mainClass="com.example.example.WeatherSdkExample"
 * }</pre>
 */
public class WeatherSdkExample {
    
    /**
     * Entry point for SDK usage example.
     * 
     * <p><b>Note:</b> To run this example, set a valid OpenWeather API key
     * in the WEATHER_API_KEY environment variable or update the code to use your key.</p>
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Get API key from environment variable
        String apiKey = System.getenv("WEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API key is not set. Please set the WEATHER_API_KEY environment variable.");
        }

        WeatherSdk sdk = null;
        try {
            System.out.println("=== Weather SDK Usage Example ===\n");

            // Example 1: Create SDK in ON_DEMAND mode
            System.out.println("1. Creating SDK in ON_DEMAND mode...");
            sdk = WeatherSdk.create(apiKey, SdkMode.ON_DEMAND);
            System.out.println("✓ SDK created successfully\n");

            // Example 2: Get weather for multiple cities
            System.out.println("2. Getting weather data for cities...");
            String[] cities = {"Moscow", "London", "Paris"};
            int errorCount = 0;
            final int MAX_ERRORS = 2;

            for (String city : cities) {
                try {
                    WeatherData weather = sdk.getCurrentWeather(city);
                    System.out.println("\n--- " + weather.getName() + " ---");
                    if (weather.getTemperature() != null) {
                        System.out.println("Temperature: " + weather.getTemperature().getTemp() + "°C");
                        System.out.println("Feels like: " + weather.getTemperature().getFeelsLike() + "°C");
                    }
                    if (weather.getWeather() != null && weather.getWeather().length > 0) {
                        System.out.println("Description: " + weather.getWeather()[0].getDescription());
                    }
                    if (weather.getWind() != null) {
                        System.out.println("Wind speed: " + weather.getWind().getSpeed() + " m/s");
                    }
                } catch (WeatherApiException e) {
                    System.err.println("Error getting weather for " + city + ": " + e.getMessage());
                    if (++errorCount >= MAX_ERRORS) {
                        throw new WeatherApiException("Too many errors while getting weather data. Execution stopped.");
                    }
                }
            }

            // Example 3: Cache info
            System.out.println("\n3. Cache info:");
            System.out.println("Number of cities in cache: " + sdk.getCacheSize());
            System.out.println("SDK mode: " + sdk.getMode());

            // Example 4: Repeat request (should return from cache)
            System.out.println("\n4. Repeat request for Moscow (should return from cache)...");
            WeatherData weather = sdk.getCurrentWeather("Moscow");
            System.out.println("✓ Data received (possibly from cache): " + weather.getName());

            System.out.println("=== Example completed successfully ===");

        } catch (WeatherApiException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sdk != null) {
                try {
                    System.out.println("\nReleasing resources...");
                    WeatherSdk.delete(apiKey);
                    System.out.println("✓ SDK deleted");
                } catch (Exception e) {
                    System.err.println("Error releasing resources: " + e.getMessage());
                }
            }
        }
    }
}

