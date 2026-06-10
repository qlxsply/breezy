<template>
  <section
    class="bz-card"
    :class="[`bz-card--${shadow}`]"
  >
    <header
      v-if="$slots.header"
      class="bz-card__header"
    >
      <slot name="header" />
    </header>
    <div class="bz-card__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzCard",
});

withDefaults(
  defineProps<{
    shadow?: "always" | "hover" | "never";
  }>(),
  {
    shadow: "always",
  },
);
</script>

<style scoped>
.bz-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.bz-card--always {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
}

.bz-card--hover {
  transition: box-shadow 0.2s ease;
}

.bz-card--hover:hover {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.09);
}

.bz-card--never {
  box-shadow: none;
}

.bz-card__header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.bz-card__body {
  padding: 16px;
}
</style>
