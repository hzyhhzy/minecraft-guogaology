# Guogaology 验证记录

最新验证日期：2026-10-06。

## 0.3.11：九核职责、无耐久手稿与强化模拟器

四版发行／源码JAR编译与打包通过；资源2,199项、双语387组键、132条配方、61份模板及28款晶核审计通过。14项Python回归、76项纯Java断言通过；模拟器与Java对612组配置完成9,192次数值检查，实际无头Edge中文／英文桌面及移动布局通过。

独立隐藏 fo262 /26.2 的最终 `effects-041-g` 正常保存退出，累计1,729项计数断言。本轮450项检查覆盖实际采集增产、战斗爆裂、氧气、无耐久手稿直接存取、两手副手优先、跳跃／飞行、真实状态效果、虚空／声波伤害入口、耐久掉核与禁附魔。铁砧材料修理和改名保留；真实落点往返、安全取消、旧世界景物及64格内动画／外侧静态回归通过。最终手稿界面截图已核对。

飞行竖直值与跨维度坠落阈值经函数／入口验证，未声称完成自然整段坠落或监守者AI战斗；速度为20TPS模型值。其他三版只编译／静态审计。详细证据、公式与范围见 [WORK-041.md](WORK-041.md)。

正常游戏关闭后已将同一实测包安装到 fo262，SHA-256为 `9fb7dd96318afc964f15fffbe48948f370591a32ae4d2e761f876b2763e0c086`。旧包备份到 `build/backups/fo262-before-0.3.11/`，仅一个主Mod，其他55个文件未变。未推送或发布GitHub。

All four targets compile. Hidden Minecraft 26.2 passes 1,729 counted assertions, including 450 new gameplay and manuscript checks, plus existing generation and rendering regressions. The offline simulator matches Java across 9,192 comparisons. The exact tested JAR is installed in normal fo262; the old main JAR is backed up and other 55 files remain unchanged. Natural full-descent survival and competing third-party flight grants are outside the verified runtime scope.

## 0.3.10：静态远核、强化叠加与安全传送

四版最终发行／源码包构建通过，资源2,199项、双语370组键、132条配方和14项Python回归通过。数字镐采集能力按0～4石、5～7铁、8钻石、9下界合金；工具与副手手稿挖速点数合并一次，原版挖速机制继续生效。基础最大耐久不变，装备分枝与副手序数点数相加后按D＝(1＋2p)²降低消耗概率，所有维度一致，原版耐久附魔保留。Laver经验加成移除，暂不启用替代强化功能，仍可演奏／融合，已有的可拆回。探索进度未加入。

`fixes-040-b`在独立隐藏fo262 / 26.2使用最终发行JAR通过1,511项计数断言，另通过既有海岸、实际景物、手稿保命和晶核距离检查。实际传送往返与未访问目的地成功，箱子未被覆盖；失败、超时、破框、离开和切换世界均取消，附近预热不建出口。晶核12格处18个移动内核正常动画，65／100格处18个缓存静态内核实际提交且各一批；28款烘焙体完整，序数Lv2保留原固定模型。三张实拍已检查，正常保存退出。

最终测试26.2包SHA-256为 `63499b1dacf15b10ee21576c320658dda936bd0d458a31cb82e370166e46984b`。其他三版只编译／静态检查，未启动客户端、服务器或GameTest。完整清单、测试修正和安装结果见 [WORK-040.md](WORK-040.md)。

All four final targets build. Hidden Minecraft 26.2 passes 1,511 counted assertions, existing generation regressions, bounded silent travel and actual 12/65/100-block body submissions. Durability uses combined probabilistic wear protection, Laver enhancement is disabled, and exploration progression remains deferred.

独立隐藏`voxy-040-a`使用同一最终包，实际Voxy烘焙通过208种方块、2,680种状态、2,265个模型及全部28款晶核，没有不可见模型或烘焙失败；近景截图已核对并正常退出。新鲜进程检查确认正常游戏已关闭，0.3.10已安装到fo262，与上述实测SHA-256一致。旧包备份至`build/backups/fo262-before-0.3.10/`，只有一个主Mod，其他55个文件哈希未变。

The installed Voxy baker validates all 28 core appearances and 2,265 models. The exact tested release is installed in normal fo262 after a fresh process check, with the previous main JAR backed up and the other 55 files unchanged.

## 0.3.9：固定返程框与归途签

三种返程框的原料均保持4份基材＋1块木板，每次产出1个；归途签采用木板在上、木棍在下的有序配方，产出1张。十二个同款框架加一张归途签分别从表界返回主世界、里界返回表界、地府返回里界。旧石土框架、混搭框架与错误祭品不会激活，前进仪式及自动反向门保留。

四版发行包／源码包构建与最终配方校验通过，资源图2,199项、双语369组键、132条配方和14项Python回归通过。`returns-039-b`在独立隐藏fo262 / 26.2中使用最终发行JAR，实际通过185项机制／模板、413项返程／配方、436项材质、145项维护及26项圣诞树检查，共1,205项断言，另通过既有海岸、真实景物生成与手稿保命回归。14种实际木板均能制作归途签；原版木棍和压力板配方不冲突。三套未激活门框、三套激活门面和最终归途签实拍已核对，测试正常保存退出。其他三版仅编译／资源验证，没有启动客户端、服务器或GameTest。

最终测试包与安装包SHA-256均为 `c906fb4b7fba62fb72434b3fea5ce3a543a76b73f0c50a58639b28471e548dd6`。新鲜进程检查确认正常游戏已关闭，0.3.9已安装到fo262；旧主Mod备份在 `build/backups/fo262-before-0.3.9/`，只保留一个主Mod，其他55个文件哈希未变。本版包含下方0.3.8修复；详见 [WORK-039.md](WORK-039.md)。

All four targets build. Hidden Minecraft 26.2 passes 1,205 assertions, existing world-generation regressions and clean shutdown; final frame outputs are one each and the token recipe preserves vanilla wood recipes. The exact tested release is installed in fo262, with the old main JAR backed up and other files unchanged.

## 0.3.8：C1 立体浮雕与表界圣诞树材质

已直接核对原0.2.16模型与C1展厅网格：332个长方体、1992个面，顶点集合相同。三阶空集恢复主层真实厚度、侧壁和原六面材质，不再以正视贴图替代；原74盒的180格占据、缺口、面积与体积核查通过。二／三级每个运动部件有闭合实体体积，固定外框无实质叠面。原始研究见 [C1-STRUCTURE.md](C1-STRUCTURE.md)。

四版发行包／源码包构建与校验通过，14项Python回归、资源2179项、双语365组键通过。`corrections-038-a`通过810项机制／材质／维护／树形检查及原有海岸回归；已检查原树叶与云杉树干、空集近侧视和0／10／21秒动画实拍。`voxy-038-a`烘焙205方块／2677状态／2262模型及28款晶核，所有方向可见，没有空或不可见模型；近处截图已查看。两个独立隐藏26.2客户端均正常保存退出，其他三版仅编译／资源校验。

实测与最终26.2包SHA-256均为 `3e83f941a123112751b16e7109cff5c6e5a990e6e4730c1a89b7147d15f9bd4f`。正常fo262仍在运行，安装器主动延期，当前唯一主Mod仍为0.3.7、哈希未变；不会强制关闭游戏。完整记录见 [WORK-038.md](WORK-038.md)。返程门仍使用现有规则，新固定材料方案只记录在 [RETURN-PORTAL-PROPOSAL.md](RETURN-PORTAL-PROPOSAL.md)。

All four targets build. The hidden 26.2 release passes 810 assertions and boundary regressions; the installed Voxy baker validates every model and all 28 core grades. C1 occupied volume and directional material checks pass. Installation is deferred while normal fo262 remains open.

## 0.3.7：木材清理、强化台与空集晶核

四版构建和发行包校验通过：1.21.1、1.21.11、26.2、26.3。运行验证仍只用独立隐藏 fo262 / 26.2，不捕获鼠标，不使用正常存档。资源图2,179项、双语364组键，无缺失引用、未引用资源和格式冲突；14项Python回归通过。

- `maintenance-037-a`：203项机制／模板、434项材质、145项维护、25项树形检查，共807项，另通过0.3.6海岸、真实空气间隙与混杂素面果糕回归。实际加载的高级／终极强化台和机柜配方、三种强化菜单、原版木板兼容、冥杉火焰／燃料／船实体规则，以及保留枇杷树叶的水果掉落均通过。
- 表界小圣诞树实际生成保留7／8／9／10格树干样本，整体9～12格；底部树冠更宽、顶部更窄，保留原高度和放置分布。实拍核对层叠锥形树冠、三种新强化台、冥杉木板，以及空集晶核两个独立动画时刻。
- 28款实际网格经过三角形级共面检查和独立GEOS复核，无实质同部件同向叠面，固定外壳也无实质叠面。保留505对正常双面结构，极微小坐标舍入残差单独记录；离线生成器可重复再生，二次处理不再变化。修复不依赖运行时模型加工。
- `voxy-037-a`：使用用户安装的Voxy烘焙器检查204种方块、2,676种状态、2,261种不同模型；28款晶核六向均可见，没有空模型或不可见变体。已查看近处截图，游戏正常保存退出。
- 从四版最终JAR重建配方总表：128条配方，无重复输入组、无无法解析的名称，四版逻辑配方一致。

两轮实测与安装包SHA-256均为 `ce66c432916bc5b7ba9f20da2184040526f27796ca8c3c097bf9ed6a47e7bc43`。游戏关闭后已装入正常fo262，旧包备份至 `build/backups/fo262-before-0.3.7/`，确认只有一个主Mod，其他55个文件哈希不变。详见 [WORK-037.md](WORK-037.md)。

All four targets build. Hidden Minecraft 26.2 passes 807 mechanics/material/maintenance/tree assertions plus the existing boundary regressions. The installed Voxy baker renders all 28 core grades and all current model variants. The tested release is installed in fo262; the old JAR is backed up. Other targets are compile/resource checked only.

## 0.3.6：LHO 海岸与素面果糕

四版构建及发行包校验通过；运行验证仅使用独立隐藏 fo262 / 26.2。`boundaries-036-d` 通过原有676项机制、模板与材质检查，以及新增的边界、真实区块和素面果糕检查，正常保存退出。资源图2402项、双语407组键，无缺失引用和格式冲突。

- 表界仍为 `lho_edge`，只保留真实虚空周围48格内的边缘带；原来远离虚空的边缘片区恢复正常群系及其地表景物。对16.78 km²范围做每16格一次的群系源采样，原10030个边缘点中6342个恢复为普通陆地，3688个保留为海岸，7011个原虚空点不变。这里是群系源采样，不是完整生成16.78 km²区块。临界气候参数并列时按声明顺序选择，额外检查不受此前查询顺序影响。
- 里界在现有LHO边界两侧连续削坡，不加群系，中心为真实空气。486根采样列通过4×8×4插值密度与水体检查；另外实际生成并完成装饰的一根区块列在376个检查高度上均为空气。大型景物与巨构整件避让。
- 四色素面果糕保留原磨砂半透明主体，移除全部表情浮雕。表界每块果糕独立80%素面／20%表情，允许同桌混杂；不增加桌子的果糕数量。10000次选择中8034块为素面；真实桌子四朝向生成获得293块素面、67块表情，19张桌子同时包含两种。四种新方块的创造栏、掉落、食材标签和门框用途均通过检查，游戏截图已核对。

完整清单、实现说明与检查记录见 [WORK-036.md](WORK-036.md)。只影响新生成地形和自然摆放；未修改旧存档或Voxy光照设置。

`boundaries-036-close` 补拍了较近的里界交界，确认玻璃岸与相邻大陆之间是开放空隙；三幅专项截图和正常退出检查通过。确认游戏关闭后已安装0.3.6到正常fo262，SHA-256为 `2d11105d1726ec7b775ac54b80334e32bb41858274947b7d678db43b7f369f58`，与两轮实测包相同。旧包位于 `build/backups/fo262-before-0.3.6/`，仅一个主模组，其他55个文件哈希未变。

All four targets build. Hidden Minecraft 26.2 passes the existing 676 checks plus bounded-coast, actual-air-gap, and mixed-gummy tests. Each outer-table gummy independently chooses 80% plain and 20% embossed, with the original quantity distribution preserved. Existing chunks are not rewritten.

## 0.3.5：统一命名空间、门与手稿美术、Voxy 远景

四版独立编译和发行校验通过：1.21.1、1.21.11、26.2、26.3。仅启动隐藏独立 fo262 / 26.2；其他版本只做编译与资源检查。所有注册项、资源、模板和嵌套桥接包都已归入 `googology`，发行包不含旧 `googology_outer` 命名空间，也不含 QA 辅助类。外界使用 `outer` 路径，保留里界原密度场。资源图2390项、双语403组键，无缺失引用或文案格式冲突。

- `namespace-035-a`：203项机制／模板检查和473项本轮材质／命名空间／传送门检查，共676项。61份模板、七种表界群系与生物、BMS／Laver／蘑菇云实际生成、四档矿块混搭内界门、拒收不合法材料、低成本返程及手稿保命回归均通过。实拍检查单面旧空集纹理、三套门面、ε₀矿物贴图及四种手稿图标。
- `voxy-035-final`：使用用户安装的 Voxy 0.2.19-beta、Fabric API 和 Sodium，遍历241种方块、3725种状态，并调用 Voxy 自身的软件烘焙器逐一烘焙2833种不同模型。没有空模型或不可见变体；28款晶体／晶核的六向图均有可见像素，已查看实际烘焙图和近处截图，游戏正常保存退出。
- 远景使用缓存的静态壳与内核，保留主体配色和透明度，按用户授权省略外伸动画。修复普通模型接口为空、Voxy忽略顶点颜色、玻璃先写入深度遮住内核三个问题。近处仍用完整模型和动画。没有修改 Voxy JAR，也不将它设为必要依赖。

测试辅助程序、截图与原始输出在本地忽略目录 `build/runtime-qa/`；没有使用正常存档。离线账户认证、Windows性能计数器及未安装可选附加Mod的探测警告不属于本模组故障。上述证据验证实际 Voxy 模型烘焙和近处渲染，并非全整合包光影兼容或性能基准。

已确认游戏关闭并安装0.3.5到正常fo262。安装包与最终Voxy实测包 SHA-256 一致：`4b619fa7ff61852c6e63df7780ab6937148ebc2bf7a0e068f0202ce9f96216dd`。旧0.3.4保存在 `build/backups/fo262-before-0.3.5`；正常实例只有一个主模组，其他55个文件哈希未变。

All four targets build. Hidden Minecraft 26.2 passes 676 gameplay/material assertions and the installed Voxy baker renders all 2,833 distinct block models, including every core grade. Static distant interiors remain visible through their glass; nearby animation is preserved. The tested release is installed in fo262, with the previous JAR backed up.

## 0.3.4：共用材质、目的地门面与数字交互

四版独立编译和发行校验通过：1.21.1、1.21.11、26.2、26.3。仅运行隐藏独立 fo262 / 26.2，发行 JAR 与待安装包 SHA-256 一致；使用 Fabric API、Sodium 和开发检查辅助包，未启动其他版本，未改正常存档或配置。

- 实际服务端通过原193项机制／模板检查及新增223项检查，共416项；覆盖所有导入方块／物品均属宿主注册域、创造栏收录、原版石头与原木别名、共用紫菜木材和玻璃、16种矩阵砖循环、6色彩灯边界值及真实掉落物状态、数字石头不变、各世界往返门的实际外观。
- 61份模板加载，BMS／紫菜桌／蘑菇云配置特征实际生成，七种表界群系生成和普通生物模型检查；原三层旅行、手稿保命与可逆强化回归通过。
- 实拍核对空集三级内外断续材质、三套目的地门面、橡木／深色橡木／冥杉／紫菜木系、白底数字砖及六色灯。保留原有动画几何和面数，不新增运行时实体。
- Python回归共8项：gzip跨平台模板一致性、共享注册映射、全部彩灯颜色的NBT跨版本状态转换、16种数字砖和原木轴向。
- 资源图2385个可达文件，无缺失和未引用资源。宿主384组双语键及内部19组双语键一致，无缺译／格式参数冲突；保留73条表界配方。

完整成功标记：`MERGE_MECHANICS_OK checks=193 templates=61`、`MATERIALS_034_OK checks=223`、`MERGE_TOTEM_OK`、`MERGE_FEATURES_OK`、`PRODUCTION_PORT_OK 26.2`。日志及原始截图位于本地忽略目录 `build/runtime-qa/materials-034-c`，游戏正常保存退出。首次检查中测试程序仍用旧命名空间查找蘑菇云，已改为实际ID映射并完整重跑成功。离线账户认证和Windows性能计数器警告与上版相同，未发现本模组资源或注册异常。

四版证据限于构建和静态检查，只有26.2做了本轮运行验证；不声称全部附加Mod的兼容性验证。按用户要求不提供删改ID的旧存档迁移。

All four targets build; hidden Minecraft 26.2 passes 416 assertions, real feature placements, visual checks and clean shutdown. The ordinary instance and saves are untouched during testing. See [the change checklist](WORK-034.md).

安装完成：进程检查确认Minecraft已关闭后，将26.2的0.3.4装入正常 `fo262/mods`。旧0.3.3移至工作区备份，确认恰好一个主模组JAR，其他55个文件哈希均未变。安装包与实测包均为 `57cbf60ad38a99aab55ee630ee91cf04c6dee32bb065868fc799b6c19bf988dc`。

## 0.3.3：跨平台发行校验修正

首次 GitHub Actions 四版编译均成功，1.21.1 在收集发行包时误报 `bms_aco.nbt` 不一致。已提交模板由 Windows 工具生成，CI 使用 Linux/Python 3.12；gzip 压缩头的 OS 字节可不同，不能用压缩流逐字节相等判断 NBT 是否相同。现代目标在 CI 当场转换模板，因此没有触发这一误报。

校验现改为解压后逐字节核对实际 NBT，不忽略方块、状态或坐标的差异，也不跳过 gzip CRC 验证。回归覆盖不同 OS 头、文件名／时间／压缩级别、错误方块调色板以及损坏的压缩校验和，并加入 CI。四个本地产物各 61 份模板的解压内容均与对应目标转换结果一致。仅发行工具和 CI 修改，游戏代码、贴图及已安装的 26.2 JAR 不变。

失败记录：[Actions 37261883631](https://github.com/hzyhhzy/minecraft-guogaology/actions/runs/37261883631)。

## 0.3.3：三层世界合并预览

四版独立编译与发行包检查通过：Minecraft 1.21.1、1.21.11、26.2、26.3。1.21.1 内嵌源码桥接模块的版本、入口和两处 Mixin 的 intermediary 目标已检查；现代版不包含该桥接模块。未启动 1.21.1、1.21.11 或 26.3。

运行验证使用隐藏独立 fo262 / Minecraft 26.2、Java 25、Fabric API 和 Sodium。不是整合包全部附加 Mod 的兼容性测试。正常游戏、配置、账户资料和存档未改动，窗口不捕获鼠标。最终测试正常保存并退出。

实际加载的服务端通过 181 项机制／模板断言，另通过真实致命伤害和三种景物实放检查：

- 主世界→表界→里界→地府及三次反向返回；六种祭品／门框组合实际生成完整门。
- 返程常用材料可徒手取得；破坏一块框架后完整门被移除。
- 强化台等级拒绝不适合的晶核、手稿可逆安装／拆出、飞行开启／卸除与安全缓降、创造飞行保留。
- 同一满配 Ω镐从表界 72 挖速切到里界 4608，回主世界恢复 72；没有携带深层倍率残留。
- 果糕手稿对真实致命伤害保命，背包两枚图腾变一枚；手稿和晶核仍保留。
- 61 份模板能解码；43 种 BMS 模板进一步确认含真实数字砖，防止“有尺寸却全是空气”的假通过。
- 在独立场地调用实际配置特征，确认 BMS 数字砖 13 格、紫菜桌木板 33 格、蘑菇云帽体 837 格。具体数值来自固定随机测试样本，不代表所有模板的大小。
- 七个表界群系的实际区块生成、七种普通生物模型和自然场景截图检查；BMS、紫菜树林、圣诞森林恢复可见景物。

实拍暴露并修复了 26.3 NBT 调色板 `id/properties` 与旧版 `Name/Properties` 的差异。61 份模板共 2,489 个位置条目保持原坐标；共用数字砖属性 `digit→number`、紫菜木材属性映射与状态格式按目标版本转换。两类模板原有的不同居中规则分别保留。日志未发现本模组的模型／配方／模板缺失、越界模板写入或注册错误；离线账户认证、系统性能计数器以及批量生成区块的超时 tick 警告单独记录，不作为完整性能基准。

资源审计：宿主 281 组中英文键、表界 137 组中英文键一致；主资源 1,893 个可达文件，表界 678 个可达文件，缺失引用和未引用资源均为零。73 条表界配方及其解锁奖励没有旧扽／斧铲锄／矿心引用。四个发行 JAR 逐项核对 28 款晶核模型、61 份按版本转换的模板、双语资源、许可文件和 Java 版本；不含测试辅助代码。

测试日志标记：`MERGE_MECHANICS_OK checks=181 templates=61`、`MERGE_TOTEM_OK`、`MERGE_FEATURES_OK`、`PRODUCTION_PORT_OK 26.2`。可复用辅助源码在 `ports/qa26`，运行范围和构建方式见 [BUILDING.md](BUILDING.md)。未进行旧六矿物品的存档迁移或长时间多人平衡测试；跨 Minecraft 版本的地形不承诺逐格一致。

All four targets compile and pass package checks. Only hidden Minecraft 26.2 is runtime tested. The final run validates three-layer travel, local-material return gates, passive Manuscripts, realm-dependent mining, inventory Totems, seven creatures, 61 templates, real BMS/Laver/cloud placements and clean shutdown. The original donor JAR is never embedded. See [the import architecture](OUTER-IMPORT.md) and [implemented choices for review](MERGE-REVIEW-CHECKLIST.md).

## 0.3.2：中英双语 / English and Simplified Chinese

285 项中英文键一一对应；163 个方块入口、51 个非方块物品入口、动态名称与 Java 界面引用均通过检查，没有缺译、格式参数不一致或硬编码中文提示。检查器接入四版批量构建、发行打包和 GitHub Actions。

在独立隐藏 fo262（Minecraft 26.2）中使用发行 JAR、Fabric API、Sodium 和已安装的 Mod Menu 实际切换 `en_us` → `zh_cn`。两种语言分别检查全部 285 条文案和 522 个创造栏变体，Mod Menu 名称／摘要／介绍正确切换；满 10 槽的强化界面显示正常，真实字体测量未发现面板文本溢出。已检查两种语言的创造物品栏与强化台原始截图。未操作正常实例、配置或玩家存档。

新增完整英文项目说明、生存指南和 142 条共享采集规则参考。英文创造栏标题缩短，避免覆盖翻页按钮；七个分类均检查真实字体宽度。动态数字、颜色、拼灯分片、门框名称和装备组件保留原行为。构建时保留 JSON 反斜杠转义，使双语元数据中的换行在四版 JAR 内仍是合法 JSON。

四版发行包构建与资源检查通过；与 0.3.1 逐项比较，只有两份语言文件和 `fabric.mod.json` 改变，业务字节码、模型、纹理及数据均一致。只有 26.2 启动客户端，其余三版未启动游戏。

All 285 English and Chinese entries, 163 block names, 51 non-block item names, and live UI references pass the localization audit. A hidden Minecraft 26.2 client reloads both languages and checks 522 creative variants, actual Mod Menu translations, enhancement-panel text widths, and screenshots. No normal game profile is used. Gameplay code, saved IDs, recipes, equipment balance, and world generation are unchanged.

## 0.3.1：GPL v3 许可更新

四版重新构建并通过元数据、完整 GPL 文本、Minecraft 链接例外及版权声明校验。二进制 JAR 与源码 JAR 都包含许可文件；资源引用检查通过。

四版业务 class、模型、纹理和游戏数据与 0.3.0 逐字节相同。本轮没有启动游戏，继续引用下面 0.3.0 的独立 fo262 运行验证。分发压缩包附带匹配的完整源码压缩包；0.3.0 的 MIT 标签及历史包保留，不被覆盖。

## 0.3.0 基线

## 四版发行包

| Minecraft | Java 字节码目标 | 独立源码目录构建 | 本轮运行验证 |
|---|---|---|---|
| 1.21.1 | 21 | 通过，发行 Mixin 引用检查通过 | 未启动 |
| 1.21.11 | 21 | 通过 | 未启动 |
| 26.2 | 25 | 通过 | 独立隐藏 fo262 通过 |
| 26.3 | 25 | 通过，含该版数据格式及 Sodium 朝向适配 | 未启动 |

四版从此仓库编译，不复制旧发行 JAR。打包检查验证版本、游戏依赖、Java 字节码、双语名称、图标、28 款晶核模型与资源一致性，以及发行包不含开发验证代码。四版全部业务类与原工作区已验证的 0.2.26 发行包逐字节一致；本次没有重新设计游戏机制。

运行验证按项目约定仅使用 Minecraft 26.2。其余三版的本轮证据是构建与静态发行检查，不能等同于在各自客户端完整游玩。GitHub Actions 四版工作流已配置，尚未在远端执行。

## 资源清理

删除 180 个无引用资源，共 45,307 字节。保留 1,955 个图检查可达资源，缺失引用为零。详情见 [RESOURCE-CLEANUP.json](RESOURCE-CLEANUP.json)。

- 保留所有在用数字、颜色、灯具分片与悲伤表情变体。
- 保留现代版和旧版护甲材质、GUI、记号目录及建筑数据。
- 旧展厅方案、临时飞行工具、试验、游戏存档、缓存和历史截图不进入源码仓库。

## fo262 实际发行包检查

使用独立世界与正常实例已安装的 Fabric API、Sodium、Iris 等图形组件。游戏窗口隐藏，不捕获鼠标，不复制正常账户资料或修改玩家存档。临时验证辅助程序在开发工作区，未打包进此仓库或发行 Mod。

通过内容：

- 全部 28 款晶核的放置模型、物品栏模型与动态渲染入口。
- 205 组珍藏材料的保存、玩家放置和生存回收，以及徒手序数晶体掉落。
- 两个维度的加载和实际帧缓冲截图。
- 四条传送门路径及大数世界坠入地府；准备完成的出口均在下一 tick 通过。
- 日志中无本 Mod 的模型、材质或 JSON 资源缺失；客户端正常退出。

离线验证账户产生的认证提示、系统性能计数器提示以及 Iris 在线更新超时不属于本 Mod 的资源故障。此次检查不是性能基准。

像素标识在运行验证后按最终要求缩为原表情的约 80%，居于 Ω 大圆环中央。表情借鉴终境巨树顶球的 `MosaicMotifs` 图案与配色，再按最终要求降低像素：30×30 采样、最近邻放大到图标中的 60×60，整体图标 128×128。该更改仅涉及图标；最终四版重新打包，并与已运行的 26.2 JAR 逐项比较，除图标外内容保持一致。

各版本的文件与 SHA-256 由 `tools/package_multiversion.py` 写入对应的 `build/releases/<版本>/`。
