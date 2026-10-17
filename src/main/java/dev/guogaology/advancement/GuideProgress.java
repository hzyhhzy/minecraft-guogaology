package dev.guogaology.advancement;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.portal.PortalKind;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registry;
import net.minecraft.registry.*;

import net.minecraft.util.math.ChunkPos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import dev.guogaology.mining.*;
import dev.guogaology.survival.*;
import dev.guogaology.world.ProceduralTerrain;
import java.util.*;

/** Only server-confirmed actions complete manual guide criteria. */
public final class GuideProgress {
    private GuideProgress(){}
    public static final Map<String,Item> LANDMARK_ICONS=new LinkedHashMap<>();
    public static void initialize(){
        Registry.register(Registries.ITEM,GuogaologyMod.id("brand_emblem"),new Item(new Item.Settings()));
        Registry.register(Registries.ITEM,GuogaologyMod.id("lho_guide_emblem"),new Item(new Item.Settings()));
        for(var theme:SurvivalTheme.values()){
            var id=GuogaologyMod.id("landmark_emblem_"+theme.id);
            LANDMARK_ICONS.put(theme.id,Registry.register(Registries.ITEM,id,new Item(new Item.Settings())));
        }
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var player:server.getPlayerManager().getPlayerList())if((server.getTicks()+player.getId())%20==0)checkPlayer(player);});
    }
    public static boolean complete(ServerPlayerEntity player,String name){
        var advancement=player.getServer().getAdvancementLoader().get(GuogaologyMod.id("guide/"+name));
        return advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone();
    }
    public static void criterion(ServerPlayerEntity player,String name,String criterion){
        var advancement=player.getServer().getAdvancementLoader().get(GuogaologyMod.id("guide/"+name));
        if(advancement!=null)player.getAdvancementTracker().grantCriterion(advancement,criterion);
    }
    public static void grant(ServerPlayerEntity player,String name){
        criterion(player,name,"done");
    }
    public static void portalActivated(ServerWorld world,ItemEntity offering,PortalKind kind){
        if(!(offering.getOwner() instanceof ServerPlayerEntity player))return;
        var source=world.getRegistryKey();
        if(source.equals(GuogaologyMod.OUTER)&&kind==PortalKind.OUTER
                ||source.equals(GuogaologyMod.DIMENSION)&&kind==PortalKind.INNER
                ||source.equals(GuogaologyMod.GUOGAO)&&kind==PortalKind.GUOGAO)grant(player,"portal_return");
    }
    public static void edited(PlayerEntity player,ItemStack gear){
        if(player instanceof ServerPlayerEntity server&&!dev.guogaology.mining.GearData.profile(gear).isEmpty())grant(server,"socketed");
    }
    public static boolean fullyMaxed(ItemStack stack,int kind){
        var spec=MiningContent.GEAR.get(stack.getItem());
        if(spec==null||spec.tier()!=4||spec.kind()!=kind||GearData.hasNativeEnchantments(stack))return false;
        var slots=GearData.coreSlots(stack);if(slots.size()!=GearData.capacity(stack))return false;
        for(var core:slots){int type=GearData.type(core);
            if(core.isEmpty()||core.getCount()!=1||type<0||!EquipmentRules.compatible(kind,type)||EquipmentRules.displayedCoreGrade(type,GearData.level(core))!=3)return false;
        }
        return true;
    }
    public static boolean omegaMastery(ServerPlayerEntity player){
        return fullyMaxed(player.getEquippedStack(EquipmentSlot.HEAD),2)&&fullyMaxed(player.getEquippedStack(EquipmentSlot.CHEST),3)
            &&fullyMaxed(player.getEquippedStack(EquipmentSlot.LEGS),4)&&fullyMaxed(player.getEquippedStack(EquipmentSlot.FEET),5)
            &&fullyMaxed(ManuscriptEffects.held(player),EquipmentRules.MANUSCRIPT);
    }
    /** Called once a second per player; no remote searches, chunk requests or global exploration flags. */
    public static void checkPlayer(ServerPlayerEntity player){
        if(!player.isAlive())return;
        if(!complete(player,"omega_mastery")&&omegaMastery(player))grant(player,"omega_mastery");
        var world=player.getServerWorld();boolean under=world.getRegistryKey().equals(GuogaologyMod.GUOGAO);
        if(!under&&!world.getRegistryKey().equals(GuogaologyMod.DIMENSION))return;
        var pos=player.getBlockPos();if(!world.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4))return;
        if(!under)for(var theme:SurvivalTheme.values())if(!theme.underworld()&&!complete(player,"biome_"+theme.id)
                &&world.getBiome(pos).matchesKey(RegistryKey.of(RegistryKeys.BIOME,GuogaologyMod.id(theme.biome))))grant(player,"biome_"+theme.id);
        boolean pending=false;for(var theme:SurvivalTheme.values())if(theme.underworld()==under&&!complete(player,"landmark_"+theme.id)){pending=true;break;}
        if(!pending)return;
        long seed=ProceduralTerrain.seed(world.getChunkManager().getNoiseConfig());
        for(var site:SurvivalStructures.sitesForChunk(seed,under,new ChunkPos(pos)))if(!complete(player,"landmark_"+site.theme().id)&&inside(site,pos)){
            grant(player,"landmark_"+site.theme().id);criterion(player,"landmarks_all",site.theme().id);
        }
    }
    public static boolean inside(SurvivalStructures.Site site,net.minecraft.util.math.BlockPos pos){
        int dx=pos.getX()-site.x(),dz=pos.getZ()-site.z();
        int x=switch(site.rotation()){case 1->dz;case 2->-dx;case 3->-dz;default->dx;};
        int z=switch(site.rotation()){case 1->-dx;case 2->-dz;case 3->dx;default->dz;};
        return site.layout().visitArea(x,pos.getY()-site.y(),z);
    }
}
