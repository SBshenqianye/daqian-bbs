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
        <div class="border border-outline-variant rounded-lg overflow-x-auto overflow-y-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无举报记录</p>
          </div>
          <table v-else class="w-full text-left min-w-[720px]">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">举报人</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">目标</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">原因</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="group in list" :key="group.representative.id" class="hover:bg-surface-container-low/50">
                  <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                    <UserCell :user-id="group.representative.reporterId" :name="group.representative.reporterName" />
                  </td>
                  <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                    <div class="flex items-center gap-2">
                      <span class="text-primary cursor-pointer hover:underline" @click="openPreview(group.representative)">
                        {{ getTargetTypeLabel(group.representative.targetType) }}#{{ group.representative.targetId }}
                      </span>
                      <el-popover
                        v-if="group.totalCount > 1"
                        placement="bottom"
                        trigger="click"
                        :popper-class="'report-history-popover'"
                        width="420"
                      >
                        <div slot="reference" class="px-2 py-0.5 rounded text-[11px] font-medium cursor-pointer bg-blue-50 text-blue-700 hover:bg-blue-100">
                          {{ group.totalCount }} 条举报 ▼
                        </div>
                        <div class="space-y-2 max-h-[300px] overflow-y-auto">
                          <div v-for="item in getHistory(group)" :key="item.id" class="flex items-center gap-2 text-[12px] py-1.5 border-b border-outline-variant/30 last:border-0">
                            <span class="text-on-surface-variant flex-shrink-0">{{ item.reporterName || '举报人 ' + item.reporterId }}</span>
                            <span :class="{
                              'px-1.5 py-0.5 rounded font-medium flex-shrink-0 whitespace-nowrap': true,
                              'bg-yellow-100 text-yellow-800': item.status === 'pending',
                              'bg-green-100 text-green-800': item.status === 'confirmed',
                              'bg-red-100 text-red-800': item.status === 'rejected'
                            }">{{ getStatusLabel(item.status) }}</span>
                            <span class="text-on-surface-variant flex-1 truncate" :title="item.reason">{{ item.reason || '无原因' }}</span>
                            <span class="text-outline flex-shrink-0">{{ item.createTime }}</span>
                          </div>
                        </div>
                        <p class="text-[11px] text-outline mt-1">共 {{ group.totalCount }} 条举报（含代表记录）</p>
                      </el-popover>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-body-sm max-w-[200px] truncate" :title="group.representative.reason">{{ group.representative.reason || '-' }}</td>
                  <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                    <span :class="{
                      'px-2 py-0.5 rounded text-[12px] font-medium whitespace-nowrap': true,
                      'bg-yellow-100 text-yellow-800': group.representative.status === 'pending',
                      'bg-green-100 text-green-800': group.representative.status === 'confirmed',
                      'bg-red-100 text-red-800': group.representative.status === 'rejected'
                    }">{{ getStatusLabel(group.representative.status) }}</span>
                  </td>
                  <td class="px-4 py-3 text-body-sm text-on-surface-variant whitespace-nowrap">{{ group.representative.createTime }}</td>
                  <td class="px-4 py-3 text-body-sm whitespace-nowrap max-w-[220px]">
                    <div v-if="group.representative.status === 'pending'" class="flex items-center gap-1 whitespace-nowrap">
                      <button class="px-2 py-1 border border-outline-variant text-on-surface-variant rounded text-[12px] hover:bg-surface-container-low transition-colors" @click="handleReview(group.representative, 'confirmed')">仅确认</button>
                      <button class="px-2 py-1 bg-error text-on-error rounded text-[12px] hover:opacity-90 transition-opacity shadow-sm" @click="openViolationDialog(group.representative)">确认并扣分</button>
                      <button class="px-2 py-1 border border-error/40 text-error rounded text-[12px] hover:bg-error/5 transition-colors" @click="openRejectDialog(group.representative)">驳回</button>
                    </div>
                    <span v-else class="text-on-surface-variant text-[12px] truncate block" :title="group.representative.reviewRemark">{{ group.representative.reviewRemark || '已处理' }}</span>
                  </td>
                </tr>
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

    <!-- 内容预览弹窗（公共组件，#11 举报/违规共用） -->
    <ContentPreviewDialog :visible="previewVisible" :item="previewItem" @close="closePreview" />

    <!-- 转违规对话框 -->
    <el-dialog title="确认举报并扣分" :visible.sync="violationDialogVisible" width="500px" :close-on-click-modal="false">
      <div class="space-y-4">
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">违规用户</label>
          <div class="w-full px-3 py-2 bg-surface-container-low border border-outline-variant rounded-lg text-body-sm">
            {{ violationDialogItem ? (violationDialogItem.targetAuthorName || '未知') + ' (ID: ' + (violationDialogItem.targetAuthorId || '?') + ')' : '' }}
          </div>
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">违规类型 <span class="text-red-500">*</span></label>
          <select v-model="violationForm.violationType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
            <option value="">请选择</option>
            <option v-for="opt in violationOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">审核备注</label>
          <textarea v-model="violationForm.remark" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="2" placeholder="填写扣分依据（选填）"></textarea>
        </div>
        <p class="text-body-sm text-on-surface-variant">关联内容: {{ violationDialogItem ? getTargetTypeLabel(violationDialogItem.targetType) + '#' + violationDialogItem.targetId : '' }}</p>
      </div>
      <div slot="footer" class="flex justify-end gap-2">
        <button class="px-4 py-2 text-body-sm border border-outline-variant rounded-lg hover:bg-surface-container-low" @click="violationDialogVisible = false">取消</button>
        <button class="px-4 py-2 text-body-sm bg-primary text-white rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="violationSubmitting" @click="submitViolation">
          {{ violationSubmitting ? '提交中...' : '确认并扣分' }}
        </button>
      </div>
    </el-dialog>

    <!-- 驳回举报对话框（可选认定为恶意举报并扣分） -->
    <el-dialog title="驳回举报" :visible.sync="rejectDialogVisible" width="480px" :close-on-click-modal="false">
      <div class="space-y-4">
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">审核备注</label>
          <textarea v-model="rejectForm.remark" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="2" placeholder="填写驳回原因（选填）"></textarea>
        </div>
        <div class="px-3 py-3 bg-red-50 border border-red-200 rounded-lg">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="rejectForm.malicious" class="accent-red-500">
            <span class="text-body-sm text-red-700 font-medium">认定为恶意/虚假举报</span>
          </label>
          <div v-if="rejectForm.malicious" class="mt-3 pl-6 flex items-center gap-2">
            <span class="text-body-sm text-on-surface-variant">扣举报人</span>
            <input
              v-model.number="rejectForm.deductPoints"
              type="number"
              min="1"
              class="w-20 px-2 py-1 border border-outline-variant rounded text-body-sm text-center"
            >
            <span class="text-body-sm text-on-surface-variant">分（默认取自数据字典，可修改）</span>
          </div>
        </div>
        <p class="text-[12px] text-on-surface-variant">
          关联内容: {{ rejectDialogItem ? getTargetTypeLabel(rejectDialogItem.targetType) + '#' + rejectDialogItem.targetId : '' }}
          <br>勾选"恶意举报"后将对举报人扣分并记录积分日志、通知举报人。
        </p>
      </div>
      <div slot="footer" class="flex justify-end gap-2">
        <button class="px-4 py-2 text-body-sm border border-outline-variant rounded-lg hover:bg-surface-container-low" @click="rejectDialogVisible = false">取消</button>
        <button class="px-4 py-2 text-body-sm bg-error text-white rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="rejectSubmitting" @click="submitReject">
          {{ rejectSubmitting ? '提交中...' : '确认驳回' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import UserCell from '@/components/UserCell.vue'
import ContentPreviewDialog from '@/components/ContentPreviewDialog.vue'

export default {
  name: 'ReportPage',
  components: { UserCell, ContentPreviewDialog },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      filterStatus: '',
      violationOptions: [],
      // 内容预览弹窗（公共组件）
      previewVisible: false,
      previewItem: null,
      // 转违规对话框
      violationDialogVisible: false,
      violationDialogItem: null,
      violationSubmitting: false,
      violationForm: {
        violationType: '',
        remark: ''
      },
      // 驳回对话框（含恶意举报扣分）
      rejectDialogVisible: false,
      rejectDialogItem: null,
      rejectSubmitting: false,
      rejectForm: {
        remark: '',
        malicious: false,
        deductPoints: 5
      },
      // 恶意举报默认扣分值（取自数据字典 violation/false_report，dict_value 即扣几分）
      maliciousDefaultPoints: 5
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
          // 恶意举报默认扣分值：取字典 violation/false_report 的 dictValue（与"确认并扣分"违规类型同源可配置）
          const falseReport = res.obj.find(d => d.dictKey === 'false_report')
          const v = falseReport && parseInt(falseReport.dictValue, 10)
          this.maliciousDefaultPoints = (v && v > 0) ? v : 5
        }
      } catch (e) { /* ignore */ }
    },
    getStatusLabel(s) { return { pending: '待审核', confirmed: '已确认', rejected: '已驳回' }[s] || s },
    /** 当前登录管理员 id（登录态 sessionStorage['admin']，与后端"审核人以登录态身份为准"配套） */
    currentAdminId() {
      try {
        const admin = JSON.parse(window.sessionStorage.getItem('admin') || '{}')
        return admin.id || 1
      } catch (e) { return 1 }
    },
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    getHistory(group) {
      return group.members.filter(m => m.id !== group.representative.id)
    },
    openPreview(item) {
      this.previewItem = item
      this.previewVisible = true
    },
    closePreview() {
      this.previewVisible = false
      this.previewItem = null
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
      const group = this.list.find(g => g.members.some(m => m.id === item.id))
      const groupHint = group && group.totalCount > 1 ? `该内容共 ${group.totalCount} 条举报，将一并确认。` : ''
      const title = status === 'confirmed'
        ? `仅确认举报（不扣分）？${groupHint}举报人各 +2 分`
        : `确定驳回该举报？`
      this.$prompt('审核备注（可选）', title, { type: status === 'confirmed' ? 'success' : 'warning' })
        .then(({ value }) => this.doReview(item.id, status, value, false, null))
        .catch(() => {})
    },
    /** 打开驳回对话框：可勾选"恶意举报"并扣分（分值默认取字典配置） */
    openRejectDialog(item) {
      this.rejectDialogItem = item
      this.rejectForm = {
        remark: '',
        malicious: false,
        deductPoints: this.maliciousDefaultPoints
      }
      this.rejectDialogVisible = true
    },
    async submitReject() {
      if (!this.rejectDialogItem) return
      if (this.rejectForm.malicious) {
        const p = parseInt(this.rejectForm.deductPoints, 10)
        if (!p || p <= 0) {
          this.$message.warning('恶意举报扣分分值必须为正整数')
          return
        }
        this.rejectForm.deductPoints = p
      }
      this.rejectSubmitting = true
      try {
        await this.doReview(this.rejectDialogItem.id, 'rejected', this.rejectForm.remark,
          this.rejectForm.malicious, this.rejectForm.malicious ? this.rejectForm.deductPoints : null)
        this.rejectDialogVisible = false
      } finally {
        this.rejectSubmitting = false
      }
    },
    async doReview(reportId, status, remark, malicious, deductPoints) {
      try {
        const res = await this.postRequest('/admin/report/review', {
          reportId,
          reviewerId: this.currentAdminId(),
          status,
          remark,
          malicious: !!malicious,
          deductPoints
        })
        const suffix = malicious && deductPoints ? `（已认定恶意举报并扣 ${deductPoints} 分）` : ''
        handleResponse(res, {
          successMsg: '审核完成' + suffix,
          errorMsg: '审核失败',
          onSuccess: () => this.loadList()
        })
      } catch (e) { console.warn('[ReportPage]', e) }
    },
    // 转违规
    openViolationDialog(item) {
      this.violationDialogItem = item
      this.violationForm = {
        violationType: item.violationType || '',
        remark: item.reason || ''
      }
      this.violationDialogVisible = true
    },
    async submitViolation() {
      if (!this.violationDialogItem.targetAuthorId) {
        this.$message.warning('无法获取被举报内容的作者信息')
        return
      }
      if (!this.violationForm.violationType) {
        this.$message.warning('请选择违规类型')
        return
      }
      this.violationSubmitting = true
      const remark = this.violationForm.remark || ''
      try {
        const vRes = await this.postRequest('/admin/violation/add', {
          userId: this.violationDialogItem.targetAuthorId,
          violationType: this.violationForm.violationType,
          relatedType: this.violationDialogItem.targetType,
          relatedId: parseInt(this.violationDialogItem.targetId),
          operatorId: this.currentAdminId(),
          remark: remark
        })
        let firstFailed = false
        handleResponse(vRes, { errorMsg: '创建违规失败', onError: () => { firstFailed = true } })
        if (firstFailed) return

        const rRes = await this.postRequest('/admin/report/review', {
          reportId: this.violationDialogItem.id,
          reviewerId: this.currentAdminId(),
          status: 'confirmed',
          remark: remark || '已转违规处理'
        })
        handleResponse(rRes, {
          successMsg: '违规已创建，举报已确认',
          errorMsg: '违规已创建，但举报确认失败，请手动处理',
          onSuccess: () => { this.violationDialogVisible = false; this.loadList() },
          onError: () => { this.violationDialogVisible = false; this.loadList() }
        })
      } catch (e) {
        console.warn('[ReportPage]', e)
      } finally {
        this.violationSubmitting = false
      }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>
