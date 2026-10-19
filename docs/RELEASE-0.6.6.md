# 果糕逻辑 · Guogaology v0.6.6

本版合入真正的 `main`，包含上次公开版0.6.3之后的0.6.4～0.6.6更新：晶核原料与配方调整、巨构箱奖收缩、原生配方书分类，以及精准采集经验漏洞修复。已放弃的0.6.3.1建筑矿物块试验不包含在内。

## 主要更新

- **整理配方书。** 新增「晶核」和「果糕逻辑」页，后者使用Mod主Logo。晶核及其他Mod专属配方不再重复出现在原四个具体分类；全局搜索仍可查询全部配方，普通木系和原版沙子＋火药TNT仍在原分类。支持可合成筛选、2×2背包及窄屏，无需JEI。
- **修复精准采集掉经验。** 分枝晶核的精准模式此前只改变物品掉落，经验计算仍使用原工具，导致完整矿石和经验同时掉落。现已统一两条计算使用的有效工具，覆盖普通及额外开采，也修复原版经验矿受影响的情况。普通／时运及原版刷怪笼的经验例外保留，不改写实际工具的附魔、晶核或数量。
- **调整八系配方。** 一级均以普通序数晶体为中心，对称使用对应主题原料；矩阵薄片和芽穗用量减少，分枝加入非果糕序数花，幂塔一级改用铭木与数字砖，空无需FOS与fffZ各一，紫菜改用紫菜薄片与标记点，核爆加入核爆菇，果糕使用馃槹／果糕块／凝胶，界限使用纸带／终符／集合膜。幂塔二级改为四颗一级晶核、三色TREE各一、SCG一和中央ε结晶块；其他融合规则保留。[详细配方](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.6/docs/WORK-065.md)
- **调整原料布景。** BMS薄片成排放在旗帜上沿；花园小白桦干树顶部平均约两芽；TREE／SCG枝条更短更多，单株资源约三倍而尺寸基本不变；里界FOS／fffZ改成1～4块相连小堆，表界不新增；提高表界LHO星辰石来源并补9合1配方；界限纸带密度约四倍；LHO巨构不再使用数字砖。生成变化只影响新区块，原料稳定机制不变。
- **收缩巨构箱奖。** 八类主箱固定一颗本系二级晶核，附带平均约三份一级配方成本的原料和2～4个Ω矿材料；副箱固定一颗本系一级晶核，附带平均约一份原料。每箱另有平均20个纯装饰方块；平均原料份数不代表每箱必能凑齐相应配方。普通建筑箱表不变。
- **修复凝胶掉落。** 果糕怪基础掉2个凝胶、果化怪基础掉1个，均受抢夺影响；原有死亡分裂行为保留。
- **兼容清理一级空无晶核。** 保留原ID和可读取的旧方块状态，旧物品中已无作用的稳定／锚定／玩家放置标记按需清理，其余组件和镶嵌物保持。成品晶核不再继承原料的消失行为；fffZ／FOS、ψ／Z的稳定机制不变。

## 下载与兼容

| Minecraft | Java | 对应文件 |
|---|---|---|
|1.21.1|21+|`guogaology-dimension-1.21.1-0.6.6.jar`|
|1.21.11|21+|`guogaology-dimension-1.21.11-0.6.6.jar`|
|26.2|25+|`guogaology-dimension-26.2-0.6.6.jar`|
|26.3|25+|`guogaology-dimension-26.3-0.6.6.jar`|

安装匹配游戏版本的一份主Mod和对应Fabric Loader／API。客户端与服务端应一起更新，经验修复需要服务端应用。JEI不是必需依赖。保留当前注册ID和存档格式，不扫描或重写旧地形、建筑和已生成箱子；不同Minecraft版本的存档转换不属于此次更新。

四版构建、成品资源／语言／许可／Mixin检查及88项Python检查通过。最终26.2成品在隔离fo262完成中英文回归，每次1546次真实生存破坏、6788项经验／物品断言和4009项原生GUI断言通过，重复精准回收经验为0。0.6.4的真实旧档升级、0.6.5的配方／箱奖／生成检查属于各开发阶段的验证；最终0.6.6与0.6.5的所有data资源及旧资源入口逐包核对不变。

其他三版本轮仅构建和静态检查；Connector仅核对缓存字节码，没有重跑Connector、独立服务端、JEI组合或Voxy环境。此前的兼容修复仍保留，不能将历史测试等同于本版已运行认证。完整范围见[0.6.4记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.6/docs/WORK-064.md)、[0.6.5记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.6/docs/WORK-065.md)、[0.6.6记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.6/docs/WORK-066.md)。

附件含四版JAR、完整对应源码、SHA-256校验和、安装说明及许可声明。许可为 `GPL-3.0-only WITH GPL-3.0-linking-exception`；Alice（AliceLdl）的0.5.4.1原始修改及其他第三方内容保留原始署名和适用许可。

## English

Version0.6.6 is merged into the real `main` branch. It includes the unpublished0.6.4–0.6.6 work since public0.6.3: core materials and recipes, reduced landmark chest rewards, native recipe-book sections and a Silk Touch XP fix. The abandoned0.6.3.1 resource-block inlay experiment is excluded.

- **Native recipe-book sections.** Cores and Guogaology have dedicated tabs, with the main Mod logo on the latter. Their recipes leave the four original concrete categories while remaining globally searchable. Generic wood and ordinary vanilla TNT stay in vanilla categories. Craftable filtering, inventory2×2 and narrow layouts are supported; JEI is optional.
- **Silk Touch experience repair.** Branch Core's virtual Silk Touch previously affected item loot without affecting XP, allowing intact ores plus experience. Both paths now receive the same eligible tool copy, including extra excavation and affected vanilla XP ores. Normal mining, Fortune and vanilla Spawner XP remain intact; the owned tool's enchantments, cores and count are not rewritten.
- **Core recipe and material rebalance.** Eight regional Lv1 recipes use symmetric themed ingredients around one ordinary Ordinal Crystal. Power Lv2 uses four Lv1 cores, one of each TREE color, one SCG Junction and a central Epsilon Block. Natural material displays gain BMS top rows, small-tree buds, shorter denser TREE/SCG branches, connected1–4block Inner FOS/FFFZ clusters, increased Outer Stellar Stone sources and more Boundary tapes. LHO giant buildings no longer use numeric bricks. These generation changes apply to new chunks; raw LHO stability rules remain unchanged.
- **Landmark chest rewards.** Main chests provide one regional Lv2 core, materials averaging three Lv1 recipe costs and2–4capital Ω materials. Side chests provide one regional Lv1 core and materials averaging one recipe cost. Each also supplies about20 decorative blocks. Average ingredient quantities do not guarantee complete recipe sets in every chest; ordinary-building loot tables are unchanged.
- **Gel drops and old Absence Core items.** Guogao mobs drop2gel and Guohua mobs1before Looting. The Lv1 Absence Core retains its saved ID and readable old states while obsolete item flags are normalized without discarding other components or socket contents. FFFZ/FOS and ψ/Z stability behavior is preserved.

Install exactly one matching JAR from the table above with the corresponding Fabric Loader/API. Update client and server together; XP behavior is server-authoritative. Existing IDs and saved formats are preserved, without rewriting saved terrain, buildings or existing chest contents. This update does not convert worlds between Minecraft versions.

All four targets pass build/resource/localization/license/Mixin checks and88Python tests. The final26.2 artifact passes hidden isolated Chinese and English runs, each with1546real survival block breaks,6788XP/item assertions and4009native UI assertions; repeated Silk ore recovery produces zero XP. The0.6.4upgrade and0.6.5recipe/loot/generation tests were run during those development stages. Final0.6.6data resources and existing asset entrypoints are byte-identical to0.6.5.

The other targets have build/static coverage this round. Connector bytecode was inspected, but Connector, dedicated-server, JEI-combination and Voxy runtime tests were not rerun. See the linked work records for exact evidence and limitations. Attachments include matching complete source, checksums and notices under `GPL-3.0-only WITH GPL-3.0-linking-exception`, retaining Alice (AliceLdl)'s original attribution and applicable third-party licenses.
