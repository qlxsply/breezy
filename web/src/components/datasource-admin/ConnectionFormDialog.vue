<!-- /src/components/datasource-admin/ConnectionFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="props.id ? '编辑数据源' : '新增数据源'"
    width="900px"
    @close="$emit('close')"
  >
    <div class="modal-body">
      <div class="form-grid">
        <div class="field full">
          <label>数据源名称 <span class="required">*</span></label>
          <bz-input
            v-model="form.name"
            placeholder="如：测试环境-用户库"
          />
        </div>

        <div class="field">
          <label
            >数据库类型
            <span
              v-if="!isAppSource"
              class="required"
              >*</span
            ></label
          >
          <bz-select
            v-model="form.dbType"
            :disabled="isAppSource"
            @change="handleDbTypeChange"
          >
            <bz-option
              label="MySQL"
              value="MYSQL"
            />
            <bz-option
              label="PostgreSQL"
              value="POSTGRESQL"
            />
            <bz-option
              label="Oracle"
              value="ORACLE"
            />
            <bz-option
              v-if="isAppSource && isUnsupportedDbType"
              :label="formatDbType(form.dbType)"
              :value="form.dbType"
            />
          </bz-select>
        </div>

        <div class="field">
          <label
            >认证方式
            <span
              v-if="!isAppSource"
              class="required"
              >*</span
            ></label
          >
          <bz-select
            v-model="form.authMode"
            :disabled="true"
          >
            <bz-option
              label="用户名 + 密码"
              value="PASSWORD"
            />
          </bz-select>
        </div>

        <template v-if="!isAppSource && supportsStructuredConfig">
          <div class="field">
            <label>连接类型 <span class="required">*</span></label>
            <bz-select
              v-model="form.connectMode"
              :disabled="form.dbType !== 'ORACLE'"
            >
              <bz-option
                v-if="form.dbType !== 'ORACLE'"
                label="主机 + 端口"
                value="HOST_PORT"
              />
              <bz-option
                v-if="form.dbType === 'ORACLE'"
                label="服务名"
                value="SERVICE_NAME"
              />
              <bz-option
                v-if="form.dbType === 'ORACLE'"
                label="SID"
                value="SID"
              />
            </bz-select>
          </div>

          <div class="field">
            <label>驱动程序 <span class="required">*</span></label>
            <bz-input
              v-model="form.driverClassName"
              placeholder="如：com.mysql.cj.jdbc.Driver"
            />
          </div>

          <div class="field host-field">
            <label>主机 <span class="required">*</span></label>
            <bz-input
              v-model="form.host"
              placeholder="如：127.0.0.1"
            />
          </div>

          <div class="field">
            <label>端口 <span class="required">*</span></label>
            <bz-input
              v-model.number="form.port"
              placeholder="如：3306"
            />
          </div>

          <div class="field full">
            <label
              >{{ databaseLabel }}
              <span
                v-if="databaseRequired"
                class="required"
                >*</span
              ></label
            >
            <bz-input
              v-model="form.databaseName"
              :placeholder="databasePlaceholder"
            />
          </div>

          <div
            v-if="showServiceName"
            class="field full"
          >
            <label>服务名 <span class="required">*</span></label>
            <bz-input
              v-model="form.serviceName"
              placeholder="如：orclpdb"
            />
          </div>

          <div
            v-if="showSid"
            class="field full"
          >
            <label>SID <span class="required">*</span></label>
            <bz-input
              v-model="form.sid"
              placeholder="如：orcl"
            />
          </div>

          <div class="field">
            <label>用户名 <span class="required">*</span></label>
            <bz-input
              v-model="form.username"
              :placeholder="usernamePlaceholder"
            />
          </div>

          <div class="field">
            <label>密码 <span class="required">*</span></label>
            <bz-input
              v-model="form.passwordRaw"
              type="password"
              show-password
              placeholder="数据库连接密码"
            />
            <p
              v-if="props.id"
              class="hint"
            >
              若不修改密码，请留空（后端将保留原密码）
            </p>
          </div>

          <div class="field full param-block">
            <label>高级参数（白名单）</label>
            <p class="hint">
              根据数据库类型提供常用参数与可选值。参数版本提示仅作参考，请以官方驱动文档为准。
            </p>

            <div
              v-if="currentParamMetas.length > 0"
              class="param-grid"
            >
              <div
                v-for="meta in currentParamMetas"
                :key="meta.key"
                class="param-item"
              >
                <div class="param-title">{{ meta.label }}</div>
                <bz-select
                  v-if="meta.options"
                  v-model="presetParams[meta.key]"
                  size="small"
                >
                  <bz-option
                    label="不设置"
                    value=""
                  />
                  <bz-option
                    v-for="opt in meta.options"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </bz-select>
                <bz-input
                  v-else
                  v-model="presetParams[meta.key]"
                  size="small"
                  :placeholder="meta.placeholder || '请输入参数值'"
                />
                <div class="param-desc">{{ meta.description }}</div>
                <div
                  v-if="meta.since || meta.until"
                  class="param-ver"
                >
                  生效参考：{{ formatVersionRange(meta.since, meta.until) }}
                </div>
              </div>
            </div>

            <div class="custom-param-list">
              <div class="custom-param-head">自定义参数</div>
              <div
                v-for="(item, index) in customParams"
                :key="`custom-${index}`"
                class="custom-param-row"
              >
                <bz-input
                  v-model="item.key"
                  size="small"
                  placeholder="参数名，如 tinyInt1isBit"
                />
                <bz-input
                  v-model="item.value"
                  size="small"
                  placeholder="参数值，如 false"
                />
                <bz-button
                  size="small"
                  text
                  type="danger"
                  @click="removeCustomParam(index)"
                  >移除</bz-button
                >
              </div>
              <bz-button
                size="small"
                @click="addCustomParam"
                >添加自定义参数</bz-button
              >
            </div>
          </div>

          <div class="field full">
            <label>JDBC URL（自动拼接）</label>
            <bz-input
              :model-value="effectiveJdbcUrl"
              type="textarea"
              :rows="3"
              readonly
            />
          </div>
        </template>

        <template v-else-if="!isAppSource">
          <div class="field full">
            <label>JDBC URL</label>
            <bz-input
              :model-value="form.jdbcUrl"
              type="textarea"
              :rows="3"
              readonly
            />
            <p class="hint error">当前数据库类型暂不支持结构化配置，仅允许查看。</p>
          </div>
        </template>

        <template v-else>
          <div class="field full">
            <label>应用数据源 JDBC URL</label>
            <bz-input
              :model-value="form.jdbcUrl"
              type="textarea"
              :rows="3"
              readonly
            />
            <p class="hint">应用数据源由系统自动同步，连接配置不可编辑。</p>
          </div>

          <div class="field full">
            <label>用户名</label>
            <bz-input
              v-model="form.username"
              disabled
            />
          </div>
        </template>

        <div class="field full">
          <label>自定义备注</label>
          <bz-input
            v-model="form.remarkCustom"
            placeholder="选填：数据源用途描述"
            :disabled="isAppSource"
          />
        </div>
      </div>

      <div
        v-if="testResult"
        :class="['test-result', testResult.success ? 'success' : 'failed']"
      >
        <div class="res-title">{{ testResult.success ? "连接测试成功" : "连接测试失败" }}</div>
        <div
          v-if="testResult.message"
          class="res-msg"
        >
          {{ testResult.message }}
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <div class="left">
          <bz-button
            :disabled="testing || isAppSource"
            @click="handleTest"
          >
            {{ isAppSource ? "应用数据源无需测试" : testing ? "测试中…" : "测试连接" }}
          </bz-button>
        </div>
        <div class="right">
          <bz-button @click="$emit('close')">取消</bz-button>
          <bz-button
            type="primary"
            :loading="saving"
            @click="handleSave"
          >
            {{ saving ? "保存中…" : "确定保存" }}
          </bz-button>
        </div>
      </div>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import {
  getDatabaseSource,
  testDatabaseSourceConfig,
  upsertDatabaseSource,
} from "../../api/database-source";
import type {
  AuthMode,
  DatabaseSource,
  DatabaseSourceSourceType,
  DatabaseType,
  TestConnectionRes,
  UpsertDatabaseSourceRequest,
} from "../../types/database-source";
import { message } from "../../utils/message";

type EditableDbType = "MYSQL" | "POSTGRESQL" | "ORACLE";

interface ParamOption {
  label: string;
  value: string;
}

interface ParamMeta {
  key: string;
  label: string;
  description: string;
  options?: ParamOption[];
  placeholder?: string;
  since?: string;
  until?: string;
}

interface CustomParamItem {
  key: string;
  value: string;
}

const MYSQL_PARAMS: ParamMeta[] = [
  {
    key: "useUnicode",
    label: "useUnicode",
    description: "是否启用 Unicode 处理",
    options: [
      { label: "true", value: "true" },
      { label: "false", value: "false" },
    ],
    since: "Connector/J 5.1+",
  },
  {
    key: "characterEncoding",
    label: "characterEncoding",
    description: "字符编码，例如 utf-8 / utf8mb4",
    options: [
      { label: "utf-8", value: "utf-8" },
      { label: "utf8mb4", value: "utf8mb4" },
      { label: "utf8", value: "utf8" },
    ],
    since: "Connector/J 5.1+",
  },
  {
    key: "useSSL",
    label: "useSSL",
    description: "是否启用 SSL",
    options: [
      { label: "true", value: "true" },
      { label: "false", value: "false" },
    ],
    since: "Connector/J 5.1+",
  },
  {
    key: "allowPublicKeyRetrieval",
    label: "allowPublicKeyRetrieval",
    description: "允许获取公钥（某些 8.x 场景需要）",
    options: [
      { label: "true", value: "true" },
      { label: "false", value: "false" },
    ],
    since: "Connector/J 8.0+",
  },
  {
    key: "serverTimezone",
    label: "serverTimezone",
    description: "服务端时区",
    options: [
      { label: "UTC", value: "UTC" },
      { label: "Asia/Shanghai", value: "Asia/Shanghai" },
    ],
    since: "Connector/J 5.1.47+",
  },
  {
    key: "rewriteBatchedStatements",
    label: "rewriteBatchedStatements",
    description: "批量写入性能优化",
    options: [
      { label: "true", value: "true" },
      { label: "false", value: "false" },
    ],
    since: "Connector/J 5.1+",
  },
  {
    key: "connectTimeout",
    label: "connectTimeout",
    description: "连接超时（毫秒）",
    placeholder: "如：5000",
    since: "Connector/J 5.1+",
  },
  {
    key: "socketTimeout",
    label: "socketTimeout",
    description: "网络读取超时（毫秒）",
    placeholder: "如：30000",
    since: "Connector/J 5.1+",
  },
];

const POSTGRESQL_PARAMS: ParamMeta[] = [
  {
    key: "sslmode",
    label: "sslmode",
    description: "SSL 模式",
    options: [
      { label: "disable", value: "disable" },
      { label: "allow", value: "allow" },
      { label: "prefer", value: "prefer" },
      { label: "require", value: "require" },
      { label: "verify-ca", value: "verify-ca" },
      { label: "verify-full", value: "verify-full" },
    ],
    since: "42.x+",
  },
  {
    key: "applicationName",
    label: "applicationName",
    description: "客户端应用名",
    placeholder: "如：breezy",
    since: "42.x+",
  },
  {
    key: "currentSchema",
    label: "currentSchema",
    description: "默认 schema",
    placeholder: "如：public",
    since: "42.x+",
  },
  {
    key: "stringtype",
    label: "stringtype",
    description: "字符串类型处理方式",
    options: [
      { label: "unspecified", value: "unspecified" },
      { label: "varchar", value: "varchar" },
    ],
    since: "42.x+",
  },
  {
    key: "connectTimeout",
    label: "connectTimeout",
    description: "连接超时（秒）",
    placeholder: "如：5",
    since: "42.x+",
  },
  {
    key: "socketTimeout",
    label: "socketTimeout",
    description: "读取超时（秒）",
    placeholder: "如：30",
    since: "42.x+",
  },
  {
    key: "prepareThreshold",
    label: "prepareThreshold",
    description: "预编译阈值",
    placeholder: "如：5",
    since: "42.x+",
  },
];

const ORACLE_PARAMS: ParamMeta[] = [
  {
    key: "oracle.net.CONNECT_TIMEOUT",
    label: "oracle.net.CONNECT_TIMEOUT",
    description: "连接超时（毫秒）",
    placeholder: "如：5000",
    since: "ojdbc8+",
  },
  {
    key: "oracle.jdbc.ReadTimeout",
    label: "oracle.jdbc.ReadTimeout",
    description: "读取超时（毫秒）",
    placeholder: "如：30000",
    since: "ojdbc8+",
  },
  {
    key: "defaultRowPrefetch",
    label: "defaultRowPrefetch",
    description: "默认预取行数",
    placeholder: "如：100",
    since: "ojdbc8+",
  },
  {
    key: "remarksReporting",
    label: "remarksReporting",
    description: "元数据备注读取增强",
    options: [
      { label: "true", value: "true" },
      { label: "false", value: "false" },
    ],
    since: "ojdbc8+",
  },
];

const PARAM_CATALOG: Record<EditableDbType, ParamMeta[]> = {
  MYSQL: MYSQL_PARAMS,
  POSTGRESQL: POSTGRESQL_PARAMS,
  ORACLE: ORACLE_PARAMS,
};

const PARAM_DEFAULTS: Record<EditableDbType, Record<string, string>> = {
  MYSQL: {
    useUnicode: "true",
    characterEncoding: "utf-8",
  },
  POSTGRESQL: {},
  ORACLE: {},
};

const DEFAULT_PORT_MAP: Record<EditableDbType, number> = {
  MYSQL: 3306,
  POSTGRESQL: 5432,
  ORACLE: 1521,
};

const DEFAULT_DRIVER_MAP: Record<EditableDbType, string> = {
  MYSQL: "com.mysql.cj.jdbc.Driver",
  POSTGRESQL: "org.postgresql.Driver",
  ORACLE: "oracle.jdbc.OracleDriver",
};

const props = defineProps<{
  id?: string | null;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "saved"): void;
}>();

const saving = ref(false);
const testing = ref(false);
const testResult = ref<TestConnectionRes | null>(null);
const sourceType = ref<DatabaseSourceSourceType>("USER");

const form = reactive<UpsertDatabaseSourceRequest>({
  id: props.id || null,
  name: "",
  dbType: "MYSQL",
  authMode: "PASSWORD",
  connectMode: "HOST_PORT",
  host: "127.0.0.1",
  port: DEFAULT_PORT_MAP.MYSQL,
  databaseName: "",
  serviceName: "",
  sid: "",
  driverClassName: DEFAULT_DRIVER_MAP.MYSQL,
  extraParams: "",
  jdbcUrl: "",
  username: "",
  passwordRaw: "",
  remarkCustom: "",
});

const presetParams = ref<Record<string, string>>({});
const customParams = ref<CustomParamItem[]>([]);

const isAppSource = computed(() => sourceType.value === "APP");
const isUnsupportedDbType = computed(() => !isEditableDbType(form.dbType));
const supportsStructuredConfig = computed(
  () => !isAppSource.value && isEditableDbType(form.dbType),
);

const databaseLabel = computed(() => {
  return form.dbType === "ORACLE" ? "数据库/Schema" : "数据库";
});

const databasePlaceholder = computed(() => {
  return form.dbType === "ORACLE" ? "如：BREEZY" : "如：breezy";
});

const databaseRequired = computed(() => form.dbType !== "ORACLE");

const showServiceName = computed(
  () => form.dbType === "ORACLE" && form.connectMode === "SERVICE_NAME",
);
const showSid = computed(() => form.dbType === "ORACLE" && form.connectMode === "SID");

const usernamePlaceholder = computed(() => {
  if (form.dbType === "POSTGRESQL") {
    return "如：postgres";
  }
  if (form.dbType === "ORACLE") {
    return "如：system";
  }
  return "如：root";
});

const currentParamMetas = computed<ParamMeta[]>(() => {
  if (!supportsStructuredConfig.value || !isEditableDbType(form.dbType)) {
    return [];
  }
  return PARAM_CATALOG[form.dbType];
});

const generatedJdbcUrl = computed(() => {
  if (!supportsStructuredConfig.value || !isEditableDbType(form.dbType)) {
    return (form.jdbcUrl || "").trim();
  }
  if (!isConnectionCoreReady()) {
    return "";
  }

  const host = (form.host || "").trim();
  const port = form.port ? String(form.port).trim() : "";
  const databaseName = (form.databaseName || "").trim();
  const serviceName = (form.serviceName || "").trim();
  const sid = (form.sid || "").trim();
  const query = buildQueryString(form.dbType, presetParams.value, customParams.value);
  const querySuffix = query ? `?${query}` : "";

  if (form.dbType === "MYSQL") {
    return `jdbc:mysql://${host}:${port}/${databaseName}${querySuffix}`;
  }
  if (form.dbType === "POSTGRESQL") {
    return `jdbc:postgresql://${host}:${port}/${databaseName}${querySuffix}`;
  }
  if (form.connectMode === "SID") {
    return `jdbc:oracle:thin:@${host}:${port}:${sid}${querySuffix}`;
  }
  return `jdbc:oracle:thin:@//${host}:${port}/${serviceName}${querySuffix}`;
});

const effectiveJdbcUrl = computed(() => generatedJdbcUrl.value || (form.jdbcUrl || "").trim());

function isEditableDbType(type: DatabaseType): type is EditableDbType {
  return type === "MYSQL" || type === "POSTGRESQL" || type === "ORACLE";
}

function formatDbType(type: DatabaseType): string {
  return {
    H2: "H2",
    MYSQL: "MySQL",
    POSTGRESQL: "PostgreSQL",
    ORACLE: "Oracle",
    SQLSERVER: "SQL Server",
  }[type];
}

function formatVersionRange(since?: string, until?: string): string {
  if (since && until) {
    return `${since} - ${until}`;
  }
  if (since) {
    return `${since} 起`;
  }
  if (until) {
    return `${until} 前`;
  }
  return "未标注";
}

function setDbDefaults(dbType: EditableDbType) {
  form.host = "127.0.0.1";
  form.port = DEFAULT_PORT_MAP[dbType];
  form.databaseName = "";
  form.serviceName = "";
  form.sid = "";
  form.connectMode = dbType === "ORACLE" ? "SERVICE_NAME" : "HOST_PORT";
  form.driverClassName = DEFAULT_DRIVER_MAP[dbType];
  form.authMode = "PASSWORD" as AuthMode;
  form.extraParams = "";
  applyPresetParamValues(dbType);
  customParams.value = [];
}

function applyPresetParamValues(dbType: EditableDbType, source?: Record<string, string>) {
  const next: Record<string, string> = {};
  const defaults = PARAM_DEFAULTS[dbType];
  PARAM_CATALOG[dbType].forEach((meta) => {
    next[meta.key] = source?.[meta.key] ?? defaults[meta.key] ?? "";
  });
  presetParams.value = next;
}

function addCustomParam() {
  customParams.value.push({ key: "", value: "" });
}

function removeCustomParam(index: number) {
  customParams.value.splice(index, 1);
}

function buildQueryString(
  dbType: EditableDbType,
  presetValues: Record<string, string>,
  customItems: CustomParamItem[],
): string {
  const map = new Map<string, string>();

  PARAM_CATALOG[dbType].forEach((meta) => {
    const value = (presetValues[meta.key] || "").trim();
    if (value) {
      map.set(meta.key, value);
    }
  });

  customItems.forEach((item) => {
    const key = item.key.trim();
    const value = item.value.trim();
    if (!key || !value) {
      return;
    }
    map.set(key, value);
  });

  return Array.from(map.entries())
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join("&");
}

function splitQueryString(
  dbType: EditableDbType,
  query: string,
): { preset: Record<string, string>; custom: CustomParamItem[] } {
  const params = new URLSearchParams(query || "");
  const presetKeys = new Set(PARAM_CATALOG[dbType].map((item) => item.key));
  const preset: Record<string, string> = {};
  const custom: CustomParamItem[] = [];

  params.forEach((value, key) => {
    if (presetKeys.has(key)) {
      preset[key] = value;
      return;
    }
    custom.push({ key, value });
  });

  return { preset, custom };
}

function fillStructuredConfigFromData(data: DatabaseSource) {
  if (!isEditableDbType(data.dbType)) {
    return;
  }
  form.authMode = data.authMode || "PASSWORD";
  form.connectMode = data.connectMode || (data.dbType === "ORACLE" ? "SERVICE_NAME" : "HOST_PORT");
  form.host = data.host || "127.0.0.1";
  form.port = data.port ?? DEFAULT_PORT_MAP[data.dbType];
  form.databaseName = data.databaseName || "";
  form.serviceName = data.serviceName || "";
  form.sid = data.sid || "";
  form.driverClassName = data.driverClassName || DEFAULT_DRIVER_MAP[data.dbType];
  form.extraParams = data.extraParams || "";

  const split = splitQueryString(data.dbType, form.extraParams || "");
  applyPresetParamValues(data.dbType, split.preset);
  customParams.value = split.custom;
}

function handleDbTypeChange() {
  if (isAppSource.value || !isEditableDbType(form.dbType)) {
    return;
  }
  setDbDefaults(form.dbType);
}

function isConnectionCoreReady(): boolean {
  const host = (form.host || "").trim();
  const port = form.port;
  if (!host || !port) {
    return false;
  }
  if (form.dbType === "ORACLE") {
    if (form.connectMode === "SID") {
      return Boolean((form.sid || "").trim());
    }
    return Boolean((form.serviceName || "").trim());
  }
  return Boolean((form.databaseName || "").trim());
}

function buildRequestPayload(): UpsertDatabaseSourceRequest | null {
  const name = form.name.trim();
  const username = form.username.trim();
  const remarkCustom = (form.remarkCustom || "").trim();
  const driverClassName = (form.driverClassName || "").trim();
  const host = (form.host || "").trim();
  const port = form.port;
  const databaseName = (form.databaseName || "").trim();
  const serviceName = (form.serviceName || "").trim();
  const sid = (form.sid || "").trim();
  const extraParams =
    supportsStructuredConfig.value && isEditableDbType(form.dbType)
      ? buildQueryString(form.dbType, presetParams.value, customParams.value)
      : (form.extraParams || "").trim();
  form.extraParams = extraParams;
  const jdbcUrl =
    supportsStructuredConfig.value && isEditableDbType(form.dbType)
      ? generatedJdbcUrl.value
      : (form.jdbcUrl || "").trim();

  if (!name) {
    message.error("请填写数据源名称");
    return null;
  }

  if (!isAppSource.value) {
    if (!isEditableDbType(form.dbType)) {
      message.error("当前仅支持 MySQL / PostgreSQL / Oracle");
      return null;
    }
    if (!driverClassName) {
      message.error("请填写驱动程序");
      return null;
    }
    if (!host || !port || !Number.isFinite(port)) {
      message.error("请完善主机和端口");
      return null;
    }
    if (form.dbType === "ORACLE") {
      if (form.connectMode === "SID" && !sid) {
        message.error("请填写 SID");
        return null;
      }
      if (form.connectMode !== "SID" && !serviceName) {
        message.error("请填写服务名");
        return null;
      }
    } else if (!databaseName) {
      message.error("请填写数据库名称");
      return null;
    }
    if (!username) {
      message.error("请填写用户名");
      return null;
    }
    if (!jdbcUrl) {
      message.error("JDBC URL 生成失败，请检查连接参数");
      return null;
    }
  }

  const payload: UpsertDatabaseSourceRequest = {
    id: form.id,
    name,
    dbType: form.dbType,
    authMode: form.authMode || "PASSWORD",
    connectMode: form.connectMode || (form.dbType === "ORACLE" ? "SERVICE_NAME" : "HOST_PORT"),
    host,
    port: Number.isFinite(port) ? port : null,
    databaseName,
    serviceName,
    sid,
    driverClassName,
    extraParams,
    jdbcUrl,
    username,
    passwordRaw: form.passwordRaw,
    remarkCustom,
  };
  return payload;
}

async function loadData() {
  if (!props.id) {
    setDbDefaults("MYSQL");
    return;
  }
  try {
    const data = await getDatabaseSource(props.id);
    form.name = data.name;
    form.dbType = data.dbType;
    form.authMode = data.authMode || "PASSWORD";
    form.connectMode =
      data.connectMode || (data.dbType === "ORACLE" ? "SERVICE_NAME" : "HOST_PORT");
    form.host = data.host || "127.0.0.1";
    form.port =
      data.port ?? (isEditableDbType(data.dbType) ? DEFAULT_PORT_MAP[data.dbType] : form.port);
    form.databaseName = data.databaseName || "";
    form.serviceName = data.serviceName || "";
    form.sid = data.sid || "";
    form.driverClassName =
      data.driverClassName ||
      (isEditableDbType(data.dbType) ? DEFAULT_DRIVER_MAP[data.dbType] : "");
    form.extraParams = data.extraParams || "";
    form.jdbcUrl = data.jdbcUrl;
    form.username = data.username;
    form.remarkCustom = data.remarkCustom || "";
    form.passwordRaw = "";
    sourceType.value = data.sourceType;

    if (!isAppSource.value && isEditableDbType(data.dbType)) {
      fillStructuredConfigFromData(data);
    }
  } catch (_e) {
    setDbDefaults("MYSQL");
  }
}

async function handleTest() {
  if (isAppSource.value) {
    message.info("应用数据源无需测试");
    return;
  }
  const payload = buildRequestPayload();
  if (!payload) {
    return;
  }

  if (!payload.id && !payload.passwordRaw) {
    message.error("新增数据源必须填写密码");
    return;
  }

  testing.value = true;
  testResult.value = null;
  try {
    const res = await testDatabaseSourceConfig(payload);
    testResult.value = res;
    if (res.success) {
      message.success("测试连接成功");
    } else {
      message.error("测试连接失败");
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    testResult.value = { success: false, message: err.message || "测试失败" };
    message.error("测试连接失败");
  } finally {
    testing.value = false;
  }
}

async function handleSave() {
  const payload = buildRequestPayload();
  if (!payload) {
    return;
  }
  if (!isAppSource.value && !payload.id && !payload.passwordRaw) {
    message.error("新增数据源必须填写密码");
    return;
  }

  saving.value = true;
  try {
    await upsertDatabaseSource(payload);
    message.success("保存成功");
    emit("saved");
  } catch (_e) {
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.modal-body {
  padding: 18px 24px 20px;
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px 16px;
}

.form-grid > * {
  min-width: 0;
}

.field.full {
  grid-column: 1 / span 2;
}

.host-field {
  grid-column: 1;
}

.field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-main);
}

.field :deep(.el-input),
.field :deep(.el-select) {
  width: 100%;
}

.required {
  color: #ef4444;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.hint.error {
  color: #b91c1c;
}

.param-block {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px;
  background: #fcfdff;
}

.param-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.param-grid > * {
  min-width: 0;
}

.param-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.param-title {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #334155;
  overflow-wrap: anywhere;
}

.param-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.param-ver {
  margin-top: 4px;
  font-size: 11px;
  color: #0369a1;
}

.custom-param-list {
  margin-top: 12px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 10px;
}

.custom-param-head {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #334155;
}

.custom-param-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 8px;
}

.custom-param-row > * {
  min-width: 0;
}

.test-result {
  margin-top: 18px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid transparent;
}

.test-result.success {
  background: #ecfdf5;
  border-color: #34d399;
  color: #065f46;
}

.test-result.failed {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #991b1b;
}

.res-title {
  font-weight: 700;
  font-size: 14px;
}

.res-msg {
  margin-top: 4px;
  font-size: 12px;
  word-break: break-all;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-footer .right {
  display: flex;
  gap: 10px;
}

@media (max-width: 900px) {
  .modal-card {
    max-width: 96vw;
  }

  .form-grid,
  .param-grid {
    grid-template-columns: 1fr;
  }

  .field.full {
    grid-column: auto;
  }

  .custom-param-row {
    grid-template-columns: 1fr;
  }
}
</style>
