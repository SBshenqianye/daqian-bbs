<template>
  <main class="ui-page max-w-5xl">
    <PageHeader
      title="我的发布"
      :subtitle="articles.length ? '共 ' + articles.length + ' 篇内容，按发布时间倒序' : '你发布的帖子会集中在这里'"
      icon="edit_square"
    >
      <router-link to="/write" class="ui-btn ui-btn-sm ui-btn-primary no-underline">
        <span class="material-symbols-outlined text-[16px]">add</span>写新帖
      </router-link>
    </PageHeader>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <!-- Article List：与论坛/收藏共用同一张卡片 -->
    <div v-else-if="articles.length > 0" class="space-y-2.5">
      <PostListItem
        v-for="(article, index) in articles"
        :key="article.articleId || index"
        :article="postItem(article)"
        :show-likes="false"
        @click="goToArticle"
      >
        <template #actions>
          <button class="ui-icon-btn" title="修改" @click.stop="handleEdit(article)">
            <span class="material-symbols-outlined">edit</span>
          </button>
          <button class="ui-icon-btn is-danger" title="删除" @click.stop="handleDelete(article)">
            <span class="material-symbols-outlined">delete</span>
          </button>
        </template>
      </PostListItem>
    </div>

    <!-- Empty State -->
    <div v-else class="ui-empty">
      <span class="material-symbols-outlined">inventory_2</span>
      <p class="ui-empty-title">暂无发布内容</p>
      <p class="ui-empty-desc">写下第一篇帖子，和同事聊聊你的想法</p>
      <router-link to="/write" class="mt-3 px-3.5 py-1.5 text-[12px] rounded-full bg-primary/10 text-primary hover:bg-primary/20 transition-colors no-underline">去发帖</router-link>
    </div>
  </main>
</template>

<script>
import { getUser } from '@/utils/auth'
import { normalizeFileUrl, friendlyTime } from '@/utils/utils'
import PageHeader from '@/components/PageHeader.vue'
import PostListItem from '@/components/PostListItem.vue'
import { handleResponse } from '../../../shared/feedback'

export default {
  name: 'BBSStat',
  components: { PageHeader, PostListItem },
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
    postItem(article) {
      return {
        ...article,
        labelName: article.labelName || '',
        isFeatured: article.isFeatured === true,
      }
    },
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
          authorOrgNameFull: user.orgNameFull || '',
          authorLevelIndex: user.levelIndex || null,
          time: friendlyTime(a.createTime || a.articleCreateTime || ''),
          views: a.articleViewNum || 0,
          comments: a.commentNum ?? a.comment_num ?? a.articleCommentNum ?? 0,
          likes: a.articleGoodNum || 0,
          labelName: a.articleLabelName || '',
          isFeatured: a.isFeatured === 1 || a.isFeatured === '1',
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
    handleDelete(article) {
      if (!article || !article.articleId) return
      this.$confirm('确定删除该文章？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.postRequest('/article/deleteArticleByArticleId', { articleId: article.articleId }).then(resp => {
          handleResponse(resp, {
            successMsg: '删除成功',
            onSuccess: () => { this.fetchMyArticles() },
          })
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
