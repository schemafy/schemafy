# Performance Audit: Canvas Ops

> 측정일시: 2026-03-11T11:50:34Z | 브라우저: Chromium | 환경: 로컬 dev 서버

## 작업별 성능 요약

| 작업          | 소요시간 | 렌더 횟수 | Long Task | 상태 |
| ------------- | -------- | --------- | --------- | ---- |
| 테이블 추가 1 | 609ms    | 13회      | 0개       | ✅   |
| 테이블 추가 2 | 606ms    | 13회      | 0개       | ✅   |
| 컬럼 추가     | 375ms    | 17회      | 0개       | ✅   |
| 엣지 연결     | 695ms    | 22회      | 0개       | ✅   |

## 최종 스냅샷

| 지표          | 측정값       |
| ------------- | ------------ |
| JS 힙 (used)  | 22.9MB       |
| JS 힙 (total) | 54.4MB       |
| DOM 노드 수   | 614개        |
| 이벤트 리스너 | 1,534개      |
| 레이아웃 횟수 | 44회 (누적)  |
| 스타일 재계산 | 166회 (누적) |
| 스크립트 실행 | 0.49s (누적) |
| 레이아웃 실행 | 0.02s (누적) |

> 작업별 Long Task 수가 0이면 양호, 1~2개면 주의, 3개 이상이면 위험으로 판정.
> 렌더 횟수는 절대 기준 없이 작업 간 상대 비교 및 추세 파악 용도로만 사용.

## 원인 분석

### 1. `connectEdge` — 렌더 22회, 695ms (가장 무거운 작업)

테이블 추가(13회)보다 렌더가 **69% 더 많다**. 엣지 연결은 hover 시 핸들 opacity 전환, 드래그 중 임시 엣지 렌더, 연결 완료 후 edge 상태 반영 등 단계별로 상태 변경이 연쇄적으로 발생하기 때문이다. 각 단계가 별도 리렌더를 트리거한다.

### 2. `addColumn` — 렌더 17회, 375ms

테이블 추가보다 렌더가 4회 더 많다. 컬럼 추가 시 테이블 노드의 높이가 변하면서 포지션 재계산, 관련 엣지 경로 재계산, 전체 노드 리렌더가 함께 발생하는 것으로 추정된다.

### 3. 이벤트 리스너 1,534개 (노드 614개 대비 ~2.5배)

DOM 노드당 평균 2.5개의 이벤트 리스너가 등록되어 있다. React Flow 기반 캔버스에서 각 노드/엣지마다 onMouseEnter, onMouseLeave, onClick, onMouseDown 등을 직접 바인딩하면 노드 수 증가에 따라 리스너가 선형 증가한다. 현재 614개 노드는 테이블 2개 + 컬럼 추가 1회 수준의 초기 상태임을 고려하면, 스키마 규모가 커질수록 급증할 수 있다.

### 4. `recalcStyleCount: 166` vs `layoutCount: 44`

스타일 재계산이 레이아웃의 4배 수준이다. CSS transition(hover opacity, edge animation)이 매 렌더마다 스타일 재계산을 유발하고 있다. 레이아웃 자체는 적지만 스타일 재계산이 과도하다.

## 해결 방법

### 1. `connectEdge` 렌더 최적화 — 상태 배칭

hover/드래그/연결 완료 상태 변경을 React `unstable_batchedUpdates` 또는 Zustand `setState` 배칭으로 묶어 렌더를 줄인다.

```ts
// 현재: 각 상태 변경이 개별 리렌더 트리거
setHoveredNode(id);
setDraggingEdge(edge);

// 개선: 배칭으로 1회 리렌더
import { unstable_batchedUpdates } from 'react-dom';
unstable_batchedUpdates(() => {
  setHoveredNode(id);
  setDraggingEdge(edge);
});
```

### 2. `addColumn` 렌더 최적화 — 선택적 노드 업데이트

컬럼 추가 시 변경된 테이블 노드만 업데이트하고, 관련 없는 노드가 리렌더되지 않도록 `React.memo` + 안정적인 props 참조를 확보한다.

```ts
// 테이블 노드 컴포넌트에 memo 적용
export const TableNode = React.memo(
  ({ data, selected }: NodeProps) => {
    // ...
  },
  (prev, next) => prev.data === next.data && prev.selected === next.selected,
);
```

### 3. 이벤트 리스너 위임 — 이벤트 버블링 활용

개별 노드마다 리스너를 바인딩하는 대신, 캔버스 컨테이너 1곳에서 이벤트를 위임 처리한다.

```ts
// 캔버스 래퍼에서 이벤트 위임
<div
  onMouseEnter={(e) => {
    const nodeEl = (e.target as HTMLElement).closest('[data-nodeid]');
    if (nodeEl) handleNodeHover(nodeEl.dataset.nodeid);
  }}
>
  <ReactFlow ... />
</div>
```

### 4. 스타일 재계산 감소 — CSS will-change 및 transform 활용

hover opacity 전환을 GPU 레이어로 분리하여 스타일 재계산 비용을 낮춘다.

```css
/* 핸들 요소 */
.react-flow__handle {
  will-change: opacity;
  transition: opacity 150ms ease;
}

/* 엣지 경로 */
.react-flow__edge-path {
  will-change: transform;
}
```

### 5. 장기 대응 — 노드 가상화

테이블 수가 증가할수록 DOM 노드와 이벤트 리스너가 함께 증가한다. 뷰포트 밖 노드를 렌더하지 않는 가상화를 적용하면 대규모 스키마에서도 성능을 일정하게 유지할 수 있다. React Flow의 `onlyRenderVisibleElements` 옵션을 활성화하는 것이 가장 빠른 적용 방법이다.

```tsx
<ReactFlow
  onlyRenderVisibleElements={true}
  // ...
/>
```
