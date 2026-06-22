<template>
  <main class="min-h-screen flex items-center justify-center p-4 bg-background">
    <div class="bg-container login-box rounded-lg border border-outline-variant shadow-sm overflow-hidden form-transition">
      <!-- Tabs -->
      <div class="flex border-b border-outline-variant p-4 gap-2 bg-surface-container-low/30">
        <button
          class="flex-1 py-2.5 rounded font-headline-sm text-headline-sm transition-all duration-300"
          :class="activeTab === 'login' ? 'bg-primary text-white shadow-[0_4px_12px_rgba(0,82,217,0.25)]' : 'text-on-surface-variant bg-surface-container-low hover:bg-surface-variant'"
          @click="switchTab('login')"
        >
          登录
        </button>
        <button
          class="flex-1 py-2.5 rounded font-headline-sm text-headline-sm transition-all duration-300"
          :class="activeTab === 'signup' ? 'bg-primary text-white shadow-[0_4px_12px_rgba(0,82,217,0.25)]' : 'text-on-surface-variant bg-surface-container-low hover:bg-surface-variant'"
          @click="switchTab('signup')"
        >
          注册
        </button>
      </div>

      <!-- Forms Container with Sliding logic -->
      <div class="overflow-hidden">
        <div class="forms-slider" :style="{ marginLeft: activeTab === 'login' ? '0%' : '-100%' }">
          <!-- ===== Login Section ===== -->
          <div class="form-section px-8 pt-8 pb-10">
            <div class="pb-6 text-center">
              <h1 class="font-headline-lg text-headline-lg text-on-surface">登录</h1>
            </div>
            <form class="space-y-5" @submit.prevent="handleLogin">
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">账号</label>
                <div class="relative">
                  <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[20px]">person</span>
                  <input
                    v-model="loginForm.username"
                    class="w-full pl-10 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                    placeholder="请输入账号"
                    type="text"
                  >
                </div>
              </div>
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">密码</label>
                <div class="relative">
                  <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[20px]">lock</span>
                  <input
                    v-model="loginForm.password"
                    class="w-full pl-10 pr-10 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                    placeholder="请输入密码"
                    :type="showLoginPassword ? 'text' : 'password'"
                  >
                  <button class="absolute right-3 top-1/2 -translate-y-1/2 text-outline hover:text-primary" type="button" @click="showLoginPassword = !showLoginPassword">
                    <span class="material-symbols-outlined text-[20px]">{{ showLoginPassword ? 'visibility_off' : 'visibility' }}</span>
                  </button>
                </div>
              </div>
              <div class="flex items-center justify-between py-1">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="loginForm.remember" class="w-4 h-4 text-primary border-outline-variant rounded focus:ring-primary" type="checkbox">
                  <span class="font-label-md text-label-md text-on-surface-variant">记住我</span>
                </label>
                <a class="font-label-md text-label-md text-primary hover:opacity-80 transition-opacity cursor-pointer">忘记密码？</a>
              </div>
              <button class="w-full py-3 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all active:scale-[0.98] shadow-sm" type="submit">
                登录
              </button>
              <p class="text-center font-body-md text-body-md text-on-surface-variant pt-2">
                没有账号？ <button class="text-primary font-semibold hover:opacity-80" type="button" @click="switchTab('signup')">立即注册</button>
              </p>
            </form>
          </div>

          <!-- ===== Signup Section ===== -->
          <div class="form-section px-8 pt-8 pb-10">
            <div class="pb-6 text-center">
              <h1 class="font-headline-lg text-headline-lg text-on-surface">注册</h1>
            </div>
            <form class="space-y-4" @submit.prevent="handleSignup">
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">账号</label>
                <input
                  v-model="signupForm.username"
                  class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                  placeholder="请输入账号"
                  type="text"
                >
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div class="space-y-1.5">
                  <label class="font-label-md text-label-md text-secondary ml-0.5">密码</label>
                  <input
                    v-model="signupForm.password"
                    class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                    placeholder="请输入密码"
                    type="password"
                  >
                </div>
                <div class="space-y-1.5">
                  <label class="font-label-md text-label-md text-secondary ml-0.5">确认密码</label>
                  <input
                    v-model="signupForm.confirmPassword"
                    class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                    placeholder="请再次输入"
                    type="password"
                  >
                </div>
              </div>
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">姓名</label>
                <input
                  v-model="signupForm.name"
                  class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                  placeholder="请输入真实姓名"
                  type="text"
                >
              </div>
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">电话</label>
                <input
                  v-model="signupForm.phone"
                  class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                  placeholder="请输入常用联系电话"
                  type="tel"
                >
              </div>
              <div class="space-y-1.5">
                <label class="font-label-md text-label-md text-secondary ml-0.5">所属单位</label>
                <div class="relative cursor-pointer" @click="showOrgModal = true">
                  <input
                    v-model="signupForm.org"
                    class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded cursor-pointer focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md"
                    placeholder="请选择单位"
                    readonly
                    type="text"
                  >
                  <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-outline">corporate_fare</span>
                </div>
              </div>
              <button class="w-full py-3 bg-primary text-on-primary font-headline-sm text-headline-sm rounded hover:opacity-90 transition-all active:scale-[0.98] shadow-sm mt-4" type="submit">
                注册
              </button>
              <p class="text-center font-body-md text-body-md text-on-surface-variant pt-2">
                已有账号？ <button class="text-primary font-semibold hover:opacity-80" type="button" @click="switchTab('login')">返回登录</button>
              </p>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== Org Selection Modal ===== -->
    <div
      v-if="showOrgModal"
      class="fixed inset-0 z-[100] modal-overlay flex items-center justify-center p-4"
      @click.self="showOrgModal = false"
      @keydown.escape="showOrgModal = false"
    >
      <div class="bg-container w-full max-w-2xl rounded-lg shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between p-5 border-b border-outline-variant">
          <h3 class="font-headline-sm text-headline-sm text-on-surface">选择供电单位</h3>
          <button class="text-outline hover:text-error transition-colors" @click="showOrgModal = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-5">
          <div class="mb-4 bg-surface rounded border border-outline-variant px-4 py-2 flex items-center">
            <span class="material-symbols-outlined text-outline mr-2">filter_list</span>
            <input v-model="orgFilter" class="bg-transparent border-none focus:ring-0 w-full text-body-md outline-none" placeholder="输入关键字进行过滤" type="text">
          </div>
          <div class="max-h-96 overflow-y-auto space-y-2 py-2">
            <div
              v-for="org in displayOrgTree"
              :key="org.name"
            >
              <div
                class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer transition-all"
                :class="signupForm.orgNo === org.id ? 'bg-primary-container/20 border border-primary/30' : 'hover:bg-surface-container-low'"
              >
                <span class="material-symbols-outlined text-outline text-[18px] cursor-pointer select-none" @click.stop="org.expanded = !org.expanded">
                  {{ org.expanded ? 'expand_more' : 'chevron_right' }}
                </span>
                <span class="material-symbols-outlined text-[18px] cursor-pointer select-none" :class="signupForm.orgNo === org.id ? 'text-primary' : 'text-outline'" @click="selectOrg(org.name, org.id)">
                  {{ signupForm.orgNo === org.id ? 'radio_button_checked' : 'radio_button_unchecked' }}
                </span>
                <span class="text-body-md font-body-md flex-1 cursor-pointer select-none" :class="signupForm.orgNo === org.id ? 'font-bold text-primary' : 'text-on-surface'" @click="selectOrg(org.name, org.id)">{{ org.name }}</span>
              </div>
              <div v-if="org.expanded && org.children" class="pl-6 space-y-2 border-l-2 border-outline-variant/20 ml-4">
                <div
                  v-for="child in org.children"
                  :key="child.name"
                >
                  <div
                    class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer transition-all"
                    :class="signupForm.orgNo === child.id ? 'bg-primary-container/20 border border-primary/30' : (child.expanded ? 'bg-primary-container/10 border border-primary/20' : 'hover:bg-surface-container-low')"
                  >
                    <span class="material-symbols-outlined text-[18px] cursor-pointer select-none" :class="child.expanded ? 'text-primary' : 'text-outline'" @click.stop="child.expanded = !child.expanded">
                      {{ child.expanded ? 'expand_more' : 'chevron_right' }}
                    </span>
                    <span class="material-symbols-outlined text-[18px] cursor-pointer select-none" :class="signupForm.orgNo === child.id ? 'text-primary' : 'text-outline'" @click="selectOrg(child.name, child.id)">
                      {{ signupForm.orgNo === child.id ? 'radio_button_checked' : 'radio_button_unchecked' }}
                    </span>
                    <span class="text-body-md flex-1 cursor-pointer select-none" :class="signupForm.orgNo === child.id ? 'font-bold text-primary' : (child.expanded ? 'font-bold text-primary' : 'text-on-surface')" @click="selectOrg(child.name, child.id)">{{ child.name }}</span>
                  </div>
                  <div v-if="child.expanded && child.children" class="pl-6 space-y-1 border-l-2 border-outline-variant/20 ml-4">
                    <div
                      v-for="sub in child.children"
                      :key="sub.name"
                      class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer transition-all"
                      :class="signupForm.orgNo === sub.id ? 'bg-primary-container/20 border border-primary/30' : 'hover:bg-surface-container'"
                      @click="selectOrg(sub.name, sub.id)"
                    >
                      <span class="material-symbols-outlined text-[18px]" :class="signupForm.orgNo === sub.id ? 'text-primary' : 'text-outline'">
                        {{ signupForm.orgNo === sub.id ? 'radio_button_checked' : 'radio_button_unchecked' }}
                      </span>
                      <span class="text-body-md" :class="signupForm.orgNo === sub.id ? 'font-medium text-primary' : 'text-on-surface-variant'">{{ sub.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="p-5 border-t border-outline-variant flex justify-end gap-3 bg-surface-container-lowest">
          <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="showOrgModal = false">取消</button>
          <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="showOrgModal = false">确认选择</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'

export default {
  name: 'BBSLogin',
  data() {
    return {
      activeTab: 'login',
      showLoginPassword: false,
      showOrgModal: false,
      orgFilter: '',
      loginLoading: false,
      signupLoading: false,
      loginForm: {
        username: '',
        password: '',
        remember: false,
      },
      signupForm: {
        username: '',
        password: '',
        confirmPassword: '',
        name: '',
        phone: '',
        org: '',
        orgNo: '',
      },
      orgTree: [],
      orgTreeLoading: false,
    }
  },
  computed: {
    displayOrgTree() {
      if (!this.orgFilter) return this.orgTree
      const filter = (nodes) => {
        return nodes.map(n => {
          const match = n.name.toLowerCase().includes(this.orgFilter.toLowerCase())
          const children = n.children ? filter(n.children) : []
          if (match || children.length > 0) {
            return { ...n, expanded: true, children }
          }
          return null
        }).filter(Boolean)
      }
      return filter(this.orgTree)
    },
  },
  mounted() {
    this.loadOrgTree()
  },
  methods: {
    switchTab(tab) {
      this.activeTab = tab
    },
    selectOrg(name, id) {
      this.signupForm.org = name
      this.signupForm.orgNo = id
      this.showOrgModal = false
    },
    loadOrgTree() {
      this.orgTreeLoading = true
      this.getRequest('/common/saOrgTree').then(resp => {
        this.orgTreeLoading = false
        const data = resp && resp.obj ? resp.obj : (Array.isArray(resp) ? resp : [])
        this.orgTree = this.transformOrgTree(data)
      }).catch(() => {
        this.orgTreeLoading = false
        this.orgTree = []
      })
    },
    transformOrgTree(nodes) {
      return nodes.map(n => ({
        name: n.label || n.orgName || n.name || '',
        id: n.id || n.orgNo || '',
        expanded: false,
        checked: false,
        children: n.children && n.children.length > 0 ? this.transformOrgTree(n.children) : [],
      }))
    },
    handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        Message({ type: 'error', message: '账号或密码不能为空！', offset: 54 })
        return
      }
      this.loginLoading = true
      this.postRequest('/common/login', { ...this.loginForm, channel: '01' }).then(resp => {
        this.loginLoading = false
        if (resp) {
          const tokenStr = resp.obj.tokenHead + resp.obj.token
          window.sessionStorage.setItem('tokenStr', tokenStr)
          this.$bus.$emit('isLogin', true)
          // Fetch user info
          this.getRequest('/common/user/info').then(userResp => {
            if (userResp) {
              window.sessionStorage.setItem('user', JSON.stringify(userResp))
            }
          })
          const redirect = this.$route.query.redirect || '/forum'
          this.$router.replace(redirect)
          setTimeout(() => { location.reload() }, 600)
        }
      }).catch(() => {
        this.loginLoading = false
      })
    },
    handleSignup() {
      const phoneRegex = /^1[3-9]\d{9}$/
      if (!this.signupForm.username || !this.signupForm.password) {
        Message({ type: 'error', message: '账号或密码不能为空！', offset: 54 })
        return
      }
      if (this.signupForm.confirmPassword !== this.signupForm.password) {
        Message({ type: 'error', message: '两次输入的密码不一致！', offset: 54 })
        return
      }
      if (!this.signupForm.name) {
        Message({ type: 'error', message: '姓名不能为空！', offset: 54 })
        return
      }
      if (!this.signupForm.phone) {
        Message({ type: 'error', message: '电话不能为空！', offset: 54 })
        return
      }
      if (!phoneRegex.test(this.signupForm.phone)) {
        Message({ type: 'error', message: '请输入正确的手机号！', offset: 54 })
        return
      }
      if (!this.signupForm.orgNo) {
        Message({ type: 'error', message: '单位不能为空，请选择单位！', offset: 54 })
        return
      }
      this.signupLoading = true
      const nickname = `${this.signupForm.org}-${this.signupForm.name.trim()}`
      this.postRequest('/common/register', {
        username: this.signupForm.username,
        password: this.signupForm.password,
        rePassword: this.signupForm.confirmPassword,
        phone: this.signupForm.phone,
        orgNo: this.signupForm.orgNo,
        nickname,
      }).then(resp => {
        this.signupLoading = false
        if (resp) {
          Message({ type: 'success', message: '注册成功，请登录！', offset: 54 })
          this.loginForm.username = this.signupForm.username
          this.loginForm.password = this.signupForm.password
          this.switchTab('login')
        }
      }).catch(() => {
        this.signupLoading = false
      })
    },
  },
}
</script>
