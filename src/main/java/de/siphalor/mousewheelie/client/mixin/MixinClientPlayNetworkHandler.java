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

package de.siphalor.mousewheelie.client.mixin;

import de.siphalor.mousewheelie.client.MWClient;
import de.siphalor.mousewheelie.client.inventory.SlotRefiller;
import de.siphalor.mousewheelie.client.network.InteractionManager;
import de.siphalor.mousewheelie.client.network.MWClientNetworking;
import de.siphalor.mousewheelie.config.MWConfigHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler {
    protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"))
    public void onHeldItemChangeBegin(UpdateSelectedSlotS2CPacket packet, CallbackInfo callbackInfo) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.HELD_ITEM_CHANGE);
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    public void onGuiSlotUpdateBegin(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callbackInfo) {
        MWClient.lastUpdatedSlot = packet.getSlot();
        InteractionManager.triggerSend(InteractionManager.TriggerType.CONTAINER_SLOT_UPDATE);
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/PlayerScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V", shift = At.Shift.BEFORE))
    public void onGuiSlotUpdateHotbar(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callbackInfo) {
        if (MWConfigHandler.getConfig().refill.enable() && MWConfigHandler.getConfig().refill.other()) {
            //noinspection ConstantConditions
            PlayerInventory inventory = client.player.getInventory();
            if (packet.getSlot() - 36 == inventory.selectedSlot) { // MAIN_HAND
                SlotRefiller.scheduleRefillChecked(Hand.MAIN_HAND, inventory, inventory.getStack(inventory.selectedSlot), packet.getStack());
            }
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V", shift = At.Shift.BEFORE))
    public void onGuiSlotUpdateOther(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callbackInfo) {
        //noinspection ConstantConditions
        if (MWConfigHandler.getConfig().refill.enable() && MWConfigHandler.getConfig().refill.other() && client.player.currentScreenHandler == client.player.playerScreenHandler && packet.getSlot() == 45) {
            PlayerInventory inventory = client.player.getInventory();
            if (packet.getSlot() == 45) { // OFF_HAND
                SlotRefiller.scheduleRefillChecked(Hand.OFF_HAND, inventory, inventory.offHand.get(0), packet.getStack());
            }
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", require = 2, at = {@At(value = "INVOKE", target = "Lnet/minecraft/screen/PlayerScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER), @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER),})
    public void onGuiSlotUpdated(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callbackInfo) {
        if (packet.getSyncId() == 0) {
            if (MWClientNetworking.areGuiUpdateRefillTriggersBlocked()) {
                MWClientNetworking.decrementGuiUpdateRefillTriggerBlocks();
                return;
            }

            SlotRefiller.performRefill();
        }
    }
}
