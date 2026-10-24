package dev.guogaology.mining;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Compact, read-only item tooltip; the enhancement station retains quantitative previews. */
public final class EquipmentTooltips {
    private EquipmentTooltips() {}

    public static void append(ItemStack stack,int tier,int kind,Consumer<Text> out) {
        var installed=stack.getOrDefault(MiningContent.CORES,List.<ItemStack>of()).stream().filter(core->!core.isEmpty()).toList();
        // Profile reads may normalize legacy Absence components: restrict that to an owned copy.
        var view=stack.copy();
        var traits=EquipmentTooltipRules.traitKeys(kind,GearData.profile(view),GearData.silkTouch(view),GearData.socketTotems(view)>0);
        if(!traits.isEmpty()) {
            Text summary=Text.translatable(traits.getFirst());
            for(int i=1;i<traits.size();i++)summary=Text.translatable("mining.guogaology.tooltip.trait_pair",summary,Text.translatable(traits.get(i)));
            out.accept(Text.translatable("mining.guogaology.tooltip.traits",summary).formatted(Formatting.GRAY));
        }
        out.accept(Text.translatable("mining.guogaology.tooltip.slots",installed.size(),EquipmentRules.slots(tier,kind)).formatted(Formatting.GRAY));
        for(int i=0;i<installed.size();i+=2) {
            var first=entry(installed.get(i));
            out.accept(i+1<installed.size()?Text.translatable("mining.guogaology.tooltip.core_pair",first,entry(installed.get(i+1))):first);
        }
    }

    private static Text entry(ItemStack core) {
        int type=GearData.type(core);
        if(type<0)return core.getName().copy();
        int grade=EquipmentRules.displayedCoreGrade(type,GearData.level(core));
        if(grade<=0)return Text.translatable("mining.guogaology.tooltip.ordinal_crystal");
        return Text.translatable("mining.guogaology.tooltip.core_entry",Text.translatable("mining.guogaology.tooltip.core."+type),grade);
    }
}
