package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.client.recipebook.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.client.ClientRecipeBook;
import net.fabricmc.loader.api.FabricLoader;

/** Actual synchronized recipe-book UI checks in a disposable native26.2 session. Never creates fake collections. */
public final class RecipeBook065Checks {
    private static int phase,ticks,checks;
    private static boolean requested;
    private static volatile boolean captured;
    private static volatile Throwable captureFailure;
    private static Double savedMouseX,savedMouseY;
    private static final JsonObject report=new JsonObject();
    private RecipeBook065Checks(){}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("RecipeBook065: "+message);}
    private static Object field(Object owner,String name)throws ReflectiveOperationException{
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static RecipeBookComponent<?> component(AbstractRecipeBookScreen<?> screen)throws ReflectiveOperationException{return (RecipeBookComponent<?>)field(screen,"recipeBookComponent");}
    @SuppressWarnings("unchecked") private static List<RecipeBookTabButton> tabs(RecipeBookComponent<?> book)throws ReflectiveOperationException{return (List<RecipeBookTabButton>)field(book,"tabButtons");}
    @SuppressWarnings("unchecked") private static List<RecipeCollection> page(RecipeBookComponent<?> book)throws ReflectiveOperationException{return (List<RecipeCollection>)field(field(book,"recipeBookPage"),"recipeCollections");}
    private static Set<String> outputs(Minecraft client,List<RecipeCollection> collections){
        var context=SlotDisplayContext.fromLevel(client.level);var ids=new TreeSet<String>();
        for(var collection:collections)for(var entry:collection.getRecipes())for(var stack:entry.resultItems(context))ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return ids;
    }
    private static RecipeBookTabButton tab(RecipeBookComponent<?> book,Object category)throws ReflectiveOperationException{
        return tabs(book).stream().filter(button->button.getCategory()==category).findFirst().orElseThrow(()->new AssertionError("Missing tab "+category));
    }
    private static void select(RecipeBookComponent<?> book,Object category)throws ReflectiveOperationException{
        var button=tab(book,category);check(button.visible,"requested native tab is visible "+category);
        check(book.mouseClicked(new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(0,0)),false),"native mouse handler accepts tab "+category);
        check(((RecipeBookTabButton)field(book,"selectedTab")).getCategory()==category,"native selected tab changes "+category);
    }
    @SuppressWarnings("unchecked") private static void filtering(RecipeBookComponent<?> book,Minecraft client,boolean desired)throws ReflectiveOperationException{
        if(client.player.getRecipeBook().isFiltering(RecipeBookType.CRAFTING)!=desired){
            var button=(CycleButton<Boolean>)field(book,"filterButton");button.onPress(new MouseButtonInfo(0,0));
        }
        check(client.player.getRecipeBook().isFiltering(RecipeBookType.CRAFTING)==desired,"native craftable filter setting changes");
    }
    private static void search(RecipeBookComponent<?> book,String text)throws ReflectiveOperationException{
        ((EditBox)field(book,"searchBox")).setValue(text);
        var method=RecipeBookComponent.class.getDeclaredMethod("checkSearchStringUpdate");method.setAccessible(true);method.invoke(book);
    }
    private static void mousePosition(Minecraft client,double x,double y)throws ReflectiveOperationException{
        var xField=net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");var yField=net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
        xField.setAccessible(true);yField.setAccessible(true);xField.setDouble(client.mouseHandler,x);yField.setDouble(client.mouseHandler,y);
    }
    private static void hover(Minecraft client,RecipeBookTabButton button)throws ReflectiveOperationException{
        // Only update this hidden test client's internal mouse coordinates. Never move/grab the operating-system cursor.
        if(savedMouseX==null){savedMouseX=client.mouseHandler.xpos();savedMouseY=client.mouseHandler.ypos();}
        var window=client.getWindow();double x=button.getX()+button.getWidth()/2.0,y=button.getY()+button.getHeight()/2.0;
        mousePosition(client,x*window.getScreenWidth()/window.getGuiScaledWidth(),y*window.getScreenHeight()/window.getGuiScaledHeight());
    }
    private static void tooltip(RecipeBookTabButton button)throws ReflectiveOperationException{
        check(button.isHovered(),"native frame marks custom tab hovered "+button.getCategory());
        var holder=(WidgetTooltipHolder)field(button,"tooltip");check(holder.get()!=null,"native widget owns a tooltip "+button.getCategory());
        check((boolean)field(holder,"wasDisplayed"),"native tooltip holder requested tooltip rendering "+button.getCategory());
        var message=(net.minecraft.network.chat.Component)field(holder.get(),"message");
        check(message.getString().equals(button.getMessage().getString()),"native tooltip uses localized tab title "+message.getString());
    }
    private static void geometry(RecipeBookComponent<?> book,int width,int height)throws ReflectiveOperationException{
        var visible=tabs(book).stream().filter(button->button.visible).toList();int previousBottom=-1;
        for(var button:visible){
            check(button.getX()>=0&&button.getX()+button.getWidth()<=width,"tab horizontal bounds "+button.getCategory());
            check(button.getY()>=0&&button.getY()+button.getHeight()<=height,"tab vertical bounds "+button.getCategory());
            check(button.getY()>=previousBottom,"native tab hit boxes do not overlap");previousBottom=button.getY()+button.getHeight();
        }
        check(visible.stream().anyMatch(b->b.getCategory()==SectionRecipeBookCategory.CORES),"core tab remains on screen");
        check(visible.stream().anyMatch(b->b.getCategory()==SectionRecipeBookCategory.GGG),"GGG tab remains on screen");
    }
    private static void memberships(Minecraft client,RecipeBookComponent<?> component)throws ReflectiveOperationException{
        var book=client.player.getRecipeBook();var all=book.getCollection(SearchRecipeBookCategory.CRAFTING);
        var cores=book.getCollection(SectionRecipeBookCategory.CORES);var ggg=book.getCollection(SectionRecipeBookCategory.GGG);
        check(!all.isEmpty()&&!cores.isEmpty()&&!ggg.isEmpty(),"real synchronized collections exist");
        for(var collection:cores)check(all.stream().anyMatch(original->original==collection),"core reuses native collection identity");
        for(var collection:ggg)check(all.stream().anyMatch(original->original==collection),"GGG reuses native collection identity");
        var coreIds=outputs(client,cores);check(coreIds.size()==28,"all28regional/ordinal core forms are in core page: "+coreIds);
        for(String id:coreIds)check(RecipeBookSections.isCore(id),"only cores in core section "+id);
        var gggIds=outputs(client,ggg);for(String id:gggIds){check(!RecipeBookSections.isCore(id),"GGG excludes cores "+id);check(!RecipeBookSections.isCommonWood(id),"GGG excludes common wood "+id);}
        check(gggIds.contains("minecraft:cobblestone"),"mod number-stone to vanilla cobblestone conversion is present");
        check(gggIds.contains("minecraft:tnt"),"mod nuke-mushroom to vanilla TNT conversion is present");
        check(!gggIds.contains("minecraft:birch_planks")&&!gggIds.contains("minecraft:dark_oak_planks"),"mod tree to vanilla wood conversions stay outside GGG");
        check(!gggIds.contains("minecraft:crafting_table")&&!gggIds.contains("minecraft:stick"),"vanilla recipes accepting mod wood tags do not enter GGG");
        for(String id:List.of("guogaology:enhancement_table","guogaology:compute_chip","guogaology:omega_pickaxe"))check(gggIds.contains(id),"mod-specific recipe present "+id);
        check(tabs(component).size()==7,"five native categories plus two client categories");
        check(tab(component,SectionRecipeBookCategory.CORES).getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("gui.guogaology.recipebook.cores").getString()),"core label is localized");
        String brandTitle=tab(component,SectionRecipeBookCategory.GGG).getMessage().getString();String language=client.getLanguageManager().getSelected();
        check(brandTitle.equals(language.equals("zh_cn")?"果糕逻辑":"Guogaology"),"brand page uses the requested localized Mod name");
        check(((RecipeBookComponent.TabInfo)field(tab(component,SectionRecipeBookCategory.GGG),"tabInfo")).primaryIcon().isEmpty(),"brand tab uses a texture rather than a visible registered item");
        var ids=new JsonArray();coreIds.forEach(ids::add);report.add("coreOutputs",ids);report.addProperty("nativeCraftingCollections",all.size());report.addProperty("gggCollections",ggg.size());
    }
    private static Map<RecipeDisplayId,RecipeDisplayEntry> entries(List<RecipeCollection> collections){
        var result=new LinkedHashMap<RecipeDisplayId,RecipeDisplayEntry>();for(var collection:collections)for(var entry:collection.getRecipes())result.put(entry.id(),entry);return result;
    }
    private static boolean result(Minecraft client,RecipeDisplayEntry entry,Item item){return entry.resultItems(SlotDisplayContext.fromLevel(client.level)).stream().anyMatch(stack->stack.is(item));}
    private static void exclusiveCategories(Minecraft client)throws Exception{
        var book=client.player.getRecipeBook();var context=SlotDisplayContext.fromLevel(client.level);
        var search=entries(book.getCollection(SearchRecipeBookCategory.CRAFTING));var cores=entries(book.getCollection(SectionRecipeBookCategory.CORES));var brand=entries(book.getCollection(SectionRecipeBookCategory.GGG));
        var ordinary=new LinkedHashMap<RecipeDisplayId,RecipeDisplayEntry>();
        for(var category:List.of(RecipeBookCategories.CRAFTING_BUILDING_BLOCKS,RecipeBookCategories.CRAFTING_REDSTONE,RecipeBookCategories.CRAFTING_EQUIPMENT,RecipeBookCategories.CRAFTING_MISC)){
            var categoryEntries=entries(book.getCollection(category));
            for(var id:categoryEntries.keySet()){check(!cores.containsKey(id),"core display ID absent from original category "+category+" / "+id);check(!brand.containsKey(id),"brand display ID absent from original category "+category+" / "+id);}
            ordinary.putAll(categoryEntries);
        }
        for(var id:cores.keySet()){check(search.containsKey(id),"global Search retains core display ID "+id);check(!brand.containsKey(id),"core and brand display IDs are disjoint");}
        for(var id:brand.keySet())check(search.containsKey(id),"global Search retains brand display ID "+id);
        var recovered=new HashSet<>(ordinary.keySet());recovered.addAll(cores.keySet());recovered.addAll(brand.keySet());check(recovered.equals(search.keySet()),"entry partition neither loses nor duplicates Search recipes");
        RecipeDisplayEntry vanillaTnt=null,modTnt=null;
        for(var entry:search.values()){
            if(result(client,entry,Items.TNT)){
                if(RecipeBookCollections.matches(entry,RecipeBookSections.Section.GGG,context)){modTnt=entry;check(brand.containsKey(entry.id()),"nuke-mushroom TNT display belongs to brand page");check(!ordinary.containsKey(entry.id()),"nuke-mushroom TNT display leaves original categories");}
                else{vanillaTnt=entry;check(ordinary.containsKey(entry.id()),"vanilla sand/gunpowder TNT display remains in original category");}
            }
            for(var stack:entry.resultItems(context))if(RecipeBookSections.isCommonWood(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))check(ordinary.containsKey(entry.id()),"wood display remains in original category "+entry.id());
        }
        check(vanillaTnt!=null&&modTnt!=null,"real synced vanilla TNT and nuke-mushroom TNT displays are both present");
        for(Item vanilla:List.of(Items.STICK,Items.CRAFTING_TABLE,Items.BUNDLE))check(ordinary.values().stream().anyMatch(entry->result(client,entry,vanilla)),"vanilla recipe remains in concrete original category "+vanilla);
        for(var collection:book.getCollection(SearchRecipeBookCategory.CRAFTING)){
            var sections=new HashSet<Integer>();for(var entry:collection.getRecipes())sections.add(RecipeBookCollections.matches(entry,RecipeBookSections.Section.CORES,context)?1:RecipeBookCollections.matches(entry,RecipeBookSections.Section.GGG,context)?2:0);
            check(sections.size()==1,"native collections are homogeneous before indexing");
        }
        // Deliberately give three real server-synchronized entries one category/group in a separate native book.
        // Vanilla must build three indexed collections, so mixed groups cannot erase its TNT entry by output ID.
        var fixture=new ClientRecipeBook();var core=cores.values().iterator().next();
        for(var entry:List.of(vanillaTnt,modTnt,core))fixture.add(new RecipeDisplayEntry(entry.id(),entry.display(),OptionalInt.of(66066),vanillaTnt.category(),entry.craftingRequirements()));
        fixture.rebuildCollections();var fixtureSearch=entries(fixture.getCollection(SearchRecipeBookCategory.CRAFTING));var fixtureOrdinary=entries(fixture.getCollection(vanillaTnt.category()));
        check(fixtureSearch.size()==3&&fixture.getCollection(SearchRecipeBookCategory.CRAFTING).size()==3,"native mixed-group rebuild produces three searchable partitions");
        check(fixtureOrdinary.size()==1&&fixtureOrdinary.containsKey(vanillaTnt.id()),"mixed group retains only original vanilla TNT in its original tab");
        check(entries(fixture.getCollection(SectionRecipeBookCategory.CORES)).keySet().equals(Set.of(core.id())),"mixed group core is exclusive by display ID");
        check(entries(fixture.getCollection(SectionRecipeBookCategory.GGG)).keySet().equals(Set.of(modTnt.id())),"mixed group mod TNT is exclusive by display ID");
        report.addProperty("layoutVersion","0.6.6");report.addProperty("ordinaryDisplayEntries",ordinary.size());report.addProperty("globalSearchDisplayEntries",search.size());report.addProperty("mixedNativeGroupRegression",true);
        byte[] texture;try(var input=client.getResourceManager().getResource(Identifier.parse(RecipeBookSections.BRAND_ICON_TEXTURE)).orElseThrow().open()){texture=input.readAllBytes();}
        var metadataIcon=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().findPath("assets/guogaology/icon.png").orElseThrow();
        check(Arrays.equals(texture,Files.readAllBytes(metadataIcon)),"brand texture is the exact Mod metadata logo PNG");
        check(java.nio.ByteBuffer.wrap(texture,16,8).getInt()==128&&java.nio.ByteBuffer.wrap(texture,20,4).getInt()==128,"original brand PNG dimensions remain128x128");
        report.addProperty("brandLogoTexture",RecipeBookSections.BRAND_ICON_TEXTURE);report.addProperty("brandLogoSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(texture)));
    }
    private static Set<String> inventoryPage(Minecraft client,RecipeBookComponent<?> book)throws ReflectiveOperationException{
        check(((RecipeBookTabButton)field(book,"selectedTab")).getCategory()==SectionRecipeBookCategory.GGG,"inventory selected tab remains GGG");
        var shown=page(book);var allowed=client.player.getRecipeBook().getCollection(SectionRecipeBookCategory.GGG);var context=SlotDisplayContext.fromLevel(client.level);
        check(!shown.isEmpty(),"inventory GGG contains fitting native collections");var ids=new TreeSet<String>();
        for(var collection:shown){
            check(allowed.stream().anyMatch(original->original==collection),"inventory page contains an actual GGG collection");
            for(var entry:collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY)){
                check(RecipeBookCollections.matches(entry,RecipeBookSections.Section.GGG,context),"inventory visible entry passes GGG ownership filter");
                for(var stack:entry.resultItems(context)){
                    String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();ids.add(id);
                    check(RecipeBookSections.isModItem(id)||id.equals("minecraft:cobblestone")||id.equals("minecraft:tnt"),"inventory output belongs to custom recipes or the two specific vanilla conversions "+id);
                    check(!RecipeBookSections.isCore(id)&&!RecipeBookSections.isCommonWood(id),"inventory GGG output excludes cores and common woods "+id);
                }
            }
        }
        check(!ids.isEmpty()&&!ids.contains("minecraft:bundle"),"inventory GGG is a populated custom page rather than vanilla Search");return ids;
    }
    private static void next(int next){phase=next;ticks=0;requested=false;captured=false;}
    private static boolean picture(Minecraft client,String name)throws Exception{
        if(!requested&&ticks>=12){
            requested=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name+".png"));captured=true;}catch(Throwable error){captureFailure=error;}});
        }
        return captured;
    }
    /** Call only on client ticks after server QA unlocks all recipes and opens a real CraftingMenu in survival. */
    public static boolean tick(Minecraft client)throws Exception{
        if(captureFailure!=null)throw new AssertionError("RecipeBook065 screenshot",captureFailure);
        ticks++;if(phase==9)return true;
        if(phase==0){
            if(!(client.gui.screen() instanceof CraftingScreen screen)||client.player==null||client.level==null)return false;
            if(ticks<30||client.player.getRecipeBook().getCollection(SectionRecipeBookCategory.CORES).isEmpty())return false;
            var book=component(screen);if(!book.isVisible())book.toggleVisibility();book.recipesUpdated();memberships(client,book);exclusiveCategories(client);geometry(book,screen.width,screen.height);
            filtering(book,client,false);select(book,SectionRecipeBookCategory.CORES);int unfiltered=page(book).size();check(unfiltered>0,"core UI shows learned recipes");
            filtering(book,client,true);var filtered=page(book);check(!filtered.isEmpty()&&filtered.size()<unfiltered,"craftable filter removes unavailable core recipes");
            for(var collection:filtered)check(collection.hasCraftable(),"every filtered core collection is craftable");
            check(outputs(client,filtered).contains("guogaology:laver_core"),"provided real inventory enables Lv1Laver recipe");
            report.addProperty("coreCollectionsUnfiltered",unfiltered);report.addProperty("coreCollectionsCraftable",filtered.size());hover(client,tab(book,SectionRecipeBookCategory.CORES));next(1);return false;
        }
        if(phase==1){
            if(ticks>=12&&!requested)tooltip(tab(component((CraftingScreen)client.gui.screen()),SectionRecipeBookCategory.CORES));
            if(picture(client,"recipebook065-cores-craftable"))next(2);return false;
        }
        if(phase==2){
            var screen=(CraftingScreen)client.gui.screen();var book=component(screen);filtering(book,client,false);select(book,SearchRecipeBookCategory.CRAFTING);
            String stick=new ItemStack(Items.STICK).getHoverName().getString();search(book,stick);
            check(outputs(client,page(book)).contains("minecraft:stick"),"native global search still returns vanilla stick recipe");
            search(book,"");select(book,SectionRecipeBookCategory.GGG);
            String chip=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:compute_chip"))).getHoverName().getString();search(book,chip);
            check(!page(book).isEmpty()&&outputs(client,page(book)).contains("guogaology:compute_chip"),"native search index accepts the same GGG collection objects");
            search(book,"guogaology_065_no_such_recipe");check(page(book).isEmpty(),"unmatched native search clears custom page");search(book,"");check(!page(book).isEmpty(),"clearing search restores GGG");hover(client,tab(book,SectionRecipeBookCategory.GGG));next(3);return false;
        }
        if(phase==3){
            if(ticks>=12&&!requested)tooltip(tab(component((CraftingScreen)client.gui.screen()),SectionRecipeBookCategory.GGG));
            if(picture(client,"recipebook065-ggg"))next(4);return false;
        }
        if(phase==4){
            var screen=(CraftingScreen)client.gui.screen();screen.resize(320,240);var book=component(screen);if(!book.isVisible())book.toggleVisibility();book.recipesUpdated();
            check(screen.width==320&&screen.height==240,"actual native screen dimensions are320x240");check((boolean)field(book,"widthTooNarrow"),"native narrow overlay path is active");
            check((int)field(book,"xOffset")==0,"narrow recipe book keeps native zero offset");geometry(book,320,240);
            check(((RecipeBookTabButton)field(book,"selectedTab")).getCategory()==SectionRecipeBookCategory.GGG,"GGG selection survives native resize");
            select(book,SectionRecipeBookCategory.CORES);filtering(book,client,true);check(!page(book).isEmpty(),"narrow craftable core page works");
            filtering(book,client,false);search(book,"guogaology_065_no_such_recipe");check(page(book).isEmpty(),"narrow native search filters custom page");search(book,"");
            report.addProperty("narrowScreenWidth",screen.width);report.addProperty("narrowScreenHeight",screen.height);next(5);return false;
        }
        if(phase==5){if(picture(client,"recipebook065-narrow"))next(6);return false;}
        if(phase==6){client.player.closeContainer();client.gui.setScreen(new InventoryScreen(client.player));next(7);return false;}
        if(phase==7){
            if(ticks<10)return false;check(client.gui.screen() instanceof InventoryScreen,"survival inventory keeps its native2x2 book");
            var screen=(InventoryScreen)client.gui.screen();var book=component(screen);if(!book.isVisible())book.toggleVisibility();book.recipesUpdated();filtering(book,client,false);geometry(book,screen.width,screen.height);
            select(book,SectionRecipeBookCategory.CORES);check(!page(book).isEmpty(),"2x2 inventory shows fitting ordinal crystal recipe");select(book,SectionRecipeBookCategory.GGG);check(!page(book).isEmpty(),"2x2 inventory shows fitting GGG recipes");
            inventoryPage(client,book);hover(client,tab(book,SectionRecipeBookCategory.GGG));next(8);return false;
        }
        if(phase==8){
            if(ticks<12)return false;
            if(!requested){
                check(client.gui.screen() instanceof InventoryScreen,"native inventory screen survives the final render wait");
                var book=component((InventoryScreen)client.gui.screen());var ids=inventoryPage(client,book);tooltip(tab(book,SectionRecipeBookCategory.GGG));
                var shown=new JsonArray();ids.forEach(shown::add);report.add("inventoryDisplayedOutputs",shown);report.addProperty("inventorySelectedTab","GGG");report.addProperty("inventoryRenderWaitTicks",ticks);
            }
            if(!picture(client,"recipebook065-inventory-ggg"))return false;
            if(savedMouseX!=null)mousePosition(client,savedMouseX,savedMouseY);
            report.addProperty("language",client.getLanguageManager().getSelected());report.addProperty("brandPageTitle",tab(component((InventoryScreen)client.gui.screen()),SectionRecipeBookCategory.GGG).getMessage().getString());report.addProperty("customTabHoverTooltips",true);
            report.addProperty("inventoryScreenshotFile","screenshots/recipebook065-inventory-ggg.png");
            report.addProperty("checks",checks);report.addProperty("status","passed");report.addProperty("source","Server-unlocked native recipe displays; native collection identities, search tree, mouse tab handlers, filter callback, hover tooltip renderer, crafting and inventory screens.");
            Files.writeString(client.gameDirectory.toPath().resolve("recipebook065-checks.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            System.out.println("RECIPE_BOOK065_PHASE_FINISH phase=inventory-ggg-rendered language="+client.getLanguageManager().getSelected()+" renderWaitTicks="+ticks+" screenshot=recipebook065-inventory-ggg.png");
            System.out.println("RECIPE_BOOK065_CHECKS_OK checks="+checks+" native-unlocked core28/GGG/wood-exclusion/search/craftable/narrow320x240/inventory2x2");phase=9;return true;
        }
        return false;
    }
}
