<?php
/**
 * Doctrine Entity Generator for PostgreSQL
 *
 * Generates Symfony 6.4+ Entity classes with PHP attributes
 * from PostgreSQL information_schema
 *
 * Usage: php doctrine-reverse-engineer.php
 */

require_once "vendor/autoload.php";

use Symfony\Component\Dotenv\Dotenv;

// Load environment variables
$dotenv = new Dotenv();
$dotenv->loadEnv(".env");

// Database configuration from .env
$databaseUrl = $_ENV["DATABASE_URL"];

// Parse DATABASE_URL format: postgresql://user:pass@host:port/dbname?sslmode=require
if (
    !preg_match(
        '/postgresql:\/\/([^:]+):([^@]+)@([^:]+):(\d+)\/(.+?)(\?.*)?$/',
        $databaseUrl,
        $matches,
    )
) {
    die(
        "Invalid DATABASE_URL format. Expected: postgresql://user:pass@host:port/dbname\n"
    );
}

$dbUser = $matches[1];
$dbPass = $matches[2];
$dbHost = $matches[3];
$dbPort = $matches[4];
$dbName = $matches[5];

// Entity namespace and output directory
$namespace = "App\\Entity";
$outputDir = __DIR__ . "/src/Entity";

// Create output directory if it doesn't exist
if (!is_dir($outputDir)) {
    mkdir($outputDir, 0777, true);
    echo "✓ Created output directory: $outputDir\n";
}

// Connect to PostgreSQL database
try {
    $dsn = "pgsql:host=$dbHost;port=$dbPort;dbname=$dbName;sslmode=require";
    $pdo = new PDO($dsn, $dbUser, $dbPass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "✓ Connected to PostgreSQL database successfully!\n";
} catch (PDOException $e) {
    die("❌ Database connection failed: " . $e->getMessage() . "\n");
}

// ============================================================================
// STEP 1: Retrieve all tables from the public schema
// ============================================================================

echo "\n📋 Scanning database schema...\n";

$stmt = $pdo->query("
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    ORDER BY table_name
");
$tables = $stmt->fetchAll(PDO::FETCH_COLUMN);

if (empty($tables)) {
    die("❌ No tables found in public schema!\n");
}

echo "✓ Found " . count($tables) . " table(s)\n";

// ============================================================================
// STEP 2: Collect table structure, constraints, and relationships
// ============================================================================

$tableInfo = [];
$foreignKeys = [];
$primaryKeys = [];
$uniqueConstraints = [];
$manyToManyTables = [];

foreach ($tables as $table) {
    // Skip system tables and migration tables
    if (
        strpos($table, "migration") !== false ||
        strpos($table, "doctrine") !== false ||
        strpos($table, "_prisma_") !== false
    ) {
        continue;
    }

    echo "\n→ Analyzing table: `$table`\n";

    // Convert table name to class name
    $className = tableNameToClassName($table);
    echo "  • Class name: $className\n";

    // ========== GET COLUMNS ==========
    $stmt = $pdo->query("
        SELECT
            column_name,
            data_type,
            is_nullable,
            column_default,
            character_maximum_length,
            numeric_precision,
            numeric_scale
        FROM information_schema.columns
        WHERE table_name = '$table' AND table_schema = 'public'
        ORDER BY ordinal_position
    ");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (empty($columns)) {
        echo "  ⚠ No columns found for table $table, skipping...\n";
        continue;
    }

    echo "  • Found " . count($columns) . " column(s)\n";

    // ========== GET PRIMARY KEY ==========
    $stmt = $pdo->query("
        SELECT a.attname
        FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid
            AND a.attnum = ANY(i.indkey)
        JOIN pg_class t ON t.oid = i.indrelid
        WHERE t.relname = '$table' AND i.indisprimary
        LIMIT 1
    ");
    $pkResult = $stmt->fetch(PDO::FETCH_ASSOC);
    $primaryKey = $pkResult ? $pkResult["attname"] : "id";

    echo "  • Primary key: $primaryKey\n";
    $primaryKeys[$table] = $primaryKey;

    // ========== GET FOREIGN KEYS ==========
    $stmt = $pdo->query("
        SELECT
            tc.constraint_name,
            kcu.column_name,
            ccu.table_name AS referenced_table,
            ccu.column_name AS referenced_column
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.key_column_usage AS kcu
            ON tc.constraint_name = kcu.constraint_name
            AND tc.table_schema = kcu.table_schema
        JOIN information_schema.constraint_column_usage AS ccu
            ON ccu.constraint_name = tc.constraint_name
            AND ccu.table_schema = tc.table_schema
        WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = '$table'
    ");
    $fkRows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (!empty($fkRows)) {
        $foreignKeys[$table] = [];
        foreach ($fkRows as $fk) {
            $foreignKeys[$table][] = [
                "column" => $fk["column_name"],
                "refTable" => $fk["referenced_table"],
                "refColumn" => $fk["referenced_column"],
            ];
        }
        echo "  • Foreign keys: " . count($fkRows) . "\n";
    }

    // ========== GET UNIQUE CONSTRAINTS (for OneToOne) ==========
    $stmt = $pdo->query("
        SELECT
            tc.constraint_name,
            kcu.column_name
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.key_column_usage AS kcu
            ON tc.constraint_name = kcu.constraint_name
            AND tc.table_schema = kcu.table_schema
        WHERE tc.constraint_type = 'UNIQUE' AND tc.table_name = '$table'
    ");
    $uniqueRows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (!empty($uniqueRows)) {
        $uniqueConstraints[$table] = $uniqueRows;
    }

    $tableInfo[$table] = [
        "className" => $className,
        "columns" => $columns,
        "primaryKey" => $primaryKey,
    ];
}

// ========== IDENTIFY MANY-TO-MANY TABLES ==========
echo "\n\n🔍 Detecting many-to-many relationships...\n";

foreach ($tables as $table) {
    if (
        strpos($table, "migration") !== false ||
        strpos($table, "doctrine") !== false ||
        !isset($foreignKeys[$table])
    ) {
        continue;
    }

    $fkCount = count($foreignKeys[$table]);
    $columns = $tableInfo[$table]["columns"];
    $columnCount = count($columns);

    // Join table heuristic: has 2+ FKs and mostly just FKs (max 1 extra column for metadata)
    if ($fkCount >= 2 && $fkCount >= $columnCount - 1) {
        $manyToManyTables[$table] = [
            "foreignKeys" => $foreignKeys[$table],
        ];
        echo "✓ Detected many-to-many table: `$table` (has $fkCount FKs from $columnCount columns)\n";
    }
}

// ============================================================================
// STEP 3: Generate Entity Classes
// ============================================================================

echo "\n\n🚀 Generating Entity classes...\n";

foreach ($tables as $table) {
    // Skip system and join tables
    if (
        strpos($table, "migration") !== false ||
        strpos($table, "doctrine") !== false ||
        isset($manyToManyTables[$table])
    ) {
        continue;
    }

    $className = $tableInfo[$table]["className"];
    $columns = $tableInfo[$table]["columns"];
    $primaryKey = $tableInfo[$table]["primaryKey"];

    echo "\n→ Generating: $className\n";

    // Generate class code
    $classCode = generateEntityClass(
        $className,
        $namespace,
        $table,
        $columns,
        $primaryKey,
        $foreignKeys[$table] ?? [],
        $uniqueConstraints[$table] ?? [],
        $tableInfo,
        $manyToManyTables,
    );

    // Write to file
    $filePath = $outputDir . "/" . $className . ".php";
    file_put_contents($filePath, $classCode);
    echo "  ✓ Written to: $filePath\n";
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Convert table name to PHP class name
 * Example: user_profiles -> UserProfile
 */
function tableNameToClassName($tableName)
{
    // Split on underscores and capitalize each word
    $parts = explode("_", $tableName);
    $className = implode("", array_map("ucfirst", $parts));

    // Remove trailing 's' for singular form if not double 's'
    if (
        strlen($className) > 1 &&
        substr($className, -1) === "s" &&
        substr($className, -2) !== "ss"
    ) {
        // Be careful with words that naturally end in 's'
        $singularExceptions = ["Address", "Class", "Status", "Process"];
        if (!in_array($className, $singularExceptions)) {
            $className = substr($className, 0, -1);
        }
    }

    return $className;
}

/**
 * Map PostgreSQL data types to PHP types
 */
function mapPostgreSQLTypeToPhpType(
    $pgType,
    $columnDefault = null,
    $isNullable = true,
) {
    $pgType = strtolower($pgType);

    // Handle arrays
    if (strpos($pgType, "[]") !== false) {
        return "array";
    }

    $phpType = match ($pgType) {
        "boolean", "bool" => "bool",
        "smallint", "integer", "bigint" => "int",
        "decimal", "numeric" => "float",
        "real", "double precision" => "float",
        "smallserial", "serial", "bigserial" => "int",
        "date" => "\\DateTimeInterface",
        "time", "time without time zone" => "\\DateTimeInterface",
        "timestamp",
        "timestamp without time zone",
        "timestamp with time zone"
            => "\\DateTimeInterface",
        "interval" => "\\DateInterval",
        "json", "jsonb" => "array",
        "uuid" => "string",
        "bytea" => "string",
        "text", "character varying", "character" => "string",
        default => "string",
    };

    // Add |null if nullable
    if ($isNullable && !in_array($phpType, ["array", "mixed"])) {
        $phpType .= "|null";
    }

    return $phpType;
}

/**
 * Map PostgreSQL data types to Doctrine types
 */
function mapPostgreSQLTypeToDoctrineType($pgType)
{
    $pgType = strtolower($pgType);

    return match ($pgType) {
        "boolean", "bool" => "boolean",
        "smallint" => "smallint",
        "integer" => "integer",
        "bigint" => "bigint",
        "decimal", "numeric" => "decimal",
        "real" => "float",
        "double precision" => "float",
        "smallserial" => "smallint",
        "serial" => "integer",
        "bigserial" => "bigint",
        "character varying", "varchar" => "string",
        "character", "char" => "string",
        "text" => "text",
        "date" => "date",
        "time", "time without time zone" => "time",
        "timestamp",
        "timestamp without time zone",
        "timestamp with time zone"
            => "datetime",
        "interval" => "string",
        "uuid" => "string",
        "json" => "json",
        "jsonb" => "json",
        "bytea" => "blob",
        default => "string",
    };
}

/**
 * Column name to property name
 * Example: created_at -> createdAt
 */
function columnNameToPropertyName($columnName)
{
    return lcfirst(
        str_replace(" ", "", ucwords(str_replace("_", " ", $columnName))),
    );
}

/**
 * Generate the full Entity class code
 */
function generateEntityClass(
    $className,
    $namespace,
    $table,
    $columns,
    $primaryKey,
    $foreignKeys = [],
    $uniqueConstraints = [],
    $tableInfo = [],
    $manyToManyTables = [],
) {
    // Build use statements and attributes
    $uses = [];
    $classAttributes = [];
    $properties = [];
    $methods = [];
    $constructorCode = "";
    $constructorParams = [];

    // Add ORM use statements
    $uses[] = "use Doctrine\ORM\Mapping as ORM;";
    $uses[] = "use Doctrine\Common\Collections\ArrayCollection;";
    $uses[] = "use Doctrine\Common\Collections\Collection;";

    // Entity class attribute
    $classAttributes[] = "#[ORM\\Entity(repositoryClass: {$namespace}\\Repository\\{$className}Repository::class)]";
    $classAttributes[] = "#[ORM\\Table(name: '$table')]";

    // Process columns
    $fkColumns = [];
    foreach ($foreignKeys as $fk) {
        $fkColumns[$fk["column"]] = $fk;
    }

    foreach ($columns as $column) {
        $columnName = $column["column_name"];
        $propertyName = columnNameToPropertyName($columnName);
        $dataType = $column["data_type"];
        $isNullable = strtolower($column["is_nullable"]) === "yes";
        $columnDefault = $column["column_default"];

        // Skip primary key and foreign keys (we'll generate relations for those)
        if ($columnName === $primaryKey) {
            // Add ID property with GeneratedValue
            $phpType = "int";
            $properties[] = "    #[ORM\\Id]";
            $properties[] = "    #[ORM\\GeneratedValue]";
            $properties[] = "    #[ORM\\Column]";
            $properties[] = "    private ?$phpType \$$propertyName = null;";
            $properties[] = "";

            $methods[] = generateGetter($propertyName, $phpType);
            continue;
        }

        if (isset($fkColumns[$columnName])) {
            continue; // Handle in relationship section
        }

        // Map to PHP type
        $phpType = mapPostgreSQLTypeToPhpType(
            $dataType,
            $columnDefault,
            $isNullable,
        );
        $doctrineType = mapPostgreSQLTypeToDoctrineType($dataType);

        // Build column attribute
        $columnAttr = "#[ORM\\Column(type: '$doctrineType'";

        if (in_array($doctrineType, ["string", "text"])) {
            $length = $column["character_maximum_length"];
            if ($length && $doctrineType === "string") {
                $columnAttr .= ", length: $length";
            }
        }

        if (in_array($doctrineType, ["decimal"])) {
            $precision = $column["numeric_precision"] ?? 10;
            $scale = $column["numeric_scale"] ?? 0;
            $columnAttr .= ", precision: $precision, scale: $scale";
        }

        if ($isNullable) {
            $columnAttr .= ", nullable: true";
        }

        $columnAttr .= ")]";

        $properties[] = "    $columnAttr";
        $properties[] =
            "    private $phpType \$$propertyName" .
            (!$isNullable ? "" : " = null") .
            ";";
        $properties[] = "";

        // Generate getter and setter
        $methods[] = generateGetter($propertyName, $phpType);
        $methods[] = generateSetter($propertyName, $phpType);
    }

    // Process relationships
    $relationshipMethods = [];

    // ManyToOne relationships (from foreign keys)
    foreach ($foreignKeys as $fk) {
        $refTable = $fk["refTable"];
        $refClassName = tableNameToClassName($refTable);
        $columnName = $fk["column"];
        $propertyName = columnNameToPropertyName($columnName);

        // Remove '_id' suffix if present
        if (substr($propertyName, -2) === "Id") {
            $propertyName = substr($propertyName, 0, -2);
        }

        $relationshipPropertyName = lcfirst($refClassName);

        $properties[] = "    #[ORM\\ManyToOne(targetEntity: $refClassName::class)]";
        $properties[] = "    #[ORM\\JoinColumn(name: '$columnName', referencedColumnName: '{$fk["refColumn"]}')]";
        $properties[] = "    private ?$refClassName \$$relationshipPropertyName = null;";
        $properties[] = "";

        $relationshipMethods[] = generateGetter(
            $relationshipPropertyName,
            "?$refClassName",
        );
        $relationshipMethods[] = generateSetter(
            $relationshipPropertyName,
            "?$refClassName",
        );
    }

    // OneToMany relationships (inverse side of ManyToOne)
    foreach ($tableInfo as $otherTable => $info) {
        if (isset($foreignKeys[$otherTable])) {
            foreach ($foreignKeys[$otherTable] as $fk) {
                if ($fk["refTable"] === $table) {
                    // This table is referenced
                    $otherClassName = $info["className"];
                    $collectionPropertyName = lcfirst($otherClassName) . "s";

                    $properties[] =
                        "    #[ORM\\OneToMany(targetEntity: $otherClassName::class, mappedBy: '" .
                        lcfirst(
                            str_replace(
                                "_id",
                                "",
                                columnNameToPropertyName($fk["column"]),
                            ),
                        ) .
                        "')]";
                    $properties[] = "    private Collection \$$collectionPropertyName;";
                    $properties[] = "";

                    $constructorParams[] = "$collectionPropertyName";
                    $constructorCode .= "        \$this->$collectionPropertyName = new ArrayCollection();\n";

                    $relationshipMethods[] = generateCollectionGetter(
                        $collectionPropertyName,
                        "Collection",
                    );
                    $relationshipMethods[] = generateCollectionAdder(
                        $collectionPropertyName,
                        $otherClassName,
                    );
                    $relationshipMethods[] = generateCollectionRemover(
                        $collectionPropertyName,
                        $otherClassName,
                    );
                }
            }
        }
    }

    // Build constructor if we have collections
    if (!empty($constructorParams)) {
        $constructor = "    public function __construct()\n";
        $constructor .= "    {\n";
        $constructor .= $constructorCode;
        $constructor .= "    }\n\n";
        array_unshift($methods, $constructor);
    }

    // Combine all methods
    $methods = array_merge($methods, $relationshipMethods);

    // Build the class
    $classCode = "<?php\n\n";
    $classCode .= "namespace " . $namespace . ";\n\n";
    $classCode .= implode("\n", $uses) . "\n\n";
    $classCode .= implode("\n", $classAttributes) . "\n";
    $classCode .= "class $className\n";
    $classCode .= "{\n";
    $classCode .= implode("\n", $properties);
    $classCode .= "\n";
    $classCode .= implode("\n", $methods);
    $classCode .= "}\n";

    return $classCode;
}

/**
 * Generate getter method
 */
function generateGetter($propertyName, $returnType)
{
    $methodName = "get" . ucfirst($propertyName);
    return "    public function $methodName(): $returnType\n    {\n        return \$this->$propertyName;\n    }\n\n";
}

/**
 * Generate setter method
 */
function generateSetter($propertyName, $type)
{
    $methodName = "set" . ucfirst($propertyName);
    // Remove |null for the parameter type
    $paramType = str_replace("|null", "", $type);
    $isNullable = strpos($type, "null") !== false ? "?" : "";

    return "    public function $methodName($isNullable$paramType \$$propertyName): static\n    {\n        \$this->$propertyName = \$$propertyName;\n\n        return \$this;\n    }\n\n";
}

/**
 * Generate getter for collections
 */
function generateCollectionGetter($propertyName, $returnType)
{
    $methodName = "get" . ucfirst($propertyName);
    return "    public function $methodName(): $returnType\n    {\n        return \$this->$propertyName;\n    }\n\n";
}

/**
 * Generate adder for collections
 */
function generateCollectionAdder($collectionPropertyName, $itemClassName)
{
    $singularName = lcfirst($itemClassName);
    $methodName = "add" . ucfirst($singularName);

    return "    public function $methodName($itemClassName \$$singularName): static\n" .
        "    {\n" .
        "        if (!\$this->{$collectionPropertyName}->contains(\$$singularName)) {\n" .
        "            \$this->{$collectionPropertyName}->add(\$$singularName);\n" .
        "        }\n\n" .
        "        return \$this;\n" .
        "    }\n\n";
}

/**
 * Generate remover for collections
 */
function generateCollectionRemover($collectionPropertyName, $itemClassName)
{
    $singularName = lcfirst($itemClassName);
    $methodName = "remove" . ucfirst($singularName);

    return "    public function $methodName($itemClassName \$$singularName): static\n" .
        "    {\n" .
        "        \$this->{$collectionPropertyName}->removeElement(\$$singularName);\n\n" .
        "        return \$this;\n" .
        "    }\n\n";
}

echo "\n✅ Entity generation complete!\n";
echo "📁 Generated entities in: $outputDir\n";
?>
