package game.modules.gift;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.modules.items.ItemModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.TimeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhenglei on 2018/5/14.
 */
public class OnlineModule implements ILogicModule {

    class Data{
        public String itemPkg;
        public int onlineTime;
    }


    private static Logger logger = LoggerFactory.getLogger(OnlineModule.class);
    private List<Data> m_listData = new ArrayList<>();

    private ItemModule m_itemModule = null;

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");

        //kernel.RegEvet(KernelEvent.KEVENT_ON_SITDOWN, "Player", this, "OnPlayerSitDown");
        //kernel.RegEvet(KernelEvent.KEVENT_ON_STANDUP, "Player", this, "OnPlayerStandUp");
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerLogin");
        kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerLogout");

        kernel.regClientMessage(C2SMsgDef.C2S_GET_ONLINE_GIFT.ordinal(), this, "OnGetOnlineGift");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/Gift/OnlineGift.xml");

        m_itemModule = (ItemModule) kernel.getModule("ItemModule");
        
        return true;
    }
	
	void RefreshCfg(IKernel kernel, String path)
	{
		if(path.equals("res/Gift/OnlineGift.xml"))
		{
			m_listData.clear();
			LoadConfig(kernel, path);
		}
	}

    boolean LoadConfig(IKernel kernel, String path) {
        ICfgReader onlinGiftConfig = kernel.loadXmlConfig(path);
        if (onlinGiftConfig == null) {
            return false;
        }

        for(int i = 0; i<onlinGiftConfig.getItemCount(); i++) {
            Data data = new Data();
            data.itemPkg    = onlinGiftConfig.getString(i,"Id");
            data.onlineTime = onlinGiftConfig.getInt(i,"OnlineMin") * 60 * 1000;
            m_listData.add(data);
        }
        return true;
    }


    @Override
    public void onDestroy() {

    }

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_LASTLEAVETIME, ValueType.STRING, false, true, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_LASTSITTIME, ValueType.LONG, false, true, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_ONLINETIME, ValueType.INT, false, true, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_ONLINEGIFTID, ValueType.STRING, false, true, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_LASTLOGINONLINETIME, ValueType.LONG, false, true, true);
    }

    public void OnPlayerOnline(IKernel kernel, IGameObject player) {


    }
    
    public void OnPlayerLogin(IKernel kernel, IGameObject player) {
    	long lastLoginTime = player.getLong(PLAYER_PROPERTY_LASTLOGINONLINETIME);
    	long currentTime = kernel.getServerTime();
    	if (!TimeUtils.isSameDay(lastLoginTime, currentTime)) {
    		 String giftId =m_listData.get(0).itemPkg;
             player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,giftId);
             player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
    	}
    	player.setProperty(PLAYER_PROPERTY_LASTLOGINONLINETIME, currentTime);
    	//logger.info("OnPlayerLogin OnlineTime:" + player.GetInt(PLAYER_PROPERTY_ONLINETIME));
    }
    
    public void OnPlayerLogout(IKernel kernel, IGameObject player) {
    	long lastLoginTime = player.getLong(PLAYER_PROPERTY_LASTLOGINONLINETIME);
    	long currentTime = kernel.getServerTime();
    	if (!TimeUtils.isSameDay(lastLoginTime, currentTime)) {
    		 String giftId =m_listData.get(0).itemPkg;
             player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,giftId);
             player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
    	}
    	String strGiftId = player.getString(PLAYER_PROPERTY_ONLINEGIFTID);
    	//全部奖励都领取完了
        if(StringUtils.isBlank(strGiftId))
        {
            return;
        }
        //如果到达上限，不记录
        int index= getGiftIndex(strGiftId);
        if(index==-1)
        {
            return;
        }
        int targetTime = m_listData.get(index).onlineTime;
        int onlineTime = player.getInt(PLAYER_PROPERTY_ONLINETIME);
        if(onlineTime>=targetTime)
        {
            return;
        }
        //getToday
        String strCurrentTime   = TimeUtils.GetCurrentDay(kernel,currentTime);
        String strLastLoginTime = TimeUtils.GetCurrentDay(kernel,lastLoginTime);
        if(!StringUtils.equals(strCurrentTime,strLastLoginTime))
        {
            //change lastSitTime to 00:00:00
        	lastLoginTime = TimeUtils.GetDayStartTime(kernel,strLastLoginTime);
        }
        //cal onlineTime
        long diff = currentTime - lastLoginTime;
        if(diff + onlineTime >= targetTime)
        {
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,targetTime);
        }else{
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,(int)diff+onlineTime);
        }
        //logger.info("OnPlayerLogout OnlineTime:" + player.GetInt(PLAYER_PROPERTY_ONLINETIME));
    }
    
    public void OnGetOnlineGift(IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException
    {
        //判断隔天
        long currentTime = kernel.getServerTime();
        long lastLoginTime = player.getLong(PLAYER_PROPERTY_LASTLOGINONLINETIME);
        if (!TimeUtils.isSameDay(lastLoginTime, currentTime)) {
   		 	String giftId =m_listData.get(0).itemPkg;
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,giftId);
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
        }

        CustomMsg.GetOnlineGift getOnlineGiftMsg = CustomMsg.GetOnlineGift.parseFrom(msg);
        String giftId =getOnlineGiftMsg.getGiftId();

        String strGiftId = player.getString(PLAYER_PROPERTY_ONLINEGIFTID);
        //如果giftId不同
        if(!StringUtils.equals(strGiftId,giftId))
        {
            return;
        }

        int index= getGiftIndex(strGiftId);
        if(index==-1)
        {
            return;
        }
        int targetTime = m_listData.get(index).onlineTime;
        //getToday
     /* String strCurrentTime = TimeUtils.GetCurrentDay(currentTime);
        String strLastLoginTime = TimeUtils.GetCurrentDay(lastLoginTime);
        if(!StringUtils.equals(strCurrentTime,strLastLoginTime))
        {
            //change lastSitTime to 00:00:00
        	lastLoginTime = TimeUtils.GetDayStartTime(strLastLoginTime);
        }*/

        //cal onlineTime
        int onlineTime = player.getInt(PLAYER_PROPERTY_ONLINETIME);
        long diff = currentTime- lastLoginTime;
        if(diff + onlineTime < targetTime)
        {
            return;
        }

        player.setProperty(PLAYER_PROPERTY_LASTLOGINONLINETIME,currentTime);
        player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
        //reach max
        if(index==m_listData.size()-1)
        {
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,"");
        }else{
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,m_listData.get(index+1).itemPkg);
        }

        //add reward
        m_itemModule.AddItem(kernel, player, strGiftId, 1, UtilFunc.System.ONLINE_GIFT.ordinal(), "client get");
        UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, strGiftId, 1);
    }
    
    private int getGiftIndex(String giftId) {
    	for(int i=0; i < m_listData.size(); i++) {
    		if(giftId.equals(m_listData.get(i).itemPkg)) {
    			return i;
    		}
    	}
    	return -1;
    }
    


    public void OnPlayerSitDown(IKernel kernel, IGameObject player, IGameObject desk)
    {
        //判断隔天
        long currentTime = kernel.getServerTime();
        this.checkChangeDay(kernel,player,currentTime);

        player.setProperty(PLAYER_PROPERTY_LASTSITTIME,currentTime);
    }

    private void checkChangeDay(IKernel kernel,IGameObject player,long currentTime)
    {
        String lastLeaveTime = player.getString(PLAYER_PROPERTY_LASTLEAVETIME);
        String strCurrentTime = TimeUtils.GetCurrentDay(kernel,currentTime);
        //隔天清零
        if(false==StringUtils.equals(strCurrentTime,lastLeaveTime))
        {
            String giftId =m_listData.get(0).itemPkg;
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,giftId);
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
            player.setProperty(PLAYER_PROPERTY_LASTLEAVETIME,currentTime);
        }
    }

    public void OnPlayerStandUp(IKernel kernel, IGameObject player, IGameObject desk)
    {
        //判断隔天
        long currentTime = kernel.getServerTime();
        this.checkChangeDay(kernel,player,currentTime);

        long lastSitTime = player.getLong(PLAYER_PROPERTY_LASTSITTIME);
        player.setProperty(PLAYER_PROPERTY_LASTSITTIME,-1L);
        String strGiftId = player.getString(PLAYER_PROPERTY_ONLINEGIFTID);
        //全部奖励都领取完了
        if(StringUtils.isBlank(strGiftId))
        {
            return;
        }

        //如果到达上限，不记录
        int index= m_listData.indexOf(strGiftId);
        if(index==-1)
        {
            return;
        }

        int targetTime = m_listData.get(index).onlineTime;
        int onlineTime = player.getInt(PLAYER_PROPERTY_ONLINETIME);
        if(onlineTime>=targetTime)
        {
            return;
        }

        //getToday
        String strCurrentTime = TimeUtils.GetCurrentDay(kernel,currentTime);
        String sitDownTime = TimeUtils.GetCurrentDay(kernel,lastSitTime);
        if(!StringUtils.equals(strCurrentTime,sitDownTime))
        {
            //change lastSitTime to 00:00:00
            lastSitTime = TimeUtils.GetDayStartTime(kernel,sitDownTime);
        }

        //cal onlineTime
        long diff = currentTime- lastSitTime;
        if(diff>=targetTime)
        {
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,diff);
        }else{
            player.setProperty(PLAYER_PROPERTY_ONLINETIME,diff+onlineTime);
        }
    }

    public void OnGetOnlineGifted(IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException
    {
        //判断隔天
        long currentTime = kernel.getServerTime();
        this.checkChangeDay(kernel,player,currentTime);

        CustomMsg.GetOnlineGift getOnlineGiftMsg = CustomMsg.GetOnlineGift.parseFrom(msg);
        String giftId =getOnlineGiftMsg.getGiftId();

        long lastSitTime = player.getLong(PLAYER_PROPERTY_LASTSITTIME);
        String strGiftId = player.getString(PLAYER_PROPERTY_ONLINEGIFTID);
        //如果giftId不同
        if(!StringUtils.equals(strGiftId,giftId))
        {
            return;
        }

        int index= m_listData.indexOf(strGiftId);
        if(index==-1)
        {
            return;
        }
        int targetTime = m_listData.get(index).onlineTime;
        //getToday
        String strCurrentTime = TimeUtils.GetCurrentDay(kernel,currentTime);
        String sitDownTime = TimeUtils.GetCurrentDay(kernel,lastSitTime);
        if(!StringUtils.equals(strCurrentTime,sitDownTime))
        {
            //change lastSitTime to 00:00:00
            lastSitTime = TimeUtils.GetDayStartTime(kernel,sitDownTime);
        }

        //cal onlineTime
        long diff = currentTime- lastSitTime;
        if(diff<targetTime)
        {
            return;
        }

        player.setProperty(PLAYER_PROPERTY_LASTSITTIME,currentTime);
        player.setProperty(PLAYER_PROPERTY_ONLINETIME,0);
        //reach max
        if(index==m_listData.size()-1)
        {
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,"");
        }else{
            player.setProperty(PLAYER_PROPERTY_ONLINEGIFTID,m_listData.get(index+1).itemPkg);
        }

        //add reward
        m_itemModule.AddItem(kernel, player, strGiftId, 1, UtilFunc.System.ONLINE_GIFT.ordinal(), "get gift");
    }


}
