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