import { useState } from 'react';
import { X } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/Select';
import * as schemaService from '../services/schema.service';
import * as tableService from '../services/table.service';
import * as columnService from '../services/column.service';
import * as constraintService from '../services/constraint.service';
import * as indexService from '../services/index.service';
import * as relationshipService from '../services/relationship.service';

type ServiceType =
  | 'schema'
  | 'table'
  | 'column'
  | 'constraint'
  | 'index'
  | 'relationship';

type MethodType =
  | 'getSchema'
  | 'getSchemaTableList'
  | 'getTable'
  | 'getTableColumnList'
  | 'getTableRelationshipList'
  | 'getTableIndexList'
  | 'getTableConstraintList'
  | 'getColumn'
  | 'getConstraint'
  | 'getIndex'
  | 'getRelationship';

const SERVICE_METHODS: Record<
  ServiceType,
  { label: string; methods: { value: MethodType; label: string }[] }
> = {
  schema: {
    label: 'Schema Service',
    methods: [
      { value: 'getSchema', label: 'getSchema(schemaId)' },
      { value: 'getSchemaTableList', label: 'getSchemaTableList(schemaId)' },
    ],
  },
  table: {
    label: 'Table Service',
    methods: [
      { value: 'getTable', label: 'getTable(tableId)' },
      { value: 'getTableColumnList', label: 'getTableColumnList(tableId)' },
      {
        value: 'getTableRelationshipList',
        label: 'getTableRelationshipList(tableId)',
      },
      { value: 'getTableIndexList', label: 'getTableIndexList(tableId)' },
      {
        value: 'getTableConstraintList',
        label: 'getTableConstraintList(tableId)',
      },
    ],
  },
  column: {
    label: 'Column Service',
    methods: [{ value: 'getColumn', label: 'getColumn(columnId)' }],
  },
  constraint: {
    label: 'Constraint Service',
    methods: [{ value: 'getConstraint', label: 'getConstraint(constraintId)' }],
  },
  index: {
    label: 'Index Service',
    methods: [{ value: 'getIndex', label: 'getIndex(indexId)' }],
  },
  relationship: {
    label: 'Relationship Service',
    methods: [
      { value: 'getRelationship', label: 'getRelationship(relationshipId)' },
    ],
  },
};

export const ApiTester = () => {
  const [isMinimized, setIsMinimized] = useState(false);
  const [serviceType, setServiceType] = useState<ServiceType>('schema');
  const [method, setMethod] = useState<MethodType>('getSchema');
  const [inputId, setInputId] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleServiceChange = (service: ServiceType) => {
    setServiceType(service);
    setMethod(SERVICE_METHODS[service].methods[0].value);
  };

  const handleMethodChange = (selectedMethod: MethodType) => {
    setMethod(selectedMethod);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputId.trim()) {
      return;
    }

    setIsLoading(true);

    try {
      let result;

      switch (method) {
        case 'getSchema':
          result = await schemaService.getSchema(inputId);
          break;
        case 'getSchemaTableList':
          result = await schemaService.getSchemaTableList(inputId);
          break;
        case 'getTable':
          result = await tableService.getTable(inputId);
          break;
        case 'getTableColumnList':
          result = await tableService.getTableColumnList(inputId);
          break;
        case 'getTableRelationshipList':
          result = await tableService.getTableRelationshipList(inputId);
          break;
        case 'getTableIndexList':
          result = await tableService.getTableIndexList(inputId);
          break;
        case 'getTableConstraintList':
          result = await tableService.getTableConstraintList(inputId);
          break;
        case 'getColumn':
          result = await columnService.getColumn(inputId);
          break;
        case 'getConstraint':
          result = await constraintService.getConstraint(inputId);
          break;
        case 'getIndex':
          result = await indexService.getIndex(inputId);
          break;
        case 'getRelationship':
          result = await relationshipService.getRelationship(inputId);
          break;
        default:
          result = { error: 'Unknown method' };
      }

      console.log(JSON.stringify(result, null, 2));
    } catch (error) {
      console.log(
        `Error: ${error instanceof Error ? error.message : String(error)}`,
      );
    } finally {
      setIsLoading(false);
    }
  };

  if (isMinimized) {
    return (
      <div className="fixed bottom-4 left-4 z-50">
        <button
          onClick={() => setIsMinimized(false)}
          className="bg-schemafy-button-bg text-schemafy-button-text px-4 py-2 rounded shadow-lg hover:bg-schemafy-dark-gray-40 transition-colors"
        >
          API Tester
        </button>
      </div>
    );
  }

  return (
    <div className="fixed overflow-hidden top-4 left-4 z-50 bg-schemafy-bg border border-schemafy-dark-gray rounded-lg shadow-xl w-96 max-h-[80vh] flex flex-col">
      <div className="flex items-center justify-between p-3 border-b border-schemafy-dark-gray bg-schemafy-button-bg">
        <h3 className="text-sm font-semibold text-schemafy-button-text">
          API Tester
        </h3>
        <div className="flex gap-2">
          <button
            onClick={() => setIsMinimized(true)}
            className="text-schemafy-button-text hover:text-schemafy-yellow transition-colors"
            title="Minimize"
          >
            <span className="text-xs">
              <X size={16} />
            </span>
          </button>
        </div>
      </div>

      <form
        onSubmit={handleSubmit}
        className="p-3 space-y-3 flex-1 overflow-y-auto"
      >
        <div>
          <label className="block text-xs font-medium text-schemafy-text mb-1">
            Service
          </label>
          <Select value={serviceType} onValueChange={handleServiceChange}>
            <SelectTrigger className="w-full h-8 text-xs px-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {Object.entries(SERVICE_METHODS).map(([key, { label }]) => (
                <SelectItem key={key} value={key} className="text-xs">
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div>
          <label className="block text-xs font-medium text-schemafy-text mb-1">
            Method
          </label>
          <Select value={method} onValueChange={handleMethodChange}>
            <SelectTrigger className="w-full h-8 text-xs px-2">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SERVICE_METHODS[serviceType].methods.map(({ value, label }) => (
                <SelectItem key={value} value={value} className="text-xs">
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div>
          <label className="block text-xs font-medium text-schemafy-text mb-1">
            ID
          </label>
          <input
            type="text"
            value={inputId}
            onChange={(e) => setInputId(e.target.value)}
            placeholder="Enter ID..."
            className="w-full bg-schemafy-secondary text-schemafy-text border border-schemafy-dark-gray-40 rounded px-2 py-1 text-xs focus:outline-none focus:border-schemafy-yellow"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-schemafy-button-bg text-schemafy-button-text font-medium py-2 rounded hover:bg-opacity-90 transition-colors disabled:opacity-50 text-xs"
        >
          {isLoading ? 'Loading...' : 'Send Request'}
        </button>
      </form>
    </div>
  );
};
