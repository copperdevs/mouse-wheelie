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

package de.siphalor.mousewheelie;

import de.siphalor.mousewheelie.client.MWClient;
import de.siphalor.mousewheelie.client.network.InteractionManager;
import de.siphalor.mousewheelie.client.util.CreativeSearchOrder;
import de.siphalor.mousewheelie.common.network.MWLogicalServerNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import de.siphalor.mousewheelie.MWConfig;

public class MouseWheelie implements ModInitializer {
    public static final String MOD_ID = "mousewheelie";
    public static final String MOD_NAME = "Mouse Wheelie";

    public static MWConfig CONFIG = MWConfig.createAndLoad();

    public static Identifier id(String id) {
        return Identifier.of(MOD_ID, id);
    }

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register(this::onPlayerUseItem);

        MWLogicalServerNetworking.setup();

        CONFIG.general.subscribeToInteractionRate(this::onReloadInteractionRate);
        CONFIG.general.subscribeToIntegratedInteractionRate(this::onReloadIntegratedInteractionRate);
        CONFIG.sort.subscribeToOptimizeCreativeSearchSort(this::onReloadOptimizeCreativeSearchSort);

    }

    private void onReloadInteractionRate(int value) {
        if (!MWClient.isOnLocalServer()) {
            InteractionManager.setTickRate(CONFIG.general.interactionRate());
        }
    }

    private void onReloadIntegratedInteractionRate(int value) {
        if (!MWClient.isOnLocalServer()) {
            InteractionManager.setTickRate(CONFIG.general.integratedInteractionRate());
        }
    }

    private void onReloadOptimizeCreativeSearchSort(boolean value) {
        CreativeSearchOrder.refreshItemSearchPositionLookup();
    }

    private TypedActionResult<ItemStack> onPlayerUseItem(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (CONFIG.general.enableQuickArmorSwapping() && !world.isClient()) {
            EquipmentSlot equipmentSlot = MouseWheelie.getPlayerPreferredEquipmentSlot(stack);
            if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack equipmentStack = player.getEquippedStack(equipmentSlot);
                int index = 5 + (3 - equipmentSlot.getEntitySlotId());
                if (!equipmentStack.isEmpty() && player.playerScreenHandler.getSlot(index).canTakeItems(player)) {
                    player.setStackInHand(hand, equipmentStack);
                    player.equipStack(equipmentSlot, stack);
                    return TypedActionResult.consume(equipmentStack);
                }
            }
        }
        return TypedActionResult.pass(stack);
    }

    public static EquipmentSlot getPlayerPreferredEquipmentSlot(ItemStack stack) {
        var player = MinecraftClient.getInstance().player;

        if (player == null) return null;

        return player.getPreferredEquipmentSlot(stack);
    }

    public static int getEnchantmentLevel(RegistryKey<Enchantment> enchantment, ItemStack stack) {
        int level = 0;
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : itemEnchantmentsComponent.getEnchantments()) {
            if (entry.matchesKey(enchantment)) {
                level = itemEnchantmentsComponent.getLevel(entry);
            }
        }

        return level;
    }
}
