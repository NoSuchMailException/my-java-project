
# Weather SDK for Java

SDK for working with OpenWeather API, providing a convenient interface for retrieving weather data with automatic caching and support for two operation modes.

## 📋 Table of Contents

- [🚀 Features](#features)
- [📦 Requirements](#requirements)
- [🔧 Installation](#installation)
- [🎯 Quick Start](#quick-start)
- [📚 API Documentation](#api-documentation)
- [💡 Usage Examples](#usage-examples)
- [🏗️ Architecture](#architecture)
- [📝 Project Structure](#project-structure)
- [🔍 JavaDoc Generation](#javadoc-generation)
- [📄 License](#license)
- [🤝 Support](#support)

<a id="features"></a>
## 🚀 Features

- Get current weather by city name
- Automatic data caching (up to 10 cities, valid for 10 minutes)
- Two operation modes: **ON_DEMAND** and **POLLING**
- Thread safety
- Registry Pattern for SDK instance management
- Error handling with detailed messages
- LRU algorithm for cache management

<a id="requirements"></a>
## 📦 Requirements

- Java JDK 17 or higher
- Maven 3.6+
- OpenWeather API key (get it at [openweathermap.org](https://openweathermap.org/api))

<a id="installation"></a>
## 🔧 Installation

1. Clone the repository or download the project
2. Install dependencies:
```bash
mvn clean install
```

<a id="quick-start"></a>
## 🎯 Quick Start

### Basic Example

```java
import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;

public class Example {
    public static void main(String[] args) {
        try (WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND)) {
            // Get weather
            WeatherData weather = sdk.getCurrentWeather("Moscow");

            // Print info
            System.out.println("City: " + weather.getName());
            if (weather.getTemperature() != null) {
                System.out.println("Temperature: " + weather.getTemperature().getTemp() + "°C");
            }
            if (weather.getWeather() != null && weather.getWeather().length > 0) {
                System.out.println("Description: " + weather.getWeather()[0].getDescription());
            }
        } catch (WeatherApiException e) {
            System.err.println("API error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("SDK error: " + e.getMessage());
        }
    }
}
```


<a id="api-documentation"></a>
## 📚 API Documentation

### Main Classes

#### `WeatherSdk`

Main SDK class for working with OpenWeather API.

**Static methods:**

##### `create(String apiKey, SdkMode mode)`

Creates or returns an existing SDK instance for the specified API key.

**Parameters:**
- `apiKey` (String) - OpenWeather API key
- `mode` (SdkMode) - SDK operation mode (ON_DEMAND or POLLING)

**Returns:** WeatherSdk instance

**Exceptions:**
- `WeatherApiException` - if API key is empty, mode is null, or SDK with this key already exists with a different mode

**Example:**
```java
WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND);
```

##### `get(String apiKey)`

Gets an existing SDK instance for the specified API key.

**Parameters:**
- `apiKey` (String) - OpenWeather API key

**Returns:** WeatherSdk instance or null if not found

**Example:**
```java
WeatherSdk sdk = WeatherSdk.get("your-api-key");
if (sdk != null) {
    // Use SDK
}
```

##### `delete(String apiKey)`

Deletes the SDK instance from the registry and releases resources.

**Parameters:**
- `apiKey` (String) - API key for which to delete the SDK

**Returns:** true if SDK was found and deleted; false otherwise

**Example:**
```java
boolean deleted = WeatherSdk.delete("your-api-key");
```

**Public methods:**

##### `getCurrentWeather(String cityName)`

Gets current weather by city name.

**Parameters:**
- `cityName` (String) - city name (e.g., "Moscow", "Москва", "New York")

**Returns:** WeatherData object with weather info

**Exceptions:**
- `WeatherApiException` - if city name is empty, city not found, or an error occurs during request

**Example:**
```java
WeatherData weather = sdk.getCurrentWeather("Moscow");
```

##### `getMode()`

Gets SDK operation mode.

**Returns:** SDK operation mode (ON_DEMAND or POLLING)

##### `getCacheSize()`

Gets current number of cities in cache.

**Returns:** number of cities in cache (0 to 10)

##### `shutdown()`

Stops the SDK and releases resources. Important to call when finished, especially in POLLING mode.

#### `WeatherData`

Weather data model (`com.example.model`).

**Main fields:**
- `name` (String) - city name
- `temperature` (MainData) - temperature data
- `weather` (Weather[]) - weather description
- `wind` (Wind) - wind data
- `visibility` (Integer) - visibility in meters
- `datetime` (Long) - timestamp
- `sys` (Sys) - sunrise/sunset data
- `timezone` (Integer) - timezone

**Example usage:**
```java
WeatherData weather = sdk.getCurrentWeather("Moscow");
if (weather.getTemperature() != null) {
    System.out.println("Temperature: " + weather.getTemperature().getTemp());
    System.out.println("Feels like: " + weather.getTemperature().getFeelsLike());
}
if (weather.getWeather() != null && weather.getWeather().length > 0) {
    System.out.println("Description: " + weather.getWeather()[0].getDescription());
}
if (weather.getWind() != null) {
    System.out.println("Wind speed: " + weather.getWind().getSpeed() + " m/s");
}
```

#### `SdkMode`

Enumeration of SDK operation modes (`com.example.config`).

**Values:**
- `ON_DEMAND` - on-demand mode, updates data only on request
- `POLLING` - polling mode, automatic data update every 5 minutes

<a id="usage-examples"></a>
## 💡 Usage Examples

### Example 1: Basic Usage (ON_DEMAND)

```java
import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;

public class BasicExample {
    public static void main(String[] args) {
        try (WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND)) {
            // Get weather for multiple cities
            String[] cities = {"Moscow", "London", "New York"};
            for (String city : cities) {
                try {
                    WeatherData weather = sdk.getCurrentWeather(city);
                    System.out.println("\n=== " + weather.getName() + " ===");
                    if (weather.getTemperature() != null) {
                        System.out.println("Temperature: " + weather.getTemperature().getTemp() + "°C");
                    }
                    if (weather.getWeather() != null && weather.getWeather().length > 0) {
                        System.out.println("Description: " + weather.getWeather()[0].getDescription());
                    }
                    if (weather.getWind() != null) {
                        System.out.println("Wind: " + weather.getWind().getSpeed() + " m/s");
                    }
                } catch (WeatherApiException e) {
                    System.err.println("Error getting weather for " + city + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("SDK error: " + e.getMessage());
        }
    }
}
```

### Example 2: Using POLLING Mode

```java
import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;

public class PollingExample {
    public static void main(String[] args) {
        try (WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.POLLING)) {
            // Preload weather data into cache
            sdk.getCurrentWeather("Moscow");
            sdk.getCurrentWeather("London");
            sdk.getCurrentWeather("Paris");
            // In POLLING mode, data is automatically updated every 5 minutes
            // All subsequent requests will be instant (from cache)
            // Wait for a while...
            Thread.sleep(60000); // 1 minute
            // Get up-to-date data from cache (auto-updated)
            WeatherData weather = sdk.getCurrentWeather("Moscow");
            if (weather.getTemperature() != null) {
                System.out.println("Temperature in Moscow: " + weather.getTemperature().getTemp() + "°C");
            }
        } catch (WeatherApiException | InterruptedException e) {
            System.err.println("API error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("SDK error: " + e.getMessage());
        }
    }
}
```

### Example 3: Working with Multiple API Keys

```java
import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.model.WeatherData;
import com.example.exception.WeatherApiException;

public class MultipleKeysExample {
    public static void main(String[] args) {
        WeatherSdk sdk1 = null;
        WeatherSdk sdk2 = null;
        try {
            // Create SDKs with different API keys
            sdk1 = WeatherSdk.create("api-key-1", SdkMode.ON_DEMAND);
            sdk2 = WeatherSdk.create("api-key-2", SdkMode.POLLING);
            // Use first SDK
            WeatherData weather1 = sdk1.getCurrentWeather("Moscow");
            if (weather1.getTemperature() != null) {
                System.out.println("Temperature in Moscow: " + weather1.getTemperature().getTemp() + "°C");
            }
            // Use second SDK
            WeatherData weather2 = sdk2.getCurrentWeather("London");
            if (weather2.getTemperature() != null) {
                System.out.println("Temperature in London: " + weather2.getTemperature().getTemp() + "°C");
            }
            // Get existing instance
            WeatherSdk sdk1Again = WeatherSdk.get("api-key-1");
            // sdk1 == sdk1Again (same object)
        } catch (WeatherApiException e) {
            System.err.println("API error: " + e.getMessage());
        } finally {
            // Release resources
            if (sdk1 != null) {
                try {
                    sdk1.close();
                } catch (Exception e) {
                    System.err.println("Error closing sdk1: " + e.getMessage());
                }
            }
            if (sdk2 != null) {
                try {
                    sdk2.close();
                } catch (Exception e) {
                    System.err.println("Error closing sdk2: " + e.getMessage());
                }
            }
        }
    }
}
```

### Example 4: Error Handling

```java
import com.example.WeatherSdk;
import com.example.config.SdkMode;
import com.example.exception.WeatherApiException;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        try (WeatherSdk sdk = WeatherSdk.create("your-api-key", SdkMode.ON_DEMAND)) {
            // Try to get weather for a non-existent city
            try {
                WeatherData weather = sdk.getCurrentWeather("InvalidCityName12345");
            } catch (WeatherApiException e) {
                System.err.println("City not found: " + e.getMessage());
            }
            // Try to get weather with an empty city name
            try {
                WeatherData weather = sdk.getCurrentWeather("");
            } catch (WeatherApiException e) {
                System.err.println("Validation error: " + e.getMessage());
            }
        } catch (WeatherApiException e) {
            System.err.println("SDK creation error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("SDK error: " + e.getMessage());
        }
    }
}
```


<a id="architecture"></a>
## 🏗️ Architecture

### Package Structure

The project is organized into logical packages:

- **`com.example`** - main package with the core class `WeatherSdk`
- **`com.example.model`** - data models (WeatherData)
- **`com.example.cache`** - caching components (WeatherCacheEntry)
- **`com.example.exception`** - exceptions (WeatherApiException)
- **`com.example.config`** - configuration and enums (SdkMode, WeatherConfig)
- **`com.example.internal`** - internal components, not intended for client use (WeatherApiClient)
- **`com.example.example`** - SDK usage examples

### Components

1. **WeatherSdk** - main SDK class (`com.example` package)
2. **WeatherData** - weather data model (`com.example.model` package)
3. **WeatherCacheEntry** - cache entry with timestamp (`com.example.cache` package)
4. **SdkMode** - operation mode enum (`com.example.config` package)
5. **WeatherApiException** - custom exception (`com.example.exception` package)
6. **WeatherApiClient** - internal API client (`com.example.internal` package)

### Caching

- Maximum 10 cities in cache
- Data is valid for 10 minutes
- LRU (Least Recently Used) algorithm for removing old entries
- Thread-safe cache access

### Operation Modes

#### ON_DEMAND
- Data is updated only on client request
- If data is valid in cache, no API call is made
- Saves API requests

#### POLLING
- Automatic data update every 5 minutes
- Zero response latency
- Requires more API requests

### Registry Pattern

The SDK uses the Registry Pattern for instance management:
- Cannot create two SDK instances with the same API key
- Existing instance is returned when creating with the same key
- The `delete()` method releases resources and removes the instance from the registry


<a id="project-structure"></a>
## 📝 Project Structure

**Note:** The SDK is a library, not an application. No entry point is required.
For usage demonstration, see the `WeatherSdkExample` class with a `main()` method.

```
my-java-project/
├── pom.xml                          # Maven configuration
├── README.md                        # Documentation
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    ├── WeatherSdk.java          # Main SDK class
                    ├── model/                   # Data models
                    │   └── WeatherData.java     # Weather data model
                    ├── cache/                   # Caching
                    │   └── WeatherCacheEntry.java # Cache entry
                    ├── exception/               # Exceptions
                    │   └── WeatherApiException.java
                    ├── config/                  # Configuration and enums
                    │   ├── SdkMode.java         # SDK operation modes
                    │   └── WeatherConfig.java   # API configuration
                    ├── internal/                # Internal components (not for clients)
                    │   └── WeatherApiClient.java # API client
                    └── example/                 # Usage examples
                        └── WeatherSdkExample.java
```


<a id="javadoc-generation"></a>
## 🔍 JavaDoc Generation

To generate JavaDoc documentation:

```bash
mvn javadoc:javadoc
```

Documentation will be created in `target/site/apidocs/`


<a id="license"></a>
## 📄 License

This project is created for educational purposes.

<a id="support"></a>
## 🤝 Support

If you have any issues or questions, please create an issue in the project repository.
