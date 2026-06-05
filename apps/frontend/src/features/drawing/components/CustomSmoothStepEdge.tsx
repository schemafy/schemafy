import { memo } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  useInternalNode,
  type EdgeProps,
  type Edge,
} from '@xyflow/react';
import { Move } from 'lucide-react';
import type { Point, EdgeData } from '../types';
import { useControlPointDrag } from '../hooks/useControlPointDrag';
import {
  getInternalNodeRect,
  getRelationshipAnchorPoint,
} from '../utils/anchorGeometry';

export const CustomSmoothStepEdge = memo(
  ({
    id,
    source,
    target,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style = {},
    markerEnd,
    markerStart,
    label,
    labelStyle,
    data,
  }: EdgeProps<Edge<EdgeData>>) => {
    const sourceNode = useInternalNode(source);
    const targetNode = useInternalNode(target);
    const sourceRect = getInternalNodeRect(sourceNode);
    const targetRect = getInternalNodeRect(targetNode);
    const sourceAnchor =
      sourceRect && targetRect
        ? getRelationshipAnchorPoint(sourceRect, targetRect, data?.fkAnchor)
        : null;
    const targetAnchor =
      sourceRect && targetRect
        ? getRelationshipAnchorPoint(targetRect, sourceRect, data?.pkAnchor)
        : null;

    const {
      path,
      handle1Position,
      handle2Position,
      labelPosition,
      draggingHandle,
      handleMouseDown,
    } = useControlPointDrag({
      id,
      sourceX: sourceAnchor?.point.x ?? sourceX,
      sourceY: sourceAnchor?.point.y ?? sourceY,
      targetX: targetAnchor?.point.x ?? targetX,
      targetY: targetAnchor?.point.y ?? targetY,
      sourcePosition: sourceAnchor?.position ?? sourcePosition,
      targetPosition: targetAnchor?.position ?? targetPosition,
      sourceRect,
      targetRect,
      data,
    });

    const renderHandle = (position: Point, handleIndex: number) => {
      const isActive = draggingHandle === handleIndex;

      return (
        <EdgeLabelRenderer key={handleIndex}>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${position.x}px, ${position.y}px)`,
              pointerEvents: 'all',
              cursor: isActive ? 'grabbing' : 'grab',
              zIndex: 1000,
            }}
            className="nodrag nopan"
            onMouseDown={handleMouseDown(handleIndex)}
          >
            <div
              className={`
              bg-schemafy-bg border-2 rounded-full shadow-lg transition-all
              ${isActive ? 'scale-125 border-schemafy-blue' : 'border-schemafy-dark-gray hover:border-schemafy-blue'}
            `}
              style={{
                width: 20,
                height: 20,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Move
                size={10}
                className={
                  isActive ? 'text-schemafy-blue' : 'text-schemafy-dark-gray'
                }
              />
            </div>
          </div>
        </EdgeLabelRenderer>
      );
    };

    const renderLabel = () => {
      if (!label) return null;

      return (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelPosition.x}px, ${labelPosition.y}px)`,
              fontSize: labelStyle?.fontSize || 12,
              fontWeight: labelStyle?.fontWeight || 'bold',
              color: labelStyle?.color || 'var(--color-schemafy-dark-gray)',
              pointerEvents: 'none',
              background: 'var(--color-schemafy-bg)',
              padding: '2px 6px',
              borderRadius: '4px',
              marginTop: '-25px',
            }}
          >
            {label}
          </div>
        </EdgeLabelRenderer>
      );
    };

    return (
      <>
        <BaseEdge
          path={path}
          markerEnd={markerEnd}
          markerStart={markerStart}
          style={style}
        />

        {renderLabel()}
        {renderHandle(handle1Position, 1)}
        {handle2Position && renderHandle(handle2Position, 2)}
      </>
    );
  },
);
