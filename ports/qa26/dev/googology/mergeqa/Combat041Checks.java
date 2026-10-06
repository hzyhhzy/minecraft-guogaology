package dev.googology.mergeqa;

import dev.googology.GoogologyBlocks;
import dev.googology.block.PortableRelicBlock;
import dev.googology.mining.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.PlayerTeam;
import java.lang.reflect.*;
import java.util.*;

/** Actual 26.2 loot, hit, oxygen and menu paths. Included only in the isolated QA JAR. */
public final class Combat041Checks {
    private static int checks;
    // All +/-6 fixture bounds remain in the login fixture's already-active chunk (0,1).
    private static final BlockPos CENTER=new BlockPos(8,250,24);
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<.002,label+": "+actual+" != "+expected);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:"+id)));}
    private static ItemStack gear(String id,String core,int count){var s=item(id);GearData.setCores(s,Collections.nCopies(count,item(core)));GearData.refresh(s);return s;}
    /** Complete only the QA handshake; synchronous checks cannot wait for a client ACK. */
    private static void acknowledgeMove(ServerPlayer player){player.hasChangedDimension();if(!player.connection.hasClientLoaded())player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());}
    private static Object invoke(Class<?> type,Object object,String name,Class<?> parameter,Object value){
        try{var method=type.getDeclaredMethod(name,parameter);method.setAccessible(true);return method.invoke(object,value);}
        catch(InvocationTargetException e){throw new AssertionError("QA vanilla entry failed: "+name,e.getCause());}
        catch(ReflectiveOperationException e){throw new AssertionError("QA vanilla entry unavailable: "+name,e);}
    }
    public static void run(ServerPlayer player){
        checks=0;var world=player.level().getServer().overworld();var oldWorld=player.level();var oldPosition=player.position();var oldVelocity=player.getDeltaMovement();var oldMode=player.gameMode();
        float oldHealth=player.getHealth(),oldAbsorption=player.getAbsorptionAmount(),oldYaw=player.getYRot(),oldPitch=player.getXRot();int oldAir=player.getAirSupply(),oldInvulnerability=player.invulnerableTime;boolean oldGround=player.onGround();
        var saved=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);for(var slot:SLOTS)saved.put(slot,player.getItemBySlot(slot));
        // Keep effect identity: the manuscript tracks its own layer by the exact instance.
        var oldEffects=List.copyOf(player.getActiveEffects());
        var blocks=new LinkedHashMap<BlockPos,BlockState>();var entities=new ArrayList<Entity>();var scoreboard=world.getScoreboard();PlayerTeam oldTeam=player.getTeam(),team=null;
        try{
            check(world.isPositionEntityTicking(CENTER),"combat fixture chunk is already entity-ticking before synchronous setup");
            for(var q:BlockPos.betweenClosed(CENTER.offset(-6,-1,-6),CENTER.offset(6,3,6))){world.getChunk(q.getX()>>4,q.getZ()>>4);if(world.getBlockEntity(q)!=null)throw new AssertionError("QA refuses to overwrite an existing block entity at "+q);blocks.put(q.immutable(),world.getBlockState(q));}
            for(var q:blocks.keySet())world.setBlock(q,q.getY()==249?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            player.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(CENTER.offset(0,0,-2)),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));player.setOnGround(true);player.setGameMode(GameType.SURVIVAL);player.removeAllEffects();for(var slot:SLOTS)player.setItemSlot(slot,ItemStack.EMPTY);
            acknowledgeMove(player);check(!player.isChangingDimension()&&player.connection.hasClientLoaded(),"combat QA player handshake ready");player.invulnerableTime=0;
            fortune(player,world);looting(player);enchantAndMerge(player,world);mainhandBook(player);
            team=scoreboard.addPlayerTeam("qa41_"+UUID.randomUUID().toString().substring(0,8));team.setAllowFriendlyFire(false);scoreboard.addPlayerToTeam(player.getScoreboardName(),team);
            var target=cow(world,CENTER,entities);var enemy=cow(world,CENTER.offset(2,0,0),entities);var ally=cow(world,CENTER.offset(0,0,2),entities);var hidden=cow(world,CENTER.offset(-2,0,0),entities);
            var visible=world.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(3));check(visible.containsAll(List.of(target,enemy,ally,hidden)),"all four QA targets are visible through the real explosion entity query; visible="+visible.size());
            scoreboard.addPlayerToTeam(ally.getScoreboardName(),team);
            // The marker is below the hit: allies and attacker stay in unobstructed sight.
            var marker=CENTER.below();world.setBlock(marker,Blocks.STONE.defaultBlockState(),2);for(int y=0;y<=2;y++)world.setBlock(CENTER.offset(-1,y,0),Blocks.STONE.defaultBlockState(),2);
            check(target.hasLineOfSight(enemy)&&target.hasLineOfSight(ally)&&target.hasLineOfSight(player)&&!target.hasLineOfSight(hidden),"QA blast layout exposes enemy/ally/attacker and obstructs only the hidden target");
            sword(player,world,target,enemy,ally,hidden,marker);
            projectiles(player,world,target,enemy,ally,hidden,marker,entities);
            oxygen(target,player);
            System.out.println("COMBAT041_CHECKS_OK checks="+checks+" real Fortune / Looting / sword-arrow-trident bursts / helmet oxygen / enchant and core-merge protection");
        } finally {
            for(var entity:entities){scoreboard.removePlayerFromTeam(entity.getScoreboardName());entity.discard();}
            if(team!=null){scoreboard.removePlayerFromTeam(player.getScoreboardName());scoreboard.removePlayerTeam(team);if(oldTeam!=null)scoreboard.addPlayerToTeam(player.getScoreboardName(),oldTeam);}
            for(var entry:blocks.entrySet())world.setBlock(entry.getKey(),entry.getValue(),2);
            for(var slot:SLOTS)player.setItemSlot(slot,saved.get(slot));player.removeAllEffects();for(var effect:oldEffects)player.addEffect(effect);
            player.setGameMode(oldMode);player.teleport(new TeleportTransition(oldWorld,oldPosition,oldVelocity,oldYaw,oldPitch,TeleportTransition.DO_NOTHING));player.setOnGround(oldGround);ManuscriptEffects.tick(player);
            acknowledgeMove(player);
            player.setHealth(Math.min(oldHealth,player.getMaxHealth()));player.setAbsorptionAmount(oldAbsorption);player.setAirSupply(oldAir);player.invulnerableTime=oldInvulnerability;
        }
    }
    private static void fortune(ServerPlayer p,ServerLevel world){
        var fortune=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        var pick=gear("true_omega_pickaxe","sequence_core_lv3",2);var book=gear("true_omega_manuscript","sequence_core_lv3",3);p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);var before=pick.copy();
        var ore=Blocks.DIAMOND_ORE.defaultBlockState();var effective=MiningEffects.lootTool(ore,world,p,pick);
        check(effective!=pick&&EnchantmentHelper.getItemEnchantmentLevel(fortune,effective)==15,"Fortune sums six tool and nine book levels once");
        int total=0,maximum=0;boolean valid=true;for(int n=0;n<128;n++){var drops=Block.getDrops(ore,world,CENTER,null,p,pick);int count=drops.stream().mapToInt(ItemStack::getCount).sum();valid&=drops.stream().allMatch(s->s.is(Items.DIAMOND))&&count>=1&&count<=16;total+=count;maximum=Math.max(maximum,count);}check(valid,"Fortune uses the ordinary diamond loot table and level15 bound");
        check(total>600&&maximum>=12,"real loot reaches combined Fortune15 rather than unenhanced/tool-only yield");check(ItemStack.matches(pick,before),"loot never enchants or changes the actual tool");
        var numeric=gear("number_pickaxe","sequence_core",1);numeric.set(MiningContent.DIGIT,9d);GearData.refresh(numeric);numeric.enchant(fortune,3);p.setItemSlot(EquipmentSlot.MAINHAND,numeric);p.setItemSlot(EquipmentSlot.OFFHAND,gear("omega_manuscript","sequence_core",1));
        check(MiningEffects.lootTool(ore,world,p,numeric)==numeric,"existing Fortune3 wins over combined core level2");
        maximum=0;for(int n=0;n<64;n++)maximum=Math.max(maximum,Block.getDrops(ore,world,CENTER,null,p,numeric).stream().mapToInt(ItemStack::getCount).sum());check(maximum==4,"real existing Fortune3 retains its loot ceiling");
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        var core=BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:sequence_core")).defaultBlockState();var storage=MiningContent.STORAGE[0].defaultBlockState();var placed=GoogologyBlocks.TREE_NODE_RED.defaultBlockState().setValue(PortableRelicBlock.PLAYER_PLACED,true);
        for(var state:List.of(core,storage,placed)){check(MiningEffects.lootTool(state,world,p,pick)==pick,"core/storage/placed state excludes virtual Fortune");var drops=Block.getDrops(state,world,CENTER,null,p,pick);check(drops.size()==1&&drops.getFirst().getCount()==1&&drops.getFirst().is(state.getBlock().asItem()),"excluded material stays one complete item");}
        var chestState=Blocks.CHEST.defaultBlockState();var chest=new ChestBlockEntity(CENTER,chestState);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        check(MiningEffects.lootTool(chestState,world,p,pick)==pick,"containers exclude virtual Fortune");var drops=Block.getDrops(chestState,world,CENTER,chest,p,pick);check(drops.size()==1&&drops.getFirst().is(Items.CHEST)&&drops.getFirst().getCount()==1&&chest.getItem(0).getCount()==7,"container contents never copied or consumed by loot evaluation");
    }
    private static void looting(ServerPlayer p){
        var looting=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        p.setItemSlot(EquipmentSlot.MAINHAND,gear("true_omega_sword","sequence_core_lv2",3));p.setItemSlot(EquipmentSlot.OFFHAND,gear("true_omega_manuscript","sequence_core_lv3",2));
        check(EnchantmentHelper.getEnchantmentLevel(looting,p)==12,"vanilla Looting entry sums sword and manuscript");
        var numeric=gear("number_sword","sequence_core",1);numeric.enchant(looting,3);p.setItemSlot(EquipmentSlot.MAINHAND,numeric);p.setItemSlot(EquipmentSlot.OFFHAND,gear("omega_manuscript","sequence_core",1));
        check(EnchantmentHelper.getEnchantmentLevel(looting,p)==3,"vanilla Looting3 wins over core level2");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));check(EnchantmentHelper.getEnchantmentLevel(looting,p)==0,"no stale Looting after removal");
    }
    private static void enchantAndMerge(ServerPlayer p,ServerLevel world){
        var enchantment=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        for(String id:List.of("true_omega_pickaxe","true_omega_sword","true_omega_helmet","true_omega_chestplate","true_omega_leggings","true_omega_boots","true_omega_manuscript")){var stack=item(id);check(!stack.isEnchantable()&&!enchantment.value().canEnchant(stack),"table/book eligibility blocked: "+id);stack.enchant(enchantment,3);check(!stack.isEnchanted(),"direct vanilla enchant entry blocked: "+id);}
        for(String id:List.of("number_pickaxe","number_sword")){var stack=item(id);check(stack.isEnchantable(),"numeric table eligibility retained");stack.enchant(enchantment,3);check(EnchantmentHelper.getItemEnchantmentLevel(enchantment,stack)==3,"numeric direct enchant retained");}
        var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);check(vanilla.isEnchantable()&&enchantment.value().canEnchant(vanilla),"vanilla eligibility retained");vanilla.enchant(enchantment,3);check(vanilla.isEnchanted(),"vanilla enchant retained");
        var book=EnchantmentHelper.createBook(new EnchantmentInstance(enchantment,1));var anvil=new AnvilMenu(411,p.getInventory(),ContainerLevelAccess.create(world,CENTER));var grindstone=new GrindstoneMenu(412,p.getInventory(),ContainerLevelAccess.create(world,CENTER));
        try{
            var mineral=item("true_omega_pickaxe");mineral.setDamageValue(400);anvil.getSlot(0).set(mineral);anvil.getSlot(1).set(book.copy());anvil.createResult();check(anvil.getSlot(2).getItem().isEmpty(),"anvil enchanted book cannot enchant ordinal equipment");
            var number=item("number_pickaxe");number.setDamageValue(30);anvil.getSlot(0).set(number);anvil.getSlot(1).set(book.copy());anvil.createResult();check(!anvil.getSlot(2).getItem().isEmpty()&&anvil.getSlot(2).getItem().isEnchanted(),"numeric anvil book route preserved");
            for(int variant=0;variant<4;variant++){
                var left=item("true_omega_pickaxe");var right=item("true_omega_pickaxe");GearData.refresh(left);GearData.refresh(right);
                if((variant&1)!=0)GearData.setCores(left,List.of(item("sequence_core")));if((variant&2)!=0)GearData.setCores(right,List.of(item("ordinal_crystal")));left.setDamageValue(400);right.setDamageValue(500);var leftBefore=left.copy();var rightBefore=right.copy();
                check(EnchantmentHelper.canStoreEnchantments(left)&&EnchantmentHelper.canStoreEnchantments(right),"empty enchantment metadata retained for ordinary anvil repair variant="+variant);
                anvil.getSlot(0).set(left);anvil.getSlot(1).set(right);anvil.createResult();check(anvil.getSlot(2).getItem().isEmpty()==(variant!=0),"anvil repairs plain gear and blocks core merges variant="+variant+" output="+anvil.getSlot(2).getItem());check(ItemStack.matches(left,leftBefore)&&ItemStack.matches(right,rightBefore),"anvil preview never consumes inputs or cores");
                grindstone.getSlot(0).set(left.copy());grindstone.getSlot(1).set(right.copy());grindstone.slotsChanged(grindstone.getSlot(0).container);check(grindstone.getSlot(2).getItem().isEmpty()==(variant!=0),"grindstone repairs plain gear and blocks core merges variant="+variant+" output="+grindstone.getSlot(2).getItem());check(ItemStack.matches(grindstone.getSlot(0).getItem(),leftBefore)&&ItemStack.matches(grindstone.getSlot(1).getItem(),rightBefore),"grindstone preview never consumes inputs or cores");
                if(variant==0)check(EnchantmentHelper.canStoreEnchantments(grindstone.getSlot(2).getItem()),"grindstone repair retains empty metadata for later anvil repair");
            }
            for(boolean withCore:new boolean[]{false,true}){
                var repair=withCore?gear("true_omega_pickaxe","sequence_core",1):item("true_omega_pickaxe");GearData.refresh(repair);repair.setDamageValue(400);var before=repair.copy();anvil.setItemName("");anvil.getSlot(0).set(repair);anvil.getSlot(1).set(new ItemStack(MiningContent.MATERIALS[3]));anvil.createResult();var repaired=anvil.getSlot(2).getItem();
                check(!repaired.isEmpty()&&repaired.getDamageValue()<400&&ItemStack.listMatches(GearData.cores(repaired),GearData.cores(before)),"material repair allowed and exact core contents conserved withCore="+withCore);check(ItemStack.matches(repair,before),"material repair preview does not consume or mutate its input");
                anvil.getSlot(1).set(ItemStack.EMPTY);anvil.setItemName("QA ordinal repair");anvil.createResult();var renamed=anvil.getSlot(2).getItem();check(!renamed.isEmpty()&&renamed.getHoverName().getString().equals("QA ordinal repair")&&renamed.getDamageValue()==400&&ItemStack.listMatches(GearData.cores(renamed),GearData.cores(before)),"rename allowed without repairing or losing cores withCore="+withCore);check(ItemStack.matches(repair,before),"rename preview does not consume or mutate its input");
            }
        } finally {for(var menu:List.<AbstractContainerMenu>of(anvil,grindstone))for(int slot=0;slot<=2;slot++)menu.getSlot(slot).set(ItemStack.EMPTY);}
    }
    private static void mainhandBook(ServerPlayer p){
        p.setItemSlot(EquipmentSlot.MAINHAND,gear("true_omega_manuscript","ordinal_crystal_lv4",2));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        near(GearData.attackWithBook(p.getMainHandItem(),p),1,"mainhand manuscript is an ordinary held item");near(GearData.bookAttackBonus(p),0,"mainhand manuscript grants no passive attack");near(GearData.snapshot(p).effects().attack(),1,"pure snapshot also excludes mainhand manuscript passives");
    }
    private static Cow cow(ServerLevel world,BlockPos pos,List<Entity> entities){var cow=EntityTypes.COW.create(world,EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("QA cow creation failed");cow.setNoAi(true);cow.setNoGravity(true);cow.setSilent(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(Vec3.atBottomCenterOf(pos));entities.add(cow);check(world.addFreshEntity(cow),"temporary QA target spawned");return cow;}
    private static void reset(Cow target,Cow enemy,Cow ally,Cow hidden){for(var cow:List.of(target,enemy,ally,hidden)){cow.setHealth(100);cow.invulnerableTime=0;cow.setDeltaMovement(Vec3.ZERO);}}
    private static void sword(ServerPlayer p,ServerLevel world,Cow target,Cow enemy,Cow ally,Cow hidden,BlockPos marker){
        reset(target,enemy,ally,hidden);var sword=gear("true_omega_sword","astra_critical_core_lv3",1);p.setItemSlot(EquipmentSlot.MAINHAND,sword);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);float health=p.getHealth();
        check(target.hurtServer(world,p.damageSources().playerAttack(p),2),"direct hit creates ordinary fresh invulnerability");int timer=target.invulnerableTime;((OrdinalGear)sword.getItem()).hurtEnemy(sword,target,p);
        near(100-target.getHealth(),2+16*.3,"sword blast includes the direct-hit target");near(100-enemy.getHealth(),16*.3,"sword blast includes a nearby enemy");near(ally.getHealth(),100,"sword blast excludes ally");near(hidden.getHealth(),100,"sword blast respects center-to-target obstruction");near(p.getHealth(),health,"sword blast excludes attacker");check(target.invulnerableTime==timer,"primary supplemental hit restores prior invulnerability timer");check(world.getBlockState(marker).is(Blocks.STONE),"sword blast does not destroy terrain");
    }
    private static void projectiles(ServerPlayer p,ServerLevel world,Cow target,Cow enemy,Cow ally,Cow hidden,BlockPos marker,List<Entity> entities){
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));p.setItemSlot(EquipmentSlot.OFFHAND,gear("true_omega_manuscript","astra_critical_core_lv3",1));
        for(boolean trident:new boolean[]{false,true}){
            reset(target,enemy,ally,hidden);float health=p.getHealth();AbstractArrow shot=trident?new ThrownTrident(world,p,new ItemStack(Items.TRIDENT)):new Arrow(world,p,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));entities.add(shot);shot.setDeltaMovement(1,0,0);shot.setBaseDamage(4);double body=trident?8:4;Class<?> entry=trident?ThrownTrident.class:AbstractArrow.class;
            invoke(entry,shot,"onHitEntity",EntityHitResult.class,new EntityHitResult(target));
            near(100-target.getHealth(),body*1.3,"real "+(trident?"trident":"arrow")+" hit includes one primary burst");near(100-enemy.getHealth(),body*.3,"projectile blast hits neighboring enemy");near(ally.getHealth(),100,"projectile blast excludes ally");near(hidden.getHealth(),100,"projectile blast respects obstruction");near(p.getHealth(),health,"projectile blast excludes its nearby owner");check(world.getBlockState(marker).is(Blocks.STONE),"projectile burst preserves terrain");
            reset(target,enemy,ally,hidden);shot.setDeltaMovement(1,0,0);invoke(entry,shot,"onHitEntity",EntityHitResult.class,new EntityHitResult(target));near(100-target.getHealth(),body,"same projectile cannot emit a second burst");near(enemy.getHealth(),100,"no second neighboring burst from reused projectile");
        }
    }
    private static void oxygen(Cow target,ServerPlayer p){
        target.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,gear("true_omega_manuscript","hydra_bud_lv3",1));check((int)invoke(LivingEntity.class,target,"decreaseAirSupply",int.class,300)==299,"manuscript branch alone grants no helmet breathing");
        for(int level=1;level<=3;level++){target.setItemSlot(EquipmentSlot.HEAD,gear("true_omega_helmet","hydra_bud"+(level==1?"":"_lv"+level),1));int consumed=0;for(int n=0;n<4096;n++){int value=(int)invoke(LivingEntity.class,target,"decreaseAirSupply",int.class,300);if(value!=300&&value!=299)throw new AssertionError("oxygen changed by more than one normal point");if(value==299)consumed++;}check(level==1?consumed>800&&consumed<1248:level==2?consumed>128&&consumed<384:consumed==0,"real helmet oxygen probability Lv"+level+" consumed="+consumed);}
        target.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
}
