package com.sky.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 百度地图工具类（day09 已有功能优化）
 *
 * 用于配送范围校验，核心能力：
 *  1. 地理编码：根据地址获取经纬度
 *  2. 距离计算：计算收货地址与门店之间的距离
 *
 * 使用前提：
 *  1. 在 https://lbsyun.baidu.com/ 注册账号并实名认证
 *  2. 创建应用（应用类型选"服务端"）获取 AK
 *  3. 将 AK 配置到 application.yml 的 sky.baidu-map.ak
 *
 * 相关接口文档：
 *  地理编码：https://lbsyun.baidu.com/index.php?title=webapi/guide/webservice-geocoding
 *  路线规划：https://lbsyun.baidu.com/index.php?title=webapi/directionlite-v1
 */
@Component
public class BaiduMapUtil {

    //地理编码接口地址（地址 -> 经纬度）
    public static final String GEOCODING_URL = "https://api.map.baidu.com/geocoding/v3/";

    //路线规划接口地址（计算两点间距离，directionlite 为轻量版，个人开发免费额度够用）
    public static final String DIRECTION_URL = "https://api.map.baidu.com/directionlite/v1/driving";

    //百度地图 AK（在 application.yml 的 sky.baidu-map.ak 中配置）
    @Value("${sky.baidu-map.ak}")
    private String ak;

    /**
     * 根据地址获取经纬度
     *
     * @param address 具体地址
     * @return "经度,纬度" 字符串（经度在前、纬度在后）；失败返回 null
     *
     * TODO 实现思路：
     *  1. 构造请求参数 Map：ak(当前ak)、address、output=json
     *  2. 调用 HttpClientUtil.doGet(GEOCODING_URL, paramMap) 发送地理编码请求
     *  3. 使用 fastjson 解析返回的 JSON：status 为 0 表示成功
     *  4. 取出 result.location.lng（经度）、result.location.lat（纬度）
     *  5. 返回 "lng,lat"（注意百度经纬度是 经度在前，纬度在后）
     */
    public String getLocation(String address) {
        return null;
    }

    /**
     * 计算两个经纬度坐标点之间的距离
     *
     * @param lng1 起点经度
     * @param lat1 起点纬度
     * @param lng2 终点经度
     * @param lat2 终点纬度
     * @return 距离（米）
     *
     * TODO 实现思路（二选一）：
     *  方案一（推荐）：调用 DIRECTION_URL 驾车路线规划接口，解析 result.routes[0].distance（单位：米）
     *     参数：ak、origin="lat,lng"（起点，注意纬度在前）、destination="lat,lng"、output=json
     *  方案二：使用 Haversine 球面距离公式自行计算（近似直线距离）
     */
    public double getDistance(double lng1, double lat1, double lng2, double lat2) {
        return 0.0;
    }
}
