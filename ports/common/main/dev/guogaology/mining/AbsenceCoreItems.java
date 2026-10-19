package dev.guogaology.mining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** Removes retired placement flags from the finished base Absence Core only. */
public final class AbsenceCoreItems {
    private AbsenceCoreItems() {}

    // Do not reference GuogaologyBlocks or MiningContent here: item loading may call
    // this before either class has completed its registry initialization.
    private static boolean needsNormalization(ItemStack stack) {
        if(stack.isEmpty())return false;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(id==null||!id.getNamespace().equals("guogaology")||!id.getPath().equals("lho_trace"))return false;
        var state=stack.get(DataComponents.BLOCK_STATE);
        if(state==null)return false;
        var properties=state.properties();
        return properties.containsKey("stable")||properties.containsKey("anchored")||properties.containsKey("player_placed");
    }

    /** Mutates only the owned stack passed by the caller; count and other data survive. */
    public static boolean normalizeStandalone(ItemStack stack) {
        if(!needsNormalization(stack))return false;
        var properties=new LinkedHashMap<>(stack.get(DataComponents.BLOCK_STATE).properties());
        properties.remove("stable");properties.remove("anchored");properties.remove("player_placed");
        if(properties.isEmpty())stack.remove(DataComponents.BLOCK_STATE);
        else stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(Map.copyOf(properties)));
        return true;
    }

    /** Returns an owned copy, leaving the source stack and all its components untouched. */
    public static ItemStack normalizedCopy(ItemStack stack) {
        var copy=stack.copy();normalizeStandalone(copy);return copy;
    }

    /** No-op reads retain list identity; changed reads copy only affected slot stacks. */
    public static List<ItemStack> normalizedInstalled(List<ItemStack> slots) {
        ArrayList<ItemStack> normalized=null;
        for(int i=0;i<slots.size();i++)if(needsNormalization(slots.get(i))) {
            if(normalized==null)normalized=new ArrayList<>(slots);
            normalized.set(i,normalizedCopy(slots.get(i)));
        }
        return normalized==null?slots:List.copyOf(normalized);
    }
}
