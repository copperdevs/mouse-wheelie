package de.siphalor.mousewheelie;

import de.siphalor.mousewheelie.client.MWClient;
import de.siphalor.mousewheelie.client.inventory.sort.SortMode;
import de.siphalor.mousewheelie.client.network.InteractionManager;
import de.siphalor.mousewheelie.client.util.CreativeSearchOrder;
import de.siphalor.mousewheelie.client.util.ItemStackUtils;
import io.wispforest.owo.config.annotation.*;

@Modmenu(modId = MouseWheelie.MOD_ID)
@Config(name = "mouse-wheelie-config", wrapperName = "MWConfig")
public class MWConfigModel {
    @Nest
    public General general = new General();

    public static class General {
        @RangeConstraint(min = 1, max = 100)
        @Hook
        public int interactionRate = 10;

        @RangeConstraint(min = 1, max = 100)
        @Hook
        public int integratedInteractionRate = 1;

        public boolean enableQuickArmorSwapping = true;

        public boolean enableDropModifier = true;

        public boolean enableQuickCraft = true;

        // Whether item types should check nbt data.
        // This is for example used by scrolling and drop-clicking.
        // NONE disables this.
        // ALL checks for exactly the same nbt.
        // SOME allows for differences in damage and enchantments.
        public ItemStackUtils.NbtMatchMode itemKindsNbtMatchMode = ItemStackUtils.NbtMatchMode.SOME;

        public enum HotbarScoping {HARD, SOFT, NONE}

        public HotbarScoping hotbarScoping = HotbarScoping.SOFT;

        public boolean betterFastDragging = false;

        // Enables dragging bundles while holding right-click to pick up or put out multiple stacks in a single swipe.
        public boolean enableBundleDragging = true;

//        @AConfigListener("interaction-rate")
        public void onReloadInteractionRate() {
            if (!MWClient.isOnLocalServer()) {
                InteractionManager.setTickRate(interactionRate);
            }
        }
//
//        @AConfigListener("integrated-interaction-rate")
        public void onReloadIntegratedInteractionRate() {
            if (MWClient.isOnLocalServer()) {
                InteractionManager.setTickRate(integratedInteractionRate);
            }
        }
    }

    @Nest
    public Scrolling scrolling = new Scrolling();

    public static class Scrolling {
        public boolean enable = true;
        public boolean invert = false;
        public boolean directionalScrolling = true;
        public boolean scrollCreativeMenuItems = true;
        public boolean scrollCreativeMenuTabs = true;
    }
    @Nest
    public Sort sort = new Sort();

    public static class Sort {
        public SortMode primarySort = SortMode.CREATIVE;
        public SortMode shiftSort = SortMode.QUANTITY;
        public SortMode controlSort = SortMode.ALPHABET;
        public boolean serverAcceleratedSorting = true;

        //        @AConfigEntry(scope = ConfigScope.SMALLEST)
        @Hook
        public boolean optimizeCreativeSearchSort = true;

        //        @AConfigListener("optimize-creative-search-sort")
        public void onReloadOptimizeCreativeSearchSort() {
            CreativeSearchOrder.refreshItemSearchPositionLookup();
        }
    }

    @Nest
    public static Refill refill = new Refill();

    public static class Refill {
        public boolean enable = true;

        public boolean playSound = true;

        public boolean offHand = true;
        public boolean restoreSelectedSlot = false;

        public boolean itemChanges = true;

        public boolean eat = true;
        public boolean drop = true;
        public boolean use = true;
        public boolean other = true;

        @Nest
        public Rules rules = new Rules();

        public static class Rules {
            public boolean anyBlock = false;
            public boolean itemgroup = false;
            public boolean itemHierarchy = false;
            public boolean blockHierarchy = false;
            public boolean food = false;
            public boolean equalItems = true;
            public boolean equalStacks = true;
        }
    }

    @Nest
    public ToolPicking toolPicking = new ToolPicking();

    public static class ToolPicking {
        public boolean holdTool = true;
        public boolean holdBlock = false;
        public boolean pickFromInventory = true;
    }
}
