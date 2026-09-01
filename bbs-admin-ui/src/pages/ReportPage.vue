<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-amber-600">feedback</span>
            举报管理
          </h1>
          <p class="text-body-md text-secondary mt-1">审核用户举报内容（同一内容的重复举报已折叠）</p>
        </div>
      </div>

      <!-- Filter -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center gap-3">
          <select v-model="filterStatus" class="px-3 py-2 bg-surface border border-outline-variant rounded-lg text-body-sm focus:border-primary outline-none">
            <option value="">全部状态</option>
            <option value="pending">待审核</option>
            <option value="confirmed">已确认</option>
            <option value="rejected">已驳回</option>
          </select>
          <button class="px-3 py-2 bg-primary-container text-on-primary-container rounded-lg hover:opacity-90 text-body-sm" @click="loadList">查询</button>
        </div>
      </div>

      <!-- List：按举报目标分组，折叠重复举报 -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center text-on-surface-variant">
            <p class="text-body-md">暂无举报记录</p>
          </div>
          <table v-else class="w-full text-left">
            <thead class="bg-surface-container-low">
              <tr>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">举报人</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">目标</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">原因</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">状态</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">时间</th>
                <th class="px-4 py-3 text-body-sm font-medium text-on-surface-variant">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/50">
              <tr v-for="group in list" :key="group.representative.id" class="hover:bg-surface-container-low/50">
                  <td class="px-4 py-3 text-body-sm">{{ group.representative.reporterName || group.representative.reporterId }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <div class="flex items-center gap-2">
                      <span class="text-primary cursor-pointer hover:underline" @click="openPreview(group.representative)">
                        {{ getTargetTypeLabel(group.representative.targetType) }}#{{ group.representative.targetId }}
                      </span>
                      <el-popover
                        v-if="group.totalCount > 1"
                        placement="bottom"
                        trigger="click"
                        :popper-class="'report-history-popover'"
                        width="420"
                      >
                        <div slot="reference" class="px-2 py-0.5 rounded text-[11px] font-medium cursor-pointer bg-blue-50 text-blue-700 hover:bg-blue-100">
                          {{ group.totalCount }} 条举报 ▼
                        </div>
                        <div class="space-y-2 max-h-[300px] overflow-y-auto">
                          <div v-for="item in getHistory(group)" :key="item.id" class="flex items-center gap-2 text-[12px] py-1.5 border-b border-outline-variant/30 last:border-0">
                            <span class="text-on-surface-variant flex-shrink-0">{{ item.reporterName || '举报人 ' + item.reporterId }}</span>
                            <span :class="{
                              'px-1.5 py-0.5 rounded font-medium flex-shrink-0 whitespace-nowrap': true,
                              'bg-yellow-100 text-yellow-800': item.status === 'pending',
                              'bg-green-100 text-green-800': item.status === 'confirmed',
                              'bg-red-100 text-red-800': item.status === 'rejected'
                            }">{{ getStatusLabel(item.status) }}</span>
                            <span class="text-on-surface-variant flex-1 truncate" :title="item.reason">{{ item.reason || '无原因' }}</span>
                            <span class="text-outline flex-shrink-0">{{ item.createTime }}</span>
                          </div>
                        </div>
                        <p class="text-[11px] text-outline mt-1">共 {{ group.totalCount }} 条举报（含代表记录）</p>
                      </el-popover>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-body-sm max-w-[200px] truncate" :title="group.representative.reason">{{ group.representative.reason || '-' }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <span :class="{
                      'px-2 py-0.5 rounded text-[12px] font-medium whitespace-nowrap': true,
                      'bg-yellow-100 text-yellow-800': group.representative.status === 'pending',
                      'bg-green-100 text-green-800': group.representative.status === 'confirmed',
                      'bg-red-100 text-red-800': group.representative.status === 'rejected'
                    }">{{ getStatusLabel(group.representative.status) }}</span>
                  </td>
                  <td class="px-4 py-3 text-body-sm text-on-surface-variant">{{ group.representative.createTime }}</td>
                  <td class="px-4 py-3 text-body-sm">
                    <div v-if="group.representative.status === 'pending'" class="flex items-center gap-1 whitespace-nowrap">
                      <button class="px-2 py-1 border border-outline-variant text-on-surface-variant rounded text-[12px] hover:bg-surface-container-low transition-colors" @click="handleReview(group.representative, 'confirmed')">仅确认</button>
                      <button class="px-2 py-1 bg-error text-on-error rounded text-[12px] hover:opacity-90 transition-opacity shadow-sm" @click="openViolationDialog(group.representative)">确认并扣分</button>
                      <button class="px-2 py-1 border border-error/40 text-error rounded text-[12px] hover:bg-error/5 transition-colors" @click="handleReview(group.representative, 'rejected')">驳回</button>
                    </div>
                    <span v-else class="text-on-surface-variant text-[12px]">{{ group.representative.reviewRemark || '已处理' }}</span>
                  </td>
                </tr>
            </tbody>
          </table>
        </div>
        <div class="flex justify-end mt-4" v-if="total > pageSize">
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
          <span class="px-3 py-1 text-body-sm text-on-surface-variant">{{ currentPage }}/{{ Math.ceil(total / pageSize) }}</span>
          <button class="px-3 py-1 border rounded text-body-sm" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- 内容预览弹窗（复用 ArticlePage 帖子详情样式） -->
    <div v-if="previewVisible" class="fixed inset-0 bg-black/30 z-40" @click="closePreview"></div>
    <div v-if="previewVisible" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="bg-container w-full max-w-4xl mx-auto my-[5vh] rounded-xl shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">article</span>
            <template v-if="isArticleContext">
              {{ detailTitle ? '文章《' + detailTitle + '》详情' : '文章详情' }}
              <span v-if="previewItem && previewItem.targetType !== 'article'" class="text-[12px] font-normal text-amber-600 ml-1">（含{{ getTargetTypeLabel(previewItem.targetType) }}举报）</span>
            </template>
            <template v-else>
              {{ previewItem ? getTargetTypeLabel(previewItem.targetType) + '详情' : '内容预览' }}
            </template>
          </h3>
          <button class="text-outline hover:text-error transition-colors" @click="closePreview">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <!-- Body -->
        <div class="p-5" v-loading="previewLoading">
          <template v-if="previewItem">
            <!-- 举报原因（仅当评论/回复无所属文章时显示在顶部） -->
            <div v-if="previewItem.targetType !== 'article' && !previewItem.targetArticleId" class="mb-4 px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-600 text-[16px]">flag</span>
              <span class="text-[12px] font-medium text-amber-700">举报原因：</span>
              <span class="text-[13px] text-amber-800">{{ previewItem.reason || '无' }}</span>
            </div>

            <!-- 文章详情（复用 ArticlePage 帖子详情样式） -->
            <template v-if="isArticleContext">
              <!-- 文章彻底不存在（非软删除，数据库中无记录） -->
              <div v-if="articleLoadFailed" class="py-12 text-center">
                <span class="material-symbols-outlined text-outline text-[48px]">article</span>
                <p class="mt-3 text-body-md text-on-surface-variant">该文章已被彻底删除（数据库中无记录）</p>
              </div>
              <template v-else>
              <!-- 整篇文章（标题+正文+附件）高亮容器 -->
              <div v-if="detailTitle" class="mb-6 relative report-highlighted rounded-lg p-5 border border-amber-300">
                <div class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                <h2 class="font-headline-md text-headline-md text-on-surface flex items-center gap-2">
                  标题：《{{ detailTitle }}》
                  <span v-if="articleDeleted" class="px-2 py-0.5 rounded text-[12px] font-medium bg-red-100 text-red-700 border border-red-200">已删除</span>
                </h2>
                <div class="flex items-center gap-3 mt-2">
                  <span class="text-body-md text-on-surface-variant flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">person</span>
                    {{ previewItem.targetAuthorName || '未知' }} (ID: {{ previewItem.targetAuthorId || '?' }})
                  </span>
                </div>
                <div class="mt-2 px-3 py-2 bg-amber-100 border border-amber-200 rounded-lg inline-flex items-center gap-2">
                  <span class="material-symbols-outlined text-amber-600 text-[16px]">flag</span>
                  <span class="text-[12px] font-medium text-amber-700">被举报内容 — {{ previewItem.reason || '无原因' }}</span>
                </div>
                <!-- 已删除文章提示 -->
                <div v-if="articleDeleted" class="mt-3 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                  <span class="material-symbols-outlined text-red-500 text-[16px]">info</span>
                  <span class="text-[13px] text-red-700">该文章已被用户删除（管理员仍可查看内容）</span>
                </div>
                <!-- 正文 -->
                <div class="mt-4 markdown-body detail-content" v-html="renderedContent"></div>
                <!-- 附件 -->
                <div v-if="detailFileList && detailFileList.length > 0" class="mt-6 bg-surface-container-low rounded-lg p-4">
                  <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary text-[20px]">attach_file</span>
                    附件列表
                  </h4>
                  <div class="space-y-2">
                    <div v-for="(file, index) in detailFileList" :key="index" class="flex items-center justify-between p-3 bg-container rounded border border-outline-variant/50">
                      <span class="font-body-md text-on-surface flex items-center gap-2">
                        <span class="material-symbols-outlined text-outline text-[18px]">description</span>
                        {{ file.fileName }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 评论 -->
              <div class="mt-6">
                <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-[20px]">comment</span>
                  评论（{{ detailCommentCount }}）
                </h4>
                <div v-if="detailComments.length > 0" class="space-y-4">
                  <div v-for="(item, index) in detailComments" :key="item.commentId || index"
                    :class="[
                      'rounded-lg p-4 border',
                      isHighlightedComment(item) ? 'bg-amber-50 border-amber-300 relative report-highlighted' : 'bg-surface-container-low border-outline-variant/50'
                    ]">
                    <!-- 被举报评论的高亮标识 -->
                    <div v-if="isHighlightedComment(item)" data-report-highlight class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                    <div v-if="isHighlightedComment(item)" class="mb-2 px-3 py-1.5 bg-amber-100 border border-amber-200 rounded-lg inline-flex items-center gap-1.5">
                      <span class="material-symbols-outlined text-amber-600 text-[14px]">flag</span>
                      <span class="text-[11px] font-medium text-amber-700">被举报内容 — {{ previewItem.reason || '无原因' }}</span>
                    </div>
                    <div class="flex items-center gap-3 mb-2">
                      <img class="w-9 h-9 rounded-full bg-surface-variant object-cover" :src="item.portrait || defaultAvatar" alt="">
                      <div>
                        <span class="font-headline-sm text-headline-sm text-on-surface">{{ item.nickname || '未知用户' }}</span>
                        <span class="text-body-md text-on-surface-variant ml-2">{{ item.commentTime }}</span>
                      </div>
                    </div>
                    <p class="text-body-md text-on-surface ml-12">{{ item.commentContent }}</p>
                    <div v-if="item.reply && item.reply.length" class="ml-12 mt-3 pl-4 border-l-2 border-outline-variant/30 space-y-3">
                      <div v-for="(reply, rIdx) in item.reply" :key="reply.replyId || rIdx"
                        :class="[
                          'rounded-lg p-3 relative',
                          isHighlightedReply(reply) ? 'bg-amber-50 border border-amber-300 relative report-highlighted' : 'bg-surface-container'
                        ]">
                        <!-- 被举报回复的高亮标识 -->
                        <div v-if="isHighlightedReply(reply)" data-report-highlight class="absolute -left-3 top-0 bottom-0 w-1 bg-amber-500 rounded-full"></div>
                        <div v-if="isHighlightedReply(reply)" class="mb-1.5 px-2 py-1 bg-amber-100 border border-amber-200 rounded inline-flex items-center gap-1">
                          <span class="material-symbols-outlined text-amber-600 text-[12px]">flag</span>
                          <span class="text-[10px] font-medium text-amber-700">被举报内容 — {{ previewItem.reason || '无原因' }}</span>
                        </div>
                        <div class="flex items-center gap-2 mb-1.5">
                          <img class="w-7 h-7 rounded-full bg-surface-variant object-cover" :src="reply.portrait || defaultAvatar" alt="">
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
                <div v-else class="py-6 text-center text-on-surface-variant text-body-md">
                  暂无评论
                </div>
              </div>
              </template>
            </template>

            <!-- 评论/回复详情（所属文章不存在或无 articleId 时） -->
            <template v-else>
              <div v-if="articleLoadFailed" class="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                <span class="material-symbols-outlined text-red-500 text-[16px]">error</span>
                <span class="text-[13px] text-red-700">所属文章已删除或不存在，无法加载完整上下文</span>
              </div>
              <div class="flex items-center gap-3 mb-3">
                <span class="px-2 py-0.5 rounded text-[12px] font-medium bg-surface-container-low text-on-surface-variant">
                  {{ getTargetTypeLabel(previewItem.targetType) }}
                </span>
                <span class="text-body-md text-on-surface-variant">
                  作者：{{ previewItem.targetAuthorName || '未知' }} (ID: {{ previewItem.targetAuthorId || '?' }})
                </span>
              </div>
              <div v-if="previewItem.targetContent" class="text-body-md text-on-surface whitespace-pre-wrap leading-relaxed bg-surface-container-low rounded-lg p-4">{{ previewItem.targetContent }}</div>
              <div v-else class="text-body-md text-outline italic py-8 text-center">（内容已删除或无法加载）</div>
            </template>
          </template>
        </div>
      </div>
    </div>

    <!-- 转违规对话框 -->
    <el-dialog title="确认举报并扣分" :visible.sync="violationDialogVisible" width="500px" :close-on-click-modal="false">
      <div class="space-y-4">
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">违规用户</label>
          <div class="w-full px-3 py-2 bg-surface-container-low border border-outline-variant rounded-lg text-body-sm">
            {{ violationDialogItem ? (violationDialogItem.targetAuthorName || '未知') + ' (ID: ' + (violationDialogItem.targetAuthorId || '?') + ')' : '' }}
          </div>
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">违规类型 <span class="text-red-500">*</span></label>
          <select v-model="violationForm.violationType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none">
            <option value="">请选择</option>
            <option v-for="opt in violationOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="block text-body-sm text-on-surface-variant mb-1">审核备注</label>
          <textarea v-model="violationForm.remark" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none" rows="2" placeholder="填写扣分依据（选填）"></textarea>
        </div>
        <p class="text-body-sm text-on-surface-variant">关联内容: {{ violationDialogItem ? getTargetTypeLabel(violationDialogItem.targetType) + '#' + violationDialogItem.targetId : '' }}</p>
      </div>
      <div slot="footer" class="flex justify-end gap-2">
        <button class="px-4 py-2 text-body-sm border border-outline-variant rounded-lg hover:bg-surface-container-low" @click="violationDialogVisible = false">取消</button>
        <button class="px-4 py-2 text-body-sm bg-primary text-white rounded-lg hover:opacity-90 disabled:opacity-60" :disabled="violationSubmitting" @click="submitViolation">
          {{ violationSubmitting ? '提交中...' : '确认并扣分' }}
        </button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it/dist/markdown-it'
import 'mavon-editor/dist/markdown/github-markdown.min.css'

export default {
  name: 'ReportPage',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      filterStatus: '',
      violationOptions: [],
      // 内容预览弹窗
      previewVisible: false,
      previewItem: null,
      previewLoading: false,
      detailTitle: '',
      detailContent: '',
      detailComments: [],
      detailFileList: [],
      defaultAvatar: require('../assets/img/img.jpeg'),
      // 文章上下文加载状态（用于评论/回复举报时判断文章是否存在）
      articleLoadFailed: false,
      // 文章是否已被删除（软删除，管理员仍可见内容）
      articleDeleted: false,
      // 转违规对话框
      violationDialogVisible: false,
      violationDialogItem: null,
      violationSubmitting: false,
      violationForm: {
        violationType: '',
        remark: ''
      }
    }
  },
  computed: {
    /** 是否在文章上下文中（举报目标是文章，或评论/回复有所属文章且文章加载成功） */
    isArticleContext() {
      if (!this.previewItem) return false
      if (this.previewItem.targetType === 'article') return true
      // 评论/回复有所属文章，且文章加载成功（非 loading 且未失败）
      return this.previewItem.targetArticleId && !this.articleLoadFailed
    },
    renderedContent() {
      if (!this.detailContent) return ''
      return this._renderMarkdown(this.detailContent)
    },
    detailCommentCount() {
      if (!this.detailComments.length) return 0
      return this.detailComments.reduce((sum, item) => {
        return sum + 1 + (item.reply && item.reply.length ? item.reply.length : 0)
      }, 0)
    }
  },
  mounted() {
    this.loadViolationOptions()
    this.loadList()
  },
  methods: {
    async loadViolationOptions() {
      try {
        const res = await this.postRequest('/admin/listDict', {})
        if (res && res.code == 200 && Array.isArray(res.obj)) {
          this.violationOptions = res.obj
            .filter(d => d.dictType === 'violation')
            .sort((a, b) => (a.dictSort || 0) - (b.dictSort || 0))
            .map(d => ({
              value: d.dictKey,
              label: d.dictLabel + (d.dictValue ? ' (-' + d.dictValue + '分)' : '')
            }))
        }
      } catch (e) { /* ignore */ }
    },
    getStatusLabel(s) { return { pending: '待审核', confirmed: '已确认', rejected: '已驳回' }[s] || s },
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    getHistory(group) {
      return group.members.filter(m => m.id !== group.representative.id)
    },
    /** 判断该评论是否是被举报的目标 */
    isHighlightedComment(comment) {
      if (!this.previewItem) return false
      return this.previewItem.targetType === 'comment' &&
        String(this.previewItem.targetId) === String(comment.commentId)
    },
    /** 判断该回复是否是被举报的目标 */
    isHighlightedReply(reply) {
      if (!this.previewItem) return false
      return this.previewItem.targetType === 'reply' &&
        String(this.previewItem.targetId) === String(reply.replyId)
    },
    /** 滚动到被举报的评论/回复（带重试，确保 DOM 已渲染） */
    scrollToHighlighted(retries = 3) {
      if (!this.previewItem) return
      const panel = this.$el.querySelector('.z-50')
      if (!panel) return
      // 查找被举报内容的高亮标记（data-report-highlight）
      const highlighted = panel.querySelector('[data-report-highlight]')
      if (highlighted) {
        // 向上找到最近的评论/回复卡片
        const card = highlighted.closest('.rounded-lg')
        if (card) {
          card.scrollIntoView({ behavior: 'smooth', block: 'center' })
          return
        }
      }
      // DOM 可能还没渲染完，重试
      if (retries > 0) {
        setTimeout(() => this.scrollToHighlighted(retries - 1), 200)
      }
    },
    _renderMarkdown(content) {
      if (!this._md) {
        this._md = new MarkdownIt({
          html: true,
          xhtmlOut: true,
          breaks: true,
          linkify: false,
          typographer: true
        })
      }
      return this._md.render(content)
    },
    openPreview(item) {
      this.previewItem = item
      this.previewVisible = true
      this.detailTitle = ''
      this.detailContent = ''
      this.detailComments = []
      this.detailFileList = []
      this.articleLoadFailed = false
      this.articleDeleted = false
      if (item.targetType === 'article') {
        this.loadArticleDetail(item.targetId)
      } else if (item.targetType === 'comment' || item.targetType === 'reply') {
        // 评论/回复 → 加载所属文章，然后高亮定位
        const articleId = item.targetArticleId
        if (articleId) {
          this.loadArticleDetail(articleId)
        }
      }
    },
    closePreview() {
      this.previewVisible = false
      this.previewItem = null
      this.detailTitle = ''
      this.detailContent = ''
      this.detailComments = []
      this.detailFileList = []
      this.articleLoadFailed = false
      this.articleDeleted = false
    },
    loadArticleDetail(articleId) {
      this.previewLoading = true
      this.articleLoadFailed = false
      this.articleDeleted = false
      // 使用含已删除的接口，管理员可查看已删除文章
      this.getRequest('/admin/getArticleByIdInclDeleted', articleId).then(resp => {
        this.previewLoading = false
        if (resp && resp.obj) {
          this.detailContent = resp.obj.articleContent || ''
          this.detailTitle = resp.obj.articleTitle || ''
          // 检测是否已删除（isDelete: 1 = 已删除）
          this.articleDeleted = resp.obj.isDelete === 1
          this.loadArticleFiles(articleId)
          this.loadArticleComments(articleId)
        } else {
          // 文章真正不存在（被彻底删除）
          this.articleLoadFailed = true
        }
      }).catch(() => { this.previewLoading = false; this.articleLoadFailed = true })
    },
    loadArticleFiles(articleId) {
      this.postRequest(`/common/getArticleFileByArticleId/${articleId}`, {}).then(res => {
        let list = []
        if (Array.isArray(res)) list = res
        else if (res && Array.isArray(res.obj)) list = res.obj
        else if (res && Array.isArray(res.listBean)) list = res.listBean
        this.detailFileList = list
      }).catch(() => { this.detailFileList = [] })
    },
    loadArticleComments(articleId) {
      this.postRequest(`/common/comment/getCommentReply/${articleId}`).then(res => {
        const raw = (res && Array.isArray(res)) ? res : []
        this.detailComments = raw.map(c => ({
          ...c,
          portrait: c.portrait || '',
          reply: (c.reply || []).map(r => ({
            ...r,
            portrait: r.portrait || '',
          }))
        }))
        // 加载完成后滚动到被举报的评论/回复（setTimeout 确保 DOM 已渲染）
        if (this.previewItem && this.previewItem.targetType !== 'article') {
          setTimeout(() => {
            this.scrollToHighlighted()
          }, 150)
        }
      }).catch(() => { this.detailComments = [] })
    },
    async loadList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.filterStatus) params.status = this.filterStatus
        const res = await this.postRequest('/admin/report/listGrouped', params)
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          this.total = res.obj.total || 0
        } else { this.list = [] }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    handleReview(item, status) {
      const group = this.list.find(g => g.members.some(m => m.id === item.id))
      const groupHint = group && group.totalCount > 1 ? `该内容共 ${group.totalCount} 条举报，将一并确认。` : ''
      const title = status === 'confirmed'
        ? `仅确认举报（不扣分）？${groupHint}举报人各 +2 分`
        : `确定驳回该举报？`
      this.$prompt('审核备注（可选）', title, { type: status === 'confirmed' ? 'success' : 'warning' })
        .then(({ value }) => this.doReview(item.id, status, value))
        .catch(() => {})
    },
    async doReview(reportId, status, remark) {
      try {
        const res = await this.postRequest('/admin/report/review', { reportId, reviewerId: 1, status, remark })
        if (res && res.code == 200) {
          this.$message.success('审核完成')
          await this.loadList()
        } else {
          this.$message.error((res && res.message) || '审核失败')
        }
      } catch (e) { this.$message.error('审核失败') }
    },
    // 转违规
    openViolationDialog(item) {
      this.violationDialogItem = item
      this.violationForm = {
        violationType: item.violationType || '',
        remark: item.reason || ''
      }
      this.violationDialogVisible = true
    },
    async submitViolation() {
      if (!this.violationDialogItem.targetAuthorId) {
        this.$message.warning('无法获取被举报内容的作者信息')
        return
      }
      if (!this.violationForm.violationType) {
        this.$message.warning('请选择违规类型')
        return
      }
      this.violationSubmitting = true
      const remark = this.violationForm.remark || ''
      try {
        const vRes = await this.postRequest('/admin/violation/add', {
          userId: this.violationDialogItem.targetAuthorId,
          violationType: this.violationForm.violationType,
          relatedType: this.violationDialogItem.targetType,
          relatedId: parseInt(this.violationDialogItem.targetId),
          operatorId: 1,
          remark: remark
        })
        if (!vRes || vRes.code != 200) {
          this.$message.error((vRes && vRes.message) || '创建违规失败')
          return
        }
        const rRes = await this.postRequest('/admin/report/review', {
          reportId: this.violationDialogItem.id,
          reviewerId: 1,
          status: 'confirmed',
          remark: remark || '已转违规处理'
        })
        if (rRes && rRes.code == 200) {
          this.$message.success('违规已创建，举报已确认')
          this.violationDialogVisible = false
          await this.loadList()
        } else {
          this.$message.warning('违规已创建，但举报确认失败，请手动处理')
          this.violationDialogVisible = false
          await this.loadList()
        }
      } catch (e) {
        this.$message.error('操作失败')
      } finally {
        this.violationSubmitting = false
      }
    },
    changePage(page) { this.currentPage = page; this.loadList() }
  }
}
</script>

<style scoped>
/* 被举报内容高亮脉冲动画 */
@keyframes report-highlight-pulse {
  0%, 100% { background-color: rgb(255 251 235); }   /* amber-50 */
  50%      { background-color: rgb(254 243 199); }   /* amber-100 */
}
.report-highlighted {
  animation: report-highlight-pulse 2s ease-in-out 3;
  box-shadow: inset 0 0 0 2px rgb(251 191 36);      /* amber-400 border */
}
</style>
