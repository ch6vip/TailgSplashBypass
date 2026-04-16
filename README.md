# Tailg LSPosed AdBlock

针对 `com.tailg.run.intelligence` 的 LSPosed 模块，目标是禁用开屏广告倒计时分支。

## 实现方式

- 入口类：`com.tailg.lsposed.adblock.TailgAdBlockModule`
- Hook 点：`com.tailg.run.intelligence.model.splash.activity.SplashActivity#setupView()`
- 行为：拦截 `setupView()`，改为直接调用 `setupViewNo()`，并短路原方法。

## 构建

1. 用 Android Studio 打开本目录 `lsposed-tailg-adblock`。
2. 同步 Gradle。
3. 构建 `app` 模块生成 APK（`debug` 或 `release`）。

## 启用

1. 安装模块 APK。
2. 在 LSPosed 中启用模块。
3. 作用域仅勾选：`com.tailg.run.intelligence`（模块已内置静态 scope）。
4. 重启目标应用（必要时重启系统）。

## 兼容性说明

- 当前使用 Modern Xposed API（`io.github.libxposed:api:101.0.1`）。
- `minSdk` 设为 26。
