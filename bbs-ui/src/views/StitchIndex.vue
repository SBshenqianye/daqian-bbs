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
        <!-- Left Sidebar: Categories -->
        <aside class="md:col-span-2 space-y-4">
          <div class="bg-container border border-border rounded-lg p-2 sticky top-8">
            <nav class="flex flex-col gap-1">
              <button
                v-for="cat in categories"
                :key="cat.name"
                class="flex items-center gap-3 w-full p-3 rounded-lg transition-colors font-label-md"
                :class="cat.active ? 'bg-surface-container-low text-primary-container' : 'text-on-surface-variant hover:bg-surface-container-low'"
                @click="selectCategory(cat)"
              >
                <span class="material-symbols-outlined" :class="{ 'filled-icon': cat.active }">{{ cat.icon }}</span>
                <span>{{ cat.name }}</span>
              </button>
            </nav>
          </div>
        </aside>

        <!-- Center Feed: Articles -->
        <section class="md:col-span-7 space-y-4">
          <article
            v-for="(article, index) in filteredArticles"
            :key="index"
            class="bg-container border border-border rounded-lg p-card-padding hover:shadow-sm transition-shadow cursor-pointer"
            @click="goToArticle(article)"
          >
            <div class="flex flex-col md:flex-row gap-6">
              <div class="flex-1">
                <h3 class="font-headline-sm text-on-surface mb-2 hover:text-primary-container transition-colors">{{ article.title }}</h3>
                <p class="text-body-md text-secondary mb-4 line-clamp-2">{{ article.summary }}</p>
                <div class="flex items-center justify-between text-label-md text-outline">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-surface-container-highest flex items-center justify-center overflow-hidden">
                      <span class="material-symbols-outlined text-[14px]">person</span>
                    </div>
                    <span class="truncate max-w-[150px]">作者：{{ article.author }}</span>
                    <span class="mx-1">•</span>
                    <span>{{ article.time }}</span>
                  </div>
                  <div class="flex items-center gap-4">
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">visibility</span> {{ article.views }}</span>
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">chat_bubble</span> {{ article.comments }}</span>
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">thumb_up</span> {{ article.likes }}</span>
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

        <!-- Right Sidebar: Hot Topics -->
        <aside class="md:col-span-3 space-y-4">
          <div class="bg-container border border-border rounded-lg p-card-padding">
            <div class="flex items-center justify-between mb-4">
              <h2 class="font-headline-sm flex items-center gap-2">
                <span class="material-symbols-outlined text-error filled-icon">local_fire_department</span>
                热榜
              </h2>
            </div>
            <ul class="space-y-4">
              <li
                v-for="(topic, index) in hotTopics"
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
export default {
  name: 'StitchIndex',
  data() {
    return {
      loading: false,
      categories: [],
      articles: [],
      hotTopics: [],
      filteredLabelId: null,
      imageBase: process.env.VUE_APP_BBS_BASE_FILE || '',
    }
  },
  mounted() {
    this.fetchArticles()
    this.fetchHotTopics()
    this.loadLabels()
    this.$bus && this.$bus.$on('forumSearch', this.handleForumSearch)
  },
  beforeDestroy() {
    this.$bus && this.$bus.$off('forumSearch', this.handleForumSearch)
  },
  computed: {
    filteredArticles() {
      if (!this.filteredLabelId) return this.articles
      return this.articles.filter(a => String(a.labelId) === String(this.filteredLabelId))
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
          time: a.createTime || a.articleCreateTime || '',
          views: a.articleViewNum || 0,
          comments: a.articleCommentNum || a.commentNum || 0,
          likes: a.articleGoodNum || 0,
          labelId: a.articleLabelId || null,
          cover: a.articleImage ? this.imageBase + a.articleImage : null,
        }))
      }).catch(() => {
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
      }).catch(() => {
        this.hotTopics = []
      })
    },
    getLabelIcon(labelName) {
      const iconMap = {
        '技术交流': 'thumb_up',
        '求助问答': 'help',
        '资源共享': 'folder_open',
      }
      return iconMap[labelName] || 'bookmark'
    },
    loadLabels() {
      this.getRequest('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.categories = resp.map((l, i) => ({
            name: l.labelName,
            icon: this.getLabelIcon(l.labelName),
            labelId: l.labelId,
            active: i === 0,
          }))
          // 不默认过滤，让用户可以看到全部文章
        }
      }).catch(() => {
        this.categories = []
      })
    },
    selectCategory(cat) {
      // 再次点击已激活的类别 → 取消过滤，显示全部
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
        this.$router.push({ name: 'stitchArticleDetails', params: { articleId: article.articleId } })
      }
    },
    goToHotTopic(topic) {
      if (topic.articleId) {
        this.$router.push({ name: 'stitchArticleDetails', params: { articleId: topic.articleId } })
      }
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
