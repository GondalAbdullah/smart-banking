|Question|Minimal decision|
|Savings rules|Minimum balance 1,000. Max 3 withdrawals per calendar month. 4% annual interest.|
|Current rules|Overdraft limit 10,000 (balance can go down to −10,000). No interest.|
|Fixed Deposit rules|7% annual interest. Term in months is set when the account is opened, which gives the maturity date. Any withdrawal before maturity is rejected.|
|Interest application|Manual menu option “Apply monthly interest” (Manager/Admin). Monthly interest = balance × rate ÷ 12, recorded as a transaction.|
|Where rules live|data/rules.properties, loaded at startup with Java’s built-in Properties class. “Configure banking rules” (Admin) edits a value and saves the file.|
|Employee login|Employee ID + password at startup. If employees.csv is empty, seed ADMIN001 / admin123. Passwords stored as salted SHA-256 hashes (MessageDigest, no library needed).|
|Branch report|Single branch. Shows customers (active/inactive), accounts per type, total deposits held, and loans per status.|
|Deliverables|No deadline. JUnit 5 is the only external library, used for tests only. A class diagram in Mermaid inside docs/, which GitHub renders automatically.|