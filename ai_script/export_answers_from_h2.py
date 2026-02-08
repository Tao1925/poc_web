#!/usr/bin/env python3
import argparse
import base64
import datetime
import json
import os
import re
import sys
import tempfile
import subprocess
from pathlib import Path

try:
    import jaydebeapi
except ImportError:  # pragma: no cover
    print("缺少依赖 jaydebeapi，请先执行: pip3 install jaydebeapi JPype1", file=sys.stderr)
    sys.exit(1)

INVALID_CHARS = re.compile(r"[\\\\/:*?\"<>|]+")
IMG_TAG_RE = re.compile(r'(<img[^>]+src=["\'])(data:image/[^;]+;base64,([^"\']+))(["\'])', re.IGNORECASE)
MIME_EXT = {
    "image/jpeg": "jpg",
    "image/jpg": "jpg",
    "image/png": "png",
    "image/gif": "gif",
    "image/webp": "webp",
    "image/bmp": "bmp",
}


def sanitize_name(value: str) -> str:
    if value is None:
        return "unknown"
    value = INVALID_CHARS.sub("_", value)
    value = value.strip().strip(".")
    return value or "unknown"


def load_data_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    users = [u["username"] for u in data.get("users", []) if u.get("username") and u["username"] != "admin"]
    chapters = []
    for ci, chapter in enumerate(data.get("chapters", []), start=1):
        questions = []
        for qi, q in enumerate(chapter.get("questions", []), start=1):
            title = q.get("title", "")
            questions.append({"chapter_index": ci, "question_index": qi, "title": title})
        chapters.append({"chapter_index": ci, "questions": questions})
    return users, chapters


def find_h2_jar():
    repo = Path.home() / ".m2" / "repository" / "com" / "h2database" / "h2"
    if not repo.exists():
        return None
    versions = sorted([p for p in repo.iterdir() if p.is_dir()], reverse=True)
    for v in versions:
        jar = v / f"h2-{v.name}.jar"
        if jar.exists():
            return jar
    return None


def build_jdbc_url(db_file: Path) -> str:
    db_path = str(db_file.resolve())
    if db_path.endswith(".mv.db"):
        db_path = db_path[:-6]
    return f"jdbc:h2:file:{db_path}"


def extract_images(html: str, output_dir: Path):
    image_count = 0

    def _replace(match):
        nonlocal image_count
        prefix, data_url, b64_data, suffix = match.groups()
        mime = data_url.split(";")[0].split(":", 1)[1].lower()
        ext = MIME_EXT.get(mime, "bin")
        image_count += 1
        filename = f"image_{image_count:03d}.{ext}"
        img_path = output_dir / filename
        try:
            raw = base64.b64decode(b64_data)
            img_path.write_bytes(raw)
        except Exception:
            image_count -= 1
            return match.group(0)
        return f"{prefix}{filename}{suffix}"

    new_html = IMG_TAG_RE.sub(_replace, html)
    return new_html, image_count


def find_latest_backup(backup_dir: Path):
    if not backup_dir.exists():
        return None
    candidates = sorted(backup_dir.glob("pocdb_*.zip"))
    return candidates[-1] if candidates else None


def restore_from_backup(h2_jar: Path, backup_zip: Path, temp_dir: Path, db_name: str):
    cmd = [
        "java",
        "-cp",
        str(h2_jar),
        "org.h2.tools.Restore",
        "-file",
        str(backup_zip),
        "-dir",
        str(temp_dir),
        "-db",
        db_name,
    ]
    subprocess.check_call(cmd)
    restored = temp_dir / f"{db_name}.mv.db"
    if not restored.exists():
        raise RuntimeError(f"恢复失败，未找到 {restored}")
    return restored


def main():
    parser = argparse.ArgumentParser(description="导出 H2 答题内容为 HTML + 图片文件")
    parser.add_argument("--db", required=True, help="H2 数据库文件路径（.mv.db）")
    parser.add_argument("--data-json", default="src/main/resources/data.json", help="data.json 路径")
    parser.add_argument("--out-dir", default="data", help="输出根目录")
    parser.add_argument("--backup-zip", default="", help="备份 zip 文件路径（不指定时自动使用 data/backups 最新文件）")
    parser.add_argument("--backup-dir", default="data/backups", help="备份目录（默认 data/backups）")
    parser.add_argument("--allow-live-db", action="store_true", help="允许直接读取运行中的数据库文件（不建议）")
    parser.add_argument("--dry-run", action="store_true", help="只打印信息，不写文件")
    args = parser.parse_args()

    db_file = Path(args.db)
    if not db_file.exists():
        print(f"数据库文件不存在: {db_file}", file=sys.stderr)
        sys.exit(1)

    data_json_path = Path(args.data_json)
    if not data_json_path.exists():
        print(f"data.json 不存在: {data_json_path}", file=sys.stderr)
        sys.exit(1)

    users, chapters = load_data_json(data_json_path)
    if not users:
        print("data.json 中未找到非 admin 用户", file=sys.stderr)
        sys.exit(1)

    h2_jar = find_h2_jar()
    if not h2_jar:
        print("未找到 H2 驱动 jar，请确认 ~/.m2/repository/com/h2database/h2", file=sys.stderr)
        sys.exit(1)

    lock_path = Path(str(db_file) + ".process.lock")
    if args.allow_live_db and lock_path.exists():
        print(f"检测到数据库锁文件 {lock_path}，请停止服务或使用备份导出", file=sys.stderr)
        sys.exit(1)

    source_db_file = db_file
    if not args.allow_live_db:
        backup_zip = Path(args.backup_zip) if args.backup_zip else None
        if not backup_zip:
            backup_zip = find_latest_backup(Path(args.backup_dir))
        if not backup_zip or not backup_zip.exists():
            print("未找到可用备份文件，请提供 --backup-zip 或确认 data/backups 中存在备份", file=sys.stderr)
            sys.exit(1)
        temp_dir = Path(tempfile.mkdtemp(prefix="h2_restore_"))
        db_name = sanitize_name(db_file.stem.replace(".mv", ""))
        source_db_file = restore_from_backup(h2_jar, backup_zip, temp_dir, db_name)
        print(f"使用备份恢复到临时库: {source_db_file}")

    jdbc_url = build_jdbc_url(source_db_file)
    conn = jaydebeapi.connect("org.h2.Driver", jdbc_url, ["sa", ""], str(h2_jar))
    cur = conn.cursor()

    cur.execute("SELECT id, username FROM users")
    user_rows = cur.fetchall()
    user_id_map = {row[1]: row[0] for row in user_rows}

    cur.execute("SELECT id, title FROM questions")
    question_rows = cur.fetchall()
    question_id_map = {row[1]: row[0] for row in question_rows}

    today = datetime.datetime.now().strftime("%y%m%d")
    base_dir = Path(args.out_dir) / f"answer_{today}"
    if not args.dry_run:
        base_dir.mkdir(parents=True, exist_ok=True)

    stats = {"users": 0, "questions": 0, "images": 0}

    for username in users:
        stats["users"] += 1
        user_dir = base_dir / sanitize_name(username)
        if not args.dry_run:
            user_dir.mkdir(parents=True, exist_ok=True)

        user_id = user_id_map.get(username)
        if user_id is None:
            continue

        for chapter in chapters:
            for q in chapter["questions"]:
                title = q["title"]
                question_id = question_id_map.get(title)
                if question_id is None:
                    continue

                cur.execute(
                    "SELECT content FROM answers WHERE user_id = ? AND question_id = ?",
                    (user_id, question_id),
                )
                rows = cur.fetchall()
                if not rows:
                    continue
                content = rows[0][0]
                if not content or not str(content).strip():
                    continue

                folder_name = f"{q['chapter_index']}_{q['question_index']}_{sanitize_name(title)}"
                question_dir = user_dir / folder_name
                if not args.dry_run:
                    question_dir.mkdir(parents=True, exist_ok=True)

                html, img_count = extract_images(str(content), question_dir)
                stats["questions"] += 1
                stats["images"] += img_count

                if not args.dry_run:
                    (question_dir / "answer.html").write_text(html, encoding="utf-8")

    cur.close()
    conn.close()

    print(f"导出完成: {base_dir}")
    print(f"用户数: {stats['users']} | 题目数: {stats['questions']} | 图片数: {stats['images']}")


if __name__ == "__main__":
    main()
