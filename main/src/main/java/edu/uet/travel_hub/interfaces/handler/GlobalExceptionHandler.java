package edu.uet.travel_hub.interfaces.handler;

import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.InviteCodeGenerationException;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.domain.exception.EmailAlreadyExistsException;
import edu.uet.travel_hub.domain.exception.UsernameAlreadyExistsException;
import edu.uet.travel_hub.interfaces.dto.response.ApiErrorResponse;
import edu.uet.travel_hub.interfaces.dto.response.ApiErrorResponse.FieldErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau.";

    private static final Map<String, String> MESSAGE_TRANSLATIONS = Map.ofEntries(
            Map.entry("Avatar file must not be empty", "Tệp ảnh đại diện không được để trống."),
            Map.entry("Bank account is required to join trip", "Bạn cần cập nhật ngân hàng và số tài khoản trước khi tham gia chuyến đi."),
            Map.entry("Cannot access preferences of another user", "Bạn không có quyền truy cập tùy chọn của người dùng khác."),
            Map.entry("Cannot follow yourself", "Bạn không thể tự theo dõi chính mình."),
            Map.entry("Current leader not found", "Không tìm thấy trưởng nhóm hiện tại."),
            Map.entry("Current password is incorrect", "Mật khẩu hiện tại không đúng."),
            Map.entry("Current user id must not be null", "Thiếu thông tin người dùng hiện tại."),
            Map.entry("Current user is not authenticated", "Bạn cần đăng nhập để thực hiện thao tác này."),
            Map.entry("Current user is not leader", "Bạn không phải trưởng nhóm của chuyến đi này."),
            Map.entry("Current user not found", "Không tìm thấy người dùng hiện tại."),
            Map.entry("District not found", "Không tìm thấy quận/huyện."),
            Map.entry("Email already exists.", "Email đã được sử dụng."),
            Map.entry("Expense not found", "Không tìm thấy khoản chi."),
            Map.entry("Failed to generate unique invite code after 5 attempts", "Không thể tạo mã mời. Vui lòng thử lại sau."),
            Map.entry("Failed to upload avatar", "Không thể tải ảnh đại diện lên. Vui lòng thử lại sau."),
            Map.entry("Init MinIO bucket failed", "Không thể khởi tạo kho lưu trữ tệp."),
            Map.entry("Join request is not pending", "Yêu cầu tham gia không còn ở trạng thái chờ duyệt."),
            Map.entry("Join request not found", "Không tìm thấy yêu cầu tham gia."),
            Map.entry("Leader must transfer role before leaving", "Trưởng nhóm cần chuyển quyền trước khi rời chuyến đi."),
            Map.entry("Member is not active", "Thành viên chưa ở trạng thái hoạt động."),
            Map.entry("Member not found", "Không tìm thấy thành viên."),
            Map.entry("New password must be different from current password", "Mật khẩu mới phải khác mật khẩu hiện tại."),
            Map.entry("Only approved members can vote", "Chỉ thành viên đã được duyệt mới có thể bình chọn."),
            Map.entry("Only leader can perform this action", "Chỉ trưởng nhóm mới có thể thực hiện thao tác này."),
            Map.entry("Paid-by user must be an active member", "Người thanh toán phải là thành viên đang hoạt động."),
            Map.entry("Poll is closed", "Bình chọn đã đóng."),
            Map.entry("Poll not found", "Không tìm thấy bình chọn."),
            Map.entry("Post not found", "Không tìm thấy bài viết."),
            Map.entry("Post userId must not be null", "Bài viết thiếu thông tin người dùng."),
            Map.entry("Province not found", "Không tìm thấy tỉnh/thành phố."),
            Map.entry("Refresh token is invalid or expired", "Refresh token không hợp lệ hoặc đã hết hạn."),
            Map.entry("Target user not found", "Không tìm thấy người dùng cần thao tác."),
            Map.entry("Travel place not found", "Không tìm thấy địa điểm du lịch."),
            Map.entry("Trip activity not found", "Không tìm thấy hoạt động trong chuyến đi."),
            Map.entry("Trip member not found", "Không tìm thấy thành viên chuyến đi."),
            Map.entry("Trip not found", "Không tìm thấy chuyến đi."),
            Map.entry("User is not an active member", "Bạn không phải thành viên đang hoạt động của chuyến đi này."),
            Map.entry("User not found", "Không tìm thấy người dùng."),
            Map.entry("Username already exists.", "Username đã được sử dụng."),
            Map.entry("You do not have permission to delete this comment.", "Bạn không có quyền xóa bình luận này."));

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Không tìm thấy", localizeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Email đã tồn tại", localizeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Username đã tồn tại", localizeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Xung đột dữ liệu",
                resolveDataIntegrityMessage(ex), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Chưa xác thực", localizeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Đăng nhập thất bại",
                "Email hoặc mật khẩu không đúng.", request);
    }

    @ExceptionHandler({ ForbiddenTripActionException.class, AccessDeniedException.class })
    public ResponseEntity<ApiErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Không có quyền", localizeMessage(ex.getMessage(),
                "Bạn không có quyền thực hiện thao tác này."), request);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", localizeMessage(ex.getMessage()),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), localizeValidationMessage(
                        error.getDefaultMessage())))
                .toList();

        List<FieldErrorResponse> objectErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new FieldErrorResponse(error.getObjectName(), localizeValidationMessage(
                        error.getDefaultMessage())))
                .toList();

        List<FieldErrorResponse> errors = new java.util.ArrayList<>(fieldErrors);
        errors.addAll(objectErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", "Dữ liệu gửi lên không hợp lệ.",
                request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<FieldErrorResponse> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(),
                        localizeValidationMessage(violation.getMessage())))
                .toList();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", "Dữ liệu gửi lên không hợp lệ.",
                request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String parameter = Objects.requireNonNullElse(ex.getName(), "tham số");
        String message = "Tham số '" + parameter + "' không đúng định dạng.";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String message = "Thiếu tham số bắt buộc '" + ex.getParameterName() + "'.";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", message, request);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPathVariable(MissingPathVariableException ex,
            HttpServletRequest request) {
        String message = "Thiếu biến đường dẫn bắt buộc '" + ex.getVariableName() + "'.";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ",
                "Body của request không hợp lệ hoặc không đúng định dạng JSON.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ",
                "Phương thức HTTP này không được hỗ trợ cho endpoint hiện tại.", request);
    }

    @ExceptionHandler({ MultipartException.class, MaxUploadSizeExceededException.class })
    public ResponseEntity<ApiErrorResponse> handleMultipart(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Tệp tải lên không hợp lệ",
                "Tệp tải lên không hợp lệ hoặc vượt quá dung lượng cho phép.", request);
    }

    @ExceptionHandler(InviteCodeGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleInviteCodeGeneration(InviteCodeGenerationException ex,
            HttpServletRequest request) {
        log.error("Failed to generate invite code for path {}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống",
                localizeMessage(ex.getMessage(), INTERNAL_ERROR_MESSAGE), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for path {}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", INTERNAL_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String error, String message,
            HttpServletRequest request) {
        return buildErrorResponse(status, error, message, request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String error, String message,
            HttpServletRequest request, List<FieldErrorResponse> errors) {
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), error, message, request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(body);
    }

    private String localizeMessage(String message) {
        return localizeMessage(message, "Yêu cầu không hợp lệ.");
    }

    private String localizeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        String translated = MESSAGE_TRANSLATIONS.get(message);
        if (translated != null) {
            return translated;
        }
        if (message.startsWith("Email already exists:")) {
            return "Email đã được sử dụng.";
        }
        if (message.startsWith("Username already exists:")) {
            return "Username đã được sử dụng.";
        }
        if (message.startsWith("User not found with id:")) {
            return "Không tìm thấy người dùng.";
        }
        if (message.startsWith("Travel place not found with id:")) {
            return "Không tìm thấy địa điểm du lịch.";
        }
        if (message.startsWith("Follower not found with id:")) {
            return "Không tìm thấy người theo dõi.";
        }
        if (message.startsWith("Following user not found with id:")) {
            return "Không tìm thấy người dùng đang được theo dõi.";
        }
        if (message.endsWith(" is required")) {
            return "Thiếu thông tin bắt buộc: " + message.replace(" is required", "") + ".";
        }
        if (message.contains("duplicate key") || message.contains("unique constraint")) {
            return "Dữ liệu đã tồn tại.";
        }
        return message;
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = Objects.toString(ex.getMostSpecificCause().getMessage(), ex.getMessage());
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("email")) {
            return "Email đã được sử dụng.";
        }
        if (lowerMessage.contains("username")) {
            return "Username đã được sử dụng.";
        }

        return localizeMessage(message, "Dữ liệu đã tồn tại hoặc không hợp lệ.");
    }

    private String localizeValidationMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Giá trị không hợp lệ.";
        }
        return switch (message) {
            case "must not be blank" -> "Không được để trống.";
            case "must not be null" -> "Không được để trống.";
            case "must be greater than 0" -> "Phải lớn hơn 0.";
            default -> {
                if (message.startsWith("size must be between")) {
                    yield "Độ dài không nằm trong giới hạn cho phép.";
                }
                if (message.startsWith("size must be less than or equal to")) {
                    yield "Độ dài vượt quá giới hạn cho phép.";
                }
                if (message.startsWith("must be greater than or equal to")) {
                    yield "Giá trị nhỏ hơn mức tối thiểu cho phép.";
                }
                if (message.startsWith("must be less than or equal to")) {
                    yield "Giá trị lớn hơn mức tối đa cho phép.";
                }
                yield localizeMessage(message, "Giá trị không hợp lệ.");
            }
        };
    }
}
