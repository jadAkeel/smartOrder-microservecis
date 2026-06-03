# SmartOrder

SmartOrder is an event-driven order and delivery backend built with Java 17, Spring Boot, Kafka, and a microservices architecture. It models a real e-commerce workflow where orders move through authentication, product data, inventory reservation, payment processing, shipping, and notification services.

## Overview

SmartOrder was built to demonstrate backend engineering concepts that appear in production distributed systems: REST APIs, service discovery, API routing, event streaming, database integration, containerized infrastructure, and distributed transaction handling with a choreography-style Saga pattern.

The project is useful for backend developers, students, and reviewers who want to see how multiple Spring Boot services can coordinate an order lifecycle without sharing one monolithic database.

## Demo

There is no hosted live demo yet.

Demo coming soon. A good next demo would be a short Postman or screen-recorded walkthrough that:

1. Registers or logs in a user.
2. Creates products and inventory.
3. Creates an order through the API Gateway.
4. Shows Kafka-driven updates across inventory, payment, shipping, and notification services.
5. Checks the final order status.

You can run the local demo with Docker Compose using the installation steps below.

## Features

- Ten Maven modules covering shared code, infrastructure, and business services.
- Authentication service with JWT creation/validation and BCrypt password hashing.
- Spring Cloud Gateway for centralized API routing.
- Eureka service registry for service discovery.
- Product, order, inventory, payment, shipping, and notification services.
- Kafka event flow for order, inventory, payment, and shipping events.
- Choreography-style Saga handlers for distributed order processing and compensation.
- PostgreSQL persistence for auth, product, order, payment, and shipping data.
- MongoDB persistence for inventory and notification data.
- Redis container included for notification/cache-oriented infrastructure.
- Docker Compose environment for databases, Kafka, Zookeeper, gateway, registry, and services.
- Unit tests for service logic, authentication, JWT utilities, and application bootstrapping.
- Shared event and DTO library used across services.

## Tech Stack

- Backend: Java 17, Spring Boot 3.2.12, Spring MVC, Spring Validation
- Microservices: Spring Cloud Gateway, Spring Cloud Netflix Eureka
- Security: JWT, BCrypt, Spring Security Crypto
- Messaging: Apache Kafka, Spring Kafka
- Databases: PostgreSQL, MongoDB, Redis
- Build: Maven multi-module project
- Testing: JUnit, Spring Boot Test, Spring Kafka Test
- DevOps: Docker, Docker Compose
- Utilities: Lombok, MapStruct

## How It Works

The repository contains an `ecommerce-microservices` parent project. Each service is a separate Spring Boot module, while `common-library` contains shared event objects, DTOs, and JWT/Kafka helpers.

Typical order flow:

1. A client sends requests through the API Gateway.
2. The gateway routes requests to the relevant service and applies JWT filtering where needed.
3. The order service creates an order and publishes an `OrderCreatedEvent`.
4. The inventory service reserves stock and publishes success or failure events.
5. The payment service reacts to successful inventory reservation and publishes payment events.
6. The shipping service creates a shipment after successful payment.
7. The notification service listens to workflow events and stores notification records.
8. If a later step fails, Saga handlers publish or react to compensation events so previous work can be rolled back where possible.

## Installation

Prerequisites:

- Java 17+
- Maven 3.8+
- Docker and Docker Compose

```bash
git clone https://github.com/jadAkeel/smartOrder-microservecis.git
cd smartOrder-microservecis/ecommerce-microservices
mvn clean package -DskipTests
docker compose up --build
```

After the containers start:

- Eureka dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8080`
- Auth service: `http://localhost:8086`
- Order service: `http://localhost:8081`
- Product service: `http://localhost:8087`
- Inventory service: `http://localhost:8082`
- Payment service: `http://localhost:8083`
- Notification service: `http://localhost:8084`
- Shipping service: `http://localhost:8085`

## Usage

Create or authenticate a user through the auth service, then send API requests through the gateway or directly to individual services during development.

Example order creation endpoint:

```http
POST /api/orders
Content-Type: application/json
Authorization: Bearer <jwt-token>
```

```json
{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "items": [
    {
      "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "productName": "Smartphone",
      "quantity": 1,
      "price": 799.99
    }
  ]
}
```

Common useful endpoints include:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/products`
- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/payments/order/{orderId}`
- `GET /api/shipping/order/{orderId}`
- `GET /api/notifications/customer/{customerId}`

Exact request bodies may vary by controller DTO. Check each service's `controller` and `dto` packages for the current contract.

## Testing

Run tests from the Maven parent project:

```bash
cd ecommerce-microservices
mvn test
```

Build all modules without tests:

```bash
mvn clean package -DskipTests
```

## Project Structure

```text
ecommerce-microservices/
  common-library/        Shared events, DTOs, and utilities
  service-registry/      Eureka service discovery
  api-gateway/           Spring Cloud Gateway and JWT filter
  auth-service/          Registration, login, JWT creation, users
  product-service/       Product catalog APIs
  order-service/         Order creation and order Saga coordination
  inventory-service/     Inventory items, reservation, Saga handling
  payment-service/       Payment processing and compensation
  notification-service/  Notification storage and event handling
  shipping-service/      Shipment creation and delivery assignment
  docker-compose.yml     Local distributed environment
```

## Author

Jad Akil

- GitHub: [jadAkeel](https://github.com/jadAkeel)
- Email: [jadakeel05@gmail.com](mailto:jadakeel05@gmail.com)
