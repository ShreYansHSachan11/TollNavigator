# Toll Plaza Finder API Documentation

## Overview

The Toll Plaza Finder API provides a RESTful interface to discover toll plazas located between two Indian pincodes. The API integrates with external routing services to calculate the optimal route and identifies all toll plazas within proximity of that route.

## Base URL

- **Local Development:** `http://localhost:8080`
- **Production:** `https://api.tollplazafinder.com`

## Interactive API Documentation

Once the application is running, you can access interactive API documentation at:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

## Authentication

Currently, the API does not require authentication. Future versions may implement API key-based authentication.

## Endpoints

### POST /api/v1/toll-plazas

Finds all toll plazas between source and destination pincodes.

#### Request

**Headers:**
```
Content-Type: application/json
```

**Body Schema:**
```json
{
  "sourcePincode": "string (6 digits, required)",
  "destinationPincode": "string (6 digits, required, must differ from source)"
}
```

#### Response

**Success Response (200 OK):**
```json
{
  "route": {
    "sourcePincode": "string",
    "destinationPincode": "string",
    "distanceInKm": "number"
  },
  "tollPlazas": [
    {
      "name": "string",
      "latitude": "number",
      "longitude": "number",
      "distanceFromSource": "number"
    }
  ]
}
```

**Error Response Schema:**
```json
{
  "error": "string",
  "timestamp": "string (ISO 8601)",
  "path": "string",
  "status": "number"
}
```

## HTTP Status Codes

| Status Code | Description | When It Occurs |
|-------------|-------------|----------------|
| 200 | OK | Request processed successfully |
| 400 | Bad Request | Validation failed (invalid pincode format, same source/destination) |
| 500 | Internal Server Error | Unexpected error occurred during processing |
| 503 | Service Unavailable | External routing service is temporarily unavailable |

## Error Codes and Messages

### Validation Errors (400)

#### Invalid Pincode Format

**Error Message:** `"Invalid source or destination pincode"`

**Cause:** Pincode does not match the required format (6 numeric digits)

**Examples:**
- Pincode with letters: `"ABC123"`
- Pincode too short: `"12345"`
- Pincode too long: `"1234567"`
- Non-numeric characters: `"110-001"`
- Null or empty pincode

**Example Request:**
```json
{
  "sourcePincode": "12345",
  "destinationPincode": "560001"
}
```

**Example Response:**
```json
{
  "error": "Invalid source or destination pincode",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

#### Same Source and Destination

**Error Message:** `"Source and destination pincodes cannot be the same"`

**Cause:** Source pincode equals destination pincode

**Example Request:**
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "110001"
}
```

**Example Response:**
```json
{
  "error": "Source and destination pincodes cannot be the same",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

#### Missing Required Fields

**Error Message:** `"Source pincode is required"` or `"Destination pincode is required"`

**Cause:** Required field is missing from request body

**Example Request:**
```json
{
  "sourcePincode": "110001"
}
```

**Example Response:**
```json
{
  "error": "Destination pincode is required",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

### Service Errors (503)

#### Routing Service Unavailable

**Error Message:** `"Routing service temporarily unavailable"`

**Cause:** External routing service (Google Maps, Mappls) is not responding or returned an error

**Possible Reasons:**
- Network connectivity issues
- Routing service API is down
- API rate limit exceeded
- Invalid API key
- Request timeout

**Example Response:**
```json
{
  "error": "Routing service temporarily unavailable",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 503
}
```

**Resolution:**
- Wait and retry the request
- Check routing service status
- Verify API key configuration
- Check network connectivity

#### Invalid Pincode (Geocoding Failure)

**Error Message:** `"Unable to determine coordinates for pincode"`

**Cause:** Routing service could not geocode the provided pincode

**Possible Reasons:**
- Pincode does not exist in India
- Pincode is not recognized by the routing service
- Geocoding service error

**Example Response:**
```json
{
  "error": "Unable to determine coordinates for pincode",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 503
}
```

### Internal Errors (500)

#### Unexpected Error

**Error Message:** `"An unexpected error occurred"`

**Cause:** Unhandled exception during request processing

**Example Response:**
```json
{
  "error": "An unexpected error occurred",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 500
}
```

**Resolution:**
- Check application logs for detailed error information
- Report the issue to support team

#### Data Load Error

**Error Message:** `"Failed to load toll plaza data"`

**Cause:** CSV file containing toll plaza data is missing or corrupted

**Resolution:**
- Verify CSV file exists at configured path
- Check CSV file format
- Review application startup logs

## Example Scenarios

### Scenario 1: Successful Request with Toll Plazas

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001",
    "destinationPincode": "560001"
  }'
```

**Response (200 OK):**
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
    },
    {
      "name": "Mumbai-Pune Toll Plaza",
      "latitude": 18.9068,
      "longitude": 73.7249,
      "distanceFromSource": 1450.2
    }
  ]
}
```

### Scenario 2: Successful Request with No Toll Plazas

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001",
    "destinationPincode": "110002"
  }'
```

**Response (200 OK):**
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "110002",
    "distanceInKm": 5.2
  },
  "tollPlazas": []
}
```

### Scenario 3: Invalid Pincode Format

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "ABC123",
    "destinationPincode": "560001"
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "Invalid source or destination pincode",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

### Scenario 4: Same Source and Destination

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001",
    "destinationPincode": "110001"
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "Source and destination pincodes cannot be the same",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

### Scenario 5: Routing Service Unavailable

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001",
    "destinationPincode": "560001"
  }'
```

**Response (503 Service Unavailable):**
```json
{
  "error": "Routing service temporarily unavailable",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 503
}
```

### Scenario 6: Missing Required Field

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001"
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "Destination pincode is required",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

### Scenario 7: Malformed JSON

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePincode": "110001",
    "destinationPincode": "560001"
  '
```

**Response (400 Bad Request):**
```json
{
  "error": "Malformed JSON request",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/toll-plazas",
  "status": 400
}
```

## Rate Limiting

Currently, the API does not implement rate limiting. Future versions may include rate limiting to prevent abuse.

## Caching

The API implements response caching to improve performance:

- **Cache Key:** Combination of source and destination pincodes
- **Cache Duration:** 24 hours (configurable)
- **Cache Size:** 1000 entries (configurable)
- **Eviction Policy:** LRU (Least Recently Used)

Subsequent requests with the same source-destination pair will return cached results without calling the external routing service, resulting in faster response times.

## Best Practices

### Request Optimization

1. **Reuse Pincode Pairs:** If you need to query the same route multiple times, the second request will be served from cache
2. **Validate Locally:** Validate pincode format on the client side before making API calls
3. **Handle Errors Gracefully:** Implement retry logic for 503 errors with exponential backoff

### Error Handling

1. **Check Status Codes:** Always check HTTP status codes to determine success or failure
2. **Parse Error Messages:** Error responses include descriptive messages to help diagnose issues
3. **Log Errors:** Log error responses for debugging and monitoring
4. **Implement Retries:** For 503 errors, implement retry logic with delays

### Performance

1. **Cache Results:** If making multiple requests, cache results on the client side
2. **Batch Requests:** If you need multiple routes, consider making requests in parallel
3. **Monitor Response Times:** Track response times to identify performance issues

## Support and Contact

For API support, bug reports, or feature requests:

- **Email:** support@tollplazafinder.com
- **Documentation:** See README.md for setup and configuration
- **Swagger UI:** Access interactive documentation at `/swagger-ui.html`

## Changelog

### Version 1.0.0 (Current)

- Initial release
- POST /api/v1/toll-plazas endpoint
- Integration with external routing services
- Response caching
- Comprehensive error handling
- OpenAPI/Swagger documentation
