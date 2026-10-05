package dev.googology.client;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerInventory;

/** Ten real sockets with live, server-authoritative equipment statistics. */
public final class EnhancementScreen extends HandledScreen<EnhancementMenu> {
    private static final Identifier TEXTURE=GoogologyMod.id("textures/gui/enhancement.png");
    public EnhancementScreen(EnhancementMenu menu,PlayerInventory inventory,Text title){super(menu,inventory,title);backgroundWidth=362;backgroundHeight=238;playerInventoryTitleX=12;playerInventoryTitleY=138;}
    private static Text tr(String key,Object... args){return Text.translatable("mining.googology."+key,args);}
    @Override public void render(DrawContext graphics,int mouseX,int mouseY,float delta){
        renderBackground(graphics,mouseX,mouseY,delta);super.render(graphics,mouseX,mouseY,delta);drawMouseoverTooltip(graphics,mouseX,mouseY);
        if(focusedSlot!=null&&!focusedSlot.hasStack()&&focusedSlot.id>=EnhancementMenu.FIRST_CORE&&focusedSlot.id<EnhancementMenu.INVENTORY)
            graphics.drawTooltip(textRenderer,tr(focusedSlot.id-EnhancementMenu.FIRST_CORE>=handler.capacity()?"locked_slot":"empty_slot"),mouseX,mouseY);
    }
    @Override protected void drawBackground(DrawContext graphics,float delta,int mouseX,int mouseY){
        graphics.drawTexture(TEXTURE,x,y,0,0,backgroundWidth,backgroundHeight,512,256);
        for(int i=handler.capacity();i<EquipmentRules.MAX_SOCKETS;i++)graphics.drawTexture(TEXTURE,x+27+(i%5)*28,y+77+(i/5)*26,362,0,18,18,512,256);
        for(int i=EnhancementMenu.INVENTORY;i<handler.slots.size();i++){
            var slot=handler.slots.get(i);if(!handler.canInsertCore(slot.getStack()))continue;
            int sx=x+slot.x,sy=y+slot.y;
            graphics.fill(sx-1,sy-1,sx+17,sy+17,0xff67e8b2);
            graphics.fill(sx,sy,sx+16,sy+16,0xff49685c);
        }
    }
    private void line(DrawContext graphics,Text text,int y,int color){graphics.drawText(textRenderer,textRenderer.trimToWidth(text.getString(),156),194,y,color,false);}
    @Override protected void drawForeground(DrawContext graphics,int mouseX,int mouseY){
        graphics.drawText(textRenderer,title,12,12,0xffe5eee9,false);
        graphics.drawText(textRenderer,tr("gear_label"),49,42,0xff445753,false);
        graphics.drawText(textRenderer,tr("socket_count",handler.installed(),handler.capacity()),18,64,0xff354e4a,false);
        graphics.drawText(textRenderer,tr("drag_hint"),14,124,0xff59716d,false);
        graphics.drawText(textRenderer,playerInventoryTitle,playerInventoryTitleX,playerInventoryTitleY,0xff445753,false);
        line(graphics,tr("status"),34,0xffecdec0);
        var gear=handler.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        line(graphics,tr("grade_limit",Math.min(handler.rank(),handler.gearGrade())),y,0xffc2d9d0);y+=14;
        line(graphics,tr("level_sum",GearData.totalLevels(gear)),y,0xffc2d9d0);y+=14;
        if(spec.kind()==0){
            line(graphics,tr("denxi",EquipmentRules.format(GearData.denxi(gear))),y,0xfff2d38a);y+=14;
            line(graphics,tr("mining_speed",EquipmentRules.format(GearData.miningSpeed(gear))),y,0xffc2d9d0);y+=14;
        }
        if(spec.kind()<2){line(graphics,tr("attack",EquipmentRules.format(GearData.power(gear))),y,0xffc2d9d0);y+=14;}
        else if(spec.kind()<6){line(graphics,tr("defense",EquipmentRules.format(GearData.power(gear)*EquipmentRules.armorShare(spec.kind()))),y,0xffc2d9d0);y+=14;}
        if(spec.kind()<6)line(graphics,tr("durability",gear.getMaxDamage()-gear.getDamage(),gear.getMaxDamage()),y,0xffc2d9d0);y+=18;
        if(handler.installed()>0){line(graphics,tr("effect_points"),y,0xffecdec0);y+=12;}
        for(int type=0;type<9;type++)if(GearData.points(gear,type)>0){
            line(graphics,tr("effect_value",tr("effect."+type),EquipmentRules.format(GearData.points(gear,type))),y,0xff9cd6cd);y+=12;
        }
    }
}
