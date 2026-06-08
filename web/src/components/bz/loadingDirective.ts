import type { Directive, DirectiveBinding } from "vue";

interface HTMLElementWithLoading extends HTMLElement {
  __bzLoadingMask?: HTMLDivElement;
}

function ensureMask(el: HTMLElementWithLoading): HTMLDivElement {
  if (el.__bzLoadingMask) {
    return el.__bzLoadingMask;
  }
  const mask = document.createElement("div");
  mask.className = "bz-loading-mask";
  mask.innerHTML = '<span class="bz-loading-spinner"></span>';
  el.__bzLoadingMask = mask;
  return mask;
}

function update(el: HTMLElementWithLoading, binding: DirectiveBinding<boolean>) {
  const shouldShow = binding.value === true;
  const mask = ensureMask(el);

  if (shouldShow) {
    const style = window.getComputedStyle(el);
    if (style.position === "static") {
      el.style.position = "relative";
    }
    if (!el.contains(mask)) {
      el.appendChild(mask);
    }
    return;
  }

  if (mask.parentElement === el) {
    el.removeChild(mask);
  }
}

export const bzLoadingDirective: Directive<HTMLElementWithLoading, boolean> = {
  mounted(el, binding) {
    update(el, binding);
  },
  updated(el, binding) {
    update(el, binding);
  },
  unmounted(el) {
    if (el.__bzLoadingMask && el.__bzLoadingMask.parentElement === el) {
      el.removeChild(el.__bzLoadingMask);
    }
    delete el.__bzLoadingMask;
  },
};
