# i2vAuthAPI - Complete API Documentation

> **Base URL:** `http://localhost:8080`
> **Auth:** JWT Bearer Token (`Authorization: Bearer <token>`)
> **Rate Limit:** 5 req/min for auth endpoints, 300 req/min for general endpoints
> **Content-Type:** `application/json`

---

## Table of Contents

1. [Authentication](#authentication)
2. [Users](#users)
3. [Employees](#employees)
4. [Departments](#departments)
5. [Designations](#designations)
6. [Modules](#modules)
7. [Module Access](#module-access)
8. [Attendance](#attendance)
9. [Admin Attendance](#admin-attendance)
10. [Leaves](#leaves)
11. [Exit Requests](#exit-requests)
12. [Manager Exit Requests](#manager-exit-requests)
13. [Hierarchy](#hierarchy)
14. [Holidays](#holidays)
15. [Files](#files)
16. [Settings](#settings)
17. [Company Settings](#company-settings)
18. [User Sessions](#user-sessions)

---

## Roles

| Role | Description |
|------|-------------|
| `GENERAL` | General user |
| `CUSTOMER` | Customer user |
| `VENDOR` | Vendor user |
| `EMPLOYEE` | Employee user |

---

## Response Format

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

```json
{
  "success": false,
  "message": "Error message",
  "error": "Error details"
}
```

---

## Authentication

### Signup
```
POST /api/auth/signup
Public
```
**Request Body:** `SignupRequest`
```json
{
  "email": "string",
  "password": "string",
  "fullName": "string",
  "role": "GENERAL | CUSTOMER | VENDOR | EMPLOYEE"
}
```
**Response:** `ApiResponse<Void>`

### Login
```
POST /api/auth/login
Public
```
**Request Body:** `LoginRequest`
```json
{
  "email": "string",
  "password": "string"
}
```
**Response:** `ApiResponse<JwtResponse>`
```json
{
  "token": "JWT access token",
  "refreshToken": "JWT refresh token"
}
```

### Logout
```
POST /api/auth/logout
Auth Required
Headers: Authorization: Bearer <token>
```
**Response:** `ApiResponse<Void>`

### Refresh Token
```
POST /api/auth/refresh-token
Auth Required
```
**Request Body:** `RefreshTokenRequest`
```json
{
  "refreshToken": "string"
}
```
**Response:** `ApiResponse<JwtResponse>`

### Forgot Password
```
POST /api/auth/forgot-password
Public
```
**Request Body:** `ForgotPasswordRequest`
```json
{
  "email": "string"
}
```
**Response:** `ApiResponse<Void>`

### Reset Password
```
POST /api/auth/reset-password
Public
```
**Request Body:** `ResetPasswordRequest`
```json
{
  "token": "string",
  "newPassword": "string"
}
```
**Response:** `ApiResponse<Void>`

### Get My Sessions
```
GET /api/auth/sessions
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<Map<String, Object>>>`

### Revoke Session
```
DELETE /api/auth/sessions/{sessionId}
Auth Required (EMPLOYEE)
Headers: X-Current-Token: <current_token>
```
**Response:** `ApiResponse<Void>`

### Revoke All Other Sessions
```
POST /api/auth/sessions/revoke-all
Auth Required (EMPLOYEE)
Headers: X-Current-Token: <current_token>
```
**Response:** `ApiResponse<Map<String, Object>>` (contains `revokedCount`)

---

## Users

### Get All Users
```
GET /api/users
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<UserResponse>>`

### Get User By ID
```
GET /api/users/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<UserResponse>`

### Get Current User
```
GET /api/users/me
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<UserResponse>`

### Update User Status
```
PUT /api/users/{id}/status
Auth Required (EMPLOYEE+ and canManageUsers)
```
**Request Body:**
```json
{
  "active": true
}
```
**Response:** `ApiResponse<Void>`

---

## Employees

### Get All Employees
```
GET /api/employees
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<EmployeeUserResponse>>`

### Get Employee By ID
```
GET /api/employees/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<EmployeeUserResponse>`

### Get Employee By Code
```
GET /api/employees/code/{employeeCode}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<EmployeeUserResponse>`

### Get Employees By Department
```
GET /api/employees/department/{departmentId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<EmployeeUserResponse>>`

### Get Employees By Designation
```
GET /api/employees/designation/{designationId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<EmployeeUserResponse>>`

### Get Current Employee
```
GET /api/employees/me
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<EmployeeUserResponse>`

### Check User Active Status
```
GET /api/employees/{id}/user-active
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Boolean>`

### Create Employee
```
POST /api/employees
Auth Required (EMPLOYEE and canManageEmployees)
```
**Request Body:** `EmployeeUserRequest`
```json
{
  "userId": "Long",
  "employeeCode": "string",
  "departmentId": "Long",
  "designationId": "Long",
  "reportingToId": "Long",
  "unitId": "Long",
  "squadId": "Long",
  "territoryId": "Long",
  "fotId": "Long",
  "joiningDate": "YYYY-MM-DD",
  "confirmationDate": "YYYY-MM-DD",
  "resignationDate": "YYYY-MM-DD",
  "employmentType": "PERMANENT | CONTRACT | INTERN | CONSULTANT | PROBATION",
  "employmentStatus": "ACTIVE | ON_LEAVE | SUSPENDED | TERMINATED | RESIGNED | INACTIVE",
  "phoneNumber": "string",
  "alternatePhone": "string",
  "address": "string",
  "city": "string",
  "state": "string",
  "country": "string",
  "postalCode": "string",
  "dateOfBirth": "YYYY-MM-DD",
  "gender": "MALE | FEMALE | OTHER",
  "bloodGroup": "string",
  "emergencyContactName": "string",
  "emergencyContactPhone": "string",
  "emergencyContactRelation": "string",
  "bankAccountNumber": "string",
  "bankName": "string",
  "bankIfscCode": "string",
  "panNumber": "string",
  "aadharNumber": "string",
  "pfNumber": "string",
  "esiNumber": "string",
  "uanNumber": "string",
  "workEmail": "string",
  "workPhone": "string",
  "extension": "string",
  "seatingLocation": "string",
  "shiftTiming": "string",
  "weeklyOff": "string",
  "probationPeriodMonths": "integer",
  "noticePeriodDays": "integer",
  "annualLeavesEntitled": "integer",
  "sickLeavesEntitled": "integer",
  "casualLeavesEntitled": "integer"
}
```
**Response:** `ApiResponse<EmployeeUserResponse>`

### Join Employee
```
POST /api/employees/join
Auth Required
```
**Request Body:** `EmployeeJoinDto`
**Response:** `ApiResponse<EmployeeUserResponse>`

### Update Employee
```
PUT /api/employees/{id}
Auth Required (EMPLOYEE and canManageEmployees)
```
**Request Body:** `EmployeeUserRequest`
**Response:** `ApiResponse<EmployeeUserResponse>`

### Delete Employee
```
DELETE /api/employees/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Deactivate User
```
POST /api/employees/{id}/deactivate-user?reason=string
Auth Required (EMPLOYEE and canManageEmployees)
```
**Response:** `ApiResponse<Void>`

### Reactivate User
```
POST /api/employees/{id}/reactivate-user?reason=string
Auth Required (EMPLOYEE and canManageEmployees)
```
**Response:** `ApiResponse<Void>`

### Deactivate User By User ID
```
POST /api/employees/user/{userId}/deactivate?reason=string
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reactivate User By User ID
```
POST /api/employees/user/{userId}/reactivate?reason=string
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Update Leave Entitlements
```
PUT /api/employees/{id}/leave-entitlements
Auth Required (EMPLOYEE and canManageEmployees)
```
**Request Body:**
```json
{
  "annualLeavesEntitled": 24,
  "sickLeavesEntitled": 12,
  "casualLeavesEntitled": 12
}
```
**Response:** `ApiResponse<EmployeeUserResponse>`

### Bulk Update Leave Entitlements
```
POST /api/employees/bulk-leave-entitlements
Auth Required (EMPLOYEE and canManageEmployees)
```
**Request Body:**
```json
{
  "departmentId": "Long",
  "leaveType": "ANNUAL | SICK | CASUAL",
  "entitledDays": 24
}
```
**Response:** `ApiResponse<Map<String, Object>>`

### Get Employee Leave Balance
```
GET /api/employees/{id}/leave-balance?year=2025
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Map<String, Object>>`
```json
{
  "annual": { "used": 5, "total": 24, "remaining": 19 },
  "sick": { "used": 2, "total": 12, "remaining": 10 },
  "casual": { "used": 0, "total": 12, "remaining": 12 }
}
```

### Update Profile
```
PATCH /api/employees/{id}/profile
Auth Required (EMPLOYEE)
```
**Request Body:** `ProfileUpdateRequest`
**Response:** `ApiResponse<EmployeeUserResponse>`

---

## Employee Exit (My Exit)

### Get My Exit Request
```
GET /api/employees/me/exit
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<ExitResponseDto>`

### Submit Exit Request
```
POST /api/employees/exit
Auth Required (EMPLOYEE)
```
**Request Body:** `ExitRequestDto`
```json
{
  "place": "string",
  "reason": "string",
  "lastWorkingDate": "YYYY-MM-DD",
  "comments": "string"
}
```
**Response:** `ApiResponse<ExitResponseDto>`

### Reopen Exit Request
```
PUT /api/employees/exit/{exitRequestId}/reopen
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<ExitResponseDto>`

### Delete Exit Request
```
DELETE /api/employees/exit/{exitRequestId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Send Exit Notification
```
POST /api/employees/exit/{exitRequestId}/send-notification
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

---

## Departments

### Get All Departments
```
GET /api/departments
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<DepartmentResponse>>`

### Get Active Departments
```
GET /api/departments/active
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<DepartmentResponse>>`

### Get Department By ID
```
GET /api/departments/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<DepartmentResponse>`

### Create Department
```
POST /api/departments
Auth Required (EMPLOYEE)
```
**Request Body:** `DepartmentRequest`
```json
{
  "name": "string",
  "code": "string",
  "description": "string",
  "parentDepartmentId": "Long",
  "unitId": "Long",
  "isHierarchyUnit": "boolean",
  "managerId": "Long",
  "isActive": "boolean",
  "budget": "decimal",
  "location": "string",
  "email": "string",
  "phone": "string"
}
```
**Response:** `ApiResponse<DepartmentResponse>`

### Update Department
```
PUT /api/departments/{id}
Auth Required (EMPLOYEE)
```
**Request Body:** `DepartmentRequest`
**Response:** `ApiResponse<DepartmentResponse>`

### Delete Department
```
DELETE /api/departments/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Get Department Weekly Off
```
GET /api/departments/{departmentId}/weekly-off
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<DepartmentWeeklyOff>`

---

## Designations

### Get All Designations
```
GET /api/designations
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<DesignationResponse>>`

### Get Designation By ID
```
GET /api/designations/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<DesignationResponse>`

### Get Designations By Department
```
GET /api/designations/department/{departmentId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<DesignationResponse>>`

### Create Designation
```
POST /api/designations
Auth Required (EMPLOYEE)
```
**Request Body:**
```json
{
  "title": "string",
  "code": "string",
  "description": "string",
  "level": "integer",
  "departmentId": "Long",
  "minSalary": "decimal",
  "maxSalary": "decimal",
  "isActive": "boolean",
  "isManagerial": "boolean"
}
```
**Response:** `ApiResponse<DesignationResponse>`

### Update Designation
```
PUT /api/designations/{id}
Auth Required (EMPLOYEE)
```
**Request Body:** Same as create
**Response:** `ApiResponse<DesignationResponse>`

### Delete Designation
```
DELETE /api/designations/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

---

## Modules

### Get All Modules
```
GET /api/modules
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<ModuleResponse>>`

### Get Active Modules
```
GET /api/modules/active
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<ModuleResponse>>`

### Get Module By ID
```
GET /api/modules/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<ModuleResponse>`

### Create Module
```
POST /api/modules
Auth Required (EMPLOYEE)
```
**Request Body:** `ModuleRequest`
**Response:** `ApiResponse<ModuleResponse>`

### Update Module
```
PUT /api/modules/{id}
Auth Required (EMPLOYEE)
```
**Request Body:** `ModuleRequest`
**Response:** `ApiResponse<ModuleResponse>`

### Delete Module
```
DELETE /api/modules/{id}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Refresh Routes
```
POST /api/modules/refresh-routes
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Get Active Routes
```
GET /api/modules/active-routes
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<String>>`

---

## Module Access

### Get Bootstrap Data
```
GET /api/module-access/bootstrap
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Object>` (contains `departments`, `designations`, `modules`)

### Get My Permissions
```
GET /api/module-access/my-permissions
Auth Required (EMPLOYEE)
```
Returns the caller's computed permission map (`modules` keyed by module code and
`modulesByRoute` keyed by normalized route path). See
[How Module Access Works](#how-module-access-works).

**Response:** `ApiResponse<Object>`

### Get Modules Assigned To Department
```
GET /api/module-access/department/{departmentId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<ModuleResponse>>`

### Get Permissions By Designation
```
GET /api/module-access/designation/{designationId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<DesignationModuleAccessResponse>>`

### Assign Modules To Department
```
POST /api/module-access/department/{departmentId}
Auth Required (EMPLOYEE and canManageModuleAccess)
```
**Request Body:** `List<Long>` — module IDs to assign (replaces the department's current module set)
```json
[1, 2, 3]
```
**Response:** `ApiResponse<Void>`

### Grant Access By Designation
```
POST /api/module-access/designation
Auth Required (EMPLOYEE and canManageModuleAccess)
```
**Request Body:** `DesignationModuleAccessRequest`
```json
{
  "designationId": "Long",
  "moduleId": "Long",
  "accessLevel": "NONE | READ | WRITE | ADMIN"
}
```
**Behavior:** granting `READ`/`WRITE`/`ADMIN` also automatically assigns the module
to the designation's department (`department_module`) when missing, so the grant
takes effect immediately. Setting `NONE` only revokes the designation access.

**Response:** `ApiResponse<Void>`

### Revoke Access By Department
```
DELETE /api/module-access/department/{departmentId}/module/{moduleId}
Auth Required (EMPLOYEE and canManageModuleAccess)
```
**Response:** `ApiResponse<Void>`

### Revoke Access By Designation
```
DELETE /api/module-access/designation/{designationId}/module/{moduleId}
Auth Required (EMPLOYEE and canManageModuleAccess)
```
**Response:** `ApiResponse<Void>`

---

### How Module Access Works

Access requires **two mappings**:

| Mapping | Table | Meaning |
|---------|-------|---------|
| Department ↔ Module | `department_module` | The module is available to the department |
| Designation ↔ Module | `designation_module_access` | What a designation can do with the module (`NONE`/`READ`/`WRITE`/`ADMIN`) |

`GET /api/module-access/my-permissions` builds the caller's permission map by
iterating the active `department_module` rows of the employee's department and
looking up the matching `designation_module_access` for the employee's designation.

**Permission resolution rules:**
- A module appears in the permission map when it has an active
  `department_module` row; its `READ`/`WRITE`/`ADMIN` flags come from the
  designation access row (missing access defaults to `NONE`).
- **Repair rule:** a module with an active designation access but **no**
  `department_module` row at all is still included. This covers modules that
  were granted to a designation before being assigned to the department.
- A module whose `department_module` row exists but is inactive is treated as
  deliberately removed and stays excluded, even if a designation access row
  remains.
- `POST /api/module-access/designation` with `READ`/`WRITE`/`ADMIN` auto-creates
  or reactivates the `department_module` row for the designation's department.

**Caching:**
- Permission maps are cached per user (`userPermissions` cache). Every
  module/access mutation endpoint evicts the cache, so changes apply on the
  next `/my-permissions` call.
- Module create/update/delete also evicts the permission cache and refreshes
  the in-memory route registry (`ModuleRouteConfig`), so newly created or
  activated modules are usable without an application restart.

---

## Attendance

> **Permissions:** check-in/check-out only require a valid JWT. `today`,
> `history`, and `summary` require the `canReadAttendance` permission, which is
> granted by `READ`/`WRITE`/`ADMIN` access on the `ATTENDANCE` or
> `ATTENDANCE_MANAGEMENT` module, or by the attendance route group; it falls
> back to allowing any authenticated employee to view their own attendance.
> The module's `route_path` must be `/attendance` for the frontend route gate.

### Check In
```
POST /api/attendance/check-in
Auth Required
```
**Response:** `ApiResponse<AttendanceResponse>`

### Check Out
```
POST /api/attendance/check-out
Auth Required
```
**Response:** `ApiResponse<AttendanceResponse>`

### Get Today's Attendance
```
GET /api/attendance/today
Auth Required (canReadAttendance)
```
**Response:** `ApiResponse<AttendanceResponse>`

### Get Attendance History
```
GET /api/attendance/history?startDate=2026-09-01&endDate=2026-09-30
Auth Required (canReadAttendance)
```
`startDate` and `endDate` are optional; when omitted, the endpoint returns records from the current month through today.

**Response:** `ApiResponse<Page<AttendanceResponse>>`

### Get Attendance Summary
```
GET /api/attendance/summary?year=2026&month=9
Auth Required (canReadAttendance)
```
`year` and `month` are optional; when omitted, the endpoint returns the current month's summary. `/api/attendance/summery` is also accepted for compatibility.

**Response:** `ApiResponse<Object>`

---

## Admin Attendance

> **Permissions:** every endpoint below requires `canManageAttendance`, which is
> granted by `WRITE`/`ADMIN` access on the `ATTENDANCE` or
> `ATTENDANCE_MANAGEMENT` module (or the attendance route group). There is no
> fallback — if the designation has only `READ` access, these endpoints return
> `403`. This is why attendance management requires enabling the
> `ATTENDANCE_MANAGEMENT` (or `ATTENDANCE`) module for the designation.

### Get All Employees for Attendance
```
GET /api/admin/attendance/employees
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<List<EmployeeUserResponse>>`

### Get Employee Attendance
```
GET /api/admin/attendance/employee/{employeeId}
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<List<AttendanceResponse>>`

### Get Employee Summary
```
GET /api/admin/attendance/employee/{employeeId}/summary
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<Object>`

### Get Employee Today
```
GET /api/admin/attendance/employee/{employeeId}/today
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<AttendanceResponse>`

### Mark Attendance
```
POST /api/admin/attendance/mark
Auth Required (EMPLOYEE and canManageAttendance)
```
**Request Body:** `AttendanceRequest`
**Response:** `ApiResponse<AttendanceResponse>`

### Update Attendance
```
PUT /api/admin/attendance/{attendanceId}
Auth Required (EMPLOYEE and canManageAttendance)
```
**Request Body:** `AttendanceRequest`
**Response:** `ApiResponse<AttendanceResponse>`

### Delete Attendance
```
DELETE /api/admin/attendance/{attendanceId}
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<Void>`

### Get Attendance Statistics
```
GET /api/admin/attendance/statistics
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<Object>`

### Bulk Mark Attendance
```
POST /api/admin/attendance/bulk-mark
Auth Required (EMPLOYEE and canManageAttendance)
```
**Request Body:** `AttendanceRequest`
**Response:** `ApiResponse<Object>`

### Get Pending Attendance
```
GET /api/admin/attendance/pending
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `ApiResponse<List<AttendanceResponse>>`

### Export Attendance
```
GET /api/admin/attendance/export
Auth Required (EMPLOYEE and canManageAttendance)
```
**Response:** `File (Excel)`

---

## Leaves

### Get My Leaves
```
GET /api/leaves/my-leaves
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

### Get My Leave Balance
```
GET /api/leaves/my-balance
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Object>`

### Apply for Leave
```
POST /api/leaves/apply
Auth Required (EMPLOYEE)
```
**Request Body:** `LeaveRequestDto`
```json
{
  "employeeId": "Long",
  "leaveType": "ANNUAL | SICK | CASUAL | MATERNITY | PATERNITY | BEREAVEMENT | UNPAID | WORK_FROM_HOME",
  "startDate": "YYYY-MM-DD",
  "endDate": "YYYY-MM-DD",
  "totalDays": "decimal",
  "reason": "string",
  "contactDuringLeave": "string",
  "alternateContact": "string"
}
```
**Response:** `ApiResponse<LeaveResponse>`

### Cancel Leave
```
PUT /api/leaves/{id}/cancel
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Get Team Leaves
```
GET /api/leaves/team-leaves
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

### Get Team Pending Leaves
```
GET /api/leaves/team-pending
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

### Get All Leaves
```
GET /api/leaves/all-leaves
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

### Approve Leave
```
PUT /api/leaves/{id}/approve
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reject Leave
```
PUT /api/leaves/{id}/reject
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Get All Pending Leaves
```
GET /api/leaves/all-pending
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

### Get All Leave Statistics
```
GET /api/leaves/all-statistics
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Object>`

### Get Leave By Employee
```
GET /api/leaves/employee/{employeeId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<LeaveResponse>>`

---

## Exit Requests

### Get All Exit Requests
```
GET /api/admin/exit-requests
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<ExitResponseDto>>`

### Approve Exit Step
```
PUT /api/admin/exit-requests/{exitRequestId}/steps/{stepId}/approve
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reject Exit Step
```
PUT /api/admin/exit-requests/{exitRequestId}/steps/{stepId}/reject
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reopen Exit Request
```
PUT /api/admin/exit-requests/{exitRequestId}/reopen
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Delete Exit Request
```
DELETE /api/admin/exit-requests/{exitRequestId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

---

## Manager Exit Requests

### Get All Exit Requests (Manager)
```
GET /api/manager/exit-requests
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<List<ExitResponseDto>>`

### Approve Exit Step (Manager)
```
PUT /api/manager/exit-requests/{exitRequestId}/steps/{stepId}/approve
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reject Exit Step (Manager)
```
PUT /api/manager/exit-requests/{exitRequestId}/steps/{stepId}/reject
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Reopen Exit Request (Manager)
```
PUT /api/manager/exit-requests/{exitRequestId}/reopen
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

### Delete Exit Request (Manager)
```
DELETE /api/manager/exit-requests/{exitRequestId}
Auth Required (EMPLOYEE)
```
**Response:** `ApiResponse<Void>`

---

## Hierarchy

### Get Full Hierarchy
```
GET /api/hierarchy/full
Auth Required
```
**Response:** `ApiResponse<Object>`

### Get Units
```
GET /api/hierarchy/units
Auth Required
```
**Response:** `ApiResponse<List<UnitResponse>>`

### Get Unit By ID
```
GET /api/hierarchy/units/{id}
Auth Required
```
**Response:** `ApiResponse<UnitResponse>`

### Create Unit
```
POST /api/hierarchy/units
Auth Required
```
**Request Body:** `UnitRequest`
**Response:** `ApiResponse<UnitResponse>`

### Update Unit
```
PUT /api/hierarchy/units/{id}
Auth Required
```
**Request Body:** `UnitRequest`
**Response:** `ApiResponse<UnitResponse>`

### Delete Unit
```
DELETE /api/hierarchy/units/{id}
Auth Required
```
**Response:** `ApiResponse<Void>`

### Get Squads
```
GET /api/hierarchy/squads
Auth Required
```
**Response:** `ApiResponse<List<SquadResponse>>`

### Get Squads By Unit
```
GET /api/hierarchy/squads/by-unit/{unitId}
Auth Required
```
**Response:** `ApiResponse<List<SquadResponse>>`

### Create Squad
```
POST /api/hierarchy/squads
Auth Required
```
**Request Body:** `SquadRequest`
**Response:** `ApiResponse<SquadResponse>`

### Update Squad
```
PUT /api/hierarchy/squads/{id}
Auth Required
```
**Request Body:** `SquadRequest`
**Response:** `ApiResponse<SquadResponse>`

### Delete Squad
```
DELETE /api/hierarchy/squads/{id}
Auth Required
```
**Response:** `ApiResponse<Void>`

### Get Territories
```
GET /api/hierarchy/territories
Auth Required
```
**Response:** `ApiResponse<List<TerritoryResponse>>`

### Get Territories By Squad
```
GET /api/hierarchy/territories/by-squad/{squadId}
Auth Required
```
**Response:** `ApiResponse<List<TerritoryResponse>>`

### Create Territory
```
POST /api/hierarchy/territories
Auth Required
```
**Request Body:** `TerritoryRequest`
**Response:** `ApiResponse<TerritoryResponse>`

### Update Territory
```
PUT /api/hierarchy/territories/{id}
Auth Required
```
**Request Body:** `TerritoryRequest`
**Response:** `ApiResponse<TerritoryResponse>`

### Delete Territory
```
DELETE /api/hierarchy/territories/{id}
Auth Required
```
**Response:** `ApiResponse<Void>`

### Get FOT Access Levels
```
GET /api/hierarchy/fot-access-levels
Auth Required
```
**Response:** `ApiResponse<List<FotAccessLevelResponse>>`

### Get FOT Access Levels By Territory
```
GET /api/hierarchy/fot-access-levels/by-territory/{territoryId}
Auth Required
```
**Response:** `ApiResponse<List<FotAccessLevelResponse>>`

### Create FOT Access Level
```
POST /api/hierarchy/fot-access-levels
Auth Required
```
**Request Body:** `FotAccessLevelRequest`
**Response:** `ApiResponse<FotAccessLevelResponse>`

### Update FOT Access Level
```
PUT /api/hierarchy/fot-access-levels/{id}
Auth Required
```
**Request Body:** `FotAccessLevelRequest`
**Response:** `ApiResponse<FotAccessLevelResponse>`

### Delete FOT Access Level
```
DELETE /api/hierarchy/fot-access-levels/{id}
Auth Required
```
**Response:** `ApiResponse<Void>`

---

## Holidays

### Get All Holidays
```
GET /api/holidays
Auth Required
```
**Response:** `ApiResponse<List<Holiday>>`

### Check Holiday
```
GET /api/holidays/check
Auth Required
```
**Response:** `ApiResponse<Object>`

### Get Holiday Info
```
GET /api/holidays/info
Auth Required
```
**Response:** `ApiResponse<Object>`

### Create Holiday
```
POST /api/holidays
Auth Required
```
**Request Body:**
```json
{
  "holidayName": "string",
  "holidayDate": "YYYY-MM-DD",
  "description": "string",
  "isRecurringYearly": "boolean"
}
```
**Response:** `ApiResponse<Holiday>`

### Update Holiday
```
PUT /api/holidays/{id}
Auth Required
```
**Request Body:** Same as create
**Response:** `ApiResponse<Holiday>`

### Delete Holiday
```
DELETE /api/holidays/{id}
Auth Required
```
**Response:** `ApiResponse<Void>`

---

## Files

### Upload File (With Submission)
```
POST /api/files/upload/{submissionId}
Auth Required
```
**Request Body:** `multipart/form-data` (file)
**Response:** `ApiResponse<FileController.FileResponse>>`

### Upload File
```
POST /api/files/upload
Auth Required
```
**Request Body:** `multipart/form-data` (file)
**Response:** `ApiResponse<FileController.FileResponse>>`

### Download File
```
GET /api/files/download/{submissionId}
Auth Required
```
**Response:** `File (binary)`

### Delete File
```
DELETE /api/files/
Auth Required
```
**Response:** `ApiResponse<Void>`

---

## Settings

### Get All Settings
```
GET /api/settings
Auth Required
```
**Response:** `ApiResponse<List<CompanySettings>>`

### Get Setting By Key
```
GET /api/settings/{key}
Auth Required
```
**Response:** `ApiResponse<CompanySettings>`

### Update Setting
```
PUT /api/settings/{key}
Auth Required
```
**Request Body:**
```json
{
  "settingValue": "string",
  "description": "string",
  "isActive": "boolean"
}
```
**Response:** `ApiResponse<CompanySettings>`

### Clear Cache
```
POST /api/settings/clear-cache
Auth Required
```
**Response:** `ApiResponse<Void>`

### Get My Weekly Off
```
GET /api/settings/my-weekly-off
Auth Required
```
**Response:** `ApiResponse<Object>`

### Get Department Weekly Off
```
GET /api/settings/department/{departmentId}/weekly-off
Auth Required
```
**Response:** `ApiResponse<DepartmentWeeklyOff>`

### Get All Department Weekly Off
```
GET /api/settings/department-weekly-off/all
Auth Required
```
**Response:** `ApiResponse<List<DepartmentWeeklyOff>>`

### Update Department Weekly Off
```
PUT /api/settings/department/{departmentId}/weekly-off
Auth Required
```
**Request Body:**
```json
{
  "weeklyOffDays": "string",
  "effectiveFrom": "YYYY-MM-DD",
  "effectiveTo": "YYYY-MM-DD"
}
```
**Response:** `ApiResponse<DepartmentWeeklyOff>`

### Check If Today Is Weekly Off
```
GET /api/settings/is-weekly-off
Auth Required
```
**Response:** `ApiResponse<Boolean>`

### Get Late Threshold
```
GET /api/settings/late-threshold
Auth Required
```
**Response:** `ApiResponse<Integer>`

### Get Official End Time
```
GET /api/settings/official-end-time
Auth Required
```
**Response:** `ApiResponse<String>`

---

## Company Settings

### Get All Company Settings
```
GET /api/company-settings
Auth Required
```
**Response:** `ApiResponse<List<CompanySettings>>`

### Get Department Weekly Off
```
GET /api/company-settings/department/{departmentId}/weekly-off
Auth Required
```
**Response:** `ApiResponse<DepartmentWeeklyOff>`

---

## Error Codes

| Code | Description |
|------|-------------|
| `400` | Bad Request - Invalid input |
| `401` | Unauthorized - Missing or invalid JWT |
| `403` | Forbidden - Insufficient permissions |
| `404` | Not Found - Resource does not exist |
| `429` | Too Many Requests - Rate limit exceeded |
| `500` | Internal Server Error |

---

## Exception Classes

| Exception | HTTP Status |
|-----------|-------------|
| `BadRequestException` | 400 |
| `ResourceNotFoundException` | 404 |
| `UnauthorizedException` | 401 |
| `GlobalExceptionHandler` | Catch-all |

---

## Security

- **JWT Algorithm:** HS512
- **Session Management:** Stateless
- **CORS:** Configured via `application.properties`
- **Rate Limiting:** Sliding window counter
  - Auth endpoints: 5 requests/minute
  - General endpoints: 300 requests/minute
- **Authentication:** BCrypt password encoder (strength 12)

---

## Project Info

- **Project:** i2vAuthAPI
- **Version:** 1.0.0
- **Base Package:** `com.world.i2v.i2vauthapi`
- **Framework:** Spring Boot 3.1.2
- **Database:** SQL Server
- **Migration Tool:** Flyway
- **Build Tool:** Maven 3.9.16
- **Java:** 17

---

## Migration Files

| Version | Description | File |
|---------|-------------|------|
| V1 | Initial Schema (Baseline) | `V1__Initial_schema.sql` |
| V7 | i2vAuthAPI Schema (22 tables) | `V7__i2vauthapi_schema.sql` |

### Tables Created by V7 Migration

1. `users` - User accounts
2. `employee_users` - Employee data (extends User via @MapsId)
3. `departments` - Department hierarchy
4. `designations` - Job designations
5. `modules` - Application modules
6. `units` - Organizational units
7. `squads` - Squads within units
8. `territories` - Territories within squads
9. `fot_access_levels` - Field officer territory access
10. `leave_requests` - Leave applications
11. `attendance` - Attendance records
12. `exit_requests` - Exit/resignation requests
13. `clearance_steps` - Exit clearance steps
14. `holidays` - Holiday definitions
15. `notifications` - User notifications
16. `password_reset_token` - Password reset tokens
17. `user_sessions` - User session tracking
18. `department_module` - Department-Module junction
19. `designation_module_access` - Designation-Module access
20. `company_settings` - Company configuration
21. `department_weekly_off` - Department weekly off days
22. `audit_logs` - Audit trail

---

## DTOs

### Request DTOs
- `SignupRequest`, `LoginRequest`, `RefreshTokenRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`
- `EmployeeUserRequest`, `EmployeeJoinDto`, `ProfileUpdateRequest`, `DepartmentRequest`
- `DesignationRequest`, `ModuleRequest`, `TerritoryRequest`, `SquadRequest`, `UnitRequest`
- `FotAccessLevelRequest`, `AttendanceRequest`, `LeaveRequestDto`, `ExitRequestDto`
- `DesignationModuleAccessRequest`

### Response DTOs
- `ApiResponse`, `UserResponse`, `EmployeeUserResponse`, `DepartmentResponse`
- `DesignationResponse`, `LeaveResponse`, `ExitResponseDto`, `ClearanceStepDto`
- `AttendanceResponse`, `JwtResponse`, `FotAccessLevelResponse`
- `ModuleResponse`, `UnitResponse`, `SquadResponse`, `TerritoryResponse`

---

## Changelog

### 2026-09-30 — Module access repair fix

- `GET /api/module-access/my-permissions` now includes modules that have an
  active designation access but no `department_module` row (repair rule),
  fixing "Access Denied" after assigning a user to a module without assigning
  it to the department.
- `POST /api/module-access/designation` auto-assigns the module to the
  designation's department when granting `READ`/`WRITE`/`ADMIN`.
- Module create/update/delete now evicts the `userPermissions` cache and
  refreshes the in-memory module route registry (`ModuleRouteConfig`), so
  newly created or activated modules (e.g. `ATTENDANCE`,
  `ATTENDANCE_MANAGEMENT`) take effect without an application restart.
- Attendance and Admin Attendance endpoint permission requirements documented
  (`canReadAttendance` fallback vs. strict `canManageAttendance`).
