<!-- /src/components/resources-admin/ResourceFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="mode === 'create' ? '新增资源' : '编辑资源'"
    width="720px"
    @close="$emit('close')"
  >
    <bz-form label-position="top">
      <div class="form-grid">
        <bz-form-item
          label="名称 *"
          class="span-2"
        >
          <bz-input
            v-model="m.name"
            placeholder="例如：JSON 格式化 / 资源管理"
            :disabled="isLocked"
          />
        </bz-form-item>

        <bz-form-item label="图标">
          <bz-input
            v-model="m.icon"
            placeholder="例如：🧭"
            :disabled="isLocked"
          />
        </bz-form-item>

        <bz-form-item label="描述">
          <bz-input
            v-model="m.description"
            placeholder="一句话描述"
            :disabled="isLocked"
          />
        </bz-form-item>

        <bz-form-item label="类型 *">
          <bz-select
            v-model="m.type"
            :disabled="isLocked"
          >
            <bz-option
              label="MENU（菜单）"
              value="MENU"
            />
            <bz-option
              label="BUTTON（按钮）"
              value="BUTTON"
            />
            <bz-option
              label="FEATURE（功能）"
              value="FEATURE"
            />
            <bz-option
              label="DATA（数据）"
              value="DATA"
            />
          </bz-select>
        </bz-form-item>

        <bz-form-item
          v-if="m.type === 'MENU'"
          label="入口范围 *"
        >
          <bz-select
            v-model="m.scope"
            :disabled="isLocked"
          >
            <bz-option
              label="SETTING（设置搜索）"
              value="SETTING"
            />
            <bz-option
              label="NONE（不进入搜索）"
              value="NONE"
            />
          </bz-select>
        </bz-form-item>

        <bz-form-item
          label="父级资源"
          class="span-2"
        >
          <bz-select
            v-model="m.parentId"
            :disabled="isLocked"
            clearable
          >
            <bz-option
              value=""
              :disabled="m.type !== 'MENU'"
              label="无（根节点）"
            />
            <bz-option
              v-for="p in parentOptions"
              :key="p.id"
              :value="p.id"
              :label="`${p.name} (${p.code})`"
            />
          </bz-select>
          <div class="tip">按钮/功能建议挂在页面级菜单下</div>
          <div
            v-if="parentDisabledWarning"
            class="tip warn"
          >
            父级已停用，子资源启用可能无效
          </div>
        </bz-form-item>

        <bz-form-item
          label="快捷编码 *"
          class="span-2"
        >
          <bz-input
            v-model="m.code"
            class="mono"
            placeholder="例如：res / usr.add"
            :disabled="isLocked"
          />
          <div class="tip">用于授权识别与搜索的唯一编码</div>
        </bz-form-item>

        <bz-form-item label="打开方式 *">
          <bz-select
            v-model="m.openMode"
            :disabled="isLocked"
          >
            <bz-option
              label="NONE（不跳转）"
              value="NONE"
            />
            <bz-option
              label="MODAL（弹窗）"
              value="MODAL"
            />
            <bz-option
              label="PAGE（页面）"
              value="PAGE"
            />
          </bz-select>
        </bz-form-item>

        <bz-form-item
          v-if="m.openMode === 'PAGE'"
          label="URL *"
        >
          <bz-input
            v-model="m.url"
            class="mono"
            placeholder="例如：/sm 或 /configs"
            :disabled="isLocked"
          />
        </bz-form-item>

        <bz-form-item
          v-if="m.openMode !== 'NONE'"
          label="加载资源 *"
          class="span-2"
        >
          <bz-input
            v-model="m.loadTarget"
            class="mono"
            placeholder="例如：pages/ResourcesAdminPage.vue"
            :disabled="isLocked"
          />
          <div class="tip">组件路径，用于动态 import</div>
        </bz-form-item>

        <bz-form-item label="序号">
          <bz-input-number
            v-model="m.orderNo"
            :min="0"
            :disabled="isLocked"
          />
        </bz-form-item>

        <bz-form-item label="级别 *">
          <bz-select
            v-model="m.level"
            :disabled="isLocked || mode === 'create'"
          >
            <bz-option
              label="SYSTEM（系统内置）"
              value="SYSTEM"
            />
            <bz-option
              label="CUSTOM（自定义）"
              value="CUSTOM"
            />
          </bz-select>
        </bz-form-item>

        <bz-form-item label="启用">
          <bz-switch
            v-model="m.enabled"
            :disabled="isLocked || parentDisabledLock"
            active-text="启用"
            :inactive-text="parentDisabledLock ? '继承停用' : '停用'"
          />
        </bz-form-item>

        <bz-form-item
          v-if="m.type === 'MENU'"
          label="游客可用"
        >
          <bz-switch
            v-model="m.guestAccess"
            :disabled="isLocked"
            active-text="允许"
            inactive-text="禁止"
          />
          <div class="tip">未登录用户可访问该菜单入口</div>
        </bz-form-item>
      </div>
    </bz-form>

    <div
      v-if="isLocked"
      class="tip locked"
    >
      系统级资源不可编辑
    </div>
    <bz-alert
      v-if="err"
      :title="err"
      type="error"
      show-icon
      class="form-error"
    />

    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        type="primary"
        :disabled="isLocked"
        @click="submit"
      >
        {{ mode === "create" ? "创建" : "保存" }}
      </bz-button>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed, reactive, ref, watch } from "vue";

import type { ResourceEntry } from "../../types/resource-admin";

// defineProps<T>()：定义 props 结构。
const props = defineProps<{
  mode: "create" | "edit";
  model: ResourceEntry | null;
  resources: ResourceEntry[];
}>();

// defineEmits<T>()：定义事件签名。
const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", model: ResourceEntry): void;
}>();

// level=SYSTEM 表示系统内置资源，不允许编辑。
const isLocked = computed(() => props.model?.level === "SYSTEM");

/**
 * 用 reactive 复制一份表单数据，避免编辑时直接修改父组件数据
 */
// reactive<T>()：把对象转成响应式，泛型指定对象结构。
const m = reactive<ResourceEntry>({
  id: "",
  parentId: "",
  name: "",
  icon: "",
  description: "",
  code: "",
  type: "MENU",
  scope: "SETTING",
  openMode: "PAGE",
  url: "",
  loadTarget: "",
  orderNo: 100,
  level: "CUSTOM",
  enabled: true,
  guestAccess: false,
});

const err = ref("");

watch(
  () => props.model,
  (v) => {
    if (!v) return;
    // Object.assign 把 v 的字段拷贝到响应式对象 m 上。
    Object.assign(m, { ...v, parentId: v.parentId ?? "" });
  },
  { immediate: true },
);

const resourceMap = computed(() => {
  const map = new Map<string, ResourceEntry>();
  props.resources.forEach((r) => map.set(r.id, r));
  return map;
});

const parentInfo = computed(() => {
  const pid = String(m.parentId ?? "").trim();
  if (!pid) return undefined;
  return resourceMap.value.get(pid);
});

const parentDisabledLock = computed(() => {
  let parent = parentInfo.value;
  while (parent) {
    if (!parent.enabled) return true;
    if (!parent.parentId) return false;
    parent = resourceMap.value.get(parent.parentId);
  }
  return false;
});

watch(parentDisabledLock, (locked) => {
  if (locked) m.enabled = false;
});

const parentDisabledWarning = computed(() => {
  const parent = parentInfo.value;
  if (!parent) return false;
  if (!parentDisabledLock.value) return false;
  return m.enabled;
});

const parentOptions = computed(() => {
  const currentId = props.model?.id;
  return props.resources.filter((r) => {
    if (r.id === currentId) return false;
    if (r.type !== "MENU") return false;
    if (m.type === "MENU") return true;
    return r.openMode === "PAGE";
  });
});

// 返回 string：空字符串表示校验通过。
function validate(): string {
  if (!m.name.trim()) return "名称不能为空";
  if (!m.code.trim()) return "编码不能为空";
  const parentId = String(m.parentId ?? "").trim();
  if (m.type !== "MENU" && !parentId) return "非菜单资源必须选择父级页面";
  if (parentId) {
    const parent = resourceMap.value.get(parentId);
    if (!parent) return "父级资源不存在";
    if (parent.type !== "MENU") return "父级资源必须是菜单类型";
    if (m.type !== "MENU" && parent.openMode !== "PAGE") return "非菜单资源必须挂在页面级菜单下";
  }
  if (m.openMode === "PAGE" && !m.url.trim()) return "PAGE 方式需要填写 URL";
  if (m.openMode !== "NONE" && !m.loadTarget.trim()) return "弹窗或页面必须填写加载资源";
  return "";
}

function submit() {
  if (isLocked.value) return;
  err.value = "";
  const e = validate();
  if (e) {
    err.value = e;
    return;
  }
  const parentId = String(m.parentId ?? "").trim();
  const next: ResourceEntry = { ...m, parentId: parentId ? parentId : null };
  if (next.type !== "MENU") next.scope = "NONE";
  if (next.type !== "MENU") next.guestAccess = false;
  if (next.openMode === "NONE") {
    next.url = "";
    next.loadTarget = "";
  }
  if (parentDisabledLock.value) next.enabled = false;
  // 展开运算符创建新对象，避免直接传出 reactive 引用。
  emit("submit", { ...next });
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.span-2 {
  grid-column: span 2;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.mono :deep(.el-input__inner) {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.tip.warn {
  color: #b45309;
  font-weight: 700;
}

.tip.locked {
  color: #f97316;
  font-weight: 700;
  margin-top: 8px;
}

.form-error {
  margin-top: 8px;
}
</style>
