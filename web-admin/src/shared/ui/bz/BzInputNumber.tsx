import { BzButton } from "./BzButton";
import styles from "./BzInputNumber.module.css";

interface BzInputNumberProps {
  modelValue?: number;
  min?: number;
  max?: number;
  step?: number;
  disabled?: boolean;
  placeholder?: string;
  onValueChange?: (value: number) => void;
  onChange?: (value: number) => void;
}

export function BzInputNumber({
  modelValue = 0,
  min = -Infinity,
  max = Infinity,
  step = 1,
  disabled = false,
  placeholder = "",
  onValueChange,
  onChange,
}: BzInputNumberProps) {
  function adjust(delta: number) {
    const next = Math.min(max, Math.max(min, (modelValue || 0) + delta));
    onValueChange?.(next);
    onChange?.(next);
  }

  return (
    <div className={[styles.root, disabled ? styles.disabled : ""].filter(Boolean).join(" ")}>
      <BzButton
        className={styles.decrease}
        disabled={disabled || (modelValue || 0) <= min}
        onClick={() => adjust(-step)}
      >
        -
      </BzButton>
      <div className={styles.input}>
        <input
          type="number"
          value={modelValue}
          disabled={disabled}
          placeholder={placeholder}
          onChange={(event) => {
            const raw = event.target.value;
            if (raw === "") return;
            const num = Number(raw);
            if (!Number.isFinite(num)) return;
            const clamped = Math.min(max, Math.max(min, num));
            onValueChange?.(clamped);
            onChange?.(clamped);
          }}
        />
      </div>
      <BzButton
        className={styles.increase}
        disabled={disabled || (modelValue || 0) >= max}
        onClick={() => adjust(step)}
      >
        +
      </BzButton>
    </div>
  );
}
