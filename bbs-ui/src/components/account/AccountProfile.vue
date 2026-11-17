<template>
  <div class="space-y-5">
    <!-- 头像 + 昵称 -->
    <div class="flex items-center gap-4 pb-5 border-b border-outline-variant">
      <div class="relative group tooltip cursor-pointer shrink-0" data-tooltip="点击上传头像" @click="showAvatarDialog = true">
        <img
          alt="用户头像"
          class="w-[72px] h-[72px] rounded-full border-4 border-surface-container-high object-cover transition-opacity group-hover:opacity-80"
          :src="avatarSrc || require('@/assets/portrait.png')"
        >
        <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <span class="material-symbols-outlined text-white text-2xl">upload</span>
        </div>
      </div>
      <div class="min-w-0">
        <h1 class="text-[19px] font-semibold text-on-surface truncate">{{ userInfo.nickname || '未设置昵称' }}</h1>
        <p v-if="userInfo.orgNameFull || userInfo.orgName" class="text-[11.5px] text-on-surface-variant mt-0.5 truncate">
          <span class="material-symbols-outlined text-[13px] align-[-2px] mr-0.5 text-outline">apartment</span>{{ userInfo.orgNameFull || userInfo.orgName }}
        </p>
        <p v-else-if="userInfo.username" class="text-[11.5px] text-on-surface-variant mt-0.5 truncate">{{ userInfo.username }}</p>
        <p class="text-[11px] text-outline mt-1">点击头像可更换</p>
      </div>
    </div>

    <!-- 保存提示 -->
    <div v-if="showSaveNotification" class="flex items-center justify-between gap-3 p-3.5 bg-primary/5 border border-primary/20 rounded-xl">
      <div class="flex items-center gap-2 min-w-0">
        <span class="material-symbols-outlined text-primary text-[18px]">info</span>
        <span class="text-[12.5px] text-on-surface truncate">资料有更新，记得保存</span>
      </div>
      <div class="flex gap-2 shrink-0">
        <button class="ui-btn ui-btn-xs ui-btn-primary" :disabled="saving" @click="saveChanges">{{ saving ? '保存中...' : '保存' }}</button>
        <button class="ui-btn ui-btn-xs text-error hover:bg-error/10" @click="cancelChanges">取消</button>
      </div>
    </div>

    <!-- 基本信息 -->
    <div>
      <h3 class="ui-section-title mb-2">
        <span class="material-symbols-outlined text-[16px] text-primary">badge</span>基本信息
      </h3>
      <div class="grid grid-cols-1 max-w-2xl">
        <div class="flex items-center py-3 border-b border-outline-variant/70">
          <label class="w-24 shrink-0 text-[11.5px] text-on-surface-variant">用户昵称</label>
          <div class="text-[13px] text-on-surface truncate">{{ userInfo.nickname }}</div>
        </div>
        <div class="flex items-center py-3 border-b border-outline-variant/70">
          <label class="w-24 shrink-0 text-[11.5px] text-on-surface-variant">人员编号</label>
          <div class="text-[13px] text-on-surface tabnum">{{ userInfo.personnelId }}</div>
        </div>
        <div class="flex items-center py-3 border-b border-outline-variant/70 group cursor-default relative">
          <label class="w-24 shrink-0 text-[11.5px] text-on-surface-variant">手机号码</label>
          <div class="flex-1 flex items-center justify-between gap-2 min-w-0">
            <div v-show="!editingPhone" class="text-[13px] text-on-surface tabnum">{{ userInfo.phone }}</div>
            <input
              v-show="editingPhone"
              ref="phoneInput"
              v-model="userInfo.phone"
              class="flex-1 px-2.5 py-1 border border-outline-variant rounded-lg text-[13px] focus:border-primary/40 outline-none"
              type="text"
              @input="showSaveNotification = true"
            >
            <button class="ui-icon-btn" title="修改手机号" @click="togglePhoneEdit">
              <span class="material-symbols-outlined">edit</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像上传弹窗 -->
    <div v-if="showAvatarDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4 dialog-overlay" @click.self="showAvatarDialog = false">
      <div class="bg-container w-full max-w-md rounded-xl shadow-xl overflow-hidden">
        <div class="px-5 py-3.5 border-b border-outline-variant flex items-center justify-between">
          <h3 class="text-[15px] font-semibold">修改头像</h3>
          <button class="ui-icon-btn" @click="showAvatarDialog = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5">
          <div class="border-2 border-dashed border-outline-variant rounded-xl p-8 text-center flex flex-col items-center justify-center gap-3 hover:border-primary transition-colors cursor-pointer bg-surface-container-lowest group" @click="triggerAvatarUpload">
            <span class="material-symbols-outlined text-[32px] text-outline group-hover:text-primary">add_photo_alternate</span>
            <p class="text-[12.5px] text-on-surface-variant">点击或拖拽图片到此处上传</p>
            <p class="text-[11px] text-outline">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange">
          <div v-if="avatarPreview" class="mt-4 p-3 border border-outline-variant rounded-xl">
            <p class="text-[11px] text-on-surface-variant mb-2">预览：</p>
            <div class="w-16 h-16 rounded-full bg-surface-container overflow-hidden mx-auto">
              <img :src="avatarPreview" class="w-full h-full object-cover">
            </div>
          </div>
        </div>
        <div class="px-5 py-3 bg-surface-container-low flex justify-end gap-2">
          <button class="ui-btn ui-btn-sm ui-btn-secondary" @click="showAvatarDialog = false">取消</button>
          <button class="ui-btn ui-btn-sm ui-btn-primary" :disabled="uploading" @click="uploadAvatar">{{ uploading ? '上传中...' : '开始上传' }}</button>
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
