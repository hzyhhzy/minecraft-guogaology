package dev.googology.mergeqa;

import dev.googology.GoogologyBlocks;
import dev.googology.block.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Names and tooltips use the real portable state, without changing game state. */
final class VariantNames043Checks {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static ItemStack stack(Block block,BlockState state){return StatefulDecorBlock.copyAppearance(new ItemStack(block),state);}
    private static String description(ItemStack item){
        var lines=new ArrayList<Component>();
        item.getItem().appendHoverText(item,Item.TooltipContext.EMPTY,TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
        return item.getHoverName().getString()+"\n"+lines.stream().map(Component::getString).reduce("",(a,b)->a+"\n"+b);
    }
    static void run(ServerPlayer player){
        checks=0;
        for(var lamp:GoogologyBlocks.LANTERNS){
            var seen=new HashSet<String>();
            for(int emotion=0;emotion<2;emotion++)for(int part=0;part<=27;part++){
                if(part==13)continue; // The centre of the 3x3 lamp is a crystal, never a lamp tile.
                var item=stack(lamp,lamp.defaultBlockState().setValue(EmojiLanternBlock.EMOTION,emotion).setValue(EmojiLanternBlock.PART,part));
                var saved=item.copy();
                check(seen.add(description(item)),"distinct expression/tile description for "+lamp+" emotion="+emotion+" part="+part);
                check(ItemStack.matches(saved,item),"reading description preserves exact appearance components");
            }
            var whole0=stack(lamp,lamp.defaultBlockState().setValue(EmojiLanternBlock.EMOTION,0));
            var whole1=stack(lamp,lamp.defaultBlockState().setValue(EmojiLanternBlock.EMOTION,1));
            check(!whole0.getHoverName().equals(whole1.getHoverName()),"whole lantern expressions have distinct names");
        }
        for(var block:List.of(GoogologyBlocks.MOSAIC_LIGHT,GoogologyBlocks.ASTRA_WEAVE)){
            var seen=new HashSet<String>();int total=block==GoogologyBlocks.MOSAIC_LIGHT?16:64;
            for(int color=0;color<total;color++){
                var state=block.defaultBlockState();
                state=block==GoogologyBlocks.MOSAIC_LIGHT?state.setValue(MosaicLightBlock.COLOR,color):state.setValue(AstraWeaveBlock.COLOR,color);
                check(seen.add(description(stack(block,state))),"distinct palette color "+block+" "+color);
            }
        }
        var tape=GoogologyBlocks.TURING_TAPE;
        var hollow=stack(tape,tape.defaultBlockState().setValue(TuringTapeBlock.INK,false));
        var solid=stack(tape,tape.defaultBlockState().setValue(TuringTapeBlock.INK,true));
        check(!description(hollow).equals(description(solid)),"hollow and solid ink dot descriptions differ");
        for(var chime:List.of(GoogologyBlocks.LTY_YARN,GoogologyBlocks.TIANYI_FIBER,GoogologyBlocks.WHITE_FIBER)){
            var seen=new HashSet<String>();
            for(int note=0;note<8;note++)check(seen.add(description(stack(chime,chime.defaultBlockState().setValue(LtyChimeBlock.NOTE,note)))),"next chime note identified "+note);
        }
        System.out.println("VARIANT_NAMES043_CHECKS_OK checks="+checks+" six lantern colors / two expressions / 26 tiles and whole / 16+64 palette colors / tape ink / chime notes");
    }
}
