MERGE INTO db_vendors (display_name, name, version, datatype_mappings, created_at, updated_at)
KEY (display_name)
VALUES (
    'MySQL 8.0',
    'mysql',
    '8.0',
    '{
      "schemaVersion": 1,
      "vendor": "mysql",
      "versionRange": ">= 8.0",
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
          "sqlType": "MEDIUMINT",
          "displayName": "MEDIUMINT",
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
          "sqlType": "DATETIME",
          "displayName": "DATETIME",
          "category": "datetime_timestamp",
          "parameters": []
        },
        {
          "sqlType": "TIMESTAMP",
          "displayName": "TIMESTAMP",
          "category": "datetime_timestamp",
          "parameters": []
        },
        {
          "sqlType": "YEAR",
          "displayName": "YEAR",
          "category": "datetime_year",
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
          "sqlType": "TEXT",
          "displayName": "TEXT",
          "category": "string_text",
          "parameters": []
        },
        {
          "sqlType": "MEDIUMTEXT",
          "displayName": "MEDIUMTEXT",
          "category": "string_text",
          "parameters": []
        },
        {
          "sqlType": "LONGTEXT",
          "displayName": "LONGTEXT",
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
          "sqlType": "MEDIUMBLOB",
          "displayName": "MEDIUMBLOB",
          "category": "binary_blob",
          "parameters": []
        },
        {
          "sqlType": "LONGBLOB",
          "displayName": "LONGBLOB",
          "category": "binary_blob",
          "parameters": []
        },
        {
          "sqlType": "ENUM",
          "displayName": "ENUM",
          "category": "string_enum",
          "sqlDeclarationTemplate": "ENUM({1})",
          "parameters": [
            {
              "name": "values",
              "label": "Values",
              "valueType": "string_array",
              "required": true,
              "order": 1
            }
          ]
        },
        {
          "sqlType": "SET",
          "displayName": "SET",
          "category": "string_set",
          "sqlDeclarationTemplate": "SET({1})",
          "parameters": [
            {
              "name": "values",
              "label": "Values",
              "valueType": "string_array",
              "required": true,
              "order": 1
            }
          ]
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

