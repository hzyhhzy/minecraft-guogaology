package dev.googology.outer.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.HitResult;
import net.minecraft.stats.Stats;

/** Vanilla boat placement, with the registered outer-world hull type. */
public final class OuterBoatItem extends Item {
    private final EntityType<Boat> type;
    public OuterBoatItem(EntityType<Boat> type,Properties p){super(p);this.type=type;}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        var hit=getPlayerPOVHitResult(world,player,ClipContext.Fluid.ANY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var eye=player.getEyePosition();
        var sweep=player.getBoundingBox().expandTowards(player.getViewVector(1).scale(5)).inflate(1);
        for(var entity:world.getEntities(player,sweep,e->!e.isSpectator()&&e.isPickable())){
            if(entity.getBoundingBox().inflate(entity.getPickRadius()).contains(eye))return InteractionResultHolder.pass(stack);
        }
        var boat=type.create(world);if(boat==null)return InteractionResultHolder.fail(stack);
        var p=hit.getLocation();boat.moveTo(p.x,p.y,p.z,player.getYRot(),0);
        if(!world.noCollision(boat,boat.getBoundingBox().deflate(.1)))return InteractionResultHolder.fail(stack);
        if(!world.isClientSide){
            world.addFreshEntity(boat);world.gameEvent(player,GameEvent.ENTITY_PLACE,p);
            stack.consume(1,player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
}
