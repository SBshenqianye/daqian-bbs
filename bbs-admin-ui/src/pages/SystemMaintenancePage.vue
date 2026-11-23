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
            :disabled="!canPurge || previewLoading || purging"
            :title="canPurge ? '先预览将被清理的账号，再确认删除' : '无清理权限'"
            @click="openPurgePreview"
          >
            <span class="material-symbols-outlined text-[18px]">delete_sweep</span>
            {{ previewLoading ? '统计中...' : '清理测试账号帖子' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 清理前预览：先看账号，再确认 -->
    <el-dialog
      title="确认清理测试账号帖子"
      :visible.sync="previewVisible"
      width="640px"
      :close-on-click-modal="false"
    >
      <div class="space-y-3">
        <div class="px-3 py-2.5 bg-amber-50 border border-amber-200 rounded-lg text-[13px] leading-relaxed">
          将删除 <span class="font-semibold">{{ previewUserCount }}</span> 个测试账号发布的
          <span class="font-semibold">{{ previewArticleCount }}</span> 篇帖子。
          执行逻辑删除（可在「帖子管理 → 已删除」复核），正常用户的帖子不受影响。
        </div>

        <div class="border border-outline-variant rounded-lg overflow-hidden">
          <div class="max-h-64 overflow-y-auto">
            <table class="w-full text-left">
              <thead class="bg-surface-container-low sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-[12px] font-medium text-on-surface-variant">用户</th>
                  <th class="px-3 py-2 text-[12px] font-medium text-on-surface-variant">账号</th>
                  <th class="px-3 py-2 text-[12px] font-medium text-on-surface-variant text-right">帖子数</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant/50">
                <tr v-for="u in previewUsers" :key="u.userId">
                  <td class="px-3 py-2 text-body-sm">
                    {{ u.nickname || ('用户' + u.userId) }}
                    <span class="text-[11px] text-on-surface-variant">#{{ u.userId }}</span>
                  </td>
                  <td class="px-3 py-2 text-body-sm text-on-surface-variant">{{ u.username || '-' }}</td>
                  <td class="px-3 py-2 text-body-sm text-right tabnum">{{ u.articleCount }}</td>
                </tr>
                <tr v-if="previewUsers.length === 0">
                  <td colspan="3" class="px-3 py-6 text-center text-body-sm text-on-surface-variant">没有测试账号发布的帖子</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div slot="footer" class="flex justify-end gap-2">
        <button class="px-4 py-2 text-body-sm border border-outline-variant rounded-lg hover:bg-surface-container-low" @click="previewVisible = false">取消</button>
        <button
          class="px-5 py-2 text-body-sm bg-error text-on-primary rounded-lg hover:opacity-90 disabled:opacity-60"
          :disabled="purging"
          @click="confirmPurge"
        >
          {{ purging ? '清理中...' : '确认清理 ' + previewArticleCount + ' 篇' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import adminStore from '@/utils/adminStore'

export default {
  name: 'SystemMaintenancePage',
  data() {
    return {
      previewLoading: false,
      previewVisible: false,
      previewUsers: [],
      previewUserCount: 0,
      previewArticleCount: 0,
      purging: false
    }
  },
  computed: {
    canPurge() {
      return adminStore.hasPermission('article:purge:test-account')
    }
  },
  methods: {
    /** 先拉预览：把将被清理的账号列出来，管理员确认后才真正删除 */
    openPurgePreview() {
      if (!this.canPurge) return
      this.previewLoading = true
      this.postRequest('/admin/article/purgeTestAccountArticles/preview', {}).then(resp => {
        this.previewLoading = false
        if (!resp || resp.code != 200 || !resp.obj) return
        const users = resp.obj.users || []
        const articleCount = Number(resp.obj.articleCount || 0)
        if (articleCount === 0) {
          this.$message.info('当前没有测试账号发布的帖子')
          return
        }
        this.previewUsers = users
        this.previewUserCount = Number(resp.obj.userCount || users.length)
        this.previewArticleCount = articleCount
        this.previewVisible = true
      }).catch(() => { this.previewLoading = false })
    },
    confirmPurge() {
      this.purging = true
      this.postRequest('/admin/article/purgeTestAccountArticles', {}).then(resp => {
        this.purging = false
        handleResponse(resp, {
          // 用后端返回的条数文案，例如「已删除 12 篇测试账号发布的帖子」
          successMsg: resp && resp.code == 200 ? resp.message : undefined,
          onSuccess: () => { this.previewVisible = false }
        })
      }).catch(() => { this.purging = false })
    }
  }
}
</script>
