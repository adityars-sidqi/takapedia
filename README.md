# Takapedia

> Aplikasi e-commerce berbasis **microservices** yang dibangun sebagai *learning project* mendalam untuk menguasai arsitektur sistem terdistribusi: Event-Driven Architecture, CQRS, dan Change Data Capture.

Prinsip yang dijaga sepanjang proyek: **satu service sampai matang, baru tambah service. Arsitektur sebelum kode. TDD: merah → hijau → commit.**

---

## Arsitektur

Takapedia terdiri dari beberapa service yang berkomunikasi lewat REST (sinkron) dan Kafka (asinkron, event-driven). Setiap service memiliki database sendiri (*database-per-service*) dan memvalidasi JWT secara mandiri (*zero-trust per service*).

```
                         ┌─────────────┐
            ┌───────────▶│ Auth Service│  (JWT RS256, register/login)
            │            └─────────────┘
            │
┌───────────────────┐    ┌──────────────────┐
│    API Gateway    │───▶│ Product Service  │  (write-model → PostgreSQL)
│ (routing, rate    │    └──────────────────┘
│  limiter, JWT     │              │ CDC (Debezium)
│  propagation)     │              ▼
└───────────────────┘    ┌──────────────────┐
            │            │  ElasticSearch   │  (read-model, search)
            │            └──────────────────┘
            │
            │            ┌──────────────────┐     OrderCreated      ┌──────────────────┐
            └───────────▶│  Order Service   │ ───── (Kafka) ──────▶ │ Inventory Service│
                         │  (publish event, │                       │  (Go, consumer,  │
                         │   consume        │                       │   idempotent)    │
                         │   PaymentDone)   │                       └──────────────────┘
                         └──────────────────┘
                            │        ▲
                 OrderCreated│        │PaymentCompleted
                    (Kafka)  ▼        │ (Kafka)
                         ┌──────────────────┐     charge (REST)     ┌──────────────────┐
                         │ Payment Service  │ ───── + Circuit ────▶ │  Midtrans (mock  │
                         │ (consume, charge,│      Breaker          │  via WireMock)   │
                         │  publish event)  │ ◀──── webhook ─────── │                  │
                         └──────────────────┘                       └──────────────────┘
```

### Pola yang digunakan

- **Database-per-service** — tiap service punya database terpisah dalam satu container PostgreSQL (`auth_db`, `product_db`, `order_db`, `inventory_db`, `payment_db`, `product_search_db`).
- **Event-Driven** — Order Service mem-*publish* `OrderCreated` ke Kafka; Inventory Service meng-*consume* dan mengurangi stok.
- **Idempotent consumer** — Inventory menjaga tabel `processed_events` dengan `order_id` sebagai primary key, sehingga event yang diproses berkali-kali (Kafka *at-least-once*) hanya berefek sekali.
- **CQRS + CDC** — Product Service menulis ke PostgreSQL (write-model); Debezium menangkap perubahan dari WAL dan menyinkronkannya ke ElasticSearch (read-model) lewat Kafka Connect, **tanpa kode aplikasi**.
- **Wire format JSON language-agnostic** — event Kafka berupa JSON polos agar dapat dikonsumsi lintas bahasa (Java ↔ Go).
- **Resilience (Circuit Breaker)** — Payment Service memanggil gateway pembayaran (Midtrans) lewat REST yang dilindungi Circuit Breaker (Resilience4j). Saat gateway gagal beruntun, circuit membuka (*OPEN*) dan panggilan berikutnya di-*block* via *fallback* — mencegah *cascading failure*. Circuit pulih otomatis (*HALF_OPEN → CLOSED*) saat gateway sehat kembali.
- **Split-transaction untuk network call** — panggilan HTTP ke gateway tidak dibungkus `@Transactional`, agar koneksi database tidak ditahan selama menunggu respons jaringan (mencegah *connection pool exhaustion*).
- **Choreography saga (dua event)** — `OrderCreated` (Order → Payment) dan `PaymentCompleted` (Payment → Order) membentuk alur pembayaran asinkron: order dibuat sebagai `WAITING_PAYMENT`, lalu berpindah ke `PAID`/`PAYMENT_FAILED` berdasarkan hasil pembayaran.

---

## Tech Stack

| Area | Teknologi |
|---|---|
| Bahasa | Java 25 (Amazon Corretto), Go |
| Framework | Spring Boot 4.1.0, Spring Security, Spring Cloud Gateway |
| Build | Maven (Java), Go Modules |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka 3.8.1 (KRaft mode) |
| Cache / Rate Limit | Redis 7 |
| Search | ElasticSearch 8.15 |
| CDC | Debezium (PostgreSQL connector) via Kafka Connect |
| Resilience | Resilience4j (Circuit Breaker) |
| Payment Gateway | Midtrans (disimulasikan via WireMock) |
| Auth | JWT RS256 (RSA key pair) |
| Container | Podman (kompatibel Docker Compose) |
| Testing (Java) | JUnit 5, Mockito, AssertJ, `@EmbeddedKafka`, WireMock, `MockRestServiceServer` |
| Testing (Go) | `testing`, Testcontainers |

---

## Struktur Repo (mono-repo)

```
Takapedia/
├── services/
│   ├── auth-service/         # Spring Boot — register, login, JWT
│   ├── product-service/      # Spring Boot — CRUD produk (write-model)
│   ├── api-gateway/          # Spring Cloud Gateway — routing, rate limiter
│   ├── order-service/        # Spring Boot — publish OrderCreated, consume PaymentCompleted
│   ├── payment-service/      # Spring Boot — consume OrderCreated, charge Midtrans (Circuit Breaker), publish PaymentCompleted
│   └── inventory-service/    # Go — consume OrderCreated, kurangi stok
├── infra/
│   ├── postgres/             # init.sql (multi-database)
│   ├── debezium/             # konfigurasi connector Debezium & ES sink
│   ├── connect-plugins/      # plugin Kafka Connect (TIDAK di-commit — lihat Setup)
│   └── wiremock/             # mappings/ — stub Midtrans untuk uji manual resilience
├── docs/
│   └── manual-resilience-testing.md   # playbook uji Circuit Breaker (matikan gateway → OPEN → recovery)
├── docker-compose.yml
├── TECH_DEBT.md
└── README.md
```

---

## Prasyarat

- **Podman** (atau Docker) + Compose
- **JDK 25** (Amazon Corretto) untuk service Java
- **Go** (1.26+) untuk Inventory Service
- **Maven** untuk build service Java

---

## Setup & Menjalankan

### 1. Infrastruktur (Podman Compose)

Jalankan PostgreSQL, Kafka, Redis, ElasticSearch, dan Kafka Connect:

```bash
podman compose up -d
```

Verifikasi:

```bash
# ElasticSearch
curl http://localhost:9200

# Kafka Connect
curl http://localhost:8083/

# Kafka UI tersedia di http://localhost:8090
```

### 2. Kunci JWT (RSA key pair)

> **Penting:** *private key* **tidak** disertakan di repo. Setiap pengembang membuat key pair-nya sendiri secara lokal. *Public key* boleh dibagi karena hanya dipakai untuk verifikasi token.

Generate RSA key pair untuk development dan testing:

```bash
# Private key (PKCS#8)
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048

# Public key
openssl rsa -pubout -in private_key.pem -out public_key.pem
```

Tempatkan key sesuai kebutuhan tiap service (lihat `src/main/resources/keys/` dan `src/test/resources/keys/`). File `*_private_key.pem` di-*ignore* oleh Git dan harus di-generate lokal — termasuk untuk CI.

### 3. Plugin Kafka Connect (ElasticSearch Sink)

Plugin connector tidak di-commit karena berukuran besar (binary). Download dan ekstrak secara lokal:

1. Unduh **Confluent ElasticSearch Sink Connector** (self-hosted) dari Confluent Hub.
2. Ekstrak ke `infra/connect-plugins/`.
3. Pastikan struktur folder berisi subfolder `lib/`.

`docker-compose.yml` me-*mount* folder ini ke container Kafka Connect via `CONNECT_PLUGIN_PATH`.

### 4. Daftarkan Connector Debezium & Sink

Setelah Kafka Connect berjalan:

```bash
# Debezium source (PostgreSQL → Kafka)
curl -X POST -H "Content-Type: application/json" \
  --data @infra/debezium/product-connector.json \
  http://localhost:8083/connectors

# ElasticSearch sink (Kafka → ElasticSearch)
curl -X POST -H "Content-Type: application/json" \
  --data @infra/debezium/elasticsearch-sink.json \
  http://localhost:8083/connectors
```

Verifikasi status connector:

```bash
curl http://localhost:8083/connectors/product-connector/status
curl http://localhost:8083/connectors/elasticsearch-sink/status
```

### 5. Jalankan Service

**Service Java (Spring Boot):**

```bash
cd services/auth-service
mvn spring-boot:run
# ulangi untuk product-service, api-gateway, order-service, payment-service
```

Port tiap service: Gateway `8080`, Auth `8081`, Product `8082`, Product-Search `8085`, Order `8086`, Payment `8087`. Kafka UI `8090`, WireMock (mock Midtrans) `8091`.

**Inventory Service (Go):**

```bash
cd services/inventory-service
go run ./cmd/inventory
```

---

## Menjalankan Test

**Service Java:**

```bash
cd services/<nama-service>
mvn test
```

**Inventory Service (Go) — membutuhkan Testcontainers:**

```bash
cd services/inventory-service
go test ./...
```

> Testcontainers membutuhkan container runtime aktif. Pada **Linux + Docker** umumnya terdeteksi otomatis. Pada **Windows + Podman** mungkin perlu mengeset `DOCKER_HOST` dan menonaktifkan Ryuk (`ryuk.disabled=true` di `~/.testcontainers.properties`).

---

## Status Progres

| Fase | Fokus | Status |
|---|---|---|
| 0 | Fondasi (mono-repo, Compose, PostgreSQL) | ✅ |
| 1 | Auth Service (Spring Boot, JWT, TDD) | ✅ |
| 2 | Product Service + API Gateway + Rate Limiter | ✅ |
| 3 | Event-Driven (Kafka, Order → Inventory, Go) | ✅ |
| 4 | CQRS + Search (Debezium CDC → ElasticSearch) | ✅ |
| 5a | Payment Service + Circuit Breaker (charge, fallback, PaymentCompleted) | ✅ |
| 5b | Webhook Midtrans + status VA (PENDING→PAID/EXPIRED) + polling endpoint | 🔄 dirancang |
| 5c | Saga (Inventory reserve→commit), reconciliation job, race handling | ⬜ |
| 6 | Observability (logging, tracing) + Notification/Delivery Service | ⬜ |

> **Catatan urutan:** Payment Service dibangun di Fase 5 (bukan Fase 6 seperti rencana roadmap awal) karena merupakan *use-case* paling natural untuk mempelajari Circuit Breaker — panggilan ke gateway pembayaran eksternal yang bisa gagal. Roadmap disesuaikan mengikuti kebutuhan belajar nyata.

---

## Catatan

Proyek ini dibangun untuk pembelajaran, dengan beberapa keputusan yang sengaja disederhanakan untuk fokus pada konsep arsitektur (mis. ElasticSearch security dimatikan untuk development, key test di-generate lokal, kontrak Midtrans disederhanakan namun tetap menyimulasikan signature webhook). Circuit Breaker pada Payment Service telah diverifikasi secara empiris lewat playbook uji manual (matikan gateway → circuit OPEN → recovery otomatis); lihat `docs/manual-resilience-testing.md`.

Peningkatan yang direncanakan (lihat `TECH_DEBT.md`): webhook Midtrans + status pembayaran berbasis VA (Fase 5b), reconciliation job + penanganan *race condition* (Fase 5c), Saga *reserve→commit* untuk Inventory, Transactional Outbox untuk publikasi event yang atomik dengan penulisan DB, migrasi JWT ke endpoint JWKS, serta centralized config.