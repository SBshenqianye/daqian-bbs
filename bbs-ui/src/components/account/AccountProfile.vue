<template>
  <div class="space-y-8">
    <!-- 头像 + 昵称 -->
    <div class="flex items-center gap-6 pb-8 border-b border-outline-variant">
      <div class="relative group tooltip cursor-pointer" data-tooltip="点击上传头像" @click="showAvatarDialog = true">
        <img
          alt="用户头像"
          class="w-24 h-24 rounded-full border-4 border-surface-container-high object-cover transition-opacity group-hover:opacity-80"
          :src="avatarSrc || require('@/assets/portrait.png')"
        >
        <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <span class="material-symbols-outlined text-white text-3xl">upload</span>
        </div>
      </div>
      <div>
        <h1 class="font-headline-lg text-headline-lg text-on-surface">{{ userInfo.nickname || '未设置昵称' }}</h1>
        <p v-if="userInfo.orgNameFull || userInfo.orgName" class="font-body-md text-body-md text-on-surface-variant mt-1">
          {{ userInfo.orgNameFull || userInfo.orgName }}
        </p>
        <p v-else-if="userInfo.username" class="font-body-md text-body-md text-on-surface-variant mt-1">{{ userInfo.username }}</p>
      </div>
    </div>

    <!-- 保存提示 -->
    <div v-if="showSaveNotification" class="flex items-center justify-between p-4 bg-primary-fixed/30 border border-primary/20 rounded-lg">
      <div class="flex items-center gap-3">
        <span class="material-symbols-outlined text-primary">info</span>
        <span class="font-body-md text-on-surface">我的资料更新</span>
      </div>
      <div class="flex gap-2">
        <button class="px-4 py-1.5 bg-primary text-white text-label-md rounded-lg hover:bg-primary-container transition-colors" :disabled="saving" @click="saveChanges">{{ saving ? '保存中...' : '保存' }}</button>
        <button class="px-4 py-1.5 bg-error text-white text-label-md rounded-lg hover:bg-red-700 transition-colors" @click="cancelChanges">取消</button>
      </div>
    </div>

    <!-- 基本信息 -->
    <div class="space-y-6">
      <h3 class="font-headline-sm text-headline-sm text-on-surface">基本信息</h3>
      <div class="grid grid-cols-1 gap-y-6 max-w-2xl">
        <div class="flex items-center py-4 border-b border-outline-variant">
          <label class="w-32 font-label-md text-label-md text-on-surface-variant">用户昵称</label>
          <div class="font-body-lg text-body-lg text-on-surface">{{ userInfo.nickname }}</div>
        </div>
        <div class="flex items-center py-4 border-b border-outline-variant">
          <label class="w-32 font-label-md text-label-md text-on-surface-variant">人员编号</label>
          <div class="font-body-lg text-body-lg text-on-surface">{{ userInfo.personnelId }}</div>
        </div>
        <div class="flex items-center py-4 border-b border-outline-variant group cursor-default relative">
          <label class="w-32 font-label-md text-label-md text-on-surface-variant">手机号码</label>
          <div class="flex-1 flex items-center justify-between">
            <div v-show="!editingPhone" class="font-body-lg text-body-lg text-on-surface">{{ userInfo.phone }}</div>
            <input
              v-show="editingPhone"
              ref="phoneInput"
              v-model="userInfo.phone"
              class="flex-1 px-3 py-1 border border-outline-variant rounded focus:ring-1 focus:ring-primary focus:border-primary outline-none"
              type="text"
              @input="showSaveNotification = true"
            >
            <button class="p-1 text-on-surface-variant hover:text-primary transition-colors" @click="togglePhoneEdit">
              <span class="material-symbols-outlined text-sm">edit</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像上传弹窗 -->
    <div v-if="showAvatarDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4 dialog-overlay" @click.self="showAvatarDialog = false">
      <div class="bg-container w-full max-w-md rounded-lg shadow-xl overflow-hidden">
        <div class="px-6 py-4 border-b border-outline-variant flex items-center justify-between">
          <h3 class="font-headline-sm text-headline-sm">修改头像</h3>
          <button class="p-1 hover:bg-surface-container-low rounded-full transition-colors" @click="showAvatarDialog = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-8">
          <div class="border-2 border-dashed border-outline-variant rounded-lg p-12 text-center flex flex-col items-center justify-center gap-4 hover:border-primary transition-colors cursor-pointer bg-surface-container-lowest group" @click="triggerAvatarUpload">
            <span class="material-symbols-outlined text-4xl text-outline group-hover:text-primary">add</span>
            <p class="font-body-md text-on-surface-variant">点击或拖拽图片到此处上传</p>
            <p class="text-xs text-outline">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange">
          <div v-if="avatarPreview" class="mt-6 p-4 border border-outline-variant rounded-lg">
            <p class="text-xs text-on-surface-variant mb-3">预览：</p>
            <div class="w-20 h-20 rounded-full bg-surface-container overflow-hidden mx-auto">
              <img :src="avatarPreview" class="w-full h-full object-cover">
            </div>
          </div>
        </div>
        <div class="px-6 py-4 bg-surface-container-low flex justify-end gap-3">
          <button class="px-4 py-2 text-label-md font-medium text-on-surface-variant hover:bg-surface-container rounded-lg" @click="showAvatarDialog = false">取消</button>
          <button class="px-6 py-2 text-label-md font-medium bg-primary text-white rounded-lg hover:bg-primary-container shadow-sm" :disabled="uploading" @click="uploadAvatar">{{ uploading ? '上传中...' : '开始上传' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Message } from 'element-ui'
import { setUser } from '@/utils/auth'

export default {
  name: 'AccountProfile',
  data() {
    return {
      userInfo: { nickname: '', userId: '', personnelId: '', phone: '', username: '', portrait: '', orgName: '', orgNameFull: '' },
      showSaveNotification: false,
      editingPhone: false,
      saving: false,
      uploading: false,
      showAvatarDialog: false,
      avatarPreview: null,
      avatarFile: null,
      originalPhone: '',
    }
  },
  computed: {
    avatarSrc() {
      if (this.avatarPreview) return this.avatarPreview
      return this.userInfo.portrait || ''
    },
  },
  mounted() {
    this.loadUserInfo()
  },
  methods: {
    loadUserInfo() {
      const cached = window.sessionStorage.getItem('user')
      if (cached) {
        try { this.applyUserInfo(JSON.parse(cached)) } catch (e) { /* ignore */ }
      }
      this.getRequest('/common/user/info').then(resp => {
        if (resp) {
          this.applyUserInfo(resp)
          window.sessionStorage.setItem('user', JSON.stringify(resp))
        }
      }).catch(err => { console.warn('[AccountProfile] loadUserInfo', err) })
    },
    applyUserInfo(u) {
      this.userInfo.nickname = u.nickname || this.userInfo.nickname
      this.userInfo.userId = String(u.id || u.userId || '')
      this.userInfo.personnelId = u.personnelId || ''
      this.userInfo.phone = u.phone || ''
      this.userInfo.username = u.username || ''
      this.userInfo.portrait = u.portrait || ''
      this.userInfo.orgName = u.orgName || ''
      this.userInfo.orgNameFull = u.orgNameFull || ''
      this.originalPhone = u.phone || ''
    },
    togglePhoneEdit() {
      this.editingPhone = !this.editingPhone
      if (this.editingPhone) {
        this.$nextTick(() => {
          if (this.$refs.phoneInput) this.$refs.phoneInput.focus()
        })
      }
    },
    saveChanges() {
      const phone = (this.userInfo.phone || '').trim()
      const reg = /^1[3-9]\d{9}$/
      if (!reg.test(phone)) {
        Message({ type: 'warning', message: '请输入正确的手机号格式（11位，1开头）', offset: 54 })
        return
      }
      this.saving = true
      this.putRequest('/user/updateUserinfo', { id: parseInt(this.userInfo.userId), phone }).then(resp => {
        this.saving = false
        if (resp) {
          this.editingPhone = false
          this.showSaveNotification = false
          this.userInfo.phone = phone
          this.originalPhone = phone
          this.getRequest('/common/user/info').then(r => {
            if (r) window.sessionStorage.setItem('user', JSON.stringify(r))
          })
          this.$emit('updated')
          Message({ type: 'success', message: '资料已更新', offset: 54 })
        }
      }).catch(err => { console.warn('[AccountProfile] saveChanges', err); this.saving = false })
    },
    cancelChanges() {
      this.userInfo.phone = this.originalPhone
      this.editingPhone = false
      this.showSaveNotification = false
    },
    triggerAvatarUpload() {
      this.$refs.avatarInput.click()
    },
    handleAvatarChange(e) {
      const file = e.target.files[0]
      if (file) {
        this.avatarFile = file
        const reader = new FileReader()
        reader.onload = (ev) => { this.avatarPreview = ev.target.result }
        reader.readAsDataURL(file)
      }
    },
    uploadAvatar() {
      if (!this.avatarFile) {
        this.showAvatarDialog = false
        return
      }
      this.uploading = true
      const formData = new FormData()
      formData.append('userId', parseInt(this.userInfo.userId))
      formData.append('file', this.avatarFile)
      this.putRequest('/user/updatePortrait', formData).then(resp => {
        this.uploading = false
        if (resp) {
          this.showAvatarDialog = false
          this.avatarPreview = null
          this.avatarFile = null
          this.getRequest('/common/user/info').then(r => {
            if (r) {
              setUser(r, !!window.localStorage.getItem('user'))
              this.applyUserInfo(r)
              this.$bus && this.$bus.$emit('portraitUpdated')
              this.$emit('updated')
            }
          })
        }
      }).catch(err => { console.warn('[AccountProfile] uploadAvatar', err); this.uploading = false })
    },
  },
}
</script>
