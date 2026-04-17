# Tailg LSPosed AdBlock

针对 `com.tailg.run.intelligence` 的 LSPosed 模块，目标是禁用开屏广告倒计时分支。

## 实现方式

- 入口类：`com.tailg.lsposed.adblock.TailgAdBlockModule`
- 多 Hook 兜底：
  - `SplashActivity#setupView()` -> `setupViewNo()`
  - `SplashActivity#countDown()` -> `countDownNo()`
  - `ConfigGetBean#getIsShow()` -> `"0"`
  - `ConfigGetBean#getHomeResource()/getFootResource()` -> `""`
  - `ConfigGetBean#getDurationTime()` -> `"0"`
- 版本检测：运行时记录目标应用 `versionName/versionCode`。
- 安全策略：
  - 方法签名/返回类型不匹配时自动跳过对应 Hook，不中断其他 Hook
  - 支持按版本前缀白名单校验（可通过配置切换严格模式）

## 可配置开关

模块 App 内置设置页（Launcher 图标），配置保存在 `tailg_adblock`：

- `enable_module`：总开关
- `strict_version_guard`：仅在受支持版本启用 Hook（默认开启）
- `hook_setup_view`：启用 `setupView` 重定向
- `hook_count_down`：启用 `countDown` 重定向
- `hook_config_bean`：启用 `ConfigGetBean` Hook
- `force_empty_res`：强制清空开屏资源 URL
- `force_duration_zero`：强制倒计时为 0
- `verbose_log`：输出详细日志（默认关闭）

## 构建

1. 用 Android Studio 打开本目录 `lsposed-tailg-adblock`。
2. 同步 Gradle。
3. 构建 `app` 模块生成 APK（`debug` 或 `release`）。

命令行构建（使用 Gradle Wrapper）：

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

仓库已提供 GitHub Actions：

- `.github/workflows/android-build.yml`：push/PR 自动编译 debug+release
- `.github/workflows/android-release-signed.yml`：手动触发签名 release

## 启用

1. 安装模块 APK。
2. 在 LSPosed 中启用模块。
3. 作用域仅勾选：`com.tailg.run.intelligence`（模块已内置静态 scope）。
4. 重启目标应用（必要时重启系统）。

## 兼容性说明

- 当前使用 Modern Xposed API（`io.github.libxposed:api:101.0.1`）。
- `minSdk` 设为 26。

## 签名发布（GitHub）

手动触发 `Android Release (Signed)` 前，请在仓库 Secrets 中配置：

- `SIGNING_KEYSTORE_BASE64`：keystore 文件 base64
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

## 自动发布到 Releases（推荐）

仓库已提供 `.github/workflows/android-release-tag.yml`：

- 触发条件：`push` 一个形如 `versionCode-versionName` 的 tag（例如 `100-v1.0.0`）
- 行为：先校验 tag 与 `app/build.gradle` 的版本配置一致，再自动签名构建 release APK 并上传到 GitHub Releases 对应 tag

发版命令示例：

```bash
git tag 100-v1.0.0
git push origin 100-v1.0.0
```

版本号规则：

- `app/build.gradle` 中显式配置了 `moduleVersionCode` 和 `moduleVersionName`。
- tag 必须严格等于 `${moduleVersionCode}-${moduleVersionName}`；不一致会在构建时直接失败。
- 当前示例配置为：`moduleVersionCode=100`、`moduleVersionName=v1.0.0`，因此 tag 必须是 `100-v1.0.0`。
