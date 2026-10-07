package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.CreativeCatalog;
import dev.guogaology.mining.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.*;
import net.minecraft.world.level.levelgen.placement.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;

/** Isolated fo262: actual item ownership, death paths, loaded ore providers and menu screenshots. */
public final class Patch051Checks {
    private static final List<String> TAB_ICONS=List.of("great_omega_bloom","server_rack","ordinal_bricks","guogao_lantern","ordinal_crystal","true_omega_pickaxe","true_omega_material");
    private int checks,phase,ticks;private boolean opening,queued,done,iconCapture;private volatile boolean ready,iconPhotographed;private volatile Throwable failure;
    private final long deadline=System.nanoTime()+360_000_000_000L;
    public static void initialize(){var t=new Patch051Checks();if(Boolean.getBoolean("guogaology.qa.creativeicons047"))t.phase=15;ClientTickEvents.END_CLIENT_TICK.register(t::tick);}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"guogaology:"+id)));}
    private static ItemStack gear(String id,String... contents){var s=item(id);GearData.setCores(s,Arrays.stream(contents).map(Patch051Checks::item).toList());return s;}
    private static ItemStack book(String... contents){return gear("true_omega_manuscript",contents);}
    private static void empty(ServerPlayer p){p.closeContainer();for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("patch QA timeout "+phase);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){
                if(phase==13){c.getLanguageManager().setSelected("en_us");c.getLanguageManager().onResourceManagerReload(c.getResourceManager());}
                queued=true;ready=false;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}});return;
            }
            if(!ready||++ticks<45)return;
            if(phase==15){
                if(iconCapture){
                    if(!iconPhotographed)return;
                    done=true;Files.writeString(Path.of("port-client-ok.txt"),Boolean.getBoolean("guogaology.qa.creativeicons047")
                            ?"CREATIVE_ICONS047_OK checks="+checks+" seven existing category items / synchronized appearances / loaded item models / native screenshot\n"
                            :"PATCH051_OK checks="+checks+" ore profiles / wear / native labels / socket totems / actual deaths / non-downward mining / 15 live bilingual dimming / dimension-aware contribution scenes and real quick-move / 7 actual creative tab icons\n");c.stop();return;
                }
                check(c.player.containerMenu instanceof ChestMenu,"synchronized creative icon overview");
                var missing=c.getModelManager().getItemModel(GuogaologyMod.id("qa_missing_icon_044"));
                for(int i=0;i<CreativeCatalog.TABS.size();i++){
                    var id=GuogaologyMod.id(TAB_ICONS.get(i));var stack=c.player.containerMenu.getSlot(10+i).getItem();
                    check(stack.getCount()==1&&id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())),"actual tab icon synchronized "+id);
                    var model=stack.get(DataComponents.ITEM_MODEL);check(id.equals(model)&&c.getModelManager().getItemModel(model)!=missing,"loaded non-missing item model "+id);
                }
                var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);iconCapture=true;
                Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("patch-15-creative-icons.png"));iconPhotographed=true;}catch(Exception e){failure=e;}});return;
            }
            check(c.player.containerMenu instanceof EnhancementMenu,"real synchronized enhancement screen");
            var m=(EnhancementMenu)c.player.containerMenu;
            if(phase==0){check(m.manuscript(),"manuscript menu");check(GearData.socketTotems(m.gear())==1,"totem slot synchronized");check(EquipmentRules.wearFactor(6,GearData.profile(m.gear()))==1,"no manuscript wear display");check(m.canInsertCore(new ItemStack(Items.TOTEM_OF_UNDYING)),"totem can be highlighted in manuscript inventory");}
            else if(phase<=5){check(!m.canInsertCore(new ItemStack(Items.TOTEM_OF_UNDYING)),"equipment rejects socket totems");}
            check(!m.canSelectInventoryItem(item("minecraft:stone"))&&!m.canSelectInventoryItem(item("minecraft:apple")),"unrelated inventory items are dimmed");
            if(phase==4){check(m.gear().isEmpty()&&m.canSelectInventoryItem(item("true_omega_pickaxe"))&&m.canSelectInventoryItem(item("true_omega_bow")),"empty station highlights equipment");check(!m.canSelectInventoryItem(item("hydra_bud")),"empty station dims cores until equipment is inserted");}
            else if(phase==5){check(!m.gear().isEmpty()&&!m.canSelectInventoryItem(item("true_omega_bow"))&&m.canSelectInventoryItem(item("hydra_bud")),"real quick-move switches emphasis from equipment to valid cores");}
            else if(phase==6){check(m.manuscript()&&m.canSelectInventoryItem(item("sequence_core"))&&m.canSelectInventoryItem(item("sequence_core_lv3")),"omega manuscript highlights all three compatible core grades");check(m.canSelectInventoryItem(item("minecraft:totem_of_undying"))&&!m.canSelectInventoryItem(item("astra_critical_core")),"manuscript highlights totems but dims incompatible criticality");}
            else if(phase==7){check(m.installed()==m.capacity()&&!m.canSelectInventoryItem(item("sequence_core"))&&!m.canSelectInventoryItem(item("minecraft:totem_of_undying")),"full sockets dim otherwise compatible items");}
            if(phase==8||phase==9){
                check(m.deep()==(phase==9),"actual dimension controls preview mode");
                var terms=EquipmentRules.attackPreviewTerms(new EquipmentRules.Gear(4,6,0,GearData.profile(m.gear())),m.deep());
                check(terms.size()==3&&Math.abs(terms.get(0)-(m.deep()?1.2:2))<1e-9,"attack calculation preserves source terms before same-bucket display aggregation");
                check(m.preview().mainhand().tier()<0&&m.preview().armor().stream().allMatch(g->g.tier()<0),"preview excludes incidental gear");
            }
            if(phase==10||phase==11){check(m.deep()==(phase==11),"weapon preview actual realm");check(m.preview().book().isEmpty(),"table preview does not borrow manuscript bonuses");}
            if(phase==12)check(Math.abs(m.preview().effects().protectionFactor()-1.4)<1e-9,"helmet protection uses weighted contribution");
            if(phase==13){boolean ground=c.player.onGround();c.player.setOnGround(true);float speed=c.player.getDestroySpeed(Blocks.STONE.defaultBlockState());check(Math.abs(speed-40.8f)<.001,"synchronized client bare-hand mining includes innate and Sequence flat+rate: speed="+speed+" actual="+GearData.profile(c.player.getOffhandItem())+" active="+GearData.profile(ManuscriptEffects.held(c.player)));c.player.setOnGround(ground);}
            var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int frame=phase;
            Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("patch-"+frame+".png"));}catch(Exception e){failure=e;}});
            if(phase==4){c.gameMode.handleContainerInput(m.containerId,EnhancementMenu.INVENTORY,0,ContainerInput.QUICK_MOVE,c.player);phase++;ticks=0;return;}
            phase++;
            queued=false;ticks=0;
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void setup(ServerPlayer p)throws Exception{
        if(phase==0){ores(p);totems(p);mining(p);wear(p);empty(p);CoreHarvest043Checks.run(p);ManuscriptMining043Checks.run(p);Recipe043Checks.run(p);Core047Checks.sanctuaryLoot(p);InnerLanding043Checks.run(p);Lamp043Checks.run(p);VariantNames043Checks.run(p);MenuInventory043Checks.run(p);LandmarkChest043Checks.run(p);Tier044Checks.run(p);}
        empty(p);
        if(phase==15){creativeIcons(p);return;}
        if(phase>=8){
            var world=p.level().getServer().getLevel(phase==9||phase==11?GuogaologyMod.DIMENSION:Level.OVERWORLD);
            p.teleport(new net.minecraft.world.level.portal.TeleportTransition(world,new net.minecraft.world.phys.Vec3(0,250,0),net.minecraft.world.phys.Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        }
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
        var inventory=List.of("true_omega_pickaxe","true_omega_sword","true_omega_bow","true_omega_manuscript","sequence_core","sequence_core_lv3","hydra_bud","lho_trace_lv3","astra_critical_core","boundary_core","laver_core","minecraft:totem_of_undying","minecraft:stone","minecraft:apple");
        for(int i=0;i<inventory.size();i++){var stack=item(inventory.get(i));check(!stack.isEmpty(),"visible inventory fixture "+inventory.get(i));p.getInventory().setItem(9+i,stack);}
        if(phase==0||phase>=6&&phase<=9||phase>=13){
            var manuscript=phase==13?book("sequence_core_lv3","hydra_bud_lv3","lho_trace_lv3","laver_core_lv3","boundary_core_lv3","guogao_heart_lv3"):phase>=8?book("power_tower_core_lv3","power_tower_core_lv3","ordinal_crystal_lv4"):phase==0?book("lho_trace_lv3","minecraft:totem_of_undying"):phase==6?gear("omega_manuscript"):book("lho_trace_lv3","lho_trace_lv3","lho_trace_lv3","lho_trace_lv3","lho_trace_lv3","lho_trace_lv3");
            p.setItemSlot(EquipmentSlot.OFFHAND,manuscript);p.inventoryMenu.broadcastFullState();check(DenxiManuscript.open(p,InteractionHand.OFF_HAND),"open manuscript at chosen capacity/grade");
        }
        else{
            var pos=p.blockPosition();p.level().setBlock(pos,MiningContent.TABLES[2].defaultBlockState(),2);
            p.openMenu(new SimpleMenuProvider((id,inv,player)->new EnhancementMenu(id,inv,p.level(),pos,3),Component.literal("Patch QA")));
            if(phase!=4){
                var s=phase==12?gear("true_omega_helmet","boundary_core_lv3"):phase>=10?gear("true_omega_sword","power_tower_core_lv3","power_tower_core_lv3"):gear(phase==1?"true_omega_pickaxe":phase==2?"true_omega_sword":"true_omega_bow","hydra_bud_lv3");
                p.containerMenu.getSlot(0).set(s);
            }
        }
        p.containerMenu.broadcastChanges();
    }
    private void creativeIcons(ServerPlayer p){
        check(CreativeCatalog.TABS.size()==7,"seven creative categories");
        var icons=new SimpleContainer(27);var ids=new HashSet<Identifier>();
        for(int i=0;i<CreativeCatalog.TABS.size();i++){
            var name=CreativeCatalog.TABS.get(i);var tab=BuiltInRegistries.CREATIVE_MODE_TAB.getValue(GuogaologyMod.id(name));
            check(tab!=null,"registered creative category "+name);var stack=tab.getIconItem().copy();var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
            check(!stack.isEmpty()&&id.equals(GuogaologyMod.id(TAB_ICONS.get(i))),"category uses its representative item "+name);
            check(ids.add(id),"distinct creative category icon "+name);
            check(CreativeCatalog.entries(name).stream().anyMatch(e->ItemStack.isSameItemSameComponents(e,stack)),"icon is an obtainable item in its own category "+name);
            icons.setItem(10+i,stack);
        }
        p.openMenu(new SimpleMenuProvider((id,inventory,who)->ChestMenu.threeRows(id,inventory,icons),Component.literal("Creative tab icons")));p.containerMenu.broadcastChanges();
    }
    private void ores(ServerPlayer p)throws Exception{
        var world=p.level().getServer().getLevel(GuogaologyMod.OUTER);
        var registry=world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var context=new WorldGenerationContext(world.getChunkSource().getGenerator(),world);
        int i=0;double gammaMean=0,omegaMean=0;
        for(String name:List.of("omega","epsilon","gamma","true_omega")){
            for(String suffix:List.of("","_hell")){
                var placed=registry.getValue(GuogaologyMod.id("ore_"+name+suffix));check(placed!=null,"loaded placed ore "+name+suffix);
                var config=(OreConfiguration)placed.feature().value().config();check(config.discardChanceOnAirExposure==.5f,"50% air discard "+name+suffix);check(config.size==new int[]{9,7,4,3}[i],"vein size unchanged");
                var count=placed.placement().stream().filter(v->v instanceof CountPlacement).findFirst().orElseThrow();var cf=CountPlacement.class.getDeclaredField("count");cf.setAccessible(true);check(((IntProvider)cf.get(count)).sample(RandomSource.create(51))==new int[]{24,12,6,2}[i],"doubled attempt count "+name+suffix);
                var height=placed.placement().stream().filter(v->v instanceof HeightRangePlacement).findFirst().orElseThrow();var hf=HeightRangePlacement.class.getDeclaredField("height");hf.setAccessible(true);var provider=(HeightProvider)hf.get(height);
                if(i<2){check(provider instanceof TrapezoidHeight,"lower ores retain their height profile");continue;}
                check(provider instanceof BiasedToBottomHeight,"bottom biased "+name);int n=200000,low=0,high=0,y50=0,min=999,max=-999;long sum=0;var random=RandomSource.create(5100+i);
                for(int k=0;k<n;k++){int y=provider.sample(random,context);sum+=y;min=Math.min(min,y);max=Math.max(max,y);if(y>=-60&&y<=-41)low++;if(y>=-20&&y<=-1)high++;if(y>=50)y50++;}
                check(min>=-64&&max<(i==2?56:16),"every height attempt is within the valid range");check(low>high*2,"bottom band clearly denser than upper band "+name);check(y50<n*.003,"almost no candidates at/above50 "+name);
                double mean=sum/(double)n;if(i==2)gammaMean=mean;else omegaMean=mean;
                System.out.println("ORE051 "+name+suffix+" n="+n+" mean="+mean+" y50="+y50+" bottom20="+low+" upper20="+high+" range="+min+".."+max);
            }i++;
        }check(gammaMean>omegaMean+8,"Gamma is higher on average than Omega");
    }
    private ServerPlayer dummy(ServerPlayer real){
        var profile=new GameProfile(UUID.randomUUID(),"Totem051");var p=new ServerPlayer(real.level().getServer(),real.level(),profile,ClientInformation.createDefault());
        p.connection=new ServerGamePacketListenerImpl(real.level().getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);p.setPos(real.position());return p;
    }
    private void lethal(ServerPlayer p){p.removeAllEffects();p.setAbsorptionAmount(0);p.invulnerableTime=0;p.setHealth(20);check(p.hurtServer(p.level(),p.damageSources().generic(),10000),"real lethal hit accepted");}
    private int count(ItemStack s){int n=s.is(Items.TOTEM_OF_UNDYING)?s.getCount():0;for(var v:GearData.coreSlots(s))n+=count(v);return n;}
    private int count(ServerPlayer p){int n=count(p.containerMenu.getCarried());for(int i=0;i<p.getInventory().getContainerSize();i++)n+=count(p.getInventory().getItem(i));return n;}
    private void totems(ServerPlayer real)throws Exception{
        var rules=real.level().getGameRules();boolean keep=rules.get(GameRules.KEEP_INVENTORY);rules.set(GameRules.KEEP_INVENTORY,true,real.level().getServer());
        try{
            for(String tier:EquipmentRules.MINERALS){var b=gear(tier+"_manuscript");var t=new ItemStack(Items.TOTEM_OF_UNDYING);check(GearData.install(b,t,1)==null&&t.isEmpty()&&GearData.socketTotems(b)==1,"any manuscript tier accepts one whole totem");check(GearData.profile(b).isEmpty(),"totem is not a core effect");check(GearData.installationError(item(tier+"_pickaxe"),new ItemStack(Items.TOTEM_OF_UNDYING),3)!=null,"pick rejects totem");}
            var p=dummy(real);p.setItemSlot(EquipmentSlot.OFFHAND,book("minecraft:totem_of_undying"));lethal(p);check(p.isAlive()&&count(p)==0,"direct socket rescue consumes exactly one without Guogao");lethal(p);check(!p.isAlive(),"spent totem cannot rescue twice");
            for(int lv:new int[]{2,3}){p=dummy(real);var b=book("guogao_heart_lv"+lv,"minecraft:totem_of_undying","minecraft:totem_of_undying");p.setItemSlot(EquipmentSlot.OFFHAND,b);p.getInventory().setItem(9,new ItemStack(Items.TOTEM_OF_UNDYING));lethal(p);check(p.isAlive()&&count(p)==2&&GearData.socketTotems(b)==2,"Guogao prioritizes backpack "+lv);lethal(p);check(p.isAlive()&&count(p)==1&&GearData.socketTotems(b)==1,"falls back to stored totem "+lv);}
            p=dummy(real);p.setItemSlot(EquipmentSlot.MAINHAND,book("minecraft:totem_of_undying"));p.setItemSlot(EquipmentSlot.OFFHAND,book());lethal(p);check(!p.isAlive()&&count(p)==1,"empty offhand book overrides loaded mainhand book");
            for(int move:new int[]{0,1,2}){
                p=dummy(real);var b=book("minecraft:totem_of_undying","lho_trace_lv3");p.setItemSlot(EquipmentSlot.OFFHAND,b);var menu=new ManuscriptMenu(81,p.getInventory(),p.level(),40,b);p.containerMenu=menu;
                if(move==1)menu.clicked(1,0,ContainerInput.PICKUP,p);if(move==2)menu.quickMoveStack(p,1);
                lethal(p);check(p.isAlive()&&count(p)==0,"active totem debits actual slot/cursor/inventory "+move);check(GearData.socketTotems(ManuscriptEffects.held(p))==0,"activation snapshot debited too "+move);p.closeContainer();check(count(p)==0,"closing cannot restore a consumed totem "+move);
            }
            for(boolean keepInventory:new boolean[]{false,true}){
                rules.set(GameRules.KEEP_INVENTORY,keepInventory,real.level().getServer());p=dummy(real);var b=book();p.setItemSlot(EquipmentSlot.OFFHAND,b);p.getInventory().setItem(9,new ItemStack(Items.TOTEM_OF_UNDYING));var menu=new ManuscriptMenu(82,p.getInventory(),p.level(),40,b);p.containerMenu=menu;
                check(menu.canInsertCore(p.getInventory().getItem(9)),"totem accepted by real menu predicate");menu.quickMoveStack(p,11);check(GearData.socketTotems(b)==1&&GearData.socketTotems(ManuscriptEffects.held(p))==0,"new totem deferred until close");
                var before=new HashSet<UUID>();for(var e:real.level().getEntitiesOfClass(ItemEntity.class,real.getBoundingBox().inflate(5)))before.add(e.getUUID());
                lethal(p);check(!p.isAlive(),"new totem inactive during editing");p.closeContainer();int n=count(p);var drops=new ArrayList<ItemEntity>();for(var e:real.level().getEntitiesOfClass(ItemEntity.class,real.getBoundingBox().inflate(5)))if(!before.contains(e.getUUID())){n+=count(e.getItem());drops.add(e);}
                check(n==1,"death keeps exactly one pending totem keepInventory="+keepInventory);menu.removed(p);int again=count(p);for(var e:drops){again+=count(e.getItem());e.discard();}check(again==1,"repeat close cannot duplicate pending totem");
            }
        }finally{rules.set(GameRules.KEEP_INVENTORY,keep,real.level().getServer());}
    }
    private void wear(ServerPlayer p){
        empty(p);var b=book("lho_trace_lv3");p.setItemSlot(EquipmentSlot.OFFHAND,b);
        for(var s:List.of(item("true_omega_pickaxe"),item("true_omega_sword"),item("true_omega_bow"),new ItemStack(Items.DIAMOND_PICKAXE))){check(GearData.wearFactor(s,p)==1,"book Empty never protects other gear");check(GearData.wearCost(s,10,p,()->.5)==10,"full normal wear with book Empty");}
        check(GearData.wearFactor(b,p)==1&&!b.isDamageableItem(),"book never has a durability channel");
    }
    private void mining(ServerPlayer p){
        empty(p);
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);var center=p.blockPosition().offset(0,5,0);var w=p.level();
        for(var q:BlockPos.betweenClosed(center.offset(-4,-4,0),center.offset(4,4,0)))w.setBlock(q,Blocks.STONE.defaultBlockState(),2);
        p.setYRot(0);p.setXRot(0);p.setItemSlot(EquipmentSlot.MAINHAND,gear("true_omega_pickaxe","sequence_core_lv3"));
        check(p.gameMode.destroyBlock(center),"actual original block breaks");int extra=0;
        for(var q:BlockPos.betweenClosed(center.offset(-4,-4,0),center.offset(4,4,0))){if(q.equals(center))continue;if(q.getY()<center.getY())check(w.getBlockState(q).is(Blocks.STONE),"lower wall never excavated "+q);else if(w.getBlockState(q).isAir())extra++;}
        check(extra==3,"unchanged normal budget uses same/upward neighbors");
        for(var q:BlockPos.betweenClosed(center.offset(-4,-4,0),center.offset(4,4,0)))w.setBlock(q,Blocks.AIR.defaultBlockState(),2);
    }
}
