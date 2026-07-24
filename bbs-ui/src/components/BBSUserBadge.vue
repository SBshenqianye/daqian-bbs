<template>
  <div class="inline-flex items-center gap-2" :class="containerClass">
    <img
      v-if="showAvatar"
      :src="avatarSrc"
      class="rounded-full bg-surface-variant object-cover flex-shrink-0"
      :class="avatarClass"
      alt=""
    >
    <div :class="layout === 'inline' ? 'inline-flex items-baseline gap-1 min-w-0' : 'flex flex-col min-w-0'">
      <span class="font-semibold text-on-surface truncate" :class="nameClass">{{ nickname }}</span>
      <span v-if="showOrg && orgName" class="text-outline truncate" :class="orgClass" :title="orgNameFull || orgName">
        {{ layout === 'inline' ? '· ' + orgName : orgName }}
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BBSUserBadge',
  props: {
    avatar: { type: String, default: '' },
    nickname: { type: String, default: '未知用户' },
    orgName: { type: String, default: '' },
    orgNameFull: { type: String, default: '' },
    size: { type: String, default: 'sm' },      // xs | sm | md | lg
    layout: { type: String, default: 'inline' }, // inline | stacked
    showAvatar: { type: Boolean, default: true },
    showOrg: { type: Boolean, default: true },
  },
  computed: {
    defaultAvatar() {
      return require('../assets/portrait.png')
    },
    avatarSrc() {
      return this.avatar || this.defaultAvatar
    },
    sizeMap() {
      return {
        xs: { container: '', avatar: 'w-7 h-7', name: 'text-body-sm', org: 'text-[11px]' },
        sm: { container: '', avatar: 'w-6 h-6', name: 'text-body-sm', org: 'text-[11px]' },
        md: { container: '', avatar: 'w-8 h-8', name: 'text-body-md', org: 'text-[12px]' },
        lg: { container: '', avatar: 'w-10 h-10', name: 'text-headline-sm', org: 'text-label-md' },
      }
    },
    containerClass() {
      return this.sizeMap[this.size]?.container || ''
    },
    avatarClass() {
      return this.sizeMap[this.size]?.avatar || 'w-6 h-6'
    },
    nameClass() {
      return this.sizeMap[this.size]?.name || 'text-body-sm'
    },
    orgClass() {
      return this.sizeMap[this.size]?.org || 'text-[11px]'
    },
  },
}
</script>
