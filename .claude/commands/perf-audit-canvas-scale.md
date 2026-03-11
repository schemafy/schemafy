# perf-audit-canvas-scale

규모별 캔버스 성능 측정 skill. 테이블 3개 → 5개 → 10개 규모로 점진적으로 확장하며 같은 작업(테이블 추가, 컬럼 추가, 엣지 연결)의 렌더 비용 변화를 추적한다. 컬럼 연속 5회 추가로 반복 시 렌더 증가 추이도 측정한다.

## 사용법

```
/perf-audit-canvas-scale [이메일] [비밀번호]
```

- `[이메일]`, `[비밀번호]`: 생략 시 기본 테스트 계정(`test@example.com` / `password123`) 사용
- 측정 경로는 `/canvas`로 고정 (`PERF_PATH` 환경변수로 변경 가능)

예시:

- `/perf-audit-canvas-scale` → 기본 계정으로 측정
- `/perf-audit-canvas-scale user@example.com mypassword` → 특정 계정으로 측정

> ⚠️ **사전 요구사항:** 루트에서 `npm run dev`로 전체 서버를 먼저 실행하세요.
> 프론트엔드(3001), BFF(4000), Spring Boot(8080), DB 모두 실행 중이어야 합니다.
> 테스트 시간: 약 1~2분 (테이블 10개 셋업 포함)

---

## 실행 절차

### 1단계: Playwright 측정 실행

```bash
cd apps/frontend && PERF_PATH=/canvas PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit-canvas-scale.spec.ts --project=chromium --reporter=line
```

**측정 구조:**

> 각 scale 단계는 N개로 셋업 후 **addTable을 측정(+1)** 하므로 결과 JSON의 `tableCount`는 N+1이다.

| 단계         | 셋업 → 측정 시 tableCount | 내용                                          |
| ------------ | ------------------------- | --------------------------------------------- |
| Scale 3→4    | 3개 셋업 → tableCount=4   | addTable·addColumn·connectEdge 측정           |
| Scale 5→6    | 5개 셋업 → tableCount=6   | addTable·addColumn·connectEdge 측정           |
| Scale 10→11  | 10개 셋업 → tableCount=11 | addTable·addColumn·connectEdge 측정           |
| repeatColumn | tableCount=11 상태        | 마지막 addTable 직후, 컬럼 5회 연속 추가 측정 |

**작업 스킵 처리:**

- `addColumn`: "Add Column" 버튼이 보이지 않으면 `skipped: true`로 기록 (0ms/0회 아님)
- `connectEdge`: 핸들이 8개 미만이면 `skipped: true`로 기록
- 리포트 분석 시 `skipped: true` 항목은 측정 실패로 처리할 것

**실패 시 즉시 중단:**

- Playwright 테스트 실패 → 오류 메시지 출력 후 종료
- `/canvas` 진입 후 `/signin` 리다이렉트 감지 → "인증 실패" 보고 후 종료
- JSON 미생성 → "측정 실패: JSON 파일이 생성되지 않았습니다" 보고 후 종료

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/`에서 `*_canvas-scale.json` 패턴의 가장 최근 파일을 읽는다.

### 3단계: AI 분석 및 리포트 생성

`apps/frontend/performance/reports/YYYY-MM-DD_canvas-scale.md`에 리포트 저장.

#### 리포트 형식

```markdown
# Performance Audit: Canvas Scale

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 규모별 성능 추이

### addTable (테이블 추가)

| tableCount | 소요시간 | 렌더 횟수 | Long Task | skipped |
| ---------- | -------- | --------- | --------- | ------- |
| 4          | Xms      | X회       | X개       | -       |
| 6          | Xms      | X회       | X개       | -       |
| 11         | Xms      | X회       | X개       | -       |

### addColumn (컬럼 추가)

| tableCount | 소요시간 | 렌더 횟수 | Long Task | skipped |
| ---------- | -------- | --------- | --------- | ------- |
| 4          | Xms      | X회       | X개       | true/-  |
| 6          | Xms      | X회       | X개       | true/-  |
| 11         | Xms      | X회       | X개       | true/-  |

### connectEdge (엣지 연결)

(동일 형식)

## 반복 작업 추이 (컬럼 5회 연속, tableCount=11 기준)

| 회차 | 소요시간 | 렌더 횟수 | Long Task | skipped |
| ---- | -------- | --------- | --------- | ------- |
| 1회  | Xms      | X회       | X개       | true/-  |
| 2회  | Xms      | X회       | X개       | true/-  |
| 3회  | Xms      | X회       | X개       | true/-  |
| 4회  | Xms      | X회       | X개       | true/-  |
| 5회  | Xms      | X회       | X개       | true/-  |

## 규모별 힙·CDP 스냅샷

| tableCount | JS 힙 (used) | JS 힙 (total) | 이벤트 리스너 | 스타일 재계산 | 스크립트 실행 | DOM 노드 |
| ---------- | ------------ | ------------- | ------------- | ------------- | ------------- | -------- |
| 4          | XMB          | XMB           | X개           | X회           | Xs            | X개      |
| 6          | XMB          | XMB           | X개           | X회           | Xs            | X개      |
| 11         | XMB          | XMB           | X개           | X회           | Xs            | X개      |

> 이벤트 리스너가 테이블 수에 비례해 선형 증가하면 가상화 또는 이벤트 위임이 필요한 신호.
> 반복 작업에서 렌더 횟수가 증가하면 누적 상태나 메모리 누수 가능성.

## 원인 분석

(규모 증가에 따라 렌더·리스너·힙이 어떻게 변하는지, 선형/비선형 여부)

## 해결 방법

(원인별 구체적인 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- 셋업 중 테이블이 겹치면 클릭이 엉킬 수 있음. 그리드 배치가 실패하면 결과 이상값으로 나타남
- `connectEdge`는 규모가 커질수록 핸들 탐색 인덱스가 틀릴 수 있어 렌더값이 0으로 기록될 수 있음
- 테스트는 새 캔버스에서 시작해야 정확함. 기존 데이터가 있는 캔버스에서 실행 시 셋업 카운트가 맞지 않음
- 결과 파일은 누적 저장됨. 분석은 항상 가장 최근 파일 기준으로 수행
