export const RelationshipMarker = () => {
  return (
    <svg style={{ position: 'absolute', top: 0, left: 0, width: 0, height: 0 }}>
      <defs>
        <marker
          id="erd-one-start"
          viewBox="0 0 12 8"
          markerWidth={12}
          markerHeight={8}
          refX={3}
          refY={4}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <line
            x1="6"
            y1="1"
            x2="6"
            y2="7"
            stroke="currentColor"
            strokeWidth="2"
            fill="none"
          />
        </marker>

        <marker
          id="erd-one-end"
          viewBox="0 0 12 8"
          markerWidth={12}
          markerHeight={8}
          refX={9}
          refY={4}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <line
            x1="6"
            y1="1"
            x2="6"
            y2="7"
            stroke="currentColor"
            strokeWidth="2"
            fill="none"
          />
        </marker>

        <marker
          id="erd-many-start"
          viewBox="0 0 14 8"
          markerWidth={14}
          markerHeight={8}
          refX={4}
          refY={4}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path
            d="M3,4 L10,1 L10,7 Z"
            stroke="currentColor"
            strokeWidth="1"
            fill="currentColor"
          />
        </marker>

        <marker
          id="erd-many-end"
          viewBox="0 0 14 8"
          markerWidth={14}
          markerHeight={8}
          refX={10}
          refY={4}
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path
            d="M3,4 L10,1 L10,7 Z"
            stroke="currentColor"
            strokeWidth="1"
            fill="currentColor"
          />
        </marker>
      </defs>
    </svg>
  );
};
