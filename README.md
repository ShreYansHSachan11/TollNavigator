# Toll Plaza Finder

A Spring Boot REST API service that identifies toll plazas located between two Indian pincodes. The system integrates with external routing services to determine the route path, matches toll plazas from a CSV data source, and returns detailed information about each toll plaza along the journey.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [CSV Data Format](#csv-data-format)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

## Features

- Find all toll plazas between two Indian pincodes
- Integration with external routing services (Google Maps, Mappls/MapmyIndia)
- **Mappls integration configured by default** - ready to use with Indian pincodes
- Response caching for improved performance
- Comprehensive input validation
- Geographic distance calculations using Haversine formula
- RESTful API with JSON request/response
- Robust error handling and logging
- Easy switching between routing providers via configuration

## Prerequisites

- Java 17 or higher
- Maven 3.8+ (or use included Maven wrapper)
- External routing service API key:
  - **Mappls (MapmyIndia)** - Recommended for Indian routes (default)
  - **Google Maps** - Alternative option
- Internet connection for routing service integration

## Quick Start with Mappls

The application is pre-configured to use Mappls (MapmyIndia) routing service, which is optimized for Indian locations.

1. Set your Mappls API key:
```cmd
set ROUTING_API_KEY=your_mappls_api_key
```

2. Run the application:
```cmd
run-with-mappls.bat
```

3. Test the API:
```cmd
curl -X POST http://localhost:8080/api/v1/toll-plazas ^
  -H "Content-Type: application/json" ^
  -d "{\"sourcePincode\":\"110001\",\"destinationPincode\":\"122001\"}"
```

For detailed Mappls testing instructions, see [MAPPLS_TESTING_GUIDE.md](MAPPLS_TESTING_GUIDE.md).

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd toll-plaza-finder
```

2. Build the project:
```bash
# Windows
mvnw.cmd clean install

# Linux/Mac
./mvnw clean install
```

## Configuration

### Application Properties

Configure the application by editing `src/main/resources/application.properties` or by setting environment variables.

#### Required Configuration

**Routing Service API Key:**
```properties
routing.service.api-key=${ROUTING_API_KEY}
```

Set the environment variable:
```bash
# Windows
set ROUTING_API_KEY=your_api_key_here

# Linux/Mac
export ROUTING_API_KEY=your_api_key_here
```

#### Optional Configuration

**Server Configuration:**
```properties
server.port=8080
server.servlet.context-path=/
```

**Routing Service:**
```properties
# Use 'mappls' for Mappls/MapmyIndia (default, recommended for India)
# Use 'google-maps' for Google Maps
routing.service.provider=mappls
routing.service.timeout=5000
routing.service.retry.max-attempts=3
```

**Toll Plaza Configuration:**
```properties
tollplaza.csv.path=classpath:data/toll-plazas.csv
tollplaza.route.proximity-threshold-km=2.0
```

**Cache Configuration:**
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=24h
```

**Logging Configuration:**
```properties
logging.level.root=INFO
logging.level.com.tollplaza=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Configuration Properties Reference

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `routing.service.api-key` | API key for external routing service | - | Yes |
| `routing.service.provider` | Routing service provider (mappls, google-maps) | mappls | No |
| `routing.service.timeout` | Request timeout in milliseconds | 5000 | No |
| `routing.service.retry.max-attempts` | Maximum retry attempts for failed requests | 3 | No |
| `tollplaza.csv.path` | Path to toll plaza CSV file | classpath:data/toll-plazas.csv | No |
| `tollplaza.route.proximity-threshold-km` | Distance threshold for route matching (km) | 2.0 | No |
| `spring.cache.caffeine.spec` | Caffeine cache specification | maximumSize=1000,expireAfterWrite=24h | No |

## Running the Application

### Using Maven Wrapper (Recommended)

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Using Maven

```bash
mvn spring-boot:run
```

### Running with Custom Configuration

```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--routing.service.api-key=YOUR_API_KEY --tollplaza.csv.path=C:/data/toll-plazas.csv"

# Linux/Mac
./mvnw spring-boot:run -Dspring-boot.run.arguments="--routing.service.api-key=YOUR_API_KEY --tollplaza.csv.path=/data/toll-plazas.csv"
```

### Running the JAR

```bash
# Build the JAR
mvnw.cmd clean package

# Run the JAR
java -jar target/toll-plaza-finder-1.0.0.jar

# Run with custom properties
java -jar target/toll-plaza-finder-1.0.0.jar --routing.service.api-key=YOUR_API_KEY
```

The application will start on `http://localhost:8080` by default.

## API Documentation

### Interactive Documentation (Swagger UI)

Once the application is running, access the interactive API documentation at:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec (JSON):** `http://localhost:8080/v3/api-docs`

The Swagger UI provides an interactive interface to explore and test the API endpoints directly from your browser.

### Detailed API Documentation

For comprehensive API documentation including all error codes, example scenarios, and best practices, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

### Endpoint: Find Toll Plazas

**URL:** `POST /api/v1/toll-plazas`

**Description:** Identifies all toll plazas between two Indian pincodes.

#### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "560001"
}
```

**Request Fields:**

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| `sourcePincode` | String | Yes | Source pincode (6 digits) | Must be exactly 6 numeric digits |
| `destinationPincode` | String | Yes | Destination pincode (6 digits) | Must be exactly 6 numeric digits, must differ from source |

#### Success Response

**Status Code:** `200 OK`

**Body:**
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2100.5
  },
  "tollPlazas": [
    {
      "name": "Delhi-Gurgaon Toll Plaza",
      "latitude": 28.4595,
      "longitude": 77.0266,
      "distanceFromSource": 15.3
    },
    {
      "name": "Jaipur Toll Plaza",
      "latitude": 26.9124,
      "longitude": 75.7873,
      "distanceFromSource": 245.8
    }
  ]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `route.sourcePincode` | String | Source pincode from request |
| `route.destinationPincode` | String | Destination pincode from request |
| `route.distanceInKm` | Number | Total route distance in kilometers |
| `tollPlazas` | Array | List of toll plazas on the route (ordered by distance from source) |
| `tollPlazas[].name` | String | Name of the toll plaza |
| `tollPlazas[].latitude` | Number | Latitude coordinate of toll plaza |
| `tollPlazas[].longitude` | Number | Longitude coordinate of toll plaza |
| `tollPlazas[].distanceFromSource` | Number | Distance from source pincode in kilometers |

#### Error Responses

**Validation Error (400 Bad Request):**
```json
{
  "error": "Invalid source or destination pincode",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

**Same Pincode Error (400 Bad Request):**
```json
{
  "error": "Source and destination pincodes cannot be the same",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

**Routing Service Error (503 Service Unavailable):**
```json
{
  "error": "Routing service temporarily unavailable",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 503
}
```

**Internal Server Error (500 Internal Server Error):**
```json
{
  "error": "An unexpected error occurred",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 500
}
```

### Example Requests

#### Using cURL

**Successful Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d "{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}"
```

**Invalid Pincode:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d "{\"sourcePincode\":\"12345\",\"destinationPincode\":\"560001\"}"
```

**Same Pincode:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d "{\"sourcePincode\":\"110001\",\"destinationPincode\":\"110001\"}"
```

#### Using PowerShell

```powershell
$body = @{
    sourcePincode = "110001"
    destinationPincode = "560001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/toll-plazas" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

#### Using Postman

1. Create a new POST request
2. Set URL to: `http://localhost:8080/api/v1/toll-plazas`
3. Set Headers: `Content-Type: application/json`
4. Set Body (raw JSON):
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "560001"
}
```
5. Click Send

## CSV Data Format

The application loads toll plaza data from a CSV file. The CSV file must follow this format:

### File Location

Default: `src/main/resources/data/toll-plazas.csv`

### Format Specification

**Header Row (Required):**
```csv
name,latitude,longitude
```

**Data Rows:**
```csv
Delhi-Gurgaon Toll Plaza,28.4595,77.0266
Jaipur Toll Plaza,26.9124,75.7873
Mumbai-Pune Toll Plaza,18.9068,73.7249
```

### Field Specifications

| Field | Type | Required | Description | Validation |
|-------|------|----------|-------------|------------|
| `name` | String | Yes | Name of the toll plaza | Non-empty string |
| `latitude` | Number | Yes | Latitude coordinate | Valid latitude (-90 to 90) |
| `longitude` | Number | Yes | Longitude coordinate | Valid longitude (-180 to 180) |

### CSV File Requirements

- File must be UTF-8 encoded
- First row must be the header row
- Fields must be comma-separated
- Malformed records will be skipped with a warning logged
- Empty lines are ignored
- At least one valid toll plaza record is required

### Example CSV File

```csv
name,latitude,longitude
Delhi-Gurgaon Toll Plaza,28.4595,77.0266
Jaipur Toll Plaza,26.9124,75.7873
Mumbai-Pune Toll Plaza,18.9068,73.7249
Bangalore-Mysore Toll Plaza,12.9716,77.5946
Chennai-Bangalore Toll Plaza,13.0827,80.2707
```

## Testing

### Run All Tests

```bash
# Windows
mvnw.cmd test

# Linux/Mac
./mvnw test
```

### Run Specific Test Class

```bash
mvnw.cmd test -Dtest=TollPlazaServiceTest
```

### Run with Coverage Report

```bash
mvnw.cmd test jacoco:report
```

Coverage report will be generated at: `target/site/jacoco/index.html`

### Test Categories

- **Unit Tests:** Test individual components in isolation
- **Integration Tests:** Test end-to-end API behavior with MockMvc
- **Property-Based Tests:** Test universal properties with jqwik (100+ iterations)

## Troubleshooting

### Application Fails to Start

**Problem:** Application fails with "DataLoadException: CSV file not found"

**Solution:**
- Verify the CSV file exists at the configured path
- Check the `tollplaza.csv.path` property in `application.properties`
- Ensure the file is in the correct location (default: `src/main/resources/data/toll-plazas.csv`)

**Problem:** Application fails with "Failed to load toll plaza data"

**Solution:**
- Verify the CSV file format matches the specification
- Check for malformed records in the CSV file
- Review application logs for specific parsing errors

### Routing Service Errors

**Problem:** "Routing service temporarily unavailable" error

**Solution:**
- Verify your routing service API key is correct
- Check your internet connection
- Verify the routing service is operational
- Check if you've exceeded API rate limits
- Review the `routing.service.timeout` setting (increase if needed)

**Problem:** "Invalid pincode" error from routing service

**Solution:**
- Verify the pincode exists and is valid in India
- Some pincodes may not be recognized by the routing service
- Try with a different pincode to verify the service is working

### Validation Errors

**Problem:** "Invalid source or destination pincode" error

**Solution:**
- Ensure pincodes are exactly 6 digits
- Ensure pincodes contain only numeric characters
- Remove any spaces or special characters

**Problem:** "Source and destination pincodes cannot be the same"

**Solution:**
- Verify you're using different pincodes for source and destination

### Performance Issues

**Problem:** Slow response times

**Solution:**
- Check if the routing service is responding slowly
- Verify cache is enabled and working (second request should be faster)
- Review the `routing.service.timeout` setting
- Check network latency to the routing service

**Problem:** Cache not working

**Solution:**
- Verify `@EnableCaching` is present in `CacheConfiguration`
- Check cache configuration in `application.properties`
- Review logs for cache-related errors
- Ensure Caffeine dependency is included

### Logging and Debugging

**Enable Debug Logging:**
```properties
logging.level.com.tollplaza=DEBUG
```

**View Detailed Request/Response:**
```properties
logging.level.org.springframework.web=DEBUG
```

**Check Application Logs:**
- INFO: Successful operations
- WARN: Expected errors (validation failures, routing service issues)
- ERROR: Unexpected errors with stack traces

## Project Structure

```
toll-plaza-finder/
├── src/
│   ├── main/
│   │   ├── java/com/tollplaza/
│   │   │   ├── config/
│   │   │   │   └── CacheConfiguration.java
│   │   │   ├── controller/
│   │   │   │   └── TollPlazaController.java
│   │   │   ├── exception/
│   │   │   │   ├── DataLoadException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidPincodeException.java
│   │   │   │   └── RoutingServiceException.java
│   │   │   ├── model/
│   │   │   │   ├── Coordinates.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── Route.java
│   │   │   │   ├── TollPlaza.java
│   │   │   │   ├── TollPlazaMatch.java
│   │   │   │   ├── TollPlazaRequest.java
│   │   │   │   └── TollPlazaResponse.java
│   │   │   ├── repository/
│   │   │   │   └── TollPlazaRepository.java
│   │   │   ├── service/
│   │   │   │   ├── GoogleMapsRoutingService.java
│   │   │   │   ├── RouteMatcherService.java
│   │   │   │   ├── RoutingService.java
│   │   │   │   └── TollPlazaService.java
│   │   │   ├── util/
│   │   │   │   ├── CsvDataLoader.java
│   │   │   │   └── GeoUtils.java
│   │   │   └── TollPlazaFinderApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data/
│   │           └── toll-plazas.csv
│   └── test/
│       ├── java/com/tollplaza/
│       │   ├── integration/
│       │   │   └── TollPlazaIntegrationTest.java
│       │   ├── repository/
│       │   │   └── TollPlazaRepositoryTest.java
│       │   ├── service/
│       │   │   ├── GoogleMapsRoutingServiceTest.java
│       │   │   └── RouteMatcherServiceTest.java
│       │   ├── util/
│       │   │   └── GeoUtilsTest.java
│       │   └── TollPlazaFinderApplicationTests.java
│       └── resources/
│           ├── application-test.properties
│           └── data/
│               └── test-toll-plazas.csv
├── pom.xml
└── README.md
```

## Dependencies

- **Spring Boot 3.2.0:** Core framework
- **Spring Web:** REST API support
- **Spring Cache:** Caching abstraction
- **Spring Validation:** Input validation
- **Caffeine:** High-performance caching library
- **OpenCSV:** CSV parsing
- **Lombok:** Boilerplate code reduction
- **jqwik:** Property-based testing framework

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]

## Support

For issues or questions, please [add contact information or issue tracker link]
