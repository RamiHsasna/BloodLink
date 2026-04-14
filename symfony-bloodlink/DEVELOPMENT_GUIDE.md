# BloodLink Development Guide

This guide provides development standards and best practices for the BloodLink Symfony project.

## Code Organization

### Service Layer Architecture

```
Request → Controller → Service(s) → Repository → Database
Response ← Controller ← Service(s)
```

Each layer has specific responsibilities:

#### Controllers (`src/Controller/Api/`)
- Accept HTTP requests
- Validate request data
- Delegate to services
- Format and return responses
- Handle HTTP status codes

#### Services (`src/Service/`)
- Implement business logic
- Coordinate between repositories
- Handle validation and error checking
- Manage transactions
- Should NOT know about HTTP

#### Repositories (`src/Repository/`)
- Query builders
- Entity queries
- Persistence operations
- Database-specific logic

#### Entities (`src/Entity/`)
- Represent database tables
- Define relationships
- Contain getters/setters
- NO business logic in entities

#### Utilities (`src/Util/`)
- Reusable functions
- Calculations (distance, compatibility)
- Helper methods
- No side effects

### Data Types

**Decimal Values**: Stored as `string` in PHP for precision
```php
// Database: numeric(10, 8)
#[ORM\Column(type: "decimal", precision: 10, scale: 8)]
private string|null $latitude = null;

// Access as string to preserve precision
$value = $donor->getLatitude(); // Returns "51.5074" as string
// Convert to float only if needed: (float)$value
```

**UUIDs**: Stored as `string` in PHP
```php
#[ORM\Id]
#[ORM\Column(type: "uuid")]
private string $hospitalId;
```

**Dates**: Use `\DateTimeInterface`
```php
#[ORM\Column(type: "datetime")]
private \DateTimeInterface $createdAt;
```

## Development Workflow

### 1. Understanding the Database Schema

Key entities in BloodLink:

- **Hospital**: Blood banks and hospitals
- **User**: Registered users (donors, staff)
- **Donor**: Donors with blood type and location
- **DonorEligibility**: Eligibility status and caching
- **BloodType**: Blood type codes (O+, AB-, etc.)
- **Donation**: Recorded donations
- **DonationEvent**: Blood drive events
- **BloodInventory**: Stock levels per hospital/blood type
- **BloodTransferRequest**: Inter-hospital blood transfers
- **Alert**: Urgent blood requests
- **DonorAlert**: Donor notifications of urgent requests

### 2. Implementing a New Feature

Example: Implementing the `HospitalController`

#### Step 1: Create/Update Entity
Verify entity exists: `src/Entity/Hospital.php` ✓

#### Step 2: Implement Service Layer

```php
// src/Service/HospitalService.php
class HospitalService {
    public function __construct(
        private readonly HospitalRepository $hospitalRepository,
        private readonly EntityManagerInterface $entityManager,
    ) {}

    public function getAllHospitals(): array {
        // Query using repository
        return $this->hospitalRepository->findAll();
    }

    public function getHospitalById(string $hospitalId): ?Hospital {
        return $this->hospitalRepository->find($hospitalId);
    }

    public function createHospital(array $data): Hospital {
        // Validate data
        // Create entity
        // Persist
        // Return
    }
}
```

#### Step 3: Implement Controller

```php
// src/Controller/Api/HospitalController.php
#[Route('/api/hospitals')]
class HospitalController extends AbstractController {
    public function __construct(
        private readonly HospitalService $hospitalService,
    ) {}

    #[Route('', methods: ['GET'])]
    public function listHospitals(): JsonResponse {
        $hospitals = $this->hospitalService->getAllHospitals();
        return new JsonResponse(ResponseUtil::success($hospitals));
    }

    #[Route('/{hospitalId}', methods: ['POST'])]
    public function createHospital(Request $request): JsonResponse {
        $data = $request->getPayload()->all();
        
        // Validate
        // Call service
        $hospital = $this->hospitalService->createHospital($data);
        
        return new JsonResponse(
            ResponseUtil::success($hospital),
            201
        );
    }
}
```

#### Step 4: Test

Create tests to verify functionality.

### 3. Key Utilities

#### LocationUtil - Distance Calculations

```php
use App\Util\LocationUtil;

// Calculate distance between two points
$distanceKm = LocationUtil::calculateDistance(
    latitude1: 51.5074,
    longitude1: -0.1278,
    latitude2: 48.8566,
    longitude2: 2.3522,
);

// Find nearby donors
$nearbyDonors = LocationUtil::findDonorsWithinRadius(
    hospitalLatitude: 51.5074,
    hospitalLongitude: -0.1278,
    donors: $donorList,
    radiusKm: 50,
);
```

#### BloodCompatibilityUtil - Blood Type Checking

```php
use App\Util\BloodCompatibilityUtil;

// Check if donation is compatible
$isCompatible = BloodCompatibilityUtil::isCompatible(
    donorBloodType: 'O+',
    recipientBloodType: 'AB-',
);

// Get compatible types
$compatibleTypes = BloodCompatibilityUtil::getCompatibleDonorTypes('AB-');
// Returns: ['O+', 'O-', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-']
```

#### ResponseUtil - Standardized Responses

```php
use App\Util\ResponseUtil;

// Success response
return new JsonResponse(
    ResponseUtil::success(
        data: $data,
        message: 'Hospital created successfully'
    ),
    201
);

// Error response
return new JsonResponse(
    ResponseUtil::error(
        message: 'Hospital not found',
        errors: null,
        statusCode: 404
    ),
    404
);

// Paginated response
return new JsonResponse(
    ResponseUtil::paginated(
        items: $hospitals,
        page: 1,
        perPage: 10,
        total: 150,
    )
);
```

## Best Practices

### 1. Dependency Injection

Always use constructor injection:

```php
// Good
public function __construct(
    private readonly HospitalRepository $repository,
    private readonly EntityManagerInterface $entityManager,
) {}

// Bad
$repository = new HospitalRepository();
```

### 2. Error Handling

```php
public function getHospital(string $hospitalId): Hospital {
    $hospital = $this->repository->find($hospitalId);
    
    if (!$hospital) {
        throw new \RuntimeException("Hospital not found");
    }
    
    return $hospital;
}
```

### 3. Data Validation

In controller:
```php
$data = $request->getPayload()->all();

if (empty($data['name'])) {
    throw new \InvalidArgumentException("Name is required");
}
```

### 4. Transactions

For operations spanning multiple entities:

```php
$this->entityManager->beginTransaction();

try {
    $this->donationService->recordDonation($donor);
    $this->inventoryService->addToInventory($hospital, $bloodType);
    $this->entityManager->commit();
} catch (\Exception $e) {
    $this->entityManager->rollback();
    throw $e;
}
```

### 5. Logging

```php
use Psr\Log\LoggerInterface;

class MyService {
    public function __construct(private readonly LoggerInterface $logger) {}
    
    public function importantOperation() {
        $this->logger->info('Starting important operation');
        
        try {
            // Do work
            $this->logger->info('Operation completed successfully');
        } catch (\Exception $e) {
            $this->logger->error('Operation failed: ' . $e->getMessage());
            throw $e;
        }
    }
}
```

## TODO Pattern

The boilerplate uses TODO comments to indicate where implementation is needed:

```php
/**
 * TODO: Implement functionality
 * 1. Step one
 * 2. Step two
 * 3. Step three
 */
public function methodName() {
    // Implementation goes here
}
```

Search for TODOs to find what needs to be implemented:
```bash
grep -r "TODO:" src/
```

## Common Patterns

### Filtering and Pagination

```php
public function listDonors(Request $request): JsonResponse {
    $page = (int)$request->query->get('page', 1);
    $perPage = (int)$request->query->get('per_page', 10);
    $bloodType = $request->query->get('blood_type');
    
    $query = $this->donorRepository->createQueryBuilder('d');
    
    if ($bloodType) {
        $query->andWhere('d.bloodType = :bloodType')
              ->setParameter('bloodType', $bloodType);
    }
    
    $total = count($query->getQuery()->getResult());
    $donors = $query->setFirstResult(($page - 1) * $perPage)
                    ->setMaxResults($perPage)
                    ->getQuery()
                    ->getResult();
    
    return new JsonResponse(
        ResponseUtil::paginated($donors, $page, $perPage, $total)
    );
}
```

### Searching by Location

```php
public function findNearbyHospitals(Request $request): JsonResponse {
    $latitude = (float)$request->query->get('latitude');
    $longitude = (float)$request->query->get('longitude');
    $radius = (float)$request->query->get('radius_km', 50);
    
    $allHospitals = $this->hospitalRepository->findAll();
    
    $nearby = LocationUtil::findDonorsWithinRadius(
        $latitude,
        $longitude,
        $allHospitals,
        $radius
    );
    
    return new JsonResponse(ResponseUtil::success($nearby));
}
```

## Debugging Tips

### Enable SQL Logging

In `.env`:
```
DOCTRINE_LOGGER_LEVEL=debug
```

### Debug Doctrine Queries

```bash
php bin/console doctrine:query:sql "SELECT * FROM hospital LIMIT 5"
```

### Check Entity Mappings

```bash
php bin/console doctrine:mapping:info
```

### Validate Schema

```bash
php bin/console doctrine:schema:validate --skip-sync
```

## Performance Considerations

1. **Lazy Loading**: Entities load related data on access. Consider eager loading for bulk operations:
   ```php
   ->leftJoin('h.donations', 'd')
   ->addSelect('d')
   ```

2. **N+1 Queries**: Avoid loading related entities in loops. Use joins instead.

3. **Caching**: Doctrine provides result caching. Use for frequently accessed data.

4. **Indexing**: Ensure database has indexes on frequently queried columns.

## Testing

Write tests for critical business logic:

```php
// tests/Service/DonorServiceTest.php
class DonorServiceTest extends TestCase {
    public function testFindEligibleDonors() {
        // Arrange
        $donors = [...];
        
        // Act
        $eligible = $this->service->findEligibleDonorsByBloodType('O+');
        
        // Assert
        $this->assertCount(2, $eligible);
    }
}
```

## Contributing

When contributing:

1. Follow this guide
2. Implement all TODOs for your feature
3. Add appropriate error handling
4. Write tests
5. Update API documentation
6. Submit PR with clear description

## Questions?

Refer to the SYMFONY_SETUP.md for setup questions, or contact the development team.