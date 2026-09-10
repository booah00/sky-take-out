package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopAddressProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springfox.documentation.swagger.readers.operation.OpenApiResponseReader;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OpenApiResponseReader openApiResponseReader;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BaiduMapUtil baiduMapUtil;

    @Autowired
    private ShopAddressProperties shopAddressProperties;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理异常（地址簿为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());

        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //TODO (day09 已有功能优化) 配送范围校验（收货地址距门店 5 公里内）：
        // 1. 拼接收货地址完整字符串：addressBook.getProvinceName() + getCityName() + getDistrictName() + getDetail()
        // 2. baiduMapUtil.getLocation(shopAddressProperties.getAddress()) 获取门店经纬度
        // 3. baiduMapUtil.getLocation(完整收货地址) 获取收货地址经纬度
        // 4. baiduMapUtil.getDistance(...) 计算两地距离，若 > 5000 米
        //    则抛 new OrderBusinessException(MessageConstant.OUT_OF_DELIVERY_RANGE)


        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//使用时间戳作为订单号
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);


        //向订单明细表插入多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());

            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;

        log.info("跳过微信支付，支付成功");

        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    // ================= 用户端：历史订单模块（day09 新功能） =================

    /**
     * 用户端：历史订单分页查询
     *
     * @param ordersPageQueryDTO 分页+查询条件（page/pageSize/status）
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 1. 开启分页（page/pageSize 由 DTO 直接带入）
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        // 2. 设置 userId = BaseContext.getCurrentId()（只能查自己的订单）；status 前端已传（可空）
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        // 3. 调用 orderMapper.userPageQuery(ordersPageQueryDTO) 得到 Page<OrderVO>
        Page<OrderVO> records = orderMapper.userPageQuery(ordersPageQueryDTO);
        // 4. 遍历 records，把每条订单的菜品名称拼接为 orderDishes（如 "宫保鸡丁x2,鱼香肉丝x1"）
        for (OrderVO record : records) {
            List<OrderDetail> list = orderDetailMapper.getByOrderId(record.getId());
            StringBuilder orderDishes = new StringBuilder();
            if (orderDetailMapper != null) {
                record.setOrderDetailList(list);
                for (OrderDetail orderDetail : list) {
                    orderDishes.append(orderDetail.getName()).append("x").append(orderDetail.getNumber()).append(",");

                }
            }
            record.setOrderDishes(orderDishes.toString());
        }
        // 5. 返回 new PageResult(page.getTotal(), page.getResult())
        return new PageResult(records.getTotal(), records.getResult());
    }

    /**
     * 查询订单详情（用户端/管理端共用）
     *
     * @param id 订单id
     * @return 订单详情（含订单明细）
     */
    @Override
    public OrderVO details(Long id) {
        // 1. orderMapper.getById(id) 查询订单，查不到则抛 OrderBusinessException(MessageConstant.ORDER_NOT_FOUND)
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2. orderDetailMapper.getByOrderId(id) 查询订单明细列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 3. 将 Orders 属性拷贝到 OrderVO，再 setOrderDetailList(明细列表)
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        // 4. 返回 OrderVO
        return orderVO;
    }

    /**
     * 用户端：取消订单
     *
     * @param id 订单id
     */
    @Override
    public void userCancelById(Long id) {
        //TODO 用户端取消订单实现思路：
        // 1. orderMapper.getById(id) 查询订单，查不到抛 OrderBusinessException(MessageConstant.ORDER_NOT_FOUND)
        // 2. 校验状态：仅 待付款(1)/待接单(2) 可以取消，其他状态抛 OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)
        // 3. 若已支付(payStatus=1)，真实项目需调用微信退款接口（个人项目可跳过）
        // 4. 构建 Orders：status=CANCELLED(6)、cancelReason="用户取消"、cancelTime=now，调用 orderMapper.update(orders)
    }

    /**
     * 用户端：再来一单（把历史订单的菜品重新加入购物车）
     *
     * @param id 订单id
     */
    @Override
    public void repetition(Long id) {
        //TODO 再来一单实现思路：
        // 1. 根据 id 查询订单（校验存在）
        // 2. orderDetailMapper.getByOrderId(id) 查询该订单的明细列表
        // 3. 遍历明细，转换为 ShoppingCart 对象：id/orderId 置空、setUserId(BaseContext.getCurrentId())、setCreateTime(now)
        // 4. 批量插入购物车（ShoppingCartMapper 目前只有单条 insert，可循环调用；
        //    或仿照 OrderDetailMapper.insertBatch 自己加一个批量插入方法）
    }

    /**
     * 用户端：催单（提醒商家尽快接单/制作）
     *
     * @param id 订单id
     */
    @Override
    public void reminder(Long id) {
        //TODO 催单实现思路：
        // 1. 根据 id 查询订单，校验订单存在
        // 2. 本阶段最简单实现：log.info("用户催单，订单id：{}", id) 后直接返回即可
        // 3. 进阶实现：通过 WebSocket 向管理端推送催单消息（后续课程会讲，本阶段可不做）
    }

    // ================= 管理端：订单管理模块（day09 新功能） =================

    /**
     * 管理端：订单搜索（条件分页查询）
     *
     * @param ordersPageQueryDTO 分页+查询条件
     * @return 分页结果
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //TODO 管理端订单搜索实现思路：
        // 1. PageHelper.startPage(page, pageSize)
        // 2. orderMapper.conditionSearch(dto) 得到 Page<OrderVO>（注意：管理端查询不设 userId）
        // 3. 遍历 records 拼接 orderDishes 字符串
        // 4. 返回 new PageResult(page.getTotal(), page.getResult())
        return null;
    }

    /**
     * 管理端：各个状态的订单数量统计
     *
     * @return 统计结果
     */
    @Override
    public OrderStatisticsVO statistics() {
        //TODO 订单数量统计实现思路：
        // 1. 调用 orderMapper.countByStatus(Orders.TO_BE_CONFIRMED) 统计待接单数量
        // 2. 调用 orderMapper.countByStatus(Orders.CONFIRMED) 统计待派送数量
        // 3. 调用 orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS) 统计派送中数量
        // 4. 封装到 OrderStatisticsVO 并返回
        return null;
    }

    /**
     * 管理端：接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //TODO 接单实现思路：
        // 1. orderMapper.getById(id) 查询订单，校验存在
        // 2. 校验订单状态必须为 待接单(2)，否则抛 OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR)
        // 3. 构建 Orders：id、status=CONFIRMED(3)，调用 orderMapper.update(orders)
    }

    /**
     * 管理端：拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //TODO 拒单实现思路：
        // 1. 查询订单，校验存在
        // 2. 校验订单状态为 待接单(2)
        // 3. 若已支付需退款（个人项目可跳过）
        // 4. 构建 Orders：status=CANCELLED(6)、rejectionReason、cancelTime=now，调用 orderMapper.update(orders)
    }

    /**
     * 管理端：取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //TODO 管理端取消订单实现思路：
        // 1. 查询订单，校验存在
        // 2. 若已支付需退款（个人项目可跳过）
        // 3. 构建 Orders：status=CANCELLED(6)、cancelReason、cancelTime=now，调用 orderMapper.update(orders)
        // 提示：与用户端取消逻辑相似，可以抽取公共私有方法复用
    }

    /**
     * 管理端：派送订单
     *
     * @param id 订单id
     */
    @Override
    public void delivery(Long id) {
        //TODO 派送订单实现思路：
        // 1. 查询订单，校验存在
        // 2. 校验订单状态为 已接单(3)
        // 3. 构建 Orders：status=DELIVERY_IN_PROGRESS(4)，调用 orderMapper.update(orders)
    }

    /**
     * 管理端：完成订单
     *
     * @param id 订单id
     */
    @Override
    public void complete(Long id) {
        //TODO 完成订单实现思路：
        // 1. 查询订单，校验存在
        // 2. 校验订单状态为 派送中(4)
        // 3. 构建 Orders：status=COMPLETED(5)、deliveryTime=now，调用 orderMapper.update(orders)
    }


}
