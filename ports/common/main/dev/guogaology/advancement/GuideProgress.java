package dev.guogaology.advancement;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.portal.PortalKind;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
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
        var brand=GuogaologyMod.id("brand_emblem");Registry.register(BuiltInRegistries.ITEM,brand,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,brand))));
        var lho=GuogaologyMod.id("lho_guide_emblem");Registry.register(BuiltInRegistries.ITEM,lho,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,lho))));
        for(var theme:SurvivalTheme.values()){
            var id=GuogaologyMod.id("landmark_emblem_"+theme.id);
            LANDMARK_ICONS.put(theme.id,Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)))));
        }
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var player:server.getPlayerList().getPlayers())if((server.getTickCount()+player.getId())%20==0)checkPlayer(player);});
    }
    public static boolean complete(ServerPlayer player,String name){
        var advancement=player.level().getServer().getAdvancements().get(GuogaologyMod.id("guide/"+name));
        return advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    public static void criterion(ServerPlayer player,String name,String criterion){
        var advancement=player.level().getServer().getAdvancements().get(GuogaologyMod.id("guide/"+name));
        if(advancement!=null)player.getAdvancements().award(advancement,criterion);
    }
    public static void grant(ServerPlayer player,String name){
        criterion(player,name,"done");
    }
    public static void portalActivated(ServerLevel world,ItemEntity offering,PortalKind kind){
        if(!(offering.getOwner() instanceof ServerPlayer player))return;
        var source=world.dimension();
        if(source.equals(GuogaologyMod.OUTER)&&kind==PortalKind.OUTER
                ||source.equals(GuogaologyMod.DIMENSION)&&kind==PortalKind.INNER
                ||source.equals(GuogaologyMod.GUOGAO)&&kind==PortalKind.GUOGAO)grant(player,"portal_return");
    }
    public static void edited(Player player,ItemStack gear){
        if(player instanceof ServerPlayer server&&!dev.guogaology.mining.GearData.profile(gear).isEmpty())grant(server,"socketed");
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
    public static boolean omegaMastery(ServerPlayer player){
        return fullyMaxed(player.getItemBySlot(EquipmentSlot.HEAD),2)&&fullyMaxed(player.getItemBySlot(EquipmentSlot.CHEST),3)
            &&fullyMaxed(player.getItemBySlot(EquipmentSlot.LEGS),4)&&fullyMaxed(player.getItemBySlot(EquipmentSlot.FEET),5)
            &&fullyMaxed(ManuscriptEffects.held(player),EquipmentRules.MANUSCRIPT);
    }
    /** Called once a second per player; no remote searches, chunk requests or global exploration flags. */
    public static void checkPlayer(ServerPlayer player){
        if(!player.isAlive())return;
        if(!complete(player,"omega_mastery")&&omegaMastery(player))grant(player,"omega_mastery");
        var world=player.level();boolean under=world.dimension().equals(GuogaologyMod.GUOGAO);
        if(!under&&!world.dimension().equals(GuogaologyMod.DIMENSION))return;
        var pos=player.blockPosition();if(!world.hasChunk(pos.getX()>>4,pos.getZ()>>4))return;
        if(!under)for(var theme:SurvivalTheme.values())if(!theme.underworld()&&!complete(player,"biome_"+theme.id)
                &&world.getBiome(pos).is(ResourceKey.create(Registries.BIOME,GuogaologyMod.id(theme.biome))))grant(player,"biome_"+theme.id);
        boolean pending=false;for(var theme:SurvivalTheme.values())if(theme.underworld()==under&&!complete(player,"landmark_"+theme.id)){pending=true;break;}
        if(!pending)return;
        long seed=ProceduralTerrain.seed(world.getChunkSource().randomState());
        for(var site:SurvivalStructures.sitesForChunk(seed,under,new ChunkPos(pos)))if(!complete(player,"landmark_"+site.theme().id)&&inside(site,pos)){
            grant(player,"landmark_"+site.theme().id);criterion(player,"landmarks_all",site.theme().id);
        }
    }
    public static boolean inside(SurvivalStructures.Site site,net.minecraft.core.BlockPos pos){
        int dx=pos.getX()-site.x(),dz=pos.getZ()-site.z();
        int x=switch(site.rotation()){case 1->dz;case 2->-dx;case 3->-dz;default->dx;};
        int z=switch(site.rotation()){case 1->-dx;case 2->-dz;case 3->dx;default->dz;};
        return site.layout().visitArea(x,pos.getY()-site.y(),z);
    }
}
