<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-rank-gold">leaderboard</span>
            积分排名
          </h1>
          <p class="text-body-md text-secondary mt-1">查看单位活跃度排名</p>
        </div>
        <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-all font-label-md text-label-md" @click="$router.push('/unitManage')">
          <span class="material-symbols-outlined text-[18px]">tune</span>
          配置排名单位
        </button>
      </div>

      <!-- Tabs -->
      <div class="bg-container border border-border rounded-xl overflow-hidden mb-6">
        <div class="flex border-b border-border">
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === '01' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="switchTab('01')">
            当月排名
            <span v-if="activeTab === '01'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative" :class="activeTab === '02' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'" @click="switchTab('02')">
            累计排名
            <span v-if="activeTab === '02'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-12">
          <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>

        <!-- Rank List -->
        <div v-else class="p-card-padding">
          <div v-if="rankList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">leaderboard</span>
            <p class="text-body-md">暂无排名数据</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="(item, index) in rankList" :key="item.orgNo || index" class="flex items-center gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/50 hover:border-primary/30 transition-all">
              <div class="flex-shrink-0 w-9 h-9 flex items-center justify-center rounded-full font-bold text-[14px]" :class="getRankClass(index + 1)">
                {{ index + 1 }}
              </div>
              <div class="flex-1 min-w-0">
                <span class="font-headline-sm text-headline-sm text-on-surface" :class="{ 'text-primary cursor-pointer hover:underline': item.isSelf === 1 }" @click="onRowClick(item)">{{ item.orgName }}</span>
              </div>
              <div class="flex items-center gap-6 text-body-md text-on-surface-variant">
                <span class="flex items-center gap-1">
                  <span class="material-symbols-outlined text-[16px]">edit_note</span>
                  {{ activeTab === '01' ? '发帖' : '累计发帖' }}: {{ item.posts }}
                </span>
                <span class="flex items-center gap-1">
                  <span class="material-symbols-outlined text-[16px]">chat_bubble</span>
                  {{ activeTab === '01' ? '回帖' : '累计回帖' }}: {{ item.replies }}
                </span>
              </div>
              <div class="text-right">
                <p class="text-[11px] text-on-surface-variant font-medium">{{ activeTab === '01' ? '当月活跃度' : '累计活跃度' }}</p>
                <p class="text-xl font-bold text-primary">{{ item.points }}</p>
              </div>
              <span class="text-label-md text-on-surface-variant bg-surface-variant px-2 py-0.5 rounded-full">#{{ item.rankNum }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail Dialog -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[8vh]" @click.self="detailVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-3xl rounded-xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant shrink-0">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">leaderboard</span>
              详情 - {{ detailOrgName }}
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="detailVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <div v-if="detailLoading" class="flex items-center justify-center py-12">
              <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
            </div>
            <div v-else>
              <div v-if="detailList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined text-[48px] opacity-20">leaderboard</span>
                <p class="text-body-md">暂无数据</p>
              </div>
              <div v-else class="space-y-3">
                <div v-for="(item, index) in detailList" :key="item.orgNo || index" class="flex items-center gap-4 p-4 bg-surface-container-low rounded-lg border border-outline-variant/50">
                  <div class="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full font-bold text-[13px] bg-surface-variant text-on-surface-variant">{{ index + 1 }}</div>
                  <div class="flex-1">
                    <span class="font-headline-sm text-headline-sm text-on-surface" :class="{ 'text-primary cursor-pointer': item.isSelf === 1 }" @click="onDetailRowClick(item)">{{ item.orgName }}</span>
                  </div>
                  <div class="flex items-center gap-4 text-body-md text-on-surface-variant">
                    <span>{{ detailRankType === '01' ? '发帖' : '累计发帖' }}: {{ item.posts }}</span>
                    <span>{{ detailRankType === '01' ? '回帖' : '累计回帖' }}: {{ item.replies }}</span>
                  </div>
                  <div class="text-right">
                    <p class="font-bold text-primary text-lg">{{ item.points }}</p>
                  </div>
                  <span class="text-label-md text-on-surface-variant">#{{ item.rankNum }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest shrink-0">
            <button class="px-6 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md" @click="detailVisible = false">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BBSPoints',
  data() {
    return {
      activeTab: '01',
      rankList: [],
      loading: false,
      detailVisible: false,
      detailOrgNo: '',
      detailOrgName: '',
      detailRankType: '01',
      detailList: [],
      detailLoading: false
    }
  },
  mounted() { this.fetchRank() },
  methods: {
    switchTab(tab) {
      this.activeTab = tab
      this.fetchRank()
    },
    fetchRank() {
      this.loading = true
      this.postRequest('/common/pointsRank', { rankType: this.activeTab, orgNo: '' }).then(resp => {
        this.loading = false
        const list = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
          : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
          : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
          : Array.isArray(resp) ? resp : []
        this.rankList = list
      }).catch(err => { console.warn('[AdminBBSPoints] load', err); this.loading = false; this.rankList = [] })
    },
    onRowClick(row) {
      if (row.isSelf === 1 && row.orgNo) {
        this.detailOrgNo = row.orgNo
        this.detailOrgName = row.orgName || ''
        this.detailRankType = this.activeTab
        this.detailVisible = true
        this.$nextTick(() => this.fetchDetail())
      }
    },
    fetchDetail() {
      if (!this.detailOrgNo) return
      this.detailLoading = true
      this.postRequest('/common/pointsRank', { rankType: this.detailRankType, orgNo: this.detailOrgNo }).then(resp => {
        this.detailLoading = false
        const list = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj
          : (resp && resp.list && Array.isArray(resp.list)) ? resp.list
          : (resp && resp.data && Array.isArray(resp.data)) ? resp.data
          : Array.isArray(resp) ? resp : []
        this.detailList = list
      }).catch(err => { console.warn('[AdminBBSPoints] fetchDetail', err); this.detailLoading = false; this.detailList = [] })
    },
    onDetailRowClick(row) {
      if (row.isSelf === 1 && row.orgNo) {
        this.detailOrgNo = row.orgNo
        this.detailOrgName = row.orgName || ''
        this.fetchDetail()
      }
    },
    getRankClass(rank) {
      if (rank === 1) return 'bg-rank-gold text-white shadow-sm'
      if (rank === 2) return 'bg-rank-silver text-white shadow-sm'
      if (rank === 3) return 'bg-rank-bronze text-white shadow-sm'
      return 'bg-surface-variant text-on-surface-variant'
    }
  }
}
</script>
