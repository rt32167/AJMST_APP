"""Verify that packaged ETCM list and basic information resources match."""

import json
import sys
from pathlib import Path


sys.stdout.reconfigure(encoding="utf-8")
RESOURCE_DIR = Path(__file__).resolve().parent.parent / "res" / "raw"
herb_list = json.loads((RESOURCE_DIR / "etcm_herb_list_zh.json").read_text(encoding="utf-8"))
details = json.loads((RESOURCE_DIR / "etcm_herb_basic_zh.json").read_text(encoding="utf-8"))

list_names = [row["中药材名称"][0] for row in herb_list["items"]]
detail_names = [row["name"] for row in details["items"]]
assert herb_list["count"] == len(list_names) == len(set(list_names))
assert details["count"] == len(detail_names) == len(set(detail_names))
assert detail_names == list_names
assert all(row["basic_info"].get("药材名") == row["name"] for row in details["items"])
assert all(row["detail_url"].startswith(details["source_detail_url_pattern"].split("{")[0])
           for row in details["items"])
assert all("related_table" not in row and "basic_info" in row for row in details["items"])
print(f"Verified {len(list_names)} ETCM herb list and basic information records")
efficacy_count = sum(bool(row["basic_info"].get("功效")) for row in details["items"])
print(f"Records with a nonempty 功效 field: {efficacy_count}/{len(detail_names)}")
