package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.StatefulDecorBlock;
import dev.guogaology.mining.GearData;
import dev.guogaology.outer.entity.FruitCakeSlimeEntity;
import dev.guogaology.outer.entity.FruitSlimeEntity;
import dev.guogaology.outer.registry.ModEntities;
import dev.guogaology.world.WorldNoise;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Current production recipes and native loot; no real mob kill or terrain mutation. */
public final class MaterialBalance065Checks {
    private static int checks,crafts,chestFills,mobRolls;
    private record Spec(String theme,String core,String decoration,String[] ring) {}
    private record Group(String token,int minimum,int maximum,Set<String> alternatives) {}
    private static Spec spec(String theme,String core,String decoration,String... ring){return new Spec(theme,core,decoration,ring);}
    private static final List<Spec> SPECS=List.of(
        spec("matrix","sequence_core","matrix_archive_ceramic","ordinal_bricks","matrix_sheet","ordinal_bricks","ridge_lamina","ridge_lamina","ordinal_bricks","matrix_sheet","ordinal_bricks"),
        spec("power","power_tower_core","recursive_bronze","power_bricks","ordinal_bricks","power_bricks","ordinal_bricks","ordinal_bricks","power_bricks","ordinal_bricks","power_bricks"),
        spec("hydra","hydra_bud","hydra_jade","#ordinal_branch_stones","hydra_sprig","#ordinal_branch_stones","#ordinal_flowers","#ordinal_flowers","#ordinal_branch_stones","hydra_sprig","#ordinal_branch_stones"),
        spec("absence","lho_trace","lho_letter_l","absence_glass","stellar_stone","absence_glass","fffz_trace","fos_trace","absence_glass","stellar_stone","absence_glass"),
        spec("weaver","laver_core","resonant_silk","laver_ribbon","iblp_marked","laver_ribbon","iblp_marked","iblp_marked","laver_ribbon","iblp_marked","laver_ribbon"),
        spec("astra","astra_critical_core","astra_marble","compute_chip","cloud_pearl","compute_chip","nuke_mushroom","nuke_mushroom","compute_chip","nuke_mushroom","compute_chip"),
        spec("frontier","boundary_core","axiom_porcelain","turing_tape","turing_endcap","turing_tape","set_membrane","set_membrane","turing_tape","turing_endcap","turing_tape"),
        spec("guogao","guogao_heart","guogao_heart_resin","fruit_cake_gel","guogao_drop","fruit_cake_gel","#fruit_guogao","#fruit_guogao","fruit_cake_gel","#fruit_guogao","fruit_cake_gel"));
    private static final Map<String,List<String>> TAGS=Map.of(
        "#ordinal_branch_stones",List.of("epsilon_symbol","omega_symbol","great_omega_symbol","zeta_symbol","psi_symbol","phi_symbol"),
        "#ordinal_flowers",List.of("epsilon_bloom","omega_bloom","great_omega_bloom","zeta_vine","psi_fern","phi_bloom"),
        "#fruit_guogao",List.of("amber_guogao","berry_guogao","lime_guogao","azure_guogao","plain_amber_guogao","plain_berry_guogao","plain_lime_guogao","plain_azure_guogao"));
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("MaterialBalance065: "+why);}
    private static Identifier id(String key){return Identifier.parse(key.contains(":")?key:"guogaology:"+key);}
    private static ItemStack item(String key){return new ItemStack(BuiltInRegistries.ITEM.getValue(id(key)));}
    private static String first(String token){return token.startsWith("#")?TAGS.get(token).getFirst():token;}
    private static String[] centered(String center,String... ring){
        if(ring.length!=8)throw new IllegalArgumentException("Eight ring cells required");
        var result=new String[9];System.arraycopy(ring,0,result,0,4);result[4]=center;System.arraycopy(ring,4,result,5,4);return result;
    }
    private static List<ItemStack> items(String... grid){return Arrays.stream(grid).map(MaterialBalance065Checks::first).map(MaterialBalance065Checks::item).toList();}
    private static ItemStack craft(ServerLevel world,List<ItemStack> cells,int width,int height){
        var input=CraftingInput.of(width,height,cells);var recipe=world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,world);
        return recipe.isEmpty()?ItemStack.EMPTY:recipe.orElseThrow().value().assemble(input);
    }
    private static ItemStack craft(ServerLevel world,String... grid){return craft(world,items(grid),3,3);}
    private static void makes(ServerLevel world,String result,int count,String... grid){
        var made=craft(world,grid);crafts++;check(made.is(item(result).getItem())&&made.getCount()==count,"native craft "+result+" got "+made);
    }
    public static void run(ServerPlayer player){
        checks=crafts=chestFills=mobRolls=0;var main=player.getMainHandItem();var off=player.getOffhandItem();
        try{
            player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            recipes(player.level());chests(player);entityLoot(player);
            System.out.println("MATERIAL_BALANCE_065_OK checks="+checks+" crafts="+crafts+" nativeChestFills="+chestFills+" nativeMobLootRolls="+mobRolls+" current8Lv1/powerLv2/otherUpgrades/tagAlternatives/noEmojiFlower/meanIngredientCosts/mean20Decoration/gelLooting0+3+6");
        }finally{player.setItemSlot(EquipmentSlot.MAINHAND,main);player.setItemSlot(EquipmentSlot.OFFHAND,off);}
    }
    private static void recipes(ServerLevel world){
        for(var spec:SPECS){
            var grid=centered("ordinal_crystal",spec.ring());makes(world,spec.core(),1,grid);
            var bad=grid.clone();String corner=bad[0];bad[0]=bad[4];bad[4]=corner;check(craft(world,bad).isEmpty(),"Lv1 base crystal stays central "+spec.core());
            String[] second;
            if(spec.theme().equals("power"))second=centered("epsilon_block","power_tower_core","scg_node","power_tower_core","tree_node_red","tree_node_green","power_tower_core","tree_node_blue","power_tower_core");
            else {var ring=new String[8];Arrays.fill(ring,spec.core());second=centered("epsilon_block",ring);}
            makes(world,spec.core()+"_lv2",1,second);second[4]="ordinal_crystal_lv2";check(craft(world,second).isEmpty(),"Lv2 rejects old universal center "+spec.core());
            var ring=new String[8];Arrays.fill(ring,spec.core()+"_lv2");var third=centered("gamma_block",ring);makes(world,spec.core()+"_lv3",1,third);
            third[4]="epsilon_block";check(craft(world,third).isEmpty(),"Lv3 needs Gamma "+spec.core());
        }
        var oldPower=new String[8];Arrays.fill(oldPower,"power_tower_core");check(craft(world,centered("epsilon_block",oldPower)).isEmpty(),"PowerLv2 no longer accepts eight identicalLv1");
        var hydra=SPECS.get(2).ring();int[] branches={0,2,5,7};
        for(int value=0;value<1296;value++){
            var ring=hydra.clone();int v=value;for(int slot:branches){ring[slot]=TAGS.get("#ordinal_branch_stones").get(v%6);v/=6;}
            makes(world,"hydra_bud",1,centered("ordinal_crystal",ring));
        }
        for(var left:TAGS.get("#ordinal_flowers"))for(var right:TAGS.get("#ordinal_flowers")){
            var ring=hydra.clone();ring[3]=left;ring[4]=right;makes(world,"hydra_bud",1,centered("ordinal_crystal",ring));
        }
        for(int slot:new int[]{3,4}){
            var bad=hydra.clone();bad[slot]="emoji_flower";check(craft(world,centered("ordinal_crystal",bad)).isEmpty(),"Guogao flower excluded from each ordinal-flower slot "+slot);
        }
        for(int slot:branches){var bad=hydra.clone();bad[slot]="epsilon_bloom";check(craft(world,centered("ordinal_crystal",bad)).isEmpty(),"Flower cannot replace Branch Stone "+slot);}
        var absence=SPECS.get(3).ring();
        for(var invalid:List.of("fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z"))for(int slot:new int[]{3,4}){
            var bad=absence.clone();if(invalid.equals("fffz_trace")&&slot==3||invalid.equals("fos_trace")&&slot==4)continue;
            bad[slot]=invalid;check(craft(world,centered("ordinal_crystal",bad)).isEmpty(),"LHO needs one of each exact fragment "+invalid+slot);
        }
        var guogao=SPECS.get(7).ring();
        for(var a:TAGS.get("#fruit_guogao"))for(var b:TAGS.get("#fruit_guogao"))for(var c:TAGS.get("#fruit_guogao")){
            var ring=guogao.clone();ring[3]=a;ring[4]=b;ring[6]=c;makes(world,"guogao_heart",1,centered("ordinal_crystal",ring));
        }
        for(var spec:List.of(SPECS.get(0),SPECS.get(1)))for(int digit=0;digit<16;digit++){
            var grid=new ArrayList<>(items(centered("ordinal_crystal",spec.ring())));
            for(int i=0;i<9;i++)if(grid.get(i).is(GuogaologyBlocks.ORDINAL_BRICKS.asItem()))grid.set(i,StatefulDecorBlock.copyAppearance(item("ordinal_bricks"),GuogaologyBlocks.ordinalBrick((digit+i)%16)));
            var result=craft(world,grid,3,3);crafts++;check(result.is(item(spec.core()).getItem()),"mixed numeral components accepted "+spec.core()+digit);
        }
        var stones=Collections.nCopies(9,item("stellar_stone"));var packed=craft(world,stones,3,3);check(packed.is(item("stellar_stone_block").getItem())&&packed.getCount()==1,"nine Stellar Stones pack into block");
        var unpacked=craft(world,List.of(item("stellar_stone_block")),1,1);check(unpacked.is(item("stellar_stone").getItem())&&unpacked.getCount()==9,"old stellar unpack recipe retained");
    }
    private static List<Group> expected(Spec spec,boolean main){
        var counts=new LinkedHashMap<String,Integer>();counts.put("ordinal_crystal",1);for(var token:spec.ring())counts.merge(token,1,Integer::sum);
        var groups=new ArrayList<Group>();groups.add(new Group(spec.core()+(main?"_lv2":""),1,1,Set.of(spec.core()+(main?"_lv2":""))));
        for(var e:counts.entrySet()){
            int c=e.getValue(),lo=main?2*c:Math.max(1,c-1),hi=main?4*c:c==1?1:c+1;
            groups.add(new Group(e.getKey(),lo,hi,e.getKey().startsWith("#")?Set.copyOf(TAGS.get(e.getKey())):Set.of(e.getKey())));
        }
        if(main)groups.add(new Group("true_omega_material",2,4,Set.of("true_omega_material")));
        groups.add(new Group(spec.decoration(),16,24,Set.of(spec.decoration())));return groups;
    }
    private static void chests(ServerPlayer p){
        var world=p.level();var params=new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,p.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);
        int tableIndex=0;
        for(var spec:SPECS)for(boolean main:new boolean[]{true,false}){
            String key=spec.theme()+(main?"_sanctum":"_ruin");var table=p.level().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,id("chests/"+key)));
            var groups=expected(spec,main);var allowed=new HashMap<String,Group>();for(var group:groups)for(var member:group.alternatives())check(allowed.put(member,group)==null,"no ambiguous loot groups "+key+member);
            var sums=new HashMap<String,Long>();var minima=new HashMap<String,Integer>();var maxima=new HashMap<String,Integer>();var seen=new HashMap<String,Set<String>>();
            for(int sample=0;sample<512;sample++){
                long seed=WorldNoise.mix(650000L+tableIndex*10000L+sample);
                var chest=new SimpleContainer(27);table.fill(chest,params,seed);chestFills++;
                var totals=new HashMap<String,Integer>();var selected=new HashMap<String,Set<String>>();
                for(int slot=0;slot<27;slot++){
                    var stack=chest.getItem(slot);if(stack.isEmpty())continue;var identifier=BuiltInRegistries.ITEM.getKey(stack.getItem());
                    check(identifier.getNamespace().equals("guogaology")&&allowed.containsKey(identifier.getPath()),"only expected registered chest items "+key+identifier);
                    check(stack.getCount()<=stack.getMaxStackSize(),"real chest stack capacity "+key+identifier);
                    var group=allowed.get(identifier.getPath());totals.merge(group.token(),stack.getCount(),Integer::sum);selected.computeIfAbsent(group.token(),v->new HashSet<>()).add(identifier.getPath());
                }
                check(totals.size()==groups.size(),"all guaranteed groups survive real27slot fill "+key+totals);
                for(var group:groups){
                    int n=totals.getOrDefault(group.token(),0);check(n>=group.minimum()&&n<=group.maximum(),"native count bounds "+key+group.token()+"="+n);
                    check(selected.getOrDefault(group.token(),Set.of()).size()==1,"one member per alternative group "+key+group.token());
                    sums.merge(group.token(),(long)n,Long::sum);minima.merge(group.token(),n,Math::min);maxima.merge(group.token(),n,Math::max);seen.computeIfAbsent(group.token(),v->new HashSet<>()).addAll(selected.get(group.token()));
                }
            }
            for(var group:groups){
                double mean=sums.get(group.token())/512.0,expected=(group.minimum()+group.maximum())/2.0,tolerance=Math.max(.20,(group.maximum()-group.minimum())*.08);
                String diagnostic=key+group.token()+" range="+minima.get(group.token())+".."+maxima.get(group.token())+" mean="+mean+" expectedRange="+group.minimum()+".."+group.maximum()+" expectedMean="+expected+" seedPolicy=mixed64";
                check(minima.get(group.token())==group.minimum()&&maxima.get(group.token())==group.maximum(),"native uniform reaches both endpoints "+diagnostic);
                check(Math.abs(mean-expected)<tolerance,"native quantity mean "+diagnostic);
                check(seen.get(group.token()).equals(group.alternatives()),"every legal tag variant appears "+key+group.token()+seen.get(group.token()));
            }
            System.out.println("MATERIAL065_CHEST table="+key+" fills=512 decorationMean="+(sums.get(spec.decoration())/512.0)+" groupMeans="+sums.entrySet().stream().map(e->e.getKey()+"="+(e.getValue()/512.0)).toList());tableIndex++;
        }
    }
    private static void entityLoot(ServerPlayer p){
        var world=p.level();var looting=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        LivingEntity[] victims={new FruitCakeSlimeEntity(ModEntities.FRUIT_CAKE_SLIME,world),new FruitSlimeEntity(ModEntities.FRUIT_SLIME,world)};
        for(int mode:new int[]{0,3,6}){
            var weapon=mode==6?item("true_omega_sword"):new ItemStack(Items.IRON_SWORD);
            if(mode==3)weapon.enchant(looting,3);if(mode==6)GearData.setCores(weapon,List.of(item("hydra_bud_lv3")));
            p.setItemSlot(EquipmentSlot.MAINHAND,weapon);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            check(EnchantmentHelper.getEnchantmentLevel(looting,p)==mode,"real effective Looting mode "+mode);
            var before=weapon.copy();
            for(int entity=0;entity<2;entity++){
                String key=entity==0?"fruit_cake_slime":"fruit_slime";int base=entity==0?2:1;
                var table=world.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,id("entities/"+key)));
                var params=new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,p.position()).withParameter(LootContextParams.THIS_ENTITY,victims[entity])
                    .withParameter(LootContextParams.DAMAGE_SOURCE,world.damageSources().playerAttack(p)).withParameter(LootContextParams.ATTACKING_ENTITY,p)
                    .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,p).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,p).create(LootContextParamSets.ENTITY);
                int min=Integer.MAX_VALUE,max=0,total=0;var histogram=new int[mode+1];
                for(int sample=0;sample<1024;sample++){
                    // Consecutive LegacyRandomSource seeds have correlated first
                    // floats. Spread seeds across64bits before testing a first-draw
                    // uniform Looting bonus, while keeping the run reproducible.
                    long seed=WorldNoise.mix(6650000L+mode*10000L+entity*2000L+sample);
                    var drops=table.getRandomItems(params,seed);mobRolls++;int count=0;
                    for(var drop:drops){check(drop.is(item("fruit_cake_gel").getItem()),"only gel from actual entity table "+key);count+=drop.getCount();}
                    check(count>=base&&count<=base+mode,"actual gel/Looting bounds "+key+mode+"="+count+" sample="+sample+" seed="+seed);min=Math.min(min,count);max=Math.max(max,count);total+=count;histogram[count-base]++;
                }
                double mean=total/1024.0;String diagnostic="entity="+key+" effectiveLooting="+mode+" samples=1024 range="+min+".."+max+" mean="+mean+" expectedRange="+base+".."+(base+mode)+" expectedMean="+(base+mode/2.0)+" histogram="+Arrays.toString(histogram)+" seedPolicy=mixed64";
                System.out.println("MATERIAL065_ENTITY "+diagnostic);
                check(min==base&&max==base+mode,"actual Looting endpoints "+diagnostic);
                check(Math.abs(mean-(base+mode/2.0))<.20,"normal vanilla Looting mean "+diagnostic);
            }
            check(ItemStack.matches(before,weapon),"loot evaluation preserves the actual held weapon and installed cores "+mode);
        }
    }
}
