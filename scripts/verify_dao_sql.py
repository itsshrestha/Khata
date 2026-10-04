#!/usr/bin/env python3
"""
Verifies the SQL inside the Room DAOs against a real SQLite database, without Android or Room.

1. Extracts every @Query(...) string from app/src/main/java/**/dao/*.kt and executes it
   (catches syntax errors, wrong column/table names, bad aliases).
2. Runs behavioural checks on the customer search / balance queries using realistic data.

The DDL below mirrors the entity classes; keep it in sync when entities change.
"""
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DAO_DIR = ROOT / "app/src/main/java/com/khata/app/data/local/dao"

DDL = """
CREATE TABLE customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL, phone TEXT, address TEXT, notes TEXT,
    isArchived INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL
);
CREATE INDEX index_customers_name ON customers(name);
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    customerId INTEGER NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL,
    description TEXT, paymentMethod TEXT,
    transactionDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
    FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE RESTRICT
);
CREATE TABLE transaction_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    transactionId INTEGER NOT NULL, itemName TEXT NOT NULL,
    quantity REAL NOT NULL, unitPrice INTEGER NOT NULL, totalPrice INTEGER NOT NULL,
    FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
"""

# Generic values for every named parameter used by the DAO queries.
PARAMS = dict(
    id=1, customerId=1, query="", archived=0, type=None, method=None,
    fromDay=None, toDay=None, limit=10, updatedAt=0, transactionId=1,
)

QUERY_RE = re.compile(r'@Query\(\s*"""(.*?)"""|@Query\(\s*"([^"]*)"', re.DOTALL)


def extract_queries():
    queries = []
    for path in sorted(DAO_DIR.glob("*.kt")):
        for match in QUERY_RE.finditer(path.read_text()):
            queries.append((path.name, (match.group(1) or match.group(2)).strip()))
    return queries


def escape_like(text):
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def main():
    db = sqlite3.connect(":memory:")
    db.executescript(DDL)
    db.execute("PRAGMA foreign_keys = ON")

    # ---- 1. Every query must be valid SQL against the schema ----
    queries = extract_queries()
    assert len(queries) >= 15, f"expected many queries, found {len(queries)}"
    for file_name, sql in queries:
        bound = {name: PARAMS[name] for name in re.findall(r":(\w+)", sql)}
        try:
            db.execute(sql, bound).fetchall()
        except sqlite3.Error as error:
            print(f"FAIL {file_name}: {error}\n{sql}")
            sys.exit(1)
    print(f"OK: {len(queries)} DAO queries are valid SQL")

    # ---- 2. Behaviour: seed data (amounts in paisa) ----
    def customer(name, phone=None, archived=0):
        return db.execute(
            "INSERT INTO customers(name, phone, isArchived, createdAt, updatedAt) VALUES (?,?,?,0,0)",
            (name, phone, archived),
        ).lastrowid

    def tx(customer_id, kind, rupees, day=20000):
        db.execute(
            "INSERT INTO transactions(customerId,type,amount,transactionDate,createdAt,updatedAt)"
            " VALUES (?,?,?,?,0,0)",
            (customer_id, kind, rupees * 100, day),
        )

    ram = customer("Ram Bahadur", "9812345678")
    sita = customer("sita Devi", "9801111111")
    hari = customer("Hari Prasad")                      # no transactions, no phone
    old = customer("Old Customer", "9700000000", archived=1)
    odd = customer("Discount 50% Man", "9855555555")
    tx(ram, "CREDIT", 2000); tx(ram, "PAYMENT", 500)
    tx(sita, "CREDIT", 1000); tx(sita, "PAYMENT", 1000)
    tx(old, "CREDIT", 300)

    list_sql = next(sql for f, sql in queries if f == "CustomerDao.kt" and "WHERE c.isArchived" in sql)

    def search(text, archived=0):
        rows = db.execute(list_sql, dict(query=escape_like(text), archived=archived)).fetchall()
        return {r[1]: (r[-2], r[-1]) for r in rows}  # name -> (credit, paid)

    everyone = search("")
    names = [r[1] for r in db.execute(list_sql, dict(query="", archived=0))]
    assert names == sorted(names, key=str.lower), f"not ordered by name ignoring case: {names}"
    assert "Old Customer" not in names, "archived customers must be hidden"

    assert everyone["Ram Bahadur"] == (200000, 50000), "Ram: credit 2000, paid 500 -> owes 1500"
    assert everyone["sita Devi"] == (100000, 100000), "Sita fully paid but still listed"
    assert everyone["Hari Prasad"] == (0, 0), "customers without transactions still appear (LEFT JOIN)"

    assert list(search("ram")) == ["Ram Bahadur"], "name search is case-insensitive"
    assert list(search("SITA")) == ["sita Devi"]
    assert list(search("9812")) == ["Ram Bahadur"], "phone search"
    assert list(search("Hari")) == ["Hari Prasad"], "customer with NULL phone is searchable by name"
    assert list(search("zzz")) == []
    assert list(search("50%")) == ["Discount 50% Man"], "percent is matched literally"
    assert list(search("%")) == ["Discount 50% Man"], "bare % must not match everything"
    assert list(search("_")) == [], "bare _ must not act as a wildcard"
    assert list(search("Old", archived=1)) == ["Old Customer"], "archived list"

    count_sql = next(sql for f, sql in queries if "HAVING" in sql)
    owing = db.execute(count_sql).fetchone()[0]
    assert owing == 1, f"only Ram owes among active customers, got {owing}"

    totals_sql = next(sql for f, sql in queries if "FROM transactions" in sql
                      and "WHERE customerId = :customerId" in sql and "SUM(CASE" in sql)
    assert db.execute(totals_sql, dict(customerId=ram)).fetchone() == (200000, 50000)
    assert db.execute(totals_sql, dict(customerId=hari)).fetchone() == (0, 0), "no rows -> zeros, not NULL"

    # Detailed credit: items belong to one CREDIT row; the balance uses the credit's amount only,
    # so item rows can never be double counted.
    detailed = customer("Detailed Buyer", "9844444444")
    cur = db.execute(
        "INSERT INTO transactions(customerId,type,amount,transactionDate,createdAt,updatedAt)"
        " VALUES (?, 'CREDIT', 185000, 20000, 0, 0)", (detailed,))
    tx_id = cur.lastrowid
    db.executemany(
        "INSERT INTO transaction_items(transactionId,itemName,quantity,unitPrice,totalPrice) VALUES (?,?,?,?,?)",
        [(tx_id, "Rice", 10, 12000, 120000), (tx_id, "Cooking Oil", 2, 25000, 50000),
         (tx_id, "Biscuits", 5, 3000, 15000)])
    assert db.execute(totals_sql, dict(customerId=detailed)).fetchone() == (185000, 0)
    items_sql = next(sql for f, sql in queries if "FROM transaction_items" in sql)
    rows = db.execute(items_sql, dict(transactionId=tx_id)).fetchall()
    assert [r[2] for r in rows] == ["Rice", "Cooking Oil", "Biscuits"], "items keep insertion order"
    assert sum(r[-1] for r in rows) == 185000, "item totals add up to the credit amount"
    db.execute("DELETE FROM transactions WHERE id = ?", (tx_id,))
    assert db.execute("SELECT COUNT(*) FROM transaction_items").fetchone()[0] == 0, "items cascade with their credit"

    # RESTRICT: a customer with history cannot be hard-deleted.
    try:
        db.execute("DELETE FROM customers WHERE id = ?", (ram,))
        print("FAIL: customer with transactions was deleted")
        sys.exit(1)
    except sqlite3.IntegrityError:
        pass

    print("OK: search, balances, archive filter, LIKE escaping and RESTRICT behave as designed")


if __name__ == "__main__":
    main()
