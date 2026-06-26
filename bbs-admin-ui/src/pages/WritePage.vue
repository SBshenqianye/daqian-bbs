<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">edit</span>
            写文章
          </h1>
          <p class="text-body-md text-secondary mt-1">编辑发布新文章（此页为预留功能，数据提交需对接后端）</p>
        </div>
      </div>

      <!-- Editor Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <mavon-editor v-model="content" ref="md" :externalLink="localExternalLink" @imgAdd="$imgAdd" @change="change" style="min-height: 600px" />
        <div class="mt-6 flex justify-end">
          <button class="inline-flex items-center gap-1.5 px-6 py-2.5 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-headline-sm text-headline-sm shadow-sm" @click="submit">
            <span class="material-symbols-outlined text-[18px]">send</span>
            提交
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

export default {
  name: 'WritePage',
  components: { mavonEditor },
  data() {
    return {
      content: '',
      html: '',
      // 离线环境：mavon-editor 外链钩子指向本地 /public/lib/ 资源
      localExternalLink: {
        hljs_js: () => process.env.BASE_URL + 'lib/highlight.min.js',
        hljs_css: (css) => process.env.BASE_URL + `lib/highlight/styles/${css}.min.css`,
        hljs_lang: (lang) => process.env.BASE_URL + `lib/highlight/languages/${lang}.min.js`,
        markdown_css: false,  // 已在 main.js 中本地导入
        katex_js: () => process.env.BASE_URL + 'lib/katex/katex.min.js',
        katex_css: () => process.env.BASE_URL + 'lib/katex/katex.min.css',
      },
    }
  },
  methods: {
    $imgAdd(pos, $file) {
      var formdata = new FormData()
      formdata.append('file', $file)
      this.$axios({
        url: '/common/upload',
        method: 'post',
        data: formdata,
        headers: { 'Content-Type': 'multipart/form-data' },
      }).then((url) => {
        this.$refs.md.$img2Url(pos, url)
      })
    },
    change(value, render) {
      this.html = render
    },
    submit() {
      this.$message.success('提交成功！（演示功能）')
    }
  }
}
</script>

<style scoped>
.editor-btn { margin-top: 20px; }
</style>
