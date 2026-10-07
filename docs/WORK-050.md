# 0.5.0 · Guogaology统一命名与命令整理

品牌统一与命令整理已完成，四版构建、严格打包命名空间审计及独立隐藏fo262原生回归通过。晶核分类与命令帮助截图已核验；正常fo262已备份旧包并安装0.5.0。

## 授权与备份

- 用户授权将本项目品牌及内部标识统一为 Guogaology / `guogaology`，覆盖显示名称、Mod ID、资源和数据命名空间、Java包与类名、命令根、Logo文件名及新构建产物前缀。
- 表界与里界英文名统一为 **Outer Guogaology / Inner Guogaology**；中文仍为大数表界／大数里界，果糕地府仍为 Guogao Underworld。
- 修改前完整备份提交为 `7f90e00`、备份分支为 `codex/backup-0.4.9-before-guogaology-namespace`，开发分支为 `codex/0.5.0-guogaology-namespace`。
- 不做旧存档迁移，不增加旧命令或旧注册ID兼容别名。运行回归使用独立隐藏fo262实例的新世界，不改正常存档和配置。
- 表示大数研究学科的 googology、Alice原作者／原模组名称、来源JAR名、原版权通知和历史版本／审计记录保留原事实。此次命名调整不改变第三方归属和许可。
- 本轮没有push或GitHub Release授权。

## 实施清单

- [x] 完成源码、资源、生成器及路径重命名，并通过引用与产物命名空间检查。
- [x] 当前README、双语生存指南、术语表、维护入口和第三方说明中的宿主路径同步新命名；Alice来源名称保持原文。
- [x] 模拟器和晶核作用表内嵌的规则翻译键同步 `block.guogaology.*`，界面品牌仍为 Guogaology／果糕逻辑。
- [x] README移除旧内部ID不变的兼容保证，补充不迁移旧存档；表界坠落阈值修正为当前的 Y≤−500。
- [x] 文档按新命令方案同步，命令整理与品牌统一标记为已完成。
- [x] 完成四版命令入口及中英文帮助的生产实现与检查。
- [x] 完成资源、语言、命名空间、模板、嵌套桥接包及打包工具的适用静态检查，39项Python测试通过。
- [x] 完成1.21.1、1.21.11、26.2、26.3四版独立构建及发行产物核验。
- [x] 完成独立隐藏fo262／26.2原生回归，26,431项断言通过；其余版本只编译和检查资源，不启动游戏。
- [x] 补拍晶核创造分类及命令帮助，并完成截图人工核验。
- [x] 游戏关闭后备份旧JAR并安装正常fo262；新包哈希匹配，唯一主Mod及其余55个文件不变。

## 命令语义

根命令为 `/guogaology`。所有玩家可查看根帮助及 `help`；传送和材料包要求管理员权限等级2。

| 命令 | 行为 |
| --- | --- |
| `/guogaology`、`/guogaology help` | 简明帮助与可点击入口 |
| `/guogaology tp outer` | 固定前往表界 |
| `/guogaology tp inner` | 固定前往里界 |
| `/guogaology tp underworld` | 固定前往地府 |
| `/guogaology tp overworld` | 固定前往主世界 |
| `/guogaology up` | 上一层：地府→里界→表界→主世界 |
| `/guogaology kit portal <outer\|inner\|underworld>` | 领取指定前进门框架与激活材料 |
| `/guogaology kit return` | 领取当前层12块归途框和1张归途签 |

`tp` 已在目标维度时只提示；`up` 不表示上次位置、床或出生点。此次命令重组不改变正常传送门的双向规则。

## 验证状态

文档已按本轮命名与命令目标整理，当前文档的本地链接和两份HTML各9个内嵌翻译键检查通过，文档差异没有空白错误。历史WORK、VALIDATION、旧RESOURCE-CLEANUP审计记录及COPYRIGHT相对备份保持不变。

39项Python测试通过。四版完整构建及严格打包命名空间检查成功，原始输出在 `build/050-final-build.log`；四份发行JAR的版本、依赖与SHA-256在 `build/releases/0.5.0/manifest.json`。

`namespace-050-c` 在独立隐藏fo262的新世界中加载Sodium、Voxy和独立creative-flight-speed工具，通过26,395项原生断言（`NAMESPACE050_OK`）。覆盖三维度真实FULL区块、组件精确NBT及空槽保存、结构模板与方块实体往返、六种材料包的精确内容、权限与同维度无操作，以及六次真实跨维度传送。日志与成功标记在 `build/runtime-qa/namespace-050-c/`。

前两轮QA失败已定位到测试自身：a轮对 `List<ItemStack>` 使用对象身份比较导致误报；b轮假设随机原点必有实地，实际落在空岛间隙。两轮仅修正QA，未改变生产代码。

c轮截图选中了原版创造分类页，因此不计作晶核分类的视觉验证。最终d轮使用Fabric公开分类分页API，验证选中的实际分类和完整物品列表，再拍摄晶核栏与真实命令帮助；两张原生截图均已人工核验。d轮同样运行完整机制检查，共26,431项断言通过，证据在 `build/runtime-qa/namespace-050-d/`。测试JAR与发行manifest的SHA-256一致。

## 正常实例安装

游戏关闭后按长期授权安装 `guogaology-dimension-26.2-0.5.0.jar`。旧0.4.9备份到 `build/backups/fo262-before-0.5.0/`，安装回执 `build/install-050.json`。新包SHA-256：`d9447a323a971ad77a9b931308bf8bdf7ed3fbf3966ae6eea14f395561c267a2`。同时检查旧/新两个Mod ID，安装后仅一个主Mod，其余55个模组文件逐个哈希不变；未修改存档、配置或附加Mod。本轮不做旧存档迁移，应使用新世界。

本轮未push或发布GitHub Release。
