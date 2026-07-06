#!/usr/bin/env python3
"""
Categorize Material Symbols icons into usage-based groups.
Run after extract-material-icons.py.
"""
import json, os, re

ROOT = os.path.dirname(os.path.abspath(__file__))
JSON_PATH = os.path.join(ROOT, '..', 'src', 'assets', 'icons', 'material-icons.json')
OUTPUT_PATH = os.path.join(ROOT, '..', 'src', 'assets', 'icons', 'material-icons-categorized.json')

with open(JSON_PATH) as f:
    all_names = json.load(f)

CATEGORIES = [
    {
        "key": "general",
        "label": "通用",
        "icon": "star",
        "prefixes": [
            "add", "remove", "delete", "close", "done", "check", "clear",
            "edit", "save", "search", "settings", "refresh", "sync", "undo", "redo",
            "share", "open", "launch", "expand", "collapse", "filter", "sort",
            "more", "menu", "home", "help", "info", "question", "cancel",
            "update", "upgrade", "download", "upload", "copy", "cut", "paste", "print",
            "favorite", "bookmark", "star", "pin", "push", "pull", "swap",
            "lock", "unlock", "restart", "pause", "stop", "play",
            "select", "deselect", "toggle", "enable", "disable",
            "hide", "show", "visibility", "fullscreen", "exit",
            "drag", "handle", "reorder", "resize", "move",
            "app", "apps", "widget", "tool", "tooltip",
            "loading", "spinner", "progress", "skeleton",
            "bug", "code", "terminal", "console", "command",
            "view", "splitscreen", "dashboard", "widget",
            "mode", "reset", "do", "no", "offline", "online",
            "running", "pending", "process",
            "total", "average", "median", "maximize", "minimize",
            "overview", "summary", "overlay",
            "thumb", "counter", "perm", "auto",
            "deployed", "developer", "attribution",
        ]
    },
    {
        "key": "navigation",
        "label": "导航",
        "icon": "directions",
        "prefixes": [
            "arrow", "chevron", "navigate", "direction",
            "east", "west", "north", "south",
            "first_page", "last_page", "keyboard_arrow",
            "subdirectory", "swap_horiz", "swap_vert",
            "corner", "turn", "roundabout", "ramp",
            "u_turn", "merge", "lane",
            "swipe", "tab", "bottom", "vertical", "horizontal",
            "arrows", "move_up", "move_down",
            "switch",
        ]
    },
    {
        "key": "text",
        "label": "文本与排版",
        "icon": "format_align_left",
        "prefixes": [
            "format", "text", "font", "align", "border", "line", "space",
            "indent", "outdent", "list", "numbered", "spellcheck",
            "strikethrough", "underline", "italic", "bold",
            "superscript", "subscript", "colorize", "highlight",
            "title", "wrap", "paragraph", "column", "margin", "padding",
            "texture", "typography", "glyph", "letter",
            "insert", "table", "content", "language", "translate",
            "mark", "draft", "edit_note", "label",
        ]
    },
    {
        "key": "file",
        "label": "文件与数据",
        "icon": "folder",
        "prefixes": [
            "folder", "file", "attachment", "document", "data", "database",
            "drive", "cloud", "storage", "backup", "inbox", "outbox", "archive",
            "report", "article", "description", "feed", "rss", "csv", "pdf",
            "note", "note_alt", "code", "json", "xml", "config",
            "upload_file", "download", "cloud_upload", "cloud_download",
            "newspaper", "book", "library", "knowledge",
            "assignment", "task", "checklist", "todo",
            "attach", "pin", "clip",
        ]
    },
    {
        "key": "communication",
        "label": "通信",
        "icon": "chat",
        "prefixes": [
            "chat", "message", "email", "mail", "forum", "comment", "feedback",
            "notification", "alert", "ring", "vibration", "contact", "contacts",
            "phone", "call", "dial", "ringtone", "sms", "mms",
            "quickreply", "reply", "forward", "send",
        ]
    },
    {
        "key": "media",
        "label": "媒体与图像",
        "icon": "photo",
        "prefixes": [
            "play", "pause", "skip", "forward", "replay", "repeat", "shuffle",
            "volume", "music", "video", "movie", "film", "camera", "photo", "image",
            "picture", "gallery", "album", "microphone", "headphone", "speaker",
            "sound", "audio", "radio", "tv", "screen", "display", "projector",
            "capture", "record", "videocam", "media", "mp", "megapixel", "pixel",
            "slow_motion", "fast_forward", "fast_rewind",
            "color", "palette", "brush", "draw", "eraser",
            "animation", "frame", "grid", "layer",
            "crop", "blur", "hdr", "looks", "wb", "tonality", "exposure",
            "mic", "screenshot", "screencast",
            "playlist", "queue_music",
            "game", "sports", "chess", "toys",
            "gif", "sticker",
            "ink", "stylus", "pen", "pencil",
        ]
    },
    {
        "key": "social",
        "label": "用户与社交",
        "icon": "person",
        "prefixes": [
            "person", "people", "group", "account", "user", "avatar", "face",
            "smile", "sentiment", "mood", "emoji", "social",
            "family", "child", "adult", "senior", "man", "woman", "boy", "girl",
            "friend", "team", "community", "diversity",
            "celebration", "party", "cake", "gift",
            "real_estate", "agent",
            "accessibility", "accessible",
            "privacy", "private",
            "personal", "individual",
        ]
    },
    {
        "key": "device",
        "label": "设备与硬件",
        "icon": "devices",
        "prefixes": [
            "phone", "tablet", "mobile", "laptop", "computer", "monitor", "watch",
            "keyboard", "mouse", "printer", "scanner", "device", "hardware",
            "sensor", "chip", "memory", "battery", "charging", "power", "cable",
            "usb", "hdmi", "bluetooth", "nfc", "sim", "sd",
            "earphone", "headset", "camera", "webcam",
            "router", "modem", "access_point",
            "smartphone", "smartwatch", "smart_toy",
            "console", "gamepad", "joystick",
            "wifi", "signal", "network", "lan", "ethernet",
            "desktop", "phonelink", "cast", "connected",
            "smart", "nest", "detector",
            "brightness", "light", "lamp", "flashlight",
            "speed", "motion", "sensor",
            "android", "assistant", "fiber", "devices",
        ]
    },
    {
        "key": "time",
        "label": "时间与日程",
        "icon": "calendar_month",
        "prefixes": [
            "calendar", "date", "schedule", "clock", "timer", "alarm", "hourglass",
            "timeline", "today", "month", "week", "day", "year",
            "event", "reminder", "appointment",
            "history", "schedule", "pending",
        ]
    },
    {
        "key": "location",
        "label": "地点与出行",
        "icon": "map",
        "prefixes": [
            "map", "location", "place", "pin", "marker", "store", "restaurant",
            "hotel", "hospital", "school", "airport", "station",
            "directions", "navigation", "near", "distance", "travel",
            "flight", "train", "bus", "bike", "shuttle", "taxi",
            "parking", "road", "traffic", "local", "place",
            "car", "transport", "trip", "tour", "explore",
            "compass", "flag", "terrain",
            "airline", "seat", "ev", "electric_car",
            "motorcycle", "sailing", "boat",
            "door", "garage", "gate", "fence",
        ]
    },
    {
        "key": "business",
        "label": "商业与购物",
        "icon": "shopping_cart",
        "prefixes": [
            "shopping", "card", "wallet", "payment", "pay", "currency",
            "dollar", "euro", "price", "checkout", "receipt", "invoice",
            "work", "business", "badge", "analytics", "chart", "graph",
            "stat", "finance", "account_balance",
            "atm", "credit", "money", "sell", "tag", "discount", "coupon",
            "store", "mall", "market", "cart", "bag",
            "contract", "deal", "handshake", "partner",
            "trending", "monitoring",
            "bank", "investment", "fund",
        ]
    },
    {
        "key": "weather",
        "label": "天气与环境",
        "icon": "partly_cloudy_day",
        "prefixes": [
            "weather", "sun", "moon", "cloud", "rain", "snow", "wind",
            "temperature", "thunder", "storm", "fog", "humidity",
            "water", "fire", "eco", "energy",
            "gas", "electric", "solar", "bolt", "flash",
            "heat", "cold", "freeze", "warm",
            "nature", "forest", "leaf", "flower", "grass",
            "earth", "globe", "public",
            "recycling", "waste", "compost",
        ]
    },
    {
        "key": "security",
        "label": "安全与隐私",
        "icon": "shield",
        "prefixes": [
            "lock", "shield", "security", "privacy", "password",
            "key", "verified", "policy", "safety", "protect",
            "gpp", "encryption", "authenticate",
            "warning", "danger", "error",
            "emergency", "alarm",
        ]
    },
    {
        "key": "health",
        "label": "健康与医疗",
        "icon": "local_hospital",
        "prefixes": [
            "hospital", "medical", "health", "healthcare",
            "doctor", "nurse", "patient", "clinic",
            "pharmacy", "medicine", "pill", "vaccine",
            "heart", "pulse", "monitor_heart",
            "fitness", "exercise", "running", "walking",
            "nutrition", "diet", "food",
            "bath", "shower", "spa",
            "injury", "bandage", "ambulance",
            "fit", "fitness", "body", "gym",
        ]
    },
    {
        "key": "construction",
        "label": "工具与施工",
        "icon": "construction",
        "prefixes": [
            "construction", "build", "engineering", "hardware",
            "tool", "tools", "plumbing", "electrician",
            "handyman", "renovation",
            "ruler", "measuring", "level",
            "ladder", "scaffold",
        ]
    },
]

def categorize(name):
    """Return category key for an icon name."""
    for cat in CATEGORIES:
        for prefix in cat["prefixes"]:
            if name == prefix or name.startswith(prefix + "_"):
                return cat["key"]
    # Single-word names -> 通用
    if "_" not in name:
        return "general"
    return None  # uncategorized


# Build categorized structure
categorized = {}
uncategorized = []
for cat in CATEGORIES:
    categorized[cat["key"]] = {"label": cat["label"], "icon": cat["icon"], "names": []}

for name in all_names:
    key = categorize(name)
    if key and key in categorized:
        categorized[key]["names"].append(name)
    else:
        uncategorized.append(name)

# Add uncategorized to "other" category
other = {
    "label": "其他",
    "icon": "more_horiz",
    "names": sorted(uncategorized)
}
all_categories = [categorized[cat["key"]] for cat in CATEGORIES] + [other]

# Build output
output = {
    "categories": [
        {"label": cat["label"], "icon": cat["icon"], "names": sorted(cat["names"])}
        for cat in all_categories
    ],
    "total": len(all_names)
}

with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"Total icons: {len(all_names)}")
for cat in output["categories"]:
    pct = len(cat["names"]) / max(len(all_names), 1) * 100
    print(f"  {cat['label']}: {len(cat['names'])} ({pct:.1f}%)")
print(f"\nSaved to: {OUTPUT_PATH}")

# Verify key icons
expected = ["thumb_up", "help", "folder_open", "home", "label", "bookmark", "search", "add", "edit", "delete"]
for name in expected:
    key = categorize(name)
    cat_label = "未分类"
    for cat in output["categories"]:
        if name in cat["names"]:
            cat_label = cat["label"]
            break
    print(f"  {name}: {cat_label}")
