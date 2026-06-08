function urlencode (str) {
    return encodeURI(str).replace(/\+/g, '%2B');
}

function fillData(key,dataEntity){
    if (key === 'sysProp'){
        $('#salt').val(dataEntity.salt);
        $('#address').val(dataEntity.address);
        $('#nickName').val(dataEntity.nickName);
        $('#phoneRegister').prop('checked',dataEntity.phoneRegister === 'true');
        $('#accountRegister').prop('checked',dataEntity.accountRegister === 'true');
        $('#deviceRegisterLimit').val(dataEntity.deviceRegisterLimit);
    }else if (key === 'realName'){
        $('#test').prop('checked',dataEntity.test === 'true');
        $('#appId').val(dataEntity.appId);
        $('#appName').val(dataEntity.appName);
        $('#bizId').val(dataEntity.bizId);
        $('#secretKey').text(dataEntity.secretKey);
    }else if (key === 'smsProp'){
        $('#test').prop('checked',dataEntity.test === 'true');
        $('#warningPhone').val(dataEntity.warningPhone);
        $('#appKey').val(dataEntity.appKey);
        $('#appSecret').val(dataEntity.appSecret);
        $('#verifyTemplateId').val(dataEntity.verifyTemplateId);
        $('#notifyTemplateId').val(dataEntity.notifyTemplateId);
    }else if (key === 'huaweiProp'){
        $('#name').val(dataEntity.name);
        $('#appId').val(dataEntity.appId);
        $('#cpId').val(dataEntity.cpId);
        $('#gameRsaPublic').text(dataEntity.gameRsaPublic);
        $('#gameRsaPrivate').text(dataEntity.gameRsaPrivate);
        $('#channelFlag').val(dataEntity.channelFlag);
    }else if (key === 'tencentProp'){
        $('#test').prop('checked',dataEntity.test === 'true');
        $('#appIdQq').val(dataEntity.appIdQq);
        $('#appIdWx').val(dataEntity.appIdWx);
        $('#appKeyQq').val(dataEntity.appKeyQq);
        $('#appKeyWx').val(dataEntity.appKeyWx);
        $('#channelFlag').val(dataEntity.channelFlag);
    }else if (key === 'xiaomiProp'){
        $('#appId').val(dataEntity.appId);
        $('#appKey').val(dataEntity.appKey);
        $('#appSecret').val(dataEntity.appSecret);
        $('#channelFlag').val(dataEntity.channelFlag);
    }else if (key === 'oppoProp'){
        $('#appId').val(dataEntity.appId);
        $('#appKey').val(dataEntity.appKey);
        $('#appSecret').val(dataEntity.appSecret);
        $('#channelFlag').val(dataEntity.channelFlag);
    } else if (key === 'wechatProp'){
        $('#appId').val(dataEntity.appId);
        $('#appSecret').val(dataEntity.appSecret);
    }
}

function initDataBeforeCommit(key){
    if (key === undefined){
        key = $('#id').val();
    }
	var data = 'id=' + key + '&data={';
    if (key === 'sysProp'){
        data += '"salt":"' + $('#salt').val() + '"';
        data += ',"address":"' + $('#address').val() + '"';
        data += ',"nickName":"' + $('#nickName').val() + '"';
        data += ',"phoneRegister":"' + $('#phoneRegister').is(':checked') + '"';
        data += ',"accountRegister":"' + $('#accountRegister').is(':checked') + '"';
        data += ',"deviceRegisterLimit":"' + $('#deviceRegisterLimit').val() + '"';
    }else if (key === 'realName'){
        data += '"appId":"' + $('#appId').val() + '"';
        data += ',"appName":"' + $('#appName').val() + '"';
        data += ',"bizId":"' + $('#bizId').val() + '"';
        data += ',"secretKey":"' + urlencode($('#secretKey').val()) + '"';
        data += ',"test":"' + $('#test').is(':checked') + '"';
    }else if (key === 'smsProp'){
        data += '"warningPhone":"' + $('#warningPhone').val() + '"';
        data += ',"test":"' + $('#test').is(':checked') + '"';
        data += ',"appKey":"' + urlencode($('#appKey').val()) + '"';
        data += ',"appSecret":"' + urlencode($('#appSecret').val()) + '"';
        data += ',"verifyTemplateId":"' + $('#verifyTemplateId').val() + '"';
        data += ',"notifyTemplateId":"' + $('#notifyTemplateId').val() + '"';
    }else if (key === 'huaweiProp'){
        data += '"appId":"' + $('#appId').val() + '"';
        data += ',"cpId":"' + $('#cpId').val() + '"';
        data += ',"gameRsaPublic":"' + urlencode($('#gameRsaPublic').val()) + '"';
        data += ',"gameRsaPrivate":"' + urlencode($('#gameRsaPrivate').val()) + '"';
        data += ',"channelFlag":"' + $('#channelFlag').val() + '"';
    }else if (key === 'tencentProp'){
        data += '"test":"' + $('#test').is(':checked') + '"';
        data += ',"appIdQq":"' + $('#appIdQq').val() + '"';
        data += ',"appIdWx":"' + $('#appIdWx').val() + '"';
        data += ',"appKeyQq":"' + $('#appKeyQq').val() + '"';
        data += ',"appKeyWx":"' + $('#appKeyWx').val() + '"';
        data += ',"channelFlag":"' + $('#channelFlag').val() + '"';
    }else if (key === 'xiaomiProp'){
        data += '"appId":"' + $('#appId').val() + '"';
        data += ',"appKey":"' + urlencode($('#appKey').val()) + '"';
        data += ',"appSecret":"' + urlencode($('#appSecret').val()) + '"';
        data += ',"channelFlag":"' + $('#channelFlag').val() + '"';
    }else if (key === 'oppoProp'){
        data += '"appId":"' + $('#appId').val() + '"';
        data += ',"appKey":"' + urlencode($('#appKey').val()) + '"';
        data += ',"appSecret":"' + urlencode($('#appSecret').val()) + '"';
        data += ',"channelFlag":"' + $('#channelFlag').val() + '"';
    }else if (key === 'wechatProp'){
        data += '"appId":"' + $('#appId').val() + '"';
        data += ',"appSecret":"' + urlencode($('#appSecret').val()) + '"';
    }
    data += '}';
    return data;
}

function getDataShowHtml(key) {
    var htmlStr = '';
    if (key === 'sysProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">加密key：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="salt" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">redis主机：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="address" class="form-control" type="text" value="127.0.0.1">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">昵称前缀：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="nickName" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">手机注册：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="phoneRegister" class="form-control" type="checkbox" value="true">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">账号注册：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="accountRegister" class="form-control" type="checkbox" value="true">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">设备限制：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="deviceRegisterLimit" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';
    }else if (key === 'realName'){

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">test：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="test" class="form-control" type="checkbox" value="false" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appName：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appName" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">bizId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="bizId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">secretKey：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<textarea id="secretKey" class="form-control" type="text"></textarea>';
        htmlStr += '</div>';
        htmlStr += '</div>';

    }else if (key === 'smsProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">测试模式：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="test" class="form-control" type="checkbox" value="false" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">预警电话：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="warningPhone" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appKey：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appKey" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appSecret：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appSecret" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">验证模板编号：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="verifyTemplateId" class="form-control" type="text" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">通知模板编号：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="notifyTemplateId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';
    }else if (key === 'huaweiProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">cpId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="cpId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">gameRsaPublic：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<textarea id="gameRsaPublic" class="form-control" type="text"></textarea>';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">gameRsaPrivate：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<textarea id="gameRsaPrivate" class="form-control" type="text"></textarea>';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">channelFlag：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="channelFlag" class="form-control" type="text" value="huawei_" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

    }else if (key === 'tencentProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">测试模式：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="test" class="form-control" type="checkbox" value="false" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appIdQq：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appIdQq" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appIdWx：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appIdWx" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appKeyQq：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appKeyQq" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appKeyWx：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appKeyWx" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">channelFlag：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="channelFlag" class="form-control" type="text" value="tencent_" >';
        htmlStr += '</div>';
        htmlStr += '</div>';

    }else if (key === 'xiaomiProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appKey：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appKey" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appSecret：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appSecret" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">channelFlag：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="channelFlag" class="form-control" type="text" value="xiaomi_" >';
        htmlStr += '</div>';
        htmlStr += '</div>';


    }else if (key === 'oppoProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appKey：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appKey" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appSecret：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appSecret" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">channelFlag：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="channelFlag" class="form-control" type="text" value="oppo_" >';
        htmlStr += '</div>';
        htmlStr += '</div>';
    }else if (key === 'wechatProp'){
        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appId：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appId" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';

        htmlStr += '<div class="form-group">';
        htmlStr += '<label class="col-sm-3 control-label">appSecret：</label>';
        htmlStr += '<div class="col-sm-8">';
        htmlStr += '<input id="appSecret" class="form-control" type="text">';
        htmlStr += '</div>';
        htmlStr += '</div>';
    }
    return htmlStr;
}