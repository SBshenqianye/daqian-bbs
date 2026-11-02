<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-[1600px] mx-auto px-page-margin-desktop py-6">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">admin_panel_settings</span>
            权限管理
          </h1>
          <p class="text-body-md text-secondary mt-1">
            Phase B/C：权限组只做额外授予（grant-only），不收回角色基线；数据范围仍由角色和版主任职决定。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button class="px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 text-body-sm flex items-center gap-1" @click="newGroup">
            <span class="material-symbols-outlined text-[16px]">add</span>
            新建权限组
          </button>
          <button class="px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant text-body-sm flex items-center gap-1" @click="loadGroups">
            <span class="material-symbols-outlined text-[16px]">refresh</span>
            刷新
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-12 gap-4">
        <!-- 左：权限组列表 -->
        <div class="xl:col-span-3 bg-container border border-border rounded-xl p-card-padding">
          <h3 class="font-title-lg text-title-lg mb-3">权限组</h3>
          <draggable v-model="groups" item-key="id" class="space-y-2" @end="saveOrder">
            <template #item="{ element }">
              <button
                class="w-full text-left px-3 py-2.5 rounded-lg border transition-colors"
                :class="form.id === element.id ? 'border-primary bg-primary-fixed/50' : 'border-outline-variant hover:bg-surface-container-low'"
                @click="selectGroup(element)"
              >
                <div class="flex items-center justify-between">
                  <span class="font-body-md text-on-surface">{{ element.groupName }}</span>
                  <span class="text-[11px] px-2 py-0.5 rounded-full" :class="element.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'">
                    {{ element.enabled ? '启用' : '停用' }}
                  </span>
                </div>
                <div class="text-[12px] text-outline mt-1">{{ element.groupCode }} · {{ element.permissionCount || 0 }} 权限点 · {{ element.userCount || 0 }} 人</div>
              </button>
            </template>
          </draggable>
          <p v-if="groups.length === 0" class="text-body-sm text-on-surface-variant py-6 text-center">暂无权限组</p>
        </div>

        <!-- 中：权限点 -->
        <div class="xl:col-span-5 bg-container border border-border rounded-xl p-card-padding">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-title-lg text-title-lg">权限点</h3>
            <span class="text-body-sm text-outline">已选 {{ selectedCodes.length }} 项</span>
          </div>
          <div class="space-y-4 max-h-[620px] overflow-y-auto pr-1">
            <div v-for="group in permissionSections" :key="group.key">
              <p class="font-label-md text-label-md text-secondary mb-2">{{ group.label }}</p>
              <div v-for="section in group.sections" :key="section.section" class="mb-2">
                <p class="text-[12px] text-outline mb-1">{{ section.section }}</p>
                <label v-for="item in section.items" :key="item.code" class="flex items-start gap-2 py-1.5 cursor-pointer">
                  <input type="checkbox" class="mt-1 accent-primary" :checked="isChecked(item.code)" @change="toggleCode(item.code)" />
                  <span>
                    <span class="block text-body-sm text-on-surface">{{ item.label }}</span>
                    <span class="block font-mono text-[11px] text-outline">{{ item.code }}</span>
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- 右：设置 -->
        <div class="xl:col-span-4 space-y-4">
          <div class="bg-container border border-border rounded-xl p-card-padding">
            <h3 class="font-title-lg text-title-lg mb-3">{{ form.id ? '编辑权限组' : '新建权限组' }}</h3>
            <div class="space-y-3">
              <div>
                <label class="font-label-md text-label-md text-secondary mb-1 block">编码</label>
                <input v-model.trim="form.groupCode" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary outline-none text-body-sm" placeholder="如 content_operator" />
              </div>
              <div>
                <label class="font-label-md text-label-md text-secondary mb-1 block">名称</label>
                <input v-model.trim="form.groupName" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary outline-none text-body-sm" placeholder="如 内容运营" />
              </div>
              <div>
                <label class="font-label-md text-label-md text-secondary mb-1 block">描述</label>
                <textarea v-model="form.description" rows="2" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary outline-none text-body-sm resize-none" />
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <label class="font-label-md text-label-md text-secondary mb-1 block">范围</label>
                  <select v-model="form.scopeType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-sm">
                    <option value="ADMIN">管理端</option>
                    <option value="FRONT">前台</option>
                    <option value="ALL">全部</option>
                  </select>
                </div>
                <div>
                  <label class="font-label-md text-label-md text-secondary mb-1 block">数据范围</label>
                  <select v-model="form.dataScopeType" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-sm">
                    <option value="ROLE_DEFAULT">跟随角色/版主任职</option>
                    <option value="BOARD_MODERATOR">引用版主任职</option>
                  </select>
                </div>
              </div>
              <label class="flex items-center gap-2 text-body-sm text-on-surface">
                <input v-model.number="form.enabled" type="checkbox" :true-value="1" :false-value="0" class="accent-primary" />
                启用
              </label>
              <div class="flex items-center gap-2 pt-1">
                <button class="px-4 py-2 bg-primary text-on-primary rounded-lg text-body-sm hover:opacity-90" @click="saveGroup">{{ form.id ? '保存' : '创建' }}</button>
                <button v-if="form.id" class="px-4 py-2 border border-error text-error rounded-lg text-body-sm hover:bg-error/5" @click="deleteGroup">删除</button>
              </div>
            </div>
          </div>

          <div v-if="form.id" class="bg-container border border-border rounded-xl p-card-padding">
            <h3 class="font-title-lg text-title-lg mb-3">成员</h3>
            <div class="flex items-center gap-2 mb-3">
              <input v-model.trim="assignUserId" class="flex-1 px-3 py-2 bg-surface border border-outline-variant rounded text-body-sm" placeholder="输入用户 ID" />
              <button class="px-3 py-2 bg-primary text-on-primary rounded-lg text-body-sm" @click="assignUser">添加</button>
            </div>
            <div class="space-y-1 max-h-40 overflow-y-auto">
              <div v-for="u in groupUsers" :key="u.userId" class="flex items-center justify-between text-body-sm py-1">
                <span>{{ u.nickname || u.username }} <span class="text-outline">#{{ u.userId }}</span></span>
                <button class="text-error text-[12px]" @click="removeUser(u.userId)">移除</button>
              </div>
              <p v-if="groupUsers.length === 0" class="text-body-sm text-on-surface-variant">暂无成员</p>
            </div>
          </div>

          <div class="bg-container border border-border rounded-xl p-card-padding">
            <h3 class="font-title-lg text-title-lg mb-3">预设方案</h3>
            <div class="flex items-center gap-2 mb-2">
              <input v-model.trim="presetName" class="flex-1 px-3 py-2 bg-surface border border-outline-variant rounded text-body-sm" placeholder="预设名称" />
              <button class="px-3 py-2 bg-surface-variant text-on-surface rounded-lg text-body-sm" :disabled="!form.id" @click="savePreset">保存当前</button>
            </div>
            <div class="space-y-1 max-h-40 overflow-y-auto">
              <div v-for="p in presets" :key="p.id" class="flex items-center justify-between text-body-sm py-1">
                <span>{{ p.presetName }} <span class="text-outline">{{ p.presetCode }}</span></span>
                <button class="text-primary text-[12px]" :disabled="!form.id" @click="applyPreset(p.id)">应用</button>
              </div>
              <p v-if="presets.length === 0" class="text-body-sm text-on-surface-variant">暂无预设</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'

const ADMIN_PERMISSIONS = [
  { section: '基础', items: [
    { code: 'admin:access', label: '进入管理后台' }
  ] },
  { section: '用户', items: [
    { code: 'user:read', label: '查看用户' }, { code: 'user:create', label: '新增用户' },
    { code: 'user:update', label: '编辑用户' }, { code: 'user:delete', label: '删除用户' }
  ] },
  { section: '内容', items: [
    { code: 'article:read', label: '查看帖子' }, { code: 'article:review', label: '审核/违规' },
    { code: 'article:delete', label: '删除帖子' }, { code: 'article:feature', label: '设置精华' },
    { code: 'article:suggestion:adopt', label: '采纳建议' }
  ] },
  { section: '治理', items: [
    { code: 'report:review', label: '举报审核' }, { code: 'appeal:review', label: '申诉审核' },
    { code: 'adopt:review', label: '采纳审批' }, { code: 'featured:recommend', label: '精华推荐' },
    { code: 'featured:review', label: '精华终审' }
  ] },
  { section: '版主/配置', items: [
    { code: 'moderator:manage', label: '版主管理' }, { code: 'moderator:complaint:review', label: '版主投诉' },
    { code: 'config:read', label: '配置只读' }, { code: 'points:read', label: '积分只读' },
    { code: 'points:adjust', label: '积分调整' }, { code: 'dashboard:read', label: '工作台' }
  ] }
]

const FRONT_PERMISSIONS = [
  { section: '发帖/互动', items: [
    { code: 'post:create', label: '发帖' }, { code: 'post:update:own', label: '编辑自己的帖子' },
    { code: 'post:delete:own', label: '删除自己的帖子' },
    { code: 'comment:create', label: '评论' }, { code: 'comment:delete:own', label: '删除自己的评论' },
    { code: 'reply:create', label: '回复' }, { code: 'reply:delete:own', label: '删除自己的回复' }
  ] },
  { section: '其他', items: [
    { code: 'report:submit', label: '提交举报' }, { code: 'appeal:submit', label: '提交申诉' },
    { code: 'moderator:complain', label: '投诉版主' }, { code: 'upload:file', label: '上传文件' },
    { code: 'profile:update', label: '修改资料' }, { code: 'password:update', label: '修改密码' }
  ] }
]

export default {
  name: 'PermissionGroupPage',
  components: { draggable },
  data() {
    return {
      groups: [],
      presets: [],
      form: { id: null, groupCode: '', groupName: '', description: '', scopeType: 'ADMIN', dataScopeType: 'ROLE_DEFAULT', enabled: 1, sortOrder: 0 },
      selectedCodes: [],
      groupUsers: [],
      assignUserId: '',
      presetName: ''
    }
  },
  computed: {
    permissionSections() {
      return [
        { key: 'admin', label: '管理端权限', sections: ADMIN_PERMISSIONS },
        { key: 'front', label: '前台动作权限', sections: FRONT_PERMISSIONS }
      ]
    }
  },
  mounted() {
    this.loadGroups()
    this.loadPresets()
  },
  methods: {
    loadGroups() {
      this.getRequestUrl('/admin/permission/groups').then(resp => {
        if (resp && resp.code === 200) this.groups = resp.obj || []
      })
    },
    loadPresets() {
      this.getRequestUrl('/admin/permission/presets').then(resp => {
        if (resp && resp.code === 200) this.presets = resp.obj || []
      })
    },
    newGroup() {
      this.form = { id: null, groupCode: '', groupName: '', description: '', scopeType: 'ADMIN', dataScopeType: 'ROLE_DEFAULT', enabled: 1, sortOrder: this.groups.length }
      this.selectedCodes = []
      this.groupUsers = []
    },
    selectGroup(group) {
      if (!group || !group.id) return
      this.getRequestUrl(`/admin/permission/groups/${group.id}`).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          const data = resp.obj
          this.form = {
            id: data.id, groupCode: data.groupCode, groupName: data.groupName,
            description: data.description || '', scopeType: data.scopeType || 'ADMIN',
            dataScopeType: data.dataScopeType || 'ROLE_DEFAULT',
            enabled: data.enabled == null ? 1 : data.enabled,
            sortOrder: data.sortOrder == null ? 0 : data.sortOrder
          }
          this.selectedCodes = data.permissionCodes || []
          this.groupUsers = data.users || []
        }
      })
    },
    isChecked(code) {
      return this.selectedCodes.indexOf(code) !== -1
    },
    toggleCode(code) {
      const index = this.selectedCodes.indexOf(code)
      if (index === -1) this.selectedCodes.push(code)
      else this.selectedCodes.splice(index, 1)
    },
    saveGroup() {
      if (!this.form.groupCode || !this.form.groupName) {
        this.$message.warning('编码和名称不能为空')
        return
      }
      const body = Object.assign({}, this.form, { permissionCodes: this.selectedCodes })
      const request = this.form.id
        ? this.putRequest(`/admin/permission/groups/${this.form.id}`, body)
        : this.postRequest('/admin/permission/groups', body)
      request.then(resp => {
        if (resp && resp.code === 200) {
          this.$message.success(this.form.id ? '保存成功' : '创建成功')
          this.loadGroups()
          if (!this.form.id && resp.obj) this.selectGroup({ id: resp.obj })
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      })
    },
    deleteGroup() {
      if (!this.form.id) return
      this.$confirm('确定删除该权限组？用户绑定会一并解除。', '提示', { type: 'warning' }).then(() => {
        this.deleteRequest(`/admin/permission/groups/${this.form.id}`).then(resp => {
          if (resp && resp.code === 200) {
            this.$message.success('已删除')
            this.newGroup()
            this.loadGroups()
          } else if (resp && resp.message) {
            this.$message.error(resp.message)
          }
        })
      }).catch(() => {})
    },
    saveOrder() {
      const tasks = this.groups.map((g, index) => this.putRequest(`/admin/permission/groups/${g.id}`, { sortOrder: index }))
      Promise.all(tasks).then(() => this.loadGroups()).catch(() => {})
    },
    assignUser() {
      const id = Number(this.assignUserId)
      if (!id) { this.$message.warning('请输入用户 ID'); return }
      this.postRequest(`/admin/permission/groups/${this.form.id}/users`, { userIds: [id] }).then(resp => {
        if (resp && resp.code === 200) {
          this.assignUserId = ''
          this.selectGroup({ id: this.form.id })
          this.loadGroups()
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      })
    },
    removeUser(userId) {
      this.deleteRequest(`/admin/permission/groups/${this.form.id}/users/${userId}`).then(resp => {
        if (resp && resp.code === 200) {
          this.selectGroup({ id: this.form.id })
          this.loadGroups()
        }
      })
    },
    savePreset() {
      if (!this.form.id) { this.$message.warning('请先选择权限组'); return }
      this.postRequest('/admin/permission/presets', {
        presetName: this.presetName || this.form.groupName,
        groupId: this.form.id
      }).then(resp => {
        if (resp && resp.code === 200) {
          this.presetName = ''
          this.loadPresets()
          this.$message.success('预设已保存')
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      })
    },
    applyPreset(presetId) {
      if (!this.form.id) { this.$message.warning('请先选择权限组'); return }
      this.$confirm('应用预设会覆盖当前权限组的名称、描述和权限点，是否继续？', '提示', { type: 'warning' }).then(() => {
        this.postRequest(`/admin/permission/presets/${presetId}/apply?groupId=${this.form.id}`, {}).then(resp => {
          if (resp && resp.code === 200) {
            this.$message.success('预设已应用')
            this.selectGroup({ id: this.form.id })
            this.loadGroups()
          } else if (resp && resp.message) {
            this.$message.error(resp.message)
          }
        })
      }).catch(() => {})
    }
  }
}
</script>
