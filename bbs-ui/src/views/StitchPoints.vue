<template>
  <div class="bg-surface font-body-md text-on-surface antialiased min-h-screen">
    <!-- Fixed Header -->
    <header class="sticky top-0 z-40 sticky-header">
      <div class="max-w-7xl mx-auto px-page-margin-desktop py-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div class="flex items-center gap-6">
          <h2 class="text-2xl font-bold text-on-surface">排名统计</h2>
          <div class="flex gap-2">
            <button class="flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-primary hover:bg-primary/5 rounded-md transition-colors" @click="toggleAll(true)">
              <span class="material-symbols-outlined text-[18px]">expand_more</span> 一键展开
            </button>
            <button class="flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-on-surface-variant hover:bg-surface-variant/50 rounded-md transition-colors" @click="toggleAll(false)">
              <span class="material-symbols-outlined text-[18px]">expand_less</span> 一键关闭
            </button>
          </div>
        </div>
        <div class="inline-flex p-1 bg-surface-container-high rounded-lg">
          <button
            class="px-6 py-2 text-sm font-semibold transition-all rounded-md"
            :class="activeTab === 'month' ? 'bg-container text-primary shadow-sm' : 'text-on-surface-variant hover:text-primary'"
            @click="switchTab('month')"
          >
            当月排名
          </button>
          <button
            class="px-6 py-2 text-sm font-semibold transition-all rounded-md"
            :class="activeTab === 'total' ? 'bg-container text-primary shadow-sm' : 'text-on-surface-variant hover:text-primary'"
            @click="switchTab('total')"
          >
            累计排名
          </button>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-page-margin-desktop py-8">
      <!-- Top 3 Podium Section -->
      <div class="flex flex-col md:flex-row items-stretch md:items-end gap-6 mb-4">
        <!-- Rank 2 -->
        <div
          v-for="podium in topThree"
          :key="podium.rank"
          class="flex-1 flex flex-col rounded-lg overflow-hidden transition-all duration-300 hover:shadow-md"
          :class="[
            podium.orderClass,
            podium.rank === 1 ? 'border-2 border-rank-gold shadow-sm md:-translate-y-2' : 'border border-outline-variant',
            { 'expanded': podium.expanded }
          ]"
        >
          <div
            class="flex flex-col items-center cursor-pointer relative"
            :class="[podium.cardClass, podium.rank === 1 ? 'p-8 min-h-[260px]' : 'p-6 min-h-[220px]']"
            @click="toggleExpand(podium.rank)"
          >
            <div class="absolute top-3 right-3 flex flex-col items-end gap-2">
              <span v-if="podium.isSelf" class="bg-primary text-white text-[10px] px-2 py-0.5 rounded-full font-bold">当前单位</span>
              <span class="material-symbols-outlined text-on-surface-variant expand-icon transition-transform">expand_more</span>
            </div>
            <div
              class="rounded-full flex items-center justify-center font-bold border-2 border-white shadow-sm mb-4"
              :class="[podium.rankBg, podium.rank === 1 ? 'w-16 h-16 text-3xl border-4 relative' : 'w-12 h-12 text-xl']"
            >
              {{ podium.rank }}
              <span v-if="podium.rank === 1" class="material-symbols-outlined absolute -top-4 -right-2 text-rank-gold rotate-12" style="font-variation-settings: 'FILL' 1;">workspace_premium</span>
            </div>
            <h3 class="font-bold text-center mb-1 line-clamp-2" :class="podium.rank === 1 ? 'text-xl' : 'text-lg'">{{ podium.name }}</h3>
            <div class="mt-auto w-full flex justify-between items-end">
              <div class="flex flex-col">
                <span class="text-on-surface-variant uppercase font-bold" :class="podium.rank === 1 ? 'text-[12px]' : 'text-[10px]'">
                  {{ activeTab === 'month' ? '当月活跃度积分' : '累计活跃度积分' }}
                </span>
                <span class="font-bold text-primary" :class="podium.rank === 1 ? 'text-4xl font-black' : 'text-2xl'">{{ podium.score }}</span>
              </div>
              <div class="text-right text-on-surface-variant" :class="podium.rank === 1 ? 'text-sm' : 'text-[11px]'">
                <p>{{ activeTab === 'month' ? '发帖' : '累计发帖' }}: {{ podium.posts }}</p>
                <p>{{ activeTab === 'month' ? '回复' : '累计回复' }}: {{ podium.replies }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Expanded Podium Content (between podium and main list) -->
      <div v-if="expandedPodiums.length" class="space-y-4 mb-8">
        <div v-for="podium in expandedPodiums" :key="'exp-'+podium.rank"
          class="bg-surface-container-low border border-outline-variant rounded-lg overflow-hidden shadow-sm"
        >
          <div class="px-6 py-3 border-b border-outline-variant flex items-center gap-3 text-sm font-medium text-on-surface">
            <span class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white shrink-0" :class="podium.rankBg">
              {{ podium.rank }}
            </span>
            {{ podium.name }} - 下属单位
          </div>
          <div v-if="podium.children && podium.children.length > 0" class="p-6">
            <table class="w-full text-left border-separate border-spacing-y-2">
              <thead>
                <tr class="text-on-surface-variant text-[11px] uppercase font-bold">
                  <th class="pb-2 px-4">单位/科室</th>
                  <th class="pb-2 px-4">{{ activeTab === 'month' ? '发帖数/累计发帖数' : '累计发帖数' }}</th>
                  <th class="pb-2 px-4">{{ activeTab === 'month' ? '当月回帖数/累计回帖数' : '累计回帖数' }}</th>
                  <th class="pb-2 px-4">{{ activeTab === 'month' ? '当月活跃度积分/累计活跃度积分' : '累计活跃度积分' }}</th>
                  <th class="pb-2 px-4 text-right">排名</th>
                </tr>
              </thead>
              <tbody class="text-sm">
                <tr v-for="(child, ci) in podium.children" :key="ci" class="bg-surface-container-lowest rounded-md shadow-sm">
                  <td class="py-3 px-4 rounded-l-md font-medium">{{ child.name }}</td>
                  <td class="py-3 px-4">{{ child.posts }}/{{ child.totalPosts }}</td>
                  <td class="py-3 px-4">{{ child.replies }}/{{ child.totalReplies }}</td>
                  <td class="py-3 px-4 text-primary font-bold">{{ child.score }}/{{ child.totalScore }}</td>
                  <td class="py-3 px-4 text-right rounded-r-md">{{ ci + 1 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="p-8 text-center text-on-surface-variant flex flex-col items-center gap-2">
            <span class="material-symbols-outlined text-4xl opacity-20">inventory_2</span>
            <p class="text-sm">暂无下属单位数据</p>
          </div>
        </div>
      </div>

      <!-- Main List Section (Ranks 4+) -->
      <div class="flex flex-col gap-4">
        <div
          v-for="item in restRanks"
          :key="item.rank"
          class="bg-surface-container-lowest border border-outline-variant rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-all"
          :class="{ 'expanded': item.expanded }"
        >
          <div class="p-5 flex items-center gap-6 cursor-pointer" @click="toggleExpand(item.rank)">
            <div class="flex-shrink-0 w-10 h-10 flex items-center justify-center bg-surface-container rounded-full text-on-surface-variant font-bold">{{ item.rank }}</div>
            <div class="flex-grow">
              <h4 class="text-lg font-bold text-on-surface hover:text-primary transition-colors">{{ item.name }}</h4>
              <div class="flex items-center gap-4 text-xs text-on-surface-variant mt-1">
                <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">edit_note</span>{{ activeTab === 'month' ? '发帖' : '累计发帖' }}: {{ item.posts }}</span>
                <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">chat_bubble</span>{{ activeTab === 'month' ? '回复' : '累计回复' }}: {{ item.replies }}</span>
              </div>
            </div>
            <div class="flex items-center gap-6">
              <div class="text-right">
                <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-tight">{{ activeTab === 'month' ? '当月活跃度积分' : '累计活跃度积分' }}</p>
                <p class="text-2xl font-bold text-on-surface">{{ item.score }}</p>
              </div>
              <span class="material-symbols-outlined text-on-surface-variant expand-icon transition-transform">expand_more</span>
            </div>
          </div>
          <div class="expand-content bg-surface-container-low border-t border-outline-variant">
            <div v-if="item.children && item.children.length > 0" class="p-6">
              <table class="w-full text-left border-separate border-spacing-y-2">
                <thead>
                  <tr class="text-on-surface-variant text-[11px] uppercase font-bold">
                    <th class="pb-2 px-4">单位/科室</th>
                    <th class="pb-2 px-4">{{ activeTab === 'month' ? '发帖数/累计发帖数' : '累计发帖数' }}</th>
                    <th class="pb-2 px-4">{{ activeTab === 'month' ? '当月回帖数/累计回帖数' : '累计回帖数' }}</th>
                    <th class="pb-2 px-4">{{ activeTab === 'month' ? '当月活跃度积分/累计活跃度积分' : '累计活跃度积分' }}</th>
                    <th class="pb-2 px-4 text-right">排名</th>
                  </tr>
                </thead>
                <tbody class="text-sm">
                  <tr v-for="(child, ci) in item.children" :key="ci" class="bg-surface-container-lowest rounded-md shadow-sm">
                    <td class="py-3 px-4 rounded-l-md font-medium">{{ child.name }}</td>
                    <td class="py-3 px-4">{{ child.posts }}/{{ child.totalPosts }}</td>
                    <td class="py-3 px-4">{{ child.replies }}/{{ child.totalReplies }}</td>
                    <td class="py-3 px-4 text-primary font-bold">{{ child.score }}/{{ child.totalScore }}</td>
                    <td class="py-3 px-4 text-right rounded-r-md">{{ ci + 1 }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="p-8 text-center text-on-surface-variant flex flex-col items-center gap-2">
              <span class="material-symbols-outlined text-4xl opacity-20">inventory_2</span>
              <p class="text-sm">暂无下属单位数据</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="mt-4 p-6 bg-surface-container-low rounded-lg flex justify-between items-center">
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
    </main>
  </div>
</template>

<script>
const RANK_COLORS = [
  { rankBg: 'bg-rank-gold', cardClass: 'rank-card-1', orderClass: 'order-1 md:order-2' },
  { rankBg: 'bg-rank-silver', cardClass: 'rank-card-2', orderClass: 'order-2 md:order-1' },
  { rankBg: 'bg-rank-bronze', cardClass: 'rank-card-3', orderClass: 'order-3' },
]

export default {
  name: 'StitchPoints',
  data() {
    return {
      activeTab: 'month',
      currentPage: 1,
      loading: false,
      // Display-ready data, mutated in place by toggle expand
      displayItems: [],
    }
  },
  computed: {
    rankedItems() {
      return [...this.displayItems].sort((a, b) => a.rank - b.rank)
    },
    topThree() {
      return this.rankedItems.slice(0, 3).map((item, i) => ({
        ...item,
        ...(RANK_COLORS[i] || RANK_COLORS[0]),
      }))
    },
    expandedPodiums() {
      return this.topThree.filter(p => p.expanded)
    },
    restRanks() {
      return this.rankedItems.slice(3).map(item => ({
        rank: item.rank,
        name: item.name,
        score: item.score,
        posts: item.posts,
        replies: item.replies,
        isSelf: item.isSelf,
        orgNo: item.orgNo,
        expanded: item.expanded,
        children: item.children,
      }))
    },
    totalCount() {
      return this.displayItems.length
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.totalCount / 10))
    },
  },
  mounted() {
    this.fetchRank()
  },
  methods: {
    switchTab(tab) {
      this.activeTab = tab
      this.currentPage = 1
      this.fetchRank()
    },
    toggleAll(expand) {
      this.displayItems.forEach(item => {
        item.expanded = expand
        if (expand && item.isSelf && item.orgNo && !item.children.length) {
          this.fetchChildren(item)
        }
      })
    },
    toggleExpand(rank) {
      const item = this.displayItems.find(i => i.rank === rank)
      if (!item) return
      item.expanded = !item.expanded
      if (item.expanded && item.isSelf && item.orgNo && !item.children.length) {
        this.fetchChildren(item)
      }
    },
    fetchRank() {
      this.loading = true
      this.postRequest('/common/pointsRank', {
        rankType: this.activeTab === 'month' ? '01' : '02',
        orgNo: '',
      }).then(resp => {
        this.loading = false
        const list = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
          : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
          : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
          : Array.isArray(resp) ? resp : []
        this.displayItems = list.map(item => ({
          rank: item.rankNum || 0,
          name: item.orgName || '',
          score: item.points || 0,
          posts: item.posts || 0,
          replies: item.replies || 0,
          isSelf: item.isSelf === 1,
          orgNo: item.orgNo || '',
          expanded: false,
          children: [],
        }))
      }).catch(() => {
        this.loading = false
        this.displayItems = []
      })
    },
    fetchChildren(item) {
      if (!item.orgNo) return
      this.postRequest('/common/pointsRank', {
        rankType: this.activeTab === 'month' ? '01' : '02',
        orgNo: item.orgNo,
      }).then(resp => {
        const list = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
          : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
          : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
          : Array.isArray(resp) ? resp : []
        item.children = list.map(child => ({
          name: child.orgName || '',
          posts: child.posts || 0,
          totalPosts: child.posts || 0,
          replies: child.replies || 0,
          totalReplies: child.replies || 0,
          score: child.points || 0,
          totalScore: child.points || 0,
        }))
      }).catch(() => {
        item.children = []
      })
    },
  },
}
</script>
