package dev.googology.outer.world.feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
public record SimpleTemplateFeature(WeightedList<TemplateEntry> templates) implements TemplatePlaceFeature {
 public static final MapCodec<SimpleTemplateFeature> CODEC=TEMPLATES_CODEC.fieldOf("templates").xmap(SimpleTemplateFeature::new,SimpleTemplateFeature::templates);
 public MapCodec<SimpleTemplateFeature> codec(){return CODEC;}
 public boolean place(WorldGenLevel w,ChunkGenerator g,RandomSource r,BlockPos p){
  var entry=templates.getRandomOrThrow(r);
  var rotation=entry.rotations().isEmpty()?net.minecraft.world.level.block.Rotation.NONE:entry.rotations().get(r.nextInt(entry.rotations().size()));
  var template=TemplatePlaceFeature.loadTemplate(w,entry.template());if(template==null)return false;
  // Vanilla 26.3 TemplateFeature centres the template (the donor's custom
  // Laver/BMS placement helper intentionally has a different, full-size offset).
  var origin=p.offset(rotation.rotate(net.minecraft.core.Direction.WEST).getUnitVec3i().multiply(template.getSize().getX()/2))
              .offset(rotation.rotate(net.minecraft.core.Direction.NORTH).getUnitVec3i().multiply(template.getSize().getZ()/2));
  var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(rotation).setRandom(r);
  return template.placeInWorld(w,origin,origin,settings,r,3);
 }
}
