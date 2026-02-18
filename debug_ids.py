import csv
import os

CSV_DIR = r'C:\Users\user\Library-Dr-Khaled\exports\open_shop_data'

def debug_ids():
    path = os.path.join(CSV_DIR, 'items.csv')
    with open(path, 'rb') as f:
        raw = f.read()
        content = raw.decode('utf-16', errors='replace')
        lines = content.splitlines()
        if lines and lines[0].strip() == 'sep=,':
            lines = lines[1:]
        reader = csv.DictReader(lines, delimiter=',')
        
        ids = []
        for i, row in enumerate(reader):
            pid = row.get('id')
            if pid:
                ids.append(pid)
            else:
                print(f"Row {i} has NO ID!")
        
        print(f"Total IDs: {len(ids)}")
        print(f"Unique IDs: {len(set(ids))}")
        
        if len(ids) != len(set(ids)):
            import collections
            duplicates = [item for item, count in collections.Counter(ids).items() if count > 1]
            print(f"Duplicates found ({len(duplicates)}): {duplicates[:10]}")
        else:
            print("No literal duplicates found in CSV list.")

if __name__ == "__main__":
    debug_ids()
