<template>
  <main class="max-w-5xl mx-auto py-8 px-page-margin-mobile md:px-page-margin-desktop min-h-screen">
    <div v-if="loading" class="py-24 text-center text-on-surface-variant">
      <span class="material-symbols-outlined animate-spin text-[28px]">progress_activity</span>
    </div>
    <div v-else-if="!profile" class="py-24 text-center text-on-surface-variant">
      <span class="material-symbols-outlined text-[44px] opacity-20 mb-2 block mx-auto">person_off</span>
      用户不存在或主页未公开
    </div>
    <template v-else>
      <!-- 头部 -->
      <section class="rounded-2xl border border-border bg-container overflow-hidden">
        <div class="h-24" :style="{ background: 'linear-gradient(120deg,' + spec.soft + ',' + spec.color + '33)' }"></div>
        <div class="px-6 md:px-8 pb-6 -mt-12">
          <div class="flex items-end justify-between gap-3 flex-wrap">
            <BBSAvatar :src="profile.portrait" :size="92" :level-index="levelIndex" />
            <div class="flex items-center gap-2 mb-1">
              <BBSLevelBadge :level-index="levelIndex" :level="profile.level || {}" />
              <span class="text-[12px] text-on-surface-variant">{{ points }} 分</span>
            </div>
          </div>
          <h1 class="mt-3 text-[24px] font-bold text-on-surface truncate">{{ profile.nickname }}</h1>
          <p class="text-[12px] text-on-surface-variant mt-1 truncate">
            {{ profile.orgNameFull || profile.orgName || '—' }}
          </p>
          <p v-if="profile.introduce" class="text-[13px] text-on-surface-variant mt-2 leading-relaxed">{{ profile.introduce }}</p>

          <div class="mt-4">
            <div class="flex items-center justify-between text-[11px] text-outline mb-1.5">
              <span>Lv.{{ levelIndex }} · {{ levelName }}</span>
              <span v-if="profile.level && profile.level.nextThreshold != null">
                距下一级还需 {{ Math.max(0, profile.level.nextThreshold - points) }} 分
              </span>
              <span v-else>已达最高等级</span>
            </div>
            <div class="h-2 rounded-full bg-surface-variant overflow-hidden">
              <div class="h-full rounded-full transition-all" :style="{ width: progress + '%', backgroundColor: spec.color }"></div>
            </div>
          </div>

          <div class="flex items-center gap-4 mt-4 text-[11px] text-outline">
            <span>加入于 {{ (profile.createTime || '').slice(0, 10) || '--' }}</span>
            <span v-if="profile.level && profile.level.iconKey" class="hidden"></span>
          </div>
        </div>
      </section>

      <!-- 数据 -->
      <section class="grid grid-cols-3 md:grid-cols-6 gap-2 mt-4">
        <div v-for="s in stats" :key="s.label" class="rounded-xl border border-border bg-container p-3 text-center">
          <p class="text-[19px] font-bold text-on-surface tabnum">{{ s.value }}</p>
          <p class="text-[11px] text-outline mt-0.5">{{ s.label }}</p>
        </div>
      </section>

      <!-- 成就墙 -->
      <section class="rounded-2xl border border-border bg-container p-5 mt-4">
        <div class="flex items-center gap-2 mb-3">
          <span class="material-symbols-outlined text-rank-gold text-[20px]">workspace_premium</span>
          <h2 class="text-[15px] font-semibold text-on-surface">成就墙</h2>
          <span class="text-[11px] text-outline">{{ unlockedCount }} / {{ achievements.length }}</span>
        </div>
        <div v-if="achievements.length" class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <div
            v-for="a in achievements"
            :key="a.id"
            class="flex items-center gap-2 rounded-xl border p-2.5"
            :class="a.unlocked ? 'border-border bg-white' : 'border-dashed border-outline-variant/70 opacity-45'"
            :title="a.description"
          >
            <span class="w-9 h-9 shrink-0" v-html="a.iconSvg"></span>
            <div class="min-w-0">
              <p class="text-[12px] font-medium text-on-surface truncate">{{ a.name }}</p>
              <p class="text-[10px] text-outline truncate">{{ a.unlocked ? '已解锁' : (a.description || '未解锁') }}</p>
            </div>
          </div>
        </div>
        <p v-else class="text-[12px] text-on-surface-variant">暂无成就</p>
      </section>

      <!-- 最近发布 -->
      <section class="rounded-2xl border border-border bg-container p-5 mt-4">
        <h2 class="text-[15px] font-semibold text-on-surface mb-3">最近发布</h2>
        <div v-if="recentArticles.length" class="divide-y divide-outline-variant/40">
          <div
            v-for="a in recentArticles"
            :key="a.articleId"
            class="py-2.5 flex items-center gap-2.5 cursor-pointer hover:text-primary transition-colors"
            @click="goArticle(a.articleId)"
          >
            <span v-if="a.isFeatured" class="material-symbols-outlined text-[15px] text-rank-gold shrink-0">stars</span>
            <span class="flex-1 truncate text-[13px]">{{ a.title }}</span>
            <span class="text-[11px] text-outline shrink-0">{{ (a.createTime || '').slice(0, 10) }}</span>
          </div>
        </div>
        <p v-else class="text-[12px] text-on-surface-variant">还没有发布过帖子</p>
      </section>
    </template>
  </main>
</template>

<script>
import BBSAvatar from '@/components/BBSAvatar.vue'
import BBSLevelBadge from '@/components/BBSLevelBadge.vue'
import { levelSpec, normalizeLevel } from '../../../shared/level-icons'

export default {
  name: 'BBSPublicProfile',
  components: { BBSAvatar, BBSLevelBadge },
  data() {
    return { loading: true, profile: null }
  },
  computed: {
    levelIndex() {
      return normalizeLevel(this.profile && this.profile.level && this.profile.level.levelIndex)
    },
    spec() {
      return levelSpec(this.levelIndex)
    },
    levelName() {
      return (this.profile && this.profile.level && this.profile.level.levelName) || `${this.spec.name}-${this.spec.minor}`
    },
    points() {
      const lv = this.profile && this.profile.level
      return lv && lv.totalPoints != null ? lv.totalPoints : 0
    },
    progress() {
      const lv = this.profile && this.profile.level
      return lv && lv.progress != null ? Math.max(0, Math.min(100, lv.progress)) : 0
    },
    achievements() {
      const list = (this.profile && this.profile.achievements) || []
      return list
    },
    unlockedCount() {
      return this.achievements.filter(a => a.unlocked === 1).length
    },
    recentArticles() {
      return (this.profile && this.profile.recentArticles) || []
    },
    stats() {
      const p = this.profile || {}
      return [
        { label: '帖子', value: p.articleCount || 0 },
        { label: '评论', value: p.commentCount || 0 },
        { label: '回复', value: p.replyCount || 0 },
        { label: '获赞', value: p.likesReceived || 0 },
        { label: '被收藏', value: p.favoritesReceived || 0 },
        { label: '浏览', value: p.viewsReceived || 0 },
      ]
    },
  },
  mounted() {
    this.load()
  },
  methods: {
    load() {
      const id = this.$route.params.userId
      this.loading = true
      this.getRequest(`/common/user/publicProfile/${id}`).then(res => {
        this.profile = (res && res.obj) || null
        this.loading = false
      }).catch(() => {
        this.profile = null
        this.loading = false
      })
    },
    goArticle(articleId) {
      if (articleId) this.$router.push({ name: 'BBSArticleDetails', params: { articleId } })
    },
  },
}
</script>
