package com.login.common.controller;

import com.login.common.config.CommonConfig;
import com.login.common.config.UeditorConfig;
import com.login.common.domain.FileDO;
import com.login.common.service.FileService;
import com.login.common.utils.FileType;
import com.login.common.utils.FileUtil;
import com.login.common.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@Slf4j
public class UeditorController {
	@Autowired
	private UeditorConfig ueditorConfig;
	@Autowired
	private FileService sysFileService;
	@Autowired
	private CommonConfig commonConfig;

	@ResponseBody
	@RequestMapping(value = "/ueditor")
	public String ueditor (@RequestParam("action") String action,HttpServletRequest request) {
		if ("config".equals(action)){
			return JSONUtils.beanToJson(ueditorConfig);
		}else if ("uploadimage".equals(action)){
			return JSONUtils.beanToJson(ueditorupload(request));
		}else{
			String error = "error action";
			Map<String,Object> result = new HashMap<String,Object>();
			result.put("state",error);
			log.error(error);
			return JSONUtils.beanToJson(result);
		}
	}

	public Map<String,Object> ueditorupload(HttpServletRequest request) {
		MultipartHttpServletRequest mReq = null;
		MultipartFile multipartFile = null;
		String fileName = "";
		//原始文件名UEDITOR创建页面元素时的alt和title属性
		String originalFileName = "";
		Map<String,Object> result = new LinkedHashMap<>();
		try {
			mReq = (MultipartHttpServletRequest) request;
			//从config.json中取得上传文件的ID
			multipartFile = mReq.getFile("upfile");
			//取得文件的原始文件名称
			originalFileName = multipartFile.getOriginalFilename();
			fileName = FileUtil.renameToUUID(originalFileName);//获取文件存储路径
			FileUtil.uploadFile(multipartFile.getBytes(),commonConfig.getUploadPath(),fileName);
			FileDO sysFile = new FileDO(FileType.fileType(fileName),originalFileName,"/files/" + fileName,new Date());
			if (sysFileService.save(sysFile) > 0) {
				result.put("state","SUCCESS");
				result.put("url",fileName);
				result.put("title",originalFileName);
				result.put("original",originalFileName);
			}else{
				result.put("state","系统异常");
			}
		} catch (Exception e) {
			String error = "文件 " + originalFileName + " 上传失败!";
			result.put("state",error);
			log.error(error,e);
		}
		return result;
	}
}
