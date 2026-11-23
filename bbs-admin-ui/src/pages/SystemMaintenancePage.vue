<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">build</span>
          系统维护
        </h1>
        <p class="text-body-md text-secondary mt-1">数据清理与运维动作；只影响测试数据，不触碰正常用户</p>
      </div>

      <!-- 测试账号帖子清理 -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="flex items-start gap-3">
          <span class="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 grid place-items-center shrink-0">
            <span class="material-symbols-outlined">cleaning_services</span>
          </span>
          <div class="min-w-0 flex-1">
            <h2 class="font-title-lg text-title-lg text-on-surface">清理测试账号帖子</h2>
            <p class="text-body-md text-on-surface-variant mt-1 leading-relaxed">
              删除全部「测试账号」发布的帖子。执行的是逻辑删除，删掉的帖子会进入「帖子管理 → 已删除」，
              可随时核查；正常用户的帖子不受影响。测试账号的评论、回复不随之删除。
            </p>
            <p class="text-[12px] text-on-surface-variant mt-2">
              测试账号在
              <router-link to="/user" class="text-primary hover:underline">用户管理</router-link>
              的编辑弹窗里勾选「标记为测试账号」；这类账号不参与排行榜与人均统计。
            </p>
          </div>
        </div>
        <div class="flex items-center justify-end gap-3 mt-5 pt-4 border-t border-outline-variant">
          <span v-if="!canPurge" class="text-[12px] text-on-surface-variant">当前账号没有 article:purge:test-account 权限</span>
          <button
            class="inline-flex items-center gap-1.5 px-5 py-2 rounded-lg font-label-md text-label-md transition-all disabled:opacity-40 disabled:cursor-not-allowed"
            :class="canPurge ? 'bg-error text-on-primary hover:opacity-90' : 'bg-surface-variant text-on-surface'"
            :disabled="!canPurge || purging"
            :title="canPurge ? '删除所有测试账号发布的帖子' : '无清理权限'"
            @click="handlePurge"
          >
            <span class="material-symbols-outlined text-[18px]">delete_sweep</span>
            {{ purging ? '清理中...' : '立即清理测试账号帖子' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import adminStore from '@/utils/adminStore'

export default {
  name: 'SystemMaintenancePage',
  data() {
    return { purging: false }
  },
  computed: {
    canPurge() {
      return adminStore.hasPermission('article:purge:test-account')
    }
  },
  methods: {
    handlePurge() {
      if (!this.canPurge) return
      this.$confirm(
        '将删除所有标记为「测试账号」的用户发布的帖子（逻辑删除，可在「帖子管理 → 已删除」查看）。此操作只影响测试账号，正常用户的帖子不受影响。确定继续？',
        '清理测试账号帖子',
        { type: 'warning', confirmButtonText: '确定清理', cancelButtonText: '取消' }
      ).then(() => {
        this.purging = true
        this.postRequest('/admin/article/purgeTestAccountArticles', {}).then(resp => {
          this.purging = false
          handleResponse(resp, {
            // 用后端返回的条数文案，例如「已删除 12 篇测试账号发布的帖子」
            successMsg: resp && resp.code == 200 ? resp.message : undefined
          })
        }).catch(() => { this.purging = false })
      }).catch(() => {})
    }
  }
}
</script>
