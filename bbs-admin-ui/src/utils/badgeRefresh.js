/**
 * 管理端待办红点刷新信号
 *
 * 待办类操作（举报审核、申诉处理、采纳审批、删帖等）成功后触发，
 * Sidebar 等订阅方据此重新拉取 /admin/dashboard/counts，保证红点即时更新。
 * 只读接口与计数接口自身不触发，避免无意义的重复请求。
 */
import bus from '@/components/common/bus'

export const BADGE_REFRESH_EVENT = 'badge-refresh'

/** 只读请求（不改变待办数量）：列表、计数、详情、统计等 */
const READ_ONLY_URL_PATTERN = /(\/dashboard\/counts|\/tabCounts|\/article\/list|\/featured\/list|\/recommendList|\/pendingAdopts|\/pageArticleLabel|\/listDict|\/systemConfig\/list|\/listByGroup|\/listGrouped|\/appeal\/list|\/violation\/list|\/moderatorComplaint\/list|\/moderator\/list|\/cancelledRewards|\/pointsRank|\/personalPointsRank|\/articleStatisticByOrg|\/getAllUser|\/getCommentReply|\/getArticleFileByArticleId)(\/|$|\?)/

let timer = null

/**
 * 判断一次成功响应是否可能改变待办数量：写操作 + 有登录态 + 非只读接口
 * @param {Object} config axios 请求 config
 * @returns {boolean}
 */
export function shouldRefreshBadges(config) {
  if (!config) return false
  if (String(config.method || '').toLowerCase() === 'get') return false
  // 登录接口不触发：此时 token 尚未写入，请求计数会误判为 401
  if (String(config.url || '').indexOf('/common/login') !== -1) return false
  if (READ_ONLY_URL_PATTERN.test(String(config.url || ''))) return false
  try {
    return !!window.sessionStorage.getItem('tokenStr')
  } catch (e) {
    return false
  }
}

/**
 * 触发红点刷新（300ms 防抖，合并批量操作产生的多次请求）
 */
export function requestBadgeRefresh() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    timer = null
    bus.$emit(BADGE_REFRESH_EVENT)
  }, 300)
}
