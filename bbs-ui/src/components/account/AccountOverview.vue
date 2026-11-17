<template>
  <div class="space-y-5">
    <!-- 头部：头像 + 等级 + 进度 -->
    <div class="rounded-2xl border border-border overflow-hidden">
      <div class="h-16" :style="{ background: 'linear-gradient(120deg,' + spec.soft + ',' + spec.color + '33)' }"></div>
      <div class="px-5 pb-4 -mt-8">
        <div class="flex items-end justify-between gap-3 flex-wrap">
          <BBSAvatar :src="avatarSrc" :size="72" :level-index="levelInfo.levelIndex" />
          <div class="flex items-center gap-2 mb-1">
            <BBSLevelBadge :level="levelInfo" />
            <span class="text-[11.5px] text-on-surface-variant tabnum">{{ levelInfo.totalPoints || 0 }} 分</span>
          </div>
        </div>
        <h1 class="mt-2 text-[19px] font-semibold text-on-surface truncate">{{ userInfo.nickname || '未设置昵称' }}</h1>
        <p class="text-[11.5px] text-on-surface-variant mt-0.5 truncate">
          <span class="material-symbols-outlined text-[13px] align-[-2px] mr-0.5 text-outline">apartment</span>{{ userInfo.orgNameFull || userInfo.orgName || userInfo.username }}
        </p>

        <div class="mt-3">
          <div class="flex items-center justify-between text-[10.5px] text-outline mb-1">
            <span>Lv.{{ levelInfo.levelIndex || 1 }} · {{ levelInfo.levelName || '未定级' }}</span>
            <span v-if="levelInfo.nextThreshold != null">距下一级还需 {{ Math.max(0, levelInfo.nextThreshold - (levelInfo.totalPoints || 0)) }} 分</span>
            <span v-else>已达最高等级</span>
          </div>
          <div class="h-1.5 rounded-full bg-surface-variant overflow-hidden">
            <div class="h-full rounded-full transition-all" :style="{ width: progressPercent + '%', backgroundColor: spec.color }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据概览 -->
    <div>
      <h3 class="ui-section-title">
        <span class="material-symbols-outlined text-[16px] text-primary">insights</span>数据概览
      </h3>
      <div class="grid grid-cols-3 md:grid-cols-6 gap-2 mt-2.5">
        <div
          v-for="s in statCards"
          :key="s.label"
          class="rounded-xl border border-border bg-surface-container-lowest p-2.5 text-center"
        >
          <p class="text-[17px] font-bold text-on-surface tabnum">{{ s.value }}</p>
          <p class="text-[10.5px] text-outline mt-0.5">{{ s.label }}</p>
        </div>
      </div>
    </div>

    <!-- 成就墙 -->
    <div>
      <div class="flex items-center gap-2">
        <h3 class="ui-section-title">
          <span class="material-symbols-outlined text-[16px] text-amber-500 filled-icon">workspace_premium</span>我的成就
        </h3>
        <span class="text-[10.5px] text-outline">{{ unlockedCount }} / {{ achievements.length }}</span>
      </div>
      <div v-if="achievements.length" class="grid grid-cols-2 md:grid-cols-4 gap-2 mt-2.5">
        <div
          v-for="a in achievements"
          :key="a.id"
          class="flex items-center gap-2 rounded-xl border p-2.5"
          :class="a.unlocked ? 'border-border bg-white' : 'border-dashed border-outline-variant/70 opacity-45'"
          :title="a.description"
        >
          <span class="w-8 h-8 shrink-0" v-html="a.iconSvg"></span>
          <div class="min-w-0">
            <p class="text-[11.5px] font-medium truncate">{{ a.name }}</p>
            <p class="text-[10px] text-outline truncate">{{ a.unlocked ? '已解锁' : (a.description || '未解锁') }}</p>
          </div>
        </div>
      </div>
      <p v-else class="text-[11.5px] text-outline mt-2">暂无成就配置</p>
    </div>

    <!-- 最近发布 -->
    <div>
      <div class="flex items-center justify-between">
        <h3 class="ui-section-title">
          <span class="material-symbols-outlined text-[16px] text-primary">edit_note</span>最近发布
        </h3>
        <button class="text-[11.5px] text-primary hover:opacity-80" @click="$router.push('/write')">去发帖</button>
      </div>
      <div v-if="recentArticles.length" class="mt-2.5 border border-border rounded-xl overflow-hidden divide-y divide-outline-variant/40">
        <div
          v-for="a in recentArticles"
          :key="a.articleId"
          class="px-3.5 py-2 flex items-center gap-2 cursor-pointer hover:bg-surface-container-low transition-colors"
          @click="goArticle(a.articleId)"
        >
          <span v-if="a.isFeatured" class="material-symbols-outlined text-[14px] text-amber-500 shrink-0 filled-icon">stars</span>
          <span class="flex-1 truncate text-[12.5px] text-on-surface">{{ a.title }}</span>
          <span class="text-[10.5px] text-outline shrink-0 hidden md:inline">浏览 {{ a.views }}</span>
          <span class="text-[10.5px] text-outline shrink-0 tabnum">{{ (a.createTime || '').slice(0, 10) }}</span>
        </div>
      </div>
      <p v-else class="text-[11.5px] text-outline mt-2">还没有发布过帖子</p>
    </div>
  </div>
</template>

<script>
import BBSAvatar from '@/components/BBSAvatar.vue'
import BBSLevelBadge from '@/components/BBSLevelBadge.vue'
import { levelSpec, normalizeLevel } from '../../../../shared/level-icons'

export default {
  name: 'AccountOverview',
  components: { BBSAvatar, BBSLevelBadge },
  data() {
    return {
      userInfo: {
        nickname: '',
        userId: '',
        username: '',
        portrait: '',
        orgName: '',
        orgNameFull: '',
      },
      levelInfo: {},
      profile: {},
      achievements: [],
      recentArticles: [],
    }
  },
  computed: {
    avatarSrc() {
      return this.userInfo.portrait || ''
    },
    spec() {
      return levelSpec(normalizeLevel(this.levelInfo && this.levelInfo.levelIndex))
    },
    progressPercent() {
      const lv = this.levelInfo || {}
      if (lv.progress != null) return Math.max(0, Math.min(100, lv.progress))
      return 0
    },
    unlockedCount() {
      return this.achievements.filter(a => a.unlocked === 1).length
    },
    statCards() {
      const p = this.profile || {}
      return [
        { label: '帖子', value: p.articleCount || 0 },
        { label: '评论', value: p.commentCount || 0 },
        { label: '回复', value: p.replyCount || 0 },
        { label: '获赞', value: p.likesReceived || 0 },
        { label: '被收藏', value: p.favoritesReceived || 0 },
        { label: '收藏', value: p.favoriteCount || 0 },
      ]
    },
  },
  mounted() {
    this.load()
  },
  methods: {
    load() {
      const cached = window.sessionStorage.getItem('user')
      if (cached) {
        try {
          const u = JSON.parse(cached)
          this.applyUserInfo(u)
          if (u.id) this.loadProfile(u.id)
        } catch (e) { /* ignore */ }
      }
      this.getRequest('/common/user/info').then(resp => {
        if (resp) {
          this.applyUserInfo(resp)
          window.sessionStorage.setItem('user', JSON.stringify(resp))
          this.loadProfile(resp.id)
        }
      }).catch(err => { console.warn('[AccountOverview] loadUserInfo', err) })
    },
    applyUserInfo(u) {
      this.userInfo.nickname = u.nickname || this.userInfo.nickname
      this.userInfo.userId = String(u.id || u.userId || '')
      this.userInfo.username = u.username || ''
      this.userInfo.portrait = u.portrait || ''
      this.userInfo.orgName = u.orgName || ''
      this.userInfo.orgNameFull = u.orgNameFull || ''
    },
    loadProfile(userId) {
      if (!userId) return
      this.getRequest(`/common/user/publicProfile/${userId}`).then(res => {
        const p = (res && res.obj) || null
        if (!p) return
        this.profile = p
        this.achievements = Array.isArray(p.achievements) ? p.achievements : []
        this.recentArticles = Array.isArray(p.recentArticles) ? p.recentArticles : []
        if (p.level) this.levelInfo = p.level
      }).catch(err => { console.warn('[AccountOverview] loadProfile', err) })
    },
    goArticle(articleId) {
      if (articleId) this.$router.push({ name: 'BBSArticleDetails', params: { articleId } })
    },
  },
}
</script>
