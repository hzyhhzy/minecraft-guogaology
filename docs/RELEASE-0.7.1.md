# 果糕逻辑 · Guogaology v0.7.1

修复 Minecraft 26.3 中强化台“挖掘图案”窗口无法正常返回，以及鼠标选格操作错误的问题。

感谢@8266等人帮忙测试

## 修复内容

- **恢复正常返回**：Esc、背包键和“返回”按钮可返回强化台主页；背包键跟随玩家自己的改绑。再次按退出键可按原版方式关闭强化台。
- **修正鼠标操作**：左键选择／取消额外开采格，右键设置参考格；“清空”和“重置”恢复正常。
- **适配版本输入**：使用对应 Minecraft 版本的原生键鼠判定，不再把旧 GLFW 编号硬编码到 26.3 的 SDL 输入中。其他支持版本采用同样的原生判断方式。

这是 0.7.0 的小范围客户端界面修复。挖掘预算、默认图案、晶核效果、装备属性、配方、地形、注册 ID 和保存格式不变。已有自定义图案继续读取，无需重新生成世界。

## 下载

| Minecraft | Java | 文件 |
| --- | --- | --- |
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.7.1.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.7.1.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.7.1.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.7.1.jar` |

安装匹配的 Fabric Loader 和 Fabric API，并选择与游戏版本一致的一份主模组。附件包含四版二进制、对应发布标签的完整源码、许可说明、发布清单及 SHA-256 校验和。

## 验证范围

以未改动的正式 0.7.0 在隐藏的 26.3 独立世界复现问题，再对最终 0.7.1 进行 26.3 主测和 26.2 回归。使用真实生产窗口、原版按键／鼠标事件、菜单按钮包和服务端状态同步，检查选格、参考格、清空、重置、返回、重新打开、关闭后的物品归属及图案保存。

四版分别构建，并逐项比较发布 JAR：仅界面输入类和精确版本元数据有功能变化；10份木机关标签文件仅换行格式变化，内容完全一致，其他生产类及资源数据逐字节不变。测试工具不进入发行 JAR。1.21.x 本轮为构建与静态检查，未新增 Connector、Voxy、JEI 联机组合或任意完整旧存档升级认证。

详细结果见[0.7.1 验证记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.1/docs/WORK-071.md)，原问题见[0.7.0 输入审计](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.1/docs/AUDIT-070-PATTERN-WINDOW.md)。正常游戏存档及配置不作为测试对象。

许可继续为 `GPL-3.0-only WITH GPL-3.0-linking-exception`，保留 Minecraft 链接例外、第三方许可及来源说明。0.5.4.1 的原始修改由 Alice（AliceLdl）完成。

## English

Fix the enhancement station's mining-pattern editor on Minecraft 26.3, where normal exit controls did not work and mouse actions used incorrect input codes.

Thanks to @8266 and others for helping test.

- Escape, the player's configured inventory key, and Back return to the enhancement page. Pressing the exit key again closes the station through the native container behavior.
- Left click selects or deselects extra mining cells; right click sets the reference cell. Clear and Reset work normally.
- Use native version-specific keyboard and mouse handling instead of legacy GLFW numeric codes on 26.3's SDL input. Apply the same native handling across supported targets.

This is a focused client-interface fix over 0.7.0. Mining budgets, default patterns, core effects, equipment attributes, recipes, terrain, registered identities and saved-data formats are unchanged. Existing custom patterns remain readable; a new world is not required.

Choose one matching Fabric JAR from the table above, with matching Loader/API. Assets include four binaries, complete corresponding source, license/provenance notices, a manifest and SHA-256 checksums.

Validation first reproduces the fault with the unchanged published 0.7.0 JAR, then uses final 0.7.1 in isolated hidden 26.3 and focused 26.2 runs. Tests dispatch actual native input events to the production screen and verify synchronized menu actions, editing, returning, reopening, pattern persistence and item ownership. Four-target package comparisons restrict functional changes to the input-screen class and exact version metadata. Ten wooden-mechanism tag files differ only in line endings; other production classes and assets/data remain byte-identical. QA helpers are excluded from release JARs. The 1.21.x targets have build/static coverage only; no new Connector, Voxy, JEI multiplayer-matrix or arbitrary complete-save certification is claimed. See the [validation record](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.1/docs/WORK-071.md).

License remains `GPL-3.0-only WITH GPL-3.0-linking-exception`, retaining the Minecraft linking permission and third-party/provenance notices. The original 0.5.4.1 changes were made by Alice (AliceLdl).
