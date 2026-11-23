<template>
  <div class="relative" ref="wrapper">
    <!-- 已选择状态 -->
    <div v-if="selectedUser" class="flex items-center gap-2 px-3 py-2 bg-surface border border-outline-variant rounded-lg cursor-pointer hover:border-primary" @click="clear">
      <img v-if="selectedUser.portrait" :src="selectedUser.portrait" class="w-6 h-6 rounded-full object-cover" />
      <div v-else class="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center text-[11px] text-primary font-medium">{{ (selectedUser.nickname || selectedUser.username || '?').charAt(0) }}</div>
      <span class="text-body-sm flex-1 truncate">{{ selectedUser.nickname || selectedUser.username }}</span>
      <span class="text-body-xs text-on-surface-variant">ID:{{ selectedUser.id }}</span>
      <span class="material-symbols-outlined text-[16px] text-on-surface-variant">close</span>
    </div>

    <!-- 搜索输入 -->
    <div v-else>
      <input
        ref="searchInput"
        v-model="keyword"
        type="text"
        class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none text-body-sm"
        :placeholder="placeholder"
        @input="onInput"
        @focus="onFocus"
        @click="onFocus"
        @blur="onBlur"
      />
    </div>

    <!-- 下拉搜索结果 -->
    <div v-if="showDropdown && results.length > 0" class="absolute z-50 mt-1 w-full bg-container border border-border rounded-lg shadow-lg max-h-60 overflow-y-auto">
      <div v-if="!keyword.trim() && candidates.length" class="sticky top-0 px-3 py-1.5 text-[11px] text-on-surface-variant bg-surface-container-low">
        被举报候选（有未处理举报）
      </div>
      <div
        v-for="user in results"
        :key="user.id + '|' + (user.candidateKey || '')"
        class="flex items-center gap-2 px-3 py-2 hover:bg-surface-container-low cursor-pointer border-b border-outline-variant/30 last:border-0"
        @mousedown.prevent="onSelect(user)"
      >
        <img v-if="user.portrait" :src="user.portrait" class="w-7 h-7 rounded-full object-cover flex-shrink-0" />
        <div v-else class="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center text-[11px] text-primary font-medium flex-shrink-0">{{ (user.nickname || user.username || '?').charAt(0) }}</div>
        <div class="flex-1 min-w-0">
          <div class="text-body-sm font-medium truncate">{{ user.nickname || user.username }}</div>
          <div class="text-[11px] text-on-surface-variant truncate">{{ user.extra || ((user.username || '') + ' · ' + (user.orgName || '未分配单位')) }}</div>
        </div>
        <el-tooltip :content="tooltipContent(user)" placement="right" :open-delay="300">
          <span class="material-symbols-outlined text-[14px] text-on-surface-variant cursor-help">info</span>
        </el-tooltip>
      </div>
    </div>

    <!-- 搜索中无结果 -->
    <div v-if="showDropdown && keyword && results.length === 0 && !searching" class="absolute z-50 mt-1 w-full bg-container border border-border rounded-lg shadow-lg p-3 text-center text-body-sm text-on-surface-variant">
      无匹配用户
    </div>
  </div>
</template>

<script>
import axios from 'axios'


export default {
  name: 'UserSelect',
  props: {
    value: { type: [Number, String], default: null },
    placeholder: { type: String, default: '搜索用户名或昵称...' },
    /** 快捷候选（如「被举报的用户」）：聚焦且未输入关键词时展示 */
    candidates: { type: Array, default: () => [] },
  },
  data() {
    return {
      keyword: '',
      selectedUser: null,
      results: [],
      showDropdown: false,
      searching: false,
      debounceTimer: null,
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        if (val && !this.selectedUser) {
          // 外部传入了 userId 但没有选中对象，尝试获取用户信息
          this.fetchUserById(val)
        } else if (!val) {
          this.selectedUser = null
          this.keyword = ''
        }
      }
    },
    candidates(list) {
      // 候选异步加载完成时：下拉已开则刷新；输入框仍聚焦（用户先点了输入框、候选后到）也直接展开
      if (this.keyword.trim()) return
      const active = this.$refs.searchInput && document.activeElement === this.$refs.searchInput
      if (this.showDropdown || active) {
        this.results = (list || []).slice()
        if (active) this.showDropdown = this.results.length > 0
      }
    }
  },
  methods: {
    tooltipContent(user) {
      return `ID: ${user.id} | 用户名: ${user.username} | 昵称: ${user.nickname || '无'} | 单位: ${user.orgName || '未分配'} | 权限组: ${user.primaryGroupName || '未分配'}`
    },
    onInput() {
      clearTimeout(this.debounceTimer)
      if (!this.keyword.trim()) {
        // 清空关键词：回到候选列表（如「被举报候选」），没有候选则收起
        this.results = this.candidates.slice()
        this.showDropdown = this.results.length > 0
        return
      }
      this.debounceTimer = setTimeout(() => {
        this.search(this.keyword.trim())
      }, 300)
    },
    onFocus() {
      if (this.keyword.trim() && this.results.length > 0) {
        this.showDropdown = true
      } else if (!this.keyword.trim() && this.candidates.length > 0) {
        this.results = this.candidates.slice()
        this.showDropdown = true
      }
    },
    onBlur() {
      // 延迟关闭：留出点击下拉项的时间；若这期间又回到输入框（焦点未移走）则不要再关
      setTimeout(() => {
        if (document.activeElement !== this.$refs.searchInput) this.showDropdown = false
      }, 200)
    },
    async search(kw) {
      this.searching = true
      this.showDropdown = true
      try {
        const token = window.sessionStorage.getItem('tokenStr')
        const res = await axios.get(`${process.env.VUE_APP_BBS_API}/admin/user/search`, {
          params: { keyword: kw },
          headers: token ? { Authorization: token } : {}
        })
        // api.js 拦截器已解包: res = { code, message, obj }
        const data = res && res.data ? res.data : res
        if (data && data.code == 200 && Array.isArray(data.obj)) {
          this.results = data.obj
        } else {
          this.results = []
        }
      } catch (e) {
        console.error('[UserSelect] search error:', e)
        this.results = []
      }
      this.searching = false
    },
    async fetchUserById(userId) {
      try {
        const token = window.sessionStorage.getItem('tokenStr')
        const res = await axios.post(`${process.env.VUE_APP_BBS_API}/common/user/getUserinfoById/${userId}`, null, {
          headers: token ? { Authorization: token } : {}
        })
        const data = res && res.data ? res.data : res
        if (data && data.id) {
          this.selectedUser = data
        }
      } catch (e) { /* ignore */ }
    },
    onSelect(user) {
      this.selectedUser = user
      this.keyword = ''
      this.results = []
      this.showDropdown = false
      this.$emit('input', user.id)
      this.$emit('select', user)
    },
    clear() {
      this.selectedUser = null
      this.keyword = ''
      this.results = []
      this.$emit('input', null)
      this.$emit('select', null)
    }
  }
}
</script>
