import "@shared/styles/theme.css";
import "@admin/styles/admin-page.css";
import "@shared/styles/bz-ui.css";
import "@shared/styles/list-page.css";

import App from "@admin/App.vue";
import router from "@admin/router";
import { BzUi } from "@shared/components/bz";
import type { Plugin } from "vue";
import { createApp } from "vue";

createApp(App)
  .use(router)
  .use(BzUi as Plugin)
  .mount("#app");
