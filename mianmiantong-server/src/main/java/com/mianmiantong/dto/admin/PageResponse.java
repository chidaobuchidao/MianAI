package com.mianmiantong.dto.admin;

import java.util.List;

/** 分页列表响应。page/pageSize 回显本次查询使用的分页参数。 */
public record PageResponse<T>(List<T> list, long total, int page, int pageSize) {

    public PageResponse {
        list = List.copyOf(list);
    }
}
