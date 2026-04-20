# BloodLink API Documentation

This document provides comprehensive documentation for the BloodLink REST API endpoints.

## Project note

The Symfony delivery keeps `Audit Logs` and `Alerts` as separate workspaces in the UI. Audit logs cover donation and transfer traceability, while alerts keep the donor communication flow separate. Donor-facing audit history appears as `My Audit Trail`.

## Base URL

```
http://localhost:8000/api
```

## Authentication

Currently, the API does not require authentication. JWT-based authentication will be added in future versions.

## Response Format

All API responses follow a standardized format:

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "errors": null,
  "statusCode": 400
}
```

### Paginated Response
```json
{
  "success": true,
  "data": [],
  "pagination": {
    "currentPage": 1,
    "perPage": 10,
    "totalItems": 100,
    "totalPages": 10
  }
}
```

## HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `204 No Content` - Successful deletion
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Endpoints

### Hospitals

#### List All Hospitals
```
GET /hospitals?page=1&per_page=10
```

**Query Parameters:**
- `page` (optional): Page number (default: 1)
- `per_page` (optional): Items per page (default: 10)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "hospitalId": "uuid",
      "name": "Central Hospital",
      "address": "123 Main St",
      "city": "New York",
      "postalCode": "10001",
      "latitude": "40.7128",
      "longitude": "-74.0060",
      "phone": "+1-555-0100",
      "email": "contact@centralhospital.com",
      "isActive": true,
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "perPage": 10,
    "totalItems": 50,
    "totalPages": 5
  }
}
```

#### Get Hospital by ID
```
GET /hospitals/{hospitalId}
```

**Path Parameters:**
- `hospitalId` (required): Hospital UUID

**Response:**
```json
{
  "success": true,
  "data": {
    "hospitalId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Central Hospital",
    "address": "123 Main St",
    "city": "New York",
    "postalCode": "10001",
    "latitude": "40.7128",
    "longitude": "-74.0060",
    "phone": "+1-555-0100",
    "email": "contact@centralhospital.com",
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

#### Create Hospital
```
POST /hospitals
```

**Request Body:**
```json
{
  "name": "New Hospital",
  "address": "456 Oak Avenue",
  "city": "Boston",
  "postalCode": "02101",
  "latitude": "42.3601",
  "longitude": "-71.0589",
  "phone": "+1-555-0200",
  "email": "contact@newhospital.com"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Hospital created successfully",
  "data": {
    "hospitalId": "550e8400-e29b-41d4-a716-446655440001",
    "name": "New Hospital",
    "address": "456 Oak Avenue",
    "city": "Boston",
    "postalCode": "02101",
    "latitude": "42.3601",
    "longitude": "-71.0589",
    "phone": "+1-555-0200",
    "email": "contact@newhospital.com",
    "isActive": true,
    "createdAt": "2024-01-16T10:30:00Z",
    "updatedAt": "2024-01-16T10:30:00Z"
  }
}
```

#### Update Hospital
```
PUT /hospitals/{hospitalId}
PATCH /hospitals/{hospitalId}
```

**Path Parameters:**
- `hospitalId` (required): Hospital UUID

**Request Body:** (any fields to update)
```json
{
  "name": "Updated Hospital Name",
  "phone": "+1-555-0300"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "hospitalId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Updated Hospital Name",
    "phone": "+1-555-0300"
  }
}
```

#### Delete Hospital
```
DELETE /hospitals/{hospitalId}
```

**Response:** `204 No Content`

#### Find Nearby Hospitals
```
GET /hospitals/nearby?latitude=40.7128&longitude=-74.0060&radius_km=50
```

**Query Parameters:**
- `latitude` (required): Hospital latitude
- `longitude` (required): Hospital longitude
- `radius_km` (optional): Search radius in kilometers (default: 50)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "hospitalId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Central Hospital",
      "city": "New York",
      "latitude": "40.7128",
      "longitude": "-74.0060",
      "distance_km": 5.2
    }
  ]
}
```

#### Get Hospital Inventory
```
GET /hospitals/{hospitalId}/inventory
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "bloodType": "O+",
      "units": 25,
      "lastUpdated": "2024-01-16T08:00:00Z"
    },
    {
      "bloodType": "O-",
      "units": 12,
      "lastUpdated": "2024-01-16T08:00:00Z"
    }
  ]
}
```

### Donors

#### List All Donors
```
GET /donors?page=1&per_page=10&blood_type=O+&eligible=true
```

**Query Parameters:**
- `page` (optional): Page number
- `per_page` (optional): Items per page
- `blood_type` (optional): Filter by blood type
- `eligible` (optional): Filter by eligibility status

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "userId": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "bloodType": "O+",
      "latitude": "40.7128",
      "longitude": "-74.0060",
      "city": "New York",
      "lastDonationDate": "2024-01-10",
      "totalDonations": 5,
      "isCurrentlyEligible": true,
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "perPage": 10,
    "totalItems": 250,
    "totalPages": 25
  }
}
```

#### Get Donor by ID
```
GET /donors/{donorId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "John",
    "lastName": "Doe",
    "bloodType": "O+",
    "latitude": "40.7128",
    "longitude": "-74.0060",
    "city": "New York",
    "lastDonationDate": "2024-01-10",
    "totalDonations": 5,
    "isCurrentlyEligible": true,
    "createdAt": "2024-01-01T10:00:00Z"
  }
}
```

#### Create Donor
```
POST /donors
```

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "bloodType": "A+",
  "latitude": "40.7500",
  "longitude": "-74.0100",
  "city": "New York"
}
```

**Response:** `201 Created`

#### Update Donor
```
PUT /donors/{donorId}
PATCH /donors/{donorId}
```

**Request Body:**
```json
{
  "city": "Boston",
  "latitude": "42.3601",
  "longitude": "-71.0589"
}
```

#### Get Donor Eligibility
```
GET /donors/{donorId}/eligibility
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isCurrentlyEligible": true,
    "daysUntilEligible": 0,
    "lastCalculatedAt": "2024-01-16T10:00:00Z",
    "reason": "Donor is eligible to donate",
    "nextEligibleDate": null
  }
}
```

#### Find Eligible Donors by Blood Type
```
GET /donors/search/by-blood-type?blood_type=O+&page=1&per_page=10
```

**Query Parameters:**
- `blood_type` (required): Blood type code
- `page` (optional): Page number
- `per_page` (optional): Items per page

#### Find Nearby Eligible Donors
```
GET /donors/search/nearby?blood_type=O+&latitude=40.7128&longitude=-74.0060&radius_km=50
```

**Query Parameters:**
- `blood_type` (required): Blood type required
- `latitude` (required): Search center latitude
- `longitude` (required): Search center longitude
- `radius_km` (optional): Search radius in kilometers (default: 50)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "userId": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "bloodType": "O+",
      "latitude": "40.7200",
      "longitude": "-74.0050",
      "distance_km": 1.2,
      "isCurrentlyEligible": true
    }
  ]
}
```

#### Check Donor Eligibility
```
POST /donors/{donorId}/check-eligibility
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isCurrentlyEligible": true,
    "daysUntilEligible": 0,
    "reason": "Donor is eligible to donate"
  }
}
```

### Blood Inventory

#### Get Hospital Inventory
```
GET /inventory/hospital/{hospitalId}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "bloodType": "O+",
      "units": 25,
      "status": "active",
      "lastUpdated": "2024-01-16T08:00:00Z"
    },
    {
      "bloodType": "A+",
      "units": 18,
      "status": "active",
      "lastUpdated": "2024-01-16T08:00:00Z"
    }
  ]
}
```

#### Get Stock Level
```
GET /inventory/hospital/{hospitalId}/{bloodType}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "bloodType": "O+",
    "units": 25,
    "status": "active",
    "lastUpdated": "2024-01-16T08:00:00Z"
  }
}
```

#### Add Blood to Inventory
```
POST /inventory/add
```

**Request Body:**
```json
{
  "hospital_id": "550e8400-e29b-41d4-a716-446655440000",
  "blood_type": "O+",
  "units": 5
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "bloodType": "O+",
    "units": 30,
    "status": "active",
    "lastUpdated": "2024-01-16T12:00:00Z"
  }
}
```

#### Remove Blood from Inventory
```
POST /inventory/remove
```

**Request Body:**
```json
{
  "hospital_id": "550e8400-e29b-41d4-a716-446655440000",
  "blood_type": "O+",
  "units": 2
}
```

#### Get Critical Stock Alerts
```
GET /inventory/alerts/hospital/{hospitalId}?threshold=5
```

**Query Parameters:**
- `threshold` (optional): Minimum threshold for alert (default: 5 units)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "bloodType": "AB-",
      "units": 3,
      "threshold": 5,
      "status": "critical"
    }
  ]
}
```

#### Update Inventory Status
```
PATCH /inventory/{inventoryId}/status
```

**Request Body:**
```json
{
  "status": "expired"
}
```

### Blood Transfer Requests

#### List Pending Transfers
```
GET /transfers?status=pending&page=1&per_page=10
```

**Query Parameters:**
- `status` (optional): Filter by status (pending, approved, rejected, completed)
- `page` (optional): Page number
- `per_page` (optional): Items per page

#### Get Hospital Transfers
```
GET /transfers/hospital/{hospitalId}?status=pending
```

#### Create Transfer Request
```
POST /transfers
```

**Request Body:**
```json
{
  "requesting_hospital_id": "550e8400-e29b-41d4-a716-446655440000",
  "approving_hospital_id": "550e8400-e29b-41d4-a716-446655440001",
  "blood_type": "O+",
  "units_needed": 10,
  "urgency": "critical",
  "notes": "Emergency surgery scheduled"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "transferId": "uuid",
    "requestingHospital": {
      "hospitalId": "uuid",
      "name": "Central Hospital"
    },
    "approvingHospital": {
      "hospitalId": "uuid",
      "name": "City Hospital"
    },
    "bloodType": "O+",
    "unitsNeeded": 10,
    "status": "pending",
    "urgency": "critical",
    "createdAt": "2024-01-16T14:30:00Z"
  }
}
```

#### Get Transfer Details
```
GET /transfers/{transferId}
```

#### Approve Transfer
```
POST /transfers/{transferId}/approve
```

**Request Body:**
```json
{
  "approver_staff_id": "550e8400-e29b-41d4-a716-446655440010"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "transferId": "uuid",
    "status": "approved",
    "approvedAt": "2024-01-16T15:00:00Z",
    "approvingStaff": {
      "staffId": "uuid",
      "name": "Dr. Smith"
    }
  }
}
```

#### Reject Transfer
```
POST /transfers/{transferId}/reject
```

**Request Body:**
```json
{
  "rejector_staff_id": "550e8400-e29b-41d4-a716-446655440010",
  "reason": "Insufficient stock available"
}
```

#### Cancel Transfer
```
POST /transfers/{transferId}/cancel
```

**Request Body:**
```json
{
  "reason": "Transfer no longer needed"
}
```

#### Complete Transfer
```
POST /transfers/{transferId}/complete
```

### Alerts

#### List Active Alerts
```
GET /alerts?status=active&page=1&per_page=10
```

**Query Parameters:**
- `status` (optional): Filter by status
- `page` (optional): Page number
- `per_page` (optional): Items per page

#### Get Hospital Alerts
```
GET /alerts/hospital/{hospitalId}
```

#### Create Alert
```
POST /alerts
```

**Request Body:**
```json
{
  "hospital_id": "550e8400-e29b-41d4-a716-446655440000",
  "blood_type": "O+",
  "units_needed": 20,
  "urgency": "critical",
  "description": "Emergency situation requiring immediate blood"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "alertId": "uuid",
    "hospital": {
      "hospitalId": "uuid",
      "name": "Central Hospital"
    },
    "bloodType": "O+",
    "unitsNeeded": 20,
    "urgency": "critical",
    "status": "active",
    "createdAt": "2024-01-16T16:00:00Z"
  }
}
```

#### Send Alert to Donors
```
POST /alerts/{alertId}/send
```

**Request Body:**
```json
{
  "target_donor_count": 50
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "alertId": "uuid",
    "donorAlertsCreated": 50,
    "status": "sent",
    "sentAt": "2024-01-16T16:05:00Z"
  }
}
```

#### Get Alert Details
```
GET /alerts/{alertId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "alertId": "uuid",
    "bloodType": "O+",
    "unitsNeeded": 20,
    "urgency": "critical",
    "status": "sent",
    "statistics": {
      "totalDonorsSent": 50,
      "acceptances": 15,
      "rejections": 5,
      "noResponse": 30,
      "responseRate": 40
    }
  }
}
```

#### Respond to Alert
```
POST /alerts/donor-alert/{donorAlertId}/respond
```

**Request Body:**
```json
{
  "response": "accepted"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "donorAlertId": "uuid",
    "response": "accepted",
    "respondedAt": "2024-01-16T16:10:00Z"
  }
}
```

#### Close Alert
```
POST /alerts/{alertId}/close
```

**Request Body:**
```json
{
  "reason": "Sufficient blood collected"
}
```

#### Cancel Alert
```
POST /alerts/{alertId}/cancel
```

### Donations

#### Create Donation
```
POST /donations
```

**Request Body:**
```json
{
  "donor_id": "550e8400-e29b-41d4-a716-446655440000",
  "donation_event_id": "550e8400-e29b-41d4-a716-446655440100",
  "hospital_id": "550e8400-e29b-41d4-a716-446655440001",
  "blood_type": "O+",
  "volume_collected": "450",
  "units_collected": 1,
  "notes": "Routine donation"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "donationId": "uuid",
    "donor": {
      "userId": "uuid",
      "firstName": "John",
      "lastName": "Doe"
    },
    "bloodType": "O+",
    "volumeCollected": "450",
    "status": "pending",
    "donationDate": "2024-01-16T10:00:00Z"
  }
}
```

#### Get Donation
```
GET /donations/{donationId}
```

#### Get Event Donations
```
GET /donations/event/{eventId}
```

#### Get Donor Donations
```
GET /donations/donor/{donorId}
```

#### Get Hospital Donations
```
GET /donations/hospital/{hospitalId}
```

#### Complete Donation
```
POST /donations/{donationId}/complete
```

**Request Body:**
```json
{
  "status": "completed"
}
```

#### Cancel Donation
```
POST /donations/{donationId}/cancel
```

**Request Body:**
```json
{
  "reason": "Medical issues detected"
}
```

#### Update Screening Results
```
PATCH /donations/{donationId}/screening
```

**Request Body:**
```json
{
  "screening_passed": true,
  "notes": "All tests passed successfully"
}
```

#### Get Hospital Statistics
```
GET /donations/stats/hospital/{hospitalId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalDonations": 150,
    "totalVolumeCollected": "67500",
    "uniqueDonors": 85,
    "bloodTypeDistribution": {
      "O+": 45,
      "O-": 20,
      "A+": 40,
      "A-": 15,
      "B+": 18,
      "B-": 7,
      "AB+": 3,
      "AB-": 2
    },
    "averageVolumePerDonation": "450"
  }
}
```

## Error Examples

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid request data",
  "errors": {
    "name": "Name is required",
    "latitude": "Latitude must be a valid number"
  },
  "statusCode": 400
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Hospital not found",
  "errors": null,
  "statusCode": 404
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error",
  "errors": "Database connection failed",
  "statusCode": 500
}
```

## Rate Limiting

Currently, there is no rate limiting implemented. This will be added in future versions.

## Pagination

Endpoints that return lists support pagination with the following query parameters:

- `page`: Page number (starts at 1)
- `per_page`: Items per page (default: 10, max: 100)

## Filtering

Endpoints support filtering with query parameters specific to each endpoint. Refer to individual endpoint documentation for available filters.

## Sorting

Sorting parameters will be implemented in future versions.

## Changelog

### Version 1.0.0 (Initial Release)
- Hospital CRUD endpoints
- Donor management endpoints
- Blood inventory management
- Blood transfer request workflow
- Donation alert system
- Donation tracking

## Support

For issues or questions about the API, refer to `docs/development-guide.md` and `docs/symfony-setup.md` first.
