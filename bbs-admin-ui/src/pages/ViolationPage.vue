<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600">gavel</span>
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
        <div class="border border-outline-variant rounded-lg overflow-x-auto overflow-y-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无违规记录</p>
          </div>
          <table v-else class="w-full text-left min-w-[860px]">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">用户</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">违规类型</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">扣分</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">关联内容</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">备注</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.id" :data-violation-id="item.id"
                :class="[
                  'hover:bg-surface-container-low/50',
                  (highlightId != null && highlightId === item.id) ? 'violation-row-highlight' : ''
                ]">
                <!-- 用户 -->
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                  <UserCell :user-id="item.userId" :name="item.nickname" />
                </td>
                <!-- 违规类型 -->
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">{{ item.violationLabel || item.violationType }}</td>
                <!-- 扣分 -->
                <td class="px-4 py-3 text-body-sm text-error font-medium whitespace-nowrap">-{{ item.pointsDeducted }}</td>
                <!-- 关联内容（#11 点击打开内容预览） -->
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                  <span v-if="item.relatedType" class="text-primary cursor-pointer hover:underline" @click="openContentPreview(item)">
                    {{ getRelatedTypeLabel(item.relatedType) }}#{{ item.relatedId }}
                  </span>
                  <span v-else class="text-on-surface-variant">-</span>
                </td>
                <!-- 备注 -->
                <td class="px-4 py-3 text-body-sm max-w-[180px]">
                  <el-tooltip :content="item.remark" placement="top" :open-delay="300" :disabled="!item.remark || item.remark.length <= 20">
                    <span class="truncate block cursor-help">{{ item.remark || '-' }}</span>
                  </el-tooltip>
                </td>
                <!-- 状态：违规状态 + 申诉状态（#14/#12 合并一列） -->
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <el-tooltip v-if="item.status === 'cancelled'" :content="(item.cancelReason || '') + (item.cancelTime ? '（' + item.cancelTime + '）' : '')" placement="top" :open-delay="300">
                      <span class="px-2 py-0.5 rounded text-[12px] font-medium bg-gray-100 text-gray-600 cursor-help">已取消</span>
                    </el-tooltip>
                    <span v-else class="px-2 py-0.5 rounded text-[12px] font-medium bg-orange-100 text-orange-700">生效中</span>
                  </div>
                  <div class="flex items-center gap-1.5 mt-1">
                    <span v-if="item.appealStatus === 'pending'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-yellow-100 text-yellow-800">申诉中</span>
                    <span v-else-if="item.appealStatus === 'accepted'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-green-100 text-green-800">申诉通过</span>
                    <span v-else-if="item.appealStatus === 'rejected'" class="px-2 py-0.5 rounded text-[12px] font-medium bg-red-100 text-red-800">申诉驳回</span>
                    <span v-else class="text-on-surface-variant text-[12px]">-</span>
                    <span v-if="item.appealStatus" class="text-primary text-[12px] cursor-pointer hover:underline" @click="goAppeal(item.id)">查看</span>
                  </div>
                </td>
                <!-- 操作（#14 取消违规） -->
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                  <button v-if="item.status !== 'cancelled'" class="px-2 py-1 border border-amber-500/50 text-amber-600 rounded text-[12px] hover:bg-amber-50" @click="openCancelDialog(item)">取消违规</button>
                  <span v-else class="text-on-surface-variant text-[12px]">已取消</span>
                </td>
                <!-- 时间 -->
                <td class="px-4 py-3 text-body-sm text-on-surface-variant whitespace-nowrap">{{ item.createTime }}</td>
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

    <!-- #11 内容预览弹窗（公共组件） -->
    <ContentPreviewDialog :visible="previewVisible" :item="previewItem" @close="previewVisible = false" />

    <!-- #14 取消违规对话框 -->
    <el-dialog title="取消违规" :visible.sync="cancelDialogVisible" width="480px" :close-on-click-modal="false">
      <div class="space-y-4" v-if="cancelTarget">
        <div class="px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg text-[13px]">
          将回滚该次扣分并恢复被隐藏/删除的关联内容。已取消的记录不可重复操作。
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">取消原因 <span class="text-red-500">*</span></label>
          <textarea v-model="cancelReason" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="3" placeholder="记录取消原因（留痕）"></textarea>
        </div>
      </div>
      <div slot="footer" class="flex justify-end gap-2">
        <button class="px-4 py-2 text-body-sm border border-outline-variant rounded-lg hover:bg-surface-container-low" @click="cancelDialogVisible = false">关闭</button>
        <button class="px-4 py-2 text-body-sm bg-primary text-white rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="cancelSubmitting" @click="submitCancel">
          {{ cancelSubmitting ? '提交中...' : '确认取消违规' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import UserSelect from '@/components/UserSelect.vue'
import UserCell from '@/components/UserCell.vue'
import ContentPreviewDialog from '@/components/ContentPreviewDialog.vue'
import { handleResponse } from '../../../shared/feedback'

export default {
  name: 'ViolationPage',
  components: { UserSelect, UserCell, ContentPreviewDialog },
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
      },
      // #11 内容预览
      previewVisible: false,
      previewItem: null,
      // #12 路由定位高亮的违规 id
      highlightId: null,
      // #14 取消违规
      cancelDialogVisible: false,
      cancelTarget: null,
      cancelReason: '',
      cancelSubmitting: false
    }
  },
  mounted() {
    this.loadViolationOptions()
    // #12 从申诉管理跳转过来时按违规 id 定位
    const qid = parseInt(this.$route.query.violationId)
    if (qid) this.highlightId = qid
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
          // 路由复用时，URL 未带定位参数则强制清高亮，避免旧值残留
          if (!this.$route.query.violationId) this.highlightId = null
          this.$nextTick(() => this.scrollToHighlight())
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
        const params = { ...this.form, userId: parseInt(this.form.userId), operatorId: this.currentAdminId() }
        if (this.form.relatedId) params.relatedId = parseInt(this.form.relatedId)
        const res = await this.postRequest('/admin/violation/add', params)
        handleResponse(res, {
          successMsg: '违规记录已添加',
          errorMsg: '操作失败',
          onSuccess: async () => {
            this.form = { userId: '', violationType: '', relatedType: '', relatedId: '', remark: '' }
            await this.loadList()
          }
        })
      } catch (e) { this.$message.error('操作失败') }
      finally { this.submitting = false }
    },
    /** 当前登录管理员 id（登录态 sessionStorage['admin']，操作人不得写死为固定值） */
    currentAdminId() {
      try {
        const admin = JSON.parse(window.sessionStorage.getItem('admin') || '{}')
        return admin.id || 1
      } catch (e) { return 1 }
    },
    getRelatedTypeLabel(t) {
      return { article: '帖子', comment: '评论', reply: '回复' }[t] || t
    },
    // ========== #11 内容预览 ==========
    openContentPreview(item) {
      this.previewItem = {
        targetType: item.relatedType,
        targetId: item.relatedId,
        targetArticleId: item.relatedArticleId || (item.relatedType === 'article' ? item.relatedId : null),
        reason: item.remark || ''
      }
      this.previewVisible = true
    },
    // ========== #12 互跳 ==========
    goAppeal(violationId) {
      this.$router.push({ path: '/appeal', query: { appealViolationId: violationId } })
    },
    /** 列表加载后滚动到路由定位的违规行并高亮 */
    scrollToHighlight() {
      if (!this.highlightId) return
      const el = document.querySelector(`[data-violation-id="${this.highlightId}"]`)
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      // 无论是否命中当前页，3 秒后都清除高亮，避免残留
      setTimeout(() => { this.highlightId = null }, 3000)
    },
    // ========== #14 取消违规 ==========
    openCancelDialog(item) {
      this.cancelTarget = item
      this.cancelReason = ''
      // 有审核中申诉：先二次确认
      const doOpen = () => { this.cancelDialogVisible = true }
      if (item.appealStatus === 'pending') {
        this.$confirm('该违规存在审核中的申诉，建议先处理该申诉。确认仍要取消该违规吗？', '二次确认', {
          type: 'warning', confirmButtonText: '仍要取消', cancelButtonText: '再想想'
        }).then(doOpen).catch(() => {})
      } else {
        doOpen()
      }
    },
    async submitCancel() {
      if (!this.cancelTarget) return
      if (!this.cancelReason || !this.cancelReason.trim()) {
        this.$message.warning('请填写取消原因')
        return
      }
      this.cancelSubmitting = true
      try {
        const res = await this.postRequest('/admin/violation/cancel', {
          violationId: this.cancelTarget.id,
          reason: this.cancelReason.trim()
        })
        handleResponse(res, {
          successMsg: '违规已取消，扣分已回滚，关联内容已恢复',
          errorMsg: '取消失败',
          onSuccess: async () => { this.cancelDialogVisible = false; await this.loadList() }
        })
      } catch (e) { this.$message.error('取消失败') }
      finally { this.cancelSubmitting = false }
    },
    changePage(page) {
      this.currentPage = page
      this.loadList()
    }
  }
}
</script>

<style scoped>
.violation-row-highlight {
  background-color: rgb(254 243 199 / 0.6);
  transition: background-color 0.6s ease;
}
</style>
