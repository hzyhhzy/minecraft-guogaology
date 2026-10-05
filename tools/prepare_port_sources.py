"""Small, explicit API spelling adapters; content and geometry stay in shared sources.

Generated copies are build output. Target-specific implementations can override a
shared class in ports/<target>/src/<main|client>/java without duplicating others.
"""
import argparse
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]


def adapt(text, target, name):
    if target == '1.21.11':
        if name in ('FruitCakeSlimeMoveControl.java','FruitSlimeMoveControl.java'):
            text=re.sub(r'extends MoveControl<[^>]+>', 'extends MoveControl',text).replace('return this.mob;', 'return (FruitSlimeEntity)this.mob;')
        if name=='EvilPigEntity.java':text=text.replace('SoundEvents.PIG_STEP.value()','SoundEvents.PIG_STEP')
        if name=='ModWoodTypes.java':text=text.replace('.soundType(SoundType.WOOD)','')
        if name=='LaverTableFeature.java':text=text.replace('Blocks.WOOL.white()','Blocks.WHITE_WOOL').replace('Blocks.WOOL.black()','Blocks.BLACK_WOOL')
        if name=='TreeSelfOverlapGuardFeature.java':text=text.replace('this.minimumSize,var5).decorators','this.minimumSize).dirt(var5).decorators')
        if name=='GoogologyClient.java':text=text.replace('ModelLayerRegistry','EntityModelLayerRegistry')
        if name=='GoogologyBoatRenderer.java':
            text=text.replace('super(var1, var3);','super(var1);\n      this.texture=var3;')
            text=text.replace('private final BoatModel model;','private final BoatModel model;\n   private final Identifier texture;\n   @Override protected net.minecraft.client.renderer.rendertype.RenderType renderType(){return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutNoCull(texture);}')
        return text
    replacements = {
        'net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup': 'net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab',
        'FabricItemGroup.builder()': 'FabricCreativeModeTab.builder()',
        'net.fabricmc.fabric.api.screenhandler.v1': 'net.fabricmc.fabric.api.menu.v1',
        'ExtendedScreenHandlerFactory': 'ExtendedMenuProvider',
        'ExtendedScreenHandlerType': 'ExtendedMenuType',
        'FuelRegistryEvents': 'FuelValueEvents',
        'recipe.assemble(input,world.registryAccess())': 'recipe.assemble(input)',
        'CompostingChanceRegistry': 'CompostableRegistry',
        'p.getTags()': 'p.entityTags()',
        'IntProvider.codec(0,64)': 'IntProviders.codec(0,64)',
        'FloatProvider.CODEC': 'FloatProviders.CODEC',
        'PayloadTypeRegistry.playS2C()': 'PayloadTypeRegistry.clientboundPlay()',
        'ServerTickEvents.END_WORLD_TICK': 'ServerTickEvents.END_LEVEL_TICK',
        'DensityFunction.HOLDER_HELPER_CODEC': 'DensityFunction.CODEC',
        'net.minecraft.world.entity.EntityType.OCELOT': 'net.minecraft.world.entity.EntityTypes.OCELOT',
        'net.minecraft.world.entity.EntityType.PARROT': 'net.minecraft.world.entity.EntityTypes.PARROT',
        'net.minecraft.world.entity.EntityType.ZOMBIE': 'net.minecraft.world.entity.EntityTypes.ZOMBIE',
        'e.getKey().x,e.getKey().z': 'e.getKey().x(),e.getKey().z()',
        '.emissiveRendering((state,world,pos)->true)': '.emissiveRendering(state->true)',
        'new net.minecraft.world.level.saveddata.SavedDataType<>("googology_portals",': 'new net.minecraft.world.level.saveddata.SavedDataType<>(dev.googology.GoogologyMod.id("portals"),',
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    for expression in ('p', 'pos', 'context.origin()'):
        for prefix in ('', 'net.minecraft.world.level.'):
            text = text.replace('new ' + prefix + 'ChunkPos(' + expression + ')', prefix + 'ChunkPos.containing(' + expression + ')')
    colors = 'WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY LIGHT_GRAY CYAN PURPLE BLUE BROWN GREEN RED BLACK'.split()
    for color in colors:
        camel = color.lower().split('_')
        method = camel[0] + ''.join(word.title() for word in camel[1:])
        text = re.sub(r'Blocks\.' + color + r'_(WOOL|CONCRETE|TERRACOTTA|STAINED_GLASS)\b', lambda m: 'Blocks.' + m[1] + '.' + method + '()', text)
    text = text.replace('Blocks.TERRACOTTA.', 'Blocks.DYED_TERRACOTTA.')
    if name in ('EnhancementMenu.java',):
        text=text.replace('ClickType','ContainerInput').replace('Items.GRAY_STAINED_GLASS_PANE','Items.STAINED_GLASS_PANE.gray()').replace('Items.LIGHT_GRAY_STAINED_GLASS_PANE','Items.STAINED_GLASS_PANE.lightGray()').replace('Items.LIME_DYE','Items.DYE.lime()')
    if name == 'EquipmentRepairRecipeMixin.java':
        text=text.replace('CraftingInput input,HolderLookup.Provider lookup,','CraftingInput input,')
    if name == 'MiningOres.java':
        text=re.sub(r'\bcp\.([xz])\b(?!\()',r'cp.\1()',text)
    if name == 'OrdinalDensity.java':
        text = text.replace('mapAll(Visitor visitor)', 'mapChildren(Visitor visitor)')
        text = text.replace('return visitor.apply(new OrdinalDensity(seedNoise.mapAll(visitor),underworld,regions));', 'return new OrdinalDensity(visitor.apply(seedNoise),underworld,regions);')
    # The old API combined system chat and action-bar messages in one boolean parameter.
    text = re.sub(r'player\.displayClientMessage\(([^;]+),\s*(true|false)\)',
                  lambda m: 'player.' + ('sendOverlayMessage' if m[2] == 'true' else 'sendSystemMessage') + '(' + m[1] + ')', text)
    if name == 'GoogologyClient.java':
        # 26.x determines block transparency from sprite alpha, including modded blocks.
        text = '\n'.join(line for line in text.splitlines() if 'BlockRenderLayerMap' not in line and 'ChunkSectionLayer' not in line) + '\n'
    if name == 'EnhancementScreen.java':
        text=text.replace('super(menu,inventory,title);imageWidth=362;imageHeight=238;', 'super(menu,inventory,title,362,238);')
        text=text.replace('GuiGraphics','GuiGraphicsExtractor').replace('void render(', 'void extractRenderState(').replace('super.render(', 'super.extractRenderState(').replace('renderTooltip(', 'extractTooltip(').replace('renderLabels(', 'extractLabels(').replace('graphics.drawString(', 'graphics.text(')
        text=text.replace('protected void renderBg(GuiGraphicsExtractor graphics,float delta,int mouseX,int mouseY){', 'public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){\n        super.extractBackground(graphics,mouseX,mouseY,delta);')
    if name == 'CoreMeshModels.java':
        for old, new in {
            'net.fabricmc.fabric.api.renderer.v1': 'net.fabricmc.fabric.api.client.renderer.v1',
            'MeshBakedGeometry': 'MeshQuadCollection',
            'net.minecraft.client.renderer.block.model.*': 'net.minecraft.client.renderer.block.dispatch.*',
            'net.minecraft.client.resources.model.*;': 'net.minecraft.client.resources.model.*;\nimport net.minecraft.client.resources.model.geometry.*;\nimport net.minecraft.client.resources.model.sprite.*;',
            'net.minecraft.world.level.BlockAndTintGetter': 'net.minecraft.client.renderer.block.BlockAndTintGetter',
            'BlockModelPart': 'BlockStateModelPart',
            'SpriteGetter sprites': 'MaterialBaker sprites',
            '.sprites()': '.materials()',
            'TextureAtlasSprite particle': 'Material.Baked particle',
            '.particleIcon()': '.particleMaterial()',
            'Material.Baked particleIcon()': 'Material.Baked particleMaterial()',
            'Map<String,TextureAtlasSprite> textures': 'Map<String,Material.Baked> textures',
            'new Material(ATLAS,Identifier.parse(e.getValue().getAsString()))': 'new Material(Identifier.parse(e.getValue().getAsString()),true)',
            'Surface(JsonObject q,TextureAtlasSprite sprite){': 'final Material.Baked material;\n        Surface(JsonObject q,Material.Baked material){\n            this.material=material;var sprite=material.sprite();',
            '.renderLayer(ChunkSectionLayer.TRANSLUCENT)': '.chunkLayer(ChunkSectionLayer.TRANSLUCENT)',
            'emitter.emit();': 'emitter.postMaterialBake(material);emitter.emit();',
            '@Override public void collectParts': '@Override public int materialFlags(){return new MeshQuadCollection(geometry.fixed).materialFlags();}\n        @Override public void collectParts',
        }.items():
            text = text.replace(old,new)
    if name == 'CoreFallbackPart.java':
        text=text.replace('net.minecraft.client.renderer.block.model.*;', 'net.minecraft.client.renderer.block.dispatch.*;\nimport net.minecraft.client.resources.model.geometry.*;\nimport net.minecraft.client.resources.model.sprite.Material;\nimport net.minecraft.client.renderer.chunk.ChunkSectionLayer;\nimport net.minecraft.client.renderer.rendertype.RenderTypes;')
        text=text.replace('BlockModelPart','BlockStateModelPart').replace('TextureAtlasSprite particle','Material.Baked particle').replace('particleIcon()','particleMaterial()')
        text=text.replace('static BakedQuad bake(JsonObject q,TextureAtlasSprite sprite){','static BakedQuad bake(JsonObject q,Material.Baked material){\n        var sprite=material.sprite();')
        text=text.replace('uv[3],-1,face,sprite,false,0)', 'uv[3],face,new BakedQuad.MaterialInfo(sprite,ChunkSectionLayer.TRANSLUCENT,RenderTypes.translucentMovingBlock(),-1,false,0))')
        text=text.replace('@Override public boolean useAmbientOcclusion()', '@Override public int materialFlags(){return BakedQuad.FLAG_TRANSLUCENT;}\n    @Override public boolean useAmbientOcclusion()')
    if name == 'CoreAnimationRenderer.java':
        text = text.replace('.client.rendering.v1.world.WorldRenderEvents', '.client.rendering.v1.level.LevelRenderEvents')
        text = text.replace('WorldRenderEvents.END_EXTRACTION', 'LevelRenderEvents.END_EXTRACTION')
        text = text.replace('.renderer.state.CameraRenderState', '.renderer.state.level.CameraRenderState')
        text = text.replace('.getBlockRenderer().getBlockModel(', '.getModelManager().getBlockStateModelSet().get(')
    if name == 'GoogologyAtmosphere.java':
        text = text.replace('.client.rendering.v1.world.World', '.client.rendering.v1.level.Level')
        text = text.replace('WorldExtractionContext', 'LevelExtractionContext').replace('WorldRenderContext','LevelRenderContext').replace('WorldRenderEvents','LevelRenderEvents')
        text = text.replace('LevelRenderEvents.AFTER_ENTITIES', 'LevelRenderEvents.COLLECT_SUBMITS')
        text = text.replace('context.worldState()', 'context.levelState()').replace('context.world()', 'context.level()').replace('context.tickCounter()', 'context.deltaTracker()')
        text = text.replace('var out = context.consumers().getBuffer(RenderTypes.debugQuads());\n        var pose = context.matrices().last();',
                            'context.submitNodeCollector().submitCustomGeometry(context.poseStack(),RenderTypes.debugQuads(),(pose,out) -> drawQuads(pose,out,quads));\n    }\n    private static void drawQuads(com.mojang.blaze3d.vertex.PoseStack.Pose pose,VertexConsumer out,List<Quad> quads) {')
    if target == '26.3':
        if name == 'CoreFallbackPart.java':
            text = text.replace('new BakedQuad.MaterialInfo(sprite,ChunkSectionLayer.TRANSLUCENT,RenderTypes.translucentMovingBlock(),-1,false,0)',
                                'BakedQuad.MaterialInfo.of(material,com.mojang.blaze3d.platform.Transparency.TRANSLUCENT,-1,Direction.UP,0)')
        if name == 'ManuscriptEffects.java':
            text = text.replace('p.hurtMarked=true;', 'p.syncVelocity=true;')
        if name == 'CoreAnimationRenderer.java':
            text = text.replace('matrices.mulPose(', 'matrices.rotate(')
        if name == 'CoreMeshModels.java':
            text = text.replace('.diffuseShade(false)', '.shadeDirectionOverride(Direction.UP)')
            # Sodium 0.9.2's packed-mesh load omits its non-serialized normalFace cache.
            # Rewriting one identical position through FRAPI makes it recompute the face,
            # without touching Sodium internals or changing any vertex/UV/colour.
            text = text.replace('geometry.fixed.outputTo(emitter);', '''
            emitter.pushTransform(quad -> {quad.pos(0,quad.x(0),quad.y(0),quad.z(0));return true;});
            try {geometry.fixed.outputTo(emitter);} finally {emitter.popTransform();}
            ''')
        if name.endswith('Block.java'):
            text = re.sub(r'    public static final MapCodec<[^\n]+simpleCodec\([^\n]+\n', '', text)
            text = re.sub(r'    @Override (?:public|protected) MapCodec<[^\n]+codec\(\)[^\n]+\n', '', text)
        text = text.replace('.isViewBlocking((state,world,pos)->false)', '.isViewBlocking((state,world,pos,bounds)->false)')
        if name == 'GoogologyCommands.java':
            text = re.sub(r'placeItemBackInInventory\((new ItemStack\([^;]+?\))\)', r'placeItemBackInInventory(\1,net.minecraft.util.Prediction.SERVER_ONLY)', text)
        if name == 'ProceduralTerrain.java':
            text = text.replace('.getValue(0,0,0)', '.get(0,0,0)')
        if name == 'GoogologyBiomeSource.java':
            text = text.replace('    @Override\n    public Holder<Biome> getNoiseBiome',
                '    @Override public net.minecraft.world.level.biome.BiomeResolver createResolver(Climate.Sampler noise){return (x,y,z)->getNoiseBiome(x,y,z,noise);}\n    public Holder<Biome> getNoiseBiome')
    return text


def prepare(target):
    out = (ROOT / 'ports' / target / 'build/generated/sources').resolve()
    assert target in ('1.21.11', '26.2', '26.3') and out.is_relative_to(ROOT.resolve())
    if out.exists():
        shutil.rmtree(out)
    for group, origin in [('main', 'common/main'), ('client', 'common/client')]:
        common = ROOT / 'ports' / origin
        specific = ROOT / 'ports' / target / 'src' / group / 'java'
        paths = {p.relative_to(common) for p in common.rglob('*.java')}
        paths.update(p.relative_to(specific) for p in specific.rglob('*.java'))
        for relative in sorted(paths):
            if target == '26.3' and relative.as_posix().startswith('dev/googology/outer/') and relative.name in ('BlockItemId.java','OuterFeatureConfig.java','TemplateEntry.java','SimpleTemplateFeature.java','OuterCaveCarver.java'):
                continue
            file = common / relative
            override = ROOT / 'ports' / target / 'src' / group / 'java' / relative
            source = override if override.is_file() else file
            output = out / group / relative
            output.parent.mkdir(parents=True, exist_ok=True)
            content=source.read_text(encoding='utf-8-sig')
            output.write_text(content if override.is_file() else adapt(content, target, file.name), encoding='utf-8')
    print(f'Prepared {target} API adapters')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('target', choices=('1.21.11', '26.2', '26.3'))
    prepare(parser.parse_args().target)
