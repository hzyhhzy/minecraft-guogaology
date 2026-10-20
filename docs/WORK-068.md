# 0.6.8 / 0.6.1a 修复记录

2026-10-20。用户授权修复 JEI 联机兼容，并追加葛立恒等物品显示、紫菜／冥杉木制品问题；优先复用原版机制。两条独立分支，本轮不合并 main、不 push、不发布 Release。

- `codex/0.6.8-jei-compat` 基于发布0.6.7：`11360c85d0149b9e1e16cfa2cef7a9e53eba2c6a`；备份 `codex/backup-0.6.7-before-jei-compat`。调查提交 `cdba951` 仅含QA和证据。
- `codex/0.6.1a-jei-compat` 基于原0.6.1：`0a890b94fb883bc30adc31653dd172d541348368`；备份 `codex/backup-0.6.1-before-061a-jei-compat`。仅回补本轮确认修复，不带入0.6.2及以后传送、配方、生成和玩法调整。
- 没有注册ID、存档schema、装备数值或自然生成变化。fffZ/FOS、ψ/Z稳定机制不变。正常实例只按长期授权在关闭后更新 fo262；不更改 fo263_2、其他模组、配置或存档。

## 确认原因与修复

### JEI

精确0.6.1客户端＋JEI31.16.0.86连接精确0.6.0服务端，JEI原版配方回退覆盖活跃客户端注册表标签。首次拾取枝石／数字刻石后，命名标签配方展示触发 `recipe_book_add` 解码中的 `Optional.orElseThrow` 并断线；自定义木材也失去客户端斧头标签。旧保护只接受JEI31.14.0.72。调查对照见 [原始调查](INVESTIGATION-BRANCH-PICKUP-061.md)。反馈者的完整日志和服务端版本仍未知，不能声称已唯一确认其现场原因。

新Plugin读取ASM类字节，不加载JEI类型。Shape沿数据来源验证首个TagLoader来自活跃RegistryAccess.freeze，第二个来自临时WORLD_REGISTRIES；验证对应pending/apply链及消费者。只重定向首个活跃base标签加载，保留临时WORLD加载、原Mixin及Fabric权威配方同步。不再用版本号白名单。缺JEI时不读目标类；配置限定客户端，服务端没有可选客户端类依赖。

未来JEI保留受检结构即可自动接受；无法确认安全的重写结构会警告并跳过，不能保证未发布的任意实现。注入前若其他变换改变已验证结构，明确拒绝不安全重定向。

### 物品显示

葛立恒等物品引用父模型cross／自定义模型，未继承原版block物品的非GUI变换，掉落scale落到1。八个受影响物品：葛立恒、葛立恒花、葛立恒藤、紫菜／冥杉／枇杷／地府枇杷树苗、软糖罐。仅物品包装模型增加原版变换：ground0.25、fixed0.5、第一人称0.4、第三人称0.375；GUI及实际种植／放置模型、源贴图不变。旧引擎导入也保留包装模型。

### 木制品

- 紫菜门原loot未限制lower，破坏后上下半格各掉一次；改为原版lower-only，整门掉1。冥杉原表已正确，不臆造相同原因；实际上下半格与工具矩阵一并验证。
- 紫菜双层台阶原表只掉1，改为原版double掉2、单层掉1。冥杉原规则保留。
- 两种木系共四种立式／墙式告示牌没有登记到原版SIGN合法集合；旧包实际放置会经BlockEntity.validateBlockState抛IllegalStateException。改为登记原版 `minecraft:sign`，沿用文字、NBT、渲染和交互。1.21.x／26.2使用原版SignItem；26.3的编辑入口已移到SignBlock.setPlacedBy，保留其原版StandingAndWallBlockItem。
- 1.21.x没有现代template_sign模型，改用原版方块实体板面渲染；仅将已有32×32告示牌贴图按原版UV无缩放搬到64×32。26.x保留原模型。无新增木种、告示牌变体或自定义方块实体。
- 栅栏、楼梯、活板门、按钮、压力板等继续用原版类及构造包装；冥杉不可燃、其他木材原有燃烧规则不变。

## 构建与范围审计

两线最终四版离线构建及最终打包检查通过。主线111项Python测试、回补线101项通过。实际Minecraft／JEI不进入纯ASM测试JVM：14个官方JEI JAR与23个安全／不安全模拟结构共37项通过，包SHA固定。静态样本71～86中的14版本；原生联机实际选择72与86，不把静态样本冒称运行测试。

`tools/audit_068_repairs.py`递归比较精确发布基线，含1.21.1嵌套JAR；只允许逐条列出的8个物品模型／旧选择器、两张紫菜loot、ModWoodDecorBlocks、旧两版告示牌模型与UV图、26.3三个JEI类，以及版本元数据。其余Java类、配方、世界模型、源贴图、loot、ID、存档类逐字节／JSON语义一致。每线改变条目数为30／23／11／14。证据：`build/jei-068/final-delta-068.json`、`final-delta-061a.json`。061a原有37个JSON仅CRLF/LF差异另列，不掩盖语义变化。

两线最终263的三个JEI类与原Mixin逐字节相同；主线最终类也与先前独立javac／规范化指令审查的24个方法相同。证据：`build/jei-068/final-production-independent-review.json`。

| Minecraft | 最终0.6.8 SHA-256 | 最终0.6.1a SHA-256 |
|---|---|---|
| 1.21.1 | `d4c7e13b4a7d7bab78421adb8e82f7bb9c759765b5c2cb2a67413b445c7fda92` | `736f7bc718f73e0860eb029e151e3cb93dfa5cb4b5454e943059c8b69453a782` |
| 1.21.11 | `699465d5f0427f57a3e8dc9c3a394376831e9b4d4f1f2e9cfe9fc6cfd1d2c1c9` | `8647f7d5104f8131e69ac22cd65aca9e6ec7cd195b4279e51bfb5b1c4c222e58` |
| 26.2 | `cdbf300554851ccee6aba13fcd55210f1688a40da01a0b6914cc0f5b5b4b74b4` | `4450f0c1cd51ad51eac47a1ae3a9ebd18f8c6d059e16e8f8721c562acb14e942` |
| 26.3 | `c15eb5f74ee645ab6253ec5333fd35fd9760cd97faf5e9ed408ac0d85beeabff` | `fc9863898c01eda5a6e6f00070a850f7064fec9797c708febc6229de025637f5` |

## 最终原生测试

26.2最终068：隐藏、不聚焦、鼠标解锁的新世界完成574项检查、50次门／台阶真实玩家破坏、4种告示牌自动编辑与双面文字压缩NBT磁盘往返、56个原生物品显示上下文，7张原始截图均查看。葛立恒场景使用真实生存破坏产生的原始掉落实体。正常实例22250项文件哈希／存档元数据未变。证据：`build/runtime-qa/item068-262-final-a/item068-independent-native-review.json`。原runner在Java正常退出后，把Gson省略的fixture:null／seed:null误判为来源不等；修正仅允许预期null被省略，保留原始native结果并独立复核，不冒称第二次Java执行或原runner退出码0。

26.3最终22次矩阵全部通过：068为12例，061a为10例，共44次真实首次拾取、144次实际JEI查询（068为85，061a为59）。每例原Python runner、客户端和服务端退出码均为0，输入SHA、监听地址、关闭端口和正常实例保护证书全部通过；root独立完整审计 success=true、pending=0。主证据 `build/jei-068/runtime-summary.json`；真实批次退出记录 `build/network-candidate-matrix/output-final-d/summary.json` 与 `final-runner-exit-observations.json`。中间候选／旧包观察不计入22次。

| 新客户端 | 服务端 | 实际组合 | 次数 |
|---|---|---|---|
| 0.6.8 | 0.6.8 | 无／仅client／仅server／both JEI86 | 4 |
| 0.6.8 | 精确0.6.0 | 同上四种＋client-only JEI72 | 5 |
| 0.6.8 | 精确0.6.1、0.6.7 | 各client-only JEI86 | 2 |
| 0.6.8 | 0.6.8 | client-only JEI86，旧Loader/API | 1 |
| 0.6.1a | 0.6.1a | 无／仅client／仅server／both JEI86 | 4 |
| 0.6.1a | 精确0.6.0 | 同上四种＋client-only JEI72 | 5 |
| 0.6.1a | 精确0.6.1 | client-only JEI86 | 1 |

新环境为Loader0.20.0／API0.163.0+26.3；旧环境控制为Loader0.19.5／API0.161.0+26.3。官方JEI实包31.14.0.72、31.16.0.86。两线各轮换六种枝石，均用独立新世界／玩家自然拾取；每次同一连接再独立首次拾取数字刻石。两线同版none额外完成70项真实门／台阶／告示牌放置破坏和56个原生模型上下文，均0问题；Graham由真实生存破坏产生原始实体，并用实际stack检查七上下文。每线两张原始远端截图已查看：八种掉落和手持尺寸合理、GUI构图保持、告示牌原版编辑屏确实打开。两线门survival上下均掉1、creative掉0；double slab掉2；20个告示牌case的原生SIGN类型、编辑许可、双面文字压缩NBT磁盘往返全通过。

最终network QA helper SHA为 `45a1376ab382be6a92687c952e01c2192eace450f25421f2a30d2925d01d5111`；26.2 helper为 `77407801a8ca4c75cc1a20e0841a8e82124957d8d49ef52714dec63d1f9ca88c`。全部为开发专用，未进入任何主Mod包。

服务端只监听127.0.0.1，query／RCON／management关闭；每例使用新世界与新玩家。独立26.3客户端使用已知本机图形规避选项 textureFiltering=1／maxAnisotropyBit=1，仅写测试options，不修改正常配置。检查自然ItemEntity拾取、原生配方解锁／展示解码、方块／物品注册表全部标签在拾取前后及JEI界面稳定后的指纹、斧头有效性、权威配方ID集合、实际JEI查询与截图。全部结束后核对进程退出码、端口关闭、输入SHA、正常实例未变，再写证书；单一success标记不充当完整通过。

没有重新认证Connector、Voxy、其他Minecraft原生运行环境或反馈者完整旧存档。告示牌NBT测试不等同完整存档停服重开。客户端JEI保护可连接已测旧服；服务端权威的掉落／告示牌需要服务端更新。060没有同步的自定义配方无法由客户端补全；完整JEI自定义配方查询需要至少061服务端。

## 中间候选与排除记录

追加物品／木制品修复之前的两个JEI-only候选已冻结到 `build/jei-068/intermediate/pre-item-fixes/`。其中068的6次TCP、12次拾取是中间证据，不计最终矩阵。独立旧差异守卫 `tools/audit_jei_patch_delta.py`仍专门检查原JEI-only范围，不能用来给扩大范围的最终包背书。

基线告示牌observe的首轮在旧包实际SIGN校验异常后，QA预览再次访问无效BE导致服务端tick报错；保留原始失败日志，不列为最终包失败或通过。后续observe e清理无效测试块后完整采集70case：紫菜门survival掉2／creative仍掉1，冥杉门survival1／creative0；紫菜double slab1；20个告示牌放置均抛无效类型；八种GROUND scale均为1。该旧观察数值有效，旧图因QA相机掉出平台不作视觉证据；仅修开发场景平台后生成最终helper，正式两版verify与截图全部通过，未放宽断言。环境loopback拒绝、人工暂停及队列保护目录也单独排除，不能凑足矩阵次数。

## 本地交付

全部最终检查后，仅关闭的fo262安装0.6.8（SHA见上），旧0.6.7已备份；唯一主Mod与SHA复核通过，55个其他Mod不变。安装保护369个文件哈希和22489个正常存档元数据条目相同；fo263_2不变。记录 `build/install-068.json`、`build/jei-068/normal-after-install.json`。本轮两个本地分支分别提交，完整对应源码从各自最终提交git archive导出，随JAR附GPL及Minecraft链接例外、版权／第三方告知和SHA256SUMS；未合并main、未推送、未发Release。四版产物分别位于 `build/releases/0.6.8/` 与 `build/releases/0.6.1a/`，每版只装匹配Minecraft的一份。
