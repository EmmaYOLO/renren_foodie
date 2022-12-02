package com.foodie.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Emma_Lyy
 * @create 2022-11-30 22:22
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
