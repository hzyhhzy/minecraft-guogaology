package dev.googology.mergeqa;

import dev.googology.*;
import dev.googology.portal.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Loaded recipes and actual return rituals for 0.3.9; development JAR only. */
final class ReturnPortalChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"googology:"+id)));}
    private static CraftingInput input(ItemStack... cells){
        var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
        for(int i=0;i<cells.length;i++)grid.set(i,cells[i]);
        return CraftingInput.of(3,3,grid);
    }
    private static void craft(ServerLevel level,Item result,int count,ItemStack... cells){
        craft(level,result,count,input(cells));
    }
    private static void craft(ServerLevel level,Item result,int count,CraftingInput input){
        var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        check(recipe.isPresent(),"loaded return recipe matches "+result);
        var output=recipe.orElseThrow().value().assemble(input);
        check(output.is(result)&&output.getCount()==count,"actual return recipe output "+result+": "+output);
    }
    private static void rejectRecipe(ServerLevel level,Item result,ItemStack... cells){
        rejectRecipe(level,result,input(cells));
    }
    private static void rejectRecipe(ServerLevel level,Item result,CraftingInput input){
        var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        check(recipe.isEmpty()||!recipe.orElseThrow().value().assemble(input).is(result),"incorrect return ingredient count/type is rejected: "+result);
    }
    private static CraftingInput diagonal(ItemStack first,ItemStack second,boolean mirror){
        return CraftingInput.of(2,2,mirror?List.of(ItemStack.EMPTY,first,second,ItemStack.EMPTY):List.of(first,ItemStack.EMPTY,ItemStack.EMPTY,second));
    }
    private static void recipes(ServerLevel level){
        int planks=0;var dread=stack("dread_planks");
        check(dread.is(ItemTags.PLANKS),"Dread Fir planks are in the general plank ingredient tag");
        for(var item:BuiltInRegistries.ITEM){
            var plank=new ItemStack(item);if(!plank.is(ItemTags.PLANKS))continue;planks++;
            craft(level,GoogologyBlocks.OUTER_RETURN_FRAME.asItem(),1,new ItemStack(Blocks.COBBLESTONE),plank,new ItemStack(Blocks.COBBLESTONE),new ItemStack(Blocks.COBBLESTONE),new ItemStack(Blocks.COBBLESTONE));
            craft(level,GoogologyBlocks.INNER_RETURN_FRAME.asItem(),1,stack("ordinal_shard"),plank,stack("ordinal_shard"),stack("ordinal_shard"),stack("ordinal_shard"));
            craft(level,GoogologyBlocks.RETURN_TOKEN,1,CraftingInput.of(1,2,List.of(plank,new ItemStack(Items.STICK))));
            for(boolean mirror:new boolean[]{false,true}){
                rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,diagonal(plank,plank.copy(),mirror));
                rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,diagonal(plank,new ItemStack(Blocks.OAK_PLANKS),mirror));
            }
            rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(2,1,List.of(plank,plank.copy())));
            rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(1,2,List.of(plank,plank.copy())));
            rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(2,1,List.of(plank,new ItemStack(Items.STICK))));
            rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(1,2,List.of(new ItemStack(Items.STICK),plank)));
        }
        check(planks>=10,"general plank recipes exercise actual vanilla and mod tag members");
        craft(level,GoogologyBlocks.GUOGAO_RETURN_FRAME.asItem(),1,new ItemStack(GoogologyBlocks.GUOGAO_LOAM),dread,new ItemStack(GoogologyBlocks.GUOGAO_LOAM),new ItemStack(GoogologyBlocks.GUOGAO_LOAM),new ItemStack(GoogologyBlocks.GUOGAO_LOAM));
        var bases=List.of(new ItemStack(Blocks.COBBLESTONE),stack("ordinal_shard"),new ItemStack(GoogologyBlocks.GUOGAO_LOAM));
        var results=List.of(GoogologyBlocks.OUTER_RETURN_FRAME.asItem(),GoogologyBlocks.INNER_RETURN_FRAME.asItem(),GoogologyBlocks.GUOGAO_RETURN_FRAME.asItem());
        for(int i=0;i<3;i++){
            var material=bases.get(i);var plank=i==2?dread:new ItemStack(Blocks.OAK_PLANKS);var result=results.get(i);
            rejectRecipe(level,result,material,material.copy(),material.copy(),plank);
            rejectRecipe(level,result,material,material.copy(),material.copy(),material.copy(),material.copy(),plank);
            rejectRecipe(level,result,material,material.copy(),material.copy(),material.copy(),new ItemStack(Items.STICK));
        }
        rejectRecipe(level,GoogologyBlocks.GUOGAO_RETURN_FRAME.asItem(),bases.get(2),bases.get(2).copy(),bases.get(2).copy(),bases.get(2).copy(),new ItemStack(Blocks.OAK_PLANKS));
        rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,new ItemStack(Blocks.OAK_PLANKS));
        rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,new ItemStack(Blocks.OAK_PLANKS),new ItemStack(Blocks.OAK_PLANKS),new ItemStack(Blocks.OAK_PLANKS));
        rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,new ItemStack(Blocks.OAK_LOG),new ItemStack(Blocks.OAK_LOG));
        rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(1,2,List.of(new ItemStack(Blocks.OAK_LOG),new ItemStack(Items.STICK))));
        rejectRecipe(level,GoogologyBlocks.RETURN_TOKEN,CraftingInput.of(1,3,List.of(new ItemStack(Blocks.OAK_PLANKS),new ItemStack(Items.STICK),new ItemStack(Items.STICK))));
        craft(level,Blocks.OAK_PRESSURE_PLATE.asItem(),1,CraftingInput.of(2,1,List.of(new ItemStack(Blocks.OAK_PLANKS),new ItemStack(Blocks.OAK_PLANKS))));
        craft(level,Items.STICK,4,CraftingInput.of(1,2,List.of(new ItemStack(Blocks.OAK_PLANKS),new ItemStack(Blocks.OAK_PLANKS))));
        System.out.println("RETURN_RECIPES_039_OK plank_members="+planks+" token_layout=plank_above_stick old_two_plank_diagonals=rejected vanilla_pressure_plate_and_sticks=preserved");
    }
    private static void clear(ServerLevel world,BlockPos center){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z);
        for(var pos:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,5,4)))world.setBlock(pos,pos.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private static void ring(ServerLevel world,BlockPos center,Block material){
        int count=0;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(PortalRitual.isFrameOffset(x,z)){world.setBlock(center.offset(x,0,z),material.defaultBlockState(),2);count++;}
        check(count==12,"return ring still uses exactly twelve frames in its 5x5 footprint");
    }
    private static ItemEntity offering(ServerLevel world,BlockPos center,ItemStack stack){return new ItemEntity(world,center.getX()+2.5,center.getY()+.6,center.getZ()+.5,stack);}
    private static void appearance(ServerLevel world,BlockPos center,PortalKind kind){
        int style=kind.appearance(world.dimension().identifier().toString());
        check(world.getBlockState(center).getValue(dev.googology.block.GoogologyPortalBlock.STYLE)==style,"return field uses destination appearance");
        check(world.getBlockState(center.offset(2,0,0)).getValue(dev.googology.block.GoogologyPortalFrameBlock.STYLE)==style,"return active frame uses destination appearance");
    }
    static void checkAutomaticArrival(ServerPlayer player,PortalKind kind){
        var world=player.level();var landing=player.blockPosition();var center=landing.offset(0,0,-3);var gate=PortalRitual.gateAt(world,center);
        check(gate!=null&&gate.kind()==kind&&PortalRitual.complete(world,gate),"forward travel creates a complete usable reverse gate without a manual return ritual");
        check(PortalTravel.isSafe(world,landing),"automatic reverse gate has a safe landing");
        appearance(world,gate.center(),kind);
    }
    static void run(ServerPlayer player){
        var server=player.level().getServer();var ow=server.overworld();recipes(ow);
        var worlds=List.of(server.getLevel(GoogologyMod.OUTER),server.getLevel(GoogologyMod.DIMENSION),server.getLevel(GoogologyMod.GUOGAO));
        var materials=List.of(GoogologyBlocks.OUTER_RETURN_FRAME,GoogologyBlocks.INNER_RETURN_FRAME,GoogologyBlocks.GUOGAO_RETURN_FRAME);
        var legacy=List.of(Blocks.DIRT,GoogologyBlocks.ABSENCE_GLASS,GoogologyBlocks.GUOGAO_LOAM);
        var innerLegacy=new ArrayList<Block>(Arrays.asList(GoogologyBlocks.NUMBER_STONES));innerLegacy.add(GoogologyBlocks.ABSENCE_GLASS);
        var legacyGroups=List.of(List.of(Blocks.DIRT,Blocks.COBBLESTONE),innerLegacy,List.of(GoogologyBlocks.GUOGAO_LOAM,GoogologyBlocks.ROOTBOUND_STONE));
        var kinds=List.of(PortalKind.GGG,PortalKind.INNER,PortalKind.GUOGAO);
        var destinations=List.of(Level.OVERWORLD,GoogologyMod.OUTER,GoogologyMod.DIMENSION);
        check(PortalRitual.isOffering(new ItemStack(GoogologyBlocks.RETURN_TOKEN)),"return token is recognized as an activation offering");
        check(PortalRitual.ritualKind(ow,new ItemStack(GoogologyBlocks.RETURN_TOKEN))==null,"return token cannot open an Overworld entry");
        for(var frame:materials){
            check(!PortalRitual.isFrameMaterial(ow,frame.defaultBlockState(),PortalKind.GGG),"return frames cannot replace the Overworld cake ring");
            check(!PortalRitual.isOffering(new ItemStack(frame)),"return frame item is not its own activation offering");
        }
        for(int i=0;i<3;i++){
            var world=worlds.get(i);var frame=materials.get(i);var old=legacy.get(i);var kind=kinds.get(i);var center=new BlockPos(56,240,56);
            check(PortalRitual.ritualKind(world,new ItemStack(GoogologyBlocks.RETURN_TOKEN))==kind,"token selects the return route for its source dimension");
            check(PortalTravel.destination(world.dimension(),kind).equals(destinations.get(i)),"fixed return route reaches the preceding dimension");
            for(var local:legacyGroups.get(i)){
                check(!PortalRitual.isOffering(new ItemStack(local)),"old local material no longer triggers a return ritual: "+BuiltInRegistries.BLOCK.getKey(local));
                check(PortalRitual.ritualKind(world,new ItemStack(local))==null,"old local offering has no return route");
                check(!PortalRitual.isFrameMaterial(world,local.defaultBlockState(),kind),"old local block cannot form a return frame");
            }
            check(!frame.defaultBlockState().requiresCorrectToolForDrops(),"fixed return frame needs no special harvest tool");
            var drops=Block.getDrops(frame.defaultBlockState(),world,center,null,player,ItemStack.EMPTY);
            check(drops.size()==1&&drops.getFirst().is(frame.asItem())&&drops.getFirst().getCount()==1,"fixed return frame is recovered barehanded");
            clear(world,center);ring(world,center,old);
            var oldRingToken=offering(world,center,new ItemStack(GoogologyBlocks.RETURN_TOKEN,2));
            check(!PortalRitual.tryActivate(world,oldRingToken)&&oldRingToken.getItem().getCount()==2,"token cannot activate an old local ring and is preserved");
            ring(world,center,frame);check(PortalRitual.isValidRing(world,center,kind),"twelve correct fixed frames form a valid ring");
            var oldOffering=offering(world,center,new ItemStack(old,2));
            check(!PortalRitual.tryActivate(world,oldOffering)&&oldOffering.getItem().getCount()==2,"old offering cannot activate a fixed ring and is preserved");
            for(int other=0;other<3;other++)if(other!=i){
                check(!PortalRitual.isFrameMaterial(world,materials.get(other).defaultBlockState(),kind),"foreign return frame rejected in this source dimension");
                world.setBlock(center.offset(2,0,0),materials.get(other).defaultBlockState(),2);
                var mixed=offering(world,center,new ItemStack(GoogologyBlocks.RETURN_TOKEN,2));
                check(!PortalRitual.isValidRing(world,center,kind)&&!PortalRitual.tryActivate(world,mixed)&&mixed.getItem().getCount()==2,"one foreign frame invalidates mixed return ring without consuming token");
                check(PortalRitual.gateAt(world,center)==null,"rejected mixed ring is not registered as a gate");
            }
            ring(world,center,frame);var token=offering(world,center,new ItemStack(GoogologyBlocks.RETURN_TOKEN,3));
            check(PortalRitual.tryActivate(world,token)&&!token.isRemoved()&&token.getItem().getCount()==2,"activation at outer frame edge consumes exactly one token from a stack");
            var gate=PortalRitual.gateAt(world,center);
            check(gate!=null&&gate.kind()==kind&&PortalRitual.complete(world,gate),"fixed frame activation creates the complete expected return gate");
            appearance(world,center,kind);
            check(!PortalRitual.tryActivate(world,token)&&token.getItem().getCount()==2,"already active return gate cannot consume another token");
            world.destroyBlock(center.offset(2,0,0),false,player);
            check(PortalRitual.gateAt(world,center)==null,"breaking return frame removes gate registration");
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(PortalRitual.isFrameOffset(x,z)||Math.abs(x)<=1&&Math.abs(z)<=1)
                check(world.getBlockState(center.offset(x,0,z)).isAir(),"breaking one return frame clears every frame and field cell");
        }
        System.out.println("RETURN_PORTALS_039_OK checks="+checks);
    }
    static void display(ServerPlayer player){
        var level=player.level().getServer().overworld();int z=600;
        // Reuse the loaded material fixture after its existing screenshots.
        for(var pos:BlockPos.betweenClosed(new BlockPos(-12,219,z-9),new BlockPos(12,225,z+10)))
            level.setBlock(pos,pos.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        Block[] frames={GoogologyBlocks.OUTER_RETURN_FRAME,GoogologyBlocks.INNER_RETURN_FRAME,GoogologyBlocks.GUOGAO_RETURN_FRAME};
        for(int i=0;i<3;i++){
            int x=(i-1)*3;
            level.setBlock(new BlockPos(x,220,z),frames[i].defaultBlockState(),2);
            level.setBlock(new BlockPos(x,220,z+3),GoogologyBlocks.PORTAL_FRAME.defaultBlockState().setValue(dev.googology.block.GoogologyPortalFrameBlock.STYLE,i),2);
            level.setBlock(new BlockPos(x,220,z+2),GoogologyBlocks.PORTAL.defaultBlockState().setValue(dev.googology.block.GoogologyPortalBlock.STYLE,i),2);
            level.setBlock(new BlockPos(x,219,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            level.setBlock(new BlockPos(x,219,z+3),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
        }
        var token=EntityTypes.ITEM_DISPLAY.create(level,EntitySpawnReason.COMMAND);
        check(token!=null&&token.getSlot(0).set(new ItemStack(GoogologyBlocks.RETURN_TOKEN)),"visible return token item display accepts the real item");
        token.snapTo(5.5,221.1,z+1.5,0,0);level.addFreshEntity(token);
        level.setBlock(new BlockPos(5,220,z+1),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
        player.setGameMode(GameType.CREATIVE);
        player.teleport(new TeleportTransition(level,new Vec3(1.5,223.2,z+10.5),Vec3.ZERO,180,19,TeleportTransition.DO_NOTHING));
        System.out.println("RETURN_ART_039_SCENE material_scene=9 frames=outer/inner/guogao active_styles=0/1/2 token=item_display");
    }
}
