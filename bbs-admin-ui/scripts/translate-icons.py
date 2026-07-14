#!/usr/bin/env python3
"""
Generate Chinese display names for Material Symbols icons.
Output: src/assets/icons/material-icons-zh.json
"""
import json, os

ROOT = os.path.dirname(os.path.abspath(__file__))
JSON_PATH = os.path.join(ROOT, '..', 'src', 'assets', 'icons', 'material-icons.json')
OUTPUT_PATH = os.path.join(ROOT, '..', 'src', 'assets', 'icons', 'material-icons-zh.json')

# ── English → Chinese dictionary for Material Symbols word components ──
DICT = {
    # Core actions
    'add': '添加', 'remove': '移除', 'delete': '删除', 'clear': '清除',
    'done': '完成', 'check': '勾选', 'cancel': '取消', 'close': '关闭',
    'edit': '编辑', 'save': '保存', 'update': '更新', 'upgrade': '升级',
    'download': '下载', 'upload': '上传', 'export': '导出', 'import': '导入',
    'copy': '复制', 'cut': '剪切', 'paste': '粘贴', 'print': '打印',
    'share': '分享', 'send': '发送', 'receive': '接收',
    'search': '搜索', 'find': '查找', 'filter': '筛选', 'sort': '排序',
    'select': '选择', 'deselect': '取消选择', 'toggle': '切换',
    'enable': '启用', 'disable': '禁用', 'lock': '锁定', 'unlock': '解锁',
    'hide': '隐藏', 'show': '显示', 'reveal': '显示',
    'open': '打开', 'launch': '启动', 'run': '运行', 'execute': '执行',
    'start': '开始', 'stop': '停止', 'pause': '暂停', 'resume': '继续',
    'restart': '重启', 'reset': '重置', 'refresh': '刷新', 'sync': '同步',
    'undo': '撤销', 'redo': '重做',
    'submit': '提交', 'confirm': '确认', 'approve': '批准', 'reject': '拒绝',
    'allow': '允许', 'deny': '拒绝', 'block': '阻止',
    'follow': '关注', 'unfollow': '取消关注', 'subscribe': '订阅',
    'thumb': '赞', 'thumbs': '赞',
    'forever': '永久', 'never': '从不', 'always': '始终',
    'above': '以上', 'below': '以下', 'under': '之下', 'over': '之上',
    'across': '跨越', 'through': '穿过', 'between': '之间',
    'around': '周围', 'about': '关于', 'along': '沿着',
    'side': '侧', 'sides': '侧', 'corner': '角落', 'edge': '边缘',
    'middle': '中间', 'center': '居中', 'central': '中央',
    'inner': '内部', 'outer': '外部', 'inside': '内部', 'outside': '外部',
    'front': '前', 'rear': '后', 'backward': '向后',
    'alternative': '替代', 'alternate': '交替',
    'access': '访问', 'accessible': '无障碍',
    'account': '账户', 'accounts': '账户',
    'activity': '活动', 'active': '活动',
    'actual': '实际', 'adjust': '调整',
    'administration': '管理', 'admin': '管理',
    'admission': '入场', 'admit': '允许',
    'advanced': '高级', 'advance': '推进',
    'album': '专辑', 'alert': '提醒',
    'ambient': '环境', 'amount': '数量',
    'android': '安卓', 'animation': '动画', 'animate': '动画',
    'announce': '公告', 'announcement': '公告', 'answer': '回答',
    'apartment': '公寓', 'api': '接口', 'app': '应用',
    'apparel': '服饰', 'appearance': '外观',
    'appliance': '家电', 'application': '应用',
    'appointment': '预约', 'approach': '方法',
    'area': '区域', 'arrange': '排列',
    'artist': '艺术家', 'art': '艺术',
    'assessment': '评估', 'assign': '分配',
    'assistant': '助手', 'assist': '协助',
    'association': '协会', 'assurance': '保证',
    'astrophotography': '天文摄影',
    'athlete': '运动员', 'athletic': '运动',
    'atmosphere': '氛围', 'atom': '原子', 'attach': '附件',
    'attack': '攻击', 'attempt': '尝试', 'attend': '参加',
    'attention': '注意', 'attract': '吸引',
    'auction': '拍卖', 'audience': '观众', 'audio': '音频',
    'audit': '审计', 'author': '作者',
    'authorization': '授权', 'authorize': '授权',
    'auto': '自动', 'automatic': '自动',
    'availability': '可用性', 'available': '可用',
    'average': '平均', 'aviation': '航空',
    'award': '奖项', 'awareness': '认知',
    'baby': '婴儿', 'bachelor': '学士',
    'background': '背景', 'backup': '备份',
    'bathroom': '浴室', 'bath': '沐浴',
    'beach': '海滩', 'beauty': '美容',
    'bed': '床', 'bedroom': '卧室',
    'behavior': '行为', 'bell': '铃铛',
    'belong': '属于', 'bench': '长凳',
    'beverage': '饮料', 'bicycle': '自行车',
    'bidding': '竞价', 'bill': '账单', 'billing': '计费',
    'biography': '传记', 'biology': '生物',
    'birthday': '生日', 'bitcoin': '比特币',
    'blade': '刀片', 'blank': '空白', 'blanket': '毯子',
    'blast': '爆破', 'bleed': '出血',
    'blend': '混合', 'bless': '祝福',
    'blind': '盲人', 'blinds': '百叶窗',
    'blink': '闪烁', 'blockchain': '区块链',
    'blood': '血液', 'bloodtype': '血型',
    'bluetooth': '蓝牙', 'blur': '模糊',
    'board': '面板', 'boarding': '登机',
    'body': '身体', 'boil': '煮沸',
    'bomb': '炸弹', 'bond': '债券',
    'bone': '骨骼', 'bonus': '奖金',
    'book': '书籍', 'booking': '预订',
    'bookshelf': '书架', 'bounce': '弹跳',
    'bowl': '碗', 'bowling': '保龄球',
    'brain': '大脑', 'branch': '分支',
    'brand': '品牌', 'brass': '黄铜',
    'break': '打断', 'breakfast': '早餐',
    'breast': '胸部', 'breath': '呼吸',
    'brew': '酿造', 'brewery': '酿酒厂',
    'bridge': '桥梁', 'brief': '简报',
    'briefcase': '公文包', 'bring': '携带',
    'broadcast': '广播', 'broken': '损坏',
    'bronze': '青铜', 'brother': '兄弟',
    'browser': '浏览器', 'brush': '画笔',
    'bubble': '气泡', 'bucket': '水桶',
    'budget': '预算', 'buffer': '缓冲',
    'builder': '构建器', 'building': '建筑',
    'bulb': '灯泡', 'bulk': '批量',
    'bullet': '项目符号', 'bullhorn': '扩音器',
    'bundle': '捆绑', 'burial': '埋葬',
    'burn': '燃烧', 'burst': '爆发',
    'bus': '公交', 'business': '商业',
    'butler': '管家', 'button': '按钮',
    'bypass': '绕过', 'byte': '字节',

    # Navigation
    'back': '返回', 'forward': '前进', 'next': '下一个', 'previous': '上一个',
    'first': '第一个', 'last': '最后一个', 'prev': '上一个',
    'up': '向上', 'down': '向下', 'left': '向左', 'right': '向右',
    'north': '北', 'south': '南', 'east': '东', 'west': '西',
    'navigate': '导航', 'navigation': '导航',
    'arrow': '箭头', 'chevron': '箭头',
    'expand': '展开', 'collapse': '折叠',
    'more': '更多', 'less': '更少', 'menu': '菜单',
    'home': '首页', 'page': '页面', 'pageview': '页面视图',

    # UI components
    'tab': '标签', 'tabs': '标签', 'panel': '面板', 'sidebar': '侧边栏',
    'toolbar': '工具栏', 'tooltip': '提示', 'tooltip': '提示',
    'dialog': '对话框', 'modal': '模态框', 'popup': '弹窗', 'popover': '弹出框',
    'badge': '徽章', 'chip': '标签', 'avatar': '头像',
    'divider': '分隔线', 'spacer': '间距', 'separator': '分隔符',
    'container': '容器', 'wrapper': '包裹',
    'viewport': '视口', 'section': '区块',
    'header': '头部', 'footer': '底部', 'content': '内容',
    'title': '标题', 'subtitle': '副标题', 'caption': '说明',
    'label': '标签', 'placeholder': '占位',

    # Data & file
    'file': '文件', 'folder': '文件夹', 'directory': '目录',
    'document': '文档', 'report': '报告', 'article': '文章',
    'data': '数据', 'database': '数据库', 'table': '表格',
    'chart': '图表', 'graph': '图表', 'stat': '统计',
    'analytics': '分析', 'insight': '洞察',
    'backup': '备份', 'restore': '恢复', 'recover': '恢复',
    'archive': '归档', 'inbox': '收件箱', 'outbox': '发件箱',
    'draft': '草稿', 'note': '笔记', 'description': '描述',
    'summary': '摘要', 'detail': '详情', 'details': '详情',
    'list': '列表', 'grid': '网格', 'table': '表格', 'card': '卡片',
    'attachment': '附件', 'code': '代码', 'script': '脚本',
    'config': '配置', 'setting': '设置', 'settings': '设置',
    'preference': '偏好', 'preferences': '偏好',

    # Communication
    'chat': '聊天', 'message': '消息', 'messages': '消息',
    'email': '邮件', 'mail': '邮件', 'inbox': '收件箱',
    'comment': '评论', 'forum': '论坛', 'feedback': '反馈',
    'notification': '通知', 'alert': '提醒', 'alarm': '闹钟',
    'call': '通话', 'phone': '电话', 'dial': '拨号',
    'contact': '联系人', 'contacts': '联系人',
    'reply': '回复', 'forward': '转发',
    'announce': '公告', 'announcement': '公告',
    'broadcast': '广播', 'notification': '通知',

    # Media
    'play': '播放', 'pause': '暂停', 'stop': '停止', 'record': '录制',
    'capture': '捕获', 'screenshot': '截图',
    'video': '视频', 'movie': '电影', 'film': '影片',
    'camera': '相机', 'photo': '照片', 'image': '图片', 'picture': '图片',
    'gallery': '相册', 'album': '专辑',
    'music': '音乐', 'audio': '音频', 'sound': '声音',
    'volume': '音量', 'speaker': '扬声器', 'mic': '麦克风',
    'microphone': '麦克风', 'headphone': '耳机', 'headset': '耳麦',
    'radio': '收音机', 'tv': '电视', 'screen': '屏幕',
    'display': '显示', 'monitor': '显示器', 'projector': '投影仪',
    'brightness': '亮度', 'contrast': '对比度',

    # User & social
    'user': '用户', 'users': '用户', 'person': '人物', 'people': '人群',
    'account': '账户', 'profile': '个人资料', 'login': '登录', 'logout': '登出',
    'register': '注册', 'sign': '签名', 'password': '密码',
    'group': '群组', 'team': '团队', 'member': '成员',
    'friend': '好友', 'follow': '关注', 'follower': '粉丝',
    'face': '表情', 'smile': '微笑', 'mood': '心情', 'emoji': '表情',
    'sentiment': '情绪', 'rating': '评分', 'review': '评价',
    'star': '星标', 'favorite': '收藏', 'bookmark': '书签',
    'like': '喜欢', 'unlike': '不喜欢', 'share': '分享',
    'vote': '投票', 'poll': '投票',

    # Device & hardware
    'device': '设备', 'devices': '设备',
    'phone': '手机', 'mobile': '移动', 'tablet': '平板',
    'laptop': '笔记本', 'computer': '电脑', 'desktop': '台式机',
    'watch': '手表', 'wearable': '可穿戴',
    'keyboard': '键盘', 'mouse': '鼠标', 'touchpad': '触摸板',
    'printer': '打印机', 'scanner': '扫描仪',
    'battery': '电池', 'power': '电源', 'charging': '充电',
    'cable': '线缆', 'wire': '线缆', 'wireless': '无线',
    'bluetooth': '蓝牙', 'wifi': '无线网络', 'network': '网络',
    'signal': '信号', 'sensor': '传感器', 'chip': '芯片',
    'memory': '内存', 'storage': '存储', 'hardware': '硬件',
    'router': '路由器', 'modem': '调制解调器',

    # Time
    'time': '时间', 'date': '日期', 'calendar': '日历',
    'clock': '时钟', 'timer': '计时器', 'alarm': '闹钟',
    'hour': '小时', 'minute': '分钟', 'second': '秒',
    'day': '日', 'week': '周', 'month': '月', 'year': '年',
    'today': '今天', 'schedule': '日程', 'event': '事件',
    'history': '历史', 'recent': '最近', 'pending': '待处理',
    'reminder': '提醒', 'appointment': '预约',

    # Location
    'location': '位置', 'place': '地点', 'map': '地图',
    'direction': '方向', 'directions': '方向',
    'distance': '距离', 'near': '附近', 'nearby': '附近',
    'travel': '旅行', 'trip': '行程', 'tour': '游览', 'explore': '探索',
    'flight': '航班', 'airport': '机场', 'airline': '航空公司',
    'train': '火车', 'bus': '公交', 'taxi': '出租车',
    'bike': '自行车', 'car': '汽车', 'parking': '停车',
    'gas': '加油站', 'fuel': '燃料',
    'hotel': '酒店', 'store': '商店', 'restaurant': '餐厅',
    'hospital': '医院', 'school': '学校',
    'flag': '旗帜', 'marker': '标记', 'pin': '图钉',

    # Business
    'business': '商业', 'work': '工作', 'office': '办公',
    'shop': '商店', 'shopping': '购物', 'store': '店铺',
    'cart': '购物车', 'bag': '袋子',
    'card': '卡片', 'credit': '信用卡', 'payment': '支付',
    'wallet': '钱包', 'price': '价格', 'cost': '成本',
    'dollar': '美元', 'euro': '欧元', 'currency': '货币',
    'receipt': '收据', 'invoice': '发票',
    'analytics': '分析', 'statistics': '统计',
    'trend': '趋势', 'trending': '趋势',

    # Weather & environment
    'weather': '天气', 'sun': '太阳', 'sunny': '晴天',
    'moon': '月亮', 'cloud': '云', 'cloudy': '多云',
    'rain': '雨', 'rainy': '雨天', 'snow': '雪', 'snowy': '雪天',
    'wind': '风', 'windy': '大风', 'storm': '暴风雨',
    'temperature': '温度', 'thermostat': '恒温器',
    'water': '水', 'drop': '水滴',
    'fire': '火', 'flame': '火焰',
    'eco': '生态', 'leaf': '叶子', 'nature': '自然',
    'environment': '环境', 'energy': '能源',
    'recycle': '回收', 'recycling': '回收',

    # Security
    'security': '安全', 'safety': '安全', 'safe': '安全',
    'privacy': '隐私', 'protect': '保护', 'protection': '保护',
    'shield': '盾牌', 'key': '密钥', 'password': '密码',
    'verified': '已验证', 'verify': '验证',
    'encryption': '加密', 'encrypt': '加密',
    'warning': '警告', 'danger': '危险', 'error': '错误',
    'emergency': '紧急',

    # Health
    'health': '健康', 'medical': '医疗', 'medicine': '药品',
    'hospital': '医院', 'doctor': '医生', 'nurse': '护士',
    'heart': '心脏', 'pulse': '脉搏',
    'fitness': '健身', 'exercise': '锻炼', 'running': '跑步',
    'walk': '步行', 'walking': '步行',
    'food': '食物', 'drink': '饮料', 'coffee': '咖啡', 'tea': '茶',
    'restaurant': '餐厅', 'meal': '餐食',

    # Construction & tools
    'tool': '工具', 'tools': '工具',
    'build': '构建', 'construction': '施工',
    'engineering': '工程', 'repair': '维修', 'maintenance': '维护',
    'ruler': '尺子', 'measure': '测量',
    'wrench': '扳手', 'screwdriver': '螺丝刀',
    'hardware': '五金', 'plumbing': '管道',

    # Development
    'code': '代码', 'coding': '编程',
    'develop': '开发', 'developer': '开发者', 'development': '开发',
    'deploy': '部署', 'deployed': '已部署', 'deployment': '部署',
    'debug': '调试', 'bug': '缺陷',
    'test': '测试', 'testing': '测试', 'unit': '单元',
    'build': '构建', 'compile': '编译',
    'commit': '提交', 'push': '推送', 'pull': '拉取',
    'branch': '分支', 'merge': '合并', 'release': '发布',
    'version': '版本', 'update': '更新',
    'api': '接口', 'endpoint': '端点',
    'server': '服务器', 'client': '客户端',
    'frontend': '前端', 'backend': '后端', 'fullstack': '全栈',
    'responsive': '响应式', 'adaptive': '自适应',

    # Common adjectives & modifiers
    'new': '新建', 'old': '旧', 'recent': '最近',
    'active': '活动', 'inactive': '非活动',
    'full': '完整', 'empty': '空', 'half': '一半',
    'high': '高', 'low': '低', 'medium': '中',
    'large': '大', 'small': '小', 'mini': '迷你',
    'long': '长', 'short': '短', 'tall': '高',
    'wide': '宽', 'narrow': '窄', 'thin': '细',
    'fast': '快速', 'slow': '慢速', 'speed': '速度',
    'light': '亮', 'dark': '暗', 'dim': '暗',
    'outline': '轮廓', 'filled': '填充', 'sharp': '尖锐',
    'round': '圆形', 'square': '方形', 'circle': '圆',
    'solid': '实心', 'hollow': '空心',
    'auto': '自动', 'manual': '手动',
    'offline': '离线', 'online': '在线',
    'public': '公开', 'private': '私有',

    # Layout & design
    'layout': '布局', 'design': '设计',
    'style': '样式', 'theme': '主题',
    'color': '颜色', 'palette': '调色板',
    'font': '字体', 'typography': '排版',
    'align': '对齐', 'alignment': '对齐',
    'center': '居中', 'middle': '中间',
    'top': '顶部', 'bottom': '底部',
    'left': '左', 'right': '右',
    'horizontal': '水平', 'vertical': '垂直',
    'margin': '外边距', 'padding': '内边距',
    'border': '边框', 'outline': '轮廓',
    'shadow': '阴影', 'elevation': '层级',
    'radius': '圆角', 'rounded': '圆角',
    'size': '大小', 'width': '宽度', 'height': '高度',

    # Text
    'text': '文本', 'letter': '字母', 'word': '单词',
    'sentence': '句子', 'paragraph': '段落',
    'bold': '粗体', 'italic': '斜体', 'underline': '下划线',
    'strikethrough': '删除线', 'highlight': '高亮',
    'format': '格式', 'formatting': '格式化',
    'indent': '缩进', 'outdent': '减少缩进',
    'list': '列表', 'bullet': '项目符号', 'number': '编号',
    'ordered': '有序', 'unordered': '无序',
    'spell': '拼写', 'spellcheck': '拼写检查',
    'translate': '翻译', 'language': '语言',

    # Numbers (for numeric icon names)
    'zero': '零', 'one': '一', 'two': '二', 'three': '三',
    'four': '四', 'five': '五', 'six': '六', 'seven': '七',
    'eight': '八', 'nine': '九', 'ten': '十',

    # Common suffixes
    'box': '框', 'field': '字段', 'input': '输入',
    'output': '输出', 'form': '表单',
    'area': '区域', 'zone': '区域', 'space': '空间',
    'point': '点', 'line': '线', 'bar': '条',
    'item': '项', 'items': '项',
    'type': '类型', 'mode': '模式',
    'view': '视图', 'display': '显示',
    'control': '控制', 'controls': '控制',
    'manager': '管理器', 'management': '管理',
    'editor': '编辑器', 'viewer': '查看器',
    'maker': '制作器', 'creator': '创建器',
    'service': '服务', 'services': '服务',
    'engine': '引擎', 'platform': '平台',
    'support': '支持', 'helper': '助手', 'assistant': '助手',
    'token': '令牌', 'badge': '徽章',
    'mark': '标记', 'marker': '标记',
    'track': '跟踪', 'tracker': '跟踪器',
    'loader': '加载器', 'loader': '加载器',

    # Miscellaneous common
    'system': '系统', 'sync': '同步',
    'process': '处理', 'processing': '处理中',
    'operation': '操作', 'action': '操作',
    'feature': '功能', 'function': '功能',
    'option': '选项', 'options': '选项',
    'default': '默认', 'custom': '自定义',
    'standard': '标准', 'advanced': '高级',
    'basic': '基础', 'simple': '简单',
    'smart': '智能', 'intelligent': '智能',
    'virtual': '虚拟', 'reality': '现实',
    'augmented': '增强', 'mixed': '混合',
    'global': '全球', 'world': '世界',
    'tutorial': '教程', 'guide': '指南',
    'faq': '常见问题', 'help': '帮助',
    'info': '信息', 'information': '信息',
    'tip': '提示', 'tips': '提示',
    'trick': '技巧', 'tricks': '技巧',
    'learn': '学习', 'education': '教育',
    'science': '科学', 'technology': '科技',
    'account': '账户', 'access': '访问',
    'activity': '活动', 'log': '日志', 'logging': '记录',
}


def translate_icon_name(name):
    """Translate a single Material Symbols icon name to Chinese."""

    # ── Special overrides for common icons (more natural translations) ──
    OVERRIDES = {
        'thumb_up': '点赞',
        'thumb_down': '点踩',
        'arrow_back': '返回',
        'arrow_forward': '前进',
        'arrow_upward': '向上',
        'arrow_downward': '向下',
        'arrow_back_ios': '返回',
        'arrow_forward_ios': '前进',
        'delete_forever': '永久删除',
        'add_circle': '添加圆圈',
        'add_box': '添加框',
        'remove_circle': '移除圆圈',
        'remove_circle_outline': '移除圆圈轮廓',
        'play_arrow': '播放',
        'play_circle': '播放圆圈',
        'stop_circle': '停止圆圈',
        'check_circle': '勾选圆圈',
        'cancel_presentation': '取消演示',
        'fullscreen': '全屏',
        'fullscreen_exit': '退出全屏',
        'menu_open': '展开菜单',
        'menu_book': '菜单书籍',
        'settings_applications': '设置应用',
        'settings_bluetooth': '蓝牙设置',
        'settings_brightness': '亮度设置',
        'settings_cell': '蜂窝设置',
        'settings_ethernet': '以太网设置',
        'settings_input': '输入设置',
        'settings_phone': '电话设置',
        'settings_power': '电源设置',
        'settings_remote': '遥控设置',
        'settings_voice': '语音设置',
        'account_balance': '账户余额',
        'account_balance_wallet': '钱包余额',
        'account_box': '账户框',
        'account_circle': '账户圆圈',
        'account_tree': '账户树',
        'contact_mail': '联系人邮件',
        'contact_phone': '联系人电话',
        'contact_support': '联系支持',
        'date_range': '日期范围',
        'event_available': '事件可用',
        'event_busy': '事件忙碌',
        'event_note': '事件笔记',
        'event_seat': '事件座位',
        'feedback': '反馈',
        'live_help': '在线帮助',
        'rate_review': '评价',
        'sentiment_dissatisfied': '不满意',
        'sentiment_neutral': '一般',
        'sentiment_satisfied': '满意',
        'sentiment_very_dissatisfied': '非常不满意',
        'sentiment_very_satisfied': '非常满意',
        'access_time': '访问时间',
        'add_alarm': '添加闹钟',
        'add_alert': '添加提醒',
        'airplanemode_active': '飞行模式开启',
        'airplanemode_inactive': '飞行模式关闭',
        'battery_alert': '电池提醒',
        'battery_charging_full': '充电中',
        'battery_full': '电池满',
        'battery_std': '标准电池',
        'battery_unknown': '电池未知',
        'computer': '电脑',
        'desktop_windows': '桌面窗口',
        'developer_board': '开发板',
        'developer_mode': '开发者模式',
        'device_hub': '设备集线器',
        'device_thermostat': '设备恒温器',
        'devices_other': '其他设备',
        'dock': '扩展坞',
        'gamepad': '游戏手柄',
        'headset_mic': '耳麦麦克风',
        'keyboard_arrow_down': '键盘向下',
        'keyboard_arrow_left': '键盘向左',
        'keyboard_arrow_right': '键盘向右',
        'keyboard_arrow_up': '键盘向上',
        'keyboard_backspace': '键盘退格',
        'keyboard_capslock': '键盘大写锁定',
        'keyboard_hide': '键盘隐藏',
        'keyboard_return': '键盘回车',
        'keyboard_tab': '键盘跳格',
        'keyboard_voice': '键盘语音',
        'laptop_chromebook': 'Chromebook',
        'laptop_mac': 'Mac笔记本',
        'laptop_windows': 'Windows笔记本',
        'memory': '内存',
        'mouse': '鼠标',
        'phone_android': '安卓手机',
        'phone_iphone': 'iPhone',
        'phonelink': '手机连接',
        'phonelink_erase': '手机擦除',
        'phonelink_lock': '手机锁定',
        'phonelink_off': '手机断开',
        'phonelink_ring': '手机铃声',
        'phonelink_setup': '手机设置',
        'power_input': '电源输入',
        'power_off': '电源关闭',
        'power_settings_new': '新电源设置',
        'router': '路由器',
        'scanner': '扫描仪',
        'screen_lock_landscape': '横屏锁定',
        'screen_lock_portrait': '竖屏锁定',
        'screen_lock_rotation': '旋转锁定',
        'screen_rotation': '屏幕旋转',
        'sd_card': 'SD卡',
        'sd_storage': 'SD存储',
        'security': '安全',
        'sim_card': 'SIM卡',
        'sim_card_alert': 'SIM卡提醒',
        'speaker_group': '扬声器组',
        'speaker_notes': '扬声器备注',
        'speaker_notes_off': '扬声器备注关闭',
        'speaker_phone': '扬声器电话',
        'tablet_mac': 'Mac平板',
        'tablet_android': '安卓平板',
        'toys': '玩具',
        'tv': '电视',
        'videogame_asset': '游戏资源',
        'watch': '手表',
        'widgets': '小部件',
        'wifi_lock': 'WiFi锁定',
        'wifi_tethering': 'WiFi热点',
        'input': '输入',
        'live_tv': '直播电视',
        'location_disabled': '位置已禁用',
        'location_off': '位置关闭',
        'location_on': '位置开启',
        'location_searching': '位置搜索中',
        'store_mall_directory': '商城',
        'storefront': '店铺门面',
        'shopping_bag': '购物袋',
        'shopping_basket': '购物篮',
        'shopping_cart': '购物车',
        'restaurant_menu': '餐厅菜单',
        'room_service': '客房服务',
        'free_breakfast': '免费早餐',
        'local_bar': '本地酒吧',
        'local_cafe': '本地咖啡',
        'local_dining': '本地餐饮',
        'local_grocery_store': '本地杂货店',
        'local_hospital': '本地医院',
        'local_laundry_service': '本地洗衣',
        'local_library': '本地图书馆',
        'local_mall': '本地商场',
        'local_offer': '本地优惠',
        'local_parking': '本地停车',
        'local_pharmacy': '本地药店',
        'local_pizza': '本地披萨',
        'local_police': '本地警察',
        'local_post_office': '本地邮局',
        'local_printshop': '本地打印店',
        'local_see': '本地查看',
        'local_shipping': '本地配送',
        'local_taxi': '本地出租车',
    }

    if name in OVERRIDES:
        return OVERRIDES[name]

    parts = name.split('_')
    translated = []

    for part in parts:
        # Skip empty parts
        if not part:
            continue

        # Try direct dictionary lookup
        if part in DICT:
            translated.append(DICT[part])
            continue

        # Handle hyphenated parts (e.g. "country" not found)
        # Check if it's digits (e.g. "2", "3d", "4k")
        if part.isdigit():
            translated.append(part)
            continue

        if len(part) == 1 and part.isdigit():
            translated.append(part)
            continue

        # Mixed digit+letter: "3d", "4k", "mp"
        if part[:-1].isdigit() and len(part) > 1:
            num = part[:-1]
            letter = part[-1]
            translated.append(f'{num}{letter.upper()}')
            continue

        # Unknown word - try to convert known patterns
        # Check if it's a plural form
        if part.endswith('ies'):
            base = part[:-3] + 'y'
            if base in DICT:
                translated.append(DICT[base])
                continue

        if part.endswith('es'):
            base = part[:-2]
            if base in DICT:
                translated.append(DICT[base])
                continue

        if part.endswith('s'):
            base = part[:-1]
            if base in DICT:
                translated.append(DICT[base])
                continue
            # Also try -ing form (e.g. settings -> setting)
            if part.endswith('ings'):
                base2 = part[:-1]
                if base2 in DICT:
                    translated.append(DICT[base2])
                    continue

        if part.endswith('ing'):
            base = part[:-3]
            if base in DICT:
                translated.append(DICT[base])
                continue
            # Also try base + 'e' (e.g. 'taking' -> 'take')
            if base + 'e' in DICT:
                translated.append(DICT[base + 'e'])
                continue

        if part.endswith('ed'):
            base = part[:-2]
            if base in DICT:
                translated.append(DICT[base])
                continue
            if base + 'e' in DICT:
                translated.append(DICT[base + 'e'])
                continue

        if part.endswith('tion'):
            base = part[:-4]
            if base in DICT:
                translated.append(DICT[base])
                continue

        # Fallback: keep the English word
        translated.append(part)

    # Special handling for short names
    if len(parts) == 1 and len(translated) == 1:
        if translated[0] == parts[0]:
            # Single word not translated - use as-is
            pass

    # Join translated parts
    result = ''.join(translated)

    # Post-processing: remove consecutive duplicates
    # e.g. "设置设置" -> "设置"
    cleaned = result
    for word in ['设置', '控制', '管理', '服务']:
        while word * 2 in cleaned:
            cleaned = cleaned.replace(word * 2, word)

    return cleaned if cleaned else name


def main():
    with open(JSON_PATH) as f:
        names = json.load(f)

    result = {}
    untranslated_words = set()

    for name in names:
        zh = translate_icon_name(name)
        result[name] = zh

        # Track untranslated words for dictionary improvement
        for part in name.split('_'):
            if part not in DICT and not part.isdigit() and len(part) > 2:
                untranslated_words.add(part)

    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    # Stats
    translated_count = sum(1 for v in result.values() if v != name)
    print(f'Total icons: {len(names)}')
    print(f'Translated: {translated_count} ({translated_count/len(names)*100:.1f}%)')
    print(f'Untranslated words ({len(untranslated_words)}): {", ".join(sorted(untranslated_words)[:50])}')

    # Verify key icons
    checks = ['add', 'home', 'search', 'thumb_up', 'label', 'bookmark',
              'arrow_back', 'delete_forever', 'settings', 'deployed_code_update']
    for name in checks:
        zh = result.get(name, 'N/A')
        status = '✓' if zh != name else '?'
        print(f'  {status} {name} -> {zh}')

    print(f'\nSaved: {OUTPUT_PATH}')


if __name__ == '__main__':
    main()
