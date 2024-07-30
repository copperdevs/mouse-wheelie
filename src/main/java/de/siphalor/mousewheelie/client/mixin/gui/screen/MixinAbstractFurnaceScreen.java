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

package de.siphalor.mousewheelie.client.mixin.gui.screen;

import de.siphalor.mousewheelie.client.util.ScrollAction;
import de.siphalor.mousewheelie.client.util.inject.IRecipeBookWidget;
import de.siphalor.mousewheelie.client.util.inject.IScrollableRecipeBook;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.AbstractFurnaceRecipeBookScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceScreen.class)
public abstract class MixinAbstractFurnaceScreen extends HandledScreen<AbstractFurnaceScreenHandler> implements IScrollableRecipeBook {

    @Shadow
    @Final
    public AbstractFurnaceRecipeBookScreen recipeBook;

    public MixinAbstractFurnaceScreen(AbstractFurnaceScreenHandler container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Override
    public ScrollAction mouseWheelie_onMouseScrollRecipeBook(double mouseX, double mouseY, double scrollAmount) {
        return ((IRecipeBookWidget) recipeBook).mouseWheelie_scrollRecipeBook(mouseX, mouseY, scrollAmount);
    }
}
