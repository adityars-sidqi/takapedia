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

- [ ] Bangun Product Catalog Service
- [ ] Sambungkan Product ke Auth lewat REST (komunikasi sinkron)
- [ ] Setup API Gateway
- [ ] Pasang routing di Gateway
- [ ] Pasang **Rate Limiter** di Gateway
- [ ] Validasi token dari Auth di setiap request

Saat pertama merasa "kok ribet" — itu memang pelajarannya.

---

## Fase 3 — Event-Driven pertama (Kafka masuk)

Target: jantung project. Service terhubung lewat event, bukan REST. Mulai dari SATU event saja.

- [ ] Bangun Order Service
- [ ] Bangun Inventory Service
- [ ] Setup Kafka (lewat Docker Compose)
- [ ] Buat producer di Order Service
- [ ] Buat consumer di Inventory Service
- [ ] Event pertama: `OrderCreated` -> Inventory kurangi stok
- [ ] (Opsional) Tulis Inventory pakai **Go** untuk merasakan perbandingannya

Pahami betul satu event ini sebelum menambah event lain.

---

## Fase 4 — CQRS + Search

Target: pisahkan write-model dan read-model di Product Service.

- [ ] Setup ElasticSearch / OpenSearch
- [ ] Pisahkan write-model (PostgreSQL) dan read-model (ElasticSearch)
- [ ] Sinkronkan read-model lewat event (Kafka dari Fase 3 dipakai ulang)
- [ ] Integrasi fitur search produk
- [ ] Uji konsistensi antara write dan read model

---

## Fase 5 — Resilience & ketahanan

Target: sistem tahan banting. Belajar dari kegagalan yang kamu buat sendiri.

- [ ] Pasang **Circuit Breaker** (Resilience4j)
- [ ] Tambah retry & timeout
- [ ] Caching dengan **Redis**
- [ ] Latihan: matikan satu service sengaja, lihat sistem rusak
- [ ] Perbaiki dengan circuit breaker, verifikasi sistem tetap jalan

---

## Fase 6 — Observability & polish

Target: lengkapi service tersisa + lapisan observasi.

- [ ] Bangun Payment Service (boleh di-mock dulu)
- [ ] Bangun Notification Service (kandidat bagus untuk **Go**)
- [ ] **Log Management** terpusat (ELK stack)
- [ ] Distributed tracing
- [ ] Refactor pakai **Design Pattern** yang muncul natural saat dibutuhkan

Design pattern paling baik dipelajari saat kamu merasa butuhnya, bukan dihafal di awal.

---

## Catatan Go vs Spring Boot

Pakai Spring Boot dulu untuk Fase 1–4 supaya fokus ke konsep arsitektur, bukan belajar dua bahasa sekaligus. Masukkan Go di Fase 3 atau 5 (Inventory atau Notification) saat sudah nyaman.

---

## Checklist konsep dari goals awal (untuk tracking)

- [ ] Spring Boot (Fase 1+)
- [ ] Go Lang (Fase 3 atau 5)
- [ ] Kafka (Fase 3)
- [ ] Redis (Fase 5)
- [ ] ElasticSearch / OpenSearch (Fase 4)
- [ ] OAuth2 / Session Management (Fase 1)
- [ ] Git (Fase 0+)
- [ ] Circuit Breaker (Fase 5)
- [ ] Rate Limiter (Fase 2)
- [ ] API versioning (Fase 1)
- [ ] Log Management (Fase 6)
- [ ] Design Pattern (Fase 6)
- [ ] Test-Driven Development (Fase 1+)
- [ ] Event-Driven (Fase 3)
- [ ] CQRS (Fase 4)
