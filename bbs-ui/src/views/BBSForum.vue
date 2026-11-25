<template>
  <main class="min-h-screen bg-surface">
    <!-- Hero Section：内江地标影像带 -->
    <section class="hero-photo relative overflow-hidden text-white">
      <!-- 图片带：多张地标图以左缘渐变叠接，末张为供图的电力大楼；
           四格图片均可在后台「系统配置 → 首页 Hero」替换，未配置时用打包内置图 -->
      <div class="hero-photo__media" aria-hidden="true">
        <div class="hero-photo__shot" :style="heroShotStyle(1, heroImages.overlook)"></div>
        <div class="hero-photo__shot hero-photo__shot--fade" :style="heroShotStyle(2, heroImages.shengshui)"></div>
        <div class="hero-photo__shot hero-photo__shot--fade hero-photo__shot--optional" :style="heroShotStyle(3, heroImages.paifang)"></div>
        <div class="hero-photo__slot" :class="{ 'hero-photo__slot--custom': !!heroConfig[4] }">
          <img class="hero-photo__slot-img" :src="heroConfig[4] || heroImages.building" alt="内江地标">
        </div>
      </div>

      <!-- 压暗层：保证标题/正文在任何图片底色上都清晰（图片颜色不干扰文字） -->
      <div class="hero-photo__scrim"></div>

      <div class="hero-photo__content relative z-10 max-w-7xl mx-auto px-page-margin-desktop py-10 md:py-14">
        <div class="max-w-3xl mx-auto text-center">
          <h1 class="hero-title text-[27px] md:text-[34px] font-semibold leading-tight mb-3">欢迎来到大千智荟创新创意交流论坛</h1>
          <p class="hero-subtitle text-[14px] md:text-[15px] max-w-2xl mx-auto">汇聚国网智慧，激发创新灵感，共同打造卓越的企业级知识共享平台。</p>
          <div class="mt-5 flex items-center justify-center gap-2 flex-wrap">
            <span class="hero-pill"><span class="material-symbols-outlined text-[15px]">forum</span>{{ articleTotal }} 篇讨论</span>
            <span class="hero-pill"><span class="material-symbols-outlined text-[15px]">category</span>{{ Math.max(0, categories.length - 1) }} 个板块</span>
            <span class="hero-pill"><span class="material-symbols-outlined text-[15px]">local_fire_department</span>热榜持续更新</span>
          </div>
        </div>
      </div>

      <p v-if="heroCaptionText" class="hero-photo__credits">{{ heroCaptionText }}</p>
    </section>

    <!-- Main Layout Container -->
    <div class="max-w-7xl mx-auto px-page-margin-mobile md:px-page-margin-desktop py-5 md:py-7">
      <div class="grid grid-cols-1 lg:grid-cols-[190px_minmax(0,1fr)] xl:grid-cols-[200px_minmax(0,1fr)_296px] gap-4 lg:gap-5 items-start">
        <!-- Left Sidebar: Categories + Featured Posts -->
        <aside class="space-y-3 min-w-0">
          <div class="bg-container border border-border rounded-xl p-2">
            <nav class="flex lg:flex-col gap-1 overflow-x-auto lg:overflow-visible custom-scrollbar">
              <el-tooltip
                v-for="cat in categories"
                :key="cat.name"
                :content="cat.description"
                placement="right"
                effect="dark"
                :disabled="!cat.description"
              >
                <button
                  class="category-item group shrink-0 lg:w-full flex items-center gap-2 px-2.5 py-2 rounded-lg transition-colors text-[13px] font-medium whitespace-nowrap"
                  :class="cat.active ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-surface-container-low hover:text-primary'"
                  @click="selectCategory(cat)"
                >
                  <span
                    class="w-6 h-6 rounded-md grid place-items-center shrink-0 transition-colors"
                    :class="cat.active ? 'bg-primary/10 text-primary' : 'bg-surface-container-low text-outline group-hover:text-primary'"
                  >
                    <span class="material-symbols-outlined text-[16px]" :class="{ 'filled-icon': cat.active }">{{ cat.icon }}</span>
                  </span>
                  <span class="truncate">{{ cat.name }}</span>
                </button>
              </el-tooltip>
            </nav>
          </div>

          <!-- Featured Posts Sidebar (below labels) -->
          <div v-if="featuredSidebar.length > 0" class="hidden xl:block bg-container border border-border rounded-xl p-3">
            <div class="flex items-center justify-between mb-2 gap-2">
              <h2 class="ui-section-title">
                <span class="material-symbols-outlined text-amber-500 text-[17px] filled-icon">stars</span>
                精华帖
              </h2>
              <button
                class="text-[11.5px] text-primary hover:opacity-80 transition-opacity flex items-center gap-0.5 whitespace-nowrap"
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
                <span class="material-symbols-outlined text-amber-500 text-[13px] shrink-0 mt-0.5">stars</span>
                <div class="flex-1 min-w-0">
                  <span class="text-[12.5px] leading-snug text-on-surface group-hover:text-primary transition-colors line-clamp-1 block">{{ item.articleTitle }}</span>
                  <span class="text-[10.5px] text-outline block mt-0.5">
                    <template v-if="item.labelName">{{ item.labelName }} · </template>{{ formatTime(item.time) }}
                  </span>
                </div>
              </li>
            </ul>
            <button
              v-if="featuredSidebar.length > 3"
              class="mt-1.5 w-full text-center text-[11.5px] text-primary hover:opacity-80 transition-opacity py-0.5"
              @click="featuredSidebarExpand = !featuredSidebarExpand"
            >
              {{ featuredSidebarExpand ? '收起' : '展开更多' }}
              <span class="material-symbols-outlined text-[12px] align-middle">{{ featuredSidebarExpand ? 'expand_less' : 'expand_more' }}</span>
            </button>
          </div>
        </aside>

        <!-- Center Feed: Articles -->
        <section class="space-y-3 min-w-0">
          <!-- Featured Top: 置顶精华帖（最新3条） -->
          <div v-if="featuredTop.length > 0" class="ui-card overflow-hidden">
            <div class="flex items-center gap-1.5 px-3.5 py-2 bg-amber-50/70 border-b border-amber-100">
              <span class="material-symbols-outlined text-amber-600 text-[15px] filled-icon">stars</span>
              <span class="text-[12.5px] font-semibold text-amber-900">精华置顶</span>
              <span class="text-[10.5px] text-amber-700/70 ml-auto">最新 3 条</span>
            </div>
            <div class="divide-y divide-outline-variant/40">
              <article
                v-for="(article, index) in featuredTop"
                :key="'ft-' + (article.articleId || index)"
                class="px-3.5 py-2 hover:bg-amber-50/40 transition-colors cursor-pointer"
                @click="goToArticle(article)"
              >
                <div class="flex items-center gap-2">
                  <span class="flex-shrink-0 w-[18px] h-[18px] flex items-center justify-center rounded-md text-[10px] font-bold"
                    :class="index === 0 ? 'bg-rank-gold text-white' : index === 1 ? 'bg-rank-silver text-white' : 'bg-rank-bronze text-white'">
                    {{ index + 1 }}
                  </span>
                  <h3 class="flex-1 text-[12.5px] font-medium text-on-surface hover:text-primary transition-colors truncate">{{ article.title }}</h3>
                  <span class="ui-meta shrink-0">
                    <span class="truncate max-w-[72px]">{{ article.author }}</span>
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
            class="ui-card overflow-hidden"
            @mouseenter="pauseShowcase"
            @mouseleave="resumeShowcase"
          >
            <div class="md:flex cursor-pointer" @click="goToArticle(currentShowcase)">
              <div class="md:w-[38%] h-40 md:h-[196px] bg-surface-container overflow-hidden shrink-0">
                <img
                  v-if="currentShowcase && (currentShowcase.cover || currentShowcase.generatedCover)"
                  :src="currentShowcase.cover || currentShowcase.generatedCover"
                  class="w-full h-full object-cover"
                  alt=""
                  @error="onShowcaseCoverError"
                >
                <div v-else class="w-full h-full flex items-center justify-center">
                  <span class="material-symbols-outlined text-[38px] text-outline/30">stars</span>
                </div>
              </div>
              <div class="flex-1 p-4 md:p-5 min-w-0">
                <div class="flex items-center gap-2 mb-2">
                  <span class="ui-chip ui-chip-gold">
                    <span class="material-symbols-outlined filled-icon">stars</span>精华展示
                  </span>
                  <span class="text-[10.5px] text-outline ml-auto tabnum">{{ showcaseIndex + 1 }} / {{ featuredShowcase.length }}</span>
                </div>
                <h2 class="text-[17px] font-semibold text-on-surface hover:text-primary transition-colors line-clamp-2 leading-snug">
                  {{ currentShowcase && currentShowcase.title }}
                </h2>
                <p class="ui-post-summary">
                  {{ currentShowcase && currentShowcase.summary }}
                </p>
              </div>
            </div>
            <!-- 元信息贴近卡片底边，字号降低一档；轮播控制与它共用同一行 -->
            <div class="flex items-center justify-between gap-2 px-3 py-1.5 border-t border-outline-variant/40">
              <div class="ui-meta text-[10.5px] min-w-0">
                <span class="ui-meta-item truncate max-w-[110px]">{{ currentShowcase && currentShowcase.author }}</span>
                <span class="text-outline-variant">·</span>
                <span class="ui-meta-item"><span class="material-symbols-outlined text-[12px]">visibility</span>{{ (currentShowcase && currentShowcase.views) || 0 }}</span>
                <span class="ui-meta-item"><span class="material-symbols-outlined text-[12px]">thumb_up</span>{{ (currentShowcase && currentShowcase.likes) || 0 }}</span>
                <span class="ui-meta-item"><span class="material-symbols-outlined text-[12px]">bookmark</span>{{ (currentShowcase && currentShowcase.favorites) || 0 }}</span>
              </div>
              <div class="flex items-center gap-1.5 shrink-0">
                <button
                  v-for="(n, i) in featuredShowcase"
                  :key="'sc-' + i"
                  class="h-1.5 rounded-full transition-all"
                  :class="i === showcaseIndex ? 'w-5 bg-primary' : 'w-1.5 bg-outline-variant hover:bg-outline'"
                  :aria-label="'第 ' + (i + 1) + ' 张'"
                  @click.stop="showcaseIndex = i"
                ></button>
                <span class="w-px h-3 bg-outline-variant/50 mx-0.5"></span>
                <button class="ui-icon-btn" @click.stop="prevShowcase">
                  <span class="material-symbols-outlined">chevron_left</span>
                </button>
                <button class="ui-icon-btn" @click.stop="nextShowcase">
                  <span class="material-symbols-outlined">chevron_right</span>
                </button>
              </div>
            </div>
          </article>

          <!-- 精华公示卡 -->
          <div v-if="featuredNotices.length" class="bg-amber-50/70 border border-amber-200/70 rounded-xl p-3.5">
            <div class="flex items-center gap-2 mb-2">
              <span class="material-symbols-outlined text-amber-700 text-[17px]">campaign</span>
              <h3 class="text-[13.5px] font-semibold text-amber-900">精华公示</h3>
              <span class="text-[10.5px] text-amber-700/80 ml-auto">公示期接受监督</span>
            </div>
            <div class="space-y-1.5">
              <div v-for="n in featuredNotices.slice(0, 3)" :key="n.recommendationId" class="flex items-center gap-2 text-[11.5px] cursor-pointer hover:text-primary" @click="goToHotTopic({ articleId: n.articleId })">
                <span class="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 shrink-0">公示中</span>
                <span class="truncate flex-1">{{ n.title }}</span>
                <span class="text-amber-700 shrink-0">至 {{ (n.publicEndTime || '').slice(0, 10) }}</span>
              </div>
            </div>
          </div>

          <!-- Filter / Sort Bar -->
          <div class="ui-toolbar">
            <div class="flex items-center gap-2">
              <span class="ui-meta-item text-[11.5px] text-on-surface-variant"><span class="material-symbols-outlined">sort</span>排序</span>
              <BBSSelect
                v-model="filters.sortBy"
                :options="sortOptions"
                size="sm"
                @change="reloadArticles"
              />
            </div>
            <label class="ui-check">
              <input v-model="filters.featured" type="checkbox" @change="reloadArticles">
              <span class="material-symbols-outlined text-[14px] text-amber-500">stars</span>
              只看精华
            </label>
            <label class="ui-check">
              <input v-model="filters.hasAttachment" type="checkbox" @change="reloadArticles">
              <span class="material-symbols-outlined text-[14px] text-outline">attach_file</span>
              只看有附件
            </label>
            <span class="ui-meta-item text-[11.5px] text-outline ml-auto"><span class="material-symbols-outlined">library_books</span>共 {{ articleTotal }} 条</span>
          </div>

          <!-- Regular Article List：与精华/收藏/我的发布共用同一张卡片 -->
          <PostListItem
            v-for="(article, index) in articles"
            :key="article.articleId || index"
            :article="postItem(article)"
            @click="goToArticle"
          />

          <!-- 分页 -->
          <div v-if="articles.length && articlePages > 1" class="ui-pager">
            <button class="ui-pager-btn" :disabled="articlePage <= 1" @click="changePage(articlePage - 1)">
              <span class="material-symbols-outlined text-[16px]">chevron_left</span>上一页
            </button>
            <span class="text-[12px] text-on-surface-variant px-1 tabnum">{{ articlePage }} / {{ articlePages }}</span>
            <button class="ui-pager-btn" :disabled="articlePage >= articlePages" @click="changePage(articlePage + 1)">
              下一页<span class="material-symbols-outlined text-[16px]">chevron_right</span>
            </button>
          </div>

          <!-- 列表到底：细分割线，不再用大卡片 -->
          <div
            v-if="!loading && articles.length && articlePage >= articlePages && articlePages > 0"
            class="ui-list-end"
          >
            <span class="text-[11px] text-outline tracking-wide">已经到底了</span>
          </div>

          <!-- 空态 -->
          <div v-if="!loading && !articles.length" class="ui-empty ui-card border-dashed">
            <span class="material-symbols-outlined">forum</span>
            <p class="ui-empty-title">这个条件下还没有帖子</p>
            <p class="ui-empty-desc">试着换个排序、标签或搜索词</p>
            <button
              v-if="hasActiveFilter"
              class="mt-3 px-3.5 py-1.5 text-[12px] rounded-full bg-primary/10 text-primary hover:bg-primary/20 transition-colors"
              @click="clearFilters"
            >清除筛选条件</button>
          </div>
        </section>

        <!-- Right Sidebar: Hot Topics + Featured -->
        <aside class="space-y-3 min-w-0 lg:col-span-2 xl:col-span-1 xl:sticky xl:top-20 self-start">
          <!-- Hot Topics -->
          <div class="ui-card p-4">
            <div class="flex items-center justify-between mb-0.5 gap-2">
              <h2 class="ui-section-title min-w-0">
                <span class="material-symbols-outlined text-error filled-icon text-[18px]">local_fire_department</span>
                <span class="truncate">{{ hotTitle }}</span>
              </h2>
              <span class="text-[10.5px] text-outline shrink-0">累计</span>
            </div>
            <p class="ui-subtitle mb-2.5">按全部时间累计数据排序，不按时间截断</p>
            <div class="flex flex-wrap gap-1 mb-3">
              <button
                v-for="metric in hotMetrics"
                :key="metric.key"
                class="px-2 py-1 text-[11px] rounded-md transition-colors"
                :class="hotMetric === metric.key ? 'bg-primary/10 text-primary font-semibold' : 'bg-surface-container text-on-surface-variant hover:text-primary'"
                @click="hotMetric = metric.key; fetchHotTopics()"
              >
                {{ metric.label }}
              </button>
            </div>
            <ul class="space-y-2.5 lg:grid lg:grid-cols-2 lg:gap-x-6 lg:gap-y-2.5 lg:space-y-0 xl:block xl:space-y-2.5">
              <li
                v-for="(topic, index) in visibleHotTopics"
                :key="topic.articleId || index"
                class="flex items-start gap-2 group cursor-pointer min-w-0"
                @click="goToHotTopic(topic)"
              >
                <span
                  class="flex-shrink-0 w-[18px] h-[18px] mt-px flex items-center justify-center rounded-md font-bold text-[10px]"
                  :class="getRankClass(index + 1)"
                >{{ index + 1 }}</span>
                <div class="flex-1 min-w-0">
                  <span class="text-[12.5px] leading-snug text-on-surface group-hover:text-primary transition-colors line-clamp-1 block">{{ topic.articleTitle }}</span>
                  <span class="text-[10.5px] text-outline block mt-0.5">{{ hotMetricLabel }} {{ topic.metricValue }}</span>
                </div>
              </li>
            </ul>
            <p v-if="!hotTopics.length" class="text-[12px] text-outline py-4 text-center">暂无热榜数据</p>
            <button
              v-if="hotTopics.length > 10"
              class="mt-2 w-full text-center text-[11.5px] text-primary hover:opacity-80 transition-opacity py-1"
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
import BBSSelect from '@/components/BBSSelect.vue'
import PostListItem from '@/components/PostListItem.vue'
import { generateFeaturedCover } from '@/utils/featured-cover'
import heroOverlook from '@/assets/hero/neijiang-overlook.jpg'
import heroShengshui from '@/assets/hero/neijiang-shengshui.jpg'
import heroPaifang from '@/assets/hero/neijiang-paifang.jpg'
import heroBuilding from '@/assets/hero/neijiang-power-building.jpg'

export default {
  name: 'BBSForum',
  components: { BBSSelect, PostListItem },
  data() {
    return {
      // 首页 Hero 地标图（顺序：左 → 右）：打包内置默认图
      heroImages: {
        overlook: heroOverlook,
        shengshui: heroShengshui,
        paifang: heroPaifang,
        building: heroBuilding,
      },
      // 后台「系统配置 → 首页 Hero」配置的图片地址（1..4），为空则该格用上面的内置图
      heroConfig: { 1: '', 2: '', 3: '', 4: '' },
      // null = 尚未读取配置（用默认文案）；'' = 后台主动清空（隐藏该行）
      heroCaptionRaw: null,
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
    this.loadHeroConfig()
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
    // 右下角来源标注：未读到配置时用内置文案，读到空串时隐藏（后台主动清空）
    heroCaptionText() {
      if (this.heroCaptionRaw === null) return '俯瞰内江·圣水寺·隆昌石牌坊·国网大楼'
      return this.heroCaptionRaw
    },
  },
  methods: {
    formatTime(val) {
      return dateStr(val)
    },
    // 单格背景：后台配了图就用配置，否则用打包内置图
    heroShotStyle(no, fallback) {
      return { backgroundImage: `url(${this.heroConfig[no] || fallback})` }
    },
    // 读取后台「系统配置 → 首页 Hero」（分组 home）；任何异常都回落到内置图
    loadHeroConfig() {
      this.postRequest('/common/systemConfig/listByGroup', { configGroup: 'home' }).then(resp => {
        const list = resp && resp.obj && Array.isArray(resp.obj) ? resp.obj : []
        const pick = key => {
          const item = list.find(c => c.configKey === key)
          const raw = item && item.configValue != null ? String(item.configValue).trim() : ''
          return raw ? normalizeFileUrl(raw) : ''
        }
        this.heroConfig = {
          1: pick('hero_image_1'),
          2: pick('hero_image_2'),
          3: pick('hero_image_3'),
          4: pick('hero_image_4'),
        }
        const caption = list.find(c => c.configKey === 'hero_caption')
        this.heroCaptionRaw = caption && caption.configValue != null ? String(caption.configValue).trim() : null
      }).catch(() => {})
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
          authorIconSvg: a.authorIconSvg || '',
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
    /** 统一卡片数据：时间格式与标签名在这里一次性补齐 */
    postItem(article) {
      return {
        ...article,
        time: this.formatTime(article.time),
        labelName: this.articleLabelName(article),
      }
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
        const list = (resp && Array.isArray(resp.obj)) ? resp.obj : []
        this.featuredShowcase = list.map(item => {
          const cover = normalizeFileUrl(item.cover || '')
          // 没有上传封面时，用标题/正文/作者现场生成内容卡，避免跑马灯左侧空白
          const generatedCover = cover ? '' : generateFeaturedCover({
            articleId: item.articleId,
            title: item.title,
            summary: item.summary,
            content: item.content,
            author: item.author,
            views: item.views,
          })
          return { ...item, cover, generatedCover }
        })
        if (this.showcaseIndex >= this.featuredShowcase.length) this.showcaseIndex = 0
      }).catch(() => { this.featuredShowcase = [] })
    },
    /** 封面地址失效时，退回到现场生成的内容卡 */
    onShowcaseCoverError() {
      const item = this.currentShowcase
      if (!item || !item.cover) return
      if (!item.generatedCover) {
        item.generatedCover = generateFeaturedCover({
          articleId: item.articleId,
          title: item.title,
          summary: item.summary,
          content: item.content,
          author: item.author,
          views: item.views,
        })
      }
      item.cover = ''
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
  /* 文字后方暗晕的横向半径：桌面端只覆盖标题所在的中段，左右两侧保持明亮 */
  --hero-scrim-w: 35%;
}

/* 窄屏标题几乎占满宽度，暗晕随之加宽，否则字尾落在亮底上 */
@media (min-width: 768px) and (max-width: 1100px) {
  .hero-photo {
    --hero-scrim-w: 46%;
  }
}

.hero-photo__media {
  position: absolute;
  inset: 0;
  display: flex;
  /* 原片偏灰（阴天/逆光），整体提亮并补一点饱和度，让左右两侧的场景看得清 */
  filter: brightness(1.16) saturate(1.1) contrast(1.03);
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
  /* 竖构图按 38% 纵向取景，保住楼体与"人民电业为人民"标语 */
  object-position: center 38%;
  /* 轻微推近：右侧供图是竖幅，放大后左缘的大片白色天空被挤出画面，
     与石牌坊的叠接处不再出现一条"发白的空带" */
  transform: scale(1.18);
  /* 与其余照片同亮度，不再单独压暗（领导反馈右侧整体偏暗） */
  filter: brightness(1.02) saturate(0.98);
}

/* 后台换成自定义图时不再做竖幅推近/取景，直接 cover 居中，避免任意图被裁得看不出内容 */
.hero-photo__slot--custom .hero-photo__slot-img {
  object-position: center;
  transform: none;
  filter: none;
}

/* 压暗层：只在标题/正文后方做柔和暗晕（保证字形对比度），左右两侧不做整体压暗，保持明亮 */
.hero-photo__scrim {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse var(--hero-scrim-w) 126% at 50% 50%,
      rgba(6, 16, 33, 0.60) 0%,
      rgba(6, 16, 33, 0.40) 38%,
      rgba(6, 16, 33, 0.14) 66%,
      rgba(6, 16, 33, 0) 84%),
    /* 极轻的上下过渡：只服务版权行与边缘收口，不削弱主体亮度 */
    linear-gradient(180deg,
      rgba(6, 16, 33, 0.08) 0%,
      rgba(6, 16, 33, 0) 34%,
      rgba(6, 16, 33, 0) 62%,
      rgba(6, 16, 33, 0.18) 100%);
}

/* Hero 文案：背景整体提亮后靠字体自身撑对比度——
   近黑描边阴影（贴合笔画）+ 大范围柔光暗影（压住周围杂色），比单层 drop-shadow 更耐亮底 */
.hero-title {
  color: #fff;
  letter-spacing: 0.01em;
  text-shadow:
    0 1px 3px rgba(3, 10, 22, 0.92),
    0 4px 18px rgba(3, 10, 22, 0.75),
    0 12px 42px rgba(3, 10, 22, 0.5);
}

.hero-subtitle {
  color: #fff;
  font-weight: 500;
  text-shadow:
    0 1px 4px rgba(3, 10, 22, 0.95),
    0 3px 16px rgba(3, 10, 22, 0.8);
}

/* Hero 数据胶囊：轻量暗玻璃底，亮画面上也能看清，但不抢标题 */
.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 11.5px;
  line-height: 1;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(8, 20, 38, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(4px);
}

.hero-photo__credits {
  position: absolute;
  right: 16px;
  bottom: 6px;
  z-index: 10;
  font-size: 10px;
  line-height: 1.2;
  color: rgba(255, 255, 255, 0.62);
  text-shadow: 0 1px 3px rgba(3, 10, 22, 0.7);
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
    /* 移动端文字已在纯色底上，影像条只需轻微压边收口，保持明亮 */
    background: linear-gradient(180deg,
      rgba(6, 16, 33, 0.16) 0%,
      rgba(6, 16, 33, 0.30) 70px,
      rgba(6, 16, 33, 0.48) 128px,
      #0a1830 128px);
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
