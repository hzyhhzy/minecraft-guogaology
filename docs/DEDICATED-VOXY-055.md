# 26.2 独立服务端：Voxy World Generator 本机验证

2026-10-16，按用户授权在独立本机实例测试，始终只监听 `127.0.0.1`。正式 Guogaology 0.5.5、正常 fo262 存档、模组及配置不修改。

## 实际安装分工

| 位置 | 模组 |
|---|---|
| 服务端 | Guogaology 0.5.5、Fabric API 0.160.0+26.2、用户现有 Voxy World Gen V2 2.4.3、独立 QA 包 |
| 隐藏客户端 | 同上，加 Voxy 0.2.19-beta、Sodium 0.9.2、Cloth Config 26.2.155 |

Minecraft 26.2、Fabric Loader 0.19.5、Java 25.0.1。本轮没有安装额外的 VoxyServer、Tellus、Iris 或 C2ME。服务端不含 Voxy 渲染主体、Sodium、Cloth Config、Minecraft 客户端 JAR 或 LWJGL；真实专服进程确认加载不到 Minecraft 客户端主类。

测试使用正常 fo262 现有文件的校验副本。World Generator 的 SHA-256 为 `b6ef773fb90af6394d332e6a40b91854dedddbcf0bf3e15d6f357bdac690b1ab`。Guogaology 正式包仍为 `0923305d2af99a72255959636e55411f2340760e0f4f212bfd633690b427189b`。

## 隔离配置

服务端游戏端口仅绑定127.0.0.1；关闭 status、query、RCON、JMX 和管理服务，不增加防火墙规则、映射或隧道。每次启动和重启通过 `netstat` 核验监听地址，停服后确认端口关闭。客户端使用虚构离线账号、隐藏窗口、不捕获鼠标。所有世界和远景缓存均位于忽略的 `build/dedicated-qa/`，不上传或发布。

仅测试副本的 `config/voxyworldgenv2.json`：

```json
{
  "enabled": true,
  "generationRadius": 32,
  "update_interval": 20,
  "maxQueueSize": 4096,
  "maxActiveTasks": 2,
  "showF3MenuStats": true
}
```

客户端普通视距3区块，Voxy `section_render_distance=32`、`service_threads=4`、远景雾关闭。正常实例原来的184区块生成半径、24并发和原有 Voxy 配置不改。视觉补拍中仅关闭测试客户端云层、调整拍摄高度和俯角，不改地形或渲染算法。

## 完整联机与重启结果

`dedicated-voxy-055-b` 完成首轮5次采样：表界→里界→地府→里界→表界，再返回主世界。正常停服、保存、重启后，启动另一个**没有本地 Voxy 缓存的新客户端**，重新采样表界→里界→地府并返回主世界。完整流程约258.64秒。

| 场景 | 首次收到的普通客户端区块缓存之外的区块 | 重启后新客户端收到的同类区块 | 抽查有效远景存储段 |
|---|---:|---:|---:|
| 表界 | 135 | 147 | 每次8 |
| 里界 | 210 | 235 | 每次8 |
| 地府 | 229 | 64 | 每次8 |

这些数量来自15～16秒观察窗口，不是生成性能排名或完整32区块范围的填充率。远景区块须满足距玩家至少7区块，且普通客户端区块缓存中不存在。八个有效存储段是检查上限，不能解释成实际只保存八段。

各次采样同时核验：

- World Generator 真实握手成功，客户端处理其原有远景数据包。
- 远处非空气区块段经过原有 Voxy 导入入口；未向它注入伪造地形。
- 实际 Voxy `WorldEngine` 的对应远处区域含非空体素数据。
- 实际渲染器使用当前维度的同一个 `WorldEngine`。
- 三维度使用不同的持久缓存标识，返回同一维度时标识保持一致。
- 当前世界变化后，没有把原世界对象交给实际远景导入入口；原始探针记录为0次错误维度导入。
- 每次采样结束，服务端 World Generator 工作线程仍存活，生成失败计数为0。

三个维度的缓存标识分别为：

| 维度 | Voxy持久标识 |
|---|---|
| `guogaology:outer` | `c414bb4b4a21f3c00a78676bb0a90847` |
| `guogaology:guogaology` | `a5171123f60693622866347b7144762c` |
| `guogaology:guogao` | `528b4618c2421f5740b7da3952a88ccd` |

两次真实监听均为 `127.0.0.1:64417`，两个进程正常保存退出，最终 `portClosedAfterShutdown=true`。凭据：

- `build/dedicated-qa/dedicated-voxy-055-b/dedicated-ok.json`
- `client/voxy-network-evidence.json`
- `client-reload/voxy-network-evidence.json`
- 两个客户端各自的 `screenshots/` 和 `client-output.log`

每次的接收字节数是区块内容序列化后的统计，**不是网卡实际传输量**，未扣除游戏协议压缩。不能直接据此选择服务器带宽。

## 视觉复核

首轮高空镜头俯角较小且表界云层遮挡，因此另用 `dedicated-voxy-055-c-visual` 补拍三个维度及返回切换。镜头俯角45°，关闭测试客户端云层；表界Y160、里界Y220、地府Y299。此轮额外保存渲染器诊断信息，完整联机重启以此前b轮为凭据，不重复其重启部分。

视觉补拍5次采样和正常返回主世界通过，耗时约161.45秒；实际监听仅 `127.0.0.1:56891`，结束后端口关闭。五张原生截图均已检查：表界灰白山地与矩阵板、里界Astra城景、地府多层树冠和彩灯均可见；返回后的景观和缓存仍对应正确维度。渲染诊断含非零模型/节点及实际Voxy帧运行数据。

最终截图为该目录 `client/screenshots/voxy-{0-outer,1-guogaology,2-guogao,3-guogaology,4-outer}.png`，完整数据在 `client/voxy-network-evidence.json`。C轮首次三个维度的普通缓存外区块分别为147/202/229，均抽查8个有效远景存储段，零错误世界导入。

收尾核验：全部本轮Java测试子进程已退出，55899/64417/56891三个测试端口均无监听。正常fo262的56个模组文件、两份Voxy配置和 `options.txt` 共59项哈希不变，正式包与已安装包哈希一致。凭据为C轮目录 `final-verification.json`。早期a轮用于调试只读探针，不作为完成凭据。

## 日志与适用范围

服务端的 `voxy not present, integration disabled` 表示该进程没有本地 Voxy 渲染/导入主体；本轮仍实际完成远景网络发送。没有 Tellus 时还会出现其可选采样集成缺失的警告。这些文本不能单独当作联网失败的判据。Voxy 另有若干保存队列竞争警告，本轮实际缓存、返回与重启检查通过；本次没有修改其第三方代码。

冷进入地府的b轮有一次约11.8秒 `Can't keep up` 落后提示。它表明首次生成重型地形会有明显负载，不能把本地功能通过解读为多人容量或稳定TPS保证。32区块/2并发是本轮验证设置，不证明184区块/24并发适合服务器。

本轮限于三个采样位置、原有画面风格和上述模组组合，不覆盖所有群系/巨构、不做近远景光照统一、不做多人压力测试，也不扩展到其他 Minecraft 版本、Linux、外网或 Connector。

## 复现与代码边界

完整测试：

```powershell
# 正式仓库 Guogaology/，Java25，已有本机依赖缓存。
./ports/gradlew.bat --offline --no-daemon --console=plain -p ports -Ptarget=26.2 -PmergeQa :26.2:mergeQaJar
python -X utf8 tools/run_dedicated_qa.py --label new-local-voxy-test --voxy-worldgen
```

纯视觉补拍可另加 `--skip-restart`；完整检查默认重启。始终使用全新标签，不覆盖已有世界或沿用成功标记。

新增源码全部属于QA：服务端验证、客户端真实连接/指令/截图、远景入口只读探针及Python启动器。探针不取消、不改写原有入口；网络重建段的非空检查读取真实方块palette，不能用尚未重算的原版 `hasOnlyAir()` 计数判定。正式Mod、World Generator和Voxy的JAR均不修改。

## English scope

The local 26.2 dedicated server runs the existing Guogaology 0.5.5 and Voxy World Gen V2 2.4.3 JARs with Fabric API. The Voxy renderer and Sodium are client-only in this setup. Real WorldGen packets populate Voxy storage beyond the ordinary chunk cache in all three additional dimensions; the renderer uses the current dimension's engine, persistent identifiers are distinct, return trips work, and a fresh uncached client receives all three worlds after a full server restart.

All listeners bind to 127.0.0.1; no firewall changes, port forwarding or tunnels are created. Normal fo262 files remain untouched. This is a local functional test at 32-chunk generation radius and 2 active tasks, not a multiplayer, large-radius, Internet or Linux performance certification. Cold Underworld generation produced a substantial tick-delay warning. Private QA observers and generated worlds/logs are excluded from release JARs.
