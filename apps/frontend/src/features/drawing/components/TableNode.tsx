import { Handle, Position } from '@xyflow/react';

type TableNodeData = {
  tableName: string;
};

export const TableNode = ({ data }: { data: TableNodeData }) => {
  return (
    <div className="bg-schemafy-bg border-2 border-schemafy-button-bg rounded-lg shadow-md min-w-48 overflow-hidden">
      <Handle
        type="target"
        position={Position.Top}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="source"
        position={Position.Bottom}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="target"
        position={Position.Left}
        style={{ background: '#141414', width: 10, height: 10 }}
      />
      <Handle
        type="source"
        position={Position.Right}
        style={{ background: '#141414', width: 10, height: 10 }}
      />

      <div className="bg-schemafy-button-bg text-white p-2 flex items-center gap-2">
        <span>{data.tableName || 'Table'}</span>
      </div>

      <div className="p-2">
        <div className=" py-1">id (INT) PK</div>
        <div className="py-1">name (VARCHAR)</div>
        <div className="py-1">created_at (DATETIME)</div>
      </div>
    </div>
  );
};
