/**
 * 管理端中文名称映射。
 *
 * 页面默认展示中文名称；内部编码通过 title（鼠标悬浮）查看。
 * 新增权限点/审计动作时，在这里补充中文名即可。
 */

export const PERMISSION_LABELS = {
  'admin:access': '进入管理后台',

  'user:read': '查看用户',
  'user:create': '新增用户',
  'user:update': '编辑用户',
  'user:delete': '删除用户',
  'user:grant:admin': '授予/调整管理员',

  'config:read': '配置只读',
  'config:write': '配置修改',
  'permission:manage': '权限管理',
  'audit:read': '查看审计日志',

  'article:read': '查看帖子',
  'article:review': '审核帖子/违规',
  'article:delete': '删除帖子',
  'article:feature': '设置精华',
  'article:suggestion:adopt': '采纳建议',

  'report:review': '举报审核',
  'appeal:review': '申诉审核',
  'violation:review': '违规查看',
  'violation:add': '新增违规',
  'violation:cancel': '取消违规',

  'moderator:manage': '版主管理',
  'moderator:complaint:review': '版主投诉处理',
  'adopt:review': '采纳审批',

  'featured:recommend': '精华推荐',
  'featured:review': '精华终审',

  'points:read': '积分只读',
  'points:adjust': '积分调整',
  'dashboard:read': '工作台',

  'post:create': '发帖',
  'post:update:own': '编辑自己的帖子',
  'post:delete:own': '删除自己的帖子',
  'comment:create': '发表评论',
  'comment:delete:own': '删除自己的评论',
  'reply:create': '回复',
  'reply:delete:own': '删除自己的回复',
  'report:submit': '提交举报',
  'appeal:submit': '提交申诉',
  'moderator:complain': '投诉版主',
  'upload:file': '上传文件',
  'profile:update': '修改资料',
  'password:update': '修改密码'
}

export const AUDIT_MODULE_LABELS = {
  ARTICLE: '文章',
  REPORT: '举报',
  VIOLATION: '违规',
  APPEAL: '申诉',
  ADOPT: '采纳',
  FEATURED: '精华',
  MODERATOR: '版主',
  USER: '用户',
  CONFIG: '配置',
  PERMISSION: '权限组',
  AUTH: '权限拒绝'
}

export const AUDIT_RESULT_LABELS = {
  success: '成功',
  fail: '失败'
}

export const AUDIT_TARGET_TYPE_LABELS = {
  ARTICLE: '文章',
  ARTICLE_LABEL: '文章标签',
  DICT: '字典',
  FEATURED_RECOMMENDATION: '精华推荐',
  MODERATOR_BOARD: '版主板块',
  MODERATOR_COMPLAINT: '版主投诉',
  MODERATOR_REWARD: '版主奖励',
  ORG: '组织',
  PERMISSION_GROUP: '权限组',
  PERMISSION_PRESET: '权限预设',
  REPORT: '举报',
  SENSITIVE_WORD: '敏感词',
  SLIDESHOW: '轮播图',
  SYSTEM_CONFIG: '系统配置',
  USER: '用户',
  VIOLATION: '违规',
  VIOLATION_TARGET: '违规对象',
  ADOPT: '采纳',
  APPEAL: '申诉',
  ENDPOINT: '接口'
}

/**
 * 审计动作按模块分组，供筛选下拉框使用。
 */
export const AUDIT_ACTION_GROUPS = [
  { module: 'ARTICLE', label: '文章', actions: [
    { value: 'ARTICLE_AUDIT', label: '文章审核' },
    { value: 'ARTICLE_BATCH_AUDIT', label: '批量审核文章' },
    { value: 'ARTICLE_DELETE', label: '删除文章' },
    { value: 'ARTICLE_BATCH_DELETE', label: '批量删除文章' },
    { value: 'ARTICLE_FEATURE_CHANGE', label: '设置/取消精华' }
  ] },
  { module: 'REPORT', label: '举报', actions: [
    { value: 'REPORT_REVIEW', label: '举报审核' }
  ] },
  { module: 'VIOLATION', label: '违规', actions: [
    { value: 'VIOLATION_ADD', label: '新增违规' },
    { value: 'VIOLATION_CANCEL', label: '取消违规' }
  ] },
  { module: 'APPEAL', label: '申诉', actions: [
    { value: 'APPEAL_REVIEW', label: '申诉审核' }
  ] },
  { module: 'ADOPT', label: '采纳', actions: [
    { value: 'ADOPT_REVIEW', label: '采纳审批' },
    { value: 'SUGGESTION_ADOPT', label: '采纳建议' }
  ] },
  { module: 'FEATURED', label: '精华', actions: [
    { value: 'FEATURED_RECOMMEND', label: '精华推荐' },
    { value: 'FEATURED_REVIEW', label: '精华终审' }
  ] },
  { module: 'MODERATOR', label: '版主', actions: [
    { value: 'MODERATOR_APPOINT', label: '任命版主' },
    { value: 'MODERATOR_DISMISS', label: '撤销版主' },
    { value: 'MODERATOR_BOARDS_UPDATE', label: '调整版主板块' },
    { value: 'MODERATOR_REWARD_GRANT', label: '发放版主奖励' },
    { value: 'MODERATOR_REWARD_CANCEL', label: '取消版主奖励' },
    { value: 'MODERATOR_REWARD_RESTORE', label: '恢复版主奖励' },
    { value: 'MODERATOR_REWARD_AUTO', label: '自动发放版主奖励' },
    { value: 'COMPLAINT_REVIEW', label: '版主投诉审核' }
  ] },
  { module: 'USER', label: '用户', actions: [
    { value: 'USER_CREATE', label: '新增用户' },
    { value: 'USER_UPDATE', label: '修改用户' },
    { value: 'USER_DELETE', label: '删除用户' },
    { value: 'USER_BATCH_DELETE', label: '批量删除用户' },
    { value: 'USER_STATUS_CHANGE', label: '启用/禁用用户' },
    { value: 'USER_ROLE_CHANGE', label: '修改用户角色' },
    { value: 'USER_RESTRICT_POST', label: '限制发帖' },
    { value: 'USER_RESET_PASSWORD', label: '重置密码' }
  ] },
  { module: 'CONFIG', label: '配置', actions: [
    { value: 'CONFIG_DICT_CREATE', label: '新增字典' },
    { value: 'CONFIG_DICT_UPDATE', label: '修改字典' },
    { value: 'CONFIG_DICT_DELETE', label: '删除字典' },
    { value: 'CONFIG_LABEL_CREATE', label: '新增标签' },
    { value: 'CONFIG_LABEL_UPDATE', label: '修改标签' },
    { value: 'CONFIG_LABEL_DELETE', label: '删除标签' },
    { value: 'CONFIG_ORG_CREATE', label: '新增组织' },
    { value: 'CONFIG_ORG_DELETE', label: '删除组织' },
    { value: 'CONFIG_ORG_DISPLAY_UPDATE', label: '修改组织展示' },
    { value: 'CONFIG_ORG_RANKING_UPDATE', label: '修改组织排名' },
    { value: 'CONFIG_SENSITIVE_ADD', label: '新增敏感词' },
    { value: 'CONFIG_SENSITIVE_DELETE', label: '删除敏感词' },
    { value: 'CONFIG_SENSITIVE_IMPORT', label: '导入敏感词' },
    { value: 'CONFIG_SLIDESHOW_CREATE', label: '新增轮播图' },
    { value: 'CONFIG_SLIDESHOW_UPDATE', label: '修改轮播图' },
    { value: 'CONFIG_SYSTEM_CREATE', label: '新增系统配置' },
    { value: 'CONFIG_SYSTEM_UPDATE', label: '修改系统配置' },
    { value: 'CONFIG_SYSTEM_DELETE', label: '删除系统配置' }
  ] },
  { module: 'PERMISSION', label: '权限组', actions: [
    { value: 'PERMISSION_GROUP_CREATE', label: '新建权限组' },
    { value: 'PERMISSION_GROUP_UPDATE', label: '修改权限组' },
    { value: 'PERMISSION_GROUP_DELETE', label: '删除权限组' },
    { value: 'PERMISSION_GROUP_ITEMS', label: '修改权限组权限' },
    { value: 'PERMISSION_GROUP_ASSIGN_USERS', label: '绑定权限组用户' },
    { value: 'PERMISSION_GROUP_REMOVE_USER', label: '移除权限组用户' },
    { value: 'PERMISSION_PRESET_CREATE', label: '保存权限预设' },
    { value: 'PERMISSION_PRESET_DELETE', label: '删除权限预设' },
    { value: 'PERMISSION_PRESET_APPLY', label: '应用权限预设' }
  ] },
  { module: 'AUTH', label: '权限拒绝', actions: [
    { value: 'AUTH_DENIED', label: '权限拒绝' }
  ] }
]

export const AUDIT_MODULE_OPTIONS = Object.keys(AUDIT_MODULE_LABELS).map(value => ({
  value,
  label: AUDIT_MODULE_LABELS[value]
}))

export const AUDIT_TARGET_TYPE_OPTIONS = Object.keys(AUDIT_TARGET_TYPE_LABELS).map(value => ({
  value,
  label: AUDIT_TARGET_TYPE_LABELS[value]
}))

const actionLabelMap = {}
AUDIT_ACTION_GROUPS.forEach(group => {
  group.actions.forEach(action => {
    actionLabelMap[action.value] = action.label
  })
})
export const AUDIT_ACTION_LABELS = actionLabelMap

export function labelOf(map, value, fallback = '—') {
  if (value === null || value === undefined || value === '') return fallback
  return map[value] || String(value)
}
