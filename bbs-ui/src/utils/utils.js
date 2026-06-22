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