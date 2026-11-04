<template>
  <div class="bg-surface min-h-screen">
    <div class="mx-auto max-w-[1500px] px-page-margin-desktop py-6">
      <!-- 页面标题 -->
      <div class="mb-5 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-600">admin_panel_settings</span>
            权限管理
          </h1>
          <p class="text-body-md text-secondary mt-1">
            权限组是账号权限的唯一来源。主权限组决定身份与数据范围，额外叠加的权限组只补充权限。
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <button
            type="button"
            class="flex items-center gap-1 rounded-lg bg-surface-variant px-3 py-2 text-body-sm text-on-surface hover:bg-outline-variant disabled:opacity-60"
            :disabled="loadingGroups"
            @click="loadGroups()"
          >
            <span class="material-symbols-outlined text-[16px]" :class="loadingGroups ? 'animate-spin' : ''">refresh</span>
            刷新
          </button>
          <button
            type="button"
            class="flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-body-sm text-on-primary hover:opacity-90"
            @click="newGroup"
          >
            <span class="material-symbols-outlined text-[16px]">add</span>
            新建权限组
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 items-start gap-5 lg:grid-cols-12">
        <!-- 左：权限组列表 -->
        <aside class="lg:col-span-4 xl:col-span-3">
          <div class="rounded-xl border border-border bg-container p-3 lg:sticky lg:top-4">
            <div class="mb-2 flex items-center justify-between px-1">
              <h2 class="font-title-lg text-title-lg text-on-surface">
                权限组
                <span class="ml-1 text-[11px] font-normal text-outline">{{ filteredGroups.length }}/{{ groups.length }}</span>
              </h2>
            </div>

            <div class="relative mb-2">
              <span class="pointer-events-none absolute inset-y-0 left-0 flex w-10 items-center justify-center text-outline">
                <svg width="18" height="18" class="h-[18px] w-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-3.5-3.5" />
                </svg>
              </span>
              <input
                v-model.trim="groupSearch"
                type="text"
                class="w-full rounded-lg border border-outline-variant bg-surface py-2 pl-9 pr-3 text-body-sm text-on-surface outline-none focus:border-primary"
                placeholder="搜索名称或编码"
                aria-label="搜索权限组"
              />
            </div>

            <div class="mb-2 flex items-center gap-1">
              <button
                v-for="filter in statusFilters"
                :key="filter.value"
                type="button"
                class="rounded-md px-2 py-1 text-[11px] transition-colors"
                :class="groupStatusFilter === filter.value
                  ? 'bg-primary/10 font-medium text-primary'
                  : 'text-on-surface-variant hover:bg-surface-container-low'"
                @click="groupStatusFilter = filter.value"
              >{{ filter.label }}</button>
            </div>

            <div class="max-h-[70vh] space-y-1.5 overflow-y-auto pr-1">
              <button
                v-for="group in filteredGroups"
                :key="group.id"
                type="button"
                class="w-full rounded-lg border px-3 py-2.5 text-left transition-colors"
                :class="form.id === group.id
                  ? 'border-primary bg-primary-fixed/40'
                  : 'border-transparent hover:border-outline-variant hover:bg-surface-container-low'"
                :aria-current="form.id === group.id ? 'true' : null"
                @click="requestSelectGroup(group)"
              >
                <div class="flex items-start justify-between gap-2">
                  <span class="min-w-0 truncate font-body-md text-on-surface">{{ group.groupName }}</span>
                  <span
                    class="shrink-0 rounded-full px-1.5 py-0.5 text-[10px]"
                    :class="group.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'"
                  >{{ group.enabled ? '启用' : '停用' }}</span>
                </div>
                <div class="mt-0.5 flex min-w-0 items-center gap-1.5 text-[11px] text-outline">
                  <span class="truncate">{{ group.groupCode }}</span>
                  <span v-if="isDefaultCode(group.groupCode)" class="shrink-0 rounded bg-amber-50 px-1 text-[10px] text-amber-700">默认</span>
                </div>
                <div class="mt-1.5 flex items-center justify-between gap-2 text-[11px] text-outline">
                  <span class="shrink-0">{{ group.permissionCount || 0 }} 权限点</span>
                  <span class="truncate">主组 {{ group.primaryUserCount || 0 }} · 叠加 {{ group.memberCount || 0 }}</span>
                </div>
              </button>

              <div v-if="filteredGroups.length === 0" class="py-10 text-center text-body-sm text-on-surface-variant">
                {{ groups.length === 0 ? '暂无权限组' : '没有匹配的权限组' }}
              </div>
            </div>
          </div>
        </aside>

        <!-- 右：编辑区 -->
        <section class="min-w-0 lg:col-span-8 xl:col-span-9">
          <div v-if="loadingGroup && !showEditor" class="flex items-center justify-center rounded-xl border border-border bg-container py-24 text-body-sm text-on-surface-variant">
            <span class="material-symbols-outlined mr-2 animate-spin text-[20px]">progress_activity</span>
            正在加载权限组…
          </div>

          <div v-else-if="!showEditor" class="flex flex-col items-center justify-center rounded-xl border border-border bg-container px-6 py-24 text-center">
            <span class="material-symbols-outlined text-[56px] text-outline">admin_panel_settings</span>
            <h2 class="mt-4 font-title-lg text-title-lg text-on-surface">选择一个权限组开始编辑</h2>
            <p class="mt-1 text-body-sm text-on-surface-variant">也可以新建一个权限组，再配置权限点和叠加成员。</p>
            <button type="button" class="mt-5 rounded-lg bg-primary px-4 py-2 text-body-sm text-on-primary hover:opacity-90" @click="newGroup">
              新建权限组
            </button>
          </div>

          <div v-else class="relative">
            <div
              v-if="loadingGroup"
              class="absolute inset-0 z-30 flex items-center justify-center rounded-xl bg-container/70 text-body-sm text-on-surface-variant"
            >
              <span class="material-symbols-outlined mr-2 animate-spin text-[20px]">progress_activity</span>
              正在加载权限组…
            </div>

            <transition name="group-fade" mode="out-in">
              <div :key="editorKey">
            <!-- 编辑区顶部：名称、状态、保存 -->
            <div class="z-20 mb-4 rounded-xl border border-border bg-container p-4 shadow-sm lg:sticky lg:top-4">
              <div class="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <h2 class="truncate font-headline-sm text-headline-sm text-on-surface">
                      {{ form.id ? form.groupName : '新建权限组' }}
                    </h2>
                    <span v-if="isDefaultGroup" class="rounded bg-amber-50 px-1.5 py-0.5 text-[11px] text-amber-700">默认组</span>
                    <span
                      class="rounded-full px-2 py-0.5 text-[11px]"
                      :class="form.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'"
                    >{{ form.enabled ? '启用' : '停用' }}</span>
                    <span v-if="isDirty" class="rounded-full bg-amber-50 px-2 py-0.5 text-[11px] text-amber-700">有未保存修改</span>
                  </div>
                  <p class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-body-sm text-outline">
                    <span class="font-mono">{{ form.groupCode || '填写编码后创建' }}</span>
                    <span>·</span>
                    <span>{{ dataScopeLabel }}</span>
                    <span>·</span>
                    <span>{{ autoScopeLabel }}</span>
                    <span v-if="form.id">· 主组 {{ form.primaryUserCount || 0 }} 人 · 叠加 {{ form.memberCount || 0 }} 人</span>
                  </p>
                </div>

                <div class="flex shrink-0 flex-wrap items-center gap-2">
                  <el-switch
                    v-model="form.enabled"
                    :active-value="1"
                    :inactive-value="0"
                    title="启用 / 停用该权限组"
                  />
                  <button
                    v-if="isDirty"
                    type="button"
                    class="rounded-lg px-2 py-2 text-body-sm text-on-surface-variant hover:text-on-surface"
                    @click="resetChanges"
                  >放弃修改</button>
                  <button
                    v-if="form.id"
                    type="button"
                    class="rounded-lg border border-error px-3 py-2 text-body-sm text-error hover:bg-error/5"
                    @click="deleteGroup"
                  >删除</button>
                  <button
                    type="button"
                    class="rounded-lg bg-primary px-4 py-2 text-body-sm text-on-primary hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="saving || (!!form.id && !isDirty)"
                    :title="form.id && !isDirty ? '没有需要保存的修改' : ''"
                    @click="saveGroup"
                  >
                    {{ saving ? '保存中…' : (form.id ? '保存修改' : '创建权限组') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 编辑区主体：分页签，避免信息堆叠 -->
            <div class="rounded-xl border border-border bg-container">
              <div class="flex items-center gap-1 overflow-x-auto border-b border-outline-variant px-3">
                <button
                  v-for="tab in tabs"
                  :key="tab.key"
                  type="button"
                  class="flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-3 text-body-sm transition-colors"
                  :class="activeTab === tab.key
                    ? 'border-primary font-medium text-primary'
                    : 'border-transparent text-on-surface-variant hover:text-on-surface'"
                  @click="activeTab = tab.key"
                >
                  <span class="material-symbols-outlined text-[17px]">{{ tab.icon }}</span>
                  {{ tab.label }}
                  <span
                    v-if="tab.count !== null"
                    class="rounded-full px-1.5 py-0.5 text-[10px]"
                    :class="activeTab === tab.key ? 'bg-primary/10 text-primary' : 'bg-surface-container-low text-outline'"
                  >{{ tab.count }}</span>
                </button>
              </div>

              <!-- 权限点 -->
              <div v-show="activeTab === 'permissions'" class="p-4">
                <div class="mb-4 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                  <div class="relative w-full md:max-w-sm">
                    <span class="pointer-events-none absolute inset-y-0 left-0 flex w-10 items-center justify-center text-outline">
                      <svg width="18" height="18" class="h-[18px] w-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="11" cy="11" r="7" />
                        <path d="m20 20-3.5-3.5" />
                      </svg>
                    </span>
                    <input
                      v-model.trim="permissionQuery"
                      type="text"
                      class="w-full rounded-lg border border-outline-variant bg-surface py-2 pl-9 pr-3 text-body-sm text-on-surface outline-none focus:border-primary"
                      placeholder="搜索权限点名称或编码"
                      aria-label="搜索权限点"
                    />
                  </div>
                  <div class="flex flex-wrap items-center gap-3 text-body-sm text-on-surface-variant">
                    <label class="flex cursor-pointer select-none items-center gap-1.5">
                      <input v-model="onlySelected" type="checkbox" class="accent-primary" />
                      只看已选
                    </label>
                    <span>已选 {{ selectedCodes.length }} / {{ totalPermissionCount }}</span>
                    <button
                      v-if="selectedCodes.length"
                      type="button"
                      class="text-error hover:underline"
                      @click="clearSelection"
                    >清空</button>
                  </div>
                </div>

                <div
                  v-if="isOwnPrimaryGroup"
                  class="mb-4 flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-[12px] leading-relaxed text-amber-800"
                >
                  <span class="material-symbols-outlined text-[16px]">lock</span>
                  <span>
                    当前编辑的是你账号的主权限组。关键权限（进入管理后台、权限管理、授予/调整管理员）已锁定，避免把自己锁在系统外。
                  </span>
                </div>

                <div class="space-y-5">
                  <div v-for="permissionGroup in visiblePermissionGroups" :key="permissionGroup.key">
                    <div class="mb-2 flex items-center justify-between">
                      <h3 class="text-body-md font-medium text-on-surface">{{ permissionGroup.label }}</h3>
                      <button
                        type="button"
                        class="text-[12px] text-primary hover:underline"
                        @click="togglePermissionGroup(permissionGroup)"
                      >{{ isPermissionGroupAllChecked(permissionGroup) ? '取消全选' : '全选' }}</button>
                    </div>

                    <div class="space-y-2">
                      <div
                        v-for="section in permissionGroup.sections"
                        :key="section.section"
                        class="overflow-hidden rounded-lg border border-outline-variant"
                      >
                        <label class="flex cursor-pointer select-none items-center justify-between gap-2 bg-surface-container-low px-3 py-2">
                          <span class="flex min-w-0 items-center gap-2">
                            <input
                              type="checkbox"
                              class="accent-primary"
                              :checked="isSectionAllChecked(section)"
                              :indeterminate.prop="isSectionIndeterminate(section)"
                              @change="toggleSection(section)"
                            />
                            <span class="truncate text-[12px] font-medium text-on-surface">{{ section.section }}</span>
                          </span>
                          <span class="shrink-0 text-[11px] text-outline">{{ sectionCheckedCount(section) }}/{{ section.items.length }}</span>
                        </label>

                        <div class="grid grid-cols-1 gap-x-3 gap-y-0.5 px-3 py-2 md:grid-cols-2 xl:grid-cols-3">
                          <label
                            v-for="item in section.items"
                            :key="item.code"
                            class="flex min-w-0 cursor-pointer items-center gap-2 rounded px-1 py-1 hover:bg-surface-container-low"
                            :title="item.code"
                          >
                            <input
                              type="checkbox"
                              class="shrink-0 accent-primary"
                              :checked="isChecked(item.code)"
                              :disabled="isLockedCode(item.code)"
                              @change="toggleCode(item.code)"
                            />
                            <span class="truncate text-[12px] text-on-surface">{{ permissionLabel(item.code, item.label) }}</span>
                            <span
                              v-if="isCriticalCode(item.code)"
                              class="material-symbols-outlined ml-auto shrink-0 text-[12px] text-amber-600"
                              title="关键权限"
                            >lock</span>
                          </label>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-if="visiblePermissionGroups.length === 0" class="py-12 text-center text-body-sm text-on-surface-variant">
                    {{ onlySelected ? '当前没有已选权限点' : '没有匹配的权限点' }}
                  </div>
                </div>
              </div>

              <!-- 基本信息 -->
              <div v-show="activeTab === 'basic'" class="p-4">
                <div class="max-w-2xl space-y-4">
                  <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                    <div>
                      <label for="permission-group-code" class="mb-1 block font-label-md text-label-md text-secondary">编码</label>
                      <input
                        id="permission-group-code"
                        v-model.trim="form.groupCode"
                        type="text"
                        class="w-full rounded-lg border border-outline-variant bg-surface px-3 py-2 text-body-sm text-on-surface outline-none focus:border-primary"
                        placeholder="如 content_operator"
                      />
                      <p class="mt-1 text-[11px] text-outline">用于程序识别，建议使用英文、数字和下划线。</p>
                    </div>
                    <div>
                      <label for="permission-group-name" class="mb-1 block font-label-md text-label-md text-secondary">名称</label>
                      <input
                        id="permission-group-name"
                        v-model.trim="form.groupName"
                        type="text"
                        class="w-full rounded-lg border border-outline-variant bg-surface px-3 py-2 text-body-sm text-on-surface outline-none focus:border-primary"
                        placeholder="如 内容运营"
                      />
                    </div>
                  </div>

                  <div>
                    <label for="permission-group-description" class="mb-1 block font-label-md text-label-md text-secondary">描述</label>
                    <textarea
                      id="permission-group-description"
                      v-model="form.description"
                      rows="3"
                      class="w-full resize-none rounded-lg border border-outline-variant bg-surface px-3 py-2 text-body-sm text-on-surface outline-none focus:border-primary"
                      placeholder="说明这个权限组的职责和使用场景"
                    />
                  </div>

                  <div>
                    <span class="mb-1 block font-label-md text-label-md text-secondary">数据范围</span>
                    <div class="flex w-full rounded-lg border border-outline-variant bg-surface-container-low p-0.5">
                      <button
                        v-for="option in dataScopeOptions"
                        :key="option.value"
                        type="button"
                        class="flex-1 rounded-md px-3 py-1.5 text-body-sm transition-colors"
                        :class="form.dataScopeType === option.value
                          ? 'bg-primary text-on-primary shadow-sm'
                          : 'text-on-surface-variant hover:bg-surface-container'"
                        @click="form.dataScopeType = option.value"
                      >{{ option.label }}</button>
                    </div>
                    <p class="mt-1 text-[11px] leading-relaxed text-outline">
                      只有主权限组的数据范围生效：全局可查看所有板块；按版主任职只能查看自己负责的板块。
                    </p>
                  </div>

                  <div class="flex items-center justify-between rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2">
                    <div>
                      <p class="text-[12px] text-on-surface">适用端（自动标记）</p>
                      <p class="mt-0.5 text-[11px] text-outline">根据勾选的权限点自动判断，不参与鉴权。</p>
                    </div>
                    <span class="rounded-full bg-primary-fixed/60 px-2.5 py-1 text-[12px] text-on-surface">{{ autoScopeLabel }}</span>
                  </div>
                </div>
              </div>

              <!-- 叠加成员 -->
              <div v-show="activeTab === 'members'" class="p-4">
                <div class="mb-4 rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-body-sm text-on-surface-variant">
                  主权限组请在「用户管理」中分配。这里添加的是额外叠加成员：成员同时拥有主组权限和本组权限。
                </div>

                <div class="flex flex-col gap-2 sm:flex-row sm:items-start">
                  <div class="min-w-0 flex-1">
                    <UserSelect v-model="assignUserId" placeholder="搜索用户名或昵称后添加" />
                  </div>
                  <button
                    type="button"
                    class="shrink-0 rounded-lg bg-primary px-4 py-2 text-body-sm text-on-primary hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="!assignUserId || !form.id"
                    @click="assignUser"
                  >添加成员</button>
                </div>

                <div v-if="groupUsers.length" class="mt-4 divide-y divide-outline-variant/60 rounded-lg border border-outline-variant">
                  <div v-for="user in groupUsers" :key="user.userId" class="flex items-center gap-3 px-3 py-2.5">
                    <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[12px] font-medium text-primary">
                      {{ (user.nickname || user.username || '?').charAt(0) }}
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-body-sm text-on-surface">{{ user.nickname || user.username }}</p>
                      <p class="truncate text-[11px] text-outline">{{ user.username }} · #{{ user.userId }}</p>
                    </div>
                    <button type="button" class="shrink-0 text-[12px] text-error hover:underline" @click="removeUser(user.userId)">移除</button>
                  </div>
                </div>
                <p v-else class="mt-4 rounded-lg border border-dashed border-outline-variant py-10 text-center text-body-sm text-on-surface-variant">
                  暂无叠加成员
                </p>
              </div>

              <!-- 预设方案 -->
              <div v-show="activeTab === 'presets'" class="p-4">
                <p class="mb-4 rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2.5 text-body-sm text-on-surface-variant">
                  预设是当前权限组配置的快照。保存后可以一键应用到其他权限组，会覆盖目标组的名称、描述和权限点。
                </p>

                <div class="mb-4 flex flex-col gap-2 sm:flex-row">
                  <input
                    v-model.trim="presetName"
                    type="text"
                    class="min-w-0 flex-1 rounded-lg border border-outline-variant bg-surface px-3 py-2 text-body-sm text-on-surface outline-none focus:border-primary"
                    :placeholder="form.groupName ? `预设名称（默认：${form.groupName}）` : '预设名称'"
                  />
                  <button
                    type="button"
                    class="shrink-0 rounded-lg bg-surface-variant px-4 py-2 text-body-sm text-on-surface hover:bg-outline-variant disabled:opacity-50"
                    :disabled="!form.id"
                    @click="savePreset"
                  >保存当前配置</button>
                </div>

                <div v-if="presets.length" class="divide-y divide-outline-variant/60 rounded-lg border border-outline-variant">
                  <div v-for="preset in presets" :key="preset.id" class="px-3 py-3">
                    <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                      <button type="button" class="min-w-0 flex-1 text-left" @click="togglePreset(preset)">
                        <p class="truncate text-body-sm text-on-surface">{{ preset.presetName }}</p>
                        <p class="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-outline">
                          <span>{{ preset.presetCode }}</span>
                          <span v-if="preset.sourceGroupName">· 来源：{{ preset.sourceGroupName }}</span>
                          <span v-if="preset.permissionCodes">· {{ preset.permissionCodes.length }} 权限点</span>
                          <span v-if="preset.dataScopeType">· {{ presetDataScopeLabel(preset.dataScopeType) }}</span>
                          <span v-if="preset.createTime">· {{ preset.createTime }}</span>
                        </p>
                      </button>
                      <div class="flex shrink-0 items-center gap-2">
                        <button type="button" class="text-[12px] text-primary hover:underline" @click="togglePreset(preset)">
                          {{ expandedPresetId === preset.id ? '收起' : '详情' }}
                        </button>
                        <button type="button" class="text-[12px] text-primary hover:underline" :disabled="!form.id" @click="applyPreset(preset)">应用</button>
                        <button type="button" class="text-[12px] text-error hover:underline" @click="deletePreset(preset)">删除</button>
                      </div>
                    </div>

                    <div v-if="expandedPresetId === preset.id" class="mt-3 rounded-lg border border-outline-variant bg-surface-container-low p-3">
                      <div class="grid grid-cols-2 gap-3 text-[11px] text-on-surface-variant sm:grid-cols-4">
                        <div>
                          <p class="text-outline">来源组</p>
                          <p class="mt-0.5 text-on-surface">{{ preset.sourceGroupName || '—' }}</p>
                        </div>
                        <div>
                          <p class="text-outline">来源编码</p>
                          <p class="mt-0.5 text-on-surface">{{ preset.sourceGroupCode || '—' }}</p>
                        </div>
                        <div>
                          <p class="text-outline">数据范围</p>
                          <p class="mt-0.5 text-on-surface">{{ presetDataScopeLabel(preset.dataScopeType) }}</p>
                        </div>
                        <div>
                          <p class="text-outline">适用端</p>
                          <p class="mt-0.5 text-on-surface">{{ presetScopeLabel(preset.scopeType) }}</p>
                        </div>
                      </div>
                      <div class="mt-3">
                        <p class="mb-1 text-[11px] text-outline">权限点（{{ (preset.permissionCodes || []).length }}）</p>
                        <div class="flex flex-wrap gap-1.5">
                          <span
                            v-for="code in (preset.permissionCodes || [])"
                            :key="code"
                            :title="code"
                            class="rounded border border-outline-variant bg-container px-2 py-0.5 text-[11px] text-on-surface"
                          >{{ permissionLabel(code, code) }}</span>
                          <span v-if="!(preset.permissionCodes || []).length" class="text-[11px] text-outline">无权限点</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="rounded-lg border border-dashed border-outline-variant py-10 text-center text-body-sm text-on-surface-variant">
                  暂无预设方案
                </p>
              </div>
            </div>
              </div>
            </transition>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import { PERMISSION_LABELS, labelOf } from '@/utils/labels'
import {
  PERMISSION_GROUPS,
  ADMIN_CODE_SET,
  FRONT_CODE_SET,
  DEFAULT_GROUP_CODES,
  CRITICAL_PERMISSION_CODES,
  allPermissionItems
} from '@/utils/permissionCatalog'
import UserSelect from '@/components/UserSelect.vue'
import adminStore from '@/utils/adminStore'

const TOTAL_PERMISSION_COUNT = allPermissionItems().length

function emptyForm(sortOrder = 0) {
  return {
    id: null,
    groupCode: '',
    groupName: '',
    description: '',
    dataScopeType: 'GLOBAL',
    enabled: 1,
    sortOrder,
    primaryUserCount: 0,
    memberCount: 0
  }
}

function normalizeDataScope(value) {
  return !value || value === 'ROLE_DEFAULT' ? 'GLOBAL' : value
}

export default {
  name: 'PermissionGroupPage',
  components: { UserSelect },
  data() {
    return {
      groups: [],
      presets: [],
      groupSearch: '',
      groupStatusFilter: 'all',
      statusFilters: [
        { value: 'all', label: '全部' },
        { value: 'enabled', label: '启用' },
        { value: 'disabled', label: '停用' }
      ],
      form: emptyForm(),
      originalGroupCode: '',
      selectedCodes: [],
      groupUsers: [],
      assignUserId: null,
      presetName: '',
      expandedPresetId: null,
      activeTab: 'permissions',
      permissionQuery: '',
      onlySelected: false,
      showEditor: false,
      loadingGroups: false,
      loadingGroup: false,
      saving: false,
      baselineSignature: '',
      totalPermissionCount: TOTAL_PERMISSION_COUNT,
      dataScopeOptions: [
        { value: 'GLOBAL', label: '全局（默认）' },
        { value: 'BOARD_MODERATOR', label: '按版主任职' }
      ]
    }
  },
  computed: {
    filteredGroups() {
      const keyword = this.groupSearch.trim().toLowerCase()
      return this.groups.filter(group => {
        if (this.groupStatusFilter === 'enabled' && !group.enabled) return false
        if (this.groupStatusFilter === 'disabled' && group.enabled) return false
        if (!keyword) return true
        return String(group.groupName || '').toLowerCase().indexOf(keyword) !== -1
          || String(group.groupCode || '').toLowerCase().indexOf(keyword) !== -1
      })
    },
    tabs() {
      return [
        { key: 'permissions', label: '权限点', icon: 'rule', count: this.selectedCodes.length },
        { key: 'basic', label: '基本信息', icon: 'edit_note', count: null },
        { key: 'members', label: '叠加成员', icon: 'group_add', count: this.groupUsers.length },
        { key: 'presets', label: '预设方案', icon: 'bookmark', count: this.presets.length }
      ]
    },
    visiblePermissionGroups() {
      const keyword = this.permissionQuery.trim().toLowerCase()
      return PERMISSION_GROUPS.map(group => ({
        ...group,
        sections: group.sections.map(section => ({
          ...section,
          items: section.items.filter(item => {
            if (this.onlySelected && !this.isChecked(item.code)) return false
            if (!keyword) return true
            return this.permissionLabel(item.code, item.label).toLowerCase().indexOf(keyword) !== -1
              || item.code.toLowerCase().indexOf(keyword) !== -1
              || section.section.toLowerCase().indexOf(keyword) !== -1
          })
        })).filter(section => section.items.length > 0)
      })).filter(group => group.sections.length > 0)
    },
    isDefaultGroup() {
      return DEFAULT_GROUP_CODES.indexOf(this.originalGroupCode || this.form.groupCode) !== -1
    },
    isOwnPrimaryGroup() {
      const primaryGroupId = adminStore.getPrimaryGroupId()
      return this.form.id != null
        && primaryGroupId != null
        && Number(this.form.id) === Number(primaryGroupId)
    },
    editorKey() {
      return this.form.id ? `group-${this.form.id}` : 'new-group'
    },
    dataScopeLabel() {
      return this.form.dataScopeType === 'BOARD_MODERATOR' ? '按版主任职' : '全局'
    },
    autoScopeType() {
      const hasAdmin = this.selectedCodes.some(code => ADMIN_CODE_SET.has(code))
      const hasFront = this.selectedCodes.some(code => FRONT_CODE_SET.has(code))
      if (hasAdmin && hasFront) return 'ALL'
      if (hasFront) return 'FRONT'
      return 'ADMIN'
    },
    autoScopeLabel() {
      if (!this.selectedCodes.length) return '未选择权限点'
      return { ADMIN: '管理端', FRONT: '前台', ALL: '全部' }[this.autoScopeType]
    },
    currentSignature() {
      return JSON.stringify({
        id: this.form.id,
        groupCode: this.form.groupCode,
        groupName: this.form.groupName,
        description: this.form.description,
        dataScopeType: this.form.dataScopeType,
        enabled: this.form.enabled,
        permissionCodes: this.selectedCodes.slice().sort()
      })
    },
    isDirty() {
      return this.showEditor && this.currentSignature !== this.baselineSignature
    }
  },
  beforeRouteLeave(to, from, next) {
    if (!this.isDirty) {
      next()
      return
    }
    this.$confirm('当前权限组有未保存的修改，离开后会丢失。', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '仍然离开',
      cancelButtonText: '留在此页'
    }).then(() => next()).catch(() => next(false))
  },
  mounted() {
    this.loadGroups()
    this.loadPresets()
  },
  methods: {
    permissionLabel(code, fallback) {
      return labelOf(PERMISSION_LABELS, code, fallback || code)
    },
    isDefaultCode(code) {
      return DEFAULT_GROUP_CODES.indexOf(code) !== -1
    },
    isChecked(code) {
      return this.selectedCodes.indexOf(code) !== -1
    },
    isCriticalCode(code) {
      return CRITICAL_PERMISSION_CODES.indexOf(code) !== -1
    },
    isLockedCode(code) {
      return this.isOwnPrimaryGroup && this.isCriticalCode(code)
    },
    toggleCode(code) {
      if (this.isLockedCode(code)) return
      const index = this.selectedCodes.indexOf(code)
      if (index === -1) this.selectedCodes.push(code)
      else this.selectedCodes.splice(index, 1)
    },
    setCodes(codes, checked) {
      codes.forEach(code => {
        if (!checked && this.isLockedCode(code)) return
        const index = this.selectedCodes.indexOf(code)
        if (checked && index === -1) this.selectedCodes.push(code)
        if (!checked && index !== -1) this.selectedCodes.splice(index, 1)
      })
    },
    sectionCheckedCount(section) {
      return section.items.filter(item => this.isChecked(item.code)).length
    },
    isSectionAllChecked(section) {
      return section.items.length > 0 && this.sectionCheckedCount(section) === section.items.length
    },
    isSectionIndeterminate(section) {
      const count = this.sectionCheckedCount(section)
      return count > 0 && count < section.items.length
    },
    toggleSection(section) {
      const checked = !this.isSectionAllChecked(section)
      this.setCodes(section.items.map(item => item.code), checked)
    },
    permissionGroupItems(group) {
      return group.sections.reduce((items, section) => items.concat(section.items), [])
    },
    isPermissionGroupAllChecked(group) {
      const items = this.permissionGroupItems(group)
      return items.length > 0 && items.every(item => this.isChecked(item.code))
    },
    togglePermissionGroup(group) {
      const checked = !this.isPermissionGroupAllChecked(group)
      this.setCodes(this.permissionGroupItems(group).map(item => item.code), checked)
    },
    clearSelection() {
      const removableCount = this.selectedCodes.filter(code => !this.isLockedCode(code)).length
      if (!removableCount) {
        this.$message.warning('当前账号主组的关键权限不可清空')
        return
      }
      this.$confirm(`清空已选的 ${removableCount} 个权限点？`, '清空权限点', {
        type: 'warning',
        confirmButtonText: '清空',
        cancelButtonText: '取消'
      }).then(() => {
        this.selectedCodes = this.selectedCodes.filter(code => this.isLockedCode(code))
      }).catch(() => {})
    },
    guardUnsaved(message) {
      if (!this.isDirty) return Promise.resolve()
      return this.$confirm(message, '未保存的修改', {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消'
      })
    },
    // ── 数据加载 ──────────────────────────────────────────────

    loadGroups(autoSelect = true) {
      this.loadingGroups = true
      this.getRequestUrl('/admin/permission/groups').then(resp => {
        if (resp && resp.code === 200) {
          this.groups = resp.obj || []
          if (!autoSelect) return
          if (this.showEditor && this.form.id && !this.groups.some(group => group.id === this.form.id)) {
            this.showEditor = false
          }
          if (!this.showEditor && this.groups.length) {
            this.loadGroup(this.groups[0].id)
          }
        }
      }).catch(() => {}).then(() => {
        this.loadingGroups = false
      })
    },
    loadPresets() {
      this.getRequestUrl('/admin/permission/presets').then(resp => {
        if (resp && resp.code === 200) this.presets = resp.obj || []
      }).catch(() => {})
    },
    loadGroup(id) {
      if (!id) return
      this.loadingGroup = true
      return this.getRequestUrl(`/admin/permission/groups/${id}`).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          const data = resp.obj
          this.form = {
            id: data.id,
            groupCode: data.groupCode,
            groupName: data.groupName,
            description: data.description || '',
            dataScopeType: normalizeDataScope(data.dataScopeType),
            enabled: data.enabled == null ? 1 : data.enabled,
            sortOrder: data.sortOrder == null ? 0 : data.sortOrder,
            primaryUserCount: data.primaryUserCount || 0,
            memberCount: data.memberCount || 0
          }
          this.selectedCodes = data.permissionCodes || []
          this.groupUsers = data.users || []
          this.originalGroupCode = data.groupCode || ''
          this.showEditor = true
          this.$nextTick(() => {
            this.baselineSignature = this.currentSignature
          })
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      }).catch(() => {}).then(() => {
        this.loadingGroup = false
      })
    },
    requestSelectGroup(group) {
      if (!group || !group.id || group.id === this.form.id) return
      this.guardUnsaved('当前修改尚未保存，切换权限组后会丢失。').then(() => {
        this.loadGroup(group.id)
      }).catch(() => {})
    },
    newGroup() {
      this.guardUnsaved('当前修改尚未保存，新建权限组后会丢失。').then(() => {
        this.startNewGroup()
      }).catch(() => {})
    },
    startNewGroup() {
      this.form = emptyForm(this.groups.length)
      this.originalGroupCode = ''
      this.selectedCodes = []
      this.groupUsers = []
      this.assignUserId = null
      this.presetName = ''
      this.showEditor = true
      this.activeTab = 'basic'
      this.$nextTick(() => {
        this.baselineSignature = this.currentSignature
      })
    },
    resetChanges() {
      this.$confirm('放弃当前未保存的修改？', '放弃修改', {
        type: 'warning',
        confirmButtonText: '放弃修改',
        cancelButtonText: '取消'
      }).then(() => {
        if (this.form.id) {
          this.loadGroup(this.form.id)
        } else {
          this.startNewGroup()
        }
      }).catch(() => {})
    },
    // ── 保存 / 删除 ───────────────────────────────────────────

    saveGroup() {
      if (!this.form.groupCode || !this.form.groupName) {
        this.$message.warning('编码和名称不能为空')
        return
      }
      if (this.saving) return
      this.saving = true
      const isCreate = !this.form.id
      const body = {
        groupCode: this.form.groupCode,
        groupName: this.form.groupName,
        description: this.form.description,
        dataScopeType: this.form.dataScopeType,
        enabled: this.form.enabled,
        sortOrder: this.form.sortOrder,
        permissionCodes: this.selectedCodes
      }
      const request = isCreate
        ? this.postRequest('/admin/permission/groups', body)
        : this.putRequest(`/admin/permission/groups/${this.form.id}`, body)
      request.then(resp => {
        if (resp && resp.code === 200) {
          this.$message.success(isCreate ? '创建成功' : '保存成功')
          const id = isCreate ? resp.obj : this.form.id
          this.loadGroups(false)
          if (id) this.loadGroup(id)
          else this.loadGroups()
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      }).catch(() => {}).then(() => {
        this.saving = false
      })
    },
    deleteGroup() {
      if (!this.form.id) return
      const isDefault = this.isDefaultGroup
      const message = isDefault
        ? `「${this.form.groupName}」是系统默认权限组。删除后原主组用户会自动迁移到普通用户组，该组的权限配置无法恢复。`
        : '确定删除该权限组？主组用户会自动迁移到普通用户组，叠加成员绑定会一并解除。'
      this.$confirm(message, isDefault ? '高风险操作' : '提示', {
        type: isDefault ? 'error' : 'warning',
        confirmButtonText: isDefault ? '继续删除' : '确定',
        cancelButtonText: '取消'
      }).then(() => {
        if (!isDefault) {
          return this.performDelete()
        }
        return this.$confirm(
          `请再次确认：删除默认组「${this.form.groupName}（${this.originalGroupCode || this.form.groupCode}）」？`,
          '再次确认',
          { type: 'error', confirmButtonText: '仍要删除', cancelButtonText: '取消' }
        ).then(() => this.performDelete())
      }).catch(() => {})
    },
    performDelete() {
      return this.deleteRequest(`/admin/permission/groups/${this.form.id}`).then(resp => {
        if (resp && resp.code === 200) {
          this.$message.success('已删除')
          this.showEditor = false
          this.form = emptyForm()
          this.originalGroupCode = ''
          this.selectedCodes = []
          this.groupUsers = []
          this.baselineSignature = ''
          this.loadGroups()
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      })
    },
    // ── 叠加成员 ──────────────────────────────────────────────

    assignUser() {
      const userId = Number(this.assignUserId)
      if (!userId) {
        this.$message.warning('请先搜索并选择用户')
        return
      }
      if (!this.form.id) return
      if (this.groupUsers.some(user => Number(user.userId) === userId)) {
        this.$message.warning('该用户已经在叠加成员中')
        return
      }
      this.postRequest(`/admin/permission/groups/${this.form.id}/users`, { userIds: [userId] }).then(resp => {
        if (resp && resp.code === 200) {
          this.assignUserId = null
          this.refreshMembersOnly()
          this.loadGroups(false)
          this.$message.success('已添加叠加成员')
        } else if (resp && resp.message) {
          this.$message.error(resp.message)
        }
      }).catch(() => {})
    },
    removeUser(userId) {
      this.$confirm('确定移除该叠加成员？', '提示', { type: 'warning' }).then(() => {
        this.deleteRequest(`/admin/permission/groups/${this.form.id}/users/${userId}`).then(resp => {
          if (resp && resp.code === 200) {
            this.refreshMembersOnly()
            this.loadGroups(false)
            this.$message.success('已移除')
          } else if (resp && resp.message) {
            this.$message.error(resp.message)
          }
        }).catch(() => {})
      }).catch(() => {})
    },
    /** 只刷新成员相关字段，保留未保存的权限/基本信息修改。 */
    refreshMembersOnly() {
      if (!this.form.id) return
      this.getRequestUrl(`/admin/permission/groups/${this.form.id}`).then(resp => {
        if (resp && resp.code === 200 && resp.obj) {
          this.groupUsers = resp.obj.users || []
          this.form.memberCount = resp.obj.memberCount || 0
          this.form.primaryUserCount = resp.obj.primaryUserCount || 0
        }
      }).catch(() => {})
    },
    // ── 预设 ──────────────────────────────────────────────────

    savePreset() {
      if (!this.form.id) {
        this.$message.warning('请先选择权限组')
        return
      }
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
      }).catch(() => {})
    },
    applyPreset(preset) {
      if (!this.form.id) {
        this.$message.warning('请先选择权限组')
        return
      }
      this.$confirm(`应用预设「${preset.presetName}」会覆盖当前组的名称、描述和权限点，是否继续？`, '应用预设', {
        type: 'warning',
        confirmButtonText: '应用',
        cancelButtonText: '取消'
      }).then(() => {
        this.postRequest(`/admin/permission/presets/${preset.id}/apply?groupId=${this.form.id}`, {}).then(resp => {
          if (resp && resp.code === 200) {
            this.$message.success('预设已应用')
            this.loadGroup(this.form.id)
            this.loadGroups(false)
          } else if (resp && resp.message) {
            this.$message.error(resp.message)
          }
        }).catch(() => {})
      }).catch(() => {})
    },
    deletePreset(preset) {
      if (!preset || !preset.id) return
      this.$confirm(`确定删除预设「${preset.presetName}」？`, '提示', { type: 'warning' }).then(() => {
        this.deleteRequest(`/admin/permission/presets/${preset.id}`).then(resp => {
          if (resp && resp.code === 200) {
            if (this.expandedPresetId === preset.id) this.expandedPresetId = null
            this.$message.success('预设已删除')
            this.loadPresets()
          } else if (resp && resp.message) {
            this.$message.error(resp.message)
          }
        }).catch(() => {})
      }).catch(() => {})
    },
    togglePreset(preset) {
      if (!preset || !preset.id) return
      this.expandedPresetId = this.expandedPresetId === preset.id ? null : preset.id
    },
    presetDataScopeLabel(value) {
      if (!value) return '—'
      return value === 'BOARD_MODERATOR' ? '按版主任职' : '全局'
    },
    presetScopeLabel(value) {
      return { ADMIN: '管理端', FRONT: '前台', ALL: '全部' }[value] || '—'
    }
  }
}
</script>

<style scoped>
.group-fade-enter-active,
.group-fade-leave-active {
  transition: opacity 0.16s ease;
}

.group-fade-enter,
.group-fade-leave-to {
  opacity: 0;
}
</style>
