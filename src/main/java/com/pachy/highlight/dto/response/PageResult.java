package com.pachy.highlight.dto.response;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
public class PageResult<T> extends Response {
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private List<T> list;

    public PageResult() {
        super(HttpStatus.OK);
    }

    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        PageResult<T> r = new PageResult<>();
        r.setPage(page.getNumber());
        r.setSize(page.getSize());
        r.setTotal(page.getTotalElements());
        r.setTotalPages(page.getTotalPages());
        r.setList(page.getContent().stream().map(mapper).toList());
        return r;
    }
}
