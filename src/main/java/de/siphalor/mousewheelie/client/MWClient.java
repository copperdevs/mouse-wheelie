/*
 * Copyright 2020-2022 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package de.siphalor.mousewheelie.client;

import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.mousewheelie.MouseWheelie;
import de.siphalor.mousewheelie.client.inventory.ToolPicker;
import de.siphalor.mousewheelie.client.keybinding.*;
import de.siphalor.mousewheelie.client.util.CreativeSearchOrder;
import de.siphalor.mousewheelie.client.util.ScrollAction;
import de.siphalor.mousewheelie.client.util.inject.IContainerScreen;
import de.siphalor.mousewheelie.client.util.inject.IScrollableRecipeBook;
import de.siphalor.mousewheelie.client.util.inject.ISpecialScrollableScreen;
import de.siphalor.mousewheelie.config.MWConfigHandler;
import dev.kingtux.tms.api.modifiers.BindingModifiers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
@SuppressWarnings("WeakerAccess")
public class MWClient implements ClientModInitializer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static final String KEY_BINDING_CATEGORY = "key.categories." + MouseWheelie.MOD_ID;

    public static final KeyBinding SORT_KEY_BINDING = new SortKeyBinding(MouseWheelie.id("sort_inventory"), InputUtil.Type.MOUSE, 2, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final KeyBinding SCROLL_UP_KEY_BINDING = new ScrollKeyBinding(MouseWheelie.id("scroll_up"), KEY_BINDING_CATEGORY, false);
    public static final KeyBinding SCROLL_DOWN_KEY_BINDING = new ScrollKeyBinding(MouseWheelie.id("scroll_down"), KEY_BINDING_CATEGORY, true);
    public static final KeyBinding PICK_TOOL_KEY_BINDING = new PickToolKeyBinding(MouseWheelie.id("pick_tool"), InputUtil.Type.KEYSYM, -1, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final ActionModifierKeybinding WHOLE_STACK_MODIFIER = new ActionModifierKeybinding(MouseWheelie.id("whole_stack_modifier"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final ActionModifierKeybinding ALL_OF_KIND_MODIFIER = new ActionModifierKeybinding(MouseWheelie.id("all_of_kind_modifier"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final ActionModifierKeybinding DROP_MODIFIER = new ActionModifierKeybinding(MouseWheelie.id("drop_modifier"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final ActionModifierKeybinding DEPOSIT_MODIFIER = new ActionModifierKeybinding(MouseWheelie.id("deposit_modifier"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_BINDING_CATEGORY, new BindingModifiers());
    public static final ActionModifierKeybinding RESTOCK_MODIFIER = new ActionModifierKeybinding(MouseWheelie.id("restock_modifier"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KEY_BINDING_CATEGORY, new BindingModifiers());

    public static int lastUpdatedSlot = -1;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(SORT_KEY_BINDING);
        KeyBindingHelper.registerKeyBinding(SCROLL_UP_KEY_BINDING);
        KeyBindingHelper.registerKeyBinding(SCROLL_DOWN_KEY_BINDING);
        KeyBindingHelper.registerKeyBinding(PICK_TOOL_KEY_BINDING);

        KeyBindingHelper.registerKeyBinding(WHOLE_STACK_MODIFIER);
        KeyBindingHelper.registerKeyBinding(ALL_OF_KIND_MODIFIER);
        KeyBindingHelper.registerKeyBinding(DROP_MODIFIER);
        KeyBindingHelper.registerKeyBinding(DEPOSIT_MODIFIER);
        KeyBindingHelper.registerKeyBinding(RESTOCK_MODIFIER);

        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            Item item = player.getMainHandStack().getItem();
            int index = -1;
            if (MWConfigHandler.getConfig().toolPicking.holdTool() && (isTool(item) || isWeapon(item))) {
                ToolPicker toolPicker = new ToolPicker(player.getInventory());
                if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult) {
                    index = toolPicker.findToolFor(player.getWorld().getBlockState(((BlockHitResult) result).getBlockPos()));
                } else {
                    index = toolPicker.findWeapon();
                }
            }
            if (MWConfigHandler.getConfig().toolPicking.holdBlock() && item instanceof BlockItem && result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult) {
                BlockState blockState = player.getWorld().getBlockState(((BlockHitResult) result).getBlockPos());
                if (blockState.getBlock() == ((BlockItem) item).getBlock()) {
                    ToolPicker toolPicker = new ToolPicker(player.getInventory());
                    index = toolPicker.findToolFor(blockState);
                }
            }
            return index == -1 || index == player.getInventory().selectedSlot ? ItemStack.EMPTY : player.getInventory().getStack(index);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CreativeSearchOrder.refreshItemSearchPositionLookup();
        });
    }

    public static boolean isTool(Item item) {
        // TODO: reimplement Fapi tool tags
        return item instanceof ToolItem || item instanceof ShearsItem;
    }

    public static boolean isWeapon(Item item) {
        return item instanceof RangedWeaponItem || item instanceof TridentItem || item instanceof SwordItem;
    }

    public static double getMouseX() {
        return CLIENT.mouse.getX() * (double) CLIENT.getWindow().getScaledWidth() / (double) CLIENT.getWindow().getWidth();
    }

    public static double getMouseY() {
        return CLIENT.mouse.getY() * (double) CLIENT.getWindow().getScaledHeight() / (double) CLIENT.getWindow().getHeight();
    }

    public static boolean isOnLocalServer() {
        return CLIENT.getServer() != null;
    }

    public static boolean triggerScroll(double mouseX, double mouseY, double scrollY) {
        double scrollAmount = scrollY * CLIENT.options.getMouseWheelSensitivity().getValue();
        ScrollAction result;
        if (CLIENT.currentScreen instanceof ISpecialScrollableScreen) {
            result = ((ISpecialScrollableScreen) CLIENT.currentScreen).mouseWheelie_onMouseScrolledSpecial(mouseX, mouseY, scrollAmount);
            if (result.cancelsCustomActions()) {
                return result.cancelsAllActions();
            }
        }
        if (CLIENT.currentScreen instanceof IContainerScreen) {
            result = ((IContainerScreen) CLIENT.currentScreen).mouseWheelie_onMouseScroll(mouseX, mouseY, scrollY);
            if (result.cancelsCustomActions()) {
                return result.cancelsAllActions();
            }
        }
        if (CLIENT.currentScreen instanceof IScrollableRecipeBook) {
            result = ((IScrollableRecipeBook) CLIENT.currentScreen).mouseWheelie_onMouseScrollRecipeBook(mouseX, mouseY, scrollY);
            if (result.cancelsCustomActions()) {
                return result.cancelsAllActions();
            }
        }
        return false;
    }
}
