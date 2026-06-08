var editor;
//initKindEditor_content('content',500,300);
function initKindEditor_content(id, width,height){
    editor = UE.getEditor(id,{
        initialFrameHeight: height,
        initialFrameWidth: width,
        toolbars: [[
            'undo', //撤销
            'redo', //重做
            'bold', //加粗
            'indent', //首行缩进
            'italic', //斜体
            'underline', //下划线
            'strikethrough', //删除线
            'subscript', //下标
            'fontborder', //字符边框
            'superscript', //上标
            'formatmatch', //格式刷
            'pasteplain', //纯文本粘贴模式
            'source', //源代码
            'horizontal', //分隔线
            'fontfamily', //字体
            'fontsize', //字号
            'paragraph', //段落格式
            'customstyle', //自定义标题
            'forecolor', //字体颜色
            'backcolor', //背景色
            'insertcode', //代码语言
            'simpleupload', //单图上传
            'insertimage', //多图上传
            'link', //超链接
            'emotion', //表情
            'map', //Baidu地图
            'justifyleft', //居左对齐
            'justifyright', //居右对齐
            'justifycenter', //居中对齐
            'justifyjustify', //两端对齐
            'fullscreen', //全屏
            'pagebreak', //分页
            'imagenone', //默认
            'imageleft', //左浮动
            'imageright', //右浮动
            'imagecenter', //居中
            'wordimage', //图片转存
            'lineheight', //行间距
            'edittip ', //编辑提示
            'scrawl', //涂鸦
            'autotypeset', //自动排版
            'selectall', //全选
            'preview', //预览
            'horizontal', //分隔线
            'removeformat', //清除格式
            'unlink', //取消链接
            'cleardoc', //清空文档
            'link', //超链接
            'emotion', //表情
            'spechars', //特殊字符
            'insertorderedlist', //有序列表
            'insertunorderedlist', //无序列表
            'rowspacingtop', //段前距
            'rowspacingbottom', //段后距
        ]]
        ,zIndex:999999999999//编辑器在页面上的z-index层级的基数，默认是900
        ,autoFloatEnabled: false//是否保持toolbar的位置不动，默认true
        ,wordCount: true
        ,maximumWords: 1000
        ,wordCountMsg:'{#count}/1000'
    });
}