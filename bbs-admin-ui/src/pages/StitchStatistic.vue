<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">bar_chart</span>
            文章统计
          </h1>
          <p class="text-body-md text-secondary mt-1">按单位查看文章发布数量统计</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <span class="font-label-md text-label-md text-on-surface-variant whitespace-nowrap">单位：</span>
            <div class="relative min-w-[180px]">
              <select v-model="selectedOrgNo" class="w-full px-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md appearance-none">
                <option value="">请选择单位</option>
                <option v-for="org in flatOrgTree" :key="org.id" :value="org.id">{{ org.label }}</option>
              </select>
              <span class="material-symbols-outlined absolute right-2 inset-y-0 flex items-center text-outline text-[18px] pointer-events-none">unfold_more</span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="font-label-md text-label-md text-on-surface-variant whitespace-nowrap">发布日期：</span>
            <input v-model="startDate" type="date" class="px-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md">
            <span class="text-on-surface-variant">至</span>
            <input v-model="endDate" type="date" class="px-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md">
          </div>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleSearch">
            <span class="material-symbols-outlined text-[18px]">search</span>
            查询
          </button>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="handleReset">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            重置
          </button>
        </div>
      </div>

      <!-- Statistics Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">单位名称</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">文章数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in tableData" :key="item.orgNo || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors cursor-pointer" @click="openDetailDialog(item)">
                <td class="p-4">
                  <span :class="item.isSelf == 1 ? 'text-primary font-medium cursor-pointer hover:underline' : 'font-body-md text-on-surface'">{{ item.orgName }}</span>
                </td>
                <td class="p-4 font-body-md text-on-surface">{{ item.articleNumber }}</td>
              </tr>
              <tr v-if="tableData.length === 0">
                <td colspan="2" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">bar_chart</span>
                    <p class="text-body-md">暂无数据</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Detail Dialog -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[8vh]" @click.self="detailVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-3xl rounded-xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant shrink-0">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">monitoring</span>
              文章统计明细
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="detailVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <StitchStatisticDetail v-if="detailVisible" :org-no="detailOrgNo" :start-time="detailStartTime" :end-time="detailEndTime" @drillDown="onDetailDrillDown" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import StitchStatisticDetail from './StitchStatisticDetail.vue'

export default {
  name: 'StitchStatistic',
  components: { StitchStatisticDetail },
  data() {
    return {
      orgTreeData: [],
      selectedOrgNo: '',
      startDate: '',
      endDate: '',
      tableData: [],
      detailVisible: false,
      detailOrgNo: '',
      detailStartTime: '',
      detailEndTime: ''
    }
  },
  computed: {
    flatOrgTree() {
      const flatten = (nodes, depth = 0) => {
        let result = []
        nodes.forEach(n => {
          result.push({ ...n, _depth: depth })
          if (n.children) result = result.concat(flatten(n.children, depth + 1))
        })
        return result
      }
      return flatten(this.orgTreeData)
    },
    currentUserOrgNo() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (!admin) return ''
        return JSON.parse(admin).orgNo || ''
      } catch (e) { return '' }
    }
  },
  mounted() {
    this.loadOrgTree()
    this.handleSearch()
  },
  methods: {
    loadOrgTree() {
      if (typeof this.getRequestUrl !== 'function') { this.orgTreeData = []; return }
      this.getRequestUrl('/common/saOrgTree').then(resp => {
        this.orgTreeData = resp && resp.obj ? this.normalizeOrgTree(resp.obj) : []
      }).catch(() => { this.orgTreeData = [] })
    },
    normalizeOrgTree(nodes) {
      if (!nodes || !Array.isArray(nodes)) return []
      return nodes.map(node => ({
        id: node.orgNo != null ? node.orgNo : node.id,
        label: node.orgName != null ? node.orgName : node.label,
        children: node.children && node.children.length ? this.normalizeOrgTree(node.children) : undefined
      }))
    },
    handleSearch() {
      if (this.startDate && this.endDate && this.endDate < this.startDate) {
        this.$message.warning('结束日期不能早于开始日期')
        return
      }
      const orgNo = this.selectedOrgNo || this.currentUserOrgNo
      this.postRequest('/admin/articleStatisticByOrg', { orgNo, startTime: this.startDate, endTime: this.endDate }).then(resp => {
        this.tableData = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj : []
      })
    },
    handleReset() {
      this.selectedOrgNo = ''
      this.startDate = ''
      this.endDate = ''
      this.tableData = []
    },
    openDetailDialog(row) {
      if (!row || !row.orgNo) return
      this.detailOrgNo = row.orgNo
      this.detailStartTime = this.startDate
      this.detailEndTime = this.endDate
      this.detailVisible = true
    },
    onDetailDrillDown(row) {
      this.openDetailDialog(row)
    }
  }
}
</script>
