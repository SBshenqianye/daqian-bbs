<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- 头部 -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">military_tech</span>
            等级管理
          </h1>
          <p class="text-[12px] text-secondary mt-1">
            拖动大阶段＝改该阶段起点，下一阶段不变、区间内小阶段按原比例缩放
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button class="px-3.5 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant text-[12px]" @click="load">刷新</button>
          <div class="flex items-center gap-1">
            <select v-model="autoMode" class="px-2 py-2 text-[12px] bg-surface border border-outline-variant rounded-lg outline-none">
              <option value="linear">等差</option>
              <option value="geometric">等比</option>
              <option value="fibonacci">斐波那契</option>
              <option value="quadratic">平方递增</option>
            </select>
            <input
              v-if="autoMode === 'geometric'"
              v-model.number="autoRatio"
              type="number"
              min="1.01"
              step="0.05"
              class="w-[64px] px-2 py-2 text-[12px] bg-surface border border-outline-variant rounded-lg text-right"
              title="等比倍率，例如 1.15"
            >
            <button class="px-3.5 py-2 border border-outline-variant rounded-lg hover:border-primary text-[12px]" @click="autoRelayout">一键重排</button>
          </div>
          <button class="px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 text-[12px] font-medium disabled:opacity-50" :disabled="saving" @click="save">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="py-24 text-center text-on-surface-variant">加载中...</div>
      <template v-else>
        <!-- 方案栏 -->
        <div class="bg-container border border-border rounded-xl px-4 py-3 mb-4 flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-2">
            <span class="text-[12px] text-secondary">等级方案</span>
            <select
              v-model="activeProfileId"
              class="px-3 py-1.5 text-[12px] bg-surface border border-outline-variant rounded-lg outline-none min-w-[240px]"
              @change="onProfileSelect"
            >
              <option v-for="p in profiles" :key="p.profile.id" :value="p.profile.id">
                {{ p.profile.profileName }}（{{ p.items.length }} 级{{ p.profile.status === 1 ? ' · 启用中' : '' }}）
              </option>
            </select>
          </div>
          <span v-if="currentProfileStatus === 1" class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px]">当前启用</span>
          <span v-else class="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 text-[11px]">未启用（仅预览 / 可编辑）</span>
          <div class="ml-auto flex items-center gap-2">
            <button class="px-3 py-1.5 text-[12px] border border-outline rounded-lg hover:border-primary" @click="cloneProfile">另存为新方案</button>
            <button
              class="px-3 py-1.5 text-[12px] border border-red-200 text-red-600 rounded-lg hover:bg-red-50 disabled:opacity-40"
              :disabled="currentProfileStatus === 1 || profiles.length <= 1"
              @click="removeProfile"
            >删除方案</button>
            <button
              class="px-4 py-1.5 text-[12px] bg-primary text-white rounded-lg disabled:opacity-50"
              :disabled="currentProfileStatus === 1 || activating"
              @click="activateProfile"
            >{{ activating ? '切换中...' : '启用此方案' }}</button>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
          <!-- 曲线 -->
          <section class="lg:col-span-8 bg-container border border-border rounded-xl p-4">
            <div class="flex flex-wrap items-center justify-between gap-2 mb-2">
              <h2 class="text-[14px] font-semibold text-on-surface">积分门槛曲线</h2>
              <div class="flex items-center gap-3 text-[11px] text-on-surface-variant">
                <span class="inline-flex items-center gap-1"><i class="legend-major"></i>大阶段（可拖）</span>
                <span class="inline-flex items-center gap-1"><i class="legend-minor"></i>小阶段（按比例）</span>
                <span class="inline-flex items-center gap-1">
                  倍率
                  <input
                    v-model.number="step"
                    type="number"
                    min="10"
                    step="10"
                    class="w-16 px-1.5 py-0.5 border border-outline-variant rounded bg-surface text-[11px] text-right"
                    @change="onStepChange"
                  >
                  分/格
                </span>
              </div>
            </div>

            <svg
              ref="chart"
              viewBox="0 0 880 340"
              class="w-full select-none touch-none"
              @mousedown.prevent="onBackgroundDown"
            >
              <!-- 横向刻度 + 网格 -->
              <g v-for="t in yTicks" :key="'tick-' + t">
                <line :x1="PAD_L" :y1="yOf(t)" :x2="VIEW_W - PAD_R" :y2="yOf(t)" stroke="#e9eef6" stroke-width="1" />
                <text :x="PAD_L - 8" :y="yOf(t) + 3.5" text-anchor="end" font-size="9.5" fill="#94a3b8">{{ t }}</text>
              </g>

              <!-- 阶段分隔与阶段名 -->
              <g v-for="(m, mi) in majorGroups" :key="'mg-' + mi">
                <line :x1="xOf(m.startIndex)" :y1="PAD_T" :x2="xOf(m.startIndex)" :y2="VIEW_H - PAD_B" stroke="#e2e8f0" stroke-dasharray="3 3" />
                <text :x="stageLabelX(m)" :y="VIEW_H - 12" text-anchor="middle" font-size="9" fill="#94a3b8">{{ m.name }}</text>
              </g>

              <!-- 坐标轴 -->
              <line :x1="PAD_L" :y1="VIEW_H - PAD_B" :x2="VIEW_W - PAD_R" :y2="VIEW_H - PAD_B" stroke="#cbd5e1" />
              <line :x1="PAD_L" :y1="PAD_T" :x2="PAD_L" :y2="VIEW_H - PAD_B" stroke="#cbd5e1" />
              <!-- X 轴刻度 -->
              <g v-for="(item, i) in items" :key="'xt-' + i">
                <line :x1="xOf(i)" :y1="VIEW_H - PAD_B" :x2="xOf(i)" :y2="VIEW_H - PAD_B + 4" stroke="#cbd5e1" />
              </g>

              <!-- 折线 -->
              <polyline :points="polyline" fill="none" stroke="#93b4f8" stroke-width="1.6" />

              <!-- 节点 -->
              <g v-for="(item, i) in items" :key="'node-' + i">
                <!-- 大阶段：菱形，更大 -->
                <template v-if="isMajorStart(item)">
                  <rect
                    :x="xOf(i) - 7.2"
                    :y="yOf(item.pointsThreshold) - 7.2"
                    width="14.4"
                    height="14.4"
                    :transform="'rotate(45 ' + xOf(i) + ' ' + yOf(item.pointsThreshold) + ')'"
                    :fill="stageColor(item.majorIndex)"
                    stroke="#ffffff"
                    stroke-width="2.2"
                    class="cursor-ns-resize"
                  />
                  <circle :cx="xOf(i)" :cy="yOf(item.pointsThreshold)" r="15" fill="transparent" class="cursor-ns-resize" @mousedown.prevent.stop="startDrag(item, i, $event)" />
                </template>
                <!-- 小阶段：圆点 -->
                <template v-else>
                  <circle :cx="xOf(i)" :cy="yOf(item.pointsThreshold)" r="4.8" fill="#ffffff" :stroke="stageColor(item.majorIndex)" stroke-width="2" />
                  <circle :cx="xOf(i)" :cy="yOf(item.pointsThreshold)" r="12" fill="transparent" class="cursor-ns-resize" @mousedown.prevent.stop="startDrag(item, i, $event)" />
                </template>
              </g>

              <!-- 拖动辅助线：跟随鼠标连续移动 -->
              <g v-if="dragging">
                <line :x1="PAD_L" :y1="yOf(pointerValue)" :x2="VIEW_W - PAD_R" :y2="yOf(pointerValue)" stroke="#2563eb" stroke-dasharray="4 3" stroke-width="1" />
                <rect :x="PAD_L + 6" :y="yOf(pointerValue) - 21" width="108" height="17" rx="4" fill="#1d4ed8" />
                <text :x="PAD_L + 12" :y="yOf(pointerValue) - 9" font-size="9.5" fill="#ffffff">Lv.{{ dragLevel }} · {{ dragValue }}</text>
              </g>
            </svg>

            <p class="text-[11px] text-on-surface-variant mt-1">
              大阶段按 {{ step }} 分吸附；拖动时小阶段保持原有比例（例：800→1100，小阶段 900/1000 为 1/3、2/3，
              把 1100 改到 900 后变为 833、866）。
            </p>
          </section>

          <!-- 概要：只放统计，逐阶段配置移到下方整行 -->
          <section class="lg:col-span-4 bg-container border border-border rounded-xl p-4">
            <h2 class="text-[14px] font-semibold text-on-surface mb-3">体系概要</h2>
            <div class="grid grid-cols-2 gap-2">
              <div class="rounded-lg border border-outline-variant bg-surface-container-lowest p-2.5">
                <p class="text-[10.5px] text-outline">大阶段</p>
                <p class="mt-0.5 text-[20px] font-semibold text-on-surface tabnum">{{ majorGroups.length }}</p>
              </div>
              <div class="rounded-lg border border-outline-variant bg-surface-container-lowest p-2.5">
                <p class="text-[10.5px] text-outline">总级数</p>
                <p class="mt-0.5 text-[20px] font-semibold text-on-surface tabnum">{{ items.length }}</p>
              </div>
              <div class="rounded-lg border border-outline-variant bg-surface-container-lowest p-2.5">
                <p class="text-[10.5px] text-outline">最高门槛</p>
                <p class="mt-0.5 text-[18px] font-semibold text-on-surface tabnum">{{ maxThreshold }}<span class="ml-0.5 text-[10px] font-normal text-outline">分</span></p>
              </div>
              <div class="rounded-lg border border-outline-variant bg-surface-container-lowest p-2.5">
                <p class="text-[10.5px] text-outline">区间跨度</p>
                <p class="mt-0.5 text-[12px] font-medium text-on-surface leading-5">{{ spanSummary }}</p>
              </div>
            </div>
            <p class="mt-3 rounded-lg bg-surface px-2.5 py-2 text-[11px] leading-4 text-on-surface-variant">
              每个大阶段的名称和小阶段数量在下方的「阶段配置」中单独调整。
            </p>
            <div class="mt-3 flex flex-wrap gap-1">
              <span
                v-for="(m, mi) in majorGroups"
                :key="'chip-' + mi"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px]"
                :style="{ backgroundColor: stageSoft(mi + 1), color: stageColor(mi + 1) }"
              >{{ m.name }}</span>
            </div>
          </section>
        </div>

        <!-- 阶段配置：整行横向铺开，避免右栏被 8 行配置撑高 -->
        <section class="bg-container border border-border rounded-xl mt-4 p-4">
          <div class="flex flex-wrap items-end justify-between gap-3 mb-3">
            <div>
              <h2 class="text-[14px] font-semibold text-on-surface">阶段配置</h2>
              <p class="text-[11px] text-on-surface-variant mt-0.5">每个大阶段可单独改名、设置小阶段数量；修改后自动重排该阶段门槛。</p>
            </div>
            <label class="flex items-center gap-2 text-[11.5px] text-secondary">
              大阶段数
              <input
                v-model.number="profile.majorCount"
                type="number"
                min="1"
                max="20"
                class="w-20 px-2 py-1.5 bg-surface border border-outline-variant rounded-lg text-[12px] text-right outline-none focus:border-primary"
                @change="onMajorCountChange"
              >
            </label>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-2">
            <div
              v-for="(m, mi) in majorGroups"
              :key="'cfg-' + mi"
              class="rounded-lg border border-outline-variant bg-surface-container-lowest p-2.5"
            >
              <div class="flex items-center gap-2">
                <span
                  class="grid place-items-center w-5 h-5 rounded-full text-[10px] text-white shrink-0"
                  :style="{ backgroundColor: stageColor(mi + 1) }"
                >{{ mi + 1 }}</span>
                <input
                  :value="m.name"
                  class="min-w-0 flex-1 px-2 py-1 bg-surface border border-outline-variant rounded text-[12px] outline-none focus:border-primary"
                  @change="renameMajor(mi + 1, $event.target.value)"
                >
              </div>
              <div class="mt-2 flex items-center justify-between gap-2">
                <span class="text-[10.5px] text-outline">小阶段数</span>
                <div class="flex items-center gap-1">
                  <button class="step-btn" @click="setMinorCount(mi, minorCounts[mi] - 1)">−</button>
                  <input
                    :value="minorCounts[mi]"
                    type="number"
                    min="1"
                    max="50"
                    class="w-12 px-1.5 py-1 bg-surface border border-outline-variant rounded text-[12px] text-center outline-none focus:border-primary"
                    @change="setMinorCount(mi, $event.target.value)"
                  >
                  <button class="step-btn" @click="setMinorCount(mi, minorCounts[mi] + 1)">+</button>
                </div>
              </div>
              <p class="mt-1 text-[10px] text-outline">
                Lv.{{ m.items[0].levelIndex }}–{{ m.items[m.items.length - 1].levelIndex }}
                · {{ m.items[0].pointsThreshold }} → {{ m.items[m.items.length - 1].pointsThreshold }} 分
              </p>
            </div>
            <p v-if="!majorGroups.length" class="text-[11px] text-on-surface-variant">暂无等级项，请先设置大阶段数。</p>
          </div>
        </section>

        <!-- 等级明细 -->
        <section class="bg-container border border-border rounded-xl mt-4 overflow-hidden">
          <div class="flex items-center justify-between px-4 py-2.5 border-b border-outline-variant">
            <h2 class="text-[14px] font-semibold text-on-surface">等级明细</h2>
            <span class="text-[11px] text-on-surface-variant">大阶段可点 − / + 按倍率调整；小阶段可精确输入</span>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-[12px]">
              <thead>
                <tr class="bg-surface-container-low text-on-surface-variant">
                  <th class="p-2 w-10 text-left">级</th>
                  <th class="p-2 w-20 text-left">图标</th>
                  <th class="p-2 w-14 text-center">小阶段</th>
                  <th class="p-2 text-left">显示名</th>
                  <th class="p-2 w-44 text-left">累计积分门槛</th>
                  <th class="p-2 w-24 text-left">操作</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="(group, gi) in majorGroups">
                  <tr :key="'g-' + gi" class="border-t border-outline-variant/70">
                    <td colspan="6" class="px-2 py-1.5" :style="{ backgroundColor: stageSoft(gi + 1) }">
                      <div class="flex items-center gap-2">
                        <span class="bbs-level-svg h-5 inline-flex" v-html="groupIcon(group)"></span>
                        <input
                          :value="group.name"
                          class="w-28 px-1.5 py-0.5 bg-transparent border border-transparent rounded text-[12px] font-medium outline-none hover:border-outline-variant focus:border-primary focus:bg-surface"
                          :style="{ color: stageColor(gi + 1) }"
                          title="点击修改大阶段名称"
                          @change="renameMajor(gi + 1, $event.target.value)"
                        >
                        <span class="text-[11px] text-on-surface-variant">
                          Lv.{{ group.items[0].levelIndex }}–{{ group.items[group.items.length - 1].levelIndex }}
                          · {{ group.items[0].pointsThreshold }} → {{ group.items[group.items.length - 1].pointsThreshold }} 分
                        </span>
                        <span
                          v-if="group.badge"
                          class="px-2 py-0.5 rounded-full text-[10px] bg-white/70"
                          :style="{ color: stageColor(gi + 1) }"
                        >徽章 {{ group.badge }}</span>
                        <span class="ml-auto text-[11px] text-on-surface-variant">
                          下一阶段 +{{ group.span || 0 }} 分
                        </span>
                      </div>
                    </td>
                  </tr>
                  <tr v-for="item in group.items" :key="'i-' + item.levelIndex" class="border-t border-outline-variant/30 hover:bg-surface-container-lowest">
                    <td class="p-2 text-on-surface-variant">{{ item.levelIndex }}</td>
                    <td class="p-2">
                      <span class="bbs-level-svg h-5 inline-flex" v-html="iconOf(item)"></span>
                    </td>
                    <td class="p-2 text-center">
                      <span
                        class="inline-flex items-center justify-center w-6 h-6 rounded-full text-[11px]"
                        :class="item.minorIndex === 1 ? 'text-white' : 'bg-surface-variant text-on-surface-variant'"
                        :style="item.minorIndex === 1 ? { backgroundColor: stageColor(item.majorIndex) } : {}"
                      >{{ item.minorIndex }}</span>
                    </td>
                    <td class="p-2">
                      <input v-model="item.displayName" class="cell-input w-full" :title="item.description || ''">
                    </td>
                    <td class="p-2">
                      <div class="inline-flex items-center gap-1">
                        <button class="step-btn" @click="nudge(item, -1)">−</button>
                        <input
                          v-model.number="item.pointsThreshold"
                          type="number"
                          :step="item.minorIndex === 1 ? step : 1"
                          min="0"
                          class="w-[74px] px-2 py-1 bg-surface border border-outline-variant rounded text-right outline-none focus:border-primary"
                          @change="onThresholdEdit(item)"
                        >
                        <button class="step-btn" @click="nudge(item, 1)">+</button>
                      </div>
                    </td>
                    <td class="p-2">
                      <div class="flex items-center gap-1">
                        <button class="icon-act" title="设置图标" @click="openIconDialog(item)">
                          <span class="material-symbols-outlined text-[15px]">palette</span>
                        </button>
                        <button class="icon-act" title="恢复自动图标" @click="resetIcon(item)">
                          <span class="material-symbols-outlined text-[15px]">restart_alt</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </div>

    <!-- 图标设置 -->
    <div v-if="iconDialog.visible" class="fixed inset-0 z-50 flex items-center justify-center p-4" :style="{ zIndex: overlayZ }" @click.self="iconDialog.visible = false">
      <div class="fixed inset-0 bg-black/30"></div>
      <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between px-5 py-3 border-b border-outline-variant">
          <h3 class="text-[14px] font-semibold text-on-surface">
            图标设置 · Lv.{{ iconDialog.levelIndex }} {{ iconDialog.displayName }}
          </h3>
          <button class="text-outline hover:text-error" @click="iconDialog.visible = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div class="flex items-center gap-4">
            <span class="bbs-level-svg h-12 inline-flex" v-html="iconPreview"></span>
            <div class="text-[11px] text-on-surface-variant">
              <p>{{ iconDialog.modeLabel }}</p>
              <p class="mt-0.5">未自定义时按等级自动生成</p>
            </div>
          </div>

          <div>
            <label class="text-[12px] text-secondary">图形</label>
            <div class="flex flex-wrap gap-2 mt-1.5">
              <button class="icon-option" :class="{ 'is-on': !iconDialog.glyph && !iconDialog.svg }" @click="pickAuto">自动</button>
              <button
                v-for="g in glyphs"
                :key="g.key"
                class="icon-option"
                :class="{ 'is-on': iconDialog.glyph === g.key && !iconDialog.svg }"
                @click="pickGlyph(g.key)"
              >
                <span class="bbs-level-svg h-4 inline-flex" v-html="glyphPreview(g.key)"></span>
                {{ g.label }}
              </button>
            </div>
          </div>

          <div>
            <label class="text-[12px] text-secondary">主色</label>
            <div class="flex items-center gap-2 mt-1.5">
              <input v-model="iconDialog.color" type="color" class="w-9 h-8 rounded border border-outline-variant bg-surface">
              <button v-for="c in palette" :key="c" class="w-6 h-6 rounded-full border border-white shadow" :style="{ backgroundColor: c }" @click="iconDialog.color = c"></button>
            </div>
          </div>

          <div class="rounded-lg border border-outline-variant p-3">
            <div class="flex items-center justify-between">
              <div class="min-w-0">
                <p class="text-[12px] text-secondary">素材库图标</p>
                <p class="text-[11px] text-outline truncate">{{ iconDialog.svg ? '已选择素材库 SVG' : '未选择' }}</p>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <button v-if="iconDialog.svg" class="text-[11px] text-outline hover:text-error" @click="clearSvg">清除</button>
                <button class="px-3 py-1.5 text-[11px] border border-outline rounded-lg hover:border-primary" @click="pickerVisible = true">从素材库选择</button>
              </div>
            </div>
            <p class="text-[11px] text-outline mt-1.5">需要新图形？去「SVG 工作台」绘制并保存后，再回来选择。</p>
          </div>
        </div>
        <div class="flex justify-end gap-2 px-5 py-3 border-t border-outline-variant bg-surface-container-lowest">
          <button class="px-4 py-2 border border-outline rounded-lg text-[12px]" @click="iconDialog.visible = false">取消</button>
          <button class="px-5 py-2 bg-primary text-white rounded-lg text-[12px]" @click="applyIcon">应用</button>
        </div>
      </div>
    </div>

    <SvgAssetPicker
      :visible="pickerVisible"
      category="level"
      title="选择等级图标"
      @close="pickerVisible = false"
      @select="onPickAsset"
    />
  </div>
</template>

<script>
import { LEVEL_TONES, LEVEL_GLYPHS, levelRowSvg, glyphSvg } from '../../../shared/level-icons'
import { handleResponse } from '../../../shared/feedback'
import { nextOverlayZ } from '@/utils/overlay'
import SvgAssetPicker from '@/components/SvgAssetPicker.vue'

const PAD_L = 62
const PAD_R = 24
const PAD_T = 20
const PAD_B = 42
const VIEW_W = 880
const VIEW_H = 340
const PLOT_W = VIEW_W - PAD_L - PAD_R
const PLOT_H = VIEW_H - PAD_T - PAD_B

const MAJOR_NAMES = ['学徒工', '初级工', '中级工', '高级工', '技师', '高级技师', '特级技师', '首席技师']
const PALETTE = ['#16a34a', '#0284c7', '#d97706', '#ca8a04', '#7c3aed', '#4f46e5', '#db2777', '#dc2626', '#0f766e', '#475569']

export default {
  name: 'LevelManagePage',
  components: { SvgAssetPicker },
  data() {
    return {
      loading: false,
      saving: false,
      step: 100,
      PAD_L,
      PAD_R,
      PAD_T,
      PAD_B,
      VIEW_W,
      VIEW_H,
      profile: { id: null, profileName: '默认等级体系', majorCount: 8, minorPerMajor: 3, status: 1 },
      profiles: [],
      activeProfileId: null,
      activating: false,
      items: [],
      minorCounts: [],
      autoMode: 'linear',
      autoRatio: 1.15,
      dragging: false,
      dragIndex: -1,
      dragLevel: 0,
      dragValue: 0,
      pointerValue: 0,
      ratios: null,
      glyphs: LEVEL_GLYPHS,
      palette: PALETTE,
      pickerVisible: false,
      overlayZ: 3000,
      iconDialog: {
        visible: false, index: -1, levelIndex: 1, displayName: '',
        glyph: '', color: '', svg: '', modeLabel: '',
      },
    }
  },
  computed: {
    majorGroups() {
      const groups = []
      this.items.forEach((item, index) => {
        if (!groups[item.majorIndex - 1]) {
          groups[item.majorIndex - 1] = { name: item.majorName, startIndex: index, items: [] }
        }
        groups[item.majorIndex - 1].items.push(item)
      })
      return groups.filter(Boolean).map(g => {
        const next = this.items.find(i => i.majorIndex > g.items[0].majorIndex && i.minorIndex === 1)
        const start = g.items[0].pointsThreshold
        const end = next ? next.pointsThreshold : g.items[g.items.length - 1].pointsThreshold
        return { ...g, span: Math.max(0, end - start), badge: g.items[0].badgeName || '' }
      })
    },
    currentProfileStatus() {
      const hit = this.profiles.find(p => p.profile && p.profile.id === this.activeProfileId)
      return hit && hit.profile ? hit.profile.status : 0
    },
    maxThreshold() {
      return this.items.reduce((m, i) => Math.max(m, Number(i.pointsThreshold) || 0), 0)
    },
    spanSummary() {
      const spans = this.majorGroups.map(g => g.span).filter(v => v > 0)
      if (!spans.length) return '—'
      return `最小 ${Math.min(...spans)} 分 / 最大 ${Math.max(...spans)} 分`
    },
    yMax() {
      const raw = Math.max(this.maxThreshold, this.step * 3)
      return (Math.ceil(raw / this.step) + 1) * this.step
    },
    yTicks() {
      const total = Math.round(this.yMax / this.step)
      const every = Math.max(1, Math.ceil(total / 8))
      const ticks = []
      for (let i = 0; i <= total; i += every) ticks.push(i * this.step)
      return ticks
    },
    polyline() {
      return this.items.map((item, i) => `${this.xOf(i)},${this.yOf(item.pointsThreshold)}`).join(' ')
    },
    iconPreview() {
      if (this.iconDialog.svg) return this.iconDialog.svg
      if (this.iconDialog.glyph) return glyphSvg(this.iconDialog.glyph, this.iconDialog.color)
      return levelRowSvg(this.iconDialog.levelIndex, 28)
    },
  },
  mounted() { this.load() },
  beforeDestroy() { this.detachDrag() },
  methods: {
    /* ---------- 坐标 ---------- */
    xOf(index) {
      const n = Math.max(1, this.items.length - 1)
      return PAD_L + (index / n) * PLOT_W
    },
    yOf(value) {
      const v = Math.max(0, Math.min(this.yMax, Number(value) || 0))
      return PAD_T + PLOT_H - (v / this.yMax) * PLOT_H
    },
    stageLabelX(group) {
      const first = group.startIndex
      const last = first + group.items.length - 1
      return (this.xOf(first) + this.xOf(last)) / 2
    },
    stageColor(majorIndex) {
      const t = LEVEL_TONES[(majorIndex - 1) % LEVEL_TONES.length]
      return t ? t.color : '#2563eb'
    },
    stageSoft(majorIndex) {
      const t = LEVEL_TONES[(majorIndex - 1) % LEVEL_TONES.length]
      return t ? t.soft : '#eff6ff'
    },
    isMajorStart(item) {
      return item.minorIndex === 1
    },
    iconOf(item) {
      if (item.iconSvg) return item.iconSvg
      if (item.iconKey && LEVEL_GLYPHS.some(g => g.key === item.iconKey)) {
        return glyphSvg(item.iconKey, item.colorToken || this.stageColor(item.majorIndex))
      }
      return levelRowSvg(item.levelIndex, 20)
    },
    groupIcon(group) {
      return this.iconOf(group.items[0])
    },
    glyphPreview(key) {
      return glyphSvg(key, this.iconDialog.color || this.stageColor(1))
    },
    /* ---------- 数据 ---------- */
    async load() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/admin/level/profiles')
        const rows = (res && res.obj) || []
        this.profiles = rows
        if (!rows.length) {
          this.rebuild()
          return
        }
        let entry = rows.find(r => r.profile && r.profile.status === 1) || rows[0]
        if (this.activeProfileId != null) {
          const keep = rows.find(r => r.profile && r.profile.id === this.activeProfileId)
          if (keep) entry = keep
        }
        this.applyProfileEntry(entry)
      } catch (e) {
        this.rebuild()
      } finally {
        this.loading = false
      }
    },
    applyProfileEntry(entry) {
      if (!entry || !entry.profile) return
      this.profile = { ...entry.profile }
      this.activeProfileId = entry.profile.id
      this.items = (entry.items || []).map(i => ({
        levelIndex: i.levelIndex,
        majorIndex: i.majorIndex,
        majorName: i.majorName,
        minorIndex: i.minorIndex,
        displayName: i.displayName,
        pointsThreshold: Number(i.pointsThreshold) || 0,
        iconKey: i.iconKey,
        colorToken: i.colorToken,
        iconSvg: i.iconSvg || '',
        badgeName: i.badgeName || '',
        description: i.description || '',
      }))
      this.minorCounts = this.deriveMinorCounts()
    },
    deriveMinorCounts() {
      const counts = []
      this.items.forEach(i => {
        const idx = Number(i.majorIndex) - 1
        if (idx >= 0) counts[idx] = (counts[idx] || 0) + 1
      })
      const majors = counts.length > 0
        ? counts.length
        : Math.max(1, Number(this.profile.majorCount) || 8)
      const fallback = Math.max(1, Number(this.profile.minorPerMajor) || 3)
      for (let i = 0; i < majors; i++) {
        if (!counts[i]) counts[i] = fallback
      }
      return counts.length ? counts : Array.from({ length: 8 }, () => 3)
    },
    onProfileSelect() {
      const entry = this.profiles.find(p => p.profile && p.profile.id === this.activeProfileId)
      if (entry) this.applyProfileEntry(entry)
    },
    async activateProfile() {
      if (!this.profile.id) return
      this.activating = true
      try {
        const res = await this.postRequest('/admin/level/activate', { id: this.profile.id })
        handleResponse(res, { successMsg: '已切换方案', errorMsg: '切换失败', onSuccess: () => this.load() })
      } finally {
        this.activating = false
      }
    },
    cloneProfile() {
      this.$prompt('会先把当前页面上的修改一起保存到新方案。', '另存为新方案', {
        inputValue: (this.profile.profileName || '等级方案') + ' 副本',
        inputPattern: /\S+/,
        inputErrorMessage: '名称不能为空',
        confirmButtonText: '保存为新方案',
      }).then(({ value }) => this.saveAsNew(value)).catch(() => {})
    },
    async saveAsNew(name) {
      this.saving = true
      try {
        this.syncProfileCounts()
        const newProfile = {
          ...this.profile,
          id: null,
          profileName: name,
          status: 0,
          createTime: null,
          updateTime: null,
        }
        const res = await this.postRequest('/admin/level/save', this.buildPayload(newProfile))
        if (res && res.code === 200) {
          this.activeProfileId = res.obj || null
          this.$message.success('当前修改已保存为新方案')
          await this.load()
        } else {
          this.$message.error((res && res.message) || '另存失败')
        }
      } catch (e) {
        this.$message.error('另存失败')
      } finally {
        this.saving = false
      }
    },
    removeProfile() {
      if (!this.profile.id) return
      this.$confirm(`确定删除方案「${this.profile.profileName}」？删除后不可恢复。`, '删除方案', { type: 'warning' })
        .then(async () => {
          const res = await this.postRequest('/admin/level/delete', { id: this.profile.id })
          if (res && res.code === 200) {
            this.$message.success('已删除')
            this.activeProfileId = null
            this.load()
          } else {
            this.$message.error((res && res.message) || '删除失败')
          }
        }).catch(() => {})
    },
    onMajorCountChange() {
      const majors = Math.max(1, Math.min(20, Math.floor(Number(this.profile.majorCount) || 1)))
      this.profile.majorCount = majors
      const counts = this.minorCounts.slice(0, majors)
      while (counts.length < majors) {
        counts.push(Math.max(1, Number(this.profile.minorPerMajor) || 3))
      }
      this.minorCounts = counts
      this.rebuild()
    },
    setMinorCount(mi, value) {
      const count = Math.max(1, Math.min(50, Math.floor(Number(value) || 1)))
      this.$set(this.minorCounts, mi, count)
      this.rebuild()
    },
    renameMajor(majorIndex, value) {
      const name = (value || '').trim()
      if (!name) return
      const group = this.items.filter(i => i.majorIndex === majorIndex)
      if (!group.length) return
      const oldName = group[0].majorName
      group.forEach(item => {
        item.majorName = name
        if (!item.displayName || item.displayName === `${oldName}-${item.minorIndex}`) {
          item.displayName = `${name}-${item.minorIndex}`
        }
      })
    },
    rebuild() {
      const majors = Math.max(1, Math.min(20, Math.floor(Number(this.profile.majorCount) || 1)))
      const counts = []
      for (let i = 0; i < majors; i++) {
        const c = Number(this.minorCounts[i])
        counts.push(Number.isFinite(c) && c >= 1 ? Math.floor(c) : 3)
      }
      this.minorCounts = counts
      this.profile.majorCount = majors
      this.profile.minorPerMajor = counts[0] || 3

      const oldItems = this.items.slice()
      const oldGroups = {}
      const oldStarts = {}
      oldItems.forEach(item => {
        if (!oldGroups[item.majorIndex]) oldGroups[item.majorIndex] = []
        oldGroups[item.majorIndex].push(item)
        if (item.minorIndex === 1) oldStarts[item.majorIndex] = Number(item.pointsThreshold) || 0
      })

      const items = []
      let level = 1
      for (let m = 1; m <= counts.length; m++) {
        const oldGroup = oldGroups[m] || []
        const name = (oldGroup[0] && oldGroup[0].majorName) || MAJOR_NAMES[m - 1] || ('阶段' + m)
        const count = counts[m - 1]
        let start = oldStarts[m]
        if (start == null) {
          const prev = items.length ? items[items.length - 1].pointsThreshold : -this.step
          start = prev + this.step
        }
        let end = (m < counts.length && oldStarts[m + 1] != null)
          ? oldStarts[m + 1]
          : start + count * this.step
        if (end <= start) end = start + count * this.step

        for (let n = 0; n < count; n++) {
          const old = oldGroup[n] || oldGroup[oldGroup.length - 1] || {}
          let threshold = n === 0
            ? start
            : Math.floor(start + (end - start) * n / count)
          const prev = items.length ? items[items.length - 1].pointsThreshold : -1
          if (threshold <= prev) threshold = prev + 1
          items.push({
            levelIndex: level++,
            majorIndex: m,
            majorName: name,
            minorIndex: n + 1,
            displayName: (old.displayName && old.majorName === name && old.minorIndex === n + 1)
              ? old.displayName
              : (name + '-' + (n + 1)),
            pointsThreshold: threshold,
            iconKey: old.iconKey || null,
            colorToken: old.colorToken || null,
            iconSvg: old.iconSvg || '',
            badgeName: old.badgeName || '',
            description: old.description || '',
          })
        }
      }
      this.items = items
    },
    onStepChange() {
      const s = Number(this.step)
      this.step = Number.isFinite(s) && s >= 10 ? Math.round(s) : 100
    },
    autoRelayout() {
      const mode = this.autoMode
      const ratio = Math.max(1.01, Number(this.autoRatio) || 1.15)
      const fib = (n) => {
        if (n <= 2) return 1
        let a = 1, b = 1
        for (let i = 3; i <= n; i++) {
          const c = a + b
          a = b
          b = c
        }
        return b
      }
      let value = 0
      this.items.forEach((item, i) => {
        if (i > 0) {
          let interval = this.step
          if (mode === 'geometric') interval = this.step * Math.pow(ratio, i - 1)
          else if (mode === 'fibonacci') interval = this.step * fib(i)
          else if (mode === 'quadratic') interval = this.step * i
          value += interval
        }
        item.pointsThreshold = Math.round(value)
      })
      this.ratios = null
    },
    findIndex(levelIndex) {
      return this.items.findIndex(i => i.levelIndex === levelIndex)
    },
    /* ---------- 比例联动 ---------- */
    /**
     * 记录每个阶段内小阶段相对「本阶段起点 → 下一阶段起点」的比例。
     * 拖动大阶段后按这些比例重算，保证 900/1000 在区间压缩为 100 时变成 833/866。
     */
    captureRatios() {
      const ratios = {}
      const majors = this.items.filter(i => i.minorIndex === 1)
      majors.forEach((major, mi) => {
        const next = majors[mi + 1] || null
        const group = this.items.filter(i => i.majorIndex === major.majorIndex)
        const start = Number(major.pointsThreshold) || 0
        let end
        if (next) {
          end = Number(next.pointsThreshold)
        } else {
          end = start + Math.max(this.step, (group.length - 1) * this.step)
        }
        const span = end - start
        group.forEach((item, gi) => {
          if (gi === 0) return
          const ratio = span > 0
            ? (Number(item.pointsThreshold) - start) / span
            : gi / Math.max(1, group.length - 1)
          ratios[item.levelIndex] = Math.max(0.01, Math.min(0.99, ratio))
        })
      })
      return ratios
    },
    applyRatios(ratios) {
      if (!ratios) return
      const majors = this.items.filter(i => i.minorIndex === 1)
      majors.forEach((major, mi) => {
        const next = majors[mi + 1] || null
        const group = this.items.filter(i => i.majorIndex === major.majorIndex)
        const start = Number(major.pointsThreshold) || 0
        let end
        if (next) {
          end = Number(next.pointsThreshold)
        } else {
          end = start + Math.max(this.step, (group.length - 1) * this.step)
        }
        for (let gi = 1; gi < group.length; gi++) {
          const item = group[gi]
          const ratio = ratios[item.levelIndex] || gi / Math.max(1, group.length - 1)
          // 用户口径：833 / 866 → 向下取整
          let value = Math.floor(start + ratio * (end - start))
          const prev = group[gi - 1].pointsThreshold
          if (value <= prev) value = prev + 1
          if (next && value >= end) value = end - 1
          item.pointsThreshold = Math.max(0, value)
        }
      })
    },
    rescaleFromMajor(majorIndex) {
      if (!this.ratios) this.ratios = this.captureRatios()
      this.applyRatios(this.ratios)
      void majorIndex
    },
    /* ---------- 门槛 ---------- */
    setThreshold(levelIndex, rawValue) {
      const idx = this.findIndex(levelIndex)
      if (idx < 0) return
      const item = this.items[idx]
      const majors = this.items.filter(i => i.minorIndex === 1)

      if (item.minorIndex === 1) {
        const pos = majors.findIndex(m => m.levelIndex === levelIndex)
        const prev = pos > 0 ? majors[pos - 1] : null
        const next = pos >= 0 && pos < majors.length - 1 ? majors[pos + 1] : null
        const lower = prev ? Number(prev.pointsThreshold) + this.step : 0
        const upper = next ? Number(next.pointsThreshold) - this.step : Number.MAX_SAFE_INTEGER
        // 拖动时沿用拖动开始时的比例；单次编辑则按当前布局重新采样
        const keepRatios = this.dragging
        if (!this.ratios) this.ratios = this.captureRatios()
        const snapped = Math.round((Number(rawValue) || 0) / this.step) * this.step
        item.pointsThreshold = Math.min(Math.max(snapped, lower), Math.max(lower, upper))
        this.applyRatios(this.ratios)
        if (!keepRatios) this.ratios = null
      } else {
        const major = majors.find(m => m.majorIndex === item.majorIndex)
        const nextMajor = majors.find(m => m.majorIndex === item.majorIndex + 1)
        const lower = major ? Number(major.pointsThreshold) + 1 : 0
        const upper = nextMajor ? Number(nextMajor.pointsThreshold) - 1 : Number.MAX_SAFE_INTEGER
        const nextItem = this.items[idx + 1]
        const prevItem = this.items[idx - 1]
        let value = Math.round(Number(rawValue) || 0)
        value = Math.min(value, upper)
        if (nextItem) value = Math.min(value, Number(nextItem.pointsThreshold) - 1)
        value = Math.max(value, lower)
        if (prevItem) value = Math.max(value, Number(prevItem.pointsThreshold) + 1)
        item.pointsThreshold = value
      }
    },
    nudge(item, dir) {
      const delta = item.minorIndex === 1 ? this.step : 1
      this.setThreshold(item.levelIndex, (Number(item.pointsThreshold) || 0) + dir * delta)
    },
    onThresholdEdit(item) {
      this.setThreshold(item.levelIndex, item.pointsThreshold)
    },
    /* ---------- 拖动 ---------- */
    startDrag(item, index, event) {
      this.dragging = true
      this.dragIndex = index
      this.dragLevel = item.levelIndex
      this.dragValue = Number(item.pointsThreshold) || 0
      this.pointerValue = this.dragValue
      this.ratios = this.captureRatios()
      window.addEventListener('mousemove', this.onDragMove)
      window.addEventListener('mouseup', this.endDrag)
      if (event) this.onDragMove(event)
    },
    onBackgroundDown() {
      // 空白处不处理，避免误触；点击节点由节点自身 mousedown 处理
    },
    onDragMove(event) {
      if (!this.dragging) return
      const svg = this.$refs.chart
      if (!svg) return
      const rect = svg.getBoundingClientRect()
      if (!rect.height) return
      const svgY = ((event.clientY - rect.top) / rect.height) * VIEW_H
      const ratio = (PAD_T + PLOT_H - svgY) / PLOT_H
      const raw = Math.max(0, Math.min(1, ratio)) * this.yMax
      this.pointerValue = raw
      const item = this.items[this.dragIndex]
      if (!item) return
      this.setThreshold(item.levelIndex, raw)
      this.dragValue = this.items[this.dragIndex].pointsThreshold
    },
    endDrag() {
      if (!this.dragging) return
      this.dragging = false
      this.dragIndex = -1
      this.ratios = null
      this.detachDrag()
    },
    detachDrag() {
      window.removeEventListener('mousemove', this.onDragMove)
      window.removeEventListener('mouseup', this.endDrag)
    },
    /* ---------- 图标 ---------- */
    openIconDialog(item) {
      this.overlayZ = nextOverlayZ()
      this.iconDialog = {
        visible: true,
        index: this.findIndex(item.levelIndex),
        levelIndex: item.levelIndex,
        displayName: item.displayName,
        glyph: LEVEL_GLYPHS.some(g => g.key === item.iconKey) ? item.iconKey : '',
        color: item.colorToken || this.stageColor(item.majorIndex),
        svg: item.iconSvg || '',
        modeLabel: item.iconSvg ? '自定义 SVG（素材库）' : (item.iconKey ? '自定义图形' : '自动生成'),
      }
    },
    pickAuto() {
      this.iconDialog.glyph = ''
      this.iconDialog.svg = ''
      this.iconDialog.modeLabel = '自动生成'
    },
    pickGlyph(key) {
      this.iconDialog.glyph = key
      this.iconDialog.svg = ''
      this.iconDialog.modeLabel = '自定义图形 · ' + key
    },
    clearSvg() {
      this.iconDialog.svg = ''
      this.iconDialog.modeLabel = this.iconDialog.glyph ? '自定义图形' : '自动生成'
    },
    onPickAsset(asset) {
      if (!asset || !asset.svg) return
      this.iconDialog.svg = asset.svg
      this.iconDialog.glyph = ''
      this.iconDialog.modeLabel = '素材库 · ' + (asset.assetName || '未命名')
      this.pickerVisible = false
    },
    applyIcon() {
      const item = this.items[this.iconDialog.index]
      if (!item) return
      if (this.iconDialog.svg) {
        item.iconSvg = this.iconDialog.svg
        item.iconKey = null
      } else {
        item.iconSvg = ''
        item.iconKey = this.iconDialog.glyph || null
        item.colorToken = this.iconDialog.glyph ? this.iconDialog.color : null
      }
      this.iconDialog.visible = false
    },
    resetIcon(item) {
      item.iconSvg = ''
      item.iconKey = null
      item.colorToken = null
    },
    /* ---------- 保存 ---------- */
    syncProfileCounts() {
      const counts = this.deriveMinorCounts()
      this.minorCounts = counts
      this.profile.majorCount = counts.length
      this.profile.minorPerMajor = counts[0] || 3
      this.profile.minorCounts = counts.slice()
    },
    buildPayload(profileOverride) {
      return {
        profile: profileOverride || { ...this.profile, minorCounts: this.minorCounts.slice() },
        items: this.items.map(i => ({
          levelIndex: i.levelIndex,
          majorIndex: i.majorIndex,
          majorName: i.majorName,
          minorIndex: i.minorIndex,
          displayName: i.displayName,
          pointsThreshold: Number(i.pointsThreshold) || 0,
          iconKey: i.iconKey || null,
          colorToken: i.colorToken || null,
          iconSvg: i.iconSvg || null,
          badgeName: i.badgeName || null,
          description: i.description || null,
        })),
      }
    },
    async save() {
      if (!this.items.length) { this.$message.warning('请先设置等级项'); return }
      this.saving = true
      try {
        this.syncProfileCounts()
        const res = await this.postRequest('/admin/level/save', this.buildPayload())
        handleResponse(res, { successMsg: '等级配置已保存', errorMsg: '保存失败', onSuccess: () => this.load() })
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
.step-btn {
  width: 22px;
  height: 24px;
  border-radius: 6px;
  border: 1px solid #dbe1ea;
  color: #475569;
  font-size: 12px;
  line-height: 1;
  background: #fff;
}
.step-btn:hover { border-color: #2563eb; color: #2563eb; }

.icon-act {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}
.icon-act:hover { background: rgba(148, 163, 184, 0.16); color: #1d4ed8; }

.cell-input {
  padding: 4px 8px;
  background: #fff;
  border: 1px solid transparent;
  border-radius: 6px;
  outline: none;
}
.cell-input:hover { border-color: #dbe1ea; }
.cell-input:focus { border-color: #2563eb; }

.icon-option {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 11px;
  color: #475569;
  background: #fff;
}
.icon-option.is-on { border-color: #2563eb; color: #1d4ed8; background: #eff6ff; }

.legend-major,
.legend-minor {
  display: inline-block;
  width: 9px;
  height: 9px;
}
.legend-major {
  background: #2563eb;
  transform: rotate(45deg);
  border-radius: 1px;
}
.legend-minor {
  background: #fff;
  border: 2px solid #2563eb;
  border-radius: 50%;
}
</style>
