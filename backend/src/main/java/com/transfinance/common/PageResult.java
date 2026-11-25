package com.transfinance.common;

import java.util.List;

/**
 * 前端分页表格使用的统一分页结果。
 *
 * @param records 当前页数据
 * @param total   总记录数
 * @param page    当前页码
 * @param size    每页数量
 * @param <T>     列表数据类型
 */
public record PageResult<T>(List<T> records, Long total, Long page, Long size) {
}
