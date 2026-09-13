import { Handle, Position } from '@xyflow/react';
import { HANDLE_STYLE } from '../../types';

interface ConnectionHandlesProps {
  nodeId: string;
}

export const ConnectionHandles = ({ nodeId }: ConnectionHandlesProps) => {
  const getHandleStyle = (position: Position) => ({
    ...HANDLE_STYLE,
    ...(position === Position.Top ? { top: 5 } : {}),
    ...(position === Position.Bottom ? { bottom: 5 } : {}),
    ...(position === Position.Left ? { left: 5 } : {}),
    ...(position === Position.Right ? { right: 5 } : {}),
  });

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
          style={getHandleStyle(position)}
          className="schemafy-connection-handle !z-20 group-hover:!opacity-100"
        />
      ))}
    </>
  );
};
