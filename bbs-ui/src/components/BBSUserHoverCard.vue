<template>
  <BBSUserBadge
    :avatar="avatar"
    :nickname="nickname"
    :org-name="orgName"
    :org-name-full="orgNameFull"
    :user-id="userId"
    :level-index="levelIndex"
    size="md"
  />
</template>

<script>
import BBSUserBadge from '@/components/BBSUserBadge.vue'

/**
 * 兼容旧调用的薄封装：真正的身份展示 + 悬浮资料卡在 BBSUserBadge 内实现。
 */
export default {
  name: 'BBSUserHoverCard',
  components: { BBSUserBadge },
  props: {
    userId: { type: [Number, String], default: null },
    nickname: { type: String, default: '' },
    avatar: { type: String, default: '' },
    orgName: { type: String, default: '' },
    orgNameFull: { type: String, default: '' },
    levelIndex: { type: [Number, String], default: null },
  },
}
</script>
