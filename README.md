🚀 SmartOrder — Event-Driven Delivery & Order Management System
Version Java Spring Boot Microservices Apache Kafka Last Updated

A production-grade backend system built with Java Spring Boot Microservices for managing online orders, inventory, payments, shipping, and real-time notifications using Event-Driven Architecture and Saga Pattern.

👨‍💻 Author
jad Akeel — Full-stack & Backend Developer

GitHub Email

This project was built from scratch to demonstrate real-world distributed systems engineering.

Table of Contents
Overview
Architecture
Microservices
Technologies
Saga Pattern Implementation
Project Structure
Setup Instructions
API Documentation
Testing
Contributing
Overview
This project implements a robust e-commerce system using a microservices architecture. The system handles order processing, inventory management, payment processing, shipping logistics, and customer notifications while maintaining data consistency across distributed services through the Saga pattern.

Architecture
The architecture follows the microservices pattern with the following components:


Microservices
Order Service:

Manages order creation and lifecycle
Initiates the order saga process
Tracks order status throughout the saga
Inventory Service:

Manages product inventory
Handles inventory reservation during order processing
Provides inventory availability checks
Payment Service:

Processes customer payments
Manages payment refunds for compensation transactions
Tracks payment status
Shipping Service:

Creates shipments for orders
Generates tracking information
Manages delivery status
Notification Service:

Sends notifications to customers
Supports multiple notification channels
Tracks notification delivery status
Infrastructure Services:

Service Registry: Service discovery with Eureka
API Gateway: Routing and cross-cutting concerns
Technologies
Java 17: Core programming language
Spring Boot 3.2.12: Application framework
Spring Cloud: Microservices toolkit
Apache Kafka: Event streaming platform for service communication
Databases:
PostgreSQL: For Order, Payment, and Shipping services
MongoDB: For Inventory and Notification services
Redis: For caching and temporary data storage
Docker & Docker Compose: Containerization and orchestration
Maven: Build and dependency management
Saga Pattern Implementation
This project implements the Saga pattern using a choreography-based approach:

Order Processing Flow:
Order Creation:

Customer places an order
Order service creates an order with PENDING status
Order service publishes OrderCreatedEvent
Inventory Reservation:

Inventory service consumes OrderCreatedEvent
Checks product availability
Reserves inventory if available
Publishes InventoryReservedEvent or InventoryReservationFailedEvent
Payment Processing:

Payment service consumes InventoryReservedEvent
Processes payment
Publishes PaymentProcessedEvent or PaymentFailedEvent
Shipping Creation:

Shipping service consumes PaymentProcessedEvent
Creates shipping record
Publishes ShipmentProcessedEvent or ShipmentFailedEvent
Order Completion:

Order service updates order status to COMPLETED
Compensation Transactions:
If any step fails, the system executes compensation transactions to maintain consistency:

Payment Failure: Inventory service releases reserved inventory
Shipping Failure: Payment service refunds payment, Inventory service releases inventory
Notification Service: Informs the customer about transaction status (success/failure)
Project Structure
ecommerce-microservices/
├── pom.xml                          # Parent POM
├── common-library/                  # Shared code between services
├── service-registry/                # Eureka Service Discovery
├── api-gateway/                     # Spring Cloud Gateway
├── order-service/                   # Order management
├── inventory-service/               # Inventory management
├── payment-service/                 # Payment processing
├── notification-service/            # Notification handling
├── shipping-service/                # Shipping management
└── docker-compose.yml               # Docker composition for all services
Setup Instructions
Prerequisites
Java 17
Maven 3.8+
Docker and Docker Compose
Kafka and ZooKeeper
PostgreSQL, MongoDB, Redis
Running the Application
Clone the repository:
git clone https://github.com/jadAkeel/smartOrder-microservecis.git
cd smartOrder-microservecis
Build the project:
mvn clean package -DskipTests
Start the infrastructure with Docker Compose:
docker-compose up -d
Check service health:
# Access Eureka dashboard
http://localhost:8761

# Check API Gateway
http://localhost:8080/actuator/health
API Documentation
Order Service
Create an Order
POST /api/orders
Content-Type: application/json

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
Get Order by ID
GET /api/orders/{orderId}
Get All Customer Orders
GET /api/orders/customer/{customerId}
Inventory Service
Create Inventory Item
POST /api/inventory
Content-Type: application/json

{
  "name": "Smartphone",
  "description": "Latest smartphone model",
  "quantity": 100
}
Check Product Availability
GET /api/inventory/check?productId={productId}&quantity={quantity}
Payment Service
Get Payment by Order ID
GET /api/payments/order/{orderId}
Shipping Service
Get Shipment by Order ID
GET /api/shipping/order/{orderId}
Notification Service
Get Customer Notifications
GET /api/notifications/customer/{customerId}
Testing
Unit Tests
mvn test
Integration Tests
mvn verify -P integration-test
End-to-End Testing
Use Postman or curl to test the full order processing flow:

Create an inventory item
Create an order
Check order status
Verify payment creation
Verify shipment creation
Verify notifications
Contributing
Fork the repository
Create your feature branch (git checkout -b feature/amazing-feature)
Commit your changes (git commit -m 'Add some amazing feature')
Push to the branch (git push origin feature/amazing-feature)
Create a new Pull Request
📬 Contact
GitHub: jadAkeel
Email: jadakeel05@gmail.com
Project: smartOrder-microservecis
Built with ❤️ by jad Akeel — 2026
