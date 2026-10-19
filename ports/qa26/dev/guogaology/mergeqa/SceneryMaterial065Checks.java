package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.survival.*;
import net.minecraft.world.level.block.Blocks;

/** Production blueprint -> registered block palette, not just numeric blueprint codes. */
public final class SceneryMaterial065Checks {
    private SceneryMaterial065Checks(){}
    public static void run(){
        var theme=SurvivalTheme.ABSENCE;var layout=SanctuaryLayout.of(theme);var site=new SurvivalStructures.Site(0,90,0,theme,0);
        int inspected=0,white=0,letters=0;
        for(int y=0;y<layout.height;y++)for(int z=-layout.depth/2;z<=layout.depth/2;z++)for(int x=-layout.width/2;x<=layout.width/2;x++){
            byte code=layout.at(x,y,z);if(code==SanctuaryLayout.KEEP||code==SanctuaryLayout.AIR)continue;
            var state=SurvivalStructures.stateFor(site,x,y,z,code);inspected++;
            if(state.is(GuogaologyBlocks.ORDINAL_BRICKS))throw new AssertionError("LHO giant still contains a native numeric brick at "+x+","+y+","+z+" code="+code);
            if(state.is(Blocks.STAINED_GLASS.white()))white++;
            if(state.is(GuogaologyBlocks.LHO_LETTER_L)||state.is(GuogaologyBlocks.LHO_LETTER_H)||state.is(GuogaologyBlocks.LHO_LETTER_O))letters++;
        }
        if(white==0||letters==0)throw new AssertionError("LHO replacement or original letter palette missing");
        System.out.println("SCENERY_MATERIAL065_OK inspected="+inspected+" numericBricks=0 whiteGlass="+white+" preservedLetters="+letters);
    }
}
