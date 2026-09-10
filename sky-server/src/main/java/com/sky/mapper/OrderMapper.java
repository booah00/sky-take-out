package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 用户端历史订单分页查询（动态SQL）
     * 查询条件：userId（必填）、status（可空）
     * TODO：在 OrderMapper.xml 中编写对应 <select>，注意用 <where> 处理动态条件
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> userPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端订单搜索分页查询（动态SQL）
     * 查询条件：number、phone、status、beginTime、endTime（均可空）
     * TODO：在 OrderMapper.xml 中编写对应 <select>
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * TODO：实现方式二选一
     *  1. 加 @Select("select * from orders where id = #{id}") 注解
     *  2. 在 OrderMapper.xml 中编写 <select id="getById">
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * TODO：实现方式二选一
     *  1. 加 @Select("select count(*) from orders where status = #{status}") 注解
     *  2. 在 OrderMapper.xml 中编写 <select id="countByStatus">
     * @param status 订单状态
     * @return
     */
    Integer countByStatus(Integer status);

}
