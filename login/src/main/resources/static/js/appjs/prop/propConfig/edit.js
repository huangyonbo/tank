$().ready(function() {
	validateRule();
	var idStr = $('#id').val()
	var htmlStr = getDataShowHtml(idStr);
	$('#dataDivId').html(htmlStr);
	$.ajax({
		cache : true,
		type : "POST",
		url : "/prop/propConfig/init/" + idStr,
		data : $('#signupForm').serialize(),//你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				fillData(idStr,data.data);
			} else {
				parent.layer.alert(data.msg)
			}

		}
	});
});

$.validator.setDefaults({
	submitHandler : function() {
		update();
	}
});
function update() {
	var key = $('#id').val();
	var _data = initDataBeforeCommit(key);
	$.ajax({
		cache : true,
		type : "POST",
		url : "/prop/propConfig/update",
		data : _data,
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
				required : icon + "请输入名字"
			}
		}
	})
}