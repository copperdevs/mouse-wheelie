package de.siphalor.mousewheelie.client.mixin;

import de.siphalor.mousewheelie.util.IRecipeBookGui;
import de.siphalor.mousewheelie.util.IScrollableRecipeBook;
import net.minecraft.client.gui.ContainerScreen;
import net.minecraft.client.gui.container.AbstractFurnaceRecipeBookScreen;
import net.minecraft.client.gui.container.AbstractFurnaceScreen;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceScreen.class)
public abstract class MixinAbstractFurnaceScreen extends ContainerScreen implements IScrollableRecipeBook {

	@Shadow @Final public AbstractFurnaceRecipeBookScreen recipeBook;

	public MixinAbstractFurnaceScreen(Container container_1, PlayerInventory playerInventory_1, Component textComponent_1) {
		super(container_1, playerInventory_1, textComponent_1);
	}

	@Override
	public boolean mouseWheelie_onMouseScrollRecipeBook(double mouseX, double mouseY, double scrollAmount) {
		return ((IRecipeBookGui) recipeBook).mouseWheelie_scrollRecipeBook(mouseX, mouseY, scrollAmount);
	}
}
