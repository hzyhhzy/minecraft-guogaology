# 果糕逻辑 · Guogaology v0.6.8 / v0.6.1a

本页同时提供两个版本：**0.6.8**在0.6.7基础上修复；**0.6.1a**仅将本次JEI、物品显示和木制品修复回补到原0.6.1，不包含0.6.2之后的配方、生成、传送门等更新。一般请选择0.6.8；需要保留0.6.1玩法的整合包可选0.6.1a。

感谢@8266等人帮忙测试

## 本次修复

- 修复26.3客户端安装JEI连接旧服务端时，配方回退覆盖客户端标签，首次拾取枝石／数字刻石解锁配方后可能断线，以及自定义木头失去斧头加速的问题。兼容保护改为检查JEI实际代码结构，取消只接受31.14.0.72的版本限制；未安装JEI时无需额外依赖。将来JEI保留受检结构即可自动适配，无法确认安全的结构会给出警告并跳过。
- 修复葛立恒及七种类似物品的掉落、手持与展示框尺寸，物品栏和实际放置外观保持不变。
- 修复紫菜门重复掉落、紫菜双层台阶只掉一个的问题。紫菜和冥杉告示牌接入原版合法类型、编辑、保存及旧版渲染；其他木制品继续采用原版机制，冥杉不可燃规则不变。

未改变配方、生成、装备平衡或存档ID。0.6.8继续包含0.6.7的26.3世界加载修复。冥杉门重复掉落未能复现，原有正确规则保留并通过实测。

## 下载与安装

| Minecraft | Java | 当前修复版 | 原0.6.1回补版 |
|---|---|---|---|
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.8.jar` | `guogaology-dimension-1.21.1-0.6.1a.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.8.jar` | `guogaology-dimension-1.21.11-0.6.1a.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.8.jar` | `guogaology-dimension-26.2-0.6.1a.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.8.jar` | `guogaology-dimension-26.3-0.6.1a.jar` |

只装与Minecraft版本匹配的一份JAR，并安装对应Fabric Loader／Fabric API，不能同时安装两条版本线。正常使用建议客户端和服务端同步更新。客户端JEI保护已验证连接旧0.6.0／0.6.1服务端；0.6.8另验证旧0.6.7服务端。服务端的门／台阶掉落和告示牌需要服务端更新；0.6.0服务端未发布的自定义配方，客户端不能代其补全，完整JEI配方查询需服务端至少为0.6.1。

## 验证与源码

两版各自四版本构建、最终打包审计通过；0.6.8／0.6.1a分别通过111／101项Python检查。14个官方JEI包及23个模拟代码结构共37项检查通过。最终26.3真实联机矩阵22组、首次拾取44次、实际JEI查询144次通过，覆盖客户端／服务端均无JEI、仅客户端、仅服务端和双方安装，以及JEI31.14.0.72、31.16.0.86和旧服务端。0.6.8另通过原生26.2的574项检查。木门、台阶、告示牌和八种物品显示另有实际放置、破坏、文字持久化和画面验证。

14个JEI包属于静态结构样本，实际联机使用上述两版JEI；本次未重新认证Connector、Voxy或反馈者的完整存档。完整环境、检查边界及JAR哈希见[0.6.8记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.8/docs/WORK-068.md)和[0.6.1a记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.1a/docs/WORK-061A.md)。

附件包含八份JAR、两份完整对应源码、分版本manifest／安装说明／源码说明，以及统一`SHA256SUMS.txt`。`v0.6.8`和`v0.6.1a`分别标记独立源码线；本次未合并main。许可为`GPL-3.0-only WITH GPL-3.0-linking-exception`；保留Alice（AliceLdl）的0.5.4.1原始修改署名和第三方许可。

## English

This single release contains **0.6.8**, based on 0.6.7, and **0.6.1a**, a narrow backport to the original 0.6.1 source. The backport does not import later recipe, world-generation or portal changes. Install only one JAR matching your Minecraft version from the table above, with matching Fabric Loader/API.

Both builds fix JEI fallback overwriting active client tags on Minecraft 26.3, which could disconnect clients on the first Branch/Numeric Stone recipe unlock and remove axe effectiveness on custom wood. The optional guard checks actual bytecode structure instead of one JEI version number. Compatible future structures are accepted; unverified rewrites warn and skip. No JEI dependency is required when JEI is absent. The builds also fix eight dropped/held item transforms, Laver door and double-slab drops, and native Laver/Dread Fir sign editing, persistence and older-engine rendering. Saved IDs and gameplay balance remain unchanged. The reported Dread Fir door duplicate was not reproduced; its correct existing loot rule is retained and tested.

Both lines pass four-target builds and final-package audits, 111/101 Python checks respectively, 37 structural JEI checks, and a combined 22 actual 26.3 client/server cases with 44 first pickups and 144 real JEI queries. Runtime coverage includes all four JEI installation combinations, the exact older servers described above, and JEI 31.14.0.72/31.16.0.86. Version 0.6.8 additionally passes 574 focused native 26.2 checks. Connector, Voxy and the reporter's complete save were not recertified.

Update the server as well for server-authoritative drops/sign fixes. A 0.6.0 server cannot supply the custom recipes it does not publish; full JEI custom-recipe queries need at least a 0.6.1 server. Matching complete source ZIPs, versioned manifests/notices and SHA-256 checksums are attached. The two source lines remain separate; main is unchanged. GPLv3 with the Minecraft linking exception and Alice (AliceLdl)/third-party attribution are preserved.
