<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">rate_review</span>
            帖子管理
          </h1>
          <p class="text-body-md text-secondary mt-1">审核和管理所有用户发布的帖子</p>
        </div>
      </div>

      <!-- Search & Filter Bar -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <!-- 搜索框 -->
          <div class="grid grid-cols-1 grid-rows-1">
            <input
              v-model="searchForm.keywords"
              class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2.5 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface placeholder:text-outline focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
              placeholder="搜索用户名、标题..."
              @keyup.enter="handleSearch"
            />
            <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">search</span>
          </div>
          <!-- 标签筛选 -->
          <select
            v-model="searchForm.labelId"
            class="w-full h-10 px-4 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all appearance-none cursor-pointer"
            @change="handleSearch"
          >
            <option value="">全部标签</option>
            <option v-for="label in labels" :key="label.labelId" :value="String(label.labelId)">{{ label.labelName }}</option>
          </select>
          <!-- 开始时间 -->
          <input
            v-model="searchForm.startTime"
            type="date"
            class="w-full h-10 px-4 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
          />
          <!-- 结束时间 -->
          <input
            v-model="searchForm.endTime"
            type="date"
            class="w-full h-10 px-4 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
          />
        </div>
        <div class="flex items-center gap-3 mt-4">
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleSearch">
            <span class="material-symbols-outlined text-[18px]">search</span>
            搜索
          </button>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="handleReset">
            <span class="material-symbols-outlined text-[18px]">clear</span>
            重置
          </button>
          <span class="text-body-md text-on-surface-variant ml-auto">{{ pagination.total }} 条结果</span>
        </div>
      </div>

      <!-- Tabs -->
      <div class="bg-container border border-border rounded-xl overflow-hidden mb-6">
        <div class="flex border-b border-border">
          <button
            class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative"
            :class="activeTab === 'done' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'"
            @click="switchTab('done')"
          >
            已审核
            <span class="ml-2 px-3 py-[3px] text-xs font-medium rounded-full" :class="activeTab === 'done' ? 'bg-primary text-on-primary' : 'bg-surface-variant text-on-surface-variant'">{{ tabCounts.done }}</span>
            <span v-if="activeTab === 'done'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button
            class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative"
            :class="activeTab === 'pending' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'"
            @click="switchTab('pending')"
          >
            未审核
            <span class="ml-2 px-3 py-[3px] text-xs font-medium rounded-full" :class="activeTab === 'pending' ? 'bg-primary text-on-primary' : 'bg-surface-variant text-on-surface-variant'">{{ tabCounts.pending }}</span>
            <span v-if="activeTab === 'pending'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
          <button
            class="flex-1 py-3.5 text-center font-headline-sm text-headline-sm transition-colors relative"
            :class="activeTab === 'featured' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface'"
            @click="switchTab('featured')"
          >
            精华帖管理
            <span class="ml-2 px-3 py-[3px] text-xs font-medium rounded-full" :class="activeTab === 'featured' ? 'bg-rank-gold text-white' : 'bg-surface-variant text-on-surface-variant'">{{ tabCounts.featured }}</span>
            <span v-if="activeTab === 'featured'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-16">
          <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>

        <!-- Empty -->
        <div v-else-if="articleList.length === 0" class="py-16 text-center flex flex-col items-center gap-2 text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px] opacity-20">inbox</span>
          <p class="text-body-md">暂无数据</p>
        </div>

        <!-- Article List -->
        <div v-else class="divide-y divide-outline-variant/30">
          <div v-for="article in articleList" :key="article.articleId" class="p-card-padding hover:bg-surface-container-low/50 transition-colors">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-2">
                  <!-- 精华帖标识 -->
                  <span v-if="article.isFeatured === 1" class="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium bg-rank-gold/10 text-rank-gold rounded-full">
                    <span class="material-symbols-outlined text-[12px]">stars</span>
                    精华
                  </span>
                  <!-- 标签标识 -->
                  <span v-if="article.articleLabelName" class="inline-flex items-center px-2 py-0.5 text-[11px] font-medium bg-primary/10 text-primary rounded-full">
                    {{ article.articleLabelName }}
                  </span>
                </div>
                <span class="font-headline-sm text-headline-sm text-primary cursor-pointer hover:underline" @click="handleRead(article.articleId)">
                  《{{ article.articleTitle }}》
                </span>
                <div class="flex items-center gap-4 mt-1.5 text-body-md text-on-surface-variant">
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[14px]">person</span>
                    {{ article.articleAuthor || '未知' }}
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[14px]">schedule</span>
                    {{ article.createTime }}
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[14px]">visibility</span>
                    {{ article.articleViewNum || 0 }}
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[14px]">chat_bubble</span>
                    {{ article.commentNum || 0 }}
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <!-- 精华帖设置（仅已审核帖子） -->
                <button
                  v-if="article.enable === 1"
                  class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium rounded transition-colors"
                  :class="article.isFeatured === 1 ? 'bg-rank-gold/10 text-rank-gold hover:bg-rank-gold/20' : 'bg-surface-variant text-on-surface-variant hover:text-rank-gold hover:bg-rank-gold/10'"
                  @click="handleToggleFeatured(article)"
                >
                  <span class="material-symbols-outlined text-[14px]">{{ article.isFeatured === 1 ? 'stars' : 'star_border' }}</span>
                  {{ article.isFeatured === 1 ? '取消精华' : '设为精华' }}
                </button>
                <!-- 通过审核（未审核帖子） -->
                <button
                  v-if="article.enable === 0 || article.enable === 0"
                  class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors"
                  @click="handleAudit(article.articleId)"
                >
                  <span class="material-symbols-outlined text-[14px]">check</span>
                  通过审核
                </button>
                <!-- 采纳建议（建议反馈标签 + 已审核 + 未采纳 + 非作者本人） -->
                <template v-if="article.articleLabelName === '建议反馈' && article.enable === 1">
                  <span
                    v-if="article.isSuggestionAdopted === true"
                    class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-green-700 bg-green-50 rounded border border-green-200"
                  >
                    <span class="material-symbols-outlined text-[14px]">check_circle</span>
                    已采纳
                  </span>
                  <button
                    v-else
                    class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-amber-700 bg-amber-50 rounded hover:bg-amber-100 transition-colors border border-amber-200"
                    @click="handleAdoptSuggestion(article)"
                  >
                    <span class="material-symbols-outlined text-[14px]">lightbulb</span>
                    采纳建议
                  </button>
                </template>
                <!-- 删除 -->
                <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDel(article.articleId)">
                  <span class="material-symbols-outlined text-[14px]">delete</span>
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="pagination.pages > 1" class="flex items-center justify-between px-card-padding py-4 border-t border-outline-variant/30 bg-surface-container-lowest">
          <span class="text-body-md text-on-surface-variant">
            共 {{ pagination.total }} 条，第 {{ pagination.page }}/{{ pagination.pages }} 页
          </span>
          <div class="flex items-center gap-2">
            <button
              class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
              :disabled="pagination.page <= 1"
              @click="changePage(pagination.page - 1)"
            >
              <span class="material-symbols-outlined text-[16px]">chevron_left</span>
            </button>
            <button
              v-for="p in pageNumbers"
              :key="p"
              class="min-w-[32px] h-8 px-2 flex items-center justify-center rounded text-[13px] font-medium transition-all"
              :class="p === pagination.page ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-variant'"
              @click="changePage(p)"
            >{{ p }}</button>
            <button
              class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30"
              :disabled="pagination.page >= pagination.pages"
              @click="changePage(pagination.page + 1)"
            >
              <span class="material-symbols-outlined text-[16px]">chevron_right</span>
            </button>
          </div>
        </div>

        <!-- Batch Actions -->
        <div v-if="activeTab !== 'featured'" class="flex items-center gap-3 px-card-padding py-4 border-t border-outline-variant/30 bg-surface-container-lowest">
          <button v-if="activeTab === 'done'" class="inline-flex items-center gap-1.5 px-4 py-2 bg-error text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleBatchDeleteArticlesByAlive">
            <span class="material-symbols-outlined text-[18px]">delete_sweep</span>
            删除全部已审核文章
          </button>
          <button v-if="activeTab === 'pending'" class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleBatchAudit">
            <span class="material-symbols-outlined text-[18px]">fact_check</span>
            全部通过审核
          </button>
        </div>
      </div>
    </div>

    <!-- Article Detail Dialog -->
    <div v-if="detailVisible" class="fixed inset-0 bg-black/30 z-40" @click="closeDetail"></div>
    <div v-if="detailVisible" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="bg-container w-full max-w-4xl mx-auto my-[5vh] rounded-xl shadow-2xl">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">article</span>
            帖子详情
          </h3>
          <button class="text-outline hover:text-error transition-colors" @click="closeDetail">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5" v-loading="detailLoading">
          <div v-if="detailTitle" class="mb-4">
            <h2 class="font-headline-md text-headline-md text-on-surface">标题：《{{ detailTitle }}》</h2>
            <div v-if="detailAuthor" class="flex items-center gap-3 mt-2">
              <span class="text-body-md text-on-surface-variant flex items-center gap-1">
                <span class="material-symbols-outlined text-[16px]">person</span>
                {{ detailAuthor }}
              </span>
              <span class="text-[12px] text-on-surface-variant bg-surface-variant px-2 py-0.5 rounded">积分: {{ detailPoints }}</span>
              <button class="text-[12px] text-primary hover:underline" @click="openPointsDialog(detailUserId, detailAuthor, 'article', detailArticleId)">调整积分</button>
              <button class="text-[12px] text-on-surface-variant hover:text-primary hover:underline" @click="openPointsLogDialog(detailUserId, detailAuthor)">查看记录</button>
            </div>
          </div>
          <div class="markdown-body detail-content" v-html="renderedContent"></div>
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
                <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="downloadFile(file.filePath, file.fileName)">
                  <span class="material-symbols-outlined text-[14px]">download</span>
                  下载
                </button>
              </div>
            </div>
          </div>
          <div class="mt-6">
            <h4 class="font-headline-sm text-headline-sm text-on-surface mb-3 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-[20px]">comment</span>
              评论（{{ detailCommentCount }}）
            </h4>
            <div v-if="detailCommentCount > 0" class="space-y-4">
              <div v-for="(item, index) in detailComments" :key="item.commentId || index" class="bg-surface-container-low rounded-lg p-4 border border-outline-variant/50">
                <div class="flex items-center gap-3 mb-2">
                  <img class="w-9 h-9 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(item.portrait)" alt="">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                      <span class="font-headline-sm text-headline-sm text-on-surface">{{ item.nickname || '未知用户' }}</span>
                      <span class="text-body-md text-on-surface-variant text-[12px]">{{ item.commentTime }}</span>
                    </div>
                    <div class="flex items-center gap-2 mt-0.5">
                      <span class="text-[11px] text-on-surface-variant bg-surface-variant px-1.5 py-0.5 rounded">积分: {{ item.points || 0 }}</span>
                      <button class="text-[11px] text-primary hover:underline" @click="openPointsDialog(item.userId, item.nickname, 'comment', item.commentId)">调整积分</button>
                      <button class="text-[11px] text-on-surface-variant hover:text-primary hover:underline" @click="openPointsLogDialog(item.userId, item.nickname)">查看记录</button>
                    </div>
                  </div>
                  <button class="shrink-0 inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDeleteComment(item.commentId)">
                    <span class="material-symbols-outlined text-[14px]">delete</span>
                    删除
                  </button>
                </div>
                <p class="text-body-md text-on-surface ml-12">{{ item.commentContent }}</p>
                <div v-if="item.reply && item.reply.length" class="ml-12 mt-3 pl-4 border-l-2 border-outline-variant/30 space-y-3">
                  <div v-for="(reply, rIdx) in item.reply" :key="reply.replyId || rIdx" class="bg-surface-container rounded-lg p-3">
                    <div class="flex items-center gap-2 mb-1.5">
                      <img class="w-7 h-7 rounded-full bg-surface-variant object-cover" :src="getAvatarUrl(reply.portrait)" alt="">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2">
                          <span class="font-headline-sm text-headline-sm text-on-surface text-[13px]">{{ reply.nickname || '未知用户' }}</span>
                          <span class="text-body-md text-on-surface-variant text-[12px]">{{ reply.replyTime }}</span>
                        </div>
                        <div class="flex items-center gap-2 mt-0.5">
                          <span class="text-[10px] text-on-surface-variant bg-surface-variant px-1.5 py-0.5 rounded">积分: {{ reply.points || 0 }}</span>
                          <button class="text-[10px] text-primary hover:underline" @click="openPointsDialog(reply.replyUserId, reply.nickname, 'reply', reply.replyId)">调整积分</button>
                          <button class="text-[10px] text-on-surface-variant hover:text-primary hover:underline" @click="openPointsLogDialog(reply.replyUserId, reply.nickname)">查看记录</button>
                        </div>
                      </div>
                      <button class="shrink-0 inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDeleteReply(reply.replyId)">
                        <span class="material-symbols-outlined text-[12px]">delete</span>
                      </button>
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
        <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-6 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="closeDetail">关 闭</button>
        </div>
      </div>
    </div>

    <!-- Points Adjustment Dialog -->
    <div v-if="pointsDialogVisible" class="fixed inset-0 bg-black/30 z-40" @click="pointsDialogVisible = false"></div>
    <div v-if="pointsDialogVisible" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="bg-container w-full max-w-md mx-auto my-[15vh] rounded-xl shadow-2xl">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-rank-gold">paid</span>
            调整积分 - {{ pointsDialogUser.nickname }}
          </h3>
          <button class="text-outline hover:text-error transition-colors" @click="pointsDialogVisible = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div>
            <label class="block text-body-md text-on-surface mb-1.5 font-medium">积分变动</label>
            <div class="flex items-center gap-2">
              <div class="flex rounded-lg border border-outline-variant overflow-hidden">
                <button
                  class="px-4 py-2 text-[13px] font-medium transition-all"
                  :class="pointsDialogForm.pointsChange >= 0 ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'"
                  @click="pointsDialogForm.pointsChange = Math.abs(pointsDialogForm.pointsChange || 0) || 1"
                >加分</button>
                <button
                  class="px-4 py-2 text-[13px] font-medium transition-all"
                  :class="pointsDialogForm.pointsChange < 0 ? 'bg-error text-on-error' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'"
                  @click="pointsDialogForm.pointsChange = -(Math.abs(pointsDialogForm.pointsChange || 0) || 1)"
                >扣分</button>
              </div>
              <input
                v-model.number="pointsDialogForm.pointsChange"
                type="number"
                min="0"
                class="flex-1 h-10 px-4 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
                placeholder="输入分值"
              />
              <span class="text-body-md text-on-surface-variant">积分</span>
            </div>
            <p class="text-[11px] text-on-surface-variant mt-1">{{ pointsDialogForm.pointsChange >= 0 ? '为该用户增加积分' : '从该用户扣除积分' }}</p>
          </div>
          <div>
            <label class="block text-body-md text-on-surface mb-1.5 font-medium">调整原因</label>
            <textarea
              v-model="pointsDialogForm.reason"
              rows="3"
              class="w-full px-4 py-2.5 bg-surface rounded-lg border border-outline-variant text-body-md text-on-surface focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all resize-none"
              placeholder="请输入调整原因（如：灌水扣分、优质内容加分等）"
            ></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="pointsDialogVisible = false">取消</button>
          <button class="px-4 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md" :disabled="pointsDialogSubmitting" @click="submitPointsAdjust">
            {{ pointsDialogSubmitting ? '提交中...' : '确认调整' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Points Log Dialog -->
    <div v-if="pointsLogDialogVisible" class="fixed inset-0 bg-black/30 z-40" @click="pointsLogDialogVisible = false"></div>
    <div v-if="pointsLogDialogVisible" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="bg-container w-full max-w-lg mx-auto my-[10vh] rounded-xl shadow-2xl">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-rank-gold">history</span>
            积分变动记录 - {{ pointsLogDialogUser.nickname }}
          </h3>
          <button class="text-outline hover:text-error transition-colors" @click="pointsLogDialogVisible = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5">
          <div v-if="pointsLogLoading" class="flex items-center justify-center py-12">
            <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
          </div>
          <div v-else-if="pointsLogList.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">inbox</span>
            <p class="text-body-md">暂无积分变动记录</p>
          </div>
          <div v-else class="space-y-3 max-h-[50vh] overflow-y-auto">
            <div v-for="log in pointsLogList" :key="log.id" class="flex items-start gap-3 p-3 rounded-lg border border-outline-variant/30" :class="log.isReversed === 1 ? 'bg-surface opacity-60' : 'bg-surface-container-low'">
              <span class="shrink-0 mt-0.5 material-symbols-outlined text-[20px]" :class="log.pointsChange > 0 ? 'text-primary' : 'text-error'">
                {{ log.isReversed === 1 ? 'undo' : (log.pointsChange > 0 ? 'add_circle' : 'remove_circle') }}
              </span>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-body-md" :class="log.isReversed === 1 ? 'text-on-surface-variant line-through' : (log.pointsChange > 0 ? 'text-primary' : 'text-error')">
                    {{ log.pointsChange > 0 ? '加 ' : '扣 ' }}{{ Math.abs(log.pointsChange) }} 积分
                  </span>
                  <span v-if="log.relatedType" class="text-[11px] text-on-surface-variant bg-surface-variant px-1.5 py-0.5 rounded">
                    {{ relatedTypeLabel(log.relatedType) }}
                  </span>
                  <span v-if="log.isReversed === 1" class="text-[11px] text-on-surface-variant bg-error/10 text-error px-1.5 py-0.5 rounded">已撤销</span>
                </div>
                <p v-if="log.reason" class="text-body-md text-on-surface-variant mt-1">{{ log.reason }}</p>
                <p class="text-[11px] text-outline mt-1">{{ log.createTime }}</p>
              </div>
              <button
                v-if="log.isReversed !== 1 && log.relatedType !== 'undo'"
                class="shrink-0 inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-on-surface-variant bg-surface-variant rounded hover:bg-error/10 hover:text-error transition-colors"
                @click="handleUndoPointsLog(log)"
              >
                <span class="material-symbols-outlined text-[14px]">undo</span>
                撤销
              </button>
            </div>
          </div>
        </div>
        <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-6 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="pointsLogDialogVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it/dist/markdown-it'
import 'mavon-editor/dist/markdown/github-markdown.min.css'

export default {
  name: 'ArticlePage',
  data() {
    return {
      activeTab: 'done',
      loading: false,
      articleList: [],
      labels: [],
      searchForm: {
        keywords: '',
        labelId: '',
        startTime: '',
        endTime: '',
      },
      pagination: {
        total: 0,
        page: 1,
        size: 10,
        pages: 1,
      },
      tabCounts: {
        done: 0,
        pending: 0,
        featured: 0,
      },
      // detail dialog
      detailVisible: false,
      detailLoading: false,
      detailArticleId: null,
      detailTitle: '',
      detailContent: '',
      detailUserId: null,
      detailAuthor: '',
      detailPoints: 0,
      detailComments: [],
      detailFileList: [],
      defaultAvatar: require('../assets/img/img.jpeg'),
      // 积分调整对话框
      pointsDialogVisible: false,
      pointsDialogSubmitting: false,
      pointsDialogUser: { id: null, nickname: '' },
      pointsDialogForm: {
        pointsChange: 1,
        reason: '',
        relatedType: '',
        relatedId: null,
      },
      // 积分记录对话框
      pointsLogDialogVisible: false,
      pointsLogLoading: false,
      pointsLogDialogUser: { id: null, nickname: '' },
      pointsLogList: [],
    }
  },
  computed: {
    detailCommentCount() {
      if (!this.detailComments.length) return 0
      return this.detailComments.reduce((sum, item) => {
        return sum + 1 + (item.reply && item.reply.length ? item.reply.length : 0)
      }, 0)
    },
    renderedContent() {
      if (!this.detailContent) return ''
      return this._renderMarkdown(this.detailContent)
    },
    pageNumbers() {
      const pages = this.pagination.pages
      const current = this.pagination.page
      if (pages <= 7) return pages
      const nums = []
      let start = Math.max(1, current - 2)
      let end = Math.min(pages, current + 2)
      if (start > 2) { nums.push(1, '...') }
      for (let i = start; i <= end; i++) nums.push(i)
      if (end < pages - 1) { nums.push('...', pages) }
      else if (end === pages - 1) { nums.push(pages) }
      return nums
    },
  },
  mounted() {
    this.loadLabels()
    this.fetchList()
  },
  methods: {
    getAvatarUrl(portrait) {
      if (!portrait) return this.defaultAvatar
      // portrait 已含 /bbs-server/files/...，直接使用
      return portrait.startsWith('/') ? portrait : `/${portrait}`
    },
    switchTab(tab) {
      this.activeTab = tab
      this.pagination.page = 1
      this.fetchList()
    },
    handleSearch() {
      this.pagination.page = 1
      this.fetchList()
    },
    handleReset() {
      this.searchForm = { keywords: '', labelId: '', startTime: '', endTime: '' }
      this.pagination.page = 1
      this.fetchList()
    },
    changePage(page) {
      if (page < 1 || page > this.pagination.pages) return
      this.pagination.page = page
      this.fetchList()
    },
    fetchList() {
      this.loading = true
      const params = {
        keywords: this.searchForm.keywords,
        labelId: this.searchForm.labelId,
        startTime: this.searchForm.startTime,
        endTime: this.searchForm.endTime,
        page: this.pagination.page,
        size: this.pagination.size,
      }

      if (this.activeTab === 'featured') {
        // 精华帖管理调专用接口
        this.postRequest('/admin/featured/list', params).then(resp => {
          this.loading = false
          if (resp && resp.obj) {
            this.articleList = Array.isArray(resp.obj.list) ? resp.obj.list : []
            this.pagination.total = resp.obj.total || 0
            this.pagination.pages = resp.obj.pages || 1
          } else {
            this.articleList = []
            this.pagination.total = 0
            this.pagination.pages = 1
          }
        }).catch(err => {
          console.warn('[ArticlePage] fetch featured list', err)
          this.loading = false
          this.articleList = []
        })
      } else {
        // 已审核/未审核调统一接口
        params.enable = this.activeTab === 'done' ? 1 : 0
        this.postRequest('/admin/article/list', params).then(resp => {
          this.loading = false
          if (resp && resp.obj) {
            this.articleList = Array.isArray(resp.obj.list) ? resp.obj.list : []
            this.pagination.total = resp.obj.total || 0
            this.pagination.pages = resp.obj.pages || 1
          } else {
            this.articleList = []
            this.pagination.total = 0
            this.pagination.pages = 1
          }
        }).catch(err => {
          console.warn('[ArticlePage] fetch list', err)
          this.loading = false
          this.articleList = []
        })
      }
    },
    loadLabels() {
      // 获取所有标签
      this.getRequestUrl('/common/getArticleLabel').then(resp => {
        if (resp && Array.isArray(resp)) {
          this.labels = resp
        } else if (resp && resp.obj && Array.isArray(resp.obj)) {
          this.labels = resp.obj
        }
      }).catch(err => {
        console.warn('[ArticlePage] loadLabels', err)
        this.labels = []
      })
    },
    handleToggleFeatured(article) {
      const newVal = article.isFeatured === 1 ? 0 : 1
      const action = newVal === 1 ? '设为精华帖' : '取消精华帖'
      this.$confirm(`确定${action}吗？`, '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/featured/set', { articleId: article.articleId, isFeatured: newVal }).then(resp => {
          if (resp) {
            this.$message.success(resp.message || (newVal === 1 ? '已设为精华帖' : '已取消精华帖'))
            this.fetchList()
          }
        })
      }).catch(() => {})
    },
    getAdminId() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (admin) return JSON.parse(admin).id
      } catch (e) {}
      return 1
    },
    handleAdoptSuggestion(article) {
      if (!article || !article.articleId) return
      this.$confirm(
        `确定采纳建议《${article.articleTitle || ''}》？\n采纳后将为作者增加5分积分。`,
        '采纳确认',
        { confirmButtonText: '确定采纳', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        this.postRequest('/admin/suggestion/adopt', {
          articleId: article.articleId,
          operatorId: this.getAdminId(),
        }).then(resp => {
          if (resp && resp.code == 200) {
            this.$message.success('采纳成功，作者已获得+5积分')
            this.fetchList()
          } else {
            this.$message.error((resp && resp.message) || '采纳失败')
          }
        }).catch(err => {
          console.warn('[ArticlePage] handleAdoptSuggestion', err)
          this.$message.error('采纳失败')
        })
      }).catch(() => {})
    },
    handleRead(articleId) {
      this.detailArticleId = articleId
      this.detailVisible = true
      this.$nextTick(() => {
        this.loadArticleDetail(articleId)
      })
    },
    closeDetail() {
      this.detailVisible = false
      this.detailArticleId = null
      this.detailTitle = ''
      this.detailContent = ''
      this.detailUserId = null
      this.detailAuthor = ''
      this.detailPoints = 0
      this.detailComments = []
      this.detailFileList = []
      this.pointsDialogVisible = false
      this.pointsLogDialogVisible = false
    },
    loadArticleDetail(articleId) {
      this.detailLoading = true
      this.getRequest('/admin/getArticleByArticleId', articleId).then(resp => {
        this.detailLoading = false
        if (resp) {
          this.detailContent = resp.obj.articleContent
          this.detailTitle = resp.obj.articleTitle
          this.detailUserId = resp.obj.userId
          this.detailAuthor = resp.obj.articleAuthor || ''
          this.getArticleFileByArticleId(articleId)
          this.getCommentByArticleId(articleId)
          // 获取帖子作者的积分
          if (resp.obj.userId) {
            this.fetchArticleAuthorPoints(resp.obj.userId)
          }
        }
      }).catch(err => { console.warn('[ArticlePage] getArticleById', err); this.detailLoading = false })
    },
    fetchArticleAuthorPoints(userId) {
      this.getRequest('/admin/points/total', userId).then(resp => {
        if (resp && resp.obj !== undefined && resp.obj !== null) {
          this.detailPoints = resp.obj || 0
        }
      }).catch(() => { this.detailPoints = 0 })
    },
    getArticleFileByArticleId(articleId) {
      this.postRequest(`/common/getArticleFileByArticleId/${articleId}`, {}).then(res => {
        let list = []
        if (Array.isArray(res)) list = res
        else if (res && Array.isArray(res.obj)) list = res.obj
        else if (res && Array.isArray(res.listBean)) list = res.listBean
        this.detailFileList = list
      }).catch(err => { console.warn('[ArticlePage] getArticleFileByArticleId', err); this.detailFileList = [] })
    },
    getCommentByArticleId(articleId) {
      this.postRequest(`/common/comment/getCommentReply/${articleId}`).then(res => {
        this.detailComments = (res && Array.isArray(res)) ? res : []
      }).catch(err => { console.warn('[ArticlePage] getCommentByArticleId', err); this.detailComments = [] })
    },
    downloadFile(filePath, fileName) {
      if (!filePath) return
      const base = process.env.VUE_APP_BBS_BASE_API || ''
      const url = base.endsWith('/') ? base + filePath.replace(/^\//, '') : (filePath.startsWith('/') ? base + filePath : base + '/' + filePath)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName || ''
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
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
    handleDel(articleId) {
      this.$confirm('删除文章会连评论一并删除，确定要删除该文章吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/deleteArticleByArticleId', { articleId }).then(resp => {
          if (resp) {
            this.$message.success('删除成功！')
            this.fetchList()
          }
        })
      }).catch(() => {})
    },
    handleAudit(articleId) {
      this.$confirm('确定此篇文章通过审核吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/auditArticleByArticleId', { articleId }).then(resp => {
          if (resp) {
            this.$message.success('审核通过！')
            this.fetchList()
          }
        })
      }).catch(() => {})
    },
    handleBatchDeleteArticlesByAlive() {
      this.$confirm('确定要删除所有已审核文章吗？', '提示', { type: 'error' }).then(() => {
        this.postRequest('/admin/handleBatchDeleteArticlesByAlive/all', {}).then(resp => {
          if (resp) { this.fetchList() }
        })
      }).catch(() => {})
    },
    handleBatchAudit() {
      this.$confirm('确定全部通过审核吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/batchAudit/', {}).then(resp => {
          if (resp) {
            this.$message.success('全部审核通过！')
            this.fetchList()
          }
        })
      }).catch(() => {})
    },
    handleDeleteComment(commentId) {
      this.$confirm('确定要删除该评论吗？删除后将无法恢复。', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/comment/deleteCommentById', { commentId }).then(resp => {
          if (resp) {
            this.$message.success('评论删除成功！')
            this.getCommentByArticleId(this.detailArticleId)
          }
        })
      }).catch(() => {})
    },
    handleDeleteReply(replyId) {
      this.$confirm('确定要删除该回复吗？删除后将无法恢复。', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/reply/deleteReplyById', { replyId }).then(resp => {
          if (resp) {
            this.$message.success('回复删除成功！')
            this.getCommentByArticleId(this.detailArticleId)
          }
        })
      }).catch(() => {})
    },
    openPointsDialog(userId, nickname, relatedType, relatedId) {
      this.pointsDialogUser = { id: userId, nickname: nickname || '未知用户' }
      this.pointsDialogForm = {
        pointsChange: 1,
        reason: '',
        relatedType: relatedType,
        relatedId: relatedId,
      }
      this.pointsDialogVisible = true
    },
    submitPointsAdjust() {
      if (!this.pointsDialogForm.pointsChange || this.pointsDialogForm.pointsChange === 0) {
        this.$message.warning('请输入有效的积分分值')
        return
      }
      const change = this.pointsDialogForm.pointsChange
      const absVal = Math.abs(change)
      const params = {
        userId: this.pointsDialogUser.id,
        pointsChange: change,
        reason: this.pointsDialogForm.reason || (change > 0 ? '管理员加分' : '管理员扣分'),
        relatedType: this.pointsDialogForm.relatedType || 'manual',
        relatedId: this.pointsDialogForm.relatedId,
      }
      this.pointsDialogSubmitting = true
      this.postRequest('/admin/points/adjust', params).then(resp => {
        this.pointsDialogSubmitting = false
        if (resp) {
          this.$message.success('积分调整成功！')
          this.pointsDialogVisible = false
          // 刷新评论列表以更新积分显示
          if (this.detailArticleId) {
            this.getCommentByArticleId(this.detailArticleId)
          }
          // 如果调整的是帖子作者的积分，刷新帖子区域的积分显示
          if (this.pointsDialogUser.id === this.detailUserId) {
            this.fetchArticleAuthorPoints(this.detailUserId)
          }
        }
      }).catch(() => {
        this.pointsDialogSubmitting = false
      })
    },
    openPointsLogDialog(userId, nickname) {
      this.pointsLogDialogUser = { id: userId, nickname: nickname || '未知用户' }
      this.pointsLogList = []
      this.pointsLogDialogVisible = true
      this.fetchPointsLog(userId)
    },
    fetchPointsLog(userId) {
      this.pointsLogLoading = true
      this.getRequest('/admin/points/log', userId).then(resp => {
        this.pointsLogLoading = false
        if (resp && resp.obj && Array.isArray(resp.obj)) {
          this.pointsLogList = resp.obj
        } else if (Array.isArray(resp)) {
          this.pointsLogList = resp
        } else {
          this.pointsLogList = []
        }
      }).catch(err => {
        console.warn('[ArticlePage] fetchPointsLog', err)
        this.pointsLogLoading = false
        this.pointsLogList = []
      })
    },
    relatedTypeLabel(type) {
      const map = {
        article: '帖子',
        comment: '评论',
        reply: '回复',
        manual: '手动',
        undo: '撤销',
      }
      return map[type] || type
    },
    handleUndoPointsLog(log) {
      this.$confirm(`确定要撤销这条积分调整吗？\n\n${log.pointsChange > 0 ? '加 ' : '扣 '}${Math.abs(log.pointsChange)} 积分`, '撤销确认', { type: 'warning' }).then(() => {
        this.postRequest(`/admin/points/undo/${log.id}`, {}).then(resp => {
          if (resp) {
            this.$message.success('撤销成功！')
            // 刷新记录列表
            this.fetchPointsLog(this.pointsLogDialogUser.id)
            // 刷新帖子区域积分（如果调整的是帖子作者）
            if (this.pointsLogDialogUser.id === this.detailUserId) {
              this.fetchArticleAuthorPoints(this.detailUserId)
            }
            // 刷新评论列表积分
            if (this.detailArticleId) {
              this.getCommentByArticleId(this.detailArticleId)
            }
          }
        })
      }).catch(() => {})
    },
  }
}
</script>

<style>
.detail-content {
  padding: 8px 0;
  background: transparent !important;
}
.detail-content h1,
.detail-content h2,
.detail-content h3,
.detail-content h4,
.detail-content h5,
.detail-content h6 {
  margin-top: 16px;
  margin-bottom: 8px;
}
.detail-content p {
  margin-bottom: 8px;
  line-height: 1.7;
}
</style>
