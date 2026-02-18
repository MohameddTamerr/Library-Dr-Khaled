import sqlite3
import os

DB_PATH = r'C:\Users\user\Library-Dr-Khaled\library.db'

def verify():
    if not os.path.exists(DB_PATH):
        print(f"Error: Database not found at {DB_PATH}")
        return

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    tables = ['categories', 'products', 'product_barcodes', 'customers', 'suppliers']
    print("--- FINAL RECORD COUNTS ---")
    for table in tables:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            print(f"{table.capitalize()}: {count}")
        except Exception as e:
            print(f"Error checking {table}: {e}")
            
    conn.close()

if __name__ == "__main__":
    verify()
