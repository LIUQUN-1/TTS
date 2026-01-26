package com.tts.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果DTO
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 构造分页结果
     */
    public PageResult(Integer page, Integer size, Long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = (int) Math.ceil((double) total / size);
    }

    /**
     * 空结果
     */
    public static <T> PageResult<T> empty(Integer page, Integer size) {
        return new PageResult<>(page, size, 0L, 0, List.of());
    }
}
