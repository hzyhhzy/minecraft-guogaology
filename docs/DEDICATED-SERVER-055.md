# 26.2 独立服务端本地验证 / Local dedicated-server validation

日期：2026-10-16。对象为现有 Guogaology 0.5.5，不修改玩法、正式 JAR 或版本号。本轮只验证 Minecraft 26.2。

随后用户另行授权的 Voxy World Generator 联机远景/维度切换/重启测试也已通过，安装分工及证据见 [DEDICATED-VOXY-055.md](DEDICATED-VOXY-055.md)。下文记录的是此前不含World Generator的基础服务端检查。

## 环境与隔离

| 项目 | 实际测试环境 |
|---|---|
| 操作系统 | 本机 Windows |
| Java | Minecraft 自带 Java 25.0.1 |
| Minecraft / Fabric Loader | 26.2 / 0.19.5 |
| Fabric API | 0.160.0+26.2（正常 fo262 现有文件；构建依赖仍为原来的版本） |
| 正式 Mod | `guogaology-dimension-26.2-0.5.5.jar` |
| 服务端依赖 | 正式 Mod + Fabric API；完整联机验证另加独立 QA 辅助包 |
| 隐藏客户端 | 同一正式 Mod、Fabric API、QA 辅助包及客户端 Sodium |
| 服务端堆内存 | `-Xms1G -Xmx5G`；这是测试配置，不是人数容量结论 |

正式 JAR SHA-256：

```text
0923305d2af99a72255959636e55411f2340760e0f4f212bfd633690b427189b
```

全部内容放在 `build/dedicated-qa/` 下的独立新目录，不写正常 fo262 的存档、配置、账号或模组。客户端隐藏且不捕获鼠标，服务端使用 `nogui`，没有可见控制台窗口。

服务端强制 `server-ip=127.0.0.1`，每轮使用独立本机端口。关闭 status、query、RCON、JMX 和 management server，不开防火墙规则、不做路由映射或公网穿透。启动及重启都用 `netstat` 检查实际监听地址；结束后检查端口已关闭。测试使用虚构离线玩家，不读取真实登录凭据。

## 验证内容

| 检查 | 结果与边界 |
|---|---|
| 纯正式包启动 | `dedicated-055-smoke-b`：不加 QA 包，启动、`list`、保存、正常停服通过 |
| 客户端依赖隔离 | 服务端不含 Minecraft 客户端 JAR 或 LWJGL；运行时确认加载不到 `net.minecraft.client.Minecraft` |
| 维度注册 | 主世界、下界、末地、表界、里界、地府六个维度均加载 |
| 0.5.5 资源规则 | 42,629 项原生检查通过：配方、采集、LHO 五格立方范围、生成原料与晶矿等 |
| 灯台 | 1,989 项原生检查通过：六色、54 形态、普通/精准掉落及关闭/死亡等物品守恒 |
| 原版附魔互斥 | 234 项原生检查通过：附魔台、铁砧、砂轮、镶嵌排斥及弓相关规则 |
| 真正分离的客户端和服务端 | 两个 JVM，通过本机 TCP 连接；客户端没有集成服务器对象 |
| 手稿 | 点击真实背包按钮打开；通过原版菜单包放入两颗晶核，关闭前飞行不激活、关闭后同步激活 |
| 强化台 | 实际右键台子、放入镐与晶核、关闭取回，服务端核验恰好一把强化镐 |
| 灯台网络操作 | 实际选择第 54 种形态，物品保留对应表情和分块；8 个输入在操作及关闭后仍合计 8 个 |
| 跨维度 | 客户端发送真实 `/guogaology tp` 命令，主世界→表界→里界→地府→主世界；手稿组件保留 |
| 手稿飞行 | 服务端接收实际飞行与冲刺输入；无碰撞高空中里界/普通世界水平、垂直位移比均约 2.000 |
| 停服重启 | 正常停服→同一存档重启→新客户端连接通过；手稿恰好 2 颗晶核、1 把镶嵌镐、合计 8 个灯均保留 |

飞行测试的两次输入均持续 48 个客户端 tick。普通世界水平约 41.376 格、垂直约 34.875 格；里界分别约 82.752 格、69.750 格。它验证现行普通世界冲刺 2 倍、里界冲刺 4 倍的相对关系，不是网络负载或绝对速度的基准测试。

最后四张原生截图保存在完整联机样例的 `client/screenshots/`：`dedicated-manuscript.png`、`dedicated-enhancement.png`、`dedicated-lamp.png`、`dedicated-return.png`。未调用系统截图或操作用户窗口。

最终凭据为 `build/dedicated-qa/dedicated-055-network-f/dedicated-ok.json`，完整流程约 115.8 秒，含两个服务端进程和两次客户端启动。首次及重启后实际监听均为 `127.0.0.1:49458`，两次正常保存退出，最终 `portClosedAfterShutdown=true`。四张截图均已人工检查。较早的调试目录只保留诊断证据，不作为完成凭据。

## 测试实现与复现

`tools/run_dedicated_qa.py` 从本机现有 fo262 读取依赖，从已缓存的原版服务端捆绑包提取校验后的服务端与库文件。不会把客户端游戏 JAR 混入服务端，也不会下载新的整合包。

完整检查的协调包和断言均属于 `mergeQa` source set，不进入正式 Mod。客户端实际操作使用原版菜单、交互、指令和正式 Mod 的飞行数据包；额外私有包只同步测试阶段，不代替玩法操作。

```powershell
# 从正式仓库 Guogaology/ 运行，预先配置 Java 25 和项目 Gradle 缓存。
./ports/gradlew.bat --offline --no-daemon --console=plain -p ports -Ptarget=26.2 -PmergeQa :26.2:mergeQaJar
python -X utf8 tools/run_dedicated_qa.py --label local-dedicated-new-name --with-qa
```

省略 `--with-qa` 可只跑纯正式包启动/保存/停服。每次必须换全新标签，禁止覆盖旧世界或沿用成功标记。

编写测试时修正了 JOIN 早于世界玩家列表登记、客户端菜单副本与副手更新时机、飞行残余输入取消传送、地形碰撞干扰测速、测试脚本自身误清理镐等夹具问题。正式功能断言与重启后的物品守恒要求没有放宽；这些不是生产代码修复。

本轮把大量原生检查集中在一次服务器 tick 内，日志会有约十余秒 `Can't keep up`，不能用它判断普通游玩 TPS。虚构离线账号也会产生 Mojang/Realms 验证警告。本轮不属于多人压力测试，也未验证 Linux、外网延迟、正版登录或整个 fo262 模组组合的专服兼容性；不扩展到其他 Minecraft 版本或 Connector。

## English scope

This local Windows test uses Minecraft 26.2, Java 25.0.1, Fabric Loader 0.19.5, Fabric API 0.160.0+26.2 and the unchanged Guogaology 0.5.5 release JAR. The pure server smoke test excludes the QA helper. The full test uses separate server/client JVMs over actual TCP, validates native resources and menus, deferred manuscript effects, item conservation, flight input and travel through all three additional dimensions, then restarts the saved server for a fresh connection.

The server binds strictly to **127.0.0.1**. No LAN/public listener, firewall opening, port forwarding or tunnel is created; RCON, query and management services are disabled. Both starts verify the actual listener and shutdown verifies the port is closed. Disposable worlds, anonymous test credentials and hidden unlocked-mouse clients leave the normal instance untouched. The QA helper is excluded from release JARs; generated worlds and logs stay in ignored build directories.

This is a local functional check, not a multiplayer capacity benchmark or Linux/Internet/online-authentication/full-modpack certification. No production source or release artifact changes are required by the checks completed here.
