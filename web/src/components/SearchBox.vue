<!-- /src/components/SearchBox.vue -->
<template>
  <div class="search-area">
    <div
      class="search-box"
      :class="{ open }"
    >
      <input
        ref="inputRef"
        class="search-input"
        type="text"
        :value="modelValue"
        :placeholder="displayPlaceholder"
        @input="onInput"
        @focus="$emit('focus')"
        @keydown="$emit('keydown', $event)"
      />

      <!-- 模式徽章 -->
      <div
        class="mode-badge"
        :class="badgeClass"
      >
        {{ badgeLabel }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> 语法糖，顶层即 setup()。
import { computed, onMounted, ref } from "vue";

// defineProps<T>()：用泛型声明 props 形状。
const props = defineProps<{
  modelValue: string;
  placeholder?: string;

  // 徽章展示（由 composable 计算后传入）
  badgeLabel: string;
  badgeClass: string;

  // 是否打开面板（用于焦点样式等）
  open: boolean;

  // 是否需要自动聚焦（首页进入时用）
  autoFocus?: boolean;
}>();

// defineEmits<T>()：用函数签名定义事件与参数类型。
const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
  (e: "focus"): void;
  (e: "keydown", ev: KeyboardEvent): void;
  (e: "mountedInput", el: HTMLInputElement): void;
}>();

// ref<T>()：声明 DOM 引用类型。
const inputRef = ref<HTMLInputElement | null>(null);

// computed 返回只读 ref，?? 是空值合并运算符。
const displayPlaceholder = computed(() => props.placeholder ?? "输入指令或名称...");

function onInput(e: Event) {
  // 类型断言：告诉 TS 这是 HTMLInputElement。
  const v = (e.target as HTMLInputElement).value;
  emit("update:modelValue", v);
}

onMounted(() => {
  if (inputRef.value) {
    emit("mountedInput", inputRef.value);
    // 可选布尔属性：只有为 true 才执行。
    if (props.autoFocus) inputRef.value.focus();
  }
});
</script>

<style scoped>
.search-area {
  width: 100%;
  max-width: 900px;
  padding: 0 20px;
  position: relative;
}

.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 4px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.1);
}

.search-input {
  flex: 1;
  padding: 12px 18px;
  font-size: 18px;
  border: none;
  outline: none;
  background: transparent;
}

/* 徽章 */
.mode-badge {
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  margin-right: 8px;
}

.mode-badge.setting {
  background: #fff7ed;
  color: #f97316;
}

.mode-badge.info {
  background: #f5f3ff;
  color: #7c3aed;
}

.mode-badge.tool {
  background: #f0fdf4;
  color: #16a34a;
}
</style>
