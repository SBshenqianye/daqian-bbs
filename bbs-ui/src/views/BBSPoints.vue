<template>
  <div class="bg-surface text-on-surface antialiased min-h-screen">
    <main class="ui-page-wide">
      <PageHeader
        title="排行榜"
        subtitle="按积分看组织与个人的阶段表现"
        icon="emoji_events"
        tone="gold"
      >
        <span class="ui-chip ui-chip-muted">
          <span class="material-symbols-outlined">calendar_month</span>
          {{ activeTimeLabel }}
        </span>
      </PageHeader>

      <!-- Tab 栏：与论坛/账户一致的轻量胶囊 -->
      <div class="inline-flex items-center gap-1 mb-3 p-1 rounded-xl border border-border bg-container overflow-x-auto max-w-full">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="ui-tab shrink-0"
          :class="{ 'is-active': activeTabKey === tab.key }"
          @click="switchTab(tab)"
        >
          <span class="material-symbols-outlined text-[16px]">{{ tab.mode === 'personal' ? 'person' : 'apartment' }}</span>
          {{ tab.label }}
        </button>
      </div>

      <!-- 时间筛选行（所有 Tab 共用） -->
      <div class="ui-toolbar mb-4">
        <span class="ui-meta-item text-[11.5px] text-on-surface-variant"><span class="material-symbols-outlined">date_range</span>时间</span>
        <button
          v-for="btn in timeQuickBtns"
          :key="btn.key"
          class="px-2.5 py-1 text-[11.5px] rounded-md transition-colors"
          :class="activeTimeKey === btn.key
            ? 'bg-primary/10 text-primary font-semibold'
            : 'text-on-surface-variant hover:text-primary hover:bg-primary/5'"
          @click="selectQuickTime(btn)"
        >
          {{ btn.label }}
        </button>
        <span class="text-outline-variant mx-1 hidden sm:inline">|</span>
        <div class="flex items-center gap-1.5">
          <input
            type="date"
            v-model="customStartDate"
            class="px-2 py-1 bg-surface border border-outline-variant rounded-md text-[11.5px] text-on-surface outline-none focus:border-primary/40"
            @change="applyCustomRange"
          />
          <span class="text-on-surface-variant text-[11px]">~</span>
          <input
            type="date"
            v-model="customEndDate"
            class="px-2 py-1 bg-surface border border-outline-variant rounded-md text-[11.5px] text-on-surface outline-none focus:border-primary/40"
            @change="applyCustomRange"
          />
        </div>
        <span class="ui-meta-item text-[10.5px] text-outline ml-auto hidden lg:flex">
          <span class="material-symbols-outlined">calendar_month</span>{{ activeRangeLabel }}
        </span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- 组织排名列表 -->
      <template v-if="!loading && activeMode === 'org'">
        <!-- 本期概况 -->
        <div v-if="orgRankList.length" class="grid grid-cols-2 md:grid-cols-4 gap-2 mb-3">
          <div class="ui-card px-3 py-2.5 flex items-center gap-2.5">
            <span class="w-7 h-7 rounded-lg bg-primary/10 text-primary grid place-items-center shrink-0">
              <span class="material-symbols-outlined text-[16px]">apartment</span>
            </span>
            <div class="min-w-0">
              <p class="text-[10px] text-outline leading-none">上榜组织</p>
              <p class="text-[16px] font-bold text-on-surface tabnum leading-6">{{ orgSummary.orgCount }}</p>
            </div>
          </div>
          <div class="ui-card px-3 py-2.5 flex items-center gap-2.5">
            <span class="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 grid place-items-center shrink-0">
              <span class="material-symbols-outlined text-[16px]">group</span>
            </span>
            <div class="min-w-0">
              <p class="text-[10px] text-outline leading-none">参与人数</p>
              <p class="text-[16px] font-bold text-on-surface tabnum leading-6">{{ orgSummary.memberCount }}</p>
            </div>
          </div>
          <div class="ui-card px-3 py-2.5 flex items-center gap-2.5">
            <span class="w-7 h-7 rounded-lg bg-sky-50 text-sky-600 grid place-items-center shrink-0">
              <span class="material-symbols-outlined text-[16px]">edit_note</span>
            </span>
            <div class="min-w-0">
              <p class="text-[10px] text-outline leading-none">本期发帖</p>
              <p class="text-[16px] font-bold text-on-surface tabnum leading-6">{{ orgSummary.posts }}</p>
            </div>
          </div>
          <div class="ui-card px-3 py-2.5 flex items-center gap-2.5">
            <span class="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 grid place-items-center shrink-0">
              <span class="material-symbols-outlined text-[16px]">chat_bubble</span>
            </span>
            <div class="min-w-0">
              <p class="text-[10px] text-outline leading-none">本期回帖</p>
              <p class="text-[16px] font-bold text-on-surface tabnum leading-6">{{ orgSummary.replies }}</p>
            </div>
          </div>
        </div>

        <!-- Top 3 领奖台 -->
        <div v-if="topThree.length" class="grid grid-cols-1 md:grid-cols-3 items-end gap-3 mb-3">
          <div
            v-for="podium in topThree"
            :key="'podium-'+podium.rank"
            class="podium-card relative rounded-2xl overflow-hidden transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg"
            :class="[
              podium.orderClass,
              podium.cardClass,
              podium.rank === 1 ? 'h-[244px]' : podium.rank === 2 ? 'h-[224px]' : 'h-[216px]'
            ]"
          >
            <span class="podium-top" :style="{ color: podium.accent }">TOP {{ podium.rank }}</span>
            <div class="relative z-10 h-full px-4 pb-4 pt-4 flex flex-col items-center text-center">
              <div class="podium-badge" :class="'podium-badge--' + podium.tone">
                <span class="text-[18px] font-black leading-none">{{ podium.rank }}</span>
                <span v-if="podium.rank === 1" class="podium-crown material-symbols-outlined">workspace_premium</span>
              </div>
              <h3 class="mt-2.5 font-semibold text-center line-clamp-2 min-h-[40px] flex items-center justify-center px-1 text-[13.5px] text-on-surface">{{ podium.name }}</h3>
              <div class="mt-1">
                <p class="text-[10px] text-outline">人均得分</p>
                <p class="font-black text-primary tabnum leading-none" :class="podium.rank === 1 ? 'text-[30px]' : 'text-[24px]'">{{ podium.score }}</p>
              </div>
              <div class="podium-bar mt-2 w-full">
                <span :style="{ width: scoreWidth(podium.score, topScore) + '%', background: podium.accent }"></span>
              </div>
              <div class="mt-2 text-[10px] text-outline truncate max-w-full">总分 {{ podium.totalPoints }} · {{ podium.memberCount }} 人</div>
              <div class="mt-1 flex items-center justify-center gap-2 text-[10px] text-on-surface-variant">
                <span>发帖 {{ podium.posts }}</span>
                <span class="text-outline-variant">·</span>
                <span>回帖 {{ podium.replies }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 排名列表 (4+) -->
        <div v-if="restRanks.length" class="space-y-2">
          <div
            v-for="item in restRanks"
            :key="item.orgNo"
            class="ui-card ui-card-hover overflow-hidden"
          >
            <div class="p-3.5 flex items-start gap-3">
              <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center bg-surface-container rounded-lg text-on-surface-variant font-bold text-[12px] tabnum mt-0.5">{{ item.rank }}</div>
              <div class="flex-grow min-w-0">
                <div class="flex items-start justify-between gap-3">
                  <h4 class="text-[14px] font-semibold text-on-surface truncate min-w-0">{{ item.name }}</h4>
                  <div class="text-right shrink-0">
                    <p class="text-[17px] font-bold text-on-surface tabnum leading-5">{{ item.score }}</p>
                    <p class="text-[9.5px] text-outline">人均得分</p>
                  </div>
                </div>
                <div class="ui-meta mt-1">
                  <span class="ui-meta-item"><span class="material-symbols-outlined">group</span>{{ item.memberCount }} 人</span>
                  <span class="ui-meta-item"><span class="material-symbols-outlined">edit_note</span>发帖 {{ item.posts }}</span>
                  <span class="ui-meta-item"><span class="material-symbols-outlined">chat_bubble</span>回帖 {{ item.replies }}</span>
                  <span class="ui-meta-item ml-auto">总分 {{ item.totalPoints }}</span>
                </div>
                <div class="mt-1.5 h-1 rounded-full bg-surface-container overflow-hidden">
                  <div class="h-full rounded-full bg-primary/60 transition-all" :style="{ width: scoreWidth(item.score, topScore) + '%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!topThree.length && !restRanks.length" class="ui-empty">
          <span class="material-symbols-outlined">leaderboard</span>
          <p class="ui-empty-title">暂无排名数据</p>
          <p class="ui-empty-desc">换个时间范围再看看</p>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="ui-pager mt-4">
          <span class="text-[11.5px] text-outline mr-auto">共 {{ totalCount }} 条记录</span>
          <button class="ui-pager-btn" :disabled="currentPage <= 1" @click="currentPage--">
            <span class="material-symbols-outlined text-[16px]">chevron_left</span>
          </button>
          <button
            v-for="page in totalPages"
            :key="page"
            class="ui-pager-btn"
            :class="{ 'is-active': page === currentPage }"
            @click="currentPage = page"
          >{{ page }}</button>
          <button class="ui-pager-btn" :disabled="currentPage >= totalPages" @click="currentPage++">
            <span class="material-symbols-outlined text-[16px]">chevron_right</span>
          </button>
        </div>
      </template>

      <!-- 个人排名列表 -->
      <template v-if="!loading && activeMode === 'personal'">
        <div v-if="personalRankList.length" class="space-y-2">
          <div class="flex items-end justify-between gap-3 mb-1">
            <div>
              <h3 class="ui-section-title">
                <span class="material-symbols-outlined text-[16px] text-primary">person</span>个人排名
              </h3>
              <p class="ui-subtitle mt-1">共 {{ personalRankList.length }} 人上榜，按积分排序</p>
            </div>
            <span class="ui-chip ui-chip-muted">{{ activeTimeLabel }}</span>
          </div>
          <div
            v-for="(item, i) in personalRankList"
            :key="item.userId"
            class="ui-card flex items-center gap-3 p-3 transition-colors"
            :class="item.userId === currentUserId ? 'border-l-[3px] border-l-primary bg-primary/5' : ''"
          >
            <div class="flex-shrink-0 w-7 h-7 flex items-center justify-center rounded-md font-bold text-[11px]" :class="getRankClass(i + 1)">
              {{ i + 1 }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2 min-w-0">
                  <BBSUserBadge
                    :avatar="normalizeFileUrl(item.portrait)"
                    :nickname="item.nickName"
                    :org-name="item.orgName"
                    :level-index="item.levelIndex"
                    :icon-svg="item.iconSvg || ''"
                    :user-id="item.userId"
                    size="md"
                    layout="inline"
                    class="min-w-0"
                  />
                  <BBSLevelBadge :level-index="item.levelIndex" :icon-svg="item.iconSvg || ''" class="shrink-0" />
                </div>
                <p class="text-[15px] font-bold text-primary tabnum leading-5 shrink-0">{{ item.points }} <span class="text-[9.5px] font-normal text-outline">分</span></p>
              </div>
              <div class="flex items-center justify-between gap-2 mt-1">
                <p class="text-[10px] text-outline">发帖 {{ item.posts }} · 回帖 {{ item.replies }}</p>
              </div>
              <div class="mt-1 h-1 rounded-full bg-surface-container overflow-hidden">
                <div class="h-full rounded-full bg-primary/60" :style="{ width: scoreWidth(item.points, personalTopScore) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!personalRankList.length" class="ui-empty">
          <span class="material-symbols-outlined">person_off</span>
          <p class="ui-empty-title">暂无个人排名数据</p>
          <p class="ui-empty-desc">换个时间范围再看看</p>
        </div>

        <!-- 底部留白，避免固定栏遮挡内容 -->
        <div v-if="currentUserInfo" class="h-24"></div>
      </template>

    </main>

    <!-- 固定底部栏：当前用户积分排名（仅个人排名 Tab 显示） -->
    <div
      v-if="showStickyBar"
      class="bottom-bar fixed bottom-0 left-0 right-0 bg-surface/95 backdrop-blur-sm border-t border-outline-variant/30 z-40"
    >
      <div class="max-w-7xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-2.5 flex items-center gap-3">
        <BBSUserBadge
          :avatar="normalizeFileUrl(currentUserInfo.portrait)"
          :nickname="currentUserInfo.nickName"
          :org-name="currentUserInfo.orgName"
          :level-index="currentUserInfo.levelIndex"
          :icon-svg="currentUserInfo.iconSvg || ''"
          :user-id="currentUserInfo.userId"
          size="md"
          layout="stacked"
          class="min-w-0 flex-1"
        />
        <div class="text-right shrink-0">
          <p class="text-[10px] text-outline">我的积分</p>
          <p class="text-[16px] font-bold text-primary tabnum leading-5">{{ currentUserInfo.points }}</p>
        </div>
        <div class="text-right shrink-0 pl-3 border-l border-outline-variant/50">
          <p class="text-[10px] text-outline">排名</p>
          <p class="text-[16px] font-bold text-on-surface tabnum leading-5">#{{ currentUserInfo.rankNum }}</p>
        </div>
      </div>
    </div>
    <!-- /固定底部栏 -->
  </div>
</template>

<script>
import { getUser } from '@/utils/auth'
import { normalizeFileUrl } from '@/utils/utils'
import PageHeader from '@/components/PageHeader.vue'
import BBSUserBadge from '@/components/BBSUserBadge.vue'
import BBSLevelBadge from '@/components/BBSLevelBadge.vue'
import { handleResponse } from '../../../shared/feedback'

const RANK_COLORS = [
  { tone: 'gold', cardClass: 'podium-card--gold', orderClass: 'order-1 md:order-2', accent: '#b77900' },
  { tone: 'silver', cardClass: 'podium-card--silver', orderClass: 'order-2 md:order-1', accent: '#64748b' },
  { tone: 'bronze', cardClass: 'podium-card--bronze', orderClass: 'order-3', accent: '#b45309' },
]

const TIME_BTNS = [
  { key: 'month', label: '本月' },
  { key: 'quarter', label: '本季' },
  { key: 'year', label: '本年' },
]

/**
 * 计算时间范围
 */
function calcTimeRange(key) {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() // 0-based
  const pad = n => String(n).padStart(2, '0')

  if (key === 'month') {
    return {
      start: `${y}-${pad(m + 1)}-01`,
      end: `${y}-${pad(m + 1)}-${pad(new Date(y, m + 1, 0).getDate())}`,
    }
  }
  if (key === 'quarter') {
    const qStartM = Math.floor(m / 3) * 3
    const qEndM = qStartM + 2
    return {
      start: `${y}-${pad(qStartM + 1)}-01`,
      end: `${y}-${pad(qEndM + 1)}-${pad(new Date(y, qEndM + 1, 0).getDate())}`,
    }
  }
  if (key === 'year') {
    return {
      start: `${y}-01-01`,
      end: `${y}-12-31`,
    }
  }
  return { start: '', end: '' }
}

export default {
  name: 'BBSPoints',
  components: { PageHeader, BBSUserBadge, BBSLevelBadge },
  data() {
    return {
      loading: false,
      // Tab
      tabs: [],
      activeTabKey: '',
      activeMode: 'org',   // 'org' | 'personal'
      activeCategoryId: null, // 当前选中的排行榜分类；初始化后取第一个分类
      // 时间
      activeTimeKey: 'month',
      customStartDate: '',
      customEndDate: '',
      // 组织排名
      orgRankList: [],
      currentPage: 1,
      pageSize: 10,
      // 个人排名
      personalRankList: [],
      currentUserInfo: null,
    }
  },
  computed: {
    activeTimeRange() {
      if (this.activeTimeKey === 'custom') {
        return { start: this.customStartDate, end: this.customEndDate }
      }
      return calcTimeRange(this.activeTimeKey)
    },
    activeRangeLabel() {
      const range = this.activeTimeRange
      return range.start && range.end ? `${range.start} ~ ${range.end}` : '自定义时间'
    },
    topScore() {
      return (this.orgRankList || []).reduce((max, item) => Math.max(max, Number(item.score) || 0), 0)
    },
    personalTopScore() {
      return (this.personalRankList || []).reduce((max, item) => Math.max(max, Number(item.points) || 0), 0)
    },
    orgSummary() {
      return (this.orgRankList || []).reduce((acc, item) => {
        acc.orgCount += 1
        acc.memberCount += Number(item.memberCount) || 0
        acc.posts += Number(item.posts) || 0
        acc.replies += Number(item.replies) || 0
        return acc
      }, { orgCount: 0, memberCount: 0, posts: 0, replies: 0 })
    },
    // 组织排名分页
    rankedItems() {
      return [...this.orgRankList].sort((a, b) => a.rank - b.rank)
    },
    topThree() {
      return this.rankedItems.slice(0, 3).map((item, i) => ({
        ...item,
        ...(RANK_COLORS[i] || RANK_COLORS[0]),
      }))
    },
    restRanks() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      // 如果前三名在 page 1，跳过它们
      if (this.currentPage === 1) {
        return this.rankedItems.slice(3).slice(0, this.pageSize)
      }
      // 其他页，从第 1 条开始（跳过前三名不在本页）
      return this.rankedItems.slice(start, end)
    },
    totalCount() {
      return this.orgRankList.length
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.totalCount / this.pageSize))
    },
    timeQuickBtns() {
      return TIME_BTNS
    },
    activeTimeLabel() {
      const hit = this.timeQuickBtns.find(b => b.key === this.activeTimeKey)
      return hit ? hit.label : '自定义'
    },
    currentUserId() {
      const user = getUser()
      return user ? user.id : null
    },
    showStickyBar() {
      return this.activeMode === 'personal' && this.currentUserInfo
    },
  },
  async mounted() {
    this.initTabs()
  },
  methods: {
    normalizeFileUrl,
    /** 得分条：以榜首为 100%，保留 4% 最小可见宽度 */
    scoreWidth(score, max) {
      const value = Number(score) || 0
      const top = Number(max) || 0
      if (value <= 0 || top <= 0) return 0
      return Math.max(4, Math.min(100, Math.round((value / top) * 100)))
    },

    /**
     * 初始化 Tab：获取排行榜分类 → 构造 Tab 列表
     */
    async initTabs() {
      this.loading = true
      try {
        const resp = await this.getRequest('/common/ranking/categories')
        const categories = (resp && resp.code == 200 && Array.isArray(resp.obj)) ? resp.obj : []
        const orgTabs = categories.map(cat => ({
          key: 'category_' + cat.id,
          label: cat.categoryName,
          categoryId: cat.id,
          legacy: !!cat.legacy,
          mode: 'org',
        }))
        // 构建完整 Tab 列表
        this.tabs = [
          ...orgTabs,
          { key: 'personal', label: '个人排名', mode: 'personal' },
        ]
        // 默认选中第一个组织分类（不再依赖历史虚拟分类 id=0）
        if (this.activeCategoryId == null && orgTabs.length && orgTabs[0].categoryId != null) {
          this.activeCategoryId = orgTabs[0].categoryId
        }

        // 默认选中第一个 Tab
        if (this.tabs.length > 0) {
          this.switchTab(this.tabs[0])
        }
      } catch (e) {
        console.warn('[BBSPoints] initTabs error:', e)
        // 降级：至少显示个人排名
        this.tabs = [{ key: 'personal', label: '个人排名', mode: 'personal' }]
        this.switchTab(this.tabs[0])
      }
      this.loading = false
    },

    /**
     * 切换 Tab
     */
    switchTab(tab) {
      this.activeTabKey = tab.key
      this.activeMode = tab.mode
      this.activeCategoryId = tab.categoryId != null ? tab.categoryId : null
      this.currentPage = 1
      this.personalRankList = []
      this.currentUserInfo = null
      this.fetchData()
    },

    /**
     * 快速时间选择
     */
    selectQuickTime(btn) {
      this.activeTimeKey = btn.key
      const range = calcTimeRange(btn.key)
      this.customStartDate = range.start
      this.customEndDate = range.end
      this.currentPage = 1
      this.fetchData()
    },

    /**
     * 自定义时间范围
     */
    applyCustomRange() {
      if (this.customStartDate && this.customEndDate) {
        this.activeTimeKey = 'custom'
        this.currentPage = 1
        this.fetchData()
      }
    },

    /**
     * 根据当前模式获取数据
     */
    fetchData() {
      if (this.activeMode === 'org') {
        this.fetchOrgRank()
      } else {
        this.fetchPersonalRank()
      }
    },

    /**
     * 获取组织排名
     */
    async fetchOrgRank() {
      this.loading = true
      try {
        const range = this.activeTimeRange
        const resp = await this.postRequest('/common/ranking/org', {
          categoryId: this.activeCategoryId,
          startTime: range.start,
          endTime: range.end,
        })
        handleResponse(resp, {
          silent: true,
          onSuccess: (r) => {
            const list = this.parseRespList(r)
            this.orgRankList = list.map(item => ({
              rank: item.rankNum || 0,
              name: item.orgName || '',
              score: Number(item.avgPoints || 0),
              totalPoints: item.points || 0,
              memberCount: item.memberCount || 0,
              posts: item.posts || 0,
              replies: item.replies || 0,
              orgNo: item.orgNo || '',
            }))
          },
          onError: () => { this.orgRankList = [] },
        })
      } catch (e) {
        console.warn('[BBSPoints] fetchOrgRank', e)
        this.orgRankList = []
      }
      this.loading = false
    },

    /**
     * 获取个人排名
     */
    async fetchPersonalRank() {
      this.loading = true
      try {
        const user = getUser()
        const range = this.activeTimeRange
        const resp = await this.postRequest('/common/personalPointsRank', {
          startTime: range.start,
          endTime: range.end,
          currentUserId: user ? user.id : null,
          size: 20,
        })
        handleResponse(resp, {
          silent: true,
          onSuccess: (r) => {
            const data = r && r.obj
            this.personalRankList = (data && data.list) || []
            this.currentUserInfo = (data && data.currentUser) || null
          },
          onError: () => {
            this.personalRankList = []
            this.currentUserInfo = null
          },
        })
      } catch (e) {
        console.warn('[BBSPoints] fetchPersonalRank', e)
        this.personalRankList = []
        this.currentUserInfo = null
      }
      this.loading = false
    },

    /**
     * 解析后端响应
     */
    parseRespList(resp) {
      return (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
        : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
        : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
        : Array.isArray(resp) ? resp : []
    },

    getRankClass(rank) {
      if (rank === 1) return 'bg-rank-gold text-white shadow-sm'
      if (rank === 2) return 'bg-rank-silver text-white shadow-sm'
      if (rank === 3) return 'bg-rank-bronze text-white shadow-sm'
      return 'bg-surface-variant text-on-surface-variant'
    },
  },
}
</script>

<style scoped>
/* 领奖台：金/银/铜只做浅色渐变与描边，避免大块高饱和色 */
.podium-card {
  position: relative;
  background: #ffffff;
  border: 1px solid #e5e8ef;
  box-shadow: 0 8px 24px -22px rgba(15, 23, 42, 0.35);
}

.podium-card::before {
  content: '';
  position: absolute;
  top: -72px;
  left: 50%;
  width: 230px;
  height: 150px;
  transform: translateX(-50%);
  border-radius: 9999px;
  background: radial-gradient(closest-side, var(--podium-soft, rgba(148, 163, 184, 0.16)), transparent);
  pointer-events: none;
}

.podium-card--gold {
  --podium-soft: rgba(255, 205, 66, 0.30);
  background: linear-gradient(180deg, #fffbeb 0%, #ffffff 66%);
  border-color: #f2d27a;
  box-shadow: 0 16px 34px -24px rgba(180, 120, 0, 0.55);
}

.podium-card--silver {
  --podium-soft: rgba(148, 163, 184, 0.24);
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 66%);
  border-color: #d9e0ea;
}

.podium-card--bronze {
  --podium-soft: rgba(194, 101, 55, 0.22);
  background: linear-gradient(180deg, #fff8f1 0%, #ffffff 66%);
  border-color: #efd2b9;
}

.podium-top {
  position: absolute;
  top: 10px;
  left: 12px;
  z-index: 2;
  padding: 2px 6px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.06);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.podium-badge {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
}

.podium-badge--gold {
  background: linear-gradient(145deg, #ffefb0 0%, #ffd045 46%, #e4a514 100%);
  color: #4a3200;
}

.podium-badge--silver {
  background: linear-gradient(145deg, #f8fafc 0%, #cbd5e1 52%, #94a3b8 100%);
  color: #243143;
}

.podium-badge--bronze {
  background: linear-gradient(145deg, #ffe3cd 0%, #e8a06f 50%, #b76733 100%);
  color: #451f08;
}

.podium-crown {
  position: absolute;
  top: -14px;
  right: -8px;
  font-size: 20px;
  color: #b77900;
  transform: rotate(12deg);
  filter: drop-shadow(0 2px 3px rgba(180, 120, 0, 0.25));
}

.podium-bar {
  height: 5px;
  border-radius: 9999px;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
}

.podium-bar > span {
  display: block;
  height: 100%;
  min-width: 6px;
  border-radius: 9999px;
  opacity: 0.9;
  transition: width 0.4s ease;
}
</style>

