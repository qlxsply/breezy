<template>
  <label
    class="bz-switch"
    :class="{ 'is-checked': modelValue, 'is-disabled': disabled }"
  >
    <input
      class="bz-switch__input"
      type="checkbox"
      :checked="modelValue"
      :disabled="disabled"
      @change="onChange"
    />
    <span class="bz-switch__core"></span>
    <span
      v-if="activeText || inactiveText"
      class="bz-switch__text"
      >{{ modelValue ? activeText : inactiveText }}</span
    >
  </label>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzSwitch",
});

withDefaults(
  defineProps<{
    modelValue: boolean;
    disabled?: boolean;
    activeText?: string;
    inactiveText?: string;
  }>(),
  {
    disabled: false,
    activeText: "",
    inactiveText: "",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "change", value: boolean): void;
}>();

function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  emit("update:modelValue", target.checked);
  emit("change", target.checked);
}
</script>

<style scoped>
.bz-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.bz-switch__input {
  display: none;
}

.bz-switch__core {
  width: 38px;
  height: 22px;
  border-radius: 999px;
  background: #cbd5e1;
  position: relative;
  transition: background-color 0.16s ease;
}

.bz-switch__core::after {
  content: "";
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.16s ease;
}

.bz-switch.is-checked .bz-switch__core {
  background: #3b82f6;
}

.bz-switch.is-checked .bz-switch__core::after {
  transform: translateX(16px);
}

.bz-switch__text {
  font-size: 13px;
  color: var(--text-main);
}

.bz-switch.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
