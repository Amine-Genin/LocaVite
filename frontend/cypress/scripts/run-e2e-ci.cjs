const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const http = require('http');
const net = require('net');
const path = require('path');

delete process.env.ELECTRON_RUN_AS_NODE;

const baseUrl = process.env.CYPRESS_BASE_URL || 'http://localhost:4200';
const reportDir = path.resolve(__dirname, '../reports/mochawesome');
const cypressBin = path.resolve(__dirname, '../../node_modules/cypress/bin/cypress');

function ensureCypressInstalled() {
  const verifyRes = spawnSync(process.execPath, [cypressBin, 'verify'], {
    stdio: 'pipe',
    env: process.env,
  });
  if (verifyRes.status !== 0) {
    console.log('[e2e:ci] Cypress binary not found or unverified. Running cypress install...');
    const installRes = spawnSync(process.execPath, [cypressBin, 'install'], {
      stdio: 'inherit',
      env: process.env,
    });
    if (installRes.status !== 0) {
      process.exit(installRes.status ?? 1);
    }
  }
}

function checkPortOpen(port = 4200, host = '127.0.0.1') {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    let settled = false;
    const done = (result) => {
      if (!settled) {
        settled = true;
        socket.destroy();
        resolve(result);
      }
    };
    socket.setTimeout(1500);
    socket.once('connect', () => done(true));
    socket.once('timeout', () => done(false));
    socket.once('error', () => done(false));
    socket.connect(port, host);
  });
}

function checkServerReady(url) {
  return new Promise((resolve) => {
    const req = http.get(
      url,
      { headers: { Accept: 'text/html,application/xhtml+xml,*/*' } },
      (res) => {
        res.resume();
        resolve(res.statusCode >= 200 && res.statusCode < 500);
      },
    );
    req.on('error', () => resolve(false));
    req.setTimeout(2000, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function isServerUp(url) {
  if (await checkServerReady(url)) return true;
  if (await checkPortOpen(4200, '127.0.0.1')) return true;
  if (await checkPortOpen(4200, '::1')) return true;
  return false;
}

function stopServerProcess(child) {
  if (!child || !child.pid) return;
  try {
    child.kill('SIGTERM');
  } catch {
    try {
      process.kill(child.pid);
    } catch {
      // Ignore cleanup errors
    }
  }
}

async function main() {
  ensureCypressInstalled();

  try {
    fs.rmSync(reportDir, { recursive: true, force: true });
    fs.mkdirSync(reportDir, { recursive: true });
  } catch {
    // Ignore directory reset errors
  }

  let serverProcess = null;
  const alreadyUp = await isServerUp(baseUrl);

  if (!alreadyUp) {
    console.log(`[e2e:ci] Starting Angular dev server on ${baseUrl}...`);
    const ngCli = path.resolve(__dirname, '../../node_modules/@angular/cli/bin/ng.js');

    let stdoutReady = false;
    serverProcess = spawn(
      process.execPath,
      [ngCli, 'serve', '--host', '127.0.0.1', '--port', '4200'],
      {
        stdio: ['ignore', 'pipe', 'pipe'],
        env: process.env,
      },
    );

    serverProcess.stdout.on('data', (chunk) => {
      const text = chunk.toString();
      process.stdout.write(text);
      if (text.includes('4200') || text.includes('Watch mode enabled')) {
        stdoutReady = true;
      }
    });

    serverProcess.stderr.on('data', (chunk) => {
      process.stderr.write(chunk.toString());
    });

    const start = Date.now();
    let ready = false;
    while (Date.now() - start < 120000) {
      if (stdoutReady || (await isServerUp(baseUrl))) {
        ready = true;
        break;
      }
      await new Promise((r) => setTimeout(r, 1000));
    }

    if (!ready) {
      console.error(`[e2e:ci] Timed out waiting for ${baseUrl}`);
      stopServerProcess(serverProcess);
      process.exit(1);
    }
    await new Promise((r) => setTimeout(r, 1500));
    console.log(`[e2e:ci] Server is ready at ${baseUrl}`);
  } else {
    console.log(`[e2e:ci] Detected running server at ${baseUrl}`);
  }

  const extraArgs = process.argv.slice(2);
  const result = spawnSync(process.execPath, [cypressBin, 'run', ...extraArgs], {
    stdio: 'inherit',
    env: process.env,
  });

  if (serverProcess) {
    console.log('[e2e:ci] Stopping Angular dev server...');
    stopServerProcess(serverProcess);
  }

  process.exit(result.status ?? 1);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
