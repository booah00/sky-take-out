package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;


    /**
     * 指定时间区域的营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //获取从begin到end的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        LocalDate date = begin;
        while (date.isBefore(end)) {
            date = date.plusDays(1);
            dateList.add(date);
        }

        //将日期转换为字符串
//        StringBuilder dateStr = new StringBuilder();
//        for (LocalDate d : dateList) {
//            dateStr.append(d.toString()).append(",");
//        }
        String dateStr = StringUtils.join(dateList, ",");

        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate d : dateList) {
            //营业额指状态为：“已完成” 的订单
            LocalDateTime start = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime last = LocalDateTime.of(d, LocalTime.MAX);

            HashMap map = new HashMap();
            map.put("start", start);
            map.put("last", last);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            if (turnover == null) turnover = 0.0;

            turnoverList.add(turnover);
        }

        String turnoverStr = StringUtils.join(turnoverList, ",");


        return TurnoverReportVO.builder()
                .dateList(dateStr)
                .turnoverList(turnoverStr)
                .build();
    }
}
