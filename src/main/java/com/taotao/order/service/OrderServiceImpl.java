package com.taotao.order.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.taotao.common.pojo.TaotaoResult;
import com.taotao.mapper.TbOrderItemMapper;
import com.taotao.mapper.TbOrderMapper;
import com.taotao.mapper.TbOrderShippingMapper;
import com.taotao.order.dao.JedisClient;
import com.taotao.order.pojo.OrderInfo;
import com.taotao.pojo.TbOrderItem;
import com.taotao.pojo.TbOrderShipping;

@Service
public class OrderServiceImpl implements OrderService{

	@Autowired
	private JedisClient jedisClient;
	
	@Autowired
	private TbOrderMapper tbOrderMapper;
	
	@Autowired
	private TbOrderShippingMapper tbOrderShippingMapper;
	
	@Autowired
	private TbOrderItemMapper tbOrderItemMapper;
	
	@Value("${REDIS_ORDER_GENKEY}")
	private String REDIS_ORDER_GENKEY ;
	
	@Value("${ORDER_ID_INIT}")
	private String ORDER_ID_INIT ;
	
	@Value("${REDIS_ORDER_DETAIL_GENKEY}")
	private String REDIS_ORDER_DETAIL_GENKEY ;
	
	// 创建订单---这里有三张表 订单表、订单明细表、物流表
	@Override
	public TaotaoResult createOrder(OrderInfo orderInfo) {
		
		// 1、插入订单表
		// 取出订单号
		String order = jedisClient.get(REDIS_ORDER_GENKEY);
		if (StringUtils.isBlank(order)) {
			// 设置初始值
			jedisClient.set(REDIS_ORDER_GENKEY, ORDER_ID_INIT);
		}
		Long orderId = jedisClient.incr(REDIS_ORDER_GENKEY);
		
		orderInfo.setOrderId(orderId + "");
		//状态：1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭
		orderInfo.setStatus(1);
		orderInfo.setCreateTime(new Date());
		orderInfo.setUpdateTime(new Date());
		// 插入表
		tbOrderMapper.insert(orderInfo) ;
		// 2、插入明细表
		List<TbOrderItem> orderItems = orderInfo.getOrderItems();
		for (TbOrderItem orderItem : orderItems) {
			// 生成订单明细id
			Long detailId = jedisClient.incr(REDIS_ORDER_DETAIL_GENKEY);
			orderItem.setId(detailId.toString());
			// 设置订单号
			orderItem.setOrderId(orderId.toString());
			// 插入
			tbOrderItemMapper.insert(orderItem) ;
		}
		// 3、插入物流表
		TbOrderShipping orderShipping = orderInfo.getOrderShipping();
		// 1、补全字段
		orderShipping.setOrderId(orderId.toString());
		orderShipping.setCreated(new Date());
		orderShipping.setUpdated(new Date());
		// 2、插入数据
		tbOrderShippingMapper.insert(orderShipping);
		// 返回TaotaoResult包装订单号。
		return TaotaoResult.ok(orderId);
	}

}
