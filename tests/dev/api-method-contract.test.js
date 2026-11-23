#!/usr/bin/env node
/**
 * 前后端 HTTP 方法契约检查（零依赖）
 *
 * 背景：2026-11-23 生产反馈「点编辑帖子报错」——后端 `@GetMapping("/article/getForEdit/{id}")`，
 * 前端却用 `postRequest(...)` 调用，登录后必然 405。单测直接调 Service、E2E 没覆盖编辑流程，
 * 两边都测不出来。
 *
 * 做法：扫描后端控制器的映射注解（含类级 @RequestMapping 前缀），再扫描用户端/管理端的
 * getRequest/postRequest/putRequest/deleteRequest 调用，路径归一化后比对方法。
 * 只有「路径能唯一匹配到某个后端映射且方法不同」才判定失败，避免动态拼路径产生噪声。
 *
 * 用法：node tests/dev/api-method-contract.test.js
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '../..')
const CONTROLLER_DIRS = [
  path.join(ROOT, 'bbs-server/src/main/java/com/walker/controller'),
]
const FRONTEND_DIRS = [
  path.join(ROOT, 'bbs-ui/src'),
  path.join(ROOT, 'bbs-admin-ui/src'),
]

const METHOD_NAME = { Get: 'GET', Post: 'POST', Put: 'PUT', Delete: 'DELETE', Patch: 'PATCH' }

function walk(dir, exts, out = []) {
  if (!fs.existsSync(dir)) return out
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    const stat = fs.statSync(full)
    if (stat.isDirectory()) walk(full, exts, out)
    else if (exts.includes(path.extname(full))) out.push(full)
  }
  return out
}

/** /article/getForEdit/{id}、/article/getForEdit/${id}、/x/:id → /article/getForEdit/* */
function normalize(p) {
  return String(p)
    .split('?')[0]
    .replace(/\$\{[^}]*\}/g, '*')
    .replace(/\{[^}]*\}/g, '*')
    .replace(/:[A-Za-z_][A-Za-z0-9_]*/g, '*')
    .replace(/\/{2,}/g, '/')
    .replace(/\/+$/, '') || '/'
}

function scanBackend() {
  const routes = []
  for (const dir of CONTROLLER_DIRS) {
    for (const file of walk(dir, ['.java'])) {
      const src = fs.readFileSync(file, 'utf8')
      const classPrefixMatch = src.match(/^@RequestMapping\(\s*(?:value\s*=\s*)?["']([^"']+)["']/m)
      const prefix = classPrefixMatch ? classPrefixMatch[1] : ''
      const re = /@(Get|Post|Put|Delete|Patch)Mapping\(\s*(?:value\s*=\s*)?["']([^"']+)["']/g
      let m
      while ((m = re.exec(src)) !== null) {
        routes.push({
          method: METHOD_NAME[m[1]],
          path: normalize(prefix + m[2]),
          file: path.relative(ROOT, file),
        })
      }
    }
  }
  return routes
}

function scanFrontend() {
  const calls = []
  const re = /\b(getRequest|postRequest|putRequest|deleteRequest)\(\s*(['"`])([^'"`]+)\2/g
  const methodOf = { getRequest: 'GET', postRequest: 'POST', putRequest: 'PUT', deleteRequest: 'DELETE' }
  for (const dir of FRONTEND_DIRS) {
    for (const file of walk(dir, ['.js', '.vue'])) {
      const src = fs.readFileSync(file, 'utf8')
      let m
      while ((m = re.exec(src)) !== null) {
        const raw = m[3]
        // 跳过明显不是接口路径的调用（如空串、纯变量拼接的开头）
        if (!raw.startsWith('/')) continue
        calls.push({
          method: methodOf[m[1]],
          path: normalize(raw),
          file: path.relative(ROOT, file),
          raw,
        })
      }
    }
  }
  return calls
}

const routes = scanBackend()
const calls = scanFrontend()
const mismatches = []
let checked = 0

for (const call of calls) {
  const candidates = routes.filter(r => r.path === call.path)
  if (candidates.length !== 1) continue // 没匹配到 / 多个映射共用同一路径 → 无法判定，跳过
  checked++
  const route = candidates[0]
  if (route.method !== call.method) {
    mismatches.push({ ...call, expected: route.method, route: route.file })
  }
}

console.log(`扫描到后端映射 ${routes.length} 个，前端调用 ${calls.length} 个，可比对 ${checked} 个`)
if (mismatches.length === 0) {
  console.log('PASS 前后端 HTTP 方法一致')
  process.exit(0)
}
for (const m of mismatches) {
  console.error(`FAIL ${m.file} 用 ${m.method} 调 ${m.raw}`)
  console.error(`     后端是 ${m.expected}（${m.route}）`)
}
process.exit(1)
