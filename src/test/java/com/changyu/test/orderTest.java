package com.changyu.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSON;
import com.changyu.foryou.model.Campus;
import com.changyu.foryou.model.Order;
import com.changyu.foryou.model.SmallOrder;
import com.changyu.foryou.model.TogetherOrder;
import com.changyu.foryou.service.CampusService;
import com.changyu.foryou.service.FoodService;
import com.changyu.foryou.service.OrderService;
import com.changyu.foryou.service.PushService;
import com.changyu.foryou.service.UserService;
import com.changyu.foryou.tools.Constants;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring.xml", "classpath:spring-mybatis.xml" })
public class orderTest { 
	private static final Logger LOGGER = Logger
			.getLogger(orderTest.class);
	private OrderService orderService;
	private FoodService foodService;
	private UserService userService;
	private CampusService campusService;   
	private PushService pushService;

	public CampusService getCampusService() {
		return campusService;
	}

	@Autowired
	public void setCampusService(CampusService campusService) {
		this.campusService = campusService;
	}

	public PushService getPushService() {
		return pushService;
	}

	@Autowired
	public void setPushService(PushService pushService) {
		this.pushService = pushService;
	}

	public OrderService getOrderService() {
		return orderService;
	}

	public FoodService getFoodService() {
		return foodService;
	}

	@Autowired
	public void setFoodService(FoodService foodService) {
		this.foodService = foodService;
	}

	public UserService getUserService() {
		return userService;
	}

	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	@Autowired
	public void setOrderService(OrderService orderService) {
		this.orderService = orderService;
	}


	/**
	 * 测试获取我的订单
	 */
	@Test
	public void getOrderInMine(){

		Map<String, Object> map = new HashMap<String, Object>();

		List<TogetherOrder> togetherOrdersList = new ArrayList<TogetherOrder>();

		try {
			Map<String, Object> paramMap = new HashMap<String, Object>();
			//case 1
			paramMap.put("phoneId", "18896554880");
			paramMap.put("status", 1);

			//case 2

			List<String> togetherIds = orderService.getTogetherId(paramMap);

			System.out.println(JSON.toJSONString(togetherIds));

			if (togetherIds.size() != 0) {
				for (String togetherId : togetherIds) {
					TogetherOrder togetherOrder = new TogetherOrder(); // 一单
					togetherOrder.setTogetherId(togetherId);

					paramMap.put("togetherId", togetherId);
					List<SmallOrder> orderList = orderService
							.getOrderListInMine(paramMap); // 一单里面的小订单
					togetherOrder.setSmallOrders(orderList);
					togetherOrder.setPayWay(orderList.get(0).getPayWay());
					Short totalStatus=0;
					if(orderList.get(0).getStatus()!=4)
					{
						totalStatus=orderList.get(0).getStatus();
					}
					else
					{
						totalStatus=5;
						for (int i = 0; i < orderList.size(); i++) {
							if (orderList.get(i).getIsRemarked() == 0) {
								totalStatus = 4;
							}
						}
					}
					togetherOrder.setStatus(totalStatus);
					togetherOrder.setTogetherDate(orderList.get(0)
							.getTogetherDate());
					togetherOrdersList.add(togetherOrder);
				}

				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "获取订单成功");
				map.put("orderList", JSON.parse(JSON
						.toJSONStringWithDateFormat(togetherOrdersList,
								"yyyy-MM-dd")));
			} else {
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "暂无订单");
			}

		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取订单失败");
		}

		LOGGER.debug("订单为:==================="+JSON.toJSONString(map));
		return ;
	}

	/**
	 * 测试立即购买
	 */
	@Test
	public void changeOrderStatus2Buy(){
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> paramMap = new HashMap<String, Object>();

		String orderId="1439889845846";
		String phoneId="18896554880";
		String rank="1427691760293";
		String reserveTime="立即送达";
		String message="苏大送达";
		Float totalPrice=1000.5f;
		Float price = 105f;
		
		Short payWay=1;

		Calendar calendar = Calendar.getInstance();

		String[] orderString = orderId.split(",");
		int flag = 0;
		String togetherId = phoneId + String.valueOf(new Date().getTime());

		paramMap.put("orderId",orderString[0]);
		paramMap.put("phoneId",phoneId);
		Campus campus=campusService.getCampus(paramMap);

		LOGGER.debug("campus is"+JSON.toJSONString(campus));
		//判断该校区是否正在营业
		if(campus.getStatus()==0){
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "该校区这段时间暂停营业哦");
			LOGGER.info(JSON.toJSONStringWithDateFormat(map,"H:m:s"));
			return ;
		}

		Calendar runOpenTime=Calendar.getInstance();
		runOpenTime.setTime(campus.getOpenTime());
		Calendar runCloseTime=Calendar.getInstance();
		runCloseTime.setTime(campus.getCloseTime());
		//判断是否超出校区营业时间

		int openHour=runOpenTime.get(Calendar.HOUR_OF_DAY);     
		int openMinute=runOpenTime.get(Calendar.MINUTE);
		int closeHour=runCloseTime.get(Calendar.HOUR_OF_DAY);
		int closeMinute=runCloseTime.get(Calendar.MINUTE);

		if(calendar.get(Calendar.HOUR_OF_DAY)>closeHour
				||(calendar.get(Calendar.HOUR_OF_DAY)==closeHour&&calendar.get(Calendar.MINUTE)>closeMinute)
				||calendar.get(Calendar.HOUR_OF_DAY)<openHour
				||(calendar.get(Calendar.HOUR_OF_DAY)==openHour&&calendar.get(Calendar.MINUTE)<runOpenTime.get(openMinute))
				){ 
			StringBuffer message2=new StringBuffer();
			message2.append("fou优的营业时间为"+openHour+":");

			if(openMinute<10){
				message2.append("0"+openMinute);
			}else{
				message2.append(openMinute);
			}
			message2.append("--"+closeHour+":");
			if(openMinute<10){
				message2.append("0"+closeMinute);
			}else{
				message2.append(closeMinute);
			}

			map.put(Constants.STATUS,Constants.FAILURE); 
			map.put(Constants.MESSAGE,message2.toString()); 
			LOGGER.info(JSON.toJSONString(map));
			return ; 
		}

		// boolean tag=true;
		for (String id : orderString) {
			// 这里做写入单价操作，还没有写！！！

			flag = orderService.changeOrderStatus2Buy(phoneId, id,
					togetherId, rank, reserveTime, message,payWay,price,totalPrice);

			// 更新库存和销量
			Order order = orderService.selectOneOrder(phoneId, id); // 获取该笔订单的消息

			paramMap.put("foodId", order.getFoodId());
			paramMap.put("orderCount", order.getOrderCount());
			paramMap.put("campusId",campus.getCampusId());
			int flag2=foodService.changeFoodCount(paramMap); // 增加销量，减少存货
			LOGGER.info(flag2);
			if (flag == 0 || flag == -1) {
				break;
			}
		}

		if (flag != -1 && flag != 0) {
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "下单成功，即将开始配送！");

			// 开启线程去访问极光推送
			pushService.sendPush("18896554880","一笔新的订单已经到达，请前往选单中查看，并尽早分派配送员进行配送。for优。", 1); 
			new Thread(new Runnable() {

				@Override public void run() { //向超级管理员推送，让其分发订单

					//推送 
					//pushService.sendPushByTag("0","一笔新的订单已经到达，请前往选单中查看，并尽早分派配送员进行配送。米奇零点。", 1);

					//Map<String, Object> paramterMap=new HashMap<String,Object>();
					//List<String>
					//superPhones=userService.getAllSuperAdminPhone(paramterMap);
					//for(String phone:superPhones){

						//推送
				//pushService.sendPush("18896554880","一笔新的订单已经到达，请前往选单中查看，并尽早分派配送员进行配送。for优。", 1); 
					//}
				} 
			}).start();


		} else {
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "下单失败，请重新开始下单");
		}

		return;
	}
   
	/**
	 * 测试通过订单号获取用户手机号和配送员手机号
	 */
	@Test
	 public void testGetUserPhoneAndAdminPhoneByTogetherId(){
		 Map<String,Object> paramMap=new HashMap<String,Object>();
		 
		  paramMap.put("togetherId", "153651867351430139990413");
		  String userPhone=orderService.getUserPhone(paramMap);
		  String adminPhone=orderService.getAdminPhone(paramMap);
		  
		  assertEquals("18896554880",userPhone);
		  assertEquals("18114706485", adminPhone);
	 }

	
}