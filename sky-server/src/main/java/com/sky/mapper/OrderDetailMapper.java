package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询订单明细
     * TODO：在 OrderDetailMapper.xml 中编写对应 <select>
     * @param orderId 订单id
     * @return 订单明细列表
     */
    List<OrderDetail> getByOrderId(Long orderId);
}
