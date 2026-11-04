/**
 * 权限点目录（与后端 com.walker.config.security.Permission 对应）。
 *
 * 页面只负责展示与勾选；真正的校验仍以后端权限点枚举为准。
 * 新增权限点时在这里补充分组和中文名，再从 labels.js 补一个短标签即可。
 */
export const PERMISSION_GROUPS = [
  {
    key: 'admin',
    label: '管理端权限',
    sections: [
      {
        section: '基础',
        items: [
          { code: 'admin:access', label: '进入管理后台' }
        ]
      },
      {
        section: '用户',
        items: [
          { code: 'user:read', label: '查看用户' },
          { code: 'user:create', label: '新增用户' },
          { code: 'user:update', label: '编辑用户' },
          { code: 'user:delete', label: '删除用户' }
        ]
      },
      {
        section: '内容',
        items: [
          { code: 'article:read', label: '查看帖子' },
          { code: 'article:review', label: '审核/违规' },
          { code: 'article:delete', label: '删除帖子' },
          { code: 'article:feature', label: '设置精华' },
          { code: 'article:suggestion:adopt', label: '采纳建议' }
        ]
      },
      {
        section: '治理',
        items: [
          { code: 'report:review', label: '举报审核' },
          { code: 'appeal:review', label: '申诉审核' },
          { code: 'violation:review', label: '违规查看' },
          { code: 'violation:add', label: '新增违规' },
          { code: 'violation:cancel', label: '取消违规' },
          { code: 'adopt:review', label: '采纳审批' },
          { code: 'featured:recommend', label: '精华推荐' },
          { code: 'featured:review', label: '精华终审' }
        ]
      },
      {
        section: '版主/配置',
        items: [
          { code: 'moderator:manage', label: '版主管理' },
          { code: 'moderator:complaint:review', label: '版主投诉' },
          { code: 'config:read', label: '配置只读' },
          { code: 'points:read', label: '积分只读' },
          { code: 'points:adjust', label: '积分调整' },
          { code: 'dashboard:read', label: '工作台' }
        ]
      },
      {
        section: '系统/高级',
        items: [
          { code: 'user:grant:admin', label: '授予/调整管理员' },
          { code: 'config:write', label: '配置修改' },
          { code: 'permission:manage', label: '权限管理' },
          { code: 'audit:read', label: '查看审计日志' }
        ]
      }
    ]
  },
  {
    key: 'front',
    label: '前台动作权限',
    sections: [
      {
        section: '发帖/互动',
        items: [
          { code: 'post:create', label: '发帖' },
          { code: 'post:update:own', label: '编辑自己的帖子' },
          { code: 'post:delete:own', label: '删除自己的帖子' },
          { code: 'comment:create', label: '评论' },
          { code: 'comment:delete:own', label: '删除自己的评论' },
          { code: 'reply:create', label: '回复' },
          { code: 'reply:delete:own', label: '删除自己的回复' }
        ]
      },
      {
        section: '其他',
        items: [
          { code: 'report:submit', label: '提交举报' },
          { code: 'appeal:submit', label: '提交申诉' },
          { code: 'moderator:complain', label: '投诉版主' },
          { code: 'upload:file', label: '上传文件' },
          { code: 'profile:update', label: '修改资料' },
          { code: 'password:update', label: '修改密码' }
        ]
      }
    ]
  }
]

const collectCodes = group => group.sections.reduce(
  (codes, section) => codes.concat(section.items.map(item => item.code)),
  []
)

export const ADMIN_CODE_SET = new Set(collectCodes(PERMISSION_GROUPS[0]))
export const FRONT_CODE_SET = new Set(collectCodes(PERMISSION_GROUPS[1]))

/** 四个内置默认组：删除时额外二次确认，并在列表里显示“默认”标记。 */
export const DEFAULT_GROUP_CODES = ['role_super', 'role_admin', 'role_moderator', 'role_user']

/**
 * 管理端关键权限：编辑当前账号自己的主组时不允许移除，
 * 否则可能把自己锁在系统外。
 */
export const CRITICAL_PERMISSION_CODES = ['admin:access', 'permission:manage', 'user:grant:admin']

export function allPermissionItems() {
  return PERMISSION_GROUPS.reduce(
    (items, group) => items.concat(group.sections.reduce(
      (sectionItems, section) => sectionItems.concat(section.items),
      []
    )),
    []
  )
}
