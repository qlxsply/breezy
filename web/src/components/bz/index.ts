import type { App, Component } from "vue";

import BzAlert from "./BzAlert.vue";
import BzAside from "./BzAside.vue";
import BzButton from "./BzButton.vue";
import BzButtonGroup from "./BzButtonGroup.vue";
import BzCard from "./BzCard.vue";
import BzCheckbox from "./BzCheckbox.vue";
import BzCheckboxGroup from "./BzCheckboxGroup.vue";
import BzConfirmHost from "./BzConfirmHost.vue";
import BzContainer from "./BzContainer.vue";
import BzDatePicker from "./BzDatePicker.vue";
import BzDialog from "./BzDialog.vue";
import BzDropdown from "./BzDropdown.vue";
import BzDropdownItem from "./BzDropdownItem.vue";
import BzDropdownMenu from "./BzDropdownMenu.vue";
import BzEmpty from "./BzEmpty.vue";
import BzForm from "./BzForm.vue";
import BzFormItem from "./BzFormItem.vue";
import BzIcon from "./BzIcon.vue";
import BzIconActionButton from "./BzIconActionButton.vue";
import BzIconClose from "./BzIconClose.vue";
import BzInput from "./BzInput.vue";
import BzInputNumber from "./BzInputNumber.vue";
import BzMain from "./BzMain.vue";
import BzMenu from "./BzMenu.vue";
import BzMenuItem from "./BzMenuItem.vue";
import BzMessage from "./BzMessage.vue";
import BzMessageHost from "./BzMessageHost.vue";
import BzOption from "./BzOption.vue";
import BzPagination from "./BzPagination.vue";
import BzRadio from "./BzRadio.vue";
import BzRadioButton from "./BzRadioButton.vue";
import BzRadioGroup from "./BzRadioGroup.vue";
import BzSelect from "./BzSelect.vue";
import BzSkeleton from "./BzSkeleton.vue";
import BzSwitch from "./BzSwitch.vue";
import BzTable from "./BzTable.vue";
import BzTableColumn from "./BzTableColumn.vue";
import BzTabPane from "./BzTabPane.vue";
import BzTabs from "./BzTabs.vue";
import BzTag from "./BzTag.vue";
import BzText from "./BzText.vue";
import BzTextField from "./BzTextField.vue";
import BzTooltip from "./BzTooltip.vue";
import BzTree from "./BzTree.vue";
import { bzLoadingDirective } from "./loadingDirective";

const components: Component[] = [
  BzButton,
  BzCard,
  BzForm,
  BzFormItem,
  BzInput,
  BzSelect,
  BzOption,
  BzTag,
  BzDropdown,
  BzDropdownMenu,
  BzDropdownItem,
  BzPagination,
  BzTable,
  BzTableColumn,
  BzDatePicker,
  BzDialog,
  BzMessage,
  BzMessageHost,
  BzConfirmHost,
  BzEmpty,
  BzAlert,
  BzText,
  BzTextField,
  BzIcon,
  BzIconClose,
  BzButtonGroup,
  BzContainer,
  BzAside,
  BzMain,
  BzInputNumber,
  BzIconActionButton,
  BzCheckbox,
  BzCheckboxGroup,
  BzRadio,
  BzRadioButton,
  BzRadioGroup,
  BzSwitch,
  BzTooltip,
  BzSkeleton,
  BzTabs,
  BzTabPane,
  BzMenu,
  BzMenuItem,
  BzTree,
];

export const BzUi = {
  install(app: App) {
    components.forEach((component) => {
      if ("name" in component && typeof component.name === "string") {
        app.component(component.name, component);
      }
    });
    app.directive("loading", bzLoadingDirective);
  },
};

export {
  BzAlert,
  BzAside,
  BzButton,
  BzButtonGroup,
  BzCard,
  BzCheckbox,
  BzCheckboxGroup,
  BzConfirmHost,
  BzContainer,
  BzDatePicker,
  BzDialog,
  BzDropdown,
  BzDropdownItem,
  BzDropdownMenu,
  BzEmpty,
  BzForm,
  BzFormItem,
  BzIcon,
  BzIconActionButton,
  BzIconClose,
  BzInput,
  BzInputNumber,
  BzMain,
  BzMenu,
  BzMenuItem,
  BzMessage,
  BzMessageHost,
  BzOption,
  BzPagination,
  BzRadio,
  BzRadioButton,
  BzRadioGroup,
  BzSelect,
  BzSkeleton,
  BzSwitch,
  BzTable,
  BzTableColumn,
  BzTabPane,
  BzTabs,
  BzTag,
  BzText,
  BzTextField,
  BzTooltip,
  BzTree,
};
