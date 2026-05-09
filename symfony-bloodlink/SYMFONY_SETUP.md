# BloodLink Symfony Backend - Setup Guide

This document provides a quick start guide for setting up the Symfony backend for BloodLink.

## Prerequisites

- PHP 8.1 or higher
- Composer
- PostgreSQL (or access to Supabase PostgreSQL)
- Git

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd BloodLink/symfony-bloodlink
```

### 2. Install Dependencies

```bash
composer install
```

### 3. Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` and fill in your database connection string:
```
DATABASE_URL="postgresql://postgres:your_password@aws-1-eu-west-1.pooler.supabase.com:5432/postgres?sslmode=require"
```

### 4. Verify Database Connection

Test the database connection:
```bash
php bin/console doctrine:query:sql "SELECT 1"
```

### 5. Verify Entity Mappings

Check that all entities are properly mapped:
```bash
php bin/console doctrine:mapping:info
```

Expected output: All 14 entities should show `[OK]`

### 6. Clear Cache (Optional but Recommended)

```bash
php bin/console cache:clear
```

## Running the Application

### Development Server

Start the built-in PHP server:
```bash
symfony server:start
```

Or use the standard PHP built-in server:
```bash
php -S 127.0.0.1:8000 -t public
```

The API will be available at `http://localhost:8000`

## Project Structure

```
symfony-bloodlink/
├── src/
│   ├── Controller/Api/          # API endpoints
│   │   ├── HospitalController.php
│   │   ├── DonorController.php
│   │   ├── BloodInventoryController.php
│   │   ├── BloodTransferRequestController.php
│   │   ├── AlertController.php
│   │   └── DonationController.php
│   ├── Service/                 # Business logic layer
│   │   ├── HospitalService.php
│   │   ├── DonorService.php
│   │   ├── BloodInventoryService.php
│   │   ├── BloodTransferService.php
│   │   ├── AlertService.php
│   │   ├── DonationService.php
│   │   └── EligibilityService.php
│   ├── Entity/                  # Doctrine entities
│   ├── Repository/              # Doctrine repositories
│   ├── DTO/                     # Data Transfer Objects
│   ├── Util/                    # Utility classes
│   │   ├── ResponseUtil.php
│   │   ├── LocationUtil.php
│   │   └── BloodCompatibilityUtil.php
│   └── Doctrine/Types/          # Custom DBAL types
├── config/
│   ├── packages/
│   │   └── doctrine.yaml        # Doctrine configuration
│   └── routes.yaml              # Route configuration
├── public/
│   └── index.php                # Application entry point
└── .env.example                 # Environment template
```

## API Endpoints

All endpoints are prefixed with `/api/`.

### Hospital Management
- `GET /api/hospitals` - List all hospitals
- `GET /api/hospitals/{hospitalId}` - Get hospital details
- `POST /api/hospitals` - Create new hospital
- `PUT /api/hospitals/{hospitalId}` - Update hospital
- `DELETE /api/hospitals/{hospitalId}` - Delete hospital
- `GET /api/hospitals/nearby?latitude=X&longitude=Y&radius_km=50` - Find nearby hospitals

### Donor Management
- `GET /api/donors` - List all donors
- `GET /api/donors/{donorId}` - Get donor details
- `POST /api/donors` - Create new donor
- `PUT /api/donors/{donorId}` - Update donor
- `GET /api/donors/{donorId}/eligibility` - Get donor eligibility status
- `GET /api/donors/search/nearby?blood_type=O+&latitude=X&longitude=Y` - Find nearby eligible donors

### Blood Inventory
- `GET /api/inventory/hospital/{hospitalId}` - Get hospital inventory
- `GET /api/inventory/hospital/{hospitalId}/{bloodType}` - Get stock level
- `POST /api/inventory/add` - Add blood to inventory
- `POST /api/inventory/remove` - Remove blood from inventory
- `GET /api/inventory/alerts/hospital/{hospitalId}` - Get low stock alerts

### Blood Transfer Requests
- `GET /api/transfers` - List pending transfers
- `GET /api/transfers/hospital/{hospitalId}` - Get hospital transfers
- `POST /api/transfers` - Create new transfer request
- `POST /api/transfers/{transferId}/approve` - Approve transfer
- `POST /api/transfers/{transferId}/reject` - Reject transfer
- `POST /api/transfers/{transferId}/cancel` - Cancel transfer

### Alerts
- `GET /api/alerts` - List active alerts
- `GET /api/alerts/hospital/{hospitalId}` - Get hospital alerts
- `POST /api/alerts` - Create new alert
- `POST /api/alerts/{alertId}/send` - Send alert to donors
- `POST /api/alerts/{alertId}/close` - Close alert

### Donations
- `POST /api/donations` - Record new donation
- `GET /api/donations/{donationId}` - Get donation details
- `GET /api/donations/donor/{donorId}` - Get donor's donations
- `POST /api/donations/{donationId}/complete` - Complete donation
- `GET /api/donations/stats/hospital/{hospitalId}` - Get hospital donation stats

## Development Guidelines

### Code Structure

1. **Controllers**: Handle HTTP requests/responses, validation, and delegation to services
2. **Services**: Implement business logic, orchestrate repository operations
3. **Repositories**: Query and persist entities via Doctrine ORM
4. **Entities**: Represent database tables with proper mappings
5. **DTOs**: Transfer data between layers (optional but recommended)
6. **Utils**: Reusable utility functions (distance calc, blood compatibility, etc.)

### Adding New Features

1. Create a new entity in `src/Entity/` if needed
2. Generate/create repository in `src/Repository/`
3. Create service(s) in `src/Service/` for business logic
4. Create controller in `src/Controller/Api/` for HTTP endpoints
5. Add DTOs if needed in `src/DTO/`
6. Write tests

### Database Migrations

Currently, the project uses existing database schema. If you modify entities:

1. Validate mapping: `php bin/console doctrine:mapping:info`
2. Check for issues: `php bin/console doctrine:schema:validate --skip-sync`
3. Generate migration: `php bin/console make:migration`
4. Review the migration file
5. Execute: `php bin/console doctrine:migrations:migrate`

## Troubleshooting

### Database Connection Issues

1. Verify `.env` DATABASE_URL is correct
2. Check network access to database host
3. Ensure SSL mode is correct (`?sslmode=require` for Supabase)

### Entity Mapping Errors

1. Run: `php bin/console doctrine:mapping:info`
2. Check entity annotations and column types
3. Verify primary key types match database

### Port Already in Use

If port 8000 is already in use:
```bash
php -S 127.0.0.1:8001 -t public
```

## Next Steps for Developers

1. **Implement Services**: Fill in TODO comments in service classes
2. **Implement Controllers**: Complete endpoint logic
3. **Add Validation**: Implement request validation and error handling
4. **Add Tests**: Write unit and integration tests
5. **Add Authentication**: Implement JWT-based authentication (optional)
6. **Add Documentation**: Expand API documentation with examples

## Resources

- [Symfony Documentation](https://symfony.com/doc/)
- [Doctrine ORM](https://www.doctrine-project.org/)
- [Supabase PostgreSQL](https://supabase.com/)

## Support

For questions or issues, please refer to the project documentation or contact the development team.
