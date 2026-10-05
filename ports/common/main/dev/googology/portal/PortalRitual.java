package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalRitual {
    private static final Set<PortalState.Gate> COLLAPSING=new HashSet<>();
    private PortalRitual() {}
    public static void initialize(){net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(PortalActivationPayload.ID,PortalActivationPayload.CODEC);}

    public static boolean isFrameOffset(int x,int z) {
        return Math.abs(x)==2 && Math.abs(z)<=1 || Math.abs(z)==2 && Math.abs(x)<=1;
    }
    public static boolean isFruit(ItemStack stack) {
        for(Block jelly:GoogologyBlocks.JELLIES) if(stack.is(jelly.asItem())) return true;
        return false;
    }
    public static boolean isOffering(ItemStack stack) { return anyReturnMaterial(stack)||stack.is(Items.APPLE)||isFruit(stack)||stack.is(dev.googology.mining.MiningContent.MATERIALS[3]); }

    private static final net.minecraft.tags.TagKey<Block> OUTER_MATERIALS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,GoogologyMod.id("portal_outer_materials"));
    private static final net.minecraft.tags.TagKey<Block> INNER_MATERIALS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,GoogologyMod.id("portal_inner_materials"));
    private static final net.minecraft.tags.TagKey<Block> HELL_MATERIALS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,GoogologyMod.id("portal_guogao_materials"));
    private static boolean localMaterial(ServerLevel world,BlockState state){
        var source=world.dimension();
        return source.equals(GoogologyMod.OUTER)?state.is(OUTER_MATERIALS):source.equals(GoogologyMod.DIMENSION)?state.is(INNER_MATERIALS):source.equals(GoogologyMod.GUOGAO)&&state.is(HELL_MATERIALS);
    }
    private static boolean localOffering(ServerLevel world,ItemStack stack){return stack.getItem() instanceof net.minecraft.world.item.BlockItem item&&localMaterial(world,item.getBlock().defaultBlockState());}
    private static boolean anyReturnMaterial(ItemStack stack){if(!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item))return false;var state=item.getBlock().defaultBlockState();return state.is(OUTER_MATERIALS)||state.is(INNER_MATERIALS)||state.is(HELL_MATERIALS);}
    public static PortalKind ritualKind(ServerLevel world,ItemStack stack){
        var source=world.dimension();
        if(source.equals(net.minecraft.world.level.Level.OVERWORLD))return stack.is(Items.APPLE)?PortalKind.GGG:null;
        if(source.equals(GoogologyMod.OUTER))return localOffering(world,stack)?PortalKind.GGG:stack.is(dev.googology.mining.MiningContent.MATERIALS[3])?PortalKind.INNER:null;
        if(source.equals(GoogologyMod.DIMENSION))return localOffering(world,stack)?PortalKind.INNER:isFruit(stack)?PortalKind.GUOGAO:null;
        return source.equals(GoogologyMod.GUOGAO)&&localOffering(world,stack)?PortalKind.GUOGAO:null;
    }

    public static boolean isFrameMaterial(BlockState state) {
        if(state.is(Blocks.CAKE)) return state.getValue(CakeBlock.BITES)==0;
        for(Block jelly:GoogologyBlocks.JELLIES) if(state.is(jelly)) return true;
        return false;
    }
    public static boolean isValidRing(ServerLevel world,BlockPos center) {
        if(!world.getWorldBorder().isWithinBounds(center.offset(-2,0,-2)) || !world.getWorldBorder().isWithinBounds(center.offset(2,0,2))) return false;
        for(int x=-2;x<=2;x+=4)for(int z=-2;z<=2;z+=4)if(!world.hasChunkAt(center.offset(x,0,z)))return false;
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var state=world.getBlockState(center.offset(x,0,z));
            if(isFrameOffset(x,z)) { if(!isFrameMaterial(state)&&!localMaterial(world,state)) return false; }
            else if(Math.abs(x)<=1 && Math.abs(z)<=1 && (!state.canBeReplaced() || !state.getFluidState().isEmpty())) return false;
        }
        return true;
    }
    public static boolean tryActivate(ServerLevel world,ItemEntity offering) {
        if(offering.isRemoved() || offering.getItem().isEmpty() || !isOffering(offering.getItem())) return false;
        PortalKind kind=ritualKind(world,offering.getItem());
        if(kind==null||world.getServer().getLevel(PortalTravel.destination(world.dimension(),kind))==null)return false;
        // React over the entire frame, without waiting for the thrown item to drift into the inner 3x3.
        for(int dy=0;dy>=-4;dy--) for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            BlockPos center=offering.blockPosition().offset(dx,dy,dz);
            if(offering.getY()<center.getY()-.1 || offering.getY()>center.getY()+4 || !isValidRing(world,center)) continue;
            fillPortal(world,center,kind);
            offering.getItem().shrink(1);
            if(offering.getItem().isEmpty()) offering.discard();
            world.sendParticles(kind==PortalKind.GGG?ParticleTypes.END_ROD:ParticleTypes.SOUL_FIRE_FLAME,center.getX()+.5,center.getY()+.3,center.getZ()+.5,65,1.3,.4,1.3,.03);
            world.playSound(null,center,SoundEvents.END_PORTAL_SPAWN,SoundSource.BLOCKS,.65f,kind==PortalKind.GGG?1.1f:.7f);
            return true;
        }
        return false;
    }
    public static void fillPortal(ServerLevel world,BlockPos center) { fillPortal(world,center,PortalKind.GGG); }
    public static void fillPortal(ServerLevel world,BlockPos center,PortalKind kind) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            if(isFrameOffset(x,z)) world.setBlock(center.offset(x,0,z),GoogologyBlocks.PORTAL_FRAME.defaultBlockState().setValue(dev.googology.block.GoogologyPortalFrameBlock.FRUIT,kind==PortalKind.GUOGAO),Block.UPDATE_ALL);
            else if(Math.abs(x)<=1 && Math.abs(z)<=1) world.setBlock(center.offset(x,0,z),kind.block().defaultBlockState(),Block.UPDATE_ALL);
        }
        PortalState.get(world.getServer()).addGate(new PortalState.Gate(world.dimension().identifier().toString(),center.immutable(),kind));
        var appearance=new PortalActivationPayload(world.dimension().identifier().toString(),center.immutable(),kind.ordinal());
        for(var player:world.players())if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(center))<=64*64&&net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,PortalActivationPayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,appearance);

    }
    public static boolean complete(ServerLevel world,PortalState.Gate gate) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var state=world.getBlockState(gate.center().offset(x,0,z));
            if(isFrameOffset(x,z) && !state.is(GoogologyBlocks.PORTAL_FRAME)) return false;
            if(Math.abs(x)<=1 && Math.abs(z)<=1 && !state.is(gate.kind().block())) return false;
        }
        return true;
    }
    public static PortalState.Gate gateAt(ServerLevel world,BlockPos position) {
        var data=PortalState.get(world.getServer());
        var gate=data.gateAt(world.dimension().identifier().toString(),position);
        if(gate!=null) return gate;
        // Upgrade complete pre-1.2 portals on first contact, without resetting the save.
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            var center=position.offset(x,0,z);var state=world.getBlockState(center);
            if(!PortalKind.isPortal(state)) continue;
            var candidate=new PortalState.Gate(world.dimension().identifier().toString(),center,PortalKind.of(state));
            if(complete(world,candidate)) { data.addGate(candidate);return candidate; }
        }
        return null;
    }
    public static void collapseAt(ServerLevel world,BlockPos position) {
        var data=PortalState.get(world.getServer());
        var gate=data.gateAt(world.dimension().identifier().toString(),position);
        if(gate==null && COLLAPSING.isEmpty()) {
            // A pre-1.2 gate may be broken before anybody has stepped into it.
            search: for(int cx=-2;cx<=2;cx++) for(int cz=-2;cz<=2;cz++) {
                var center=position.offset(cx,0,cz);PortalKind kind=null;
                for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                    var state=world.getBlockState(center.offset(x,0,z));if(PortalKind.isPortal(state)) kind=PortalKind.of(state);
                }
                if(kind==null) continue;
                boolean complete=true;
                for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                    var p=center.offset(x,0,z);if(p.equals(position)) continue;
                    var state=world.getBlockState(p);
                    if(isFrameOffset(x,z) && !state.is(GoogologyBlocks.PORTAL_FRAME)) complete=false;
                    if(Math.abs(x)<=1 && Math.abs(z)<=1 && !state.is(kind.block())) complete=false;
                }
                if(complete) { gate=new PortalState.Gate(world.dimension().identifier().toString(),center,kind);break search; }
            }
        }
        if(gate==null || !COLLAPSING.add(gate)) return;
        try {
            data.removeGate(gate);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                if(!isFrameOffset(x,z) && !(Math.abs(x)<=1 && Math.abs(z)<=1)) continue;
                var pos=gate.center().offset(x,0,z);var state=world.getBlockState(pos);
                if(state.is(GoogologyBlocks.PORTAL_FRAME)||PortalKind.isPortal(state)) world.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
        } finally { COLLAPSING.remove(gate); }
    }
}
