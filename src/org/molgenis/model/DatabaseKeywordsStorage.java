package org.molgenis.model;

public class DatabaseKeywordsStorage {

    private DatabaseKeywordsStorage() {
    }

    public static final String DB_DRIVER = "db_driver";
    public static final String DB_USER = "db_user";
    public static final String DB_PASSWORD = "db_password";
    public static final String DB_URI = "db_uri";
    public static final String MOLGENIS_PROPERTIES = "molgenis.properties";
    public static final String TABLE_NAME = "TABLE_NAME";
    public static final String INDEX_NAME = "INDEX_NAME";
    public static final String COLUMN_NAME = "COLUMN_NAME";
    public static final String DATA_TYPE = "DATA_TYPE";
    public static final String COLUMN_DEF = "COLUMN_DEF";
    public static final String REMARKS = "REMARKS";
    public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
    public static final String COLUMN_SIZE = "COLUMN_SIZE";
    public static final String NULLABLE = "NULLABLE";
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
    public static final String FOREIGN_KEY_COLUMN_NAME = "FKCOLUMN_NAME";
    public static final String PRIMARY_KEY_COLUMN_NAME = "PKTABLE_NAME";

    public static final String[] MOLGENIS_KEYWORDS =
            {"entity", "field", "form", "menu", "screen", "plugin"};

    public static final String[] HSQL_KEYWORDS =
            {"ALIAS", "ALTER", "AUTOCOMMIT", "CALL", "CHECKPOINT", "COMMIT", "CONNECT", "CREATE", "COLLATION", "COUNT",
                    "DATABASE", "DEFRAG", "DELAY", "DELETE", "DISCONNECT", "DROP", "END", "EXPLAIN", "EXTRACT", "GRANT",
                    "IGNORECASE", "INDEX", "INSERT", "INTEGRITY", "LOGSIZE", "PASSWORD", "POSITION", "PLAN", "PROPERTY",
                    "READONLY", "REFERENTIAl", "REVOKE", "ROLE", "ROLLBACK", "SAVEPOINT", "SCHEMA", "SCRIPT", "SCRIPTFORMAT",
                    "SELECT", "SEQUENCE", "SET", "SHUTDOWN", "SOURCE", "TABLE", "TRIGGER", "UPDATE", "USER", "VIEW", "WRITE"};
    /**
     * http://dev.mysql.com/doc/refman/5.0/en/reserved-words.html
     */
    public static final String[] MYSQL_KEYWORDS =
            {"Type", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT",
                    "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK",
                    "COLLATE", "COLUMN", "CONDITION", "CONNECTION", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS",
                    "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DATABASES",
                    "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT",
                    "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE",
                    "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN", "FALSE",
                    "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "GRANT", "GROUP",
                    "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "IN", "INDEX",
                    "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8",
                    "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", "LEADING", "LEAVE", "LEFT",
                    "LIKE", "LIMIT", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT",
                    "LOOP", "LOW_PRIORITY", "MATCH", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT",
                    "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NOT", "NO_WRITE_TO_BINLOG", "NULL",
                    "NUMERIC", "ON", "OPTIMIZE", "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "PRECISION",
                    "PRIMARY", "PROCEDURE", "PURGE", "RAID0", "READ", "READS", "REAL", "REFERENCES", "REGEXP", "RELEASE",
                    "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA",
                    "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SMALLINT", "SONAME",
                    "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT",
                    "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", "STRAIGHT_JOIN", "TABLE", "TERMINATED",
                    "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE",
                    "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES",
                    "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING", "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "X509",
                    "XOR", "YEAR_MONTH", "ZEROFILL"};
    /**
     * https://cis.med.ucalgary.ca/http/java.sun.com/docs/books/tutorial/java/
     * nutsandbolts/_keywords.html
     */
    public static final String[] JAVA_KEYWORDS =
            {"abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized",
                    "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte",
                    "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch",
                    "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally",
                    "long", "strictfp", "volatile", "const", "float", "native", "super", "while"};

    public static final String[] JAVASCRIPT_KEYWORDS =
            {"function"};

    public static final String[] ORACLE_KEYWORDS =
            {

                    "ACCESS", "ELSE", "MODIFY", "START", "ADD", "EXCLUSIVE", "NOAUDIT", "SELECT", "ALL", "EXISTS", "NOCOMPRESS",
                    "SESSION", "ALTER", "FILE", "NOT", "SET", "AND", "FLOAT", "NOTFOUND", "SHARE", "ANY", "FOR", "NOWAIT",
                    "SIZE", "ARRAYLEN", "FROM", "NULL", "SMALLINT", "AS", "GRANT", "NUMBER", "SQLBUF", "ASC", "GROUP", "OF",
                    "SUCCESSFUL", "AUDIT", "HAVING", "OFFLINE", "SYNONYM", "BETWEEN", "IDENTIFIED", "ON", "SYSDATE", "BY",
                    "IMMEDIATE", "ONLINE", "TABLE", "CHAR", "IN", "OPTION", "THEN", "CHECK", "INCREMENT", "OR", "TO",
                    "CLUSTER", "INDEX", "ORDER", "TRIGGER", "COLUMN", "INITIAL", "PCTFREE", "UID", "COMMENT", "INSERT",
                    "PRIOR", "UNION", "COMPRESS", "INTEGER", "PRIVILEGES", "UNIQUE", "CONNECT", "INTERSECT", "PUBLIC",
                    "UPDATE", "CREATE", "INTO", "RAW", "USER", "CURRENT", "IS", "RENAME", "VALIDATE", "DATE", "LEVEL",
                    "RESOURCE", "VALUES", "DECIMAL", "LIKE", "REVOKE", "VARCHAR", "DEFAULT", "LOCK", "ROW", "VARCHAR2",
                    "DELETE", "LONG", "ROWID", "VIEW", "DESC", "MAXEXTENTS", "ROWLABEL", "WHENEVER", "DISTINCT", "MINUS",
                    "ROWNUM", "WHERE", "DROP", "MODE", "ROWS", "WITH"};
}
