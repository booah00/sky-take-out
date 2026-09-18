package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
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

    @Autowired
    private UserMapper userMapper;


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

    /**
     * 指定时间区域的用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        LocalDate date = begin;
        while (date.isBefore(end)) {
            date = date.plusDays(1);
            dateList.add(date);
        }

        String dateStr = StringUtils.join(dateList, ",");

        List<Integer> totalUserCountList = new ArrayList<>();
        List<Integer> newUserCountList = new ArrayList<>();

        for (LocalDate d : dateList) {
            //营业额指状态为：“已完成” 的订单
            LocalDateTime start = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime last = LocalDateTime.of(d, LocalTime.MAX);

            HashMap nmap = new HashMap();
            nmap.put("start", start);
            nmap.put("last", last);

            HashMap tmap = new HashMap();
            tmap.put("last", last);

            Integer newUserCount = userMapper.countByMap(nmap);
            Integer totalUserCount = userMapper.countByMap(tmap);
            if (totalUserCount == null) totalUserCount = 0;
            if (newUserCount == null) newUserCount = 0;

            totalUserCountList.add(totalUserCount);
            newUserCountList.add(newUserCount);
        }

        String userCountStr = StringUtils.join(totalUserCountList, ",");
        String newUserCountStr = StringUtils.join(newUserCountList, ",");

        return UserReportVO.builder()
                .dateList(dateStr)
                .totalUserList(userCountStr)
                .newUserList(newUserCountStr)
                .build();

    }


    /**
     * 指定时间区域的订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //获取从begin到end的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        LocalDate date = begin;
        while (date.isBefore(end)) {
            date = date.plusDays(1);
            dateList.add(date);
        }

        String dateStr = StringUtils.join(dateList, ",");

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;

        for (LocalDate d : dateList){
            LocalDateTime start = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime last = LocalDateTime.of(d, LocalTime.MAX);

            HashMap tmap = new HashMap();
            tmap.put("start", start);
            tmap.put("last", last);


            HashMap vmap = new HashMap();
            vmap.put("start", start);
            vmap.put("last", last);
            vmap.put("status", Orders.COMPLETED);

            Integer totalOrder = orderMapper.countByMap(tmap);
            Integer validOrder = orderMapper.countByMap(vmap);

            if (totalOrder == null) totalOrder = 0;
            if (validOrder == null) validOrder = 0;

            totalOrderCount += totalOrder;
            validOrderCount += validOrder;

            orderCountList.add(totalOrder);
            validOrderCountList.add(validOrder);
        }
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(dateStr)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }
}
