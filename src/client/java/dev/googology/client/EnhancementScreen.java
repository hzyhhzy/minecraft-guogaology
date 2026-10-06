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
        graphics.drawText(textRenderer,tr(handler.manuscript()?"manuscript_label":"gear_label"),49,42,0xff445753,false);
        graphics.drawText(textRenderer,tr("socket_count",handler.installed(),handler.capacity()),18,64,0xff354e4a,false);
        graphics.drawText(textRenderer,tr(handler.manuscript()?"manuscript_drag_hint":"drag_hint"),14,124,0xff59716d,false);
        graphics.drawText(textRenderer,playerInventoryTitle,playerInventoryTitleX,playerInventoryTitleY,0xff445753,false);
        line(graphics,tr("status"),34,0xffecdec0);
        var gear=handler.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        line(graphics,tr("grade_limit",Math.min(handler.rank(),handler.gearGrade())),y,0xffc2d9d0);y+=14;
        var preview=handler.preview();var values=preview.effects();
        double attack=values.attack();
        if(handler.manuscript()&&client.player!=null&&!MiningContent.GEAR.containsKey(client.player.getMainHandStack().getItem()))attack=EquipmentRules.attack(GearData.baseAttack(client.player.getMainHandStack()),java.util.List.of(),preview.book(),preview.deep());
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
