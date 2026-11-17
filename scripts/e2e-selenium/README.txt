大千智荟论坛 · 界面级 E2E（Selenium）离线包
================================================

一、这是什么
------------
用 Selenium 驱动真实 Chrome 浏览器，模拟用户“点开页面/登录/浏览/点赞/评论”，
逐步断言页面内容，并截图、生成 HTML 报告。
与 HTA 版（接口级 E2E）互补：
  - HTA：快、只读、适合每次部署后跑；
  - 本包：真实点击，能发现“按钮没绑定/页面没渲染”等纯界面问题。

二、运行环境
------------
  - Windows 7 SP1 及以上（同样适用于 Win10/Win11）
  - Java 8（JRE 即可；离线机器请提前安装）
  - Google Chrome 109（Win7 能装的最后一个大版本；Win10+ 可用新版）
  - 本包已自带与 Chrome 109 匹配的 chromedriver.exe

  注意：Chrome 与 chromedriver 的大版本必须一致。
  - 如果现场 Chrome 不是 109：把 driver 换成对应版本即可；
  - 如果只有 360 / Edge：需换成对应内核的 driver，建议优先安装 Chrome 109。

三、目录结构
------------
  run.bat                    启动脚本（双击即可）
  config.properties          配置（地址、账号、只读/写入模式）
  e2e-ui.jar                 测试程序（含全部依赖）
  driver/chromedriver.exe    浏览器驱动
  reports/                   运行后生成：HTML 报告 + 截图

四、怎么用
----------
  1) 打开 config.properties，至少改 baseUrl；
     需要登录类用例就填 username / password；
  2) 双击 run.bat，等待浏览器跑完（无头模式看不到窗口）；
  3) 打开 reports/report-时间.html 查看通过/失败与截图。

  生产环境建议：
    mode=readonly
    detail.enabled=false
    （只浏览列表/精华/排行/登录页，不点任何会写数据的按钮）

  预发/测试环境可开：
    mode=write          点赞、评论（并尝试删除自己刚发的评论）
    detail.enabled=true 打开帖子详情（注意会 +1 浏览量）

五、生产只读模式会做什么
------------------------
  - 打开登录页、断言“登录”文案；
  - 登录（只签发 token，不写业务库）；
  - 论坛列表渲染出帖子卡片；
  - 精华页、排行榜、个人中心、头像跳转；
  - 每一步截图，失败时也截图。

六、常见问题
------------
  1) 报 “session not created: This version of ChromeDriver only supports Chrome version 109”
     → Chrome 版本与 driver 不一致，换匹配的 chromedriver。

  2) 双击没反应 / 闪退
     → 命令行运行 run.bat，查看 Java 是否安装（java -version）。

  3) 页面空白或一直超时
     → 确认 baseUrl 能从本机访问；内网机器先浏览器打开一次；
       如为 HTTPS 自签证书，程序已允许不安全证书。

  4) 登录失败
     → 确认账号未被锁定、密码正确；生产建议使用 is_test_account=1 的专用验收账号。

七、安全边界
------------
  - readonly 模式不会点赞/评论/发帖；
  - write 模式会创建数据，只应在测试库使用；
  - 本程序不读取数据库，不修改配置，不上传文件。

八、定制
--------
  用例源码：scripts/e2e-selenium/src/main/java/com/daqian/e2e/Main.java
  重新打包（开发机）：
    cd scripts/e2e-selenium
    mvn -DskipTests package
    产物 target/bbs-e2e-ui.jar 复制为包内 e2e-ui.jar

九、负向自检（已内置，无需额外命令）
------------------------------------
  目的：确认“判据本身”能变红，避免“全绿但其实是装饰品”。

  每次运行 run.bat 时，会自动先执行 4 项只读检查：
    1) 基线：登录页能渲染出“登录”；
    2) 错误 baseUrl（127.0.0.1:1）必须失败；
    3) 错误断言（__MUST_NOT_EXIST__）必须抛错；
    4) 未登录访问业务接口必须返回 401。

  报告与退出码：
    - 负向结果以 MUT-PASS / MUT-FAIL 标记，单独统计，不计入业务失败；
    - 任一项负向检查失败（说明判据坏了），整体退出码仍为 1。

  关闭方式（不建议）：
    在 config.properties 中设置 selfcheck.enabled=false


