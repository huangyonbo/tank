package com.login.ip.controller;

import com.login.common.utils.PageData;
import com.login.common.utils.Query;
import com.login.common.utils.R;
import com.login.ip.domain.LimitIpDO;
import com.login.ip.service.LimitIpService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ip禁用
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 11:30:55
 */
 
@Controller
@RequestMapping("/ip/limitIp")
public class LimitIpController {

	@Autowired
	private LimitIpService limitIpService;

	@GetMapping()
	@RequiresPermissions("ip:limitIp:limitIp")
	String LimitIp(){
		return "ip/limitIp/limitIp";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("ip:limitIp:limitIp")
	public PageData list(@RequestParam Map<String, Object> params){
		//查询列表数据
		Query query = new Query(params);
		List<LimitIpDO> limitIpList = limitIpService.list(query);
		int total = limitIpService.count(query);
		return new PageData(limitIpList, total);
	}

	@GetMapping("/add")
	@RequiresPermissions("ip:limitIp:add")
	String add(){
		return "ip/limitIp/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("ip:limitIp:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		LimitIpDO limitIp = limitIpService.get(id);
		model.addAttribute("limitIp", limitIp);
		return "ip/limitIp/edit";
	}

	/**
	 * 保存
	 */
	@PostMapping("/save")
	@ResponseBody
	@RequiresPermissions("ip:limitIp:add")
	public R save( LimitIpDO limitIp){
		if (limitIpService.save(limitIp)>0){
			return R.ok();
		}
		return R.error();
	}

	/**
	 * 修改
	 */
	@ResponseBody
	@PostMapping("/update")
	@RequiresPermissions("ip:limitIp:edit")
	public R update( LimitIpDO limitIp){
		limitIpService.update(limitIp);
		return R.ok();
	}
	
	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("ip:limitIp:remove")
	public R remove( Integer id){
		if (limitIpService.remove(id)>0){
			return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("ip:limitIp:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		limitIpService.batchRemove(ids);
		return R.ok();
	}
}
