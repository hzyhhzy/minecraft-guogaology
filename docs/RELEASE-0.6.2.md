# 果糕逻辑 · Guogaology v0.6.2

本版汇总自公开版 **v0.6.0** 以来的0.6.1联机兼容修复与0.6.2传送门优化，从独立分支发布，未合并到 `main`。

## 主要变化

- **Minecraft 26.3可选JEI兼容。** 修复JEI仅装客户端时覆盖服务端标签，导致紫菜木/冥杉不受斧头加速，以及首次取得数字刻石时因 `recipe_book_add` 解码失败断线的问题。保护钩子仅对JEI **31.14.0.72** 启用；不强制安装JEI，也不对其他版本盲目注入。
- **数字工具配方完整同步。** 26.3通过Fabric公开接口同步原版及本模组配方，使JEI可查询数字镐、数字剑和数字刻石换圆石，并展开全部十种数字原料；保留原版合成、烧炼查询。服务端数据包仍是配方来源。此修复随0.6.1加入，仅改变26.3适配。
- **按坐标比例复用已有门。** 主世界/表界两侧均搜24格；表界→里界、里界→地府的目的地搜96格，反向搜24格。范围是按精确缩放中心比较的水平正方形，覆盖负坐标与边界取整情况。选择最近的完整且有安全落点的门，没有永久配对；出现更近的合格门时，往返仍可能换门。
- **扩大记录筛选范围，不扩大地形扫描。** 每次只异步准备一个旧门的实际范围，最多显式请求四个FULL区块；无效候选继续尝试下一扇。找不到可复用门时，仍用原来的24格、局部3×3区块地形搜索。并发准备会重新检查刚建成的门；源门邻区尚未加载时等待，不误判为破门。保留600tick/45秒期限和安全回退。
- **更可靠的进入和到达。** 到达尝试四个方向、每侧三个位置，统一检查地板、头顶、流体、危险物与加载状态。实体进入要求玩家中心位于内场向内缩0.15格的范围，脚部高度在门面上下0.5格内，避免站框或擦角误传。
- **里界门到达高度不超过Y200。** 限制同时覆盖旧门复用、新门生成和最终传送。旧高门不会删除或搬动，仍可作为出发门；返回时可能需要另建较低出口。独立的坠落换界机制保留。

## 下载与安装

| Minecraft | Java | 独立Fabric JAR |
| --- | --- | --- |
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.2.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.2.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.2.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.2.jar` |

只安装匹配游戏版本的一个主模组JAR，使用对应Fabric Loader与Fabric API，并移走旧主模组。**客户端与服务端应同时更新到0.6.2，使用相同Minecraft版本的对应包。** JEI可两端都不装、只装任一端或两端都装，不是主模组依赖。

完整数字工具查询修复需要服务端也更新。0.6.1曾实测新客户端连接旧0.6.0服务端仍可保护标签、避免首次拾取断线，但旧服务端没有发布完整配方快照，单独更新客户端不能补齐这些配方。该混合版本对照不代替两端配套更新。

附件包含四版JAR、SHA-256清单、依赖说明及完整对应源码。

## 存档兼容与验证范围

- 保持0.6.0的注册ID、门记录、返程地址和装备持久化格式，不引入门绑定或旧区域扫描转换。四版静态审计确认相关保存类及网络载荷与实际0.6.0包一致。
- 实际升级测试使用**Minecraft 26.2中由正式0.6.0创建并保存的独立夹具**：八扇门、里界Y230旧高门、返程地址、枢纽、箱内物品、玩家放置标记和数字工具组件均保留，八次旅行全部成功。它不代表所有用户存档、第三方模组组合或26.3旧档都经过实测。升级前备份整个世界；不支持跨Minecraft版本降级打开，回退应恢复完整备份。
- 四版构建、打包检查及84项Python测试通过。本轮传送门实际运行限于隐藏、独立的**原生Fabric26.2集成服务器/客户端**，包含源门邻区等待、精确范围、真实站框/擦角排除、多个安全侧、共享票据、并发造门及原生命令/加载界面。

| 26.2有限实测样本 | 成功 | 服务端请求耗时中位数 | p95 / 最慢 |
| --- | ---: | ---: | ---: |
| 已加载旧门复用 | 24 / 24 | 29.06毫秒 | 72.32 / 87.16毫秒 |
| 未生成目的地 | 12 / 12 | 1.867秒 | 10.529 / 10.529秒 |
| 0.6.0保存门，含高门低处返程 | 8 / 8 | 56.35毫秒 | 415.72 / 415.72毫秒 |

耗时从服务端排队计至传送或回退完成，排除夹具准备和截图等待，**不是客户端地形加载画面完全消失的时间**。冷样本在每次请求前核实目标九个FULL区块未加载、对应区域文件不存在。另以挂起加载的故障注入验证第601tick回退，晚完成后再观察45tick无补传或造门；此预期失败不混入正常成功率。上述样本中没有自然失败，不构成任意地形、机器或服务器负载下的零失败保证。

26.3的JEI修复在0.6.1已用真实本机TCP专服/隐藏客户端验证四种JEI安装组合，检查完整标签、原生拾取解锁、七种斧头、数字工具与原版JEI查询及实际配方页面。0.6.2保留该实现并通过打包审计；本轮未新增26.3传送门运行测试。1.21.1与1.21.11本轮限于构建和静态检查，未新增专服、外网、Linux、Voxy或Connector认证。

许可：`GPL-3.0-only WITH GPL-3.0-linking-exception`；Alice及其他第三方内容保留各自版权与许可声明。

## English

This release collects **0.6.1 and 0.6.2 changes since public v0.6.0**. It is released from its independent branch; `main` has not been merged or changed for this release.

### Changes

- **Optional JEI compatibility on Minecraft 26.3:** protect server-synchronized client tags from JEI 31.14.0.72's client-only fallback. This fixes missing axe acceleration on Laver/Dread Fir timber and the first Numeric Stone pickup disconnect while decoding `recipe_book_add`. The hook is enabled only for that exact JEI version. JEI is not a required dependency.
- **Complete recipe synchronization on 26.3:** Fabric's public API publishes both vanilla and Guogaology recipes, allowing JEI to query Numeric Pickaxe, Numeric Sword and Numeric Stone→Cobblestone with all ten numeric ingredients, alongside vanilla crafting/smelting recipes. Server data packs remain authoritative. These 26.3-only repairs were introduced in 0.6.1.
- **Scale-aware portal reuse:** Overworld/Outer searches 24 blocks in either direction. Outer→Inner and Inner→Underworld search 96 blocks at the destination; their reverse directions search 24. Square horizontal windows use the exact scaled centers, including negative coordinates. The nearest complete gate with a safe landing wins. Gates have no permanent pairing, so closer eligible competitors can change a return route.
- **Bounded loading:** the wider window filters saved gate records. Only one candidate footprint is prepared at a time, requesting at most four FULL chunks explicitly. Terrain placement keeps the local 24-block/3×3-chunk search. Concurrent preparations reconsider newly built gates. Unready source neighbors wait instead of being treated as a broken frame; 600-tick/45-second deadlines and safe retreat remain.
- **Safer entry and exits:** try twelve landing positions across four sides, using the same ground, headroom, fluid, hazard and loaded-chunk checks. Physical entry requires the player's center inside the field inset by 0.15 blocks and feet within gate Y±0.5, avoiding frame/corner contact activation.
- **Inner arrivals at Y≤200:** this applies to reused and new exits and final transfer. Existing higher gates remain usable departures and are not removed; returning may create a lower exit. The separate void-fall transition is preserved.

### Installation and compatibility

Use exactly one matching Fabric JAR from the table above, with matching Fabric Loader/API. Java 21+ is required for 1.21.x and Java 25+ for 26.x. Remove the old main JAR. **Update both client and server to 0.6.2 on the same Minecraft version.** JEI may be absent, client-only, server-only or installed on both sides.

Complete Numeric recipe queries require the updated server too. A test connecting a 0.6.1 client to a 0.6.0 server verified tag/disconnection protection, but that old server cannot supply the missing full recipe snapshot. It does not replace the recommendation to update both ends.

Saved IDs, portal/return records and equipment formats remain compatible with 0.6.0. A genuine saved 26.2/0.6.0 fixture passed eight upgrade trips and retained its gates, high Inner departure, return/hub data, chest contents, placed-core flag and numbered tool. This is a controlled fixture, not exhaustive old-world or third-party compatibility certification. Back up the complete world; do not downgrade it across Minecraft versions. Rollback requires the full backup.

### Validation

All four targets pass builds/package audits and 84 Python tests. New portal runtime testing is limited to an isolated hidden native Fabric 26.2 integrated client/server. The finite samples above record 24/24 warm reuse trips, 12/12 ungenerated destinations and 8/8 saved-fixture trips. Times measure server queue-to-result, excluding setup/screenshots and later client terrain-screen completion. Every cold destination was verified unloaded and unsaved before its request. An injected unfinished load safely retreats at tick 601, with no late teleport/build during 45 ticks after completion; it is excluded from normal failure rates. These local samples are not universal latency or zero-failure guarantees.

The inherited 26.3 JEI repair was tested in 0.6.1 using real loopback dedicated-server/client connections for all four JEI combinations, including tags, pickup/unlocks, seven axe materials, actual recipe queries and a displayed Numeric recipe page. 0.6.2 preserves that implementation; no new 26.3 portal runtime test is claimed. The 1.21.x targets have build/static checks in this round. No new Internet, Linux, Voxy or Connector certification is claimed.

The release includes all four binaries, checksums, dependency information and matching complete source. License: `GPL-3.0-only WITH GPL-3.0-linking-exception`; Alice's attribution and applicable third-party notices remain in force.
