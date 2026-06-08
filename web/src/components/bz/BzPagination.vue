<template>
  <div class="bz-pagination">
    <div class="bz-pagination__size">
      <span>每页</span>
      <select
        class="bz-pagination__size-select"
        :value="pageSize"
        @change="onSizeChange"
      >
        <option
          v-for="size in pageSizes"
          :key="size"
          :value="size"
        >
          {{ size }}
        </option>
      </select>
      <span>项</span>
    </div>

    <div class="bz-pagination__pages">
      <button
        class="bz-page-btn"
        type="button"
        :disabled="isFirstPage"
        @click="goToPage(1)"
      >
        首页
      </button>
      <button
        class="bz-page-btn"
        type="button"
        :disabled="isFirstPage"
        @click="goToPage(currentPage - 1)"
      >
        上一页
      </button>

      <template
        v-for="(token, tokenIndex) in pageTokens"
        :key="`${String(token)}-${tokenIndex}`"
      >
        <button
          v-if="typeof token === 'number'"
          class="bz-page-btn"
          :class="{ 'is-active': token === currentPage }"
          type="button"
          @click="goToPage(token)"
        >
          {{ token }}
        </button>
        <span
          v-else
          class="bz-page-ellipsis"
          >...</span
        >
      </template>

      <button
        class="bz-page-btn"
        type="button"
        :disabled="isLastPage"
        @click="goToPage(currentPage + 1)"
      >
        下一页
      </button>
      <button
        class="bz-page-btn"
        type="button"
        :disabled="isLastPage"
        @click="goToPage(totalPages)"
      >
        末页
      </button>
    </div>

    <form
      class="bz-pagination__jump"
      @submit.prevent="goToJumpPage"
    >
      <span>跳转</span>
      <input
        v-model="jumpValue"
        class="bz-pagination__jump-input"
        type="number"
        min="1"
        :max="totalPages"
      />
      <span>页</span>
      <button
        class="bz-page-btn"
        type="submit"
      >
        确定
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

defineOptions({
  name: "BzPagination",
});

const props = withDefaults(
  defineProps<{
    total: number;
    pageSize: number;
    currentPage: number;
    pageSizes?: number[];
  }>(),
  {
    pageSizes: () => [10, 20, 50, 100],
  },
);

const emit = defineEmits<{
  (e: "current-change", pageNo: number): void;
  (e: "size-change", pageSize: number): void;
}>();

const totalPages = computed(() => {
  if (props.total <= 0) {
    return 1;
  }
  return Math.max(1, Math.ceil(props.total / props.pageSize));
});

const isFirstPage = computed(() => props.currentPage <= 1);
const isLastPage = computed(() => props.currentPage >= totalPages.value);

const jumpValue = ref(String(props.currentPage));

watch(
  () => props.currentPage,
  (value) => {
    jumpValue.value = String(value);
  },
);

const pageTokens = computed<Array<number | "ellipsis">>(() => {
  const total = totalPages.value;
  const current = Math.min(Math.max(props.currentPage, 1), total);
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }

  if (current <= 4) {
    return [1, 2, 3, 4, 5, "ellipsis", total];
  }

  if (current >= total - 3) {
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  }

  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
});

function goToPage(pageNo: number) {
  const clamped = Math.min(Math.max(pageNo, 1), totalPages.value);
  if (clamped === props.currentPage) {
    return;
  }
  emit("current-change", clamped);
}

function onSizeChange(event: Event) {
  const target = event.target as HTMLSelectElement;
  const nextPageSize = Number(target.value);
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0) {
    return;
  }
  emit("size-change", nextPageSize);
}

function goToJumpPage() {
  const raw = Number(jumpValue.value);
  if (!Number.isFinite(raw)) {
    return;
  }
  goToPage(Math.trunc(raw));
}
</script>

<style scoped>
.bz-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.bz-pagination__size,
.bz-pagination__jump {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.bz-pagination__size-select,
.bz-pagination__jump-input {
  height: 30px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 0 8px;
  font-size: 13px;
  color: var(--text-main);
  background: #fff;
}

.bz-pagination__jump-input {
  width: 68px;
}

.bz-pagination__pages {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.bz-page-btn {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  color: var(--text-main);
  cursor: pointer;
  font-size: 12px;
}

.bz-page-btn:hover:not(:disabled) {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.bz-page-btn.is-active {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.bz-page-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.bz-page-ellipsis {
  color: #94a3b8;
  font-size: 12px;
}
</style>
