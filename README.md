<!--suppress HtmlDeprecatedAttribute -->
<h1 align="center">🏦 Nexus Ledger</h1>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Java-25-orange.svg?style=flat-square" alt="Java 25"></a>
  <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?style=flat-square" alt="Spring Boot"></a>
  <a href="#"><img src="https://img.shields.io/badge/Build-Gradle-blue.svg?style=flat-square" alt="Gradle"></a>
  <a href="#"><img src="https://img.shields.io/badge/Architecture-Event--Driven-ff69b4.svg?style=flat-square" alt="Architecture"></a>
</p>

<p align="center">
  <b>An enterprise-grade, distributed core banking engine engineered for high-availability, fault tolerance, and high-velocity data streaming.</b>
</p>

## 📖 Overview

**Nexus Ledger** is a comprehensive backend system designed to manage personal bank accounts and orchestrate financial transactions with strict ACID compliance. Engineered to showcase advanced backend architecture and SRE practices, this project bridges a robust, immutable relational core with a high-velocity edge pipeline for real-time telemetry, fraud detection, and analytics.

This repository serves as a testament to the capabilities of modern Java ecosystems, demonstrating how to seamlessly handle concurrent financial requests, mitigate distributed system bottlenecks, and maintain resilient infrastructure.

## ✨ Key Features

- **Double-Entry Bookkeeping**: Guarantees absolute financial consistency using PostgreSQL. Every transaction is strictly atomic - total credits always equal total debits.
- **High-Velocity Analytics Edge**: Offloads heavy analytical workloads to preserve the core DB. High-frequency transaction logs are ingested into **ScyllaDB**, asynchronously streamed via **Apache Kafka**, and indexed in **Elasticsearch** for sub-millisecond querying in **Kibana**.
- **Idempotency & Caching**: Prevents double-spending and race conditions during network retries by utilizing **Valkey (Redis)** for distributed locking and caching user sessions.
- **Cryptographic Security**: Implements rigorous authentication flows utilizing JSON Web Tokens (JWT) signed with the **ES512** algorithm (ECDSA using P-521 and SHA-512) for superior cryptographic resilience.
- **Contract-First API**: Leverages `openapi-generator` to automatically synthesize server controllers and DTOs from an OpenAPI 3.0 specification, ensuring strict adherence to API contracts.
- **Robust Observability**: Fully instrumented for an SRE environment, exposing metrics for Prometheus and Grafana integration.

## 🛠️ Technology Stack & Justification

### Core Backend
* **Java 25**: Leveraging the latest language features (pattern matching, virtual threads) for highly concurrent, boilerplate-free logic.
* **Spring Boot 3.x**: The enterprise standard for rapid, secure, and production-ready microservices.
* **Gradle**: Declarative, high-performance build automation.
* **Hibernate & Spring Data JPA**: ORM layer for seamless database interactions.
* **Lombok & MapStruct**: Drastically reduces boilerplate code and automates type-safe DTO-to-Entity mapping.
* **Validation**: Enforces strict payload constraints natively at the controller level.

### Authentication & API
* **JWT (ES512)**: Asymmetric cryptography ensures that token verification is highly secure and CPU-efficient.
* **OpenAPI Generator**: Establishes a single source of truth for the API design, accelerating frontend integration.

### Data Storage & Event Streaming
* **PostgreSQL (Core Ledger)**: The absolute source of truth. Provides rock-solid ACID compliance essential for financial ledgers.
* **Liquibase**: Automates database schema migrations, ensuring version-controlled infrastructure.
* **Valkey / Redis**: Acts as a lightning-fast caching layer for exchange rates, session states, and distributed idempotency keys.
* **ScyllaDB**: Acts as a high-throughput, low-latency edge datastore for temporary transaction buffering.
* **Apache Kafka**: The nervous system of the architecture, decoupling the core ledger from downstream analytical consumers.
* **ELK Stack (Elasticsearch, Kibana)**: Aggregates Kafka event streams, enabling comprehensive search capabilities and real-time dashboarding.

### DevOps & SRE
* **Docker & Docker Compose v2**: Containerizes the entire ecosystem, ensuring parity across local, staging, and production environments.
* **Testcontainers**: Spins up ephemeral Docker containers (Postgres, Kafka, Valkey) during the Gradle build phase for flawless integration testing.

## 🏗️ Architecture & Data Flow

1. **Client Request**: The client submits a transaction request. The API Gateway authenticates the user via ES512 JWT.
2. **Idempotency Check**: The application queries **Valkey** to ensure this specific transaction hash hasn't been processed previously.
3. **Core Processing**: The transaction is executed in **PostgreSQL** under a strict database lock (pessimistic locking) to prevent race conditions.
4. **Edge Ingestion**: A lightweight event representation is immediately persisted to **ScyllaDB** for fast client-side fetching.
5. **Event Streaming**: The event is published to a **Kafka** topic (`tx-events`).
6. **Analytics Indexing**: A consumer service reads the Kafka stream and indexes the enriched transaction data into **Elasticsearch**.
7. **Visualization**: System administrators and risk analysts monitor aggregate metrics and potential anomalies in real-time via **Kibana**.

## 🚀 Getting Started

### Prerequisites
- JDK 25
- Docker

### Quickstart

1. **Clone the repository**:

```shell
git clone https://github.com/SBER-SPRINGBOOT-FEB26/backend.git
cd backend
```

2. **Generate the OpenAPI sources**:

```shell
./gradlew openApiGenerate
```

3. **Spin up the infrastructure (DBs, Brokers, Caches)**:

```shell
docker compose up -d
```

*This will initialize PostgreSQL, Valkey, ScyllaDB, Kafka, Elasticsearch, and Kibana.*

4. **Build and run the application**:

```shell
./gradlew bootRun
```

## 📚 API Documentation

Once the application is running, the interactive API documentation (provided by Swagger UI) is accessible at:
`http://localhost:8080/swagger-ui.html`

The core entities exposed include:
- `User`: Registration, KYC status, and profile management.
- `Account`: Bank account creation, balance retrieval, and statement generation.
- `Transaction`: Intra-bank transfers, deposits, and external wire simulations.

## 📊 Observability

As an SRE-focused project, telemetry is treated as a first-class citizen.
- **Health Checks & Metrics**: Accessible via Spring Boot Actuator at `http://localhost:8080/actuator/prometheus`.
- **Kibana Dashboards**: Navigate to `http://localhost:5601` to view real-time transaction heatmaps and system logs.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/SBER-SPRINGBOOT-FEB26/whitebox/blob/backend/LICENSE.txt) file for details.