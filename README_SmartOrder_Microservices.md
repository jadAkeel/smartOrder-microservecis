# SmartOrder — Event-Driven Delivery & Order Management System
## Overview

**SmartOrder** is a backend system built with **Java Spring Boot Microservices** for managing online orders, inventory, payments, delivery assignment, notifications, and order tracking.

The idea is inspired by platforms like **Talabat, Toters, Uber Eats, Amazon Delivery**, but implemented as a clean backend architecture using modern enterprise technologies.

This project is not just a simple CRUD application. It is designed as a real-world distributed system where each business domain is separated into an independent microservice.

---

## Project Idea

The user can browse products, place an order, pay for it, and track the delivery status.  
Behind the scenes, multiple microservices communicate together to complete the order flow.

### Basic Flow

```text
Customer places an order
        ↓
Order Service creates the order
        ↓
Inventory Service checks and reserves stock
        ↓
Payment Service processes payment
        ↓
Delivery Service assigns a driver
        ↓
Notification Service sends updates
        ↓
Customer tracks order status
```

---

## Main Goal

The main goal of this project is to build a powerful backend system that demonstrates:

- Microservices architecture
- Spring Boot backend development
- Service-to-service communication
- Event-driven architecture
- API Gateway
- Authentication and authorization
- Distributed database design
- Kafka or RabbitMQ messaging
- Dockerized services
- Fault tolerance and resilience
- Real-world business logic

---

## System Architecture

```text
                    ┌─────────────────────┐
                    │   Client / Frontend  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   API Gateway        │
                    │ Spring Cloud Gateway │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│ Auth Service  │      │ User Service  │      │ Product Svc   │
└───────────────┘      └───────────────┘      └───────────────┘
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│ Order Service │─────▶│ Inventory Svc │─────▶│ Payment Svc   │
└───────────────┘      └───────────────┘      └───────────────┘
        │                                             │
        ▼                                             ▼
┌───────────────┐                              ┌───────────────┐
│ Delivery Svc  │                              │ Notification  │
└───────────────┘                              └───────────────┘
        │                                             │
        └──────────────────┬──────────────────────────┘
                           ▼
                  ┌─────────────────┐
                  │ Kafka/RabbitMQ  │
                  │ Event Broker    │
                  └─────────────────┘
```

---

## Microservices

### 1. API Gateway

The API Gateway is the single entry point for all client requests.

Responsibilities:

- Route requests to the correct microservice
- Validate JWT tokens
- Handle CORS
- Centralize security rules
- Apply rate limiting
- Hide internal service URLs from the client

Example routes:

```text
/api/auth/**        → auth-service
/api/users/**       → user-service
/api/products/**    → product-service
/api/orders/**      → order-service
/api/payments/**    → payment-service
/api/delivery/**    → delivery-service
```

---

### 2. Auth Service

Responsible for authentication and authorization.

Features:

- Register user
- Login user
- Generate JWT token
- Validate JWT token
- Role-based access control

Roles:

```text
ADMIN
CUSTOMER
DRIVER
SUPPORT
```

Example APIs:

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/validate-token
```

---

### 3. User Service

Responsible for user profile management.

Features:

- Create user profile
- Update user information
- Get customer details
- Manage driver information
- Manage addresses

Example APIs:

```http
GET    /api/users/{id}
PUT    /api/users/{id}
POST   /api/users/{id}/addresses
GET    /api/users/drivers/available
```

---

### 4. Product Service

Responsible for products and categories.

Features:

- Add product
- Update product
- Delete product
- Search products
- Filter by category
- Manage prices

Example APIs:

```http
GET    /api/products
GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
```

Product example:

```json
{
  "id": 1,
  "name": "Chicken Burger",
  "description": "Grilled chicken burger with fries",
  "price": 7.99,
  "category": "Food"
}
```

---

### 5. Inventory Service

Responsible for stock management.

Features:

- Check product quantity
- Reserve stock when order is created
- Release stock if payment fails
- Reduce stock after successful payment

Important business rule:

> The system should reserve the product quantity before payment.  
> If payment fails, the reserved quantity must be released.

Example APIs:

```http
GET  /api/inventory/products/{productId}
POST /api/inventory/reserve
POST /api/inventory/release
POST /api/inventory/confirm
```

---

### 6. Order Service

The core service of the system.

Features:

- Create order
- Update order status
- Get order details
- Get customer orders
- Track order lifecycle
- Publish order events

Order statuses:

```text
CREATED
INVENTORY_RESERVED
PAYMENT_PENDING
PAID
DELIVERY_ASSIGNED
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
FAILED
```

Example APIs:

```http
POST /api/orders
GET  /api/orders/{id}
GET  /api/orders/customer/{customerId}
PUT  /api/orders/{id}/status
```

Order example:

```json
{
  "customerId": 10,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "deliveryAddress": "Beirut, Lebanon"
}
```

---

### 7. Payment Service

Responsible for payment processing.

Features:

- Process payment
- Simulate payment success or failure
- Store payment transactions
- Prevent duplicate payments
- Publish payment events

Example APIs:

```http
POST /api/payments/pay
GET  /api/payments/order/{orderId}
```

Payment statuses:

```text
PENDING
SUCCESS
FAILED
REFUNDED
```

Business rule:

> Payment requests must be idempotent.  
> If the same order is paid twice by mistake, the system should not create two successful payments.

---

### 8. Delivery Service

Responsible for assigning drivers and tracking delivery.

Features:

- Assign nearest available driver
- Update delivery status
- Track driver location
- Estimate delivery time
- Mark order as delivered

Example APIs:

```http
POST /api/delivery/assign
GET  /api/delivery/order/{orderId}
PUT  /api/delivery/{deliveryId}/status
PUT  /api/delivery/{deliveryId}/location
```

Delivery statuses:

```text
DRIVER_ASSIGNED
PICKED_UP
OUT_FOR_DELIVERY
DELIVERED
FAILED
```

---

### 9. Notification Service

Responsible for sending notifications to users.

Features:

- Email notifications
- SMS notifications
- Push notifications
- WebSocket real-time updates
- Notify customer when order status changes

Example notifications:

```text
Your order has been created.
Your payment was successful.
A driver has been assigned.
Your order is out for delivery.
Your order has been delivered.
```

Possible APIs:

```http
POST /api/notifications/send-email
POST /api/notifications/send-sms
POST /api/notifications/order-update
```

---

### 10. Analytics Service

Optional advanced service for reporting and statistics.

Features:

- Daily sales reports
- Most ordered products
- Number of cancelled orders
- Driver performance
- Customer order history analytics

Example APIs:

```http
GET /api/analytics/sales/daily
GET /api/analytics/products/top
GET /api/analytics/orders/status-summary
```

---

## Event-Driven Communication

The project uses an event broker such as **Kafka** or **RabbitMQ** for asynchronous communication between services.

### Why Events?

Events reduce direct dependencies between services.

For example, when an order is created, the Order Service does not need to directly call every other service.  
It can publish an event, and other services can react to it.

---

## Main Events

```text
OrderCreatedEvent
InventoryReservedEvent
InventoryReservationFailedEvent
PaymentRequestedEvent
PaymentSucceededEvent
PaymentFailedEvent
DeliveryAssignedEvent
DeliveryStatusUpdatedEvent
OrderDeliveredEvent
NotificationRequestedEvent
```

---

## Order Flow Using Events

```text
1. Customer creates order
2. Order Service saves order with CREATED status
3. Order Service publishes OrderCreatedEvent
4. Inventory Service consumes OrderCreatedEvent
5. Inventory Service reserves stock
6. Inventory Service publishes InventoryReservedEvent
7. Payment Service consumes InventoryReservedEvent
8. Payment Service processes payment
9. Payment Service publishes PaymentSucceededEvent
10. Delivery Service consumes PaymentSucceededEvent
11. Delivery Service assigns driver
12. Delivery Service publishes DeliveryAssignedEvent
13. Notification Service sends updates to customer
14. Order Service updates final order status
```

---

## Saga Pattern

Because the system is distributed, one transaction cannot easily cover all services.

For example:

```text
Order created
Inventory reserved
Payment failed
```

In this case, the system must undo the inventory reservation.

This is called a **compensating action**.

### Example Compensation Flow

```text
PaymentFailedEvent
        ↓
Inventory Service releases reserved quantity
        ↓
Order Service marks order as FAILED
        ↓
Notification Service informs customer
```

---

## Database Design

Each microservice should have its own database.

This is an important microservices rule:

> Database per service.

Example:

```text
auth_db
user_db
product_db
inventory_db
order_db
payment_db
delivery_db
notification_db
analytics_db
```

Recommended database:

```text
PostgreSQL
```

Optional:

```text
Redis for caching
MongoDB for logs or notifications
Elasticsearch for product search
```

---

## Example Tables

### users

```text
id
full_name
email
phone
role
created_at
updated_at
```

### products

```text
id
name
description
price
category_id
is_available
created_at
updated_at
```

### inventory

```text
id
product_id
available_quantity
reserved_quantity
updated_at
```

### orders

```text
id
customer_id
total_price
status
delivery_address
created_at
updated_at
```

### order_items

```text
id
order_id
product_id
quantity
price
```

### payments

```text
id
order_id
amount
status
payment_method
transaction_reference
created_at
```

### deliveries

```text
id
order_id
driver_id
status
current_location
estimated_arrival_time
created_at
updated_at
```

---

## Suggested Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 or Java 21 |
| Framework | Spring Boot 3 |
| Microservices | Spring Cloud |
| Gateway | Spring Cloud Gateway |
| Discovery | Eureka Server or Consul |
| Config | Spring Cloud Config |
| Security | Spring Security + JWT |
| Messaging | Kafka or RabbitMQ |
| Database | PostgreSQL |
| Cache | Redis |
| Communication | REST + OpenFeign |
| Resilience | Resilience4j |
| Documentation | Swagger / OpenAPI |
| Containers | Docker + Docker Compose |
| Monitoring | Prometheus + Grafana |
| Tracing | Zipkin / OpenTelemetry |
| CI/CD | GitHub Actions |

---

## Suggested Repository Structure

```text
smartorder-microservices/
│
├── api-gateway/
├── service-discovery/
├── config-server/
│
├── auth-service/
├── user-service/
├── product-service/
├── inventory-service/
├── order-service/
├── payment-service/
├── delivery-service/
├── notification-service/
├── analytics-service/
│
├── docker-compose.yml
├── README.md
└── docs/
    ├── architecture.md
    ├── api-docs.md
    └── database-design.md
```

---

## API Gateway Example

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
        - id: payment-service
          uri: lb://PAYMENT-SERVICE
          predicates:
            - Path=/api/payments/**
```

---

## Example Order Service Entity

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String deliveryAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## Example Order Status Enum

```java
public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAID,
    DELIVERY_ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    FAILED
}
```

---

## Example Event Class

```java
public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private LocalDateTime createdAt;
}
```

---

## Security

The system uses JWT-based authentication.

### Authentication Flow

```text
1. User registers or logs in
2. Auth Service validates credentials
3. Auth Service generates JWT token
4. Client sends token with every request
5. API Gateway validates token
6. Request is forwarded to the target service
```

### Example Header

```http
Authorization: Bearer <jwt-token>
```

---

## Service Communication

The system uses two types of communication:

### 1. Synchronous Communication

Used when an immediate response is required.

Example:

```text
Order Service → Product Service
Order Service → User Service
```

Technology:

```text
OpenFeign / REST Template / WebClient
```

### 2. Asynchronous Communication

Used for events and background processing.

Example:

```text
OrderCreatedEvent → Inventory Service
PaymentSucceededEvent → Delivery Service
```

Technology:

```text
Kafka / RabbitMQ
```

---

## Resilience and Fault Tolerance

The system should handle service failures.

Recommended patterns:

- Circuit Breaker
- Retry
- Timeout
- Fallback method
- Bulkhead
- Rate limiting

Example:

```text
If Payment Service is down:
- Order should not crash the whole system
- Order status becomes PAYMENT_PENDING or FAILED
- Customer receives a clear message
```

Technology:

```text
Resilience4j
```

---

## Docker Compose Services

The project can be started using Docker Compose.

Suggested containers:

```text
PostgreSQL
Kafka
Zookeeper
Redis
Eureka Server
Config Server
API Gateway
All microservices
Prometheus
Grafana
Zipkin
```

Example command:

```bash
docker-compose up -d
```

---

## How to Run the Project

### Prerequisites

Install:

```text
Java 17 or Java 21
Maven
Docker
Docker Compose
PostgreSQL
Git
```

### Clone Repository

```bash
git clone https://github.com/your-username/smartorder-microservices.git
cd smartorder-microservices
```

### Start Infrastructure

```bash
docker-compose up -d postgres kafka redis
```

### Start Service Discovery

```bash
cd service-discovery
mvn spring-boot:run
```

### Start Config Server

```bash
cd config-server
mvn spring-boot:run
```

### Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

### Start Microservices

```bash
cd auth-service
mvn spring-boot:run
```

Repeat the same for:

```text
user-service
product-service
inventory-service
order-service
payment-service
delivery-service
notification-service
```

---

## MVP Version

For the first version, build only these services:

```text
auth-service
product-service
inventory-service
order-service
notification-service
api-gateway
service-discovery
```

This version should support:

- User login
- Product listing
- Create order
- Reserve inventory
- Update order status
- Send notification

---

## Advanced Version

After completing the MVP, add:

```text
payment-service
delivery-service
analytics-service
config-server
Kafka
Redis
Prometheus
Grafana
Kubernetes
```

Advanced features:

- Payment simulation
- Driver assignment
- Real-time order tracking
- WebSocket notifications
- Distributed tracing
- Monitoring dashboards
- CI/CD pipeline
- Kubernetes deployment

---

## Main Business Rules

1. A customer cannot create an order without authentication.
2. A product must be available before it can be ordered.
3. Inventory quantity must be reserved before payment.
4. If payment fails, inventory reservation must be released.
5. If payment succeeds, order status becomes PAID.
6. After payment success, a driver should be assigned.
7. Customer should receive notifications for every important status update.
8. Admin can manage products and inventory.
9. Driver can only update assigned delivery orders.
10. Duplicate payment requests should not create duplicate transactions.

---

## Suggested Development Phases

### Phase 1 — Project Setup

- Create GitHub repository
- Create all service folders
- Setup Maven projects
- Setup Docker Compose
- Setup PostgreSQL databases

### Phase 2 — Core Services

- Auth Service
- User Service
- Product Service
- Inventory Service
- Order Service

### Phase 3 — Communication

- Add OpenFeign
- Add Kafka or RabbitMQ
- Create event classes
- Implement order flow

### Phase 4 — Security

- Add Spring Security
- Add JWT
- Secure API Gateway
- Add role-based permissions

### Phase 5 — Advanced Logic

- Add Payment Service
- Add Delivery Service
- Add Saga Pattern
- Add compensation logic

### Phase 6 — Monitoring and Deployment

- Add Docker
- Add Prometheus
- Add Grafana
- Add Zipkin
- Add GitHub Actions
- Prepare Kubernetes manifests

---

## Why This Project Is Strong

This project is strong because it shows real backend engineering skills:

- It is based on real business logic.
- It uses multiple microservices.
- It includes synchronous and asynchronous communication.
- It handles failure cases.
- It uses security and roles.
- It supports Docker and deployment.
- It can be explained easily in interviews.
- It is expandable and professional.

---

## Possible Future Features

- Real-time tracking map
- Coupon and discount service
- Wallet service
- Refund service
- Customer support chat
- Restaurant/vendor service
- Recommendation system
- AI-based delivery time prediction
- Multi-language support
- Admin dashboard
- Mobile app integration

---

## Project Title

Recommended final title:

```text
SmartOrder: Event-Driven Delivery & Order Management System using Java Spring Boot Microservices
```

---

## Short Description

```text
SmartOrder is a Java Spring Boot microservices backend system for managing online orders, inventory, payments, delivery, notifications, and analytics using event-driven architecture.
```

---

## Author

Created as a backend microservices project using:

```text
Java
Spring Boot
Spring Cloud
Kafka
PostgreSQL
Docker
JWT
Microservices Architecture
```
