<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <!-- Page Title -->
    <header class="mb-10">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的发布</h1>
    </header>

    <!-- Article List Container -->
    <div v-if="articles.length > 0" class="space-y-gutter">
      <div
        v-for="(article, index) in articles"
        :key="index"
        class="bg-container border border-outline-variant rounded-lg p-card-padding flex flex-col md:flex-row gap-6 relative group card-shadow hover:border-primary-container transition-colors cursor-pointer card-clickable"
        :style="{ transition: 'all 0.3s ease' }"
        @click="goToArticle(article)"
      >
        <div class="flex-grow flex flex-col justify-between min-w-0">
          <div>
            <h2 class="font-headline-sm text-headline-sm text-on-surface mb-2 truncate">{{ article.title }}</h2>
            <p class="font-body-md text-body-md text-on-surface-variant mb-6 line-clamp-2" style="white-space: pre-line">{{ article.summary }}</p>
          </div>
          <!-- Metadata Bar -->
          <div class="flex items-center flex-wrap gap-4 font-label-md text-label-md text-on-surface-variant">
            <div class="flex items-center gap-2">
              <bbs-user-badge :avatar="article.authorAvatar" :nickname="article.author" :org-name="article.authorOrgName" size="sm" layout="inline" />
            </div>
            <span class="text-outline">•</span>
            <span>{{ article.time }}</span>
            <div class="flex items-center gap-6 ml-auto">
              <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">visibility</span> {{ article.views }}</span>
              <span class="flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">chat_bubble</span> {{ article.comments }}</span>
            </div>
          </div>
        </div>
        <!-- Optional Cover Image -->
        <div v-if="article.cover" class="w-full md:w-48 h-32 flex-shrink-0 rounded overflow-hidden">
          <img alt="Article Cover" class="w-full h-full object-cover" :src="article.cover">
        </div>
        <!-- Action Controls -->
        <div class="flex md:flex-col gap-2 md:justify-center border-l md:border-l-0 md:pl-0 pl-4 border-outline-variant">
          <button
            class="flex items-center justify-center gap-1 px-4 py-2 rounded font-label-md text-label-md border border-on-tertiary-fixed-variant text-on-tertiary-fixed-variant bg-tertiary-fixed hover:bg-tertiary-container hover:text-white transition-all active:scale-95"
            @click.stop="handleDelete(index)"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
            <span>删除</span>
          </button>
          <button
            class="flex items-center justify-center gap-1 px-4 py-2 rounded font-label-md text-label-md border border-outline text-secondary hover:bg-secondary-container hover:border-secondary transition-all active:scale-95"
            @click.stop="handleEdit(article)"
          >
            <span class="material-symbols-outlined text-[18px]">edit_note</span>
            <span>修改</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
      <span class="material-symbols-outlined text-6xl mb-4 opacity-20">inventory_2</span>
      <p class="font-body-lg text-body-lg">暂无发布内容</p>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'
import { getUser } from '@/utils/auth'
import { normalizeFileUrl, friendlyTime } from '@/utils/utils'
import BBSUserBadge from '@/components/BBSUserBadge'

export default {
  name: 'BBSStat',
  components: { BBSUserBadge },
  data() {
    return {
      loading: false,
      articles: [],
    }
  },
  mounted() {
    this.fetchMyArticles()
  },
  methods: {
    fetchMyArticles() {
      const user = getUser()
      if (!user) {
        this.articles = []
        this.loading = false
        return
      }
      const userId = user.id
      const userAvatar = normalizeFileUrl(user.portrait || '')
      this.loading = true
      this.getRequest(`/article/getArticleByUserId?userId=${userId}`).then(resp => {
        this.loading = false
        const list = Array.isArray(resp) ? resp : (resp && resp.data && Array.isArray(resp.data) ? resp.data : [])
        this.articles = list.map(a => ({
          articleId: a.articleId,
          title: a.articleTitle || '',
          summary: a.articleSummary || '',
          author: a.articleAuthor || '',
          authorAvatar: userAvatar,
          authorOrgName: user.orgName || '',
          time: friendlyTime(a.createTime || a.articleCreateTime || ''),
          views: a.articleViewNum || 0,
          comments: a.commentNum ?? a.comment_num ?? a.articleCommentNum ?? 0,
          likes: a.articleGoodNum || 0,
          cover: normalizeFileUrl(a.articleImage || null),
        }))
      }).catch(err => {
        console.warn('[BBSStat] fetchMyArticles', err)
        this.loading = false
        this.articles = []
      })
    },
    goToArticle(article) {
      if (article.articleId) {
        this.$router.push({ name: 'BBSArticleDetails', params: { articleId: article.articleId } })
      }
    },
    handleDelete(index) {
      const article = this.articles[index]
      if (!article || !article.articleId) return
      this.$confirm('确定删除该文章？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.postRequest('/article/deleteArticleByArticleId', { articleId: article.articleId }).then(resp => {
          if (resp) {
            this.fetchMyArticles()
          }
        })
      }).catch(() => {})
    },
    handleEdit(article) {
      if (article.articleId) {
        this.$router.push({ name: 'BBSArticleWrite', query: { articleId: article.articleId } })
      }
    },
  },
}
</script>
