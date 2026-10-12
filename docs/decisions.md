# Decisions

Answers to the questions the specification leaves open. When a choice was unclear, the simplest option that keeps the system safe was chosen.

## From the specification

| Question | Minimal decision |
|---|---|
| Savings rules | Minimum balance 1,000. Max 3 withdrawals per calendar month. 4% annual interest. |
| Current rules | Overdraft limit 10,000 (balance can go down to −10,000). No interest. |
| Fixed Deposit rules | 7% annual interest. Term in months is set when the account is opened, which gives the maturity date. Any withdrawal before maturity is rejected. |
| Interest application | Manual menu option “Apply monthly interest” (Manager/Admin). Monthly interest = balance × rate ÷ 12, recorded as a transaction. |
| Where rules live | data/rules.properties, loaded at startup with Java’s built-in Properties class. Created with the defaults on first run. “Configure banking rules” (Admin) edits a value and saves the file. |
| Employee login | Employee ID + password at startup. If there are no employees, seed E0001 / admin123, which must be changed at first login. IDs never encode the role, because roles can change. Passwords stored as salted PBKDF2-HMAC-SHA256 hashes (built into the JDK, no library needed); plain SHA-256 is too fast to resist guessing. |
| Branch report | Single branch. Shows customers (active/inactive), accounts per status, open accounts per type, total deposits held, and loans per status (added with the Loans module). |
| Deliverables | No deadline. JUnit 5 is the only external library, used for tests only. A class diagram in Mermaid inside docs/, which GitHub renders automatically. |

## Transactions

| Question | Minimal decision |
|---|---|
| Does a transfer out of Savings count toward the 3-withdrawals limit? | Yes. Money leaving a savings account counts as a withdrawal, however it leaves. |
| Can monthly interest be applied twice in one month? | Not blocked by the system. Applying it once a month is the Manager's/Admin's responsibility; every run is recorded in the audit trail. |
| Does a matured Fixed Deposit keep earning interest? | Yes, until the money is withdrawn. |
| Are failed attempts shown to the user? | Recorded with status FAILED and a reason. Shown in history and search; the mini statement shows successful transactions only. |

## Employees and roles

| Question | Minimal decision |
|---|---|
| Can a Manager do everything a Teller can? | Yes. Manager = all Teller permissions + supervisory ones. |
| Can the Admin move money? | No (separation of duties). Admin manages employees, rules and reports, and can apply interest. |
| Permissions the spec does not mention | Update customer and transfer → Teller. Deactivate customer → Manager. Viewing (customers, accounts, history) → any logged-in employee. |
| Failed logins | Account locks after 5 wrong passwords in a row. An Admin unlocks it by resetting the password. |

## Rules and reports

| Question | Minimal decision |
|---|---|
| Do rule changes affect existing accounts? | No. Each account keeps the terms it was opened with; new values apply to accounts opened afterwards. |
| What counts as “total deposits held”? | Sum of positive balances on open accounts. Overdrawn balances are reported separately as “overdrafts owed”. |

## Notifications

| Question | Minimal decision |
|---|---|
| Which events send an email? | Exactly the four in the spec: deposit, withdrawal, transfer, account closure. Opening deposits, interest and refused attempts send nothing. |
| Who is emailed for a transfer? | Both sides: the sender gets “Transfer sent”, the recipient gets “Transfer received”, even when it is the same customer. |
| What if sending fails? | The banking operation stands; the failure is printed as a warning. A notification can never undo money that has already moved. |
| What do emails show? | Amount, reference, new balance, and only the last 4 characters of the account number. |

## Loans

| Question | Minimal decision |
|---|---|
| Who records applications and repayments? | Tellers (and Managers, who inherit Teller permissions). Only a Manager approves or rejects. |
| Which account can receive a loan? | One of the customer's own accounts that can accept a deposit right now: not a fixed deposit, not frozen or closed. Checked when the application is recorded. |
| Is there loan interest? | No. The specification does not mention it; the customer repays exactly the principal. |
| What is “outstanding” before approval? | Zero. It becomes the principal on approval and goes down with each repayment. |
| Which loans block closing their account? | PENDING and APPROVED. REJECTED and REPAID do not. |
| What if the money cannot be credited at approval (e.g. the account is frozen)? | The approval fails, the loan stays PENDING, and a FAILED disbursement is recorded. |
| How are repayments taken? | From the linked account, under that account's normal withdrawal rules (a savings repayment counts toward the monthly limit). At most the outstanding amount. |
| Can one account have several loans? | Yes. |
| Notifications | The customer is emailed when a loan is approved or rejected. Repayments send nothing. |