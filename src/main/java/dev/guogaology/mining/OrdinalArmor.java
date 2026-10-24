package dev.guogaology.mining;

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
        EquipmentTooltips.append(stack,tier,kind,out::add);
    }
}
