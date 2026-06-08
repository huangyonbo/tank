var cursor = 0,showCount = 0;
function downDivMοuseOver(obj) {
	obj.style.color = 'white';
	obj.style.backgroundColor = 'blue';
	var idStr = obj.id.substring(7, obj.id.length);
	cursor = parseInt(idStr);
}
function downDivMοuseLeave(obj){
	obj.style.color = 'black';
	obj.style.backgroundColor = 'white';
}
function downDivClick(inputId,selectId,downDivId,obj) {
	$("#" + selectId).val(obj.value);
	$("#" + downDivId).hide();
	$("#" + inputId).val(obj.innerHTML);
	cursor = 0;
}
function autoScroll(downDiv,flag){
	if (flag){
		if (cursor > 1){
			downDiv.scrollTop((cursor - 1) * 14);
		}
	}else{
		if (cursor == 1){
			downDiv.scrollTop(0);
		}else{
			downDiv.scrollTop((cursor -1) * 14);
		}
	}
}
function searchInSelect(selectId,downDivId,obj) {
	var value = obj.value.toLowerCase();
	var selectObj =  $("#" + selectId);
	var downDiv   =  $("#" + downDivId);
	if (value == ""){
		downDiv.hide();
		return;
	}
	if(event.keyCode != 40 && event.keyCode != 38 && event.keyCode != 13){
		cursor = 0;
		showCount = 0;
		downDiv.scrollTop(0);
		downDiv.show();
		var htmlStr = '';
		var id = 1;
		$("#" + selectId + " option").each(function () {
			var val = $(this).val();
			var text = $(this).text();
			if (text.toLowerCase().indexOf(value) == 0){
				showCount ++;
				htmlStr += '<div id="selText' + id + '" onmouseover="downDivMοuseOver(this)" onmouseleave="downDivMοuseLeave(this)" ';
				htmlStr += 'onclick="downDivClick(\'' + obj.id + '\',\''+ selectId + '\',\'' + downDivId + '\',this)" ';
				htmlStr += 'value="' + val + '" ';
				htmlStr += 'style="color:black;width:100%;">';
				htmlStr += text;
				htmlStr += '</div>';
				id ++;
			}
		});
		downDiv.html(htmlStr);
	}else if(event.keyCode==40){//press down key
		var preObj = $("#selText" + cursor);
		if (preObj != null){
			preObj.css({'color' : 'black', 'background-color': 'white'});
		}
		cursor ++;
		if (cursor > showCount){
			cursor = showCount;
		}
		var curObj = $("#selText" + cursor);
		if(curObj != null){
			curObj.css({'color' : 'white', 'background-color': 'blue'});
			autoScroll(downDiv,true);
		}
	}else if (event.keyCode==38){//press up key
		var preObj = $("#selText" + cursor);
		if (preObj != null){
			preObj.css({'color' : 'black', 'background-color': 'white'});
		}
		cursor --;
		if (cursor < 1){
			cursor = 1;
		}
		var curObj = $("#selText" + cursor);
		if (curObj != null){
			curObj.css({'color' : 'white', 'background-color': 'blue'});
			autoScroll(downDiv,false);
		}
	}else if (event.keyCode == 13 ){//press enter key
		var tmpObj = $("#selText" + cursor);
		if (tmpObj){
			obj.value = tmpObj.text();
			selectObj.val(tmpObj.val());
		}
		downDiv.hide();
	}
}
function selectChangeInput(inputId,data) {
	//这里要显示<option>标签体里面的内容不是value
	var str = data.options[data.selectedIndex].innerHTML;
	$("#" + inputId).val(str);
}