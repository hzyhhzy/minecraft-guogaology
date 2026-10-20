# Guogaology 0.6.8 综合审计（2026-10-20）

本轮为审计，不是修复发布。正式源码、四版JAR、正常实例、存档与配置未改；未合并main、推送或发布。基线 `c122ac5`，审计分支 `codex/audit-0.6.8-general`。

## 已确认问题

| 编号 | 优先级 | 问题 | 影响与证据 |
|---|---|---|---|
| A01 | P2 | 26.3 核爆追加伤害被受伤冷却拦截 | 26.3；原生复现＋正确字段对照 |
| A02 | P3 | 紫菜木船实体名称显示翻译键 | 四版；26.3／26.2原生确认；原生中英＋四版包核对 |
| A03 | P2 | 五种方向性方块放置朝向固定 | 四版；26.3／26.2原生确认；26.3、26.2各20次；其余两版静态核对 |
| A04 | P2 | 紫菜原木和紫菜木不能用斧头去皮 | 四版；26.3／26.2原生确认；原生铁斧useOn＋冥杉对照 |
| A05 | P2 | 自定义船摔毁掉橡木板 | 仅1.21.1；源码＋对应原版字节码；未启动1.21.1 |
| A06 | P3 | 改名船放置后丢失名称 | 仅1.21.1；源码＋对应原版字节码；未启动1.21.1 |

### A01 26.3 核爆追加伤害被受伤冷却拦截

剑直击16HP后，应再追加4.8HP，主目标实际追加0；旁边目标正常追加4.8。弓两项实际16.5，预期21.45。只清除正确冷却字段的QA对照恢复正常；26.2同场景直接目标与邻居均正常追加4.8。已有受伤冷却时，较弱追加会被拒绝，较强追加也可能只算差额；不是所有核爆溅射都失效。

原因：26.3的真实受伤冷却已移到LivingEntity.damageCooldownTime；当前两个适配点清理的仍是Entity.invulnerableTime。

建议：仅改26.3适配器的两处字段，保留保存、置零、finally恢复及lastHurt恢复。修复后重新做剑／弓原生回归。

来源：`tools/prepare_port_sources.py:176`、`tools/prepare_port_sources.py:178`、`ports/common/main/dev/guogaology/mining/MiningEffects.java:68`、`ports/common/main/dev/guogaology/mining/BowEffects.java:80`。

### A02 紫菜木船实体名称显示翻译键

缺entity.guogaology.laver_boat。库存里的船物品名正常，错误发生于船实体名称，不能把两者混为一谈。冥杉船正常。

原因：item键有翻译，entity键遗漏；现有静态语言审计没有枚举动态注册实体。

建议：补中英实体名称及copy_catalog，并把注册实体加入翻译覆盖检查。

来源：`src/main/resources/assets/guogaology/lang/zh_cn.json`、`src/main/resources/assets/guogaology/lang/en_us.json`、`tools/audit_localization.py`。

### A03 五种方向性方块放置朝向固定

Astra机柜server_rack、终端office_monitor没有facing状态；椅子chair、震惊瘫坐椅nuke_chair、蛇头snake_head虽有四向模型，普通放置仍固定north。分别按玩家四个方向放置，结果均不变。

原因：前两种注册为普通Block；后三种缺少按玩家方向选择放置状态的实现。

建议：添加原版式水平放置／旋转／镜像。新状态默认方向应保持现有外观，以免旧建筑整体转向。

来源：`ports/common/main/dev/guogaology/GuogaologyBlocks.java:167`、`ports/common/main/dev/guogaology/outer/block/ChairBlock.java:22`、`ports/common/main/dev/guogaology/outer/block/SnakeHeadBlock.java:22`。

### A04 紫菜原木和紫菜木不能用斧头去皮

laver_vein、laver_wood返回Pass并保持原块；对应去皮方块已存在。冥杉原木和木块同样操作都成功。这里说的是右键去皮，不是之前排查的斧头挖速。

原因：四版都只注册了冥杉的两条去皮映射。

建议：把紫菜两条映射接入各版本原版去皮接口，保留木纹轴向。

来源：`ports/26.3/src/main/java/dev/guogaology/outer/registry/ModBlocks.java:115`、`ports/26.3/src/main/java/dev/guogaology/outer/registry/ModBlocks.java:116`。

### A05 自定义船摔毁掉橡木板

两种船继承默认OAK变体，满足原版落地摔毁条件时掉3橡木板＋2木棍。现代三版使用带对应掉落供应器的原版Boat实体，不属于这一路径。

原因：仅覆写普通船物品掉落，未覆盖摔毁时按木材变体拆板的路径。

建议：使该路径使用对应木材，并补旧版原生摔毁测试。

来源：`ports/outer-1.21.1/src/main/java/dev/guogaology/outer/registry/ModBoats.java`。

### A06 改名船放置后丢失名称

紫菜／冥杉船的自定义OuterBoatItem漏掉原版放置配置步骤。1.21.11、26.2、26.3使用原版BoatItem，不受此遗漏影响。

原因：缺少EntityType.createDefaultStackConfig应用物品自定义名称／实体数据。

建议：复用原版配置步骤，并验证改名→放置→回收。

来源：`ports/outer-1.21.1/src/main/java/dev/guogaology/outer/item/OuterBoatItem.java`。

## 本轮实际验证

- 26.3：4个种子独立新世界，三个自定义维度抽样与返回、28晶核／序数晶体模型；最终d轮291注册物品×7上下文、2716状态原生模型查询及NBT往返、145真实工作台配方均无结构错误。模型解析不能代替逐个截图审美检查；tag配方各取一组材料，未穷举所有组合。
- 26.3菜单119项、附魔互斥234项、手稿169项通过；4次真实编辑中死亡及重复关闭均精确守恒。1546次生存模式服务端实际破坏／6788项经验断言通过（不代表按住鼠标计时），精采循环经验0。弓运行完成但**2项数值失败**，不得将观察收集器的正常退出当作全绿。
- 26.2：中英两轮重点回归，每轮569项；包括50个木制品实际采集案例、4种原生告示牌编辑与文本保存、56显示上下文。另有A01伤害对照及船名／去皮／20次四向放置观察。
- 四版最终包静态审查、111个Python检查通过；2378在用资源无缺失引用、644中英键一致。额外动态语言审查揭示原静态检查的实体键盲点；进度249项（76可见）、148配方、248战利品表的引用与父链补充审计见data-audit.json。后者不等于逐一实际解锁所有成就／抽取全部loot。

### 26.3真实TCP矩阵

JEI31.16.0.86，Loader0.20.0／API0.163.0+26.3。每组自然首次捡枝石和数字刻石；核对服务端权威配方与标签。旧服为正式0.6.0。所有监听仅127.0.0.1、双方正常退出、端口关闭、正常实例哈希保护通过。

| 组合 | 结果 | 整轮秒数 |
|---|---|---|
| none | 通过 | 86.4 |
| client-jei | 通过 | 90.0 |
| server-jei | 通过 | 79.7 |
| both-jei | 通过 | 90.3 |
| old060-client-jei | 通过 | 90.5 |

整轮时间包含启动、加载、操作和退出，不是传送门性能指标。旧服不具备的新配方仍不存在；本轮未新增Connector／Voxy测试。

## 风险与未覆盖

- **超容量核心组件：**外部命令／模组可构造超过当前装备容量的合法组件；打开菜单同步可能截断尾槽。正常镶嵌流程不能产生它，未证实0.6.0正常存档受影响。 建议：保留或返还溢出槽，不应静默丢弃。
- **传送门断线重连：**pending请求按UUID查当前玩家，没有验证新旧ServerPlayer是否同会话；极窄同UUID重连场景尚未网络复现。 建议：断线取消，或检查玩家对象身份。
- **弓命中账本／跨MC迁移：**损坏账本和跨Minecraft版本数据路径未实测；正常同版本流程未发现复制或串玩家。 建议：独立做损坏记录恢复和跨MC迁移测试。
- **额外物品展示上下文：**新版本ON_SHELF等额外上下文不在本轮七种原生上下文里；不能用2037项检查覆盖它。 建议：后续按该功能专测，不计为当前确证bug。

本轮未穷举八巨构内饰、所有外部模组、所有伤害源、持续键盘飞行、长时间多人负载、所有旧存档或强退时序；不能声称没有其他Bug。早期过时弓断言与早期未满足驻留／实体查询前置条件的QA失败已排除，最终d轮预热且验证实体可查询后仍确认A01。

## 证据与复现

- 可视报告：`C:\Users\sigmoid\Documents\ChatGPT\mc_dimension_mod\outputs\20261020-general-audit-068\审计.html`
- 结构化汇总：`build/audit-068-general/summary.json`
- 最终263：`build/runtime-qa263/audit068-general-263-d/`
- 最终262：`build/runtime-qa/audit068-focus-262-b/`
- 网络：`build/network-qa263/audit068-network-*/`，每组的`network-verification-ok.json`和`network-result.json`共同确认结果。
- 木制品／方向／动态语言／战斗／数据／所有权只读子审计：`build/audit-068-general/*.json`。

QA均在独立source set；新观察套件、263适配准备器和报告生成器在`ports/qa263`、`ports/qa26`和`tools/audit-support`，不进入正式JAR。原始测试日志包含已知失败，不删改。修复后必须重新生成证据，不能复用本轮失败结果认证修复。

四版正式SHA256未变：

| MC | SHA256 |
|---|---|
| 1.21.1 | `d4c7e13b4a7d7bab78421adb8e82f7bb9c759765b5c2cb2a67413b445c7fda92` |
| 1.21.11 | `699465d5f0427f57a3e8dc9c3a394376831e9b4d4f1f2e9cfe9fc6cfd1d2c1c9` |
| 26.2 | `cdbf300554851ccee6aba13fcd55210f1688a40da01a0b6914cc0f5b5b4b74b4` |
| 26.3 | `c15eb5f74ee645ab6253ec5333fd35fd9760cd97faf5e9ed408ac0d85beeabff` |
