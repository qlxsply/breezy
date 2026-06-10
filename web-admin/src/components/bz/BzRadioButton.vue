<template>
  <button
    class="bz-radio-button"
    :class="{ 'is-active': checked, 'is-disabled': mergedDisabled }"
    type="button"
    :disabled="mergedDisabled"
    @click="select"
  >
    <slot>{{ labelText }}</slot>
  </button>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";

import { bzRadioGroupContextKey } from "./radioContext";

defineOptions({
  name: "BzRadioButton",
});

const props = defineProps<{
  label: unknown;
  disabled?: boolean;
}>();

const group = inject(bzRadioGroupContextKey, null);

const mergedDisabled = computed(() => props.disabled || group?.disabled === true);
const checked = computed(() => group?.isChecked(props.label) === true);
const labelText = computed(() => (typeof props.label === "string" ? props.label : ""));

function select() {
  if (!group || mergedDisabled.value) {
    return;
  }
  group.update(props.label);
}
</script>

<style scoped>
.bz-radio-button {
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  color: var(--text-main);
  font-size: 13px;
  cursor: pointer;
}

.bz-radio-button.is-active {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.bz-radio-button.is-disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
