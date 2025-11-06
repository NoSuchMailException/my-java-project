package com.example.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataTest {

    private WeatherData weatherData;

    @BeforeEach
    void setUp() {
        weatherData = new WeatherData();
    }

    @Test
    void testName() {
        // No setter, so test default value
        assertNull(weatherData.getName());
    }

    @Test
    void testTemperature() {
        WeatherData.MainData temp = new WeatherData.MainData();
        // No setters, so test default value
        assertNull(temp.getTemp());
        assertNull(temp.getFeelsLike());
        // Attach to WeatherData
        // No setter, so test default value
        assertNull(weatherData.getTemperature());
    }

    @Test
    void testWeatherArray() {
        WeatherData.Weather[] arr = new WeatherData.Weather[1];
        arr[0] = new WeatherData.Weather();
        assertNull(arr[0].getMain());
        assertNull(arr[0].getDescription());
        // Attach to WeatherData
        assertNull(weatherData.getWeather());
    }

    @Test
    void testWind() {
        WeatherData.Wind wind = new WeatherData.Wind();
        assertNull(wind.getSpeed());
        assertNull(weatherData.getWind());
    }

    @Test
    void testSys() {
        WeatherData.Sys sys = new WeatherData.Sys();
        assertNull(sys.getSunrise());
        assertNull(sys.getSunset());
        assertNull(weatherData.getSys());
    }

    @Test
    void testVisibilityAndTimezoneAndDatetime() {
        assertNull(weatherData.getVisibility());
        assertNull(weatherData.getTimezone());
        assertNull(weatherData.getDatetime());
    }

    // Removed obsolete setter/getter tests for legacy model
}

