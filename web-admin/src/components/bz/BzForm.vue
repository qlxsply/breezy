<template>
  <form
    class="bz-form"
    :class="{ 'is-inline': inline }"
    @submit="onSubmit"
  >
    <slot />
  </form>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzForm",
});

withDefaults(
  defineProps<{
    inline?: boolean;
  }>(),
  {
    inline: false,
  },
);

const emit = defineEmits<{
  (e: "submit", event: SubmitEvent): void;
}>();

function onSubmit(event: SubmitEvent) {
  emit("submit", event);
}
</script>

<style scoped>
.bz-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bz-form.is-inline {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px;
}
</style>
