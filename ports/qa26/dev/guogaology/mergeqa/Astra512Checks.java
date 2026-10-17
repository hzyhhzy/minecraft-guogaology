package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.block.*;
import dev.guogaology.world.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Actual production scenery, recipe matcher and native art, in a disposable world. */
public final class Astra512Checks {
    private static int checks;
    private static long occupied,eligible,pearls;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static ItemStack craft(ServerLevel w,List<ItemStack> items){
        var input=CraftingInput.of(3,3,items);var found=w.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,w);
        return found.isPresent()?found.orElseThrow().value().assemble(input):ItemStack.EMPTY;
    }
    private static List<ItemStack> astra(ItemStack lamp){
        var inputs=new ArrayList<ItemStack>();inputs.add(item("cloud_pearl"));for(int i=0;i<6;i++)inputs.add(item("compute_chip"));inputs.add(lamp);inputs.add(item("ordinal_crystal"));return inputs;
    }
    public static void run(ServerPlayer p)throws Exception{
        for(var lamp:GuogaologyBlocks.LANTERNS)for(int emotion=0;emotion<2;emotion++)for(int part=0;part<28;part++)if(part!=13){
            var stack=StatefulDecorBlock.copyAppearance(new ItemStack(lamp),lamp.defaultBlockState().setValue(EmojiLanternBlock.EMOTION,emotion).setValue(EmojiLanternBlock.PART,part));
            var inputs=astra(stack);Collections.rotate(inputs,part%9);var result=craft(p.level(),inputs);
            check(result.is(GuogaologyBlocks.ASTRA_CRITICAL_CORE.asItem())&&result.getCount()==1,"native Astra recipe accepts every lamp color/expression/form");
        }
        for(int digit=0;digit<16;digit++){
            var matrix=new ArrayList<ItemStack>();for(String name:List.of("matrix_sheet","ridge_lamina","projection_glass","y_leaves"))matrix.add(item(name));
            for(int k=0;k<4;k++)matrix.add(StatefulDecorBlock.copyAppearance(item("ordinal_bricks"),GuogaologyBlocks.ordinalBrick((digit+k*3)%16)));
            matrix.add(item("ordinal_crystal"));check(craft(p.level(),matrix).is(GuogaologyBlocks.SEQUENCE_CORE.asItem()),"Matrix recipe accepts mixed digit bricks");
        }
        var missing=astra(item("guogao_lantern"));missing.set(5,ItemStack.EMPTY);check(craft(p.level(),missing).isEmpty(),"six separate chips are required");
        var old=List.of("cloud_pearl","cloud_pearl","compute_chip","compute_chip","astra_cloud","astra_cloud","astra_marble","astra_mint","ordinal_crystal").stream().map(Astra512Checks::item).toList();
        check(craft(p.level(),old).isEmpty(),"old stone-and-cloud recipe removed");
        var boundary=List.of("set_membrane","set_membrane","turing_tape","turing_tape","proof_stone","turing_endcap","formula_stone","rayo_rose","ordinal_crystal").stream().map(Astra512Checks::item).toList();
        check(craft(p.level(),boundary).is(GuogaologyBlocks.BOUNDARY_CORE.asItem()),"Turing Terminus replaces exactly one Proof Stone");
        for(long salt:new long[]{1,93811,451321,512512})cloud(salt,salt==1);
        double rate=pearls/(double)eligible;check(rate>.009&&rate<.011,"buried pearl frequency is1%: "+rate);
        city();
        sprigs(p);
        guogao(p);
        Laver511Checks.run(p);
        var shape=CoreResources.SET_MEMBRANE.defaultBlockState().getShape(p.level(),p.blockPosition());
        for(var box:shape.toAabbs())check(box.minX>=.125&&box.maxX<=.875&&box.minY>=.125&&box.maxY<=.875,"membrane shape stays symmetric about center");
        System.out.println("ASTRA512_SERVER_OK checks="+checks+" clouds="+occupied+" eligible="+eligible+" pearls="+pearls+" rate="+rate+" 324lampRecipes/sixChips/removedOldRecipe/terminus/chairs/starlamps/haloDeterminism");
    }
    private static void cloud(long salt,boolean compareChunks)throws Exception{
        int cx=-7,cz=9,base=50,r=NaturalForms.cloudWidth(salt)/2+3,h=NaturalForms.cloudHeight(salt),sx=2*r+1,sy=h+24;
        var full=new BitSet(sx*sx*sy);
        NaturalForms.cloud(new VoxelBrush(cx-r,cx+r,base-12,base+h+11,cz-r,cz+r,(x,y,z,m)->full.set(((x-cx+r)*sx+z-cz+r)*sy+y-base+12)),cx,base,cz,salt);
        var points=new HashSet<BlockPos>();CloudPearls.draw(new VoxelBrush(cx-r,cx+r,-62,317,cz-r,cz+r,(x,y,z,m)->{check(m==NaturalForms.CLOUD_PEARL,"only raw pearls");points.add(new BlockPos(x,y,z));}),cx-r,cx+r,cz-r,cz+r,cx,base,cz,salt);
        long count=0;
        for(int i=full.nextSetBit(0);i>=0;i=full.nextSetBit(i+1)){
            int y=i%sy,x=i/(sx*sy),z=i/sy%sx;boolean inner=x>0&&x<sx-1&&z>0&&z<sx-1&&y>0&&y<sy-1;
            for(int dx=-1;dx<=1&&inner;dx++)for(int dz=-1;dz<=1&&inner;dz++)for(int dy=-1;dy<=1;dy++)if(!full.get(i+(dx*sx+dz)*sy+dy)){inner=false;break;}
            if(inner)count++;
        }
        for(var q:points)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=1;dy++)check(full.get(((q.getX()+dx-cx+r)*sx+q.getZ()+dz-cz+r)*sy+q.getY()+dy-base+12),"every pearl replaces an actually buried cloud voxel including all26 neighbors");
        check(points.size()>100,"large cloud contains appreciable raw supply");
        if(compareChunks){
            var clipped=new HashSet<BlockPos>();
            for(int x=Math.floorDiv(cx-r,16)*16;x<=cx+r;x+=16)for(int z=Math.floorDiv(cz-r,16)*16;z<=cz+r;z+=16)
                CloudPearls.draw(new VoxelBrush(x,x+15,-62,317,z,z+15,(a,b,c,m)->clipped.add(new BlockPos(a,b,c))),x,x+15,z,z+15,cx,base,cz,salt);
            check(clipped.equals(points),"chunk clips, negative coordinates and halo produce identical pearl sets");
            var painted=new HashMap<BlockPos,BlockState>();var brush=new SceneryBrush(cx-r,cx+r,cz-r,cz+r,(x,y,z,s)->painted.put(new BlockPos(x,y,z),s));
            var method=NaturalScenery.class.getDeclaredMethod("drawObject",SceneryBrush.class,NaturalScenery.Site.class,SceneryDistribution.Form.class);method.setAccessible(true);
            method.invoke(null,brush,new NaturalScenery.Site(512,salt,cx,cz,5,base,Integer.MIN_VALUE,false),SceneryDistribution.Form.CLOUD);
            for(var q:points){check(painted.get(q).is(CoreResources.CLOUD_PEARL),"real scenery adapter writes the raw material at every planned position");for(var direction:Direction.values())check(painted.get(q.relative(direction))!=null&&!painted.get(q.relative(direction)).isAir(),"final real cloud leaves no exposed pearl");}
            check(painted.values().stream().noneMatch(s->s.is(GuogaologyBlocks.ASTRA_CRITICAL_CORE)),"no finished cores added to wilderness");
        }
        occupied+=full.cardinality();eligible+=count;pearls+=points.size();
        System.out.println("ASTRA512_CLOUD salt="+salt+" cloud="+full.cardinality()+" interior="+count+" pearls="+points.size());
    }
    private static void guogao(ServerPlayer p)throws Exception{
        var recipe=List.of("dread_resin","guogao_drop","dread_leaves","emoji_flower","amber_guogao","star_gold","amber_sequence_light","guogao_lantern","ordinal_crystal").stream().map(Astra512Checks::item).toList();
        check(craft(p.level(),recipe).is(GuogaologyBlocks.GUOGAO_HEART.asItem()),"Heart recipe uses a Drop and Emoji Flower with one resin and one leaf");
        for(int height:new int[]{30,90,250})for(int dir:new int[]{-1,1}){
            var map=new HashMap<BlockPos,Integer>();FirDecorations.star(new VoxelBrush(-15,15,-62,317,-15,15,(x,y,z,m)->map.put(new BlockPos(x,y,z),m)),0,150,0,height,dir,true);
            var drops=map.entrySet().stream().filter(e->e.getValue()==NaturalForms.GUOGAO_DROP).toList();check(drops.size()==1,"one raw drop per Guogao star");
            for(var direction:Direction.values())check(map.getOrDefault(drops.getFirst().getKey().relative(direction),0)==NaturalForms.GOLD_STAR,"drop is inside the star with all6faces covered");
        }
        var layout=dev.guogaology.survival.SanctuaryLayout.of(dev.guogaology.survival.SurvivalTheme.GUOGAO);
        check(layout.emojiLampCenters().size()==24,"24existing emoji globes retained");
        for(var q:layout.emojiLampCenters())check(layout.at(q.x(),q.y(),q.z())==dev.guogaology.survival.SanctuaryLayout.GUOGAO_DROP,"exact former Lv1 Heart center now contains raw Drop");
        check(layout.finaleCores().size()==8,"eight finale tributes unchanged");
        var mapped=new HashMap<BlockPos,BlockState>();forms(mapped).set(0,220,0,NaturalForms.GUOGAO_DROP);check(mapped.get(new BlockPos(0,220,0)).is(CoreResources.GUOGAO_DROP),"natural raw-material index resolves to the new block");
    }
    private static void sprigs(ServerPlayer p)throws Exception{
        var method=NaturalScenery.class.getDeclaredMethod("hydra",SceneryBrush.class,NaturalScenery.Site.class);method.setAccessible(true);int upright=0,hanging=0;
        for(long salt=1;salt<=16;salt++){
            var site=new NaturalScenery.Site(512,salt,0,0,2,80,260,false);var map=new HashMap<BlockPos,BlockState>();
            method.invoke(null,new SceneryBrush(-100,100,-100,100,(x,y,z,state)->map.put(new BlockPos(x,y,z),state)),site);
            for(var e:map.entrySet())if(e.getValue().is(CoreResources.HYDRA_SPRIG)){
                boolean below=map.containsKey(e.getKey().below()),above=map.containsKey(e.getKey().above());
                check(below||above,"no sprig floats without vertical support");
                boolean inverted=e.getValue().getValue(HydraSprigBlock.HANGING);check(inverted==!below,"real branch support sets orientation, with floor priority");
                if(inverted)hanging++;else upright++;
                var bounds=e.getValue().getShape(p.level(),e.getKey()).bounds();check(inverted?bounds.maxY==1:bounds.minY==0,"matching collision reaches its supporting face");
            }
            if(salt<=2){
                var pieces=new HashMap<BlockPos,BlockState>();
                for(int x=-112;x<112;x+=16)for(int z=-112;z<112;z+=16)method.invoke(null,new SceneryBrush(x,x+15,z,z+15,(a,b,c,state)->pieces.put(new BlockPos(a,b,c),state)),site);
                check(pieces.equals(map),"upright and hanging Hydra generation is chunk-order independent");
            }
        }
        check(upright>0&&hanging>0,"both new sprig orientations naturally generate");System.out.println("ASTRA512_SPRIGS upright="+upright+" hanging="+hanging+" supported/shape/chunkClip");
    }
    private static VoxelBrush forms(Map<BlockPos,BlockState> blocks)throws Exception{
        var b=new SceneryBrush(-1000000,1000000,-1000000,1000000,(x,y,z,s)->blocks.put(new BlockPos(x,y,z),s));
        var method=NaturalScenery.class.getDeclaredMethod("forms",SceneryBrush.class);method.setAccessible(true);return (VoxelBrush)method.invoke(null,b);
    }
    private static void city()throws Exception{
        var blocks=new HashMap<BlockPos,BlockState>();AstraCity.draw(forms(blocks),new AstraCity.Building(0,220,0,13,13,3,0,512,220));int chairs=0;
        for(var e:blocks.entrySet())if(BuiltInRegistries.BLOCK.getKey(e.getValue().getBlock()).getPath().equals("nuke_chair")){
            chairs++;check(e.getValue().getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.SOUTH,"slump chair faces its desk");check(blocks.get(e.getKey().south()).is(GuogaologyBlocks.OFFICE_DESK),"chair keeps original office placement");
        }
        check(chairs>=4,"ordinary building has actual slump chairs");
        boolean lamps=false;
        for(int cx=-32;cx<=32&&!lamps;cx++)for(int cz=-32;cz<=32&&!lamps;cz++)if(AstraCity.district(512,cx*64+5,cz*64+12)){
            var materials=new HashMap<BlockPos,Integer>();int x=cx*64+5,z=cz*64+12;
            AstraCity.streets(new VoxelBrush(x,x,-62,317,z,z,(a,b,c,m)->materials.put(new BlockPos(a,b,c),m)),512,x,x,z,z);
            lamps=materials.containsValue(NaturalForms.GOLD_STAR);if(lamps){check(!materials.containsValue(NaturalForms.CRYSTAL),"streetlamp no longer uses stained glass");var mapped=new HashMap<BlockPos,BlockState>();forms(mapped).set(0,220,0,NaturalForms.GOLD_STAR);check(mapped.get(new BlockPos(0,220,0)).is(GuogaologyBlocks.STAR_GOLD),"native streetlamp material is Starburst Glass");}
        }
        check(lamps,"real eligible street has a Starburst Glass lamp");
    }
    public static void display(ServerPlayer p,int frame)throws Exception{
        var w=p.level().getServer().overworld();p.closeContainer();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var at:BlockPos.betweenClosed(new BlockPos(-12,219,-8),new BlockPos(14,232,12)))w.setBlock(at,at.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        Vec3 eye,target;
        if(frame==0){
            w.setBlock(new BlockPos(0,220,0),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),CoreResources.CLOUD_PEARL.defaultBlockState(),2);eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);
        }else if(frame==1){
            w.setBlock(new BlockPos(0,220,0),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),GuogaologyBlocks.ASTRA_CRITICAL_CORE.defaultBlockState(),2);eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);
        }else if(frame==2){
            w.setBlock(new BlockPos(0,220,0),GuogaologyBlocks.SET_GLASS.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),CoreResources.SET_MEMBRANE.defaultBlockState(),2);eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);
        }else if(frame==7){
            w.setBlock(new BlockPos(0,220,0),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),CoreResources.GUOGAO_DROP.defaultBlockState(),2);eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);
        }else if(frame==5||frame==6){
            boolean hanging=frame==6;
            w.setBlock(new BlockPos(0,hanging?222:220,0),GuogaologyBlocks.OMEGA_SYMBOL.defaultBlockState(),2);
            w.setBlock(new BlockPos(0,221,0),CoreResources.HYDRA_SPRIG.defaultBlockState().setValue(HydraSprigBlock.HANGING,hanging),2);
            eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);
        }else if(frame==4){
            w.setBlock(new BlockPos(0,220,0),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),CoreResources.MATRIX_SHEET.defaultBlockState(),2);eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.3,.5);
        }else{
            var blocks=new HashMap<BlockPos,BlockState>();AstraCity.draw(forms(blocks),new AstraCity.Building(0,220,0,13,13,3,0,512,220));
            for(var e:blocks.entrySet())if(e.getKey().getY()<=227&&e.getKey().getY()>=219&&Math.abs(e.getKey().getX())<13&&Math.abs(e.getKey().getZ())<13)w.setBlock(e.getKey(),e.getValue(),2);
            w.setBlock(new BlockPos(0,221,-4),CoreResources.TURING_ENDCAP.defaultBlockState(),2);
            w.setBlock(new BlockPos(1,221,-4),GuogaologyBlocks.STAR_GOLD.defaultBlockState(),2);
            eye=new Vec3(-3,224,-5);target=new Vec3(-8,221.5,0);
        }
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        p.teleport(new TeleportTransition(w,eye,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.getAbilities().flying=true;p.onUpdateAbilities();
    }
}
