<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-warning">gavel</span>
            违规管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理用户违规记录与积分扣减</p>
        </div>
      </div>

      <!-- Add Violation Dialog -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-4">新增违规记录</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">选择用户</label>
            <UserSelect v-model="form.userId" placeholder="搜索用户名或昵称..." />
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">违规类型</label>
            <select v-model="form.violationType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
              <option value="">请选择</option>
              <option v-for="opt in violationOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">关联类型</label>
            <select v-model="form.relatedType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
              <option value="">无</option>
              <option value="article">帖子</option>
              <option value="comment">评论</option>
              <option value="reply">回复</option>
            </select>
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">关联ID</label>
            <input v-model="form.relatedId" type="number" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="可选">
          </div>
          <div class="md:col-span-2">
            <label class="block text-body-sm text-on-surface-variant mb-1">备注说明</label>
            <textarea v-model="form.remark" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="2" placeholder="可选"></textarea>
          </div>
        </div>
        <div class="mt-4 flex justify-end">
          <button class="px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="submitting" @click="handleAdd">
            {{ submitting ? '提交中...' : '记录违规' }}
          </button>
        </div>
      </div>

      <!-- List -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-title-lg text-title-lg">违规记录</h3>
          <div class="flex items-center gap-2">
            <input v-model="searchUserId" type="number" class="px-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-body-sm focus:border-primary outline-none w-32" placeholder="用户ID">
            <button class="px-3 py-1.5 bg-primary-container text-on-primary-container rounded-lg hover:opacity-90 text-body-sm" @click="loadList">搜索</button>
          </div>
        </div>
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无违规记录</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">用户</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">违规类型</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">扣分</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">关联内容</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">备注</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">申诉状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.id" class="hover:bg-surface-container-low/50">
                <!-- 用户 -->
                <td class="px-4 py-3 text-body-sm">
                  <el-tooltip :content="'ID: ' + item.userId + ' | 昵称: ' + (item.nickname || '无')" placement="top" :open-delay="300">
                    <span class="cursor-help text-primary hover:underline">{{ item.nickname || '用户#' + item.userId }}</span>
                  </el-tooltip>
                </td>
                <!-- 违规类型 -->
                <td class="px-4 py-3 text-body-sm">{{ item.violationLabel || item.violationType }}</td>
                <!-- 扣分 -->
                <td class="px-4 py-3 text-body-sm text-error font-medium">-{{ item.pointsDeducted }}</td>
                <!-- 关联内容 -->
                <td class="px-4 py-3 text-body-sm">
                  <span v-if="item.relatedType" class="text-primary cursor-pointer hover:underline">{{ item.relatedType }}#{{ item.relatedId }}</span>
                  <span v-else class="text-on-surface-variant">-</span>
                </td>
                <!-- 备注 -->
                <td class="px-4 py-3 text-body-sm max-w-[180px]">
                  <el-tooltip :content="item.remark" placement="top" :open-delay="300" :disabled="!item.remark || item.remark.length <= 20">
                    <span class="truncate block cursor-help">{{ item.remark || '-' }}</span>
                  </el-tooltip>
                </td>
                <!-- 申诉状态 -->
                <td class="px-4 py-3 text-body-sm">
                  <span v-if="item.appealStatus === 'pending'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-yellow-100 text-yellow-800">申诉中</span>
                  <span v-else-if="item.appealStatus === 'accepted'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-green-100 text-green-800">申诉通过</span>
                  <span v-else-if="item.appealStatus === 'rejected'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-red-100 text-red-800">申诉驳回</span>
                  <span v-else class="text-on-surface-variant text-[12px]">-</span>
                </td>
                <!-- 时间 -->
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.createTime }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- Pagination -->
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
import UserSelect from '@/components/UserSelect.vue'

export default {
  name: 'ViolationPage',
  components: { UserSelect },
  data() {
    return {
      loading: false,
      submitting: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      searchUserId: '',
      violationOptions: [],
      form: {
        userId: '',
        violationType: '',
        relatedType: '',
        relatedId: '',
        remark: ''
      }
    }
  },
  mounted() {
    this.loadViolationOptions()
    this.loadList()
  },
  methods: {
    async loadViolationOptions() {
      try {
        const res = await this.postRequest('/admin/listDict', {})
        if (res && res.code == 200 && Array.isArray(res.obj)) {
          this.violationOptions = res.obj
            .filter(d => d.dictType === 'violation')
            .sort((a, b) => (a.dictSort || 0) - (b.dictSort || 0))
            .map(d => ({
              value: d.dictKey,
              label: d.dictLabel + (d.dictValue ? ' (-' + d.dictValue + '分)' : '')
            }))
        }
      } catch (e) { /* ignore */ }
    },
    async loadList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.searchUserId) params.userId = parseInt(this.searchUserId)
        const res = await this.postRequest('/admin/violation/list', params)
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async handleAdd() {
      if (!this.form.userId || !this.form.violationType) {
        this.$message.warning('请填写用户ID和违规类型')
        return
      }
      this.submitting = true
      try {
        const params = { ...this.form, userId: parseInt(this.form.userId), operatorId: 1 }
        if (this.form.relatedId) params.relatedId = parseInt(this.form.relatedId)
        const res = await this.postRequest('/admin/violation/add', params)
        if (res && res.code == 200) {
          this.$message.success('违规记录已添加')
          this.form = { userId: '', violationType: '', relatedType: '', relatedId: '', remark: '' }
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '操作失败')
        }
      } catch (e) { this.$message.error('操作失败') }
      finally { this.submitting = false }
    },
    changePage(page) {
      this.currentPage = page
      this.loadList()
    }
  }
}
</script>
