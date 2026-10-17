package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.block.*;
import dev.guogaology.mining.EquipmentRules;
import dev.guogaology.world.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;

/** Actual recipe/loot/anchoring/feature registries and bounded fresh-world checks. */
public final class CoreResources055Checks {
    private static int checks;
    private static final String[] REGIONAL={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart"};
    private static final String[] BIOMES={"ordinal_crags","power_desert","epsilon_meadow","lho_absence","laver_tablelands","astra_awakening","limit_highlands","guogao_forest","misaligned_strata","silent_mire","descending_caverns"};
    private static final String[] BATCHES={"uniform","lho"};
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Core resources055: "+why);}
    private static Identifier id(String name){return Identifier.parse(name.contains(":")?name:"guogaology:"+name);}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(id(name));}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(id(name)));}
    private static void intact(List<ItemStack> drops,Block block,String label){check(drops.size()==1&&drops.getFirst().is(block.asItem())&&drops.getFirst().getCount()==1,label+": "+drops);}
    private static ItemStack craft(ServerLevel world,String... input){
        var grid=CraftingInput.of(input.length==1?1:input.length==4?2:3,input.length==1?1:input.length==4?2:input.length/3,Arrays.stream(input).map(CoreResources055Checks::item).toList());
        var found=world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,world);
        return found.isEmpty()?ItemStack.EMPTY:found.orElseThrow().value().assemble(grid);
    }
    private static void crafts(ServerLevel world,String output,int count,String... input){var value=craft(world,input);check(value.is(item(output).getItem())&&value.getCount()==count,"loaded native recipe "+output+" ×"+count+" got "+value);}
    private static String[] centered(String center,String... ring){
        if(ring.length!=8)throw new IllegalArgumentException("Eight surrounding ingredients required");
        var grid=new String[9];System.arraycopy(ring,0,grid,0,4);grid[4]=center;System.arraycopy(ring,4,grid,5,4);return grid;
    }
    public static void run(ServerPlayer player)throws Exception{
        checks=0;var main=player.getMainHandItem();var off=player.getOffhandItem();var mode=player.gameMode();
        try{
            player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);player.setGameMode(GameType.SURVIVAL);
            registrationsAndDrops(player);recipes(player.level());oreRegistries(player);lhaAnchors(player);graphNodes();wildScenery(player);freshOres(player);
            System.out.println("CORE_RESOURCES_055_CHECKS_OK checks="+checks+" registrations/anyTool+Silk+Fortune/8nativeRecipes+upgrades/11biomeIronOre/LHO5cube+overlap+vanishing/TREE+SCG/wildNoCores/freshOreCounts");
        }finally{player.setItemSlot(EquipmentSlot.MAINHAND,main);player.setItemSlot(EquipmentSlot.OFFHAND,off);player.setGameMode(mode);}
    }
    private static void registrationsAndDrops(ServerPlayer player){
        var world=player.level();var at=player.blockPosition();var lookup=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk=new ItemStack(Items.WOODEN_PICKAXE);silk.enchant(lookup.getOrThrow(Enchantments.SILK_TOUCH),1);
        var fortune=new ItemStack(Items.WOODEN_PICKAXE);fortune.enchant(lookup.getOrThrow(Enchantments.FORTUNE),3);
        var tools=List.of(ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.IRON_SWORD),new ItemStack(Items.SHEARS),silk,fortune);
        for(var material:CoreResources.MATERIALS){
            check(material!=Blocks.AIR&&GuogaologyBlocks.ALL.contains(material),"raw material is registered and catalogued "+material);
            check(!material.defaultBlockState().requiresCorrectToolForDrops(),"raw material has no native tool-drop gate");
            for(var tool:tools)intact(Block.getDrops(material.defaultBlockState(),world,at,null,player,tool),material,"raw any-tool drop "+material);
        }
        check(!GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState().requiresCorrectToolForDrops(),"ordinary crystal has no native tool-drop gate");
        for(var tool:tools)intact(Block.getDrops(GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),world,at,null,player,tool),GuogaologyBlocks.ORDINAL_CRYSTAL,"ordinary crystal returns whole with every tool");
        for(var ore:CoreResources.ORES){
            check(ore!=Blocks.AIR&&!ore.defaultBlockState().requiresCorrectToolForDrops(),"three ore skins registered without native tool-drop gate");int min=Integer.MAX_VALUE,max=0,fortuneTotal=0,plainTotal=0;
            for(int sample=0;sample<64;sample++){
                var plain=Block.getDrops(ore.defaultBlockState(),world,at,null,player,tools.get(sample%4));
                check(plain.size()==1&&plain.getFirst().is(item("ordinal_shard").getItem()),"ore drops shards with any tool");
                int n=plain.getFirst().getCount();check(n>=4&&n<=6,"ore base4..6");min=Math.min(min,n);max=Math.max(max,n);plainTotal+=n;
                var extra=Block.getDrops(ore.defaultBlockState(),world,at,null,player,fortune);check(extra.size()==1&&extra.getFirst().is(item("ordinal_shard").getItem())&&extra.getFirst().getCount()>=4&&extra.getFirst().getCount()<=24,"native Fortune bounds");fortuneTotal+=extra.getFirst().getCount();
            }
            check(min==4&&max==6&&fortuneTotal>plainTotal,"actual native Fortune increases ore yield and both base endpoints appear");
            intact(Block.getDrops(ore.defaultBlockState(),world,at,null,player,silk),ore,"native Silk returns each ore skin");
        }
        for(String root:REGIONAL)for(int grade=1;grade<=3;grade++){
            var value=block(root+(grade==1?"":"_lv"+grade));check(value!=Blocks.AIR,"regional grade registered");
            var state=value.defaultBlockState();if(state.hasProperty(PortableRelicBlock.PLAYER_PLACED))state=state.setValue(PortableRelicBlock.PLAYER_PLACED,true);
            for(var tool:tools)intact(Block.getDrops(state,world,at,null,player,tool),value,"placed core any-tool intact "+root+grade);
        }
        intact(Block.getDrops(GuogaologyBlocks.GIANT_LAVER.defaultBlockState(),world,at,null,player,new ItemStack(Items.SHEARS)),GuogaologyBlocks.GIANT_LAVER,"ordinary shears collect first Laver ingredient");
        for(var tree:GuogaologyBlocks.TREE_NODES)for(var tool:tools)intact(Block.getDrops(tree.defaultBlockState(),world,at,null,player,tool),tree,"TREE node is ordinary any-tool material");
    }
    public static void runRecipes(ServerLevel world){
        int before=checks;recipes(world);System.out.println("CORE_RESOURCES_RECIPES_OK checks="+(checks-before)+" regionalLv1/centralEpsilonGamma/alternatives/ordinalNinefold");
    }
    private static void recipes(ServerLevel world){
        String[][] inputs={
            {"matrix_sheet","ridge_lamina","projection_glass","y_leaves","ordinal_bricks","ordinal_bricks","ordinal_bricks","ordinal_bricks"},
            {"arrow_knot","arrow_knot","scg_node","scg_node","tree_node_red","tree_node_green","tree_node_blue","graham"},
            {"hydra_sprig","hydra_sprig","veblen_petal","veblen_petal","psi_symbol","great_omega_symbol","epsilon_bloom","omega_bloom"},
            {"lho_letter_l","lho_letter_h","lho_letter_o","fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z","stellar_stone"},
            {"laver_gel","laver_ribbon","iblp_node","iblp_node","iblp_marked","iblp_blank","gummy","tianyi_fiber"},
            {"cloud_pearl","compute_chip","compute_chip","compute_chip","compute_chip","compute_chip","nuke_mushroom","guogao_lantern"},
            {"set_membrane","set_membrane","turing_tape","turing_tape","proof_stone","turing_endcap","formula_stone","rayo_rose"},
            {"dread_resin","guogao_drop","dread_leaves","emoji_flower","loquat","star_gold","fruit_cake_gel","guogao_lantern"}};
        for(int i=0;i<REGIONAL.length;i++){
            var first=centered("ordinal_crystal",inputs[i]);crafts(world,REGIONAL[i],1,first);
            var misplaced=first.clone();var edge=misplaced[0];misplaced[0]=misplaced[4];misplaced[4]=edge;
            check(craft(world,misplaced).isEmpty(),"Lv1 crystal must occupy the center "+REGIONAL[i]);
            var second=new String[9];Arrays.fill(second,REGIONAL[i]);second[4]="epsilon_block";crafts(world,REGIONAL[i]+"_lv2",1,second);
            second[4]="ordinal_crystal_lv2";check(craft(world,second).isEmpty(),"old Ordinal center rejected for regional Lv2 "+REGIONAL[i]);
            Arrays.fill(second,REGIONAL[i]+"_lv2");second[4]="gamma_block";crafts(world,REGIONAL[i]+"_lv3",1,second);
            second[4]="epsilon_block";check(craft(world,second).isEmpty(),"regional Lv3 requires Gamma rather than Epsilon "+REGIONAL[i]);
        }
        var alternative=inputs[1].clone();alternative[7]="graham_flower";crafts(world,"power_tower_core",1,centered("ordinal_crystal",alternative));
        crafts(world,"ordinal_crystal",1,"ordinal_shard","ordinal_shard","ordinal_shard","ordinal_shard");crafts(world,"ordinal_shard",4,"ordinal_crystal");
        for(int stage=2;stage<=4;stage++){
            String lower=stage==2?"ordinal_crystal":"ordinal_crystal_lv"+(stage-1),higher="ordinal_crystal_lv"+stage;
            var inputs9=new String[9];Arrays.fill(inputs9,lower);crafts(world,higher,1,inputs9);crafts(world,lower,9,higher);
        }
    }
    private static void oreRegistries(ServerPlayer player){
        var access=player.level().registryAccess();var features=access.lookupOrThrow(Registries.CONFIGURED_FEATURE);var placed=access.lookupOrThrow(Registries.PLACED_FEATURE);
        for(String suffix:new String[]{""}){
            var feature=features.getValue(id("ore_ordinal_crystal"+suffix));check(feature!=null&&feature.feature()==Feature.ORE,"native OreFeature configured "+suffix);
            check(feature.config() instanceof OreConfiguration,"native OreConfiguration codec");var config=(OreConfiguration)feature.config();
            check(config.size==9&&config.discardChanceOnAirExposure==0&&config.targetStates.size()==3,"small uniform veins and visible cave-wall ore");
            var random=RandomSource.create(55);
            for(int digit=0;digit<10;digit++)check(config.targetStates.get(0).target.test(GuogaologyBlocks.NUMBER_STONES[digit].defaultBlockState(),random),"all numeric mother rocks are replaceable");
            check(config.targetStates.get(1).target.test(GuogaologyBlocks.ABSENCE_GLASS.defaultBlockState(),random),"transparent LHO mother rock is replaceable");
            check(config.targetStates.get(2).target.test(GuogaologyBlocks.ROOTBOUND_STONE.defaultBlockState(),random),"Underworld mother rock is replaceable");
            for(var target:config.targetStates)check(!target.target.test(Blocks.BEDROCK.defaultBlockState(),random)&&!target.target.test(Blocks.OAK_PLANKS.defaultBlockState(),random),"native rules exclude bedrock/building material");
        }
        for(String name:BIOMES){var biome=access.lookupOrThrow(Registries.BIOME).getValue(id(name));check(biome!=null,"biome present "+name);for(String batch:BATCHES){var feature=placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,id("ore_ordinal_crystal_"+batch)));check(biome.getGenerationSettings().features().get(6).contains(feature)==batch.equals(name.equals("lho_absence")?"lho":"uniform"),"one biome-specific ore feature step6 "+name+batch);}}
        var ores=TagKey.create(Registries.BLOCK,id("ordinal_crystal_ores"));for(var ore:CoreResources.ORES)check(ore.defaultBlockState().is(ores),"three ore skins share native ore tag");
    }
    private static void lhaAnchors(ServerPlayer player){
        var world=player.level();var center=new BlockPos(8,220,8);var saved=new LinkedHashMap<BlockPos,BlockState>();var playerPos=player.position();
        var types=List.of(GuogaologyBlocks.LHO_HYDRA_PSI,GuogaologyBlocks.LHO_HYDRA_Z,GuogaologyBlocks.FFFZ_TRACE,GuogaologyBlocks.FOS_TRACE);var ghosts=new ArrayList<BlockPos>();
        try{
            for(var p:BlockPos.betweenClosed(center.offset(-5,-4,-5),center.offset(11,4,5))){saved.put(p.immutable(),world.getBlockState(p));world.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            int i=0;for(int x:new int[]{-2,2})for(int y:new int[]{-2,2})for(int z:new int[]{-2,2}){var p=center.offset(x,y,z);ghosts.add(p);world.setBlock(p,types.get(i++%4).defaultBlockState(),2);}
            var far=center.offset(3,0,0);world.setBlock(far,GuogaologyBlocks.FOS_TRACE.defaultBlockState(),2);
            var persistent=center.offset(-2,0,0);world.setBlock(persistent,GuogaologyBlocks.LHO_HYDRA_PSI.defaultBlockState().setValue(PortableRelicBlock.PLAYER_PLACED,true).setValue(AnchoredBlock.STABLE,true),2);
            for(var p:ghosts)check(!OrdinalAnchoring.protects(world,p)&&!world.getBlockState(p).getValue(AnchoredBlock.ANCHORED),"unanchored default color");
            world.setBlock(center,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);
            for(var p:ghosts){var state=world.getBlockState(p);check(OrdinalAnchoring.protects(world,p)&&state.getValue(AnchoredBlock.ANCHORED),"live anchor reaches5cube corner and changes state");intact(Block.getDrops(state,world,p,null,player,ItemStack.EMPTY),state.getBlock(),"corner allows actual anchored harvest");}
            check(!OrdinalAnchoring.protects(world,far)&&!world.getBlockState(far).getValue(AnchoredBlock.ANCHORED)&&Block.getDrops(world.getBlockState(far),world,far,null,player,ItemStack.EMPTY).isEmpty(),"distance3 lies outside two rings");
            var second=center.offset(4,0,0);world.setBlock(second,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);world.setBlock(center,Blocks.AIR.defaultBlockState(),3);
            for(var p:ghosts)check(world.getBlockState(p).getValue(AnchoredBlock.ANCHORED)==(p.getX()>=center.getX()),"removing first anchor respects overlapping field");
            world.setBlock(second,Blocks.AIR.defaultBlockState(),3);
            for(var p:ghosts)check(!OrdinalAnchoring.protects(world,p)&&!world.getBlockState(p).getValue(AnchoredBlock.ANCHORED),"last anchor removal restores unstable state");
            player.setPos(center.getX()+.5,center.getY(),center.getZ()+.5);player.setItemSlot(EquipmentSlot.MAINHAND,item("ordinal_crystal"));
            world.setBlock(center,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);world.setBlock(center,Blocks.AIR.defaultBlockState(),3);
            for(var p:ghosts)check(OrdinalAnchoring.protects(world,p)&&!world.getBlockState(p).getValue(AnchoredBlock.ANCHORED),"held crystal protects after placed-anchor removal without persisting its color");
            player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            for(var p:ghosts)check(!OrdinalAnchoring.protects(world,p)&&!world.getBlockState(p).getValue(AnchoredBlock.ANCHORED),"putting held crystal away cannot leave false stable color");
            var edgeAnchor=center.offset(7,0,0);var edgeGhost=center.offset(9,0,0);world.setBlock(edgeGhost,GuogaologyBlocks.FOS_TRACE.defaultBlockState(),2);
            world.setBlock(edgeAnchor,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);
            OrdinalAnchoring.refreshChunkColors(world,ChunkPos.containing(edgeAnchor),false);
            check(!world.getBlockState(edgeGhost).getValue(AnchoredBlock.ANCHORED),"departing anchor chunk clears the two-block border in its loaded neighbor");
            OrdinalAnchoring.refreshChunkColors(world,ChunkPos.containing(edgeAnchor),true);
            check(world.getBlockState(edgeGhost).getValue(AnchoredBlock.ANCHORED),"reloaded anchor chunk recolors ghosts across its border");
            world.setBlock(edgeAnchor,Blocks.AIR.defaultBlockState(),3);world.setBlock(edgeGhost,world.getBlockState(edgeGhost).setValue(AnchoredBlock.ANCHORED,true),2);
            OrdinalAnchoring.refreshChunkColors(world,ChunkPos.containing(edgeGhost),true);
            check(!world.getBlockState(edgeGhost).getValue(AnchoredBlock.ANCHORED),"loaded ghost chunk repairs a persisted stale anchor color");
            world.setBlock(edgeGhost,Blocks.AIR.defaultBlockState(),2);
            var fixed=world.getBlockState(persistent);check(AnchoredBlock.protectedAt(fixed,world,persistent),"player placement remains permanently protected");
            fixed.getBlock().playerWillDestroy(world,persistent,fixed,player);check(world.getBlockState(persistent).is(fixed.getBlock()),"natural chain cannot remove player block");
            for(Block type:List.of(GuogaologyBlocks.FFFZ_TRACE,GuogaologyBlocks.FOS_TRACE)){
                world.setBlock(far,type.defaultBlockState(),2);world.getBlockState(far).useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(far),Direction.UP,far,false));check(world.getBlockState(far).isAir(),"unprotected fragment keeps original touch/use disappearance");
                world.setBlock(center,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);var near=center.offset(1,0,0);world.setBlock(near,type.defaultBlockState(),2);world.getBlockState(near).useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(near),Direction.UP,near,false));check(world.getBlockState(near).is(type),"protected fragment survives native use");world.setBlock(center,Blocks.AIR.defaultBlockState(),3);world.setBlock(near,Blocks.AIR.defaultBlockState(),2);
            }
            var a=center.offset(0,0,1);var b=center.offset(0,1,1);world.setBlock(a,GuogaologyBlocks.LHO_HYDRA_PSI.defaultBlockState(),2);world.setBlock(b,GuogaologyBlocks.LHO_HYDRA_Z.defaultBlockState(),2);
            var state=world.getBlockState(a);state.getBlock().playerWillDestroy(world,a,state,player);check(world.getBlockState(a).isAir()&&world.getBlockState(b).isAir(),"unanchored psi/Z chain still vanishes together");check(world.getBlockState(persistent).is(fixed.getBlock()),"player block remains after real chain disappearance");
        }finally{player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setPos(playerPos);for(var p:saved.keySet())world.setBlock(p,Blocks.AIR.defaultBlockState(),2);for(var entry:saved.entrySet())world.setBlock(entry.getKey(),entry.getValue(),2);}
    }
    private static void graphNodes(){
        int nodes=0;for(long salt=0;salt<24;salt++)for(boolean scg:new boolean[]{false,true}){
            int height=32;var geometry=new HashMap<BlockPos,Integer>();var brush=new VoxelBrush(-128,128,-62,317,-128,128,(x,y,z,m)->geometry.put(new BlockPos(x,y,z),m));
            CombinatorialForms.drawGraph(brush,0,80,0,height,salt,scg);var graph=CombinatorialForms.spacedTree(salt,scg,height);
            for(var n:graph.nodes()){var at=new BlockPos((int)Math.round(n.x()*height),(int)Math.round(80+n.y()*height),(int)Math.round(n.z()*height));check(geometry.get(at)==(scg?NaturalForms.SCG_NODE:CombinatorialForms.LABEL+n.label()),"every TREE/SCG center is dedicated raw material");nodes++;}
        }
        int fffz=0,fos=0;for(long salt=0;salt<4096;salt++){int value=NaturalForms.absenceFragmentMaterial(salt,7);check(value==NaturalForms.FFFZ||value==NaturalForms.FOS,"former empty-set selection never emits a core");if(value==NaturalForms.FFFZ)fffz++;else fos++;}
        check(Math.abs(fffz-fos)<256,"former empty-set third is shared approximately equally");System.out.println("CORE055_GRAPH_NODES nodes="+nodes+" fffZ="+fffz+" FOS="+fos);
    }
    private static void wildScenery(ServerPlayer player)throws ReflectiveOperationException{
        var inner=player.level().getServer().getLevel(GuogaologyMod.DIMENSION);long seed=ProceduralTerrain.seed(inner.getChunkSource().randomState());var sites=new HashMap<Integer,ChunkPos>();
        for(int ring=1;ring<18&&sites.size()<7;ring++)for(int k=0;k<ring*8;k++){double a=k*Math.PI*2/(ring*8);int x=(int)Math.round(Math.cos(a)*ring*512),z=(int)Math.round(Math.sin(a)*ring*512);int kind=BiomeRegions.kind(seed,x,z);sites.putIfAbsent(kind,new ChunkPos(x>>4,z>>4));}
        check(sites.size()==7,"bounded sampler locates all seven Inner biome themes");
        var forbidden=new HashSet<Block>();for(String root:EquipmentRules.CORES)for(int level=1;level<=(root.equals("ordinal_crystal")?4:3);level++)forbidden.add(block(root+(level==1?"":"_lv"+level)));
        var counts=new HashMap<Block,Integer>();long[] writes={0};
        SceneryBrush.Sink sink=(x,y,z,state)->{writes[0]++;check(!forbidden.contains(state.getBlock()),"wild painter emits no finished core/crystal: "+state);counts.merge(state.getBlock(),1,Integer::sum);};
        for(var entry:new TreeMap<>(sites).entrySet()){long before=writes[0];NaturalScenery.render(seed,false,entry.getValue(),sink);System.out.println("CORE055_WILD biome="+BiomeRegions.NAMES[entry.getKey()]+" chunk="+entry.getValue()+" writes="+(writes[0]-before));}
        var under=player.level().getServer().getLevel(GuogaologyMod.GUOGAO);long underSeed=ProceduralTerrain.seed(under.getChunkSource().randomState());NaturalScenery.render(underSeed,true,new ChunkPos(400,400),sink);
        check(writes[0]>0,"native scenery sampler actually emitted geometry");
        // In addition to random clipped samples, exercise whole production object
        // painters so every new raw source is represented regardless of seed luck.
        var draw=NaturalScenery.class.getDeclaredMethod("drawObject",SceneryBrush.class,NaturalScenery.Site.class,SceneryDistribution.Form.class);draw.setAccessible(true);
        var brush=new SceneryBrush(-96,96,-96,96,sink);
        var forms=List.of(SceneryDistribution.Form.RIDGE_CROWN,SceneryDistribution.Form.GRAHAM_TREE,SceneryDistribution.Form.SCG,SceneryDistribution.Form.VEBLEN,SceneryDistribution.Form.KNOT,SceneryDistribution.Form.SET_SHELL);
        for(var form:forms)draw.invoke(null,brush,new NaturalScenery.Site(seed,1234,0,0,form==SceneryDistribution.Form.SET_SHELL?7:0,90,Integer.MIN_VALUE,false),form);
        for(int attempt=0;attempt<4&&!counts.containsKey(CoreResources.LAVER_GEL);attempt++)draw.invoke(null,brush,new NaturalScenery.Site(seed,1234+attempt,0,0,4,90,Integer.MIN_VALUE,false),SceneryDistribution.Form.LAVER);
        NaturalScenery.treeAt(brush,new NaturalScenery.Site(underSeed,1234,0,0,6,90,Integer.MIN_VALUE,true),true,false,90,1,40);
        check(counts.keySet().containsAll(CoreResources.MATERIALS),"each of eight new raw materials occurs in its production scenery painter");
        System.out.println("CORE055_WILD_TOTAL writes="+writes[0]+" rawKinds="+counts.keySet().stream().filter(CoreResources.MATERIALS::contains).count());
    }
    private static void freshOres(ServerPlayer player){
        // A fixed chunk coordinate can lie entirely between floating continents.
        // Search land, not ore: the same eight separated patches apply to every seed.
        int[][] patches={{520,520},{584,520},{520,584},{456,520},{520,456},{584,584},{456,584},{584,456}};
        for(var world:List.of(player.level().getServer().getLevel(GuogaologyMod.DIMENSION),player.level().getServer().getLevel(GuogaologyMod.GUOGAO))){
            int ores=0,hosts=0,chunks=0,hostChunks=0,skippedAir=0,skippedThin=0,patchesTried=0;long start=System.nanoTime();var oreCounts=new int[3];
            for(var patch:patches){
                patchesTried++;System.out.println("CORE055_ORE_PATCH dimension="+world.dimension().identifier()+" candidate="+patchesTried+" origin="+Arrays.toString(patch));
                for(int cx=patch[0];cx<patch[0]+2;cx++)for(int cz=patch[1];cz<patch[1]+2;cz++){
                    var chunk=world.getChunk(cx,cz);chunks++;int chunkHosts=0,chunkOres=0;
                    for(int sectionIndex=0;sectionIndex<chunk.getSectionsCount();sectionIndex++){
                        var section=chunk.getSection(sectionIndex);if(section.hasOnlyAir())continue;
                        for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                            var state=section.getBlockState(x,y,z);int ore=CoreResources.ORES.indexOf(state.getBlock());
                            if(ore>=0){chunkOres++;oreCounts[ore]++;}else if(state.getBlock() instanceof NumberStoneBlock||state.is(GuogaologyBlocks.ABSENCE_GLASS)||state.is(GuogaologyBlocks.ROOTBOUND_STONE))chunkHosts++;
                        }
                    }
                    hosts+=chunkHosts;ores+=chunkOres;
                    if(chunkHosts+chunkOres>=1000)hostChunks++;
                    else if(chunkHosts+chunkOres==0)skippedAir++;
                    else skippedThin++;
                }
                if(hostChunks>=4)break;
            }
            check(hostChunks>=4,"bounded native ore sampler found four host-bearing chunks: "+world.dimension()+" patches="+patchesTried+" hostChunks="+hostChunks+" skippedAirOrHostless="+skippedAir+" skippedThin="+skippedThin);
            check(ores>0,"fresh host-bearing native dimension chunks contain actual Ordinal ore: "+world.dimension()+" host="+hosts+" ore="+ores);
            System.out.println("CORE055_FRESH_ORE dimension="+world.dimension().identifier()+" patches="+patchesTried+" chunks="+chunks+" hostBearingChunks="+hostChunks+" skippedAirOrHostless="+skippedAir+" skippedThin="+skippedThin+" motherRock="+hosts+" ore="+ores+" skins="+Arrays.toString(oreCounts)+" seconds="+(System.nanoTime()-start)/1e9+" sampleOnlyNotKm2Calibration=true");
        }
    }
    public static void display(ServerPlayer player,int frame){
        var world=player.level().getServer().overworld();player.closeContainer();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getInventory().clearContent();
        for(var p:BlockPos.betweenClosed(new BlockPos(-14,219,-5),new BlockPos(14,227,13)))world.setBlock(p,p.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        if(frame==0){
            for(int i=0;i<CoreResources.MATERIALS.size();i++){var at=new BlockPos((i%4)*6-9,220,(i/4)*5);world.setBlock(at,Blocks.CALCITE.defaultBlockState(),2);world.setBlock(at.above(),CoreResources.MATERIALS.get(i).defaultBlockState(),2);}
            for(int i=0;i<CoreResources.ORES.size();i++){var at=new BlockPos(i*6-6,220,10);world.setBlock(at,CoreResources.ORES.get(i).defaultBlockState(),2);world.setBlock(at.above(),CoreResources.ORES.get(i).defaultBlockState(),2);}
        }else{
            var types=List.of(GuogaologyBlocks.LHO_HYDRA_PSI,GuogaologyBlocks.LHO_HYDRA_Z,GuogaologyBlocks.FFFZ_TRACE,GuogaologyBlocks.FOS_TRACE);
            for(int i=0;i<types.size();i++)for(int row=0;row<2;row++){
                var at=new BlockPos(i*6-9,221,row*8);world.setBlock(at.below(),row==0?Blocks.CONCRETE.gray().defaultBlockState():GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),3);
                world.setBlock(at,types.get(i).defaultBlockState().setValue(AnchoredBlock.ANCHORED,row==1),2);
            }
        }
        player.teleport(new TeleportTransition(world,new Vec3(10.5,226,21.5),Vec3.ZERO,155,20,TeleportTransition.DO_NOTHING));player.hasChangedDimension();if(!player.connection.hasClientLoaded())player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());player.getAbilities().flying=true;player.onUpdateAbilities();
    }
}
