# 果糕逻辑 · Guogaology v0.6.7

修复 Minecraft 26.3 无法打开新旧世界的问题。0.6.5／0.6.6的四张巨构箱子战利品表包含六个旧格式标签条目，26.3要求用 `"items":"#标签ID"` 代替 `"name":"标签ID"`；旧格式会报 `No key items`，阻止世界资源加载。

本版只在26.3适配中转换这些字段，涵盖 `guogao_ruin`、`guogao_sanctum`、`hydra_ruin` 和 `hydra_sanctum`。标签成员、数量、概率和抽取规则不变；其他三版保留原格式。相较0.6.6，所有Java类及其他二进制资源逐字节相同，1.21.1内嵌兼容JAR仅更新元数据版本，不改注册ID或存档格式。0.6.6的精准采集经验修复与原生配方书专页、0.6.5的配方和箱奖规则继续保留。

## 下载与验证

| Minecraft | Java | 对应文件 |
|---|---|---|
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.7.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.7.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.7.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.7.jar` |

安装匹配游戏版本的一份JAR及对应Fabric Loader／API，客户端与服务端使用相同Mod版本。四版构建、最终打包检查和94项Python检查通过；新增检查递归验证最终包的标签格式、标签及成员，防止同类遗漏。

实际26.3测试使用精确发布0.6.0创建并保存隔离夹具，以精确0.6.6复现原错误，再用最终0.6.7验证新建世界、旧档升级保存、保存后重开。三轮均完整加载Guogaology的248张战利品表、解析3标签20成员，共24588次原生箱奖填充通过，并核对四维度及玩家／物品／方块持久化数据。最终26.2补充中文回归通过：1546次真实生存破坏、6788项经验断言和4009项原生配方书断言。

本次确认的是资源解析错误，不将“世界打不开”等同于已证实的存档文件损坏；隔离060夹具升级成功也不代表已经恢复用户报告中的完整存档。1.21.x本轮只构建，未新增独立服务端、Connector、JEI组合或Voxy实测。完整环境、存档范围及成品SHA见[0.6.7验证记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.7/docs/WORK-067.md)。

附件含四版JAR、完整对应源码、SHA-256校验和、安装说明及许可声明。许可继续为 `GPL-3.0-only WITH GPL-3.0-linking-exception`；Alice（AliceLdl）的0.5.4.1原始修改及其他第三方内容保留原始署名和适用许可。

## English

Version 0.6.7 fixes new and existing worlds failing to load on Minecraft 26.3. Four landmark chest loot tables in 0.6.5/0.6.6 contain six legacy tag entries. Native 26.3 requires `"items":"#tagID"` instead of `"name":"tagID"`; the old format reports `No key items` and blocks world resource loading.

The 26.3 adapter now converts these entries in `guogao_ruin`, `guogao_sanctum`, `hydra_ruin` and `hydra_sanctum`. Tag members, counts, probabilities and selection rules remain unchanged. The other three targets retain their existing format. All Java classes and other binary resources are byte-identical to 0.6.6; the nested 1.21.1 compatibility JAR changes only its metadata version. Registry IDs and saved formats are unchanged. The 0.6.6 Silk Touch XP repair and native recipe-book sections, and the 0.6.5 recipe/reward balance, remain included.

Install one matching JAR from the table above with the corresponding Fabric Loader/API; clients and servers need matching Mod versions. All four builds, final-package checks and 94 Python tests pass. The new package guard recursively validates loot-tag formats and resolves their tags and members.

Actual 26.3 tests create and save an isolated fixture with the exact published 0.6.0 artifact, reproduce the failure with exact 0.6.6, and verify fresh-world creation, saved-world upgrade and reopening with final 0.6.7. Each fixed run loads all 248 Guogaology loot tables and resolves 3 tags containing 20 members. The three runs pass 24,588 native chest fills in total, alongside four-dimension and player/item/block persistence checks. A final Chinese 26.2 regression passes 1,546 real survival block breaks, 6,788 XP assertions and 4,009 native recipe-book assertions.

This establishes a resource-loading failure, not proven physical save-file corruption. Successful upgrade of the isolated 0.6.0 fixture does not certify recovery of the reporter's complete world. The 1.21.x targets have build coverage only this round; dedicated-server, Connector, JEI-combination and Voxy runtime tests were not rerun. See the [0.6.7 validation record](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.7/docs/WORK-067.md) for environments, save-test scope and artifact hashes.

Attachments include all four JARs, matching complete source, SHA-256 checksums, installation instructions and notices under `GPL-3.0-only WITH GPL-3.0-linking-exception`. Alice (AliceLdl)'s original 0.5.4.1 attribution and applicable third-party licenses are preserved.
