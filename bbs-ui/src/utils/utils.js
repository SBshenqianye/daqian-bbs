/**
 * 格式化日期
 * @prama t 时间戳
 * @return str MM-dd HH:mm
 */
export function formatDate(t) {
    t = t || Date.now();
    let time = new Date(t);
    let str = time.getMonth() < 9 ? '0' + (time.getMonth() + 1) : time.getMonth() + 1;
    str += '-';
    str += time.getDate() < 10 ? '0' + time.getDate() : time.getDate();
    str += ' ';
    str += time.getHours();
    str += ':';
    str += time.getMinutes() < 10 ? '0' + time.getMinutes() : time.getMinutes();
    return str;
};

/**
 * 修复 markdown / HTML 链接中缺少协议前缀的 URL，自动补上 http://
 * 例如 [百度](www.4399.com) → [百度](http://www.4399.com)
 * 例如 <a href="www.4399.com"> → <a href="http://www.4399.com">
 * @param {string} content  markdown 或 HTML 内容
 * @returns {string}
 */
export function normalizeUrls(content) {
  if (!content) return content

  content = normalizeFileUrls(content)

  // 处理 markdown 链接: [text](url) 中 url 没有协议的情况
  content = content.replace(
    /\[([^\]]*)\]\(((?!https?:\/\/|ftp:\/\/|\/\/|data:|mailto:|tel:|#|\/)[^\s\)]+)\)/g,
    (match, text, url) => {
      if (url.startsWith('./') || url.startsWith('../')) return match
      if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(url)) return match
      return `[${text}](http://${url})`
    }
  )

  // 处理 HTML 链接: href="url" 中 url 没有协议的情况
  content = content.replace(
    /href="((?!https?:\/\/|ftp:\/\/|\/\/|data:|mailto:|tel:|#|\/)[^"]+)"/g,
    (match, url) => {
      if (url.startsWith('./') || url.startsWith('../')) return match
      if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(url)) return match
      return `href="http://${url}"`
    }
  )

  return content
}

const FILE_CONTEXT_PATH = '/bbs-server'
const FILE_PREFIX = `${FILE_CONTEXT_PATH}/files/`

function normalizeRelativeFilePath(path) {
  if (!path || typeof path !== 'string') return path

  let normalized = path.replace(/\/bbs-server\/bbs-server\/files\//g, FILE_PREFIX)

  if (normalized.startsWith('bbs-server/files/')) {
    return `/${normalized}`
  }
  if (normalized.startsWith('files/')) {
    return `${FILE_CONTEXT_PATH}/${normalized}`
  }
  if (normalized.startsWith('/files/')) {
    return `${FILE_CONTEXT_PATH}${normalized}`
  }
  return normalized
}

export function normalizeFileUrl(url) {
  if (!url || typeof url !== 'string') return url
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  const normalized = normalizeRelativeFilePath(url)

  if (normalized.startsWith(FILE_PREFIX)) {
    if (typeof window !== 'undefined' && window.location && window.location.origin) {
      return `${window.location.origin}${normalized}`
    }
    return normalized
  }
  return normalized
}

export function normalizeFileUrls(content) {
  if (!content || typeof content !== 'string') return content
  return content.replace(
    /\/bbs-server\/bbs-server\/files\/|\/bbs-server\/files\/|\/files\/|bbs-server\/files\/|files\//g,
    (match, offset, source) => {
      const prev = offset > 0 ? source[offset - 1] : ''
      if ((match === 'bbs-server/files/' || match === 'files/') && /[a-zA-Z0-9_-]/.test(prev)) {
        return match
      }
      return normalizeRelativeFilePath(match)
    }
  )
}

/**
 * 友好时间展示
 * @param {string|number} time 时间字符串（如 "2026-07-20 08:47:15"）或 13 位时间戳
 * @returns {string}
 *   - 刚刚（< 1 分钟）
 *   - xx分钟前（< 1 小时）
 *   - xx小时xx分钟前（当天内）
 *   - xx天前（7 天内）
 *   - xx月xx日（今年）
 *   - xxxx年xx月xx日（往年）
 */
export function friendlyTime(time) {
  if (!time && time !== 0) return ''
  const date = new Date(typeof time === 'string' ? time.replace(/-/g, '/') : time)
  if (isNaN(date.getTime())) return String(time)

  const now = new Date()
  const diffMs = now - date
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`

  if (diffHours < 24) {
    const remainMinutes = diffMinutes - diffHours * 60
    if (remainMinutes === 0) return `${diffHours}小时前`
    return `${diffHours}小时${remainMinutes}分钟前`
  }

  if (diffDays < 7) return `${diffDays}天前`

  const month = date.getMonth() + 1
  const day = date.getDate()

  if (date.getFullYear() === now.getFullYear()) {
    return `${month}月${day}日`
  }

  return `${date.getFullYear()}年${month}月${day}日`
}

/**
 * 距当前时间点的时长
 * @prama time 13位时间戳
 * @return str x秒 / x分钟 / x小时
 */
export function formateTime(time) {
    const second = 1000;
    const minute = second * 60;
    const hour = minute * 60;
    const day = hour * 24;
    const now = new Date().getTime();
    const diffValue = now - time;

    // 计算差异时间的量级
    const secondC = diffValue / second;
    const minC = diffValue / minute;
    const hourC = diffValue / hour;
    const dayC = diffValue / day;

    if (dayC >= 1) {
        return parseInt(dayC) + "天";
    } else if (hourC >= 1) {
        return parseInt(hourC) + "小时";
    } else if (minC >= 1) {
        return parseInt(minC) + "分钟";
    } else if (secondC >= 1) {
        return parseInt(secondC) + "秒";
    } else {
        return '0秒';
    }
}
