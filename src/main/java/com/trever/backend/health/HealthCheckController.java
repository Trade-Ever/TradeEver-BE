package com.trever.backend.health;


import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name="HealthCheck", description = "HealthCheck 관련 API 입니다.")
public class HealthCheckController {

    // 응답 시 데이터 반환 없이 응답코드, 응답 메세지만 보낼때
    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {

        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }

    // 응답 시 데이터 반환 과 함께 응답코드, 응답 메세지를 보낼때
    @GetMapping("/health-check-data")
    public ResponseEntity<ApiResponse<String>> healthCheckData() {


        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, "OK");
    }

    // 예외처리 예제
    @GetMapping("/exception-test/{data}")
    public ResponseEntity<ApiResponse<Void>> healthCheckData(@PathVariable("data") String data) {

        if (data.equals("run")){
            // 커스텀 예외처리(BadRequstException) 사용방법 및 ErrorStatus 사용방법
            throw new BadRequestException(ErrorStatus.VALIDATION_REQUEST_MISSING_EXCEPTION.getMessage());
        }

        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }
}
