# Toll Plaza Finder

A Spring Boot REST API that finds toll plazas between two Indian pincodes using Mappls routing service.

## Features

- Find toll plazas between any two Indian pincodes
- Mappls (MapmyIndia) integration for accurate Indian routes
- Response caching for better performance
- Input validation and error handling
- RESTful JSON API

## Prerequisites

- Java 17+
- Maven 3.8+ (or use included wrapper)
- Mappls API key

## Quick Start

1. Edit `run-with-mappls.bat` and set your Mappls API credentials:
```cmd
set MAPPLS_REST_API_KEY=your_rest_api_key
set MAPPLS_CLIENT_ID=your_client_id
set MAPPLS_CLIENT_SECRET=your_client_secret
```

2. Run:
```cmd
run-with-mappls.bat
```

3. Test:
```cmd
curl -X POST http://localhost:8080/api/v1/toll-plazas ^
  -H "Content-Type: application/json" ^
  -d "{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}"
```

## Build

```bash
mvnw.cmd clean install
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Mappls API Configuration
routing.service.provider=mappls
routing.service.timeout=10000
tollplaza.route.proximity-threshold-km=10.0

# Cache Configuration
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=1h
```

Set environment variables (or edit run-with-mappls.bat):
```cmd
set MAPPLS_REST_API_KEY=your_rest_api_key
set MAPPLS_CLIENT_ID=your_client_id
set MAPPLS_CLIENT_SECRET=your_client_secret
```

## Running

Using the provided script (recommended):
```bash
run-with-mappls.bat
```

Or manually:
```bash
mvnw.cmd spring-boot:run
```

Or build and run the JAR:
```bash
mvnw.cmd clean package
java -jar target/toll-plaza-finder-1.0.0.jar
```

App runs on `http://localhost:8080`

## API

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

### POST /api/v1/toll-plazas

Request:
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "122001"
}
```

Response:
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2126.16
  },
  "tollPlazas": [
    {
      "name": "IGI Toll Plaza",
      "latitude": 28.5436113,
      "longitude": 77.1155492,
      "distanceFromSource": 12.01
    }
  ]
}
```

Error responses: 400 (validation), 503 (service unavailable), 500 (server error)

## CSV Data Format

Location: `src/main/resources/data/toll-plazas.csv`

Format:
```csv
longitude,latitude,toll_name,geo_state
77.0266,28.4595,Sample Toll Plaza,Haryana
75.7873,26.9124,Another Plaza,Rajasthan
```

The application includes a comprehensive database of 2385+ toll plazas across India.

## Testing

```bash
mvnw.cmd test
```

## Troubleshooting

**CSV file not found:** Check `src/main/resources/data/toll-plazas.csv` exists

**Mappls API errors:** Verify your REST API key, Client ID, and Client Secret are correct

**Validation errors:** Ensure pincodes are 6 digits and different

**No toll plazas found:** The route may not pass within 10km of any toll plazas in the database

**Slow first request:** Initial request fetches OAuth token and route data; subsequent requests use cache

Enable debug logging in `application.properties`:
```properties
logging.level.com.tollplaza=DEBUG
```

## Tech Stack

- Spring Boot 3.2.0
- Mappls API
- Caffeine Cache
- OpenCSV
- Lombok
