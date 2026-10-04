package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

/** The two dedicated LHO tree materials disappear together; ordinary psi plants are unaffected. */
public final class VanishingHydraBlock extends AnchoredBlock {
    public VanishingHydraBlock(Settings settings) { super(settings); }
    public static Set<BlockPos> connected(ServerWorld world,BlockPos start) {
        var found=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
        found.add(start.toImmutable());queue.add(start.toImmutable());
        while(!queue.isEmpty()&&found.size()<16384) {
            var p=queue.removeFirst();
            for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) for(int z=-1;z<=1;z++) {
                var next=p.add(x,y,z);
                if(found.contains(next)||Math.abs(next.getX()-start.getX())>64||Math.abs(next.getY()-start.getY())>64||Math.abs(next.getZ()-start.getZ())>64||world.isOutOfHeightLimit(next)) continue;
                // The small natural tree may cross a chunk border. Finish reading it before removal.
                var state=world.getBlockState(next);
                if(state.getBlock() instanceof VanishingHydraBlock&&!stable(state)) { found.add(next);queue.addLast(next); }
            }
        }
        return found;
    }
    @Override public BlockState onBreak(World world,BlockPos pos,BlockState state,PlayerEntity player) {
        var result=super.onBreak(world,pos,state,player);
        if(world instanceof ServerWorld server) {
            if(protectedAt(state,world,pos))return result;
            var tree=connected(server,pos);int index=0;
            var protection=OrdinalAnchoring.around(world,tree);
            for(var p:tree) {
                if(stable(server.getBlockState(p))||protection.protects(p))continue;
                server.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
                if(index++%12==0) server.spawnParticles(ParticleTypes.REVERSE_PORTAL,p.getX()+.5,p.getY()+.5,p.getZ()+.5,2,.2,.2,.2,.01);
            }
        }
        return result;
    }
}
