import sqlite3
import os

DB_PATH = r'C:\Users\user\Library-Dr-Khaled\library.db'
SQL_OUT = r'C:\Users\user\Library-Dr-Khaled\products_sql.txt'

def get_sql():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT sql FROM sqlite_master WHERE name='products'")
    res = c.fetchone()[0]
    with open(SQL_OUT, 'w', encoding='utf-8') as f:
        f.write(res)
    
    # Also get indexes info
    c.execute("PRAGMA index_list(products)")
    indexes = c.fetchall()
    with open(r'C:\Users\user\Library-Dr-Khaled\indexes_sql.txt', 'w', encoding='utf-8') as f:
        for idx in indexes:
            f.write(str(idx) + '\n')
            # Get info for each index
            c.execute(f"PRAGMA index_info({idx[1]})")
            info = c.fetchall()
            f.write("  " + str(info) + '\n')

    conn.close()

if __name__ == "__main__":
    get_sql()
