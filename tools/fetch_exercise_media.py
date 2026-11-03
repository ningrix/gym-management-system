# -*- coding: utf-8 -*-
"""按需从部分克隆的仓库中取回媒体文件（并行 git show，缺什么取什么）。"""
import json
import os
import subprocess
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.join(ROOT, 'exercises-dataset')
DATA = os.path.join(REPO, 'data', 'exercises.json')

GIF_LIMIT = 200          # 首批 GIF 数量
WORKERS = 12


def git_show(path, out_path):
    if os.path.exists(out_path) and os.path.getsize(out_path) > 0:
        return 0
    r = subprocess.run(['git', 'show', 'HEAD:' + path], cwd=REPO,
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    if r.returncode != 0 or not r.stdout:
        return -1
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, 'wb') as f:
        f.write(r.stdout)
    return len(r.stdout)


def main():
    items = json.load(open(DATA, encoding='utf-8'))
    jobs = []
    for idx, ex in enumerate(items):
        if ex.get('image'):
            jobs.append(ex['image'])
        if idx < GIF_LIMIT and ex.get('gif_url'):
            jobs.append(ex['gif_url'])

    print('待取文件', len(jobs), '（GIF 上限 %d）' % GIF_LIMIT)
    total = 0
    failed = 0
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futs = [pool.submit(git_show, p, os.path.join(REPO, p.replace('/', os.sep))) for p in jobs]
        for i, f in enumerate(futs, 1):
            n = f.result()
            if n > 0:
                total += n
            elif n < 0:
                failed += 1
            if i % 200 == 0:
                print('  %d/%d  已下载 %.1f MB' % (i, len(jobs), total / 1048576))
    print('完成：成功 %d，失败 %d，合计 %.1f MB' % (len(jobs) - failed, failed, total / 1048576))


if __name__ == '__main__':
    main()
