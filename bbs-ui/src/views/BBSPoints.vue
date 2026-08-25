<template>
  <div class="bg-surface font-body-md text-on-surface antialiased min-h-screen">
    <!-- Fixed Header -->
    <header class="sticky top-0 z-40 bg-surface/95 backdrop-blur-sm border-b border-outline-variant/30">
      <div class="max-w-7xl mx-auto px-page-margin-desktop py-4">
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <h2 class="text-2xl font-bold text-on-surface">排名统计</h2>
        </div>

        <!-- Tab 栏 -->
        <div class="flex items-center gap-1 mt-3 overflow-x-auto -mb-4 pb-4">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-5 py-2 text-sm font-semibold rounded-lg transition-all whitespace-nowrap shrink-0"
            :class="activeTabKey === tab.key
              ? 'bg-primary text-on-primary shadow-sm'
              : 'text-on-surface-variant hover:text-primary hover:bg-primary/5'"
            @click="switchTab(tab)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- 时间筛选行（所有 Tab 共用） -->
      <div class="flex flex-wrap items-center gap-3 mb-6 p-4 bg-surface-container-low rounded-xl border border-outline-variant/50">
        <span class="text-sm font-medium text-on-surface-variant mr-1">时间：</span>
        <button
          v-for="btn in timeQuickBtns"
          :key="btn.key"
          class="px-4 py-1.5 text-sm font-medium rounded-lg transition-all"
          :class="activeTimeKey === btn.key
            ? 'bg-primary text-on-primary shadow-sm'
            : 'bg-surface border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary'"
          @click="selectQuickTime(btn)"
        >
          {{ btn.label }}
        </button>
        <span class="text-outline-variant mx-1 hidden sm:inline">|</span>
        <div class="flex items-center gap-2">
          <input
            type="date"
            v-model="customStartDate"
            class="px-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-sm text-on-surface outline-none focus:border-primary"
            @change="applyCustomRange"
          />
          <span class="text-on-surface-variant text-sm">~</span>
          <input
            type="date"
            v-model="customEndDate"
            class="px-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-sm text-on-surface outline-none focus:border-primary"
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
        <div v-if="topThree.length" class="flex flex-col md:flex-row items-stretch md:items-end gap-6 mb-4">
          <div
            v-for="podium in topThree"
            :key="'podium-'+podium.rank"
            class="flex-1 flex flex-col rounded-lg overflow-hidden transition-all duration-300 hover:shadow-md"
            :class="[
              podium.orderClass,
              podium.rank === 1 ? 'border-2 border-rank-gold shadow-sm h-[300px]'
                : podium.rank === 2 ? 'border border-outline-variant h-[270px]'
                : 'border border-outline-variant h-[250px]'
            ]"
          >
            <div class="flex flex-col items-center relative p-6 flex-1" :class="podium.cardClass">
              <div class="flex-1"></div>
              <div
                class="rounded-full flex items-center justify-center font-bold border-2 border-white shadow-sm mb-4"
                :class="[podium.rankBg, podium.rank === 1 ? 'w-16 h-16 text-3xl border-4 relative' : 'w-12 h-12 text-xl']"
              >
                {{ podium.rank }}
                <span v-if="podium.rank === 1" class="material-symbols-outlined absolute -top-4 -right-2 text-rank-gold rotate-12" style="font-variation-settings: 'FILL' 1;">workspace_premium</span>
              </div>
              <h3 class="font-bold text-center line-clamp-2 min-h-[56px] flex items-center justify-center" :class="podium.rank === 1 ? 'text-xl' : 'text-lg'">{{ podium.name }}</h3>
              <div class="w-full flex justify-between items-end pt-4">
                <div class="flex flex-col">
                  <span class="text-on-surface-variant uppercase font-bold" :class="podium.rank === 1 ? 'text-[12px]' : 'text-[10px]'">活跃度积分</span>
                  <span class="font-bold text-primary" :class="podium.rank === 1 ? 'text-4xl font-black' : 'text-2xl'">{{ podium.score }}</span>
                </div>
                <div class="text-right text-on-surface-variant" :class="podium.rank === 1 ? 'text-sm' : 'text-[11px]'">
                  <p>发帖: {{ podium.posts }}</p>
                  <p>回帖: {{ podium.replies }}</p>
                </div>
              </div>
              <div class="flex-1"></div>
            </div>
          </div>
        </div>

        <!-- 排名列表 (4+) -->
        <div v-if="restRanks.length" class="space-y-3 mt-4">
          <div
            v-for="item in restRanks"
            :key="item.orgNo"
            class="bg-surface-container-lowest border border-outline-variant rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-all"
          >
            <div class="p-5 flex items-center gap-6">
              <div class="flex-shrink-0 w-10 h-10 flex items-center justify-center bg-surface-container rounded-full text-on-surface-variant font-bold text-sm">{{ item.rank }}</div>
              <div class="flex-grow min-w-0">
                <h4 class="text-lg font-bold text-on-surface truncate">{{ item.name }}</h4>
                <div class="flex items-center gap-4 text-xs text-on-surface-variant mt-1">
                  <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">edit_note</span>发帖: {{ item.posts }}</span>
                  <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">chat_bubble</span>回帖: {{ item.replies }}</span>
                </div>
              </div>
              <div class="flex items-center gap-6 shrink-0">
                <div class="text-right">
                  <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-tight">活跃度积分</p>
                  <p class="text-2xl font-bold text-on-surface">{{ item.score }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!topThree.length && !restRanks.length" class="py-16 text-center flex flex-col items-center gap-3 text-on-surface-variant">
          <span class="material-symbols-outlined text-5xl opacity-20">leaderboard</span>
          <p class="text-body-md">暂无排名数据</p>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="mt-6 p-6 bg-surface-container-low rounded-lg flex justify-between items-center">
          <span class="text-sm text-on-surface-variant font-medium">共 {{ totalCount }} 条记录</span>
          <div class="flex gap-2">
            <button class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="currentPage <= 1" @click="currentPage--">
              <span class="material-symbols-outlined text-[20px]">chevron_left</span>
            </button>
            <button
              v-for="page in totalPages"
              :key="page"
              class="w-9 h-9 flex items-center justify-center rounded-md font-bold text-sm transition-all"
              :class="page === currentPage ? 'bg-primary text-white shadow-sm' : 'border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary'"
              @click="currentPage = page"
            >
              {{ page }}
            </button>
            <button class="w-9 h-9 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="currentPage >= totalPages" @click="currentPage++">
              <span class="material-symbols-outlined text-[20px]">chevron_right</span>
            </button>
          </div>
        </div>
      </template>

      <!-- 个人排名列表 -->
      <template v-if="!loading && activeMode === 'personal'">
        <div v-if="personalRankList.length" class="space-y-2">
          <div
            v-for="(item, i) in personalRankList"
            :key="item.userId"
            class="flex items-center gap-4 p-4 bg-surface-container-lowest border border-outline-variant rounded-lg hover:shadow-sm transition-all"
            :class="item.userId === currentUserId ? 'border-l-4 border-l-primary bg-primary/[0.04]' : ''"
          >
            <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-sm" :class="getRankClass(i + 1)">
              {{ i + 1 }}
            </div>
            <img
              :src="normalizeFileUrl(item.portrait) || require('@/assets/portrait.png')"
              class="w-10 h-10 rounded-full object-cover border border-outline-variant shrink-0"
              alt="头像"
              @error="$event.target.src = require('@/assets/portrait.png')"
            />
            <div class="flex-1 min-w-0">
              <p class="font-bold text-on-surface truncate">{{ item.nickName }}</p>
              <p class="text-xs text-on-surface-variant truncate">{{ item.orgName }}</p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-xs text-on-surface-variant">发帖 {{ item.posts }} / 回帖 {{ item.replies }}</p>
              <p class="text-xl font-bold text-primary">{{ item.points }} 分</p>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!personalRankList.length" class="py-16 text-center flex flex-col items-center gap-3 text-on-surface-variant">
          <span class="material-symbols-outlined text-5xl opacity-20">person_off</span>
          <p class="text-body-md">暂无个人排名数据</p>
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
      <div class="max-w-7xl mx-auto px-page-margin-desktop py-3 flex items-center gap-4">
        <img
          :src="normalizeFileUrl(currentUserInfo.portrait) || require('@/assets/portrait.png')"
          class="w-11 h-11 rounded-full object-cover border-2 border-primary shrink-0"
          alt="头像"
          @error="$event.target.src = require('@/assets/portrait.png')"
        />
        <div class="flex-1 min-w-0">
          <p class="font-bold text-on-surface text-sm truncate">{{ currentUserInfo.nickName }}</p>
          <p class="text-xs text-on-surface-variant truncate">{{ currentUserInfo.orgName }}</p>
        </div>
        <div class="text-right shrink-0">
          <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-tight">我的积分</p>
          <p class="text-xl font-bold text-primary">{{ currentUserInfo.points }}</p>
        </div>
        <div class="text-right shrink-0 pl-4 border-l border-outline-variant/50">
          <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-tight">排名</p>
          <p class="text-xl font-bold text-on-surface">#{{ currentUserInfo.rankNum }}</p>
        </div>
      </div>
    </div>
    <!-- /固定底部栏 -->
  </div>
</template>

<script>
import { getUser } from '@/utils/auth'
import { normalizeFileUrl } from '@/utils/utils'

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
  data() {
    return {
      loading: false,
      // Tab
      tabs: [],
      activeTabKey: '',
      activeMode: 'org',   // 'org' | 'personal'
      activeOrgNo: '',     // 当前选中 Tab 的 orgNo（仅 org 模式）
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
     * 初始化 Tab：获取组织树 → 构造 Tab 列表
     */
    async initTabs() {
      this.loading = true
      try {
        // 获取组织树
        const tree = await this.getRequest('/common/saOrgTree')
        const rootNode = (tree && tree.obj && tree.obj[0]) || null
        const orgTabs = []
        if (rootNode && rootNode.children) {
          // 该二级组织及其全部下级均未参与排名时，不生成 Tab
          rootNode.children.forEach(child => {
            if (!this.hasRankingOrg(child)) return
            orgTabs.push({
              key: 'org_' + child.id,
              label: child.label,
              orgNo: child.id,
              mode: 'org',
            })
          })
        }
        // 构建完整 Tab 列表
        this.tabs = [
          ...orgTabs,
          { key: 'personal', label: '个人排名', mode: 'personal' },
        ]

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
     * 判断组织节点自身或任一子级是否参与排名
     */
    hasRankingOrg(node) {
      if (node.isRankingSelected === 1 || node.isRankingSelected === true) return true
      if (node.children && node.children.length) {
        return node.children.some(child => this.hasRankingOrg(child))
      }
      return false
    },

    /**
     * 切换 Tab
     */
    switchTab(tab) {
      this.activeTabKey = tab.key
      this.activeMode = tab.mode
      this.activeOrgNo = tab.orgNo || ''
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
      if (!this.activeOrgNo) {
        this.orgRankList = []
        return
      }
      this.loading = true
      try {
        const range = this.activeTimeRange
        const resp = await this.postRequest('/common/pointsRank', {
          rankType: '01',
          orgNo: this.activeOrgNo,
          startTime: range.start,
          endTime: range.end,
        })
        const list = this.parseRespList(resp)
        this.orgRankList = list.map(item => ({
          rank: item.rankNum || 0,
          name: item.orgName || '',
          score: item.points || 0,
          posts: item.posts || 0,
          replies: item.replies || 0,
          orgNo: item.orgNo || '',
        })).filter(item => item.orgNo !== this.activeOrgNo) // 点4：只展示第三级，排除本级单位
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
        const data = resp && resp.obj
        this.personalRankList = (data && data.list) || []
        this.currentUserInfo = (data && data.currentUser) || null
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

