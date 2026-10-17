package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.CoreGrades;
import dev.guogaology.block.CoreMiningRules;
import dev.guogaology.block.PortableRelicBlock;
import dev.guogaology.mining.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** 0.5.7 real loaded registries, armor attributes, survival progress and drops.
 * Wood is deliberately excluded: user requested static inspection only.
 */
public final class Balance057Checks {
    private static int checks;
    private static final String[] MINERALS={"omega","epsilon","gamma","true_omega"};
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final String[] PIECES={"helmet","chestplate","leggings","boots"};
    private Balance057Checks(){}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double value,double expected,String message){check(Math.abs(value-expected)<1e-6*Math.max(1,Math.abs(expected)),message+": "+value+" != "+expected);}
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+id));}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static void update(ServerPlayer p)throws Exception{var m=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");m.setAccessible(true);m.invoke(p);}
    private static void fluid(ServerPlayer p)throws Exception{var m=Entity.class.getDeclaredMethod("updateFluidInteraction");m.setAccessible(true);m.invoke(p);}
    private static int clientTicks(float progress){float value=0;int ticks=0;while(value<1&&ticks<1000){value+=progress;ticks++;}return ticks;}

    public static void run(ServerPlayer p)throws Exception {
        checks=0;var world=p.level();var oldPosition=p.position();var oldVelocity=p.getDeltaMovement();var oldMode=p.gameMode();
        boolean oldGround=p.onGround(),oldGravity=p.isNoGravity();var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var inventory=p.getInventory();var savedItems=new ArrayList<ItemStack>();for(int i=0;i<inventory.getContainerSize();i++)savedItems.add(inventory.getItem(i));
        var savedBlocks=new LinkedHashMap<BlockPos,BlockState>();var feet=p.blockPosition().above(5);var sample=feet.offset(3,0,0);
        try {
            p.closeContainer();for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,ItemStack.EMPTY);
            for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);update(p);
            p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(feet.getX()+.5,feet.getY(),feet.getZ()+.5);p.setOnGround(true);
            for(var pos:List.of(feet,feet.above(),sample)){savedBlocks.put(pos,world.getBlockState(pos));world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}fluid(p);
            oreCounts(p);armor(p);fixedCores(p,sample,feet);ordinaryCrystal(p,sample);
            System.out.println("BALANCE057_NATIVE_OK checks="+checks+" armor integers/real attributes +27cores fixed100/200/400ticks +tool/effects/book/water/air independence +native drops +ordinary crystal stone hardness +ore24/6/6/4; wood static-only");
        }finally{
            p.removeAllEffects();for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);
            for(int i=0;i<savedItems.size();i++)inventory.setItem(i,savedItems.get(i));update(p);
            for(var e:savedBlocks.entrySet())world.setBlock(e.getKey(),e.getValue(),2);
            p.setPos(oldPosition);p.setDeltaMovement(oldVelocity);p.setOnGround(oldGround);p.setNoGravity(oldGravity);fluid(p);p.setGameMode(oldMode);
            for(var effect:effects)p.addEffect(effect);
        }
    }
    private static void oreCounts(ServerPlayer p){
        var registry=p.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for(int i=0;i<MINERALS.length;i++)for(String suffix:new String[]{"","_hell"}){
            var key=net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,Identifier.parse("guogaology:ore_"+MINERALS[i]+suffix));
            var f=registry.getOrThrow(key).value();int count=0;
            for(var modifier:f.placement())if(modifier instanceof net.minecraft.world.level.levelgen.placement.CountPlacement){
                long n=modifier.getPositions(new net.minecraft.world.level.levelgen.placement.PlacementContext(p.level(),p.level().getChunkSource().getGenerator(),Optional.empty()),net.minecraft.util.RandomSource.create(57),BlockPos.ZERO).count();
                check(n==new int[]{24,6,6,4}[i],"loaded ore candidate count "+key);count++;
            }
            check(count==1,"exactly one count placement "+key);
        }
    }
    private static void armor(ServerPlayer p)throws Exception{
        int[][] expected={{2,6,5,2},{3,8,6,3},{4,9,7,4},{5,11,9,5}};
        for(int t=0;t<4;t++){
            for(int k=0;k<4;k++){
                near(EquipmentRules.nativeArmor(t+1,k+2),expected[t][k],"piece armor integer");
                near(EquipmentRules.nativeToughness(t+1,k+2),new int[]{0,2,3,4}[t],"piece toughness integer");
                if(t>=2){check(expected[t][k]>=new int[]{3,8,6,3}[k],"all high pieces beat reference armor");check(new int[]{0,2,3,4}[t]>=3,"all high pieces match netherite toughness");}
                var stack=item(MINERALS[t]+"_"+PIECES[k]);GearData.refresh(stack);p.setItemSlot(ARMOR[k],stack);
            }
            update(p);near(p.getAttributeValue(Attributes.ARMOR),new int[]{15,20,24,30}[t],"real worn full armor tier "+t);
            near(p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),new int[]{0,8,12,16}[t],"real worn toughness tier "+t);
            // Force the old revision marker and prove existing stacks refresh too.
            var head=p.getItemBySlot(EquipmentSlot.HEAD);head.set(MiningContent.RULES,48);head.getItem().inventoryTick(head,p.level(),p,EquipmentSlot.HEAD);
            check(head.getOrDefault(MiningContent.RULES,0)==49,"old armor refreshes to current rules");
        }
        for(var slot:ARMOR)p.setItemSlot(slot,ItemStack.EMPTY);update(p);
    }
    private static void fixedCores(ServerPlayer p,BlockPos sample,BlockPos feet)throws Exception{
        var fast=new ItemStack(Items.NETHERITE_PICKAXE);fast.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY),5);
        var book=item("true_omega_manuscript");GearData.setCores(book,Collections.nCopies(6,item("sequence_core_lv3")));GearData.refresh(book);
        var tools=List.of(ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.IRON_AXE),fast,item("true_omega_pickaxe"));
        int cores=0;
        for(String root:CoreGrades.ROOTS)for(int level=root.equals("ordinal_crystal")?2:1;level<=(root.equals("ordinal_crystal")?4:3);level++){
            String name=root+(level==1?"":"_lv"+level);int ticks=100<<(root.equals("ordinal_crystal")?level-2:level-1);
            var b=block(name);var state=b.defaultBlockState();if(state.hasProperty(PortableRelicBlock.PLAYER_PLACED))state=state.setValue(PortableRelicBlock.PLAYER_PLACED,true);
            check(b!=Blocks.AIR&&CoreMiningRules.ticks(name)==ticks,"fixed displayed-grade mapping "+name);
            check(!state.requiresCorrectToolForDrops(),"any-tool core permission "+name);
            for(int environment=0;environment<4;environment++){
                p.removeAllEffects();p.setOnGround(environment!=2);
                p.setItemSlot(EquipmentSlot.OFFHAND,environment==0?ItemStack.EMPTY:book);
                worldWater(p,feet,environment==3);
                if(environment==1)p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,5));
                if(environment==2)p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,200,3));
                for(var tool:tools){
                    p.setItemSlot(EquipmentSlot.MAINHAND,tool);update(p);
                    float progress=state.getDestroyProgress(p,p.level(),sample);
                    near(progress,CoreMiningRules.progress(name),"tool/effect independent native progress "+name+" env"+environment);
                    check(clientTicks(progress)==ticks,"client exact break tick count "+name);
                    check(progress*(ticks-1)<1&&progress*ticks>=1,"server exact break tick count "+name);
                    var drops=Block.getDrops(state,p.level(),sample,null,p,tool);
                    check(drops.size()==1&&drops.getFirst().is(b.asItem())&&drops.getFirst().getCount()==1,"intact any-tool core drop "+name);
                }
            }
            cores++;
        }
        check(cores==27,"all27 graded cores covered");worldWater(p,feet,false);p.removeAllEffects();p.setOnGround(true);
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);update(p);
    }
    private static void worldWater(ServerPlayer p,BlockPos feet,boolean wet)throws Exception{
        p.level().setBlock(feet,wet?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        p.level().setBlock(feet.above(),wet?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);fluid(p);
        check(p.isEyeInFluid(FluidTags.WATER)==wet,"actual submerged condition");
    }
    private static void ordinaryCrystal(ServerPlayer p,BlockPos sample)throws Exception{
        var state=GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState();near(state.getDestroySpeed(p.level(),sample),1.5,"ordinary crystal matches stone hardness");
        check(CoreMiningRules.ticks("ordinal_crystal")==0,"ungraded crystal exempt from fixed time");
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);update(p);float hand=state.getDestroyProgress(p,p.level(),sample);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));update(p);float pick=state.getDestroyProgress(p,p.level(),sample);
        near(pick/hand,6,"ordinary crystal retains native pick acceleration");
        for(var tool:List.of(ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.IRON_PICKAXE))){
            var drops=Block.getDrops(state,p.level(),sample,null,p,tool);check(drops.size()==1&&drops.getFirst().is(state.getBlock().asItem()),"ordinary crystal intact drop");
        }
    }
}
