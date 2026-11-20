<template>
  <main class="max-w-4xl mx-auto py-6 md:py-8 px-page-margin-mobile md:px-page-margin-desktop min-h-screen">
    <div v-if="loading" class="flex items-center justify-center py-16 text-on-surface-variant">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>
    <div v-else-if="!profile" class="ui-empty">
      <span class="material-symbols-outlined">person_off</span>
      <p class="ui-empty-title">用户不存在或主页未公开</p>
    </div>
    <template v-else>
      <!-- 头部 -->
      <section class="rounded-2xl border border-border bg-container overflow-hidden">
        <div class="h-20" :style="{ background: 'linear-gradient(120deg,' + spec.soft + ',' + spec.color + '33)' }"></div>
        <div class="px-5 md:px-6 pb-5 -mt-10">
          <div class="flex items-end justify-between gap-3 flex-wrap">
            <BBSAvatar :src="profile.portrait" :size="80" :level-index="levelIndex" :icon-svg="(profile.level && profile.level.iconSvg) || ''" />
            <div class="flex items-center gap-2 mb-1">
              <BBSLevelBadge :level-index="levelIndex" :level="profile.level || {}" />
              <span class="text-[11.5px] text-on-surface-variant tabnum">{{ points }} 分</span>
            </div>
          </div>
          <h1 class="mt-2 text-[21px] font-semibold text-on-surface truncate">{{ profile.nickname }}</h1>
          <p class="text-[11.5px] text-on-surface-variant mt-0.5 truncate">
            <span class="material-symbols-outlined text-[13px] align-[-2px] mr-0.5 text-outline">apartment</span>{{ profile.orgNameFull || profile.orgName || '—' }}
          </p>
          <p v-if="profile.introduce" class="text-[12.5px] text-on-surface-variant mt-2 leading-relaxed">{{ profile.introduce }}</p>

          <div class="mt-3">
            <div class="flex items-center justify-between text-[10.5px] text-outline mb-1">
              <span>Lv.{{ levelIndex }} · {{ levelName }}</span>
              <span v-if="profile.level && profile.level.nextThreshold != null">
                距下一级还需 {{ Math.max(0, profile.level.nextThreshold - points) }} 分
              </span>
              <span v-else>已达最高等级</span>
            </div>
            <div class="h-1.5 rounded-full bg-surface-variant overflow-hidden">
              <div class="h-full rounded-full transition-all" :style="{ width: progress + '%', backgroundColor: spec.color }"></div>
            </div>
          </div>

          <div class="flex items-center gap-2 mt-3 text-[10.5px] text-outline">
            <span class="ui-chip ui-chip-muted">
              <span class="material-symbols-outlined">calendar_month</span>
              加入于 {{ (profile.createTime || '').slice(0, 10) || '--' }}
            </span>
          </div>
        </div>
      </section>

      <!-- 数据 -->
      <section class="grid grid-cols-3 md:grid-cols-6 gap-2 mt-3">
        <div v-for="s in stats" :key="s.label" class="rounded-xl border border-border bg-container p-2.5 text-center">
          <p class="text-[17px] font-bold text-on-surface tabnum">{{ s.value }}</p>
          <p class="text-[10.5px] text-outline mt-0.5">{{ s.label }}</p>
        </div>
      </section>

      <!-- 成就墙 -->
      <section class="rounded-2xl border border-border bg-container p-4 mt-3">
        <div class="flex items-center gap-2 mb-2.5">
          <span class="material-symbols-outlined text-amber-500 text-[18px] filled-icon">workspace_premium</span>
          <h2 class="text-[14px] font-semibold text-on-surface">成就墙</h2>
          <span class="text-[10.5px] text-outline">{{ unlockedCount }} / {{ achievements.length }}</span>
        </div>
        <div v-if="achievements.length" class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <div
            v-for="a in achievements"
            :key="a.id"
            class="flex items-center gap-2 rounded-xl border p-2.5"
            :class="a.unlocked ? 'border-border bg-white' : 'border-dashed border-outline-variant/70 opacity-45'"
            :title="a.description"
          >
            <span class="w-8 h-8 shrink-0" v-html="a.iconSvg"></span>
            <div class="min-w-0">
              <p class="text-[11.5px] font-medium text-on-surface truncate">{{ a.name }}</p>
              <p class="text-[10px] text-outline truncate">{{ a.unlocked ? '已解锁' : (a.description || '未解锁') }}</p>
            </div>
          </div>
        </div>
        <p v-else class="text-[11.5px] text-on-surface-variant">暂无成就</p>
      </section>

      <!-- 最近发布 -->
      <section class="rounded-2xl border border-border bg-container p-4 mt-3">
        <h2 class="ui-section-title mb-2">
          <span class="material-symbols-outlined text-[16px] text-primary">edit_note</span>最近发布
        </h2>
        <div v-if="recentArticles.length" class="divide-y divide-outline-variant/40">
          <div
            v-for="a in recentArticles"
            :key="a.articleId"
            class="py-2 flex items-center gap-2 cursor-pointer hover:text-primary transition-colors"
            @click="goArticle(a.articleId)"
          >
            <span v-if="a.isFeatured" class="material-symbols-outlined text-[14px] text-amber-500 shrink-0 filled-icon">stars</span>
            <span class="flex-1 truncate text-[12.5px]">{{ a.title }}</span>
            <span class="text-[10.5px] text-outline shrink-0 tabnum">{{ (a.createTime || '').slice(0, 10) }}</span>
          </div>
        </div>
        <p v-else class="text-[11.5px] text-on-surface-variant">还没有发布过帖子</p>
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
