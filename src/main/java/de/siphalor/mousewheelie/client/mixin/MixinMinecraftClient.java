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

import de.siphalor.mousewheelie.client.inventory.SlotRefiller;
import de.siphalor.mousewheelie.config.MWConfigHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    public ClientPlayerEntity player;

    @Unique
    private ItemStack mainHandStack;
    @Unique
    private ItemStack offHandStack;

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"))
    public void onItemUse(CallbackInfo callbackInfo) {
        if (MWConfigHandler.getConfig().refill.enable() && MWConfigHandler.getConfig().refill.use()) {
            mainHandStack = player.getMainHandStack();
            mainHandStack = mainHandStack.isEmpty() ? null : mainHandStack.copy();
            offHandStack = player.getOffHandStack();
            offHandStack = offHandStack.isEmpty() ? null : offHandStack.copy();
        }
    }

    @Inject(method = "doItemUse", at = @At("RETURN"))
    public void onItemUsed(CallbackInfo callbackInfo) {
        boolean refillScheduled = false;
        if (mainHandStack != null) {
            refillScheduled = SlotRefiller.scheduleRefillChecked(Hand.MAIN_HAND, player.getInventory(), mainHandStack, player.getMainHandStack());
        }
        if (!refillScheduled && offHandStack != null) {
            SlotRefiller.scheduleRefillChecked(Hand.OFF_HAND, player.getInventory(), offHandStack, player.getOffHandStack());
        }
        SlotRefiller.performRefill();
        mainHandStack = null;
        offHandStack = null;
    }
}
