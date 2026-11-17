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
            class="px-2 py-1 bg-surface border border-outline-variant rounded-md text-[11.5px] text-on-surface outline-none focus:border-primary"
            @change="applyCustomRange"
          />
          <span class="text-on-surface-variant text-[11px]">~</span>
          <input
            type="date"
            v-model="customEndDate"
            class="px-2 py-1 bg-surface border border-outline-variant rounded-md text-[11.5px] text-on-surface outline-none focus:border-primary"
            @change="applyCustomRange"
          />
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- 组织排名列表 -->
      <template v-if="!loading && activeMode === 'org'">
        <!-- Top 3 领奖台 -->
        <div v-if="topThree.length" class="flex flex-col md:flex-row items-stretch md:items-end gap-3 mb-3">
          <div
            v-for="podium in topThree"
            :key="'podium-'+podium.rank"
            class="flex-1 flex flex-col rounded-xl overflow-hidden transition-all duration-300 hover:shadow-md"
            :class="[
              podium.orderClass,
              podium.rank === 1 ? 'border-2 border-rank-gold shadow-sm h-[240px]'
                : podium.rank === 2 ? 'border border-outline-variant h-[218px]'
                : 'border border-outline-variant h-[204px]'
            ]"
          >
            <div class="flex flex-col items-center relative p-4 flex-1" :class="podium.cardClass">
              <div class="flex-1"></div>
              <div
                class="rounded-full flex items-center justify-center font-bold border-2 border-white shadow-sm mb-2.5"
                :class="[podium.rankBg, podium.rank === 1 ? 'w-12 h-12 text-xl border-4 relative' : 'w-9 h-9 text-sm']"
              >
                {{ podium.rank }}
                <span v-if="podium.rank === 1" class="material-symbols-outlined absolute -top-4 -right-2 text-amber-500 rotate-12 text-[20px]" style="font-variation-settings: 'FILL' 1;">workspace_premium</span>
              </div>
              <h3 class="font-semibold text-center line-clamp-2 min-h-[44px] flex items-center justify-center px-1" :class="podium.rank === 1 ? 'text-[15px]' : 'text-[13.5px]'">{{ podium.name }}</h3>
              <div class="w-full flex justify-between items-end pt-3">
                <div class="flex flex-col min-w-0">
                  <span class="text-on-surface-variant text-[10px]">人均得分</span>
                  <span class="font-bold text-primary tabnum" :class="podium.rank === 1 ? 'text-[26px] leading-8' : 'text-[19px]'">{{ podium.score }}</span>
                  <span class="text-[10px] text-outline mt-0.5 truncate">总分 {{ podium.totalPoints }} · {{ podium.memberCount }} 人</span>
                </div>
                <div class="text-right text-on-surface-variant text-[10.5px] shrink-0">
                  <p>发帖 {{ podium.posts }}</p>
                  <p>回帖 {{ podium.replies }}</p>
                </div>
              </div>
              <div class="flex-1"></div>
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
            <div class="p-3.5 flex items-center gap-3">
              <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center bg-surface-container rounded-lg text-on-surface-variant font-bold text-[12px] tabnum">{{ item.rank }}</div>
              <div class="flex-grow min-w-0">
                <h4 class="text-[14px] font-semibold text-on-surface truncate">{{ item.name }}</h4>
                <div class="ui-meta mt-1">
                  <span class="ui-meta-item"><span class="material-symbols-outlined">group</span>{{ item.memberCount }} 人</span>
                  <span class="ui-meta-item"><span class="material-symbols-outlined">edit_note</span>发帖 {{ item.posts }}</span>
                  <span class="ui-meta-item"><span class="material-symbols-outlined">chat_bubble</span>回帖 {{ item.replies }}</span>
                </div>
              </div>
              <div class="text-right shrink-0">
                <p class="text-[10px] text-on-surface-variant">人均得分</p>
                <p class="text-[19px] font-bold text-on-surface tabnum leading-6">{{ item.score }}</p>
                <p class="text-[10px] text-outline">总分 {{ item.totalPoints }}</p>
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
          <div
            v-for="(item, i) in personalRankList"
            :key="item.userId"
            class="ui-card flex items-center gap-3 p-3"
            :class="item.userId === currentUserId ? 'border-l-[3px] border-l-primary bg-primary/5' : ''"
          >
            <div class="flex-shrink-0 w-7 h-7 flex items-center justify-center rounded-md font-bold text-[11px]" :class="getRankClass(i + 1)">
              {{ i + 1 }}
            </div>
            <img
              :src="normalizeFileUrl(item.portrait) || require('@/assets/portrait.png')"
              class="w-9 h-9 rounded-full object-cover border border-outline-variant shrink-0"
              alt="头像"
              @error="$event.target.src = require('@/assets/portrait.png')"
            />
            <div class="flex-1 min-w-0">
              <p class="text-[13px] font-semibold text-on-surface truncate">{{ item.nickName }}</p>
              <p class="text-[10.5px] text-on-surface-variant truncate">{{ item.orgName }}</p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-[10.5px] text-outline">发帖 {{ item.posts }} · 回帖 {{ item.replies }}</p>
              <p class="text-[16px] font-bold text-primary tabnum leading-6">{{ item.points }} 分</p>
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
        <img
          :src="normalizeFileUrl(currentUserInfo.portrait) || require('@/assets/portrait.png')"
          class="w-9 h-9 rounded-full object-cover border-2 border-primary shrink-0"
          alt="头像"
          @error="$event.target.src = require('@/assets/portrait.png')"
        />
        <div class="flex-1 min-w-0">
          <p class="font-semibold text-on-surface text-[12.5px] truncate">{{ currentUserInfo.nickName }}</p>
          <p class="text-[10.5px] text-on-surface-variant truncate">{{ currentUserInfo.orgName }}</p>
        </div>
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
import { handleResponse } from '../../../shared/feedback'

const RANK_COLORS = [
  { rankBg: 'bg-rank-gold', cardClass: 'rank-card-1', orderClass: 'order-1 md:order-2' },
  { rankBg: 'bg-rank-silver', cardClass: 'rank-card-2', orderClass: 'order-2 md:order-1' },
  { rankBg: 'bg-rank-bronze', cardClass: 'rank-card-3', orderClass: 'order-3' },
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
  components: { PageHeader },
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
      const hit = TIME_BTNS.find(b => b.key === this.activeTimeKey)
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
  mounted() {
    this.initTabs()
  },
  methods: {
    normalizeFileUrl,

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

