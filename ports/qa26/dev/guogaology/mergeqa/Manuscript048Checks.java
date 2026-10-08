package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.CoreGrades;
import dev.guogaology.mining.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;

/** Isolated real-client flight, real C2S inventory button and lethal-damage conservation. */
public final class Manuscript048Checks {
    private int phase,ticks,checks;private boolean opening,queued,done;private volatile boolean ready;private volatile Throwable failure;
    private ManuscriptFlight044Checks flightChecks;
    private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var test=new Manuscript048Checks();ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private void near(double value,double expected,String label){check(Math.abs(value-expected)<.00001,label+": "+value+" != "+expected);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(id)));}
    private static ItemStack book(String... names){var s=item("true_omega_manuscript");GearData.setCores(s,Arrays.stream(names).map(Manuscript048Checks::item).toList());return s;}
    private static void empty(Player p){for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(System.nanoTime()>deadline)throw new AssertionError("focused manuscript timeout phase "+phase);
            if(failure!=null)throw new RuntimeException(failure);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){queued=true;ready=false;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}});return;}
            if(!ready||++ticks<45)return;
            if(phase==0){
                if(ticks==45)for(String root:CoreGrades.ROOTS)for(var block:CoreGrades.levels(root)){
                    var sprite=c.getModelManager().getBlockStateModelSet().get(block.defaultBlockState()).particleMaterial().sprite();
                    check(!sprite.contents().name().getPath().contains("missing"),"real particle material "+root);
                }
                if(ticks==45){c.gui.setScreen(new InventoryScreen(c.player));return;}
                if(ticks<55)return; // Capture the rendered inventory, not the preceding frame.
                var button=button(c);check(button!=null&&button.active,"inventory has enabled manuscript button");
                photo(c,"inventory-button");button.onPress(new KeyEvent(257,0,0));phase++;ticks=0;return;
            }
            if(phase==1){
                check(c.player.containerMenu instanceof ManuscriptMenu,"actual button C2S opens bound menu");
                check(c.player.getOffhandItem().getItem() instanceof DenxiManuscript,"original stays in offhand on client");photo(c,"manuscript-preview");c.player.closeContainer();
            }else if(phase>=2&&phase<=4){
                if(flightChecks==null)flightChecks=new ManuscriptFlight044Checks(phase!=2);
                if(!flightChecks.tick(c))return;
                checks+=flightChecks.checks();flightChecks=null;
            }else if(phase==5){
                c.gui.setScreen(new InventoryScreen(c.player));check(button(c)!=null,"button exists after another inventory open");
                done=true;Files.writeString(Path.of("port-client-ok.txt"),"MANUSCRIPT048_OK checks="+checks+" inventory button / deferred activation / death conservation / 96 sustained native keyboard flight scenes / C2S intent and coordinate speeds / 28 particle materials\n");c.stop();return;
            }
            phase++;ticks=0;queued=false;
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static Button button(Minecraft c){return Screens.getWidgets(c.gui.screen()).stream().filter(w->w instanceof Button&&w.getMessage().equals(net.minecraft.network.chat.Component.translatable("mining.guogaology.manuscript.button"))).map(w->(Button)w).findFirst().orElse(null);}
    private void photo(Minecraft c,String name)throws Exception{
        var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name+".png"));}catch(Exception e){failure=e;}});
    }
    private void setup(ServerPlayer p)throws Exception{
        if(phase==0){
            InnerLanding043Checks.run(p);ManuscriptMining043Checks.run(p);
            Manuscript047Checks.run(p);deaths(p);
            p.closeContainer();empty(p);p.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.OFFHAND,book("power_tower_core_lv3","boundary_core_lv3"));
            p.getInventory().setItem(9,item("laver_core_lv3"));p.getInventory().setItem(10,item("guogao_heart_lv3"));
            p.teleport(new TeleportTransition(p.level().getServer().overworld(),new Vec3(512.5,500,512.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();ManuscriptEffects.tick(p);
        }else if(phase>=2&&phase<=4){
            p.closeContainer();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(p);p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.setGameMode(GameType.SURVIVAL);
            var level=phase==2?p.level().getServer().overworld():p.level().getServer().getLevel(phase==3?GuogaologyMod.DIMENSION:GuogaologyMod.GUOGAO);
            p.teleport(new TeleportTransition(level,new Vec3(512.5,500,512.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));ManuscriptEffects.tick(p);p.inventoryMenu.broadcastChanges();
        }else if(phase==5){p.setGameMode(GameType.SURVIVAL);}
    }
    private void tally(Map<String,Integer> map,ItemStack s){
        if(s.isEmpty())return;String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();map.merge(id,s.getCount(),Integer::sum);
        for(var core:GearData.cores(s))tally(map,core);
    }
    private Map<String,Integer> inventory(Player p){var counts=new TreeMap<String,Integer>();for(int i=0;i<p.getInventory().getContainerSize();i++)tally(counts,p.getInventory().getItem(i));tally(counts,p.containerMenu.getCarried());return counts;}
    private void deaths(ServerPlayer real)throws Exception{
        // Death drops must be in the real client's entity-loaded chunk.
        var level=real.level();var rules=level.getGameRules();boolean keep=rules.get(GameRules.KEEP_INVENTORY);var area=real.getBoundingBox().inflate(4);
        try{for(boolean keepInventory:new boolean[]{false,true})for(boolean offhand:new boolean[]{false,true}){
            rules.set(GameRules.KEEP_INVENTORY,keepInventory,level.getServer());
            var profile=new GameProfile(UUID.randomUUID(),"SocketDeathQA");var p=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
            p.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
            // A real client sends this after loading. Without it vanilla makes
            // the synthetic connection invulnerable, so lethal damage is ignored.
            p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            p.setGameMode(GameType.SURVIVAL);p.setPos(real.position());p.getInventory().setSelectedSlot(2);int slot=offhand?40:2;
            var original=book("sequence_core","sequence_core");p.getInventory().setItem(slot,original);p.getInventory().setItem(9,new ItemStack(item("laver_core").getItem(),4));var expected=inventory(p);
            var menu=new ManuscriptMenu(72,p.getInventory(),level,slot,original);p.containerMenu=menu;
            menu.quickMoveStack(p,11);menu.clicked(1,0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==1,"death fixture holds removed core on cursor");
            check(ManuscriptEffects.level(ManuscriptEffects.held(p),4)==0,"newly installed Laver inactive before lethal damage");
            var before=new HashSet<UUID>();for(var drop:level.getEntitiesOfClass(ItemEntity.class,area))before.add(drop.getUUID());
            p.hurtServer(level,p.damageSources().generic(),10000);check(!p.isAlive(),"actual lethal damage reaches death while editing");p.closeContainer();
            var actual=inventory(p);var spawned=new ArrayList<ItemEntity>();for(var drop:level.getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(drop.getUUID())){tally(actual,drop.getItem());spawned.add(drop);}
            check(actual.equals(expected),"death preserves exact item multiset keep="+keepInventory+" offhand="+offhand+" expected="+expected+" actual="+actual);
            var previous=new TreeMap<>(actual);menu.removed(p);actual=inventory(p);for(var drop:spawned)tally(actual,drop.getItem());check(actual.equals(previous),"duplicate close callback cannot duplicate items");
            for(var drop:spawned)drop.discard();
        }}finally{rules.set(GameRules.KEEP_INVENTORY,keep,level.getServer());}
    }
}
