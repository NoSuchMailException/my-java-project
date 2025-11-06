package com.example.exception;

/**
 * Exception for handling errors when working with OpenWeather API.
 */
public class WeatherApiException extends Exception {
    
    public WeatherApiException(String message) {
        super(message);
    }
    
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

