$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});



function save() {
	let type = $("#type").val();
	if (type == 1){
		let start = $("#start").val();
		if (start == ''){
			parent.layer.alert("请输入禁用开始时间");
			return;
		}
		let end = $("#end").val();
		if (end == ''){
			parent.layer.alert("请输入禁用结束时间");
			return;
		}
	}
	$.ajax({
		cache : true,
		type : "POST",
		url : "/ip/limitIp/save",
		data : $('#signupForm').serialize(),// 你的formid
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
			ip : {
				required : true
			}
		},
		messages : {
			ip : {
				required : icon + "请输入IP地址"
			}
		}
	})
}