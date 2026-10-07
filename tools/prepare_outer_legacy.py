"""1.21.1 outer-world compatibility bridge, compiled in Mojang mappings.

The host remains Yarn mapped. The nested result uses intermediary names, so the
game loads one public JAR and there is no runtime translation or reflection.
Only API spellings are generated; substantive legacy behavior lives in overrides.
"""
from pathlib import Path
import re
from prepare_port_sources import adapt
R=Path(__file__).resolve().parents[1];TARGET=R/'ports/outer-1.21.1'
def legacy(s,name):
    s=adapt(s,'1.21.11',name)
    s=s.replace('net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks','net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks')
    s=s.replace('net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock','net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock')
    s=s.replace('net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT','net.minecraft.client.renderer.RenderType.cutout()').replace('net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT','net.minecraft.client.renderer.RenderType.translucent()')
    for a,b in {
        'net.minecraft.resources.Identifier':'net.minecraft.resources.ResourceLocation','Identifier':'ResourceLocation',
        '.identifier()':'.location()',
        '.getMinY()':'.getMinBuildHeight()', '.getMaxY()':'.getMaxBuildHeight()',
        '.getValue(GuogaologyMod.id(':'.get(GuogaologyMod.id(',
        '.isClientSide()':'.isClientSide',
        'EntitySpawnReason':'MobSpawnType',
        'SoundEvents.PIG_STEP.value()':'SoundEvents.PIG_STEP',
        'LevelHeightAccessor':'LevelHeightAccessor',
        '.useBlockDescriptionPrefix()':'',
        '.emissiveRendering(state->true)':'.emissiveRendering((state,world,pos)->true)',
        'dev.guogaology.mining.MiningContent.STORAGE[3]':'BuiltInRegistries.BLOCK.get(GuogaologyMod.id("true_omega_block"))',
        'dev.guogaology.GuogaologyBlocks.GUOGAO_SLICE':'BuiltInRegistries.ITEM.get(GuogaologyMod.id("fruit_cake"))',
        'import dev.guogaology.block.MosaicLightBlock;':'import net.minecraft.world.level.block.state.properties.IntegerProperty;',
        'MosaicLightBlock.COLOR':'IntegerProperty.create("color",0,15)',
        'net.minecraft.world.entity.animal.fish.WaterAnimal':'net.minecraft.world.entity.animal.WaterAnimal',
        'net.minecraft.world.entity.vehicle.boat.Boat':'net.minecraft.world.entity.vehicle.Boat',
        'net.minecraft.world.level.storage.ValueInput':'net.minecraft.nbt.CompoundTag',
        'net.minecraft.world.level.storage.ValueOutput':'net.minecraft.nbt.CompoundTag',
        'ValueInput':'CompoundTag', 'ValueOutput':'CompoundTag',
        'MobEffects.NAUSEA':'MobEffects.CONFUSION',
        '.noCollision()':'.noCollission()', '.snapTo(':'.moveTo(',
        'Block.column(8.0, 0.0, 8.0)':'Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0)',
        '.getUnitVec3i()':'.getNormal()',
        'net.minecraft.util.random.WeightedList':'net.minecraft.util.random.SimpleWeightedRandomList',
        'WeightedList':'SimpleWeightedRandomList',
        'SimpleWeightedRandomList.codec(':'SimpleWeightedRandomList.wrappedCodec(',
        '.getRandomOrThrow(var2)':'.getRandomValue(var2).orElseThrow()',
        '.getRandomOrThrow(random)':'.getRandomValue(random).orElseThrow()',
        'BlockTags.BATS_SPAWNABLE_ON':'net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.withDefaultNamespace("bats_spawnable_on"))',
        'this.delegate.getMinBuildHeight()':'this.delegate.getMinY()',
        'var1.setBlockState(var7, var5x,0)':'var1.setBlockState(var7, var5x,false)',
        'new StandingAndWallBlockItem(var6, var4, Direction.DOWN, new Properties())':'new StandingAndWallBlockItem(var6, var4, new Properties(), Direction.DOWN)',
    }.items():s=s.replace(a,b)
    s=s.replace('SimpleSimpleWeightedRandomList','SimpleWeightedRandomList')
    s=re.sub(r'BuiltInRegistries\.(BLOCK|ITEM)\.getValue\(', r'BuiltInRegistries.\1.get(',s)
    if name=='SnakeEntity.java':
        s=s.replace('protected void addAdditionalSaveData','public void addAdditionalSaveData').replace('protected void readAdditionalSaveData','public void readAdditionalSaveData').replace('getIntOr("SnakeHostile", 0)','getInt("SnakeHostile")')
    if name=='FlyYEntity.java': s=s.replace('ResourceLocation.withDefaultNamespace', 'net.minecraft.resources.ResourceLocation.withDefaultNamespace')
    if name=='ModEntities.java': s=s.replace('var1.build(var2)','var1.build(var2.location().toString())')
    if name=='ModSpawnEggs.java':s=s.replace('SpawnEggItem::new, new Properties().spawnEgg(var4)','p -> new SpawnEggItem(var4, 0x577C74, 0xCCDBBA, p), new Properties()')
    if name=='ModFoods.java':s='\n'.join(l for l in s.splitlines() if 'Consumable' not in l)
    if name=='ModItems.java':s=re.sub(r',\s*ModFoods\.\w+_CONSUMABLE','',s)
    if name=='ModBushBlock.java':s=s.replace('public class ModBushBlock extends BushBlock {','public class ModBushBlock extends BushBlock {\n   @Override protected com.mojang.serialization.MapCodec<? extends BushBlock> codec(){return simpleCodec(ModBushBlock::new);}')
    if name=='LhoChunkGenerator.java':
        s=s.replace('ChunkAccess chunk){worldSeed=seed;', 'ChunkAccess chunk,net.minecraft.world.level.levelgen.GenerationStep.Carving stage){worldSeed=seed;')
        s=s.replace('delegate.applyCarvers(world,seed,random,biomes,structures,chunk);carveLhoVoid(chunk,random);','delegate.applyCarvers(world,seed,random,biomes,structures,chunk,stage);if(stage==net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR)carveLhoVoid(chunk,random);')
    s=re.sub(r'\.setId\([^;]*?\)', '',s) if False else s
    # Only simple local key variables are passed to setId in the reconstructed source.
    s=re.sub(r'\.setId\((?:var\d+|key|var\d+\.(?:block|item)\(\))\)','',s)
    s=s.replace('new StandingAndWallBlockItem(var6, var4, Direction.DOWN, new Properties())','new StandingAndWallBlockItem(var6, var4, new Properties(), Direction.DOWN)')
    s=s.replace('new StandingAndWallBlockItem(var6, var4, Direction.DOWN, ModBlocks.woodItemProperties(var1))','new StandingAndWallBlockItem(var6, var4, ModBlocks.woodItemProperties(var1), Direction.DOWN)')
    s=re.sub(r'\.getRandomOrThrow\((\w+)\)',r'.getRandomValue(\1).orElseThrow()',s)
    s=s.replace('Monster::checkMonsterSpawnRules','(type, world, reason, pos, random) -> Monster.checkMonsterSpawnRules((EntityType)type, world, reason, pos, random)')
    s=s.replace('net.minecraft.client.model.object.boat.BoatModel','net.minecraft.client.model.BoatModel').replace('BoatModel::createBoatModel','BoatModel::createBodyModel')
    s=s.replace('net.minecraft.client.model.animal.pig.PigModel','net.minecraft.client.model.PigModel')
    entities={'Beaver':'BusyBeaverEntity','Whale':'DeepSeekWhaleEntity','Snake':'SnakeEntity','FlyY':'FlyYEntity','FruitCakeSlime':'FruitCakeSlimeEntity','FruitSlime':'FruitSlimeEntity','EvilPig':'EvilPigEntity'}
    for stem,entity in entities.items():
        if name==stem+'Model.java':
            s=s.replace('import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;',f'import dev.guogaology.outer.entity.{entity};')
            s=s.replace('extends EntityModel<LivingEntityRenderState>',f'extends net.minecraft.client.model.HierarchicalModel<{entity}>')
            s=s.replace('private static final float', 'private static final float',1)
            s=s.replace('super(var1);','super();this.rootPart=var1;')
            s=s.replace('   public static LayerDefinition', '   private final ModelPart rootPart;\n   @Override public ModelPart root(){return rootPart;}\n   public static LayerDefinition',1)
            s=s.replace('setupAnim(LivingEntityRenderState var1)',f'setupAnim({entity} entity,float walkPos,float walkSpeed,float age,float yaw,float pitch)')
            s=s.replace('super.setupAnim(var1);','rootPart.getAllParts().forEach(ModelPart::resetPose);')
            for a,b in {'var1.xRot':'pitch','var1.yRot':'yaw','var1.ageInTicks':'age','var1.walkAnimationPos':'walkPos','var1.walkAnimationSpeed':'walkSpeed'}.items():s=s.replace(a,b)
        if name==stem+'Renderer.java':
            s=s.replace('import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;','')
            s=s.replace(', LivingEntityRenderState,',',')
            s=re.sub(r'   public LivingEntityRenderState createRenderState\(\) \{\s*return new LivingEntityRenderState\(\);\s*\}','',s)
            s=s.replace('getTextureLocation(LivingEntityRenderState var1)',f'getTextureLocation({entity} var1)')
            if stem=='EvilPig':s=s.replace('EvilPigEntity, PigModel>', 'EvilPigEntity, PigModel<EvilPigEntity>>').replace('new PigModel(', 'new PigModel<>(')
    return s
def prepare():
    out=TARGET/'build/generated/sources';out.mkdir(parents=True,exist_ok=True)
    expected=set()
    for side in ('main','client'):
      base=R/f'ports/common/{side}';override=TARGET/f'src/{side}/java'
      sources={p.relative_to(base):p for p in (base/'dev/guogaology/outer').rglob('*.java')}
      if side=='main':
        for mixin in ('OuterNoiseSettingsMixin','OuterSpawnPlacementInvoker'):sources[Path(f'dev/guogaology/mixin/{mixin}.java')]=base/f'dev/guogaology/mixin/{mixin}.java'
      sources.update({p.relative_to(override):p for p in override.rglob('*.java')})
      for rel,p in sources.items():
        target=out/side/rel;target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(p.read_text('utf8') if p.is_relative_to(override) else legacy(p.read_text('utf8'),p.name),'utf8');expected.add(target)
    for p in out.rglob('*.java'):
        if p not in expected:p.unlink()
if __name__=='__main__':prepare()
