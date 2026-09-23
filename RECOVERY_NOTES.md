# APK 对照记录

基准：仓库旧构建 `AJMST/AJMST_Android/bin/AJMST_Android.apk`。目标：从旧平板导出的 APK。两者包名和版本相同，目标 APK 的 `classes.dex`、资源表与旧构建不同。

| 范围 | APK 中确认的变化 | 新工程位置 |
| --- | --- | --- |
| 数据库 | 版本 15；`advspkfk.isBarcodeUpdate` 列升级 | `src/com/ajmst/android/service/DatabaseHelper.java`、`res/raw/ormlite_config.txt` |
| 商品条码 | 手动编辑、扫码填入、保存标记和队列上报 | `AdvSpkfk.java`、`SpkfkService.java`、`MsgQueueService.java`、`SpkfkDetailActivity.java`、`SpkfkSelectActivity.java`、`IWebServiceName.java` |
| 养护记录 | `createOrUpdate`、导入 ID、柜号和商品编号排序 | `MaintainService.java`、`MaintainActivity.java` |
| 商品查询 | 上限 100 改为 1000；个位柜号补零 | `SpkfkService.java` |
| 下载 | 每批 600 改为 4000；间隔 3 秒改为 5 秒 | `ConfigActivity.java` |
| 首页 | APK 中仅添加商品查询页，不启动 `MsgSendService` | `MainActivity.java` |
| 界面 | 条码输入框和录入按钮；商品查询提示文字 | `res/layout/activity_spkfk_detail.xml`、`res/layout/activity_spkfk_select.xml` |

其余应用 Java 文件沿用 Git 原源码。APK 中多数其他方法的二进制差异由新资源 ID 导致；通过分别反编译新旧 APK 的应用代码比对后，未发现额外业务逻辑变化。保留了 APK 自身的行为，包括可能存在的旧实现问题，没有凭推测改写业务规则。
