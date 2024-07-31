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

package de.siphalor.mousewheelie.common.network;

import de.siphalor.mousewheelie.MouseWheelie;
import de.siphalor.mousewheelie.network.ReorderInventoryPayload;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Mouse;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that handles functionality on the logical server side.
 */
public class MWLogicalServerNetworking {

    private MWLogicalServerNetworking() {
    }

    public static void setup() {
        PayloadTypeRegistry.playC2S().register(ReorderInventoryPayload.ID, ReorderInventoryPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReorderInventoryPayload.ID, (payload, context) -> {
            onReorderInventoryPacket(context.player().getServer(), context.player(), payload);
        });
    }

    private static void onReorderInventoryPacket(MinecraftServer server, ServerPlayerEntity player, ReorderInventoryPayload payload) {
        if (payload == null) {
            MouseWheelie.logWarn("Failed to read reorder inventory packet from player {}!", player);
            return;
        }

        if (player.currentScreenHandler == null) {
            MouseWheelie.logWarn("Player {} tried to reorder inventory without having an open container!", player);
            return;
        }

        if (payload.syncId() == player.playerScreenHandler.syncId) {
            server.execute(() -> reorder(player, player.playerScreenHandler, payload.slotMappings()));
        } else if (payload.syncId() == player.currentScreenHandler.syncId) {
            server.execute(() -> reorder(player, player.currentScreenHandler, payload.slotMappings()));
        }
    }

    private static void reorder(PlayerEntity player, ScreenHandler screenHandler, int[] slotMapping) {
        if (!checkReorder(player, screenHandler, slotMapping)) {
            MouseWheelie.logWarn("Reorder inventory packet from player {} contains invalid data, ignoring!", player);
            return;
        }

        List<ItemStack> stacks = screenHandler.slots.stream().map(Slot::getStack).collect(Collectors.toList());

        for (int i = 0; i < slotMapping.length; i += 2) {
            int originSlotId = slotMapping[i];
            int destSlotId = slotMapping[i + 1];

            screenHandler.slots.get(destSlotId).setStack(stacks.get(originSlotId));
        }
    }

    private static boolean checkReorder(PlayerEntity player, ScreenHandler screenHandler, int[] slotMappings) {
        if (slotMappings.length < 4) {
            MouseWheelie.logWarn("Reorder inventory packet contains too few slots!");
            return false;
        }

        IntSet requestedSlots = new IntAVLTreeSet();
        Inventory targetInv;

        Slot firstSlot = screenHandler.slots.get(slotMappings[0]);
        targetInv = firstSlot.inventory;

        for (int i = 0; i < slotMappings.length; i += 2) {
            int originSlotId = slotMappings[i];
            int destSlotId = slotMappings[i + 1];

            if (!checkReorderSlot(screenHandler, originSlotId, targetInv)) {
                return false;
            }
            if (!requestedSlots.add(originSlotId)) {
                MouseWheelie.logWarn("Reorder inventory packet contains duplicate origin slot {}!", originSlotId);
                return false;
            }

            if (!checkReorderSlot(screenHandler, destSlotId, targetInv)) {
                return false;
            }

            if (originSlotId == destSlotId) {
                continue;
            }

            Slot originSlot = screenHandler.getSlot(originSlotId);
            if (!originSlot.canTakeItems(player)) {
                MouseWheelie.logWarn("Player {} tried to reorder slot {}, but that slot doesn't allow taking items!", player, originSlotId);
                return false;
            }
            Slot destSlot = screenHandler.getSlot(destSlotId);
            if (!destSlot.canInsert(originSlot.getStack())) {
                MouseWheelie.logWarn("Player {} tried to reorder slot {}, but that slot doesn't allow inserting the origin stack!", player, destSlotId);
                return false;
            }
        }

        for (int i = 1; i < slotMappings.length; i += 2) {
            int destSlotId = slotMappings[i];
            if (!requestedSlots.remove(destSlotId)) {
                MouseWheelie.logWarn("Reorder inventory packet contains duplicate destination slot or slot without origin: {}!", i);
                return false;
            }
        }
        if (!requestedSlots.isEmpty()) {
            MouseWheelie.logWarn("Invalid state during checking reorder packet, please report this to the {} bug tracker. Requested slots: {}", MouseWheelie.MOD_NAME, requestedSlots);
            return false;
        }
        return true;
    }

    private static boolean checkReorderSlot(ScreenHandler screenHandler, int slotId, Inventory targetInv) {
        Slot slot = screenHandler.getSlot(slotId);
        if (slot == null) {
            MouseWheelie.logWarn("Reorder inventory packet contains invalid slot id!");
            return false;
        }

        if (targetInv != slot.inventory) {
            MouseWheelie.logWarn("Reorder inventory packet contains slots from different inventories, first: {}, now: {}!", targetInv, slot.inventory);
            return false;
        }
        return true;
    }
}
