package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 用户端历史订单分页查询（动态SQL）
     * 查询条件：userId（必填）、status（可空）
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> userPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端订单搜索分页查询（动态SQL）
     * 查询条件：number、phone、status、beginTime、endTime（均可空）
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * 实现方式二选一
     * 1. 加 @Select("select * from orders where id = #{id}") 注解
     * 2. 在 OrderMapper.xml 中编写 <select id="getById">
     *
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     *
     * @param status 订单状态
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countByStatus(Integer status);

    /**
     * 根据状态和订单时间查询订单
     *
     * @param status
     * @param orderTime
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 指定时间的营业额统计
     *
     * @param map
     * @return
     */
    @Select("select sum(amount) from orders  where order_time >= #{start} and order_time < #{last} and status = #{status}")
    Double sumByMap(HashMap<String, Object> map);

    /**
     * 指定时间的订单数统计
     *
     * @param map
     * @return
     */
    Integer countByMap(HashMap map);
}
