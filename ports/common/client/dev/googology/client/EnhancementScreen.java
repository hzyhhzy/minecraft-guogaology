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
        graphics.drawString(font,tr("gear_label"),49,42,0xff445753,false);
        graphics.drawString(font,tr("socket_count",menu.installed(),menu.capacity()),18,64,0xff354e4a,false);
        graphics.drawString(font,tr("drag_hint"),14,124,0xff59716d,false);
        graphics.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xff445753,false);
        line(graphics,tr("status"),34,0xffecdec0);
        var gear=menu.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        line(graphics,tr("grade_limit",Math.min(menu.rank(),menu.gearGrade())),y,0xffc2d9d0);y+=14;
        line(graphics,tr("level_sum",GearData.totalLevels(gear)),y,0xffc2d9d0);y+=14;
        if(spec.kind()==0){
            line(graphics,tr("denxi",EquipmentRules.format(GearData.denxi(gear))),y,0xfff2d38a);y+=14;
            line(graphics,tr("mining_speed",EquipmentRules.format(GearData.miningSpeed(gear))),y,0xffc2d9d0);y+=14;
        }
        if(spec.kind()<2){line(graphics,tr("attack",EquipmentRules.format(GearData.power(gear))),y,0xffc2d9d0);y+=14;}
        else if(spec.kind()<6){line(graphics,tr("defense",EquipmentRules.format(GearData.power(gear)*EquipmentRules.armorShare(spec.kind()))),y,0xffc2d9d0);y+=14;}
        if(spec.kind()<6)line(graphics,tr("durability",gear.getMaxDamage()-gear.getDamageValue(),gear.getMaxDamage()),y,0xffc2d9d0);y+=18;
        if(menu.installed()>0){line(graphics,tr("effect_points"),y,0xffecdec0);y+=12;}
        for(int type=0;type<9;type++)if(GearData.points(gear,type)>0){
            line(graphics,tr("effect_value",tr("effect."+type),EquipmentRules.format(GearData.points(gear,type))),y,0xff9cd6cd);y+=12;
        }
    }
}
