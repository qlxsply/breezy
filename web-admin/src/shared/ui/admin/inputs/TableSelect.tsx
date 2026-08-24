"use client";

import {
  type CSSProperties,
  type FocusEvent,
  forwardRef,
  isValidElement,
  type KeyboardEvent,
  type MouseEvent,
  type MouseEventHandler,
  type ReactNode,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import { createPortal } from "react-dom";

import styles from "./TableSelect.module.css";
import {
  TableSelectCheckIcon,
  TableSelectClearIcon,
  TableSelectCloseIcon,
} from "./TableSelectIcons";

export type TableSelectPrimitive = string | number;

export interface TableSelectLabeledValue {
  value: TableSelectPrimitive;
  label: ReactNode;
}

export interface TableSelectOption {
  label: ReactNode;
  value: TableSelectPrimitive;
  disabled?: boolean;
  extra?: unknown;
}

export interface TableSelectOptionGroup {
  label: ReactNode;
  options: readonly TableSelectOption[];
}

export type TableSelectModelValue =
  | TableSelectPrimitive
  | TableSelectPrimitive[]
  | TableSelectLabeledValue
  | TableSelectLabeledValue[];

export type TableSelectInputChangeReason =
  "manual" | "optionChecked" | "optionListHide" | "tokenSeparator";

export interface TableSelectProps {
  value?: TableSelectModelValue | null;
  defaultValue?: TableSelectModelValue;
  options: readonly (TableSelectOption | TableSelectOptionGroup)[];
  mode?: "multiple" | "tags";
  labelInValue?: boolean;
  placeholder?: string;
  disabled?: boolean;
  loading?: boolean;
  allowClear?: boolean;
  allowCreate?:
    | boolean
    | {
        formatter: (inputValue: string, creating: boolean) => TableSelectOption;
      };
  showSearch?:
    | boolean
    | {
        retainInputValue?: boolean;
        retainInputValueWhileSelect?: boolean;
      };
  inputValue?: string;
  filterOption?: boolean | ((inputValue: string, option: TableSelectOption) => boolean);
  tokenSeparators?: readonly string[];
  defaultActiveFirstOption?: boolean;
  defaultPopupVisible?: boolean;
  popupVisible?: boolean;
  unmountOnExit?: boolean;
  notFoundContent?: ReactNode;
  maxTagCount?: number | { count: number; render?: (hiddenCount: number) => ReactNode };
  dragToSort?: boolean;
  animation?: boolean;
  status?: "error" | "warning";
  size?: "mini" | "small" | "default" | "large";
  addBefore?: ReactNode;
  prefix?: ReactNode;
  suffixIcon?: ReactNode;
  arrowIcon?: ReactNode | null;
  clearIcon?: ReactNode;
  removeIcon?: ReactNode | null;
  trigger?: "click" | "focus" | "hover";
  triggerElement?:
    | ReactNode
    | ((params: {
        value: TableSelectModelValue | undefined;
        option: TableSelectOption | TableSelectOption[] | undefined;
      }) => ReactNode);
  renderFormat?: (
    option: TableSelectOption | null,
    value: TableSelectPrimitive | TableSelectLabeledValue,
  ) => ReactNode;
  renderTag?: (
    props: {
      value: TableSelectPrimitive;
      label: ReactNode;
      closable: boolean;
      onClose: (event: MouseEvent<HTMLButtonElement>) => void;
    },
    index: number,
    values: TableSelectPrimitive[],
  ) => ReactNode;
  className?: string;
  popupClassName?: string;
  dropdownMenuClassName?: string;
  style?: CSSProperties;
  popupStyle?: CSSProperties;
  dropdownMenuStyle?: CSSProperties;
  id?: string;
  name?: string;
  "aria-label"?: string;
  onValueChange?: (value: TableSelectPrimitive | TableSelectPrimitive[]) => void;
  onChange?: (
    value: TableSelectModelValue | undefined,
    option: TableSelectOption | TableSelectOption[] | undefined,
  ) => void;
  onSelect?: (
    value: TableSelectPrimitive | TableSelectLabeledValue,
    option: TableSelectOption,
  ) => void;
  onDeselect?: (
    value: TableSelectPrimitive | TableSelectLabeledValue,
    option: TableSelectOption,
  ) => void;
  onClear?: (popupVisible: boolean) => void;
  onSearch?: (value: string, reason: TableSelectInputChangeReason) => void;
  onInputValueChange?: (value: string, reason: TableSelectInputChangeReason) => void;
  onVisibleChange?: (visible: boolean) => void;
  onPopupScroll?: (element: HTMLDivElement) => void;
  onFocus?: (event: FocusEvent<HTMLButtonElement | HTMLInputElement>) => void;
  onBlur?: (event: FocusEvent<HTMLButtonElement | HTMLInputElement>) => void;
  onKeyDown?: (event: KeyboardEvent<HTMLButtonElement | HTMLInputElement>) => void;
  onPaste?: React.ClipboardEventHandler<HTMLInputElement>;
  onClick?: MouseEventHandler<HTMLDivElement>;
  getPopupContainer?: (trigger: HTMLElement) => Element;
}

export interface TableSelectHandle {
  dom: HTMLDivElement | null;
  focus: () => void;
  blur: () => void;
  activeOptionValue: TableSelectPrimitive | undefined;
  getOptionInfoList: () => TableSelectOption[];
  getOptionInfoByValue: (value: TableSelectPrimitive) => TableSelectOption | undefined;
  scrollIntoView: (value: TableSelectPrimitive, options?: ScrollIntoViewOptions) => void;
}

interface FlatOption extends TableSelectOption {
  key: string;
  groupLabel?: ReactNode;
}

interface PopupPosition {
  left: number;
  top?: number;
  bottom?: number;
  width: number;
}

export const TableSelect = forwardRef<TableSelectHandle, TableSelectProps>(function TableSelect(
  {
    value,
    defaultValue,
    options,
    mode,
    labelInValue = false,
    placeholder = "请选择",
    disabled = false,
    loading = false,
    allowClear,
    allowCreate = false,
    showSearch = false,
    inputValue,
    filterOption = true,
    tokenSeparators,
    defaultActiveFirstOption = true,
    defaultPopupVisible = false,
    popupVisible,
    unmountOnExit = true,
    notFoundContent = "暂无数据",
    maxTagCount,
    dragToSort = false,
    animation = true,
    status,
    size = "small",
    addBefore,
    prefix,
    suffixIcon,
    arrowIcon,
    clearIcon,
    removeIcon,
    trigger = "click",
    triggerElement,
    renderFormat,
    renderTag,
    className,
    popupClassName,
    dropdownMenuClassName,
    style,
    popupStyle,
    dropdownMenuStyle,
    id,
    name,
    "aria-label": ariaLabel,
    onValueChange,
    onChange,
    onSelect,
    onDeselect,
    onClear,
    onSearch,
    onInputValueChange,
    onVisibleChange,
    onPopupScroll,
    onFocus,
    onBlur,
    onKeyDown,
    onPaste,
    onClick,
    getPopupContainer,
  },
  ref,
) {
  const multiple = mode === "multiple" || mode === "tags";
  const clearable = allowClear ?? multiple;
  const searchable = Boolean(showSearch || allowCreate || multiple);
  const rootRef = useRef<HTMLDivElement>(null);
  const controlRef = useRef<HTMLButtonElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const popupRef = useRef<HTMLDivElement>(null);
  const hoverCloseTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const optionRefs = useRef(new Map<string, HTMLDivElement>());
  const listboxId = `${useId().replaceAll(":", "")}-table-select-listbox`;
  const [innerValue, setInnerValue] = useState<TableSelectModelValue | undefined>(defaultValue);
  const [innerInputValue, setInnerInputValue] = useState("");
  const [innerVisible, setInnerVisible] = useState(defaultPopupVisible);
  const [activeKey, setActiveKey] = useState<string>();
  const [createdOptions, setCreatedOptions] = useState<TableSelectOption[]>([]);
  const [popupPosition, setPopupPosition] = useState<PopupPosition>();
  const controlled = value !== undefined;
  const visibleControlled = popupVisible !== undefined;
  const inputControlled = inputValue !== undefined;
  const currentModel = controlled ? (value ?? undefined) : innerValue;
  const currentVisible = visibleControlled ? popupVisible : innerVisible;
  const currentInputValue = inputControlled ? inputValue : innerInputValue;
  const selectedValues = modelToValues(currentModel, multiple);
  const flatOptions = flattenOptions(options).concat(
    createdOptions.map((option, index) => ({ ...option, key: `created-${index}-${option.value}` })),
  );
  const optionMap = new Map(flatOptions.map((option) => [String(option.value), option]));
  const filteredOptions = getFilteredOptions(flatOptions, currentInputValue, filterOption);
  const creatingOption = getCreatingOption(currentInputValue, flatOptions, allowCreate);
  const visibleOptions = creatingOption
    ? [{ ...creatingOption, key: `creating-${creatingOption.value}` }, ...filteredOptions]
    : filteredOptions;
  const enabledOptions = visibleOptions.filter((option) => !option.disabled);
  const selectedOptions = selectedValues
    .map((item) => optionMap.get(String(item)))
    .filter((item): item is FlatOption => Boolean(item));

  function setVisible(nextVisible: boolean) {
    if (disabled || loading) {
      return;
    }
    if (!visibleControlled) {
      setInnerVisible(nextVisible);
    }
    if (nextVisible !== currentVisible) {
      onVisibleChange?.(nextVisible);
    }
    if (!nextVisible && !getSearchConfig(showSearch).retainInputValue) {
      updateInputValue("", "optionListHide");
    }
  }

  function handleHoverEnter() {
    if (trigger !== "hover") {
      return;
    }
    clearTimeout(hoverCloseTimerRef.current);
    setVisible(true);
  }

  function handleHoverLeave() {
    if (trigger !== "hover") {
      return;
    }
    clearTimeout(hoverCloseTimerRef.current);
    hoverCloseTimerRef.current = setTimeout(() => setVisible(false), 100);
  }

  function updateInputValue(nextValue: string, reason: TableSelectInputChangeReason) {
    if (!inputControlled) {
      setInnerInputValue(nextValue);
    }
    onInputValueChange?.(nextValue, reason);
    onSearch?.(nextValue, reason);
  }

  function commitValues(nextValues: TableSelectPrimitive[], changedOption?: TableSelectOption) {
    const normalizedValues = multiple ? nextValues : nextValues.slice(-1);
    const nextModel = valuesToModel(
      normalizedValues,
      multiple,
      labelInValue,
      optionMap,
      changedOption,
    );
    const callbackOptions = normalizedValues
      .map(
        (item) =>
          optionMap.get(String(item)) ??
          (item === changedOption?.value ? changedOption : undefined),
      )
      .filter((item): item is TableSelectOption => Boolean(item));
    if (!controlled) {
      setInnerValue(nextModel);
    }
    onValueChange?.(multiple ? normalizedValues : (normalizedValues[0] ?? ""));
    onChange?.(nextModel, multiple ? callbackOptions : callbackOptions[0]);
  }

  function selectOption(option: TableSelectOption) {
    if (option.disabled) {
      return;
    }
    const exists = selectedValues.some((item) => String(item) === String(option.value));
    const callbackValue = labelInValue
      ? { value: option.value, label: option.label }
      : option.value;
    if (multiple) {
      if (exists) {
        commitValues(
          selectedValues.filter((item) => String(item) !== String(option.value)),
          option,
        );
        onDeselect?.(callbackValue, option);
      } else {
        if (!optionMap.has(String(option.value))) {
          setCreatedOptions((current) => [...current, option]);
        }
        commitValues([...selectedValues, option.value], option);
        onSelect?.(callbackValue, option);
      }
      if (!getSearchConfig(showSearch).retainInputValueWhileSelect) {
        updateInputValue("", "optionChecked");
      }
      searchRef.current?.focus();
      return;
    }
    if (!exists) {
      if (!optionMap.has(String(option.value))) {
        setCreatedOptions((current) => [...current, option]);
      }
      commitValues([option.value], option);
      onSelect?.(callbackValue, option);
    }
    setVisible(false);
    controlRef.current?.focus();
  }

  function removeValue(item: TableSelectPrimitive, event?: MouseEvent<HTMLButtonElement>) {
    event?.stopPropagation();
    const option = optionMap.get(String(item));
    if (option?.disabled) {
      return;
    }
    commitValues(selectedValues.filter((valueItem) => String(valueItem) !== String(item)));
    if (option) {
      onDeselect?.(
        labelInValue ? { value: option.value, label: option.label } : option.value,
        option,
      );
    }
  }

  function clearValue(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    event.stopPropagation();
    const retainedValues = multiple
      ? selectedValues.filter((item) => optionMap.get(String(item))?.disabled)
      : [];
    commitValues(retainedValues);
    updateInputValue("", "manual");
    onClear?.(currentVisible);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement | HTMLInputElement>) {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      if (!currentVisible) {
        setVisible(true);
      } else {
        const direction = event.key === "ArrowDown" ? 1 : -1;
        const currentIndex = enabledOptions.findIndex((option) => option.key === activeKey);
        const nextIndex =
          currentIndex < 0
            ? direction > 0
              ? 0
              : enabledOptions.length - 1
            : (currentIndex + direction + enabledOptions.length) % enabledOptions.length;
        setActiveKey(enabledOptions[nextIndex]?.key);
      }
    } else if (event.key === "Home" && currentVisible) {
      event.preventDefault();
      setActiveKey(enabledOptions[0]?.key);
    } else if (event.key === "End" && currentVisible) {
      event.preventDefault();
      setActiveKey(enabledOptions.at(-1)?.key);
    } else if (event.key === "Enter") {
      event.preventDefault();
      if (!currentVisible) {
        setVisible(true);
      } else {
        const option = visibleOptions.find((item) => item.key === activeKey);
        if (option) {
          selectOption(option);
        }
      }
    } else if (event.key === "Escape") {
      event.preventDefault();
      setVisible(false);
    } else if (event.key === "Tab") {
      setVisible(false);
    } else if (
      event.key === "Backspace" &&
      multiple &&
      currentInputValue === "" &&
      selectedValues.length > 0
    ) {
      removeValue(selectedValues.at(-1) as TableSelectPrimitive);
    }
    onKeyDown?.(event);
  }

  function separateInput(rawValue: string): boolean {
    if (!multiple || !tokenSeparators?.length) {
      return false;
    }
    const separatorPattern = new RegExp(`[${tokenSeparators.map(escapeRegExp).join("")}]`);
    if (!separatorPattern.test(rawValue)) {
      return false;
    }
    const parts = rawValue
      .split(separatorPattern)
      .map((item) => item.trim())
      .filter(Boolean);
    const nextValues = [...selectedValues];
    const nextCreated: TableSelectOption[] = [];
    for (const part of parts) {
      const matched = flatOptions.find(
        (option) => String(option.value) === part || nodeToText(option.label) === part,
      );
      const option = matched ?? getCreatingOption(part, flatOptions, allowCreate);
      if (
        option &&
        !option.disabled &&
        !nextValues.some((item) => String(item) === String(option.value))
      ) {
        nextValues.push(option.value);
        if (!matched) {
          nextCreated.push(option);
        }
      }
    }
    if (nextCreated.length) {
      setCreatedOptions((current) => [...current, ...nextCreated]);
    }
    commitValues(nextValues);
    updateInputValue("", "tokenSeparator");
    return true;
  }

  useEffect(() => {
    if (!currentVisible) {
      return;
    }
    const selectedKey = selectedOptions[0]?.key;
    setActiveKey(
      selectedKey && visibleOptions.some((option) => option.key === selectedKey)
        ? selectedKey
        : defaultActiveFirstOption
          ? enabledOptions[0]?.key
          : undefined,
    );
    const frame = window.requestAnimationFrame(() => {
      updatePopupPosition(rootRef.current, setPopupPosition);
      if (searchable) {
        searchRef.current?.focus();
      }
    });
    const updatePosition = () => updatePopupPosition(rootRef.current, setPopupPosition);
    window.addEventListener("resize", updatePosition);
    window.addEventListener("scroll", updatePosition, true);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("resize", updatePosition);
      window.removeEventListener("scroll", updatePosition, true);
    };
  }, [currentVisible]);

  useEffect(() => {
    if (!currentVisible) {
      return;
    }
    const handleOutsidePointer = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!rootRef.current?.contains(target) && !popupRef.current?.contains(target)) {
        setVisible(false);
      }
    };
    document.addEventListener("pointerdown", handleOutsidePointer);
    return () => document.removeEventListener("pointerdown", handleOutsidePointer);
  }, [currentVisible]);

  useEffect(() => {
    if (!activeKey) {
      return;
    }
    optionRefs.current.get(activeKey)?.scrollIntoView({ block: "nearest" });
  }, [activeKey]);

  useEffect(() => () => clearTimeout(hoverCloseTimerRef.current), []);

  useImperativeHandle(ref, () => ({
    dom: rootRef.current,
    focus: () => (searchable ? searchRef.current : controlRef.current)?.focus(),
    blur: () => (searchable ? searchRef.current : controlRef.current)?.blur(),
    activeOptionValue: visibleOptions.find((option) => option.key === activeKey)?.value,
    getOptionInfoList: () => flatOptions,
    getOptionInfoByValue: (optionValue) => optionMap.get(String(optionValue)),
    scrollIntoView: (optionValue, scrollOptions) =>
      optionRefs.current.get(String(optionValue))?.scrollIntoView(scrollOptions),
  }));

  const displayedTags = getDisplayedTags(selectedValues, maxTagCount);
  const selectedOption = selectedOptions[0];
  const currentCallbackOption = multiple ? selectedOptions : selectedOption;
  const triggerContent =
    typeof triggerElement === "function"
      ? triggerElement({ value: currentModel, option: currentCallbackOption })
      : triggerElement;
  const shouldRenderPopup = currentVisible || (!unmountOnExit && Boolean(popupPosition));
  const popup =
    shouldRenderPopup && popupPosition ? (
      <div
        ref={popupRef}
        id={listboxId}
        className={[
          styles.popup,
          !currentVisible ? styles.hidden : "",
          dropdownMenuClassName,
          popupClassName,
        ]
          .filter(Boolean)
          .join(" ")}
        style={{ ...dropdownMenuStyle, ...popupStyle, ...popupPosition }}
        role="listbox"
        aria-multiselectable={multiple || undefined}
        onScroll={(event) => onPopupScroll?.(event.currentTarget)}
        onMouseDown={(event) => event.preventDefault()}
        onMouseEnter={handleHoverEnter}
        onMouseLeave={handleHoverLeave}
      >
        {loading ? (
          <div className={styles.empty}>加载中...</div>
        ) : visibleOptions.length === 0 ? (
          <div className={styles.empty}>{notFoundContent}</div>
        ) : (
          visibleOptions.map((option, index) => {
            const previousGroup = visibleOptions[index - 1]?.groupLabel;
            const selected = selectedValues.some((item) => String(item) === String(option.value));
            return (
              <div key={option.key}>
                {option.groupLabel && option.groupLabel !== previousGroup ? (
                  <div className={styles.group}>{option.groupLabel}</div>
                ) : null}
                <div
                  ref={(node) => {
                    if (node) {
                      optionRefs.current.set(option.key, node);
                      optionRefs.current.set(String(option.value), node);
                    } else {
                      optionRefs.current.delete(option.key);
                      optionRefs.current.delete(String(option.value));
                    }
                  }}
                  className={[
                    styles.option,
                    selected ? styles.selected : "",
                    option.key === activeKey ? styles.active : "",
                    option.disabled ? styles.optionDisabled : "",
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  role="option"
                  aria-selected={selected}
                  aria-disabled={option.disabled || undefined}
                  onMouseEnter={() => !option.disabled && setActiveKey(option.key)}
                  onClick={() => selectOption(option)}
                >
                  {multiple ? (
                    <span
                      className={styles.checkbox}
                      aria-hidden="true"
                    >
                      {selected ? <TableSelectCheckIcon size={11} /> : null}
                    </span>
                  ) : null}
                  <span className={styles.label}>{option.label}</span>
                  {!multiple && selected ? (
                    <span
                      className={styles.check}
                      aria-hidden="true"
                    >
                      <TableSelectCheckIcon />
                    </span>
                  ) : null}
                </div>
              </div>
            );
          })
        )}
      </div>
    ) : null;

  return (
    <div
      ref={rootRef}
      className={[
        styles.select,
        styles[size],
        currentVisible ? styles.open : "",
        disabled ? styles.disabled : "",
        !animation ? styles.noAnimation : "",
        status ? styles[status] : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      style={style}
      onClick={(event) => {
        if (trigger === "click" && (triggerContent || multiple)) {
          setVisible(triggerContent ? !currentVisible : true);
        }
        onClick?.(event);
      }}
      onFocus={() => triggerContent && trigger === "focus" && setVisible(true)}
      onMouseEnter={handleHoverEnter}
      onMouseLeave={handleHoverLeave}
    >
      {triggerContent ?? (
        <div className={styles.content}>
          {addBefore ? <span className={styles.addBefore}>{addBefore}</span> : null}
          {prefix ? <span className={styles.prefix}>{prefix}</span> : null}
          {multiple ? (
            <div className={styles.tags}>
              {displayedTags.values.map((item, index) => {
                const option = optionMap.get(String(item));
                const label = option?.label ?? item;
                const closable = !disabled && !option?.disabled;
                const tag = renderTag?.(
                  {
                    value: item,
                    label,
                    closable,
                    onClose: (event) => removeValue(item, event),
                  },
                  index,
                  selectedValues,
                );
                return (
                  <span
                    key={String(item)}
                    className={styles.tag}
                    draggable={dragToSort && selectedValues.length > 1}
                    onDragStart={(event) => {
                      if (dragToSort) {
                        event.dataTransfer.setData("text/plain", String(item));
                      }
                    }}
                    onDragOver={(event) => dragToSort && event.preventDefault()}
                    onDrop={(event) => {
                      if (!dragToSort) {
                        return;
                      }
                      event.preventDefault();
                      const source = event.dataTransfer.getData("text/plain");
                      const sourceIndex = selectedValues.findIndex(
                        (valueItem) => String(valueItem) === source,
                      );
                      const targetIndex = selectedValues.findIndex(
                        (valueItem) => String(valueItem) === String(item),
                      );
                      if (sourceIndex >= 0 && targetIndex >= 0 && sourceIndex !== targetIndex) {
                        const reordered = [...selectedValues];
                        const [moved] = reordered.splice(sourceIndex, 1);
                        reordered.splice(targetIndex, 0, moved);
                        commitValues(reordered);
                      }
                    }}
                  >
                    {tag ?? (
                      <>
                        <span className={styles.tagLabel}>{label}</span>
                        {closable && removeIcon !== null ? (
                          <button
                            type="button"
                            onClick={(event) => removeValue(item, event)}
                          >
                            {removeIcon ?? <TableSelectCloseIcon size={12} />}
                          </button>
                        ) : null}
                      </>
                    )}
                  </span>
                );
              })}
              {displayedTags.hiddenCount > 0 ? (
                <span className={`${styles.tag} ${styles.summaryTag}`}>
                  {displayedTags.render?.(displayedTags.hiddenCount) ??
                    `+${displayedTags.hiddenCount}`}
                </span>
              ) : null}
              {searchable ? (
                <input
                  ref={searchRef}
                  className={styles.search}
                  value={currentInputValue}
                  placeholder={selectedValues.length === 0 ? placeholder : ""}
                  disabled={disabled || loading}
                  onChange={(event) => {
                    const nextValue = event.currentTarget.value;
                    if (!separateInput(nextValue)) {
                      updateInputValue(nextValue, "manual");
                      setVisible(true);
                    }
                  }}
                  onFocus={(event) => {
                    if (trigger === "focus") {
                      setVisible(true);
                    }
                    onFocus?.(event);
                  }}
                  onBlur={onBlur}
                  onKeyDown={handleKeyDown}
                  onPaste={(event) => {
                    separateInput(event.clipboardData.getData("text"));
                    onPaste?.(event);
                  }}
                />
              ) : null}
            </div>
          ) : searchable && currentVisible ? (
            <input
              ref={searchRef}
              className={styles.search}
              value={currentInputValue}
              placeholder={placeholder}
              disabled={disabled || loading}
              role="combobox"
              aria-controls={listboxId}
              aria-expanded={currentVisible}
              aria-activedescendant={activeKey}
              onChange={(event) => {
                updateInputValue(event.currentTarget.value, "manual");
                setVisible(true);
              }}
              onFocus={onFocus}
              onBlur={onBlur}
              onKeyDown={handleKeyDown}
              onPaste={onPaste}
            />
          ) : (
            <button
              ref={controlRef}
              id={id}
              className={styles.value}
              type="button"
              disabled={disabled || loading}
              role="combobox"
              aria-label={ariaLabel}
              aria-controls={listboxId}
              aria-expanded={currentVisible}
              aria-activedescendant={activeKey}
              onClick={() => trigger === "click" && setVisible(!currentVisible)}
              onFocus={(event) => {
                if (trigger === "focus") {
                  setVisible(true);
                }
                onFocus?.(event);
              }}
              onBlur={onBlur}
              onKeyDown={handleKeyDown}
            >
              {selectedOption
                ? (renderFormat?.(
                    selectedOption,
                    labelInValue
                      ? { value: selectedOption.value, label: selectedOption.label }
                      : selectedOption.value,
                  ) ?? selectedOption.label)
                : placeholder}
            </button>
          )}
          {name ? (
            <input
              type="hidden"
              name={name}
              value={selectedValues.join(",")}
            />
          ) : null}
          {clearable && selectedValues.length > 0 && !disabled && !loading ? (
            <button
              className={styles.clear}
              type="button"
              title="取消选中"
              aria-label="取消选中内容"
              onPointerDown={(event) => {
                event.preventDefault();
                event.stopPropagation();
              }}
              onClick={clearValue}
            >
              {clearIcon ?? <TableSelectClearIcon />}
            </button>
          ) : null}
          <span
            className={styles.suffix}
            aria-hidden="true"
          >
            {loading ? (
              <span className={styles.spinner} />
            ) : (
              (suffixIcon ??
              (arrowIcon === undefined ? <span className={styles.arrow} /> : arrowIcon))
            )}
          </span>
        </div>
      )}
      {popup && typeof document !== "undefined"
        ? createPortal(popup, getPopupContainer?.(rootRef.current as HTMLElement) ?? document.body)
        : null}
    </div>
  );
});

function flattenOptions(
  options: readonly (TableSelectOption | TableSelectOptionGroup)[],
): FlatOption[] {
  return options.flatMap((item, groupIndex) => {
    if ("options" in item) {
      return item.options.map((option, optionIndex) => ({
        ...option,
        key: `group-${groupIndex}-${optionIndex}-${option.value}`,
        groupLabel: item.label,
      }));
    }
    return [{ ...item, key: `option-${groupIndex}-${item.value}` }];
  });
}

function modelToValues(
  value: TableSelectModelValue | null | undefined,
  multiple: boolean,
): TableSelectPrimitive[] {
  if (value == null) {
    return [];
  }
  const values = Array.isArray(value) ? value : [value];
  const normalized = values
    .map((item) => (typeof item === "object" ? item.value : item))
    .filter((item) => item !== "");
  return multiple ? normalized : normalized.slice(0, 1);
}

function valuesToModel(
  values: TableSelectPrimitive[],
  multiple: boolean,
  labelInValue: boolean,
  optionMap: Map<string, FlatOption>,
  fallbackOption?: TableSelectOption,
): TableSelectModelValue | undefined {
  if (values.length === 0) {
    return multiple ? [] : undefined;
  }
  const output = labelInValue
    ? values.map((value) => ({
        value,
        label:
          optionMap.get(String(value))?.label ??
          (fallbackOption?.value === value ? fallbackOption.label : value),
      }))
    : values;
  return multiple ? output : output[0];
}

function getFilteredOptions(
  options: FlatOption[],
  inputValue: string,
  filterOption: TableSelectProps["filterOption"],
): FlatOption[] {
  if (!inputValue || filterOption === false) {
    return options;
  }
  if (typeof filterOption === "function") {
    return options.filter((option) => filterOption(inputValue, option));
  }
  const keyword = inputValue.toLocaleLowerCase();
  return options.filter(
    (option) =>
      nodeToText(option.label).toLocaleLowerCase().includes(keyword) ||
      String(option.value).toLocaleLowerCase().includes(keyword),
  );
}

function getCreatingOption(
  inputValue: string,
  options: readonly TableSelectOption[],
  allowCreate: TableSelectProps["allowCreate"],
): TableSelectOption | undefined {
  const normalizedValue = inputValue.trim();
  if (
    !allowCreate ||
    !normalizedValue ||
    options.some((option) => String(option.value) === normalizedValue)
  ) {
    return undefined;
  }
  return typeof allowCreate === "object"
    ? allowCreate.formatter(normalizedValue, true)
    : { label: normalizedValue, value: normalizedValue };
}

function getSearchConfig(showSearch: TableSelectProps["showSearch"]) {
  return typeof showSearch === "object" ? showSearch : {};
}

function getDisplayedTags(
  values: TableSelectPrimitive[],
  maxTagCount: TableSelectProps["maxTagCount"],
) {
  const count = typeof maxTagCount === "object" ? maxTagCount.count : maxTagCount;
  const visibleCount = typeof count === "number" ? Math.max(0, count) : values.length;
  return {
    values: values.slice(0, visibleCount),
    hiddenCount: Math.max(0, values.length - visibleCount),
    render: typeof maxTagCount === "object" ? maxTagCount.render : undefined,
  };
}

function updatePopupPosition(
  trigger: HTMLElement | null,
  update: (position: PopupPosition) => void,
) {
  if (!trigger) {
    return;
  }
  const rect = trigger.getBoundingClientRect();
  const spaceBelow = window.innerHeight - rect.bottom;
  const openAbove = spaceBelow < 160 && rect.top > spaceBelow;
  update({
    left: rect.left,
    width: rect.width,
    ...(openAbove ? { bottom: window.innerHeight - rect.top + 4 } : { top: rect.bottom + 4 }),
  });
}

function nodeToText(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") {
    return String(node);
  }
  if (Array.isArray(node)) {
    return node.map(nodeToText).join("");
  }
  if (isValidElement<{ children?: ReactNode }>(node)) {
    return nodeToText(node.props.children);
  }
  return "";
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
