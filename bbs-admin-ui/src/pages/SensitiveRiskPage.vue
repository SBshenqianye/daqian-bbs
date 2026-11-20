<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6 flex-wrap gap-3">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-error">local_fire_department</span>
            敏感词风险
          </h1>
          <p class="text-body-md text-secondary mt-1">
            每个方块是一名用户，颜色越深表示累计命中敏感词越多；只做提醒与画像，不做处置
          </p>
        </div>
        <div class="flex items-center gap-2">
          <el-radio-group v-model="tab" size="small">
            <el-radio-button label="grid">热力方阵</el-radio-button>
            <el-radio-button label="rank">排行榜</el-radio-button>
          </el-radio-group>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="reload">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            刷新
          </button>
        </div>
      </div>

      <!-- 热力方阵 -->
      <div v-show="tab === 'grid'" class="bg-container border border-border rounded-xl p-card-padding" v-loading="gridLoading">
        <div class="flex flex-wrap items-center gap-3 mb-4">
          <div class="grid grid-cols-1 grid-rows-1 min-w-[220px]">
            <input v-model="searchKeyword" class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:outline-none font-body-md text-body-md" placeholder="搜索昵称，高亮对应方块">
            <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">search</span>
          </div>
          <el-radio-group v-model="sortMode" size="small">
            <el-radio-button label="id">默认顺序</el-radio-button>
            <el-radio-button label="hits">按热度</el-radio-button>
          </el-radio-group>
          <span class="text-body-sm text-secondary">共 {{ grid.total }} 名用户 · {{ grid.columns }} 列</span>
          <span v-if="grid.scoped" class="text-body-sm text-warning">（仅统计您负责板块内的命中）</span>

          <!-- 图例 -->
          <div class="flex items-center gap-1 ml-auto text-[11px] text-secondary">
            <span>少</span>
            <i v-for="n in 6" :key="n" class="w-3.5 h-3.5 rounded-sm" :class="'heat-' + (n - 1)"></i>
            <span>多</span>
          </div>
        </div>

        <div v-if="!cells.length" class="py-16 text-center text-on-surface-variant">
          <span class="material-symbols-outlined text-[48px] opacity-20">grid_view</span>
          <p class="text-body-md mt-2">暂无可展示的用户</p>
        </div>
        <div
          v-else
          class="heat-grid"
          :style="{ gridTemplateColumns: `repeat(${grid.columns}, minmax(0, 1fr))` }"
          @mouseover="onCellHover"
          @mouseleave="tip.show = false"
        >
          <div
            v-for="(cell, idx) in cells"
            :key="cell.userId"
            class="heat-cell"
            :class="[heatClass(cell.hits), { 'heat-hit': isMatched(cell) }]"
            :data-idx="idx"
            @click="openUser(cell)"
          ></div>
        </div>

        <!-- 悬浮提示 -->
        <div v-if="tip.show" class="heat-tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
          <div class="font-medium text-on-surface">{{ tip.cell.nickname }}</div>
          <div class="text-[11px] text-secondary">
            累计命中 {{ tip.cell.hits }} 条<template v-if="tip.cell.lastHit"> · 最近 {{ tip.cell.lastHit }}</template>
          </div>
          <div class="text-[10px] text-outline mt-0.5">点击查看详情</div>
        </div>
      </div>

      <!-- 排行榜 -->
      <div v-show="tab === 'rank'" class="bg-container border border-border rounded-xl p-card-padding" v-loading="rankLoading">
        <div class="flex flex-wrap items-center gap-3 mb-4">
          <el-radio-group v-model="rankDays" size="small" @change="loadRank(1)">
            <el-radio-button :label="7">近 7 天</el-radio-button>
            <el-radio-button :label="30">近 30 天</el-radio-button>
            <el-radio-button :label="90">近 90 天</el-radio-button>
          </el-radio-group>
          <span class="text-body-sm text-secondary">
            排序：命中内容数；热力条为近 12 周分布（左旧右新）
          </span>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-body-md">
            <thead>
              <tr class="text-left text-secondary border-b border-outline-variant">
                <th class="py-2 pr-3 w-14">排名</th>
                <th class="py-2 pr-3">用户</th>
                <th class="py-2 pr-3 w-40">近 12 周</th>
                <th class="py-2 pr-3 w-20 text-right">命中</th>
                <th class="py-2 pr-3 w-20 text-right">发布</th>
                <th class="py-2 pr-3 w-20 text-right">命中率</th>
                <th class="py-2 pr-3 w-40">最近命中</th>
                <th class="py-2 w-20 text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in rank.records" :key="row.userId" class="border-b border-outline-variant/40 hover:bg-surface-container-low/50">
                <td class="py-2 pr-3 text-secondary">{{ (rank.page - 1) * rank.size + i + 1 }}</td>
                <td class="py-2 pr-3">
                  <span class="text-on-surface">{{ row.nickname }}</span>
                  <span class="text-[11px] text-outline ml-2">#{{ row.userId }}</span>
                </td>
                <td class="py-2 pr-3">
                  <div class="heat-strip">
                    <i
                      v-for="(v, wi) in row.weekly"
                      :key="wi"
                      :class="stripClass(v, maxWeekly(row))"
                      :title="'第' + (wi + 1) + '周：' + v"
                    ></i>
                  </div>
                </td>
                <td class="py-2 pr-3 text-right font-medium text-error">{{ row.hits }}</td>
                <td class="py-2 pr-3 text-right text-secondary">{{ row.published }}</td>
                <td class="py-2 pr-3 text-right text-secondary">{{ row.rate }}%</td>
                <td class="py-2 pr-3 text-secondary text-[12px]">{{ row.lastHit || '-' }}</td>
                <td class="py-2 text-right">
                  <button class="text-primary text-[12px] hover:underline" @click="openUser(row)">查看</button>
                </td>
              </tr>
              <tr v-if="!rank.records.length">
                <td colspan="8" class="py-12 text-center text-on-surface-variant">暂无命中记录</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="rank.total > 0" class="flex items-center justify-between mt-4 pt-4 border-t border-outline-variant">
          <span class="text-body-sm text-secondary">共 {{ rank.total }} 名用户</span>
          <el-pagination
            background
            layout="prev, pager, next"
            :total="rank.total"
            :page-size="rank.size"
            :current-page="rank.page"
            @current-change="loadRank"
          />
        </div>
      </div>

      <!-- 用户详情 -->
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="detailVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-3xl rounded-xl shadow-2xl overflow-hidden max-h-[85vh] flex flex-col">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-error">person_alert</span>
              {{ detail.user ? detail.user.nickname : '用户详情' }}
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="detailVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="p-5 overflow-y-auto" v-loading="detailLoading">
            <div v-if="detail.user" class="grid grid-cols-4 gap-3 mb-4">
              <div class="bg-surface-container-low rounded-lg p-3">
                <div class="text-[11px] text-secondary">累计命中</div>
                <div class="font-headline-sm text-error">{{ detail.total }}</div>
              </div>
              <div class="bg-surface-container-low rounded-lg p-3">
                <div class="text-[11px] text-secondary">累计发布</div>
                <div class="font-headline-sm text-on-surface">{{ detail.published }}</div>
              </div>
              <div class="bg-surface-container-low rounded-lg p-3">
                <div class="text-[11px] text-secondary">命中率</div>
                <div class="font-headline-sm text-on-surface">{{ hitRate(detail) }}%</div>
              </div>
              <div class="bg-surface-container-low rounded-lg p-3">
                <div class="text-[11px] text-secondary">所属单位</div>
                <div class="text-body-md text-on-surface truncate">{{ detail.user.orgName || '-' }}</div>
              </div>
            </div>

            <div v-for="(hit, i) in detail.records" :key="i" class="border border-outline-variant rounded-lg p-3 mb-2">
              <div class="flex items-center justify-between gap-2">
                <div class="text-body-md text-on-surface truncate">
                  <span class="text-[11px] px-1.5 py-0.5 rounded bg-primary/10 text-primary mr-2">{{ typeLabel(hit.targetType) }}</span>
                  {{ hit.title || ('#' + hit.targetId) }}
                </div>
                <button class="text-primary text-[12px] whitespace-nowrap hover:underline" @click="loadHit(hit)">
                  查看命中词
                </button>
              </div>
              <div class="text-[11px] text-outline mt-1">{{ hit.createTime }} · 命中 {{ hit.hitCount }} 个词</div>

              <div v-if="hitDetail && hitDetail.targetId === hit.targetId && hitDetail.targetType === hit.targetType" class="mt-2 bg-surface-container-low rounded p-2">
                <div class="flex flex-wrap gap-1 mb-1">
                  <span v-for="w in hitDetail.hitWords" :key="w" class="text-[11px] px-1.5 py-0.5 rounded bg-error/10 text-error">{{ w }}</span>
                </div>
                <div v-for="(s, si) in hitDetail.snippets" :key="si" class="text-[12px] text-secondary leading-5">
                  …{{ s.context }}…
                </div>
              </div>
            </div>

            <div v-if="detail.records && !detail.records.length" class="py-10 text-center text-on-surface-variant">
              暂无命中记录
            </div>
          </div>

          <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md" @click="detailVisible = false">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SensitiveRiskPage',
  data() {
    return {
      tab: 'grid',
      sortMode: 'id',
      searchKeyword: '',
      gridLoading: false,
      rankLoading: false,
      detailLoading: false,
      grid: { total: 0, columns: 1, cells: [], scoped: false },
      rank: { records: [], total: 0, page: 1, size: 20 },
      rankDays: 30,
      tip: { show: false, x: 0, y: 0, cell: null },
      detailVisible: false,
      detail: { user: null, records: [], total: 0, published: 0 },
      hitDetail: null
    }
  },
  computed: {
    cells() {
      const list = (this.grid.cells || []).slice()
      if (this.sortMode === 'hits') {
        list.sort((a, b) => b.hits - a.hits || a.userId - b.userId)
      } else {
        list.sort((a, b) => a.userId - b.userId)
      }
      return list
    }
  },
  mounted() {
    this.loadGrid()
    this.loadRank(1)
  },
  methods: {
    async loadGrid() {
      if (typeof this.getRequestUrl !== 'function') return
      this.gridLoading = true
      try {
        const res = await this.getRequestUrl('/admin/sensitive/risk/grid')
        if (res && res.code == 200 && res.obj) {
          this.grid = res.obj
        }
      } catch (e) {
        this.$message.error('加载热力图失败')
      } finally {
        this.gridLoading = false
      }
    },
    async loadRank(page) {
      if (typeof this.getRequestUrl !== 'function') return
      this.rankLoading = true
      try {
        const res = await this.getRequestUrl(
          `/admin/sensitive/risk/rank?page=${page || 1}&size=${this.rank.size}&days=${this.rankDays}`
        )
        if (res && res.code == 200 && res.obj) {
          this.rank = res.obj
        }
      } catch (e) {
        this.$message.error('加载排行榜失败')
      } finally {
        this.rankLoading = false
      }
    },
    reload() {
      this.loadGrid()
      this.loadRank(this.rank.page)
    },
    heatClass(hits) {
      const n = hits || 0
      if (n <= 0) return 'heat-0'
      if (n === 1) return 'heat-1'
      if (n <= 3) return 'heat-2'
      if (n <= 7) return 'heat-3'
      if (n <= 15) return 'heat-4'
      return 'heat-5'
    },
    stripClass(value, max) {
      if (!value) return 'strip-0'
      const ratio = max > 0 ? value / max : 0
      if (ratio <= 0.2) return 'strip-1'
      if (ratio <= 0.4) return 'strip-2'
      if (ratio <= 0.6) return 'strip-3'
      if (ratio <= 0.8) return 'strip-4'
      return 'strip-5'
    },
    maxWeekly(row) {
      return (row.weekly || []).reduce((max, v) => Math.max(max, v), 0)
    },
    isMatched(cell) {
      const keyword = (this.searchKeyword || '').trim()
      return !!keyword && String(cell.nickname || '').indexOf(keyword) !== -1
    },
    onCellHover(event) {
      const idx = event.target && event.target.dataset ? event.target.dataset.idx : null
      if (idx == null) {
        this.tip.show = false
        return
      }
      const cell = this.cells[Number(idx)]
      if (!cell) return
      this.tip = {
        show: true,
        x: Math.min(event.clientX + 12, window.innerWidth - 200),
        y: Math.min(event.clientY + 12, window.innerHeight - 80),
        cell
      }
    },
    openUser(cell) {
      if (!cell || cell.userId == null) return
      this.detailVisible = true
      this.hitDetail = null
      this.loadUserDetail(cell.userId)
    },
    async loadUserDetail(userId) {
      this.detailLoading = true
      try {
        const res = await this.getRequestUrl(`/admin/sensitive/risk/user/${userId}?page=1&size=50`)
        if (res && res.code == 200 && res.obj) {
          this.detail = res.obj
        } else {
          this.$message.error((res && res.message) || '加载用户详情失败')
        }
      } catch (e) {
        this.$message.error('加载用户详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    async loadHit(hit) {
      try {
        const res = await this.getRequestUrl(
          `/admin/sensitive/hit?targetType=${encodeURIComponent(hit.targetType)}&targetId=${hit.targetId}`
        )
        if (res && res.code == 200 && res.obj) {
          this.hitDetail = res.obj
        } else {
          this.$message.error((res && res.message) || '加载命中详情失败')
        }
      } catch (e) {
        this.$message.error('加载命中详情失败')
      }
    },
    typeLabel(type) {
      if (type === 'article') return '帖子'
      if (type === 'comment') return '评论'
      if (type === 'reply') return '回复'
      return type || '内容'
    },
    hitRate(detail) {
      const published = detail.published || 0
      if (!published) return 0
      return Math.round((detail.total || 0) * 1000 / published) / 10
    }
  }
}
</script>

<style scoped>
.heat-grid {
  display: grid;
  gap: 2px;
}
.heat-cell {
  aspect-ratio: 1 / 1;
  border-radius: 2px;
  cursor: pointer;
  transition: transform 0.08s ease, outline-color 0.08s ease;
}
.heat-cell:hover {
  transform: scale(1.35);
  outline: 1px solid rgba(0, 0, 0, 0.25);
  z-index: 2;
}
.heat-hit {
  outline: 2px solid #f59e0b;
  outline-offset: -1px;
}
.heat-0 { background: #eef2f7; }
.heat-1 { background: #dbe7f5; }
.heat-2 { background: #b6d0ec; }
.heat-3 { background: #7fb0dd; }
.heat-4 { background: #4a86c5; }
.heat-5 { background: #1f5c99; }

.heat-tip {
  position: fixed;
  z-index: 60;
  pointer-events: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 140px;
  max-width: 220px;
}

.heat-strip {
  display: flex;
  gap: 2px;
  align-items: center;
  height: 16px;
}
.heat-strip i {
  flex: 1;
  height: 100%;
  border-radius: 2px;
  display: block;
}
.strip-0 { background: #eef2f7; }
.strip-1 { background: #dbe7f5; }
.strip-2 { background: #b6d0ec; }
.strip-3 { background: #7fb0dd; }
.strip-4 { background: #4a86c5; }
.strip-5 { background: #1f5c99; }
</style>
