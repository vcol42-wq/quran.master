import sqlite3

db_path = 'app/src/main/assets/azkar.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Get all tables
cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
tables = cursor.fetchall()

for table in tables:
    table_name = table[0]
    print(f"Table: {table_name}")
    cursor.execute(f"PRAGMA table_info({table_name});")
    columns = cursor.fetchall()
    for col in columns:
        print(f"  {col[1]} - {col[2]}")
    
    cursor.execute(f"SELECT * FROM {table_name} LIMIT 3;")
    rows = cursor.fetchall()
    print("  Sample rows:")
    for row in rows:
        print(f"    {row}")
    print()

conn.close()
