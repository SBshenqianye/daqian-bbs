<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">tune</span>
            排名单位配置
          </h1>
          <p class="text-body-md text-secondary mt-1">勾选参与排名的县级单位，未勾选的不参与排名展示</p>
        </div>
        <div class="flex gap-3">
          <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="selectAll">全选</button>
          <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="deselectAll">取消全选</button>
          <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- Error -->
      <div v-else-if="loadError" class="bg-error-container border border-error/20 rounded-xl p-8 text-center">
        <span class="material-symbols-outlined text-[48px] text-error/60">error_outline</span>
        <p class="text-body-md text-error mt-2">{{ loadError }}</p>
      </div>

      <!-- Org Tree -->
      <div v-else class="bg-container border border-border rounded-xl p-6">
        <div v-if="orgList.length === 0" class="text-center py-12 text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px] opacity-30">account_tree</span>
          <p class="text-body-md mt-2">暂无可配置的单位数据</p>
        </div>
        <div v-else>
          <div class="mb-4 p-3 bg-surface-container-low rounded-lg flex items-center gap-2 text-body-md text-on-surface-variant">
            <span class="material-symbols-outlined text-[18px]">info</span>
            以下为各区县供电公司，勾选后将在排名页面展示
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            <div
              v-for="org in orgList"
              :key="org.orgNo"
              class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-all"
              :class="org.isRankingSelected ? 'bg-primary/5 border-primary/30' : 'bg-surface border-outline-variant hover:border-primary/30'"
              @click="toggleOrg(org)"
            >
              <span
                class="material-symbols-outlined text-[22px]"
                :class="org.isRankingSelected ? 'text-primary' : 'text-outline'"
              >
                {{ org.isRankingSelected ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <span class="font-body-md" :class="org.isRankingSelected ? 'text-primary font-semibold' : 'text-on-surface'">
                {{ org.orgName }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BBSPointsConfig',
  data() {
    return {
      loading: false,
      saving: false,
      loadError: '',
      orgList: [],
      originalState: {}, // 用于检测是否有修改
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.loadError = ''
      try {
        const resp = await this.getRequestUrl('/common/saOrg/rankingOrgs')
        if (resp && resp.obj && Array.isArray(resp.obj)) {
          // 只保留 length(org_no)=7 的区县级单位，且 is_delete=0
          this.orgList = resp.obj
            .filter(o => o.orgNo && o.orgNo.length === 7 && o.isDelete === 0)
            .map(o => ({
              orgNo: o.orgNo,
              orgName: o.orgName,
              isRankingSelected: o.isRankingSelected === 1,
            }))
          // 保存原始状态
          this.orgList.forEach(o => { this.originalState[o.orgNo] = o.isRankingSelected })
        } else {
          this.loadError = '获取单位数据失败'
        }
      } catch (e) {
        this.loadError = '加载失败，请检查网络'
      }
      this.loading = false
    },
    toggleOrg(org) {
      org.isRankingSelected = !org.isRankingSelected
    },
    selectAll() {
      this.orgList.forEach(o => { o.isRankingSelected = true })
    },
    deselectAll() {
      this.orgList.forEach(o => { o.isRankingSelected = false })
    },
    hasChanges() {
      return this.orgList.some(o => this.originalState[o.orgNo] !== o.isRankingSelected)
    },
    handleSave() {
      if (!this.hasChanges()) {
        this.$message.info('没有需要保存的变更')
        return
      }
      this.saving = true
      const rankingMap = {}
      this.orgList.forEach(o => {
        rankingMap[o.orgNo] = o.isRankingSelected
      })
      this.postRequest('/common/saOrg/batchUpdateRanking', rankingMap).then(resp => {
        this.saving = false
        if (resp) {
          this.$message.success('保存成功')
          // 更新原始状态
          this.orgList.forEach(o => { this.originalState[o.orgNo] = o.isRankingSelected })
        }
      }).catch(() => {
        this.saving = false
        this.$message.error('保存失败')
      })
    },
  },
}
</script>
