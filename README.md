# 学习积分 · 日历（Android App）

一个**纯本地**的个人学习 / 生活记录 App：用日历记录每天的积分与开销，配合四象限待办、每日规划、艾宾浩斯复习提醒和数据趋势图。

- **零第三方依赖**：只用 Android 系统自带 API（没有 androidx、没有网络库）
- **数据只存在手机本地**：SharedPreferences，不联网、不上传
- **可导出 / 导入备份**：换机或重装时可恢复

## 功能

### 每日积分
- 月历视图，日期下方显示当天积分与开销
- 点日期切换 / 编辑当天记录
- 学习、工作、生活（睡眠 / 饮食 / 运动）积分
- **自定义积分类型**：可设「数据总览 / 底部显示 / 日历显示」，支持勾选项计分（长按勾选项可改分值或删除）
- **记账**：名称 + 价格，显示当日合计
- **统计**：近 7 天日均、累计总览、近 30 天开销日均 / 合计
- **复习项目**：每天可记录要复习的内容，勾选完成，长按删除

### 数据总览
- 年 / 月 / 周的积分总计与日均（月、周可切换）
- 年 / 月 / 周趋势图（带数值标签、◀ ▶ 翻页）
- **账本总览**：今日 / 本月 / 今年 / 日均支出 + 支出趋势 + 按日期倒序的明细

### 四象限待办
- 四象限记录待办，右下角 `+` 添加
- 点一下切换完成（划线变灰）
- **长按菜单**：更改重要程度 / 链接到日期 / 删除
- 待办可链接**多个日期**，名称前自动显示日期
- 每日积分页会显示「链接到当天的待办」

### 规划与复习提醒
- 今日规划、明日规划（可嵌入待办 `[[项目名]]`，点击即切换完成）
- 一键「移到今日」
- 艾宾浩斯复习提醒（1 / 2 / 4 / 7 / 15 / 30 天），显示日期 + 对应复习项目

### 备份
- 顶部【导入/导出】：导出为 JSON 文件、从 JSON 恢复
- 使用系统文件选择器（SAF），**不需要存储权限**

## 环境要求

只需要 **Android Studio**（任一较新版本：Hedgehog / Iguana / Jellyfish / Koala / Ladybug 等均可）。
不需要装任何第三方库，工程只用了安卓系统自带 API。

- minSdk 24，compileSdk 34，AGP 8.5.2，Gradle 8.7，Kotlin 1.9.24

## 构建与运行

1. `File > Open` 选择本项目目录
2. 等待 Gradle 同步（首次会下载 Gradle 与 AGP，需要联网，几分钟）
3. 手机开启「开发者选项 → USB 调试」并连接，或启动模拟器
4. 点 ▶ 运行

> 若提示 Gradle wrapper 缺失，执行一次 `File > Sync Project with Gradle Files`；
> 想用命令行构建，安装 Gradle 后在项目根目录执行 `gradle wrapper` 生成 `gradlew`。

### 打包 APK

- **调试版**：`Build > Build Bundle(s)/APK(s) > Build APK(s)`，完成后点提示里的 `locate` 找到 `app-debug.apk`
- **发布版**：`Build > Generate Signed Bundle / APK`，需要先创建签名证书

## 网络 / 代理问题（Gradle 同步失败时看这里）

1. **Android Studio 弹「HTTP Proxy」对话框**：
   - 没有代理 / VPN → 选 **No proxy**，点 OK
   - 有代理（如 Clash）→ 选 **Manual proxy configuration** → HTTP → Host 填 `127.0.0.1`、Port 填代理端口（常见 7890）
   - 校园网 / 公司网有代理 → 填对应的地址和端口

2. **工程已预置阿里云镜像**：`settings.gradle.kts` 里 Google / Maven 仓库已改成阿里云镜像优先（官方仓库兜底），国内同步会快很多。

3. **Gradle 本体下载慢 / 失败**：打开 `gradle/wrapper/gradle-wrapper.properties`，把 `distributionUrl` 换成文件里注释的华为云镜像地址。

4. **SDK 34 未安装**：按提示点击安装即可，或把 `app/build.gradle.kts` 里 `compileSdk` / `targetSdk` 改成你已装的版本。

## 数据与隐私

- 所有数据保存在本机 `SharedPreferences`（键名 `studypoints`），**不上传任何服务器**
- 唯一的网络行为是 Gradle 构建时下载依赖
- 备份文件是纯 JSON，包含你的全部记录，**分享前请留意隐私**

## 目录结构

```
StudyPointsApp/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/studypoints/
        │   ├── MainActivity.kt      # 全部界面与逻辑
        │   └── TrendChartView.kt    # 柱状趋势图（Canvas 手绘）
        └── res/
            ├── layout/              # 页面与弹窗布局
            ├── values/              # 颜色、字符串、主题
            └── drawable/            # 图标与背景
```

## 许可

[MIT](LICENSE)
