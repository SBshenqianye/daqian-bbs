<template>
  <main class="min-h-screen bg-surface">
    <!-- Hero Section：内江地标影像带 -->
    <section class="hero-photo relative overflow-hidden text-white">
      <!-- 图片带：多张地标图以左缘渐变叠接，末张为供图的电力大楼 -->
      <div class="hero-photo__media" aria-hidden="true">
        <div class="hero-photo__shot" :style="{ backgroundImage: 'url(' + heroImages.tuojiang + ')' }"></div>
        <div class="hero-photo__shot hero-photo__shot--fade" :style="{ backgroundImage: 'url(' + heroImages.shengshui + ')' }"></div>
        <div class="hero-photo__shot hero-photo__shot--fade hero-photo__shot--optional" :style="{ backgroundImage: 'url(' + heroImages.paifang + ')' }"></div>
        <div class="hero-photo__slot">
          <img class="hero-photo__slot-img" :src="heroImages.building" alt="国网大楼">
        </div>
      </div>

      <!-- 压暗层：保证标题/正文在任何图片底色上都清晰（图片颜色不干扰文字） -->
      <div class="hero-photo__scrim"></div>

      <div class="hero-photo__content relative z-10 max-w-7xl mx-auto px-page-margin-desktop py-16 md:py-20">
        <div class="max-w-3xl mx-auto text-center">
          <h1 class="font-headline-lg text-[32px] md:text-[40px] leading-tight mb-4 tracking-tight drop-shadow-[0_2px_10px_rgba(0,0,0,0.6)]">欢迎来到大千智荟创新创意交流论坛</h1>
          <p class="font-body-lg text-white/90 max-w-2xl mx-auto drop-shadow-[0_1px_6px_rgba(0,0,0,0.55)]">汇聚国网智慧，激发创新灵感，共同打造卓越的企业级知识共享平台。</p>
        </div>
      </div>

      <p class="hero-photo__credits">沱江风光·圣水寺·隆昌石牌坊·国网大楼</p>
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
                  <span class="text-[11px] text-outline block">{{ formatTime(item.time) }}</span>
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
                    <span>{{ formatTime(article.time) }}</span>
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
                <p class="text-body-md text-secondary mb-4 line-clamp-2" style="white-space: pre-line">{{ article.summary }}</p>
                <div class="flex items-center justify-between text-label-md text-outline">
                  <div class="flex items-center gap-2">
                    <bbs-user-badge :avatar="article.authorAvatar" :nickname="article.author" :org-name="article.authorOrgName" :org-name-full="article.authorOrgNameFull" size="sm" layout="inline" />
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

        </aside>
      </div>
    </div>
  </main>
</template>

<script>
import { normalizeFileUrl } from '@/utils/utils'
import { dateStr } from '@/utils/time'
import BBSUserBadge from '@/components/BBSUserBadge'
import heroTuojiang from '@/assets/hero/neijiang-tuojiang.jpg'
import heroShengshui from '@/assets/hero/neijiang-shengshui.jpg'
import heroPaifang from '@/assets/hero/neijiang-paifang.jpg'
import heroBuilding from '@/assets/hero/neijiang-power-building.jpg'

export default {
  name: 'BBSForum',
  components: { BBSUserBadge },
  data() {
    return {
      // 首页 Hero 地标图（顺序：左 → 右）
      heroImages: {
        tuojiang: heroTuojiang,
        shengshui: heroShengshui,
        paifang: heroPaifang,
        building: heroBuilding,
      },
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
    formatTime(val) {
      return dateStr(val)
    },
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
          authorOrgName: a.authorOrgName || '',
          authorOrgNameFull: a.authorOrgNameFull || '',
          isFeatured: a.isFeatured === 1 || a.isFeatured === '1',
          labelId: a.articleLabelId || null,
          time: a.createTime || a.articleCreateTime || '',
          views: a.articleViewNum || 0,
          comments: a.commentNum ?? a.comment_num ?? 0,
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
          this.categories = [
            { name: '全部', icon: 'apps', labelId: null, active: this.filteredLabelId === null, description: '' },
            ...resp.map((l) => ({
              name: l.labelName,
              icon: l.icon || 'bookmark',
              labelId: l.labelId,
              active: false,
              description: l.description || '',
            }))
          ]
          // 如果已有选中的标签，激活对应分类
          if (this.filteredLabelId !== null) {
            const match = this.categories.find(c => String(c.labelId) === String(this.filteredLabelId))
            if (match) match.active = true
          }
        }
      }).catch(err => {
        console.warn('[BBSForum] loadLabels', err)
        this.categories = []
      })
    },
    selectCategory(cat) {
      if (cat.labelId === null) {
        // 点击"全部"：取消所有分类选中，显示全部文章
        this.categories.forEach(c => { c.active = false })
        cat.active = true
        this.filteredLabelId = null
        return
      }
      // 点击具体分类
      if (cat.active) {
        // 取消选中，回到"全部"
        cat.active = false
        this.filteredLabelId = null
        const allCat = this.categories.find(c => c.labelId === null)
        if (allCat) allCat.active = true
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

<style scoped>
/* ===== 首页 Hero：内江地标影像带 ===== */
.hero-photo {
  background-color: #0a1830;
  min-height: 260px;
}

.hero-photo__media {
  position: absolute;
  inset: 0;
  display: flex;
}

.hero-photo__shot {
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 相邻图片（含末张供图）向左叠压 4%，后一张仅以左缘 14% 渐变淡入：
   下层图片始终不透明，叠接处不会出现暗缝，也不会出现硬边 */
.hero-photo__shot + .hero-photo__shot,
.hero-photo__shot + .hero-photo__slot {
  margin-left: -4%;
}

.hero-photo__shot--fade {
  -webkit-mask-image: linear-gradient(to right, rgba(0, 0, 0, 0) 0%, #000 14%, #000 100%);
          mask-image: linear-gradient(to right, rgba(0, 0, 0, 0) 0%, #000 14%, #000 100%);
}

/* 末张供图（内江电力大楼）：与其他图片同样左缘渐入，无缝接入影像带 */
.hero-photo__slot {
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  -webkit-mask-image: linear-gradient(to right, rgba(0, 0, 0, 0) 0%, #000 14%, #000 100%);
          mask-image: linear-gradient(to right, rgba(0, 0, 0, 0) 0%, #000 14%, #000 100%);
}

.hero-photo__slot-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 竖构图按 35% 纵向取景，保留楼顶与"人民电业为人民"标语 */
  object-position: center 35%;
  /* 轻度压亮，与其余照片观感一致，避免最右侧过亮抢标题 */
  filter: brightness(0.9) saturate(0.95);
}

/* 压暗层：中间略深，保证白色标题与正文的对比度不受图片颜色影响 */
.hero-photo__scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(7, 18, 36, 0.52) 0%, rgba(7, 18, 36, 0.46) 45%, rgba(7, 18, 36, 0.62) 100%),
    radial-gradient(ellipse at 50% 45%, rgba(5, 13, 26, 0.55) 0%, rgba(5, 13, 26, 0.18) 62%, rgba(5, 13, 26, 0.4) 100%);
}

.hero-photo__credits {
  position: absolute;
  right: 16px;
  bottom: 6px;
  z-index: 10;
  font-size: 10px;
  line-height: 1.2;
  color: rgba(255, 255, 255, 0.45);
  pointer-events: none;
}

/* 移动端：影像带收成顶部横条，标题与正文落在纯色底上，彻底避免图片干扰文字 */
@media (max-width: 767px) {
  .hero-photo {
    min-height: 0;
  }
  .hero-photo__media {
    height: 128px;
  }
  /* 移动端只保留两张地标图 + 供图，避免单格过窄 */
  .hero-photo__shot--optional {
    display: none;
  }
  .hero-photo__shot + .hero-photo__shot,
  .hero-photo__shot + .hero-photo__slot {
    margin-left: -6%;
  }
  .hero-photo__scrim {
    background: linear-gradient(180deg, rgba(7, 18, 36, 0.5) 0%, rgba(7, 18, 36, 0.58) 128px, #0a1830 128px);
  }
  .hero-photo__content {
    padding-top: 160px;
    padding-bottom: 28px;
  }
  .hero-photo__credits {
    display: none;
  }
}
</style>
