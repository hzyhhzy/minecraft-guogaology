# 展厅 C1 空集晶核：原始构造核查

核查日期：2026-10-06。研究对象是原展厅 **C1**，不是 A1、B1 或后来的 D1。

## 同源证据

原 0.2.16 `models/block/lho_trace.json` 的 SHA-256 为 `0e7298e7d237485222cfce60812102ce1c9b54bf7b9d3a70042dbe0058c84910`。原展厅 `c_lho_trace_lv1.json` 有 1,992 个面，等于原模型 332 个长方体的六面；两者全部顶点的集合完全相同。展厅 C1 为静止模型，后来的高阶动画不属于 C1 原构造。

当前仓库的可维护输入 `tools/reference/absence-c1.json` 存储上述来源哈希及筛选后的原始几何；修复不依赖外部历史目录。`tools/generate_absence_core.py` 负责生成当前三阶，客户端只变换预先烘焙的部件。

## 原模型的 332 个长方体

坐标使用 Minecraft 模型单位，16 单位为一格。

| 构成 | 数量 | 构造 |
|---|---:|---|
| 断续外框 | 24 | 正方体十二条棱，各分两段，中间是真实空隙；线宽 0.6 单位 |
| 极淡雾盒 | 1 | 外框内侧 1.7～14.3 单位的半透明盒 |
| 前主层 | 74 | 空集图案逐行拼成实体小长方体，Z=5.2～6.4 |
| 前幽影夹层 | 74 | 主图案偏移 X+0.22、Y+0.25，Z=6.85～7.07 |
| 后主层 | 74 | 前主层左右镜像，Z=9.6～10.8，从相反方向读取 |
| 后幽影夹层 | 74 | 同类偏移，Z=8.3～8.52 |
| 漂浮碎块 | 11 | 0.45 单位的小立方体，位置为离线确定的散点 |

因此内部是**四层有实际厚度的浮雕**，不是四张贴图。两主层厚 1.2 单位（0.075 格）；两幽影层厚 0.22 单位。空集正视轮廓虽然在一个平面上，侧面仍能看到小方块的厚度、边缘和各层之间的间隔。

## 断续纹理与透明度

三个虚无材料——空集、fffZ、FOS——采用同一套构造语言。空集主色 RGB 为 `(170, 222, 212)`；其虚无感来自真实几何缺口与分级透明度，没有高清字体，也没有运行时随机闪烁。

| 材质 | Alpha（0～255） | 用途 |
|---|---:|---|
| ink | 182 | 主层的清楚笔画 |
| soft | 100 | 主层淡笔画及实体四个侧面 |
| ghost | 34 | 主层背面、幽影夹层与部分散点 |
| frame | 98 | 断续棱框；贴图边缘有少量更亮的细线 |
| mist | 20 | 极淡内盒 |

前主层正面有 59 个 ink 块、15 个 soft 块，背面全部 ghost，四个侧面全部 soft。后主层把正背面材质交换，且几何左右镜像。这样避免两层清楚图案透视叠加成双字；后主层并不是整层仅用 ghost。模型关闭环境遮蔽，元素 `shade=false`，没有普通石块的侧面阴影。

原生成器读取 32×32 图案遮罩，每行按连续且同透明度的像素段生成长方体，像素步距 0.375 单位。缺口和阶梯斜线因此由实体占据决定。不能用“画一个新 ∅ 字符，再给它加透明噪点”替代原图案。

## 0.3.7 的问题与修复原则

0.3.7 把原主层正视轮廓采样成贴图，贴在两个相隔很近的面上。虽然保留了正视纹理，但丢掉真实侧壁，也改变了原主层厚度；用户看到内部像平面，确实是复刻错误。

- Lv1：复用原前主层的 74 个长方体及六面材质，仅将其居中；不重新画符号，不再加反向主层。
- Lv2：从同一原始占据提取圆环及像素斜线，保留挤出厚度、侧面和缺口；两个环与斜线分属不同运动部件。
- Lv3：内含大小两套立体部件；外围沿用用户最近要求的较圆圆弧。材料继续同源于 C1，外围与内部运动分开。
- 合并被遮挡的内部面及真正重合的外框接头，不能通过删除侧面或重新投影来减面。
- 近景、物品模型、静态 Voxy 替身必须取同一份几何。验证应包含接近侧视的游戏实拍、多个动画时刻、厚度和六向外表面检查。

## English summary

The original gallery C1 exactly reproduces 332 cuboids from the 0.2.16 block model, yielding 1,992 faces. It has two extruded main reliefs, two faint extruded ghost layers, a broken cage, a faint inner box and scattered cubes. Main reliefs are 1.2/16 block thick; ghost layers are 0.22/16 thick. Their visible fronts, faint backs and softened sidewalls use different alpha levels of the same pale mint palette. The 0.3.7 texture-plane reconstruction lost the original sidewalls. The correction restores the original occupied volume and directional materials while retaining the current independently moving higher-grade shapes.
