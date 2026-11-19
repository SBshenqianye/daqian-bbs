<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-[1500px] mx-auto px-page-margin-desktop py-5">
      <!-- 头部 -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">draw</span>
            SVG 工作台
          </h1>
          <p class="text-[12px] text-secondary mt-1">绘制勋章 / 等级图标 / 底板；保存后可在成就与等级管理中直接引用</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="px-2 py-1 rounded-md text-[11px]" :class="dirty ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'">
            {{ dirty ? '未保存' : '已保存' }}
          </span>
          <button class="toolbar-btn" @click="undo" :disabled="historyIndex <= 0" title="撤销 Ctrl+Z">
            <span class="material-symbols-outlined text-[18px]">undo</span>
          </button>
          <button class="toolbar-btn" @click="redo" :disabled="historyIndex >= history.length - 1" title="重做 Ctrl+Y">
            <span class="material-symbols-outlined text-[18px]">redo</span>
          </button>
          <button class="px-3.5 py-2 bg-surface-variant rounded-lg text-[12px] hover:bg-outline-variant" @click="resetDoc">新建</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px] font-medium hover:opacity-90" @click="openSave">保存素材</button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-3 items-start">
        <!-- 画布：主区域 -->
        <section class="order-2 lg:order-1 lg:col-span-8">
          <div class="bg-container border border-border rounded-xl p-3">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-3 text-[11px] text-secondary">
                <span>画布 {{ doc.width }}×{{ doc.height }}</span>
                <label class="inline-flex items-center gap-1 cursor-pointer">
                  <input v-model="snap" type="checkbox" class="w-3 h-3 accent-primary">吸附 {{ grid }}px
                </label>
                <label class="inline-flex items-center gap-1 cursor-pointer">
                  <input v-model="showGrid" type="checkbox" class="w-3 h-3 accent-primary">网格
                </label>
              </div>
              <div class="flex items-center gap-1">
                <button class="toolbar-btn" @click="setZoom(-1)">−</button>
                <span class="text-[11px] text-secondary w-10 text-center">{{ Math.round(zoom * 100) }}%</span>
                <button class="toolbar-btn" @click="setZoom(1)">+</button>
                <button class="toolbar-btn" @click="zoom = 1" title="重置缩放"><span class="material-symbols-outlined text-[16px]">restart_alt</span></button>
              </div>
            </div>

            <div class="flex justify-center bg-surface-container-lowest rounded-lg border border-outline-variant p-3 overflow-auto">
              <svg
                ref="canvas"
                :viewBox="`0 0 ${doc.width} ${doc.height}`"
                :style="{ width: (doc.width * zoom) + 'px', height: (doc.height * zoom) + 'px', background: doc.background }"
                class="touch-none select-none rounded"
                @mousedown.prevent="onDown"
                @mousemove="onMove"
                @mouseup="onUp"
                @mouseleave="onUp"
              >
                <defs>
                  <pattern id="studio-grid" :width="grid" :height="grid" patternUnits="userSpaceOnUse">
                    <path :d="`M${grid} 0H0V${grid}`" fill="none" stroke="#e8eef6" stroke-width="0.6" />
                  </pattern>
                </defs>
                <rect v-if="showGrid" width="256" height="256" fill="url(#studio-grid)" />

                <!-- 已完成的图层 -->
                <g v-for="l in layers" :key="l.id">
                  <g v-if="l.visible" :opacity="l.opacity">
                    <line v-if="l.type === 'line'" :x1="l.x" :y1="l.y" :x2="l.x2" :y2="l.y2" :stroke="paint(l.stroke)" :stroke-width="l.strokeWidth" stroke-linecap="round" />
                    <rect v-else-if="l.type === 'rect'" :x="l.x" :y="l.y" :width="l.w" :height="l.h" :rx="l.rx" :fill="paint(l.fill)" :stroke="paint(l.stroke)" :stroke-width="l.strokeWidth" />
                    <circle v-else-if="l.type === 'circle'" :cx="l.cx" :cy="l.cy" :r="l.r" :fill="paint(l.fill)" :stroke="paint(l.stroke)" :stroke-width="l.strokeWidth" />
                    <ellipse v-else-if="l.type === 'ellipse'" :cx="l.cx" :cy="l.cy" :rx="l.rx" :ry="l.ry" :fill="paint(l.fill)" :stroke="paint(l.stroke)" :stroke-width="l.strokeWidth" />
                    <g v-else-if="l.type === 'raw'" v-html="rawMarkup(l)"></g>
                    <path v-else :d="pathOf(l)" :fill="paint(l.fill)" :stroke="paint(l.stroke)" :stroke-width="l.strokeWidth" stroke-linejoin="round" stroke-linecap="round" />
                  </g>
                </g>

                <!-- 绘制中的实时预览（带填充） -->
                <g v-if="draft">
                  <line v-if="draft.type === 'line'" :x1="draft.x" :y1="draft.y" :x2="draft.x2" :y2="draft.y2" :stroke="paint(style.stroke)" :stroke-width="style.strokeWidth" stroke-linecap="round" />
                  <rect v-else-if="draft.type === 'rect' || draft.type === 'roundRect'" :x="boxX" :y="boxY" :width="boxW" :height="boxH" :rx="draft.type === 'roundRect' ? 16 : 0" :fill="previewFill" :stroke="paint(style.stroke)" :stroke-width="style.strokeWidth" stroke-dasharray="5 4" />
                  <circle v-else-if="draft.type === 'circle'" :cx="draft.cx" :cy="draft.cy" :r="draft.r" :fill="previewFill" :stroke="paint(style.stroke)" :stroke-width="style.strokeWidth" stroke-dasharray="5 4" />
                  <ellipse v-else-if="draft.type === 'ellipse'" :cx="draft.cx" :cy="draft.cy" :rx="draft.rx" :ry="draft.ry" :fill="previewFill" :stroke="paint(style.stroke)" :stroke-width="style.strokeWidth" stroke-dasharray="5 4" />
                  <path v-else-if="draft.type === 'polygon' || draft.type === 'star'" :d="draftShapePath" :fill="previewFill" :stroke="paint(style.stroke)" :stroke-width="style.strokeWidth" stroke-dasharray="5 4" />

                  <!-- 折线/曲线：随点成型，实时填充 + 橡皮筋 -->
                  <template v-else-if="draft.type === 'polyline' || draft.type === 'curve'">
                    <path
                      v-if="draft.points.length > 1"
                      :d="draftPreviewPath"
                      :fill="previewFill"
                      :stroke="paint(style.stroke)"
                      :stroke-width="style.strokeWidth"
                      stroke-dasharray="5 4"
                      stroke-linejoin="round"
                    />
                    <line
                      v-if="draft.points.length"
                      :x1="lastPoint.x"
                      :y1="lastPoint.y"
                      :x2="pointer.x"
                      :y2="pointer.y"
                      stroke="#2563eb"
                      stroke-width="1"
                      stroke-dasharray="3 3"
                    />
                    <circle v-for="(p, pi) in draft.points" :key="'dp-' + pi" :cx="p.x" :cy="p.y" r="3" fill="#fff" stroke="#2563eb" stroke-width="1.4" />
                  </template>
                </g>

                <!-- 选中图层的锚点 -->
                <g v-if="activeLayer && (activeLayer.type === 'polyline' || activeLayer.type === 'curve')">
                  <circle
                    v-for="(p, pi) in activeLayer.points"
                    :key="'ap-' + pi"
                    :cx="p.x"
                    :cy="p.y"
                    r="3.2"
                    fill="#fff"
                    stroke="#2563eb"
                    stroke-width="1.5"
                    class="cursor-move"
                    @mousedown.prevent.stop="startAnchorDrag(pi)"
                  />
                </g>
              </svg>
            </div>

            <div class="flex items-center justify-between mt-2 gap-3">
              <div class="flex items-center gap-2 min-w-0">
                <button v-if="draft && (draft.type === 'polyline' || draft.type === 'curve')" class="preset" @click="finishDraft(true)">闭合</button>
                <button v-if="draft && (draft.type === 'polyline' || draft.type === 'curve')" class="preset" @click="finishDraft(false)">结束（不闭合）</button>
                <button v-if="draft" class="preset" @click="cancelDraft">取消</button>
                <span class="text-[11px] text-outline truncate">{{ statusText }}</span>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <div class="flex items-end gap-1 rounded-md bg-surface-container-lowest border border-outline-variant px-2 py-1">
                  <span class="bbs-level-svg h-8 inline-flex" v-html="svgText"></span>
                  <span class="text-[9px] text-outline mb-1">64px</span>
                </div>
                <div class="flex items-end gap-1 rounded-md bg-surface-container-lowest border border-outline-variant px-2 py-1">
                  <span class="bbs-level-svg h-5 inline-flex" v-html="svgText"></span>
                  <span class="text-[9px] text-outline mb-0.5">20px</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 右：工具面板（标签页切换，一次只展开一组） -->
        <section class="order-1 lg:order-2 lg:col-span-4">
          <div class="bg-container border border-border rounded-xl overflow-hidden">
            <div class="flex flex-wrap gap-1 p-2 border-b border-outline-variant bg-surface-container-lowest">
              <button
                v-for="t in studioTabs"
                :key="t.key"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-[11px] transition-colors"
                :class="studioTab === t.key ? 'bg-primary text-white shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-low'"
                @click="switchStudioTab(t.key)"
              >
                <span class="material-symbols-outlined text-[15px]">{{ t.icon }}</span>
                {{ t.label }}
                <span
                  v-if="t.badge"
                  class="px-1 rounded-full text-[9px] leading-4"
                  :class="studioTab === t.key ? 'bg-white/20' : 'bg-surface-variant'"
                >{{ t.badge }}</span>
              </button>
            </div>

            <div class="p-3">
              <!-- 绘制工具 -->
              <div v-show="studioTab === 'tools'">
                <div class="grid grid-cols-2 gap-1.5">
                  <button
                    v-for="t in tools"
                    :key="t.key"
                    class="tool"
                    :class="{ 'is-on': tool === t.key }"
                    @click="selectTool(t.key)"
                  >
                    <span class="material-symbols-outlined text-[16px]">{{ t.icon }}</span>{{ t.label }}
                  </button>
                </div>
                <p class="text-[10px] text-outline mt-2 leading-relaxed">{{ toolHint }}</p>
              </div>

              <!-- 快速底板 -->
              <div v-show="studioTab === 'backplate'">
                <div class="grid grid-cols-3 gap-1.5">
                  <button v-for="b in backplates" :key="b.key" class="preset" :title="'添加' + b.label" @click="addBackplate(b.key)">
                    {{ b.label }}
                  </button>
                </div>
                <div class="grid grid-cols-2 gap-2 mt-2">
                  <div>
                    <p class="text-[10px] text-outline mb-0.5">填充</p>
                    <input v-model="backplate.color" type="color" class="w-full h-7 rounded border border-outline-variant bg-surface">
                  </div>
                  <div>
                    <p class="text-[10px] text-outline mb-0.5">描边</p>
                    <input v-model="backplate.stroke" type="color" class="w-full h-7 rounded border border-outline-variant bg-surface">
                  </div>
                </div>
                <div class="mt-2">
                  <div class="flex items-center justify-between text-[10px] text-outline">
                    <span>底板尺寸</span><span>{{ Math.round(backplate.scale * 100) }}%</span>
                  </div>
                  <input v-model.number="backplate.scale" type="range" min="0.5" max="0.98" step="0.02" class="w-full accent-primary">
                </div>
                <p class="text-[10px] text-outline mt-1">底板会自动居中铺好，你只需在中间画图案。</p>
              </div>

              <!-- 图层 -->
              <div v-show="studioTab === 'layers'">
                <div class="flex items-center justify-between mb-2">
                  <p class="text-[11px] text-secondary">图层 ({{ layers.length }})</p>
                  <button class="text-[11px] text-error hover:opacity-80" @click="clearAll">清空</button>
                </div>
                <div class="space-y-1 max-h-[420px] overflow-y-auto custom-scrollbar">
                  <div
                    v-for="l in layersOrdered"
                    :key="l.id"
                    class="layer-row"
                    :class="{ 'is-on': l.id === activeId }"
                    @click="selectLayer(l.id)"
                  >
                    <button class="w-5 text-outline hover:text-primary" :title="l.visible ? '隐藏' : '显示'" @click.stop="toggleVisible(l)">
                      <span class="material-symbols-outlined text-[15px]">{{ l.visible ? 'visibility' : 'visibility_off' }}</span>
                    </button>
                    <span class="flex-1 truncate text-[11px]">{{ l.name }}</span>
                    <button class="w-5 text-outline hover:text-primary" title="上移" @click.stop="moveLayer(l.id, 1)">
                      <span class="material-symbols-outlined text-[15px]">arrow_upward</span>
                    </button>
                    <button class="w-5 text-outline hover:text-primary" title="下移" @click.stop="moveLayer(l.id, -1)">
                      <span class="material-symbols-outlined text-[15px]">arrow_downward</span>
                    </button>
                    <button class="w-5 text-outline hover:text-error" title="删除" @click.stop="removeLayer(l.id)">
                      <span class="material-symbols-outlined text-[15px]">delete</span>
                    </button>
                  </div>
                  <p v-if="!layers.length" class="text-[11px] text-outline py-6 text-center">还没有图形，去「工具」页绘制</p>
                </div>
              </div>

              <!-- 样式 -->
              <div v-show="studioTab === 'style'" class="space-y-2">
                <div class="flex items-center gap-2">
                  <label class="w-12 text-[11px] text-on-surface-variant">填充</label>
                  <input v-model="style.fill" type="color" class="w-8 h-7 rounded border border-outline-variant bg-surface">
                  <button class="preset !py-1" @click="style.fill = 'none'">无</button>
                  <span class="text-[10px] text-outline truncate">{{ style.fill }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <label class="w-12 text-[11px] text-on-surface-variant">描边</label>
                  <input v-model="style.stroke" type="color" class="w-8 h-7 rounded border border-outline-variant bg-surface">
                  <button class="preset !py-1" @click="style.stroke = 'none'">无</button>
                  <span class="text-[10px] text-outline truncate">{{ style.stroke }}</span>
                </div>
                <div>
                  <div class="flex items-center justify-between text-[11px] text-on-surface-variant">
                    <span>线宽</span><span>{{ style.strokeWidth }}</span>
                  </div>
                  <input v-model.number="style.strokeWidth" type="range" min="0" max="16" step="0.5" class="w-full accent-primary">
                </div>
                <div>
                  <div class="flex items-center justify-between text-[11px] text-on-surface-variant">
                    <span>不透明度</span><span>{{ Math.round(style.opacity * 100) }}%</span>
                  </div>
                  <input v-model.number="style.opacity" type="range" min="0.1" max="1" step="0.05" class="w-full accent-primary">
                </div>
                <button class="w-full py-1.5 text-[11px] border border-outline rounded-lg hover:border-primary disabled:opacity-40" :disabled="!activeLayer" @click="applyStyle">
                  应用到选中图层
                </button>
              </div>

              <!-- 图形模板 -->
              <div v-show="studioTab === 'templates'">
                <div class="grid grid-cols-2 gap-1.5">
                  <button v-for="t in templates" :key="t.key" class="preset" @click="applyTemplate(t.key)">{{ t.label }}</button>
                </div>
                <p class="text-[10px] text-outline mt-2 leading-relaxed">模板会作为一个新图层加入画布，可继续用工具和样式调整。</p>
              </div>

            </div>
          </div>
        </section>
      </div>

      <!-- 素材库：独立整行，不再塞进右侧标签 -->
      <section class="bg-container border border-border rounded-xl mt-3 overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-2 px-4 py-3 border-b border-outline-variant bg-surface-container-lowest">
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px] text-primary">collections</span>
            <h2 class="text-[13.5px] font-semibold text-on-surface">素材库 ({{ assets.length }})</h2>
            <span class="text-[10.5px] text-outline">点击缩略图可载入画布</span>
          </div>
          <div class="flex flex-wrap items-center gap-1">
            <button
              v-for="c in assetFilters"
              :key="c.key"
              class="px-2.5 py-1 rounded-md text-[10.5px] border transition-colors"
              :class="assetFilter === c.key ? 'bg-primary text-white border-primary' : 'bg-surface text-on-surface-variant border-outline-variant hover:border-primary'"
              @click="assetFilter = c.key"
            >{{ c.label }}</button>
            <button class="ml-1 px-2.5 py-1 rounded-md text-[10.5px] text-primary border border-outline-variant hover:border-primary" @click="loadAssets">刷新</button>
          </div>
        </div>
        <div class="p-3">
          <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6 gap-2">
            <div
              v-for="a in filteredAssets"
              :key="a.id"
              class="group rounded-lg border border-outline-variant bg-surface-container-lowest p-2 hover:border-primary transition-colors"
            >
              <div
                class="h-24 rounded-md bg-surface grid place-items-center overflow-hidden cursor-pointer"
                :title="'载入到画布：' + a.assetName"
                @click="loadAssetToCanvas(a)"
              >
                <span v-if="a.category === 'chart'" class="asset-chart-preview" v-html="a.svg"></span>
                <span v-else class="bbs-level-svg h-14 w-14 inline-flex" v-html="a.svg"></span>
              </div>
              <p class="mt-1.5 text-[11px] text-on-surface truncate" :title="a.assetName">{{ a.assetName }}</p>
              <div class="flex items-center justify-between mt-1">
                <span class="text-[9px] text-outline">{{ categoryLabel(a.category) }}</span>
                <div class="flex items-center gap-0.5">
                  <button class="asset-act" title="载入到画布" @click="loadAssetToCanvas(a)">
                    <span class="material-symbols-outlined text-[15px]">add_photo_alternate</span>
                  </button>
                  <button class="asset-act" title="重命名" @click="renameAsset(a)">
                    <span class="material-symbols-outlined text-[15px]">edit</span>
                  </button>
                  <button class="asset-act is-danger" title="删除" @click="deleteAsset(a)">
                    <span class="material-symbols-outlined text-[15px]">delete</span>
                  </button>
                </div>
              </div>
            </div>
            <p v-if="!filteredAssets.length" class="col-span-full text-[11px] text-outline text-center py-8">暂无素材</p>
          </div>
        </div>
      </section>
    </div>

    <!-- 保存 -->
    <div v-if="saveDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="saveDialog = false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between px-5 py-3 border-b border-outline-variant">
          <h3 class="text-[14px] font-semibold">保存素材</h3>
          <button class="text-outline hover:text-error" @click="saveDialog = false"><span class="material-symbols-outlined">close</span></button>
        </div>
        <div class="p-5 space-y-3">
          <div>
            <label class="text-[12px] text-secondary">素材名称 *</label>
            <input v-model="saveForm.assetName" class="field" maxlength="100" placeholder="如：燕尾旗·金">
          </div>
          <div>
            <label class="text-[12px] text-secondary">分类</label>
            <select v-model="saveForm.category" class="field">
              <option value="medal">勋章</option>
              <option value="level">等级图标</option>
              <option value="chart">等级图表</option>
              <option value="other">其他</option>
            </select>
          </div>
          <div class="rounded-lg bg-surface p-2 text-[11px] text-outline">
            当前画布：{{ layers.length }} 个图形 · {{ Math.round(zoom * 100) }}% 缩放
          </div>
        </div>
        <div class="flex justify-end gap-2 px-5 py-3 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="saveDialog = false">取消</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px]" :disabled="saving" @click="doSave">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { nextOverlayZ } from '@/utils/overlay'

const TOOLS = [
  { key: 'select', label: '选择', icon: 'near_me' },
  { key: 'line', label: '直线', icon: 'show_chart' },
  { key: 'rect', label: '矩形', icon: 'crop_square' },
  { key: 'roundRect', label: '圆角矩形', icon: 'rounded_corner' },
  { key: 'circle', label: '圆形', icon: 'circle' },
  { key: 'ellipse', label: '椭圆', icon: 'egg' },
  { key: 'polygon', label: '正六边形', icon: 'hexagon' },
  { key: 'star', label: '五角星', icon: 'star' },
  { key: 'polyline', label: '折线', icon: 'polyline' },
  { key: 'curve', label: '曲线', icon: 'gesture' },
]

const TEMPLATES = [
  { key: 'flag', label: '燕尾旗' },
  { key: 'shield', label: '盾形' },
  { key: 'circle', label: '圆徽' },
  { key: 'star', label: '五角星' },
  { key: 'hexagon', label: '六边形' },
  { key: 'rect', label: '圆角方牌' },
]

const BACKPLATES = [
  { key: 'flag', label: '燕尾旗' },
  { key: 'shield', label: '盾形' },
  { key: 'circle', label: '圆徽' },
  { key: 'star', label: '星星' },
  { key: 'hexagon', label: '六边形' },
  { key: 'rect', label: '方牌' },
  { key: 'moon', label: '月亮' },
  { key: 'sun', label: '太阳' },
  { key: 'crown', label: '皇冠' },
]

const GLYPH_STAR = 'M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z'
const GLYPH_MOON = 'M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z'
const GLYPH_CROWN = 'M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z'
const GLYPH_SUN_RAYS = [
  [12, 1.7, 12, 4.7], [12, 19.3, 12, 22.3], [1.7, 12, 4.7, 12], [19.3, 12, 22.3, 12],
  [4.6, 4.6, 6.8, 6.8], [17.2, 17.2, 19.4, 19.4], [19.4, 4.6, 17.2, 6.8], [6.8, 17.2, 4.6, 19.4],
]

const ASSET_FILTERS = [
  { key: 'all', label: '全部' },
  { key: 'medal', label: '勋章' },
  { key: 'level', label: '等级' },
  { key: 'chart', label: '图表' },
  { key: 'other', label: '其他' },
]

let seq = 0
const nextId = () => 'l' + (++seq)

function polygonPoints(cx, cy, r, sides, rotate) {
  const pts = []
  for (let i = 0; i < sides; i++) {
    const a = (Math.PI * 2 * i) / sides - Math.PI / 2 + (rotate || 0)
    pts.push({ x: Math.round(cx + r * Math.cos(a)), y: Math.round(cy + r * Math.sin(a)) })
  }
  return pts
}

function starPoints(cx, cy, r, points) {
  const pts = []
  for (let i = 0; i < points * 2; i++) {
    const rr = i % 2 === 0 ? r : r * 0.42
    const a = -Math.PI / 2 + (Math.PI * i) / points
    pts.push({ x: Math.round(cx + rr * Math.cos(a)), y: Math.round(cy + rr * Math.sin(a)) })
  }
  return pts
}

function roundRectPoints(x, y, w, h, r) {
  const pts = []
  const q = Math.min(r, w / 2, h / 2)
  const push = (px, py) => pts.push({ x: Math.round(px), y: Math.round(py) })
  for (let i = 0; i <= 6; i++) {
    const a = Math.PI + (Math.PI / 2) * (i / 6)
    push(x + q + q * Math.cos(a), y + q + q * Math.sin(a))
  }
  for (let i = 0; i <= 6; i++) {
    const a = -Math.PI / 2 + (Math.PI / 2) * (i / 6)
    push(x + w - q + q * Math.cos(a), y + q + q * Math.sin(a))
  }
  for (let i = 0; i <= 6; i++) {
    const a = (Math.PI / 2) * (i / 6)
    push(x + w - q + q * Math.cos(a), y + h - q + q * Math.sin(a))
  }
  for (let i = 0; i <= 6; i++) {
    const a = Math.PI / 2 + (Math.PI / 2) * (i / 6)
    push(x + q + q * Math.cos(a), y + h - q + q * Math.sin(a))
  }
  return pts
}

export default {
  name: 'SvgStudioPage',
  data() {
    return {
      doc: { width: 256, height: 256, background: 'transparent' },
      grid: 8,
      snap: true,
      showGrid: true,
      zoom: 1.2,
      tools: TOOLS,
      templates: TEMPLATES,
      backplates: BACKPLATES,
      assetFilters: ASSET_FILTERS,
      tool: 'select',
      layers: [],
      activeId: null,
      draft: null,
      pointer: { x: 0, y: 0 },
      drag: { mode: '', layerId: null, anchor: -1, startX: 0, startY: 0, origin: null },
      style: { fill: '#1f6f5c', stroke: '#ffffff', strokeWidth: 6, opacity: 1 },
      backplate: { color: '#1f6f5c', stroke: '#ffffff', scale: 0.86 },
      assets: [],
      assetFilter: 'all',
      studioTab: 'tools',
      saveDialog: false,
      saving: false,
      saveForm: { assetName: '', category: 'medal' },
      overlayZ: 3000,
      dirty: false,
      history: [],
      historyIndex: -1,
    }
  },
  computed: {
    studioTabs() {
      return [
        { key: 'tools', label: '工具', icon: 'build' },
        { key: 'backplate', label: '底板', icon: 'layers' },
        { key: 'layers', label: '图层', icon: 'account_tree', badge: this.layers.length },
        { key: 'style', label: '样式', icon: 'palette' },
        { key: 'templates', label: '模板', icon: 'category' },
      ]
    },
    layersOrdered() {
      return this.layers.slice().reverse()
    },
    activeLayer() {
      return this.layers.find(l => l.id === this.activeId) || null
    },
    previewFill() {
      if (this.style.fill === 'none') return 'rgba(37,99,235,0.06)'
      return this.hexToRgba(this.style.fill, 0.35)
    },
    boxX() { return this.draft ? Math.min(this.draft.x || 0, this.draft.x2 || 0) : 0 },
    boxY() { return this.draft ? Math.min(this.draft.y || 0, this.draft.y2 || 0) : 0 },
    boxW() { return this.draft ? Math.abs((this.draft.x2 || 0) - (this.draft.x || 0)) : 0 },
    boxH() { return this.draft ? Math.abs((this.draft.y2 || 0) - (this.draft.y || 0)) : 0 },
    lastPoint() {
      if (!this.draft || !this.draft.points || !this.draft.points.length) return { x: 0, y: 0 }
      return this.draft.points[this.draft.points.length - 1]
    },
    draftPreviewPath() {
      if (!this.draft || !this.draft.points || this.draft.points.length < 2) return ''
      return this.draft.type === 'curve'
        ? this.smoothPath(this.draft.points, false, this.pointer)
        : this.polylineOf(this.draft.points, false, this.pointer)
    },
    draftShapePath() {
      if (!this.draft) return ''
      const pts = this.draft.previewPoints || []
      return this.polylineOf(pts, true)
    },
    toolHint() {
      const map = {
        select: '点击选中图层，拖动移动整体；折线/曲线图层可拖动白色锚点。',
        line: '按住拖动绘制直线。',
        rect: '按住拖动绘制矩形。',
        roundRect: '按住拖动绘制圆角矩形。',
        circle: '按住拖动，按拖拽尺寸生成圆形。',
        ellipse: '按住拖动，按拖拽宽高生成椭圆。',
        polygon: '从中心向外拖动，松开生成正六边形。',
        star: '从中心向外拖动，松开生成五角星。',
        polyline: '逐点点击，实时预览填充；回车或点「闭合」完成。',
        curve: '逐点点击生成平滑曲线，实时预览；回车或点「闭合」完成。',
      }
      return map[this.tool] || ''
    },
    statusText() {
      if (this.draft) {
        const n = this.draft.points ? this.draft.points.length : 0
        return n ? `绘制中：${n} 个节点，回车闭合` : '绘制中…'
      }
      if (this.activeLayer) return `已选中：${this.activeLayer.name}`
      return `${this.layers.length} 个图形 · ${this.toolLabel}`
    },
    toolLabel() {
      const t = TOOLS.find(x => x.key === this.tool)
      return t ? t.label : ''
    },
    filteredAssets() {
      if (this.assetFilter === 'all') return this.assets
      return this.assets.filter(a => (a.category || 'other') === this.assetFilter)
    },
    svgText() {
      const body = this.layers.filter(l => l.visible).map(l => this.markupOf(l)).join('')
      return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${this.doc.width} ${this.doc.height}">${body}</svg>`
    },
  },
  mounted() {
    this.loadAssets()
    window.addEventListener('keydown', this.onKey)
  },
  beforeDestroy() {
    window.removeEventListener('keydown', this.onKey)
  },
  methods: {
    /* ---------- 面板 ---------- */
    switchStudioTab(key) {
      this.studioTab = key
    },

    /* ---------- 基础 ---------- */
    paint(color) {
      return color === 'none' || !color ? 'none' : color
    },
    hexToRgba(hex, alpha) {
      if (!hex || hex[0] !== '#') return `rgba(37,99,235,${alpha})`
      const h = hex.length === 4
        ? '#' + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3]
        : hex
      const r = parseInt(h.slice(1, 3), 16)
      const g = parseInt(h.slice(3, 5), 16)
      const b = parseInt(h.slice(5, 7), 16)
      return `rgba(${r},${g},${b},${alpha})`
    },
    categoryLabel(c) {
      return ({ medal: '勋章', level: '等级图标', chart: '等级图表', other: '其他' })[c] || '其他'
    },
    pt(event) {
      const svg = this.$refs.canvas
      if (!svg) return { x: 0, y: 0 }
      const rect = svg.getBoundingClientRect()
      if (!rect.width || !rect.height) return { x: 0, y: 0 }
      let x = ((event.clientX - rect.left) / rect.width) * this.doc.width
      let y = ((event.clientY - rect.top) / rect.height) * this.doc.height
      x = Math.max(0, Math.min(this.doc.width, x))
      y = Math.max(0, Math.min(this.doc.height, y))
      if (this.snap) {
        x = Math.round(x / this.grid) * this.grid
        y = Math.round(y / this.grid) * this.grid
      }
      return { x: Math.round(x), y: Math.round(y) }
    },
    selectTool(key) {
      const changed = this.tool !== key
      this.tool = key
      this.draft = null
      if (changed) {
        this.$message({ type: 'info', message: `已切换到「${this.toolLabel}」工具`, duration: 1200 })
      }
    },
    /* ---------- 指针 ---------- */
    onDown(event) {
      const p = this.pt(event)
      this.pointer = p
      if (this.tool === 'select') {
        const hit = this.hitTest(p)
        if (hit) {
          this.activeId = hit.id
          this.drag = { mode: 'move', layerId: hit.id, anchor: -1, startX: p.x, startY: p.y, origin: JSON.parse(JSON.stringify(hit)) }
        } else {
          this.activeId = null
        }
        return
      }
      if (this.tool === 'polyline' || this.tool === 'curve') {
        if (!this.draft) this.draft = { type: this.tool, points: [] }
        this.draft.points.push(p)
        return
      }
      if (this.tool === 'polygon' || this.tool === 'star') {
        this.draft = { type: this.tool, cx: p.x, cy: p.y, r: 0, previewPoints: [] }
        return
      }
      // 线/矩形/圆/椭圆：拖拽框
      this.draft = { type: this.tool, x: p.x, y: p.y, x2: p.x, y2: p.y, cx: p.x, cy: p.y, r: 0, rx: 0, ry: 0 }
    },
    onMove(event) {
      const p = this.pt(event)
      this.pointer = p
      if (this.drag.mode === 'move' && this.drag.layerId) {
        const layer = this.layers.find(l => l.id === this.drag.layerId)
        const origin = this.drag.origin
        if (!layer || !origin) return
        const dx = p.x - this.drag.startX
        const dy = p.y - this.drag.startY
        if (origin.points) {
          layer.points = origin.points.map(q => ({ x: q.x + dx, y: q.y + dy }))
        } else if (origin.type === 'raw') {
          layer.x = origin.x + dx
          layer.y = origin.y + dy
        } else {
          layer.x = (origin.x != null ? origin.x : 0) + dx
          layer.y = (origin.y != null ? origin.y : 0) + dy
          if (origin.cx != null) { layer.cx = origin.cx + dx; layer.cy = origin.cy + dy }
          if (origin.x2 != null) { layer.x2 = origin.x2 + dx; layer.y2 = origin.y2 + dy }
        }
        this.dirty = true
        return
      }
      if (this.drag.mode === 'anchor' && this.activeLayer) {
        this.activeLayer.points.splice(this.drag.anchor, 1, p)
        this.dirty = true
        return
      }
      if (!this.draft) return
      const d = this.draft
      if (d.type === 'polygon') {
        d.r = Math.round(Math.hypot(p.x - d.cx, p.y - d.cy))
        d.previewPoints = polygonPoints(d.cx, d.cy, d.r, 6, 0)
      } else if (d.type === 'star') {
        d.r = Math.round(Math.hypot(p.x - d.cx, p.y - d.cy))
        d.previewPoints = starPoints(d.cx, d.cy, d.r, 5)
      } else if (d.type === 'circle') {
        const r = Math.round(Math.max(Math.abs(p.x - d.x), Math.abs(p.y - d.y)))
        d.cx = d.x
        d.cy = d.y
        d.r = r
      } else if (d.type === 'ellipse') {
        d.cx = (d.x + p.x) / 2
        d.cy = (d.y + p.y) / 2
        d.rx = Math.abs(p.x - d.x) / 2
        d.ry = Math.abs(p.y - d.y) / 2
        d.x2 = p.x
        d.y2 = p.y
      } else {
        d.x2 = p.x
        d.y2 = p.y
      }
    },
    onUp() {
      const d = this.draft
      if (d && d.type !== 'polyline' && d.type !== 'curve') {
        if (d.type === 'line') {
          if (Math.hypot(d.x2 - d.x, d.y2 - d.y) > 4) {
            this.pushLayer({ id: nextId(), type: 'line', name: '直线 ' + (this.layers.length + 1), x: d.x, y: d.y, x2: d.x2, y2: d.y2, fill: 'none', stroke: this.style.stroke, strokeWidth: this.style.strokeWidth, opacity: this.style.opacity, visible: true })
          }
        } else if (d.type === 'rect' || d.type === 'roundRect') {
          if (this.boxW > 4 && this.boxH > 4) {
            this.pushLayer({ id: nextId(), type: 'rect', name: (d.type === 'roundRect' ? '圆角矩形 ' : '矩形 ') + (this.layers.length + 1), x: this.boxX, y: this.boxY, w: this.boxW, h: this.boxH, rx: d.type === 'roundRect' ? 16 : 0, fill: this.style.fill, stroke: this.style.stroke, strokeWidth: this.style.strokeWidth, opacity: this.style.opacity, visible: true })
          }
        } else if (d.type === 'circle') {
          if (d.r > 4) {
            this.pushLayer({ id: nextId(), type: 'circle', name: '圆形 ' + (this.layers.length + 1), cx: d.cx, cy: d.cy, r: d.r, x: d.cx - d.r, y: d.cy - d.r, fill: this.style.fill, stroke: this.style.stroke, strokeWidth: this.style.strokeWidth, opacity: this.style.opacity, visible: true })
          }
        } else if (d.type === 'ellipse') {
          if (d.rx > 4 && d.ry > 4) {
            this.pushLayer({ id: nextId(), type: 'ellipse', name: '椭圆 ' + (this.layers.length + 1), cx: d.cx, cy: d.cy, rx: d.rx, ry: d.ry, x: d.cx - d.rx, y: d.cy - d.ry, fill: this.style.fill, stroke: this.style.stroke, strokeWidth: this.style.strokeWidth, opacity: this.style.opacity, visible: true })
          }
        } else if (d.type === 'polygon' || d.type === 'star') {
          if (d.r > 6) {
            this.pushLayer({
              id: nextId(),
              type: 'polyline',
              name: (d.type === 'star' ? '五角星 ' : '六边形 ') + (this.layers.length + 1),
              points: d.previewPoints,
              fill: this.style.fill,
              stroke: this.style.stroke,
              strokeWidth: this.style.strokeWidth,
              opacity: this.style.opacity,
              visible: true,
            })
          }
        }
        this.draft = null
      }
      this.drag = { mode: '', layerId: null, anchor: -1, startX: 0, startY: 0, origin: null }
    },
    cancelDraft() {
      this.draft = null
      this.$message.info('已取消当前绘制')
    },
    finishDraft(closed) {
      const d = this.draft
      if (!d) return
      const min = d.type === 'curve' ? 2 : 3
      if (!closed) {
        if (d.points.length < 2) { this.$message.warning('至少需要 2 个节点'); return }
      } else if (d.points.length < min) {
        this.$message.warning(`至少需要 ${min} 个节点才能闭合`)
        return
      }
      this.pushLayer({
        id: nextId(),
        type: d.type,
        name: (d.type === 'curve' ? '曲线 ' : '折线 ') + (this.layers.length + 1),
        points: d.points.slice(),
        closed,
        fill: closed ? this.style.fill : 'none',
        stroke: this.style.stroke,
        strokeWidth: this.style.strokeWidth,
        opacity: this.style.opacity,
        visible: true,
      })
      this.draft = null
      this.$message.success(closed ? '已闭合当前图形' : '已完成当前图形')
    },
    startAnchorDrag(index) {
      this.drag = { mode: 'anchor', layerId: this.activeId, anchor: index, startX: 0, startY: 0, origin: null }
    },
    /* ---------- 命中 ---------- */
    hitTest(p) {
      for (let i = this.layers.length - 1; i >= 0; i--) {
        const l = this.layers[i]
        if (!l.visible) continue
        if (l.type === 'rect') {
          if (p.x >= l.x && p.x <= l.x + l.w && p.y >= l.y && p.y <= l.y + l.h) return l
        } else if (l.type === 'circle') {
          if (Math.hypot(p.x - l.cx, p.y - l.cy) <= l.r + 2) return l
        } else if (l.type === 'ellipse') {
          const dx = (p.x - l.cx) / Math.max(1, l.rx)
          const dy = (p.y - l.cy) / Math.max(1, l.ry)
          if (dx * dx + dy * dy <= 1.05) return l
        } else if (l.type === 'line') {
          if (this.pointToSegment(p, { x: l.x, y: l.y }, { x: l.x2, y: l.y2 }) < 8) return l
        } else if (l.type === 'raw') {
          if (p.x >= l.x && p.x <= l.x + l.w && p.y >= l.y && p.y <= l.y + l.h) return l
        } else if (l.points) {
          const near = l.points.some(q => Math.abs(q.x - p.x) <= 10 && Math.abs(q.y - p.y) <= 10)
          if (near) return l
          if (l.points.length > 2 && this.pointInPolygon(p, l.points)) return l
        }
      }
      return null
    },
    pointToSegment(p, a, b) {
      const dx = b.x - a.x
      const dy = b.y - a.y
      const len2 = dx * dx + dy * dy
      if (!len2) return Math.hypot(p.x - a.x, p.y - a.y)
      let t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2
      t = Math.max(0, Math.min(1, t))
      return Math.hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    },
    pointInPolygon(p, pts) {
      let inside = false
      for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
        const xi = pts[i].x; const yi = pts[i].y
        const xj = pts[j].x; const yj = pts[j].y
        const intersect = (yi > p.y) !== (yj > p.y) && p.x < ((xj - xi) * (p.y - yi)) / (yj - yi) + xi
        if (intersect) inside = !inside
      }
      return inside
    },
    /* ---------- 图层 ---------- */
    pushLayer(layer) {
      this.layers.push(layer)
      this.activeId = layer.id
      this.commit()
      this.dirty = true
    },
    selectLayer(id) {
      this.activeId = id
      const l = this.layers.find(x => x.id === id)
      if (l && l.type !== 'raw') {
        this.style = {
          fill: l.fill || 'none',
          stroke: l.stroke || 'none',
          strokeWidth: l.strokeWidth == null ? 6 : l.strokeWidth,
          opacity: l.opacity == null ? 1 : l.opacity,
        }
      }
    },
    toggleVisible(l) {
      l.visible = !l.visible
      this.dirty = true
      this.commit()
    },
    removeLayer(id) {
      const l = this.layers.find(x => x.id === id)
      this.layers = this.layers.filter(x => x.id !== id)
      if (this.activeId === id) this.activeId = null
      this.dirty = true
      this.commit()
      if (l) this.$message.success(`已删除「${l.name}」`)
    },
    moveLayer(id, dir) {
      const i = this.layers.findIndex(l => l.id === id)
      const j = i + dir
      if (i < 0 || j < 0 || j >= this.layers.length) return
      const arr = this.layers.slice()
      const tmp = arr[i]
      arr[i] = arr[j]
      arr[j] = tmp
      this.layers = arr
      this.dirty = true
      this.commit()
    },
    clearAll() {
      if (!this.layers.length) return
      this.$confirm('清空画布上的全部图形？', '清空画布', { type: 'warning' }).then(() => {
        this.layers = []
        this.activeId = null
        this.draft = null
        this.dirty = true
        this.commit()
        this.$message.success('画布已清空')
      }).catch(() => {})
    },
    applyStyle() {
      const l = this.activeLayer
      if (!l || l.type === 'raw') return
      l.fill = this.style.fill
      l.stroke = this.style.stroke
      l.strokeWidth = this.style.strokeWidth
      l.opacity = this.style.opacity
      this.dirty = true
      this.commit()
      this.$message.success('样式已应用')
    },
    resetDoc() {
      if (this.layers.length) {
        this.$confirm('新建会清空当前画布（未保存内容将丢失），继续？', '新建', { type: 'warning' }).then(() => {
          this.doReset()
        }).catch(() => {})
        return
      }
      this.doReset()
    },
    doReset() {
      this.layers = []
      this.activeId = null
      this.draft = null
      this.dirty = false
      this.history = []
      this.historyIndex = -1
      this.commit()
      this.$message.success('已新建空白画布')
    },
    /* ---------- 形状 ---------- */
    pathOf(l) {
      return l.type === 'curve' ? this.smoothPath(l.points, l.closed !== false) : this.polylineOf(l.points, l.closed !== false)
    },
    polylineOf(points, closed, extra) {
      if (!points || !points.length) return ''
      const all = extra ? points.concat([extra]) : points
      return 'M' + all.map(p => `${p.x} ${p.y}`).join(' L') + (closed ? ' Z' : '')
    },
    smoothPath(points, closed, extra) {
      if (!points || points.length < 2) return ''
      const all = extra ? points.concat([extra]) : points
      let d = `M${all[0].x} ${all[0].y}`
      for (let i = 0; i < all.length - 1; i++) {
        const p0 = all[i - 1] || all[i]
        const p1 = all[i]
        const p2 = all[i + 1]
        const p3 = all[i + 2] || p2
        const c1x = p1.x + (p2.x - p0.x) / 6
        const c1y = p1.y + (p2.y - p0.y) / 6
        const c2x = p2.x - (p3.x - p1.x) / 6
        const c2y = p2.y - (p3.y - p1.y) / 6
        d += ` C${c1x.toFixed(1)} ${c1y.toFixed(1)} ${c2x.toFixed(1)} ${c2y.toFixed(1)} ${p2.x} ${p2.y}`
      }
      if (closed) d += ' Z'
      return d
    },
    markupOf(l) {
      const fill = this.paint(l.fill)
      const stroke = this.paint(l.stroke)
      if (l.type === 'line') {
        return `<line x1="${l.x}" y1="${l.y}" x2="${l.x2}" y2="${l.y2}" stroke="${stroke}" stroke-width="${l.strokeWidth}" stroke-linecap="round" opacity="${l.opacity}"/>`
      }
      if (l.type === 'rect') {
        return `<rect x="${l.x}" y="${l.y}" width="${l.w}" height="${l.h}" rx="${l.rx || 0}" fill="${fill}" stroke="${stroke}" stroke-width="${l.strokeWidth}" opacity="${l.opacity}"/>`
      }
      if (l.type === 'circle') {
        return `<circle cx="${l.cx}" cy="${l.cy}" r="${l.r}" fill="${fill}" stroke="${stroke}" stroke-width="${l.strokeWidth}" opacity="${l.opacity}"/>`
      }
      if (l.type === 'ellipse') {
        return `<ellipse cx="${l.cx}" cy="${l.cy}" rx="${l.rx}" ry="${l.ry}" fill="${fill}" stroke="${stroke}" stroke-width="${l.strokeWidth}" opacity="${l.opacity}"/>`
      }
      if (l.type === 'raw') {
        return this.rawMarkup(l)
      }
      return `<path d="${this.pathOf(l)}" fill="${fill}" stroke="${stroke}" stroke-width="${l.strokeWidth}" stroke-linejoin="round" stroke-linecap="round" opacity="${l.opacity}"/>`
    },
    /* 素材以嵌套 svg 形式作为图层 */
    rawMarkup(l) {
      if (!l.rawSvg) return ''
      const w = l.w || 120
      const h = l.h || 120
      return l.rawSvg
        .replace(/<svg\b([^>]*)>/i, (m, attrs) => {
          const cleaned = attrs
            .replace(/\s(width|height|x|y)=("[^"]*"|'[^']*')/gi, '')
          return `<svg${cleaned} x="${l.x}" y="${l.y}" width="${w}" height="${h}">`
        })
    },
    /* ---------- 模板 / 底板 ---------- */
    templatePoints(key, scale) {
      const s = scale || 1
      const cx = 128
      const cy = 128
      if (key === 'flag') {
        return [{ x: 48, y: 40 }, { x: 208, y: 40 }, { x: 208, y: 208 }, { x: 128, y: 168 }, { x: 48, y: 208 }]
          .map(p => ({ x: Math.round(cx + (p.x - cx) * s), y: Math.round(cy + (p.y - cy) * s) }))
      }
      if (key === 'shield') {
        return [{ x: 128, y: 24 }, { x: 216, y: 64 }, { x: 216, y: 136 }, { x: 128, y: 232 }, { x: 40, y: 136 }, { x: 40, y: 64 }]
          .map(p => ({ x: Math.round(cx + (p.x - cx) * s), y: Math.round(cy + (p.y - cy) * s) }))
      }
      if (key === 'circle') return polygonPoints(cx, cy, 100 * s, 36, 0)
      if (key === 'hexagon') return polygonPoints(cx, cy, 104 * s, 6, 0)
      if (key === 'star') return starPoints(cx, cy, 104 * s, 5)
      const w = 176 * s
      const h = 120 * s
      return roundRectPoints(cx - w / 2, cy - h / 2, w, h, 20 * s)
    },
    applyTemplate(key) {
      const pts = this.templatePoints(key, 0.92)
      this.pushLayer({
        id: nextId(),
        type: key === 'circle' ? 'curve' : 'polyline',
        name: (TEMPLATES.find(t => t.key === key) || {}).label + ' ' + (this.layers.length + 1),
        points: pts,
        closed: true,
        fill: this.style.fill,
        stroke: this.style.stroke,
        strokeWidth: this.style.strokeWidth,
        opacity: this.style.opacity,
        visible: true,
      })
      this.$message.success('已添加模板图形')
    },
    backplateGlyphSvg(key) {
      const color = this.backplate.color
      const stroke = this.backplate.stroke
      const sw = 0.7
      let body = ''
      if (key === 'star') {
        body = `<path d="${GLYPH_STAR}" fill="${color}" stroke="${stroke}" stroke-width="${sw}" stroke-linejoin="round"/>`
      } else if (key === 'moon') {
        body = `<path d="${GLYPH_MOON}" fill="${color}" stroke="${stroke}" stroke-width="${sw}" stroke-linejoin="round"/>`
      } else if (key === 'sun') {
        const rays = GLYPH_SUN_RAYS
          .map(([x1, y1, x2, y2]) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}"/>`)
          .join('')
        body = `<circle cx="12" cy="12" r="4.7" fill="${color}" stroke="${stroke}" stroke-width="${sw}"/>`
          + `<g stroke="${color}" stroke-width="2.1" stroke-linecap="round">${rays}</g>`
      } else {
        body = `<path d="${GLYPH_CROWN}" fill="${color}" stroke="${stroke}" stroke-width="${sw}" stroke-linejoin="round"/>`
          + `<rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="${color}" stroke="${stroke}" stroke-width="${sw}"/>`
      }
      return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">${body}</svg>`
    },
    addBackplate(key) {
      const meta = BACKPLATES.find(b => b.key === key) || { label: key }
      let layer
      if (['star', 'moon', 'sun', 'crown'].includes(key)) {
        const size = Math.round(256 * (this.backplate.scale || 0.86))
        const x = Math.round((256 - size) / 2)
        const y = Math.round((256 - size) / 2)
        layer = {
          id: nextId(),
          type: 'raw',
          name: '底板 · ' + meta.label,
          rawSvg: this.backplateGlyphSvg(key),
          x,
          y,
          w: size,
          h: size,
          fill: 'none',
          stroke: 'none',
          strokeWidth: 0,
          opacity: 1,
          visible: true,
        }
      } else {
        layer = {
          id: nextId(),
          type: 'polyline',
          name: '底板 · ' + meta.label,
          points: this.templatePoints(key, this.backplate.scale || 0.86),
          closed: true,
          fill: this.backplate.color,
          stroke: this.backplate.stroke,
          strokeWidth: 6,
          opacity: 1,
          visible: true,
        }
      }
      this.layers.unshift(layer)
      this.dirty = true
      this.commit()
      this.$message.success('底板已置于最底层，可直接在中间绘制图案')
    },
    /* ---------- 历史 ---------- */
    snapshot() {
      return JSON.stringify({ layers: this.layers, activeId: this.activeId })
    },
    commit() {
      const snap = this.snapshot()
      if (this.history[this.historyIndex] === snap) return
      this.history = this.history.slice(0, this.historyIndex + 1)
      this.history.push(snap)
      if (this.history.length > 80) this.history.shift()
      this.historyIndex = this.history.length - 1
    },
    restore(snap) {
      if (!snap) return
      const data = JSON.parse(snap)
      this.layers = data.layers
      this.activeId = data.activeId
    },
    undo() {
      if (this.historyIndex <= 0) return
      this.historyIndex -= 1
      this.restore(this.history[this.historyIndex])
      this.dirty = true
    },
    redo() {
      if (this.historyIndex >= this.history.length - 1) return
      this.historyIndex += 1
      this.restore(this.history[this.historyIndex])
      this.dirty = true
    },
    onKey(e) {
      const meta = e.ctrlKey || e.metaKey
      if (meta && e.key.toLowerCase() === 'z') { e.preventDefault(); this.undo() }
      if (meta && e.key.toLowerCase() === 'y') { e.preventDefault(); this.redo() }
      if (meta && e.key.toLowerCase() === 's') { e.preventDefault(); this.openSave() }
      if (e.key === 'Escape') { this.draft = null }
      if (e.key === 'Enter' && this.draft) { e.preventDefault(); this.finishDraft(true) }
      if ((e.key === 'Delete' || e.key === 'Backspace') && this.activeId && this.tool === 'select') {
        e.preventDefault(); this.removeLayer(this.activeId)
      }
    },
    setZoom(dir) {
      const next = Math.round((this.zoom + dir * 0.15) * 100) / 100
      this.zoom = Math.max(0.4, Math.min(3, next))
    },
    /* ---------- 素材库 ---------- */
    async loadAssets() {
      try {
        const res = await this.getRequestUrl('/admin/svg-asset/list')
        this.assets = (res && res.obj) || []
      } catch (e) {
        this.assets = []
      }
    },
    loadAssetToCanvas(asset) {
      this.layers.push({
        id: nextId(),
        type: 'raw',
        name: '素材 · ' + (asset.assetName || '未命名'),
        rawSvg: asset.svg,
        x: 48,
        y: 48,
        w: 160,
        h: 160,
        fill: 'none',
        stroke: 'none',
        strokeWidth: 0,
        opacity: 1,
        visible: true,
      })
      this.activeId = null
      this.dirty = true
      this.commit()
      this.$message.success(`已把「${asset.assetName}」加入画布，可拖动调整位置`)
    },
    renameAsset(asset) {
      this.$prompt('新的素材名称', '重命名', { inputValue: asset.assetName, inputPattern: /\S+/, inputErrorMessage: '名称不能为空' })
        .then(async ({ value }) => {
          const res = await this.postRequest('/admin/svg-asset/save', {
            id: asset.id,
            assetName: value,
            category: asset.category || 'other',
            svg: asset.svg,
          })
          if (res && res.code === 200) {
            this.$message.success('已重命名')
            this.loadAssets()
          } else {
            this.$message.error((res && res.message) || '重命名失败')
          }
        }).catch(() => {})
    },
    deleteAsset(asset) {
      this.$confirm(`确定删除素材「${asset.assetName}」？已被引用的图标不受影响（SVG 已存入对应配置）。`, '删除素材', { type: 'warning' })
        .then(async () => {
          const res = await this.postRequest('/admin/svg-asset/delete', { id: asset.id })
          if (res && res.code === 200) {
            this.$message.success('已删除')
            this.loadAssets()
          } else {
            this.$message.error((res && res.message) || '删除失败')
          }
        }).catch(() => {})
    },
    /* ---------- 保存 ---------- */
    openSave() {
      if (!this.layers.length) {
        this.$message.warning('画布为空，先绘制图形或添加底板')
        return
      }
      this.overlayZ = nextOverlayZ()
      this.saveForm.assetName = this.saveForm.assetName || ''
      this.saveDialog = true
    },
    async doSave() {
      if (!this.saveForm.assetName || !this.saveForm.assetName.trim()) {
        this.$message.warning('请输入素材名称')
        return
      }
      this.saving = true
      try {
        const res = await this.postRequest('/admin/svg-asset/save', {
          id: null,
          assetName: this.saveForm.assetName.trim(),
          category: this.saveForm.category,
          svg: this.svgText,
        })
        if (res && res.code === 200) {
          this.saveDialog = false
          this.dirty = false
          this.$message.success(`已保存「${this.saveForm.assetName.trim()}」，可在图标选择器中引用`)
          this.saveForm.assetName = ''
          this.loadAssets()
        } else {
          this.$message.error((res && res.message) || '保存失败')
        }
      } catch (e) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },
  },
}
</script>

<style scoped>
.toolbar-btn {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  background: #fff;
  color: #475569;
}
.toolbar-btn:hover:not(:disabled) { border-color: #2563eb; color: #1d4ed8; }
.toolbar-btn:disabled { opacity: 0.35; }

.tool {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 11px;
}
.tool.is-on {
  border-color: #2563eb;
  color: #1d4ed8;
  background: #eff6ff;
}

.preset {
  padding: 4px 8px;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 11px;
}
.preset:hover { border-color: #2563eb; color: #1d4ed8; }

.layer-row {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  border-radius: 6px;
  cursor: pointer;
}
.layer-row:hover { background: #f4f6fa; }
.layer-row.is-on { background: #eff6ff; color: #1d4ed8; }

.asset-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 4px;
  border-radius: 6px;
}
.asset-row:hover { background: #f4f6fa; }
.asset-chart-preview {
  display: block;
  width: 100%;
  height: 100%;
  padding: 4px;
}
.asset-chart-preview ::v-deep svg {
  display: block;
  width: 100%;
  height: 100%;
}

.asset-act {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}
.asset-act:hover { background: rgba(148, 163, 184, 0.16); color: #1d4ed8; }
.asset-act.is-danger:hover { color: #dc2626; }

.field {
  width: 100%;
  margin-top: 4px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
}
.field:focus { border-color: #2563eb; }
</style>
