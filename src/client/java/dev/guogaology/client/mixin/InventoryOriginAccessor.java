package dev.guogaology.client.mixin;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(HandledScreen.class)
public interface InventoryOriginAccessor {
    @Accessor("x") int guogaology$left();
    @Accessor("y") int guogaology$top();
}
