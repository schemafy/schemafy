import { Handle, Position } from '@xyflow/react';
import { HANDLE_STYLE } from '../../types';

interface ConnectionHandlesProps {
  nodeId: string;
}

export const ConnectionHandles = ({ nodeId }: ConnectionHandlesProps) => {
  const handles = [
    {
      position: Position.Top,
      id: `${nodeId}-top-handle`,
    },
    {
      position: Position.Bottom,
      id: `${nodeId}-bottom-handle`,
    },
    {
      position: Position.Left,
      id: `${nodeId}-left-handle`,
    },
    {
      position: Position.Right,
      id: `${nodeId}-right-handle`,
    },
  ];

  return (
    <>
      {handles.map(({ position, id }) => (
        <Handle
          key={id}
          type={'source'}
          position={position}
          id={id}
          style={HANDLE_STYLE}
          className="group-hover:!opacity-100"
        />
      ))}
    </>
  );
};
