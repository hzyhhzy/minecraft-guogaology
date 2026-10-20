# 果糕逻辑 · Guogaology v0.6.9

修复0.6.8综合审计确认的六类问题。0.6.8的JEI兼容、物品尺寸、木制品和0.6.7的世界加载修复继续保留；本次将完整当前版本合并到真正的`main`。

感谢@8266等人帮忙测试

## 修复内容

- **Minecraft 26.3**：核爆之心在剑和弓上的追加伤害使用正确的生物受伤冷却字段。直击目标也能正常受到追加伤害，完成后恢复原冷却；不移除实体真正的临时无敌。
- 补齐紫菜船的中英文实体名称，避免部分场合显示翻译键。
- 机柜、终端、椅子、震惊瘫坐椅、蛇头按玩家方向放置，并支持原版旋转／镜像。旧机柜和终端缺少方向属性时默认朝北，保持旧模型外观；其他家具已有四向状态不变。
- 补齐紫菜原木／木块的原版斧头去皮映射，保留轴向；冥杉原有正确行为继续保留。
- **Minecraft 1.21.1**：船摔毁时掉落自身木材的木板，保留原版摔毁条件和三木板、两木棍数量。
- **Minecraft 1.21.1**：放置命名船时复用原版物品到实体配置，正确应用名称及原版允许的数据。已经丢失的旧名称无法恢复。

未修改注册ID、组件codec、配方、战利品数量、生成、装备平衡或fffZ/FOS、ψ/Z稳定机制。两种设备仅新增默认值兼容的水平方向属性；客户端和服务端请使用相同版本的主Mod。

## 下载与安装

| Minecraft | Java | 文件 |
|---|---|---|
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.9.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.9.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.9.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.9.jar` |

只安装与游戏版本匹配的一份JAR及对应Fabric Loader／Fabric API。JEI为可选模组，不需要额外安装。0.6.1a仍在[上一份合并发行包](https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.8)中，本次不重新发布该回补线。

## 验证及旧存档

四版构建、最终包审计及125项Python检查通过。13次独立原生运行通过：26.3简中＋Sodium、英文＋原版渲染两轮；26.2重点回归一轮；两条旧存档各三阶段共六轮；四组JEI真实联机。26.3覆盖291种物品的7种显示上下文、2722方块状态模型与NBT往返、145个实际工作台配方，以及菜单、手稿编辑中死亡守恒、附魔互斥、弓和掉落／经验。26.2重点回归569项并检查本轮新增修复。

0.6.0和0.6.8正式JAR先创建并保存独立测试存档，再升级0.6.9保存并在第三个JVM重开。每阶段四维度96个方块状态、24个复杂箱内物品、3个玩家物品、8艘命名船及原LHO稳定／来源控制均通过。另独立解析实际region、entity和player NBT复核；未用用户正常存档作为测试材料。

26.3实际联机验证了双方均无JEI、仅客户端、仅服务端和双方安装JEI31.16.0.86，首次拾取枝石／数字刻石、标签和配方同步通过。所有网络测试仅监听127.0.0.1，结束后端口关闭。

本轮1.21.x仅构建及静态／字节码检查，未重测Connector、Voxy、任意未来JEI结构或不同主Mod版本混用联机。测试夹具升级通过不代表任意已损坏存档均可恢复，也不提供早期`googology`命名空间迁移。环境、检查边界和精确JAR哈希见[0.6.9验证记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.9/docs/WORK-069.md)。

附件包含四份已核验JAR、最终标签提交的完整源码ZIP、manifest、SHA-256校验和、安装说明及许可声明。许可仍为`GPL-3.0-only WITH GPL-3.0-linking-exception`，保留Alice（AliceLdl）的0.5.4.1原始修改及第三方署名。

## English

Version 0.6.9 fixes six confirmed issue families from the 0.6.8 audit and integrates the current source into `main`. Earlier JEI, item-transform, wooden-product and world-loading fixes remain included.

- On **Minecraft 26.3**, Nuclear Blast Heart sword/bow extra hits use the correct living-entity hurt cooldown, including the direct target, and restore the original timer without removing true entity invulnerability.
- Add the missing Laver boat entity translations. Server racks, monitors, chairs, Shocked Reclining Chairs and snake heads gain player-relative placement and native rotation/mirroring. Old racks/monitors default north and retain their original appearance.
- Register native Laver log/wood stripping while preserving the axis and existing Dread Fir behavior.
- On **Minecraft 1.21.1**, boats broken by falls use their own planks under the unchanged native conditions/counts; placed boats apply native item-to-entity configuration, including custom names. Previously lost names cannot be reconstructed.

Saved IDs and component codecs, recipes, loot quantities, generation, equipment balance and raw LHO stability mechanics are unchanged. Only the two devices add a horizontal facing property with a compatible default. Use matching client/server Mod versions and exactly one JAR for your Minecraft version from the table above, with matching Fabric Loader/API. JEI is optional.

Four builds/package audits and 125 Python tests pass. Thirteen native runs cover two primary 26.3 render/language configurations, focused 26.2 regression, two three-stage old-save chains and four JEI installation combinations. Native 26.3 checks include 291 items in seven contexts, 2,722 block states and 145 actual crafting recipes; 26.2 passes 569 focused checks plus this repair's controls. Exact published 0.6.0/0.6.8 artifacts write independent saves, which 0.6.9 upgrades, saves and reopens in separate JVMs; complex items, old furniture, named boats and original LHO behavior are retained and independently checked from disk NBT. Loopback-only 26.3 networking with JEI 31.16.0.86 passes first Branch/Numeric Stone pickups and synchronization in all four installation combinations.

The 1.21.x targets have build/static coverage only this round. Connector, Voxy, arbitrary future JEI rewrites and mixed Mod-version networking were not recertified. This does not promise recovery of arbitrary damaged saves or migration from the older `googology` namespace. See the [validation record](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.9/docs/WORK-069.md) for exact scope and artifact hashes. Attachments include all four unchanged verified JARs, complete source from the final tag, manifests, checksums and GPLv3/Minecraft-linking-exception notices; Alice (AliceLdl) and third-party attribution remain preserved.
