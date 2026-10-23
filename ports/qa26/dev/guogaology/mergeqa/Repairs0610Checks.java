package dev.guogaology.mergeqa;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.*;
/** Only the explicit disposable0610 runner invokes these strict regressions. */
public final class Repairs0610Checks {
 public static boolean enabled(){return Boolean.getBoolean("guogaology.repair0610");}
 private static void write(String name,JsonObject result)throws Exception{
  result.addProperty("success",true);
  result.addProperty("version",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());
  Files.writeString(FabricLoader.getInstance().getGameDir().resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
 }
 public static void server(ServerPlayer player)throws Exception{
  player.level().getChunk(new net.minecraft.core.BlockPos(12,240,12));
  Bow0610Checks.run(player);Small0610Checks.run(player);
  if(Combat0611Checks.enabled())Combat0611Checks.run(player);
  var out=new JsonObject();out.add("wood",Wood0610Checks.run(player));
  out.addProperty("bowPickupAndSurvivalControls",true);out.addProperty("actualRecipesDropsCollision",true);
  write("repair0610-server-ok.json",out);
 }
 public static void client(Minecraft client)throws Exception{
  Bow0610Checks.client(client);
  var out=new JsonObject();out.add("wood",Wood0610Checks.client(client));
  out.addProperty("actualDrawModelControls",true);write("repair0610-client-ok.json",out);
 }
}
