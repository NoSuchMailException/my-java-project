package com.example.internal;

import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;
import com.example.config.WeatherConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client for OpenWeather API 3.0
 * 
 * <p><b>Internal class:</b> This class is intended for internal SDK use
 * and should not be used directly by library clients.</p>
 */
public class WeatherApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private static final String GEOCODING_URL = "http://api.openweathermap.org/geo/1.0/direct";
    // Use 2.5 endpoint for free API keys
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    /**
     * Creates a client with the specified API key
     * 
     * @param apiKey OpenWeather API key
     * @throws WeatherApiException if API key is not provided
     */
    public WeatherApiClient(String apiKey) throws WeatherApiException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new WeatherApiException("API key cannot be empty");
        }
        this.apiKey = apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Internal class for storing geocoding results from OpenWeather API
     * Used for JSON deserialization of the Geocoding API response
     * 
     * @see <a href="https://openweathermap.org/api/geocoding-api">Geocoding API docs</a>
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeocodingResult {
        /** City name from the API response */
        public String name;
        /** Latitude coordinate */
        public double lat;
        /** Longitude coordinate */
        public double lon;
        /** Country code (ISO 3166-1 alpha-2) from the API response */
        public String country;
    }
    /**
     * Gets coordinates for the specified city name using Geocoding API
     * @param cityName name of the city
     * @return GeocodingResult or null if not found
     * @throws WeatherApiException if error occurs
     */
    public GeocodingResult getCoordinatesByCityName(String cityName) throws WeatherApiException {
        return getCoordinates(cityName);
    }

    /**
     * Gets coordinates for the specified city name
                }
     * @return city coordinates
     * @throws WeatherApiException if city is not found or an error occurs
     */
    private GeocodingResult getCoordinates(String cityName) throws WeatherApiException {
        try {
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&limit=1&appid=%s", 
                GEOCODING_URL, encodedCity, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                GeocodingResult[] results = objectMapper.readValue(
                    response.body(), GeocodingResult[].class);
                
                if (results.length == 0) {
                    throw new WeatherApiException("City not found: " + cityName);
                }
                
                return results[0];
            } else {
                throw new WeatherApiException("API error while searching for city: " + 
                    response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new WeatherApiException("Error while getting coordinates: " + 
                e.getMessage());
        }
    }
    
    /**
     * Creates a client using API key from WeatherConfig
     * 
     * @throws WeatherApiException if API key is not set in WeatherConfig
     */
    public WeatherApiClient() throws WeatherApiException {
        if (!WeatherConfig.isApiKeySet()) {
            throw new WeatherApiException("API key is not set! " +
                    "Please add your API key to WeatherConfig.API_KEY");
        }
        this.apiKey = WeatherConfig.API_KEY.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gets current weather for the specified city
     * 
     * @param cityName name of the city (e.g., "Moscow" or "London")
     * @return WeatherData object containing weather information
     * @throws WeatherApiException if city is not found or an error occurs during the request
     */
    public WeatherData getCurrentWeather(String cityName) throws WeatherApiException {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new WeatherApiException("City name cannot be empty");
        }

        // First get city coordinates using Geocoding API
        GeocodingResult coords = getCoordinates(cityName);

        try {
            // Form URL for weather request using coordinates via Weather API 3.0
            String url = String.format("%s?lat=%.6f&lon=%.6f&appid=%s&units=metric&lang=ru",
                WEATHER_URL, coords.lat, coords.lon, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), WeatherData.class);
            } else if (response.statusCode() == 404) {
                throw new WeatherApiException("City not found: " + cityName);
            } else {
                throw new WeatherApiException("Error while getting weather: " +
                    response.statusCode() + " - " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException("Request was interrupted", e);
        } catch (IOException e) {
            throw new WeatherApiException("Error processing API response: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new WeatherApiException("Unexpected error while getting weather: " + e.getMessage(), e);
        }
    }

    /**
     * Gets current weather using coordinates
     *
     * @param lat latitude (from -90 to 90)
     * @param lon longitude (from -180 to 180)
     * @return WeatherData object containing weather information
     * @throws WeatherApiException if coordinates are invalid or an error occurs
     */
    public WeatherData getCurrentWeatherByCoordinates(double lat, double lon) throws WeatherApiException {
        if (lat < -90 || lat > 90) {
            throw new WeatherApiException("Invalid latitude. Must be between -90 and 90");
        }
        if (lon < -180 || lon > 180) {
            throw new WeatherApiException("Invalid longitude. Must be between -180 and 180");
        }
        
        try {
            String url = String.format("%s?lat=%.6f&lon=%.6f&appid=%s&units=metric&lang=ru",
                    WEATHER_URL,
                    lat,
                    lon,
                    apiKey);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), WeatherData.class);
            } else {
                throw new WeatherApiException("API error: " + response.statusCode() + 
                    " - " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException("Request was interrupted", e);
        } catch (IOException e) {
            throw new WeatherApiException("Error processing API response", e);
        } catch (Exception e) {
            throw new WeatherApiException("Unexpected error while requesting weather: " + e.getMessage(), e);
        }
    }
}