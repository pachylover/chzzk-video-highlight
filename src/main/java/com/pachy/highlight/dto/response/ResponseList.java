package com.pachy.highlight.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseList<T> extends Response {
    private int count;
    private List<T> list;

    public ResponseList(HttpStatus status) {
        super(status);
    }
}
