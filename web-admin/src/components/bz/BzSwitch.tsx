interface BzSwitchProps {
  modelValue: boolean;
  disabled?: boolean;
  activeText?: string;
  inactiveText?: string;
  onValueChange?: (value: boolean) => void;
  onChange?: (value: boolean) => void;
}

export function BzSwitch({
  modelValue = false,
  disabled = false,
  activeText = "",
  inactiveText = "",
  onValueChange,
  onChange,
}: BzSwitchProps) {
  return (
    <label
      className={`bz-switch${modelValue ? " is-checked" : ""}${disabled ? " is-disabled" : ""}`}
    >
      <input
        className="bz-switch__input"
        type="checkbox"
        checked={modelValue}
        disabled={disabled}
        onChange={(event) => {
          const checked = event.target.checked;
          onValueChange?.(checked);
          onChange?.(checked);
        }}
      />
      <span className="bz-switch__core" />
      {activeText || inactiveText ? (
        <span className="bz-switch__text">{modelValue ? activeText : inactiveText}</span>
      ) : null}
    </label>
  );
}
