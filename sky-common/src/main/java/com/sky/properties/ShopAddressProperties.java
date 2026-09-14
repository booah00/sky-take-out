package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 门店信息配置类（day09 已有功能优化）
 *
 * 对应 application.yml 中的 sky.shop 配置项，
 * 用于配送范围校验：下单时计算"收货地址"与"门店地址"的距离
 */
@Component
@ConfigurationProperties(prefix = "sky.shop")
@Data
public class ShopAddressProperties {

    //商家门店地址（配置在 application.yml 中）
    private String address;

}
