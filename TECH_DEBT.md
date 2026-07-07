# Tech Debt — Takapedia

> Daftar utang teknis yang sengaja ditunda, dengan konteks dan rencana penyelesaian.
> Dicatat eksplisit agar keputusan penyederhanaan tidak terlupakan.

---

## Test broken across services after JWKS migration
- Discovered: 2026-07-02, during Fase 5a Commit A refactor
- Root cause: application-test.yml override datasource+jpa tapi lupa
  provide mock untuk spring.security.oauth2.resourceserver.jwt.jwk-set-uri
- Workaround saat ini: mock jwk-set-uri di application-test.yml per service
- Proper fix: bikin shared test utility (mungkin TestSecurityConfig common)
- Services affected: Order Service (confirmed), Auth/Product/Product-Search (unverified)
- Action: setelah Fase 5a selesai, audit dan fix systematically

## Inventory Service tidak track perubahan kontrak OrderCreatedEvent
- OrderCreatedEvent struct di Go masih versi lama (belum ada paymentMethod, amount)
- Aman karena Go JSON ignore unknown fields, dan Inventory tidak butuh field baru
- Risk: kontrak event jadi implisit, mudah drift
- Action: pertimbangkan versioning schema (Confluent Schema Registry) atau at minimum, dokumentasi kontrak event terpusat

## Publisher failure setelah DB commit di PaymentService
- Skenario: Payment record sudah di-finalize (SUCCESS/FAILED), publish Kafka gagal
- Konsekuensi: Order Service tidak tahu payment sudah selesai
- Solusi proper: Transactional Outbox pattern
- Ditunda ke fase resilience terpisah

## Payment stuck di INITIATED bila crash antara Transaction 1 dan Midtrans call
- Skenario: split-transaction — Payment tersimpan INITIATED (T1 commit), lalu proses
  crash sebelum charge Midtrans selesai. Payment nyangkut di INITIATED selamanya.
- Konsekuensi: order stuck WAITING_PAYMENT, tidak ada VA, tidak ada notifikasi
- Solusi proper: scheduled recovery job (scan INITIATED > N menit → retry charge)
- Ditunda ke Fase 5c (reconciliation)

## Re-publish PaymentCompleted saat AlreadyProcessedException (Opsi B)
- Sekarang (Opsi A): event duplikat OrderCreated → skip total, tidak re-publish
- Risk: bila PaymentCompleted pertama hilang di jaringan, Order Service menunggu selamanya
- Opsi B: query Payment existing → re-publish PaymentCompleted dari state
- Ditunda; butuh idempotency PaymentCompleted di Order Service juga

## Real pricing (formula dummy)
- Sekarang: amount = quantity × 10.000 (hardcoded, ada TODO eksplisit di OrderService)
- Proper: ambil harga dari Product Service saat order dibuat
- Ditunda sampai Fase 5 selesai

## userId dari JWT di Order Service
- Sekarang: order belum menyimpan siapa pembuatnya
- Proper: ekstrak userId dari JWT claim saat POST /orders
- Ditunda ke sub-fase security

## shippingAddress di Order
- Sekarang: order belum punya alamat pengiriman
- Dibutuhkan saat Delivery Service dibangun (Fase 6)

## ✅ EXPIRED mapping di Order Service (Fase 5b) — SELESAI 2026-07-07
- Keputusan di-lock: OrderStatus.EXPIRED eksplisit (bukan digabung ke PAYMENT_FAILED),
  agar data tidak ambigu untuk reconciliation Fase 5c
- PaymentOutcome (Order) + OrderStatus kini punya nilai EXPIRED
- OrderPaymentUpdateService: switch exhaustive — SUCCESS→PAID, EXPIRED→EXPIRED, FAILED→PAYMENT_FAILED
- Test: OrderPaymentUpdateServiceTest.applyPaymentOutcome_transitionsToExpired_...

## ✅ Race condition webhook vs scheduled reconciliation (Fase 5c) — DITANGANI 2026-07-07
- Skenario: user bayar di detik terakhir; webhook settlement (PAID) bertabrakan dengan
  scheduled job yang menandai EXPIRED
- Solusi terpasang: reconciliation job memakai conditional update atomik
  (expireIfStillPending → updateStatusIfCurrentMatches WHERE status=PENDING).
  Kalau webhook menang balapan, update kembalikan 0 → job skip, TIDAK publish EXPIRED.
- Optimistic locking (@Version) belum diperlukan untuk transisi tunggal ini.
- Catatan: tech debt "Payment stuck di INITIATED" (butuh retry-charge) masih terpisah & terbuka.

## Centralized config
- Sekarang: config (mis. midtrans.base-url, server key) tersebar di application.yml per service
- Kandidat: Spring Cloud Config Server (atau Consul/Vault)
- Ditunda; kandidat sub-fase antara Fase 5 dan 6

## Elasticsearch tertahan di 8.15.0
- Upgrade ke ES 9 gagal: Confluent ES Sink Connector 15.1.2 belum kompatibel
- Tetap di 8.15.0 sampai connector mendukung ES 9

## JWT di Payment Service (Fase 5b)
- Endpoint GET /payments/{orderId} akan protected (JWT), webhook TIDAK (pakai signature)
- Ikuti pola JWKS/static public key seperti service lain
- Terkait juga: rencana migrasi JWT static public key → endpoint JWKS lintas service

## Authorization granular di GET /payments/{orderId} (Fase 5b)
- Sekarang (Opsi A): endpoint cukup "authenticated" — user login manapun bisa
  melihat payment milik siapa pun (termasuk order user lain)
- Risk: kebocoran data lintas user (user A lihat VA/status order user B)
- Opsi B (proper): user hanya boleh melihat payment untuk order miliknya sendiri
- Blocker: Payment Service belum menyimpan userId. Butuh:
  1. userId masuk ke OrderCreated event (dari JWT claim saat POST /orders)
  2. Payment entity simpan userId
  3. Endpoint cek userId dari JWT == userId Payment
- Terkait tech debt "userId dari JWT di Order Service"
- Ditunda; kandidat sub-fase security setelah Fase 5b


## Reserve gagal (stok kurang) tidak dikompensasi ke Order (Fase 5c)
- Flow saga dipilih: Inventory potong stok saat OrderCreated (bukan saat bayar sukses),
  lalu kembalikan saat PaymentCompleted(FAILED/EXPIRED). Alasan: sesuai keputusan produk.
- Gap: kalau stok kurang saat reserve (ErrInsufficientStock), Inventory hanya log — TIDAK
  ada event kompensasi ke Order. Order tetap lanjut minta VA & bisa PAID walau stok tak ada.
- Proper: publish event (mis. StockReservationFailed) → Order set PAYMENT_FAILED/CANCELLED +
  Payment batalkan VA. Ditunda; butuh event baru + consumer di Order Service.
- Terkait: idempotency Inventory kini via Reservation.status (DEDUCTED→CONFIRMED/RETURNED),
  menggantikan processed_events.
