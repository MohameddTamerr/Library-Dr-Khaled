import sqlite3
import csv
import os
import sys
from datetime import datetime

# Absolute paths
DB_PATH = r'C:\Users\user\Library-Dr-Khaled\library.db'
CSV_DIR = r'C:\Users\user\Library-Dr-Khaled\exports\open_shop_data'

csv.field_size_limit(sys.maxsize)

def import_data():
    print(f"Checking DB path: {DB_PATH} -> Exists: {os.path.exists(DB_PATH)}")
    print(f"Checking CSV dir: {CSV_DIR} -> Exists: {os.path.exists(CSV_DIR)}")

    now_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    def read_csv(filename):
        path = os.path.join(CSV_DIR, filename)
        if not os.path.exists(path):
            print(f"CRITICAL: {filename} NOT FOUND at {path}")
            return []
        print(f"Reading {filename}...")
        with open(path, 'rb') as f:
            raw = f.read()
            content = raw.decode('utf-16', errors='replace')
            lines = content.splitlines()
            if lines and lines[0].strip() == 'sep=,':
                lines = lines[1:]
            reader = csv.DictReader(lines, delimiter=',')
            data = list(reader)
            print(f"Loaded {len(data)} rows from {filename}")
            return data

    try:
        # Clear existing data
        print("Clearing tables...")
        for t in ['product_barcodes', 'products', 'categories', 'customers', 'suppliers']:
            cursor.execute(f"DELETE FROM {t}")
        conn.commit()
        print("Tables cleared and committed.")

        # Load data
        items_raw = read_csv('items.csv')
        barcodes_raw = read_csv('product_barcodes.csv')
        accounts_raw = read_csv('accounts.csv')

        # 1. Categories
        print("Processing Categories...")
        unique_categories = sorted(list(set(row.get('category', '').strip() for row in items_raw if row.get('category'))))
        for i, cat_name in enumerate(unique_categories, 1):
            cursor.execute("INSERT INTO categories (category_id, name, created_at) VALUES (?, ?, ?)", (i, cat_name, now_str))
        print(f"Inserted {len(unique_categories)} categories.")

        # Pre-process barcodes for products
        primary_barcodes = {}
        for row in barcodes_raw:
            iid = row.get('item_id')
            bc = row.get('barcode', '').strip()
            if iid and bc and iid not in primary_barcodes:
                primary_barcodes[iid] = bc

        # 2. Products
        print("Processing Products...")
        product_count = 0
        used_p_barcodes = set()
        items_map = {row['id']: row for row in items_raw if row.get('id')}
        
        for pid_str, row in items_map.items():
            pid = int(pid_str)
            pname = row.get('iname')
            if pname:
                p_barcode = primary_barcodes.get(pid_str, '').strip()
                if not p_barcode or p_barcode in used_p_barcodes:
                    p_barcode = f"P-{pid}"
                used_p_barcodes.add(p_barcode)
                
                cursor.execute("""
                    INSERT INTO products (
                        product_id, product_name, barcode, category, cost, price, 
                        quantity, min_stock, supplier, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    pid, pname, p_barcode, row.get('category', '').strip(),
                    float(row.get('price_pur', 0) or 0), float(row.get('price1', 0) or 0),
                    0, 0, '', now_str, now_str
                ))
                product_count += 1
        print(f"Inserted {product_count} products.")

        # 3. Barcodes
        print("Processing Barcodes...")
        bc_count = 0
        inserted_barcodes = set()
        for i, row in enumerate(barcodes_raw, 1):
            bc = row.get('barcode', '').strip()
            iid = row.get('item_id')
            if bc and iid and iid in items_map:
                if bc not in inserted_barcodes:
                    cursor.execute("INSERT INTO product_barcodes (product_barcode_id, barcode, product_id) VALUES (?, ?, ?)", 
                                   (bc_count + 1, bc, int(iid)))
                    inserted_barcodes.add(bc)
                    bc_count += 1
        print(f"Inserted {bc_count} unique barcodes into product_barcodes.")

        # 4. Accounts
        print("Processing Accounts...")
        acc_count = 0
        unique_accounts = {row['id']: row for row in accounts_raw if row.get('id')}
        for aid_str, row in unique_accounts.items():
            aid = int(aid_str)
            aname = row.get('acc_name')
            if aname:
                # Customers
                cursor.execute("""
                    INSERT INTO customers (
                        customer_id, address, created_at, customer_code, 
                        customer_name, mobile, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, (aid, row.get('acc_address', ''), now_str, f"C-{aid}", aname, row.get('acc_phone', ''), now_str))
                
                # Suppliers - Use 'ACTIVE' to satisfy CHECK (status IN ('ACTIVE','INACTIVE'))
                cursor.execute("""
                    INSERT INTO suppliers (
                        supplier_id, address, balance_due, created_at, 
                        email, name, phone, status, total_paid, total_purchases
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (aid, row.get('acc_address', ''), 0.0, now_str, '', aname, row.get('acc_phone', ''), 'ACTIVE', 0.0, 0.0))
                acc_count += 1
        print(f"Inserted {acc_count} accounts.")

        conn.commit()
        print("FINAL COMMIT SUCCESSFUL.")

        # Immediate Verification
        print("--- POST-IMPORT COUNTS ---")
        for t in ['categories', 'products', 'product_barcodes', 'customers', 'suppliers']:
            cursor.execute(f"SELECT COUNT(*) FROM {t}")
            print(f"{t}: {cursor.fetchone()[0]}")

    except Exception as e:
        conn.rollback()
        print(f"FATAL ERROR: {e}")
        import traceback
        traceback.print_exc()
    finally:
        conn.close()

if __name__ == "__main__":
    import_data()
