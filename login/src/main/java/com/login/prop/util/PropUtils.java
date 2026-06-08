package com.login.prop.util;


import com.login.common.utils.JSONUtils;
import com.login.prop.domain.*;
import com.login.prop.domain.*;

public class PropUtils {

    private static Class<?> getType(String key){
        if ("sysProp".equals(key)){
            return AppProperties.class;
        }else if ("realName".equals(key)){
            return RealNameProperties.class;
        }else if ("smsProp".equals(key)){
            return SmsProperties.class;
        }else if ("huaweiProp".equals(key)){
            return HuaWeiProperties.class;
        }else if ("tencentProp".equals(key)){
            return TencentProperties.class;
        }else if ("xiaomiProp".equals(key)){
            return XiaoMiProperties.class;
        }else if ("oppoProp".equals(key)){
            return OppoProperties.class;
        }else if ("wechatProp".equals(key)){
            return WechatProperties.class;
        }
        return null;
    }

    public static<T> T transform(PropConfigDO propConfig){
        Class<?> type = getType(propConfig.getId());
        return JSONUtils.jsonToBean(propConfig.getData(),type);
    }
}
