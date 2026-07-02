## Test broken across services after JWKS migration
- Discovered: 2026-07-02, during Fase 5a Commit A refactor
- Root cause: application-test.yml override datasource+jpa tapi lupa
  provide mock untuk spring.security.oauth2.resourceserver.jwt.jwk-set-uri
- Workaround saat ini: mock jwk-set-uri di application-test.yml per service
- Proper fix: bikin shared test utility (mungkin TestSecurityConfig common)
- Services affected: Order Service (confirmed), Auth/Product/Product-Search (unverified)
- Action: setelah Fase 5a selesai, audit dan fix systematically