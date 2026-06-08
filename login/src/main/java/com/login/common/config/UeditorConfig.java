package com.login.common.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class UeditorConfig {
    private static String basePrefix = "http://localhost";
    private String imageActionName = "uploadimage"; /* 执行上传图片的action名称 */
    private String imageFieldName= "upfile"; /* 提交的图片表单名称 */
    private int imageMaxSize = 2048000;/* 上传大小限制，单位B */
    private String[] imageAllowFiles= new String []{".png", ".jpg", ".jpeg", ".gif", ".bmp"}; /* 上传图片格式显示 */
    private boolean imageCompressEnable = true; /* 是否压缩图片,默认是true */
    private int imageCompressBorder = 1600; /* 图片压缩最长边限制 */
    private String imageInsertAlign= "none"; /* 插入的图片浮动方式 */
    private String imageUrlPrefix = basePrefix; /* 图片访问路径前缀 */
    /* 上传保存路径,可以自定义保存路径和文件名格式 */
    /* {filename} 会替换成原文件名,配置这项需要注意中文乱码问题 */
    /* {rand:6} 会替换成随机数,后面的数字是随机数的位数 */
    /* {time} 会替换成时间戳 */
    /* {yyyy} 会替换成四位年份 */
    /* {yy} 会替换成两位年份 */
    /* {mm} 会替换成两位月份 */
    /* {dd} 会替换成两位日期 */
    /* {hh} 会替换成两位小时 */
    /* {ii} 会替换成两位分钟 */
    /* {ss} 会替换成两位秒 */
    /* 非法字符 \ : * ? " < > | */
    /* 具请体看线上文档: fex.baidu.com/ueditor/#use-format_upload_filename */
    public String imagePathFormat = "/images/upload/image/{yyyy}{mm}{dd}/{time}{rand:6}";

    /* 涂鸦图片上传配置项 */
    private String scrawlActionName= "uploadscrawl"; /* 执行上传涂鸦的action名称 */
    private String scrawlFieldName= "upfile"; /* 提交的图片表单名称 */
    private String scrawlPathFormat= "/images/upload/image/{yyyy}{mm}{dd}/{time}{rand:6}";/* 上传保存路径,可以自定义保存路径和文件名格式 */
    private int scrawlMaxSize= 2048000; /* 上传大小限制，单位B */
    private String scrawlUrlPrefix= basePrefix;/* 图片访问路径前缀 */
    private String scrawlInsertAlign= "none";
    /* 截图工具上传 */
    private String snapscreenActionName= "uploadimage"; /* 执行上传截图的action名称 */
    private String snapscreenPathFormat= "/images/upload/image/{yyyy}{mm}{dd}/{time}{rand:6}"; /* 上传保存路径,可以自定义保存路径和文件名格式 */
    private String snapscreenUrlPrefix= basePrefix; /* 图片访问路径前缀 */
    private String snapscreenInsertAlign= "none"; /* 插入的图片浮动方式 */

    /* 抓取远程图片配置 */
    private String[] catcherLocalDomain = new String[]{"127.0.0.1", "localhost", "img.baidu.com"};
    private String catcherActionName= "catchimage"; /* 执行抓取远程图片的action名称 */
    private String catcherFieldName= "source"; /* 提交的图片列表表单名称 */
    private String catcherPathFormat= "/images/upload/image/{yyyy}{mm}{dd}/{time}{rand:6}";/* 上传保存路径,可以自定义保存路径和文件名格式 */
    private String catcherUrlPrefix= basePrefix; /* 图片访问路径前缀 */
    private int catcherMaxSize= 2048000;/* 上传大小限制，单位B */
    private String[] catcherAllowFiles= new String[]{".png", ".jpg", ".jpeg", ".gif", ".bmp"}; /* 抓取图片格式显示 */
    /* 上传视频配置 */
    private String videoActionName= "uploadvideo";/* 执行上传视频的action名称 */
    private String videoFieldName= "upfile"; /* 提交的视频表单名称 */
    private String videoPathFormat= "/images/upload/video/{yyyy}{mm}{dd}/{time}{rand:6}"; /* 上传保存路径,可以自定义保存路径和文件名格式 */
    private String videoUrlPrefix= basePrefix; /* 视频访问路径前缀 */
    private int videoMaxSize= 102400000; /* 上传大小限制，单位B，默认100MB */
    private String[] videoAllowFiles= new String[]{
            ".flv", ".swf", ".mkv", ".avi",
            ".rm", ".rmvb", ".mpeg", ".mpg",
            ".ogg", ".ogv", ".mov", ".wmv",
            ".mp4", ".webm", ".mp3", ".wav",
            ".mid"}; /* 上传视频格式显示 */
    /* 上传文件配置 */
    private String fileActionName= "uploadfile"; /* controller里,执行上传视频的action名称 */
    private String fileFieldName= "upfile"; /* 提交的文件表单名称 */
    private String filePathFormat= "/images/upload/file/{yyyy}{mm}{dd}/{time}{rand:6}"; /* 上传保存路径,可以自定义保存路径和文件名格式 */
    private String fileUrlPrefix= basePrefix;/* 文件访问路径前缀 */
    private int fileMaxSize= 51200000; /* 上传大小限制，单位B，默认50MB */
    private String[] fileAllowFiles= new String[]{
            ".png", ".jpg", ".jpeg", ".gif", ".bmp",
            ".flv", ".swf", ".mkv", ".avi", ".rm", ".rmvb", ".mpeg", ".mpg",
            ".ogg", ".ogv", ".mov", ".wmv", ".mp4", ".webm", ".mp3", ".wav", ".mid",
            ".rar", ".zip", ".tar", ".gz", ".7z", ".bz2", ".cab", ".iso",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf", ".txt", ".md", ".xml"
    }; /* 上传文件格式显示 */
    /* 列出指定目录下的图片 */
    private String imageManagerActionName= "listimage";/* 执行图片管理的action名称 */
    private String imageManagerListPath= "/images/upload/image/"; /* 指定要列出图片的目录 */
    private int imageManagerListSize = 20; /* 每次列出文件数量 */
    private String imageManagerUrlPrefix= basePrefix; /* 图片访问路径前缀 */
    private String imageManagerInsertAlign= "none"; /* 插入的图片浮动方式 */
    private String[] imageManagerAllowFiles= new String[]{".png", ".jpg", ".jpeg", ".gif", ".bmp"}; /* 列出的文件类型 */
    /* 列出指定目录下的文件 */
    private String fileManagerActionName= "listfile"; /* 执行文件管理的action名称 */
    private String fileManagerListPath= "/images/upload/file/"; /* 指定要列出文件的目录 */
    private String fileManagerUrlPrefix= basePrefix; /* 文件访问路径前缀 */
    private int fileManagerListSize= 20; /* 每次列出文件数量 */
    private String[] fileManagerAllowFiles = new String[]{
            ".png", ".jpg", ".jpeg", ".gif", ".bmp",
            ".flv", ".swf", ".mkv", ".avi", ".rm", ".rmvb", ".mpeg", ".mpg",
            ".ogg", ".ogv", ".mov", ".wmv", ".mp4", ".webm", ".mp3", ".wav", ".mid",
            ".rar", ".zip", ".tar", ".gz", ".7z", ".bz2", ".cab", ".iso",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf", ".txt", ".md", ".xml"
    }; /* 列出的文件类型 */
}
