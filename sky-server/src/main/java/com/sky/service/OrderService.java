package com.sky.service;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    // ================= 用户端：历史订单模块（day09 新功能） =================

    /**
     * 用户端：历史订单分页查询
     * @param ordersPageQueryDTO 分页+查询条件（page/pageSize/status）
     * @return 分页结果
     */
    PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单详情（用户端/管理端共用）
     * @param id 订单id
     * @return 订单详情（含订单明细）
     */
    OrderVO details(Long id);

    /**
     * 用户端：取消订单
     * @param id 订单id
     */
    void userCancelById(Long id);

    /**
     * 用户端：再来一单（把历史订单的菜品重新加入购物车）
     * @param id 订单id
     */
    void repetition(Long id);

    /**
     * 用户端：催单
     * @param id 订单id
     */
    void reminder(Long id);

    // ================= 管理端：订单管理模块（day09 新功能） =================

    /**
     * 管理端：订单搜索（条件分页查询）
     * @param ordersPageQueryDTO 分页+查询条件
     * @return 分页结果
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端：各个状态的订单数量统计
     * @return 统计结果（待接单/待派送/派送中）
     */
    OrderStatisticsVO statistics();

    /**
     * 管理端：接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 管理端：拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 管理端：取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 管理端：派送订单
     * @param id 订单id
     */
    void delivery(Long id);

    /**
     * 管理端：完成订单
     * @param id 订单id
     */
    void complete(Long id);

}

