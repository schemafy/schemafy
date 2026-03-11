# perf-audit-canvas-scale

규모별 캔버스 성능 측정. 3→5→10개 규모로 확장하며 같은 작업의 렌더 비용 변화를 추적한다. 컬럼 5회 연속 추가로 반복 시 열화 추이도 측정.

## 사용법

```
/perf-audit-canvas-scale [이메일] [비밀번호]
```

생략 시 기본값: `test@example.com` / `password123`. 경로 고정: `/canvas`. 소요: ~1~2분.

> ⚠️ 루트에서 `npm run dev` 먼저 실행 (프론트엔드 3001, BFF 4000, Spring Boot 8080, DB).

---

## 실행 절차

### 1단계: Playwright 측정

```bash
cd apps/frontend && PERF_PATH=/canvas PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit-canvas-scale.spec.ts --project=chromium --reporter=line
```

**측정 구조:** N개 셋업 후 addTable 측정(+1) → 결과 `tableCount` = N+1.

| 단계         | tableCount | 내용                                          |
| ------------ | ---------- | --------------------------------------------- |
| Scale 3→4    | 4          | addTable·addColumn·connectEdge 측정           |
| Scale 5→6    | 6          | addTable·addColumn·connectEdge 측정           |
| Scale 10→11  | 11         | addTable·addColumn·connectEdge 측정           |
| repeatColumn | 11         | 마지막 addTable 직후, 컬럼 5회 연속 추가 측정 |

| 작업          | 스킵 조건                |
| ------------- | ------------------------ |
| `addColumn`   | "Add Column" 버튼 비가시 |
| `connectEdge` | 핸들 8개 미만            |

스킵 시 `skipped: true` 기록 (0ms/0회 아님). 리포트 분석 시 측정 실패로 처리.

**즉시 중단 조건:** 테스트 실패 | `/signin` 리다이렉트(인증 실패) | JSON 미생성

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/`에서 `*_canvas-scale.json` 최근 파일 읽기.

### 3단계: 리포트 생성

`apps/frontend/performance/reports/YYYY-MM-DD_canvas-scale.md`에 저장.

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

## 반복 작업 추이 (컬럼 5회 연속, tableCount=11)

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

> 이벤트 리스너가 테이블 수에 비례 선형 증가 → 가상화·이벤트 위임 필요 신호.
> 반복 작업에서 렌더 횟수 증가 → 누적 상태·메모리 누수 가능성.

## 원인 분석

(규모 증가에 따른 렌더·리스너·힙 변화, 선형/비선형 여부)

## 해결 방법

(원인별 구체적 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- 셋업 중 테이블 겹침 → 클릭 엉킴 → 이상값. 새 캔버스에서 실행해야 정확.
- `connectEdge`: 규모 커질수록 핸들 인덱스 불일치 가능 → 렌더값 0 기록 가능
- 결과 파일 누적 저장 → 항상 최근 파일 기준으로 분석
