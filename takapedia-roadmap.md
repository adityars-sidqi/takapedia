# Roadmap Belajar — Takapedia

> Aplikasi E-commerce dengan arsitektur Microservices + CQRS
> Prinsip utama: **satu service dulu sampai matang, baru tambah service.** Jangan loncat fase.

---

## Dua hal yang menyebar di SEMUA fase

Ini kebiasaan, bukan fitur. Pakai terus dari awal sampai akhir.

- [x] **Git** — branching workflow, commit yang rapi (mulai Fase 0)
- [x] **TDD** — tulis test dulu, baru kode. Jangan ditunda (mulai Fase 1)

---

## Fase 0 — Fondasi (sebelum coding apa pun)

Target: bisa menjalankan satu container database lewat Docker Compose.

- [x] Pasang Git + tentukan branching workflow
- [x] Pasang Docker & Docker Compose
- [x] Tentukan struktur repo (mono-repo vs multi-repo)
- [x] Jalankan 1 container PostgreSQL lewat Docker Compose sebagai latihan
- [x] Verifikasi bisa connect ke database dari host

Belum sentuh Kafka atau microservices di fase ini.

---

## Fase 1 — Satu service utuh (Auth Service)

Target: kuasai siklus penuh membangun satu service Spring Boot. Belum ada Kafka, belum ada service kedua.

- [x] Setup project Spring Boot
- [x] Bikin REST API dasar
- [x] Terapkan API versioning sejak awal
- [x] OAuth2 / Session Management (Spring Security)
- [x] Registrasi & login
- [x] Terbitkan & validasi token
- [x] Koneksi ke PostgreSQL
- [x] **TDD: tulis test sebelum kode** (mulai kebiasaan ini sekarang)

Estimasi: 2–4 minggu. Wajar terasa lambat — ini investasi fondasi.

---

## Fase 2 — Service kedua + komunikasi sinkron

Target: dua service yang saling bicara lewat REST.

- [x] Bangun Product Catalog Service
- [x] Sambungkan Product ke Auth lewat REST (komunikasi sinkron)
- [x] Setup API Gateway
- [x] Pasang routing di Gateway
- [x] Pasang **Rate Limiter** di Gateway
- [x] Validasi token dari Auth di setiap request

Saat pertama merasa "kok ribet" — itu memang pelajarannya.

---

## Fase 3 — Event-Driven pertama (Kafka masuk)

Target: jantung project. Service terhubung lewat event, bukan REST. Mulai dari SATU event saja.

- [x] Bangun Order Service
- [x] Bangun Inventory Service
- [x] Setup Kafka (lewat Docker Compose)
- [x] Buat producer di Order Service
- [x] Buat consumer di Inventory Service
- [x] Event pertama: `OrderCreated` -> Inventory kurangi stok
- [x] (Opsional) Tulis Inventory pakai **Go** untuk merasakan perbandingannya

Pahami betul satu event ini sebelum menambah event lain.

---

## Fase 4 — CQRS + Search

Target: pisahkan write-model dan read-model di Product Service.

- [x] Setup ElasticSearch / OpenSearch
- [x] Pisahkan write-model (PostgreSQL) dan read-model (ElasticSearch)
- [x] Sinkronkan read-model lewat event (Kafka dari Fase 3 dipakai ulang)
- [x] Integrasi fitur search produk
- [x] Uji konsistensi antara write dan read model

---

## Fase 5 — Resilience & ketahanan (Payment Service)

Target: sistem tahan banting. Belajar dari kegagalan yang kamu buat sendiri.

> **Perubahan dari rencana awal:** Payment Service dipindah dari Fase 6 ke Fase 5. Alasannya, memanggil gateway pembayaran eksternal (Midtrans) adalah *use-case* paling natural untuk Circuit Breaker — ada dependency eksternal nyata yang bisa gagal. Belajar resilience jadi konkret, bukan buatan.

### Fase 5a — Circuit Breaker + alur pembayaran async ✅

- [x] Refactor Order: status `WAITING_PAYMENT`/`PAID`/`PAYMENT_FAILED`, field `paymentMethod` + `amount`
- [x] Payment Service: consume `OrderCreated`, idempotent via `processed_events` (PK `order_id`)
- [x] `MidtransClient` (RestClient) dengan connect/read timeout
- [x] Bedakan error 4xx (bug kita) vs 5xx/timeout (masalah gateway) untuk Circuit Breaker
- [x] Pasang **Circuit Breaker** (Resilience4j) + fallback, `ignoreExceptions` untuk 4xx
- [x] **Split-transaction**: HTTP call di luar `@Transactional` (jangan tahan koneksi DB saat network call)
- [x] Publish `PaymentCompleted`; Order Service consume → update status (idempotent via state Order)
- [x] WireMock sebagai mock Midtrans di Compose (port 8091)
- [x] **Latihan resilience**: matikan WireMock sengaja → circuit OPEN → hidupkan → recovery otomatis (playbook `docs/manual-resilience-testing.md`)

### Fase 5b — Webhook Midtrans + status VA (dirancang) 🔄

- [ ] Status Payment: `INITIATED → PENDING → PAID/EXPIRED/FAILED`
- [ ] Charge sukses → status `PENDING` + simpan nomor VA (belum publish `PaymentCompleted`)
- [ ] Endpoint webhook `POST /webhooks/midtrans` + verifikasi **signature SHA512**
- [ ] Idempotency webhook via state Payment (hanya proses saat `PENDING`)
- [x] Endpoint polling `GET /payments/{orderId}` (nomor VA) + JWT
- [x] Expiry via webhook `expire` → `PaymentCompleted(EXPIRED)` (Order map ke `OrderStatus.EXPIRED`)

### Fase 5c — Reconciliation & Saga (belum) ⬜

- [x] Scheduled job: scan Payment `PENDING` melewati batas → tandai kadaluarsa
- [x] Penanganan **race condition** (conditional update) webhook vs job
- [x] Saga: Inventory potong stok saat `OrderCreated` (VA dibuat), kembalikan saat `PaymentCompleted(FAILED/EXPIRED)` — compensating transaction (deduct-on-order)
- [x] Caching dengan **Redis** (cache-aside di endpoint polling `GET /payments/{orderId}`, TTL pendek + evict saat transisi status)

---

## Fase 6 — Observability & service pelengkap

Target: lengkapi service tersisa + lapisan observasi.

- [x] Bangun **Notification Service** (Go): consume `payment.completed` → notifikasi/invoice per outcome, idempotent
- [x] Bangun **Delivery Service** (consume `OrderPaid` → buat Shipment PENDING, idempotent)
- [ ] **Log Management** terpusat (ELK stack)
- [ ] Distributed tracing
- [ ] **Transactional Outbox** — publikasi event atomik dengan penulisan DB
- [ ] Refactor pakai **Design Pattern** yang muncul natural saat dibutuhkan

Design pattern paling baik dipelajari saat kamu merasa butuhnya, bukan dihafal di awal.

---

## Catatan Go vs Spring Boot

Pakai Spring Boot dulu untuk Fase 1–4 supaya fokus ke konsep arsitektur, bukan belajar dua bahasa sekaligus. Go sudah dipakai di Fase 3 (Inventory Service). Kandidat berikutnya: Notification Service (Fase 6).

---

## Checklist konsep dari goals awal (untuk tracking)

- [x] Spring Boot (Fase 1+)
- [x] Go Lang (Fase 3 — Inventory Service)
- [x] Kafka (Fase 3)
- [x] Redis (Fase 5c — caching payment polling; juga rate limiter di Fase 2)
- [x] ElasticSearch / OpenSearch (Fase 4)
- [x] OAuth2 / Session Management (Fase 1)
- [x] Git (Fase 0+)
- [x] Circuit Breaker (Fase 5a — Resilience4j)
- [x] Rate Limiter (Fase 2)
- [x] API versioning (Fase 1)
- [ ] Log Management (Fase 6)
- [ ] Design Pattern (Fase 6)
- [x] Test-Driven Development (Fase 1+)
- [x] Event-Driven (Fase 3)
- [x] CQRS (Fase 4)

### Konsep tambahan yang muncul saat jalan (tidak ada di goals awal)

- [x] Idempotent consumer (`processed_events` PK) — Fase 3 & 5a
- [x] Change Data Capture / Debezium — Fase 4
- [x] Webhook + signature verification — Fase 5b (dirancang)
- [x] Split-transaction (network call di luar `@Transactional`) — Fase 5a
- [ ] Transactional Outbox — Fase 6
- [x] Saga (deduct-on-order + compensate-on-failure) — Fase 5c
- [x] Reconciliation job + race condition handling — Fase 5c