package dev.guogaology.mergeqa;

import com.mojang.authlib.GameProfile;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.EmojiLanternBlock;
import dev.guogaology.lamp.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/** Real server menu transactions and actual loot, without touching normal saves. */
final class LampShaping055Checks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ServerPlayer dummy(ServerPlayer real){
        var profile=new GameProfile(UUID.randomUUID(),"Lamp055");var p=new ServerPlayer(real.level().getServer(),real.level(),profile,ClientInformation.createDefault());
        p.connection=new ServerGamePacketListenerImpl(real.level().getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);p.setPos(real.position());return p;
    }
    private static LampShapingMenu menu(ServerPlayer p,net.minecraft.core.BlockPos pos){var menu=new LampShapingMenu(155,p.getInventory(),p.level(),pos);p.containerMenu=menu;return menu;}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    static void run(ServerPlayer real){
        checks=0;var pos=real.blockPosition().offset(3,0,0);var world=real.level();var saved=world.getBlockState(pos);
        try{
            world.setBlock(pos,LampShapingContent.BLOCK.defaultBlockState(),2);
            var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
            for(var lamp:GuogaologyBlocks.LANTERNS){
                var p=dummy(real);var menu=menu(p,pos);
                try{
                    check(LampShapingMenu.accepts(new ItemStack(lamp)),"all six lamp colors are accepted");
                    menu.getSlot(0).set(new ItemStack(lamp,2));
                    for(int choice=0;choice<LampShapes.COUNT;choice++){
                        check(menu.clickMenuButton(p,choice),"server accepts each catalog choice");
                        var state=lamp.defaultBlockState().setValue(EmojiLanternBlock.PART,LampShapes.part(choice)).setValue(EmojiLanternBlock.EMOTION,LampShapes.emotion(choice));
                        var output=menu.getSlot(1).getItem();
                        check(output.is(lamp.asItem())&&output.getCount()==1,"same color and one-to-one output");
                        check(output.get(DataComponents.BLOCK_STATE).apply(lamp.defaultBlockState()).equals(state),"menu writes exact portable form");
                        var natural=Block.getDrops(state,world,pos,null,p,ItemStack.EMPTY);
                        check(natural.size()==1&&natural.getFirst().is(lamp.asItem())&&natural.getFirst().getCount()==1,"normal mining yields exactly one same-color lamp");
                        check(natural.getFirst().get(DataComponents.BLOCK_STATE).apply(lamp.defaultBlockState()).equals(lamp.defaultBlockState()),"normal mining normalizes tile and expression");
                        var preserved=Block.getDrops(state,world,pos,null,p,silk);
                        check(preserved.size()==1&&preserved.getFirst().get(DataComponents.BLOCK_STATE).apply(lamp.defaultBlockState()).equals(state),"Silk Touch preserves actual tile and expression");
                    }
                    for(int invalid:new int[]{-1,54,Integer.MAX_VALUE})check(!menu.clickMenuButton(p,invalid),"server rejects forged choice");
                    check(menu.getSlot(0).getItem().getCount()==2,"selection alone consumes no input");
                }finally{p.closeContainer();}
            }
            check(!LampShapingMenu.accepts(new ItemStack(GuogaologyBlocks.PLAIN_LIGHTS[0]))&&!LampShapingMenu.accepts(new ItemStack(GuogaologyBlocks.SEQUENCE_LIGHTS[0])),"blank and numeric lights are not editable emoji lamps");
            transactions(real,pos);
            System.out.println("LAMP_SHAPING_055_OK checks="+checks+" colors=6 forms=54 normalSilk=324 selections=validated itemConservation=pickup/shift/fullInventory/close/death");
        }finally{world.setBlock(pos,saved,2);}
    }
    private static void transactions(ServerPlayer real,net.minecraft.core.BlockPos pos){
        var lamp=GuogaologyBlocks.GUOGAO_LANTERN.asItem();var p=dummy(real);var menu=menu(p,pos);
        try{
            check(menu.getSlot(1).getItem().isEmpty()&&!menu.clickMenuButton(p,0),"empty menu cannot output");
            check(!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),"arbitrary items cannot enter input");
            menu.getSlot(0).set(new ItemStack(lamp,7));menu.clickMenuButton(p,53);
            menu.clicked(1,0,ContainerInput.PICKUP,p);
            check(menu.getCarried().is(lamp)&&menu.getCarried().getCount()==1&&menu.getSlot(0).getItem().getCount()==6,"cursor pickup consumes exactly one input");
            menu.clicked(2,0,ContainerInput.PICKUP,p);
            check(count(p,lamp)==1&&menu.getCarried().isEmpty(),"cursor result can be placed normally");
            menu.clicked(1,0,ContainerInput.QUICK_MOVE,p);
            check(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&count(p,lamp)==7,"actual Shift gesture converts exactly all remaining inputs");
            for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            menu.getSlot(0).set(new ItemStack(lamp,3));
            check(menu.quickMoveStack(p,1).isEmpty()&&menu.getSlot(0).getItem().getCount()==3,"full inventory never consumes blocked output");
            for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);
            p.closeContainer();check(count(p,lamp)==3,"closing returns only three real input lamps");
            menu.removed(p);check(count(p,lamp)==3,"repeated close cannot duplicate input or preview");
        }finally{p.closeContainer();}
        var q=dummy(real);var gestures=menu(q,pos);gestures.getSlot(0).set(new ItemStack(lamp,5));gestures.clickMenuButton(q,1);
        var liveArea=q.getBoundingBox().inflate(10);var liveBefore=new HashSet<UUID>();for(var entity:q.level().getEntitiesOfClass(ItemEntity.class,liveArea))liveBefore.add(entity.getUUID());
        try{
            gestures.clicked(1,0,ContainerInput.THROW,q);
            check(gestures.getSlot(0).getItem().getCount()==4,"Q output consumes one source lamp");
            gestures.clicked(1,7,ContainerInput.SWAP,q);
            check(gestures.getSlot(0).getItem().getCount()==3&&q.getInventory().getItem(7).is(lamp)&&q.getInventory().getItem(7).getCount()==1,"number-key output consumes exactly one source");
            gestures.clicked(1,0,ContainerInput.PICKUP,q);gestures.clicked(2,0,ContainerInput.PICKUP_ALL,q);
            check(gestures.getSlot(0).getItem().getCount()==2&&gestures.getCarried().getCount()==2,"double click gathers owned output only and never the preview");
            q.closeContainer();int dropped=0;for(var entity:q.level().getEntitiesOfClass(ItemEntity.class,liveArea))if(!liveBefore.contains(entity.getUUID())&&entity.getItem().is(lamp))dropped+=entity.getItem().getCount();
            check(dropped==1&&count(q,lamp)==4,"cursor, hotbar swap, output throw and closing conserve all five real lamps");
        }finally{q.closeContainer();for(var entity:q.level().getEntitiesOfClass(ItemEntity.class,liveArea))if(!liveBefore.contains(entity.getUUID()))entity.discard();}
        var dead=dummy(real);var dying=menu(dead,pos);dying.getSlot(0).set(new ItemStack(lamp,5));
        var area=dead.getBoundingBox().inflate(10);var before=new HashSet<UUID>();for(var entity:dead.level().getEntitiesOfClass(ItemEntity.class,area))before.add(entity.getUUID());
        try{
            dead.setHealth(0);dying.removed(dead);dying.removed(dead);
            int dropped=0;for(var entity:dead.level().getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(entity.getUUID())&&entity.getItem().is(lamp))dropped+=entity.getItem().getCount();
            int inventory=count(dead,lamp),cursor=dying.getCarried().is(lamp)?dying.getCarried().getCount():0;
            System.out.println("LAMP055_DEATH_CLOSE world="+dropped+" inventory="+inventory+" cursor="+cursor+" pos="+dead.position());
            // Native26.2 returns container contents to a dead-but-not-removed
            // player's inventory. The death-loot inventory drop is a separate phase.
            check(dropped+inventory+cursor==5&&dying.getSlot(0).getItem().isEmpty()&&dying.getSlot(1).getItem().isEmpty(),
                    "death close conserves exactly5 real lamps: world="+dropped+" inventory="+inventory+" cursor="+cursor+" pos="+dead.position());
            dead.getInventory().dropAll();
            dropped=0;for(var entity:dead.level().getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(entity.getUUID())&&entity.getItem().is(lamp))dropped+=entity.getItem().getCount();
            check(dropped==5&&count(dead,lamp)==0,"native death-loot phase drops original input once: world="+dropped+" inventory="+count(dead,lamp)+" pos="+dead.position());
        }finally{for(var entity:dead.level().getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(entity.getUUID()))entity.discard();}
    }
}
