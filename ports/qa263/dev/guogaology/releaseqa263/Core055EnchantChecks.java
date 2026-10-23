package dev.guogaology.releaseqa263;

import dev.guogaology.mining.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Real native enchant/anvil/grindstone/menu entrypoints; no production test hooks. */
public final class Core055EnchantChecks {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("0.5.5 enchant: "+label);checks++;}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static void updateGrindstone(GrindstoneMenu menu)throws ReflectiveOperationException{var method=GrindstoneMenu.class.getDeclaredMethod("createResult");method.setAccessible(true);method.invoke(menu);}
    public static void run(ServerPlayer player)throws Exception{
        checks=0;var inventory=new ArrayList<ItemStack>();for(int i=0;i<player.getInventory().getContainerSize();i++)inventory.add(player.getInventory().getItem(i));
        var mode=player.gameMode();var world=player.level();var pos=player.blockPosition();var prior=world.getBlockState(pos);
        var arrows=new ArrayList<AbstractArrow>();
        try{
            player.closeContainer();player.getInventory().clearContent();player.setGameMode(GameType.SURVIVAL);
            var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var efficiency=registry.getOrThrow(Enchantments.EFFICIENCY);var sharpness=registry.getOrThrow(Enchantments.SHARPNESS);var protection=registry.getOrThrow(Enchantments.PROTECTION);var power=registry.getOrThrow(Enchantments.POWER);
            var unbreaking=registry.getOrThrow(Enchantments.UNBREAKING);var infinity=registry.getOrThrow(Enchantments.INFINITY);var flame=registry.getOrThrow(Enchantments.FLAME);
            var branch=item("hydra_bud");
            for(var entry:MiningContent.GEAR.entrySet()){
                var spec=entry.getValue();var stack=new ItemStack(entry.getKey());GearData.refresh(stack);
                if(spec.kind()==EquipmentRules.MANUSCRIPT){check(!stack.isEnchantable(),"empty manuscripts have no fake vanilla category");continue;}
                check(stack.isEnchantable(),"empty gear table eligibility "+entry.getKey());
                var enchant=spec.kind()==0?efficiency:spec.kind()==1?sharpness:spec.kind()==EquipmentRules.BOW?power:protection;
                check(enchant.value().canEnchant(stack)&&enchant.value().isPrimaryItem(stack),"native table category "+entry.getKey());
                check(unbreaking.value().canEnchant(stack),"native durability category "+entry.getKey());
                stack.enchant(enchant,1);check(stack.isEnchanted(),"native enchant application "+entry.getKey());
                GearData.refresh(stack);check(stack.isEnchanted(),"refresh preserves native enchantment "+entry.getKey());
                check("enchanted".equals(GearData.installationError(stack,branch,3)),"enchanted item rejects installation "+entry.getKey());
                stack.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);check(!GearData.forbidsSockets(stack),"disenchanting reopens socket path");
            }
            var pick=item("number_pickaxe");pick.set(MiningContent.DIGIT,9d);GearData.refresh(pick);
            check(GearData.install(pick,branch.copy(),3)==null,"numeric pick socket installation");
            check(!pick.isEnchantable()&&!efficiency.value().canEnchant(pick),"socketed numeric pick refuses table and book selection");
            pick.enchant(efficiency,5);check(!pick.isEnchanted(),"direct native enchant operation cannot bypass sockets");
            var reclaimed=GearData.remove(pick,0);check(reclaimed.is(branch.getItem())&&pick.isEnchantable(),"last core removal restores native enchanting");
            pick.enchant(efficiency,5);var before=pick.copy();var offered=branch.copy();check("enchanted".equals(GearData.install(pick,offered,3))&&offered.getCount()==1&&ItemStack.matches(before,pick),"failed install consumes nothing");

            // Force a mixed component stack as a command/other mod could. Preserve
            // ownership, suppress every socket effect, and reactivate on disenchant.
            var stored=pick.get(DataComponents.ENCHANTMENTS);pick.set(MiningContent.CORES,List.of(branch.copy()));
            check(GearData.profile(pick).isEmpty()&&GearData.points(pick,2)==0,"malformed mixed stack cannot stack core effects");
            GearData.refresh(pick);check(pick.isEnchanted()&&GearData.cores(pick).size()==1,"mixed stack refresh never destroys either material");
            pick.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);check(!GearData.profile(pick).isEmpty(),"profile cache reopens after enchant removal");

            // Real station slots, not a simulated predicate: normal and shift-click
            // installation must both reject enchanted tools without stealing cores.
            check(world.getBlockEntity(pos)==null,"station fixture has no existing block entity");world.setBlock(pos,MiningContent.TABLES[2].defaultBlockState(),2);
            var menu=new EnhancementMenu(155,player.getInventory(),world,pos,3);var enchanted=item("true_omega_pickaxe");enchanted.enchant(efficiency,3);menu.getSlot(0).set(enchanted);
            check(!menu.getSlot(1).mayPlace(branch)&&!menu.canSelectInventoryItem(branch),"station insertion and highlights agree for enchanted gear");
            menu.getSlot(EnhancementMenu.INVENTORY).set(branch.copy());menu.quickMoveStack(player,EnhancementMenu.INVENTORY);
            check(GearData.cores(enchanted).isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"shift-click cannot bypass enchant exclusion");menu.getSlot(0).set(ItemStack.EMPTY);

            var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,stored);
            var anvil=new AnvilMenu(156,player.getInventory(),ContainerLevelAccess.create(world,pos));var sword=item("true_omega_pickaxe");
            anvil.getSlot(0).set(sword);anvil.getSlot(1).set(book);anvil.createResult();check(!anvil.getSlot(2).getItem().isEmpty()&&anvil.getSlot(2).getItem().isEnchanted(),"native anvil book applies to empty ordinal gear");
            GearData.setCores(sword,List.of(branch.copy()));anvil.getSlot(0).set(sword);anvil.createResult();check(anvil.getSlot(2).getItem().isEmpty(),"anvil book cannot enchant socketed gear");
            var grinder=new GrindstoneMenu(157,player.getInventory(),ContainerLevelAccess.create(world,pos));grinder.getSlot(0).set(enchanted);updateGrindstone(grinder);
            var cleaned=grinder.getSlot(2).getItem();check(!cleaned.isEmpty()&&!cleaned.isEnchanted()&&GearData.installationError(cleaned,branch,3)==null,"native grindstone restores socket eligibility");

            // A Totem is a real occupied slot too; malformed enchanted manuscripts
            // must not activate or consume stored cores/Totems.
            var manuscript=item("true_omega_manuscript");GearData.setCores(manuscript,List.of(new ItemStack(Items.TOTEM_OF_UNDYING),item("sequence_core")));
            check(GearData.forbidsEnchantments(manuscript),"Totem occupancy reserves socket path");manuscript.set(DataComponents.ENCHANTMENTS,stored);
            check(GearData.profile(manuscript).isEmpty()&&GearData.socketTotems(manuscript)==0&&!GearData.consumeSocketTotem(manuscript),"mixed manuscript has no socket effects or phantom Totem activation");
            check(GearData.cores(manuscript).size()==2,"suppression preserves real manuscript contents");

            // Native Infinity and Flame actually work on the custom bow release.
            player.getInventory().clearContent();var bow=item("true_omega_bow");bow.enchant(infinity,1);bow.enchant(flame,1);var ammo=new ItemStack(Items.ARROW,2);player.setItemSlot(EquipmentSlot.MAINHAND,bow);player.getInventory().setItem(19,ammo);
            var priorArrows=new HashSet<UUID>();for(var arrow:world.getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(8)))priorArrows.add(arrow.getUUID());
            check(BowEffects.release(bow,world,player,1,4),"native enchanted ordinal bow releases");
            for(var arrow:world.getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(8),a->!priorArrows.contains(a.getUUID())))arrows.add(arrow);
            check(arrows.size()==1&&ammo.getCount()==2,"native Infinity keeps real ammunition");check(arrows.getFirst().isOnFire()&&arrows.getFirst().pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"native Flame and free-arrow creative-only pickup policy");
            System.out.println("CORE055_ENCHANT_CHECKS_OK checks="+checks+" native table categories/anvil/grindstone/sockets/cache/Infinity/Flame");
        }finally{
            for(var arrow:arrows)arrow.discard();player.closeContainer();player.containerMenu=player.inventoryMenu;world.setBlock(pos,prior,2);
            player.getInventory().clearContent();for(int i=0;i<inventory.size();i++)player.getInventory().setItem(i,inventory.get(i));player.setGameMode(mode);
        }
    }
}
