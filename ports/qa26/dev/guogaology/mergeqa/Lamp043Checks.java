package dev.guogaology.mergeqa;

import dev.guogaology.CreativeCatalog;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.ChristmasDigitBlock;
import dev.guogaology.world.ChristmasSequences;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real registry, interaction and dropped-state checks, restricted to isolated QA worlds. */
final class Lamp043Checks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void run(ServerPlayer player){
        checks=0;var world=player.level();var pos=player.blockPosition().offset(3,0,0);
        var saved=world.getBlockState(pos);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        String[] colors={"amber","cyan","rose","lime","violet","scarlet"};
        try{
            for(int c=0;c<6;c++){
                var plain=GuogaologyBlocks.PLAIN_LIGHTS[c];var numbered=GuogaologyBlocks.SEQUENCE_LIGHTS[c];
                check(plain!=numbered&&plain.asItem()!=numbered.asItem(),"plain and numbered lamps have distinct block/item IDs");
                check(BuiltInRegistries.BLOCK.getKey(plain).getPath().equals(colors[c]+"_light"),"plain registered ID");
                var blank=ChristmasSequences.blank(c);
                check(blank.is(plain)&&blank.getProperties().isEmpty(),"plain worldgen state has no digit or color component");
                world.setBlock(pos,blank,2);blank.useWithoutItem(world,player,hit);
                check(world.getBlockState(pos)==blank,"right-clicking a plain light leaves it plain");
                var plainDrops=Block.getDrops(blank,world,pos,null,player,ItemStack.EMPTY);
                check(plainDrops.size()==1&&plainDrops.getFirst().is(plain.asItem())&&plainDrops.getFirst().getCount()==1,"plain lamp drops its own item");
                check(plainDrops.getFirst().get(DataComponents.BLOCK_STATE)==null,"plain item carries no digit component");
                check(numbered.defaultBlockState().getValue(ChristmasDigitBlock.DIGIT)==0,"numbered default is zero");
                check(ChristmasDigitBlock.DIGIT.getPossibleValues().size()==33,"exact original 0..32 numbered range");
                for(int digit=0;digit<=32;digit++){
                    var state=ChristmasSequences.light(digit,c);world.setBlock(pos,state,2);state.useWithoutItem(world,player,hit);
                    var next=world.getBlockState(pos);int expected=(digit+1)%33;
                    check(next.is(numbered)&&next.getValue(ChristmasDigitBlock.DIGIT)==expected,"numbered right-click cycles within0..32");
                    var drops=Block.getDrops(next,world,pos,null,player,ItemStack.EMPTY);
                    check(drops.size()==1&&drops.getFirst().is(numbered.asItem())&&drops.getFirst().getCount()==1,"numeric lamp drops its own item");
                    var appearance=drops.getFirst().get(DataComponents.BLOCK_STATE);
                    check(appearance!=null&&appearance.properties().get("digit").equals(Integer.toString(expected)),"numeric drop keeps edited digit");
                    check(appearance.apply(numbered.defaultBlockState()).equals(next),"dropped appearance reapplies the same placement state");
                }
                check(CreativeCatalog.entries("lighting").stream().filter(s->s.is(plain.asItem())).count()==1,"plain lamp appears once in lighting tab");
                check(CreativeCatalog.entries("lighting").stream().noneMatch(s->s.is(numbered.asItem())),"numeric lamps stay in numbers tab");
                check(CreativeCatalog.entries("numbers").stream().filter(s->s.is(numbered.asItem())).count()==33,"all33 numeric variants remain in creative inventory");
            }
            System.out.println("LAMPS_043_OK checks="+checks+" plainIDs=6 numberedVariants=198 plainRightClick=unchanged numericCycle=0..32 drops=preserved");
        }finally{world.setBlock(pos,saved,2);}
    }
}
