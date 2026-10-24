package dev.guogaology.client.mixin;

import dev.guogaology.mining.MiningContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
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

    @Inject(method = "appendAttributeModifiersTooltip", at = @At("HEAD"), cancellable = true)
    private void guogaology$compactEquipmentAttributes(CallbackInfo ci) {
        if (MiningContent.GEAR.containsKey(((ItemStack) (Object) this).getItem())) ci.cancel();
    }

    /** Wrap only our discrete-capability row; native/debug/core rows retain their objects. */
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void guogaology$wrapEquipmentTraits(CallbackInfoReturnable<List<Text>> cir) {
        if (!MiningContent.GEAR.containsKey(((ItemStack) (Object) this).getItem())) return;
        var client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null || client.getWindow() == null) return;
        int width = Math.max(1, Math.min(320, client.getWindow().getScaledWidth() - 24));
        var original = cir.getReturnValue();
        List<Text> wrapped = null;
        for (int i = 0; i < original.size(); i++) {
            var line = original.get(i);
            if (line.getContent() instanceof TranslatableTextContent content
                    && content.getKey().equals(GUOGAOLOGY_TRAITS) && client.textRenderer.getWidth(line) > width) {
                if (wrapped == null) {
                    wrapped = new ArrayList<>(original.size() + 4);
                    wrapped.addAll(original.subList(0, i));
                }
                for (var fragment : client.textRenderer.getTextHandler().wrapLines(line, width, Style.EMPTY))
                    wrapped.add(Text.translatable(GUOGAOLOGY_TRAITS, guogaology$logicalTraitLine(fragment)).setStyle(line.getStyle()));
            } else if (wrapped != null) wrapped.add(line);
        }
        if (wrapped != null) cir.setReturnValue(wrapped);
    }

    /** Preserve logical text and segment styles, leaving bidi ordering to the final renderer. */
    @Unique
    private static Text guogaology$logicalTraitLine(StringVisitable fragment) {
        var result = Text.empty();
        fragment.visit((style, text) -> {
            result.append(Text.literal(text).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
