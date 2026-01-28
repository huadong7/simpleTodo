# SimpleTodo 构建指南 (Build Guide)

由于当前环境未预装 Android SDK，无法直接生成 APK 文件。您需要将此项目源码传输到安装了 **Android Studio** 的电脑上进行编译。

## 1. 传输源码
将 `c:\pro\yt\SimpleTodo` 文件夹完整复制到您的开发电脑上。

## 2. 打开项目
1. 启动 **Android Studio**。
2. 点击 **Open**，选择 `SimpleTodo` 文件夹。
3. 等待 Gradle Sync 完成（初次打开可能需要下载依赖，需保持网络畅通）。

## 3. 生成 APK
1. 在菜单栏点击 **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**。
2. 等待编译完成。
3. 编译成功后，右下角会弹出提示，点击 **locate** 即可找到 `app-debug.apk` 文件。

## 4. 安装到手机
1. 将 `app-debug.apk` 发送到您的 HyperOS 手机（通过 USB、微信、QQ 等）。
2. 在手机上点击 APK 文件进行安装。
   - 如果提示“禁止安装未知来源应用”，请在设置中允许。
   - HyperOS 可能会进行安全扫描，选择“继续安装”即可。

## 5. 常见问题
- **JDK 版本**：请确保 Android Studio 设置的 Gradle JDK 版本为 17 或更高（Settings > Build, Execution, Deployment > Build Tools > Gradle）。
- **SDK 版本**：项目配置为 Compile SDK 34 (Android 14)，请确保 Android Studio 已下载相应的 SDK Platform。
