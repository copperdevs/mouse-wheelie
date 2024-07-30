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

package de.siphalor.mousewheelie.client.util;

import com.google.common.collect.Sets;
import de.siphalor.mousewheelie.MWConfig;
import de.siphalor.mousewheelie.MouseWheelie;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.awt.*;
import java.util.Iterator;
import java.util.Set;

public class ItemStackUtils {
    private static final ComponentMap EMPTY_COMPONENT_MAP = new ComponentMapImpl(ComponentMap.EMPTY);

    public static boolean canCombine(ItemStack a, ItemStack b) {
        return ItemStack.areItemsAndComponentsEqual(a, b);
    }

    public static int compareEqualItems(ItemStack a, ItemStack b) {
        // compare counts
        int cmp = Integer.compare(b.getCount(), a.getCount());
        if (cmp != 0) {
            return cmp;
        }
        return compareEqualItems2(a, b);
    }

    private static int compareEqualItems2(ItemStack a, ItemStack b) {
        // compare names
        if (!a.contains(DataComponentTypes.CUSTOM_NAME)) // True means a has custom name
        {
            if (b.contains(DataComponentTypes.CUSTOM_NAME)) // True means b does not have custom name
            {
                return -1;
            }
            return compareEqualItems3(a, b);
        }
        // a does not have custom name
        if (!b.contains(DataComponentTypes.CUSTOM_NAME)) // true means b has custom name
        {
            return 1;
        }
        return compareEqualItems3(a, b);
    }

    private static int compareEqualItems3(ItemStack a, ItemStack b) {
        // compare tooltips
        Iterator<Text> tooltipsA = a.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC).iterator();
        Iterator<Text> tooltipsB = b.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC).iterator();

        while (tooltipsA.hasNext()) {
            if (!tooltipsB.hasNext()) {
                return 1;
            }

            int cmp = tooltipsA.next().getString().compareToIgnoreCase(tooltipsB.next().getString());
            if (cmp != 0) {
                return cmp;
            }
        }
        if (tooltipsB.hasNext()) {
            return -1;
        }
        return compareEqualItems4(a, b);
    }

    private static int compareEqualItems4(ItemStack a, ItemStack b) {
        // compare special item properties
        if (a.isIn(ItemTags.DYEABLE)) {
            int colorA = DyedColorComponent.getColor(a, -6265536);
            int colorB = DyedColorComponent.getColor(b, -6265536);
            float[] hsbA = Color.RGBtoHSB(colorA >> 16 & 0xFF, colorA >> 8 & 0xFF, colorA & 0xFF, null);
            float[] hsbB = Color.RGBtoHSB(colorB >> 16 & 0xFF, colorB >> 8 & 0xFF, colorB & 0xFF, null);
            int cmp = Float.compare(hsbA[0], hsbB[0]);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Float.compare(hsbA[1], hsbB[1]);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Float.compare(hsbA[2], hsbB[2]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return compareEqualItems5(a, b);
    }

    private static int compareEqualItems5(ItemStack a, ItemStack b) {
        // compare damage
        return Integer.compare(a.getDamage(), b.getDamage());
    }

    public static ComponentMap getComponentOrEmpty(ItemStack stack) {
        return stack.getComponentChanges().isEmpty() ? EMPTY_COMPONENT_MAP: stack.getComponents();
    }

    public static boolean areComponentsEqualExcept(ItemStack a, ItemStack b, String... componentNames) {
        ComponentMap tagA = getComponentOrEmpty(a);
        ComponentMap tagB = getComponentOrEmpty(b);
        Set<String> checkedComponents = Sets.newHashSet(componentNames);
        if (!areTagsEqualExceptOneSided(tagA, tagB, checkedComponents)) {
            return false;
        }
        return areTagsEqualExceptOneSided(tagB, tagA, checkedComponents);
    }

    private static boolean areTagsEqualExceptOneSided(ComponentMap compA, ComponentMap compB, Set<String> checkedComponentTypes) {
        for (Component comp : compA) {
            if (checkedComponentTypes.contains(comp.comp_2443().toString())) {
                continue;
            }
            if (!compB.contains(comp.comp_2443())) {
                return false;
            }
            checkedComponentTypes.add(comp.comp_2443().toString());
        }
        return true;
    }

    public static boolean areItemsOfSameKind(ItemStack stack1, ItemStack stack2) {
        return areItemsOfSameKind(stack1, stack2, MouseWheelie.CONFIG.general.itemComponentMatchMode());
    }

    public static boolean areItemsOfSameKind(ItemStack stack1, ItemStack stack2, ComponentTypeMatchMode mode) {
        switch (mode) {
            case NONE -> {
                return stack1.getItem() == stack2.getItem();
            }
            case ALL -> {
                return ItemStack.areEqual(stack1, stack2);
            }
            case SOME -> {
                if (!ItemStack.areItemsEqual(stack1, stack2)) {
                    return false;
                }
                return areComponentsEqualExcept(stack1, stack2, DataComponentTypes.DAMAGE.toString(),DataComponentTypes.ENCHANTMENTS.toString());
            }
        }
        return false; // unreachable
    }

    public static int hashByKind(ItemStack stack, ComponentTypeMatchMode mode) {
        switch (mode) {
            case NONE:
                return stack.getItem().hashCode();
            case ALL:
                return stack.hashCode();
            case SOME:
                HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
                        .append(stack.getItem());

                ComponentMap componentMap = stack.getComponents();
                if (componentMap == null) {
                    return hashCodeBuilder.toHashCode();
                }

                for (Component component : componentMap)
                {
                    if (!(component.comp_2443().equals(DataComponentTypes.DAMAGE) || component.comp_2443().equals(DataComponentTypes.ENCHANTMENTS)))
                    {
                        hashCodeBuilder.append(component.comp_2443().toString()).append(component.comp_2444().toString());
                    }
                }
        }
        return 0; // unreachable
    }

    public enum ComponentTypeMatchMode {
        NONE, SOME, ALL
    }
}