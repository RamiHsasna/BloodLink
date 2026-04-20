<?php

namespace App\Command;

use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\Process\Process;

#[AsCommand(
    name: 'app:reverse-engineer:entities',
    description: 'Run the database-first entity generator used for the BloodLink reverse-engineering workshop flow.',
)]
class ReverseEngineerEntitiesCommand extends Command
{
    public function __construct(
        #[Autowire('%kernel.project_dir%')]
        private readonly string $projectDir,
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this->setHelp(
            <<<'HELP'
This command is the Symfony-facing entrypoint for BloodLink's reverse-engineering workflow.

It runs the existing reverse-engineering.php script at the project root, which scans the
PostgreSQL schema and regenerates Doctrine entities under src/Entity.

Typical validation commands for the first checkpoint:
  php bin/console app:reverse-engineer:entities --help
  php bin/console doctrine:mapping:info
HELP,
        );
    }

    protected function execute(
        InputInterface $input,
        OutputInterface $output,
    ): int {
        $scriptPath = $this->projectDir . DIRECTORY_SEPARATOR . 'reverse-engineering.php';

        if (!is_file($scriptPath)) {
            $output->writeln(
                sprintf('<error>Reverse-engineering script not found: %s</error>', $scriptPath),
            );

            return Command::FAILURE;
        }

        $process = new Process([PHP_BINARY, $scriptPath], $this->projectDir);
        $process->setTimeout(null);
        $process->run(static function (string $type, string $buffer) use ($output): void {
            $output->write($buffer);
        });

        return $process->isSuccessful() ? Command::SUCCESS : Command::FAILURE;
    }
}
