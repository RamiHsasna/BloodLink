<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260422171000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Drop unique constraints/indexes on users.email to allow duplicate email values';
    }

    public function up(Schema $schema): void
    {
        $this->addSql(<<<'SQL'
DO $$
DECLARE
    v_constraint_name text;
    v_index_name text;
BEGIN
    FOR v_constraint_name IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
          ON tc.constraint_name = ccu.constraint_name
         AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'users'
          AND tc.constraint_type = 'UNIQUE'
          AND ccu.column_name = 'email'
    LOOP
        EXECUTE format('ALTER TABLE public.users DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END LOOP;

    FOR v_index_name IN
        SELECT i.indexname
        FROM pg_indexes i
        JOIN pg_class t ON t.relname = i.tablename
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND i.tablename = 'users'
          AND i.indexdef ILIKE 'CREATE UNIQUE INDEX % (email%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS public.%I', v_index_name);
    END LOOP;
END $$;
SQL);
    }

    public function down(Schema $schema): void
    {
        $this->addSql('CREATE UNIQUE INDEX IF NOT EXISTS uniq_users_email ON public.users (email)');
    }
}
