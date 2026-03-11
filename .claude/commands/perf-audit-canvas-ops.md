# perf-audit-canvas-ops

캔버스 조작 성능 측정. 테이블 추가·컬럼 추가·엣지 연결의 렌더 비용을 측정한다.

## 사용법

```
/perf-audit-canvas-ops [이메일] [비밀번호]
```

생략 시 기본값: `test@example.com` / `password123`. 경로 고정: `/canvas`.

> ⚠️ 루트에서 `npm run dev` 먼저 실행 (프론트엔드 3001, BFF 4000, Spring Boot 8080, DB).

---

## 실행 절차

### 1단계: Playwright 측정

```bash
cd apps/frontend && PERF_PATH=/canvas PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit-canvas-ops.spec.ts --project=chromium --reporter=line
```

| 작업 키       | 내용                         | 스킵 조건                |
| ------------- | ---------------------------- | ------------------------ |
| `addTable1`   | 첫 번째 테이블 추가 (좌상단) | -                        |
| `addTable2`   | 두 번째 테이블 추가 (우하단) | -                        |
| `addColumn`   | 첫 번째 테이블에 컬럼 추가   | "Add Column" 버튼 비가시 |
| `connectEdge` | 두 테이블 간 엣지 연결       | 핸들 8개 미만            |

스킵 시 `skipped: true` 기록 (0ms/0회 아님). 리포트 분석 시 측정 실패로 처리.

**즉시 중단 조건:** 테스트 실패 | `/signin` 리다이렉트(인증 실패) | JSON 미생성

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/`에서 `*_canvas-ops.json` 최근 파일 읽기.

### 3단계: 리포트 생성

`apps/frontend/performance/reports/YYYY-MM-DD_canvas-ops.md`에 저장.

```markdown
# Performance Audit: Canvas Ops

> 측정일시: <timestamp> | 브라우저: Chromium | 환경: 로컬 dev 서버

## 작업별 성능 요약

| 작업          | 소요시간 | 렌더 횟수 | Long Task | 상태     |
| ------------- | -------- | --------- | --------- | -------- |
| 테이블 추가 1 | Xms      | X회       | X개       | ✅/⚠️/❌ |
| 테이블 추가 2 | Xms      | X회       | X개       | ✅/⚠️/❌ |
| 컬럼 추가     | Xms      | X회       | X개       | ✅/⚠️/❌ |
| 엣지 연결     | Xms      | X회       | X개       | ✅/⚠️/❌ |

## 최종 스냅샷

| 지표          | 측정값 |
| ------------- | ------ |
| JS 힙 (used)  | XMB    |
| JS 힙 (total) | XMB    |
| DOM 노드 수   | X개    |
| 이벤트 리스너 | X개    |
| 스타일 재계산 | X회    |
| 스크립트 실행 | Xs     |

> Long Task: 0개=양호, 1~2개=주의, 3개+=위험. 렌더 횟수는 작업 간 상대 비교 용도.

## 원인 분석

(렌더·Long Task가 높은 작업 위주)

## 해결 방법

(원인별 구체적 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- Toolbar 셀렉터: `data-testid="toolbar-table"`, `data-testid="toolbar-pointer"`
- 결과 파일 누적 저장 → 항상 최근 파일 기준으로 분석
