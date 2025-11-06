package com.example;

import com.example.model.WeatherData;
import com.example.cache.WeatherCacheEntry;
import com.example.exception.WeatherApiException;
import com.example.config.SdkMode;
import com.example.config.WeatherConfig;
import java.lang.AutoCloseable;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SDK for working with OpenWeather API.
 * 
 * <p>Main features:</p>
 * <ul>
 *   <li>Get current weather by city name</li>
 *   <li>Automatic data caching (up to 10 cities, valid for 10 minutes)</li>
 *   <li>Two operation modes: ON_DEMAND and POLLING</li>
 *   <li>Instance management via Registry Pattern</li>
 *   <li>Supports try-with-resources for automatic resource cleanup</li>
 * </ul>
 * 
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * // Create SDK in ON_DEMAND mode
 * WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND);
 * 
 * // Get weather for a city
 * WeatherData weather = sdk.getCurrentWeather("Moscow");
 * 
 * // Release resources
 * WeatherSdk.delete("your-api-key");
 * }</pre>
 * 
 * <p><b>Packages:</b></p>
 * <ul>
 *   <li>{@link com.example.model.WeatherData} - weather data model</li>
 *   <li>{@link com.example.config.SdkMode} - SDK operation modes</li>
 *   <li>{@link com.example.exception.WeatherApiException} - exceptions</li>
 * </ul>
 * 
 * <p><b>Operation modes:</b></p>
 * <ul>
 *   <li><b>ON_DEMAND</b> - data is updated only on client request</li>
 *   <li><b>POLLING</b> - automatic data update for all cached cities every 5 minutes</li>
 * </ul>
 * 
 * <p><b>Caching:</b></p>
 * <ul>
 *   <li>Maximum 10 cities in cache</li>
 *   <li>Data is valid for 10 minutes</li>
 *   <li>Uses LRU (Least Recently Used) algorithm for eviction</li>
 * </ul>
 * 
 * @author Weather SDK Team
 * @version 1.0
 * @since 1.0
 */
public class WeatherSdk implements AutoCloseable {
    private static final int MAX_CACHE_SIZE = 10;
    private static final long CACHE_TTL_SECONDS = 600; // 10 минут
    
    private final String apiKey;
    private final SdkMode mode;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, WeatherCacheEntry> cache;
    private final Map<String, String> originalCityNames; // Stores original city names
    private final LinkedHashMap<String, Instant> accessOrder; // For LRU
    private final Object cacheLock = new Object();
    private ScheduledExecutorService pollingExecutor;
    private volatile boolean isRunning = true;
    private final com.example.internal.WeatherApiClient apiClient;
    
    /**
     * Creates SDK with specified API key and operation mode
     * 
     * @param apiKey OpenWeather API key
     * @param mode SDK operation mode (ON_DEMAND or POLLING)
     * @throws WeatherApiException if API key is not provided
     */
    @Override
    public void close() throws Exception {
        try {
            shutdown();
            WeatherSdk.delete(this.apiKey);
        } catch (Exception e) {
            throw new WeatherApiException("Error during SDK cleanup: " + e.getMessage());
        }
    }

    private WeatherSdk(String apiKey, SdkMode mode) throws WeatherApiException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new WeatherApiException("API ключ не может быть пустым");
        }
        if (mode == null) {
            throw new WeatherApiException("Режим работы не может быть null");
        }
        
        this.apiKey = apiKey.trim();
        this.mode = mode;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = new HashMap<>();
        this.originalCityNames = new HashMap<>();
        this.accessOrder = new LinkedHashMap<>(16, 0.75f, true); // LRU order
        
        if (mode == SdkMode.POLLING) {
            startPolling();
        }
        this.apiClient = new com.example.internal.WeatherApiClient(this.apiKey);
    }
    
    /**
     * Gets current weather by city name.
     * 
     * <p>Returns weather data for the first found city with the specified name.
     * If data is already cached and valid (less than 10 minutes old),
     * returns from cache without API request.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND);
     * WeatherData weather = sdk.getCurrentWeather("Moscow");
     * System.out.println("Temperature: " + weather.getTemperature().getTemp());
     * }</pre>
     * 
     * @param cityName city name (e.g., "Moscow", "Москва", "New York")
     * @return WeatherData object with weather info (temperature, description, wind, etc.)
     * @throws WeatherApiException if:
     *   <ul>
     *     <li>city name is empty or null</li>
     *     <li>city not found</li>
     *     <li>API request error</li>
     *     <li>API key is invalid</li>
     *   </ul>
     */
    public WeatherData getCurrentWeather(String cityName) throws WeatherApiException {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new WeatherApiException("City name cannot be empty");
        }

        String normalizedCityName = cityName.trim().toLowerCase();

        synchronized (cacheLock) {
            WeatherCacheEntry cached = cache.get(normalizedCityName);
            if (cached != null && cached.isUpToDate(CACHE_TTL_SECONDS)) {
                accessOrder.put(normalizedCityName, Instant.now());
                return cached.getWeatherData();
            }
        }

        // Use geocoding to get coordinates for the city
        com.example.internal.WeatherApiClient.GeocodingResult coords = apiClient.getCoordinatesByCityName(cityName.trim());
        if (coords == null) {
            throw new WeatherApiException("City not found: " + cityName);
        }
        WeatherData weatherData = getCurrentWeatherByCoordinates(coords.lat, coords.lon);

        synchronized (cacheLock) {
            // LRU cache logic
            if (cache.size() >= MAX_CACHE_SIZE && !cache.containsKey(normalizedCityName)) {
                String oldestCity = accessOrder.entrySet().iterator().next().getKey();
                cache.remove(oldestCity);
                originalCityNames.remove(oldestCity);
                accessOrder.remove(oldestCity);
            }
            cache.put(normalizedCityName, new WeatherCacheEntry(weatherData, Instant.now()));
            originalCityNames.put(normalizedCityName, cityName.trim());
            accessOrder.put(normalizedCityName, Instant.now());
        }

        return weatherData;
    }

    /**
     * Gets current weather using coordinates (latitude, longitude)
     *
     * @param lat latitude (from -90 to 90)
     * @param lon longitude (from -180 to 180)
     * @return WeatherData object containing weather information
     * @throws WeatherApiException if coordinates are invalid or an error occurs
     */
    public WeatherData getCurrentWeatherByCoordinates(double lat, double lon) throws WeatherApiException {
        return apiClient.getCurrentWeatherByCoordinates(lat, lon);
    }
    
    /**
     * Performs request to OpenWeather API
     */
    private WeatherData fetchWeatherFromApi(String cityName) throws WeatherApiException {
        try {
            String encodedCityName = URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/weather?q=%s&appid=%s&units=metric&lang=ru",
                    WeatherConfig.BASE_URL,
                    encodedCityName,
                    apiKey);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                String errorMessage = response.body();
                
                // Handle various status codes
                switch (response.statusCode()) {
                    case 401:
                        throw new WeatherApiException("Invalid API key. Check your key.");
                    case 404:
                        if (errorMessage != null && errorMessage.contains("city not found")) {
                            throw new WeatherApiException("City not found: " + cityName);
                        }
                        throw new WeatherApiException("City not found: " + cityName);
                    case 429:
                        throw new WeatherApiException("Request limit exceeded. Try again later.");
                    case 500:
                    case 502:
                    case 503:
                        throw new WeatherApiException("OpenWeather server error. Try again later.");
                    default:
                        throw new WeatherApiException("API error: " + response.statusCode() + 
                                " - " + (errorMessage != null ? errorMessage : "Unknown error"));
                }
            }
            
            WeatherData weatherData = objectMapper.readValue(response.body(), WeatherData.class);
            
            // Check that we received city data
            if (weatherData.getName() == null || weatherData.getName().isEmpty()) {
                throw new WeatherApiException("Failed to get city data: " + cityName);
            }
            
            return weatherData;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException("Request was interrupted: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new WeatherApiException("Error getting weather data: " + e.getMessage(), e);
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherApiException("Error getting weather data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Starts background thread for updating data in POLLING mode
     */
    private void startPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WeatherSdk-Polling");
            t.setDaemon(true);
            return t;
        });
        
    // Update data every 5 minutes (to keep data fresh)
        pollingExecutor.scheduleWithFixedDelay(
                this::updateAllCachedCities,
                0,
                5,
                TimeUnit.MINUTES
        );
    }
    
    /**
     * Updates data for all cities in cache
     */
    private void updateAllCachedCities() {
        if (!isRunning) {
            return;
        }
        
        List<String> citiesToUpdate;
        synchronized (cacheLock) {
            citiesToUpdate = new ArrayList<>(cache.keySet());
        }
        
        for (String normalizedCityName : citiesToUpdate) {
            if (!isRunning) {
                break;
            }
            
            try {
                // Get original city name
                String originalCityName;
                synchronized (cacheLock) {
                    originalCityName = originalCityNames.get(normalizedCityName);
                }
                
                if (originalCityName != null) {
                    WeatherData weatherData = fetchWeatherFromApi(originalCityName);
                    synchronized (cacheLock) {
                        cache.put(normalizedCityName, new WeatherCacheEntry(weatherData, Instant.now()));
                        accessOrder.put(normalizedCityName, Instant.now());
                    }
                }
            } catch (WeatherApiException e) {
                // Log error but continue updating other cities
                System.err.println("Error updating data for city " + normalizedCityName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Stops SDK and releases resources.
     * 
     * <p>Call this method when done with SDK, especially in POLLING mode,
     * to properly stop background update threads.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.POLLING);
     * // ... work with SDK ...
     * sdk.shutdown(); // Stop background threads
     * }</pre>
     * 
     * <p>Note: when using {@link #delete(String)}, shutdown() is called automatically.</p>
     */
    public void shutdown() {
        isRunning = false;
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Gets SDK operation mode.
     * 
     * @return SDK operation mode (ON_DEMAND or POLLING)
     */
    public SdkMode getMode() {
        return mode;
    }
    
    /**
     * Gets current number of cities in cache.
     * 
     * <p>Maximum number of cities in cache is limited by {@value #MAX_CACHE_SIZE}.</p>
     * 
     * @return number of cities in cache (from 0 to {@value #MAX_CACHE_SIZE})
     */
    public int getCacheSize() {
        synchronized (cacheLock) {
            return cache.size();
        }
    }
    
    // Registry pattern for managing SDK instances
    private static final Map<String, WeatherSdk> instances = new HashMap<>();
    private static final Object registryLock = new Object();
    
    /**
     * Creates or returns existing SDK instance for the specified API key.
     * 
     * <p>This method implements the Registry pattern, ensuring a single SDK instance
     * for each unique API key. If an SDK with this key already exists,
     * returns the existing instance (if the mode matches).</p>
     * 
     * <p><b>Note:</b> You cannot create two SDK instances with the same API key.
     * If you try to create an instance with an existing key but a different mode,
     * an exception will be thrown.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // First call - creates new instance
     * WeatherSdk sdk1 = WeatherSdk.create("api-key-123", SdkMode.ON_DEMAND);
     * 
     * // Second call with same key - returns existing instance
     * WeatherSdk sdk2 = WeatherSdk.create("api-key-123", SdkMode.ON_DEMAND);
     * // sdk1 == sdk2 (same object)
     * 
     * // Call with different key - creates new instance
     * WeatherSdk sdk3 = WeatherSdk.create("api-key-456", SdkMode.POLLING);
     * // sdk1 != sdk3 (different objects)
     * }</pre>
     * 
     * @param apiKey OpenWeather API key (get at https://openweathermap.org/api)
     * @param mode SDK operation mode ({@link SdkMode#ON_DEMAND} or {@link SdkMode#POLLING})
     * @return WeatherSdk instance (new or existing)
     * @throws WeatherApiException if:
     *   <ul>
     *     <li>API key is empty or null</li>
     *     <li>mode is null</li>
     *     <li>SDK with this key already exists with a different mode</li>
     *   </ul>
     */
    public static WeatherSdk create(String apiKey, SdkMode mode) throws WeatherApiException {
        synchronized (registryLock) {
            String normalizedKey = apiKey != null ? apiKey.trim() : null;
            if (normalizedKey == null || normalizedKey.isEmpty()) {
                throw new WeatherApiException("API ключ не может быть пустым");
            }
            
            WeatherSdk existing = instances.get(normalizedKey);
            if (existing != null) {
                // Если режим отличается, выбрасываем исключение
                if (existing.getMode() != mode) {
                    throw new WeatherApiException("SDK с таким API ключом уже существует с другим режимом работы");
                }
                return existing;
            }
            
            WeatherSdk newInstance = new WeatherSdk(normalizedKey, mode);
            instances.put(normalizedKey, newInstance);
            return newInstance;
        }
    }
    
    /**
     * Deletes SDK instance from registry and releases resources.
     * 
     * <p>This method removes the SDK instance from the internal registry and automatically
     * calls {@link #shutdown()} to release resources (especially important for POLLING mode).</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.POLLING);
     * // ... work with SDK ...
     * 
     * // Delete SDK and release resources
     * boolean deleted = WeatherSdk.delete("your-api-key");
     * if (deleted) {
     *     System.out.println("SDK deleted successfully");
     * }
     * }</pre>
     * 
     * @param apiKey API key for which to delete SDK
     * @return true if SDK was found and deleted; false if SDK with this key was not found
     */
    public static boolean delete(String apiKey) {
        synchronized (registryLock) {
            String normalizedKey = apiKey != null ? apiKey.trim() : null;
            if (normalizedKey == null || normalizedKey.isEmpty()) {
                return false;
            }
            
            WeatherSdk instance = instances.remove(normalizedKey);
            if (instance != null) {
                instance.shutdown();
                return true;
            }
            return false;
        }
    }
    
    /**
     * Gets existing SDK instance for the specified API key.
     * 
     * <p>Unlike {@link #create(String, SdkMode)}, this method does not create a new instance,
     * only returns an existing one. If SDK with this key is not found, returns null.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // Create SDK
     * WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND);
     * 
     * // Get existing instance
     * WeatherSdk sdk = WeatherSdk.get("your-api-key");
     * if (sdk != null) {
     *     WeatherData weather = sdk.getCurrentWeather("Moscow");
     * }
     * }</pre>
     * 
     * @param apiKey OpenWeather API key
     * @return WeatherSdk instance or null if SDK with this key is not found
     */
    public static WeatherSdk get(String apiKey) {
        synchronized (registryLock) {
            String normalizedKey = apiKey != null ? apiKey.trim() : null;
            return normalizedKey != null ? instances.get(normalizedKey) : null;
        }
    }
}

