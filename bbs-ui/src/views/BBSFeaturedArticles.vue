<template>
  <main class="min-h-screen bg-surface">
    <!-- Header -->
    <section class="bg-gradient-to-r from-rank-gold/90 to-amber-600 text-white py-16 px-page-margin-desktop">
      <div class="max-w-7xl mx-auto">
        <div class="flex items-center gap-3 mb-3">
          <span class="material-symbols-outlined text-[32px]">stars</span>
          <h1 class="font-headline-lg text-[28px] md:text-[36px] leading-tight tracking-tight">精华帖专区</h1>
        </div>
        <p class="font-body-lg opacity-90 max-w-2xl">汇聚社区最优质的精华内容，发现值得深度阅读的好帖子</p>
      </div>
    </section>

    <!-- Content -->
    <div class="max-w-5xl mx-auto px-page-margin-desktop py-8">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <span class="inline-block w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- Empty -->
      <div v-else-if="articles.length === 0" class="flex flex-col items-center justify-center py-20 text-on-surface-variant">
        <span class="material-symbols-outlined text-[64px] opacity-20">stars</span>
        <p class="text-body-lg mt-4">暂无精华帖</p>
        <p class="text-body-md text-outline mt-1">管理员还没有设置任何精华帖</p>
      </div>

      <!-- Article List -->
      <template v-else>
        <div class="space-y-4">
          <article
            v-for="(article, index) in articles"
            :key="article.articleId || index"
            class="bg-container border border-border rounded-lg p-card-padding hover:shadow-sm transition-shadow cursor-pointer"
            @click="goToArticle(article)"
          >
            <div class="flex items-start gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-2">
                  <!-- 精华帖标识 -->
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium bg-rank-gold/10 text-rank-gold rounded-full">
                    <span class="material-symbols-outlined text-[12px]">stars</span>
                    精华
                  </span>
                  <!-- 标签标识 -->
                  <span v-if="article.articleLabelName" class="inline-flex items-center px-2 py-0.5 text-[11px] font-medium bg-primary/10 text-primary rounded-full">
                    {{ article.articleLabelName }}
                  </span>
                </div>
                <h3 class="font-headline-sm text-headline-sm text-on-surface mb-2 hover:text-primary-container transition-colors">{{ article.articleTitle }}</h3>
                <p class="text-body-md text-secondary mb-4 line-clamp-2" style="white-space: pre-line">{{ article.articleSummary }}</p>
                <div class="flex items-center justify-between text-label-md text-outline">
                  <div class="flex items-center gap-2">
                    <bbs-user-badge :avatar="article.authorAvatar" :nickname="article.author" :org-name="article.authorOrgName" size="sm" layout="inline" />
                    <span class="mx-1">•</span>
                    <span>{{ formatTime(article.time) }}</span>
                  </div>
                  <div class="flex items-center gap-4">
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">visibility</span> {{ article.views }}</span>
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">chat_bubble</span> {{ article.comments }}</span>
                  </div>
                </div>
              </div>
              <div v-if="article.cover" class="w-full md:w-32 h-24 rounded-lg bg-surface-container overflow-hidden shrink-0 border border-outline-variant">
                <img alt="Article Cover" class="w-full h-full object-cover" :src="article.cover">
              </div>
            </div>
          </article>
        </div>

        <!-- Pagination -->
        <div v-if="pagination.pages > 1" class="flex items-center justify-between mt-8">
          <span class="text-body-md text-on-surface-variant">
            共 {{ pagination.total }} 条，第 {{ pagination.page }}/{{ pagination.pages }} 页
          </span>
          <div class="flex items-center gap-2">
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
              :disabled="pagination.page <= 1"
              @click="changePage(pagination.page - 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <button
              v-for="p in pageNumbers"
              :key="p"
              class="min-w-[36px] h-9 px-2 flex items-center justify-center rounded-lg text-[13px] font-medium transition-all"
              :class="p === pagination.page ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-variant'"
              @click="changePage(p)"
            >{{ p }}</button>
            <button
              class="w-9 h-9 flex items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
              :disabled="pagination.page >= pagination.pages"
              @click="changePage(pagination.page + 1)"
            >
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>

        <!-- End of list -->
        <div v-if="articles.length > 0" class="flex flex-col items-center justify-center py-12 border-2 border-dashed border-outline-variant rounded-lg bg-surface-container-low opacity-60 mt-6">
          <span class="material-symbols-outlined text-outline text-[48px] mb-2">stars</span>
          <p class="font-label-md text-outline">已经到底啦，没有更多精华帖了</p>
        </div>
      </template>
    </div>
  </main>
</template>

<script>
import { normalizeFileUrl } from '@/utils/utils'
import { dateStr } from '@/utils/time'
import BBSUserBadge from '@/components/BBSUserBadge'

export default {
  name: 'BBSFeaturedArticles',
  components: { BBSUserBadge },
  data() {
    return {
      loading: false,
      articles: [],
      pagination: {
        total: 0,
        page: 1,
        size: 10,
        pages: 1,
      },
    }
  },
  computed: {
    pageNumbers() {
      const pages = this.pagination.pages
      const current = this.pagination.page
      if (pages <= 7) return Array.from({ length: pages }, (_, i) => i + 1)
      const nums = []
      let start = Math.max(1, current - 2)
      let end = Math.min(pages, current + 2)
      if (start > 2) { nums.push(1, '...') }
      for (let i = start; i <= end; i++) nums.push(i)
      if (end < pages - 1) { nums.push('...', pages) }
      else if (end === pages - 1) { nums.push(pages) }
      return nums
    },
  },
  mounted() {
    this.fetchFeatured()
  },
  methods: {
    formatTime(val) {
      return dateStr(val)
    },
    fetchFeatured() {
      this.loading = true
      this.getRequest(`/common/article/getFeatured?page=${this.pagination.page}&size=${this.pagination.size}`).then(resp => {
        this.loading = false
        if (resp && resp.obj) {
          const list = Array.isArray(resp.obj.list) ? resp.obj.list : []
          this.articles = list.map(a => ({
            articleId: a.articleId,
            title: a.articleTitle || '',
            summary: a.articleSummary || '',
            author: a.articleAuthor || '',
            userId: a.userId,
            authorAvatar: normalizeFileUrl(a.portrait || ''),
            authorOrgName: a.authorOrgName || '',
            time: a.createTime || '',
            views: a.articleViewNum || 0,
            comments: a.commentNum ?? a.comment_num ?? 0,
            likes: a.articleGoodNum || 0,
            articleLabelName: a.articleLabelName || '',
            cover: normalizeFileUrl(a.articleImage || null),
          }))
          this.pagination.total = resp.obj.total || 0
          this.pagination.pages = resp.obj.pages || 1
        } else {
          this.articles = []
          this.pagination.total = 0
          this.pagination.pages = 1
        }
      }).catch(err => {
        console.warn('[BBSFeaturedArticles] fetch', err)
        this.loading = false
        this.articles = []
      })
    },
    changePage(page) {
      if (page < 1 || page > this.pagination.pages) return
      this.pagination.page = page
      this.fetchFeatured()
    },
    goToArticle(article) {
      if (article.articleId) {
        this.$router.push({ name: 'BBSArticleDetails', params: { articleId: article.articleId } })
      }
    },
  },
}
</script>
