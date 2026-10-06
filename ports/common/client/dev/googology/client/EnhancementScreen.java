package dev.googology.client;

import dev.googology.GoogologyMod;
import net.minecraft.client.gui.components.Button;
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
    private Button silkButton;
    public EnhancementScreen(EnhancementMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=362;imageHeight=238;inventoryLabelX=12;inventoryLabelY=138;}
    @Override protected void init(){super.init();silkButton=addRenderableWidget(Button.builder(tr("silk_off"),button->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+18,topPos+122,154,14).build());}
    private static Component tr(String key,Object... args){return Component.translatable("mining.googology."+key,args);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        if(silkButton!=null){silkButton.visible=menu.canToggleSilk();silkButton.active=silkButton.visible;silkButton.setMessage(tr(menu.silkTouch()?"silk_on":"silk_off"));}
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
        if(!menu.canToggleSilk())graphics.drawString(font,tr(menu.manuscript()?"manuscript_drag_hint":"drag_hint"),14,124,0xff59716d,false);
        graphics.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xff445753,false);
        line(graphics,tr("status"),34,0xffecdec0);
        var gear=menu.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        line(graphics,tr("grade_limit",Math.min(menu.rank(),menu.gearGrade())),y,0xffc2d9d0);y+=12;
        var preview=menu.preview();var values=preview.effects();
        double attack=values.attack();
        if(menu.manuscript()&&minecraft.player!=null&&!MiningContent.GEAR.containsKey(minecraft.player.getMainHandItem().getItem()))attack=EquipmentRules.attack(GearData.baseAttack(minecraft.player.getMainHandItem()),java.util.List.of(),preview.book(),preview.deep(),EquipmentRules.bookBase(spec.tier()),EquipmentRules.bookAttackHp(spec.tier()));
        var profile=GearData.profile(gear);
        double wear=EquipmentRules.wearFactor(profile,java.util.List.of());
        if(spec.kind()==7){line(graphics,tr("bow_speed",EquipmentRules.format(new double[]{1.1,1.2,1.3,1.5}[spec.tier()-1])),y,0xffc2d9d0);y+=12;line(graphics,tr("bow_native_damage"),y,0xffc2d9d0);y+=12;}
        if(spec.kind()<2||spec.kind()==6){
            line(graphics,tr("status_attack",EquipmentRules.format(attack)),y,0xffc2d9d0);y+=12;

        }
        if(spec.kind()==0){line(graphics,tr("status_mining_speed",EquipmentRules.format(EquipmentRules.baseMining(spec.tier(),GearData.digit(gear)))),y,0xffc2d9d0);y+=12;line(graphics,tr("status_range",values.extraBlocks()),y,0xffc2d9d0);y+=12;}
        if((spec.kind()==0||spec.kind()==1)&&values.yieldLevel()>0){line(graphics,tr("status_yield",values.yieldLevel()),y,0xffc2d9d0);y+=12;}
        if(spec.kind()>=2&&spec.kind()<=5){line(graphics,tr("native_armor",EquipmentRules.format(EquipmentRules.nativeArmor(spec.tier(),spec.kind())),EquipmentRules.format(EquipmentRules.nativeToughness(spec.tier(),spec.kind()))),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==6&&EquipmentRules.efficiencyLevel(profile)>0){line(graphics,tr("status_efficiency",EquipmentRules.efficiencyLevel(profile)),y,0xffc2d9d0);y+=12;}
        if((spec.kind()==1||spec.kind()==7)&&values.criticalCoefficient()>0){line(graphics,tr("status_burst",EquipmentRules.format(100*values.criticalCoefficient())),y,0xffc2d9d0);y+=12;}
        if(values.controlSeconds()>0){line(graphics,tr("status_duration",EquipmentRules.format(values.controlSeconds())),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==2&&EquipmentRules.highest(profile,4)>0){line(graphics,tr("status_oxygen",EquipmentRules.format(EquipmentRules.oxygenConsumption(profile))),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==5&&EquipmentRules.highest(profile,4)>0){line(graphics,tr("status_water",EquipmentRules.highest(profile,4)),y,0xffc2d9d0);y+=12;}
        if(values.reach()>0){line(graphics,tr("status_reach",EquipmentRules.format(values.reach())),y,0xffc2d9d0);y+=12;}
        if(values.regeneration()>0){line(graphics,tr("status_healing",EquipmentRules.format(values.regeneration())),y,0xffc2d9d0);y+=12;}
        if(values.bonusHealth()>0){line(graphics,tr("status_health",EquipmentRules.format(values.bonusHealth())),y,0xffc2d9d0);y+=12;}
        if(values.protectionFactor()>1){line(graphics,tr("status_defense",EquipmentRules.format(1/values.protectionFactor())),y,0xffc2d9d0);y+=12;}
        if(wear>1){line(graphics,tr("status_wear",EquipmentRules.format(wear)),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==6){
            int stealth=EquipmentRules.highest(profile,3),laver=EquipmentRules.highest(profile,4);
            if(stealth>0){line(graphics,tr("status_stealth",stealth),y,0xffc2d9d0);y+=12;}
            if(laver>=2){line(graphics,tr("status_food",laver>=3?19:10),y,0xffc2d9d0);y+=12;}
            int jump=0;for(var core:GearData.profile(gear))if(core.type()==6&&core.level()==1)jump++;
            if(jump>0){line(graphics,tr("status_jump",jump),y,0xffc2d9d0);y+=12;}
            int flight=EquipmentRules.highest(GearData.profile(gear),6);
            if(flight>=2)line(graphics,tr("status_flight",tr(flight>=3?preview.deep()?"flight_deep":"flight_normal":"flight_slow")),y,0xffc2d9d0);
        }
    }
}
