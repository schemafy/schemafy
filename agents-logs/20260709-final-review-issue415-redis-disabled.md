# 2026-07-09 final-review issue415 redis-disabled

- Branch: `feat/#415`
- Scope reviewed: signup email verification naming, 1 minute email verification TTL, Redis disabled local auth token adapter, optional Redis cache router startup behavior, env examples, frontend verification naming/button height adjustment.
- Fresh verification:
  - `./gradlew :core:test :api:test --tests com.schemafy.core.user.application.service.SendSignUpEmailCodeServiceTest --tests com.schemafy.core.user.application.service.VerifySignUpEmailServiceTest --tests com.schemafy.core.user.application.service.SignUpUserServiceTest --tests com.schemafy.api.cache.service.CacheRouterTest --tests com.schemafy.api.user.adapter.out.AuthTokenLocalAdapterTest --tests com.schemafy.api.user.adapter.out.AuthTokenRedisAdapterTest --tests com.schemafy.api.user.adapter.out.EmailVerificationMailTemplateTest --tests com.schemafy.api.user.adapter.out.EmailVerificationMailAdapterTest --tests com.schemafy.api.user.controller.AuthControllerTest --no-daemon` passed.
  - `./gradlew :core:spotlessCheck :api:spotlessCheck --no-daemon` passed.
  - `npm exec eslint src/features/auth/components/SignUpForm.tsx` passed.
  - `git diff --check` passed.
- Final decision: completion is possible with risk. Main risk is workspace hygiene: new local auth token adapter files are still untracked and unrelated untracked files exist, so staging must be selective before commit.
