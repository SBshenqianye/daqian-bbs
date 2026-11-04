#!/usr/bin/env node
/**
 * 开发端口预检查。
 *
 * Vue CLI 使用 portfinder，端口被占用时会自动递增到下一个端口，
 * 可能出现 admin-ui 抢到后端 9083 的情况。启动前显式检查，占用时直接失败。
 *
 * 用法：node scripts/check-dev-port.js <port>
 */
const net = require('net')

const port = Number(process.argv[2])
if (!Number.isInteger(port) || port <= 0) {
  console.error('用法: node scripts/check-dev-port.js <port>')
  process.exit(1)
}

const server = net.createServer()
server.once('error', err => {
  if (err.code === 'EADDRINUSE') {
    console.error(`\n[端口检查] ${port} 已被占用，已停止启动。`)
    console.error('请先结束占用该端口的进程：')
    console.error(`  Get-NetTCPConnection -LocalPort ${port} -State Listen | Select-Object OwningProcess`)
    console.error('  Stop-Process -Id <上面的 OwningProcess> -Force\n')
  } else {
    console.error(`[端口检查] 无法检查端口 ${port}: ${err.message}`)
  }
  process.exit(1)
})
server.once('listening', () => {
  server.close(() => process.exit(0))
})
server.listen(port, '0.0.0.0')
