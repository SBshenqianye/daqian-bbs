#!/usr/bin/env node
/**
 * doc-check.mjs —— 实现文档的「事实层」生成与校验
 *
 *   原理：实现文档的右端（是什么/怎么做）必须能从代码里读出来。
 *         所以不手写，而是从代码抽取 → 生成 docs/agent/实现事实.md → 提交前比对。
 *
 * 用法：
 *   node scripts/dev/doc-check.mjs            # 校验（不一致 exit 1，打印差异）
 *   node scripts/dev/doc-check.mjs --write    # 重新生成
 *
 * 判定：只比对"事实"部分；文件头的生成时间/commit 不参与比对。
 * 扩展位：设计文档与实现事实的「间距」= 完成度，见 docs/README.md（后续 --delta）。
 */
import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';

const ROOT = process.cwd();
const OUT = path.join(ROOT, 'docs/agent/实现事实.md');
const WRITE = process.argv.includes('--write');

const read = (p) => {
  try { return fs.readFileSync(path.join(ROOT, p), 'utf8'); } catch { return ''; }
};
const uniq = (a) => [...new Set(a)].filter(Boolean).sort();

function git(cmd, fallback = 'unknown') {
  try { return execSync(cmd, { cwd: ROOT, stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim(); }
  catch { return fallback; }
}

/* ---------------- 1. 配置事实：.env.example ---------------- */
const env = {};
for (const line of read('scripts/.env.example').split(/\r?\n/)) {
  const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
  if (m) env[m[1]] = m[2].replace(/\s+#.*$/, '').trim();
}

/* ---------------- 2. Spring 配置 ---------------- */
const appYml = read('bbs-server/src/main/resources/application.yml');
const podmanYml = read('bbs-server/src/main/resources/application-podman.yml');
const grab = (text, re, def = '—') => (text.match(re)?.[1] ?? def).trim();

const springPort = grab(appYml, /port:\s*\$\{BBS_SERVER_PORT:(\d+)\}/);
const contextPath = grab(appYml, /context-path:\s*(\S+)/);
const profiles = grab(appYml, /active:\s*([^\n]+)/);
const dbUrl = grab(podmanYml, /url:\s*(jdbc:postgresql:\/\/\S+)/);
const storagePath = grab(podmanYml, /storage:\s*\n\s*path:\s*(\S+)/);

/* ---------------- 3. 部署入口 deploy.sh ---------------- */
const deploy = read('deploy.sh');
const deployVer = grab(deploy, /DEPLOY_VERSION="([\d.]+)"/);
const dispatch = grab(deploy, /case "\$CMD" in\n([\s\S]*?)\n\s*esac/, '');
const subcommands = uniq(
  [...dispatch.matchAll(/^\s{4}([a-z][a-z|-]*)\)/gm)]
    .flatMap((m) => m[1].split('|'))
);
const onceAliases = (deploy.match(/^\s*(once\|[a-z|]+)\)\s*ONCE=1/m)?.[1] ?? 'once').split('|');
const homeEntries = uniq(
  [...deploy.matchAll(/\$BBS_HOME\/([A-Za-z0-9._-]+)/g)]
    .map((m) => m[1])
    .filter((d) => !d.startsWith('..'))
);
// deploy.sh 里既有目录也有文件/软链；分类表放在这里，避免文档自己猜
const FILE_ENTRIES = new Set(['deploy.sh', 'deploy-offline.sh', 'deploy.env', 'current', 'current.json', 'src', '.deploy.lock', '.env']);
const homeDirs = homeEntries.filter((d) => !FILE_ENTRIES.has(d));
const homeFiles = homeEntries.filter((d) => FILE_ENTRIES.has(d));
const endpoints = uniq([...deploy.matchAll(/"(\/bbs-[a-z]+\/)"/g)].map((m) => m[1]));

/* ---------------- 4. 运行时与镜像 ---------------- */
const dockerfile = read('bbs-server/Dockerfile.base');
const baseImage = grab(dockerfile, /^FROM\s+(\S+)/m);
const javaPkg = grab(dockerfile, /yum install -y ([a-z0-9.\-]+)/);
const pom = read('bbs-server/pom.xml');
const javaVer = grab(pom, /<java\.version>([^<]+)/);
const springBoot = grab(pom, /<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>([^<]+)/);
const nginxTpl = read('nginx/nginx.conf.template');
const upstream = grab(nginxTpl, /server\s+127\.0\.0\.1:\$\{BBS_SERVER_PORT\}/);

/* ---------------- 5. 脚本清单 ---------------- */
function scriptTree() {
  const base = path.join(ROOT, 'scripts');
  if (!fs.existsSync(base)) return [];
  const rows = [];
  for (const d of fs.readdirSync(base, { withFileTypes: true })) {
    if (!d.isDirectory()) continue;
    if (d.name === 'test' ) continue;             // 验收工具不参与发布物
    const full = path.join(base, d.name);
    const files = [];
    (function walk(p, depth) {
      for (const e of fs.readdirSync(p, { withFileTypes: true })) {
        if (e.name === 'node_modules' || e.name === 'target') continue;
        const fp = path.join(p, e.name);
        if (e.isDirectory()) { if (depth < 2) walk(fp, depth + 1); }
        else if (/\.(sh|ps1|js|mjs)$/.test(e.name)) files.push(path.relative(base, fp).replace(/\\/g, '/'));
      }
    })(full, 0);
    rows.push({ dir: d.name, files: files.sort() });
  }
  return rows.sort((a, b) => a.dir.localeCompare(b.dir));
}

/* ---------------- 6. 数据库迁移 ---------------- */
const upgradePg = read('bbs-server/src/main/resources/db/init/upgrade-pg.sql');
const migrations = [...upgradePg.matchAll(/^-- @migration:\s+(\S+)/gm)].map((m) => m[1]);
const lastMigration = migrations.at(-1) ?? '—';

/* ---------------- 7. Git ---------------- */
const commit = git('git rev-parse --short HEAD');
const branch = git('git rev-parse --abbrev-ref HEAD');
const dirty = git('git status --porcelain').split(/\r?\n/).filter(Boolean).length;

/* ---------------- 8. 设计承诺（--delta） ---------------- */
const DESIGN_DIR = 'docs/human/设计';
function commitments() {
  const out = [];
  if (!fs.existsSync(path.join(ROOT, DESIGN_DIR))) return out;
  for (const f of fs.readdirSync(path.join(ROOT, DESIGN_DIR)).filter((n) => n.endsWith('.md'))) {
    const t = read(path.join(DESIGN_DIR, f));
    t.split(/\r?\n/).forEach((line, i) => {
      const m = line.match(/^\s*-\s*\[([ xX])\]\s+(.*)$/);
      if (m) out.push({ file: f, line: i + 1, done: m[1].toLowerCase() === 'x', text: m[2].trim() });
    });
  }
  return out;
}
function printDelta(detail) {
  const all = commitments();
  if (!all.length) {
    console.log('[DELTA] 设计文档里没有 `- [ ]` 承诺清单（约定见 docs/README.md）');
    return;
  }
  const done = all.filter((x) => x.done).length;
  const pct = Math.round((done / all.length) * 100);
  console.log(`[DELTA] 设计承诺 ${all.length} / 已实现 ${done} / 完成度 ${pct}%（间距 = 未实现 ${all.length - done} 项）`);
  if (detail) {
    for (const x of all.filter((v) => !v.done)) {
      console.log(`  [ ] ${x.file}:${x.line}  ${x.text}`);
    }
  } else if (all.length - done > 0) {
    console.log('  （加 --delta 查看未实现清单）');
  }
}

/* ---------------- 9. 证据核验（[x] 里反引号指向的代码必须存在） ---------------- */
const NAME_EXT = /\.(md|sql|js|mjs|ts|java|yml|yaml|template|json)$/i;
let fileIndexCache = null;
function fileIndex() {
  if (fileIndexCache) return fileIndexCache;
  const idx = new Map();
  for (const p of git('git ls-files').split(/\r?\n/).filter(Boolean)) {
    const base = p.split('/').pop();
    if (!idx.has(base)) idx.set(base, []);
    idx.get(base).push(p);
  }
  fileIndexCache = idx;
  return idx;
}
const contentCache = new Map();
function fileHas(file, needle) {
  if (!contentCache.has(file)) {
    contentCache.set(file, read(file));
  }
  return contentCache.get(file).includes(needle);
}
function hasBasename(base) {
  return fileIndex().has(base);
}
function hasPermissionLiteral(tok) {
  try {
    execSync(`git grep -q -F -- "${tok}" -- bbs-server/src bbs-admin-ui/src`, { cwd: ROOT, stdio: 'ignore' });
    return true;
  } catch { return false; }
}

/** 判断一个证据 token 是否能在代码里找到；返回 null 表示"描述性，不做校验" */
function checkToken(tok) {
  const t = tok.trim();
  if (!t || /\s/.test(t)) return null;                       // 命令/短语
  if (t.startsWith('/')) return null;                        // URL / 路由模式
  if (/^\d/.test(t)) return null;
  if (t.includes('{') && t.includes('}')) {                  // a-{x,y}.sql
    const names = [];
    t.replace(/\{([^}]+)\}/g, (_, list) => { names.push(...list.split(',')); return ''; });
    const head = t.slice(0, t.indexOf('{'));
    const tail = t.slice(t.indexOf('}') + 1);
    const missing = names.map((n) => `${head}${n}${tail}`).filter((n) => !hasBasename(n.split('/').pop()));
    return { kind: 'brace', ok: missing.length === 0, detail: missing.join(', ') };
  }
  if (t.includes('/') || NAME_EXT.test(t)) {                 // 路径
    if (fs.existsSync(path.join(ROOT, t))) return { kind: 'path', ok: true, detail: t };
    const base = t.split('/').pop();
    return { kind: 'path', ok: hasBasename(base), detail: t };
  }
  let m = t.match(/^([A-Z][A-Za-z0-9_]*)\.([a-zA-Z_]\w*)$/);  // Class.member
  if (m) {
    const [, cls, member] = m;
    const idx = fileIndex();
    const files = idx.get(`${cls}.java`) || [];
    if (!files.length) return { kind: 'class.member', ok: false, detail: `${cls}.java 不存在` };
    const ok = files.some((f) => fileHas(f, member));
    return { kind: 'class.member', ok, detail: ok ? '' : `${cls} 中没有 ${member}` };
  }
  if (/^[A-Z][A-Za-z0-9_]*$/.test(t)) {                       // ClassName
    return { kind: 'class', ok: hasBasename(`${t}.java`), detail: `${t}.java` };
  }
  m = t.match(/^([a-z][A-Za-z0-9_]*)\.([a-zA-Z_]\w*)$/);      // module.member（前端）
  if (m) {
    const [, mod, member] = m;
    const idx = fileIndex();
    const files = ['js', 'mjs', 'ts'].flatMap((ext) => idx.get(`${mod}.${ext}`) || []);
    if (!files.length) return { kind: 'module.member', ok: false, detail: `${mod}.js|mjs|ts 不存在` };
    const ok = files.some((f) => fileHas(f, member));
    return { kind: 'module.member', ok, detail: ok ? '' : `${mod} 中没有 ${member}` };
  }
  if (/^[a-z][\w-]*(:[a-zA-Z][\w-]*)+$/.test(t)) {            // 权限码 user:grant:admin
    return { kind: 'literal', ok: hasPermissionLiteral(t), detail: t };
  }
  return null;                                                // 其余视为描述，跳过
}

function verifyEvidence() {
  const all = commitments().filter((x) => x.done);
  const failures = [];
  let checked = 0;
  for (const item of all) {
    for (const m of item.text.matchAll(/`([^`]+)`/g)) {
      const r = checkToken(m[1]);
      if (!r) continue;
      checked++;
      if (!r.ok) failures.push({ file: item.file, line: item.line, token: m[1], detail: r.detail });
    }
  }
  if (!failures.length) {
    console.log(`[EVIDENCE] ${all.length} 条 [x] 的证据标识 ${checked} 个，全部能在代码中找到`);
    return true;
  }
  console.error(`[FAIL] 有 ${failures.length} 个证据标识在代码里找不到（虚勾/改名/路径过期）：`);
  for (const f of failures) {
    console.error(`  - \`${f.token}\`  ${f.file}:${f.line}  ${f.detail ? `（${f.detail}）` : ''}`);
  }
  return false;
}

/* ---------------- 输出 ---------------- */
const lines = [];
const P = (s = '') => lines.push(s);

P('<!-- 由 scripts/dev/doc-check.mjs 生成，请勿手改；改代码后运行 node scripts/dev/doc-check.mjs --write -->');
P('');
P('# 实现事实（从代码抽取）');
P('');
P('> 这一页回答「是什么 / 怎么做」，不回答「为什么」。');
P('> 设计与需求的取舍在 `docs/human/`；两端的差距 = 当前完成度。');
P('');
P(`- 生成时间：${new Date().toISOString().slice(0, 19).replace('T', ' ')}`);
P(`- commit：\`${commit}\`（${branch}，未提交 ${dirty} 个文件）`);
P(`- deploy.sh 版本：${deployVer}`);
P('');
P('## 1. 端口与地址');
P('');
P('| 项 | 值 | 来源 |');
P('| --- | --- | --- |');
P(`| 后端端口（生产） | ${env.BBS_SERVER_PORT ?? '—'} | \`scripts/.env.example\` |`);
P(`| 后端默认端口（开发） | ${springPort} | \`application.yml\` |`);
P(`| Nginx 端口 | ${env.NGINX_PORT ?? '—'} | \`scripts/.env.example\` |`);
P(`| 后端上下文路径 | \`${contextPath}\` | \`application.yml\` |`);
P(`| 数据库 | \`${env.BBS_DB_HOST ?? '—'}:${env.BBS_DB_PORT ?? '—'}/${env.BBS_DB_NAME ?? '—'}\`（用户 \`${env.BBS_DB_USER ?? '—'}\`） | \`scripts/.env.example\` |`);
P(`| JDBC 模板 | \`${dbUrl}\` | \`application-podman.yml\` |`);
P(`| 上传目录 | \`${env.BBS_UPLOAD_DIR ?? storagePath}\` | \`.env.example\` / \`application-podman.yml\` |`);
P('');
P('## 2. 运行时与镜像');
P('');
P('| 项 | 值 | 来源 |');
P('| --- | --- | --- |');
P(`| 基础镜像 | \`${baseImage}\` | \`bbs-server/Dockerfile.base\` |`);
P(`| JDK 包 | \`${javaPkg}\` | 同上 |`);
P(`| Java / Spring Boot | ${javaVer} / ${springBoot} | \`bbs-server/pom.xml\` |`);
P(`| 后端容器名 | \`${env.BBS_SERVER_CONTAINER ?? 'bbs-server'}\` | \`scripts/.env.example\` |`);
P(`| Nginx 容器名 | \`${env.BBS_NGINX_CONTAINER ?? 'bbs-nginx'}\` | \`scripts/.env.example\` |`);
P(`| Nginx 上游 | ${upstream ? '`127.0.0.1:${BBS_SERVER_PORT}`' : '—'} | \`nginx/nginx.conf.template\` |`);
P(`| Spring profiles | \`${profiles}\` | \`application.yml\` |`);
P('');
P('## 3. 部署入口命令');
P('');
P('| 项 | 值 |');
P('| --- | --- |');
P(`| 日常升级 | \`bash deploy.sh\` |`);
P(`| 一次性动作 | \`bash deploy.sh ${onceAliases[0] ?? 'once'} <动作>\`（别名：${onceAliases.map((a) => `\`${a}\``).join(' / ')}） |`);
P(`| 子命令 | ${subcommands.map((c) => `\`${c}\``).join(' · ')} |`);
P(`| 验证端点 | ${endpoints.map((e) => `\`${e}\``).join(' · ')} |`);
P('');
P('## 4. 部署根目录契约（deploy.sh 实际引用）');
P('');
P('目录：');
P('');
P(homeDirs.map((d) => `- \`${d}/\``).join('\n'));
P('');
P('文件 / 软链：');
P('');
P(homeFiles.map((d) => `- \`${d}\``).join('\n') || '- （无）');
P('');
P('## 5. 脚本清单（scripts/，不含 test/）');
P('');
for (const g of scriptTree()) {
  P(`### ${g.dir}/（${g.files.length}）`);
  P('');
  for (const f of g.files) P(`- \`scripts/${f}\``);
  P('');
}
P('## 6. 数据库');
P('');
P('| 项 | 值 |');
P('| --- | --- |');
P(`| 迁移记录表 | \`bbs_schema_version\` |`);
P(`| 最新迁移块 | \`${lastMigration}\`（共 ${migrations.length} 段） |`);
P(`| 迁移文件 | \`init-pg.sql\` / \`upgrade-pg.sql\`（MySQL 同名，四份同步） |`);
P('');
P('## 7. 待对齐（设计承诺 → 实现）');
P('');
P('> 由 `--delta` 生成：扫描 `docs/human/设计/*.md` 里的 `- [ ]` / `- [x]` 条目，');
P('> 与本节的事实比对，输出「已实现 / 未实现」清单与完成度百分比。当前版本先留位。');
P('');

const content = lines.join('\n');
const HEAD_RE = /^- 生成时间：.*$/m;

if (WRITE) {
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, content);
  console.log(`[OK] 已生成 ${path.relative(ROOT, OUT)}（${content.split('\n').length} 行）`);
  const evOk = verifyEvidence();
  printDelta(process.argv.includes('--delta'));
  process.exit(evOk ? 0 : 1);
}

// 只比对正文（从 "## 1." 起）：文件头的生成时间/commit/未提交数天然会变，不参与判定
const body = (s) => {
  const i = s.indexOf('## 1.');
  return (i >= 0 ? s.slice(i) : s).replace(/\r\n/g, '\n').trimEnd();
};

const old = read(path.relative(ROOT, OUT));
if (!old) {
  console.error(`[FAIL] 缺少 ${path.relative(ROOT, OUT)}；先运行 node scripts/dev/doc-check.mjs --write`);
  process.exit(1);
}

if (body(old) === body(content)) {
  console.log('[OK] 实现事实与代码一致');
  const evOk = verifyEvidence();
  printDelta(process.argv.includes('--delta'));
  process.exit(evOk ? 0 : 1);
}

const a = body(old).split('\n');
const b = body(content).split('\n');
const max = Math.max(a.length, b.length);
let shown = 0;
console.error('[FAIL] 实现事实与代码不一致（代码改了但文档没更新，或反之）：');
for (let i = 0; i < max && shown < 40; i++) {
  if ((a[i] ?? '') !== (b[i] ?? '')) {
    console.error(`  第 ${i + 1} 行`);
    console.error(`    - 文档: ${a[i] ?? '(缺失)'}`);
    console.error(`    + 代码: ${b[i] ?? '(缺失)'}`);
    shown++;
  }
}
if (shown === 0) console.error('  （仅空白差异）');
console.error('\n修复：node scripts/dev/doc-check.mjs --write');
printDelta(false);
process.exit(1);
