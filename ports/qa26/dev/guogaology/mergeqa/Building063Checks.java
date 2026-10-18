package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.survival.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;

/** Native palette/placement checks; no normal saves or old-world rewrites. */
public final class Building063Checks {
    private static int checks;
    private static volatile Vec3 camera=Vec3.ZERO;
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("Building063: "+message);}
    private static SurvivalStructures.Site site(SurvivalTheme theme){return new SurvivalStructures.Site(0,90,0,theme,0);}
    private static BlockState material(SurvivalTheme theme,int x,int y,int z,byte code){return SurvivalStructures.stateFor(site(theme),x,y,z,code);}
    public static boolean cameraReady(Vec3 position){return position.distanceToSqr(camera)<.25;}
    public static void run(ServerPlayer player)throws Exception{
        Astra512Checks.runSanctuaryMaterials();
        var tower=SanctuaryLayout.of(SurvivalTheme.POWER);
        for(int x:new int[]{-6,6}){
            byte code=tower.at(x,5,28);
            check(code==SanctuaryLayout.FIXED_NUMBER+1,"actual tower doorway blueprint has digit1");
            check(material(SurvivalTheme.POWER,x,5,28,code).equals(GuogaologyBlocks.ordinalBrick(1)),"tower doorway native material is digit1");
        }
        for(var theme:new SurvivalTheme[]{SurvivalTheme.ABSENCE,SurvivalTheme.GUOGAO}){
            var layout=SanctuaryLayout.of(theme);int ones=0;
            for(int y=0;y<layout.height;y++)for(int z=-layout.depth/2;z<=layout.depth/2;z++)for(int x=-layout.width/2;x<=layout.width/2;x++){
                byte code=layout.at(x,y,z);if(code!=SanctuaryLayout.FIXED_NUMBER+1)continue;
                ones++;check(material(theme,x,y,z,code).equals(GuogaologyBlocks.ordinalBrick(1)),"actual fixed1 native material "+theme);
            }
            check(ones==(theme==SurvivalTheme.ABSENCE?17:12),"retained authored digit1 count "+theme+"/"+ones);
        }
        check(CoreResources.GUOGAO_DROP.defaultBlockState().getLightEmission()==15,"legitimate Guogao retains native light15");
        System.out.println("BUILDING063_SERVER_OK checks="+checks+" fixedDigits128/tower2/lho17/garland12/globes24/nativeLight");
        Files.writeString(Path.of("building063-native-checks.txt"),"checks="+checks+"\nNative registered palette + actual production blueprints; no saved-data migration.\n");
    }
    public static void display(ServerPlayer player,int scene){
        var world=player.level().getServer().overworld();
        for(var q:BlockPos.betweenClosed(new BlockPos(-13,89,-4),new BlockPos(24,108,20)))
            world.setBlock(q,q.getY()==89?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),FLAGS);
        if(scene==0){
            for(int digit=0;digit<16;digit++){
                var q=new BlockPos(digit-7,91,0);var state=material(SurvivalTheme.POWER,0,0,0,(byte)(SanctuaryLayout.FIXED_NUMBER+digit));
                world.setBlock(q,state,FLAGS);check(world.getBlockState(q).equals(GuogaologyBlocks.ordinalBrick(digit)),"placed/read native fixed digit "+digit);
            }
            for(int digit=0;digit<3;digit++)world.setBlock(new BlockPos(digit-1,93,0),material(SurvivalTheme.ABSENCE,0,0,0,(byte)(SanctuaryLayout.FIXED_NUMBER+digit)),FLAGS);
            camera=new Vec3(.5,94.0,15.5);
        }else{
            int[] digits={1,1,2,4,2};
            for(int i=0;i<digits.length;i++)world.setBlock(new BlockPos(i-2,92,0),material(SurvivalTheme.GUOGAO,0,0,0,(byte)(SanctuaryLayout.FIXED_NUMBER+digits[i])),FLAGS);
            var q=SanctuaryLayout.of(SurvivalTheme.GUOGAO).emojiLampCenters().getFirst();
            var raw=material(SurvivalTheme.GUOGAO,q.x(),q.y(),q.z(),SanctuaryLayout.of(SurvivalTheme.GUOGAO).at(q.x(),q.y(),q.z()));
            world.setBlock(new BlockPos(-5,92,0),raw,FLAGS);world.setBlock(new BlockPos(5,92,0),raw,FLAGS);
            check(world.getBlockState(new BlockPos(-2,92,0)).equals(GuogaologyBlocks.ordinalBrick(1)),"native11242first1");
            check(world.getBlockState(new BlockPos(-1,92,0)).equals(GuogaologyBlocks.ordinalBrick(1)),"native11242second1");
            check(world.getBlockState(new BlockPos(-5,92,0)).is(CoreResources.GUOGAO_DROP),"native nearby legitimate Guogao remains distinct");
            camera=new Vec3(.5,94.0,13.5);
        }
        player.closeContainer();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);
        player.teleport(new TeleportTransition(world,camera,Vec3.ZERO,180,8,TeleportTransition.DO_NOTHING));player.hasChangedDimension();
    }
}
