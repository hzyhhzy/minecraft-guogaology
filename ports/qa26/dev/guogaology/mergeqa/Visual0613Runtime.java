package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.lamp.*;
import dev.guogaology.mining.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Opt-in disposable native visual repair evidence; no production entry point. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class Visual0613Runtime implements ClientModInitializer {
 private static final String WORLD="visual-0613-fresh";
 private static final BlockPos SHAPER=new BlockPos(0,270,0);
 private static final String[] NAMES={"omega","epsilon","gamma","true_omega"};
 private static final String[] SCENES={"bows-gui-all-stages","vanilla-first-pull0","vanilla-first-pull1","vanilla-first-pull2","omega-first-pull0","omega-first-pull1","omega-first-pull2","vanilla-third-front","omega-third-front","omega-offhand-left-first","omega-offhand-left-third","shaper-front-right","shaper-rear-left","shaper-top","shaper-gui"};
 private final JsonObject result=new JsonObject();
 private final JsonArray poseRows=new JsonArray(),selectionRows=new JsonArray(),spriteRows=new JsonArray(),pictures=new JsonArray();
 private final long deadline=System.nanoTime()+600_000_000_000L;
 private boolean opening,queued,capturing,done,audited;
 private volatile boolean ready,captured;
 private volatile Throwable failure;
 private int scene=-1,ticks,checks;
 private Path directory;
 private Field remaining;
 @Override public void onInitializeClient(){
  directory=FabricLoader.getInstance().getGameDir();
  if(!Boolean.getBoolean("guogaology.visual0613")||!Files.isRegularFile(directory.resolve("visual0613-inputs.json")))throw new IllegalStateException("Visual0613 requires explicit opt-in and disposable provenance");
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
 }
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError("Visual0613: "+why);}
 private static ItemStack bow(int tier){return tier<0?new ItemStack(Items.BOW):new ItemStack(MiningContent.BOWS[tier]);}
 private void using(Minecraft c,ItemStack stack,InteractionHand hand,HumanoidArm arm,int draw)throws Exception{
  var p=c.player;p.stopUsingItem();p.setMainArm(arm);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setItemInHand(hand,stack);
  if(draw>=0){p.startUsingItem(hand);remaining.setInt(p,stack.getUseDuration(p)-draw);}
 }
 private JsonObject pose(Minecraft c)throws Exception{
  EntityRenderer renderer=c.getEntityRenderDispatcher().getRenderer(c.player);var state=renderer.createRenderState();renderer.extractRenderState(c.player,state,0);
  check(state instanceof HumanoidRenderState,"actual humanoid render extraction");var humanoid=(HumanoidRenderState)state;
  HumanoidModel model=(HumanoidModel)((LivingEntityRenderer)renderer).getModel();model.setupAnim(humanoid);
  var out=new JsonObject();out.addProperty("rightPose",humanoid.rightArmPose.name());out.addProperty("leftPose",humanoid.leftArmPose.name());out.addProperty("using",humanoid.isUsingItem);out.addProperty("useHand",humanoid.useItemHand.name());
  var rotations=new JsonArray();for(var part:List.of(model.rightArm,model.leftArm,model.head,model.body)){rotations.add(part.xRot);rotations.add(part.yRot);rotations.add(part.zRot);}out.add("nativeModelRotations",rotations);return out;
 }
 private static boolean same(JsonArray a,JsonArray b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(Math.abs(a.get(i).getAsDouble()-b.get(i).getAsDouble())>.00001)return false;return true;}
 private String selection(Minecraft c)throws Exception{
  String type=System.getProperty("guogaology.visual0613.minecraft").equals("26.3")?"net.minecraft.client.player.FirstPersonHandsAndItems":"net.minecraft.client.renderer.ItemInHandRenderer";
  var klass=Class.forName(type);var method=klass.getDeclaredMethod("evaluateWhichHandsToRender",net.minecraft.client.player.LocalPlayer.class);method.setAccessible(true);return ((Enum<?>)method.invoke(null,c.player)).name();
 }
 private void audit(Minecraft c)throws Exception{
  remaining=LivingEntity.class.getDeclaredField("useItemRemaining");remaining.setAccessible(true);
  String actual=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();check(actual.equals(System.getProperty("guogaology.visual0613.release")),"exact production version");result.addProperty("productionVersion",actual);
  var savedMain=c.player.getMainHandItem();var savedOff=c.player.getOffhandItem();var savedArm=c.player.getMainArm();
  try{
   for(var arm:HumanoidArm.values())for(var hand:InteractionHand.values())for(int draw:new int[]{0,13,20}){
    using(c,bow(-1),hand,arm,draw);var baseline=pose(c);String expectedSelection=selection(c);check(expectedSelection.equals(hand==InteractionHand.MAIN_HAND?"RENDER_MAIN_HAND_ONLY":"RENDER_OFF_HAND_ONLY"),"native bow reference hand selection");
    for(int tier=0;tier<4;tier++){
     using(c,bow(tier),hand,arm,draw);var actualPose=pose(c);check(actualPose.get("rightPose").equals(baseline.get("rightPose"))&&actualPose.get("leftPose").equals(baseline.get("leftPose")),"same actual native BOW arm poses as vanilla");check(same(actualPose.getAsJsonArray("nativeModelRotations"),baseline.getAsJsonArray("nativeModelRotations")),"same native model limb rotations as vanilla");check(selection(c).equals(expectedSelection),"same first-person rendered hands as vanilla");
     var row=actualPose.deepCopy();row.addProperty("tier",tier+1);row.addProperty("mainArm",arm.name());row.addProperty("hand",hand.name());row.addProperty("drawTicks",draw);row.add("vanilla",baseline);poseRows.add(row);
    }
   }
   // Other held items must never leak a second hand into an actively drawn bow.
   for(int tier=0;tier<4;tier++)for(var hand:InteractionHand.values())for(int otherCase=0;otherCase<4;otherCase++){
    var other=new ItemStack(otherCase==0?Items.APPLE:otherCase==1?Items.BOW:Items.CROSSBOW);if(otherCase==3)other.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,net.minecraft.world.item.component.ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
    using(c,bow(tier),hand,HumanoidArm.RIGHT,20);c.player.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,other);String chosen=selection(c);check(chosen.equals(hand==InteractionHand.MAIN_HAND?"RENDER_MAIN_HAND_ONLY":"RENDER_OFF_HAND_ONLY"),"active ordinal draw owns one hand even with other bow/crossbow");var row=new JsonObject();row.addProperty("tier",tier+1);row.addProperty("hand",hand.name());row.addProperty("other",BuiltInRegistries.ITEM.getKey(other.getItem()).toString());row.addProperty("chargedCrossbow",otherCase==3);row.addProperty("selection",chosen);selectionRows.add(row);
   }
   using(c,new ItemStack(Items.APPLE),InteractionHand.MAIN_HAND,HumanoidArm.RIGHT,-1);c.player.setItemInHand(InteractionHand.OFF_HAND,bow(3));check(selection(c).equals("RENDER_BOTH_HANDS"),"idle ordinal bow leaves native selection alone");
   for(int tier=0;tier<4;tier++)for(int draw:new int[]{-1,0,13,18,20}){
    var stack=bow(tier);using(c,stack,InteractionHand.MAIN_HAND,HumanoidArm.RIGHT,draw);var model=new ItemStackRenderState();c.getItemModelResolver().updateForLiving(model,stack,ItemDisplayContext.GUI,c.player);check(!model.isEmpty(),"native GUI resolves");String sprite=model.pickParticleMaterial(RandomSource.create(613)).sprite().contents().name().toString();String expected="guogaology:item/"+NAMES[tier]+"_bow"+(draw<0?"":draw<13?"_pulling_0":draw<18?"_pulling_1":"_pulling_2");check(sprite.equals(expected),"native correct draw-state sprite "+sprite);var row=new JsonObject();row.addProperty("tier",tier+1);row.addProperty("drawTicks",draw);row.addProperty("sprite",sprite);spriteRows.add(row);
   }
   for(String name:List.of("omega_pickaxe","epsilon_pickaxe","gamma_pickaxe","true_omega_pickaxe","number_pickaxe")){
    var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(name)));check(!stack.isEmpty(),"tooltip pick registered");GearData.setCores(stack,List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id("sequence_core_lv3")))));var lines=Screen.getTooltipFromItem(c,stack).stream().map(Component::getString).toList();check(lines.stream().noneMatch(line->line.contains("潜行挖掘")||line.contains("关闭范围开采")||line.contains("Sneak to")||line.contains("sneak_bypass")),"removed sneak sentence absent from actual item tooltip "+name);
   }
   auditGeometry(c);
  }finally{c.player.stopUsingItem();c.player.setMainArm(savedArm);c.player.setItemSlot(EquipmentSlot.MAINHAND,savedMain);c.player.setItemSlot(EquipmentSlot.OFFHAND,savedOff);}
  result.add("nativeVanillaPoseComparisons",poseRows);result.add("nativeFirstPersonSelection",selectionRows);result.add("nativeGuiSprites",spriteRows);System.out.println("VISUAL0613_NATIVE_AUDIT_OK checks="+checks+" poses="+poseRows.size());
 }
 private void auditGeometry(Minecraft c)throws Exception{
  var block=LampShapingContent.BLOCK;var model=c.getModelManager().getBlockStateModelSet().get(block.defaultBlockState());var parts=new ArrayList<BlockStateModelPart>();model.collectParts(RandomSource.create(613),parts);check(!parts.isEmpty(),"actual baked shaper parts");var quads=new ArrayList<BakedQuad>();for(var part:parts){quads.addAll(part.getQuads(null));for(var direction:Direction.values())quads.addAll(part.getQuads(direction));}
  check(!quads.isEmpty(),"actual baked shaper quads");var rectangles=new ArrayList<double[]>();
  for(var quad:quads){double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};for(int vertex=0;vertex<4;vertex++){var v=quad.position(vertex);for(int axis=0;axis<3;axis++){double n=v.get(axis);bounds[axis]=Math.min(bounds[axis],n);bounds[axis+3]=Math.max(bounds[axis+3],n);}}int plane=-1;for(int axis=0;axis<3;axis++)if(Math.abs(bounds[axis+3]-bounds[axis])<1e-7)plane=axis;check(plane>=0,"axis-aligned baked shaper face");var row=Arrays.copyOf(bounds,7);row[6]=plane;rectangles.add(row);}
  for(int i=0;i<rectangles.size();i++)for(int j=0;j<i;j++){var a=rectangles.get(i);var b=rectangles.get(j);int axis=(int)a[6];if(axis!=(int)b[6]||Math.abs(a[axis]-b[axis])>1e-7)continue;double area=1;for(int k=0;k<3;k++)if(k!=axis)area*=Math.max(0,Math.min(a[k+3],b[k+3])-Math.max(a[k],b[k]));check(area<1e-8,"no positive-area coplanar overlap in actual baked shaper quads "+i+" / "+j+" area="+area);}
  var geometry=new JsonObject();geometry.addProperty("parts",parts.size());geometry.addProperty("bakedQuads",quads.size());geometry.addProperty("coplanarOverlapArea",0);result.add("actualNativeShaperGeometry",geometry);
 }
 @FunctionalInterface private interface Action{void run(ServerPlayer p)throws Exception;}
 private void queue(Minecraft c,Action action){queued=true;ready=false;ticks=0;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{action.run(c.getSingleplayerServer().getPlayerList().getPlayer(uuid));ready=true;}catch(Throwable error){failure=error;}});}
 private void prepare(ServerPlayer p){
  check(p!=null,"real integrated server player");p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();
  for(var q:BlockPos.betweenClosed(-7,268,-7,7,276,7))p.level().setBlock(q,q.getY()==268?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
  p.level().setBlock(SHAPER,LampShapingContent.BLOCK.defaultBlockState(),2);position(p,new Vec3(.5,270,4.5),new Vec3(.5,271,.5));
 }
 private static void position(ServerPlayer p,Vec3 camera,Vec3 target){Vec3 direction=target.subtract(camera.add(0,1.62,0));float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z)),pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)));p.teleport(new TeleportTransition(p.level(),camera,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));}
 private void setup(ServerPlayer p)throws Exception{
  p.closeContainer();p.stopUsingItem();p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
  if(scene<=10){position(p,new Vec3(.5,270,4.5),new Vec3(.5,271,-20));if(scene>0){int tier=scene<=3||scene==7?-1:3;var hand=scene==9||scene==10?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;p.setItemInHand(hand,bow(tier));p.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));p.inventoryMenu.broadcastFullState();p.startUsingItem(hand);}}
  else if(scene==11)position(p,new Vec3(2.8,270.25,3.1),Vec3.atCenterOf(SHAPER));
  else if(scene==12)position(p,new Vec3(-2.3,270.25,-2.8),Vec3.atCenterOf(SHAPER));
  else if(scene==13)position(p,new Vec3(1.9,272.8,2.1),Vec3.atCenterOf(SHAPER));
  else{position(p,new Vec3(.5,270,3.5),Vec3.atCenterOf(SHAPER));p.openMenu(new SimpleMenuProvider((id,inventory,who)->{var menu=new LampShapingMenu(id,inventory,p.level(),SHAPER);menu.getSlot(0).set(new ItemStack(GuogaologyBlocks.GUOGAO_LANTERN,64));return menu;},Component.translatable("block.guogaology.guogao_lamp_shaper")));p.containerMenu.broadcastFullState();}
 }
 private void tick(Minecraft c){
  if(done)return;try{
   if(failure!=null)throw new RuntimeException(failure);if(System.nanoTime()>deadline)throw new AssertionError("visual0613 timeout scene="+scene);
   c.options.pauseOnLostFocus=false;c.options.renderDistance().set(4);c.options.framerateLimit().set(60);c.options.fov().set(60);c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
   // F1 suppresses first-person hands as well as HUD. These photographs must
   // retain native hand rendering; hide HUD only in the GUI/world/third views.
   boolean firstPersonBow=scene>=1&&scene<=6||scene==9;
   if(c.gui.hud.isHidden()==firstPersonBow)c.gui.hud.toggle();
   if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;check(!Files.exists(directory.resolve("saves").resolve(WORLD)),"never open an existing save");var settings=new LevelSettings("Visual0613 isolated native QA",GameType.CREATIVE,LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT);c.createWorldOpenFlows().createFreshLevel(WORLD,settings,new WorldOptions(613025L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());}return;}
   if(c.player==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;
   if(scene==-1){if(!queued){queue(c,this::prepare);return;}if(!ready||++ticks<40)return;if(!audited){audit(c);audited=true;}scene=0;queued=false;ticks=0;return;}
   if(scene==SCENES.length){finish(c);return;}
   if(!queued){c.gui.setScreen(null);queue(c,this::setup);return;}if(!ready)return;
   if(scene==0){if(!(c.gui.screen() instanceof BowGrid))c.gui.setScreen(new BowGrid(c,this));}
   else if(scene<=10){
    if(c.gui.screen()!=null)return;var hand=scene==9||scene==10?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;var stack=c.player.getItemInHand(hand);int tier=scene<=3||scene==7?-1:3;if(!stack.is(bow(tier).getItem())){if(++ticks>200)throw new AssertionError("actual held bow did not synchronize");return;}
    c.player.setMainArm(scene>=9?HumanoidArm.LEFT:HumanoidArm.RIGHT);if(!c.player.isUsingItem())c.player.startUsingItem(hand);int draw=scene<=6?new int[]{0,13,20}[(scene-1)%3]:20;remaining.setInt(c.player,stack.getUseDuration(c.player)-draw);
    c.options.setCameraType(scene==7||scene==8||scene==10?CameraType.THIRD_PERSON_FRONT:CameraType.FIRST_PERSON);
   }else c.options.setCameraType(CameraType.FIRST_PERSON);
   if(scene==14&&!(c.gui.screen() instanceof dev.guogaology.client.LampShapingScreen)){if(++ticks>200)throw new AssertionError("real shaper GUI not synchronized");return;}
   if(++ticks<35)return;if(!capturing){check(!c.mouseHandler.isMouseGrabbed(),"QA never captures mouse");check(c.gui.hud.isHidden()!=firstPersonBow,"native F1 state preserves hands in first-person bow photograph");if(firstPersonBow){check(c.options.getCameraType()==CameraType.FIRST_PERSON,"native first-person camera for bow photograph");check(c.player.isUsingItem()&&c.player.getUseItem().getItem() instanceof BowItem,"actual drawn bow active for first-person photograph");}capturing=true;var folder=directory.resolve("screenshots");Files.createDirectories(folder);String file="visual0613-"+SCENES[scene]+".png";Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(file));captured=true;}catch(Throwable error){failure=error;}});pictures.add(file);return;}
   if(captured){scene++;ticks=0;queued=capturing=captured=false;}
  }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(directory.resolve("visual0613-failed.txt"),error.toString());Files.writeString(directory.resolve("visual0613-partial.json"),result.toString());}catch(Exception ignored){}c.stop();}
 }
 private void finish(Minecraft c)throws Exception{check(pictures.size()==SCENES.length,"all bounded native scene captures complete");result.addProperty("success",true);result.addProperty("minecraft",System.getProperty("guogaology.visual0613.minecraft"));result.addProperty("checks",checks);result.add("screenshots",pictures);result.add("inputs",JsonParser.parseString(Files.readString(directory.resolve("visual0613-inputs.json"))));result.addProperty("scope","Actual native draw sprite/model resolution; both main arms and both use hands; vanilla arm-pose and model-rotation controls; actual first-person hand-selection method with other held items; source-resource-free baked shaper overlap audit; GUI and real synchronized world screenshots. Fixed client draw ticks are photographic poses, not simulated user input or TCP multiplayer.");Files.writeString(directory.resolve("visual0613-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);Files.writeString(directory.resolve("visual0613-ok.txt"),"VISUAL0613_NATIVE_OK checks="+checks+" screenshots="+pictures.size()+"\n",StandardOpenOption.CREATE_NEW);done=true;c.stop();}
 private static final class BowGrid extends Screen{
  private final Minecraft client;private final Visual0613Runtime qa;
  BowGrid(Minecraft client,Visual0613Runtime qa){super(Component.literal("Native GUI bow draw stages"));this.client=client;this.qa=qa;}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
   g.fill(0,0,width,height,0xff15232e);g.text(font,"Native bow GUI: idle / pull 0 / pull 1 / full draw",20,15,0xffffffff,false);var savedMain=client.player.getMainHandItem();var savedOff=client.player.getOffhandItem();var savedArm=client.player.getMainArm();
   try{for(int row=0;row<4;row++){g.text(font,NAMES[row],20,55+row*50,0xffd5edf0,false);for(int col=0;col<4;col++){var stack=bow(row);qa.using(client,stack,InteractionHand.MAIN_HAND,HumanoidArm.RIGHT,new int[]{-1,0,13,20}[col]);int x=160+col*65,y=45+row*50;g.fill(x-3,y-3,x+35,y+35,0xff31495a);g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(2,2);g.item(stack,0,0);g.pose().popMatrix();}}}
   catch(Exception error){qa.failure=error;}finally{client.player.stopUsingItem();client.player.setMainArm(savedArm);client.player.setItemSlot(EquipmentSlot.MAINHAND,savedMain);client.player.setItemSlot(EquipmentSlot.OFFHAND,savedOff);}
  }
 }
}
