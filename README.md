# Tailg LSPosed AdBlock

针对 `com.tailg.run.intelligence` 的 LSPosed 模块，目标是禁用开屏广告、首页 Banner 广告与 App 升级弹窗。

## 实现方式

- 入口类：`com.tailg.lsposed.adblock.TailgAdBlockModule`
- 多 Hook 兜底：
  - `SplashActivity#setupView()` -> `setupViewNo()`
  - `SplashActivity#countDown()` -> `countDownNo()`
  - `ConfigGetBean#getIsShow()` -> `"0"`
  - `ConfigGetBean#getHomeResource()/getFootResource()` -> `""`
  - `ConfigGetBean#getDurationTime()` -> `"0"`
  - `ConfigGetBean#getBanners()/getBannerOssIds()` -> `""`（首页 Banner 广告；`ControlViewModel` 中 `isNotEmptyString(getBanners())` 为假即不渲染）
  - `CheckAppVersionBean#getIsPop()` -> `"0"`、`getIsForce()` -> `"0"`（App 升级弹窗；`HomeActivity` 仅在 `"1".equals(getIsPop())` 时弹框）
- 版本检测：运行时记录目标应用 `versionName/versionCode`。
- 安全策略：
  - 方法签名/返回类型不匹配时自动跳过对应 Hook，不中断其他 Hook
  - 支持按版本前缀白名单校验（可通过配置切换严格模式）

## 界面

设置页采用 **Material 3（`Theme.Material3.DayNight`）**，观感参考 HookVip：

- 开关按「总控 / 开屏广告 / 首页·弹窗 / 调试」分组为圆角卡片，每行标题 + 副标题说明。
- **即时保存**：拨动开关立即写入 `tailg_adblock`，无需保存按钮（重启目标应用后生效）。
- **自动明暗（DayNight）**；Android 12+ 由 `DynamicColors` 套用壁纸取色（Material You）。
- **联动置灰**：关闭「启用模块」置灰其余项；关闭「拦截开屏广告配置」（`hook_config_bean`）置灰其从属的清空资源/倒计时归零/首页 Banner。
- UI 由 `MainActivity` 中的开关表数据驱动，新增开关只需加一行 + 两条字符串。

## 可配置开关

模块 App 内置设置页（Launcher 图标），配置保存在 `tailg_adblock`：

- `enable_module`：总开关
- `strict_version_guard`：仅在受支持版本启用 Hook（默认开启）
- `hook_setup_view`：启用 `setupView` 重定向
- `hook_count_down`：启用 `countDown` 重定向
- `hook_config_bean`：启用 `ConfigGetBean` Hook（开屏/Banner 总开关）
- `force_empty_res`：强制清空开屏资源 URL
- `force_duration_zero`：强制倒计时为 0
- `force_empty_banner`：清空首页 Banner 广告（需 `hook_config_bean` 开启，默认开启）
- `hook_app_update`：拦截 App 升级弹窗（默认开启；仅作用于 `CheckAppVersionBean`，不影响固件/OTA 升级）
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
- 设置页使用 `com.google.android.material:material:1.12.0`（Material 3）；模块 Hook 逻辑本身不依赖它。
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
