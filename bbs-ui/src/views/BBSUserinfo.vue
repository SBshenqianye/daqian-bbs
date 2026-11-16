<template>
  <main class="max-w-6xl mx-auto py-12 px-page-margin-desktop bg-surface min-h-screen">
    <div class="flex flex-col md:flex-row bg-container rounded-lg shadow-sm overflow-hidden border border-border min-h-[600px]">
      <!-- Side Navigation -->
      <aside class="w-full md:w-64 border-r border-outline-variant bg-surface-container-lowest">
        <div class="p-6">
          <h2 class="font-headline-sm text-headline-sm text-primary mb-6">账户设置</h2>
          <nav class="space-y-1">
            <button
              v-for="item in primaryTabs"
              :key="item.key"
              class="w-full text-left px-4 py-3 font-label-md text-label-md rounded-l-lg transition-all duration-200 flex items-center gap-2"
              :class="activeTab === item.key ? 'sidebar-tab-active' : 'text-on-surface-variant hover:bg-surface-container-low'"
              @click="goTab(item.key)"
            >
              <span class="material-symbols-outlined text-[18px]">{{ item.icon }}</span>
              {{ item.label }}
            </button>

            <div class="border-t border-outline-variant my-2 mx-2"></div>

            <button
              v-for="item in panelTabs"
              :key="item.key"
              class="w-full text-left px-4 py-3 font-label-md text-label-md rounded-l-lg transition-all duration-200 flex items-center gap-2"
              :class="activeTab === item.key ? 'sidebar-tab-active' : 'text-on-surface-variant hover:bg-surface-container-low'"
              @click="goTab(item.key)"
            >
              <span class="material-symbols-outlined text-[18px]">{{ item.icon }}</span>
              {{ item.label }}
              <span v-if="badges[item.key]" class="ml-auto w-2 h-2 rounded-full bg-error shrink-0" title="有新内容"></span>
            </button>
          </nav>
        </div>
      </aside>

      <!-- Content Area：所有 Tab 统一由组件渲染 -->
      <section class="flex-1 p-8 md:p-12">
        <component
          v-if="currentTab"
          :key="currentTab.key"
          :is="currentTab.component"
          :embedded="!!currentTab.panel"
        />
      </section>
    </div>
  </main>
</template>

<script>
import AccountOverview from '@/components/account/AccountOverview.vue'
import AccountProfile from '@/components/account/AccountProfile.vue'
import AccountPassword from '@/components/account/AccountPassword.vue'
import BBSFavorites from '@/views/BBSFavorites.vue'
import BBSMyReplies from '@/views/BBSMyReplies.vue'
import BBSMyPointsLog from '@/views/BBSMyPointsLog.vue'
import BBSMyViolations from '@/views/BBSMyViolations.vue'
import BBSMyAppeals from '@/views/BBSMyAppeals.vue'
import BBSModeratorComplaint from '@/views/BBSModeratorComplaint.vue'
import BBSMyReports from '@/views/BBSMyReports.vue'

/**
 * 账户设置：只负责「侧栏 + Tab 路由 + 红点」，
 * 每个 Tab 都是独立组件，与页面解耦。
 * URL 统一为 /userinfo?tab=<key>。
 */
export default {
  name: 'BBSUserinfo',
  components: {
    AccountOverview,
    AccountProfile,
    AccountPassword,
    BBSFavorites,
    BBSMyReplies,
    BBSMyPointsLog,
    BBSMyViolations,
    BBSMyAppeals,
    BBSModeratorComplaint,
    BBSMyReports,
  },
  data() {
    return {
      activeTab: 'overview',
      primaryTabs: [
        { key: 'overview', label: '我的概览', icon: 'dashboard', component: 'AccountOverview' },
        { key: 'info', label: '信息设置', icon: 'badge', component: 'AccountProfile' },
        { key: 'password', label: '密码修改', icon: 'lock', component: 'AccountPassword' },
      ],
      panelTabs: [
        { key: 'favorites', label: '我的收藏', icon: 'bookmark', component: 'BBSFavorites', panel: true },
        { key: 'replies', label: '我的回复', icon: 'forum', component: 'BBSMyReplies', panel: true },
        { key: 'points', label: '积分明细', icon: 'toll', component: 'BBSMyPointsLog', panel: true },
        { key: 'violations', label: '违规记录', icon: 'gavel', component: 'BBSMyViolations', panel: true },
        { key: 'appeals', label: '申诉记录', icon: 'assignment', component: 'BBSMyAppeals', panel: true },
        { key: 'complaint', label: '版主投诉', icon: 'feedback', component: 'BBSModeratorComplaint', panel: true },
        { key: 'reports', label: '我的举报', icon: 'flag', component: 'BBSMyReports', panel: true },
      ],
      // Tab 红点：有“上次查看之后的新内容”时亮起
      badges: { favorites: false, replies: false, points: false, violations: false, appeals: false, complaint: false, reports: false },
    }
  },
  computed: {
    allTabs() {
      return this.primaryTabs.concat(this.panelTabs)
    },
    tabKeys() {
      return this.allTabs.map(t => t.key)
    },
    currentTab() {
      return this.allTabs.find(t => t.key === this.activeTab) || null
    },
  },
  watch: {
    '$route.query': {
      handler() {
        this.applyRouteTab()
      },
      deep: true,
    },
  },
  mounted() {
    this.applyRouteTab()
    this.loadBadges()
    this.$root.$on('account-tab', this.onAccountTab)
  },
  beforeDestroy() {
    this.$root.$off('account-tab', this.onAccountTab)
  },
  methods: {
    /** 统一 Tab 跳转：所有 Tab 都写 ?tab=<key> */
    goTab(tab, extra) {
      if (this.tabKeys.indexOf(tab) < 0) return
      this.activeTab = tab
      if (this.isPanel(tab)) this.markSeen(tab)
      const query = { tab, ...(extra || {}) }
      if (String(this.$route.path).indexOf('/userinfo') === 0) {
        this.$router.replace({ path: '/userinfo', query }).catch(() => {})
      } else {
        this.$router.push({ path: '/userinfo', query }).catch(() => {})
      }
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    onAccountTab(tab, extra) {
      this.goTab(tab, extra || {})
    },
    isPanel(tab) {
      return this.panelTabs.some(t => t.key === tab)
    },
    /** 读取地址栏 tab；缺失或非法时补成 overview，保证 URL 格式统一 */
    applyRouteTab() {
      const t = this.$route.query.tab
      if (t && this.tabKeys.indexOf(t) >= 0) {
        this.activeTab = t
        if (this.isPanel(t)) this.markSeen(t)
        return
      }
      this.activeTab = 'overview'
      if (String(this.$route.path).indexOf('/userinfo') === 0 && t !== 'overview') {
        this.$router.replace({ path: '/userinfo', query: { tab: 'overview' } }).catch(() => {})
      }
    },
    currentUid() {
      try {
        const u = JSON.parse(window.sessionStorage.getItem('user') || 'null')
        return u && u.id != null ? String(u.id) : ''
      } catch (e) {
        return ''
      }
    },
    seenKey(tab) {
      return `bbs:acct:seen:${this.currentUid()}:${tab}`
    },
    markSeen(tab) {
      this.badges[tab] = false
      try {
        const d = new Date()
        const pad = n => (n < 10 ? '0' + n : '' + n)
        const now = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
        window.localStorage.setItem(this.seenKey(tab), now)
      } catch (e) { /* 忽略存储失败 */ }
    },
    loadBadges() {
      if (!this.currentUid()) return
      this.getRequest('/user/account/badges').then(res => {
        const data = (res && res.obj) || {}
        const seen = (tab) => {
          try { return window.localStorage.getItem(this.seenKey(tab)) || '' } catch (e) { return '' }
        }
        Object.keys(this.badges).forEach(tab => {
          const latest = data[tab]
          if (!latest) { this.badges[tab] = false; return }
          const last = seen(tab)
          if (!last) {
            // 首次进入：仅“回复我的”用未读通知点亮，其余视为已读
            if (tab === 'replies') {
              this.badges[tab] = true
            } else {
              this.badges[tab] = false
              this.markSeen(tab)
            }
            return
          }
          this.badges[tab] = String(latest) > String(last)
        })
      }).catch(() => { /* 红点失败不影响页面 */ })
    },
  },
}
</script>
