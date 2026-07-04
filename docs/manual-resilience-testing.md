# Manual Resilience Testing Playbook — Fase 5a

Panduan untuk memverifikasi Circuit Breaker di Payment Service bekerja secara real,
bukan hanya di test otomatis. Uji ini menggunakan WireMock sebagai simulasi Midtrans
dan memanipulasi state-nya (matikan/hidupkan) untuk memicu Circuit Breaker.

## Prasyarat

Semua infrastruktur harus jalan:

\`\`\`powershell
podman compose up -d postgres kafka kafka-ui wiremock
\`\`\`

Verifikasi semua container `Up`:

\`\`\`powershell
podman ps
\`\`\`

Yang harus ada: `takapedia-postgres`, `takapedia-kafka`, `takapedia-kafka-ui`,
`takapedia-wiremock`.

## Persiapan service

Buka 2 terminal terpisah:

**Terminal 1 — Order Service** (port 8086):
\`\`\`powershell
cd services\order-service
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
\`\`\`

**Terminal 2 — Payment Service** (port 8087):
\`\`\`powershell
cd services\payment-service
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
\`\`\`

Tunggu sampai kedua service log `Started ... in X.XXX seconds`.

## Skenario A: Happy path (baseline)

**Tujuan**: verifikasi flow lengkap bekerja saat gateway sehat.

**Langkah**:

1. Buka Kafka UI: http://localhost:8090
2. Cari topic `order.created` → Produce Message
3. Kirim payload:
   \`\`\`json
   {
   "orderId": "aaaaaaaa-0000-0000-0000-000000000001",
   "productId": "fedcba98-7654-3210-fedc-ba9876543210",
   "quantity": 3,
   "paymentMethod": "BCA_VA",
   "amount": 30000,
   "occurredAt": "2026-07-04T10:00:00Z"
   }
   \`\`\`

4. Cek Payment Service log:
   \`\`\`
   Payment initialized for orderId=aaaaaaaa-0000-0000-0000-000000000001
   \`\`\`

5. Cek DB Payment:
   \`\`\`powershell
   podman exec -it takapedia-postgres psql -U takapedia -d payment_db \
   -c "SELECT order_id, status, external_reference FROM payments WHERE order_id = 'aaaaaaaa-0000-0000-0000-000000000001';"
   \`\`\`
   Expected: `status = SUCCESS`, `external_reference = VA-BCA-XXXXXXXX`

6. Cek Circuit Breaker state:
   \`\`\`powershell
   curl http://localhost:8087/actuator/health/circuitBreakers
   \`\`\`
   Expected: `"state": "CLOSED"`, `"failureRate": "-1.0%"` atau `"0.0%"`.

## Skenario B: Gateway down → Circuit OPEN

**Tujuan**: verifikasi Circuit Breaker transisi ke OPEN setelah threshold failure tercapai.

### B.1: Matikan WireMock

\`\`\`powershell
podman stop takapedia-wiremock
\`\`\`

Verifikasi mati:
\`\`\`powershell
curl http://localhost:8091/v2/charge
\`\`\`
Expected: `Failed to connect` / `Connection refused`.

### B.2: Kirim 5 event beruntun

Kirim event dari Kafka UI dengan `orderId` yang unik setiap kali (misal
`bbbbbbbb-0000-0000-0000-000000000001` sampai `bbbbbbbb-0000-0000-0000-000000000005`).

Cek Payment Service log — untuk tiap event:
\`\`\`
Midtrans charge unreachable for orderId=...
Circuit breaker fallback triggered for orderId=...
\`\`\`

Setelah **5 event** (sesuai `slidingWindowSize: 10`, `minimumNumberOfCalls: 5`,
`failureRateThreshold: 50%` di config production), Circuit Breaker seharusnya OPEN.

### B.3: Verifikasi Circuit Breaker state = OPEN

\`\`\`powershell
curl http://localhost:8087/actuator/health/circuitBreakers
\`\`\`
Expected: `"state": "OPEN"`.

\`\`\`powershell
curl http://localhost:8087/actuator/circuitbreakerevents/midtrans
\`\`\`
Cari event bertipe `STATE_TRANSITION` dari `CLOSED` ke `OPEN`. Contoh output:
\`\`\`json
{
"eventType": "STATE_TRANSITION",
"circuitBreakerName": "midtrans",
"stateTransition": "CLOSED_TO_OPEN"
}
\`\`\`

### B.4: Verifikasi Payment tetap tercatat (FAILED)

Meski Midtrans down, Payment record tetap ada dengan status FAILED:

\`\`\`powershell
podman exec -it takapedia-postgres psql -U takapedia -d payment_db \
-c "SELECT order_id, status, failure_reason FROM payments WHERE order_id LIKE 'bbbbbbbb-%';"
\`\`\`
Expected: 5 row, semua `status = FAILED`, `failure_reason = GATEWAY_UNAVAILABLE`.

### B.5: Verifikasi Order tetap terupdate (PAYMENT_FAILED)

Meski gateway down, sistem tetap konsisten — Order Service menerima
`PaymentCompleted(FAILED)` dan update statusnya:

\`\`\`powershell
podman exec -it takapedia-postgres psql -U takapedia -d order_db \
-c "SELECT id, status FROM orders WHERE id LIKE 'bbbbbbbb-%';"
\`\`\`
Expected: `status = PAYMENT_FAILED` untuk semua row.

**Ini bukti utama resilience**: sistem end-to-end tetap konsisten (nggak crash, nggak
hang, nggak stuck di `WAITING_PAYMENT`) meski dependency eksternal down.

## Skenario C: Recovery — Circuit HALF_OPEN → CLOSED

**Tujuan**: verifikasi Circuit Breaker auto-recover setelah gateway kembali.

### C.1: Hidupkan WireMock

\`\`\`powershell
podman start takapedia-wiremock
\`\`\`

Tunggu 10-15 detik sampai WireMock siap.

### C.2: Tunggu Circuit transisi otomatis ke HALF_OPEN

Config: `waitDurationInOpenState: 10s` + `automaticTransitionFromOpenToHalfOpenEnabled: true`.

Setelah 10 detik dari transisi OPEN, Circuit otomatis pindah ke HALF_OPEN. Cek:

\`\`\`powershell
curl http://localhost:8087/actuator/health/circuitBreakers
\`\`\`
Expected: `"state": "HALF_OPEN"`.

### C.3: Kirim 3 event untuk trial

Config: `permittedNumberOfCallsInHalfOpenState: 3`. Circuit akan izinkan 3 call,
kalau sukses maka transisi ke CLOSED.

Kirim 3 event dari Kafka UI dengan `orderId` unik (misal `cccccccc-0000-0000-0000-00000000000X`).

### C.4: Verifikasi CLOSED

\`\`\`powershell
curl http://localhost:8087/actuator/health/circuitBreakers
\`\`\`
Expected: `"state": "CLOSED"`.

\`\`\`powershell
curl http://localhost:8087/actuator/circuitbreakerevents/midtrans
\`\`\`
Cari event `STATE_TRANSITION` dari `HALF_OPEN` ke `CLOSED`.

Cek Payment di DB:
\`\`\`powershell
podman exec -it takapedia-postgres psql -U takapedia -d payment_db \
-c "SELECT order_id, status, external_reference FROM payments WHERE order_id LIKE 'cccccccc-%';"
\`\`\`
Expected: `status = SUCCESS` untuk 3 row.

## Reset state (opsional)

Untuk memulai dari state bersih (misal untuk uji ulang):

\`\`\`powershell
podman exec -it takapedia-postgres psql -U takapedia -d payment_db -c "TRUNCATE payments, processed_events;"
podman exec -it takapedia-postgres psql -U takapedia -d order_db -c "TRUNCATE orders;"
\`\`\`

Untuk reset Circuit Breaker (restart Payment Service):
\`\`\`powershell
# Ctrl+C di terminal Payment Service, lalu re-run
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
\`\`\`

## Yang telah kita buktikan

Setelah menjalankan Skenario A, B, dan C secara lengkap:

1. **Happy path** — sistem berjalan normal saat semua sehat
2. **Failure isolation** — saat gateway down, sistem tetap konsisten (Payment FAILED,
   Order PAYMENT_FAILED, event tetap terbit). Tidak ada stuck state.
3. **Circuit Breaker mencegah cascading failure** — call ke gateway di-block saat OPEN,
   fallback jalan cepat, resource tidak habis untuk retry.
4. **Auto recovery** — sistem otomatis kembali normal saat gateway sehat, tanpa
   restart service.

Ini bukti empiris bahwa Fase 5a — Resilience Payment Service — bekerja seperti yang
dirancang.

## Catatan tech debt (untuk fase berikutnya)

- **Fase 5b**: webhook dari Midtrans belum ada. Sekarang PaymentCompleted terbit
  segera saat charge sukses/gagal (bukan saat user beneran bayar).
- **Fase 5c**: Saga pattern untuk stok — sekarang Inventory Service potong stok saat
  OrderCreated, bukan saat PaymentCompleted(SUCCESS).
- **Outbox pattern**: publish PaymentCompleted setelah DB commit, tapi belum atomik
  dengan DB write. Jika Kafka down setelah DB commit, event hilang.