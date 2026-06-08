package com.login.prop.controller;

import com.login.common.utils.PageData;
import com.login.common.utils.Query;
import com.login.common.utils.R;
import com.login.prop.domain.PropConfigDO;
import com.login.prop.service.PropConfigService;
import com.login.prop.util.PropUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 18:04:02
 */
 
@Controller
@RequestMapping("/prop/propConfig")
public class PropConfigController {

	@Autowired
	private PropConfigService propConfigService;

	@GetMapping()
	@RequiresPermissions("prop:propConfig:propConfig")
	String PropConfig(){
		return "prop/propConfig/propConfig";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("prop:propConfig:propConfig")
	public PageData list(@RequestParam Map<String, Object> params){
		//查询列表数据
		Query query = new Query(params);
		List<PropConfigDO> propConfigList = propConfigService.list(query);
		int total = propConfigService.count(query);
		return new PageData(propConfigList, total);
	}

	@GetMapping("/add")
	@RequiresPermissions("prop:propConfig:add")
	String add(){
		return "prop/propConfig/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("prop:propConfig:edit")
	String edit(@PathVariable("id") String id,Model model){
		PropConfigDO propConfig = propConfigService.get(id);
		model.addAttribute("propConfig",propConfig);
		return "prop/propConfig/edit";
	}

	@PostMapping("/init/{id}")
	@ResponseBody
	@RequiresPermissions("prop:propConfig:edit")
	public R initData(@PathVariable("id") String id){
		PropConfigDO propConfig = propConfigService.get(id);
		if (propConfig == null){
			return R.error("数据异常");
		}
		Object obj = PropUtils.transform(propConfig);
		return R.ok().put("data",obj);
	}

	/**
	 * 保存
	 */
	@PostMapping("/save")
	@ResponseBody
	@RequiresPermissions("prop:propConfig:add")
	public R save( PropConfigDO propConfig){
		PropConfigDO old = propConfigService.get(propConfig.getId());
		if (old != null){
			return R.error("配置已存在");
		}
		if (propConfigService.save(propConfig) > 0 ){
			return R.ok();
		}
		return R.error();
	}

	/**
	 * 修改
	 */
	@ResponseBody
	@PostMapping("/update")
	@RequiresPermissions("prop:propConfig:edit")
	public R update( PropConfigDO propConfig){
		propConfigService.update(propConfig);
		return R.ok();
	}
	
	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("prop:propConfig:remove")
	public R remove( String id){
		if (propConfigService.remove(id)>0){
			return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("prop:propConfig:batchRemove")
	public R remove(@RequestParam("ids[]") String[] ids){
		propConfigService.batchRemove(ids);
		return R.ok();
	}
}
