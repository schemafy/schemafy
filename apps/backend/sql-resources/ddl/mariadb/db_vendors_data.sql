INSERT INTO db_vendors (
    id, display_name, name, version, datatype_mappings, capabilities, created_at, updated_at)
VALUES (
    1,
    'MySQL 8.0',
    'mysql',
    '8.0',
    '{
      "schemaVersion": 2,
      "vendor": "mysql",
      "versionRange": ">= 8.0 < 9.0",
      "types": [
        {
          "sqlType": "TINYINT",
          "aliases": ["BOOL", "BOOLEAN"],
          "displayName": "TINYINT",
          "category": "numeric_integer",
          "parameters": [],
          "sqlDeclarationTemplate": "TINYINT",
          "properties": {"autoIncrementAllowed": true, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "tinyint"}
        },
        {
          "sqlType": "SMALLINT",
          "aliases": [],
          "displayName": "SMALLINT",
          "category": "numeric_integer",
          "parameters": [],
          "sqlDeclarationTemplate": "SMALLINT",
          "properties": {"autoIncrementAllowed": true, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "smallint"}
        },
        {
          "sqlType": "MEDIUMINT",
          "aliases": [],
          "displayName": "MEDIUMINT",
          "category": "numeric_integer",
          "parameters": [],
          "sqlDeclarationTemplate": "MEDIUMINT",
          "properties": {"autoIncrementAllowed": true, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "mediumint"}
        },
        {
          "sqlType": "INT",
          "aliases": ["INTEGER"],
          "displayName": "INT",
          "category": "numeric_integer",
          "parameters": [],
          "sqlDeclarationTemplate": "INT",
          "properties": {"autoIncrementAllowed": true, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "int"}
        },
        {
          "sqlType": "BIGINT",
          "aliases": [],
          "displayName": "BIGINT",
          "category": "numeric_integer",
          "parameters": [],
          "sqlDeclarationTemplate": "BIGINT",
          "properties": {"autoIncrementAllowed": true, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "bigint"}
        },
        {
          "sqlType": "DECIMAL",
          "aliases": ["NUMERIC", "DEC", "FIXED"],
          "displayName": "DECIMAL",
          "category": "numeric_decimal",
          "parameters": [
            {"name": "precision", "label": "Precision (M)", "valueType": "integer", "required": false, "order": 1, "minValue": 1, "maxValue": 65},
            {"name": "scale", "label": "Scale (D)", "valueType": "integer", "required": false, "order": 2, "minValue": 0, "maxValue": 30}
          ],
          "sqlDeclarationTemplate": "DECIMAL[({precision}[, {scale}])]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "decimal"}
        },
        {
          "sqlType": "FLOAT",
          "aliases": [],
          "displayName": "FLOAT",
          "category": "numeric_float",
          "parameters": [
            {"name": "length", "label": "Precision (p)", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 53}
          ],
          "sqlDeclarationTemplate": "FLOAT[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "float"}
        },
        {
          "sqlType": "DOUBLE",
          "aliases": ["REAL"],
          "displayName": "DOUBLE",
          "category": "numeric_float",
          "parameters": [],
          "sqlDeclarationTemplate": "DOUBLE",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "double"}
        },
        {
          "sqlType": "BIT",
          "aliases": [],
          "displayName": "BIT",
          "category": "numeric_bit",
          "parameters": [
            {"name": "length", "label": "Bits", "valueType": "integer", "required": false, "order": 1, "minValue": 1, "maxValue": 64}
          ],
          "sqlDeclarationTemplate": "BIT[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "bit"}
        },
        {
          "sqlType": "DATE",
          "aliases": [],
          "displayName": "DATE",
          "category": "datetime_date",
          "parameters": [],
          "sqlDeclarationTemplate": "DATE",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "date"}
        },
        {
          "sqlType": "TIME",
          "aliases": [],
          "displayName": "TIME",
          "category": "datetime_time",
          "parameters": [
            {"name": "length", "label": "Fractional seconds", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 6}
          ],
          "sqlDeclarationTemplate": "TIME[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "time"}
        },
        {
          "sqlType": "DATETIME",
          "aliases": [],
          "displayName": "DATETIME",
          "category": "datetime_timestamp",
          "parameters": [
            {"name": "length", "label": "Fractional seconds", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 6}
          ],
          "sqlDeclarationTemplate": "DATETIME[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "datetime"}
        },
        {
          "sqlType": "TIMESTAMP",
          "aliases": [],
          "displayName": "TIMESTAMP",
          "category": "datetime_timestamp",
          "parameters": [
            {"name": "length", "label": "Fractional seconds", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 6}
          ],
          "sqlDeclarationTemplate": "TIMESTAMP[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "timestamp"}
        },
        {
          "sqlType": "YEAR",
          "aliases": [],
          "displayName": "YEAR",
          "category": "datetime_year",
          "parameters": [],
          "sqlDeclarationTemplate": "YEAR",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "year"}
        },
        {
          "sqlType": "CHAR",
          "aliases": [],
          "displayName": "CHAR",
          "category": "string_fixed",
          "parameters": [
            {"name": "length", "label": "Length", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 255}
          ],
          "sqlDeclarationTemplate": "CHAR[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["BTREE", "FULLTEXT"], "foreignKeyGroup": "character"}
        },
        {
          "sqlType": "VARCHAR",
          "aliases": [],
          "displayName": "VARCHAR",
          "category": "string_variable",
          "parameters": [
            {"name": "length", "label": "Length", "valueType": "integer", "required": true, "order": 1, "minValue": 0, "maxValue": 65535}
          ],
          "sqlDeclarationTemplate": "VARCHAR({length})",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["BTREE", "FULLTEXT"], "foreignKeyGroup": "character"}
        },
        {
          "sqlType": "TINYTEXT",
          "aliases": [],
          "displayName": "TINYTEXT",
          "category": "string_text",
          "parameters": [],
          "sqlDeclarationTemplate": "TINYTEXT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["FULLTEXT"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "TEXT",
          "aliases": [],
          "displayName": "TEXT",
          "category": "string_text",
          "parameters": [],
          "sqlDeclarationTemplate": "TEXT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["FULLTEXT"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "MEDIUMTEXT",
          "aliases": [],
          "displayName": "MEDIUMTEXT",
          "category": "string_text",
          "parameters": [],
          "sqlDeclarationTemplate": "MEDIUMTEXT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["FULLTEXT"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "LONGTEXT",
          "aliases": [],
          "displayName": "LONGTEXT",
          "category": "string_text",
          "parameters": [],
          "sqlDeclarationTemplate": "LONGTEXT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["FULLTEXT"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "ENUM",
          "aliases": [],
          "displayName": "ENUM",
          "category": "string_enum",
          "parameters": [
            {"name": "values", "label": "Values", "valueType": "string_array", "required": true, "order": 1, "minItems": 1, "maxItems": 65535, "minItemLength": 1, "maxItemLength": 255}
          ],
          "sqlDeclarationTemplate": "ENUM({values})",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["BTREE"], "foreignKeyGroup": "character"}
        },
        {
          "sqlType": "SET",
          "aliases": [],
          "displayName": "SET",
          "category": "string_set",
          "parameters": [
            {"name": "values", "label": "Values", "valueType": "string_array", "required": true, "order": 1, "minItems": 1, "maxItems": 64, "minItemLength": 1, "maxItemLength": 255}
          ],
          "sqlDeclarationTemplate": "SET({values})",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": true, "indexTypes": ["BTREE"], "foreignKeyGroup": "character"}
        },
        {
          "sqlType": "BINARY",
          "aliases": [],
          "displayName": "BINARY",
          "category": "binary_fixed",
          "parameters": [
            {"name": "length", "label": "Length", "valueType": "integer", "required": false, "order": 1, "minValue": 0, "maxValue": 255}
          ],
          "sqlDeclarationTemplate": "BINARY[({length})]",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "binary"}
        },
        {
          "sqlType": "VARBINARY",
          "aliases": [],
          "displayName": "VARBINARY",
          "category": "binary_variable",
          "parameters": [
            {"name": "length", "label": "Length", "valueType": "integer", "required": true, "order": 1, "minValue": 0, "maxValue": 65535}
          ],
          "sqlDeclarationTemplate": "VARBINARY({length})",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["BTREE"], "foreignKeyGroup": "binary"}
        },
        {
          "sqlType": "TINYBLOB",
          "aliases": [],
          "displayName": "TINYBLOB",
          "category": "binary_blob",
          "parameters": [],
          "sqlDeclarationTemplate": "TINYBLOB",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": [], "foreignKeyGroup": null}
        },
        {
          "sqlType": "BLOB",
          "aliases": [],
          "displayName": "BLOB",
          "category": "binary_blob",
          "parameters": [],
          "sqlDeclarationTemplate": "BLOB",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": [], "foreignKeyGroup": null}
        },
        {
          "sqlType": "MEDIUMBLOB",
          "aliases": [],
          "displayName": "MEDIUMBLOB",
          "category": "binary_blob",
          "parameters": [],
          "sqlDeclarationTemplate": "MEDIUMBLOB",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": [], "foreignKeyGroup": null}
        },
        {
          "sqlType": "LONGBLOB",
          "aliases": [],
          "displayName": "LONGBLOB",
          "category": "binary_blob",
          "parameters": [],
          "sqlDeclarationTemplate": "LONGBLOB",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": [], "foreignKeyGroup": null}
        },
        {
          "sqlType": "JSON",
          "aliases": [],
          "displayName": "JSON",
          "category": "json",
          "parameters": [],
          "sqlDeclarationTemplate": "JSON",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": [], "foreignKeyGroup": null}
        },
        {
          "sqlType": "GEOMETRY",
          "aliases": [],
          "displayName": "GEOMETRY",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "GEOMETRY",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "POINT",
          "aliases": [],
          "displayName": "POINT",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "POINT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "LINESTRING",
          "aliases": [],
          "displayName": "LINESTRING",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "LINESTRING",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "POLYGON",
          "aliases": [],
          "displayName": "POLYGON",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "POLYGON",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "MULTIPOINT",
          "aliases": [],
          "displayName": "MULTIPOINT",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "MULTIPOINT",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "MULTILINESTRING",
          "aliases": [],
          "displayName": "MULTILINESTRING",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "MULTILINESTRING",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "MULTIPOLYGON",
          "aliases": [],
          "displayName": "MULTIPOLYGON",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "MULTIPOLYGON",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        },
        {
          "sqlType": "GEOMETRYCOLLECTION",
          "aliases": [],
          "displayName": "GEOMETRYCOLLECTION",
          "category": "spatial",
          "parameters": [],
          "sqlDeclarationTemplate": "GEOMETRYCOLLECTION",
          "properties": {"autoIncrementAllowed": false, "charsetCollationAllowed": false, "indexTypes": ["SPATIAL"], "foreignKeyGroup": null}
        }
      ]
    }',
    '{
      "schemaVersion": 2,
      "indexes": {
        "supportedTypes": ["BTREE", "FULLTEXT", "SPATIAL"],
        "sortDirectionTypes": ["BTREE"]
      },
      "identifiers": {
        "maxLength": 64,
        "lengthUnit": "CODE_POINTS"
      }
    }',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    datatype_mappings = VALUES(datatype_mappings),
    capabilities = VALUES(capabilities),
    updated_at = CURRENT_TIMESTAMP;
