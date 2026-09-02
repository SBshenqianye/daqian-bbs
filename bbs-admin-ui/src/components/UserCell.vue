<template>
  <el-popover v-if="userId" placement="right" trigger="click" :width="320">
    <div class="space-y-1">
      <p class="font-medium text-sm">{{ displayName }}</p>
      <p class="text-xs text-gray-500">用户名: {{ userInfo.username || '-' }}</p>
      <p class="text-xs text-gray-500">昵称: {{ userInfo.nickname || '-' }}</p>
      <p v-if="userInfo.orgPath" class="text-xs text-gray-500 break-all">单位: {{ userInfo.orgPath }}</p>
      <p v-else class="text-xs text-gray-500">单位: {{ userInfo.orgNameFull || userInfo.orgName || '未分配' }}</p>
    </div>
    <span slot="reference" class="cursor-pointer text-blue-600 hover:underline hover:text-blue-800">{{ displayName }}</span>
  </el-popover>
  <span v-else class="text-gray-400">未指定</span>
</template>

<script>
import axios from 'axios'

// 模块级共享缓存，多个 UserCell 实例共享同一份数据
const userCache = {}
// 正在请求中的 userId 集合，避免并发重复请求
const pendingIds = {}

/**
 * 后端 getUserinfoById 返回裸 Map（非 ResultBean），
 * axios 拦截器已解包，res 就是响应体本身。
 */
async function fetchUser(userId) {
  if (userCache[userId] || pendingIds[userId]) return
  pendingIds[userId] = true
  try {
    const res = await axios.post(`${process.env.VUE_APP_BBS_API}/common/user/getUserinfoById/${userId}`)
    if (res && res.id) {
      userCache[userId] = {
        id: res.id,
        username: res.username,
        nickname: res.nickname,
        orgName: res.orgName,
        deptName: res.deptName,
        orgNameFull: res.orgNameFull,
        orgPath: res.orgPath,
        portrait: res.portrait
      }
    }
  } catch (e) {
    // 请求失败时缓存空对象，避免重复请求
    userCache[userId] = {}
  } finally {
    delete pendingIds[userId]
  }
}

export default {
  name: 'UserCell',
  props: {
    userId: { type: [Number, String], default: null },
    /** 可选：已有的昵称/名称，优先用于显示（避免等待异步加载） */
    name: { type: String, default: '' }
  },
  data() {
    return {
      userInfo: userCache[this.userId] || {},
      _pollTimer: null
    }
  },
  computed: {
    displayName() {
      const u = this.userInfo
      if (u && (u.nickname || u.username)) {
        return u.nickname || u.username
      }
      if (this.name) return this.name
      return '用户#' + this.userId
    }
  },
  watch: {
    userId: {
      immediate: true,
      handler(val) {
        if (val && !userCache[val]) {
          fetchUser(val).then(() => this._syncInfo())
        } else {
          this.userInfo = userCache[val] || {}
        }
      }
    }
  },
  mounted() {
    // 等待异步缓存填充（批量挂载时仅首个实例发请求，其余需轮询）
    if (this.userId && !userCache[this.userId]) {
      this._pollTimer = setInterval(() => {
        if (userCache[this.userId]) {
          this.userInfo = userCache[this.userId]
          clearInterval(this._pollTimer)
        }
      }, 200)
      setTimeout(() => clearInterval(this._pollTimer), 5000)
    }
  },
  beforeDestroy() {
    if (this._pollTimer) clearInterval(this._pollTimer)
  },
  methods: {
    _syncInfo() {
      if (this.userId && userCache[this.userId]) {
        this.userInfo = userCache[this.userId]
      }
    }
  }
}
</script>
