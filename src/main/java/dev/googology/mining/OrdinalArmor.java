package dev.googology.mining;

import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import java.util.List;

public final class OrdinalArmor extends ArmorItem {
    private final int tier,kind;
    public OrdinalArmor(RegistryEntry<ArmorMaterial> material,Type type,Settings settings,int tier,int kind){super(material,type,settings);this.tier=tier;this.kind=kind;}
    @Override public void inventoryTick(ItemStack s,net.minecraft.world.World world,net.minecraft.entity.Entity entity,int slot,boolean selected){if(!world.isClient&&(s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION||s.getOrDefault(MiningContent.DEEP,false)!=ManuscriptEffects.deep(world))){s.set(MiningContent.DEEP,ManuscriptEffects.deep(world));GearData.refresh(s);}}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> out,TooltipType type){
        out.add(Text.translatable("mining.googology.native_armor",EquipmentRules.format(EquipmentRules.nativeArmor(tier,kind)),EquipmentRules.format(EquipmentRules.nativeToughness(tier,kind))));
        out.add(Text.translatable("mining.googology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,kind),EquipmentRules.grade(tier)));
        if(GearData.points(stack,3)>0)out.add(Text.translatable("mining.googology.wear_factor",EquipmentRules.format(EquipmentRules.wearFactor(GearData.profile(stack),java.util.List.of()))));
        for(var c:GearData.cores(stack))out.add(Text.literal("• ").append(c.getName()).append(" — ").append(Text.translatable("mining.googology.effect."+GearData.type(c))));
    }
}
