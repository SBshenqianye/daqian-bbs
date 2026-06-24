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
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="triggerImport">
            <span class="material-symbols-outlined text-[18px]">upload_file</span>
            导入用户
          </button>
          <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileImport">
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
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">操作</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[60px]">ID</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">用户名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">姓名</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[130px]">手机号</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">角色</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">状态</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[170px]">注册时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, index) in users" :key="user.id" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
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

      <!-- Edit User Dialog -->
      <div v-if="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="editDialogVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">edit</span>
              编辑用户
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="editDialogVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
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
                <div class="relative">
                  <button class="w-full px-3 py-2 bg-surface border border-outline-variant rounded text-body-md text-left flex items-center justify-between hover:border-primary transition-colors" @click="editOrgTreeExpanded = !editOrgTreeExpanded" type="button">
                    <span>{{ editForm.orgName || '请选择单位' }}</span>
                    <span class="material-symbols-outlined text-[18px] text-outline">{{ editOrgTreeExpanded ? 'expand_less' : 'expand_more' }}</span>
                  </button>
                  <div v-if="editOrgTreeExpanded" class="absolute z-10 mt-1 w-full bg-container border border-outline-variant rounded-lg shadow-lg max-h-60 overflow-y-auto">
                    <div v-if="editOrgTree.length === 0" class="p-4 text-center text-on-surface-variant text-body-md">加载中...</div>
                    <div v-else class="p-2">
                      <div v-for="org in editOrgTree" :key="org.id" class="mb-1">
                        <div class="flex items-center gap-2 px-3 py-1.5 rounded cursor-pointer hover:bg-primary/5" :class="editForm.orgNo === org.id ? 'bg-primary/10 text-primary font-semibold' : ''" @click="selectEditOrg(org.id, org.label)">
                          <span class="material-symbols-outlined text-[18px] text-outline">folder</span>
                          <span class="text-body-md">{{ org.label }}</span>
                        </div>
                        <div v-if="org.children && org.children.length" class="ml-5 border-l border-outline-variant/30">
                          <div v-for="child in org.children" :key="child.id" class="flex items-center gap-2 px-3 py-1.5 rounded cursor-pointer hover:bg-primary/5 ml-2" :class="editForm.orgNo === child.id ? 'bg-primary/10 text-primary font-semibold' : ''" @click="selectEditOrg(child.id, child.label)">
                            <span class="material-symbols-outlined text-[18px] text-outline">folder_open</span>
                            <span class="text-body-md">{{ child.label }}</span>
                          </div>
                          <div v-if="child.children && child.children.length" class="ml-5 border-l border-outline-variant/20">
                            <div v-for="sub in child.children" :key="sub.id" class="flex items-center gap-2 px-3 py-1.5 rounded cursor-pointer hover:bg-primary/5 ml-2" :class="editForm.orgNo === sub.id ? 'bg-primary/10 text-primary font-semibold' : ''" @click="selectEditOrg(sub.id, sub.label)">
                              <span class="material-symbols-outlined text-[18px] text-outline">corporate_fare</span>
                              <span class="text-body-md">{{ sub.label }}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
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

      <!-- Import Preview Dialog -->
      <div v-if="importPreviewVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="importPreviewVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-3xl rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
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
            <table class="w-full text-left border-collapse" v-if="importPreviewData.users && importPreviewData.users.length">
              <thead>
                <tr class="bg-surface-container-low border-b border-border">
                  <th class="p-2 text-label-md text-on-surface-variant">姓名</th>
                  <th class="p-2 text-label-md text-on-surface-variant">身份证号</th>
                  <th class="p-2 text-label-md text-on-surface-variant">账号（可编辑）</th>
                  <th class="p-2 text-label-md text-on-surface-variant">单位</th>
                  <th class="p-2 text-label-md text-on-surface-variant">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in importPreviewData.users" :key="user.rowNum" class="border-b border-border">
                  <td class="p-2 font-body-md">{{ user.nickname }}</td>
                  <td class="p-2 font-body-md text-on-surface-variant">{{ user.idCard }}</td>
                  <td class="p-2">
                    <input v-model="user.username" class="w-full px-2 py-1 border border-outline-variant rounded text-body-md focus:border-primary focus:ring-1 focus:ring-primary outline-none" :class="{ 'bg-warning/10 border-warning': getDefaultUsername(user) !== user.username }">
                  </td>
                  <td class="p-2 font-body-md text-on-surface-variant">{{ user.orgName }}</td>
                  <td class="p-2">
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-[12px] font-medium" :class="user.action === 'new' ? 'bg-green-50 text-green-700' : 'bg-blue-50 text-blue-700'">
                      {{ user.action === 'new' ? '新增' : '覆盖' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-else class="text-center text-on-surface-variant py-8">暂无可预览的数据</p>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="importPreviewVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" :disabled="importLoading" @click="confirmImport">
              {{ importLoading ? '导入中...' : '确认导入' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Import Result Dialog -->
      <div v-if="importResultVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="importResultVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-2xl rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">assignment_turned_in</span>
              导入结果
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="importResultVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5" v-if="importResultData">
            <div class="flex gap-6 mb-4">
              <div class="flex-1 bg-green-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-green-700">{{ importResultData.userSuccessCount || 0 }}</p>
                <p class="text-sm text-green-600">导入成功</p>
              </div>
              <div class="flex-1 bg-red-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-red-700">{{ importResultData.userFailCount || 0 }}</p>
                <p class="text-sm text-red-600">导入失败</p>
              </div>
              <div class="flex-1 bg-blue-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-blue-700">{{ importResultData.orgCreatedCount || 0 }}</p>
                <p class="text-sm text-blue-600">新建组织</p>
              </div>
            </div>
            <div class="max-h-64 overflow-y-auto" v-if="importResultData.details && importResultData.details.length">
              <table class="w-full text-left border-collapse">
                <thead>
                  <tr class="bg-surface-container-low border-b border-border">
                    <th class="p-2 text-label-md text-on-surface-variant">行号</th>
                    <th class="p-2 text-label-md text-on-surface-variant">姓名</th>
                    <th class="p-2 text-label-md text-on-surface-variant">账号</th>
                    <th class="p-2 text-label-md text-on-surface-variant">操作</th>
                    <th class="p-2 text-label-md text-on-surface-variant">结果</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="d in importResultData.details" :key="d.rowNum" class="border-b border-border" :class="d.success ? '' : 'bg-error/5'">
                    <td class="p-2 font-body-md">{{ d.rowNum }}</td>
                    <td class="p-2 font-body-md">{{ d.nickname }}</td>
                    <td class="p-2 font-body-md">{{ d.username }}</td>
                    <td class="p-2 font-body-md">{{ d.action }}</td>
                    <td class="p-2">
                      <span class="inline-flex items-center gap-1" :class="d.success ? 'text-green-600' : 'text-red-600'">
                        <span class="material-symbols-outlined text-[16px]">{{ d.success ? 'check_circle' : 'error' }}</span>
                        {{ d.message }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="importResultVisible = false; getAllUserPage()">确定</button>
          </div>
        </div>
      </div>

    </div>
  </div>

</div>
</template>

<script>
export default {
  name: 'UserPage',
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
      orgFilterText: '',
      importPreviewVisible: false,
      importResultVisible: false,
      importLoading: false,
      importPreviewData: { users: [], totalCount: 0 },
      importResultData: null,
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
      editOrgTree: [],
      editOrgTreeExpanded: false
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
    },
    // ====== Excel 导入 ======
    triggerImport() {
      this.$refs.fileInput.click()
    },
    handleFileImport(event) {
      const file = event.target.files[0]
      if (!file) return
      this.importFile = file

      const formData = new FormData()
      formData.append('file', file)
      this.importLoading = true
      this.uploadFile('/admin/previewImport', formData).then(resp => {
        this.importLoading = false
        if (resp) {
          const defaults = {}
          if (resp.users) resp.users.forEach(u => { defaults[u.rowNum] = u.username })
          resp.__defaults = defaults
          this.importPreviewData = resp
          this.importPreviewVisible = true
        } else {
          this.$message.error('预览失败，请检查文件格式')
        }
      }).catch(() => {
        this.importLoading = false
        this.$message.error('预览失败，请检查文件格式')
      })
      event.target.value = ''
    },
    getDefaultUsername(user) {
      return (this.importPreviewData.__defaults || {})[user.rowNum] || user.username
    },
    confirmImport() {
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

      this.importLoading = true
      this.uploadFile('/admin/importUsers', formData).then(resp => {
        this.importLoading = false
        this.importPreviewVisible = false
        if (resp) {
          this.importResultData = resp
          this.importResultVisible = true
        } else {
          this.$message.error('导入失败')
        }
      }).catch(() => {
        this.importLoading = false
        this.importPreviewVisible = false
        this.$message.error('导入失败，请检查文件格式和数据')
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
      this.editOrgTreeExpanded = false
      // 加载单位树
      if (this.editOrgTree.length === 0) {
        this.getRequestUrl('/common/saOrgTree').then(resp => {
          this.editOrgTree = resp && resp.obj ? this.normalizeOrgTree(resp.obj) : []
        }).catch(() => { this.editOrgTree = [] })
      }
      this.editDialogVisible = true
    },
    selectEditOrg(orgNo, orgName) {
      this.editForm.orgNo = orgNo
      this.editForm.orgName = orgName
      this.editOrgTreeExpanded = false
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
  }
}
</script>
