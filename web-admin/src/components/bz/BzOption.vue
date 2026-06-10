<template>
  <option
    :value="optionValue"
    :disabled="disabled"
  >
    <slot>{{ label }}</slot>
  </option>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzOption",
});

const props = withDefaults(
  defineProps<{
    label?: string;
    value?: string | number | null;
    disabled?: boolean;
  }>(),
  {
    label: "",
    value: "",
    disabled: false,
  },
);

const optionValue = computed(() => String(props.value ?? ""));
</script>
