//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.login.common.aspect;

import com.login.app.domain.BaseLoginParamsDTO;
import com.login.app.domain.LoginParamsDTO;
import com.login.common.utils.HttpServletUtils;
import com.login.common.utils.R;
import com.login.enums.LoginResultEnum;
import com.login.ip.domain.LimitIpDO;
import com.login.ip.service.LimitIpService;
import com.login.maintain.domain.MaintainRecordDO;
import com.login.maintain.service.MaintainRecordService;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class LimitCheckAspect {
    private static final Logger log = LoggerFactory.getLogger(LimitCheckAspect.class);
    @Autowired
    private MaintainRecordService maintainRecordService;
    @Autowired
    private LimitIpService limitIpService;

    public LimitCheckAspect() {
    }

    @Pointcut("@annotation(com.login.common.annotation.LimitCheck)")
    public void limitPointCut() {
    }

    private String checkMaintain(int channel, List<LimitIpDO> limitIps, final String ip) {
        String clearnIp = ip == null ? "" : ip.replaceAll("[\"'“”‘’\\s]", "");
        if (channel != 0) {
            List<MaintainRecordDO> maintainRecordDOS = this.maintainRecordService.list();
            if (maintainRecordDOS != null && maintainRecordDOS.size() > 0) {
                Date now = new Date();
                Iterator var7 = maintainRecordDOS.iterator();

                MaintainRecordDO maintainRecordDO;
                List channels;
                do {
                    do {
                        do {
                            if (!var7.hasNext()) {
                                return null;
                            }

                            maintainRecordDO = (MaintainRecordDO) var7.next();
                        } while (now.before(maintainRecordDO.getStart()));
                    } while (now.after(maintainRecordDO.getEnd()));

                    if ("[-1]".equals(maintainRecordDO.getChannel()) && (limitIps.size() == 0 || !limitIps.stream().anyMatch((limitIp) -> {
                        return limitIp.getType() == 2 && limitIp.checkIp(clearnIp);
                    }))) {
                        log.info("{} not in white list  {}   {} {}", new Object[]{clearnIp, limitIps, limitIps.stream().anyMatch((limitIp) -> {
                            return limitIp.getType() == 2 && limitIp.checkIp(clearnIp);
                        })});
                        return maintainRecordDO.getShowMessage();
                    }

                    channels = JSON.parseArray(maintainRecordDO.getChannel(), Integer.class);
                } while (channels == null || !channels.contains(channel) || limitIps.stream().anyMatch((limitIp) -> {
                    return limitIp.getType() == 2 && limitIp.checkIp(clearnIp);
                }));

                log.info("{} not in white list", clearnIp);
                return maintainRecordDO.getShowMessage();
            }
        }

        return null;
    }

    @Around("limitPointCut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object[] params = pjp.getArgs();
        String ip = HttpServletUtils.getRealIp((HttpServletRequest) params[1]);
        List<LimitIpDO> limitIps = this.limitIpService.list((Map) null);
        String finalIp = ip;
        if (limitIps.stream().anyMatch((limitIp) -> {
            return limitIp.getType() < 2 && limitIp.checkIp(finalIp);
        })) {
            log.info("{} in black list", ip);
            return R.result(LoginResultEnum.DECODE_ERROR1.ordinal());
        } else {
            Object obj = params[0];
            String mainInfo = null;
            int channelId;
            if (obj instanceof BaseLoginParamsDTO) {
                BaseLoginParamsDTO loginParamsDTO = (BaseLoginParamsDTO) obj;
                channelId = loginParamsDTO.getLoginInfo().getChannelId();
                ip = loginParamsDTO.getClientIP();
                mainInfo = this.checkMaintain(channelId, limitIps, ip);
            } else if (obj instanceof LoginParamsDTO) {
                LoginParamsDTO loginParamsDTO = (LoginParamsDTO) obj;
                channelId = loginParamsDTO.getSdkType();
                ip = loginParamsDTO.getRealIp();
                log.info("真实的IP {}", ip);
                mainInfo = this.checkMaintain(channelId, limitIps, ip);
            }

            return mainInfo != null ? R.result(LoginResultEnum.DECODE_ERROR2.ordinal()).put("mainInfo", mainInfo) : pjp.proceed();
        }
    }
}
