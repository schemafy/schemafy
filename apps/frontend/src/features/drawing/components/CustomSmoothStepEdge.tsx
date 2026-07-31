import { memo } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  useInternalNode,
  type EdgeProps,
  type Edge,
} from '@xyflow/react';
import type { Point, EdgeData } from '../types';
import { useControlPointDrag } from '../hooks/useControlPointDrag';
import {
  getInternalNodeRect,
  resolveRelationshipAnchor,
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
    selected,
  }: EdgeProps<Edge<EdgeData>>) => {
    const sourceNode = useInternalNode(source);
    const targetNode = useInternalNode(target);
    const sourceRect = getInternalNodeRect(sourceNode);
    const targetRect = getInternalNodeRect(targetNode);
    const sourceAnchor =
      sourceRect && targetRect
        ? resolveRelationshipAnchor(sourceRect, targetRect)
        : null;
    const targetAnchor =
      sourceRect && targetRect
        ? resolveRelationshipAnchor(targetRect, sourceRect)
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
      data,
    });

    const renderHandle = (position: Point, handleIndex: number) => {
      const isActive = draggingHandle === handleIndex;
      const isVisible = selected || isActive;

      return (
        <EdgeLabelRenderer key={handleIndex}>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${position.x}px, ${position.y}px)`,
              pointerEvents: isVisible ? 'all' : 'none',
              cursor: isActive ? 'grabbing' : 'grab',
              zIndex: 1000,
              opacity: isVisible ? 1 : 0,
            }}
            className="nodrag nopan"
            onMouseDown={handleMouseDown(handleIndex)}
          >
            <div
              className={`
              schemafy-edge-control schemafy-canvas-panel rounded-full transition-all
              ${isActive ? 'scale-110 border-schemafy-soft-blue text-schemafy-soft-blue' : 'text-schemafy-dark-gray hover:border-schemafy-soft-blue hover:text-schemafy-soft-blue'}
            `}
              style={{
                width: 16,
                height: 16,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <span
                className={
                  isActive
                    ? 'h-1.5 w-1.5 rounded-full bg-schemafy-soft-blue'
                    : 'h-1.5 w-1.5 rounded-full bg-schemafy-dark-gray'
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
              fontSize: labelStyle?.fontSize || 11,
              fontWeight: labelStyle?.fontWeight || 600,
              color: labelStyle?.color || 'var(--color-schemafy-dark-gray)',
              pointerEvents: 'none',
              background: 'hsl(var(--schemafy-panel))',
              border: '1px solid hsl(var(--schemafy-glass-border))',
              boxShadow: 'none',
              backdropFilter: 'none',
              padding: '2px 7px',
              borderRadius: '999px',
              lineHeight: 1.35,
              marginTop: '-25px',
              maxWidth: 140,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
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
