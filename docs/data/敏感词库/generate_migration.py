# -*- coding: utf-8 -*-
"""生成 bbs_sensitive_word 迁移 SQL 并追加到 upgrade-pg.sql / upgrade-mysql.sql（可重复运行）。

- 读取 docs/敏感词库/sensitive_words.txt（GitHub SensitivePy words.txt）
- 清理：去空行、去重、剔除 >255 字符（varchar(255) 上限）、单引号转义
- 分块生成（每 500 条一个 INSERT），全幂等：
  PG   : INSERT ... ON CONFLICT (keyword) DO NOTHING;
  MySQL: INSERT IGNORE INTO ...
- 迁移编号统一为 v031-sensitive-words-import（MySQL 侧 v030 已被占用）
- 若目标文件已存在 v031 块，先截断再重新生成（幂等）
"""
import io
import os

BASE = r'D:\workspace\20260522-20260615-daqianbbsfrontend\daqian-bbs'
SRC = os.path.join(BASE, 'docs', '敏感词库', 'sensitive_words.txt')
PG = os.path.join(BASE, 'bbs-server', 'src', 'main', 'resources', 'db', 'init', 'upgrade-pg.sql')
MY = os.path.join(BASE, 'bbs-server', 'src', 'main', 'resources', 'db', 'init', 'upgrade-mysql.sql')
MARKER = '-- @migration: v031-sensitive-words-import'

# ── 1. 读词库并清理 ──
seen = set()
words = []
skipped_long = 0
for line in io.open(SRC, encoding='utf-8-sig'):
    w = line.strip()
    if not w:
        continue
    if len(w) > 255:
        skipped_long += 1
        continue
    if w in seen:
        continue
    seen.add(w)
    words.append(w)

escaped = [w.replace("'", "''") for w in words]
N = len(escaped)
CHUNK = 500
chunks = [escaped[i:i + CHUNK] for i in range(0, N, CHUNK)]

# ── 2. 生成 SQL 块 ──
def sql_rows(chunk):
    return ',\n'.join("('%s')" % w for w in chunk)

pg_parts = []
for c in chunks:
    pg_parts.append(
        "INSERT INTO bbs_sensitive_word (keyword) VALUES\n"
        + sql_rows(c)
        + "\nON CONFLICT (keyword) DO NOTHING;"
    )
pg_block = '\n'.join(['-- @migration: v031-sensitive-words-import 导入国内常见敏感词库（来源：SensitivePy words.txt，全量幂等）'] + pg_parts)

my_parts = []
for c in chunks:
    my_parts.append(
        "INSERT IGNORE INTO `bbs_sensitive_word` (`keyword`) VALUES\n"
        + sql_rows(c)
        + ';'
    )
my_block = '\n'.join(['-- @migration: v031-sensitive-words-import 导入国内常见敏感词库（来源：SensitivePy words.txt，全量幂等）'] + my_parts)

# ── 3. 截断旧块 + 追加（保留原编码/BOM） ──
def rewrite(path, block):
    raw = open(path, 'rb').read()
    has_bom = raw.startswith(b'\xef\xbb\xbf')
    body = raw[3:] if has_bom else raw
    text = body.decode('utf-8')
    idx = text.find(MARKER)
    if idx != -1:
        text = text[:idx].rstrip('\r\n') + '\n'
    new = text + block + '\n'
    with open(path, 'wb') as f:
        if has_bom:
            f.write(b'\xef\xbb\xbf')
        f.write(new.encode('utf-8'))

rewrite(PG, pg_block)
rewrite(MY, my_block)

total_raw = sum(1 for _ in io.open(SRC, encoding='utf-8-sig'))
print('raw lines       :', total_raw)
print('skipped long>255:', skipped_long)
print('inserted words  :', N)
print('chunks          :', len(chunks))
print('PG ON CONFLICT count:', pg_block.count('ON CONFLICT (keyword) DO NOTHING;'))
print('MY INSERT IGNORE count:', my_block.count('INSERT IGNORE'))
