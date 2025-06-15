import os
import json
import re
from glob import glob
from collections import defaultdict

def parse_time_to_milliseconds(time_str):
    """Parses a time string like '00h 00m 00s 116ms 957µs' to total milliseconds."""
    pattern = r'(?:(\d+)h)?\s*(?:(\d+)m)?\s*(?:(\d+)s)?\s*(?:(\d+)ms)?\s*(?:(\d+)µs)?'
    match = re.match(pattern, time_str.strip())
    if not match:
        return 0.0
    hours, minutes, seconds, milliseconds, microseconds = [int(g or 0) for g in match.groups()]
    total_ms = (hours * 3600 + minutes * 60 + seconds) * 1000 + milliseconds + microseconds / 1000
    return total_ms

def format_milliseconds(ms_total):
    """Formats milliseconds back to a timestamp string."""
    total_microseconds = int(ms_total * 1000)
    hours = total_microseconds // 3_600_000_000
    remaining = total_microseconds % 3_600_000_000
    minutes = remaining // 60_000_000
    remaining %= 60_000_000
    seconds = remaining // 1_000_000
    remaining %= 1_000_000
    milliseconds = remaining // 1000
    microseconds = remaining % 1000

    return f"{hours:02d}h {minutes:02d}m {seconds:02d}s {milliseconds:03d}ms {microseconds:03d}µs"

def process_jsonl_file(filepath):
    accum = defaultdict(float)
    count = 0
    with open(filepath, 'r') as f:
        for line in f:
            data = json.loads(line)
            count += 1
            for key, value in data.items():
                if key == "input":
                    continue
                if isinstance(value, (int, float)):
                    accum[key] += value
                elif isinstance(value, str) and re.match(r'\d+h.*', value):
                    accum[key] += parse_time_to_milliseconds(value)

    if count == 0:
        return

    print("=" * 60)
    print(f"Averages for file: {os.path.basename(filepath)}")
    print("-" * 60)
    max_key_len = max(len(key) for key in accum.keys())
    for key, total in sorted(accum.items()):
        avg = total / count
        if 'elapsed' in key:
            formatted = format_milliseconds(avg)
            print(f"{key:<{max_key_len}} : {formatted}")
        else:
            print(f"{key:<{max_key_len}} : {avg:.3f}")
    print("=" * 60 + "\n")

def main():
    folder_path = ""
    jsonl_files = glob(os.path.join(folder_path, "*.jsonl"))

    if not jsonl_files:
        print("No JSONL files found in the directory.")
        return

    for file_path in sorted(jsonl_files):
        process_jsonl_file(file_path)

if __name__ == "__main__":
    main()
