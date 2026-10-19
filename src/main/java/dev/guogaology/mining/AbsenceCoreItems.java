package dev.guogaology.mining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/** Removes retired placement flags from the finished base Absence Core only. */
public final class AbsenceCoreItems {
    private AbsenceCoreItems() {}

    // Do not reference GuogaologyBlocks or MiningContent here: item loading may call
    // this before either class has completed its registry initialization.
    private static boolean needsNormalization(ItemStack stack) {
        if(stack.isEmpty())return false;
        var id=Registries.ITEM.getId(stack.getItem());
        if(id==null||!id.getNamespace().equals("guogaology")||!id.getPath().equals("lho_trace"))return false;
        var state=stack.get(DataComponentTypes.BLOCK_STATE);
        if(state==null)return false;
        var properties=state.properties();
        return properties.containsKey("stable")||properties.containsKey("anchored")||properties.containsKey("player_placed");
    }

    /** Mutates only the owned stack passed by the caller; count and other data survive. */
    public static boolean normalizeStandalone(ItemStack stack) {
        if(!needsNormalization(stack))return false;
        var properties=new LinkedHashMap<>(stack.get(DataComponentTypes.BLOCK_STATE).properties());
        properties.remove("stable");properties.remove("anchored");properties.remove("player_placed");
        if(properties.isEmpty())stack.remove(DataComponentTypes.BLOCK_STATE);
        else stack.set(DataComponentTypes.BLOCK_STATE,new BlockStateComponent(Map.copyOf(properties)));
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
