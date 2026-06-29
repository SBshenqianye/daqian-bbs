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
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="importStore.status !== 'idle'"
            @click="triggerImport"
          >
            <span class="material-symbols-outlined text-[18px]">upload_file</span>
            {{
              importButtonLabel
            }}
          </button>
          <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileImport">
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md"
            @click="handleOpenAddDialog"
          >
            <span class="material-symbols-outlined text-[18px]">person_add</span>
            添加用户
          </button>
          <div class="flex-1 min-w-[200px]">
            <div class="grid grid-cols-1 grid-rows-1">
              <input v-model="searchInfo" class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="搜索用户名/姓名/人员编号" @keyup.enter="handleSearch">
              <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">search</span>
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
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">操作</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">人员编号</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">用户名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">姓名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[130px]">手机号</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">角色</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">状态</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[170px]">注册时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, index) in users" :key="user.id" class="border-b border-border hover:bg-surface-container-low/50 transition-colors" @mouseenter="showUserTooltip(user, $event)" @mousemove="updateTooltipPosition($event)" @mouseleave="hideUserTooltip">
                <td class="p-4">
                  <input type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded" :checked="isSelected(user)" :disabled="!canCheck(user)" @change="toggleSelect(user)">
                </td>
                <td class="p-4">
                  <div v-if="canShowOperation(user)">
                    <button class="inline-flex items-center justify-center w-8 h-8 rounded-full text-info bg-info/5 hover:bg-info/15 hover:text-info transition-colors" title="编辑" @click="handleOpenEditDialog(user)">
                      <span class="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                  </div>
                  <span v-else class="text-on-surface-variant text-body-md">-</span>
                </td>
                <td class="p-4 font-body-md text-on-surface">
                  <span class="text-on-surface">{{ user.personnelId }}</span>
                </td>
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



      <!-- Edit User Dialog (含内嵌单位树) -->
      <div v-show="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="editDialogVisible = false"></div>
        <div ref="dialogEdit" class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl flex flex-col max-h-[85vh]" style="transform:translate3d(0,0,0);transition:none">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant" @mousedown.prevent="startDrag($event, 'edit')" style="user-select:none">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">edit</span>
              编辑用户
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="editDialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4 overflow-y-auto flex-1">
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">人员编号</label>
                <input class="w-full px-3 py-2 bg-surface-variant border border-outline-variant rounded text-body-md text-on-surface-variant" :value="editUser.personnelId" readonly disabled>
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">用户名</label>
                <input class="w-full px-3 py-2 bg-surface-variant border border-outline-variant rounded text-body-md text-on-surface-variant" :value="editUser.username" readonly disabled>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">姓名</label>
                <input v-model="editForm.nickname" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入姓名">
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">手机号</label>
                <input v-model="editForm.phone" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入手机号">
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">身份证号</label>
                <input class="w-full px-3 py-2 bg-surface-variant border border-outline-variant rounded text-body-md text-on-surface-variant" :value="editUser.idCard" readonly disabled>
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">所属单位</label>
                <button class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md text-left flex items-center justify-between hover:border-primary transition-colors" @click="orgPickerVisible = true" type="button">
                  <span :class="editForm.orgName ? 'text-on-surface' : 'text-on-surface-variant'">{{ editForm.orgName || '请选择单位' }}</span>
                  <span class="material-symbols-outlined text-[18px] text-outline">search</span>
                </button>
              </div>
            </div>
            <div class="flex items-center gap-6">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">角色</label>
                <select v-model="editForm.userType" class="px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary outline-none">
                  <option value="1">普通用户</option>
                  <option value="2">管理员</option>
                </select>
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">状态</label>
                <select v-model="editForm.isAlive" class="px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary outline-none">
                  <option :value="0">活跃</option>
                  <option :value="1">禁用</option>
                </select>
              </div>
              <div class="space-y-1 pt-5">
                <button class="px-4 py-2 border border-warning text-warning rounded hover:bg-warning/5 transition-all text-label-md" @click="handleResetPassword">
                  <span class="material-symbols-outlined text-[16px] align-middle">lock_reset</span>
                  重置密码
                </button>
              </div>
            </div>
          </div>
          <div class="flex justify-between gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="inline-flex items-center gap-1 px-4 py-2 border border-error text-error rounded hover:bg-error/5 transition-all font-label-md text-label-md" @click="handleDeleteFromEdit">
              <span class="material-symbols-outlined text-[16px]">delete</span>
              删除用户
            </button>
            <div class="flex gap-3">
              <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="editDialogVisible = false">取消</button>
              <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" :disabled="editSaving" @click="handleSaveEdit">
                {{ editSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Org Tree Picker Component -->
      <OrgTreePicker
        :visible="orgPickerVisible"
        :selected-id="editForm.orgNo"
        :selected-label="editForm.orgName"
        @select="onOrgPickerSelect"
        @close="orgPickerVisible = false"
      />

      <!-- Import Preview Dialog -->
      <div v-show="importPreviewVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="importPreviewVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div ref="dialogPreview" class="relative bg-container w-full max-w-5xl rounded-xl shadow-2xl overflow-hidden" style="transform:translate3d(0,0,0);transition:none">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant" @mousedown.prevent="startDrag($event, 'preview')" style="user-select:none">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">preview</span>
              导入预览
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="importPreviewVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 max-h-96 overflow-y-auto">
            <p class="text-body-md text-on-surface-variant mb-4">共 {{ importPreviewData.totalCount }} 条数据，请确认以下账号信息，多音字可手动修正：</p>

            <!-- missing keys warning -->
            <div v-if="missingKeyRows.length" class="mb-4 px-4 py-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-2">
              <span class="material-symbols-outlined text-amber-600" style="font-size: 20px;">warning</span>
              <div>
                <p class="text-body-md text-amber-800 font-medium">以下 {{ missingKeyRows.length }} 行缺少人员编号和身份证号</p>
                <p class="text-body-md text-amber-600">行号：{{ missingKeyRows.join(', ') }}</p>
                <p class="text-body-md text-amber-600">导入时这些行将被跳过，不阻碍其他数据导入</p>
              </div>
            </div>

            <table class="w-full text-left border-collapse whitespace-nowrap" v-if="importPreviewData.users && importPreviewData.users.length">
              <thead>
                <tr class="bg-surface-container-low border-b border-border">
                  <th class="p-2 text-label-md text-on-surface-variant w-14">姓名</th>
                  <th class="p-2 text-label-md text-on-surface-variant w-20">人员编号</th>
                  <th class="p-2 text-label-md text-on-surface-variant w-24">身份证号</th>
                  <th class="p-2 text-label-md text-on-surface-variant">账号（可编辑）</th>
                  <th class="p-2 text-label-md text-on-surface-variant min-w-[120px]">单位</th>
                  <th class="p-2 text-label-md text-on-surface-variant w-14">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in importPreviewData.users" :key="user.rowNum" class="border-b border-border"
                  :class="{ 'bg-amber-50/50': !user.personnelId && !user.idCard }">
                  <td class="p-2 font-body-md">{{ user.nickname }}</td>
                  <td class="p-2 font-body-md text-on-surface-variant">{{ user.personnelId || '-' }}</td>
                  <td class="p-2 font-body-md text-on-surface-variant">{{ user.idCard || '-' }}</td>
                  <td class="p-2">
                    <input v-model="user.username" class="w-full px-2 py-1 border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" :class="{ 'bg-warning/10 border-warning': getDefaultUsername(user) !== user.username }" :disabled="!user.personnelId && !user.idCard">
                  </td>
                  <td class="p-2 font-body-md text-on-surface-variant max-w-[180px] truncate" :title="orgTitle(user)">{{ user.orgName }}</td>
                  <td class="p-2">
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-[12px] font-medium whitespace-nowrap"
                      :class="!user.personnelId && !user.idCard ? 'bg-amber-50 text-amber-700' : (user.action === 'new' ? 'bg-green-50 text-green-700' : 'bg-blue-50 text-blue-700')">
                      {{ !user.personnelId && !user.idCard ? '跳过' : (user.action === 'new' ? '新增' : '覆盖') }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-else class="text-center text-on-surface-variant py-8">暂无可预览的数据</p>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="importPreviewVisible = false">取消</button>
            <button
              class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="importStore.status === 'importing'"
              @click="confirmImport">
            <span class="flex items-center gap-1.5">
              <span v-if="importStore.status === 'importing'" class="material-symbols-outlined animate-spin" style="font-size: 16px;">sync</span>
              {{ importStore.status === 'importing' ? '导入中...' : '确认导入' }}
            </span>
          </button>
          </div>
        </div>
      </div>

      <!-- no separate import result dialog — handled by ImportOverlay -->

      <!-- Add User Dialog -->
      <div v-show="addDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/30" @click="addDialogVisible = false"></div>
        <div ref="dialogAdd" class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl flex flex-col max-h-[85vh]" style="transform:translate3d(0,0,0);transition:none">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant" @mousedown.prevent="startDrag($event, 'add')" style="user-select:none">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">person_add</span>
              添加用户
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="addDialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4 overflow-y-auto flex-1">
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">
                  用户名
                  <span class="text-error">*</span>
                </label>
                <input v-model="addForm.username" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入用户名">
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">
                  姓名
                  <span class="text-error">*</span>
                </label>
                <input v-model="addForm.nickname" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入姓名">
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">手机号</label>
                <input v-model="addForm.phone" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" placeholder="输入手机号（选填）">
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">
                  所属单位
                  <span class="text-error">*</span>
                </label>
                <button class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md text-left flex items-center justify-between hover:border-primary transition-colors" @click="addOrgPickerVisible = true" type="button">
                  <span :class="addForm.orgName ? 'text-on-surface' : 'text-on-surface-variant'">{{ addForm.orgName || '请选择单位' }}</span>
                  <span class="material-symbols-outlined text-[18px] text-outline">search</span>
                </button>
              </div>
            </div>
            <div class="flex items-center gap-6">
              <div class="space-y-1">
                <label class="text-label-md text-secondary">角色</label>
                <select v-model="addForm.userType" class="px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary outline-none">
                  <option value="1">普通用户</option>
                  <option value="2">管理员</option>
                </select>
              </div>
              <div class="space-y-1">
                <label class="text-label-md text-secondary">状态</label>
                <select v-model="addForm.isAlive" class="px-3 py-2 bg-surface border border-outline-variant rounded text-body-md focus:border-primary outline-none">
                  <option :value="0">活跃</option>
                  <option :value="1">禁用</option>
                </select>
              </div>
            </div>
            <div class="px-0 pt-2 text-body-sm text-on-surface-variant">
              初始密码默认为 <code class="px-1.5 py-0.5 bg-surface-variant rounded text-error text-label-md">1234@abcD</code>，用户首次登录需修改密码
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="addDialogVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" :disabled="addSaving" @click="handleSaveAdd">
              {{ addSaving ? '创建中...' : '创建' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Add User Org Tree Picker -->
      <OrgTreePicker
        :visible="addOrgPickerVisible"
        :selected-id="addForm.orgNo"
        :selected-label="addForm.orgName"
        @select="onAddOrgPickerSelect"
        @close="addOrgPickerVisible = false"
      />

      <!-- User Hover Tooltip -->
      <div v-if="hoverUser" class="fixed z-[100]" :style="tooltipStyle" @mouseenter="keepTooltipVisible" @mouseleave="hideUserTooltip">
        <div class="bg-container border border-border rounded-xl shadow-2xl p-3" style="width:420px">
          <div class="space-y-1.5">
            <!-- Header: username + nickname + role/status -->
            <div class="flex items-center gap-2 pb-1.5 border-b border-border">
              <span class="material-symbols-outlined text-primary text-[18px]">badge</span>
              <div class="flex-1 min-w-0">
                <span class="font-label-md text-label-md text-on-surface">{{ hoverUser.username }}</span>
                <span class="text-body-sm text-on-surface-variant ml-1">({{ hoverUser.nickname }})</span>
              </div>
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium whitespace-nowrap" :class="roleClass(hoverUser.userType)">{{ roleLabel(hoverUser.userType) }}</span>
              <span class="inline-flex items-center gap-1 text-[11px] font-medium whitespace-nowrap" :class="hoverUser.isAlive === 0 ? 'text-green-700' : 'text-error'">
                <span class="w-1.5 h-1.5 rounded-full" :class="hoverUser.isAlive === 0 ? 'bg-green-500' : 'bg-error'"></span>
                {{ hoverUser.isAlive === 0 ? '活跃' : '禁用' }}
              </span>
            </div>
            <!-- Info grid: 编号/手机号左右并排，其余全宽 -->
            <div class="grid grid-cols-2 gap-x-3 gap-y-0.5 text-body-sm">
              <div><span class="text-on-surface-variant">编号：</span><span class="text-on-surface">{{ hoverUser.personnelId }}</span></div>
              <div><span class="text-on-surface-variant">手机：</span><span class="text-on-surface">{{ hoverUser.phone || '-' }}</span></div>
              <div class="col-span-2"><span class="text-on-surface-variant">身份证：</span><span class="text-on-surface break-all">{{ hoverUser.idCard || '-' }}</span></div>
              <div class="col-span-2"><span class="text-on-surface-variant">单位：</span><span class="text-on-surface">{{ hoverUser.orgName || '-' }}</span></div>
              <div class="col-span-2"><span class="text-on-surface-variant">注册时间：</span><span class="text-on-surface">{{ hoverUser.createTime || '-' }}</span></div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import OrgTreePicker from '@/components/OrgTreePicker.vue'
import { importStore, resetImport } from '@/utils/importStore'

export default {
  name: 'UserPage',
  components: { OrgTreePicker },
  data() {
    return {
      searchInfo: '',
      users: [],
      multipleSelection: [],
      pageParams: { pageIndex: 1, pageSize: 15 },
      total: 0,
      orgPickerVisible: false,
      importPreviewVisible: false,
      importPreviewData: { users: [], totalCount: 0 },
      importFile: null,
      // 编辑用户
      editDialogVisible: false,
      editSaving: false,
      editUser: {},
      editForm: {
        nickname: '',
        phone: '',
        orgNo: '',
        orgName: '',
        userType: '1',
        isAlive: 0,
      },
      searchTimer: null,
      // 用户悬浮信息卡
      hoverUser: null,
      tooltipStyle: {},
      tooltipTimer: null,
      // 新增用户
      addDialogVisible: false,
      addSaving: false,
      addForm: {
        username: '',
        nickname: '',
        phone: '',
        orgNo: '',
        orgName: '',
        userType: '1',
        isAlive: 0,
      },
      addOrgPickerVisible: false,
      // 编辑用户 - end
      // 弹窗拖动
      dialogPos: {
        edit: { x: 0, y: 0 },
        preview: { x: 0, y: 0 },
        add: { x: 0, y: 0 },
      },
      dialogDrag: null, // { name, startX, startY, origX, origY }
    }
  },
  computed: {
    currentUserType() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (!admin) return 0
        const parsed = JSON.parse(admin)
        return parsed.userType != null ? Number(parsed.userType) : 0
      } catch (e) { return 0 }
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageParams.pageSize))
    },
    importStore() { return importStore },
    importButtonLabel() {
      const s = importStore.status
      if (s === 'previewing') return '解析中...'
      if (s === 'importing') return '导入中...'
      if (s === 'done') return '导入完成，请确认'
      if (s === 'error') return '导入出错，请确认'
      return '导入用户'
    },
    isAllSelected() {
      const checkable = this.users.filter(u => this.canCheck(u))
      return checkable.length > 0 && checkable.every(u => this.isSelected(u))
    },
    missingKeyRows() {
      if (!this.importPreviewData.users) return []
      return this.importPreviewData.users
        .filter(u => !u.personnelId && !u.idCard)
        .map(u => u.rowNum)
    },
  },
  mounted() {
    this.getAllUserPage()
  },
  watch: {
    searchInfo() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.pageParams.pageIndex = 1
        this.getAllUserPage()
      }, 300)
    },
    'importStore.status'(val) {
      // 导入完成后刷新用户列表
      if (val === 'done' && this.importStore.result) {
        this.getAllUserPage()
      }
    },
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag)
    document.removeEventListener('mouseup', this.stopDrag)
  },
  methods: {
    canCheck(row) {
      if (row.userType == 3) return false
      if (row.userType == 2) return this.currentUserType == 3
      return true
    },
    canShowOperation(row) { return this.canCheck(row) },
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
      this.postRequest('/getAllUser', params).then(resp => {
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
    },
    // ====== Excel 导入 ======
    triggerImport() {
      if (importStore.status !== 'idle') return
      this.$refs.fileInput.click()
    },
    handleFileImport(event) {
      const file = event.target.files[0]
      if (!file) return
      // 防止重复提交
      if (importStore.status !== 'idle') {
        event.target.value = ''
        return
      }
      this.importFile = file

      resetImport()
      importStore.status = 'previewing'
      importStore.previewLoading = true

      const formData = new FormData()
      formData.append('file', file)
      this.uploadFile('/admin/previewImport', formData).then(resp => {
        importStore.previewLoading = false
        if (resp) {
          const defaults = {}
          if (resp.users) resp.users.forEach(u => { defaults[u.rowNum] = u.username })
          resp.__defaults = defaults
          this.importPreviewData = resp
          this.importPreviewVisible = true
          importStore.status = 'previewDone'
          importStore.previewData = resp

          // 标记缺失键的行
          importStore.rowsWithMissingKeys = (resp.users || [])
            .filter(u => !u.personnelId && !u.idCard)
            .map(u => u.rowNum)
        } else {
          this.$message.error('预览失败，请检查文件格式')
          resetImport()
        }
      }).catch(err => {
        console.error('[UserPage] 预览请求异常：', err)
        resetImport()
        this.$message.error('预览失败：网络异常或服务器不可用')
      })
      event.target.value = ''
    },
    getDefaultUsername(user) {
      return (this.importPreviewData.__defaults || {})[user.rowNum] || user.username
    },
    orgTitle(user) {
      let t = user.orgName || ''
      if (user.deptName) t += '\n部门：' + user.deptName
      return t
    },
    confirmImport() {
      // 防止重复提交
      if (importStore.status === 'importing') return

      const adjustments = {}
      const defaults = this.importPreviewData.__defaults || {}
      if (this.importPreviewData.users) {
        this.importPreviewData.users.forEach(u => {
          if (defaults[u.rowNum] && defaults[u.rowNum] !== u.username) {
            adjustments[u.rowNum] = u.username
          }
        })
      }

      const formData = new FormData()
      formData.append('file', this.importFile)
      if (Object.keys(adjustments).length > 0) {
        formData.append('adjustments', JSON.stringify(adjustments))
      }

      // 关闭预览弹窗，切换到异步导入进度
      this.importPreviewVisible = false
      resetImport()
      importStore.status = 'importing'
      importStore.progress = 0
      importStore.total = 0

      this.uploadFile('/admin/importUsers', formData).then(resp => {
        if (resp && resp.code === 200 && resp.obj && resp.obj.taskId) {
          importStore.taskId = resp.obj.taskId
          if (resp.obj.reused) {
            this.$message({
              message: '检测到正在进行的导入任务，已自动接续',
              type: 'info',
              duration: 10000,
              showClose: true,
            })
          }
          // ImportOverlay 会自动轮询进度
        } else {
          importStore.status = 'error'
          importStore.error = (resp && resp.message) || '导入任务创建失败'
        }
      }).catch(err => {
        console.error('[UserPage] 导入请求异常：', err)
        importStore.status = 'error'
        importStore.error = '导入请求失败：网络异常或服务器不可用'
      })
    },
    // ====== 编辑用户 ======
    handleOpenEditDialog(user) {
      this.editUser = user
      this.editForm = {
        nickname: user.nickname || '',
        phone: user.phone || '',
        orgNo: user.orgNo || '',
        orgName: user.orgName || '',
        userType: user.userType || '1',
        isAlive: user.isAlive != null ? user.isAlive : 0,
      }
      this.editDialogVisible = true
    },
    onOrgPickerSelect(org) {
      if (org && org.id) {
        this.editForm.orgNo = org.id
        this.editForm.orgName = org.label
      }
      this.orgPickerVisible = false
    },
    handleResetPassword() {
      this.$confirm('确定要重置该用户的密码为默认密码(1234@abcD)吗？', '重置密码', { type: 'warning' }).then(() => {
        this.postRequest('/admin/updateUserDetail', {
          id: this.editUser.id,
          resetPassword: true,
        }).then(resp => {
          if (resp) {
            this.$message.success('密码已重置为 1234@abcD，用户下次登录将强制修改密码')
          }
        })
      }).catch(() => {})
    },
    handleSaveEdit() {
      const params = { id: this.editUser.id }
      if (this.editForm.nickname !== this.editUser.nickname) params.nickname = this.editForm.nickname
      if (this.editForm.phone !== this.editUser.phone) params.phone = this.editForm.phone
      if (this.editForm.orgNo !== this.editUser.orgNo) params.orgNo = this.editForm.orgNo
      if (this.editForm.userType !== this.editUser.userType) params.userType = this.editForm.userType
      if (this.editForm.isAlive !== this.editUser.isAlive) params.isAlive = this.editForm.isAlive

      if (Object.keys(params).length <= 1) {
        this.$message.info('没有需要保存的变更')
        return
      }

      this.editSaving = true
      this.postRequest('/admin/updateUserDetail', params).then(resp => {
        this.editSaving = false
        if (resp) {
          this.$message.success('保存成功')
          this.editDialogVisible = false
          this.getAllUserPage()
        }
      }).catch(() => {
        this.editSaving = false
      })
    },
    handleDeleteFromEdit() {
      var userId = this.editUser.id
      if (!userId) { this.$message.error('用户信息有误'); return }
      this.$confirm('确定要删除该用户吗？此操作不可恢复。', '删除确认', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }).then(() => {
        this.postRequest('/admin/deleteUserByUserId', { userId }).then(resp => {
          if (resp) {
            this.$message.success('删除成功')
            this.editDialogVisible = false
            this.getAllUserPage()
          }
        })
      }).catch(() => {})
    },
    // ====== 新增用户 ======
    handleOpenAddDialog() {
      this.addForm = {
        username: '',
        nickname: '',
        phone: '',
        orgNo: '',
        orgName: '',
        userType: '1',
        isAlive: 0,
      }
      this.addDialogVisible = true
    },
    onAddOrgPickerSelect(org) {
      if (org && org.id) {
        this.addForm.orgNo = org.id
        this.addForm.orgName = org.label
      }
      this.addOrgPickerVisible = false
    },
    handleSaveAdd() {
      if (!this.addForm.username.trim()) {
        this.$message.warning('请输入用户名')
        return
      }
      if (!this.addForm.nickname.trim()) {
        this.$message.warning('请输入姓名')
        return
      }
      if (!this.addForm.orgNo) {
        this.$message.warning('请选择所属单位')
        return
      }

      this.addSaving = true
      this.postRequest('/admin/addUser', {
        username: this.addForm.username.trim(),
        nickname: this.addForm.nickname.trim(),
        phone: this.addForm.phone.trim() || null,
        orgNo: this.addForm.orgNo,
        userType: this.addForm.userType,
        isAlive: this.addForm.isAlive,
      }).then(resp => {
        this.addSaving = false
        if (resp) {
          this.$message.success('用户创建成功')
          this.addDialogVisible = false
          this.getAllUserPage()
        }
      }).catch(() => {
        this.addSaving = false
      })
    },
    // ====== 弹窗拖动（直接DOM操作） ======
    showUserTooltip(user, event) {
      if (this.tooltipTimer) {
        clearTimeout(this.tooltipTimer)
        this.tooltipTimer = null
      }
      this.hoverUser = user
      this.setTooltipPosition(event)
    },
    setTooltipPosition(event) {
      const tooltipWidth = 420
      const tooltipHeight = 180
      let left = event.clientX + 16
      let top = event.clientY + 16
      if (left + tooltipWidth > window.innerWidth - 8) {
        left = event.clientX - tooltipWidth - 8
      }
      if (top + tooltipHeight > window.innerHeight - 8) {
        top = event.clientY - tooltipHeight - 8
      }
      this.tooltipStyle = {
        left: left + 'px',
        top: top + 'px',
      }
    },
    updateTooltipPosition(event) {
      if (!this.hoverUser) return
      this.setTooltipPosition(event)
    },
    keepTooltipVisible() {
      if (this.tooltipTimer) {
        clearTimeout(this.tooltipTimer)
        this.tooltipTimer = null
      }
    },
    hideUserTooltip() {
      this.tooltipTimer = setTimeout(() => {
        this.hoverUser = null
      }, 200)
    },
    startDrag(e, name) {
      e.preventDefault()
      const refMap = { edit: 'dialogEdit', preview: 'dialogPreview', add: 'dialogAdd' }
      const el = this.$refs[refMap[name]]
      if (!el) return
      const pos = this.dialogPos[name]
      // 恢复上次位置
      if (pos && (pos.x || pos.y)) {
        el.style.transform = 'translate3d(' + pos.x + 'px,' + pos.y + 'px,0)'
      }
      this.dialogDrag = {
        name, el,
        startX: e.clientX,
        startY: e.clientY,
        origX: pos ? pos.x : 0,
        origY: pos ? pos.y : 0
      }
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'grabbing'
      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.stopDrag)
    },
    onDrag(e) {
      if (!this.dialogDrag || !this.dialogDrag.el) return
      const d = this.dialogDrag
      d.el.style.transform = 'translate3d(' + (d.origX + e.clientX - d.startX) + 'px,' + (d.origY + e.clientY - d.startY) + 'px,0)'
    },
    stopDrag() {
      if (!this.dialogDrag) return
      const d = this.dialogDrag
      if (d.el) {
        const s = d.el.style.transform
        const m = s && s.match(/translate3?d?\(([-\d.]+)px,?\s*([-\d.]+)px/)
        if (m) {
          this.dialogPos[d.name].x = parseFloat(m[1])
          this.dialogPos[d.name].y = parseFloat(m[2])
        }
      }
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      d.el = null
      this.dialogDrag = null
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
    },
  }
}
</script>
