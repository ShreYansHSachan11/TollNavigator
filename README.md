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

1. Set your API key:
```cmd
set ROUTING_API_KEY=your_mappls_api_key
```

2. Run:
```cmd
mvnw.cmd spring-boot:run
```

3. Test:
```cmd
curl -X POST http://localhost:8080/api/v1/toll-plazas ^
  -H "Content-Type: application/json" ^
  -d "{\"sourcePincode\":\"110001\",\"destinationPincode\":\"122001\"}"
```

## Build

```bash
mvnw.cmd clean install
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Required
routing.service.api-key=${ROUTING_API_KEY}

# Optional
routing.service.provider=mappls
routing.service.timeout=5000
tollplaza.route.proximity-threshold-km=2.0
```

Set environment variable:
```cmd
set ROUTING_API_KEY=your_api_key
```

## Running

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
    "destinationPincode": "122001",
    "distanceInKm": 30.5
  },
  "tollPlazas": [
    {
      "name": "Sample Toll Plaza",
      "latitude": 28.4595,
      "longitude": 77.0266,
      "distanceFromSource": 15.3
    }
  ]
}
```

Error responses: 400 (validation), 503 (service unavailable), 500 (server error)

## CSV Data Format

Location: `src/main/resources/data/toll-plazas.csv`

Format:
```csv
name,latitude,longitude
Sample Toll Plaza,28.4595,77.0266
Another Plaza,26.9124,75.7873
```

## Testing

```bash
mvnw.cmd test
```

## Troubleshooting

**CSV file not found:** Check `src/main/resources/data/toll-plazas.csv` exists

**Routing service errors:** Verify API key and internet connection

**Validation errors:** Ensure pincodes are 6 digits and different

**Slow responses:** First request hits the API, subsequent requests use cache

Enable debug logging:
```properties
logging.level.com.tollplaza=DEBUG
```

## Tech Stack

- Spring Boot 3.2.0
- Mappls API
- Caffeine Cache
- OpenCSV
- Lombok
