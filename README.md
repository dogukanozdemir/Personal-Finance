# Local-First Personal Spending Analytics

A premium dark-mode spending analytics application that helps you understand where your money goes, spot blind spots, and cut waste. Built with Java Spring Boot backend and React frontend, with intelligent deduplication for daily bank statement uploads.

## Features

- 📊 **Dashboard**: Real-time KPIs, spending trends, category breakdowns
- 💳 **Transactions**: Searchable table with filters and quick actions
- 💡 **Insights**: Automated findings about spending patterns and blind spots
- 🎯 **Budgets**: Per-category monthly caps with progress tracking
- 🤖 **AI Analysis**: Chat-style interface for spending questions
- 📤 **Import**: Drag-and-drop Excel/CSV uploads with automatic deduplication

## Technology Stack

- **Backend**: Java Spring Boot 3.2+
- **Frontend**: React 18 + Vite + Tailwind CSS + Recharts
- **Database**: PostgreSQL 16
- **Deployment**: Docker/Podman compatible

## Quick Start

### Prerequisites

- Podman (or Docker)
- Podman machine must be running on macOS

### Setup Podman Machine (macOS)

```bash
# Check if Podman machine exists
podman machine list

# Start the machine if it's not running
podman machine start

# Verify it's running
podman ps
```

### Running the Application

1. Navigate to the project:
```bash
cd spending-analytics
```

2. Start all services:
```bash
podman compose up -d
# or use the provided script:
./start.sh
```

3. Access the application:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432 (Database server)

### Stopping the Application

```bash
# Stop containers (data persists in volume)
podman compose down

# Stop and remove volume (CAUTION: deletes all data!)
podman compose down -v
```

### First Time Setup

1. Open the **Import** page
2. Upload your bank statement (Excel or CSV)
3. The system will automatically detect the format and import transactions
4. Go to **Dashboard** to see your spending analytics

## Supported Bank Formats

Currently supports **Garanti Bank** (Turkey):
- Debit accounts (with Dekont No)
- Credit cards (with Bonus points)

The system auto-detects the account type and applies the appropriate deduplication logic.

## Deduplication Logic

The app intelligently handles re-uploads of overlapping date ranges:

**Debit Accounts**: Uses `account_id + date + dekont_no` (transaction ID from bank)

**Credit Cards**: Uses `account_id + date + merchant + amount` (composite key)

This allows you to upload partial statements daily without creating duplicates.

## Data Persistence

All data is stored in PostgreSQL within a Docker/Podman volume named `spending-analytics_postgres-data`.

The database persists across container restarts and rebuilds.

### Database Connection Details
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `spending_analytics` (or value from `.env`)
- **Username**: `spending_user` (or value from `.env`)
- **Password**: Check your `.env` file

### Backup Database
```bash
# Using pg_dump
podman exec spending-analytics-postgres pg_dump -U spending_user spending_analytics > backup.sql

# Or backup entire database
podman exec spending-analytics-postgres pg_dumpall -U spending_user > backup_all.sql
```

### Restore Database
```bash
# Restore from backup
cat backup.sql | podman exec -i spending-analytics-postgres psql -U spending_user spending_analytics
```

### Access Database

PostgreSQL is now accessible directly via standard host:port connection:

**Connection Details:**
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: Check your `.env` file (default: `spending_analytics`)
- **Username**: Check your `.env` file (default: `spending_user`)
- **Password**: Check your `.env` file
- **Connection String**: `postgresql://[username]:[password]@localhost:5432/spending_analytics`

**Method 1: Using psql CLI**
```bash
# Connect from inside container
podman exec -it spending-analytics-postgres psql -U spending_user spending_analytics

# Or from your local machine (if you have psql installed)
psql -h localhost -p 5432 -U spending_user -d spending_analytics
# When prompted, enter password: spending_pass

# Quick queries
podman exec spending-analytics-postgres psql -U spending_user spending_analytics -c "SELECT COUNT(*) FROM transactions;"
podman exec spending-analytics-postgres psql -U spending_user spending_analytics -c "SELECT category, SUM(amount) FROM transactions GROUP BY category;"
```

**Method 2: GUI Database Tools**

You can use any PostgreSQL client with the credentials above:

- **DBeaver** (Free, cross-platform) - https://dbeaver.io
- **pgAdmin** (Official PostgreSQL GUI) - https://www.pgadmin.org
- **TablePlus** (macOS/Windows, paid) - https://tableplus.com
- **Postico** (macOS, paid) - https://eggerapps.at/postico

Simply create a new PostgreSQL connection and use the credentials above.

**Method 3: Programming Language Access**

Python:
```python
import psycopg2

conn = psycopg2.connect(
    host="localhost",
    port=5432,
    database="spending_analytics",
    user="spending_user",
    password="spending_pass"
)
cursor = conn.cursor()

# Example queries
cursor.execute("SELECT COUNT(*) FROM transactions")
print(f"Total transactions: {cursor.fetchone()[0]}")

cursor.execute("SELECT category, SUM(amount) FROM transactions WHERE amount < 0 GROUP BY category")
for row in cursor.fetchall():
    print(f"{row[0]}: {row[1]:.2f} TL")

conn.close()
```

Node.js:
```javascript
const { Client } = require('pg');

const client = new Client({
  host: 'localhost',
  port: 5432,
  database: 'spending_analytics',
  user: 'spending_user',
  password: 'spending_pass'
});

await client.connect();
const res = await client.query('SELECT * FROM transactions');
console.log(res.rows);
await client.end();
```

**Backend API Endpoints (Alternative Access):**
- **Base URL**: `http://localhost:8080/api`
- `GET /api/transactions` - Get all transactions
- `GET /api/transactions/recent?days=30` - Recent transactions  
- `GET /api/dashboard/kpis?period=month` - Dashboard KPIs
- `GET /api/insights` - Generated insights

## Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## First Time Setup & Testing

### Handling Strict OOXML Excel Files

If you get an error about "Strict OOXML" when importing Excel files, you need to convert them first.

**Quick conversion script:**
```python
# Create convert_excel.py
from zipfile import ZipFile
import xml.etree.ElementTree as ET
from openpyxl import Workbook

def extract_data(filename):
    with ZipFile(filename, 'r') as zip_ref:
        strings_xml = zip_ref.read('xl/sharedStrings.xml')
        strings_root = ET.fromstring(strings_xml)
        shared_strings = [t.text or '' for si in strings_root for t in si if t.tag.endswith('t')]
        
        sheet_xml = zip_ref.read('xl/worksheets/sheet1.xml')
        sheet_root = ET.fromstring(sheet_xml)
        
        rows = []
        for row_elem in sheet_root.iter():
            if row_elem.tag.endswith('row'):
                row_data = []
                for cell in row_elem:
                    if cell.tag.endswith('c'):
                        v_elem = next((c for c in cell if c.tag.endswith('v')), None)
                        if v_elem is not None and v_elem.text:
                            idx = int(v_elem.text) if cell.get('t') == 's' else v_elem.text
                            row_data.append(shared_strings[idx] if cell.get('t') == 's' and idx < len(shared_strings) else str(idx))
                        else:
                            row_data.append('')
                if row_data:
                    rows.append(row_data)
        return rows

def convert(input_file, output_file):
    data = extract_data(input_file)
    wb = Workbook()
    ws = wb.active
    for row_idx, row_data in enumerate(data, 1):
        for col_idx, value in enumerate(row_data, 1):
            ws.cell(row=row_idx, column=col_idx, value=value)
    wb.save(output_file)
    print(f"✓ Converted {input_file} -> {output_file}")

# Convert your files
convert('debit.xlsx', 'debit_converted.xlsx')
convert('credit.xlsx', 'credit_converted.xlsx')
```

Then upload the `*_converted.xlsx` files in the app.

## Architecture

```
spending-analytics/
├── backend/           # Java Spring Boot API
│   ├── src/
│   │   └── main/
│   │       ├── java/com/spendinganalytics/
│   │       │   ├── config/      # SQLite, CORS config
│   │       │   ├── controller/  # REST API endpoints
│   │       │   ├── model/       # JPA entities
│   │       │   ├── repository/  # Data access layer
│   │       │   ├── service/     # Business logic
│   │       │   └── util/        # Excel parser, hash utils
│   │       └── resources/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/          # React SPA
│   ├── src/
│   │   ├── components/   # Reusable UI components
│   │   ├── pages/        # Main pages
│   │   ├── hooks/        # Custom React hooks
│   │   ├── utils/        # API client
│   │   └── styles/       # Global CSS
│   ├── package.json
│   └── Dockerfile
│
├── data/              # SQLite database (persisted)
└── docker-compose.yml
```

## API Endpoints

### Dashboard
- `GET /api/dashboard/kpis?period={today|week|month|year}`

### Transactions
- `GET /api/transactions`
- `GET /api/transactions/recent?days=30`
- `GET /api/transactions/range?start=2025-01-01&end=2025-01-31`
- `PUT /api/transactions/{id}`

### Import
- `POST /api/import/upload` (multipart/form-data)
- `GET /api/import/accounts`

### Insights
- `GET /api/insights`
- `POST /api/insights/generate`

### AI
- `POST /api/ai/chat`

## Common Commands

```bash
# View logs
podman logs -f spending-analytics-backend
podman logs -f spending-analytics-frontend

# Restart a service
podman restart spending-analytics-backend

# Rebuild after code changes
podman compose up --build -d

# View running containers
podman ps

# View volumes
podman volume ls

# Database backup
podman exec spending-analytics-backend cat /app/data/spending.db > backup_$(date +%Y%m%d).db

# Database restore
cat backup.db | podman exec -i spending-analytics-backend sh -c 'cat > /app/data/spending.db'
podman restart spending-analytics-backend

# SQL query
podman exec spending-analytics-backend sqlite3 /app/data/spending.db "SELECT COUNT(*) FROM transactions;"
```

## Future Enhancements

- [ ] Multi-bank support (detect various Excel formats)
- [ ] CSV import with column mapping UI
- [ ] Export reports (PDF/Excel)
- [ ] Mobile-responsive design
- [ ] Transaction attachments (receipts)
- [ ] Advanced ML insights
- [ ] Multi-currency support
- [ ] OpenAI integration for AI analysis

## License

MIT

## Author

Built for personal spending analytics and financial awareness.

