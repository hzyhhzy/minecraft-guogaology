package dev.guogaology.client.mixin;

import dev.guogaology.mining.MiningContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Compact equipment hover text without modifying the item's actual attributes. */
@Mixin(ItemStack.class)
public abstract class EquipmentTooltipAttributesMixin {
    @Unique private static final String GUOGAOLOGY_TRAITS = "mining.guogaology.tooltip.traits";

    @Inject(method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void guogaology$compactEquipmentAttributes(CallbackInfo ci) {
        if (MiningContent.GEAR.containsKey(((ItemStack) (Object) this).getItem())) ci.cancel();
    }

    /** Wrap only our discrete-capability row; native/debug/core rows retain their objects. */
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void guogaology$wrapEquipmentTraits(CallbackInfoReturnable<List<Component>> cir) {
        if (!MiningContent.GEAR.containsKey(((ItemStack) (Object) this).getItem())) return;
        var client = Minecraft.getInstance();
        if (client == null || client.font == null || client.getWindow() == null) return;
        int width = Math.max(1, Math.min(320, client.getWindow().getGuiScaledWidth() - 24));
        var original = cir.getReturnValue();
        List<Component> wrapped = null;
        for (int i = 0; i < original.size(); i++) {
            var line = original.get(i);
            if (line.getContents() instanceof TranslatableContents content
                    && content.getKey().equals(GUOGAOLOGY_TRAITS) && client.font.width(line) > width) {
                if (wrapped == null) {
                    wrapped = new ArrayList<>(original.size() + 4);
                    wrapped.addAll(original.subList(0, i));
                }
                for (var fragment : client.font.getSplitter().splitLines(line, width, Style.EMPTY))
                    wrapped.add(Component.translatable(GUOGAOLOGY_TRAITS, guogaology$logicalTraitLine(fragment)).setStyle(line.getStyle()));
            } else if (wrapped != null) wrapped.add(line);
        }
        if (wrapped != null) cir.setReturnValue(wrapped);
    }

    /** Preserve logical text and segment styles, leaving bidi ordering to the final renderer. */
    @Unique
    private static Component guogaology$logicalTraitLine(FormattedText fragment) {
        var result = Component.empty();
        fragment.visit((style, text) -> {
            result.append(Component.literal(text).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
