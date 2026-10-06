package dev.googology.client;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Ten real sockets with live, server-authoritative equipment statistics. */
public final class EnhancementScreen extends AbstractContainerScreen<EnhancementMenu> {
    private static final Identifier TEXTURE=GoogologyMod.id("textures/gui/enhancement.png");
    public EnhancementScreen(EnhancementMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=362;imageHeight=238;inventoryLabelX=12;inventoryLabelY=138;}
    private static Component tr(String key,Object... args){return Component.translatable("mining.googology."+key,args);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);renderTooltip(graphics,mouseX,mouseY);
        if(hoveredSlot!=null&&!hoveredSlot.hasItem()&&hoveredSlot.index>=EnhancementMenu.FIRST_CORE&&hoveredSlot.index<EnhancementMenu.INVENTORY)
            graphics.setTooltipForNextFrame(font,tr(hoveredSlot.index-EnhancementMenu.FIRST_CORE>=menu.capacity()?"locked_slot":"empty_slot"),mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics,float delta,int mouseX,int mouseY){
        graphics.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,512,256);
        for(int i=menu.capacity();i<EquipmentRules.MAX_SOCKETS;i++)graphics.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,leftPos+27+(i%5)*28,topPos+77+(i/5)*26,362,0,18,18,512,256);
        for(int i=EnhancementMenu.INVENTORY;i<menu.slots.size();i++){
            var slot=menu.slots.get(i);if(!menu.canInsertCore(slot.getItem()))continue;
            int x=leftPos+slot.x,y=topPos+slot.y;
            graphics.fill(x-1,y-1,x+17,y+17,0xff67e8b2);
            graphics.fill(x,y,x+16,y+16,0xff49685c);
        }
    }
    private void line(GuiGraphics graphics,Component text,int y,int color){graphics.drawString(font,font.plainSubstrByWidth(text.getString(),156),194,y,color,false);}
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
        graphics.drawString(font,title,12,12,0xffe5eee9,false);
        graphics.drawString(font,tr(menu.manuscript()?"manuscript_label":"gear_label"),49,42,0xff445753,false);
        graphics.drawString(font,tr("socket_count",menu.installed(),menu.capacity()),18,64,0xff354e4a,false);
        graphics.drawString(font,tr(menu.manuscript()?"manuscript_drag_hint":"drag_hint"),14,124,0xff59716d,false);
        graphics.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xff445753,false);
        line(graphics,tr("status"),34,0xffecdec0);
        var gear=menu.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        line(graphics,tr("grade_limit",Math.min(menu.rank(),menu.gearGrade())),y,0xffc2d9d0);y+=14;
        var preview=menu.preview();var values=preview.effects();
        double attack=values.attack();
        if(menu.manuscript()&&minecraft.player!=null&&!MiningContent.GEAR.containsKey(minecraft.player.getMainHandItem().getItem()))attack=EquipmentRules.attack(GearData.baseAttack(minecraft.player.getMainHandItem()),java.util.List.of(),preview.book(),preview.deep());
        double wear=spec.kind()>=2&&spec.kind()<6?EquipmentRules.wearFactor(GearData.profile(gear),preview.book()):values.wearFactor();
        if(spec.kind()<2||spec.kind()==6){
            line(graphics,tr("status_attack",EquipmentRules.format(attack)),y,0xffc2d9d0);y+=14;
            line(graphics,tr("status_mining",EquipmentRules.format(values.miningMultiplier())),y,0xffc2d9d0);y+=14;
            line(graphics,tr("status_yield",values.yieldLevel()),y,0xffc2d9d0);y+=14;
        }
        if(spec.kind()==0){line(graphics,tr("status_range",values.extraBlocks()),y,0xffc2d9d0);y+=14;}
        if(values.reach()>0){line(graphics,tr("status_reach",EquipmentRules.format(values.reach())),y,0xffc2d9d0);y+=14;}
        if(values.regeneration()>0){line(graphics,tr("status_healing",EquipmentRules.format(values.regeneration())),y,0xffc2d9d0);y+=14;}
        if(values.bonusHealth()>0){line(graphics,tr("status_health",EquipmentRules.format(values.bonusHealth())),y,0xffc2d9d0);y+=14;}
        if(values.protectionFactor()>1){line(graphics,tr("status_defense",EquipmentRules.format(1/values.protectionFactor())),y,0xffc2d9d0);y+=14;}
        if(wear>1){line(graphics,tr("status_wear",EquipmentRules.format(wear)),y,0xffc2d9d0);y+=14;}
        if(spec.kind()==6){
            int jump=0;for(var core:GearData.profile(gear))if(core.type()==3&&core.level()==1)jump++;
            if(jump>0){line(graphics,tr("status_jump",jump),y,0xffc2d9d0);y+=14;}
            int flight=EquipmentRules.highest(GearData.profile(gear),3);
            if(flight>=2)line(graphics,tr("status_flight",tr(flight>=3?preview.deep()?"flight_deep":"flight_normal":"flight_slow")),y,0xffc2d9d0);
        }
    }
}
