<template>
  <article class="ui-card ui-card-hover group cursor-pointer p-4" @click="$emit('click', article)">
    <div class="flex items-start gap-3">
      <div class="flex-1 min-w-0">
        <!-- 标题行：精华用小星星点缀，正文仍是第一视觉 -->
        <h3 class="ui-post-title line-clamp-2 group-hover:text-primary">
          <span
            v-if="article.isFeatured"
            class="material-symbols-outlined text-rank-gold align-[-3px] mr-0.5 text-[17px] filled-icon"
            title="精华帖"
          >stars</span>{{ article.title }}
        </h3>

        <p v-if="article.summary" class="ui-post-summary">{{ article.summary }}</p>

        <!-- 元信息固定一行：作者/时间/标签 左，统计/操作 右 -->
        <div class="flex items-center justify-between gap-3 mt-2.5">
          <div class="ui-meta">
            <span class="inline-flex min-w-0" @click.stop>
              <BBSUserBadge
                :avatar="article.authorAvatar"
                :nickname="article.author"
                :org-name="article.authorOrgName"
                :org-name-full="article.authorOrgNameFull"
                :level-index="article.authorLevelIndex"
                :icon-svg="article.authorIconSvg || ''"
                :user-id="article.userId"
                size="xs"
                layout="inline"
              />
            </span>
            <span class="text-outline-variant shrink-0">·</span>
            <span class="whitespace-nowrap shrink-0">{{ article.time }}</span>
            <span v-if="article.labelName" class="ui-chip ui-chip-primary hidden sm:inline-flex">
              {{ article.labelName }}
            </span>
            <span v-if="article.isAnonymous" class="ui-chip ui-chip-muted">匿名</span>
          </div>

          <div class="ui-meta shrink-0">
            <span class="ui-meta-item"><span class="material-symbols-outlined">visibility</span>{{ article.views || 0 }}</span>
            <span class="ui-meta-item"><span class="material-symbols-outlined">chat_bubble</span>{{ article.comments || 0 }}</span>
            <span v-if="showLikes" class="ui-meta-item"><span class="material-symbols-outlined">thumb_up</span>{{ article.likes || 0 }}</span>
            <slot name="actions" />
          </div>
        </div>
      </div>

      <div v-if="article.cover" class="w-[96px] h-[68px] md:w-[108px] md:h-[74px] rounded-lg bg-surface-container overflow-hidden shrink-0 border border-outline-variant">
        <img :src="article.cover" class="w-full h-full object-cover" alt="" loading="lazy">
      </div>
    </div>

    <div v-if="$slots.footer" class="mt-2.5 pt-2 border-t border-outline-variant/40 flex items-center justify-between gap-2">
      <slot name="footer" />
    </div>
  </article>
</template>

<script>
import BBSUserBadge from '@/components/BBSUserBadge.vue'

export default {
  name: 'PostListItem',
  components: { BBSUserBadge },
  props: {
    article: { type: Object, required: true },
    showLikes: { type: Boolean, default: true },
  },
}
</script>
