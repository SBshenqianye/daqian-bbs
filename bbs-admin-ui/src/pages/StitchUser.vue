<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">manage_accounts</span>
            用户管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理所有注册用户的信息与权限</p>
        </div>
      </div>

      <!-- Search & Actions Bar -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-error text-on-error rounded-lg hover:opacity-90 transition-all font-label-md text-label-md disabled:opacity-50 disabled:cursor-not-allowed" :disabled="multipleSelection.length === 0" @click="delAllSelection">
            <span class="material-symbols-outlined text-[18px]">delete</span>
            批量删除
          </button>
          <div class="flex-1 min-w-[200px]">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[18px]">search</span>
              <input v-model="searchInfo" class="w-full pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="搜索用户名/姓名" @keyup.enter="handleSearch">
            </div>
          </div>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleSearch">
            <span class="material-symbols-outlined text-[18px]">search</span>
            搜索
          </button>
        </div>
      </div>

      <!-- Users Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left">
                  <input type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded" :checked="isAllSelected" @change="selectAll">
                </th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">ID</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">用户名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">姓名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[130px]">手机号</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">角色</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">状态</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[170px]">注册时间</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[260px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, index) in users" :key="user.id" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4">
                  <input type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded" :checked="isSelected(user)" :disabled="!canCheck(user)" @change="toggleSelect(user)">
                </td>
                <td class="p-4 font-body-md text-on-surface">{{ user.id }}</td>
                <td class="p-4 font-body-md text-on-surface max-w-[160px] truncate" :title="user.username">{{ user.username }}</td>
                <td class="p-4 font-body-md text-on-surface max-w-[160px] truncate" :title="user.nickname">{{ user.nickname }}</td>
                <td class="p-4 font-body-md text-on-surface-variant">{{ user.phone }}</td>
                <td class="p-4">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[12px] font-medium" :class="roleClass(user.userType)">{{ roleLabel(user.userType) }}</span>
                </td>
                <td class="p-4">
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-medium" :class="user.isAlive === 0 ? 'bg-green-50 text-green-700' : 'bg-error-container text-error'">
                    <span class="w-1.5 h-1.5 rounded-full" :class="user.isAlive === 0 ? 'bg-green-500' : 'bg-error'"></span>
                    {{ user.isAlive === 0 ? '活跃' : '禁用' }}
                  </span>
                </td>
                <td class="p-4 font-body-md text-on-surface-variant">{{ user.createTime }}</td>
                <td class="p-4">
                  <div class="flex items-center gap-2" v-if="canShowOperation(user)">
                    <button v-if="currentUserType === 3 && (user.userType == 1 || user.userType == 2)" class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="handleUpdateUserRole(user)">
                      <span class="material-symbols-outlined text-[14px]">swap_horiz</span>
                      {{ user.userType == 1 ? '转管理员' : '转用户' }}
                    </button>
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-warning bg-warning/5 rounded hover:bg-warning/10 transition-colors" @click="handleUpdateAlive(index, user.id)">
                      <span class="material-symbols-outlined text-[14px]">toggle_on</span>
                      状态
                    </button>
                    <button v-if="user.userType != 3" class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-secondary bg-secondary/5 rounded hover:bg-secondary/10 transition-colors" @click="handleOpenOrgDialog(user)">
                      <span class="material-symbols-outlined text-[14px]">corporate_fare</span>
                      单位
                    </button>
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDelete(index, user.id)">
                      <span class="material-symbols-outlined text-[14px]">delete</span>
                      删除
                    </button>
                  </div>
                  <span v-else class="text-on-surface-variant text-body-md">-</span>
                </td>
              </tr>
              <tr v-if="users.length === 0">
                <td colspan="9" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">people</span>
                    <p class="text-body-md">暂无用户数据</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pagination -->
      <div class="mt-4 bg-container border border-border rounded-xl px-6 py-4 flex items-center justify-between">
        <span class="text-body-md text-on-surface-variant">共 {{ total }} 条记录</span>
        <div class="flex items-center gap-3">
          <span class="text-body-md text-on-surface-variant">每页</span>
          <select v-model.number="pageParams.pageSize" class="bg-surface border border-outline-variant rounded px-3 py-1.5 text-body-md text-on-surface outline-none focus:border-primary" @change="handleSizeChange">
            <option :value="15">15</option>
            <option :value="20">20</option>
            <option :value="40">40</option>
            <option :value="60">60</option>
            <option :value="100">100</option>
          </select>
          <span class="text-body-md text-on-surface-variant">条</span>
          <div class="flex items-center gap-1">
            <button class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="pageParams.pageIndex <= 1" @click="pageParams.pageIndex--; getAllUserPage()">
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <span class="px-3 py-1.5 font-body-md text-on-surface">{{ pageParams.pageIndex }} / {{ totalPages }}</span>
            <button class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="pageParams.pageIndex >= totalPages" @click="pageParams.pageIndex++; getAllUserPage()">
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Org Dialog -->
      <div v-if="orgDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="orgDialogVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">corporate_fare</span>
              修改单位
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="orgDialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5">
            <div class="relative mb-4">
              <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[18px]">filter_list</span>
              <input v-model="orgFilterText" class="w-full pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="输入关键字筛选单位">
            </div>
            <div class="max-h-80 overflow-y-auto border border-outline-variant rounded-lg bg-surface">
              <div v-if="orgTreeData.length === 0" class="p-8 text-center text-on-surface-variant flex flex-col items-center gap-2">
                <span class="material-symbols-outlined text-[36px] opacity-30">account_tree</span>
                <p class="text-body-md">暂无组织数据</p>
              </div>
              <div v-else class="p-2">
                <div v-for="org in filteredOrgTree" :key="org.id">
                  <div class="flex items-center gap-2 px-3 py-2 rounded cursor-pointer hover:bg-surface-container-low" :class="{ 'bg-primary/5 border border-primary/20': orgCheckValue.id === org.id }" @click="orgCheckValue = { id: org.id, label: org.label }">
                    <span class="material-symbols-outlined text-outline text-[18px]">folder</span>
                    <span class="font-body-md" :class="orgCheckValue.id === org.id ? 'text-primary font-semibold' : 'text-on-surface'">{{ org.label }}</span>
                  </div>
                  <div v-if="org.children && org.children.length" class="ml-6 border-l border-outline-variant/30 pl-2">
                    <div v-for="child in org.children" :key="child.id" class="flex items-center gap-2 px-3 py-2 rounded cursor-pointer hover:bg-surface-container-low" :class="{ 'bg-primary/5 border border-primary/20': orgCheckValue.id === child.id }" @click="orgCheckValue = { id: child.id, label: child.label }">
                      <span class="material-symbols-outlined text-outline text-[18px]">folder_open</span>
                      <span class="font-body-md" :class="orgCheckValue.id === child.id ? 'text-primary font-semibold' : 'text-on-surface'">{{ child.label }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="orgDialogVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="handleConfirmUpdateOrg">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StitchUser',
  data() {
    return {
      searchInfo: '',
      users: [],
      multipleSelection: [],
      pageParams: { pageIndex: 1, pageSize: 15 },
      total: 0,
      orgDialogVisible: false,
      orgTreeData: [],
      orgCheckValue: { label: '请选择单位', id: '' },
      currentEditUserId: null,
      orgFilterText: ''
    }
  },
  computed: {
    currentUserType() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (!admin) return 0
        return JSON.parse(admin).userType != null ? Number(JSON.parse(admin).userType) : 0
      } catch (e) { return 0 }
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageParams.pageSize))
    },
    isAllSelected() {
      const checkable = this.users.filter(u => this.canCheck(u))
      return checkable.length > 0 && checkable.every(u => this.isSelected(u))
    },
    filteredOrgTree() {
      if (!this.orgFilterText) return this.orgTreeData
      const filter = (nodes) => {
        return nodes.map(n => {
          const match = n.label.toLowerCase().includes(this.orgFilterText.toLowerCase())
          const children = n.children ? filter(n.children) : []
          if (match || children.length > 0) return { ...n, children }
          return null
        }).filter(Boolean)
      }
      return filter(this.orgTreeData)
    }
  },
  watch: {
    orgFilterText() {
      // Filter is computed, no need for $nextTick
    }
  },
  mounted() {
    this.getAllUserPage()
  },
  methods: {
    canCheck(row) {
      if (row.userType == 3) return false
      if (row.userType == 2) return this.currentUserType == 3
      return true
    },
    canShowOperation(row) {
      if (row.userType == 3) return false
      if (row.userType == 2) return this.currentUserType == 3
      return true
    },
    isSelected(user) {
      return this.multipleSelection.some(s => s.id === user.id)
    },
    toggleSelect(user) {
      if (!this.canCheck(user)) return
      const idx = this.multipleSelection.findIndex(s => s.id === user.id)
      if (idx >= 0) this.multipleSelection.splice(idx, 1)
      else this.multipleSelection.push(user)
    },
    selectAll() {
      if (this.isAllSelected) {
        this.multipleSelection = []
      } else {
        this.multipleSelection = this.users.filter(u => this.canCheck(u))
      }
    },
    roleClass(type) {
      if (type == 1) return 'bg-blue-50 text-blue-700'
      if (type == 2) return 'bg-purple-50 text-purple-700'
      if (type == 3) return 'bg-amber-50 text-amber-700'
      return 'bg-surface-variant text-on-surface-variant'
    },
    roleLabel(type) {
      if (type == 1) return '用户'
      if (type == 2) return '管理员'
      if (type == 3) return '超级管理员'
      return '-'
    },
    getAllUserPage() {
      const params = {
        pageIndex: this.pageParams.pageIndex,
        pageSize: this.pageParams.pageSize,
        searchInfo: this.searchInfo
      }
      this.getRequest('/getAllUser', JSON.stringify(params)).then(resp => {
        if (resp) {
          this.total = resp.obj.total
          this.users = resp.obj.list
        }
      })
    },
    handleSearch() {
      this.pageParams.pageIndex = 1
      this.getAllUserPage()
    },
    handleSizeChange() {
      this.pageParams.pageIndex = 1
      this.getAllUserPage()
    },
    loadOrgTree() {
      if (!this.orgTreeData.length) {
        return this.getRequestUrl('/common/saOrgTree').then(resp => {
          this.orgTreeData = resp && resp.obj ? this.normalizeOrgTree(resp.obj) : []
        }).catch(() => { this.orgTreeData = [] })
      }
      return Promise.resolve()
    },
    normalizeOrgTree(nodes) {
      if (!nodes || !Array.isArray(nodes)) return []
      return nodes.map(node => ({
        id: node.orgNo != null ? node.orgNo : node.id,
        label: node.orgName != null ? node.orgName : node.label,
        children: node.children && node.children.length ? this.normalizeOrgTree(node.children) : undefined
      }))
    },
    handleOpenOrgDialog(row) {
      this.currentEditUserId = row.id
      this.orgCheckValue = { id: row.orgNo || '', label: row.orgName || '请选择单位' }
      this.orgFilterText = ''
      this.loadOrgTree()
      this.orgDialogVisible = true
    },
    handleConfirmUpdateOrg() {
      if (!this.currentEditUserId) {
        this.$message.error('用户信息有误，请刷新后重试')
        return
      }
      if (!this.orgCheckValue || !this.orgCheckValue.id) {
        this.$message.warning('请选择单位')
        return
      }
      this.$confirm('确定要修改该用户单位吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/user/modOrgNo', { id: this.currentEditUserId, orgNo: this.orgCheckValue.id }).then(resp => {
          if (resp) {
            this.$message.success('修改单位成功')
            this.orgDialogVisible = false
            this.currentEditUserId = null
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    },
    handleDelete(index, userId) {
      this.$confirm('确定要删除吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/deleteUserByUserId', { userId }).then(resp => {
          if (resp) {
            this.$message.success('删除成功')
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    },
    handleUpdateUserRole(row) {
      const roleType = row.userType == 1 ? '02' : '01'
      const tip = row.userType == 1 ? '确定将该用户转为管理员吗？' : '确定将该用户转为普通用户吗？'
      this.$confirm(tip, '提示', { type: 'warning' }).then(() => {
        this.postRequest('/updateUserRole', { userId: row.id, roleType }).then(resp => {
          if (resp) {
            this.$message.success('修改成功')
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    },
    handleUpdateAlive(index, userId) {
      this.$confirm('确定要修改状态吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/updateUserAliveByUserId', { userId }).then(resp => {
          if (resp) {
            this.$message.success('修改成功')
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    },
    delAllSelection() {
      const users = this.multipleSelection
      if (!users || users.length === 0) {
        this.$message.warning('请先选择需要删除的用户')
        return
      }
      const userIds = users.map(u => u.id).join(',')
      this.$confirm('确定要删除选中的用户吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/batchDeleteUsersByUserIds', { userIds }).then(resp => {
          if (resp) {
            this.$message.success('修改成功')
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    }
  }
}
</script>
