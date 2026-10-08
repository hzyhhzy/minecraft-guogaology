# Sinytra Connector / 原生 Fabric

Guogaology 0.5.2 的同一份 **Minecraft 1.21.1 JAR** 同时用于下列两种加载方式。没有 Connector 专用分支产物，也不需要安装额外的 Guogaology 补丁。

| 加载方式 | 本轮实际验证的依赖 |
| --- | --- |
| 原生 Fabric 1.21.1 | Java21、Fabric Loader0.19.5、Fabric API0.116.17+1.21.1 |
| NeoForge + Connector 1.21.1 | Java21、NeoForge21.1.248、Connector2.0.0-beta.16+1.21.1 / beta.17+1.21.1、Forgified Fabric API0.116.15+2.3.1+1.21.1 |
| 原生 Fabric 26.2 | 独立隐藏fo262、Java25、对应Fabric API、Sodium、Voxy及独立创造飞行调速工具 |

NeoForge 实例安装对应的 **Forgified Fabric API**，不要同时放普通 Fabric API。原生 Fabric 实例继续使用普通 Fabric API，不需要 Connector。用户最初提供的崩溃来自1.21.1；官方没有26.2 Connector可供本次验证，不能把原生26.2测试写成Connector26.2测试。

## 验证内容

使用发行JAR创建新的普通世界，进入主世界及三个自定义维度；验证实际摔落／石笋伤害、手稿飞行权限恢复、其他来源及创造飞行不受影响、裸手与镐挖速、原版急迫、多重箭实体／无限弹药／穿透／射击快照、真实装备耐久损耗、原版耐久附魔、防护对虚空伤害的作用、装备损坏退回晶核，以及同步后的手稿GUI和晶核画面。

26.2另有96组持续原生键盘飞行场景，包含两个方向、表里倍率、飞行工具独立性、编辑延迟生效和死亡物品守恒。最终fo262新世界回归覆盖真实传送、三个维度的完整区块、装饰与方块实体、命令及材质。

这些是实际客户端及其集成服务端的检查，不是专用服务器联机压力测试，也不代表任意第三方整合包组合都经过验证。1.21.11和26.3保持编译及资源校验，本轮不启动它们。精确运行目录、计数与SHA-256见 [WORK-0502.md](WORK-0502.md)。

## 修复原则

- 飞行摔落兼容：包住完整的摔落处理方法，不再定位原版特定的`allowFlying`字段指令。NeoForge将该指令换成了`mayFly()`，旧注入会令Connector在转换阶段崩溃。仅在本模组生存飞行处理非普通摔落来源时临时调整权限，`finally`恢复，不发能力包、不影响其他飞行来源。
- 耐久兼容：1.21.1挂接两种加载器都保留的原版`EnchantmentHelper.getItemDamage`。NeoForge拆分／扩展了`ItemStack.damage`的参数；旧局部参数捕获会失败，一种看似成功的转换甚至会丢掉减耗计算。防具继续使用实际受击时的作用域快照；存活的消耗点仍经过原版耐久附魔。
- 表界生成：构造器中的噪声设置改为可组合的`WrapOperation`，调用原操作，避开Connector对旧`Redirect + @Local`的错误转换。
- 1.21.1资源：只对构建输出适配新版整数梯形分布、植物引用、植被标签、岩团配置及纹理字段，修复原生Fabric同样存在的世界注册失败。完整保留整数分布权重、植物尝试次数与基底限制。1.21.1没有的灌木／干草分别用蕨／枯灌木；后续版本的原始资源不改。

## 开发回归

```powershell
# Java21；辅助JAR独立输出到build/qa，不进入发行包
./gradlew.bat -PconnectorQa :build connectorQaJar

python -X utf8 tools/run_connector_qa.py --loader fabric --java-home '<JDK21>' --label fabric-check
python -X utf8 tools/run_connector_qa.py --loader connector --beta 16 --java-home '<JDK21>' --label connector16-check
python -X utf8 tools/run_connector_qa.py --loader connector --beta 17 --java-home '<JDK21>' --label connector17-check
```

当前Windows本地启动器需要已有原生`1.21.1-Fabric 0.19.5`的库与资产；官方NeoForge21.1.248客户端安装在忽略目录`build/connector-investigation/client-base`，相应Connector及FFAPI依赖也仅存在该忽略目录。脚本从正常启动器只读库／资产，将所有世界、设置、缓存与日志放进新的`build/runtime-qa/<label>`，拒绝复用目录。窗口隐藏且不捕获鼠标。绝不可把测试辅助JAR装入正常实例。

## English

The same Guogaology **1.21.1** release JAR runs on native Fabric and the tested NeoForge/Connector combinations above. Connector is optional. Use **Forgified Fabric API**, not ordinary Fabric API, in the NeoForge setup. Version26.2 is tested on native Fabric only; no official Connector26.2 build was available.

Actual fresh-world checks cover dimensions, fall/flight permissions, mining, arrows, durability/Unbreaking, protected void damage, exact core drops on equipment breakage, synchronized menus and rendering. Tests run in hidden disposable clients with integrated servers, not a dedicated-server multiplayer stress test. Other third-party modpack combinations are not implicitly certified. Minecraft1.21.11 and26.3 receive build and resource checks only.

The patch replaces fragile bytecode/local-variable targets with stable method/operation wrappers, preserves native Unbreaking and foreign flight permissions, and repairs missing1.21.1 resource adaptations without changing modern world generation. See the linked work record for evidence and checksums.
