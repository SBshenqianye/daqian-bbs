<template>
  <main class="ui-page">
    <PageHeader
      title="精华帖专区"
      subtitle="汇聚社区最优质的精华内容，发现值得深度阅读的好帖子"
      icon="stars"
      tone="gold"
    >
      <span class="ui-chip ui-chip-gold">共 {{ pagination.total }} 篇</span>
    </PageHeader>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <!-- Empty -->
    <div v-else-if="articles.length === 0" class="ui-empty">
      <span class="material-symbols-outlined">auto_awesome</span>
      <p class="ui-empty-title">暂无精华帖</p>
      <p class="ui-empty-desc">管理员还没有设置任何精华帖</p>
    </div>

    <!-- Article List：与论坛首页共用 PostListItem -->
    <template v-else>
      <div class="space-y-2.5">
        <PostListItem
          v-for="(article, index) in articles"
          :key="article.articleId || index"
          :article="postItem(article)"
          @click="goToArticle"
        />
      </div>

      <!-- Pagination -->
      <div v-if="pagination.pages > 1" class="ui-pager mt-4">
        <button class="ui-pager-btn" :disabled="pagination.page <= 1" @click="changePage(pagination.page - 1)">
          <span class="material-symbols-outlined text-[16px]">chevron_left</span>
        </button>
        <button
          v-for="p in pageNumbers"
          :key="p"
          class="ui-pager-btn"
          :class="{ 'is-active': p === pagination.page }"
          :disabled="p === '...'"
          @click="changePage(p)"
        >{{ p }}</button>
        <button class="ui-pager-btn" :disabled="pagination.page >= pagination.pages" @click="changePage(pagination.page + 1)">
          <span class="material-symbols-outlined text-[16px]">chevron_right</span>
        </button>
        <span class="text-[11.5px] text-outline ml-1 tabnum">{{ pagination.page }} / {{ pagination.pages }}</span>
      </div>

      <!-- End of list -->
      <div class="ui-list-end mt-2">
        <span class="text-[11px] text-outline tracking-wide">没有更多精华帖了</span>
      </div>
    </template>
  </main>
</template>

<script>
import { normalizeFileUrl } from '@/utils/utils'
import { dateStr } from '@/utils/time'
import PageHeader from '@/components/PageHeader.vue'
import PostListItem from '@/components/PostListItem.vue'

export default {
  name: 'BBSFeaturedArticles',
  components: { PageHeader, PostListItem },
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
    postItem(article) {
      return {
        articleId: article.articleId,
        title: article.articleTitle,
        summary: article.articleSummary,
        author: article.author,
        userId: article.userId,
        authorAvatar: article.authorAvatar,
        authorOrgName: article.authorOrgName,
        authorOrgNameFull: article.authorOrgNameFull,
        authorLevelIndex: article.authorLevelIndex,
        time: this.formatTime(article.time),
        views: article.views,
        comments: article.comments,
        likes: article.likes,
        labelName: article.articleLabelName,
        isFeatured: true,
        cover: article.cover,
      }
    },
    fetchFeatured() {
      this.loading = true
      this.getRequest(`/common/article/getFeatured?page=${this.pagination.page}&size=${this.pagination.size}`).then(resp => {
        this.loading = false
        if (resp && resp.obj) {
          const list = Array.isArray(resp.obj.list) ? resp.obj.list : []
          this.articles = list.map(a => ({
            articleId: a.articleId,
            articleTitle: a.articleTitle || '',
            articleSummary: a.articleSummary || '',
            author: a.articleAuthor || '',
            userId: a.userId,
            authorAvatar: normalizeFileUrl(a.portrait || ''),
            authorOrgName: a.authorOrgName || '',
            authorOrgNameFull: a.authorOrgNameFull || '',
            authorLevelIndex: a.authorLevelIndex || null,
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