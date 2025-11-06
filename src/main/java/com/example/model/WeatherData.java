
package com.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WeatherData model for OpenWeather SDK response.
 * Matches required JSON structure for SDK API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    @JsonProperty("weather")
    private Weather[] weather;

    @JsonProperty("main")
    private MainData temperature;

    @JsonProperty("visibility")
    private Integer visibility;

    @JsonProperty("wind")
    private Wind wind;

    @JsonProperty("dt")
    private Long datetime;

    @JsonProperty("sys")
    private Sys sys;

    @JsonProperty("timezone")
    private Integer timezone;

    @JsonProperty("name")
    private String name;

    // Getters
    public Weather[] getWeather() { return weather; }
    public MainData getTemperature() { return temperature; }
    public Integer getVisibility() { return visibility; }
    public Wind getWind() { return wind; }
    public Long getDatetime() { return datetime; }
    public Sys getSys() { return sys; }
    public Integer getTimezone() { return timezone; }
    public String getName() { return name; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        @JsonProperty("main")
        private String main;
        @JsonProperty("description")
        private String description;
        // Getters
        public String getMain() { return main; }
        public String getDescription() { return description; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MainData {
        @JsonProperty("temp")
        private Double temp;
        @JsonProperty("feels_like")
        private Double feelsLike;
        // Getters
        public Double getTemp() { return temp; }
        public Double getFeelsLike() { return feelsLike; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        @JsonProperty("speed")
        private Double speed;
        // Getters
        public Double getSpeed() { return speed; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        @JsonProperty("sunrise")
        private Long sunrise;
        @JsonProperty("sunset")
        private Long sunset;
        // Getters
        public Long getSunrise() { return sunrise; }
        public Long getSunset() { return sunset; }
    }
}