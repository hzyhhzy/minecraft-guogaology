package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.minecraft.client.*;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Real keyboard decoding, consecutive networked ticks and coordinate displacement; never forces sprint on. */
final class ManuscriptFlight044Checks {
    private enum Axis {
        FORWARD,RIGHT,UP,DOWN;
        double along(Vec3 v){return switch(this){case FORWARD->v.z;case RIGHT->-v.x;case UP->v.y;case DOWN->-v.y;};}
    }
    private record Scenario(int grade,int slider,Axis axis,boolean sprint){}
    private static final Vec3 ORIGIN=new Vec3(512.5,500,512.5);
    private final List<Scenario> scenarios=new ArrayList<>();
    private final Map<Scenario,Double> distances=new HashMap<>();
    private final boolean deep;
    private int scene,stage,ticks,checks;
    private CompletableFuture<Void> pending;
    private Vec3 start,last;
    private KeyMapping[] keys;
    private boolean[] savedKeys;
    private net.minecraft.client.player.ClientInput savedInput;
    private Object settings;
    private Method setMultiplier;
    private double savedMultiplier;

    ManuscriptFlight044Checks(boolean deep){
        this.deep=deep;
        for(int grade:new int[]{2,3})for(int slider:new int[]{1,31})for(var axis:Axis.values())for(boolean sprint:new boolean[]{false,true})scenarios.add(new Scenario(grade,slider,axis,sprint));
    }
    int checks(){return checks;}
    private void check(boolean ok,String label){if(!ok)throw new AssertionError("FLIGHT044 "+label);checks++;}
    private void near(double actual,double expected,String label){check(Math.abs(actual-expected)<Math.max(.0001,Math.abs(expected)*.025),label+": "+actual+" != "+expected);}
    private static ItemStack book(int grade){var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(GoogologyMod.id("true_omega_manuscript")));GearData.setCores(stack,List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(GoogologyMod.id("boundary_core_lv"+grade)))));return stack;}
    private CompletableFuture<Void> server(Minecraft c,Consumer<ServerPlayer> action){var id=c.player.getUUID();return c.getSingleplayerServer().submit(()->action.accept(c.getSingleplayerServer().getPlayerList().getPlayer(id)));}
    private void releaseKeys(){for(var key:keys)key.setDown(false);}
    private void press(Scenario s){releaseKeys();keys[switch(s.axis()){case FORWARD->0;case RIGHT->3;case UP->4;case DOWN->5;}].setDown(true);keys[6].setDown(s.sprint());}
    private void initialize(Minecraft c)throws Exception{
        var o=c.options;keys=new KeyMapping[]{o.keyUp,o.keyDown,o.keyLeft,o.keyRight,o.keyJump,o.keyShift,o.keySprint};savedKeys=new boolean[keys.length];for(int i=0;i<keys.length;i++)savedKeys[i]=keys[i].isDown();
        savedInput=c.player.input;c.player.input=new KeyboardInput(o);
        var flight=Class.forName("dev.creativeflight.CreativeFlight");settings=flight.getMethod("settings").invoke(null);setMultiplier=settings.getClass().getMethod("setMultiplier",double.class);savedMultiplier=(Double)settings.getClass().getMethod("multiplier").invoke(settings);
        check(ManuscriptEffects.deep(c.level)==deep,"actual realm matches movement cases");
    }
    boolean tick(Minecraft c)throws Exception{
        if(keys==null)initialize(c);
        if(stage>=100)return lifecycle(c);
        var s=scenarios.get(scene);
        if(stage==0){
            releaseKeys();setMultiplier.invoke(settings,(double)s.slider());
            pending=server(c,p->{
                p.closeContainer();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(p);p.setGameMode(GameType.SURVIVAL);p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.setSprinting(false);
                p.teleport(new TeleportTransition(p.level(),ORIGIN,Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setItemSlot(EquipmentSlot.OFFHAND,book(s.grade()));ManuscriptEffects.tick(p);p.getAbilities().flying=true;p.onUpdateAbilities();p.inventoryMenu.broadcastChanges();
            });stage=1;ticks=0;return false;
        }
        if(stage==1){
            if(!pending.isDone())return false;pending.join();if(++ticks<12)return false;
            check(ManuscriptEffects.ownsFlight(c.player)&&c.player.getAbilities().flying,"owned flight synchronized "+s);
            check(ManuscriptEffects.level(ManuscriptEffects.held(c.player),6)==s.grade(),"active manuscript synchronized "+s);
            c.player.setSprinting(false);c.player.setDeltaMovement(Vec3.ZERO);press(s);stage=2;ticks=0;return false;
        }
        if(stage==2){
            ++ticks;
            check(c.player.getAbilities().flying,"flight persists through native ticks "+s);
            if(ticks==12){start=c.player.position();last=start;}
            if(ticks>12){
                check(s.axis().along(c.player.position().subtract(last))>=-.02,"no backwards correction during continuous motion "+s);last=c.player.position();
                check(ManuscriptEffects.flightSprint(c.player)==(s.grade()==3&&s.sprint()),"dedicated intent follows key, including pure vertical/strafe "+s);
            }
            if(ticks<36)return false;
            var end=c.player.position();double distance=s.axis().along(end.subtract(start));check(distance>.05,"actual coordinates advance "+s);distances.put(s,distance);
            pending=server(c,p->{
                check(ManuscriptEffects.flightSprint(p)==(s.grade()==3&&s.sprint()),"C2S intent reached server "+s);
                check(ManuscriptEffects.ownsFlight(p)&&p.getAbilities().flying,"server keeps owned flight "+s);
                check(p.position().distanceTo(end)<12,"server follows client displacement without rubber-banding "+s+" server="+p.position()+" client="+end);
                check(s.axis().along(p.position().subtract(ORIGIN))>distance*.75,"server accepted substantial actual movement "+s);
                System.out.println("FLIGHT044_MOVE realm="+p.level().dimension()+" "+s+" measuredTicks=24 distance="+distance+" server="+p.position()+" client="+end);
            });releaseKeys();stage=3;return false;
        }
        if(!pending.isDone())return false;pending.join();
        if(++scene<scenarios.size()){stage=0;return false;}
        for(int grade:new int[]{2,3})for(var axis:Axis.values()){
            for(int slider:new int[]{1,31}){
                double normal=distances.get(new Scenario(grade,slider,axis,false)),sprint=distances.get(new Scenario(grade,slider,axis,true));
                near(sprint/normal,grade==2?1:deep?8:2,"coordinate sprint multiplier grade="+grade+" slider="+slider+" axis="+axis);
            }
            for(boolean sprint:new boolean[]{false,true})near(distances.get(new Scenario(grade,31,axis,sprint))/distances.get(new Scenario(grade,1,axis,sprint)),1,"creative slider does not alter manuscript movement grade="+grade+" axis="+axis+" sprint="+sprint);
        }
        for(var axis:Axis.values())near(distances.get(new Scenario(2,1,axis,false))/distances.get(new Scenario(3,1,axis,false)),deep?1.0/3:1.0/6,"Lv2 coordinate speed relative to independent Lv3 base "+axis);
        releaseKeys();keys[6].setDown(true);stage=100;ticks=0;return false;
    }
    private boolean lifecycle(Minecraft c)throws Exception{
        if(++ticks<10)return false;
        var p=c.player;
        if(stage==100){
            check(ManuscriptEffects.flightSprint(p),"held sprint key accelerates even without forward input");
            p.getAbilities().flying=false;p.onUpdateAbilities();stage=101;ticks=0;return false;
        }
        if(stage==101){
            check(!ManuscriptEffects.flightSprint(p),"stopping flight clears client intent");pending=server(c,sp->check(!ManuscriptEffects.flightSprint(sp),"stopping flight clears server intent"));stage=102;return false;
        }
        if(stage==102){
            if(!pending.isDone())return false;pending.join();p.getAbilities().flying=true;p.onUpdateAbilities();stage=103;ticks=0;return false;
        }
        if(stage==103){
            check(ManuscriptEffects.flightSprint(p),"unchanged held key resumes dedicated intent after flight restarts");
            pending=server(c,sp->{check(ManuscriptEffects.flightSprint(sp),"restarted flight re-sends intent");sp.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(sp);sp.inventoryMenu.broadcastChanges();});stage=104;ticks=0;return false;
        }
        if(stage==104){
            if(!pending.isDone())return false;pending.join();check(!ManuscriptEffects.ownsFlight(p)&&!ManuscriptEffects.flightSprint(p)&&!p.getAbilities().flying,"removing manuscript clears client flight and intent");
            pending=server(c,sp->{ManuscriptEffects.setFlightSprint(sp,true);check(!ManuscriptEffects.flightSprint(sp)&&!sp.getAbilities().mayfly,"rejected sprint request cannot grant flight");sp.setGameMode(GameType.CREATIVE);sp.setItemSlot(EquipmentSlot.OFFHAND,book(3));ManuscriptEffects.tick(sp);sp.getAbilities().flying=true;sp.onUpdateAbilities();sp.inventoryMenu.broadcastChanges();});stage=105;ticks=0;return false;
        }
        if(stage==105){
            if(!pending.isDone())return false;pending.join();check(p.isCreative()&&!ManuscriptEffects.ownsFlight(p)&&!ManuscriptEffects.flightSprint(p),"creative mode never inherits manuscript ownership or intent");
            p.setSprinting(false);var speed=Player.class.getDeclaredMethod("getFlyingSpeed");speed.setAccessible(true);near((Float)speed.invoke(p),p.getAbilities().getFlyingSpeed()*31,"independent creative 31x slider resumes after manuscript flight");
            pending=server(c,sp->{check(!ManuscriptEffects.flightSprint(sp),"creative clears server dedicated intent");ManuscriptEffects.setFlightSprint(sp,true);check(!ManuscriptEffects.flightSprint(sp),"creative rejects manuscript sprint packet");});stage=106;return false;
        }
        if(!pending.isDone())return false;pending.join();releaseKeys();for(int i=0;i<keys.length;i++)keys[i].setDown(savedKeys[i]);p.input=savedInput;setMultiplier.invoke(settings,savedMultiplier);
        System.out.println("FLIGHT044_OK deep="+deep+" checks="+checks+" 32 sustained keyboard/network scenes; coordinate speeds and lifecycle");return true;
    }
}
