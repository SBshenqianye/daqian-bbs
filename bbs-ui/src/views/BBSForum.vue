<template>
  <main class="min-h-screen bg-surface">
    <!-- Hero Section -->
    <section class="hero-gradient text-white py-16 px-page-margin-desktop">
      <div class="max-w-7xl mx-auto text-center">
        <h1 class="font-headline-lg text-[32px] md:text-[40px] leading-tight mb-4 tracking-tight">欢迎来到大千智荟创新创意交流论坛</h1>
        <p class="font-body-lg opacity-90 max-w-2xl mx-auto">汇聚国网智慧，激发创新灵感，共同打造卓越的企业级知识共享平台。</p>
      </div>
    </section>

    <!-- Main Layout Container -->
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-8">
      <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
        <!-- Left Sidebar: Categories + Featured Posts -->
        <aside class="md:col-span-2 space-y-4">
          <div class="bg-container border border-border rounded-lg p-2">
            <nav class="flex flex-col gap-1">
              <el-tooltip
                v-for="cat in categories"
                :key="cat.name"
                :content="cat.description"
                placement="right"
                effect="dark"
                :disabled="!cat.description"
              >
                <button
                  class="flex items-center gap-3 w-full p-3 rounded-lg transition-colors font-label-md"
                  :class="cat.active ? 'bg-surface-container-low text-primary-container' : 'text-on-surface-variant hover:bg-surface-container-low'"
                  @click="selectCategory(cat)"
                >
                  <span class="material-symbols-outlined" :class="{ 'filled-icon': cat.active }">{{ cat.icon }}</span>
                  <span>{{ cat.name }}</span>
                </button>
              </el-tooltip>
            </nav>
          </div>

          <!-- Featured Posts Sidebar (below labels) -->
          <div v-if="featuredSidebar.length > 0" class="bg-container border border-border rounded-lg p-3">
            <div class="flex items-center justify-between mb-2">
              <h2 class="font-headline-sm flex items-center gap-1 text-[15px]">
                <span class="material-symbols-outlined text-rank-gold text-[18px]">stars</span>
                精华帖
              </h2>
              <button
                class="text-[12px] text-primary hover:text-primary-container transition-colors flex items-center gap-0.5"
                @click="goToFeaturedArticles"
              >
                全部
                <span class="material-symbols-outlined text-[12px]">arrow_forward</span>
              </button>
            </div>
            <ul class="space-y-2">
              <li
                v-for="(item, index) in visibleFeaturedSidebar"
                :key="item.articleId || index"
                class="flex items-start gap-1.5 cursor-pointer group"
                @click="goToArticle(item)"
              >
                <span class="material-symbols-outlined text-rank-gold text-[13px] shrink-0">stars</span>
                <div class="flex-1 min-w-0">
                  <span class="text-[13px] text-on-surface group-hover:text-primary-container transition-colors line-clamp-1 block">{{ item.articleTitle }}</span>
                  <span v-if="item.labelName" class="text-[11px] text-outline truncate block">{{ item.labelName }}</span>
                  <span class="text-[11px] text-outline block">{{ item.time }}</span>
                </div>
              </li>
            </ul>
            <button
              v-if="featuredSidebar.length > 3"
              class="mt-1.5 w-full text-center text-[12px] text-primary hover:text-primary-container transition-colors py-0.5"
              @click="featuredSidebarExpand = !featuredSidebarExpand"
            >
              {{ featuredSidebarExpand ? '收起' : '展开更多' }}
              <span class="material-symbols-outlined text-[12px] align-middle">{{ featuredSidebarExpand ? 'expand_less' : 'expand_more' }}</span>
            </button>
          </div>
        </aside>

        <!-- Center Feed: Articles -->
        <section class="md:col-span-7 space-y-4">
          <!-- Featured Top: 置顶精华帖（最新3条） -->
          <div v-if="featuredTop.length > 0" class="bg-surface-container-low border border-outline-variant rounded-lg overflow-hidden">
            <div class="flex items-center gap-1.5 px-4 py-1.5 bg-primary-fixed-dim/20 border-b border-outline-variant">
              <span class="material-symbols-outlined text-rank-gold text-[15px]">stars</span>
              <span class="text-[13px] font-semibold text-on-surface">精华置顶</span>
              <span class="text-[10px] text-on-surface-variant ml-auto">最新 3 条</span>
            </div>
            <div class="divide-y divide-outline-variant/40">
              <article
                v-for="(article, index) in featuredTop"
                :key="'ft-' + (article.articleId || index)"
                class="px-4 py-1.5 hover:bg-primary-fixed-dim/10 transition-colors cursor-pointer"
                @click="goToArticle(article)"
              >
                <div class="flex items-center gap-2">
                  <span class="flex-shrink-0 w-4 h-4 flex items-center justify-center rounded-full text-[9px] font-bold"
                    :class="index === 0 ? 'bg-rank-gold text-white' : index === 1 ? 'bg-rank-silver text-white' : 'bg-rank-bronze text-white'">
                    {{ index + 1 }}
                  </span>
                  <h3 class="flex-1 text-[13px] font-medium text-on-surface hover:text-primary transition-colors truncate">{{ article.title }}</h3>
                  <span class="flex items-center gap-1.5 shrink-0 text-[10px] text-on-surface-variant">
                    <span class="truncate max-w-[70px]">{{ article.author }}</span>
                    <span class="text-outline-variant">·</span>
                    <span>{{ article.time }}</span>
                  </span>
                </div>
              </article>
            </div>
          </div>

          <!-- Regular Article List -->
          <article
            v-for="(article, index) in filteredArticles"
            :key="index"
            class="bg-container border border-border rounded-lg p-card-padding hover:shadow-sm transition-shadow cursor-pointer"
            @click="goToArticle(article)"
          >
            <div class="flex flex-col md:flex-row gap-6">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <!-- 精华帖标识 -->
                  <span v-if="article.isFeatured" class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium bg-rank-gold/10 text-rank-gold rounded-full">
                    <span class="material-symbols-outlined text-[12px]">stars</span>
                    精华
                  </span>
                  <!-- 标签标识 -->
                  <span v-if="getLabelName(article.labelId)" class="inline-flex items-center px-2 py-0.5 text-[11px] font-medium bg-primary/10 text-primary rounded-full">
                    {{ getLabelName(article.labelId) }}
                  </span>
                </div>
                <h3 class="font-headline-sm text-on-surface mb-2 hover:text-primary-container transition-colors">{{ article.title }}</h3>
                <p class="text-body-md text-secondary mb-4 line-clamp-2">{{ article.summary }}</p>
                <div class="flex items-center justify-between text-label-md text-outline">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-surface-container-highest flex items-center justify-center overflow-hidden">
                      <img :src="article.authorAvatar || require('@/assets/portrait.png')" alt="Avatar" class="w-full h-full object-cover">
                    </div>
                    <span class="truncate max-w-[150px]">作者：{{ article.author }}</span>
                    <span class="mx-1">•</span>
                    <span>{{ article.time }}</span>
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

          <!-- End of feed indicator -->
          <div class="flex flex-col items-center justify-center py-12 border-2 border-dashed border-outline-variant rounded-lg bg-surface-container-low opacity-60">
            <span class="material-symbols-outlined text-outline text-[48px] mb-2">inbox</span>
            <p class="font-label-md text-outline">已经到底啦，没有更多帖子了</p>
          </div>
        </section>

        <!-- Right Sidebar: Hot Topics + Featured -->
        <aside class="md:col-span-3 space-y-4 sticky top-20 self-start">
          <!-- Hot Topics -->
          <div class="bg-container border border-border rounded-lg p-card-padding">
            <div class="flex items-center justify-between mb-4">
              <h2 class="font-headline-sm flex items-center gap-2">
                <span class="material-symbols-outlined text-error filled-icon">local_fire_department</span>
                热榜
              </h2>
            </div>
            <ul class="space-y-4">
              <li
                v-for="(topic, index) in visibleHotTopics"
                :key="topic.articleId || index"
                class="flex items-start gap-3 group cursor-pointer"
                @click="goToHotTopic(topic)"
              >
                <span
                  class="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full font-bold text-[12px]"
                  :class="getRankClass(index + 1)"
                >{{ index + 1 }}</span>
                <span class="font-body-md text-on-surface group-hover:text-primary-container transition-colors line-clamp-1">{{ topic.articleTitle }}</span>
              </li>
            </ul>
            <button
              v-if="hotTopics.length > 10"
              class="mt-2 w-full text-center text-label-md text-primary hover:text-primary-container transition-colors py-1"
              @click="hotTopicsExpand = !hotTopicsExpand"
            >
              {{ hotTopicsExpand ? '收起' : '展开更多' }}
              <span class="material-symbols-outlined text-[14px] align-middle">{{ hotTopicsExpand ? 'expand_less' : 'expand_more' }}</span>
            </button>
          </div>

          <!-- Side Promotion Card -->
          <div class="bg-primary-container text-white rounded-lg p-card-padding relative overflow-hidden">
            <div class="relative z-10">
              <h4 class="font-headline-sm mb-1">智荟专题</h4>
              <p class="text-body-md opacity-80">探索最前沿的电力行业技术专题。</p>
            </div>
            <span class="material-symbols-outlined absolute -bottom-4 -right-4 text-white/20 text-[100px] select-none pointer-events-none">lightbulb</span>
          </div>
        </aside>
      </div>
    </div>
  </main>
</template>

<script>
import { normalizeFileUrl } from '@/utils/utils'

export default {
  name: 'BBSForum',
  data() {
    return {
      loading: false,
      categories: [],
      articles: [],
      hotTopics: [],
      hotTopicsExpand: false,
      featuredSidebar: [],
      featuredSidebarExpand: false,
      featuredTop: [],
      filteredLabelId: null,
    }
  },
  mounted() {
    const kw = this.$route.query.keywords
    this.fetchArticles(kw || '')
    this.fetchHotTopics()
    this.fetchFeaturedTop()
    this.fetchFeaturedSidebar()
    this.loadLabels()
    this.$bus && this.$bus.$on('forumSearch', this.handleForumSearch)
  },
  beforeDestroy() {
    this.$bus && this.$bus.$off('forumSearch', this.handleForumSearch)
  },
  watch: {
    '$route.query.keywords': function (kw) {
      if (kw) {
        this.fetchArticles(kw)
      }
    },
  },
  computed: {
    filteredArticles() {
      if (!this.filteredLabelId) return this.articles
      return this.articles.filter(a => String(a.labelId) === String(this.filteredLabelId))
    },
    visibleHotTopics() {
      return this.hotTopicsExpand ? this.hotTopics : this.hotTopics.slice(0, 10)
    },
    visibleFeaturedSidebar() {
      return this.featuredSidebarExpand ? this.featuredSidebar : this.featuredSidebar.slice(0, 3)
    },
  },
  methods: {
    fetchArticles(keywords = '') {
      this.loading = true
      this.getRequest(`/common/article/getArticle?keywords=${encodeURIComponent(keywords)}`).then(resp => {
        this.loading = false
        const list = Array.isArray(resp) ? resp : (resp && resp.data && Array.isArray(resp.data) ? resp.data : [])
        this.articles = list.map(a => ({
          articleId: a.articleId,
          title: a.articleTitle || '',
          summary: a.articleSummary || '',
          author: a.articleAuthor || '',
          userId: a.userId,
          authorAvatar: normalizeFileUrl(a.portrait || ''),
          isFeatured: a.isFeatured === 1 || a.isFeatured === '1',
          labelId: a.articleLabelId || null,
          time: a.createTime || a.articleCreateTime || '',
          views: a.articleViewNum || 0,
          comments: a.commentNum || 0,
          likes: a.articleGoodNum || 0,
          cover: normalizeFileUrl(a.articleImage || null),
        }))
      }).catch(err => {
        console.warn('[BBSForum] fetchArticles', err)
        this.loading = false
        this.articles = []
      })
    },
    fetchHotTopics() {
      this.getRequest('/common/article/getHot').then(resp => {
        const list = Array.isArray(resp) ? resp : (resp && resp.data && Array.isArray(resp.data) ? resp.data : [])
        this.hotTopics = list.map(h => ({
          articleId: h.articleId,
          articleTitle: h.articleTitle || '',
        })).filter(h => h.articleId)
      }).catch(err => {
        console.warn('[BBSForum] fetchHotTopics', err)
        this.hotTopics = []
      })
    },
    fetchFeaturedTop() {
      this.getRequest('/common/article/getFeaturedTop').then(resp => {
        const data = resp && resp.obj
        const list = Array.isArray(data) ? data : []
        this.featuredTop = list.map(a => ({
          articleId: a.articleId,
          title: a.articleTitle || '',
          author: a.articleAuthor || '',
          time: a.createTime || '',
          views: a.articleViewNum || 0,
          labelName: a.articleLabelName || '',
          articleLabelName: a.articleLabelName || '',
        })).filter(a => a.articleId)
      }).catch(err => {
        console.warn('[BBSForum] fetchFeaturedTop', err)
        this.featuredTop = []
      })
    },
    fetchFeaturedSidebar() {
      this.getRequest('/common/article/getFeatured?page=1&size=10').then(resp => {
        if (resp && resp.obj) {
          const list = Array.isArray(resp.obj.list) ? resp.obj.list : []
          this.featuredSidebar = list.map(a => ({
            articleId: a.articleId,
            articleTitle: a.articleTitle || '',
            labelName: a.articleLabelName || '',
            time: a.createTime || '',
          })).filter(a => a.articleId)
        } else {
          this.featuredSidebar = []
        }
      }).catch(err => {
        console.warn('[BBSForum] fetchFeaturedSidebar', err)
        this.featuredSidebar = []
      })
    },
    loadLabels() {
      this.getRequest('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.categories = resp.map((l, i) => ({
            name: l.labelName,
            icon: l.icon || 'bookmark',
            labelId: l.labelId,
            active: i === 0,
            description: l.description || '',
          }))
        }
      }).catch(err => {
        console.warn('[BBSForum] loadLabels', err)
        this.categories = []
      })
    },
    selectCategory(cat) {
      if (cat.active) {
        cat.active = false
        this.filteredLabelId = null
        return
      }
      this.categories.forEach(c => { c.active = false })
      cat.active = true
      this.filteredLabelId = cat.labelId || null
    },
    goToArticle(article) {
      if (article.articleId) {
        this.$router.push({ name: 'BBSArticleDetails', params: { articleId: article.articleId } })
      }
    },
    goToHotTopic(topic) {
      if (topic.articleId) {
        this.$router.push({ name: 'BBSArticleDetails', params: { articleId: topic.articleId } })
      }
    },
    goToFeaturedArticles() {
      this.$router.push({ name: 'BBSFeaturedArticles' })
    },
    getLabelName(labelId) {
      if (!labelId) return ''
      const cat = this.categories.find(c => String(c.labelId) === String(labelId))
      return cat ? cat.name : ''
    },
    handleForumSearch(keywords) {
      this.fetchArticles(keywords || '')
    },
    getRankClass(rank) {
      if (rank === 1) return 'rank-1'
      if (rank === 2) return 'rank-2'
      if (rank === 3) return 'rank-3'
      return 'rank-other'
    },
  },
}
</script>
