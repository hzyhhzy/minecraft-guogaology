package dev.guogaology.client;

import dev.guogaology.lamp.LampShapes;
import dev.guogaology.lamp.LampShapingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

/** All 54 physical shapes are visible together; color is inherited from the real input. */
public final class LampShapingScreen extends AbstractContainerScreen<LampShapingMenu> {
    private final Button[] choices=new Button[LampShapes.COUNT];
    public LampShapingScreen(LampShapingMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=256;imageHeight=238;inventoryLabelX=48;inventoryLabelY=144;
    }
    @Override protected void init(){
        super.init();
        for(int i=0;i<LampShapes.COUNT;i++){
            final int choice=i;choices[i]=addRenderableWidget(Button.builder(Component.empty(),button->{
                if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,choice);
            }).bounds(leftPos+12+(i%9)*20,topPos+18+(i/9)*18,18,18).build());
        }
    }
    private Component label(int choice){
        int part=LampShapes.part(choice);
        Component emotion=Component.translatable("item.guogaology.lantern_emotion."+LampShapes.emotion(choice));
        if(part==27)return emotion;
        return Component.translatable("item.guogaology.expression_variant",emotion,
                Component.translatable("item.guogaology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        for(var button:choices)if(button!=null)button.active=menu.hasInput();
        super.render(graphics,mouseX,mouseY,delta);
        for(int i=0;i<LampShapes.COUNT;i++){
            int px=leftPos+13+(i%9)*20,py=topPos+19+(i/9)*18;
            graphics.renderItem(menu.preview(i),px,py);
            if(i==menu.selection()){
                graphics.fill(px-1,py-1,px+17,py,0xff55e6ca);graphics.fill(px-1,py+16,px+17,py+17,0xff55e6ca);
                graphics.fill(px-1,py-1,px,py+17,0xff55e6ca);graphics.fill(px+16,py-1,px+17,py+17,0xff55e6ca);
            }
        }
        renderTooltip(graphics,mouseX,mouseY);
        for(int i=0;i<LampShapes.COUNT;i++)if(choices[i]!=null&&choices[i].isHovered())graphics.setTooltipForNextFrame(font,label(i),mouseX,mouseY);
    }
    private void slot(GuiGraphics g,int sx,int sy){g.fill(leftPos+sx-1,topPos+sy-1,leftPos+sx+17,topPos+sy+17,0xff667879);g.fill(leftPos+sx,topPos+sy,leftPos+sx+17,topPos+sy+17,0xff152d35);}
    @Override protected void renderBg(GuiGraphics graphics,float delta,int mouseX,int mouseY){
        graphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff182d37);
        graphics.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xff29434b);
        graphics.fill(leftPos+8,topPos+16,leftPos+199,topPos+128,0xff142b34);
        graphics.fill(leftPos+8,topPos+71,leftPos+199,topPos+72,0xff56b7ba);
        for(var slot:menu.slots)slot(graphics,slot.x,slot.y);
        graphics.fill(leftPos+228,topPos+74,leftPos+232,topPos+93,0xff78d9cd);
        graphics.fill(leftPos+224,topPos+91,leftPos+236,topPos+95,0xff78d9cd);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY){
        graphics.drawString(font,font.plainSubstrByWidth(title.getString(),232),12,6,0xffe6f3ed,false);
        graphics.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xffc2d9d0,false);
        var hint=Component.translatable("lamp.guogaology.input_hint");
        graphics.drawString(font,font.plainSubstrByWidth(hint.getString(),232),12,132,0xffb9d3cb,false);
    }
}
