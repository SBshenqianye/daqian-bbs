<template>
  <main class="ui-page">
    <!-- 精华页视觉头图：深蓝品牌底 + 金色勋章，保留“亮眼”但不做整块金色 -->
    <section class="featured-hero relative overflow-hidden rounded-2xl mb-5">
      <span class="featured-hero__watermark material-symbols-outlined" aria-hidden="true">stars</span>

      <div class="relative z-10 px-6 md:px-9 py-7 md:py-9 flex items-center justify-between gap-6">
        <div class="max-w-xl min-w-0">
          <div class="flex items-center gap-2 flex-wrap mb-3">
            <span class="hero-kicker">
              <span class="material-symbols-outlined text-[14px] filled-icon">stars</span>
              编辑精选 · CURATED
            </span>
            <span class="hero-count">共 {{ pagination.total }} 篇</span>
          </div>

          <h1 class="text-[26px] md:text-[34px] font-bold leading-tight tracking-tight text-white">
            精华帖专区
          </h1>
          <p class="mt-2 text-[13px] md:text-[14px] leading-relaxed text-white/70 max-w-lg">
            汇聚社区最优质的精华内容，发现值得深度阅读的好帖子。
          </p>

          <div class="mt-4 flex flex-wrap items-center gap-2">
            <span class="hero-tag"><span class="material-symbols-outlined">auto_awesome</span>深度阅读</span>
            <span class="hero-tag"><span class="material-symbols-outlined">lightbulb</span>经验沉淀</span>
            <span class="hero-tag"><span class="material-symbols-outlined">rocket_launch</span>创新灵感</span>
          </div>
        </div>

        <div class="featured-seal-wrap hidden sm:flex">
          <span class="featured-seal-ring"></span>
          <span class="featured-seal-ring featured-seal-ring--2"></span>
          <span class="featured-dot featured-dot--1"></span>
          <span class="featured-dot featured-dot--2"></span>
          <span class="featured-seal">
            <span class="material-symbols-outlined text-[34px] filled-icon">workspace_premium</span>
            <span class="featured-seal__text">FEATURED</span>
          </span>
        </div>
      </div>
    </section>

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
import PostListItem from '@/components/PostListItem.vue'

export default {
  name: 'BBSFeaturedArticles',
  components: { PostListItem },
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
          authorIconSvg: article.authorIconSvg || '',
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
          authorIconSvg: a.authorIconSvg || '',
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

<style scoped>
/* 精华页头图：深蓝品牌底 + 金色光晕 + 勋章，保持亮眼但不过度使用金色 */
.featured-hero {
  background:
    radial-gradient(120% 150% at 88% 0%, rgba(255, 215, 106, 0.26) 0%, rgba(255, 215, 106, 0) 46%),
    radial-gradient(95% 130% at 8% 100%, rgba(58, 99, 240, 0.58) 0%, rgba(58, 99, 240, 0) 60%),
    linear-gradient(120deg, #061a3d 0%, #0a2f73 50%, #0052d9 100%);
  box-shadow: 0 18px 42px -26px rgba(0, 52, 140, 0.65);
  min-height: 196px;
}

.featured-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.18) 1px, transparent 1px);
  background-size: 22px 22px;
  opacity: 0.2;
  -webkit-mask-image: linear-gradient(90deg, transparent 0%, #000 55%, #000 100%);
  mask-image: linear-gradient(90deg, transparent 0%, #000 55%, #000 100%);
  pointer-events: none;
}

.featured-hero__watermark {
  position: absolute;
  right: -24px;
  bottom: -86px;
  font-size: 300px;
  line-height: 1;
  color: #ffffff;
  opacity: 0.05;
  transform: rotate(-8deg);
  pointer-events: none;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.10);
  color: #ffe6a3;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.03em;
  backdrop-filter: blur(4px);
}

.hero-count {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.62);
  font-size: 10.5px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.72);
  font-size: 11px;
}

.hero-tag .material-symbols-outlined {
  font-size: 13px;
  color: #ffd76a;
}

.featured-seal-wrap {
  position: relative;
  width: 132px;
  height: 132px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.featured-seal-ring {
  position: absolute;
  inset: 8px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 9999px;
}

.featured-seal-ring--2 {
  inset: -2px;
  border-style: dashed;
  border-color: rgba(255, 215, 106, 0.34);
}

.featured-seal {
  position: relative;
  z-index: 2;
  width: 94px;
  height: 94px;
  border-radius: 9999px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #452a00;
  background: linear-gradient(145deg, #ffefb0 0%, #ffd045 42%, #e79b17 100%);
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.featured-seal__text {
  margin-top: 2px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: rgba(69, 42, 0, 0.78);
}

.featured-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: #ffd76a;
  box-shadow: 0 0 12px rgba(255, 215, 106, 0.9);
}

.featured-dot--1 {
  top: 10px;
  right: 22px;
}

.featured-dot--2 {
  bottom: 16px;
  left: 6px;
}

@media (max-width: 639px) {
  .featured-hero {
    min-height: 0;
  }
  .featured-hero__watermark {
    font-size: 220px;
    bottom: -70px;
  }
}
</style>
