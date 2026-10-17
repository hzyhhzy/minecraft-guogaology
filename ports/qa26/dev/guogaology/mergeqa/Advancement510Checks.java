package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.advancement.GuideProgress;
import dev.guogaology.advancement.GuideLayout;
import dev.guogaology.ambience.*;
import dev.guogaology.block.AnchoredBlock;
import dev.guogaology.block.OrdinalAnchoring;
import dev.guogaology.block.PortableRelicBlock;
import dev.guogaology.mining.*;
import dev.guogaology.survival.*;
import dev.guogaology.world.ProceduralTerrain;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import java.util.*;

/** Native registered criteria, generated sites, menu ownership and real table interaction. */
public final class Advancement510Checks {
    private static final String[] THEMES={"matrix","power","hydra","absence","weaver","astra","frontier","guogao"};
    private static final String[] CORES={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart"};
    private static final String[] BIOME_ICONS={"y_sequence_stone","power_bricks","epsilon_bloom","lho_guide_emblem","iblp_node","nuke_chair","turing_tape","guogao_lantern"};
    private static final List<EquipmentSlot> PLAYER_SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static AdvancementHolder holder(ServerPlayer p,String id){var a=p.level().getServer().getAdvancements().get(GuogaologyMod.id(id.equals("arrival")?id:"guide/"+id));check(a!=null,"loaded "+id);return a;}
    private static boolean done(ServerPlayer p,String id){return p.getAdvancements().getOrStartProgress(holder(p,id)).isDone();}
    private static void complete(ServerPlayer p,String id){check(done(p,id),"complete "+id);}
    private static void reset(ServerPlayer p,String id){var a=holder(p,id);for(String key:a.value().criteria().keySet())p.getAdvancements().revoke(a,key);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"guogaology:"+id)));}
    private static void inventory(ServerPlayer p,String... names){p.getInventory().clearContent();for(int i=0;i<names.length;i++)p.getInventory().setItem(i,item(names[i]));for(String name:names)CriteriaTriggers.INVENTORY_CHANGED.trigger(p,p.getInventory(),item(name));}
    private static void move(ServerPlayer p,ServerLevel world,BlockPos pos){p.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(pos),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));}
    public static void run(ServerPlayer p)throws Exception{
        checks=0;var server=p.level().getServer();var original=p.level();var start=p.blockPosition();p.setGameMode(GameType.CREATIVE);
        check(server.getAdvancements().getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("guogaology")&&a.value().display().isPresent()).count()==76,"all 76 guide definitions decoded");
        for(var advancement:server.getAdvancements().getAllAdvancements())if(advancement.id().getNamespace().equals("guogaology")&&advancement.value().display().isPresent()){
            var expected=GuideLayout.position(advancement.id().toString());var actual=advancement.value().display().orElseThrow();
            check(expected!=null&&Math.abs(expected.x()-actual.getX())<.001&&Math.abs(expected.y()-actual.getY())<.001,"native layout uses stable guide positions "+advancement.id());
        }
        float previous=-1;for(int index=0;index<THEMES.length;index++){String theme=THEMES[index];
            String id=theme.equals("guogao")?"enter_underworld":"biome_"+theme;var display=holder(p,id).value().display().orElseThrow();
            check(display.getY()>previous,"fixed biome sequence "+theme);previous=display.getY();
            var representative=item(BIOME_ICONS[index]);check(!representative.isEmpty()&&display.getIcon().is(representative.getItem()),"existing scenery item for biome "+theme);
            check(!CoreGrades.ROOTS.contains(BIOME_ICONS[index]),"biome icon is not a core "+theme);
        }
        check(holder(p,"compute_setup").value().parent().orElseThrow().equals(GuogaologyMod.id("guide/material_astra")),"computing is under Astra materials");
        for(String retired:List.of("raw_material","first_core","core2","core3","landmark_first","portal_outer","portal_inner","portal_underworld"))check(server.getAdvancements().get(GuogaologyMod.id("guide/"+retired))==null,"retired duplicate "+retired);
        inventory(p,"omega_material");complete(p,"first_ore");check(!done(p,"first_omega"),"omega is distinct from capital Omega");
        inventory(p,"true_omega_ore");complete(p,"first_omega");
        check(holder(p,"enter_inner").value().parent().orElseThrow().equals(GuogaologyMod.id("guide/first_omega")),"Omega is the guide prerequisite, not a travel lock");
        for(String[] goal:new String[][]{{"omega_pickaxe","first_tool"},{"omega_manuscript","first_book"},{"number_pickaxe","number_tools"},{"server_rack","compute_setup"},{"ordinal_shard","shards"},{"ordinal_crystal","crystal"},{"ordinal_crystal_lv2","ordinal1"},{"ordinal_crystal_lv3","ordinal2"},{"ordinal_crystal_lv4","ordinal3"},{"enhancement_table","station"},{"enhancement_table_2","higher_station"},{"enhancement_table_3","ultimate_station"},{"guogao_lamp_shaper","lamp_shaper"}}){inventory(p,goal[0]);complete(p,goal[1]);}
        inventory(p,"omega_helmet","epsilon_chestplate","gamma_leggings");check(!done(p,"full_set"),"three parts are not a set");inventory(p,"omega_helmet","epsilon_chestplate","gamma_leggings","true_omega_boots");complete(p,"full_set");
        inventory(p,"return_token");check(!done(p,"return_ready"),"return token alone insufficient");inventory(p,"return_token","inner_return_frame");complete(p,"return_ready");
        var book=item("omega_manuscript");GuideProgress.edited(p,book);check(!done(p,"socketed"),"blank editor insufficient");GearData.setCores(book,List.of(item("sequence_core")));GuideProgress.edited(p,book);complete(p,"socketed");
        String[][] materials={{"matrix_sheet"},{"arrow_knot","tree_node_green"},{"hydra_sprig"},{"ordinal_crystal","fos_trace"},{"laver_gel"},{"cloud_pearl","compute_chip"},{"turing_endcap"},{"dread_resin"}};
        for(int i=0;i<THEMES.length;i++){
            String id="material_"+THEMES[i];reset(p,id);inventory(p,"minecraft:dirt");check(!done(p,id),"unrelated material does not count "+id);
            if(materials[i].length>1){inventory(p,materials[i][0]);check(!done(p,id),"all material groups required "+id);}
            inventory(p,materials[i]);complete(p,id);
            for(int grade=1;grade<=3;grade++){inventory(p,CORES[i]+(grade==1?"":"_lv"+grade));complete(p,"core_"+THEMES[i]+"_"+grade);}
            var logo=GuideProgress.LANDMARK_ICONS.get(THEMES[i]);check(logo!=null&&holder(p,"landmark_"+THEMES[i]).value().display().orElseThrow().getIcon().is(logo),"dedicated landmark icon "+THEMES[i]);
            for(String tab:CreativeCatalog.TABS)check(CreativeCatalog.entries(tab).stream().noneMatch(s->s.is(logo)),"landmark icons hidden from creative "+tab);
        }
        for(String lho:List.of("fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z")){reset(p,"material_absence");inventory(p,lho);check(!done(p,"material_absence"),"LHO material also needs stabilizer");inventory(p,"ordinal_crystal",lho);complete(p,"material_absence");}
        reset(p,"eat_guogao");CriteriaTriggers.CONSUME_ITEM.trigger(p,new ItemStack(Items.APPLE));check(!done(p,"eat_guogao"),"apple is not Guogao");CriteriaTriggers.CONSUME_ITEM.trigger(p,item("guogao_slice"));complete(p,"eat_guogao");
        table(p);
        mastery(p);
        // Same biome IDs in Overworld must not grant Inner exploration.
        var inner=server.getLevel(GuogaologyMod.DIMENSION);var under=server.getLevel(GuogaologyMod.GUOGAO);var probe=new BlockPos(8,112,8);
        original.getChunk(probe);for(var theme:SurvivalTheme.values())if(!theme.underworld()){
            reset(p,"biome_"+theme.id);fillBiome(original,probe,theme.biome);p.snapTo(8.5,112,8.5,0,0);GuideProgress.checkPlayer(p);check(!done(p,"biome_"+theme.id),"matching biome name in Overworld is not Inner "+theme.id);
        }
        inner.getChunk(probe);move(p,inner,probe);
        for(var theme:SurvivalTheme.values())if(!theme.underworld()){
            fillBiome(inner,probe,theme.biome);GuideProgress.checkPlayer(p);complete(p,"biome_"+theme.id);
        }
        geometry();
        lhoDisplays(p);
        reset(p,"landmarks_all");int n=0;
        for(String id:THEMES){
            var theme=Arrays.stream(SurvivalTheme.values()).filter(t->t.id.equals(id)).findFirst().orElseThrow();var world=theme.underworld()?under:inner;
            long seed=ProceduralTerrain.seed(world.getChunkSource().randomState());var site=SurvivalStructures.locate(seed,BlockPos.ZERO,theme);check(site!=null,"real native generation candidate "+id);
            // Only one real entrance chunk is needed, not the entire megastructure.
            var entry=site.entrance();world.getChunk(entry);check(SurvivalStructures.sitesForChunk(seed,theme.underworld(),new ChunkPos(entry.getX()>>4,entry.getZ()>>4)).contains(site),"current-chunk candidate matches generation "+id);
            check(!world.getBlockState(entry.below()).isAir(),"generated entrance has a real supporting block "+id);
            reset(p,"landmark_"+id);move(p,world,entry);check(GuideProgress.inside(site,p.blockPosition()),"real entrance in authored visit space "+id);
            GuideProgress.checkPlayer(p);complete(p,"landmark_"+id);
            if(++n<8)check(!done(p,"landmarks_all"),"all-eight not complete early");
            System.out.println("ADVANCEMENT510_VISIT "+id+" "+entry+"; no chest opened");
        }
        complete(p,"landmarks_all");move(p,original,start);
        Advancement057Checks.portals(p);
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.OUTER,Level.OVERWORLD);complete(p,"return_home");CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.GUOGAO,GuogaologyMod.DIMENSION);complete(p,"return_up");
        for(var a:server.getAdvancements().getAllAdvancements())if(a.id().getNamespace().equals("guogaology")&&a.value().display().isPresent())check(p.getAdvancements().getOrStartProgress(a).isDone(),"route actually exercised "+a.id());
        System.out.println("ADVANCEMENT510_SERVER_OK checks="+checks+"; eight biome/material/three-core/real visits, native food/table, full38 sockets and deferred edits");
    }
    private static void fillBiome(ServerLevel world,BlockPos pos,String biome){
        var source=world.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(world);
        // BiomeManager uses jittered neighboring quart cells: fill the neighborhood, not one quart.
        world.getServer().getCommands().performPrefixedCommand(source,"fillbiome "+(pos.getX()-8)+" "+(pos.getY()-8)+" "+(pos.getZ()-8)+" "+(pos.getX()+7)+" "+(pos.getY()+7)+" "+(pos.getZ()+7)+" guogaology:"+biome);
        check(world.getBiome(pos).is(ResourceKey.create(Registries.BIOME,GuogaologyMod.id(biome))),"native fillbiome fixture "+biome);
    }
    private static void geometry(){
        for(var theme:SurvivalTheme.values())for(int rotation=0;rotation<4;rotation++){
            var site=new SurvivalStructures.Site(1200,20,-600,theme,rotation);var plan=site.layout();check(GuideProgress.inside(site,site.entrance()),"rotated entrance "+theme+" "+rotation);
            if(theme==SurvivalTheme.ABSENCE){
                check(SurvivalStructures.stateFor(site,0,0,0,SanctuaryLayout.LIGHT).isAir(),"LHO lamps removed");
                check(SurvivalStructures.stateFor(site,0,0,0,SanctuaryLayout.CRYSTAL).isAir(),"LHO former crystal light placeholders removed");
                for(var q:plan.railingDisplays()){
                    byte code=plan.at(q.pos().x(),q.pos().y(),q.pos().z());
                    var state=SurvivalStructures.stateFor(site,q.pos().x(),q.pos().y(),q.pos().z(),code);
                    if(state.getBlock() instanceof dev.guogaology.block.AnchoredBlock)check(!dev.guogaology.block.AnchoredBlock.stable(state),"natural railing sample is unstable");
                }
                for(byte code:new byte[]{SanctuaryLayout.PHANTOM_FFFZ,SanctuaryLayout.PHANTOM_FOS,SanctuaryLayout.PHANTOM_EMPTY,SanctuaryLayout.PHANTOM_PSI,SanctuaryLayout.PHANTOM_Z}){
                    var state=SurvivalStructures.stateFor(site,0,0,0,code);check(!dev.guogaology.block.AnchoredBlock.stable(state),"natural interior sample is unstable "+code);
                }
            }else if(theme==SurvivalTheme.ASTRA)check(SurvivalStructures.stateFor(site,0,0,0,SanctuaryLayout.LIGHT).is(GuogaologyBlocks.ASTRA_LIGHT),"Astra lighting retained");
            check(!GuideProgress.inside(site,new BlockPos(site.x(),site.y()-1,site.z())),"below building excluded");check(!GuideProgress.inside(site,new BlockPos(site.x(),site.y()+plan.height+2,site.z())),"above building excluded");
            int padding=0,rooms=0;
            for(int y=2;y<plan.height;y+=7)for(int x=-plan.width/2;x<=plan.width/2;x+=7)for(int z=-plan.depth/2;z<=plan.depth/2;z+=7){
                if(plan.at(x,y,z)==SanctuaryLayout.KEEP&&plan.at(x,y+1,z)==SanctuaryLayout.KEEP&&plan.at(x,y-1,z)==SanctuaryLayout.KEEP){check(!GuideProgress.inside(site,site.position(new SanctuaryLayout.Point(x,y,z),0)),"natural bounding-box padding excluded "+theme);padding++;}
                else if(plan.at(x,y,z)==SanctuaryLayout.AIR){check(GuideProgress.inside(site,site.position(new SanctuaryLayout.Point(x,y,z),0)),"authored room included "+theme);rooms++;}
            }
            check(padding>0&&rooms>0,"both natural padding and authored space tested "+theme);
        }
    }
    private static void lhoDisplays(ServerPlayer p){
        var world=p.level();var center=new BlockPos(8,250,8);var anchor=center.offset(2,0,0);
        var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var site=new SurvivalStructures.Site(1200,20,-600,SurvivalTheme.ABSENCE,0);
        try{
            for(var q:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4))){saved.put(q.immutable(),world.getBlockState(q));world.setBlock(q,Blocks.AIR.defaultBlockState(),2);}
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            for(byte code:new byte[]{SanctuaryLayout.PHANTOM_FFFZ,SanctuaryLayout.PHANTOM_FOS,SanctuaryLayout.PHANTOM_EMPTY}){
                var state=SurvivalStructures.stateFor(site,0,0,0,code);var hit=new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false);
                world.setBlock(center,state,2);check(Block.getDrops(state,world,center,null,p,ItemStack.EMPTY).isEmpty(),"natural unanchored display cannot be harvested "+code);
                state.useWithoutItem(world,p,hit);check(world.getBlockState(center).isAir(),"natural display vanishes on use "+code);
                world.setBlock(center,state,2);world.setBlock(anchor,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);
                state.useWithoutItem(world,p,hit);check(world.getBlockState(center).is(state.getBlock())&&OrdinalAnchoring.protects(world,center),"live crystal protects natural display "+code);
                check(Block.getDrops(world.getBlockState(center),world,center,null,p,ItemStack.EMPTY).stream().anyMatch(s->s.is(state.getBlock().asItem())),"live crystal permits harvest "+code);
                world.setBlock(anchor,Blocks.AIR.defaultBlockState(),3);
                var placed=state.setValue(PortableRelicBlock.PLAYER_PLACED,true).setValue(AnchoredBlock.STABLE,true);world.setBlock(center,placed,2);
                placed.useWithoutItem(world,p,hit);check(world.getBlockState(center).is(state.getBlock()),"player display remains safe without crystal "+code);
                world.setBlock(center,Blocks.AIR.defaultBlockState(),2);
            }
            for(byte code:new byte[]{SanctuaryLayout.PHANTOM_PSI,SanctuaryLayout.PHANTOM_Z}){
                var state=SurvivalStructures.stateFor(site,0,0,0,code);var next=center.offset(1,0,0);
                world.setBlock(center,state,2);world.setBlock(next,state,2);state.getBlock().playerWillDestroy(world,center,state,p);
                check(world.getBlockState(center).isAir()&&world.getBlockState(next).isAir(),"natural building tree chain disappears "+code);
                world.setBlock(center,state,2);world.setBlock(next,state,2);world.setBlock(anchor,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);
                state.getBlock().playerWillDestroy(world,center,state,p);check(world.getBlockState(next).is(state.getBlock()),"anchor prevents building chain disappearance "+code);
                world.setBlock(anchor,Blocks.AIR.defaultBlockState(),3);world.setBlock(center,Blocks.AIR.defaultBlockState(),2);world.setBlock(next,Blocks.AIR.defaultBlockState(),2);
            }
        }finally{for(var e:saved.entrySet())world.setBlock(e.getKey(),e.getValue(),2);}
    }
    private static ItemStack maxed(String name,int kind){var gear=item(name);var cores=new ArrayList<ItemStack>();for(int n=0;n<GearData.capacity(gear);n++)cores.add(item(kind==6?"ordinal_crystal_lv4":"boundary_core_lv3"));GearData.setCores(gear,cores);return gear;}
    private static void mastery(ServerPlayer p){
        p.getInventory().clearContent();for(var slot:PLAYER_SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);reset(p,"omega_mastery");
        var parts=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);String[] names={"helmet","chestplate","leggings","boots"};
        for(int i=0;i<4;i++)p.setItemSlot(parts.get(i),maxed("true_omega_"+names[i],i+2));
        var book=maxed("true_omega_manuscript",6);p.setItemSlot(EquipmentSlot.MAINHAND,book);check(GuideProgress.omegaMastery(p),"full main-hand manuscript valid");
        p.setItemSlot(EquipmentSlot.OFFHAND,item("omega_manuscript"));check(!GuideProgress.omegaMastery(p),"offhand lower book overrides maxed main book");p.setItemSlot(EquipmentSlot.OFFHAND,book);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        check(GuideProgress.omegaMastery(p),"38 compatible max-grade cores valid");
        var bad=book.copy();var partial=new ArrayList<>(GearData.coreSlots(bad));partial.set(0,item("minecraft:totem_of_undying"));GearData.setCores(bad,partial);check(!GuideProgress.fullyMaxed(bad,6),"totem is not a maximum-grade core");
        partial.set(0,item("astra_critical_core_lv3"));GearData.setCores(bad,partial);check(!GuideProgress.fullyMaxed(bad,6),"incompatible core not counted");
        partial.set(0,item("ordinal_crystal_lv3"));GearData.setCores(bad,partial);check(!GuideProgress.fullyMaxed(bad,6),"ordinal raw stage3 is only displayed grade2");
        partial.set(0,ItemStack.EMPTY);GearData.setCores(bad,partial);check(!GuideProgress.fullyMaxed(bad,6),"empty socket not counted");
        var enchanted=item("true_omega_helmet");enchanted.enchant(p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),1);GearData.setCores(enchanted,GearData.coreSlots(p.getItemBySlot(EquipmentSlot.HEAD)));check(enchanted.isEnchanted()&&!GuideProgress.fullyMaxed(enchanted,2),"malformed native-enchantment/socket mixture cannot qualify");
        var helmet=p.getItemBySlot(EquipmentSlot.HEAD);p.setItemSlot(EquipmentSlot.HEAD,maxed("gamma_helmet",2));check(!GuideProgress.omegaMastery(p),"lower material helmet not counted");p.setItemSlot(EquipmentSlot.HEAD,helmet);
        var draft=item("true_omega_manuscript");p.setItemSlot(EquipmentSlot.OFFHAND,draft);var menu=new ManuscriptMenu(78,p.getInventory(),p.level(),40,draft);p.containerMenu=menu;
        var insert=item("ordinal_crystal_lv4");insert.setCount(6);p.getInventory().setItem(9,insert);menu.clicked(11,0,ContainerInput.QUICK_MOVE,p);
        check(GearData.cores(draft).size()==6,"actual menu ownership stores all six draft cores");check(!GuideProgress.omegaMastery(p),"uncommitted draft not eligible");GuideProgress.checkPlayer(p);check(!done(p,"omega_mastery"),"tick does not award draft");p.closeContainer();GuideProgress.checkPlayer(p);complete(p,"omega_mastery");
        p.getInventory().clearContent();for(var slot:PLAYER_SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);
    }
    private static void table(ServerPlayer p){
        var world=p.level();var base=new BlockPos(8,250,8);for(int x=-3;x<=5;x++)for(int z=-3;z<=5;z++)world.setBlock(base.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setShiftKeyDown(false);reset(p,"play_laver");
        var hit=new BlockHitResult(Vec3.atCenterOf(base),Direction.UP,base,false);world.setBlock(base,GuogaologyBlocks.IBLP_NODE.defaultBlockState(),2);
        UseBlockCallback.EVENT.invoker().interact(p,world,InteractionHand.MAIN_HAND,hit);check(!done(p,"play_laver"),"unreadable isolated cell gives no progress");
        world.setBlock(base,GuogaologyBlocks.LAVER_CORE.defaultBlockState(),2);for(int row=1;row<=3;row++)for(int col=0;col<=row;col++)world.setBlock(base.offset(col,0,row),(col==0?GuogaologyBlocks.IBLP_MARKED:GuogaologyBlocks.IBLP_NODE).defaultBlockState(),2);
        check(LiveLaverTable.read(world,base)!=null,"actual triangular table readable");
        p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.APPLE));UseBlockCallback.EVENT.invoker().interact(p,world,InteractionHand.MAIN_HAND,hit);check(!done(p,"play_laver"),"occupied hand gives no performance");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        UseBlockCallback.EVENT.invoker().interact(p,world,InteractionHand.MAIN_HAND,hit);complete(p,"play_laver");check(LaverMusic.activeCount(world)>0,"real performance started");
    }
}
