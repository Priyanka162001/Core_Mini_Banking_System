package in.bank.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private String status;        // SUCCESS / ERROR
    private String message;
    private T data;
    private String code;          // AUTH_200, AUTH_400
    private LocalDateTime timestamp;
}