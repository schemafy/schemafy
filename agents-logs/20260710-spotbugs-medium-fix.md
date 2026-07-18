# 2026-07-10 SpotBugs medium fix

- CI reported `SpotBugs found 0 high and 5 medium priority bugs`.
- Reproduced locally with Java 21: `./gradlew spotbugsMain --no-daemon` failed in `:api:spotbugsMain` and `:core:spotbugsMain`.
- Fixed API findings:
  - Moved `CacheRouter` optional Redis resolution out of the constructor into `@PostConstruct`.
  - Replaced `ConcurrentHashMap` monitor synchronization in `AuthTokenLocalAdapter` with a private lock and `HashMap`.
- Fixed core findings by removing two dead local stores in `MySqlDdlGenerator`.
- Verification passed:
  - `./gradlew spotlessCheck`
  - `./gradlew spotbugsMain --no-daemon`
  - targeted `:api:test :core:test` for `CacheRouterTest`, `AuthTokenLocalAdapterTest`, and `MySqlDdlGeneratorTest`.
