using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace DaqianE2E
{
    /// <summary>
    /// 大千智荟论坛 · 界面级 E2E 桌面启动器（WinForms，.NET 4.0+ / Win7 可用）
    /// 负责：编辑配置、启动内置 JRE 运行 Selenium 测试、实时显示日志、打开报告。
    /// </summary>
    public class Launcher : Form
    {
        private readonly string baseDir;
        private TextBox txtBaseUrl;
        private TextBox txtApiBase;
        private TextBox txtUser;
        private TextBox txtPassword;
        private ComboBox cboMode;
        private CheckBox chkHeadless;
        private CheckBox chkDetail;
        private Button btnStart;
        private Button btnStop;
        private Button btnOpenReports;
        private Button btnSave;
        private TextBox txtLog;
        private Label lblStatus;
        private Process running;

        public Launcher()
        {
            baseDir = AppDomain.CurrentDomain.BaseDirectory;
            BuildUi();
            LoadConfigToUi();
        }

        private void BuildUi()
        {
            Text = "大千智荟论坛 · 界面级 E2E";
            Width = 860;
            Height = 620;
            StartPosition = FormStartPosition.CenterScreen;
            Font = new Font("Microsoft YaHei", 9F);
            MinimumSize = new Size(720, 480);

            var top = new TableLayoutPanel();
            top.Dock = DockStyle.Top;
            top.Height = 176;
            top.ColumnCount = 4;
            top.RowCount = 5;
            top.Padding = new Padding(12, 10, 12, 4);
            top.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 92));
            top.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));
            top.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 92));
            top.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 50));

            top.Controls.Add(Label("前端地址"), 0, 0);
            txtBaseUrl = Input();
            top.Controls.Add(txtBaseUrl, 1, 0);
            top.Controls.Add(Label("API 地址"), 2, 0);
            txtApiBase = Input();
            txtApiBase.Width = 260;
            top.Controls.Add(txtApiBase, 3, 0);

            top.Controls.Add(Label("验收账号"), 0, 1);
            txtUser = Input();
            top.Controls.Add(txtUser, 1, 1);
            top.Controls.Add(Label("密码"), 2, 1);
            txtPassword = Input();
            txtPassword.UseSystemPasswordChar = true;
            top.Controls.Add(txtPassword, 3, 1);

            top.Controls.Add(Label("模式"), 0, 2);
            cboMode = new ComboBox();
            cboMode.DropDownStyle = ComboBoxStyle.DropDownList;
            cboMode.Items.AddRange(new object[] { "readonly（只读，生产推荐）", "write（含点赞/评论，仅测试库）" });
            cboMode.SelectedIndex = 0;
            cboMode.Dock = DockStyle.Fill;
            top.Controls.Add(cboMode, 1, 2);
            var opts = new FlowLayoutPanel();
            opts.Dock = DockStyle.Fill;
            chkHeadless = new CheckBox { Text = "无头运行", AutoSize = true, Checked = true };
            chkDetail = new CheckBox { Text = "打开帖子详情（会 +1 浏览量）", AutoSize = true };
            opts.Controls.Add(chkHeadless);
            opts.Controls.Add(chkDetail);
            top.Controls.Add(opts, 2, 2);
            top.SetColumnSpan(opts, 2);

            var actions = new FlowLayoutPanel();
            actions.Dock = DockStyle.Fill;
            btnStart = new Button { Text = "开始验收", Width = 110, Height = 30 };
            btnStart.Click += OnStart;
            btnStop = new Button { Text = "停止", Width = 80, Height = 30, Enabled = false };
            btnStop.Click += OnStop;
            btnOpenReports = new Button { Text = "打开报告目录", Width = 110, Height = 30 };
            btnOpenReports.Click += OnOpenReports;
            btnSave = new Button { Text = "保存配置", Width = 90, Height = 30 };
            btnSave.Click += OnSave;
            actions.Controls.Add(btnStart);
            actions.Controls.Add(btnStop);
            actions.Controls.Add(btnOpenReports);
            actions.Controls.Add(btnSave);
            top.Controls.Add(actions, 0, 3);
            top.SetColumnSpan(actions, 4);

            var tip = new Label();
            tip.AutoSize = true;
            tip.ForeColor = Color.FromArgb(110, 110, 110);
            tip.Text = "负向自检已内置：每次运行先验证判据能变红（错误地址/错误断言/未登录 401），失败单独统计。";
            top.Controls.Add(tip, 0, 4);
            top.SetColumnSpan(tip, 4);

            txtLog = new TextBox();
            txtLog.Multiline = true;
            txtLog.ReadOnly = true;
            txtLog.ScrollBars = ScrollBars.Vertical;
            txtLog.Dock = DockStyle.Fill;
            txtLog.BackColor = Color.FromArgb(15, 23, 42);
            txtLog.ForeColor = Color.FromArgb(214, 226, 245);
            txtLog.Font = new Font("Consolas", 9F);

            lblStatus = new Label();
            lblStatus.Dock = DockStyle.Bottom;
            lblStatus.Height = 24;
            lblStatus.Text = "就绪";
            lblStatus.Padding = new Padding(12, 5, 0, 0);

            Controls.Add(txtLog);
            Controls.Add(lblStatus);
            Controls.Add(top);
        }

        private static Label Label(string text)
        {
            return new Label { Text = text, AutoSize = true, TextAlign = ContentAlignment.MiddleLeft, Padding = new Padding(0, 6, 0, 0) };
        }

        private static TextBox Input()
        {
            return new TextBox { Dock = DockStyle.Fill, Margin = new Padding(3, 4, 12, 4) };
        }

        /* ---------------- 配置 ---------------- */

        private string ConfigPath { get { return Path.Combine(baseDir, "config.properties"); } }

        private Dictionary<string, string> ReadConfig()
        {
            var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            if (!File.Exists(ConfigPath)) return map;
            foreach (var raw in File.ReadAllLines(ConfigPath, Encoding.UTF8))
            {
                var line = raw.Trim();
                if (line.Length == 0 || line.StartsWith("#")) continue;
                int idx = line.IndexOf('=');
                if (idx <= 0) continue;
                map[line.Substring(0, idx).Trim()] = line.Substring(idx + 1).Trim();
            }
            return map;
        }

        private void LoadConfigToUi()
        {
            var cfg = ReadConfig();
            txtBaseUrl.Text = Get(cfg, "baseUrl", "http://127.0.0.1/bbs-user/");
            txtApiBase.Text = Get(cfg, "apiBase", "");
            txtUser.Text = Get(cfg, "username", "");
            txtPassword.Text = Get(cfg, "password", "");
            cboMode.SelectedIndex = "write".Equals(Get(cfg, "mode", "readonly"), StringComparison.OrdinalIgnoreCase) ? 1 : 0;
            chkHeadless.Checked = !"false".Equals(Get(cfg, "headless", "true"), StringComparison.OrdinalIgnoreCase);
            chkDetail.Checked = "true".Equals(Get(cfg, "detail.enabled", "false"), StringComparison.OrdinalIgnoreCase);
        }

        private static string Get(Dictionary<string, string> map, string key, string def)
        {
            string v;
            return map.TryGetValue(key, out v) ? v : def;
        }

        private void SaveUiToConfig()
        {
            var cfg = ReadConfig();
            cfg["baseUrl"] = txtBaseUrl.Text.Trim();
            cfg["apiBase"] = txtApiBase.Text.Trim();
            cfg["username"] = txtUser.Text.Trim();
            cfg["password"] = txtPassword.Text;
            cfg["mode"] = cboMode.SelectedIndex == 1 ? "write" : "readonly";
            cfg["headless"] = chkHeadless.Checked ? "true" : "false";
            cfg["detail.enabled"] = chkDetail.Checked ? "true" : "false";
            if (!cfg.ContainsKey("detail.url")) cfg["detail.url"] = "";
            if (!cfg.ContainsKey("driver.path")) cfg["driver.path"] = "driver/chromedriver.exe";
            if (!cfg.ContainsKey("chrome.binary")) cfg["chrome.binary"] = "";
            if (!cfg.ContainsKey("timeout")) cfg["timeout"] = "20";
            if (!cfg.ContainsKey("screenshot")) cfg["screenshot"] = "true";
            cfg["selfcheck.enabled"] = "true";

            var sb = new StringBuilder();
            sb.AppendLine("# 大千智荟论坛 · 界面级 E2E 配置（桌面启动器自动维护）");
            foreach (var kv in cfg)
            {
                sb.Append(kv.Key).Append('=').AppendLine(kv.Value);
            }
            File.WriteAllText(ConfigPath, sb.ToString(), new UTF8Encoding(false));
        }

        /* ---------------- 运行 ---------------- */

        private void OnStart(object sender, EventArgs e)
        {
            if (running != null && !running.HasExited) return;
            string baseUrl = txtBaseUrl.Text.Trim();
            if (baseUrl.Length == 0)
            {
                MessageBox.Show("请填写前端地址", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }
            if (!baseUrl.StartsWith("http://") && !baseUrl.StartsWith("https://"))
            {
                MessageBox.Show("前端地址需以 http:// 或 https:// 开头", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            string jar = Path.Combine(baseDir, "e2e-ui.jar");
            if (!File.Exists(jar))
            {
                MessageBox.Show("缺少 e2e-ui.jar，请确认与启动器同目录", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }
            string java = Path.Combine(baseDir, Path.Combine("jre", Path.Combine("bin", "java.exe")));
            if (!File.Exists(java)) java = "java";

            try
            {
                SaveUiToConfig();
            }
            catch (Exception ex)
            {
                MessageBox.Show("保存配置失败：" + ex.Message, "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            txtLog.Clear();
            var psi = new ProcessStartInfo();
            psi.FileName = java;
            psi.Arguments = "-Dfile.encoding=UTF-8 -jar \"" + jar + "\"";
            psi.WorkingDirectory = baseDir;
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;

            try
            {
                running = new Process();
                running.StartInfo = psi;
                running.OutputDataReceived += delegate(object s, DataReceivedEventArgs a) { AppendLog(a.Data); };
                running.ErrorDataReceived += delegate(object s, DataReceivedEventArgs a) { AppendLog(a.Data); };
                running.EnableRaisingEvents = true;
                running.Exited += OnProcessExited;
                running.Start();
                running.BeginOutputReadLine();
                running.BeginErrorReadLine();
                btnStart.Enabled = false;
                btnStop.Enabled = true;
                btnSave.Enabled = false;
                lblStatus.Text = "运行中…（可点“停止”中止）";
            }
            catch (Exception ex)
            {
                MessageBox.Show("启动失败：" + ex.Message, "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                btnStart.Enabled = true;
                btnStop.Enabled = false;
                btnSave.Enabled = true;
            }
        }

        private void AppendLog(string line)
        {
            if (line == null) return;
            if (IsDisposed || !IsHandleCreated) return;
            try
            {
                BeginInvoke((Action)delegate
                {
                    txtLog.AppendText(line + Environment.NewLine);
                });
            }
            catch (Exception) { }
        }

        private void OnProcessExited(object sender, EventArgs e)
        {
            if (IsDisposed || !IsHandleCreated) return;
            try
            {
                BeginInvoke((Action)delegate
                {
                    int code = -1;
                    try { code = running.ExitCode; } catch (Exception) { }
                    btnStart.Enabled = true;
                    btnStop.Enabled = false;
                    btnSave.Enabled = true;
                    lblStatus.Text = code == 0
                        ? "完成：全部通过（exit 0）"
                        : "完成：存在失败项（exit " + code + "），请看日志与报告";
                    txtLog.AppendText(Environment.NewLine + "===== 进程结束，exit code = " + code + " =====" + Environment.NewLine);
                });
            }
            catch (Exception) { }
        }

        private void OnStop(object sender, EventArgs e)
        {
            if (running == null || running.HasExited) return;
            try { running.Kill(); } catch (Exception) { }
            lblStatus.Text = "已停止";
        }

        private void OnOpenReports(object sender, EventArgs e)
        {
            string dir = Path.Combine(baseDir, "reports");
            if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);
            Process.Start("explorer.exe", "\"" + dir + "\"");
        }

        private void OnSave(object sender, EventArgs e)
        {
            try
            {
                SaveUiToConfig();
                lblStatus.Text = "配置已保存：" + ConfigPath;
            }
            catch (Exception ex)
            {
                MessageBox.Show("保存失败：" + ex.Message, "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (running != null && !running.HasExited)
            {
                try { running.Kill(); } catch (Exception) { }
            }
            base.OnFormClosing(e);
        }

        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new Launcher());
        }
    }
}
