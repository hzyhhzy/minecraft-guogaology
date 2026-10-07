package dev.guogaology.client;

import dev.guogaology.GuogaologyMod;
import net.minecraft.client.gui.widget.ButtonWidget;
import dev.guogaology.mining.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerInventory;

/** Ten real sockets with live, server-authoritative equipment statistics. */
public final class EnhancementScreen extends HandledScreen<EnhancementMenu> {
    private static final Identifier TEXTURE=GuogaologyMod.id("textures/gui/enhancement.png");
    private ButtonWidget silkButton;
    public EnhancementScreen(EnhancementMenu menu,PlayerInventory inventory,Text title){super(menu,inventory,title);backgroundWidth=362;backgroundHeight=238;playerInventoryTitleX=12;playerInventoryTitleY=138;}
    @Override protected void init(){super.init();silkButton=addDrawableChild(ButtonWidget.builder(tr("silk_off"),button->{if(client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,0);}).dimensions(x+18,y+122,154,14).build());}
    private static Text tr(String key,Object... args){return Text.translatable("mining.guogaology."+key,args);}
    @Override public void render(DrawContext graphics,int mouseX,int mouseY,float delta){
        if(silkButton!=null){silkButton.visible=handler.canToggleSilk();silkButton.active=silkButton.visible;silkButton.setMessage(tr(handler.silkTouch()?"silk_on":"silk_off"));}
        renderBackground(graphics,mouseX,mouseY,delta);super.render(graphics,mouseX,mouseY,delta);drawMouseoverTooltip(graphics,mouseX,mouseY);
        if(focusedSlot!=null&&!focusedSlot.hasStack()&&focusedSlot.id>=EnhancementMenu.FIRST_CORE&&focusedSlot.id<EnhancementMenu.INVENTORY)
            graphics.drawTooltip(textRenderer,tr(focusedSlot.id-EnhancementMenu.FIRST_CORE>=handler.capacity()?"locked_slot":"empty_slot"),mouseX,mouseY);
    }
    @Override protected void drawBackground(DrawContext graphics,float delta,int mouseX,int mouseY){
        graphics.drawTexture(TEXTURE,x,y,0,0,backgroundWidth,backgroundHeight,512,256);
        for(int i=handler.capacity();i<EquipmentRules.MAX_SOCKETS;i++)graphics.drawTexture(TEXTURE,x+27+(i%5)*28,y+77+(i/5)*26,362,0,18,18,512,256);
        for(int i=EnhancementMenu.INVENTORY;i<handler.slots.size();i++){
            var slot=handler.slots.get(i);if(!handler.canSelectInventoryItem(slot.getStack()))continue;
            int sx=x+slot.x,sy=y+slot.y;
            graphics.fill(sx-1,sy-1,sx+17,sy+17,0xff67e8b2);
            graphics.fill(sx,sy,sx+16,sy+16,0xff49685c);
        }
    }
    @Override protected void drawSlot(DrawContext graphics,net.minecraft.screen.slot.Slot slot){
        super.drawSlot(graphics,slot);
        if(slot.id>=EnhancementMenu.INVENTORY&&slot.hasStack()&&!handler.canSelectInventoryItem(slot.getStack())){
            graphics.getMatrices().push();graphics.getMatrices().translate(0,0,300);
            graphics.fill(slot.x,slot.y,slot.x+16,slot.y+16,0xbd383c40);
            graphics.getMatrices().pop();
        }
    }
    private void line(DrawContext graphics,Text text,int y,int color){graphics.drawText(textRenderer,textRenderer.trimToWidth(text.getString(),156),194,y,color,false);}
    @Override protected void drawForeground(DrawContext graphics,int mouseX,int mouseY){
        graphics.drawText(textRenderer,title,12,12,0xffe5eee9,false);
        var gearLabel=tr(handler.manuscript()?"manuscript_label":"gear_label");
        graphics.drawText(textRenderer,gearLabel,78-textRenderer.getWidth(gearLabel),42,0xff445753,false);
        graphics.drawText(textRenderer,tr("socket_count",handler.installed(),handler.capacity()),18,64,0xff354e4a,false);
        if(!handler.canToggleSilk())graphics.drawText(textRenderer,tr(handler.manuscript()?"manuscript_drag_hint":"drag_hint"),14,124,0xff59716d,false);
        graphics.drawText(textRenderer,playerInventoryTitle,playerInventoryTitleX,playerInventoryTitleY,0xff445753,false);
        line(graphics,tr("manuscript.preview"),34,0xffecdec0);
        var gear=handler.gear();var spec=MiningContent.GEAR.get(gear.getItem());
        if(spec==null){line(graphics,tr("insert_gear"),55,0xffc2d9d0);return;}
        int y=52;
        var preview=handler.preview();var values=preview.effects();
        var profile=GearData.profile(gear);
        double wear=EquipmentRules.wearFactor(spec.kind(),profile);
        if(spec.kind()==7){line(graphics,tr("bow_speed",EquipmentRules.format(new double[]{1.1,1.2,1.3,1.5}[spec.tier()-1])),y,0xffc2d9d0);y+=12;line(graphics,tr("bow_native_damage"),y,0xffc2d9d0);y+=12;}
        if(spec.kind()<2){line(graphics,tr("status_base_attack",EquipmentRules.format(EquipmentRules.baseAttack(spec.tier(),spec.kind(),GearData.digit(gear)))),y,0xffc2d9d0);y+=12;}
        var current=new EquipmentRules.Gear(spec.tier(),spec.kind(),GearData.digit(gear),profile);
        var attackTerms=EquipmentRules.attackPreviewTerms(current,preview.deep());
        if(!attackTerms.isEmpty()){
            String expression=preview.deep()?attackTerms.stream().map(value->"×"+EquipmentRules.format(value)).collect(java.util.stream.Collectors.joining(" ")):"+"+EquipmentRules.format(attackTerms.stream().mapToDouble(Double::doubleValue).sum());
            line(graphics,tr(preview.deep()?"status_attack_factor":"status_attack",expression),y,0xffc2d9d0);y+=12;
        }
        if(spec.kind()==0){line(graphics,tr("status_mining_speed",EquipmentRules.format(EquipmentRules.baseMining(spec.tier(),GearData.digit(gear)))),y,0xffc2d9d0);y+=12;line(graphics,tr("status_range",values.extraBlocks()),y,0xffc2d9d0);y+=12;}
        if((spec.kind()==0||spec.kind()==1)&&values.yieldLevel()>0){line(graphics,tr("status_enchantment",Text.translatable(EquipmentRules.coreEffectKey(spec.kind(),2,1)),values.yieldLevel()),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==EquipmentRules.BOW){int branch=EquipmentRules.highest(profile,2);for(int level=1;level<=branch;level++){line(graphics,Text.translatable(EquipmentRules.coreEffectKey(EquipmentRules.BOW,2,level)),y,0xffc2d9d0);y+=12;}}
        if(spec.kind()>=2&&spec.kind()<=5){line(graphics,tr("native_armor",EquipmentRules.format(EquipmentRules.nativeArmor(spec.tier(),spec.kind())),EquipmentRules.format(EquipmentRules.nativeToughness(spec.tier(),spec.kind()))),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==6){
            double flat=EquipmentRules.manuscriptMiningFlat(profile);
            if(flat>0){line(graphics,tr("status_mining_flat",EquipmentRules.format(flat)),y,0xffc2d9d0);y+=12;}
            line(graphics,tr("status_mining_rate",String.format(java.util.Locale.ROOT,"%.2f",1+EquipmentRules.bookMiningBase(spec.tier())+EquipmentRules.manuscriptMiningRate(profile))),y,0xffc2d9d0);y+=12;
        }
        if((spec.kind()==1||spec.kind()==7)&&values.criticalCoefficient()>0){line(graphics,tr("status_burst",EquipmentRules.format(100*values.criticalCoefficient())),y,0xffc2d9d0);y+=12;}
        if(values.controlSeconds()>0){line(graphics,tr("status_duration",EquipmentRules.format(values.controlSeconds())),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==2&&EquipmentRules.highest(profile,4)>0){line(graphics,tr("status_oxygen",EquipmentRules.format(EquipmentRules.oxygenConsumption(profile))),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==5&&EquipmentRules.highest(profile,4)>0){line(graphics,tr("status_water",EquipmentRules.highest(profile,4)),y,0xffc2d9d0);y+=12;}
        if(values.reach()>0){line(graphics,tr("status_reach",EquipmentRules.format(values.reach())),y,0xffc2d9d0);y+=12;}
        if(values.regeneration()>0){line(graphics,tr("status_healing",EquipmentRules.format(values.regeneration())),y,0xffc2d9d0);y+=12;}
        if(values.bonusHealth()>0){line(graphics,tr("status_health",EquipmentRules.format(values.bonusHealth())),y,0xffc2d9d0);y+=12;}
        if(values.protectionFactor()>1){
            boolean armor=spec.kind()>=2&&spec.kind()<=5;
            line(graphics,tr(armor?"status_armor_protection":"status_defense",EquipmentRules.format(armor?100*(values.protectionFactor()-1):values.protectionFactor())),y,0xffc2d9d0);y+=12;
        }
        if(wear>1){line(graphics,tr("status_wear",EquipmentRules.format(wear)),y,0xffc2d9d0);y+=12;}
        if(spec.kind()==6){
            double duration=EquipmentRules.controlDuration(java.util.List.of(new EquipmentRules.Core(7,1)),profile,EquipmentRules.bookDurationBase(spec.tier()))/2;
            line(graphics,tr("status_duration_factor",EquipmentRules.format(duration)),y,0xffc2d9d0);y+=12;
            int totems=GearData.socketTotems(gear);if(totems>0){line(graphics,tr("manuscript.totem_slots",totems),y,0xffc2d9d0);y+=12;}
            int stealth=EquipmentRules.highest(profile,3),laver=EquipmentRules.highest(profile,4);
            if(stealth>0){line(graphics,tr("status_stealth",stealth),y,0xffc2d9d0);y+=12;}
            if(laver>=2){line(graphics,tr("status_food",laver>=3?19:10),y,0xffc2d9d0);y+=12;}
            int jump=0;for(var core:GearData.profile(gear))if(core.type()==6&&core.level()==1)jump++;
            if(jump>0){line(graphics,tr("status_jump",jump),y,0xffc2d9d0);y+=12;}
            double walking=EquipmentRules.boundaryWalkRate(profile);
            if(walking>0){line(graphics,tr("status_walking",EquipmentRules.format(100*walking)),y,0xffc2d9d0);y+=12;}
            int flight=EquipmentRules.highest(profile,6);
            if(jump>0){line(graphics,tr("status_safe_fall",2),y,0xffc2d9d0);y+=12;}
            if(flight>0){line(graphics,tr("status_landing",EquipmentRules.format(flight>=2?0:.5),EquipmentRules.format(flight>=3?0:jump>0?.5:1)),y,0xffc2d9d0);y+=12;}
            if(flight>=2)line(graphics,tr("status_flight",tr(flight>=3?preview.deep()?"flight_deep":"flight_normal":"flight_slow")),y,0xffc2d9d0);
        }
    }
}
