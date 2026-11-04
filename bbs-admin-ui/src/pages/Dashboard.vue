<template>
  <div class="min-h-screen bg-surface">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-8">
      <!-- Welcome Hero -->
      <div class="bg-gradient-to-br from-primary to-primary-fixed-dim rounded-xl p-8 md:p-10 mb-8 text-white">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-4 mb-4">
              <span class="material-symbols-outlined text-[40px] text-white/90">admin_panel_settings</span>
              <div>
                <h1 class="font-headline-lg text-headline-lg text-on-primary">欢迎使用后台管理系统</h1>
                <p class="text-body-md text-on-primary/70 mt-1">大千智荟创新创意交流论坛 - 管理平台</p>
              </div>
            </div>
            <p class="text-body-lg text-on-primary/80 max-w-2xl">
              在这里管理用户、内容、版块与权限组。主权限组决定账号身份与数据范围，叠加组只补充权限。
            </p>
          </div>

          <div class="w-full max-w-sm shrink-0 rounded-xl border border-white/20 bg-white/10 p-4 backdrop-blur-sm">
            <div class="mb-2 flex items-center justify-between gap-2">
              <span class="text-[11px] uppercase tracking-wider text-white/70">我的权限</span>
              <span class="rounded-full bg-white/15 px-2 py-0.5 text-[11px] text-white/90">{{ roleLabel }}</span>
            </div>
            <p class="truncate text-title-md font-semibold text-white">{{ currentGroupName }}</p>
            <p class="mt-0.5 truncate text-[11px] text-white/70">
              {{ currentGroupCode }} · {{ dataScopeLabel }} · {{ permissionCount }} 项权限
            </p>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <router-link
                v-if="canManagePermission"
                to="/permission-groups"
                class="inline-flex items-center gap-1 rounded-lg bg-white px-3 py-1.5 text-[12px] font-medium text-primary hover:bg-white/90"
              >
                <span class="material-symbols-outlined text-[15px]">settings</span>
                管理权限组
              </router-link>
              <span v-else class="text-[11px] text-white/70">权限由管理员统一配置</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 数据概览（#7 真实统计） -->
      <div class="mb-8">
        <h2 class="font-headline-md text-headline-md text-on-surface mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">insights</span>
          数据概览
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <div v-for="card in overviewCards" :key="card.label"
               class="bg-container border border-border rounded-xl p-4 hover:shadow-sm transition-shadow">
            <div class="flex items-center justify-between mb-2">
              <span class="material-symbols-outlined text-[24px]" :class="card.iconColor">{{ card.icon }}</span>
            </div>
            <p class="font-headline-sm text-on-surface text-xl leading-tight">{{ formatNum(card.value) }}</p>
            <p class="text-label-sm text-on-surface-variant mt-1">{{ card.label }}</p>
            <p v-if="card.hint" class="text-caption-sm text-on-surface-variant/70 mt-0.5">{{ card.hint }}</p>
          </div>
        </div>
      </div>

      <!-- 权限与安全 -->
      <div class="mb-8 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div class="rounded-xl border border-border bg-container p-5">
          <h2 class="flex items-center gap-2 font-title-lg text-title-lg text-on-surface">
            <span class="material-symbols-outlined text-primary">badge</span>
            我的权限
          </h2>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <div>
              <p class="text-[11px] text-outline">主权限组</p>
              <p class="mt-0.5 truncate text-body-md text-on-surface">{{ currentGroupName }}</p>
            </div>
            <div>
              <p class="text-[11px] text-outline">数据范围</p>
              <p class="mt-0.5 text-body-md text-on-surface">{{ dataScopeLabel }}</p>
            </div>
            <div>
              <p class="text-[11px] text-outline">权限点</p>
              <p class="mt-0.5 text-body-md text-on-surface">{{ permissionCount }} 项</p>
            </div>
            <div>
              <p class="text-[11px] text-outline">账号身份</p>
              <p class="mt-0.5 text-body-md text-on-surface">{{ roleLabel }}</p>
            </div>
          </div>
          <p class="mt-4 text-[11px] leading-relaxed text-outline">
            主权限组决定身份与数据范围；额外叠加的权限组只补充权限，不改变身份。
          </p>
        </div>

        <div class="rounded-xl border border-border bg-container p-5">
          <h2 class="flex items-center gap-2 font-title-lg text-title-lg text-on-surface">
            <span class="material-symbols-outlined text-primary">shield</span>
            权限安全
          </h2>
          <template v-if="canManagePermission">
            <p class="mt-3 text-body-sm leading-relaxed text-on-surface-variant">
              系统会始终保留至少一个启用且具备关键权限的超级管理员组。当前账号主组的关键权限已在权限管理页锁定，避免误操作导致无人能管理系统。
            </p>
            <div class="mt-4 flex flex-wrap gap-2">
              <router-link to="/permission-groups" class="rounded-lg bg-surface-container-low px-3 py-1.5 text-[12px] text-on-surface hover:bg-surface-container">
                权限组管理
              </router-link>
              <router-link to="/permission-matrix" class="rounded-lg bg-surface-container-low px-3 py-1.5 text-[12px] text-on-surface hover:bg-surface-container">
                权限矩阵
              </router-link>
              <router-link v-if="canViewAudit" to="/audit" class="rounded-lg bg-surface-container-low px-3 py-1.5 text-[12px] text-on-surface hover:bg-surface-container">
                审计日志
              </router-link>
            </div>
          </template>
          <p v-else class="mt-3 text-body-sm leading-relaxed text-on-surface-variant">
            如需调整账号权限，请联系超级管理员。权限变更会在下一次请求时生效。
          </p>
        </div>
      </div>

      <!-- 浮动快捷操作（#7）：右下角 dock，待办模块优先前置 -->
      <div class="fixed bottom-6 right-6 z-40 flex flex-col items-end gap-2">
        <transition name="dock-fade">
          <div
            v-if="!quickDockCollapsed"
            class="flex flex-wrap justify-end gap-2 max-w-[600px] bg-container border border-outline-variant/60 rounded-2xl shadow-lg p-3"
          >
            <router-link
              v-for="item in sortedQuickItems"
              :key="item.label"
              :to="item.path"
              class="flex items-center gap-1.5 px-3 py-2 bg-surface-container-low rounded-lg hover:bg-surface-container transition-colors border border-outline-variant/50 group"
            >
              <span class="material-symbols-outlined text-[20px] transition-colors group-hover:text-primary" :class="item.iconColor || 'text-on-surface-variant'">{{ item.icon }}</span>
              <span class="font-label-md text-label-md text-on-surface group-hover:text-primary transition-colors">{{ item.label }}</span>
              <span v-if="countOf(item) > 0" class="bg-error text-on-error text-[11px] font-medium leading-none px-1.5 py-1 rounded-full">{{ countOf(item) > 99 ? '99+' : countOf(item) }}</span>
            </router-link>
          </div>
        </transition>
        <button
          class="w-12 h-12 rounded-full bg-primary text-on-primary shadow-lg flex items-center justify-center hover:bg-primary-fixed-dim transition-colors relative"
          :title="quickDockCollapsed ? '展开快捷操作' : '收起快捷操作'"
          @click="quickDockCollapsed = !quickDockCollapsed"
        >
          <span class="material-symbols-outlined text-[24px]">{{ quickDockCollapsed ? 'bolt' : 'close' }}</span>
          <span
            v-if="quickDockCollapsed && totalPending > 0"
            class="absolute -top-1 -right-1 bg-error text-on-error text-[10px] font-medium leading-none min-w-[18px] h-[18px] px-1 rounded-full flex items-center justify-center"
          >{{ totalPending > 99 ? '99+' : totalPending }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import adminStore from '@/utils/adminStore'

export default {
  name: 'Dashboard',
  data() {
    return {
      // 浮动快捷操作是否收起（#7）
      quickDockCollapsed: false,
      // 快捷入口（badgeKey 为 null 表示不参与待办排序的常驻入口）
      quickItems: [
        { label: '举报管理', path: '/report', icon: 'flag', badgeKey: 'reportPending', iconColor: 'text-rank-gold', permission: 'report:review' },
        { label: '帖子管理', path: '/article', icon: 'rate_review', badgeKey: 'articlePending', iconColor: 'text-tertiary-container', permission: 'article:read' },
        { label: '违规管理', path: '/violation', icon: 'gavel', badgeKey: 'violationActive', iconColor: 'text-error', permission: 'violation:review' },
        { label: '申诉管理', path: '/appeal', icon: 'assignment', badgeKey: 'appealPending', permission: 'appeal:review' },
        { label: '版主投诉', path: '/moderator-complaint', icon: 'feedback', badgeKey: 'moderatorComplaintPending', permission: 'moderator:complaint:review' },
        { label: '采纳审批', path: '/approve-adopt', icon: 'check_circle', badgeKey: 'adoptPending', permission: 'adopt:review' },
        { label: '精华审批', path: '/featured-recommendation', icon: 'stars', badgeKey: 'featuredPending', permission: 'featured:review' },
        { label: '用户管理', path: '/user', icon: 'manage_accounts', badgeKey: null, iconColor: 'text-primary', permission: 'user:read' },
        { label: '权限管理', path: '/permission-groups', icon: 'admin_panel_settings', badgeKey: null, iconColor: 'text-primary', permission: 'permission:manage' },
        { label: '权限矩阵', path: '/permission-matrix', icon: 'policy', badgeKey: null, permission: 'permission:manage' },
        { label: '审计日志', path: '/audit', icon: 'receipt_long', badgeKey: null, permission: 'audit:read' },
        { label: '积分排名', path: '/points', icon: 'leaderboard', badgeKey: null, iconColor: 'text-rank-gold', permission: 'points:read' },
      ],
      stats: {
        userCount: 0,
        articleCount: 0,
        commentCount: 0,
        reportPending: 0,
        appealPending: 0,
        violationActive: 0,
        articlePending: 0,
        featuredCount: 0,
        moderatorComplaintPending: 0,
        adoptPending: 0,
        featuredPending: 0,
      }
    }
  },
  computed: {
    currentAdmin() {
      return adminStore.state.admin || {}
    },
    currentGroupName() {
      return this.currentAdmin.primaryGroupName || '未分配权限组'
    },
    currentGroupCode() {
      return this.currentAdmin.primaryGroupCode || '—'
    },
    permissionCount() {
      return Array.isArray(this.currentAdmin.permissions) ? this.currentAdmin.permissions.length : 0
    },
    dataScopeLabel() {
      return this.currentAdmin.dataScopeType === 'BOARD_MODERATOR' ? '按版主任职' : '全局'
    },
    roleLabel() {
      const role = adminStore.getRole()
      return {
        SUPER_ADMIN: '超级管理员',
        ADMIN: '管理员',
        MODERATOR: '版主',
        USER: '普通用户'
      }[role] || '管理员'
    },
    canManagePermission() {
      return adminStore.hasPermission('permission:manage')
    },
    canViewAudit() {
      return adminStore.hasPermission('audit:read')
    },
    overviewCards() {
      const cards = [
        { label: '注册用户', value: this.stats.userCount, icon: 'group', iconColor: 'text-primary' },
        { label: '帖子总数', value: this.stats.articleCount, icon: 'article', iconColor: 'text-tertiary-container', hint: '待审 ' + this.stats.articlePending + ' · 精华 ' + this.stats.featuredCount },
        { label: '评论总数', value: this.stats.commentCount, icon: 'chat', iconColor: 'text-tertiary-container' },
        { label: '举报待审', value: this.stats.reportPending, icon: 'flag', iconColor: 'text-rank-gold' },
        { label: '申诉待审', value: this.stats.appealPending, icon: 'gavel', iconColor: 'text-rank-gold' },
        { label: '进行中违规', value: this.stats.violationActive, icon: 'gpp_bad', iconColor: 'text-error' }
      ]
      // MODERATOR 只展示本板块口径，隐藏全局用户/评论指标
      if (adminStore.isModerator()) {
        return cards.filter(c => c.label !== '注册用户' && c.label !== '评论总数')
      }
      return cards
    },
    /** 按权限点过滤快捷入口 */
    visibleQuickItems() {
      return this.quickItems.filter(item => adminStore.hasPermission(item.permission))
    },
    /** 快捷入口的待办数（badgeKey 为空返回 0） */
    totalPending() {
      return this.visibleQuickItems.reduce((sum, item) => sum + this.countOf(item), 0)
    },
    /** 排序：有待办的在前面（按待办数降序），无待办按默认顺序 */
    sortedQuickItems() {
      return [...this.visibleQuickItems].sort((a, b) => {
        const ba = this.countOf(a)
        const bb = this.countOf(b)
        if (ba > 0 && bb > 0) return bb - ba
        if (ba > 0) return -1
        if (bb > 0) return 1
        return this.visibleQuickItems.indexOf(a) - this.visibleQuickItems.indexOf(b)
      })
    },
  },
  mounted() {
    this.loadStats()
  },
  methods: {
    /** 快捷入口的待办数（badgeKey 为空返回 0） */
    countOf(item) {
      return item.badgeKey ? (Number(this.stats[item.badgeKey]) || 0) : 0
    },
    formatNum(n) {
      const v = Number(n) || 0
      return v >= 10000 ? (v / 10000).toFixed(1).replace(/\.0$/, '') + 'w' : String(v)
    },
    loadStats() {
      this.postRequest('/admin/dashboard/counts', {}).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          this.stats = Object.assign({}, this.stats, resp.obj)
        }
      }).catch(err => {
        console.warn('[Dashboard] load counts', err)
      })
    }
  }
}
</script>

<style scoped>
/* 浮动快捷 dock 展开/收起过渡（#7） */
.dock-fade-enter-active,
.dock-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dock-fade-enter,
.dock-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
