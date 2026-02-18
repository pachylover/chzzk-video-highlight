package com.pachy.highlight.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private int resultCode;
    private String resultMsg;

    public Response(HttpStatus status) {
        this.resultCode = status.value();
        this.resultMsg = status.getReasonPhrase();
    }
}
