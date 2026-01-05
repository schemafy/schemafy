export const RelationshipMarker = () => {
  return (
    <svg
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: 0,
        height: 0,
        color: 'var(--color-schemafy-dark-gray)',
      }}
    >
      <defs>
        <marker
          id="erd-one-start"
          viewBox="0 0 10 10"
          markerWidth={10}
          markerHeight={10}
          refX={4}
          refY={5}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path d="M5 1L5 9" stroke="currentColor" strokeLinecap="round" />
          <path d="M9 5H1" stroke="currentColor" strokeLinecap="round" />
        </marker>

        <marker
          id="erd-one-end"
          viewBox="0 0 10 10"
          markerWidth={10}
          markerHeight={10}
          refX={6}
          refY={5}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path d="M5 1L5 9" stroke="currentColor" strokeLinecap="round" />
          <path d="M9 5H1" stroke="currentColor" strokeLinecap="round" />
        </marker>

        <marker
          id="erd-many-start"
          viewBox="0 0 8 10"
          markerWidth={8}
          markerHeight={10}
          refX={3}
          refY={5}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path d="M1 9L7 5" stroke="currentColor" strokeLinecap="round" />
          <path d="M1 1L7 5" stroke="currentColor" strokeLinecap="round" />
          <path d="M7 5L0.5 5" stroke="currentColor" strokeLinecap="round" />
        </marker>

        <marker
          id="erd-many-end"
          viewBox="0 0 8 10"
          markerWidth={8}
          markerHeight={10}
          refX={5}
          refY={5}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path d="M7 9L1 5" stroke="currentColor" strokeLinecap="round" />
          <path d="M7 1L1 5" stroke="currentColor" strokeLinecap="round" />
          <path d="M1 5L7.5 5" stroke="currentColor" strokeLinecap="round" />
        </marker>
      </defs>
    </svg>
  );
};
