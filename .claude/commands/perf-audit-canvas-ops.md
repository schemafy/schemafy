# perf-audit-canvas-ops

캔버스 조작 성능 측정 skill. 테이블 추가, 컬럼 추가, 엣지 연결 등 실제 조작 작업별 렌더 비용을 측정한다.

## 사용법

```
/perf-audit-canvas-ops [이메일] [비밀번호]
```

- `[이메일]`, `[비밀번호]`: 생략 시 기본 테스트 계정(`test@example.com` / `password123`) 사용
- 측정 경로는 `/canvas`로 고정 (`PERF_PATH` 환경변수로 변경 가능)

예시:

- `/perf-audit-canvas-ops` → 기본 계정으로 측정
- `/perf-audit-canvas-ops user@example.com mypassword` → 특정 계정으로 측정

> ⚠️ **사전 요구사항:** 루트에서 `npm run dev`로 전체 서버를 먼저 실행하세요.
> 프론트엔드(3001), BFF(4000), Spring Boot(8080), DB 모두 실행 중이어야 합니다.

---

## 실행 절차

### 1단계: Playwright 측정 실행

```bash
cd apps/frontend && PERF_PATH=/canvas PERF_EMAIL=<이메일> PERF_PASSWORD=<비밀번호> npx playwright test tests/perf-audit-canvas-ops.spec.ts --project=chromium --reporter=line
```

**측정 작업 목록:**

| 작업 키       | 내용                                     |
| ------------- | ---------------------------------------- |
| `addTable1`   | 첫 번째 테이블 추가 (캔버스 좌상단 클릭) |
| `addTable2`   | 두 번째 테이블 추가 (캔버스 우하단 클릭) |
| `addColumn`   | 첫 번째 테이블에 컬럼 추가               |
| `connectEdge` | 두 테이블 간 엣지 연결 시도              |

**작업 스킵 처리:**

- `addColumn`: "Add Column" 버튼이 보이지 않으면 `skipped: true`로 기록 (0ms/0회 아님)
- `connectEdge`: 핸들이 8개 미만이면 `skipped: true`로 기록
- 리포트 분석 시 `skipped: true` 항목은 측정 실패로 처리할 것

**실패 시 즉시 중단:**

- Playwright 테스트 실패 → 오류 메시지 출력 후 종료
- `/canvas` 진입 후 `/signin` 리다이렉트 감지 → "인증 실패" 보고 후 종료
- JSON 미생성 → "측정 실패: JSON 파일이 생성되지 않았습니다" 보고 후 종료

### 2단계: 결과 JSON 읽기

`apps/frontend/performance/results/`에서 `*_canvas-ops.json` 패턴의 가장 최근 파일을 읽는다.

### 3단계: AI 분석 및 리포트 생성

`apps/frontend/performance/reports/YYYY-MM-DD_canvas-ops.md`에 리포트 저장.

#### 리포트 형식

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

> 작업별 Long Task 수가 0이면 양호, 1~2개면 주의, 3개 이상이면 위험으로 판정.
> 렌더 횟수는 절대 기준 없이 작업 간 상대 비교 및 추세 파악 용도로만 사용.

## 원인 분석

(렌더·Long Task가 높은 작업 위주로 원인 서술)

## 해결 방법

(원인별 구체적인 해결 방법. 가능하면 코드 예시 포함)
```

---

## 주의사항

- `connectEdge`: 핸들 부족 시 `skipped: true` 기록 (0ms와 구별됨). hover 의존으로 연결 자체가 실패해도 렌더는 측정됨
- Toolbar 버튼은 `data-testid="toolbar-table"`, `data-testid="toolbar-pointer"` 기반으로 셀렉팅
- 결과 파일은 누적 저장됨. 분석은 항상 가장 최근 파일 기준으로 수행
