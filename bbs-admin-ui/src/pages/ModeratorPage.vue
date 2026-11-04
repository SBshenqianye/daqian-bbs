<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">shield</span>
            版主管理
          </h1>
          <p class="text-body-md text-secondary mt-1">任命和撤销版块管理员</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-60 text-body-sm flex items-center gap-1" :disabled="rewarding" @click="handleMonthlyReward">
            <span class="material-symbols-outlined text-[16px]" style="vertical-align: text-bottom;">payments</span>
            {{ rewarding ? '发放中...' : '手动发放本月奖励' }}
          </button>
        </div>
      </div>

      <!-- 自动发放设置 -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-4">自动发放设置</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">自动发放开关</label>
            <select v-model="autoConfig.enabled" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
              <option value="0">关闭</option>
              <option value="1">开启</option>
            </select>
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">每月发放日（1-28）</label>
            <input v-model="autoConfig.day" type="number" min="1" max="28" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" placeholder="1">
          </div>
          <div class="flex gap-2">
            <button class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60 text-body-sm" :disabled="savingConfig" @click="saveAutoConfig">
              {{ savingConfig ? '保存中...' : '保存设置' }}
            </button>
            <button class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 text-body-sm" @click="handleTriggerAuto">
              立即触发
            </button>
          </div>
        </div>
      </div>

      <!-- 本月取消列表 -->
      <div v-if="cancelledList.length > 0" class="bg-orange-50 border border-orange-200 rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-3 text-orange-800">本月已取消奖励的版主（{{ cancelledList.length }}人）</h3>
        <div class="flex flex-wrap gap-2">
          <span v-for="item in cancelledList" :key="item.userId" class="inline-flex items-center gap-1 px-3 py-1 bg-white border border-orange-300 rounded-full text-body-sm">
            {{ item.userName || '用户#' + item.userId }}
            <button class="text-green-600 hover:text-green-800 ml-1" @click="handleRestore(item)" title="恢复奖励">恢复</button>
          </span>
        </div>
      </div>

      <!-- Appoint Dialog -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-title-lg text-title-lg mb-4">任命版主</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">选择用户</label>
            <UserSelect v-model="form.userId" placeholder="搜索用户名或昵称..." />
          </div>
          <div>
            <label class="block text-body-sm text-on-surface-variant mb-1">选择板块</label>
            <select v-model="form.labelId" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
              <option value="" disabled>请选择板块</option>
              <option v-for="label in labelList" :key="label.labelId" :value="label.labelId">{{ label.labelName }}</option>
            </select>
          </div>
          <div class="flex items-end">
            <button class="px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="appointing" @click="handleAppoint">
              {{ appointing ? '任命中...' : '任命' }}
            </button>
          </div>
        </div>
      </div>

      <!-- List -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="flex items-center justify-between mb-4 gap-3">
          <h3 class="font-title-lg text-title-lg">当前版主</h3>
          <div class="flex items-center gap-2">
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索用户名/昵称"
              class="px-3 py-1.5 w-52 bg-surface border border-outline-variant rounded-lg text-body-sm outline-none focus:border-primary"
              @keyup.enter="currentPage = 1; loadList()"
            />
            <button class="px-3 py-1.5 border rounded text-body-sm hover:bg-surface-variant" @click="currentPage = 1; loadList()">搜索</button>
          </div>
        </div>
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无版主</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">用户</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">权限组</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">管理板块</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">最近任命</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="item in list" :key="item.userId" class="hover:bg-surface-container-low/50">
                <td class="px-4 py-3 text-body-sm">
                  <UserCell :user-id="item.userId" />
                  <div class="text-[11px] text-on-surface-variant">ID: {{ item.userId }}</div>
                </td>
                <td class="px-4 py-3 text-body-sm">{{ item.primaryGroupName || '未分配' }}</td>
                <td class="px-4 py-3 text-body-sm">
                  <span v-if="!item.boards || item.boards.length === 0" class="text-on-surface-variant">未分配板块</span>
                  <span
                    v-for="board in item.boards"
                    :key="board.labelId"
                    class="inline-flex items-center gap-1 mr-1 mb-1 px-2 py-0.5 rounded-full bg-primary-fixed/60 text-on-surface text-[12px]"
                  >
                    {{ board.labelName || labelNameById(board.labelId) }}
                    <button
                      class="text-error hover:text-red-700 leading-none"
                      title="撤销该板块"
                      @click="handleDismissBoard(item, board)"
                    >×</button>
                  </span>
                </td>
                <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ item.appointTime || '—' }}</td>
                <td class="px-4 py-3 text-body-sm whitespace-nowrap">
                  <el-button size="mini" type="primary" plain @click="openBoardDialog(item)">编辑板块</el-button>
                  <el-button size="mini" type="warning" plain @click="handleCancel(item)" title="取消本月奖励">取消奖励</el-button>
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

      <!-- 编辑板块弹窗 -->
      <div v-show="boardDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="boardDialogVisible = false"></div>
        <div class="relative bg-container rounded-xl shadow-xl w-full max-w-lg p-6">
          <h3 class="font-headline-sm text-headline-sm mb-1">编辑板块</h3>
          <p class="text-body-sm text-on-surface-variant mb-4">
            {{ boardForm.nickname || ('用户#' + boardForm.userId) }}
            · 勾选该版主负责的板块，保存后按差异增删
          </p>
          <div class="max-h-72 overflow-y-auto border border-outline-variant rounded-lg p-3 grid grid-cols-1 sm:grid-cols-2 gap-2">
            <label
              v-for="label in labelList"
              :key="label.labelId"
              class="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-surface-container-low cursor-pointer text-body-sm"
            >
              <input v-model="boardForm.labelIds" type="checkbox" :value="label.labelId" class="accent-primary" />
              <span>{{ label.labelName }}</span>
            </label>
            <div v-if="labelList.length === 0" class="text-body-sm text-on-surface-variant col-span-2">暂无可选板块</div>
          </div>
          <p class="text-[12px] text-on-surface-variant mt-3">
            撤销最后一个板块后账号仍保留版主角色，可继续登录管理后台（列表为空）。
          </p>
          <div class="flex justify-end gap-2 mt-5">
            <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant" @click="boardDialogVisible = false">取消</button>
            <button class="px-4 py-2 bg-primary text-on-primary rounded hover:opacity-90 disabled:opacity-60" :disabled="boardSaving" @click="saveBoards">
              {{ boardSaving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 取消奖励弹窗 -->
      <div v-show="cancelDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="cancelDialogVisible = false"></div>
        <div class="relative bg-container rounded-xl shadow-xl w-full max-w-md p-6">
          <h3 class="font-headline-sm text-headline-sm mb-4">取消版主本月奖励</h3>
          <p class="text-body-md text-on-surface-variant mb-2">
            将取消 <span class="font-medium">{{ cancelForm.userName }}</span> 本月的履职奖励。
          </p>
          <div class="mb-4">
            <label class="block text-body-sm text-on-surface-variant mb-1">取消原因</label>
            <textarea v-model="cancelForm.remark" rows="3" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none" placeholder="填写取消原因..."></textarea>
          </div>
          <div class="flex justify-end gap-2">
            <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant" @click="cancelDialogVisible = false">取消</button>
            <button class="px-4 py-2 bg-orange-600 text-white rounded hover:bg-orange-700 disabled:opacity-60" :disabled="cancelSaving" @click="doCancel">
              {{ cancelSaving ? '处理中...' : '确认取消' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import UserSelect from '@/components/UserSelect.vue'
import UserCell from '@/components/UserCell.vue'
import axios from 'axios'
import { handleResponse } from '../../../shared/feedback'

export default {
  name: 'ModeratorPage',
  components: { UserSelect, UserCell },
  data() {
    return {
      loading: false,
      appointing: false,
      rewarding: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      form: { userId: '', labelId: '' },
      autoConfig: { enabled: '1', day: '1' },
      savingConfig: false,
      cancelledList: [],
      cancelDialogVisible: false,
      cancelSaving: false,
      cancelForm: { userId: null, userName: '', remark: '' },
      boardDialogVisible: false,
      boardSaving: false,
      boardForm: { userId: null, nickname: '', labelIds: [] },
      keyword: '',
      labelList: [] // labelId -> { labelId, labelName }
    }
  },
  mounted() {
    this.loadList()
    this.loadAutoConfig()
    this.loadCancelledList()
    this.loadLabels()
  },
  methods: {
    /** 当前登录管理员 id（登录态 sessionStorage['admin']，操作人不得写死为固定值） */
    currentAdminId() {
      try {
        const admin = JSON.parse(window.sessionStorage.getItem('admin') || '{}')
        return admin.id || 1
      } catch (e) { return 1 }
    },
    async loadLabels() {
      try {
        const res = await axios.get(`${process.env.VUE_APP_BBS_API}/common/getArticleLabel`)
        if (Array.isArray(res)) {
          this.labelList = res
        } else if (res && res.code == 200 && Array.isArray(res.obj)) {
          this.labelList = res.obj
        }
      } catch (e) { /* ignore */ }
    },
    labelNameById(labelId) {
      const label = this.labelList.find(l => l.labelId === labelId)
      return label ? label.labelName : '标签#' + labelId
    },
    openBoardDialog(item) {
      this.boardForm = {
        userId: item.userId,
        nickname: item.nickname || item.username || '',
        labelIds: (item.boards || []).map(b => b.labelId)
      }
      this.boardDialogVisible = true
    },
    async saveBoards() {
      if (!this.boardForm.userId) return
      this.boardSaving = true
      try {
        const res = await this.postRequest('/admin/moderator/boards', {
          userId: this.boardForm.userId,
          labelIds: this.boardForm.labelIds.map(Number)
        })
        handleResponse(res, {
          successMsg: '板块已更新',
          errorMsg: '保存失败',
          onSuccess: () => {
            this.boardDialogVisible = false
            this.loadList()
          }
        })
      } catch (e) { console.warn('[ModeratorPage]', e) }
      finally { this.boardSaving = false }
    },
    async loadList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.keyword && this.keyword.trim()) params.keyword = this.keyword.trim()
        const res = await this.postRequest('/admin/moderator/list', params)
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async loadAutoConfig() {
      try {
        const res = await this.postRequest('/admin/listDict', {})
        if (res && res.code == 200 && Array.isArray(res.obj)) {
          const autoItem = res.obj.find(r => r.dictKey === 'moderator_reward_auto')
          if (autoItem) this.autoConfig.enabled = autoItem.dictValue || '0'
          const dayItem = res.obj.find(r => r.dictKey === 'moderator_reward_day')
          if (dayItem) this.autoConfig.day = dayItem.dictValue || '1'
        }
      } catch (e) { /* ignore */ }
    },
    async saveAutoConfig() {
      this.savingConfig = true
      try {
        // 先获取字典列表，找到对应记录的 id
        const listRes = await this.postRequest('/admin/listDict', {})
        if (!handleResponse(listRes, { errorMsg: '获取字典数据失败' })) return
        const dictList = listRes.obj
        const autoItem = dictList.find(r => r.dictKey === 'moderator_reward_auto')
        const dayItem = dictList.find(r => r.dictKey === 'moderator_reward_day')
        // 更新自动发放开关
        if (autoItem) {
          await this.postRequest('/admin/updateDict', {
            id: autoItem.id, dictType: 'moderator_reward_auto', dictValue: this.autoConfig.enabled,
            dictLabel: '版主奖励自动发放开关', dictKey: 'moderator_reward_auto'
          })
        }
        // 更新发放日
        if (dayItem) {
          await this.postRequest('/admin/updateDict', {
            id: dayItem.id, dictType: 'moderator_reward_day', dictValue: this.autoConfig.day,
            dictLabel: '版主奖励发放日', dictKey: 'moderator_reward_day'
          })
        }
        handleResponse({ code: 200, message: '设置已保存' })
      } catch (e) { console.warn('[ModeratorPage]', e) }
      finally { this.savingConfig = false }
    },
    handleTriggerAuto() {
      this.$confirm('立即触发自动发放？将按当前取消列表执行。', '触发自动发放', { type: 'info' })
        .then(async () => {
          try {
            const res = await this.postRequest('/admin/moderator/triggerAutoReward', {})
            handleResponse(res, { errorMsg: '触发失败' })
          } catch (e) { console.warn('[ModeratorPage]', e) }
        }).catch(() => {})
    },
    async loadCancelledList() {
      try {
        const res = await this.postRequest('/admin/moderator/cancelledRewards', {})
        if (res && res.code == 200 && res.obj) {
          this.cancelledList = res.obj
        } else { this.cancelledList = [] }
      } catch (e) { this.cancelledList = [] }
    },
    handleCancel(item) {
      this.cancelForm = { userId: item.userId, userName: item.nickname || item.userName || ('用户#' + item.userId), remark: '' }
      this.cancelDialogVisible = true
    },
    async doCancel() {
      this.cancelSaving = true
      try {
        const res = await this.postRequest('/admin/moderator/cancelReward', {
          userId: this.cancelForm.userId,
          operatorId: this.currentAdminId(),
          remark: this.cancelForm.remark || null
        })
        handleResponse(res, {
          successMsg: '已取消',
          errorMsg: '取消失败',
          onSuccess: async () => {
            this.cancelDialogVisible = false
            await this.loadCancelledList()
          }
        })
      } catch (e) { console.warn('[ModeratorPage]', e) }
      finally { this.cancelSaving = false }
    },
    async handleRestore(item) {
      try {
        const res = await this.postRequest('/admin/moderator/restoreReward', { userId: item.userId })
        handleResponse(res, {
          successMsg: '已恢复',
          errorMsg: '恢复失败',
          onSuccess: () => this.loadCancelledList()
        })
      } catch (e) { console.warn('[ModeratorPage]', e) }
    },
    async handleAppoint() {
      if (!this.form.userId || !this.form.labelId) {
        this.$message.warning('请选择用户和板块')
        return
      }
      this.appointing = true
      try {
        const res = await this.postRequest('/admin/moderator/appoint', {
          userId: parseInt(this.form.userId), labelId: parseInt(this.form.labelId), operatorId: this.currentAdminId()
        })
        handleResponse(res, {
          successMsg: '任命成功',
          errorMsg: '任命失败',
          onSuccess: () => {
            this.form = { userId: '', labelId: '' }
            this.loadList()
          }
        })
      } catch (e) { console.warn('[ModeratorPage]', e) }
      finally { this.appointing = false }
    },
    handleDismissBoard(item, board) {
      this.$confirm(
        `确定撤销 ${item.nickname || item.username || ('用户#' + item.userId)} 的「${board.labelName || this.labelNameById(board.labelId)}」版主吗？`,
        '提示',
        { type: 'warning' }
      )
        .then(() => this.doDismiss(item.userId, board.labelId, board.labelName))
        .catch(() => {})
    },
    async doDismiss(userId, labelId, labelName) {
      try {
        const res = await this.postRequest('/admin/moderator/dismiss', { userId, labelId })
        handleResponse(res, {
          successMsg: `已撤销${labelName ? '「' + labelName + '」' : ''}版主`,
          errorMsg: '撤销失败',
          onSuccess: () => this.loadList()
        })
      } catch (e) { console.warn('[ModeratorPage]', e) }
    },
    changePage(page) { this.currentPage = page; this.loadList() },
    handleMonthlyReward() {
      this.$confirm('确定为所有有效版主发放本月履职奖励（每人15积分）？已发放过的版主将自动跳过。', '发放履职奖励', { type: 'info' })
        .then(() => this.doMonthlyReward())
        .catch(() => {})
    },
    async doMonthlyReward() {
      this.rewarding = true
      try {
        const res = await this.postRequest('/admin/moderator/monthlyReward', { operatorId: this.currentAdminId() })
        handleResponse(res, { errorMsg: '发放失败' })
      } catch (e) { console.warn('[ModeratorPage]', e) }
      finally { this.rewarding = false }
    }
  }
}
</script>
