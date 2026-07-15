/**
 * 将日期转换为友好格式
 * 支持输入：Unix 时间戳（秒）、ISO 字符串、'YYYY-MM-DD HH:mm:ss' 字符串
 *
 * 规则：
 *   < 1 分钟     → "刚刚"
 *   < 1 小时     → "x分钟前"
 *   < 24 小时    → "x小时x分钟前"
 *   < 7 天       → "x天前"
 *   今年内       → "x月x日"
 *   往年         → "xxxx年x月x日"
 *
 * @param {string|number} dateVal — 时间值
 * @returns {string}
 */
export function dateStr(dateVal) {
  if (!dateVal && dateVal !== 0) return ''

  const now = new Date()

  // 尝试解析为 Date 对象
  let dateObj = tryParseDate(dateVal)
  if (!dateObj) return String(dateVal) // 无法解析时原样返回

  const diffMs = now.getTime() - dateObj.getTime() // 毫秒差
  if (diffMs < 0) return '刚刚' // 未来时间

  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return diffMin + '分钟前'

  if (diffHour < 24) {
    const remainingMin = diffMin % 60
    if (remainingMin === 0) return diffHour + '小时前'
    return diffHour + '小时' + remainingMin + '分钟前'
  }

  if (diffDay < 7) return diffDay + '天前'

  const y = dateObj.getFullYear()
  const m = dateObj.getMonth() + 1
  const d = dateObj.getDate()
  const nowY = now.getFullYear()

  if (y === nowY) return m + '月' + d + '日'
  return y + '年' + m + '月' + d + '日'
}

/**
 * 尝试多种格式解析为 Date 对象
 */
function tryParseDate(val) {
  // 数字（Unix 秒级时间戳）
  if (typeof val === 'number') return new Date(val * 1000)

  // 数字字符串（Unix 秒级时间戳）
  if (/^\d{10}$/.test(val)) return new Date(parseInt(val) * 1000)

  // 'YYYY-MM-DD HH:mm:ss' 或 'YYYY/MM/DD HH:mm:ss'
  if (/^\d{4}[-/]\d{1,2}[-/]\d{1,2}/.test(val)) {
    // Safari 不支持 'YYYY-MM-DD' 直接 new Date，统一替换分隔符
    const normalized = val.replace(/-/g, '/')
    const d = new Date(normalized)
    if (!isNaN(d.getTime())) return d
  }

  // 其他（ISO 8601 等）
  const d = new Date(val)
  if (!isNaN(d.getTime())) return d

  return null
}


