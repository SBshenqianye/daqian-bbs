<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center mb-6">
        <button class="inline-flex items-center gap-1 px-3 py-1.5 text-primary hover:bg-primary/5 rounded-lg transition-colors font-body-md mr-4" @click="goBack">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          返回
        </button>
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface">文章详情</h1>
        </div>
      </div>

      <!-- Article Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="mb-4">
          <h2 class="font-headline-md text-headline-md text-on-surface">标题：《{{ articleTitle }}》</h2>
        </div>
        <mavon-editor ref="md" v-model="editor.value" v-bind="editor" />
      </div>

      <!-- Attachments -->
      <div v-if="fileList && fileList.length > 0" class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <h3 class="font-headline-sm text-headline-sm text-on-surface mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary text-[20px]">attach_file</span>
          附件列表
        </h3>
        <div class="space-y-2">
          <div v-for="(file, index) in fileList" :key="index" class="flex items-center justify-between p-3 bg-surface-container-low rounded-lg border border-outline-variant/50">
            <span class="font-body-md text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-outline text-[18px]">description</span>
              {{ file.fileName }}
            </span>
            <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="downloadFile(file.filePath)">
              <span class="material-symbols-outlined text-[14px]">download</span>
              下载
            </button>
          </div>
        </div>
      </div>

      <!-- Comments -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <h3 class="font-headline-sm text-headline-sm text-on-surface mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary text-[20px]">comment</span>
          评论数：{{ commentCount }}
        </h3>

        <div v-if="comments.length > 0" class="space-y-4">
          <div v-for="(item, index) in comments" :key="item.commentId || index" class="bg-surface-container-low rounded-lg p-4 border border-outline-variant/50">
            <div class="flex items-center gap-3 mb-2">
              <img class="w-9 h-9 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(item.portrait)" alt="">
              <div>
                <span class="font-headline-sm text-headline-sm text-on-surface">{{ item.nickname || '未知用户' }}</span>
                <span class="text-body-md text-on-surface-variant ml-2">{{ item.commentTime }}</span>
              </div>
            </div>
            <p class="text-body-md text-on-surface ml-12">{{ item.commentContent }}</p>
            <div v-if="item.reply && item.reply.length" class="ml-12 mt-3 pl-4 border-l-2 border-outline-variant/30 space-y-3">
              <div v-for="(reply, rIdx) in item.reply" :key="reply.replyId || rIdx" class="bg-surface-container rounded-lg p-3">
                <div class="flex items-center gap-2 mb-1.5">
                  <img class="w-7 h-7 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(reply.portrait)" alt="">
                  <span class="font-headline-sm text-headline-sm text-on-surface text-[13px]">{{ reply.nickname || '未知用户' }}</span>
                  <span class="text-body-md text-on-surface-variant text-[12px]">{{ reply.replyTime }}</span>
                </div>
                <p class="text-body-md text-on-surface-variant ml-9">
                  <span v-if="reply.replyToNickname" class="text-primary">回复 {{ reply.replyToNickname }}：</span>
                  {{ reply.replyContent }}
                </p>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-center py-8 text-on-surface-variant flex flex-col items-center gap-2">
          <span class="material-symbols-outlined text-[36px] opacity-30">chat</span>
          <p class="text-body-md">暂无评论</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

export default {
  name: 'StitchArticleDetails',
  components: { mavonEditor },
  data() {
    return {
      articleTitle: '',
      editor: {
        value: '',
        toolbarsFlag: false,
        subfield: false,
        defaultOpen: 'preview'
      },
      comments: [],
      fileList: [],
      avatarBase: process.env.VUE_APP_BBS_API || ''
    }
  },
  computed: {
    commentCount() {
      if (!this.comments.length) return 0
      return this.comments.reduce((sum, item) => {
        return sum + 1 + (item.reply && item.reply.length ? item.reply.length : 0)
      }, 0)
    }
  },
  mounted() {
    this.getArticleByArticleId(this.$route.query.articleId)
  },
  activated() {
    this.getArticleByArticleId(this.$route.query.articleId)
  },
  methods: {
    goBack() {
      this.$router.go(-1)
    },
    getAvatarUrl(portrait) {
      if (!portrait) return require('../assets/img/img.jpeg')
      const path = portrait.startsWith('/') ? portrait : `/${portrait}`
      return `${this.avatarBase}${path}`
    },
    normalizeUrls(content) {
      if (!content) return content
      // 处理 markdown 链接: [text](url) 中 url 没有协议的情况
      content = content.replace(
        /\[([^\]]*)\]\(((?!https?:\/\/|ftp:\/\/|\/\/|data:|mailto:|tel:|#|\/)[^\s\)]+)\)/g,
        (match, text, url) => {
          if (url.startsWith('./') || url.startsWith('../')) return match
          if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(url)) return match
          return `[${text}](http://${url})`
        }
      )
      // 处理 HTML 链接: href="url" 中 url 没有协议的情况
      content = content.replace(
        /href="((?!https?:\/\/|ftp:\/\/|\/\/|data:|mailto:|tel:|#|\/)[^"]+)"/g,
        (match, url) => {
          if (url.startsWith('./') || url.startsWith('../')) return match
          if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(url)) return match
          return `href="http://${url}"`
        }
      )
      return content
    },
    getArticleByArticleId(articleId) {
      this.getRequest('/admin/getArticleByArticleId', articleId).then(resp => {
        if (resp) {
          this.editor.value = this.normalizeUrls(resp.obj.articleContent)
          this.articleTitle = resp.obj.articleTitle
          this.getArticleFileByArticleId(articleId)
          this.getCommentByArticleId(articleId)
        }
      })
    },
    getArticleFileByArticleId(articleId) {
      this.postRequest(`/common/getArticleFileByArticleId/${articleId}`, {}).then(res => {
        let list = []
        if (Array.isArray(res)) list = res
        else if (res && Array.isArray(res.obj)) list = res.obj
        else if (res && Array.isArray(res.listBean)) list = res.listBean
        this.fileList = list
      }).catch(() => { this.fileList = [] })
    },
    downloadFile(filePath) {
      if (!filePath) return
      const base = process.env.VUE_APP_BBS_BASE_API || ''
      const url = base.endsWith('/') ? base + filePath.replace(/^\//, '') : (filePath.startsWith('/') ? base + filePath : base + '/' + filePath)
      const iframe = document.createElement('iframe')
      iframe.setAttribute('style', 'position:fixed;left:-9999px;top:0;width:0;height:0;border:0;visibility:hidden;')
      document.body.appendChild(iframe)
      iframe.src = url
      setTimeout(() => { if (iframe.parentNode) document.body.removeChild(iframe) }, 6000)
    },
    getCommentByArticleId(articleId) {
      this.postRequest(`/common/comment/getCommentReply/${articleId}`).then(res => {
        this.comments = (res && Array.isArray(res)) ? res : []
      }).catch(() => { this.comments = [] })
    }
  }
}
</script>
