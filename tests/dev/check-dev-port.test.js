const { test } = require('node:test');
const assert = require('node:assert');
const net = require('node:net');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const SCRIPT = path.resolve(__dirname, '../../scripts/check-dev-port.js');

function listen(port = 0) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(port, '0.0.0.0', () => resolve(server));
  });
}

function close(server) {
  return new Promise(resolve => server.close(resolve));
}

function runCheck(port) {
  return spawnSync(process.execPath, [SCRIPT, String(port)], { encoding: 'utf8' });
}

test('端口空闲时预检查通过，允许前端启动', async () => {
  const server = await listen();
  const port = server.address().port;
  await close(server);

  const result = runCheck(port);
  assert.strictEqual(result.status, 0, result.stderr);
});

test('端口被占用时预检查失败，阻止前端抢用后端端口', async () => {
  const server = await listen();
  const port = server.address().port;
  try {
    const result = runCheck(port);
    assert.strictEqual(result.status, 1, '端口占用时必须返回非零状态');
    assert.match(result.stderr, new RegExp(`\\[端口检查\\] ${port} 已被占用`));
  } finally {
    await close(server);
  }
});

test('非法端口参数直接失败并给出用法', () => {
  const result = spawnSync(process.execPath, [SCRIPT], { encoding: 'utf8' });
  assert.strictEqual(result.status, 1);
  assert.match(result.stderr, /用法: node scripts\/check-dev-port\.js <port>/);
});
