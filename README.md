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
- 销售单将单号、顾客和合计压缩到窄栏，让商品明细占据主要空间；结算按钮始终可见。左滑商品露出删除按钮，右滑返回商品查询。商品行显示小计与每 10g 单价，并可点按“数量 · 修改”调整数量。
- 查询页显示商品已加入的数量、销售单商品项数及当前合计；中药价格按每 10g 标注，销售单按实际克数计算小计。
- 商品详情和数量输入使用相同的卡片、输入框与按钮样式，空白的规格、单位和厂家字段会自动隐藏。
- 应用图标以中药叶片和算盘珠表示药价查询与中药方计价，原始设计稿保存在 `branding/ajmst_logo.png`。

## 构建

需要 JDK 11、Android SDK Platform 19、Build Tools 33.0.2。用 Android Studio 打开此目录，或在 Windows 命令行运行：

```bat
gradlew.bat assembleDebug
```

构建前设置 `ANDROID_HOME`，或在本机创建未纳入版本控制的 `local.properties` 并配置 `sdk.dir`。调试 APK 输出为 `build\outputs\apk\debug\AJMST_Android_Recovered-debug.apk`。调试版包名为 `com.ajmst.android.recovered`，手机上显示“AJMST 还原版”，可与已安装的原版并存。

## 数据库

应用源码中的数据库路径为外部存储根目录下的 `AJMST.db`（旧设备上通常是 `/sdcard/AJMST.db`）。工程没有打包用户数据库。使用 `python tools/verify_database.py <数据库文件>` 可以只读检查版本与 ORM 表结构；本次提供的数据库为 `user_version=15`，5 张 ORM 表及其配置列均匹配。

## 安装限制

构建产物使用调试签名，不能直接覆盖安装签名不同的原 APK。发行版配置保留原包名 `com.ajmst.android`，调试版使用独立包名，因此不会替换原版应用及其内部数据。两个应用按源码均使用外部存储根目录的同一份 `AJMST.db`，请避免同时操作。`versionCode=2`、`versionName=2.0`、`minSdkVersion=14` 和 `targetSdkVersion=14` 与原 APK 保持一致。已验证源码编译、Gradle 打包和数据库结构；2026-09-23 已将调试版安装到 Android 16 手机，并确认首页能启动及显示商品数据。
