$().ready(function() {
	validateRule();
	$(".chosen-select").chosen({
		maxHeight : 200
	});
	var htmlStr = getDataShowHtml('sysProp');
	$('#dataDivId').html(htmlStr);
	$('#id').change(function(){
		var htmlStr = getDataShowHtml($(this).val());
		$('#dataDivId').html(htmlStr);
	});
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});

function save() {
	var _data = initDataBeforeCommit();
	$.ajax({
		cache : true,
		type : "POST",
		url : "/prop/propConfig/save",
		data : _data,// 你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				parent.layer.msg("操作成功");
				parent.reLoad();
				var index = parent.layer.getFrameIndex(window.name); // 获取窗口索引
				parent.layer.close(index);
			} else {
				parent.layer.alert(data.msg)
			}

		}
	});

}
function validateRule() {
	var icon = "<i class='fa fa-times-circle'></i> ";
	$("#signupForm").validate({
		rules : {
			name : {
				required : true
			}
		},
		messages : {
			name : {
				required : icon + "请输入姓名"
			}
		}
	})
}