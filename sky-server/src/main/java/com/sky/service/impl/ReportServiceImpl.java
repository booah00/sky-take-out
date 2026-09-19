package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;


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

        for (LocalDate d : dateList) {
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


    /**
     * 指定时间区域内的Top10畅销菜品
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime start = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime last = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(start, last);

        //stream流拼接字符串
        List<String> name = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(name, ",");

        List<Integer> number = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(number, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();

    }

    /**
     * 导出运营数据报表
     *
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库数据
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDateTime startTime = LocalDateTime.of(beginDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(startTime, endTime);

        //通过POI写入数据
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            sheet1.getRow(1).getCell(1).setCellValue("时间：" + beginDate + "至" + endDate);
            XSSFRow row4 = sheet1.getRow(3);
            row4.getCell(2).setCellValue(businessData.getTurnover());
            row4.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row4.getCell(6).setCellValue(businessData.getNewUsers());

            XSSFRow row5 = sheet1.getRow(4);
            row5.getCell(2).setCellValue(businessData.getValidOrderCount());
            row5.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = beginDate.plusDays(i);
                BusinessDataVO businessData1 = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow row = sheet1.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData1.getTurnover());
                row.getCell(3).setCellValue(businessData1.getValidOrderCount());
                row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData1.getUnitPrice());
                row.getCell(6).setCellValue(businessData1.getNewUsers());
            }


            //通过输出流将表格传给客户端
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            //关闭资源
            outputStream.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
