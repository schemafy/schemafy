import { memo, useState, useEffect, useCallback, useMemo } from 'react';
import {
  ReactFlow,
  MiniMap,
  Background,
  BackgroundVariant,
  ConnectionMode,
  applyNodeChanges,
  type Node,
  type NodeChange,
  type Edge,
  type EdgeChange,
  type Connection,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import type { TableData } from '../types';
import type { MemoData } from '@/features/memo/hooks/memo.helper';
import { TableNode } from './TableNode/index';
import { Memo } from '@/features/memo/components';
import { RelationshipMarker } from './RelationshipMarker';
import { CustomControls } from './CustomControls';
import { CustomSmoothStepEdge } from './CustomSmoothStepEdge';
import { CustomConnectionLine } from './CustomConnectionLine';
import { TablePreview } from './TablePreview';
import { MemoPreview } from '@/features/memo/components';

const NODE_TYPES = {
  table: TableNode,
  memo: Memo,
};

const EDGE_TYPES = {
  customSmoothStep: CustomSmoothStepEdge,
};

const MINIMAP_NODE_COLOR = () => 'var(--color-schemafy-soft-blue)';

// React Flow uses style width/height for SVG viewBox math, so these must be numeric CSS pixels.
const MINIMAP_WIDTH = 176;
const MINIMAP_HEIGHT = 112;

const MINIMAP_STYLE = {
  backgroundColor: 'hsl(var(--schemafy-panel))',
  border: '1px solid hsl(var(--schemafy-glass-border))',
  boxShadow: 'none',
  borderRadius: '14px',
  overflow: 'hidden' as const,
  position: 'absolute' as const,
  bottom: '1.5rem',
  right: '1.5rem',
  margin: '0',
  backdropFilter: 'none',
  width: MINIMAP_WIDTH,
  height: MINIMAP_HEIGHT,
};

const PRO_OPTIONS = { hideAttribution: true };

const areSameNodes = (prev: Node[], next: Node[]) =>
  prev.length === next.length &&
  prev.every((node, index) => node === next[index]);

interface ReactFlowCanvasProps {
  tables: Node<TableData>[];
  memos: Node<MemoData>[];
  relationships: Edge[];
  activeTool: string;
  onTableDrag: (event: React.MouseEvent, node: Node<TableData>) => void;
  onTableDragStop: (event: React.MouseEvent, node: Node<TableData>) => void;
  onTablesDelete: (nodes: Node<TableData>[]) => void;
  onMemosChange: (changes: NodeChange[]) => void;
  onRelationshipsChange: (changes: EdgeChange[]) => void;
  onConnect: (connection: Connection) => void;
  onRelationshipClick: (event: React.MouseEvent, edge: Edge) => void;
  handleMoveEnd: (
    ...args: Parameters<
      NonNullable<React.ComponentProps<typeof ReactFlow>['onMoveEnd']>
    >
  ) => void;
  handlePaneClick: (event: React.MouseEvent) => void;
  handleMouseMove: (event: React.MouseEvent) => void;
}

const ReactFlowCanvasComponent = ({
  tables,
  memos,
  relationships,
  activeTool,
  onTableDrag,
  onTableDragStop,
  onTablesDelete,
  onMemosChange,
  onRelationshipsChange,
  onConnect,
  onRelationshipClick,
  handleMoveEnd,
  handlePaneClick,
  handleMouseMove,
}: ReactFlowCanvasProps) => {
  const tableIds = useMemo(() => new Set(tables.map((t) => t.id)), [tables]);
  const memoIds = useMemo(() => new Set(memos.map((m) => m.id)), [memos]);

  const [localNodes, setLocalNodes] = useState<Node[]>(() => [
    ...tables,
    ...memos,
  ]);

  useEffect(() => {
    setLocalNodes((previousNodes) => {
      const previousNodeMap = new Map(previousNodes.map((n) => [n.id, n]));

      const nextNodes = [...tables, ...memos].map((node) => {
        const prev = previousNodeMap.get(node.id);
        if (!prev || prev === node) return prev ?? node;

        if (
          prev.position.x === node.position.x &&
          prev.position.y === node.position.y &&
          prev.data === node.data
        ) {
          return prev;
        }

        return node;
      });

      return areSameNodes(previousNodes, nextNodes) ? previousNodes : nextNodes;
    });
  }, [tables, memos]);

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => {
      setLocalNodes((nds) => applyNodeChanges(changes, nds));

      const memoChanges: NodeChange[] = [];

      changes.forEach((change) => {
        if (!('id' in change)) return;
        if (change.type === 'position' && change.dragging) return;

        if (memoIds.has(change.id)) {
          memoChanges.push(change);
        }
      });

      if (memoChanges.length > 0) onMemosChange(memoChanges);
    },
    [memoIds, onMemosChange],
  );

  const handleNodeDragStop = useCallback(
    (event: React.MouseEvent, node: Node) => {
      if (tableIds.has(node.id)) {
        onTableDragStop(event, node as Node<TableData>);
      }
    },
    [tableIds, onTableDragStop],
  );

  const handleNodeDrag = useCallback(
    (event: React.MouseEvent, node: Node) => {
      if (tableIds.has(node.id)) {
        onTableDrag(event, node as Node<TableData>);
      }
    },
    [onTableDrag, tableIds],
  );

  const handleNodesDelete = useCallback(
    (deletedNodes: Node[]) => {
      const tableNodes: Node<TableData>[] = [];

      deletedNodes.forEach((node) => {
        if (tableIds.has(node.id)) {
          tableNodes.push(node as Node<TableData>);
        }
      });

      if (tableNodes.length > 0) onTablesDelete(tableNodes);
    },
    [tableIds, onTablesDelete],
  );

  return (
    <div
      style={{ cursor: activeTool === 'hand' ? 'grab' : 'default' }}
      className="h-full w-full bg-schemafy-canvas"
    >
      <RelationshipMarker />
      <ReactFlow
        nodes={localNodes}
        edges={relationships}
        onNodesChange={handleNodesChange}
        onNodeDrag={handleNodeDrag}
        onNodeDragStop={handleNodeDragStop}
        onNodesDelete={handleNodesDelete}
        onEdgesChange={onRelationshipsChange}
        onPaneClick={handlePaneClick}
        onPaneMouseMove={handleMouseMove}
        onMoveEnd={handleMoveEnd}
        nodesDraggable={activeTool !== 'hand'}
        elementsSelectable={activeTool !== 'hand'}
        panOnDrag={activeTool === 'hand'}
        onConnect={onConnect}
        onEdgeClick={onRelationshipClick}
        edgesReconnectable={false}
        nodeTypes={NODE_TYPES}
        edgeTypes={EDGE_TYPES}
        connectionLineComponent={CustomConnectionLine}
        proOptions={PRO_OPTIONS}
        connectionMode={ConnectionMode.Loose}
        fitView={false}
        minZoom={0.1}
        maxZoom={4}
        className="schemafy-scrollbar"
      >
        <MiniMap
          nodeColor={MINIMAP_NODE_COLOR}
          maskColor="hsl(var(--schemafy-bg) / 0.16)"
          style={MINIMAP_STYLE}
          zoomable
          pannable
        />
        <CustomControls />
        <Background
          variant={BackgroundVariant.Dots}
          color="var(--color-schemafy-canvas-dot)"
          gap={20}
          size={1}
        />
        {activeTool === 'table' && <TablePreview />}
        {activeTool === 'memo' && <MemoPreview />}
      </ReactFlow>
    </div>
  );
};

export const ReactFlowCanvas = memo(ReactFlowCanvasComponent);
