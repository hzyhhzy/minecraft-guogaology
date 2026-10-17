# 0.5.9 — 引导起点的实际完成条件

基线0.5.8 / `32b4120`，备份`codex/backup-0.5.8-before-guide-start`，开发分支`codex/0.5.9-guide-start`。不合并main、不push、不发Release；只在目标关闭时更新正常fo262，fo263_2不改。

- [x] 保留用户要求的登录自动完成目录根节点，命名“果糕逻辑”/Guogaology，保证新存档可以看到引导页；不再使用“展开开始”。
- [x] 第一条实际进度为“初入表界”，只以真正进入表界为完成条件；材料、激活门框及进入其他维度都不能提前完成。说明同时包含12蛋糕、水平5×5去四角、向内投苹果并穿门的提示。
- [x] 三种前进门不单独设造门成就，分别并入首次进入表界/里界/地府的说明；36节点。保留返程门制作/激活进度、原版页、背景修复和其他触发条件。
- [x] 页面Logo设计为蛋糕＋苹果；采用专用隐藏展示物品，只供进度图标，不加入创造分类/生存配方。
- [x] 同步中英文本、生成器、指南与检查。
- [x] “数字也能成器”与“自己的算力”归入走进里界后的分支，说明同步本地来源。
- [x] 四版构建，隐藏独立fo262验证目录可见、首个实际进度未完成、真正入表触发、页面背景与新图标，含第三方分页界面。
- [x] 用户确认退出后安装关闭的fo262，核对旧包备份、哈希、唯一主Mod与其他文件不变；本地提交。

## 图标制作

使用内置`image_gen`生成蛋糕与苹果的透明像素图，不修改Mod主Logo。终稿提交于`src/main/resources/assets/guogaology/textures/item/guide_emblem.png`，为64×64 RGBA；采用最近邻缩放以适配物品纹理图集，保持透明背景。原始生成图不作为运行依赖，模型与专用展示物品均在仓库内。

生成提示词：

> Use case: logo-brand. Asset type: one small square Minecraft mod advancement-tab icon, used at 16–26 pixels on screen. Primary request: a clearly recognizable Minecraft-style white-frosted block cake with a few red icing squares, together with one bright red apple with a brown stem and one small green leaf. The cake and the apple are the only elements. Design as crisp chunky pixel art on a strict 32 by 32 logical pixel grid, displayed enlarged with nearest-neighbor pixel edges. Composition: unified compact balanced square silhouette; cake slightly behind, apple overlaps its lower-right corner but does not obscure most of the cake. Plenty of contrast, familiar vanilla Minecraft item palette, limited colors and simple isometric cake shading, clean readable outline. Actual transparent background. No lettering, digits, extra symbols, emoji, faces, frame, platform, shadows beyond the objects, gradients, antialiasing or photographic texture. One icon only, not a sheet or mockup. Keep the objects large enough to read as cake plus apple at 16 pixels.

## 已完成的验证与安装

- 66项Python检查通过；中英各554个语言键，格式参数、缺失翻译、硬编码中文检查通过；资源引用无缺失、无孤立资源。生成器复跑检查通过，不会重新加入三个前进门节点和弃用文案。
- `build-all.ps1 -Offline`通过，1.21.1、1.21.11、26.2、26.3均有独立0.5.9 JAR，最终包内的36节点、背景格式、现有矿物/门框策略核验通过。仅26.2实际启动。
- 独立隐藏fo262：`advancements-059-a`为中文Paginated Advancements＋Cloth＋Sodium/Voxy，`advancements-059-b`为英文原版进度界面＋Sodium/Voxy，两轮各230项原生检查通过。
- 全新世界自动目录可见、初入表界未完成；持蛋糕/苹果、进入错误维度、仅激活前进门都不能替代对应入界条件。三条首次入界的注册触发器均通过；三条返程门实际投掷激活保留成功。原版1688个进度/配方定义、五个原版根页及本模组页均保留并可选择。
- 每轮生成9张原生截图，已查看首入前、目录Logo、里界/地府门提示等中英关键画面，确认背景正确、提示可读及蛋糕苹果图标清楚。专用展示物品不会进入七个创造分类。
- 正常fo262最初仍在运行，未触碰旧包；收到用户“已退出，可以替换”后安装0.5.9。旧0.5.8备份在`build/backups/fo262-before-0.5.9/`；唯一主Mod、55个其他模组及130个配置文件核验不变，fo263_2主Mod不变。记录`build/install-059.json`。
- 已安装26.2 JAR SHA-256：`51cc56e9f8b78a350c6af825dcc8f797df12944a9928ceefaa9f224cb9196c83`。26.3已编译，未安装到fo263_2。
