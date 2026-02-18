import sqlite3
conn = sqlite3.connect('library.db')
c = conn.cursor()

for t in ['customers', 'suppliers']:
    print(f"--- {t} ---")
    c.execute(f"PRAGMA table_info({t})")
    for row in c.fetchall():
        # cid, name, type, notnull, dflt_value, pk
        print(f"COL: {row[1]} TYPE: {row[2]} NOTNULL: {row[3]} DFLT: {row[4]}")
conn.close()
