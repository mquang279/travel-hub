-- Xoá toàn bộ dữ liệu cũ trong bảng users và các bảng phụ thuộc (follows), sau đó reset lại ID về 1
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- Thêm 5 dữ liệu mặc định mới (Bổ sung date_of_birth, gender, location, posts_count)
INSERT INTO users (username, name, email, avatar_url, bio, phone_number, followers_count, following_count, posts_count, date_of_birth, gender, location) VALUES
('nguyenvana', 'Nguyễn Văn A', 'nguyenvana@gmail.com', 'https://example.com/avatar1.jpg', 'Người đam mê du lịch và khám phá.', '0123456789', 2, 3, 0, '1995-05-15', 'Male', 'Hanoi, Vietnam'),
('tranthib', 'Trần Thị B', 'tranthib@gmail.com', 'https://example.com/avatar2.jpg', 'Thích đi phượt và thưởng thức ẩm thực.', '0987654321', 1, 1, 0, '1998-10-20', 'Female', 'Ho Chi Minh City, Vietnam'),
('leminhc', 'Lê Minh C', 'leminhc@gmail.com', 'https://example.com/avatar3.jpg', 'Nhiếp ảnh gia tự do.', '0369852147', 1, 0, 0, '1990-02-14', 'Male', 'Da Nang, Vietnam'),
('phamthid', 'Phạm Thị D', 'phamthid@gmail.com', 'https://example.com/avatar4.jpg', 'Food blogger nổi tiếng.', '0912345678', 1, 0, 0, '1999-11-11', 'Female', 'Hue, Vietnam'),
('hoangvane', 'Hoàng Văn E', 'hoangvane@gmail.com', 'https://example.com/avatar5.jpg', 'Yêu thích leo núi.', '0909090909', 0, 1, 0, '1985-07-07', 'Male', 'Sapa, Vietnam');

-- Thêm list follow nhau
-- id 1 (Nguyễn Văn A) follow 3 người: 2 (Trần Thị B), 3 (Lê Minh C), 4 (Phạm Thị D)
-- id 1 (Nguyễn Văn A) được 2 người follow lại: 2 (Trần Thị B), 5 (Hoàng Văn E)
INSERT INTO follows (follower_id, following_id) VALUES
(1, 2),
(1, 3),
(1, 4),
(2, 1),
(5, 1);

-- ensure posts_count is initialized
UPDATE users SET posts_count = 0;

-- ================================================== ===================
-- GỢI Ý CÁC API TƯƠNG ỨNG CHO TRANG PROFILE
-- http://localhost:8080/api/users/1
-- =====================================================================
-- 1. Xem thông tin Profile (gồm các trường mới: dateOfBirth, gender, location, isFollowing)
--    GET /api/users/{id}
--
-- 2. Xem danh sách những người đang theo dõi user này (Followers)
--    GET /api/users/{id}/followers
--
-- 3. Xem danh sách những người user này đang theo dõi (Following)
--    GET /api/users/{id}/following
--
-- 4. Chỉnh sửa thông tin cá nhân (Edit Profile)
--    PUT /api/users/{id}
--    Body JSON minh họa:
--    {
    -- {
    -- "avatarUrl": "https://example.com/avatar1.jpg",
    -- "bio": "Người đam mê du lịch.",
    -- "dateOfBirth": "1995-05-18",
    -- "email": "nguyenvana@gmail.com",
    -- "followersCount": 2,
    -- "following": false,
    -- "followingCount": 3,
    -- "gender": "Male",
    -- "id": 1,
    -- "location": "Hanoi, Vietnam",
    -- "name": "Nguyễn Văn A",
    -- "phoneNumber": "0123456789",
    -- "postsCount": 0,
    -- "username": "abc"
    -- }
--    }
--
-- ***Danh sách các hàm tuyến API (Controller methods):***
-- UserController:
--    getMyProfile()             => GET /api/users/me
--    getProfile(Long userId)    => GET /api/users/{userId}
--    updateMyProfile(...)       => PUT /api/users/me
--    updateUserProfile(...)     => PUT /api/users/{userId}
--    uploadAvatar(...)          => POST /api/users/me/avatar
-- FollowController:
--    getFollowers(...)          => GET /api/users/{userId}/followers
--    getFollowing(...)          => GET /api/users/{userId}/following
--    followUser(...)            => POST /api/users/{targetUserId}/follow
--    unfollowUser(...)          => DELETE /api/users/{targetUserId}/follow
--
-- ***Danh sách các hàm service/repository đã bổ sung:***
-- FollowUseCaseImpl:
--    followUser(Long currentUserId, Long targetUserId)
--    unfollowUser(Long currentUserId, Long targetUserId)
-- JpaUserRepository:
--    incrementFollowing(Long id)
--    decrementFollowing(Long id)
--    incrementFollowers(Long id)
--    decrementFollowers(Long id)
--    incrementPosts(Long id)
--    decrementPosts(Long id)
--
-- ***Các field mới trong UserEntity/UserProfileResponse:***
--    int postsCount
-- =====================================================================
-- 1A