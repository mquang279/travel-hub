# Travel Hub API Documentation

Base URL: `/api`

Authentication: most user/trip/post/place actions expect `Authorization: Bearer <accessToken>`. Public read endpoints depend on security configuration, but controllers that call `CurrentUserProvider.getCurrentUserId()` require an authenticated user.

Date/time formats:

- `LocalDate`: `YYYY-MM-DD`
- `LocalTime`: `HH:mm:ss`
- `Instant`: ISO-8601 timestamp, for example `2026-05-20T10:30:00Z`
- `LocalDateTime`: `YYYY-MM-DDTHH:mm:ss`
- Money fields use JSON number values and map to Java `BigDecimal` unless noted.

Common pagination response:

```json
{
  "pageNumber": 0,
  "pageSize": 10,
  "totalPages": 3,
  "totalElements": 25,
  "data": []
}
```

Common error response from `GlobalExceptionHandler`:

```json
{
  "error": "Error message",
  "status": 400
}
```

## Auth

### Register

`POST /api/auth/register`

Create a user account and return authentication tokens.

Request body: `RegisterRequest`

```json
{
  "email": "user@example.com",
  "username": "minhquang",
  "password": "password"
}
```

Response `200 OK`: `AuthResponse`

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "userId": 1,
  "isOnboarded": false
}
```

### Login

`POST /api/auth/login`

Authenticate a user and return tokens.

Request body: `LoginRequest`

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Response `200 OK`: `AuthResponse`

### Logout

`POST /api/auth/logout`

Invalidate the current authenticated user's refresh token/session.

Headers: `Authorization: Bearer <accessToken>`

Response `204 No Content`.

## Users

### Get My Profile

`GET /api/users/me`

Return the authenticated user's profile.

Response `200 OK`: `UserProfileResponse`

```json
{
  "id": 1,
  "avatarUrl": "https://cdn.example.com/avatar.jpg",
  "name": "Minh Quang",
  "username": "minhquang",
  "bio": "Travel lover",
  "email": "user@example.com",
  "phoneNumber": "0123456789",
  "dateOfBirth": "2003-01-01",
  "gender": "MALE",
  "location": "Ha Noi",
  "followersCount": 12,
  "followingCount": 5,
  "postsCount": 8,
  "isFollowing": false
}
```

### Get User Profile

`GET /api/users/{userId}`

Return a user's public profile. If authenticated, `isFollowing` is resolved against the current user.

Path parameters:

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | Long | yes | User id. |

Response `200 OK`: `UserProfileResponse`

### Search Users

`GET /api/users/search?username={username}&page={page}&pageSize={pageSize}`

Search users by username.

Query parameters:

| Name | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `username` | String | no | `""` | Username keyword. |
| `page` | int | no | `0` | Zero-based page number. |
| `pageSize` | int | no | `10` | Page size. |

Response `200 OK`: `PaginationResponse<UserProfileResponse>`

### Get User Posts

`GET /api/users/{userId}/posts?page={page}&pageSize={pageSize}`

Return posts created by a user.

Query parameters:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | int | no | `0` |
| `pageSize` | int | no | `5` |

Response `200 OK`: `PaginationResponse<PostResponse>`

### Get User Liked Posts

`GET /api/users/{userId}/liked-posts?page={page}&pageSize={pageSize}`

Return posts liked by a user. If authenticated, each post's `isLiked` value is resolved against the current user.

Path parameters:

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | Long | yes | User id. |

Query parameters:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | int | no | `0` |
| `pageSize` | int | no | `5` |

Response `200 OK`: `PaginationResponse<PostResponse>`

### Get User Saved Posts

`GET /api/users/{userId}/saved-posts?page={page}&pageSize={pageSize}`

Return posts saved by a user. If authenticated, each post's `isLiked` value is resolved against the current user.

Path parameters:

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | Long | yes | User id. |

Query parameters:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | int | no | `0` |
| `pageSize` | int | no | `5` |

Response `200 OK`: `PaginationResponse<PostResponse>`

### Update My Profile

`PUT /api/users/me`

Update the authenticated user's profile.

Request body: `UpdateProfileRequest`

```json
{
  "name": "Minh Quang",
  "username": "minhquang",
  "bio": "Travel lover",
  "email": "user@example.com",
  "phoneNumber": "0123456789",
  "dateOfBirth": "2003-01-01",
  "avatarUrl": "https://cdn.example.com/avatar.jpg",
  "location": "Ha Noi"
}
```

Response `200 OK`: `UserProfileResponse`

### Update User Profile

`PUT /api/users/{userId}`

Update a user profile by id.

Request body: `UpdateProfileRequest`

Response `200 OK`: `UserProfileResponse`

### Get Preferences

`GET /api/users/{userId}/preferences`

Return preferences for the authenticated user. The controller rejects access when `userId` is not the current user.

Response `200 OK`: `PreferenceResponse`

```json
{
  "user_id": 1,
  "trip_type": "BACKPACKING",
  "interests": ["food", "culture"],
  "destination": "Da Nang",
  "updated_at": "2026-05-20T10:30:00Z",
  "isOnboarded": true
}
```

### Update Preferences

`PUT /api/users/{userId}/preferences`

Update preferences for the authenticated user.

Request body: `PreferenceUpdateRequest`

```json
{
  "trip_type": "BACKPACKING",
  "interests": ["food", "culture"],
  "destination": "Da Nang",
  "isOnboarded": true
}
```

Response `200 OK`: `PreferenceResponse`

## Follow

Note: current controller uses `MOCK_CURRENT_USER_ID = 1L` instead of `CurrentUserProvider`.

### Get Followers

`GET /api/users/{userId}/followers?page={page}&size={size}`

Response `200 OK`: Spring `Page<UserFollowResponse>`

`UserFollowResponse` fields: `id`, `name`, `username`, `avatarUrl`, `following`.

### Get Following

`GET /api/users/{userId}/following?page={page}&size={size}`

Response `200 OK`: Spring `Page<UserFollowResponse>`

### Follow User

`POST /api/users/{targetUserId}/follow`

Response `200 OK` with empty body.

### Unfollow User

`DELETE /api/users/{targetUserId}/follow`

Response `200 OK` with empty body.

## Devices

### Add Device Token

`POST /api/devices/token`

Register a push notification token.

Request body: `AddDeviceTokenRequest`

```json
{
  "token": "fcm-device-token"
}
```

Response `204 No Content`.

## Notifications

### List Notifications

`GET /api/notifications?page={page}&pageSize={pageSize}`

Response `200 OK`: `PaginationResponse<NotificationModel>`

```json
{
  "pageNumber": 0,
  "pageSize": 10,
  "totalPages": 1,
  "totalElements": 1,
  "data": [
    {
      "title": "New like",
      "body": "Someone liked your post",
      "isRead": false,
      "type": "LIKE",
      "targetId": 10,
      "createdAt": "2026-05-20T10:30:00Z"
    }
  ]
}
```

### List Unread Notifications

`GET /api/notifications/unread?page={page}&pageSize={pageSize}`

Response `200 OK`: `PaginationResponse<NotificationModel>`

### Mark All Notifications As Read

`PUT /api/notifications/read-all`

Mark all unread notifications of the authenticated user as read and set `readAt`.

Headers: `Authorization: Bearer <accessToken>`

Response `204 No Content`.

Notification `type` values: `COMMENT`, `LIKE`, `FOLLOW`.

## Posts

### List Posts

`GET /api/posts?page={page}&pageSize={pageSize}`

Return paginated posts.

Response `200 OK`: `PaginationResponse<PostResponse>`

### Search Posts

`GET /api/posts/search?description={description}&page={page}&pageSize={pageSize}`

Search posts by description text.

Response `200 OK`: `PaginationResponse<PostResponse>`

### Create Post

`POST /api/posts`

Request body: `CreatePostRequest`

```json
{
  "description": "Great trip",
  "imageUrls": ["https://cdn.example.com/post-1.jpg"],
  "travelPlaceId": 3
}
```

Response `201 Created`: `PostResponse`

```json
{
  "id": 10,
  "description": "Great trip",
  "imageUrls": ["https://cdn.example.com/post-1.jpg"],
  "owner": {
    "id": 1,
    "username": "minhquang"
  },
  "likeCount": 0,
  "commentCount": 0,
  "isLiked": false,
  "travelPlaceId": 3,
  "location": "Da Nang",
  "createdAt": "2026-05-20T10:30:00Z",
  "updatedAt": "2026-05-20T10:30:00Z"
}
```

### Update Post

`PUT /api/posts/{postId}`

Request body: `ModifyPostRequest`

```json
{
  "description": "Updated description"
}
```

Response `200 OK`: `PostResponse`

### Like Post

`POST /api/posts/{postId}/like`

Response `200 OK`: `LikePostResponse`

```json
{
  "postId": 10,
  "liked": true,
  "likeCount": 1
}
```

### Unlike Post

`DELETE /api/posts/{postId}/unlike`

Response `200 OK`: `LikePostResponse`

### Save Post

`POST /api/posts/{postId}/save`

Save a post for the authenticated user. This endpoint is idempotent; saving an already saved post still returns `saved: true`.

Headers: `Authorization: Bearer <accessToken>`

Response `200 OK`: `SavePostResponse`

```json
{
  "postId": 10,
  "saved": true
}
```

### Comment On Post

`POST /api/posts/{postId}/comments`

Request body: `CommentRequest`

```json
{
  "content": "Nice post"
}
```

Response `200 OK`: `CommentModel`

Important fields: `id`, `content`, `owner`, `post`, `createdAt`, `updatedAt`.

### Delete Comment

`DELETE /api/posts/{postId}/comments/{commentId}`

Response `204 No Content`.

### List Post Comments

`GET /api/posts/{postId}/comments?page={page}&pageSize={pageSize}`

Response `200 OK`: `PaginationResponse<CommentModel>`

## Upload

### Create Presigned Upload URLs

`POST /api/upload`

Request body: `UploadFileRequest`

```json
{
  "folderName": "posts",
  "files": 2
}
```

Response `200 OK`: `UploadResponse`

```json
{
  "items": [
    {
      "objectName": "posts/file-name.jpg",
      "url": "https://storage.example.com/presigned-url"
    }
  ]
}
```

## Locations

### List Provinces

`GET /api/locations/provinces`

Response `200 OK`: `List<AdminProvinceResponse>`

```json
[
  {
    "id": 1,
    "name": "Ha Noi",
    "codename": "ha_noi",
    "divisionType": "thanh pho trung uong",
    "phoneCode": 24,
    "image": "https://cdn.example.com/hanoi.jpg"
  }
]
```

### List Districts By Province

`GET /api/locations/provinces/{provinceId}/districts`

Response `200 OK`: `List<AdminDistrictResponse>`

### List Wards By District

`GET /api/locations/districts/{districtId}/wards`

Response `200 OK`: `List<AdminWardResponse>`

## Travel Places

### List Places

`GET /api/places?page={page}&pageSize={pageSize}&provinceId={provinceId}&keyword={keyword}`

Query parameters:

| Name | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `page` | int | no | `0` | Zero-based page number. |
| `pageSize` | int | no | `10` | Page size. |
| `provinceId` | Long | no | none | Filter by province. |
| `keyword` | String | no | none | Search by place keyword. |

Response `200 OK`: `PaginationResponse<TravelPlaceListItemResponse>`

### Recommended Places

`GET /api/places/recommendations?page={page}&pageSize={pageSize}&provinceId={provinceId}`

Return personalized place recommendations for the authenticated user.

Response `200 OK`: `PaginationResponse<TravelPlaceListItemResponse>`

### Get Place Detail

`GET /api/places/{placeId}`

Return place detail. Also records/uses optional current user for `myReview`.

Response `200 OK`: `TravelPlaceDetailResponse`

```json
{
  "id": 1,
  "name": "Ba Na Hills",
  "description": "Tourist destination",
  "lat": 16.0,
  "lon": 108.0,
  "views": 100,
  "openingTime": "08:00-17:00",
  "province": {
    "id": 48,
    "name": "Da Nang",
    "image": "https://cdn.example.com/danang.jpg"
  },
  "images": [
    {
      "id": 1,
      "imageUrl": "https://cdn.example.com/place.jpg",
      "main": true
    }
  ],
  "reviewSummary": {
    "averageRating": 4.5,
    "reviewCount": 20
  },
  "myReview": null
}
```

### List Place Reviews

`GET /api/places/{placeId}/reviews?page={page}&pageSize={pageSize}`

Response `200 OK`: `PaginationResponse<TravelPlaceReviewResponse>`

### Create Or Update My Place Review

`PUT /api/places/{placeId}/review`

Request body: `UpsertTravelPlaceReviewRequest`

```json
{
  "rating": 5,
  "content": "Worth visiting"
}
```

Validation: `rating` must be from `1` to `5`; `content` is required and max 5000 chars.

Response `200 OK`: `TravelPlaceReviewResponse`

## Admin Places

### Get Place Detail For Admin

`GET /api/admin/places/{placeId}`

Response `200 OK`: `TravelPlaceDetailResponse`

### Create Place

`POST /api/admin/places`

Request body: `UpsertTravelPlaceRequest`

```json
{
  "provinceId": 48,
  "name": "Ba Na Hills",
  "description": "Tourist destination",
  "lat": 16.0,
  "lon": 108.0,
  "openingTime": "08:00-17:00",
  "imageUrls": ["https://cdn.example.com/place.jpg"]
}
```

Validation: `provinceId` and `name` are required; `description` max 5000 chars; `openingTime` max 255 chars.

Response `201 Created`: `TravelPlaceDetailResponse`

### Update Place

`PUT /api/admin/places/{placeId}`

Request body: `UpsertTravelPlaceRequest`

Response `200 OK`: `TravelPlaceDetailResponse`

## Place View History

### Get My Place View History

`GET /api/users/me/place-view-history?page={page}&pageSize={pageSize}`

Response `200 OK`: `PaginationResponse<TravelPlaceViewHistoryResponse>`

```json
{
  "placeId": 1,
  "placeName": "Ba Na Hills",
  "mainImage": "https://cdn.example.com/place.jpg",
  "provinceName": "Da Nang",
  "viewedAt": "2026-05-20T10:30:00Z"
}
```

## Trips

### Dashboard

`GET /api/users/me/dashboard`

Return active, upcoming, and past trips for the authenticated user.

Response `200 OK`: `TripDashboardResponse`

```json
{
  "activeTrip": {
    "tripId": 1,
    "name": "Da Nang Trip",
    "location": "Da Nang",
    "coverImageUrl": "https://cdn.example.com/trip.jpg",
    "startDate": "2026-05-20",
    "endDate": "2026-05-23"
  },
  "upcomingTrips": [
    {
      "tripId": 2,
      "name": "Hue Trip",
      "location": "Hue",
      "coverImageUrl": "https://cdn.example.com/hue.jpg",
      "daysLeft": 5,
      "memberCount": 4
    }
  ],
  "pastTrips": [
    {
      "tripId": 3,
      "locationName": "Sa Pa",
      "dateString": "Jan 2026",
      "imageUrl": "https://cdn.example.com/sapa.jpg"
    }
  ]
}
```

### Get Trip Detail

`GET /api/trips/{tripId}`

Response `200 OK`: `TripDetailResponse`

```json
{
  "tripInfo": {
    "id": 1,
    "name": "Da Nang Trip",
    "location": "Da Nang",
    "coverImageUrl": "https://cdn.example.com/trip.jpg",
    "description": "Summer trip",
    "startDate": "2026-05-20",
    "endDate": "2026-05-23",
    "budgetMin": 1000000,
    "budgetMax": 3000000,
    "status": "ONGOING",
    "inviteCode": "ABCDEFGH",
    "maxMembers": null
  },
  "myRole": "LEADER",
  "members": [
    {
      "userId": 1,
      "name": "Minh Quang",
      "avatarUrl": "https://cdn.example.com/avatar.jpg",
      "role": "LEADER"
    }
  ],
  "highlights": {
    "topExpense": {
      "title": "Hotel",
      "amount": 1200000
    },
    "winningPoll": {
      "title": "Visit beach",
      "votesCount": 3
    }
  },
  "recentActivities": [
    {
      "type": "ADD_EXPENSE",
      "description": "expense added",
      "actorName": "Minh Quang",
      "createdAt": "2026-05-20T10:30:00Z"
    }
  ]
}
```

`TripStatus` values: `PLANNING`, `UPCOMING`, `ONGOING`, `COMPLETED`.

### Create Trip

`POST /api/trips`

Create a trip. Trip days are not created here; they are created lazily when a trip activity is created for a date.

Request body: `CreateTripRequest`

```json
{
  "name": "Da Nang Trip",
  "destination": "Da Nang",
  "startDate": "2026-06-01",
  "endDate": "2026-06-05",
  "budgetMin": 1000000,
  "budgetMax": 3000000
}
```

Response `201 Created`: `TripDetailResponse` for the newly created trip.

### Update Trip

`PUT /api/trips/{tripId}`

Request body: `UpdateTripRequest`

```json
{
  "name": "Updated Trip",
  "destination": "Da Nang",
  "startDate": "2026-06-01",
  "endDate": "2026-06-05",
  "budgetMin": 1000000,
  "budgetMax": 3500000
}
```

Response `204 No Content`.

### Delete Trip

`DELETE /api/trips/{tripId}`

Response `204 No Content`.

### Join Trip By Invite Code

`POST /api/trips/join`

Request body: `JoinTripRequest`

```json
{
  "inviteCode": "ABCDEFGH"
}
```

Validation: invite code must be exactly 8 characters.

Response `201 Created`: `JoinTripResultResponse`

```json
{
  "tripId": 1,
  "status": "PENDING",
  "message": "Join request sent"
}
```

### Get Trip Invite Code

`GET /api/trips/{tripId}/invite-code`

Response `200 OK`: `TripInviteCodeResponse`

```json
{
  "inviteCode": "ABCDEFGH",
  "inviteLink": "travelhub://trips/join?code=ABCDEFGH",
  "expiredAt": "2026-05-27T10:30:00"
}
```

### Regenerate Trip Invite Code

`POST /api/trips/{tripId}/invite-code/regenerate`

Response `200 OK`: `TripInviteCodeResponse`

## Trip Days And Activities

### List Trip Days

`GET /api/trips/{tripId}/days`

Return all existing trip days and their activities. Only days that already have/had activities are stored.

Response `200 OK`: `List<TripDayResponse>`

```json
[
  {
    "id": 1,
    "tripId": 1,
    "date": "2026-06-01",
    "dayNumber": 1,
    "activities": [
      {
        "id": 10,
        "tripDayId": 1,
        "title": "Breakfast",
        "description": "Local food",
        "startTime": "08:00:00",
        "endTime": "09:00:00",
        "locationName": "Market",
        "address": "Da Nang",
        "type": "FOOD",
        "orderIndex": 1
      }
    ]
  }
]
```

### Create Trip Activity

`POST /api/trips/{tripId}/activities`

Create an activity on a date. If `trip_days` does not contain a row for `(tripId, date)`, the API creates it first.

Request body: `CreateTripActivityRequest`

```json
{
  "date": "2026-06-01",
  "title": "Breakfast",
  "description": "Local food",
  "startTime": "08:00:00",
  "endTime": "09:00:00",
  "locationName": "Market",
  "address": "Da Nang",
  "type": "FOOD",
  "orderIndex": 1
}
```

Validation: `date` and `title` are required. Date must be within trip start/end dates when those dates exist.

Response `201 Created`: `TripActivityResponse`

### Update Trip Activity

`PUT /api/trips/{tripId}/activities/{activityId}`

Update an activity. If the new date has no `TripDay`, the API creates the target day.

Request body: `UpdateTripActivityRequest`

Response `200 OK`: `TripActivityResponse`

### Delete Trip Activity

`DELETE /api/trips/{tripId}/activities/{activityId}`

Delete an activity. If its trip day becomes empty, the API deletes the empty `TripDay`.

Response `204 No Content`.

## Trip Members

### List Join Requests

`GET /api/trips/{tripId}/requests`

Response `200 OK`: `List<TripJoinRequestResponse>`

```json
[
  {
    "userId": 2,
    "name": "Lan",
    "avatarUrl": "https://cdn.example.com/lan.jpg",
    "requestedAt": "2026-05-20T10:30:00Z"
  }
]
```

### Approve Join Request

`POST /api/trips/{tripId}/requests/{userId}/approve`

Response `204 No Content`.

### Reject Join Request

`POST /api/trips/{tripId}/requests/{userId}/reject`

Response `204 No Content`.

### Remove Member

`DELETE /api/trips/{tripId}/members/{userId}`

Response `204 No Content`.

### Leave Trip

`POST /api/trips/{tripId}/leave`

Response `204 No Content`.

### Update Member Role

`PUT /api/trips/{tripId}/members/{userId}/role`

Request body: `UpdateTripMemberRoleRequest`

```json
{
  "role": "MEMBER"
}
```

`role` values: `LEADER`, `MEMBER`.

Response `200 OK`: `TripMemberResponse`

## Trip Polls

### List Polls

`GET /api/trips/{tripId}/polls`

Response `200 OK`: `List<TripPollResponse>`

```json
[
  {
    "id": 1,
    "title": "Where to eat?",
    "category": "RESTAURANT",
    "votesCount": 2,
    "isWinning": true,
    "hasVoted": false,
    "voters": ["Minh Quang", "Lan"]
  }
]
```

`category` values: `PLACE`, `RESTAURANT`, `ACTIVITY`.

### Create Poll

`POST /api/trips/{tripId}/polls`

Request body: `CreateTripPollRequest`

```json
{
  "title": "Where to eat?",
  "category": "RESTAURANT"
}
```

Response `201 Created`: `TripPollResponse`

### Toggle Vote

`POST /api/trips/{tripId}/polls/{pollId}/vote`

Toggle current user's vote for a poll.

Response `200 OK`: `List<TripPollResponse>`

### Update Poll

`PUT /api/trips/{tripId}/polls/{pollId}`

Request body: `UpdateTripPollRequest`

```json
{
  "title": "Updated poll",
  "category": "ACTIVITY"
}
```

Response `200 OK`: `TripPollResponse`

### Close Poll

`POST /api/trips/{tripId}/polls/{pollId}/close`

Response `200 OK`: `TripPollResponse`

### Delete Poll

`DELETE /api/trips/{tripId}/polls/{pollId}`

Response `204 No Content`.

## Trip Expenses

### List Expenses

`GET /api/trips/{tripId}/expenses`

Response `200 OK`: `TripExpenseResponse`

```json
{
  "summary": {
    "totalAmount": 1500000,
    "perPersonAmount": 500000,
    "myBalance": 100000
  },
  "contributions": [
    {
      "userId": 1,
      "userName": "Minh Quang",
      "avatarUrl": "https://cdn.example.com/avatar.jpg",
      "amountPaid": 600000,
      "percentage": 40
    }
  ],
  "transactions": [
    {
      "id": 1,
      "title": "Hotel",
      "category": "STAY",
      "paidByUserId": 1,
      "paidByName": "Minh Quang",
      "amount": 1200000,
      "date": "2026-05-20T10:30:00Z"
    }
  ]
}
```

`TripExpenseCategory` values: `FOOD`, `STAY`, `TRANSPORT`, `ENTRY`.

### Add Expense

`POST /api/trips/{tripId}/expenses`

Request body: `CreateTripExpenseRequest`

```json
{
  "title": "Hotel",
  "amount": 1200000,
  "category": "STAY",
  "paidByUserId": 1
}
```

Validation: `title`, `amount`, `category`, and `paidByUserId` are required; `amount` must be positive.

Response `201 Created`: `TripExpenseTransactionResponse`

### Update Expense

`PUT /api/trips/{tripId}/expenses/{expenseId}`

Request body: `UpdateTripExpenseRequest`

Response `200 OK`: `TripExpenseTransactionResponse`

### Delete Expense

`DELETE /api/trips/{tripId}/expenses/{expenseId}`

Response `204 No Content`.

## Schema Reference

### Request DTOs

| DTO | Fields |
| --- | --- |
| `RegisterRequest` | `email: String`, `username: String`, `password: String` |
| `LoginRequest` | `email: String`, `password: String` |
| `AddDeviceTokenRequest` | `token: String` |
| `CreatePostRequest` | `description: String`, `imageUrls: List<String>`, `travelPlaceId: Long` |
| `ModifyPostRequest` | `description: String` |
| `CommentRequest` | `content: String` |
| `UploadFileRequest` | `folderName: String`, `files: int` |
| `UpdateProfileRequest` | `name`, `username`, `bio`, `email`, `phoneNumber`, `dateOfBirth`, `avatarUrl`, `location` |
| `PreferenceUpdateRequest` | `trip_type: String`, `interests: List<String>`, `destination: String`, `isOnboarded: Boolean` |
| `UpsertTravelPlaceRequest` | `provinceId: Long`, `name: String`, `description: String`, `lat: Double`, `lon: Double`, `openingTime: String`, `imageUrls: List<String>` |
| `UpsertTravelPlaceReviewRequest` | `rating: Integer`, `content: String` |
| `CreateTripRequest` | `name: String`, `destination: String`, `startDate: LocalDate`, `endDate: LocalDate`, `budgetMin: Double`, `budgetMax: Double` |
| `UpdateTripRequest` | `name: String`, `destination: String`, `startDate: LocalDate`, `endDate: LocalDate`, `budgetMin: BigDecimal`, `budgetMax: BigDecimal` |
| `JoinTripRequest` | `inviteCode: String` |
| `CreateTripActivityRequest` | `date: LocalDate`, `title: String`, `description: String`, `startTime: LocalTime`, `endTime: LocalTime`, `locationName: String`, `address: String`, `type: String`, `orderIndex: Integer` |
| `UpdateTripActivityRequest` | Same fields as `CreateTripActivityRequest` |
| `UpdateTripMemberRoleRequest` | `role: TripMemberRole` |
| `CreateTripPollRequest` | `title: String`, `category: TripPollCategory` |
| `UpdateTripPollRequest` | `title: String`, `category: TripPollCategory` |
| `CreateTripExpenseRequest` | `title: String`, `amount: BigDecimal`, `category: TripExpenseCategory`, `paidByUserId: Long` |
| `UpdateTripExpenseRequest` | Same fields as `CreateTripExpenseRequest` |

### Response DTOs

| DTO | Fields |
| --- | --- |
| `AuthResponse` | `accessToken`, `refreshToken`, `userId`, `isOnboarded` |
| `UserProfileResponse` | `id`, `avatarUrl`, `name`, `username`, `bio`, `email`, `phoneNumber`, `dateOfBirth`, `gender`, `location`, `followersCount`, `followingCount`, `postsCount`, `isFollowing` |
| `UserFollowResponse` | `id`, `name`, `username`, `avatarUrl`, `following` |
| `PreferenceResponse` | `user_id`, `trip_type`, `interests`, `destination`, `updated_at`, `isOnboarded` |
| `PostResponse` | `id`, `description`, `imageUrls`, `owner`, `likeCount`, `commentCount`, `isLiked`, `travelPlaceId`, `location`, `createdAt`, `updatedAt` |
| `LikePostResponse` | `postId`, `liked`, `likeCount` |
| `SavePostResponse` | `postId`, `saved` |
| `UploadResponse` | `items: List<UploadModel>` |
| `UploadModel` | `objectName`, `url` |
| `AdminProvinceResponse` | `id`, `name`, `codename`, `divisionType`, `phoneCode`, `image` |
| `AdminDistrictResponse` | `id`, `provinceId`, `name`, `codename`, `divisionType` |
| `AdminWardResponse` | `id`, `districtId`, `provinceId`, `name`, `codename`, `divisionType` |
| `TravelPlaceListItemResponse` | `id`, `name`, `description`, `province`, `mainImage`, `views`, `openingTime`, `averageRating`, `reviewCount` |
| `TravelPlaceDetailResponse` | `id`, `name`, `description`, `lat`, `lon`, `views`, `openingTime`, `province`, `images`, `reviewSummary`, `myReview` |
| `TravelPlaceReviewResponse` | `id`, `user`, `rating`, `content`, `createdAt`, `updatedAt` |
| `TravelPlaceViewHistoryResponse` | `placeId`, `placeName`, `mainImage`, `provinceName`, `viewedAt` |
| `TripDashboardResponse` | `activeTrip`, `upcomingTrips`, `pastTrips` |
| `TripDetailResponse` | `tripInfo`, `myRole`, `members`, `highlights`, `recentActivities` |
| `TripInfoResponse` | `id`, `name`, `location`, `coverImageUrl`, `description`, `startDate`, `endDate`, `budgetMin`, `budgetMax`, `status`, `inviteCode`, `maxMembers` |
| `TripInviteCodeResponse` | `inviteCode`, `inviteLink`, `expiredAt` |
| `JoinTripResultResponse` | `tripId`, `status`, `message` |
| `TripDayResponse` | `id`, `tripId`, `date`, `dayNumber`, `activities` |
| `TripActivityResponse` | `id`, `tripDayId`, `title`, `description`, `startTime`, `endTime`, `locationName`, `address`, `type`, `orderIndex` |
| `TripJoinRequestResponse` | `userId`, `name`, `avatarUrl`, `requestedAt` |
| `TripMemberResponse` | `userId`, `name`, `avatarUrl`, `role` |
| `TripPollResponse` | `id`, `title`, `category`, `votesCount`, `isWinning`, `hasVoted`, `voters` |
| `TripExpenseResponse` | `summary`, `contributions`, `transactions` |
| `TripExpenseTransactionResponse` | `id`, `title`, `category`, `paidByUserId`, `paidByName`, `amount`, `date` |
| `NotificationModel` | `title`, `body`, `isRead`, `type`, `targetId`, `createdAt` |

### Enum Values

| Enum | Values |
| --- | --- |
| `TripStatus` | `PLANNING`, `UPCOMING`, `ONGOING`, `COMPLETED` |
| `TripRole` | `LEADER`, `MEMBER`, `PENDING`, `NON_MEMBER` |
| `TripMemberRole` | `LEADER`, `MEMBER` |
| `TripMemberStatus` | `PENDING`, `ACTIVE`, `REJECTED` |
| `TripPollCategory` | `PLACE`, `RESTAURANT`, `ACTIVITY` |
| `TripExpenseCategory` | `FOOD`, `STAY`, `TRANSPORT`, `ENTRY` |
| `NotificationType` | `COMMENT`, `LIKE`, `FOLLOW` |
