#!/usr/bin/env python3
import argparse
import base64
import io
import json
import os
import random
import sys
import time
import textwrap
import urllib.request
import uuid

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:  # pragma: no cover
    print("缺少依赖：Pillow，请先执行 pip3 install pillow", file=sys.stderr)
    sys.exit(1)


FONT_CANDIDATES = [
    "/System/Library/Fonts/PingFang.ttc",
    "/System/Library/Fonts/STHeiti Medium.ttc",
    "/Library/Fonts/Arial Unicode.ttf",
    "/Library/Fonts/Songti.ttc",
]

FORMAT_MAP = {
    "png": ("PNG", "image/png"),
    "jpg": ("JPEG", "image/jpeg"),
    "jpeg": ("JPEG", "image/jpeg"),
    "webp": ("WEBP", "image/webp"),
}


def load_font(font_path, font_size):
    if font_path and os.path.exists(font_path):
        try:
            return ImageFont.truetype(font_path, font_size)
        except Exception:
            pass
    for candidate in FONT_CANDIDATES:
        if os.path.exists(candidate):
            try:
                return ImageFont.truetype(candidate, font_size)
            except Exception:
                continue
    return ImageFont.load_default()


def random_color(low=200, high=255):
    return (
        random.randint(low, high),
        random.randint(low, high),
        random.randint(low, high),
    )


def add_noise(image, strength=0.25):
    noise = Image.effect_noise(image.size, random.randint(40, 80)).convert("RGB")
    return Image.blend(image, noise, strength)


def render_image_bytes(text, fmt, min_bytes, width_range, height_range, font_path):
    attempts = 0
    width = random.randint(*width_range)
    height = random.randint(*height_range)

    while attempts < 8:
        attempts += 1
        bg = Image.new("RGB", (width, height), color=random_color(210, 255))
        draw = ImageDraw.Draw(bg)
        font_size = max(24, int(min(width, height) * 0.06))
        font = load_font(font_path, font_size)

        lines = textwrap.wrap(text, width=16)
        y = 20
        for line in lines:
            draw.text((20, y), line, font=font, fill=(20, 20, 20))
            y += font_size + 10
            if y > height - font_size:
                break

        bg = add_noise(bg, strength=0.25)

        buf = io.BytesIO()
        pil_format, _ = FORMAT_MAP[fmt]
        save_kwargs = {}
        if pil_format == "JPEG":
            save_kwargs["quality"] = random.randint(88, 95)
            save_kwargs["optimize"] = False
        elif pil_format == "PNG":
            save_kwargs["compress_level"] = 0
        elif pil_format == "WEBP":
            save_kwargs["quality"] = random.randint(88, 95)
        bg.save(buf, format=pil_format, **save_kwargs)
        size = buf.tell()
        if size >= min_bytes:
            return buf.getvalue()

        width = int(width * 1.18)
        height = int(height * 1.18)

    return buf.getvalue()


def to_data_url(image_bytes, mime):
    encoded = base64.b64encode(image_bytes).decode("ascii")
    return f"data:{mime};base64,{encoded}"


def build_text(username, title, min_chars):
    base = f"用户 {username} 针对题目《{title}》的答题说明与截图补充说明。"
    result = base
    while len(result) < min_chars:
        result += " " + base
    return result


def build_answer_html(username, title, image_count, min_chars, min_image_kb,
                      width_range, height_range, formats, font_path):
    parts = []
    text_block = build_text(username, title, min_chars)
    parts.append(f"<p>{text_block}</p>")
    for idx in range(image_count):
        fmt = random.choice(formats)
        pil_format, mime = FORMAT_MAP[fmt]
        label = f"用户名:{username} 题目:{title} 图片:{idx + 1}"
        image_bytes = render_image_bytes(
            label,
            fmt,
            min_image_kb * 1024,
            width_range,
            height_range,
            font_path,
        )
        data_url = to_data_url(image_bytes, mime)
        parts.append(f"<img src=\"{data_url}\" style=\"max-width:100%;height:auto;\"/>")
        parts.append("<br/>")
    return "<div>" + "".join(parts) + "</div>"


def post_multipart(url, fields, timeout):
    boundary = "----Boundary" + uuid.uuid4().hex
    chunks = []
    for name, value in fields.items():
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8"))
        chunks.append(value.encode("utf-8"))
        chunks.append(b"\r\n")
    chunks.append(f"--{boundary}--\r\n".encode("utf-8"))
    body = b"".join(chunks)

    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    req.add_header("Content-Length", str(len(body)))
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.status, resp.read().decode("utf-8", errors="ignore")


def parse_data_json(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    users = [u["username"] for u in data.get("users", []) if u.get("username") and u["username"] != "admin"]
    questions = []
    for c in data.get("chapters", []):
        for q in c.get("questions", []):
            if q.get("title"):
                questions.append(q["title"])
    return users, questions


def main():
    parser = argparse.ArgumentParser(description="生成压力测试答题数据")
    parser.add_argument("--data-json", default="src/main/resources/data.json")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--min-images", type=int, default=10)
    parser.add_argument("--min-text", type=int, default=100)
    parser.add_argument("--heavy-images", type=int, default=100)
    parser.add_argument("--heavy-text", type=int, default=1000)
    parser.add_argument("--heavy-question-index", type=int, default=0)
    parser.add_argument("--min-image-kb", type=int, default=300)
    parser.add_argument("--width-min", type=int, default=600)
    parser.add_argument("--width-max", type=int, default=1400)
    parser.add_argument("--height-min", type=int, default=400)
    parser.add_argument("--height-max", type=int, default=1000)
    parser.add_argument("--formats", default="png,jpeg")
    parser.add_argument("--font-path", default="")
    parser.add_argument("--sleep", type=float, default=0.2)
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--only-user", default="")
    parser.add_argument("--only-question", default="")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    users, questions = parse_data_json(args.data_json)
    if args.only_user:
        users = [u for u in users if u == args.only_user]
    if args.only_question:
        questions = [q for q in questions if q == args.only_question]

    if not users or not questions:
        print("未找到可用的用户或题目，请检查 data.json", file=sys.stderr)
        sys.exit(1)

    formats = [f.strip().lower() for f in args.formats.split(",") if f.strip()]
    formats = [f for f in formats if f in FORMAT_MAP]
    if not formats:
        print("可用的图片格式为空，请检查 --formats 参数", file=sys.stderr)
        sys.exit(1)

    width_range = (args.width_min, args.width_max)
    height_range = (args.height_min, args.height_max)

    save_url = args.base_url.rstrip("/") + "/quiz/save"
    print(f"将写入 {len(users)} 个用户、{len(questions)} 道题目，目标: {save_url}")

    for user_idx, username in enumerate(users, start=1):
        for q_idx, title in enumerate(questions):
            is_heavy = (q_idx == args.heavy_question_index)
            image_count = args.heavy_images if is_heavy else args.min_images
            text_min = args.heavy_text if is_heavy else args.min_text

            html = build_answer_html(
                username=username,
                title=title,
                image_count=image_count,
                min_chars=text_min,
                min_image_kb=args.min_image_kb,
                width_range=width_range,
                height_range=height_range,
                formats=formats,
                font_path=args.font_path,
            )

            if args.dry_run:
                print(f"[dry-run] user={username} question={title} images={image_count} text={text_min}")
                continue

            fields = {
                "questionTitle": title,
                "content": html,
                "username": username,
            }
            status, resp = post_multipart(save_url, fields, args.timeout)
            print(f"写入 user={username} question={title} status={status} resp={resp[:120]}")
            time.sleep(args.sleep)

    print("完成")


if __name__ == "__main__":
    main()
