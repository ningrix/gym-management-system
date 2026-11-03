# -*- coding: utf-8 -*-
"""通过 Gitee raw 通道并行补齐动作库媒体（比逐个 git show 快一个数量级）。

用法：
  python tools/fetch_media_fast.py            # 补图片 + 前 200 个 GIF
  python tools/fetch_media_fast.py --all      # 图片与 GIF 全量
  python tools/fetch_media_fast.py --gif 600  # 指定 GIF 数量
"""
import argparse
import json
import os
import sys
import threading
import urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.join(ROOT, 'exercises-dataset')
DATA = os.path.join(REPO, 'data', 'exercises.json')
BASE = 'https://gitee.com/will-eventually-become-a-cat/exercises-dataset/raw/main/'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36'

lock = threading.Lock()
stats = {'ok': 0, 'skip': 0, 'fail': 0, 'bytes': 0}


def fetch(rel):
    out = os.path.join(REPO, rel.replace('/', os.sep))
    if os.path.exists(out) and os.path.getsize(out) > 0:
        with lock:
            stats['skip'] += 1
        return
    req = urllib.request.Request(BASE + rel, headers={'User-Agent': UA, 'Referer': 'https://gitee.com/'})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data = r.read()
        if not data:
            raise ValueError('empty')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, 'wb') as f:
            f.write(data)
        with lock:
            stats['ok'] += 1
            stats['bytes'] += len(data)
    except Exception:
        with lock:
            stats['fail'] += 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--all', action='store_true', help='下载全部 GIF')
    ap.add_argument('--gif', type=int, default=200, help='GIF 下载数量')
    ap.add_argument('--workers', type=int, default=24)
    args = ap.parse_args()

    items = json.load(open(DATA, encoding='utf-8'))
    gif_limit = len(items) if args.all else args.gif
    jobs = []
    for idx, ex in enumerate(items):
        if ex.get('image'):
            jobs.append(ex['image'])
        if idx < gif_limit and ex.get('gif_url'):
            jobs.append(ex['gif_url'])

    missing = [p for p in jobs if not os.path.exists(os.path.join(REPO, p.replace('/', os.sep)))]
    print('总任务 %d，其中缺失 %d，并发 %d' % (len(jobs), len(missing), args.workers))
    if not missing:
        print('全部就绪')
        return 0

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        list(pool.map(fetch, missing))

    print('完成：新增 %d，跳过 %d，失败 %d，下载 %.1f MB'
          % (stats['ok'], stats['skip'], stats['fail'], stats['bytes'] / 1048576))
    return 0 if stats['fail'] == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
