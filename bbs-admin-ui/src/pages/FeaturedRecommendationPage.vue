<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600">stars</span>
            精华帖审批
          </h1>
          <p class="text-body-md text-secondary mt-1">版主推荐 → 总运营终审</p>
        </div>
      </div>

      <!-- 状态筛选 -->
      <div class="flex gap-2 mb-4">
        <button v-for="tab in statusTabs" :key="tab.value"
          class="px-4 py-2 rounded-lg text-body-sm font-medium transition-colors"
          :class="currentStatus === tab.value ? 'bg-primary text-on-primary' : 'bg-container border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'"
          @click="currentStatus = tab.value; loadList()">
          {{ tab.label }}
        </button>
      </div>

      <!-- 列表 -->
      <div class="bg-container border border-border rounded-xl overflow-hidden" v-loading="loading">
        <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
          <p class="text-body-md">暂无推荐记录</p>
        </div>
        <table v-else class="w-full text-left">
          <thead class="bg-surface-container-low">
            <tr>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">ID</th>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">推荐帖子</th>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">推荐人</th>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">推荐时间</th>
              <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/50">
            <tr v-for="item in list" :key="item.id" class="hover:bg-surface-container-low/50">
              <td class="px-4 py-3 text-body-sm">{{ item.id }}</td>
              <td class="px-4 py-3 text-body-sm">
                <span class="text-primary cursor-pointer hover:underline" @click="$router.push('/articleDetails/' + item.articleId)">{{ item.articleTitle || '帖子#' + item.articleId }}</span>
              </td>
              <td class="px-4 py-3 text-body-sm">
                <UserCell :user-id="item.recommenderId" :name="item.recommenderName" />
              </td>
              <td class="px-4 py-3 text-body-sm">
                <span class="px-2 py-0.5 rounded text-[11px] font-medium"
                  :class="{
                    'bg-yellow-100 text-yellow-800': item.status === 'pending',
                    'bg-green-100 text-green-800': item.status === 'approved',
                    'bg-red-100 text-red-800': item.status === 'rejected'
                  }">{{ statusLabel(item.status) }}</span>
              </td>
              <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.createTime }}</td>
              <td class="px-4 py-3 text-body-sm">
                <template v-if="item.status === 'pending'">
                  <button class="px-2 py-1 bg-green-50 text-green-700 rounded text-[12px] hover:bg-green-100 mr-1" @click="handleReview(item, 'approved')">通过</button>
                  <button class="px-2 py-1 bg-red-50 text-red-700 rounded text-[12px] hover:bg-red-100" @click="handleReview(item, 'rejected')">拒绝</button>
                </template>
                <span v-else class="text-body-sm text-on-surface-variant">{{ item.reviewRemark || '—' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页 -->
      <div class="flex justify-end mt-4" v-if="total > pageSize">
        <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
        <span class="px-3 py-1 text-body-sm text-on-surface-variant">{{ currentPage }}/{{ Math.ceil(total / pageSize) }}</span>
        <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- 审核弹窗 -->
    <div v-show="reviewDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/30" @click="reviewDialogVisible = false"></div>
      <div class="relative bg-container rounded-xl shadow-xl w-full max-w-md p-6">
        <h3 class="font-headline-sm text-headline-sm mb-4">
          {{ reviewForm.status === 'approved' ? '通过推荐' : '拒绝推荐' }}
        </h3>
        <p class="text-body-md text-on-surface-variant mb-2">
          {{ reviewForm.status === 'approved' ? '确认后将帖子设为精华帖，作者获得+10积分。' : '拒绝该推荐，帖子不变。' }}
        </p>
        <div class="mb-4">
          <label class="block text-body-sm text-on-surface-variant mb-1">审核备注（选填）</label>
          <textarea v-model="reviewForm.remark" rows="3" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none" placeholder="填写审核备注..."></textarea>
        </div>
        <div class="flex justify-end gap-2">
          <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant" @click="reviewDialogVisible = false">取消</button>
          <button class="px-4 py-2 rounded text-on-primary disabled:opacity-60"
            :class="reviewForm.status === 'approved' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'"
            :disabled="reviewSaving" @click="doReview">
            {{ reviewSaving ? '处理中...' : (reviewForm.status === 'approved' ? '确认通过' : '确认拒绝') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import UserCell from '@/components/UserCell.vue'

export default {
  name: 'FeaturedRecommendationPage',
  components: { UserCell },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      currentStatus: '',
      statusTabs: [
        { label: '全部', value: '' },
        { label: '待审核', value: 'pending' },
        { label: '已通过', value: 'approved' },
        { label: '已拒绝', value: 'rejected' }
      ],
      reviewDialogVisible: false,
      reviewSaving: false,
      reviewForm: { id: null, status: '', remark: '' }
    }
  },
  mounted() { this.loadList() },
  methods: {
    statusLabel(s) {
      return { pending: '待审核', approved: '已通过', rejected: '已拒绝' }[s] || s
    },
    async loadList() {
      this.loading = true
      try {
        const res = await this.postRequest('/admin/featured/recommendList', {
          page: this.currentPage, size: this.pageSize, status: this.currentStatus || undefined
        })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    handleReview(item, status) {
      this.reviewForm = { id: item.id, status, remark: '' }
      this.reviewDialogVisible = true
    },
    async doReview() {
      this.reviewSaving = true
      try {
        const user = JSON.parse(sessionStorage.getItem('user') || '{}')
        const res = await this.postRequest('/admin/featured/review', {
          recommendationId: this.reviewForm.id,
          status: this.reviewForm.status,
          remark: this.reviewForm.remark || null,
          reviewerId: user.id || 1
        })
        handleResponse(res, {
          successMsg: '审核完成',
          errorMsg: '审核失败',
          onSuccess: () => {
            this.reviewDialogVisible = false
            this.loadList()
          }
        })
      } catch (e) { this.$message.error('审核失败') }
      finally { this.reviewSaving = false }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>
