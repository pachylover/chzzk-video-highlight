package com.pachy.highlight.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseData<T> extends Response {
    private T data;

    public ResponseData(HttpStatus status) {
        super(status);
    }

    public ResponseData(HttpStatus status, T data) {
        super(status);
        this.data = data;
    }
}
