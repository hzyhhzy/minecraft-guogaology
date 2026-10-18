# 果糕逻辑 · Guogaology v0.6.3

本版修复巨构蓝图的材料编号冲突，并专项检查 Astra 巨构与普通楼的避让。从独立分支发布，未合并到 `main`。包含此前0.6.1的26.3可选JEI兼容和0.6.2的传送门优化；详见[0.6.2发行说明](RELEASE-0.6.2.md)。

## 修改与排查

- **修复数字“1”变成馃槹。** 内部蓝图编号49同时被固定数字1与馃槹占用，导致31处本应为数字砖的位置用了错误材料：幂塔门口2处、LHO回廊17处、地府巨树数字带12处。移除冲突编号；24个原本应有的巨树表情球中心仍为馃槹，位置、外观和发光保留。
- **Astra巨构避让专项检查。** 当前已有包含基础柱、入口楼梯和屋顶的整栋普通楼避让。3个种子、36座巨构与46栋被接受的附近普通楼没有复现相交；23个冲突原候选被拒绝或整栋移开。本版增加针对性回归，没有改动生产布局算法。这是有限样本检查，不能据此认定所有历史反馈均已解决。
- **保留资源和玩法。** 没有调整巨构资源预算、箱子、内饰密度、普通地景之间的重叠、配方或战斗系统。资源块的新设计仅在讨论中，不包含于0.6.3。

## 下载、兼容与验证

| Minecraft | Java | 对应Fabric文件 |
|---|---|---|
|1.21.1|21+|`guogaology-dimension-1.21.1-0.6.3.jar`|
|1.21.11|21+|`guogaology-dimension-1.21.11-0.6.3.jar`|
|26.2|25+|`guogaology-dimension-26.2-0.6.3.jar`|
|26.3|25+|`guogaology-dimension-26.3-0.6.3.jar`|

只安装匹配Minecraft版本的一份主Mod，搭配相应Fabric Loader/API；建议客户端和服务端同时更新。JEI不是必需依赖。

保留0.6.0–0.6.2的注册ID和存档格式，不扫描或重写旧区块。**错误材料的修复作用于新生成建筑，已经生成的错误方块不会自动替换。** 升级前备份整个世界；不同Minecraft版本之间不能靠换JAR转换存档。

四版构建、资源/打包审计和84项Python测试通过。新增几何回归覆盖八类巨构的四种旋转和547个相交区块查询。最终26.2包在隐藏、独立的新fo262副本完成233项原生材质/蓝图/放置检查及两张实景截图检查。其他三个版本本轮仅构建与静态检查；没有新增Connector、Voxy或26.3实际运行测试。完整范围及有限样本限制见[工作记录](WORK-063.md)。

附件提供四个独立JAR、完整对应源码、SHA-256校验和及许可声明。许可为 `GPL-3.0-only WITH GPL-3.0-linking-exception`；保留 Alice（AliceLdl）及其他第三方内容的原始署名和适用许可。

## English

This maintenance release fixes a giant-building blueprint material collision and adds focused Astra office/giant reservation checks. It is published from its independent branch without merging `main`. It includes the earlier optional26.3 JEI fixes and0.6.2 portal improvements; see the [previous release notes](RELEASE-0.6.2.md).

- **Restore authored digit1 blocks.** Internal blueprint code49 was assigned to both digit1 and Guogao. This affected31 intended numeric positions:2 at the Power tower doorway,17 in the LHO galleries and12 in the Underworld tree's numeric garlands. Remove the conflicting alias and palette override. All24 legitimate Guogao emoji-globe centers retain the same actual block, position, appearance and light.
- **Check Astra office avoidance.** The current whole-building reservation includes office plinths, front stairs and roofs. Three seeds with36 giant sites and46 accepted nearby offices showed no intersections;23 conflicting original proposals were rejected or relocated. Production layout logic is unchanged. These finite samples do not establish that every historical overlap report is resolved.
- **Keep existing gameplay.** No changes to giant resource budgets, loot, furnishing density, ordinary scenery mutual overlaps, recipes or combat. Proposed additional resource blocks are not part of0.6.3.

Install exactly one JAR matching the Minecraft version in the table above, with matching Fabric Loader/API. Update both client and server where possible. JEI is optional.

Registered IDs and saved formats from0.6.0–0.6.2 are retained. There is no old-chunk scan or rewrite: **corrected numeric materials apply to new construction generation; already saved wrong blocks stay as they are.** Back up the complete world before updating. Switching JARs does not convert saves between Minecraft versions.

All four targets pass build/resource/package audits and84 Python tests. New pure geometry regression covers eight giant themes in four rotations and547 intersecting chunk queries. The final26.2 production JAR passes233 native palette/blueprint/placed-block checks in a hidden disposable fo262 instance, with two rendered screenshots inspected. The other three targets have build/static checks this round; no new Connector, Voxy or26.3 runtime certification is claimed. See [the work record](WORK-063.md) for evidence and limits.

Attachments include all four binaries, complete matching source, SHA-256 checksums and license notices. License: `GPL-3.0-only WITH GPL-3.0-linking-exception`; Alice (AliceLdl)'s original attribution and applicable third-party notices remain in force.
