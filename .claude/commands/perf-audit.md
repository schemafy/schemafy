# perf-audit

프론트엔드 성능 자동 측정 + AI 원인 분석 리포트 생성 skill.

## 사용법

```
/perf-audit <경로> [이메일] [비밀번호]
```

- `<경로>`: `/`로 시작하는 앱 내부 경로. 쿼리스트링(`?`) 포함 가능, 해시(`#`) 미지원, 외부 URL 불가
- `[이메일]`, `[비밀번호]`: 인증 필요 경로에서 생략 시 기본 테스트 계정 사용

예시:

- `/perf-audit /` → 랜딩 페이지 측정 (인증 불필요)
- `/perf-audit /canvas` → 캔버스 페이지 측정: 마우스 이동·휠 줌·패닝 phases 측정
- `/perf-audit /canvas user@example.com mypassword` → 특정 계정으로 측정

캔버스 조작 성능(테이블·컬럼 추가, 엣지 연결) 측정은 `/perf-audit-canvas-ops` 사용.

## 인증 필요 경로

`/canvas` 로 시작하는 경로는 자동으로 로그인 처리함.
이메일/비밀번호 미입력 시 기본값 사용: `test@example.com` / `password123`

> ⚠️ **사전 요구사항:** 인증 필요 경로 측정 전 루트에서 `npm run dev`로 전체 서버를 먼저 실행하세요.
> 프론트엔드(3001), BFF(4000), Spring Boot 백엔드(8080), DB가 모두 실행 중이어야 로그인이 가능합니다.

---

## 실행 절차

### 1단계: Playwright 측정 실행

인자에서 경로, 이메일, 비밀번호를 파싱한 뒤 아래 명령어 실행.

인증 불필요 경로 (`/`, `/signin`, `/signup` 등):

```bash
cd apps/frontend && PERF_PATH=<경로> npx playwright test tests/perf-audit.spec.ts --project=chromium --reporter=line
```

인증 필요 경로 (`/canvas` 등):

```bash
cd apps/frontend && PERF_PATH=<경로> PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit.spec.ts --project=chromium --reporter=line
```

**페이지 준비 완료 판정 기준:**
- `/canvas`: `.react-flow` 셀렉터 가시화 시점
- 그 외: `load` 이벤트 완료 시점
- LCP 안정화: 위 조건 이후 LCP 감지 또는 최대 3초 중 빠른 것

**실패 시 즉시 중단하고 원인 보고:**
- Playwright 테스트가 실패한 경우 → 오류 메시지 그대로 출력 후 종료
- `/canvas` 진입 후 `/signin` 리다이렉트 감지 → "인증 실패: 로그인 정보를 확인하세요" 보고 후 종료
- `performance/results/` 에 JSON이 생성되지 않은 경우 → "측정 실패: JSON 파일이 생성되지 않았습니다" 보고 후 종료

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/` 에서 **가장 최근에 생성된** `.json` 파일을 읽는다.
결과 파일명 규칙: `<ISO타임스탬프>_<경로를_로_치환>.json`

### 3단계: AI 분석 및 리포트 생성

읽은 JSON 데이터를 바탕으로 아래 기준에 따라 분석하고,
`apps/frontend/performance/reports/YYYY-MM-DD_<safePath>.md` 에 리포트를 저장한다.

> ⚠️ **주의:** 아래 기준은 **로컬 개발환경(dev 서버) 기준 참고값**입니다.
> 실제 프로덕션/CI 환경과 수치 차이가 있을 수 있으므로 절대 임계치로 사용하지 마세요.

#### 지표 판정 기준

경로 유형에 따라 기준이 다릅니다.

**일반 페이지** (`/`, `/signin`, `/signup` 등 — 첫 방문 기준):

| 지표      | 양호     | 주의        | 위험     |
| --------- | -------- | ----------- | -------- |
| LCP       | < 2500ms | 2500~4000ms | > 4000ms |
| FCP       | < 1800ms | 1800~3000ms | > 3000ms |
| CLS       | < 0.1    | 0.1~0.25    | > 0.25   |
| 로드 타임 | < 3000ms | 3000~5000ms | > 5000ms |
| JS 힙     | < 50MB   | 50~100MB    | > 100MB  |

**캔버스 페이지** (`/canvas` 등 — SPA 내부 라우팅, 리소스 캐시 상태):

| 지표            | 양호     | 주의        | 위험     | 비고 |
| --------------- | -------- | ----------- | -------- | ---- |
| LCP             | —        | —           | —        | SPA 캐시로 항상 낮음, 참고용 |
| FCP             | —        | —           | —        | 동상 |
| CLS             | < 0.1    | 0.1~0.25    | > 0.25   | — |
| 로드 타임       | < 3000ms | 3000~5000ms | > 5000ms | `.react-flow` 가시화 기준 |
| JS 힙           | < 100MB  | 100~200MB   | > 200MB  | 노드 수에 비례 증가 |
| React 커밋 횟수 | —        | —           | —        | 참고용 (추세 파악) |
| DOM 노드 수     | —        | —           | —        | 참고용 (추세 파악) |
| 상호작용 렌더/초 | < 5/s   | 5~15/s      | > 15/s   | 마우스 이동 3초 동안 측정 |
| Long Task 수    | 0        | 1~3         | > 3      | 50ms 이상 블로킹 작업 수 |

#### 리포트 형식

경로가 `/canvas`로 시작하면 캔버스 템플릿을, 그 외는 일반 템플릿을 사용한다.

**일반 페이지 템플릿:**

```markdown
# Performance Audit: <경로>

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 측정 지표 요약

| 지표            | 측정값 | 기준     | 상태     |
| --------------- | ------ | -------- | -------- |
| LCP             | Xms    | <2500ms  | ✅/⚠️/❌ |
| FCP             | Xms    | <1800ms  | ✅/⚠️/❌ |
| CLS             | X      | <0.1     | ✅/⚠️/❌ |
| 로드 타임       | Xms    | <3000ms  | ✅/⚠️/❌ |
| JS 힙           | XMB    | <50MB    | ✅/⚠️/❌ |
| React 커밋 횟수 | X회    | —        | 참고용   |
| DOM 노드 수     | X개    | —        | 참고용   |

> React 커밋 횟수와 DOM 노드 수는 best-effort 참고 지표입니다.
> 절대 임계치 없이 추세 파악 용도로만 사용하세요.
```

**캔버스 페이지 템플릿 (`/canvas` 등):**

```markdown
# Performance Audit: <경로>

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 측정 지표 요약

| 지표            | 측정값 | 기준     | 상태     |
| --------------- | ------ | -------- | -------- |
| LCP             | Xms    | —        | 참고용   |
| FCP             | Xms    | —        | 참고용   |
| CLS             | X      | <0.1     | ✅/⚠️/❌ |
| 로드 타임       | Xms    | <3000ms  | ✅/⚠️/❌ |
| JS 힙           | XMB    | <100MB   | ✅/⚠️/❌ |
| React 커밋 횟수  | X회    | —        | 참고용   |
| DOM 노드 수      | X개    | —        | 참고용   |
| 상호작용 총 렌더/초 | X/s  | < 5/s    | ✅/⚠️/❌ |
| Long Task 수      | X개   | 0개      | ✅/⚠️/❌ |

### 상호작용 phases 상세 (interaction.phases)

| Phase      | renderCount | longTaskCount |
| ---------- | ----------- | ------------- |
| mouseMove  | X           | X             |
| wheelZoom  | X           | X             |
| pan        | X           | X             |

> LCP/FCP는 SPA 내부 라우팅 특성상 캐시된 리소스로 인해 실제 체감 성능보다 낮게 측정됩니다.
> React 커밋 횟수와 DOM 노드 수는 best-effort 참고 지표입니다. 추세 파악 용도로만 사용하세요.

## 원인 분석

(각 문제 지표에 대해 측정 데이터를 근거로 원인을 구체적으로 서술)

## 해결 방법

(원인별 구체적인 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- 서버는 자동으로 시작하지 않음. 측정 전 루트에서 `npm run dev`로 직접 실행할 것
- 측정은 Chromium 단독으로 실행 (CDP 성능 측정은 Chromium 전용, 일관성 유지 목적)
- Firefox/WebKit으로 `npm run test:e2e` 전체 실행 시 이 테스트는 자동으로 skip됨
- 결과 파일은 누적 저장됨. 분석은 항상 가장 최근 파일 기준으로 수행
