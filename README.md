# Tailg LSPosed 工具箱

面向台铃官方 App `com.tailg.run.intelligence` 3.5.9 的 LSPosed 模块。模块提供广告清理、隐私控制、轨迹导出、隐藏数据页入口、首页精简、启动优化、蓝牙连接恢复、车辆能力诊断和感应距离阈值覆盖。

## 功能

### 广告与弹窗

- 跳过开屏初始化和倒计时。
- 清空开屏资源和首页 Banner 配置。
- 拦截 App 自身升级弹窗，不影响车辆固件或 OTA。

### 隐私

- 屏蔽 `TailgRepository#collectReport(String, List)`，以正常完成的空 `Observable` 替代 `app/v2/collect/report` 请求。
- 可选跳过 `HomeActivity#initTencentBugly()`，阻止 Bugly 崩溃上报初始化。
- 两项功能互相独立，不会拦截车辆控制、定位、配置或登录接口。

### 轨迹导出

- 在官方轨迹详情页标题栏增加“导出”入口。
- 支持 GPX 1.1 和 CSV，导出官方返回的原始 GPS/WGS84 坐标。
- 通过官方 App 已有的 FileProvider 和 Android 分享面板保存，不上传到模块服务器。
- 可选移除起点和终点附近各 200 米；裁剪后不足两个点时拒绝导出，避免隐私选项静默失效。
- 临时导出文件位于官方 App 的 `external-files/shareData/tailg_export`，超过 24 小时的模块导出文件会在下次导出时清理。

### 首页与诊断

- 可隐藏“圈子”和“商城”，不删除 Fragment、不修改官方固定下标。
- 可将底部导航的“爱车”和“服务”互换位置；只调整 View 顺序，控件 ID、点击逻辑和 Fragment 下标保持不变。
- 长按首页“爱车”打开只读诊断页，展示车型、在线/设防状态、电量、里程、GPS 时间、能力位和 RSSI 参数。
- BLE 名称会脱敏；PIN、MAC、IMEI、车架号、SIM、MQTT 凭据、链接码和位置坐标不会显示。
- 默认在官方 App 的“我的 → 设置”中、账号功能上方显示“Tailg 工具箱”入口。点击后直接在官方 App 内打开完整配置面板，不切换到模块 App；可在总控中关闭该入口。
- 模块桌面图标只负责拉起官方 App 内的同一配置面板；即使关闭模块总开关或隐藏官方入口，也可通过桌面图标恢复配置。
- 配置面板采用 Material Design 3 布局，并跟随官方 App 的明暗模式与主题强调色。

### 隐藏数据与车辆页面

- 可放开首页月度骑行数据入口；仍保留官方设备检查、Presenter 就绪检查及本地/云端页面分流。
- 可让 AI 骑行记录详情把制动力能力参数传给轨迹回放页；只显示官方已返回的数据，不生成缺失的制动力数据。
- 可直接在官方 App 内的 Material 3 配置面板点击“电池信息”，按当前车型自动打开普通、C39、TLV 或 BMS 电池页；无法安全判断的非 GPS 车型不会误跳转。
- 可在主车 TBox 设置底部的“Tailg 工具箱”分组补充“电池动态”入口；页面只读取官方 UUID 对应的服务端历史，后端没有记录时可能为空。
- 可在车辆已绑定、且能力配置明确上报 `tbox_voice_cust == 1` 时补充“自定义音效”入口；录音权限、音效库、上传和保存均复用官方实现。
- 这些功能互相独立；四个功能开关默认关闭，“电池信息”为直接操作项。模块不会全局改写 `DeviceFunction`，也不会伪造 ECU/BLE 能力或发送自定义控车指令。

### 启动优化

- 可选跳过首页阶段的腾讯 X5 配置和七鱼客服 SDK 初始化，定位服务、车辆告警推送与首页 BLE 初始化保持不变。
- 首次创建 X5 WebView 前补做官方原有的 X5 配置；首次真正打开客服前补做七鱼初始化。
- 仅延迟当前未使用的 SDK，不改写首页 Fragment 下标，也不阻断浏览器与客服功能。

### 蓝牙连接恢复

- 返回首页或系统蓝牙重新开启后，按设置的间隔执行有限次数恢复，默认间隔 15 秒、最多 3 轮。
- 间隔支持 5–60 秒、步进 5 秒，最多恢复轮数支持 1–5 次。
- 已登录或正在连接时不会重复发起；权限不足或蓝牙关闭时静默停止，不弹出模块权限框。
- 按车型复用官方 `BleHandler`、`initBleTLink()` 或 `initBleTLinkQgj()`，不发送自定义控车指令，也不主动断开现有连接。

### 感应距离

- 使用两个 0.5 米步进的滑块设置靠近解锁与远离落锁阈值。
- 落锁阈值始终至少比解锁阈值大 0.5 米，范围为 0.5 至 10 米。
- 实现只覆盖旧 `BleConnectService` 读取的 `CarControlInfoBean#getMinRssiDistance()` 和 `getMaxRssiDistance()`；不会主动开启感应解锁、启动 RSSI 轮询或发送车辆指令。
- 新车型的 `TLinkBleManager#setModeDistance(int)` 使用 1–30 原生档位，官方车辆设置页已经提供对应滑块。模块不会把该档位武断换算成米，也不会覆盖用户在官方页面选择的值。
- 旧 RSSI 调用链可能只对部分车型生效，默认关闭，需结合真机日志验证。

## 安全与兼容

- 严格版本保护默认开启，仅允许已验证的 3.5.9 安装 Hook。
- 类、方法签名或返回类型不匹配时跳过对应 Hook，其他功能继续安装。
- 设置页和 Hook 运行在官方 App 进程内，共同读写官方 App 私有目录中的多进程 MMKV，不依赖模块 App 后台进程。
- 升级到 v1.6.0 时会将旧 LSPosed 远程配置一次性迁移到宿主 MMKV；旧配置只读保留，不会删除。
- 设置即时保存，重启目标 App 后生效。
- 不提供登录/车主认证绕过、NFC 安全绕过、OTA 安全绕过、原始 BLE 指令注入或全局硬件能力伪造。

## 默认设置

- 默认开启：模块总开关、严格版本保护、官方设置入口、现有广告清理、App 升级弹窗拦截、使用行为上报屏蔽、轨迹导出、轨迹首尾隐私裁剪、车辆能力诊断。
- 默认关闭：极速启动、蓝牙连接恢复、Bugly 屏蔽、首页导航精简、爱车与服务互换、月度骑行数据、制动力数据、电池动态入口、自定义车辆音效、感应距离覆盖、详细日志。

## 构建

本项目禁止在本地编译 APK。所有 Android 构建、单元测试和 lint 均通过 GitHub Actions 执行：

- `.github/workflows/android-build.yml`：推送或 PR 后执行单元测试、lint，并构建 debug/release APK artifact。
- `.github/workflows/android-release-signed.yml`：手动触发签名 release。
- `.github/workflows/android-release-tag.yml`：推送与 `moduleVersionName` 一致的版本 tag 后创建 GitHub Release。

当前版本由 `gradle.properties` 唯一管理：`moduleVersionCode=10902`、`moduleVersionName=v1.9.2`。

## 启用

1. 安装 GitHub Actions 生成的模块 APK。
2. 在 LSPosed 中启用模块。
3. 作用域只勾选 `com.tailg.run.intelligence`。
4. 重启目标 App。

## 签名发布

签名工作流需要以下 GitHub Secrets：

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

发布 tag 必须严格等于 `gradle.properties` 中的 `moduleVersionName`。
