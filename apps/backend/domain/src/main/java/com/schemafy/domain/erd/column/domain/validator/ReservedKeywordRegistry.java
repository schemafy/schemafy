package com.schemafy.domain.erd.column.domain.validator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReservedKeywordRegistry {

  private static final Set<String> MYSQL_RESERVED_KEYWORDS = Set.of(
      "SELECT", "INSERT", "UPDATE", "DELETE", "REPLACE",
      "CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME",
      "TABLE", "COLUMN", "INDEX", "KEY", "PRIMARY", "FOREIGN", "REFERENCES",
      "CONSTRAINT", "UNIQUE", "DEFAULT", "AUTO_INCREMENT",
      "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "CROSS",
      "ON", "USING", "NATURAL",
      "ORDER", "GROUP", "BY", "HAVING", "ASC", "DESC",
      "UNION", "INTERSECT", "EXCEPT", "ALL", "DISTINCT",
      "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE",
      "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "IF",
      "AS", "CAST", "CONVERT", "COALESCE", "NULLIF",
      "LIMIT", "OFFSET", "FETCH", "FIRST", "NEXT", "ROWS", "ONLY",
      "BEGIN", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION",
      "GRANT", "REVOKE", "PRIVILEGES",
      "DATABASE", "SCHEMA", "USE", "SHOW", "DESCRIBE", "EXPLAIN",
      "SET", "VALUES", "INTO", "PROCEDURE", "FUNCTION", "TRIGGER", "VIEW",
      "CURSOR", "DECLARE", "HANDLER", "LOOP", "WHILE", "REPEAT", "LEAVE",
      "ITERATE", "RETURN", "CALL", "DO",
      "ANALYZE", "OPTIMIZE", "REPAIR", "LOCK", "UNLOCK", "FORCE",
      "IGNORE", "DELAYED", "HIGH_PRIORITY", "LOW_PRIORITY",
      "PARTITION", "RANGE", "LIST", "HASH", "LINEAR",
      "FULLTEXT", "SPATIAL", "MATCH", "AGAINST",
      "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "MEDIUMINT",
      "FLOAT", "DOUBLE", "DECIMAL", "NUMERIC", "REAL",
      "CHAR", "VARCHAR", "TEXT", "BLOB", "BINARY", "VARBINARY",
      "DATE", "TIME", "DATETIME", "TIMESTAMP", "YEAR",
      "BOOLEAN", "BOOL", "BIT", "JSON", "ENUM",
      "ADD", "CHANGE", "MODIFY", "AFTER", "BEFORE",
      "CASCADE", "RESTRICT", "NO", "ACTION",
      "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
      "LOCALTIME", "LOCALTIMESTAMP",
      "INTERVAL", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER",
      "SECOND", "MICROSECOND",
      "DIV", "MOD", "XOR", "REGEXP", "RLIKE",
      "OUTFILE", "INFILE", "LOAD", "DATA", "TERMINATED", "ENCLOSED", "ESCAPED",
      "LINES", "STARTING", "OPTIONALLY",
      "REQUIRE", "SSL", "X509", "CIPHER", "ISSUER", "SUBJECT",
      "WITH", "READ", "WRITE", "BOTH", "LEADING", "TRAILING",
      "COLLATE", "CHARACTER", "CHARSET",
      "ENGINE", "STORAGE", "MEMORY", "TEMPORARY",
      "ZEROFILL", "UNSIGNED", "SIGNED",
      "AVG", "COUNT", "MAX", "MIN", "SUM", "GROUP_CONCAT",
      "OVER", "WINDOW", "PRECEDING", "FOLLOWING", "UNBOUNDED", "CURRENT");

  private static final Map<String, Set<String>> KEYWORDS_BY_VENDOR = Map.of(
      "mysql", MYSQL_RESERVED_KEYWORDS,
      "mariadb", MYSQL_RESERVED_KEYWORDS);

  private ReservedKeywordRegistry() {}

  public static Set<String> getKeywords(String dbVendorName) {
    if (dbVendorName == null) {
      return MYSQL_RESERVED_KEYWORDS;
    }
    return KEYWORDS_BY_VENDOR.getOrDefault(
        dbVendorName.toLowerCase(Locale.ROOT),
        MYSQL_RESERVED_KEYWORDS);
  }

  public static boolean isReserved(String dbVendorName, String name) {
    if (name == null) {
      return false;
    }
    return getKeywords(dbVendorName)
        .contains(name.trim().toUpperCase(Locale.ROOT));
  }

}
