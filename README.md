# BloodLink - Integrated Blood Bank Management System

_A comprehensive blood bank management platform built with Symfony,JavaFX and PostgreSQL, developed as part of PIDEV (Integrated Development Project) at ESPRIT._

---

## Overview

BloodLink is a full-stack blood bank management system developed as part of the PIDEV 3A coursework at **ESPRIT - Ecole Supérieure Privée d'Ingénierie et de Technologie**. This project explores modern enterprise application development with a focus on multi-tier architecture, database design, real-time operations, and role-based access control.

The platform addresses critical challenges in healthcare blood banking, including inventory management, donor coordination, inter-hospital blood transfers, and complete operational traceability through advanced audit logging and AI-powered anomaly detection.

---

## Features

### Core Blood Banking Operations

**Hospital Management**

- Maintain centralized registry of participating hospitals with geographic location data
- Support multi-hospital network coordination and queries
- Track hospital operational status and staffing
- Enable hospital-specific data filtering and authorization

**Donor Management**

- Create and maintain comprehensive donor profiles with demographic information
- Track donor blood types, contact details, and availability
- Support location-based donor search and identification
- Maintain complete donor participation history and engagement metrics

**Donor Eligibility Assessment**

- Automated eligibility determination based on health criteria and regulatory standards
- Generate detailed eligibility reports with acceptance or deferral reasoning
- Track eligibility status changes and historical records
- Enable targeted outreach to eligible donor populations
- Maintain compliance documentation for audits

**Blood Inventory Management**

- Real-time blood stock monitoring by type across hospital network
- Track inventory additions (donations received) and reductions (transfusions/usage)
- Automated critical stock level alerts and notifications
- Hospital-specific inventory dashboards and status tracking
- Prevent blood shortages through proactive monitoring

**Blood Transfer Workflow**

- Structured inter-hospital blood transfer request management
- Automated validation of blood type compatibility and source availability
- Multi-step approval workflow (pending, approved, rejected, completed)
- Distance calculations for transfer route optimization
- Transfer history tracking and status transition logging

**Donation Management**

- Record individual blood donations with donor identification and type
- Associate donations with organized donation events (blood drives, campaigns)
- Track donation lifecycle status and special conditions
- Link donations to subsequent transfer or usage operations
- Maintain complete donation history

**Donation Events Coordination**

- Organize blood collection campaigns and donation drives
- Register donors for specific events
- Track multi-hospital participation in coordinated events
- Monitor collection goals versus actual results

### Alert & Notification System

**Multi-Channel Alerting**

- Send critical blood need alerts to donor population
- Notify hospital staff of inventory problems
- Support system notifications, SMS (via Twilio), and email delivery
- Track alert delivery status and recipient responses
- Urgent SMS notifications for critical anomalies

### Audit & Compliance

**Complete Audit Trail**

- Log all donation status transitions with timestamp and responsible user
- Record blood transfer request status changes with decision tracking
- Generate cryptographic QR codes for audit record verification
- Maintain chronological records for regulatory compliance
- Store notes and special conditions with each operation

**AI-Powered Anomaly Detection**

- Detect suspicious donation patterns (unusual frequency, volume, timing)
- Identify irregular transfer patterns indicating problems
- Flag high-risk activities for human review
- Send immediate SMS alerts for critical anomalies
- Support fraud detection and quality assurance

**Donor-Facing Transparency**

- Allow donors to view their complete personal donation history
- Display information about blood transfers and usage
- Provide accountability and transparency into blood journey

### User & Security Management

**Role-Based Access Control**

- Support multiple user types (Administrator, Hospital Staff, Donor)
- Enforce granular permissions by role
- Implement secure session management and authentication
- Enable hospital-specific data segregation in multi-hospital environment
- Support secure logout and session termination

### Analytics & Reporting

**Operational Insights**

- Generate blood inventory statistics by hospital and type
- Calculate donor participation metrics and trends
- Monitor donation frequency and volume patterns
- Track blood transfer success rates and efficiency
- Report critical alert frequency and response times
- Provide time-series data for trend analysis and forecasting

### Blood Type Safety

**Compatibility Management**

- Validate ABO blood group compatibility
- Verify Rh factor compatibility (positive/negative)
- Prevent incompatible blood transfers automatically
- Support universal donor/recipient identification
- Enable medical decision support through compatibility lookup

---

## Tech Stack

### Backend

- **Framework**: Symfony 6.4+ (PHP 8.1+)
- **Database**: PostgreSQL 15 / Supabase (cloud database)
- **ORM**: Doctrine 2.x with Reverse Engineering support
- **Authentication**: JWT-based (LexikJWT Bundle)
- **API Architecture**: RESTful services with standardized response formatting

### Frontend

- **Template Engine**: Twig
- **UI Framework**: Bootstrap 5 with responsive design
- **JavaScript**: Stimulus framework for interactivity
- **Asset Pipeline**: Symfony AssetMapper
- **Desktop Application**: JavaFX (for dedicated user management interface)

### External Integrations

- **SMS Notifications**: Twilio API for urgent alerts
- **AI Services**: Anthropic Claude API for anomaly detection
- **Email**: Symfony Mailer with Gmail/SMTP support
- **PDF Generation**: KnpSnappy (wkhtmltopdf)
- **AI Infrastructure**: NVIDIA API for supporting services

### Development & Deployment

- **Web Server**: PHP built-in server / Apache
- **Database Migrations**: Doctrine Migrations
- **Monitoring**: Monolog with debug logging
- **Message Queue**: Symfony Messenger with Doctrine transport
- **Caching**: Redis-compatible architecture

---

## Directory Structure

```
BloodLink/
├── symfony-bloodlink/              # Main Symfony web application
│   ├── src/
│   │   ├── Controller/             # Web & API controllers
│   │   ├── Service/                # Business logic layer (18+ services)
│   │   ├── Repository/             # Database query abstraction
│   │   ├── Entity/                 # Domain model entities (15+ entities)
│   │   ├── Form/                   # Symfony form definitions
│   │   ├── EventSubscriber/        # Event listeners
│   │   ├── Util/                   # Utility classes (location, compatibility)
│   │   └── Doctrine/               # Custom Doctrine types
│   │
│   ├── templates/                  # Twig template files
│   │   ├── auth/                   # Authentication pages
│   │   ├── dashboard/              # Admin dashboard layouts
│   │   ├── log_management/         # Audit trail pages
│   │   └── shared/                 # Reusable components
│   │
│   ├── migrations/                 # Database migrations
│   ├── config/                     # Configuration files
│   ├── public/                     # Web root
│   ├── tests/                      # PHPUnit test suite
│   ├── docs/                       # Technical documentation
│   └── composer.json               # PHP dependencies
│
├── src/tn/edu/esprit/              # JavaFX desktop application
│   ├── entities/                   # Domain entities
│   ├── services/                   # Business logic
│   ├── gui/                        # JavaFX user interface
│   └── Tools/                      # Database connectivity
│
└── docs/                           # Project documentation
    ├── API_DOCUMENTATION.md
    ├── DEVELOPMENT_GUIDE.md
    ├── logmanagement-module-spec.md
    └── ...
```

---

## Getting Started

### Prerequisites

- PHP 8.1 or higher
- PostgreSQL 15+ or Supabase PostgreSQL database
- Composer (PHP package manager)
- Java 8+ (for JavaFX desktop application, optional)

### Installation Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/RamiHsasna/BloodLink.git
   cd BloodLink/symfony-bloodlink
   ```

2. **Install PHP dependencies**

   ```bash
   composer install
   ```

3. **Configure environment**

   ```bash
   cp .env.example .env
   # Update DATABASE_URL in .env with your PostgreSQL/Supabase credentials
   ```

4. **Verify database connectivity**

   ```bash
   php bin/console doctrine:query:sql "SELECT 1"
   php bin/console doctrine:mapping:info
   ```

5. **Run the application**

   ```bash
   php -S 127.0.0.1:8000 -t public
   ```

6. **Access the platform**
   - Web: `http://127.0.0.1:8000`
   - Authentication: `/auth` endpoint
   - Admin Dashboard: `/dashboard`

### Running the JavaFX Desktop Application

The project includes a dedicated JavaFX application for user management:

```bash
cd ../src
# Ensure JavaFX SDK is configured in your IDE
# Run tn.edu.esprit.gui.UserManagementApp
```

---

## Project Documentation

Comprehensive documentation is available in the `docs/` directory:

- **API_DOCUMENTATION.md** - Complete REST API endpoint reference
- **DEVELOPMENT_GUIDE.md** - Code standards and architecture patterns
- **SYMFONY_SETUP.md** - Detailed setup and configuration guide
- **SUPABASE_INTEGRATION.md** - Cloud database integration
- **logmanagement-module-spec.md** - Audit logs module specification
- **reverse-engineering.php** - Database-first entity generation proof

---

## Key Technologies & Keywords

**Core Technologies**: Symfony 6.4, PHP 8.1, PostgreSQL 15, Doctrine ORM, Twig, Bootstrap 5, JavaScript ES6, JavaFX

**Architectural Patterns**: Layered Architecture, Service Layer Pattern, Repository Pattern, Role-Based Access Control (RBAC), RESTful API Design

**Features**: Real-time Inventory Management, Geographic Location Queries, Blood Type Compatibility Validation, AI Anomaly Detection, JWT Authentication, Multi-Hospital Coordination, Audit Trail & Compliance

**Healthcare Domain**: Blood Bank Management, Donor Coordination, Blood Inventory, Inter-Hospital Transfers, Eligibility Assessment, Regulatory Compliance, Medical Traceability

**Development Practices**: Database-First Design (Reverse Engineering), Clean Code, Design Patterns, Test-Driven Development, Git Workflow

---

## Acknowledgments

This project was developed as part of the **PIDEV (Integrated Development Project) - 3A** curriculum at **[ESPRIT - Ecole Supérieure Privée d'Ingénierie et de Technologie](https://www.esprit.tn)**.

**Project Lead**: Rami Hsasna  
**Contributors**: Development Team  
**Academic Supervision**: ESPRIT Faculty

The project implements real-world enterprise application development practices with emphasis on modern web technologies, database design, security, and healthcare domain expertise.

---

## License

This project is created for educational purposes as part of ESPRIT's PIDEV program.

---

**Last Updated**: May 12, 2026  
**Repository**: [RamiHsasna/BloodLink](https://github.com/RamiHsasna/BloodLink)

The generated entity layer was then repaired only where needed for Symfony correctness and module delivery.

## Verification Commands

```bash
php bin/console doctrine:mapping:info
php bin/console lint:twig templates
php bin/console app:reverse-engineer:entities --help
```

## Notes

- `Audit Logs` and `Alerts` are separate workspaces.
- `DONOR` users stay in Front Office only.
- `HOSPITAL_STAFF` is scoped to the staff hospital.
- If a local helper script is ever needed from this workspace, use `py`.
