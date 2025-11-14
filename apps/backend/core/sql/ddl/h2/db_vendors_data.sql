MERGE INTO db_vendors (display_name, name, version, datatype_mappings, created_at, updated_at)
KEY (display_name)
VALUES (
    'H2 2.2',
    'h2',
    '2.2',
    '{
      "schemaVersion": 1,
      "vendor": "h2",
      "versionRange": ">= 2.0",
      "types": [
        {
          "sqlType": "TINYINT",
          "displayName": "TINYINT",
          "category": "numeric_integer",
          "parameters": []
        },
        {
          "sqlType": "SMALLINT",
          "displayName": "SMALLINT",
          "category": "numeric_integer",
          "parameters": []
        },
        {
          "sqlType": "INTEGER",
          "displayName": "INTEGER",
          "category": "numeric_integer",
          "parameters": []
        },
        {
          "sqlType": "INT",
          "displayName": "INT",
          "category": "numeric_integer",
          "parameters": []
        },
        {
          "sqlType": "BIGINT",
          "displayName": "BIGINT",
          "category": "numeric_integer",
          "parameters": []
        },
        {
          "sqlType": "DECIMAL",
          "displayName": "DECIMAL",
          "category": "numeric_decimal",
          "sqlDeclarationTemplate": "DECIMAL({1}, {2})",
          "parameters": [
            {
              "name": "precision",
              "label": "Precision (M)",
              "valueType": "integer",
              "required": true,
              "order": 1
            },
            {
              "name": "scale",
              "label": "Scale (D)",
              "valueType": "integer",
              "required": true,
              "order": 2
            }
          ]
        },
        {
          "sqlType": "NUMERIC",
          "displayName": "NUMERIC",
          "category": "numeric_decimal",
          "sqlDeclarationTemplate": "NUMERIC({1}, {2})",
          "parameters": [
            {
              "name": "precision",
              "label": "Precision (M)",
              "valueType": "integer",
              "required": true,
              "order": 1
            },
            {
              "name": "scale",
              "label": "Scale (D)",
              "valueType": "integer",
              "required": true,
              "order": 2
            }
          ]
        },
        {
          "sqlType": "REAL",
          "displayName": "REAL",
          "category": "numeric_float",
          "parameters": []
        },
        {
          "sqlType": "FLOAT",
          "displayName": "FLOAT",
          "category": "numeric_float",
          "parameters": []
        },
        {
          "sqlType": "DOUBLE",
          "displayName": "DOUBLE",
          "category": "numeric_float",
          "parameters": []
        },
        {
          "sqlType": "BOOLEAN",
          "displayName": "BOOLEAN",
          "category": "boolean",
          "parameters": []
        },
        {
          "sqlType": "BIT",
          "displayName": "BIT",
          "category": "numeric_bit",
          "sqlDeclarationTemplate": "BIT({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Bits",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "DATE",
          "displayName": "DATE",
          "category": "datetime_date",
          "parameters": []
        },
        {
          "sqlType": "TIME",
          "displayName": "TIME",
          "category": "datetime_time",
          "parameters": []
        },
        {
          "sqlType": "TIMESTAMP",
          "displayName": "TIMESTAMP",
          "category": "datetime_timestamp",
          "parameters": []
        },
        {
          "sqlType": "TIMESTAMP WITH TIME ZONE",
          "displayName": "TIMESTAMP WITH TIME ZONE",
          "category": "datetime_timestamp",
          "parameters": []
        },
        {
          "sqlType": "CHAR",
          "displayName": "CHAR",
          "category": "string_fixed",
          "sqlDeclarationTemplate": "CHAR({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "VARCHAR",
          "displayName": "VARCHAR",
          "category": "string_variable",
          "sqlDeclarationTemplate": "VARCHAR({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "VARCHAR_IGNORECASE",
          "displayName": "VARCHAR_IGNORECASE",
          "category": "string_variable",
          "sqlDeclarationTemplate": "VARCHAR_IGNORECASE({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "CHARACTER VARYING",
          "displayName": "CHARACTER VARYING",
          "category": "string_variable",
          "sqlDeclarationTemplate": "CHARACTER VARYING({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "BINARY",
          "displayName": "BINARY",
          "category": "binary_fixed",
          "sqlDeclarationTemplate": "BINARY({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "VARBINARY",
          "displayName": "VARBINARY",
          "category": "binary_variable",
          "sqlDeclarationTemplate": "VARBINARY({1})",
          "parameters": [
            {
              "name": "length",
              "label": "Length",
              "valueType": "integer",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "CLOB",
          "displayName": "CLOB",
          "category": "string_text",
          "parameters": []
        },
        {
          "sqlType": "BLOB",
          "displayName": "BLOB",
          "category": "binary_blob",
          "parameters": []
        },
        {
          "sqlType": "UUID",
          "displayName": "UUID",
          "category": "uuid",
          "parameters": []
        },
        {
          "sqlType": "ARRAY",
          "displayName": "ARRAY",
          "category": "array",
          "sqlDeclarationTemplate": "ARRAY",
          "parameters": []
        },
        {
          "sqlType": "JSON",
          "displayName": "JSON",
          "category": "json",
          "parameters": []
        }
      ]
    }',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

