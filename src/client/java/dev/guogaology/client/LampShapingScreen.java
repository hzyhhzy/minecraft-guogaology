package dev.guogaology.client;

import dev.guogaology.lamp.LampShapes;
import dev.guogaology.lamp.LampShapingMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** All 54 physical shapes are visible together; color is inherited from the real input. */
public final class LampShapingScreen extends HandledScreen<LampShapingMenu> {
    private final ButtonWidget[] choices=new ButtonWidget[LampShapes.COUNT];
    public LampShapingScreen(LampShapingMenu menu,PlayerInventory inventory,Text title){
        super(menu,inventory,title);backgroundWidth=256;backgroundHeight=238;playerInventoryTitleX=48;playerInventoryTitleY=144;
    }
    @Override protected void init(){
        super.init();
        for(int i=0;i<LampShapes.COUNT;i++){
            final int choice=i;choices[i]=addDrawableChild(ButtonWidget.builder(Text.empty(),button->{
                if(client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,choice);
            }).dimensions(x+12+(i%9)*20,y+18+(i/9)*18,18,18).build());
        }
    }
    private Text label(int choice){
        int part=LampShapes.part(choice);
        Text emotion=Text.translatable("item.guogaology.lantern_emotion."+LampShapes.emotion(choice));
        if(part==27)return emotion;
        return Text.translatable("item.guogaology.expression_variant",emotion,
                Text.translatable("item.guogaology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
    }
    @Override public void render(DrawContext graphics,int mouseX,int mouseY,float delta){
        for(var button:choices)if(button!=null)button.active=handler.hasInput();
        renderBackground(graphics,mouseX,mouseY,delta);super.render(graphics,mouseX,mouseY,delta);
        for(int i=0;i<LampShapes.COUNT;i++){
            int px=x+13+(i%9)*20,py=y+19+(i/9)*18;
            graphics.drawItem(handler.preview(i),px,py);
            if(i==handler.selection()){
                graphics.fill(px-1,py-1,px+17,py,0xff55e6ca);graphics.fill(px-1,py+16,px+17,py+17,0xff55e6ca);
                graphics.fill(px-1,py-1,px,py+17,0xff55e6ca);graphics.fill(px+16,py-1,px+17,py+17,0xff55e6ca);
            }
        }
        drawMouseoverTooltip(graphics,mouseX,mouseY);
        for(int i=0;i<LampShapes.COUNT;i++)if(choices[i]!=null&&choices[i].isHovered())graphics.drawTooltip(textRenderer,label(i),mouseX,mouseY);
    }
    private void slot(DrawContext g,int sx,int sy){g.fill(x+sx-1,y+sy-1,x+sx+17,y+sy+17,0xff667879);g.fill(x+sx,y+sy,x+sx+17,y+sy+17,0xff152d35);}
    @Override protected void drawBackground(DrawContext graphics,float delta,int mouseX,int mouseY){
        graphics.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xff182d37);
        graphics.fill(x+2,y+2,x+backgroundWidth-2,y+backgroundHeight-2,0xff29434b);
        graphics.fill(x+8,y+16,x+199,y+128,0xff142b34);
        graphics.fill(x+8,y+71,x+199,y+72,0xff56b7ba);
        for(var slot:handler.slots)slot(graphics,slot.x,slot.y);
        graphics.fill(x+228,y+74,x+232,y+93,0xff78d9cd);
        graphics.fill(x+224,y+91,x+236,y+95,0xff78d9cd);
    }
    @Override protected void drawForeground(DrawContext graphics,int mouseX,int mouseY){
        graphics.drawText(textRenderer,textRenderer.trimToWidth(title.getString(),232),12,6,0xffe6f3ed,false);
        graphics.drawText(textRenderer,playerInventoryTitle,playerInventoryTitleX,playerInventoryTitleY,0xffc2d9d0,false);
        var hint=Text.translatable("lamp.guogaology.input_hint");
        graphics.drawText(textRenderer,textRenderer.trimToWidth(hint.getString(),232),12,132,0xffb9d3cb,false);
    }
}
