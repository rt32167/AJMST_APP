"""Download ETCM 2.0 herb list, then each herb's Basic Information section.

The script runs sequentially, limits request starts, and resumes from checkpoints
under build/etcm_fetch. It deliberately excludes related tables and network data.
"""

import argparse
import html
import json
import math
import random
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen


PROJECT = Path(__file__).resolve().parent.parent
RESOURCE_DIR = PROJECT / "res" / "raw"
CHECKPOINT_DIR = PROJECT / "build" / "etcm_fetch"
LIST_FILE = RESOURCE_DIR / "etcm_herb_list_zh.json"
DETAIL_FILE = RESOURCE_DIR / "etcm_herb_basic_zh.json"
LIST_CHECKPOINT = CHECKPOINT_DIR / "list_pages.json"
DETAIL_CHECKPOINT = CHECKPOINT_DIR / "basic_info.jsonl"
API = "http://www.tcmip.cn:18124"
LIST_URL = "http://www.tcmip.cn/ETCM2/front/#/browse/herb"
DETAIL_PREFIX = "http://www.tcmip.cn/ETCM2/front/#/Detail/herb/"
HEADERS = {
    "Accept": "application/json",
    "Content-Type": "application/json; charset=utf-8",
    "Referer": "http://www.tcmip.cn/ETCM2/front/",
    "User-Agent": "AJMST-ETCM-herb-import/1.0 (sequential, rate-limited)",
}


def utc_now():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as output:
        json.dump(value, output, ensure_ascii=False, indent=2)
        output.write("\n")
    temporary.replace(path)


class RateLimitedClient:
    def __init__(self, interval):
        self.interval = interval
        self.next_request = 0.0

    def request(self, path, payload=None):
        url = API + path
        for attempt in range(5):
            wait = self.next_request - time.monotonic()
            if wait > 0:
                time.sleep(wait)
            self.next_request = time.monotonic() + self.interval
            body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
            request = Request(url, data=body, headers=HEADERS, method="GET" if body is None else "POST")
            try:
                with urlopen(request, timeout=45) as response:
                    result = json.load(response)
                if result.get("code") != 1:
                    raise ValueError(f"API error at {path}: {result.get('msg')}")
                return result
            except (HTTPError, URLError, TimeoutError, OSError, ValueError) as error:
                if isinstance(error, HTTPError) and error.code not in (429, 500, 502, 503, 504):
                    raise
                if attempt == 4:
                    raise RuntimeError(f"Request failed after retries: {path}") from error
                delay = min(30, 3 * (2 ** attempt)) + random.uniform(0, 1)
                print(f"Retry {attempt + 1}/4 in {delay:.1f}s: {path}: {error}", flush=True)
                time.sleep(delay)


def clean_value(value):
    if isinstance(value, str):
        return html.unescape(re.sub(r"<[^>]+>", "", value)).strip()
    if isinstance(value, list):
        return [clean_value(item) for item in value]
    return value


def herb_name(row):
    names = row.get("中药材名称")
    if not isinstance(names, list) or len(names) != 1 or not names[0]:
        raise ValueError(f"Unexpected herb name in list row: {row}")
    return names[0]


def fetch_list(client, page_size):
    checkpoint = {"pages": {}}
    if LIST_CHECKPOINT.exists():
        checkpoint = json.loads(LIST_CHECKPOINT.read_text(encoding="utf-8"))
        if checkpoint.get("page_size") != page_size:
            raise ValueError("Existing list checkpoint uses a different page size")
    checkpoint["page_size"] = page_size

    def get_page(page_number):
        key = str(page_number)
        if key not in checkpoint["pages"]:
            result = client.request("/home/browse/", {
                "type": "herb", "pageNo": page_number, "pageSize": page_size, "language": "cn"
            })
            section = result["data"][0]
            if section.get("type") != "herb":
                raise ValueError(f"Unexpected list section on page {page_number}")
            checkpoint["pages"][key] = {"count": section["count"], "rows": section["data"]}
            write_json(LIST_CHECKPOINT, checkpoint)
            print(f"List page {page_number}: {len(section['data'])} rows", flush=True)
        return checkpoint["pages"][key]

    first = get_page(1)
    expected = first["count"]
    pages = math.ceil(expected / page_size)
    for page_number in range(2, pages + 1):
        get_page(page_number)

    rows = []
    for page_number in range(1, pages + 1):
        page = checkpoint["pages"][str(page_number)]
        if page["count"] != expected:
            raise ValueError("List count changed during download; restart with a fresh checkpoint")
        rows.extend({key: clean_value(value) for key, value in row.items() if key != "linkformat"}
                    for row in page["rows"])
    names = [herb_name(row) for row in rows]
    if len(rows) != expected or len(set(names)) != expected:
        raise ValueError(f"List incomplete or duplicated: rows={len(rows)}, unique={len(set(names))}, expected={expected}")
    write_json(LIST_FILE, {
        "source": "ETCM 2.0", "source_url": LIST_URL, "language": "cn",
        "retrieved_at_utc": utc_now(), "count": expected, "items": rows,
    })
    print(f"List complete: {expected} herbs -> {LIST_FILE}", flush=True)
    return names


def parse_basic_info(result, requested_name):
    sections = result.get("data") or []
    section = next((part for part in sections if part.get("id") == "base_information"), None)
    if not section or not isinstance(section.get("value"), list):
        raise ValueError(f"Missing Basic Information for {requested_name}")
    fields = {}
    for field in section["value"]:
        if "key" in field and "value" in field:
            fields[field["key"]] = clean_value(field["value"])
    if fields.get("药材名") != requested_name:
        raise ValueError(f"Detail name mismatch: requested={requested_name}, received={fields.get('药材名')}")
    return fields


def read_detail_checkpoint():
    records = {}
    if DETAIL_CHECKPOINT.exists():
        with DETAIL_CHECKPOINT.open(encoding="utf-8") as input_file:
            for line in input_file:
                if line.strip():
                    record = json.loads(line)
                    records[record["name"]] = record
    return records


def fetch_details(client, names):
    records = read_detail_checkpoint()
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)
    with DETAIL_CHECKPOINT.open("a", encoding="utf-8", newline="\n") as checkpoint:
        for index, name in enumerate(names, 1):
            if name not in records:
                result = client.request("/home/detail/?" + urlencode({
                    "id": name, "type": "herb", "language": "cn"
                }))
                record = {
                    "name": name,
                    "detail_url": DETAIL_PREFIX + quote(name, safe=""),
                    "basic_info": parse_basic_info(result, name),
                }
                checkpoint.write(json.dumps(record, ensure_ascii=False) + "\n")
                checkpoint.flush()
                records[name] = record
            if index % 25 == 0 or index == len(names):
                print(f"Basic information: {index}/{len(names)}", flush=True)

    ordered = [records[name] for name in names]
    write_json(DETAIL_FILE, {
        "source": "ETCM 2.0", "source_url": LIST_URL,
        "source_detail_url_pattern": DETAIL_PREFIX + "{name}",
        "language": "cn", "retrieved_at_utc": utc_now(),
        "count": len(ordered), "items": ordered,
    })
    print(f"Basic information complete: {len(ordered)} herbs -> {DETAIL_FILE}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("list", "details", "all"), default="all")
    parser.add_argument("--interval", type=float, default=1.0,
                        help="Minimum seconds between request starts (default: 1.0)")
    parser.add_argument("--page-size", type=int, default=100)
    args = parser.parse_args()
    if args.interval < 1 or args.page_size < 1 or args.page_size > 100:
        parser.error("interval must be >= 1 second and page size must be 1..100")

    client = RateLimitedClient(args.interval)
    if args.phase in ("list", "all"):
        names = fetch_list(client, args.page_size)
    else:
        if not LIST_FILE.exists():
            parser.error("Download the list first with --phase list")
        saved = json.loads(LIST_FILE.read_text(encoding="utf-8"))
        if len(saved["items"]) != saved["count"]:
            raise ValueError("Saved list is incomplete")
        names = [herb_name(row) for row in saved["items"]]
    if args.phase in ("details", "all"):
        fetch_details(client, names)


if __name__ == "__main__":
    main()
