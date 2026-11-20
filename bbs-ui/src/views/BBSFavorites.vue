<template>
  <main :class="embedded ? '' : 'ui-page max-w-4xl'">
    <PageHeader v-if="!embedded" title="我的收藏" :subtitle="'共 ' + total + ' 篇帖子'" icon="bookmark">
      <button class="ui-btn ui-btn-sm ui-btn-secondary" @click="$router.push('/userinfo')">
        <span class="material-symbols-outlined text-[16px]">arrow_back</span>返回个人中心
      </button>
    </PageHeader>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-else>
      <div v-if="list.length" class="space-y-2.5">
        <PostListItem
          v-for="item in list"
          :key="item.articleId"
          :article="postItem(item)"
          :show-likes="false"
          @click="goArticle"
        >
          <template #actions>
            <button class="ui-icon-btn is-danger" title="取消收藏" @click.stop="unfavorite(item)">
              <span class="material-symbols-outlined">bookmark_remove</span>
            </button>
          </template>
        </PostListItem>
      </div>

      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">bookmark_border</span>
        <p class="ui-empty-title">还没有收藏任何帖子</p>
        <p class="ui-empty-desc">在帖子详情页点击「收藏」即可加入这里</p>
        <button class="mt-3 px-3.5 py-1.5 rounded-full bg-primary/10 text-primary text-[12px] hover:bg-primary/20" @click="$router.push('/forum')">去逛逛论坛</button>
      </div>

      <div v-if="pages > 1" class="ui-pager mt-4">
        <button class="ui-pager-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span class="text-[12px] text-on-surface-variant px-1 tabnum">{{ page }} / {{ pages }}</span>
        <button class="ui-pager-btn" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
      </div>
    </template>
  </main>
</template>

<script>
import PageHeader from '@/components/PageHeader.vue'
import PostListItem from '@/components/PostListItem.vue'
import { normalizeFileUrl } from '@/utils/utils'

export default {
  name: 'BBSFavorites',
  components: { PageHeader, PostListItem },
  props: { embedded: { type: Boolean, default: false } },
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
    postItem(item) {
      return {
        articleId: item.articleId,
        title: item.title,
        summary: item.summary,
        author: item.author,
        authorAvatar: item.authorAvatar,
        authorOrgName: item.authorOrgName,
        authorOrgNameFull: item.authorOrgNameFull,
        authorLevelIndex: item.authorLevelIndex,
          authorIconSvg: item.authorIconSvg || '',
        userId: item.authorId,
        time: (item.time || '').slice(0, 10),
        views: item.views,
        comments: item.comments,
        labelName: item.labelName,
        cover: item.cover,
      }
    },
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
            authorOrgName: a.authorOrgName || '',
            authorOrgNameFull: a.authorOrgNameFull || '',
            authorLevelIndex: a.authorLevelIndex || null,
          authorIconSvg: a.authorIconSvg || '',
            labelName: a.articleLabelName || '',
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
