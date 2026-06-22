<template>
  <div class="overflow-x-auto">
    <table class="w-full">
      <thead>
        <tr class="bg-surface-container-low border-b border-border">
          <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">单位名称</th>
          <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">文章数</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in tableData" :key="item.orgNo || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors cursor-pointer" @click="handleRowClick(item)">
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
</template>

<script>
export default {
  name: 'StatisticDetailPage',
  props: {
    orgNo: { type: String, default: '' },
    startTime: { type: String, default: '' },
    endTime: { type: String, default: '' }
  },
  data() {
    return { tableData: [] }
  },
  watch: {
    orgNo: { handler: 'loadList', immediate: true },
    startTime: { handler: 'loadList' },
    endTime: { handler: 'loadList' }
  },
  methods: {
    loadList() {
      this.postRequest('/admin/articleStatisticByOrg', {
        orgNo: this.orgNo || '', startTime: this.startTime || '', endTime: this.endTime || ''
      }).then(resp => {
        this.tableData = (resp && resp.obj && Array.isArray(resp.obj)) ? resp.obj : []
      }).catch(() => { this.tableData = [] })
    },
    handleRowClick(row) {
      if (row && row.orgNo) this.$emit('drillDown', row)
    }
  }
}
</script>
