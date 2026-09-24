# AJMST Android 还原工程

本工程以 Git 提交 `4b83e8e9d8d6074f18de256c12b01c7d8d88651f` 的 Android 源码为基础，按从旧平板导出的 APK（SHA-256 `1119024c72694f7ae4a72045ad3b2e3bffae56cd8c53687b845a0974f43207cc`）还原。APK 是编译产物，原始注释、局部变量名和精确源码格式无法恢复；关键业务逻辑来自 APK 的 DEX 反编译，并与仓库自带的旧 APK 逐项对照。

## 主要还原内容

- 商品条码可扫码填入或手动录入，保存时更新 `isBarcodeUpdate` 并创建 `Save_Spkfk_Barcode` 消息。
- 数据库版本从 14 调整为 15，升级时为 `advspkfk` 添加 `isBarcodeUpdate` 列。
- 还原养护记录的 ID 生成、保存方式和排序，商品查询数量上限与柜号查询规则，以及下载分片大小。
- 首页入口与原 APK 一致；更新商品详情和查询界面布局。
- 修正两张 JPEG 资源缺少文件扩展名的问题，使现有资源可由现代构建工具编译。

详见 [RECOVERY_NOTES.md](RECOVERY_NOTES.md)。

## 界面改进

- 商品查询使用统一的青绿色与浅色背景。商品列表每件仅占一行，显示名称、柜号、价格和加入按钮；点按商品可查看完整详情。
- 销售单将单号、顾客和合计压缩到窄栏，让商品明细占据主要空间；结算按钮始终可见。左滑商品露出删除按钮，右滑返回商品查询。商品行显示小计与每 10g 单价，点按数量和铅笔图标可调整数量。
- 查询页显示商品已加入的数量、销售单商品项数及当前合计；中药价格按每 10g 标注，销售单按实际克数计算小计。
- 商品详情和数量输入使用相同的卡片、输入框与按钮样式，空白的规格、单位和厂家字段会自动隐藏。
- 应用图标以中药叶片和算盘珠表示药价查询与中药方计价，原始设计稿保存在 `branding/ajmst_logo.png`。

## 构建

需要 JDK 17、Android SDK Platform 36、Build Tools 35.0.0 或更新版本。最低运行版本为 Android 11（API 30），编译和目标版本为 API 36。用 Android Studio 打开此目录，或在 Windows 命令行运行：

```bat
gradlew.bat assembleDebug
```

构建前设置 `ANDROID_HOME`，或在本机创建未纳入版本控制的 `local.properties` 并配置 `sdk.dir`。调试 APK 输出为 `build\outputs\apk\debug\AJMST_Android_Recovered-debug.apk`。调试版包名为 `com.ajmst.android.recovered`，手机上显示“AJMST 还原版”，可与已安装的原版并存。

## 数据库

数据库现在位于应用私有目录。首次启动会提示选择旧的 `AJMST.db`（旧设备上通常是 `/sdcard/AJMST.db`）；系统文件选择器授予单文件读取权限后，应用会校验版本和商品表，并复制到私有目录，原文件保持不变。可以选择空白开始。工程没有打包用户数据库。使用 `python tools/verify_database.py <数据库文件>` 可以只读检查版本与 ORM 表结构；本次提供的数据库为 `user_version=15`，5 张 ORM 表及其配置列均匹配。

## ETCM 中药材资料

`res/raw/etcm_herb_list_zh.json` 保存 [ETCM 2.0 中药材列表](http://www.tcmip.cn/ETCM2/front/#/browse/herb)；`res/raw/etcm_herb_basic_zh.json` 保存列表中每条中药材详情页的“基本信息”。资料使用网站的简体中文版本，保留来源链接及采集时间，不包含相关信息表、成分或网络分析。

更新资料时运行 `python tools/fetch_etcm_herbs.py`。脚本先下载完整列表，再逐条获取基本信息；同一时刻只发一个请求，默认两次请求开始至少间隔 1 秒，并在 `build/etcm_fetch/` 保存断点。可以分别使用 `--phase list` 和 `--phase details`，中断后重运行会从断点继续。

## 安装限制

构建产物使用调试签名，不能直接覆盖安装签名不同的原 APK。发行版配置保留原包名 `com.ajmst.android`，调试版使用独立包名，因此不会替换原版应用及其内部数据。调试版升级安装时会保留其私有数据；原版仍使用根目录数据库。新版不再直接访问外部存储根目录，也不会修改原数据库文件。
