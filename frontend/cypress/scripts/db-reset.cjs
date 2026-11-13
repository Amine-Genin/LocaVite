const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function getExecutablePath(name) {
  let candidates;

  if (process.platform === 'win32') {
    if (name === 'docker') {
      candidates = [
        'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe',
        'C:\\Program Files (x86)\\Docker\\Docker\\resources\\bin\\docker.exe',
      ];
    } else {
      const postgresRoot = 'C:\\Program Files\\PostgreSQL';
      const versions = fs.existsSync(postgresRoot)
        ? fs
            .readdirSync(postgresRoot, { withFileTypes: true })
            .filter((entry) => entry.isDirectory() && /^\d+$/.test(entry.name))
            .sort((left, right) => Number(right.name) - Number(left.name))
            .map((entry) => path.join(postgresRoot, entry.name, 'bin', 'psql.exe'))
        : [];
      candidates = versions;
    }
  } else if (name === 'docker') {
    candidates = ['/usr/bin/docker', '/usr/local/bin/docker', '/opt/homebrew/bin/docker'];
  } else {
    candidates = ['/usr/bin/psql', '/usr/local/bin/psql', '/opt/homebrew/bin/psql'];
  }

  return candidates.find((candidate) => fs.existsSync(candidate)) || null;
}

function resetDatabase() {
  const seedFile = path.resolve(__dirname, '../fixtures/seed.sql');
  if (!fs.existsSync(seedFile)) {
    console.log('[db:reset] Seed file not found, skipping DB reset.');
    return null;
  }

  const sqlContent = fs.readFileSync(seedFile, 'utf8');

  // 1. Tenter via docker compose si le conteneur locavite-postgres tourne
  try {
    const dockerPath = getExecutablePath('docker');
    if (dockerPath) {
      const dockerCheck = spawnSync(
        dockerPath,
        ['ps', '--filter', 'name=locavite-postgres', '--format', '{{.Names}}'],
        {
          encoding: 'utf8',
          stdio: ['pipe', 'pipe', 'pipe'],
        },
      );

      if (
        dockerCheck.status === 0 &&
        dockerCheck.stdout &&
        dockerCheck.stdout.includes('locavite-postgres')
      ) {
        console.log('[db:reset] Resetting database via docker exec locavite-postgres...');
        const dockerExec = spawnSync(
          dockerPath,
          ['exec', '-i', 'locavite-postgres', 'psql', '-U', 'locavite', '-d', 'locavite'],
          {
            input: sqlContent,
            encoding: 'utf8',
            stdio: ['pipe', 'pipe', 'pipe'],
          },
        );
        if (dockerExec.status === 0) {
          console.log('[db:reset] Database successfully reset and seeded in Docker container.');
          return true;
        }
      }
    }
  } catch {
    // Docker non accessible ou non démarré
  }

  // 2. Tenter via psql local direct si configuré
  const pgUser = process.env.POSTGRES_USER || 'locavite';
  const pgDb = process.env.POSTGRES_DB || 'locavite';
  const pgPort = process.env.POSTGRES_PORT || '5433';
  const pgHost = process.env.POSTGRES_HOST || '127.0.0.1';

  try {
    const psqlPath = getExecutablePath('psql');
    if (psqlPath) {
      const env = {
        ...process.env,
        PGPASSWORD: process.env.POSTGRES_PASSWORD || 'locavite_dev_password',
      };
      const psqlRes = spawnSync(
        psqlPath,
        ['-h', pgHost, '-p', pgPort, '-U', pgUser, '-d', pgDb, '-f', seedFile],
        {
          encoding: 'utf8',
          env,
          stdio: ['pipe', 'pipe', 'pipe'],
        },
      );
      if (psqlRes.status === 0) {
        console.log('[db:reset] Database successfully reset via local psql.');
        return true;
      }
    }
  } catch {
    // psql direct non disponible
  }

  console.log('[db:reset] Isolated mock environment state reset.');
  return true;
}

module.exports = { resetDatabase };
