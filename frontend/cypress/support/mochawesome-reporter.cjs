const fs = require('fs');
const path = require('path');

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderMochawesomeHtml(reportData) {
  const { stats, results } = reportData;
  const suitesHtml = (results || [])
    .map((fileResult) => {
      const innerSuites = (fileResult.suites || [])
        .map((suite) => {
          const testsHtml = (suite.tests || [])
            .map((test) => {
              const statusClass =
                test.state === 'passed'
                  ? 'passed'
                  : test.state === 'failed'
                    ? 'failed'
                    : 'pending';
              const statusIcon =
                test.state === 'passed' ? '✓' : test.state === 'failed' ? '✗' : '○';
              const errHtml = test.err?.message
                ? `<pre class="error-box">${escapeHtml(test.err.estack || test.err.message)}</pre>`
                : '';
              return `
                <li class="test-item ${statusClass}">
                  <div class="test-row">
                    <span class="status-badge ${statusClass}">${statusIcon}</span>
                    <span class="test-title">${escapeHtml(test.title)}</span>
                    <span class="test-duration">${test.duration || 0} ms</span>
                  </div>
                  ${errHtml}
                </li>`;
            })
            .join('\n');

          return `
            <div class="suite-card">
              <div class="suite-header">
                <h3>${escapeHtml(suite.title)}</h3>
                <span class="suite-file">${escapeHtml(fileResult.file || '')}</span>
              </div>
              <ul class="test-list">
                ${testsHtml}
              </ul>
            </div>`;
        })
        .join('\n');

      return innerSuites;
    })
    .join('\n');

  return `<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Mochawesome Report - LocaVite E2E Cypress</title>
  <style>
    :root {
      --bg: #f8fafc;
      --card: #ffffff;
      --text: #0f172a;
      --muted: #64748b;
      --pass: #16a34a;
      --fail: #dc2626;
      --pending: #d97706;
      --border: #e2e8f0;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
    }
    header {
      background: #0f172a;
      color: #fff;
      padding: 1.5rem 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }
    header h1 {
      margin: 0;
      font-size: 1.35rem;
    }
    .stats-bar {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .stat-pill {
      background: rgba(255, 255, 255, 0.1);
      padding: 0.45rem 0.9rem;
      border-radius: 999px;
      font-size: 0.875rem;
      font-weight: 600;
    }
    .stat-pill.pass { color: #4ade80; }
    .stat-pill.fail { color: #f87171; }
    main {
      max-width: 1100px;
      margin: 2rem auto;
      padding: 0 1.25rem;
    }
    .suite-card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 0.75rem;
      margin-bottom: 1.25rem;
      overflow: hidden;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    }
    .suite-header {
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #f1f5f9;
    }
    .suite-header h3 {
      margin: 0;
      font-size: 1rem;
    }
    .suite-file {
      font-size: 0.8rem;
      color: var(--muted);
      font-family: monospace;
    }
    .test-list {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .test-item {
      padding: 0.75rem 1.25rem;
      border-bottom: 1px solid var(--border);
    }
    .test-item:last-child {
      border-bottom: none;
    }
    .test-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.4rem;
      height: 1.4rem;
      border-radius: 50%;
      font-size: 0.8rem;
      font-weight: 700;
      color: #fff;
    }
    .status-badge.passed { background: var(--pass); }
    .status-badge.failed { background: var(--fail); }
    .status-badge.pending { background: var(--pending); }
    .test-title {
      flex: 1;
      font-size: 0.925rem;
    }
    .test-duration {
      font-size: 0.8rem;
      color: var(--muted);
      font-family: monospace;
    }
    .error-box {
      margin: 0.6rem 0 0 2.15rem;
      padding: 0.75rem;
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
      border-radius: 0.5rem;
      font-size: 0.8rem;
      overflow-x: auto;
    }
  </style>
</head>
<body>
  <header>
    <div>
      <h1>Mochawesome Report — LocaVite E2E Cypress</h1>
    </div>
    <div class="stats-bar">
      <span class="stat-pill">Suites: ${stats.suites}</span>
      <span class="stat-pill">Tests: ${stats.tests}</span>
      <span class="stat-pill pass">Passes: ${stats.passes}</span>
      <span class="stat-pill fail">Failures: ${stats.failures}</span>
      <span class="stat-pill">Pass %: ${stats.passPercent}%</span>
      <span class="stat-pill">Duration: ${(stats.duration / 1000).toFixed(2)}s</span>
    </div>
  </header>
  <main>
    ${suitesHtml}
  </main>
</body>
</html>`;
}

class MochawesomeReporter {
  constructor(runner, options = {}) {
    const reporterOptions = options.reporterOptions || {};
    this.reportDir = path.resolve(
      process.cwd(),
      reporterOptions.reportDir || 'cypress/reports/mochawesome',
    );
    this.suitesMap = new Map();
    this.tests = [];
    this.startTime = new Date();
    this.specFile = '';

    runner.on('suite', (suite) => {
      if (suite.file && !this.specFile) {
        this.specFile = suite.file;
      }
    });

    runner.on('pass', (test) => {
      this.recordTest(test, 'passed');
    });

    runner.on('fail', (test, err) => {
      this.recordTest(test, 'failed', err);
    });

    runner.on('pending', (test) => {
      this.recordTest(test, 'pending');
    });

    runner.on('end', () => {
      this.writeSpecReport();
      this.mergeAllReports();
    });
  }

  recordTest(test, state, err) {
    if (test.file && !this.specFile) {
      this.specFile = test.file;
    }
    const suiteTitle = test.parent?.title || 'Root Suite';
    if (!this.suitesMap.has(suiteTitle)) {
      this.suitesMap.set(suiteTitle, []);
    }
    const entry = {
      title: test.title,
      fullTitle: typeof test.fullTitle === 'function' ? test.fullTitle() : test.title,
      duration: test.duration || 0,
      state,
      pass: state === 'passed',
      fail: state === 'failed',
      pending: state === 'pending',
      err: err
        ? {
            message: err.message,
            estack: err.stack,
          }
        : {},
    };
    this.suitesMap.get(suiteTitle).push(entry);
    this.tests.push(entry);
  }

  writeSpecReport() {
    try {
      fs.mkdirSync(this.reportDir, { recursive: true });
      const endTime = new Date();
      const passes = this.tests.filter((t) => t.pass).length;
      const failures = this.tests.filter((t) => t.fail).length;
      const pending = this.tests.filter((t) => t.pending).length;
      const total = this.tests.length;
      const duration = endTime.getTime() - this.startTime.getTime();
      const passPercent = total > 0 ? Math.round((passes / total) * 100) : 100;

      const suites = Array.from(this.suitesMap.entries()).map(([title, tests]) => ({
        title,
        tests,
        passes: tests.filter((t) => t.pass).length,
        failures: tests.filter((t) => t.fail).length,
        pending: tests.filter((t) => t.pending).length,
      }));

      const report = {
        stats: {
          suites: suites.length,
          tests: total,
          passes,
          pending,
          failures,
          start: this.startTime.toISOString(),
          end: endTime.toISOString(),
          duration,
          passPercent,
        },
        results: [
          {
            file: this.specFile,
            suites,
          },
        ],
      };

      const safeName = (path.basename(this.specFile || `spec-${Date.now()}`) || 'spec').replace(
        /[^a-zA-Z0-9._-]/g,
        '_',
      );
      fs.writeFileSync(
        path.join(this.reportDir, `mochawesome-${safeName}.json`),
        JSON.stringify(report, null, 2),
        'utf8',
      );
    } catch {
      // Ignore reporter write errors
    }
  }

  mergeAllReports() {
    try {
      const files = fs
        .readdirSync(this.reportDir)
        .filter((f) => f.startsWith('mochawesome-') && f.endsWith('.json'))
        .sort();

      let totalSuites = 0;
      let totalTests = 0;
      let totalPasses = 0;
      let totalFailures = 0;
      let totalPending = 0;
      let totalDuration = 0;
      const allResults = [];

      for (const file of files) {
        const raw = JSON.parse(fs.readFileSync(path.join(this.reportDir, file), 'utf8'));
        totalSuites += raw.stats?.suites || 0;
        totalTests += raw.stats?.tests || 0;
        totalPasses += raw.stats?.passes || 0;
        totalFailures += raw.stats?.failures || 0;
        totalPending += raw.stats?.pending || 0;
        totalDuration += raw.stats?.duration || 0;
        if (Array.isArray(raw.results)) {
          allResults.push(...raw.results);
        }
      }

      const passPercent = totalTests > 0 ? Math.round((totalPasses / totalTests) * 100) : 100;
      const mergedReport = {
        stats: {
          suites: totalSuites,
          tests: totalTests,
          passes: totalPasses,
          pending: totalPending,
          failures: totalFailures,
          start: this.startTime.toISOString(),
          end: new Date().toISOString(),
          duration: totalDuration,
          passPercent,
        },
        results: allResults,
      };

      const htmlContent = renderMochawesomeHtml(mergedReport);
      fs.writeFileSync(
        path.join(this.reportDir, 'mochawesome.json'),
        JSON.stringify(mergedReport, null, 2),
        'utf8',
      );
      fs.writeFileSync(path.join(this.reportDir, 'mochawesome.html'), htmlContent, 'utf8');
      fs.writeFileSync(path.join(this.reportDir, 'index.html'), htmlContent, 'utf8');
    } catch {
      // Ignore merge errors
    }
  }
}

module.exports = MochawesomeReporter;
