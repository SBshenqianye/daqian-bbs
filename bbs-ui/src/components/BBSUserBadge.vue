<template>
  <span
    class="inline-flex min-w-0"
    :class="layout === 'inline' ? 'items-center gap-2' : 'flex-col items-start gap-1'"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
  >
    <span class="inline-flex items-center gap-2 min-w-0 cursor-pointer" @click="goProfile">
      <BBSAvatar
        v-if="showAvatar"
        :src="avatar"
        :size="avatarBox"
        :level-index="levelIndex"
      />
      <!-- inline：作者与单位压成一行，避免列表卡片被单位名撑高；stacked：仍分两行 -->
      <span v-if="layout === 'inline'" class="inline-flex items-baseline gap-1 min-w-0">
        <span class="font-semibold text-on-surface truncate" :class="nameClass">{{ displayName }}</span>
        <span
          v-if="showOrg && orgName"
          class="text-outline truncate max-w-[9em]"
          :class="orgClass"
          :title="orgNameFull || orgName"
        >· {{ orgName }}</span>
      </span>
      <span v-else class="flex flex-col min-w-0 leading-tight">
        <span class="font-semibold text-on-surface truncate" :class="nameClass">{{ displayName }}</span>
        <span
          v-if="showOrg && orgName"
          class="text-outline truncate"
          :class="orgClass"
          :title="orgNameFull || orgName"
        >{{ orgName }}</span>
      </span>
    </span>

    <!-- 悬浮资料卡：fixed 定位 + 高层级，彻底避开父级 overflow / 列表遮挡 -->
    <div
      v-if="visible && profile"
      class="bbs-user-card fixed z-[9999] w-[300px] rounded-2xl border border-border bg-container shadow-[0_18px_40px_-12px_rgba(15,23,42,0.35)] overflow-hidden"
      :style="panelStyle"
      @mouseenter="cancelClose"
      @mouseleave="onLeave"
    >
      <div class="h-14" :style="{ background: 'linear-gradient(120deg,' + spec.soft + ',' + spec.color + '33)' }"></div>
      <div class="px-4 pb-4 -mt-8">
        <div class="flex items-end justify-between gap-2">
          <BBSAvatar :src="profile.portrait" :size="56" :level-index="profileLevel" />
          <button
            class="mb-1 px-3 py-1.5 rounded-full text-[12px] font-medium text-white transition-opacity hover:opacity-90"
            :style="{ backgroundColor: spec.color }"
            @click.stop="goProfile"
          >{{ isSelf ? '个人中心' : '查看主页' }}</button>
        </div>
        <p class="mt-2 font-bold text-on-surface truncate">{{ profile.nickname }}</p>
        <p class="text-[11px] text-on-surface-variant truncate">{{ profile.orgNameFull || profile.orgName || '—' }}</p>
        <p
          v-if="profile.introduce"
          class="mt-1 text-[11px] text-on-surface-variant leading-snug truncate"
          :title="profile.introduce"
        >{{ profile.introduce }}</p>

        <div class="flex items-center gap-2 mt-2">
          <BBSLevelBadge :level-index="profileLevel" :level="{ levelName: levelName, totalPoints: levelPoints }" />
          <span class="text-[11px] text-on-surface-variant">{{ levelPoints }} 分</span>
        </div>

        <div class="mt-2 h-1.5 rounded-full bg-surface-variant overflow-hidden">
          <div class="h-full rounded-full" :style="{ width: progress + '%', backgroundColor: spec.color }"></div>
        </div>
        <p class="mt-1 text-[10px] text-outline">
          <template v-if="profile.level && profile.level.nextThreshold != null">
            距下一级还需 {{ Math.max(0, profile.level.nextThreshold - levelPoints) }} 分
          </template>
          <template v-else>已达当前最高等级</template>
        </p>

        <div class="grid grid-cols-3 gap-2 mt-3 text-center">
          <div><p class="text-sm font-bold text-on-surface">{{ profile.articleCount || 0 }}</p><p class="text-[10px] text-outline">帖子</p></div>
          <div><p class="text-sm font-bold text-on-surface">{{ profile.commentCount || 0 }}</p><p class="text-[10px] text-outline">评论</p></div>
          <div><p class="text-sm font-bold text-on-surface">{{ profile.replyCount || 0 }}</p><p class="text-[10px] text-outline">回复</p></div>
        </div>

        <div v-if="unlockedAchievements.length" class="mt-3">
          <p class="text-[10px] text-outline mb-1">成就 {{ unlockedAchievements.length }} 枚</p>
          <div class="flex flex-wrap gap-1">
            <span
              v-for="a in unlockedAchievements.slice(0, 5)"
              :key="a.id"
              class="w-6 h-6"
              v-html="a.iconSvg"
              :title="a.name"
            ></span>
          </div>
        </div>
      </div>
    </div>
  </span>
</template>

<script>
import BBSAvatar from '@/components/BBSAvatar.vue'
import BBSLevelBadge from '@/components/BBSLevelBadge.vue'
import { levelSpec, normalizeLevel } from '../../../shared/level-icons'

const AVATAR_BOX = { xs: 24, sm: 28, md: 36, lg: 48 }
const NAME_CLASS = {
  xs: 'text-[12px]',
  sm: 'text-[13px]',
  md: 'text-[14px]',
  lg: 'text-[16px]',
}
const ORG_CLASS = { xs: 'text-[10px]', sm: 'text-[11px]', md: 'text-[11px]', lg: 'text-[12px]' }

export default {
  name: 'BBSUserBadge',
  components: { BBSAvatar, BBSLevelBadge },
  props: {
    avatar: { type: String, default: '' },
    nickname: { type: String, default: '未知用户' },
    orgName: { type: String, default: '' },
    orgNameFull: { type: String, default: '' },
    levelIndex: { type: [Number, String], default: null },
    /** 传 userId 才启用悬浮卡与跳转 */
    userId: { type: [Number, String], default: null },
    /** xs | sm | md | lg */
    size: { type: String, default: 'sm' },
    /** inline | stacked */
    layout: { type: String, default: 'inline' },
    showAvatar: { type: Boolean, default: true },
    showOrg: { type: Boolean, default: true },
    /** 是否允许悬浮资料卡 */
    hover: { type: Boolean, default: true },
  },
  data() {
    return { visible: false, profile: null, openTimer: null, closeTimer: null, panelStyle: {} }
  },
  computed: {
    avatarBox() {
      return AVATAR_BOX[this.size] || 28
    },
    nameClass() {
      return NAME_CLASS[this.size] || NAME_CLASS.sm
    },
    orgClass() {
      return ORG_CLASS[this.size] || ORG_CLASS.sm
    },
    displayName() {
      return this.nickname || '匿名用户'
    },
    currentUserId() {
      try {
        const raw = window.sessionStorage.getItem('user')
        return raw ? (JSON.parse(raw).id != null ? String(JSON.parse(raw).id) : null) : null
      } catch (e) {
        return null
      }
    },
    isSelf() {
      return this.userId != null && this.currentUserId != null && String(this.userId) === this.currentUserId
    },
    profileLevel() {
      if (this.profile && this.profile.level && this.profile.level.levelIndex != null) {
        return normalizeLevel(this.profile.level.levelIndex)
      }
      return normalizeLevel(this.levelIndex)
    },
    spec() {
      return levelSpec(this.profileLevel)
    },
    levelName() {
      if (this.profile && this.profile.level && this.profile.level.levelName) return this.profile.level.levelName
      return `${this.spec.name}-${this.spec.minor}`
    },
    levelPoints() {
      const lv = this.profile && this.profile.level
      return lv && lv.totalPoints != null ? lv.totalPoints : 0
    },
    progress() {
      const lv = this.profile && this.profile.level
      if (!lv) return 0
      if (lv.progress != null) return Math.max(0, Math.min(100, lv.progress))
      return 0
    },
    unlockedAchievements() {
      const list = (this.profile && this.profile.achievements) || []
      return list.filter(a => a.unlocked === 1)
    },
  },
  methods: {
    onEnter() {
      if (!this.hover || this.userId == null) return
      this.cancelClose()
      if (this.openTimer) clearTimeout(this.openTimer)
      this.openTimer = setTimeout(() => this.open(), 220)
    },
    onLeave() {
      if (this.openTimer) { clearTimeout(this.openTimer); this.openTimer = null }
      if (this.closeTimer) clearTimeout(this.closeTimer)
      this.closeTimer = setTimeout(() => { this.visible = false }, 180)
    },
    cancelClose() {
      if (this.closeTimer) { clearTimeout(this.closeTimer); this.closeTimer = null }
    },
    open() {
      const el = this.$el && this.$el.querySelector && this.$el
      if (!el) return
      const rect = el.getBoundingClientRect()
      const width = 300
      const estimatedHeight = 300
      const pad = 10
      let left = rect.left
      if (left + width > window.innerWidth - pad) left = window.innerWidth - width - pad
      left = Math.max(pad, left)
      let top = rect.bottom + 8
      if (top + estimatedHeight > window.innerHeight - pad) {
        top = Math.max(pad, rect.top - estimatedHeight - 8)
      }
      this.panelStyle = { left: left + 'px', top: top + 'px' }
      this.visible = true
      if (!this.profile || (this.profile.id !== this.userId && String(this.profile.id) !== String(this.userId))) {
        this.load()
      }
    },
    load() {
      if (this.userId == null) return
      this.getRequest(`/common/user/publicProfile/${this.userId}`).then(res => {
        this.profile = (res && res.obj) || null
        if (!this.profile) this.visible = false
      }).catch(() => { this.visible = false })
    },
    goProfile() {
      if (this.userId == null) return
      this.$router.push(this.isSelf
        ? { name: 'BBSUserinfo' }
        : { name: 'BBSPublicProfile', params: { userId: this.userId } })
    },
  },
  beforeDestroy() {
    if (this.openTimer) clearTimeout(this.openTimer)
    if (this.closeTimer) clearTimeout(this.closeTimer)
  },
}
</script>
