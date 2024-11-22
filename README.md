# LookUp
<table>
  <tr>
    <td>
      <img src="/src/main/resources/org/fireworkrocket/lookup/icon.png" alt="GitHub" width="145" height=155">
    </td>
    <td>
      <h1>LookUp！</h1>
      <p>由Java构建的图床API 图片预览/获取应用</p>
    </td>
  </tr>
</table>

![GitHub license](https://img.shields.io/github/license/FireworkRocket/LookUp)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/FireworkRocket/LookUp)
![GitHub last commit](https://img.shields.io/github/last-commit/FireworkRocket/LookUp)
![GitHub issues](https://img.shields.io/github/issues/FireworkRocket/LookUp)

## 项目简介
本项目是一个图片处理应用，使用多个API获取图片并进行处理。项目使用Java语言开发，并使用Maven进行构建和依赖管理。

## 功能特性
- 根据API随机获取桌面壁纸
- 根据API随机获取动态壁纸
- 预览API随机图片
- 设置要获取的API
- 兼容常见API返回格式

## 使用说明
### 如何下载？
- 请前往[Releases](https://github.com/FireworkRocket/LookUp/releases)下载最新版本的LookUp JAR包

### 常见问题Q&A

- **1.为什么我下载后无法启动？**
    - 请检查您是否安装了Java，如果您安装了Java，那么您下载的.jar文件的图标看起来是一个咖啡杯，如果不是，请尝试重新安装Java
    - 此外，本软件暂不即点即用，.exe版本正在加急制作中，在此本您需要在命令行中输入`java -Xmx256M --module-path C:\path\to\your\lib --add-modules javafx.controls,javafx.fxml -jar LookUp.jar`并将`C:\path\to\your\lib`替换为您的JavaFX库路径后执行命令即可启动
    - 如果您仍然无法启动，请尝试重新下载本软件


- **2.为什么我无法获取图片/在点击刷新页面后一直报错/在页面内发现图片包含空缺？**
    - 请检查您的网络连接是否正常，如果您的网络连接正常，可能是API格式不受支持，请在验证可用性并确认返回的格式为JSON后提出[Issues](https://github.com/FireworkRocket/LookUp/issues)
    - 有些API可能返回的是Pixiv等中国大陆无法访问的网站，我们通常推荐您删除或者更换此类API，请您自行斟酌
    - 如果您更换API后仍然无法获取图片，请尝试重新启动本软件


- **3.为什么我无法设置API？**
    - 很抱歉，本软件暂时不支持自定义API，我们将在未来的版本中加入此功能


- **4.为什么我无法设置壁纸？**
    - 请检查您的操作系统是否支持设置壁纸，如果您的操作系统支持设置壁纸，可能是您的操作系统不支持JavaFX的设置壁纸功能，请尝试使用其他方法设置壁纸
    - 如果您的操作系统不支持设置壁纸，我们建议您使用Windows 10或者macOS等支持JavaFX的操作系统
    - ***请注意，动态壁纸功能暂不支持Linux、MacOS等操作系统，我们将在未来的版本中加入此功能***


- **5.为什么软件启动时，提示"错误: 缺少 JavaFX 运行时组件, 需要使用该组件来运行此应用程序"？**
    - 请检查您的JavaFX库路径是否正确，如果您的JavaFX库路径正确，可能是您的JavaFX库版本不受支持，请尝试更换JavaFX库版本
    - 如果您更换JavaFX库版本后仍然无法启动，请尝试重新下载本软件


- **6.为什么我在Linux或MacOS上使用“设置壁纸”功能报错/无法使用？**
    - 请注意，动态壁纸功能暂不支持Linux、MacOS等操作系统，我们将在未来的版本中加入此功能
    - 若您设置的是静态壁纸，请查看壁纸设置兼容性

- **7.在哪里下载JavaFX库？**
    - 请前往[JavaFX官网](https://openjfx.io/)后点击“Download”，下滑并在筛选器中选择您的操作系统和Java版本后下载JavaFX库
    - 下载后，请解压出压缩包内的所有文件，进入lib文件夹复制您的路径替换问题1中的`C:\path\to\your\lib`

## 当前版本壁纸设置兼容性
<body>
    <table>
        <thead>
            <tr>
                <th>操作系统</th>
                <th>静态壁纸</th>
                <th>动态壁纸</th>
                <th>备注</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Windows 10/8.X/7/Vista</td>
                <td>支持</td>
                <td>支持</td>
                <td></td>
            </tr>
            <tr>
                <td>Windows 11</td>
                <td>支持</td>
                <td>包含问题</td>
                <td>动态壁纸功能在Windows11 24H2上可能无法正常使用</td>
            </tr>
            <tr>
                <td>Linux</td>
                <td>支持</td>
                <td>不支持</td>
                <td>静态壁纸设置部分仅支持GNOME桌面环境，动态壁纸功能将在未来版本中加入</td>
            </tr>
            <tr>
                <td>macOS</td>
                <td>支持</td>
                <td>不支持</td>
                <td>静态壁纸设置部分未经过测试，猜测仅支持MacOS 10.6 (Snow Leopard) 及更高版本设置静态壁纸，动态壁纸功能将在未来版本中加入</td>
            </tr>
        </tbody>
    </table>
</body>

  
  
## 开发指导
### 环境要求
- 本项目开发时使用 OpenJDK 22.0.2，但理论上支持所有JDK 8及以上版本
- 建议使用Maven3.9.8及以上版本

### 构建项目

1. 克隆项目目到本地：
    ```bash
    git clone https://github.com/FireworkRocket/LookUp.git
    ```
2. 进入项目目录：
    ```bash
    cd LookUp
    ```
3. 使用Maven构建项目：
    ```bash
    mvn clean install
    ```
4. 构建成功后，将在`target`目录下生成`LookUp.jar`文件。

5. 运行`LookUp.jar`文件：
    ```bash
    java -jar LookUp.jar
    ```

### 运行项目
**本项目使用JavaFX作为图形界面，因此需要在运行时指定JavaFX的模块路径和库路径。**

推荐的JVM参数如下：
```bash
-Xmx256M --module-path C:\path\your\lib --add-modules javafx.controls,javafx.fxml
```


## 想成为此应用的贡献者？
- 或者通过提交[Issues](https://github.com/FireworkRocket/LookUp/issues)来帮助我们改进本应用
- 您可以通过提交[Pull Request](https://github.com/FireworkRocket/LookUp/pulls)
- 您也可以通过[Discussions](https://github.com/FireworkRocket/LookUp/discussions)来与我们交流
- 我们十分欢迎您的加入！

## 致谢
特别感谢以下以下提供商为提供的API接口（以下排名不分先后）：
- 💀

（有意与我合作？立即提交一个新的[Discussions](https://github.com/FireworkRocket/LookUp/discussions)）

## 正在寻找JavaDOC？
稍等几天，我们正在努力为您准备JavaDOC文档。

## 项目结构
```plaintext
│  .gitignore
│  mvnw
│  mvnw.cmd
│  pom.xml
│  README.md
│
├─.mvn
│  └─wrapper
│          maven-wrapper.jar
│          maven-wrapper.properties
└─src
    └─main
        ├─java
        │  └─org
        │      └─fireworkrocket
        │          └─lookup
        │              │  ApiConfig.java
        │              │  Config.java
        │              │  Main.java
        │              │  Untested.java
        │              │
        │              ├─exception
        │              │      DialogUtil.java
        │              │      EatException.java
        │              │      ExceptionHandler.java
        │              │
        │              ├─function
        │              │  │  Download_Manager.java
        │              │  │  PicProcessing.java
        │              │  │  ProcessUtils.java
        │              │  │
        │              │  └─wallpaperchanger
        │              │          DynamicWallpaper.java
        │              │          WallpaperChanger.java
        │              │
        │              ├─FXMLController
        │              │      FXMLLoaderUtil.java
        │              │      HomeController.java
        │              │      ImageController.java
        │              │      SettingController.java
        │              │      SingletonFactory.java
        │              │
        │              └─processor
        │                      DEFAULT_API_CONFIG.java
        │                      JSON_Data_Processor.java
        │                      JSON_Read_Configuration.java
        │                      Trust_All_Certificates.java
        │                      UserConfig.java
        │
        └─resources
            └─org
                └─fireworkrocket
                    └─lookup
                        │  Home.fxml
                        │  icon.png
                        │  log4j2.xml
                        │  styles.css
                        │
                        └─FXMLController
                                Image.fxml
                                Setting.fxml
                                test.css
                                Test_Image.png

```

## 协议
本软件采用Apache-2.0协议进行许可，详情请参阅[LICENSE](https://github.com/FireworkRocket/LookUp/blob/main/LICENSE)文件。