# 2026-07-10 CacheRouter lazy Redis resolution

- Replaced `CacheRouter` `@PostConstruct` Redis resolution with lazy resolution on `CacheType.REDIS` access.
- Kept Redis-disabled startup behavior: the router can start without a Redis cache bean, and Redis cache requests fail with `Redis cache is disabled`.
- Removed the constructor initialization log because lazy resolution makes Redis availability unknown at startup.
- Verification:
  - `./gradlew spotlessCheck` passed.
  - `./gradlew :api:test --tests com.schemafy.api.cache.service.CacheRouterTest --no-daemon` passed with Java 21.
  - `./gradlew :api:spotbugsMain --no-daemon` passed with Java 21 after rerunning the task by itself.
