# Tailg LSPosed 工具箱

面向台铃官方 App `com.tailg.run.intelligence` 3.5.9 的 LSPosed 模块。模块提供广告清理、隐私控制、启动优化。

## 功能

### 广告与弹窗

- 跳过开屏初始化和倒计时。
- 清空开屏资源和首页 Banner 配置。
- 拦截 App 自身升级弹窗，不影响车辆固件或 OTA。

### 隐私

- 屏蔽 `TailgRepository#collectReport(String, List)`，以正常完成的空 `Observable` 替代 `app/v2/collect/report` 请求。
- 可选跳过 `HomeActivity#initTencentBugly()`，阻止 Bugly 崩溃上报初始化。
- 两项功能互相独立，不会拦截车辆控制、定位、配置或登录接口。

### 启动优化

- 可选跳过首页阶段的腾讯 X5 配置和七鱼客服 SDK 初始化,定位服务、车辆告警推送与首页 BLE 初始化保持不变。
- 首次创建 X5 WebView 前补做官方原有的 X5 配置;首次真正打开客服前补做七鱼初始化。
- 仅延迟当前未使用的 SDK,不改写首页 Fragment 下标,也不阻断浏览器与客服功能。

### 配置面板

- 模块桌面图标拉起官方 App 内的 Material Design 3 配置面板,跟随官方 App 的明暗模式与主题强调色。
- 配置面板运行在官方 App 进程内,即时保存,重启目标 App 后生效。

## 安全与兼容

- 严格版本保护默认开启,仅允许已验证的 3.5.9 安装 Hook。
- 类、方法签名或返回类型不匹配时跳过对应 Hook,其他功能继续安装。
- 设置页和 Hook 运行在官方 App 进程内,共同读写官方 App 私有目录中的多进程 MMKV,不依赖模块 App 后台进程。
- 升级到 v1.6.0 时会将旧 LSPosed 远程配置一次性迁移到宿主 MMKV;旧配置只读保留,不会删除。
- 设置即时保存,重启目标 App 后生效。
- 不提供登录/车主认证绕过、NFC 安全绕过、OTA 安全绕过、原始 BLE 指令注入或全局硬件能力伪造。

## 默认设置

- 默认开启:模块总开关、严格版本保护、现有广告清理、App 升级弹窗拦截、使用行为上报屏蔽。
- 默认关闭:极速启动、Bugly 屏蔽、详细日志。

## 构建

本项目禁止在本地编译 APK。所有 Android 构建、单元测试和 lint 均通过 GitHub Actions 执行：

- `.github/workflows/android-build.yml`：推送或 PR 后执行单元测试、lint，并构建 debug/release APK artifact。
- `.github/workflows/android-release-signed.yml`：手动触发签名 release。
- `.github/workflows/android-release-tag.yml`：推送与 `moduleVersionName` 一致的版本 tag 后创建 GitHub Release。

当前版本由 `gradle.properties` 唯一管理：`moduleVersionCode=10904`、`moduleVersionName=v1.9.4`。

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
