package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

import java.util.List;

/** Read the actual resolved native layers, rather than treating resource JSON as rendered evidence. */
final class ItemClientRegression {
    static final List<String> ITEMS=List.of("graham","graham_flower","graham_vine","dread_sapling","laver_sapling","loquat_sapling","hell_loquat_sapling","gummy_jar");
    private static final List<ItemDisplayContext> CONTEXTS=List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIXED,
        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    private static JsonArray vec(Vector3fc v){var a=new JsonArray();a.add(v.x());a.add(v.y());a.add(v.z());return a;}
    private static JsonObject bounds(AABB b){var o=new JsonObject();o.addProperty("minX",b.minX);o.addProperty("minY",b.minY);o.addProperty("minZ",b.minZ);o.addProperty("maxX",b.maxX);o.addProperty("maxY",b.maxY);o.addProperty("maxZ",b.maxZ);o.addProperty("xSize",b.getXsize());o.addProperty("ySize",b.getYsize());o.addProperty("zSize",b.getZsize());return o;}
    private static ItemStackRenderState resolve(Minecraft c,String id,ItemDisplayContext context){var state=new ItemStackRenderState();c.getItemModelResolver().updateForLiving(state,new ItemStack(NetworkProbe.item("guogaology:"+id)),context,c.player);return state;}
    private static boolean near(float a,float b){return Math.abs(a-b)<.00001;}
    private static boolean vector(Vector3fc a,float x,float y,float z){return near(a.x(),x)&&near(a.y(),y)&&near(a.z(),z);}
    private static void expect(JsonArray issues,boolean ok,String reason){if(!ok)issues.add(reason);}
    static JsonObject inspect(Minecraft c,JsonObject server)throws Exception{
        var rows=new JsonArray();var issues=new JsonArray();
        var countField=ItemStackRenderState.class.getDeclaredField("activeLayerCount");countField.setAccessible(true);
        var layersField=ItemStackRenderState.class.getDeclaredField("layers");layersField.setAccessible(true);
        for(String id:ITEMS){
            var gui=resolve(c,id,ItemDisplayContext.GUI).getModelBoundingBox();
            for(var context:CONTEXTS){
                var state=resolve(c,id,context);var row=new JsonObject();row.addProperty("item","guogaology:"+id);row.addProperty("context",context.getSerializedName());
                expect(issues,!state.isEmpty(),id+" "+context+" native item model loaded");
                var box=state.getModelBoundingBox();row.add("transformedModelBounds",bounds(box));expect(issues,box.getYsize()>0&&Double.isFinite(box.getYsize()),id+" "+context+" nonempty native transformed model height");
                float scale=switch(context){case GROUND->.25f;case FIXED->.5f;case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND->.4f;case THIRD_PERSON_LEFT_HAND,THIRD_PERSON_RIGHT_HAND->.375f;default->1f;};
                var layers=new JsonArray();int count=countField.getInt(state);Object[] nativeLayers=(Object[])layersField.get(state);expect(issues,count>0,id+" "+context+" actual active baked layers");
                for(int i=0;i<count;i++){
                    var field=nativeLayers[i].getClass().getDeclaredField("itemTransform");field.setAccessible(true);var transform=(ItemTransform)field.get(nativeLayers[i]);
                    var layer=new JsonObject();layer.add("rotationDegrees",vec(transform.rotation()));layer.add("translationBlocks",vec(transform.translation()));layer.add("scale",vec(transform.scale()));layers.add(layer);
                    expect(issues,vector(transform.scale(),scale,scale,scale),id+" "+context+" actual layer scale "+transform.scale()+" expected "+scale);
                    if(context==ItemDisplayContext.GROUND)expect(issues,vector(transform.rotation(),0,0,0)&&vector(transform.translation(),0,3f/16,0),id+" native GROUND standard rotation/translation");
                    if(context==ItemDisplayContext.FIXED||context==ItemDisplayContext.GUI)expect(issues,vector(transform.rotation(),0,0,0)&&vector(transform.translation(),0,0,0),id+" native "+context+" original rotation/translation");
                    if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        expect(issues,vector(transform.rotation(),75,45,0)&&vector(transform.translation(),0,2.5f/16,0),id+" native third-person block transform");
                    if(context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                        expect(issues,vector(transform.rotation(),0,context.leftHand()?225:45,0)&&vector(transform.translation(),0,0,0),id+" native first-person block transform");
                }
                row.add("nativeLayers",layers);row.addProperty("expectedScale",scale);
                if(context==ItemDisplayContext.GROUND||context==ItemDisplayContext.FIXED){double ratio=box.getYsize()/gui.getYsize();row.addProperty("heightRelativeToUnchangedGui",ratio);expect(issues,Math.abs(ratio-scale)<.00001,id+" native "+context+" transformed bounds ratio "+ratio);}
                rows.add(row);
            }
        }
        var entities=new JsonArray();var actualGraham=new JsonArray();
        for(var value:server.getAsJsonArray("actualGroundItemEntities")){
            var expected=value.getAsJsonObject();var entity=c.level.getEntity(expected.get("entityId").getAsInt());boolean valid=entity instanceof ItemEntity drop&&BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString().equals(expected.get("item").getAsString());
            var row=expected.deepCopy();row.addProperty("actualRemoteItemEntityPresent",valid);entities.add(row);expect(issues,valid,"actual server ItemEntity reached client for "+expected.get("item"));
            if(valid&&expected.get("item").getAsString().equals("guogaology:graham")){
                var drop=(ItemEntity)entity;
                for(var context:CONTEXTS){
                    var state=new ItemStackRenderState();
                    if(context==ItemDisplayContext.GROUND)c.getItemModelResolver().updateForNonLiving(state,drop.getItem(),context,drop);
                    else c.getItemModelResolver().updateForLiving(state,drop.getItem(),context,c.player);
                    var actual=new JsonObject();actual.addProperty("context",context.getSerializedName());actual.addProperty("actualDropComponentsPatch",drop.getItem().getComponentsPatch().toString());actual.add("transformedModelBounds",bounds(state.getModelBoundingBox()));
                    var nominal=resolve(c,"graham",context);boolean same=state.getModelBoundingBox().equals(nominal.getModelBoundingBox());actual.addProperty("sameBoundsAsNominalModel",same);expect(issues,same,"actual survival-dropped Graham components preserve its native "+context+" bounds");actualGraham.add(actual);
                }
            }
        }
        var out=new JsonObject();out.addProperty("mode",NetworkProbe.itemRegression());out.addProperty("actualNativeModelResolver",true);out.addProperty("itemCount",ITEMS.size());out.addProperty("contextsPerItem",CONTEXTS.size());out.add("models",rows);out.add("actualGroundEntities",entities);out.add("actualSurvivalGrahamStackModels",actualGraham);out.add("issues",issues);out.addProperty("expectedBehaviorMatches",issues.isEmpty());
        return out;
    }
}
