import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { BzCard } from "@admin/shared/ui/bz";

import styles from "./AdminHelpPage.module.css";

export function AdminHelpPage() {
  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <BzCard
            className={`${layoutStyles.panel} ${layoutStyles.tableCard}`}
            shadow="never"
            header={
              <div className={layoutStyles.tableHeader}>
                <div className={layoutStyles.tableTitle}>问题与帮助</div>
              </div>
            }
          >
            <div className={styles.grid}>
              <section className={styles.card}>
                <div className={styles.title}>账号使用</div>
                <p>可在个人中心查看基础信息与最近登录/退出记录。</p>
                <p>密码修改请前往“修改密码”，个性化设置请前往“偏好设置”。</p>
              </section>
              <section className={styles.card}>
                <div className={styles.title}>界面说明</div>
                <p>后台页面统一使用顶部工具区、内容工作区和标准操作按钮。</p>
                <p>通用提交动作优先使用“确认”，避免为相同语义重复造新按钮。</p>
              </section>
              <section className={styles.card}>
                <div className={styles.title}>安全提示</div>
                <p>离开共享设备前请主动退出登录，避免后台会话被他人继续使用。</p>
                <p>如发现异常登录记录，请立即修改密码并联系系统管理员。</p>
              </section>
            </div>
          </BzCard>
        </div>
      </div>
    </div>
  );
}
