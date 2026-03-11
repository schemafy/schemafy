# perf-audit

프론트엔드 성능 자동 측정 + AI 원인 분석 리포트 생성 skill.

## 사용법

```
/perf-audit <경로> [이메일] [비밀번호]
```

- `<경로>`: `/`로 시작하는 앱 내부 경로 (쿼리스트링 가능, 해시·외부 URL 불가)
- `/canvas`로 시작하면 자동 로그인. 생략 시 기본값: `test@example.com` / `password123`
- 캔버스 조작 성능(테이블·컬럼·엣지)은 `/perf-audit-canvas-ops` 사용

> ⚠️ 루트에서 `npm run dev` 먼저 실행 (프론트엔드 3001, BFF 4000, Spring Boot 8080, DB).

---

## 실행 절차

### 1단계: Playwright 측정

인증 불필요 (`/`, `/signin` 등):
```bash
cd apps/frontend && PERF_PATH=<경로> npx playwright test tests/perf-audit.spec.ts --project=chromium --reporter=line
```

인증 필요 (`/canvas` 등):
```bash
cd apps/frontend && PERF_PATH=<경로> PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit.spec.ts --project=chromium --reporter=line
```

**페이지 준비 판정:** `/canvas` → `.react-flow` 가시화 | 그 외 → `load` 이벤트. LCP 안정화: 위 조건 후 LCP 감지 또는 최대 3초 중 빠른 것.

**즉시 중단 조건:** 테스트 실패 | `/signin` 리다이렉트(인증 실패) | JSON 미생성

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/`에서 최근 `.json` 파일 읽기.

### 3단계: 리포트 생성

`apps/frontend/performance/reports/YYYY-MM-DD_<safePath>.md`에 저장.

> ⚠️ 아래 기준은 로컬 dev 서버 참고값. 프로덕션/CI와 수치 차이 있음.

#### 지표 판정 기준

**일반 페이지** (`/`, `/signin` 등):

| 지표      | 양호     | 주의        | 위험     |
| --------- | -------- | ----------- | -------- |
| LCP       | < 2500ms | 2500~4000ms | > 4000ms |
| FCP       | < 1800ms | 1800~3000ms | > 3000ms |
| CLS       | < 0.1    | 0.1~0.25    | > 0.25   |
| 로드 타임 | < 3000ms | 3000~5000ms | > 5000ms |
| JS 힙     | < 50MB   | 50~100MB    | > 100MB  |

**캔버스 페이지** (`/canvas` 등):

| 지표             | 양호    | 주의      | 위험    | 비고                    |
| ---------------- | ------- | --------- | ------- | ----------------------- |
| LCP / FCP        | —       | —         | —       | SPA 캐시로 항상 낮음    |
| CLS              | < 0.1   | 0.1~0.25  | > 0.25  | —                       |
| 로드 타임        | < 3000ms| 3000~5000ms| > 5000ms| `.react-flow` 기준      |
| JS 힙            | < 100MB | 100~200MB | > 200MB | 노드 수 비례            |
| 상호작용 렌더/초  | < 5/s   | 5~15/s    | > 15/s  | 마우스 이동 3초 측정    |
| Long Task 수     | 0       | 1~3       | > 3     | 50ms+ 블로킹            |

#### 리포트 형식

**일반 페이지:**

```markdown
# Performance Audit: <경로>

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 측정 지표 요약

| 지표            | 측정값 | 기준    | 상태     |
| --------------- | ------ | ------- | -------- |
| LCP             | Xms    | <2500ms | ✅/⚠️/❌ |
| FCP             | Xms    | <1800ms | ✅/⚠️/❌ |
| CLS             | X      | <0.1    | ✅/⚠️/❌ |
| 로드 타임       | Xms    | <3000ms | ✅/⚠️/❌ |
| JS 힙           | XMB    | <50MB   | ✅/⚠️/❌ |
| React 커밋 횟수 | X회    | —       | 참고용   |
| DOM 노드 수     | X개    | —       | 참고용   |

## 원인 분석

(문제 지표 위주로 측정 데이터 근거 서술)

## 해결 방법

(원인별 구체적 해결 방법. 가능하면 코드 예시 포함)
```

**캔버스 페이지 (`/canvas` 등):**

```markdown
# Performance Audit: <경로>

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 측정 지표 요약

| 지표               | 측정값 | 기준    | 상태     |
| ------------------ | ------ | ------- | -------- |
| LCP                | Xms    | —       | 참고용   |
| FCP                | Xms    | —       | 참고용   |
| CLS                | X      | <0.1    | ✅/⚠️/❌ |
| 로드 타임          | Xms    | <3000ms | ✅/⚠️/❌ |
| JS 힙              | XMB    | <100MB  | ✅/⚠️/❌ |
| React 커밋 횟수    | X회    | —       | 참고용   |
| DOM 노드 수        | X개    | —       | 참고용   |
| 상호작용 총 렌더/초 | X/s   | < 5/s   | ✅/⚠️/❌ |
| Long Task 수       | X개    | 0개     | ✅/⚠️/❌ |

### 상호작용 phases 상세

| Phase     | renderCount | longTaskCount |
| --------- | ----------- | ------------- |
| mouseMove | X           | X             |
| wheelZoom | X           | X             |
| pan       | X           | X             |

## 원인 분석

(문제 지표 위주로 측정 데이터 근거 서술)

## 해결 방법

(원인별 구체적 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- 서버 자동 시작 안 함. 측정 전 `npm run dev` 직접 실행
- Chromium 단독 실행 (CDP 전용). 전체 `test:e2e` 실행 시 자동 skip
- 결과 파일 누적 저장 → 항상 최근 파일 기준으로 분석
