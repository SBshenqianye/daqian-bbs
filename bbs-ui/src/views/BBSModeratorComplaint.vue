<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8 flex items-center gap-3">
      <button
        class="p-2 -ml-2 rounded-full hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="$router.back()"
      >
        <span class="material-symbols-outlined text-[24px]">arrow_back</span>
      </button>
      <h1 class="font-headline-lg text-headline-lg text-on-surface">版主投诉</h1>
    </header>

    <p class="text-body-md text-on-surface-variant mb-6">如发现版主存在徇私评定精华、随意删帖、履职不作为等问题，可在此提交投诉。科技数字化工作部将在5个工作日内核查反馈。</p>

    <!-- 提交投诉 -->
    <div class="bg-container border border-outline-variant rounded-lg p-card-padding mb-8">
      <h2 class="font-headline-sm text-headline-sm mb-4">提交投诉</h2>

      <!-- 板块（选填） -->
      <div class="mb-4">
        <label class="block text-body-sm font-medium text-on-surface mb-1">
          板块 <span class="text-on-surface-variant font-normal">（选填，选择后可缩小版主范围）</span>
        </label>
        <select
          v-model="selectedLabelId"
          class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none text-body-md"
          @change="onLabelChange"
        >
          <option :value="null">全部板块</option>
          <option v-for="label in labels" :key="label.labelId" :value="label.labelId">
            {{ label.labelName }}
          </option>
        </select>
      </div>

      <!-- 版主（选填） -->
      <div class="mb-4">
        <label class="block text-body-sm font-medium text-on-surface mb-1">
          被投诉版主 <span class="text-on-surface-variant font-normal">（选填，可按姓名搜索）</span>
        </label>
        <input
          v-model="moderatorSearch"
          type="text"
          class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none text-body-md mb-2"
          placeholder="输入版主姓名搜索..."
        >
        <div v-if="moderatorsLoading" class="flex items-center justify-center py-4">
          <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>
        <div v-else-if="filteredModerators.length === 0 && (selectedLabelId || moderatorSearch.trim())" class="py-3 text-center text-on-surface-variant text-body-sm">
          未找到匹配的版主
        </div>
        <div v-else-if="filteredModerators.length > 0" class="space-y-2 max-h-[240px] overflow-y-auto border border-outline-variant/50 rounded-lg p-2">
          <div
            v-for="mod in filteredModerators"
            :key="mod.userId + '-' + mod.labelId"
            class="flex items-center gap-3 p-2 rounded-lg border transition-all cursor-pointer"
            :class="selectedModerator && selectedModerator.userId === mod.userId && selectedModerator.labelId === mod.labelId
              ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
              : 'border-transparent hover:bg-surface-container-low'"
            @click="selectModerator(mod)"
          >
            <div class="flex-shrink-0">
              <span v-if="selectedModerator && selectedModerator.userId === mod.userId && selectedModerator.labelId === mod.labelId" class="material-symbols-outlined text-primary text-[20px]">radio_button_checked</span>
              <span v-else class="material-symbols-outlined text-outline text-[20px]">radio_button_unchecked</span>
            </div>
            <img
              v-if="mod.avatar"
              :src="mod.avatar"
              class="w-7 h-7 rounded-full object-cover flex-shrink-0"
            >
            <span v-else class="w-7 h-7 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant text-[12px] flex-shrink-0">
              {{ (mod.nickname || '').charAt(0) }}
            </span>
            <span class="font-body-md text-on-surface">{{ mod.nickname }}</span>
            <span v-if="!selectedLabelId && mod.labelName" class="text-body-sm text-on-surface-variant">· {{ mod.labelName }}</span>
          </div>
        </div>
        <p v-if="selectedModerator" class="mt-1 text-body-sm text-primary">
          已选择：{{ selectedModerator.nickname }}
          <button class="ml-2 text-on-surface-variant hover:text-error" @click="selectedModerator = null">清除</button>
        </p>
      </div>

      <!-- 投诉内容（必填） -->
      <div class="mb-4">
        <label class="block text-body-sm font-medium text-on-surface mb-1">
          投诉内容 <span class="text-error">*</span>
        </label>
        <textarea
          v-model="form.content"
          rows="5"
          class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none resize-none text-body-md"
          :placeholder="'请详细描述投诉事项，包括具体行为、时间等信息...\n如未选择板块和版主，请在内容中尽量说明以便管理员核查。'"
          maxlength="500"
        ></textarea>
        <span class="text-body-sm text-on-surface-variant">{{ (form.content || '').length }}/500</span>
      </div>

      <div class="flex justify-end">
        <button
          class="px-6 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed font-label-md"
          :disabled="submitting || !form.content.trim()"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交投诉' }}
        </button>
      </div>
    </div>

    <!-- 我的投诉记录 -->
    <div class="bg-container border border-outline-variant rounded-lg p-card-padding">
      <h2 class="font-headline-sm text-headline-sm mb-4">我的投诉记录</h2>
      <div v-if="loading" class="flex items-center justify-center py-8">
        <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>
      <div v-else-if="complaints.length === 0" class="py-8 text-center text-on-surface-variant">
        暂无投诉记录
      </div>
      <div v-else class="space-y-3">
        <div v-for="item in complaints" :key="item.id" class="border border-outline-variant/50 rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="text-body-sm text-on-surface-variant">
              投诉 #{{ item.id }}
              <template v-if="item.moderatorName"> · 版主：{{ item.moderatorName }}</template>
              <template v-else> · 未指定版主</template>
            </span>
            <span class="px-2 py-0.5 rounded text-[11px] font-medium"
              :class="{
                'bg-yellow-100 text-yellow-800': item.status === 'pending',
                'bg-green-100 text-green-800': item.status === 'accepted',
                'bg-red-100 text-red-800': item.status === 'rejected'
              }">{{ statusLabel(item.status) }}</span>
          </div>
          <p class="text-body-md text-on-surface mb-2">{{ item.content }}</p>
          <div class="text-body-sm text-on-surface-variant space-y-1">
            <p>提交时间：{{ item.createTime }}</p>
            <p v-if="item.reviewRemark">审核结果：{{ item.reviewRemark }}</p>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'

export default {
  name: 'BBSModeratorComplaint',
  data() {
    return {
      submitting: false,
      loading: false,
      labels: [],
      selectedLabelId: null,
      allModerators: [],
      moderatorsLoading: false,
      moderatorSearch: '',
      selectedModerator: null,
      form: { content: '' },
      complaints: []
    }
  },
  computed: {
    filteredModerators() {
      let list = this.allModerators
      // 按板块过滤
      if (this.selectedLabelId) {
        list = list.filter(m => m.labelId === this.selectedLabelId)
      }
      // 按姓名搜索
      const q = this.moderatorSearch.trim().toLowerCase()
      if (q) {
        list = list.filter(m => (m.nickname || '').toLowerCase().includes(q))
      }
      return list
    }
  },
  mounted() {
    this.loadLabels()
    this.loadAllModerators()
    this.loadComplaints()
  },
  methods: {
    statusLabel(s) {
      return { pending: '审核中', accepted: '已采纳', rejected: '已驳回' }[s] || s
    },
    async loadLabels() {
      try {
        const res = await this.getRequest('/common/getArticleLabel')
        if (Array.isArray(res)) {
          this.labels = res
        } else if (res && Array.isArray(res.obj)) {
          this.labels = res.obj
        }
      } catch (e) { this.labels = [] }
    },
    async loadAllModerators() {
      this.moderatorsLoading = true
      try {
        const res = await this.getRequest('/common/moderator/listAll')
        if (res && res.code == 200 && Array.isArray(res.obj)) {
          // 为每个版主记录附加板块名称
          const labelMap = {}
          this.labels.forEach(l => { labelMap[l.labelId] = l.labelName })
          this.allModerators = res.obj.map(m => ({
            ...m,
            labelName: labelMap[m.labelId] || ''
          }))
        } else {
          this.allModerators = []
        }
      } catch (e) { this.allModerators = [] }
      finally { this.moderatorsLoading = false }
    },
    onLabelChange() {
      this.selectedModerator = null
    },
    selectModerator(mod) {
      if (this.selectedModerator && this.selectedModerator.userId === mod.userId && this.selectedModerator.labelId === mod.labelId) {
        this.selectedModerator = null
      } else {
        this.selectedModerator = mod
      }
    },
    async loadComplaints() {
      const userStr = sessionStorage.getItem('user')
      if (!userStr) return
      const user = JSON.parse(userStr)
      this.loading = true
      try {
        const res = await this.postRequest('/user/moderatorComplaint/myList', { reporterId: user.id })
        if (res && res.code == 200 && res.obj) {
          this.complaints = res.obj
        } else { this.complaints = [] }
      } catch (e) { this.complaints = [] }
      finally { this.loading = false }
    },
    async handleSubmit() {
      const userStr = sessionStorage.getItem('user')
      if (!userStr) { this.$message.warning('请先登录'); return }
      const user = JSON.parse(userStr)
      if (!this.form.content.trim()) { this.$message.warning('请填写投诉内容'); return }
      this.submitting = true
      try {
        const res = await this.postRequest('/user/moderatorComplaint/submit', {
          reporterId: user.id,
          moderatorId: this.selectedModerator ? this.selectedModerator.userId : null,
          labelId: this.selectedLabelId,
          content: this.form.content.trim()
        })
        handleResponse(res, {
          successMsg: '投诉已提交',
          errorMsg: '提交失败',
          onSuccess: () => {
            this.form = { content: '' }
            this.selectedModerator = null
            this.moderatorSearch = ''
            this.loadComplaints()
          }
        })
      } catch (e) { this.$message.error('提交失败') }
      finally { this.submitting = false }
    }
  }
}
</script>
