<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600">feedback</span>
            举报管理
          </h1>
          <p class="text-body-md text-secondary mt-1">审核用户举报内容（同一内容的重复举报已折叠）</p>
        </div>
      </div>

      <!-- Filter -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center gap-3">
          <select v-model="filterStatus" class="px-3 py-2 bg-surface border border-outline-variant rounded-lg text-body-sm focus:border-primary outline-none">
            <option value="">全部状态</option>
            <option value="pending">待审核</option>
            <option value="confirmed">已确认</option>
            <option value="rejected">已驳回</option>
          </select>
          <button class="px-3 py-2 bg-primary-container text-on-primary-container rounded-lg hover:opacity-90 text-body-sm" @click="loadList">查询</button>
        </div>
      </div>

      <!-- List：按举报目标分组，折叠重复举报 -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无举报记录</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">ID</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">举报人</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">目标</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">原因</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <template v-for="group in list">
                <tr
                  v-for="item in group.members"
                  :key="item.id"
                  v-show="isGroupExpanded(group) || item.id === group.representative.id"
                  class="hover:bg-surface-container-low/50"
                >
                  <td class="px-4 py-3 text-body-sm">{{ item.id }}</td>
                  <td class="px-4 py-3 text-body-sm">{{ item.reporterId }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <div class="flex items-center gap-2">
                      <span>{{ getTargetTypeLabel(item.targetType) }}#{{ item.targetId }}</span>
                      <!-- 折叠态下在代表行上显示重复数徽标 + 展开按钮 -->
                      <button
                        v-if="item.id === group.representative.id && group.totalCount > 1 && !isGroupExpanded(group)"
                        class="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-[11px] font-medium hover:bg-blue-100"
                        @click="toggleGroup(group)"
                      >
                        +{{ group.totalCount - 1 }} 条重复举报 ▼
                      </button>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-body-sm max-w-[200px] truncate" :title="item.reason">{{ item.reason || '-' }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <span :class="{
                      'px-2 py-0.5 rounded text-[12px] font-medium': true,
                      'bg-yellow-100 text-yellow-800': item.status === 'pending',
                      'bg-green-100 text-green-800': item.status === 'confirmed',
                      'bg-red-100 text-red-800': item.status === 'rejected'
                    }">{{ getStatusLabel(item.status) }}</span>
                  </td>
                  <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.createTime }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <div v-if="item.status === 'pending'" class="flex gap-1">
                      <button class="px-2 py-1 bg-green-50 text-green-700 rounded text-[12px] hover:bg-green-100" @click="handleReview(item, 'confirmed')">确认</button>
                      <button class="px-2 py-1 bg-red-50 text-red-700 rounded text-[12px] hover:bg-red-100" @click="handleReview(item, 'rejected')">驳回</button>
                    </div>
                    <span v-else class="text-on-surface-variant text-[12px]">{{ item.reviewRemark || '已处理' }}</span>
                  </td>
                </tr>
                <!-- 展开态：收起按钮行 -->
                <tr v-if="isGroupExpanded(group) && group.totalCount > 1" :key="'collapse-' + group.targetType + '-' + group.targetId">
                  <td colspan="7" class="py-2 text-center bg-surface-container-low/40">
                    <button class="text-blue-600 hover:underline text-[12px]" @click="toggleGroup(group)">▲ 收起其余 {{ group.totalCount - 1 }} 条重复举报</button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <div class="flex justify-end mt-4" v-if="total > pageSize">
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
          <span class="px-3 py-1 text-body-sm text-on-surface-variant">{{ currentPage }}/{{ Math.ceil(total / pageSize) }}</span>
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ReportPage',
  data() {
    return {
      loading: false,
      list: [],           // 分组数据：[{ representative, members, totalCount, targetType, targetId }]
      total: 0,           // 组数（分页单位是组，不是记录）
      currentPage: 1,
      pageSize: 10,
      filterStatus: '',
      expandedKeys: {}    // 展开状态的组 key -> true
    }
  },
  mounted() { this.loadList() },
  methods: {
    getStatusLabel(s) { return { pending: '待审核', confirmed: '已确认', rejected: '已驳回' }[s] || s },
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    groupKey(group) { return group.targetType + '-' + group.targetId },
    isGroupExpanded(group) { return !!this.expandedKeys[this.groupKey(group)] },
    toggleGroup(group) {
      const key = this.groupKey(group)
      this.$set(this.expandedKeys, key, !this.expandedKeys[key])
    },
    async loadList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.filterStatus) params.status = this.filterStatus
        const res = await this.postRequest('/admin/report/listGrouped', params)
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    handleReview(item, status) {
      const label = status === 'confirmed' ? '确认' : '驳回'
      // 全组计分提示：确认任一条 = 同内容全部待审举报一并确认，所有举报人各 +2 分
      const group = this.list.find(g => g.members.some(m => m.id === item.id))
      const title = status === 'confirmed' && group && group.totalCount > 1
        ? `确定确认该举报？该内容共 ${group.totalCount} 条举报，将一并确认，全部举报人各 +2 分`
        : `确定${label}该举报？`
      this.$prompt('审核备注（可选）', title, { type: status === 'confirmed' ? 'success' : 'warning' })
        .then(({ value }) => this.doReview(item.id, status, value))
        .catch(() => {})
    },
    async doReview(reportId, status, remark) {
      try {
        const res = await this.postRequest('/admin/report/review', { reportId, reviewerId: 1, status, remark })
        if (res && res.code == 200) {
          this.$message.success('审核完成')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '审核失败')
        }
      } catch (e) { this.$message.error('审核失败') }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>
