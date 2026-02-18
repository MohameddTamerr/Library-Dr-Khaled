import sqlite3
conn = sqlite3.connect('library.db')
c = conn.cursor()
c.execute("SELECT sql FROM sqlite_master WHERE name='products'")
sql = c.fetchone()[0]
print("--- FULL SQL ---")
print(sql)
print("--- END ---")
conn.close()
