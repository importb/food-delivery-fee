# Food Delivery Fee Calculator

A Spring Boot REST API that calculates delivery fees for food couriers based on the destination city, vehicle type, and real-time weather conditions.

This project was built as a technical assessment. It integrates with the external weather portal of the Estonian Environment Agency ([ilmateenistus.ee](https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php)) to fetch live weather data and applies a dynamic set of pricing rules to determine the final delivery cost.

## How It Works

The total delivery fee consists of a **Regional Base Fee** (which varies by city and vehicle type) plus **Extra Weather Fees**.

* **Base Fees:** Depend on the selected city (Tallinn, Tartu, Pärnu) and vehicle (Car, Scooter, Bike).
* **Air Temperature:** Adds a surcharge for scooters and bikes if the weather is freezing.
* **Wind Speed:** Adds a surcharge for bikes in high winds. If the wind exceeds 20 m/s, bikes are strictly forbidden.
* **Weather Phenomenon:** Adds surcharges for snow, sleet, or rain. Extreme conditions like glaze, hail, or thunderstorms completely forbid the use of two-wheeled vehicles.

Instead of hardcoding these rules, the application features a **Dynamic Rule Engine**. All pricing matrices and weather thresholds are stored in the database and can be modified via a REST API.

## Key Features

* **Automated Weather Import:** A configurable cron job runs periodically to fetch, parse, and store the latest XML weather observations.
* **Delivery Fee Calculation:** A REST endpoint computes the total fee based on the active rules and the latest weather data.
* **Dynamic Rule Management (CRUD):** Complete REST interface to add, update, or delete pricing and weather rules on the fly without changing code.
* **Historical Calculations:** Supports passing a specific datetime parameter to calculate what the fee *would have been* in the past, using historical weather data and the exact business rules that were active at that specific moment.

## Tech Stack

* **Java 17+**
* **Spring Boot 3** (Web, Data JPA, Validation)
* **H2 Database** (In-memory, for easy local testing)
* **Jackson XML** (For parsing the external weather API)
* **ShedLock** (For distributed cron job locking)
* **Swagger/OpenAPI** (For API documentation)

## Getting Started

You do not need a local Maven installation to run this project. Simply clone the repository and run the Maven wrapper:

```bash
./mvnw clean spring-boot:run
```

## API Documentation

Once the application is running, you can explore and test the endpoints using the interactive Swagger UI:
- **http://localhost:8080/swagger-ui/index.html**

### 1. Calculate Delivery Fee
**`GET /api/delivery-fee`**

**Parameters:**
* `city` (String, required): `Tallinn`, `Tartu`, or `Pärnu`
* `vehicleType` (String, required): `Car`, `Scooter`, or `Bike`
* `datetime` (String, optional): ISO-8601 timestamp (e.g., `2026-03-20T10:00:00Z`).

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/delivery-fee?city=Tallinn&vehicleType=Bike"
```
*Note: If weather conditions exceed safety limits, the API returns a `400 Bad Request` explaining that the vehicle type is forbidden.*

### 2. Manage Rules
**`GET, POST, PUT, DELETE /api/rules`**

Use these endpoints to inspect or modify the active pricing and weather rules.

## Architecture & Design Decisions

* **Rule Versioning for Historical Accuracy:** To support querying historical delivery fees, we need to know what the rules were in the past. When a rule is updated or deleted via the CRUD API, the existing database record is not overwritten. Instead, its `validTo` column is set to the current timestamp, and a new row is created. The calculation service always filters rules based on the requested `datetime`.
* **Idempotent Imports:** The `WeatherData` entity utilizes a unique constraint on `(stationName, observationTimestamp)`. This ensures that if the cron job is triggered multiple times for the same observation window, the database isn't polluted with duplicate records.
* **Distributed Locking (ShedLock):** In a real-world scenario with multiple instances running behind a load balancer, we want to prevent all nodes from hammering the external XML portal simultaneously. ShedLock is configured with a JDBC provider to ensure the scheduled task runs on only one instance at a time.
* **Layered Architecture:** The codebase strictly separates concerns: Controllers handle HTTP routing and validation, Services handle business logic and external API communication, and Repositories handle data access.

## Testing & Local Development

The project includes unit and integration tests covering the REST controllers and the core calculation logic. External dependencies (like the database and the weather API) are mocked in the service tests to ensure they run reliably.

To run the test suite:
```bash
./mvnw test
```

### Database Inspection
If you want to view the stored weather data or the active fee rules directly, you can access the H2 console while the app is running:
* **URL:** `http://localhost:8080/h2-console`
* **JDBC URL:** `jdbc:h2:mem:deliverydb`
* **Username:** `sa`
* **Password:** *(leave blank)*