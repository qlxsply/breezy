import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzDialog } from "../bz/BzDialog";

interface PasswordResetDialogProps {
  onClose: () => void;
  onSubmit: () => void;
}

export function PasswordResetDialog({ onClose, onSubmit }: PasswordResetDialogProps) {
  return (
    <BzDialog
      modelValue={true}
      title="重置密码"
      width="420px"
      onClose={onClose}
      footer={
        <div style={{ display: "flex", gap: 8 }}>
          <BzButton onClick={onClose}>取消</BzButton>
          <BzButton
            buttonType="primary"
            onClick={onSubmit}
          >
            确认
          </BzButton>
        </div>
      }
    >
      <BzAlert
        title="确认将该用户密码重置为 123456"
        type="warning"
        showIcon
      />
      <div className="hint">重置后用户下次登录需修改密码。</div>
    </BzDialog>
  );
}
