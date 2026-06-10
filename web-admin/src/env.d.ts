/// <reference types="vite/client" />

import type * as Bz from "./components/bz";

declare module "*.vue" {
  import type { DefineComponent } from "vue";

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}

declare module "vue" {
  export interface GlobalComponents {
    BzAlert: Bz.BzAlert;
    BzAside: Bz.BzAside;
    BzButton: Bz.BzButton;
    BzButtonGroup: Bz.BzButtonGroup;
    BzCard: Bz.BzCard;
    BzCheckbox: Bz.BzCheckbox;
    BzCheckboxGroup: Bz.BzCheckboxGroup;
    BzConfirmHost: Bz.BzConfirmHost;
    BzContainer: Bz.BzContainer;
    BzDatePicker: Bz.BzDatePicker;
    BzDialog: Bz.BzDialog;
    BzDropdown: Bz.BzDropdown;
    BzDropdownItem: Bz.BzDropdownItem;
    BzDropdownMenu: Bz.BzDropdownMenu;
    BzEmpty: Bz.BzEmpty;
    BzForm: Bz.BzForm;
    BzFormItem: Bz.BzFormItem;
    BzIcon: Bz.BzIcon;
    BzIconActionButton: Bz.BzIconActionButton;
    BzIconClose: Bz.BzIconClose;
    BzInput: Bz.BzInput;
    BzInputNumber: Bz.BzInputNumber;
    BzMain: Bz.BzMain;
    BzMenu: Bz.BzMenu;
    BzMenuItem: Bz.BzMenuItem;
    BzMessage: Bz.BzMessage;
    BzMessageHost: Bz.BzMessageHost;
    BzOption: Bz.BzOption;
    BzPagination: Bz.BzPagination;
    BzRadio: Bz.BzRadio;
    BzRadioButton: Bz.BzRadioButton;
    BzRadioGroup: Bz.BzRadioGroup;
    BzSelect: Bz.BzSelect;
    BzSkeleton: Bz.BzSkeleton;
    BzSwitch: Bz.BzSwitch;
    BzTable: Bz.BzTable;
    BzTableColumn: Bz.BzTableColumn;
    BzTabPane: Bz.BzTabPane;
    BzTabs: Bz.BzTabs;
    BzTag: Bz.BzTag;
    BzText: Bz.BzText;
    BzTextField: Bz.BzTextField;
    BzTooltip: Bz.BzTooltip;
    BzTree: Bz.BzTree;
  }
}

export {};
