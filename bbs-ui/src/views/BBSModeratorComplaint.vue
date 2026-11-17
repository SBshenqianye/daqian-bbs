<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <header v-if="!embedded" class="mb-8 flex items-center gap-3">
      <button
        class="p-2 -ml-2 rounded-full hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="$router.back()"
      >
        <span class="material-symbols-outlined text-[24px]">arrow_back</span>
      </button>
      <h1 class="font-headline-lg text-headline-lg text-on-surface">版主投诉</h1>
    </header>

    <p class="text-[12px] leading-relaxed text-on-surface-variant mb-4 flex items-start gap-1.5">
      <span class="material-symbols-outlined text-[15px] text-primary mt-px">info</span>
      <span>如发现版主存在徇私评定精华、随意删帖、履职不作为等问题，可在此提交投诉。科技数字化工作部将在 5 个工作日内核查反馈。</span>
    </p>

    <!-- 提交投诉 -->
    <div class="ui-card p-4 mb-3">
      <h2 class="ui-section-title mb-3">
        <span class="w-6 h-6 rounded-md bg-primary/10 text-primary grid place-items-center">
          <span class="material-symbols-outlined text-[15px]">campaign</span>
        </span>
        提交投诉
      </h2>

      <!-- 板块（选填） -->
      <div class="mb-3">
        <label class="ui-form-label">
          板块 <span class="font-normal text-outline">（选填，选择后可缩小版主范围）</span>
        </label>
        <select v-model="selectedLabelId" class="ui-input" @change="onLabelChange">
          <option :value="null">全部板块</option>
          <option v-for="label in labels" :key="label.labelId" :value="label.labelId">
            {{ label.labelName }}
          </option>
        </select>
      </div>

      <!-- 版主（选填） -->
      <div class="mb-3">
        <label class="ui-form-label">
          被投诉版主 <span class="font-normal text-outline">（选填，可按姓名搜索）</span>
        </label>
        <input
          v-model="moderatorSearch"
          type="text"
          class="ui-input mb-2"
          placeholder="输入版主姓名搜索..."
        >
        <div v-if="moderatorsLoading" class="flex items-center justify-center py-4">
          <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>
        <div v-else-if="filteredModerators.length === 0 && (selectedLabelId || moderatorSearch.trim())" class="py-3 text-center text-on-surface-variant text-[11.5px]">
          未找到匹配的版主
        </div>
        <div v-else-if="filteredModerators.length > 0" class="space-y-1 max-h-[220px] overflow-y-auto border border-outline-variant/60 rounded-lg p-1.5 custom-scrollbar">
          <div
            v-for="mod in filteredModerators"
            :key="mod.userId + '-' + mod.labelId"
            class="flex items-center gap-2.5 p-2 rounded-lg border transition-all cursor-pointer"
            :class="selectedModerator && selectedModerator.userId === mod.userId && selectedModerator.labelId === mod.labelId
              ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
              : 'border-transparent hover:bg-surface-container-low'"
            @click="selectModerator(mod)"
          >
            <div class="flex-shrink-0">
              <span v-if="selectedModerator && selectedModerator.userId === mod.userId && selectedModerator.labelId === mod.labelId" class="material-symbols-outlined text-primary text-[18px]">radio_button_checked</span>
              <span v-else class="material-symbols-outlined text-outline text-[18px]">radio_button_unchecked</span>
            </div>
            <img
              v-if="mod.avatar"
              :src="mod.avatar"
              class="w-7 h-7 rounded-full object-cover flex-shrink-0"
            >
            <span v-else class="w-7 h-7 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant text-[12px] flex-shrink-0">
              {{ (mod.nickname || '').charAt(0) }}
            </span>
            <span class="text-[12.5px] text-on-surface">{{ mod.nickname }}</span>
            <span v-if="!selectedLabelId && mod.labelName" class="text-[11px] text-on-surface-variant">· {{ mod.labelName }}</span>
          </div>
        </div>
        <p v-if="selectedModerator" class="mt-1 text-[11.5px] text-primary">
          已选择：{{ selectedModerator.nickname }}
          <button class="ml-2 text-on-surface-variant hover:text-error" @click="selectedModerator = null">清除</button>
        </p>
      </div>

      <!-- 投诉内容（必填） -->
      <div class="mb-3">
        <label class="ui-form-label">
          投诉内容 <span class="text-error">*</span>
        </label>
        <textarea
          v-model="form.content"
          rows="5"
          class="ui-textarea"
          :placeholder="'请详细描述投诉事项，包括具体行为、时间等信息...\n如未选择板块和版主，请在内容中尽量说明以便管理员核查。'"
          maxlength="500"
        ></textarea>
        <span class="text-[11px] text-outline tabnum">{{ (form.content || '').length }}/500</span>
      </div>

      <div class="flex justify-end">
        <button
          class="ui-btn ui-btn-sm ui-btn-primary"
          :disabled="submitting || !form.content.trim()"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交投诉' }}
        </button>
      </div>
    </div>

    <!-- 我的投诉记录 -->
    <div class="ui-card p-4">
      <h2 class="ui-section-title mb-3">我的投诉记录</h2>
      <div v-if="loading" class="flex items-center justify-center py-8">
        <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>
      <div v-else-if="complaints.length === 0" class="ui-empty py-8">
        <span class="material-symbols-outlined">feedback</span>
        <p class="ui-empty-title">暂无投诉记录</p>
      </div>
      <div v-else class="space-y-2">
        <div v-for="item in complaints" :key="item.id" class="border border-outline-variant/60 rounded-lg p-3">
          <div class="flex items-center justify-between mb-1.5">
            <span class="ui-meta">
              投诉 #{{ item.id }}
              <template v-if="item.moderatorName"> · 版主：{{ item.moderatorName }}</template>
              <template v-else> · 未指定版主</template>
            </span>
            <span class="ui-chip"
              :class="{
                'bg-amber-50 text-amber-700': item.status === 'pending',
                'bg-green-50 text-green-700': item.status === 'accepted',
                'bg-red-50 text-red-700': item.status === 'rejected'
              }">{{ statusLabel(item.status) }}</span>
          </div>
          <p class="text-[12.5px] text-on-surface leading-relaxed">{{ item.content }}</p>
          <div class="text-[11px] text-on-surface-variant space-y-0.5 mt-1.5">
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
  props: { embedded: { type: Boolean, default: false } },
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
