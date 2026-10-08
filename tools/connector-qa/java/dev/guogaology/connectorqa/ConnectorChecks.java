package dev.guogaology.connectorqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.connectorqa.mixin.PlayerFixtureAccess;
import dev.guogaology.mining.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.*;
import java.util.*;

/** Same intermediary QA JAR runs on native Fabric and through real Connector. */
public final class ConnectorChecks implements ClientModInitializer {
    private int phase,ticks,checks,settling;private boolean opening,queued,done;
    private volatile boolean ready;private volatile Throwable failure;
    private Runnable pending;
    private java.util.function.BooleanSupplier chunksReady=()->true;
    private final long deadline=System.nanoTime()+720_000_000_000L;
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            if(pending!=null&&--settling<=0&&chunksReady.getAsBoolean()){var action=pending;pending=null;try{action.run();ready=true;}catch(Throwable e){failure=e;}}
        });
    }
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private void near(double actual,double expected,String label){check(Math.abs(actual-expected)<.01,label+": "+actual+" != "+expected);}
    private static ItemStack item(String id){return new ItemStack(Registries.ITEM.get(GuogaologyMod.id(id)));}
    private static ItemStack gear(String id,String... cores){var stack=item(id);GearData.setCores(stack,Arrays.stream(cores).map(ConnectorChecks::item).toList());return stack;}
    private static ItemStack book(String... cores){return gear("true_omega_manuscript",cores);}
    private static void clear(ServerPlayerEntity p){
        p.closeHandledScreen();for(var slot:EquipmentSlot.values())p.equipStack(slot,ItemStack.EMPTY);
        p.clearStatusEffects();p.setOnGround(true);p.setSprinting(false);ManuscriptEffects.tick(p);
        p.getAbilities().allowFlying=false;p.getAbilities().flying=false;p.getAbilities().invulnerable=false;
        p.setAbsorptionAmount(0);p.timeUntilRegen=0;p.setHealth(p.getMaxHealth());
    }
    private void tick(MinecraftClient c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Connector QA timeout phase="+phase+" screen="+c.currentScreen);
            c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(60);
            if(c.world==null){
                if(!opening&&c.currentScreen instanceof TitleScreen&&c.getOverlay()==null&&++ticks>30){
                    if(Boolean.getBoolean("guogaology.qa.startupOnly")){finish(c);return;}
                    opening=true;ticks=0;
                    if(Files.exists(c.runDirectory.toPath().resolve("saves/connector-qa")))throw new IllegalStateException("Fresh disposable world required");
                    c.createIntegratedServerLoader().createAndStart("connector-qa",new LevelInfo("Connector QA",GameMode.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE),new GeneratorOptions(50220261008L,true,false),WorldPresets::createDemoOptions,new TitleScreen());
                }return;
            }
            if(c.player==null||c.getServer()==null)return;
            if(!queued){queued=true;ready=false;var id=c.player.getUuid();c.getServer().execute(()->{try{setup(c.getServer().getPlayerManager().getPlayer(id));if(pending==null)ready=true;}catch(Throwable e){failure=e;}});return;}
            if(!ready||++ticks<70)return;
            if(phase==4){
                check(c.player.currentScreenHandler instanceof ManuscriptMenu,"actual synchronized manuscript screen");
                var m=(ManuscriptMenu)c.player.currentScreenHandler;
                check(m.manuscript()&&m.capacity()==6,"synchronized six-slot manuscript");
                check(m.canInsertCore(item("sequence_core_lv3")),"high grade socket candidate");
                check(!m.canInsertCore(item("astra_critical_core")),"incompatible core rejected");
            }
            var file="connector-"+phase+".png";
            ScreenshotRecorder.saveScreenshot(c.runDirectory,file,c.getFramebuffer(),message->System.out.println("CONNECTOR_SCREENSHOT "+message.getString()));
            if(++phase>4){finish(c);return;}
            queued=false;ticks=0;
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("connector-qa-failed.txt"),e.toString());}catch(Exception ignored){}c.scheduleStop();}
    }
    private void finish(MinecraftClient c)throws Exception{
        done=true;Files.writeString(Path.of("connector-qa-ok.txt"),"CONNECTOR_QA_OK checks="+checks+" version="+net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion()+"\n");c.scheduleStop();
    }
    private void setup(ServerPlayerEntity p)throws Exception{
        if(phase<4){
            var key=switch(phase){case 1->GuogaologyMod.OUTER;case 2->GuogaologyMod.DIMENSION;case 3->GuogaologyMod.GUOGAO;default->World.OVERWORLD;};
            var world=p.getServer().getWorld(key);check(world!=null,"dimension exists "+key);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.setChunkForced(x,z,true);
            p.teleportTo(new TeleportTarget(world,new Vec3d(8.5,300,14.5),Vec3d.ZERO,180,15,TeleportTarget.NO_OP));p.onTeleportationDone();((PlayerFixtureAccess)p).qa$joinInvulnerability(0);p.setNoGravity(true);p.changeGameMode(GameMode.SURVIVAL);
            p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(200);
            // Let destination chunks acquire entity-ticking status before firing.
            chunksReady=()->world.shouldTickEntity(new BlockPos(8,300,14));
            settling=80;pending=()->{try{exercise(p,world);}catch(Exception e){throw new RuntimeException(e);}};
        }else{
            clear(p);var stack=book("boundary_core_lv3","laver_core_lv2");p.equipStack(EquipmentSlot.OFFHAND,stack);ManuscriptEffects.tick(p);
            p.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inventory,player)->new ManuscriptMenu(id,inventory,p.getWorld(),40,stack),Text.literal("Connector QA")));
        }
    }
    private void exercise(ServerPlayerEntity p,net.minecraft.server.world.ServerWorld world)throws Exception{
            clear(p);falls(p);mining(p);bows(p);durability(p);
            p.changeGameMode(GameMode.CREATIVE);clear(p);
            for(int x=0;x<17;x++)for(int z=0;z<14;z++)world.setBlockState(new BlockPos(x,298,z),Blocks.QUARTZ_BLOCK.getDefaultState());
            String[] cores={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart","ordinal_crystal"};
            for(int i=0;i<cores.length;i++)for(int lv=1;lv<=3;lv++){
                var id=GuogaologyMod.id(cores[i]+(lv==1?"":"_lv"+lv));var block=Registries.BLOCK.get(id);check(block!=Blocks.AIR,"registered core "+id);
                var pos=new BlockPos(2+(i%5)*3,299,2+(i/5)*5+lv);world.setBlockState(pos,block.getDefaultState());check(world.getBlockState(pos).isOf(block),"native core placement "+id);
            }
            p.teleportTo(new TeleportTarget(world,new Vec3d(8.5,303,18.5),Vec3d.ZERO,180,25,TeleportTarget.NO_OP));
            System.out.println("CONNECTOR_REALM_OK "+world.getRegistryKey()+" checks="+checks);
    }
    private double impact(ServerPlayerEntity p,boolean ordinary){
        p.timeUntilRegen=0;p.setHealth(p.getMaxHealth());float start=p.getHealth();
        p.handleFallDamage(15,1,ordinary?p.getDamageSources().fall():p.getDamageSources().stalagmite());return start-p.getHealth();
    }
    private void falls(ServerPlayerEntity p)throws Exception{
        boolean under=p.getWorld().getRegistryKey().equals(GuogaologyMod.GUOGAO);clear(p);
        check(under||impact(p,true)>0,"native unprotected fall");
        for(int level=2;level<=3;level++){
            clear(p);p.equipStack(EquipmentSlot.OFFHAND,book("boundary_core_lv"+level));ManuscriptEffects.tick(p);
            check(ManuscriptEffects.ownsFlight(p),"manuscript owns permission");p.getAbilities().flying=true;
            near(impact(p,true),0,"ordinary fall immunity");
            double spike=impact(p,false);check(under?spike==0:spike>0,"stalagmite is not ordinary fall immunity "+spike);
            check(p.getAbilities().allowFlying&&p.getAbilities().flying,"permission restored after native impact");
            p.setOnGround(false);ManuscriptEffects.setFlightSprint(p,true);
            near(ManuscriptEffects.horizontalSpeed(p,0),ManuscriptFlightRules.horizontal(level,ManuscriptEffects.deep(p.getWorld()),level>=3),"flight horizontal");
            near(ManuscriptEffects.verticalSpeed(p,0),ManuscriptFlightRules.vertical(level,ManuscriptEffects.deep(p.getWorld()),level>=3),"flight vertical");
        }
        clear(p);p.getAbilities().allowFlying=true;p.getAbilities().flying=true;
        near(impact(p,false),0,"foreign flight immunity untouched");check(p.getAbilities().allowFlying,"foreign permission preserved");
        clear(p);p.changeGameMode(GameMode.CREATIVE);near(impact(p,false),0,"creative immunity untouched");p.changeGameMode(GameMode.SURVIVAL);clear(p);
    }
    private void mining(ServerPlayerEntity p){
        clear(p);near(p.getBlockBreakingSpeed(Blocks.STONE.getDefaultState()),1,"empty hand native speed");
        p.equipStack(EquipmentSlot.OFFHAND,book("sequence_core_lv3"));ManuscriptEffects.tick(p);
        near(p.getBlockBreakingSpeed(Blocks.STONE.getDefaultState()),40.8,"book mining flat plus rate on bare hands");
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,200,1));near(p.getBlockBreakingSpeed(Blocks.STONE.getDefaultState()),47.6,"Haste bucket remains additive");
        p.clearStatusEffects();p.equipStack(EquipmentSlot.MAINHAND,gear("true_omega_pickaxe","hydra_bud_lv3"));
        check(p.getBlockBreakingSpeed(Blocks.STONE.getDefaultState())>40.8,"ordinal pick native tool contribution");clear(p);
    }
    private void bows(ServerPlayerEntity p){
        clear(p);var bow=gear("true_omega_bow","hydra_bud_lv3","guogao_heart_lv3","astra_critical_core_lv3");p.equipStack(EquipmentSlot.MAINHAND,bow);
        p.getInventory().setStack(9,new ItemStack(Items.ARROW,5));
        check(BowEffects.release(bow,p.getWorld(),p,1,4),"real enhanced bow release");
        near(p.getInventory().getStack(9).getCount(),5,"Infinity preserves arrows");
        var arrows=p.getWorld().getEntitiesByClass(net.minecraft.entity.projectile.PersistentProjectileEntity.class,p.getBoundingBox().expand(4),a->a.getOwner()==p);
        if(arrows.size()!=3){
            var all=new ArrayList<String>();for(var entity:p.getServerWorld().iterateEntities())if(entity instanceof net.minecraft.entity.projectile.PersistentProjectileEntity a)all.add(a.getPos()+" owner="+a.getOwner()+" world="+a.getWorld().getRegistryKey());
            System.out.println("BOW_FIXTURE_DIAGNOSTIC player="+p.getPos()+" world="+p.getWorld().getRegistryKey()+" all="+all);
        }
        check(arrows.size()==3,"actual Multishot arrows "+arrows.size());
        for(var arrow:arrows){check(((ArrowShotAccess)arrow).guogaology$shot()!=null,"arrow enhancement snapshot");check(arrow.getPierceLevel()==1,"Piercing enabled");arrow.discard();}clear(p);
    }
    private void durability(ServerPlayerEntity p){
        clear(p);
        var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);
        vanilla.damage(7,p.getServerWorld(),p,i->{});
        near(vanilla.getDamage(),7,"native durability unchanged");
        var plain=gear("true_omega_pickaxe");plain.damage(17,p.getServerWorld(),p,i->{});
        near(plain.getDamage(),17,"unmodified ordinal wear");
        var protectedPick=gear("true_omega_pickaxe","lho_trace_lv3");
        protectedPick.damage(1024,p.getServerWorld(),p,i->{});
        check(protectedPick.getDamage()>0&&protectedPick.getDamage()<48,"actual shared wear filtering "+protectedPick.getDamage());
        var armor=gear("true_omega_chestplate");
        Object context=ArmorProtectionContext.enter(p,1000);
        try{armor.damage(1024,p.getServerWorld(),p,i->{});}
        finally{ArmorProtectionContext.restore(context);}
        check(armor.getDamage()<12,"wear hook retains living owner "+armor.getDamage());
        var following=new ItemStack(Items.DIAMOND_PICKAXE);
        following.damage(9,p.getServerWorld(),null,i->{});
        near(following.getDamage(),9,"owner context does not leak");
        var unbreaking=new ItemStack(Items.DIAMOND_PICKAXE);
        unbreaking.addEnchantment(p.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(net.minecraft.enchantment.Enchantments.UNBREAKING).orElseThrow(),3);
        unbreaking.damage(1024,p.getServerWorld(),p,i->{});
        check(unbreaking.getDamage()>128&&unbreaking.getDamage()<384,"native Unbreaking still filters wear "+unbreaking.getDamage());
        p.equipStack(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));ManuscriptEffects.tick(p);
        double factor=GearData.protectionFactor(p);float health=p.getHealth();p.timeUntilRegen=0;
        check(p.damage(p.getDamageSources().outOfWorld(),24),"native void damage reaches protected player");
        near(health-p.getHealth(),24/factor,"native damage protection survives translation");
        check(ArmorProtectionContext.owner()==null,"real damage snapshot restored");clear(p);
        var breakable=gear("true_omega_pickaxe","sequence_core");breakable.setDamage(breakable.getMaxDamage()-1);
        p.equipStack(EquipmentSlot.MAINHAND,breakable);
        var area=p.getBoundingBox().expand(4);
        int before=p.getWorld().getEntitiesByClass(ItemEntity.class,area,e->e.getStack().isOf(item("sequence_core").getItem())).stream().mapToInt(e->e.getStack().getCount()).sum();
        breakable.damage(1,p,EquipmentSlot.MAINHAND);
        check(breakable.isEmpty(),"native owner-aware wear destroys exhausted gear");
        int after=p.getWorld().getEntitiesByClass(ItemEntity.class,area,e->e.getStack().isOf(item("sequence_core").getItem())).stream().mapToInt(e->e.getStack().getCount()).sum();
        near(after-before,1,"exhausted gear drops exactly one installed core");clear(p);
    }
}
