package dev.googology.block;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The two dedicated LHO tree materials disappear together; ordinary psi plants are unaffected. */
public final class VanishingHydraBlock extends AnchoredBlock {
    public VanishingHydraBlock(Properties settings) { super(settings); }
    public static Set<BlockPos> connected(ServerLevel world,BlockPos start) {
        var found=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
        found.add(start.immutable());queue.add(start.immutable());
        while(!queue.isEmpty()&&found.size()<16384) {
            var p=queue.removeFirst();
            for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) for(int z=-1;z<=1;z++) {
                var next=p.offset(x,y,z);
                if(found.contains(next)||Math.abs(next.getX()-start.getX())>64||Math.abs(next.getY()-start.getY())>64||Math.abs(next.getZ()-start.getZ())>64||world.isOutsideBuildHeight(next)) continue;
                // The small natural tree may cross a chunk border. Finish reading it before removal.
                var state=world.getBlockState(next);
                if(state.getBlock() instanceof VanishingHydraBlock&&!stable(state)) { found.add(next);queue.addLast(next); }
            }
        }
        return found;
    }
    @Override public BlockState playerWillDestroy(Level world,BlockPos pos,BlockState state,Player player) {
        var result=super.playerWillDestroy(world,pos,state,player);
        if(world instanceof ServerLevel server) {
            if(protectedAt(state,world,pos))return result;
            var tree=connected(server,pos);int index=0;
            var protection=OrdinalAnchoring.around(world,tree);
            for(var p:tree) {
                if(stable(server.getBlockState(p))||protection.protects(p))continue;
                server.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                if(index++%12==0) server.sendParticles(ParticleTypes.REVERSE_PORTAL,p.getX()+.5,p.getY()+.5,p.getZ()+.5,2,.2,.2,.2,.01);
            }
        }
        return result;
    }
}
