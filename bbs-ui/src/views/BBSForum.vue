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

          <!-- 精华展示卡（多帖轮播） -->
          <article
            v-if="featuredShowcase.length"
            class="bg-container border border-border rounded-xl overflow-hidden"
            @mouseenter="pauseShowcase"
            @mouseleave="resumeShowcase"
          >
            <div class="md:flex cursor-pointer" @click="goToArticle(currentShowcase)">
              <div class="md:w-[42%] h-44 md:h-auto bg-surface-container overflow-hidden shrink-0">
                <img
                  v-if="currentShowcase && currentShowcase.cover"
                  :src="currentShowcase.cover"
                  class="w-full h-full object-cover"
                  alt=""
                >
                <div v-else class="w-full h-full flex items-center justify-center">
                  <span class="material-symbols-outlined text-[40px] text-outline/30">stars</span>
                </div>
              </div>
              <div class="flex-1 p-5 min-w-0">
                <div class="flex items-center gap-2 mb-2">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium bg-rank-gold/10 text-rank-gold rounded-full">
                    <span class="material-symbols-outlined text-[12px]">stars</span>精华展示
                  </span>
                  <span class="text-[11px] text-outline ml-auto">{{ showcaseIndex + 1 }} / {{ featuredShowcase.length }}</span>
                </div>
                <h2 class="font-headline-sm text-headline-sm text-on-surface hover:text-primary transition-colors line-clamp-2 leading-snug">
                  {{ currentShowcase && currentShowcase.title }}
                </h2>
                <p class="text-[13px] text-on-surface-variant mt-2 line-clamp-2 leading-relaxed">
                  {{ currentShowcase && currentShowcase.summary }}
                </p>
                <div class="flex items-center gap-3 mt-3 text-[12px] text-outline">
                  <span class="truncate max-w-[120px]">{{ currentShowcase && currentShowcase.author }}</span>
                  <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[13px]">visibility</span>{{ (currentShowcase && currentShowcase.views) || 0 }}</span>
                  <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[13px]">thumb_up</span>{{ (currentShowcase && currentShowcase.likes) || 0 }}</span>
                  <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[13px]">bookmark</span>{{ (currentShowcase && currentShowcase.favorites) || 0 }}</span>
                </div>
              </div>
            </div>
            <div class="flex items-center justify-between px-5 pb-3 pt-1">
              <div class="flex items-center gap-1.5">
                <button
                  v-for="(n, i) in featuredShowcase"
                  :key="'sc-' + i"
                  class="h-1.5 rounded-full transition-all"
                  :class="i === showcaseIndex ? 'w-6 bg-primary' : 'w-1.5 bg-outline-variant hover:bg-outline'"
                  :aria-label="'第 ' + (i + 1) + ' 张'"
                  @click.stop="showcaseIndex = i"
                ></button>
              </div>
              <div class="flex items-center gap-1">
                <button class="w-7 h-7 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-variant" @click.stop="prevShowcase">
                  <span class="material-symbols-outlined text-[18px]">chevron_left</span>
                </button>
                <button class="w-7 h-7 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-variant" @click.stop="nextShowcase">
                  <span class="material-symbols-outlined text-[18px]">chevron_right</span>
                </button>
              </div>
            </div>
          </article>

          <!-- 精华公示卡 -->
          <div v-if="featuredNotices.length" class="bg-amber-50 border border-amber-200 rounded-xl p-4">
            <div class="flex items-center gap-2 mb-2">
              <span class="material-symbols-outlined text-amber-700 text-[18px]">campaign</span>
              <h3 class="font-headline-sm text-headline-sm text-amber-900">精华公示</h3>
              <span class="text-[11px] text-amber-700 ml-auto">公示期接受监督</span>
            </div>
            <div class="space-y-2">
              <div v-for="n in featuredNotices.slice(0, 3)" :key="n.recommendationId" class="flex items-center gap-2 text-[12px] cursor-pointer hover:text-primary" @click="goToHotTopic({ articleId: n.articleId })">
                <span class="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 shrink-0">公示中</span>
                <span class="truncate flex-1">{{ n.title }}</span>
                <span class="text-amber-700 shrink-0">至 {{ (n.publicEndTime || '').slice(0, 10) }}</span>
              </div>
            </div>
          </div>

          <!-- Filter / Sort Bar -->
          <div class="bg-container border border-border rounded-lg px-4 py-2.5 flex flex-wrap items-center gap-3">
            <div class="flex items-center gap-2">
              <span class="text-[12px] text-on-surface-variant">排序</span>
              <BBSSelect
                v-model="filters.sortBy"
                :options="sortOptions"
                size="sm"
                @change="reloadArticles"
              />
            </div>
            <label class="inline-flex items-center gap-1.5 text-[12px] text-on-surface-variant cursor-pointer select-none">
              <input v-model="filters.featured" type="checkbox" class="w-3.5 h-3.5 text-primary border-outline-variant rounded" @change="reloadArticles">
              只看精华
            </label>
            <label class="inline-flex items-center gap-1.5 text-[12px] text-on-surface-variant cursor-pointer select-none">
              <input v-model="filters.hasAttachment" type="checkbox" class="w-3.5 h-3.5 text-primary border-outline-variant rounded" @change="reloadArticles">
              只看有附件
            </label>
            <span class="text-[12px] text-on-surface-variant ml-auto">共 {{ articleTotal }} 条</span>
          </div>

          <!-- Regular Article List -->
          <article
            v-for="(article, index) in articles"
            :key="article.articleId || index"
            class="bg-container border border-border rounded-xl px-5 py-4 hover:border-primary/40 hover:shadow-[0_2px_10px_rgba(15,23,42,0.06)] transition-all cursor-pointer"
            @click="goToArticle(article)"
          >
            <div class="flex gap-4">
              <div class="flex-1 min-w-0">
                <!-- 标题永远在最上方 -->
                <h3 class="post-title text-[16px] font-semibold leading-snug text-on-surface hover:text-primary transition-colors line-clamp-2">
                  {{ article.title }}
                </h3>

                <p v-if="article.summary" class="post-summary text-[13px] text-on-surface-variant mt-2 line-clamp-2 leading-relaxed">
                  {{ article.summary }}
                </p>

                <!-- 元信息 + 统计 -->
                <div class="flex items-center justify-between gap-3 mt-3">
                  <div class="flex items-center gap-1.5 min-w-0 text-[12px] text-outline">
                    <BBSUserBadge
                      :avatar="article.authorAvatar"
                      :nickname="article.author"
                      :org-name="article.authorOrgName"
                      :org-name-full="article.authorOrgNameFull"
                      :level-index="article.authorLevelIndex"
                      :user-id="article.userId"
                      size="xs"
                      layout="inline"
                    />
                    <span class="text-outline-variant shrink-0">·</span>
                    <span class="shrink-0">{{ formatTime(article.time) }}</span>
                  </div>
                  <div class="flex items-center gap-3 text-[12px] text-outline shrink-0">
                    <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">visibility</span>{{ article.views }}</span>
                    <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">chat_bubble</span>{{ article.comments }}</span>
                    <span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">thumb_up</span>{{ article.likes }}</span>
                  </div>
                </div>

                <!-- 标签统一放在卡片底部 -->
                <div
                  v-if="articleLabelName(article) || article.isFeatured || article.isAnonymous"
                  class="flex items-center flex-wrap gap-1.5 mt-2.5"
                >
                  <span
                    v-if="article.isFeatured"
                    class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] bg-rank-gold/10 text-rank-gold rounded"
                  >
                    <span class="material-symbols-outlined text-[12px]">stars</span>精华
                  </span>
                  <span
                    v-if="articleLabelName(article)"
                    class="inline-flex items-center px-2 py-0.5 text-[11px] bg-primary/8 text-primary rounded"
                  >{{ articleLabelName(article) }}</span>
                  <span v-if="article.isAnonymous" class="inline-flex items-center px-2 py-0.5 text-[11px] bg-surface-variant text-on-surface-variant rounded">匿名</span>
                </div>
              </div>

              <div v-if="article.cover" class="w-28 h-20 rounded-lg bg-surface-container overflow-hidden shrink-0 border border-outline-variant">
                <img alt="Article Cover" class="w-full h-full object-cover" :src="article.cover">
              </div>
            </div>
          </article>

          <!-- 分页 -->
          <div v-if="articles.length && articlePages > 1" class="flex items-center justify-center gap-2 pt-1">
            <button
              class="px-3 py-1.5 text-[12px] rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary disabled:opacity-40 disabled:hover:border-outline-variant transition-colors"
              :disabled="articlePage <= 1"
              @click="changePage(articlePage - 1)"
            >上一页</button>
            <span class="text-[12px] text-on-surface-variant px-1">{{ articlePage }} / {{ articlePages }}</span>
            <button
              class="px-3 py-1.5 text-[12px] rounded-lg border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary disabled:opacity-40 disabled:hover:border-outline-variant transition-colors"
              :disabled="articlePage >= articlePages"
              @click="changePage(articlePage + 1)"
            >下一页</button>
          </div>

          <!-- 列表到底：细分割线，不再用大卡片 -->
          <div
            v-if="!loading && articles.length && articlePage >= articlePages && articlePages > 0"
            class="flex items-center gap-3 py-2 select-none"
          >
            <span class="h-px flex-1 bg-border"></span>
            <span class="text-[11px] text-outline tracking-wide">已经到底了</span>
            <span class="h-px flex-1 bg-border"></span>
          </div>

          <!-- 空态 -->
          <div
            v-if="!loading && !articles.length"
            class="bg-container border border-dashed border-outline-variant rounded-xl py-16 flex flex-col items-center text-on-surface-variant"
          >
            <span class="material-symbols-outlined text-[44px] mb-3 opacity-20">forum</span>
            <p class="text-[14px] font-medium text-on-surface mb-1">这个条件下还没有帖子</p>
            <p class="text-[12px] mb-4">试着换个排序、标签或搜索词</p>
            <button
              v-if="hasActiveFilter"
              class="px-4 py-1.5 text-[12px] rounded-full bg-primary/8 text-primary hover:bg-primary/12 transition-colors"
              @click="clearFilters"
            >清除筛选条件</button>
          </div>
        </section>

        <!-- Right Sidebar: Hot Topics + Featured -->
        <aside class="md:col-span-3 space-y-4 sticky top-20 self-start">
          <!-- Hot Topics -->
          <div class="bg-container border border-border rounded-lg p-card-padding">
            <div class="flex items-center justify-between mb-1 gap-2">
              <h2 class="font-headline-sm flex items-center gap-2 min-w-0">
                <span class="material-symbols-outlined text-error filled-icon">local_fire_department</span>
                <span class="truncate">{{ hotTitle }}</span>
              </h2>
              <span class="text-[11px] text-outline shrink-0">累计</span>
            </div>
            <p class="text-[11px] text-outline mb-2">按全部时间累计数据排序，不按时间截断</p>
            <div class="flex flex-wrap gap-1 mb-3">
              <button
                v-for="metric in hotMetrics"
                :key="metric.key"
                class="px-2 py-1 text-[11px] rounded transition-colors"
                :class="hotMetric === metric.key ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface-variant hover:text-primary'"
                @click="hotMetric = metric.key; fetchHotTopics()"
              >
                {{ metric.label }}
              </button>
            </div>
            <ul class="space-y-3">
              <li
                v-for="(topic, index) in visibleHotTopics"
                :key="topic.articleId || index"
                class="flex items-start gap-2.5 group cursor-pointer"
                @click="goToHotTopic(topic)"
              >
                <span
                  class="flex-shrink-0 w-5 h-5 mt-0.5 flex items-center justify-center rounded-md font-bold text-[11px]"
                  :class="getRankClass(index + 1)"
                >{{ index + 1 }}</span>
                <div class="flex-1 min-w-0">
                  <span class="text-[13px] leading-snug text-on-surface group-hover:text-primary transition-colors line-clamp-1 block">{{ topic.articleTitle }}</span>
                  <span class="text-[11px] text-outline">{{ hotMetricLabel }} {{ topic.metricValue }}</span>
                </div>
              </li>
            </ul>
            <p v-if="!hotTopics.length" class="text-[12px] text-outline py-4 text-center">暂无热榜数据</p>
            <button
              v-if="hotTopics.length > 10"
              class="mt-2 w-full text-center text-[12px] text-primary hover:text-primary-container transition-colors py-1"
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
import BBSSelect from '@/components/BBSSelect.vue'
import heroTuojiang from '@/assets/hero/neijiang-tuojiang.jpg'
import heroShengshui from '@/assets/hero/neijiang-shengshui.jpg'
import heroPaifang from '@/assets/hero/neijiang-paifang.jpg'
import heroBuilding from '@/assets/hero/neijiang-power-building.jpg'

export default {
  name: 'BBSForum',
  components: { BBSUserBadge, BBSSelect },
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
      hotMetric: 'views',
      hotMetrics: [
        { key: 'views', label: '浏览' },
        { key: 'comments', label: '回复' },
        { key: 'favorites', label: '收藏' },
        { key: 'likes', label: '点赞' },
        { key: 'shares', label: '分享' },
      ],
      sortOptions: [
        { value: 'publishTime', label: '最新发布' },
        { value: 'lastReplyTime', label: '最新回复' },
        { value: 'lastActivityTime', label: '最后活跃' },
        { value: 'views', label: '最多浏览' },
        { value: 'comments', label: '最多评论' },
        { value: 'likes', label: '最多点赞' },
        { value: 'favorites', label: '最多收藏' },
        { value: 'shares', label: '最多分享' },
      ],
      articleTotal: 0,
      articlePages: 0,
      articlePage: 1,
      pageSize: 10,
      keywords: '',
      filters: {
        sortBy: 'publishTime',
        labelId: null,
        featured: false,
        hasAttachment: false,
      },
      featuredSidebar: [],
      featuredSidebarExpand: false,
      featuredTop: [],
      featuredNotices: [],
      featuredShowcase: [],
      showcaseIndex: 0,
      showcaseTimer: null,
      filteredLabelId: null,
    }
  },
  mounted() {
    const kw = this.$route.query.keywords
    this.keywords = kw || ''
    this.fetchArticles(kw || '')
    this.fetchHotTopics()
    this.fetchFeaturedTop()
    this.fetchFeaturedSidebar()
    this.fetchFeaturedNotices()
    this.fetchFeaturedShowcase()
    this.loadLabels()
    this.startShowcaseTimer()
    this.$bus && this.$bus.$on('forumSearch', this.handleForumSearch)
  },
  beforeDestroy() {
    this.$bus && this.$bus.$off('forumSearch', this.handleForumSearch)
    this.stopShowcaseTimer()
  },
  watch: {
    '$route.query.keywords': function (kw) {
      if (kw) {
        this.fetchArticles(kw)
      }
    },
  },
  computed: {
    hotTitle() {
      const cat = this.categories.find(c => String(c.labelId) === String(this.filters.labelId))
      return cat && cat.labelId != null ? cat.name + '热榜' : '全部热榜'
    },
    visibleHotTopics() {
      return this.hotTopicsExpand ? this.hotTopics : this.hotTopics.slice(0, 10)
    },
    currentShowcase() {
      if (!this.featuredShowcase.length) return null
      return this.featuredShowcase[this.showcaseIndex % this.featuredShowcase.length] || null
    },
    hotMetricLabel() {
      const hit = this.hotMetrics.find(m => m.key === this.hotMetric)
      return hit ? hit.label : ''
    },
    hasActiveFilter() {
      return !!(this.keywords || this.filters.labelId || this.filters.featured || this.filters.hasAttachment)
    },
    visibleFeaturedSidebar() {
      return this.featuredSidebarExpand ? this.featuredSidebar : this.featuredSidebar.slice(0, 3)
    },
  },
  methods: {
    formatTime(val) {
      return dateStr(val)
    },
    fetchArticles(keywords = this.keywords) {
      this.loading = true
      this.keywords = keywords || ''
      const body = {
        keywords: this.keywords,
        labelId: this.filters.labelId,
        featured: this.filters.featured ? 1 : null,
        hasAttachment: this.filters.hasAttachment ? 1 : null,
        sortBy: this.filters.sortBy,
        sortOrder: 'desc',
        page: this.articlePage,
        size: this.pageSize,
      }
      this.postRequest('/common/article/list', body).then(resp => {
        this.loading = false
        const data = (resp && resp.obj) || {}
        const list = Array.isArray(data.list) ? data.list : []
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
          isAnonymous: a.isAnonymous === 1 || a.isAnonymous === '1',
          labelId: a.articleLabelId || null,
          labelName: a.articleLabelName || '',
          time: a.createTime || a.articleCreateTime || '',
          lastReplyTime: a.lastReplyTime || '',
          views: a.articleViewNum || 0,
          comments: a.commentNum ?? a.commentNumCount ?? a.comment_num ?? 0,
          likes: a.articleGoodNum || 0,
          favorites: a.favoriteNum || 0,
          authorLevelIndex: a.authorLevelIndex || null,
          cover: normalizeFileUrl(a.articleImage || null),
        }))
        this.articleTotal = Number(data.total || 0)
        this.articlePages = Number(data.pages || 0)
      }).catch(err => {
        console.warn('[BBSForum] fetchArticles', err)
        this.loading = false
        this.articles = []
        this.articleTotal = 0
        this.articlePages = 0
      })
    },
    reloadArticles() {
      this.articlePage = 1
      this.fetchArticles()
    },
    changePage(page) {
      if (page < 1 || (this.articlePages && page > this.articlePages)) return
      this.articlePage = page
      this.fetchArticles()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    fetchHotTopics() {
      // 热榜按全部时间累计数据排序（不做时间窗筛选）
      this.postRequest('/common/article/hot', {
        labelId: this.filters.labelId,
        metric: this.hotMetric,
        limit: 10,
      }).then(resp => {
        const list = (resp && Array.isArray(resp.obj)) ? resp.obj : []
        const valueOf = (h) => {
          switch (this.hotMetric) {
            case 'comments': return h.commentNumCount || h.commentNum || 0
            case 'favorites': return h.favoriteNum || 0
            case 'likes': return h.articleGoodNum || 0
            case 'shares': return h.shareNum || 0
            default: return h.articleViewNum || 0
          }
        }
        this.hotTopics = list
          .filter(h => h && h.articleId)
          .map(h => ({
            articleId: h.articleId,
            articleTitle: h.articleTitle || '',
            metricValue: valueOf(h),
          }))
      }).catch(err => {
        console.warn('[BBSForum] fetchHotTopics', err)
        this.hotTopics = []
      })
    },
    startShowcaseTimer() {
      this.stopShowcaseTimer()
      this.showcaseTimer = setInterval(() => {
        if (this.featuredShowcase.length > 1) {
          this.showcaseIndex = (this.showcaseIndex + 1) % this.featuredShowcase.length
        }
      }, 5000)
    },
    stopShowcaseTimer() {
      if (this.showcaseTimer) {
        clearInterval(this.showcaseTimer)
        this.showcaseTimer = null
      }
    },
    pauseShowcase() {
      this.stopShowcaseTimer()
    },
    resumeShowcase() {
      this.startShowcaseTimer()
    },
    prevShowcase() {
      if (!this.featuredShowcase.length) return
      const n = this.featuredShowcase.length
      this.showcaseIndex = (this.showcaseIndex - 1 + n) % n
      this.startShowcaseTimer()
    },
    nextShowcase() {
      if (!this.featuredShowcase.length) return
      this.showcaseIndex = (this.showcaseIndex + 1) % this.featuredShowcase.length
      this.startShowcaseTimer()
    },
    articleLabelName(article) {
      return article.labelName || this.getLabelName(article.labelId)
    },
    clearFilters() {
      this.keywords = ''
      this.filters.labelId = null
      this.filters.featured = false
      this.filters.hasAttachment = false
      this.categories.forEach(c => { c.active = c.labelId === null })
      this.articlePage = 1
      this.fetchArticles('')
      this.fetchHotTopics()
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
    fetchFeaturedNotices() {
      this.getRequest('/common/featured/notices').then(resp => {
        this.featuredNotices = (resp && Array.isArray(resp.obj)) ? resp.obj : []
      }).catch(() => { this.featuredNotices = [] })
    },
    fetchFeaturedShowcase() {
      this.getRequest('/common/featured/showcase?limit=5').then(resp => {
        this.featuredShowcase = (resp && Array.isArray(resp.obj)) ? resp.obj : []
        if (this.showcaseIndex >= this.featuredShowcase.length) this.showcaseIndex = 0
      }).catch(() => { this.featuredShowcase = [] })
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
          if (this.filters.labelId !== null) {
            const match = this.categories.find(c => String(c.labelId) === String(this.filters.labelId))
            if (match) match.active = true
          }
        }
      }).catch(err => {
        console.warn('[BBSForum] loadLabels', err)
        this.categories = []
      })
    },
    selectCategory(cat) {
      if (cat.labelId === null || cat.active) {
        this.categories.forEach(c => { c.active = c.labelId === null })
        this.filters.labelId = null
      } else {
        this.categories.forEach(c => { c.active = c === cat })
        this.filters.labelId = cat.labelId
      }
      this.articlePage = 1
      this.fetchArticles()
      this.fetchHotTopics()
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
      this.articlePage = 1
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

/* 压暗层：中间略深保证标题对比度，左右两侧更亮（领导反馈两侧看不清），过渡平滑 */
.hero-photo__scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg,
      rgba(7, 18, 36, 0.10) 0%,
      rgba(7, 18, 36, 0.20) 16%,
      rgba(7, 18, 36, 0.48) 38%,
      rgba(7, 18, 36, 0.58) 50%,
      rgba(7, 18, 36, 0.48) 62%,
      rgba(7, 18, 36, 0.20) 84%,
      rgba(7, 18, 36, 0.10) 100%),
    linear-gradient(180deg, rgba(7, 18, 36, 0.16) 0%, rgba(7, 18, 36, 0.28) 100%);
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
