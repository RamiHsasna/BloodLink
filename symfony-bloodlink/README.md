# BloodLink Symfony Backend

A modern, scalable REST API backend for the BloodLink blood donation platform built with Symfony and PostgreSQL.

## 🎯 Overview

BloodLink is a comprehensive blood donation management system that connects hospitals, blood banks, and donors. This Symfony backend provides:

- **Hospital Management**: Register and manage blood banks and hospitals
- **Donor Management**: Track donors, eligibility status, and donation history
- **Blood Inventory**: Monitor blood stock levels across facilities
- **Blood Transfer Requests**: Manage inter-hospital blood transfers
- **Alert System**: Send urgent blood donation requests to eligible nearby donors
- **Donation Tracking**: Record and track all blood donations with screening results
- **Location-Based Services**: Find nearby hospitals and eligible donors using geolocation

## 🚀 Quick Start

### Prerequisites

- PHP 8.1 or higher
- Composer
- PostgreSQL (or Supabase)
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BloodLink/symfony-bloodlink
   ```

2. **Install dependencies**
   ```bash
   composer install
   ```

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and add your DATABASE_URL
   ```

4. **Verify setup**
   ```bash
   php bin/console doctrine:query:sql "SELECT 1"
   php bin/console doctrine:mapping:info
   ```

5. **Start the server**
   ```bash
   php -S 127.0.0.1:8000 -t public
   ```

The API will be available at `http://localhost:8000/api`

## 📁 Project Structure

```
symfony-bloodlink/
├── src/
│   ├── Controller/Api/           # REST API endpoints
│   │   ├── HospitalController.php
│   │   ├── DonorController.php
│   │   ├── BloodInventoryController.php
│   │   ├── BloodTransferRequestController.php
│   │   ├── AlertController.php
│   │   └── DonationController.php
│   ├── Service/                  # Business logic layer
│   │   ├── HospitalService.php
│   │   ├── DonorService.php
│   │   ├── BloodInventoryService.php
│   │   ├── BloodTransferService.php
│   │   ├── AlertService.php
│   │   ├── DonationService.php
│   │   └── EligibilityService.php
│   ├── Entity/                   # Doctrine ORM entities
│   ├── Repository/               # Data access layer
│   ├── DTO/                      # Data transfer objects
│   ├── Util/                     # Utility functions
│   │   ├── ResponseUtil.php      # Standardized API responses
│   │   ├── LocationUtil.php      # Distance calculations
│   │   └── BloodCompatibilityUtil.php
│   └── Doctrine/Types/           # Custom DBAL types
├── config/
│   └── packages/
│       └── doctrine.yaml         # Database configuration
├── public/
│   └── index.php                 # Application entry point
├── .env.example                  # Environment template
├── SYMFONY_SETUP.md              # Setup guide
├── DEVELOPMENT_GUIDE.md          # Developer guidelines
└── API_DOCUMENTATION.md          # API endpoint documentation
```

## 🏗️ Architecture

### Layered Architecture

```
HTTP Request
    ↓
Controller (Request validation, delegation)
    ↓
Service (Business logic, orchestration)
    ↓
Repository (Data queries)
    ↓
Database (PostgreSQL)
```

### Key Components

- **Controllers**: Handle HTTP requests/responses, validate input
- **Services**: Implement business logic, coordinate repositories
- **Repositories**: Query and persist entities
- **Entities**: Domain models representing database tables
- **Utilities**: Reusable functions (distance calc, blood compatibility)

## 🔌 API Endpoints

### Core Resources

**Hospitals**
- `GET /api/hospitals` - List all hospitals
- `POST /api/hospitals` - Create hospital
- `GET /api/hospitals/{id}` - Get hospital details
- `PUT /api/hospitals/{id}` - Update hospital
- `DELETE /api/hospitals/{id}` - Delete hospital
- `GET /api/hospitals/nearby?latitude=X&longitude=Y` - Find nearby

**Donors**
- `GET /api/donors` - List donors
- `POST /api/donors` - Register donor
- `GET /api/donors/{id}` - Get donor details
- `GET /api/donors/{id}/eligibility` - Check eligibility
- `GET /api/donors/search/nearby` - Find nearby eligible donors

**Blood Inventory**
- `GET /api/inventory/hospital/{id}` - Get hospital inventory
- `POST /api/inventory/add` - Add blood units
- `POST /api/inventory/remove` - Remove blood units
- `GET /api/inventory/alerts/{id}` - Get low stock alerts

**Blood Transfers**
- `GET /api/transfers` - List pending transfers
- `POST /api/transfers` - Create transfer request
- `POST /api/transfers/{id}/approve` - Approve transfer
- `POST /api/transfers/{id}/reject` - Reject transfer

**Alerts**
- `GET /api/alerts` - List active alerts
- `POST /api/alerts` - Create blood request alert
- `POST /api/alerts/{id}/send` - Send to eligible donors

**Donations**
- `POST /api/donations` - Record donation
- `GET /api/donations/{id}` - Get donation details
- `POST /api/donations/{id}/complete` - Complete donation
- `GET /api/donations/stats/hospital/{id}` - Get statistics

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for complete endpoint details.

## 🛠️ Development

### Getting Started with Features

1. **Understand the database schema** - Review entities in `src/Entity/`
2. **Implement services** - Add business logic in `src/Service/`
3. **Create controllers** - Handle HTTP in `src/Controller/Api/`
4. **Write tests** - Ensure quality and reliability
5. **Update documentation** - Keep API docs current

### Finding What to Implement

Search for TODO comments:
```bash
grep -r "TODO:" src/
```

### Code Standards

- **PSR-12** - PHP coding standards
- **Dependency Injection** - Constructor-based injection
- **Doctrine ORM** - Entity mappings and relationships
- **RESTful APIs** - Standard HTTP methods and status codes

See [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) for detailed guidelines.

## 📊 Database Schema

The system uses 14 main entities:

- **Hospital** - Blood banks and hospitals
- **User** - User accounts
- **Donor** - Donor profiles
- **DonorEligibility** - Eligibility tracking with caching
- **BloodType** - Blood type definitions (O+, AB-, etc.)
- **Donation** - Recorded blood donations
- **DonationEvent** - Blood drive events
- **BloodInventory** - Stock tracking per hospital/blood type
- **BloodTransferRequest** - Inter-hospital transfers
- **BloodTransferRequestLog** - Transfer history
- **Alert** - Urgent blood requests
- **DonorAlert** - Individual donor notifications
- **DonationLog** - Donation event history
- **HospitalStaff** - Staff member accounts

All primary keys are UUIDs (PostgreSQL uuid type) for distributed systems support.

## 🔐 Security Notes

- Current implementation has no authentication (JWT planned)
- All decimal coordinates are stored as strings for precision
- UUID types are mapped via custom Doctrine types
- PostgreSQL array types handled via custom DBAL type

## 📋 Data Types

**Coordinates (latitude/longitude)**
- Stored as: `string` (e.g., "40.7128")
- Precision: up to 8 decimal places
- Used for location-based searches

**UUIDs**
- Stored as: `string`
- Format: Standard UUID v4
- All primary keys use UUIDs

**Dates**
- Stored as: `DateTimeInterface`
- Format: ISO 8601 with timezone

## 🗂️ Configuration

### Environment Variables

Key environment variables in `.env`:

```env
DATABASE_URL=postgresql://user:password@host:port/database?sslmode=require
APP_ENV=dev
APP_DEBUG=true
APP_SECRET=your-secret-key
```

See `.env.example` for all available options.

## 🧪 Testing

Run tests with:
```bash
php bin/console make:test
```

Ensure tests cover:
- Service business logic
- Controller request/response handling
- Repository queries
- Entity relationships

## 📚 Documentation

- **[SYMFONY_SETUP.md](SYMFONY_SETUP.md)** - Detailed setup instructions
- **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)** - Code standards and best practices
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Complete API reference

## 🚧 Current Status

**Implemented:**
- ✅ Database entities and relationships
- ✅ Repository layer (14 repositories)
- ✅ Service layer boilerplate with TODOs
- ✅ API controller boilerplate with TODOs
- ✅ Utility functions (ResponseUtil, LocationUtil, BloodCompatibilityUtil)
- ✅ Custom Doctrine types (UUID, TextArray)
- ✅ Documentation and guides

**In Progress:**
- Service implementation (marked with TODO)
- Controller endpoint implementation (marked with TODO)
- Request validation
- Error handling

**Planned:**
- JWT authentication
- Input validation framework
- Unit and integration tests
- API versioning
- Rate limiting
- Caching layer

## 👥 Contributing

When implementing features:

1. Follow the [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
2. Find TODOs and implement them
3. Add error handling and validation
4. Write tests for business logic
5. Update API documentation
6. Ensure code follows PSR-12

## 🔗 Related Projects

- **JavaFX Frontend**: `BloodLink/` (Java desktop application)
- **Database**: Supabase PostgreSQL

## 📞 Support

For questions:
1. Check the documentation files
2. Review TODO comments for guidance
3. Follow patterns in existing code
4. Contact the development team

## 📝 License

[Add your license information here]

## 🎓 Learning Resources

- [Symfony Documentation](https://symfony.com/doc/)
- [Doctrine ORM](https://www.doctrine-project.org/)
- [RESTful API Design](https://restfulapi.net/)
- [PostgreSQL](https://www.postgresql.org/docs/)

---

**Last Updated:** January 2024
**Version:** 1.0.0 (Boilerplate)