<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {
        ModuleSetting,
        TextSetting,
    } from "../../../integration/types";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{cSetting.name}</div>
    <input type="text" class="value" placeholder={setting.name} bind:value={cSetting.value} on:change={handleChange}
           spellcheck="false">
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .setting {
    padding: 7px 0px;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .value {
    width: 100%;
    background-color: rgba($clickgui-base-color, .36);
    font-family: monospace;
    font-size: 12px;
    color: $clickgui-text-color;
    border: none;
    border-bottom: solid 2px transparent;
    padding: 5px;
    border-radius: 3px;
    border-bottom: solid 1px transparent;
    transition: ease border-color .2s;

    &:focus {
      border-color: $accent-color;
    }

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
