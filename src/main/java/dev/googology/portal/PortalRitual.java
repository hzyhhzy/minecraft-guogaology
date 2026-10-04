package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;

public final class PortalRitual {
    private static final Set<PortalState.Gate> COLLAPSING=new HashSet<>();
    private PortalRitual() {}
    public static void initialize(){net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(PortalActivationPayload.ID,PortalActivationPayload.CODEC);}

    public static boolean isFrameOffset(int x,int z) {
        return Math.abs(x)==2 && Math.abs(z)<=1 || Math.abs(z)==2 && Math.abs(x)<=1;
    }
    public static boolean isFruit(ItemStack stack) {
        for(Block jelly:GoogologyBlocks.JELLIES) if(stack.isOf(jelly.asItem())) return true;
        return false;
    }
    public static boolean isOffering(ItemStack stack) { return stack.isOf(Items.APPLE)||isFruit(stack); }
    public static boolean isFrameMaterial(BlockState state) {
        if(state.isOf(Blocks.CAKE)) return state.get(CakeBlock.BITES)==0;
        for(Block jelly:GoogologyBlocks.JELLIES) if(state.isOf(jelly)) return true;
        return false;
    }
    public static boolean isValidRing(ServerWorld world,BlockPos center) {
        if(!world.getWorldBorder().contains(center.add(-2,0,-2)) || !world.getWorldBorder().contains(center.add(2,0,2))) return false;
        for(int x=-2;x<=2;x+=4)for(int z=-2;z<=2;z+=4)if(!world.isChunkLoaded(center.add(x,0,z)))return false;
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var state=world.getBlockState(center.add(x,0,z));
            if(isFrameOffset(x,z)) { if(!isFrameMaterial(state)) return false; }
            else if(Math.abs(x)<=1 && Math.abs(z)<=1 && (!state.isReplaceable() || !state.getFluidState().isEmpty())) return false;
        }
        return true;
    }
    public static boolean tryActivate(ServerWorld world,ItemEntity offering) {
        if(offering.isRemoved() || offering.getStack().isEmpty() || !isOffering(offering.getStack())) return false;
        PortalKind kind=isFruit(offering.getStack())?PortalKind.GUOGAO:PortalKind.GGG;
        if(world.getServer().getWorld(kind==PortalKind.GGG?GoogologyMod.DIMENSION:GoogologyMod.GUOGAO)==null) return false;
        // React over the entire frame, without waiting for the thrown item to drift into the inner 3x3.
        for(int dy=0;dy>=-4;dy--) for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            BlockPos center=offering.getBlockPos().add(dx,dy,dz);
            if(offering.getY()<center.getY()-.1 || offering.getY()>center.getY()+4 || !isValidRing(world,center)) continue;
            fillPortal(world,center,kind);
            offering.getStack().decrement(1);
            if(offering.getStack().isEmpty()) offering.discard();
            world.spawnParticles(kind==PortalKind.GGG?ParticleTypes.END_ROD:ParticleTypes.SOUL_FIRE_FLAME,center.getX()+.5,center.getY()+.3,center.getZ()+.5,65,1.3,.4,1.3,.03);
            world.playSound(null,center,SoundEvents.BLOCK_END_PORTAL_SPAWN,SoundCategory.BLOCKS,.65f,kind==PortalKind.GGG?1.1f:.7f);
            return true;
        }
        return false;
    }
    public static void fillPortal(ServerWorld world,BlockPos center) { fillPortal(world,center,PortalKind.GGG); }
    public static void fillPortal(ServerWorld world,BlockPos center,PortalKind kind) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            if(isFrameOffset(x,z)) world.setBlockState(center.add(x,0,z),GoogologyBlocks.PORTAL_FRAME.getDefaultState().with(dev.googology.block.GoogologyPortalFrameBlock.FRUIT,kind==PortalKind.GUOGAO),Block.NOTIFY_ALL);
            else if(Math.abs(x)<=1 && Math.abs(z)<=1) world.setBlockState(center.add(x,0,z),kind.block().getDefaultState(),Block.NOTIFY_ALL);
        }
        PortalState.get(world.getServer()).addGate(new PortalState.Gate(world.getRegistryKey().getValue().toString(),center.toImmutable(),kind));
        var appearance=new PortalActivationPayload(world.getRegistryKey().getValue().toString(),center.toImmutable(),kind==PortalKind.GUOGAO);
        for(var player:world.getPlayers())if(player.squaredDistanceTo(center.toCenterPos())<=64*64&&net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,PortalActivationPayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,appearance);

    }
    public static boolean complete(ServerWorld world,PortalState.Gate gate) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var state=world.getBlockState(gate.center().add(x,0,z));
            if(isFrameOffset(x,z) && !state.isOf(GoogologyBlocks.PORTAL_FRAME)) return false;
            if(Math.abs(x)<=1 && Math.abs(z)<=1 && !state.isOf(gate.kind().block())) return false;
        }
        return true;
    }
    public static PortalState.Gate gateAt(ServerWorld world,BlockPos position) {
        var data=PortalState.get(world.getServer());
        var gate=data.gateAt(world.getRegistryKey().getValue().toString(),position);
        if(gate!=null) return gate;
        // Upgrade complete pre-1.2 portals on first contact, without resetting the save.
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var center=position.add(x,0,z);var state=world.getBlockState(center);
            if(!PortalKind.isPortal(state)) continue;
            var candidate=new PortalState.Gate(world.getRegistryKey().getValue().toString(),center,PortalKind.of(state));
            if(complete(world,candidate)) { data.addGate(candidate);return candidate; }
        }
        return null;
    }
    public static void collapseAt(ServerWorld world,BlockPos position) {
        var data=PortalState.get(world.getServer());
        var gate=data.gateAt(world.getRegistryKey().getValue().toString(),position);
        if(gate==null && COLLAPSING.isEmpty()) {
            // A pre-1.2 gate may be broken before anybody has stepped into it.
            search: for(int cx=-2;cx<=2;cx++) for(int cz=-2;cz<=2;cz++) {
                var center=position.add(cx,0,cz);PortalKind kind=null;
                for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                    var state=world.getBlockState(center.add(x,0,z));if(PortalKind.isPortal(state)) kind=PortalKind.of(state);
                }
                if(kind==null) continue;
                boolean complete=true;
                for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                    var p=center.add(x,0,z);if(p.equals(position)) continue;
                    var state=world.getBlockState(p);
                    if(isFrameOffset(x,z) && !state.isOf(GoogologyBlocks.PORTAL_FRAME)) complete=false;
                    if(Math.abs(x)<=1 && Math.abs(z)<=1 && !state.isOf(kind.block())) complete=false;
                }
                if(complete) { gate=new PortalState.Gate(world.getRegistryKey().getValue().toString(),center,kind);break search; }
            }
        }
        if(gate==null || !COLLAPSING.add(gate)) return;
        try {
            data.removeGate(gate);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                if(!isFrameOffset(x,z) && !(Math.abs(x)<=1 && Math.abs(z)<=1)) continue;
                var pos=gate.center().add(x,0,z);var state=world.getBlockState(pos);
                if(state.isOf(GoogologyBlocks.PORTAL_FRAME)||PortalKind.isPortal(state)) world.setBlockState(pos,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
            }
        } finally { COLLAPSING.remove(gate); }
    }
}
