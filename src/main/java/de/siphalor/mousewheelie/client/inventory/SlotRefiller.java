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

package de.siphalor.mousewheelie.client.inventory;

import de.siphalor.mousewheelie.Logger;
import de.siphalor.mousewheelie.MouseWheelie;
import de.siphalor.mousewheelie.client.network.InteractionManager;
import de.siphalor.mousewheelie.config.MWConfigHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class SlotRefiller {
    /**
     * Indicates the maximum time in milliseconds a refill is expected to take.
     * If a refill has been started with no recorded end, it is treated as done after this time.
     */
    private static long MAX_REFILL_MILLIS = 1000;
    private static final InteractionManager.InteractionEvent REFILL_END_EVENT = () -> {
        endRefill();
        return InteractionManager.DUMMY_WAITER;
    };

    private static PlayerInventory playerInventory;
    private static ItemStack stack;
    private static long refillStartTime = System.currentTimeMillis() - MAX_REFILL_MILLIS;

    private static final ConcurrentLinkedDeque<Rule> rules = new ConcurrentLinkedDeque<>();
    private static Hand refillHand = null;

    private SlotRefiller() {
    }

    public static void ChangeMaxRefillTime(long maxRefillTime) {
        MAX_REFILL_MILLIS = maxRefillTime;
    }

    /**
     * Schedules a refill if a refill scenario is encountered.
     *
     * @param hand      the hand to potentially refill
     * @param inventory the player inventory
     * @param oldStack  the old stack in the hand
     * @param newStack  the new stack in the hand
     * @return whether a refill has been scheduled
     */
    public static boolean scheduleRefillChecked(Hand hand, PlayerInventory inventory, ItemStack oldStack, ItemStack newStack) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            return false;
        }

        if (!oldStack.isEmpty() && (newStack.isEmpty() || (MWConfigHandler.getConfig().refill.itemChanges() && oldStack.getItem() != newStack.getItem()))) {
            scheduleRefillUnchecked(hand, inventory, oldStack.copy());
            return true;
        }
        return false;
    }

    /**
     * Unconditionally schedules a refill.
     *
     * @param hand           the hand to refill
     * @param inventory      the player inventory
     * @param referenceStack the stack to decide the refilling by
     */
    public static void scheduleRefillUnchecked(Hand hand, PlayerInventory inventory, ItemStack referenceStack) {
        refillHand = hand;
        setupRefill(inventory, referenceStack);
    }

    public static boolean performRefill() {
        if (refillHand == null) return false;

        Hand hand = refillHand;
        refillHand = null;
        if (hand == Hand.OFF_HAND && !MWConfigHandler.getConfig().refill.offHand()) {
            return false;
        }
        refill(hand);

        return true;
    }


    public static void setupRefill(PlayerInventory playerInventory, ItemStack stack) {
        SlotRefiller.playerInventory = playerInventory;
        SlotRefiller.stack = stack;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean refill(Hand hand) {
        if (isRefillInProgress()) {
            Logger.info("refill in progress {}", refillStartTime);
            return false;
        }

        if (stack.getItem() == Items.TRIDENT && MouseWheelie.getEnchantmentLevel(Enchantments.LOYALTY, stack) > 0) {
            return false;
        }

        Iterator<Rule> iterator = rules.descendingIterator();
        while (iterator.hasNext()) {
            Rule rule = iterator.next();
            if (!rule.matches(stack)) {
                continue;
            }

            int slot = rule.findMatchingStack(playerInventory, stack);

            if (slot != -1) {
                startRefill();

                refillFromSlot(hand, slot);
                return true;
            }
        }
        return false;
    }

    private static void startRefill() {
        refillStartTime = System.currentTimeMillis();

        scheduleRefillSound();
    }

    private static void endRefill() {
        refillStartTime = System.currentTimeMillis() - MAX_REFILL_MILLIS;
    }

    private static boolean isRefillInProgress() {
        return System.currentTimeMillis() - refillStartTime < MAX_REFILL_MILLIS;
    }

    private static void scheduleRefillSound() {
        if (MWConfigHandler.getConfig().refill.playSound()) {
            InteractionManager.delay(SlotRefiller::playRefillSound, Duration.of(200, ChronoUnit.MILLIS));
        }
    }

    private static void playRefillSound() {
        if (MWConfigHandler.getConfig().refill.playSound()) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, 0.2F, 1F));
        }
    }

    private static void refillFromSlot(Hand hand, int slot) {
        if (slot == playerInventory.selectedSlot) {
            return;
        }

        if (slot < 9) {
            refillFromHotbar(hand, slot);
        } else {
            refillFromInventory(hand, slot);
        }

        InteractionManager.push(REFILL_END_EVENT);
    }

    private static void refillFromHotbar(Hand hand, int hotbarSlot) {
        if (MWConfigHandler.getConfig().refill.restoreSelectedSlot()) {
            if (hand == Hand.MAIN_HAND && !playerInventory.offHand.getFirst().isEmpty()) {
                InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            }
            InteractionManager.push(new InteractionManager.PacketEvent(new UpdateSelectedSlotC2SPacket(hotbarSlot), InteractionManager.Waiter.equal(InteractionManager.TriggerType.HELD_ITEM_CHANGE)));
            InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            InteractionManager.push(new InteractionManager.PacketEvent(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot), InteractionManager.TICK_WAITER));
            if (hand == Hand.MAIN_HAND) {
                InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            }
        } else {
            if (hand == Hand.OFF_HAND) {
                InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            }
            playerInventory.selectedSlot = hotbarSlot;
            InteractionManager.push(new InteractionManager.PacketEvent(new UpdateSelectedSlotC2SPacket(hotbarSlot), InteractionManager.TICK_WAITER));
            if (hand == Hand.OFF_HAND) {
                InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            }
        }
    }

    private static void refillFromInventory(Hand hand, int inventorySlot) {
        if (hand == Hand.OFF_HAND) {
            ItemStack mainHandStack = playerInventory.getMainHandStack();
            InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);

            pickFromInventory(inventorySlot);

            InteractionManager.push(InteractionManager.SWAP_WITH_OFFHAND_EVENT);
            // Sometimes the swapping visually duplicates the stack on the client,
            // so we're manually fixing the visuals here
            InteractionManager.push(() -> {
                playerInventory.setStack(playerInventory.selectedSlot, mainHandStack);
                return InteractionManager.DUMMY_WAITER;
            });
        } else {
            pickFromInventory(inventorySlot);
        }
    }

    private static void pickFromInventory(int inventorySlot) {
        InteractionManager.push(new InteractionManager.PacketEvent(new PickFromInventoryC2SPacket(inventorySlot), triggerType -> triggerType == InteractionManager.TriggerType.HELD_ITEM_CHANGE));
    }

    static {
        rules.add(new BlockRule());
        rules.add(new ItemGroupRule());
        rules.add(new ItemHierarchyRule());
        rules.add(new BlockHierarchyRule());
        rules.add(new FoodRule());
        rules.add(new EqualItemRule());
        rules.add(new EqualStackRule());
    }

    public abstract static class Rule {
        /**
         * Creates a new rule.
         * Automatically registers this rule to the list of rules.
         */
        protected Rule() {
            rules.add(this);
        }

        /**
         * Checks if the rule is valid for the given stack.
         *
         * @param oldStack The stack to check.
         * @return Whether the rule applies to the given stack.
         */
        abstract boolean matches(ItemStack oldStack);

        /**
         * Find a matching slot for the given base stack in the player inventory.
         *
         * @param playerInventory The player inventory to search in.
         * @param oldStack        The base stack to search for.
         * @return The slot index of the matching stack or -1 if no match was found.
         */
        abstract int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack);

        /***
         * Utility function that iterates over all slots of the player inventory and returns the first slot that matches the given predicate.
         * @param playerInventory The player inventory to search in.
         * @param predicate       The predicate to check for.
         * @return The slot index of the matching stack or -1 if no match was found.
         */
        protected int iterateInventory(PlayerInventory playerInventory, Predicate<ItemStack> predicate) {
            for (int i = 0; i < playerInventory.main.size(); i++) {
                if (predicate.test(playerInventory.main.get(i))) return i;
            }
            return -1;
        }
    }

    public static class BlockRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.anyBlock() && oldStack.getItem() instanceof BlockItem;
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            return iterateInventory(playerInventory, stack -> stack.getItem() instanceof BlockItem);
        }
    }

    public static class ItemGroupRule extends Rule {

        private static boolean containsBroad(ItemGroup group, ItemStack stack) {
            return group.contains(stack) || group.contains(stack.getItem().getDefaultStack());
        }

        @Override
        boolean matches(ItemStack oldStack) {
            if (!MWConfigHandler.getConfig().refill.rules.itemgroup()) {
                return false;
            }
            for (ItemGroup group : ItemGroups.getGroups()) {
                if (containsBroad(group, oldStack)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            List<ItemGroup> checkGroups = new ArrayList<>();
            for (ItemGroup group : ItemGroups.getGroups()) {
                if (containsBroad(group, oldStack)) {
                    checkGroups.add(group);
                }
            }
            if (checkGroups.isEmpty()) {
                return -1;
            }
            return iterateInventory(playerInventory, stack -> {
                for (ItemGroup group : checkGroups) {
                    if (containsBroad(group, stack)) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    public static class ItemHierarchyRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.itemHierarchy() && oldStack.getItem().getClass() != Item.class && !(oldStack.getItem() instanceof BlockItem);
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            return findBestThroughClassHierarchy(oldStack, playerInventory.main, Item::getClass, Item.class);
        }
    }

    public static class BlockHierarchyRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.blockHierarchy() && oldStack.getItem() instanceof BlockItem;
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            return findBestThroughClassHierarchy(oldStack, playerInventory.main, item -> {
                if (item instanceof BlockItem) {
                    return ((BlockItem) item).getBlock().getClass();
                } else {
                    return null;
                }
            }, Block.class);
        }
    }

    private static int findBestThroughClassHierarchy(ItemStack baseStack, DefaultedList<ItemStack> inventory, Function<Item, Class<?>> getClass, Class<?> baseClass) {
        int currentRank = 0;
        Collection<Class<?>> classes = new ArrayList<>(10);
        Class<?> clazz = getClass.apply(baseStack.getItem());
        while (clazz != baseClass) {
            classes.add(clazz);
            clazz = clazz.getSuperclass();
        }
        int classesSize = classes.size();
        if (classesSize == 0) return -1;

        int index = -1;

        outer:
        for (int i = 0; i < inventory.size(); i++) {
            clazz = getClass.apply(inventory.get(i).getItem());
            if (clazz == null) {
                continue;
            }
            while (clazz != baseClass) {
                int classRank = classesSize;
                for (Iterator<Class<?>> iterator = classes.iterator(); iterator.hasNext(); classRank--) {
                    if (classRank <= 0) break;
                    if (classRank <= currentRank) continue outer;
                    if (Objects.equals(clazz, iterator.next())) {
                        if (classRank >= classesSize) return i;
                        currentRank = classRank;
                        index = i;
                        continue outer;
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
        return index;
    }

    public static class FoodRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.food() && oldStack.getComponents().contains(DataComponentTypes.FOOD);
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            return iterateInventory(playerInventory, itemStack -> itemStack.getComponents().contains(DataComponentTypes.FOOD));
        }
    }

    public static class EqualItemRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.equalItems();
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            Item item = oldStack.getItem();
            return iterateInventory(playerInventory, stack -> stack.getItem() == item);
        }
    }

    public static class EqualStackRule extends Rule {
        @Override
        boolean matches(ItemStack oldStack) {
            return MWConfigHandler.getConfig().refill.rules.equalStacks();
        }

        @Override
        int findMatchingStack(PlayerInventory playerInventory, ItemStack oldStack) {
            return playerInventory.indexOf(oldStack);
        }
    }
}
