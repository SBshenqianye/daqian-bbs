<template>
  <main class="max-w-4xl mx-auto py-8 px-page-margin-mobile md:px-page-margin-desktop min-h-screen">
    <div class="flex items-center justify-between mb-5">
      <div>
        <h1 class="text-[22px] font-bold text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">bookmark</span>
          我的收藏
        </h1>
        <p class="text-[12px] text-on-surface-variant mt-1">共 {{ total }} 篇帖子</p>
      </div>
      <button class="text-[12px] text-primary hover:opacity-80 flex items-center gap-1" @click="$router.push('/userinfo')">
        <span class="material-symbols-outlined text-[16px]">arrow_back</span>返回个人中心
      </button>
    </div>

    <div v-if="loading" class="py-16 text-center text-on-surface-variant">
      <span class="material-symbols-outlined animate-spin text-[26px]">progress_activity</span>
    </div>

    <template v-else>
      <div v-if="list.length" class="space-y-3">
        <article
          v-for="item in list"
          :key="item.articleId"
          class="bg-container border border-border rounded-xl px-5 py-4 hover:border-primary/40 transition-colors cursor-pointer"
          @click="goArticle(item)"
        >
          <div class="flex gap-4">
            <div class="flex-1 min-w-0">
              <h3 class="text-[15px] font-semibold text-on-surface leading-snug line-clamp-2">{{ item.title }}</h3>
              <p v-if="item.summary" class="text-[12px] text-on-surface-variant mt-1.5 line-clamp-2">{{ item.summary }}</p>
              <div class="flex items-center justify-between gap-3 mt-2.5">
                <div class="flex items-center gap-1.5 min-w-0 text-[12px] text-outline">
                  <BBSUserBadge
                    :avatar="item.authorAvatar"
                    :nickname="item.author"
                    :level-index="item.authorLevelIndex"
                    :user-id="item.authorId"
                    size="xs"
                  />
                  <span class="shrink-0">·</span>
                  <span class="shrink-0">{{ (item.time || '').slice(0, 10) }}</span>
                </div>
                <div class="flex items-center gap-3 text-[12px] text-outline shrink-0">
                  <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">visibility</span>{{ item.views }}</span>
                  <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">chat_bubble</span>{{ item.comments }}</span>
                </div>
              </div>
            </div>
            <div v-if="item.cover" class="w-24 h-[72px] rounded-lg bg-surface-container overflow-hidden shrink-0 border border-outline-variant">
              <img :src="item.cover" class="w-full h-full object-cover" alt="">
            </div>
          </div>
          <div class="flex justify-end mt-2">
            <button
              class="text-[11px] text-outline hover:text-error flex items-center gap-1 px-2 py-1 rounded hover:bg-error/5 transition-colors"
              @click.stop="unfavorite(item)"
            >
              <span class="material-symbols-outlined text-[14px]">bookmark_remove</span>取消收藏
            </button>
          </div>
        </article>
      </div>

      <div v-else class="bg-container border border-dashed border-outline-variant rounded-xl py-20 flex flex-col items-center text-on-surface-variant">
        <span class="material-symbols-outlined text-[44px] mb-3 opacity-20">bookmark_border</span>
        <p class="text-[14px] text-on-surface mb-1">还没有收藏任何帖子</p>
        <p class="text-[12px] mb-4">在帖子详情页点击「收藏」即可加入这里</p>
        <button class="px-4 py-1.5 rounded-full bg-primary/8 text-primary text-[12px] hover:bg-primary/12" @click="$router.push('/forum')">去逛逛论坛</button>
      </div>

      <div v-if="pages > 1" class="flex items-center justify-center gap-2 mt-5">
        <button
          class="px-3 py-1.5 text-[12px] rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary disabled:opacity-40"
          :disabled="page <= 1"
          @click="changePage(page - 1)"
        >上一页</button>
        <span class="text-[12px] text-on-surface-variant px-1">{{ page }} / {{ pages }}</span>
        <button
          class="px-3 py-1.5 text-[12px] rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary disabled:opacity-40"
          :disabled="page >= pages"
          @click="changePage(page + 1)"
        >下一页</button>
      </div>
    </template>
  </main>
</template>

<script>
import BBSUserBadge from '@/components/BBSUserBadge.vue'
import { normalizeFileUrl } from '@/utils/utils'

export default {
  name: 'BBSFavorites',
  components: { BBSUserBadge },
  data() {
    return { loading: true, list: [], total: 0, page: 1, pages: 1, size: 10 }
  },
  computed: {
    userId() {
      try {
        const raw = window.sessionStorage.getItem('user')
        return raw ? JSON.parse(raw).id : null
      } catch (e) {
        return null
      }
    },
  },
  mounted() {
    this.load()
  },
  methods: {
    load() {
      if (!this.userId) {
        this.loading = false
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      this.loading = true
      this.getRequest(`/common/user/favorites?userId=${this.userId}&page=${this.page}&size=${this.size}`)
        .then(res => {
          const data = (res && res.obj) || {}
          const records = Array.isArray(data.list) ? data.list : []
          this.list = records.map(a => ({
            articleId: a.articleId,
            title: a.articleTitle || '',
            summary: a.articleSummary || '',
            author: a.articleAuthor || '',
            authorId: a.userId,
            authorAvatar: normalizeFileUrl(a.portrait || ''),
            authorLevelIndex: a.authorLevelIndex || null,
            time: a.createTime || '',
            views: a.articleViewNum || 0,
            comments: a.commentNum ?? a.commentNumCount ?? 0,
            cover: normalizeFileUrl(a.articleImage || ''),
          }))
          this.total = Number(data.total || 0)
          this.pages = Number(data.pages || 1)
          this.loading = false
        })
        .catch(() => {
          this.list = []
          this.total = 0
          this.pages = 1
          this.loading = false
        })
    },
    changePage(p) {
      if (p < 1 || (this.pages && p > this.pages)) return
      this.page = p
      this.load()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    goArticle(item) {
      if (item && item.articleId) {
        this.$router.push({ name: 'BBSArticleDetails', params: { articleId: item.articleId } })
      }
    },
    unfavorite(item) {
      this.postRequest('/article/favorite', { articleId: Number(item.articleId) }).then(() => {
        this.$message.success('已取消收藏')
        if (this.list.length === 1 && this.page > 1) this.page -= 1
        this.load()
      }).catch(() => {})
    },
  },
}
</script>
