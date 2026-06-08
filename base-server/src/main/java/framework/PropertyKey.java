package framework;


public interface PropertyKey {
	String PLAYER_PROPERTY_ID        = "Id";//编号
	String PLAYER_PROPERTY_SCRIPT    = "Script";//脚本
	String PLAYER_PROPERTY_CAPACITY  = "Capacity";//容量
	String PLAYER_PROPERTY_UID       = "Uid";//编号
	String PLAYER_PROPERTY_NAME      = "Name";//昵称
	String PLAYER_PROPERTY_SEX       = "Sex";//性别
	String PLAYER_PROPERTY_HEAD      = "Head";//头像
	String PLAYER_PROPERTY_HEADID    = "HeadId";//头像编号
    String PLAYER_ACCOUNT_STATUS     = "AccountStatus";//账号状态

	String PLAYER_PROPERTY_FRONTSER  = "FrontSer";//gate名称
	String PLAYER_PROPERTY_DESKID    = "DeskID";//桌子编号
	String PLAYER_PROPERTY_SEATID    = "SeatID";//座位号
	String PLAYER_PROPERTY_CHANNEL   = "Channel";//渠道编号
	String PLAYER_PROPERTY_REGTIME   = "RegTime";//注册时间
	String PLAYER_PROPERTY_LAST_SAVE = "LastSave";//上次存档时间
	String PLAYER_PROPERTY_VERSION   = "Version";//版本号
	String PLAYER_PROPERTY_DEVICEID  = "DeviceID";//设备号
	String PLAYER_PROPERTY_MACADDR   = "MacAddr";//设备mac编号
	String PLAYER_PROPERTY_IPADDR    = "IpAddr";//IP
	String PLAYER_PROPERTY_PAYINFO   = "PayInfo";//支付信息
	String PLAYER_PROPERTY_LASTOPT   = "LastOpt";//上次操作
	String PLAYER_PROPERTY_PHONE     = "Phone";//手机号
	String PLAYER_PROPERTY_RECRUITED = "Recruited";//招募者分享码
	String PLAYER_PROPERTY_CERTIFICATION = "Certification";//是否实名认证
	String PLAYER_PROPERTY_AGE       = "Age";//年龄
	String PLAYER_PROPERTY_TESTPAY   = "testPay";//是否模拟充值
	String PLAYER_PROPERTY_GOLD      = "Gold";//金币
	String PLAYER_PROPERTY_DIAMOND   = "Diamond";//钻石
	String PLAYER_PROPERTY_BOMB_COIN = "BombCoin";//魔晶
	String PLAYER_PROPERTY_BOMB_ITEM = "BombItem";//炸弹
	String PLAYER_PROPERTY_TOTALPLAY = "TotalPlay";//总玩
	String PLAYER_PROPERTY_TOTALWIN  = "TotalWin";//总赢
	String PLAYER_PROPERTY_BOMBTOTALPLAY = "BombTotalPlay";//玩家海神殿总玩
	String PLAYER_PROPERTY_BOMBTOTALWIN  = "BombTotalWin";//玩家海神殿总赢
	String PLAYER_PROPERTY_LEVEL = "Level";//等级
	String PLAYER_PROPERTY_OFFPROTECT = "OffProtect";
	String PLAYER_PROPERTY_EXP  = "Exp";//经验
	String PLAYER_PROPERTY_SYNC_FLAG = "FishSyncFlag";//渔场同步标志
	String PLAYER_PROPERTY_VIPLEVEL  = "VipLevel";//vip等级
	String PLAYER_PROPERTY_TITLEID   = "TitleId";//称号编号
	String PLAYER_PROPERTY_BULLETLEVEL  = "BulletLevel";//炮值等级
	String PLAYER_PROPERTY_NBULLETLEVEL  = "NBulletLevel";//魔晶炮值等级
	String PLAYER_PROPERTY_NBULLETLEVEL_RATE  = "UpNBulletRate";//魔晶炮升级成功率
	String PLAYER_PROPERTY_BULLETVALUE  = "BulletValue";//当前炮值
	String PLAYER_PROPERTY_GIVENAUTOFIRE10M  = "GivenAutoFire10m";//自动发炮试用是否已给
	String PLAYER_PROPERTY_COLORTICKET  = "ColorTicket";//彩券
	String PLAYER_PROPERTY_TOTALCOLORTICKET  = "TotalColorTicket";//累计彩券
	String PLAYER_PROPERTY_TOTALUSECOLORTICKET  = "TotalUseColorTicket";//累计使用彩券
	String PLAYER_PROPERTY_SHUTUP         = "Shutup";//禁言
	String PLAYER_PROPERTY_LASTLOGINTIME  = "LastLoginTime";
	String PLAYER_PROPERTY_DAYRELIEFCOUNT  = "DayReliefCount";
	String PLAYER_PROPERTY_HAVEBOMBSCORE  = "HaveBombScore";//炸弹积分
	String PLAYER_PROPERTY_USEBOMBSCORE  = "UseBombScore";//使用炸弹积分
	String PLAYER_PROPERTY_HITFISHSCORE  = "HitFishScore";//击杀鱼积分
	String PLAYER_PROPERTY_SIGN  = "Sign";
	String PLAYER_PROPERTY_LASTLOGINTIME4NOTICE  = "LastLoginTime4Notice";
	String PLAYER_PROPERTY_SHOWPOPNOTICE  = "ShowPopNotice";
	String PLAYER_PROPERTY_FREECHANGENAME  = "FreeChangeName";
	String PLAYER_PROPERTY_ISNEW  = "IsNew";
	String PLAYER_PROPERTY_TOTALDEBRISGOLDENSTORM  = "TotalDebrisGoldenStorm";
	String PLAYER_PROPERTY_TOTALDEBRISCHUXI  = "TotalDebrisChuXi";
	String PLAYER_PROPERTY_REALNAME  = "RealName";//实名认证标志
	String PLAYER_PROPERTY_IDENTITYCARD  = "IdentityCard";//身份证
	String PLAYER_PROPERTY_BINDCHANNEL  = "BindChannel";//绑定渠道标志
	String PLAYER_PROPERTY_BINDPHONE_BEFORE  = "BindPhoneBefore";// 绑定手机号标志 只要绑定了一次就有
	String PLAYER_PROPERTY_MAIL_CAN_SEND_NUM  = "MailCanSendNum"; // 可以邮寄道具的数量
	String PLAYER_PROPERTY_INFOBG  = "InfoBg";
	String PLAYER_PROPERTY_FIRSTINROOM  = "FirstInRoom";
	String PLAYER_PROPERTY_ISIOS  = "IsIos";
	String PLAYER_PROPERTY_BOSSBETADD  = "BossBetAdd";
	String PLAYER_PROPERTY_BATTERYSKILLID  = "BatterySkillID";//炮台技能编号
	String PLAYER_PROPERTY_CHANGEROOM  = "ChangeRoom";
	String PLAYER_PROPERTY_BATTERYSKILL  = "BatterySkill";//使用炮台技能标志
	String PLAYER_PROPERTY_SKILLBULLETVAL  = "SkillBulletVal";
	String PLAYER_PROPERTY_SKILLFISHBET  = "SkillFishBet";
	String PLAYER_PROPERTY_LOCKFISH  = "LockFish";//锁定鱼
	String PLAYER_PROPERTY_LOCKEND  = "LockEnd";//锁定结束时间
	String PLAYER_PROPERTY_ENTERDESKTP = "EnterDeskTP";
	String PLAYER_PROPERTY_AUTOFIREEND = "AutoFireEnd";//自动发炮结束时间
	String PLAYER_PROPERTY_LASTHIT = "LastHit";//上次击杀的时间
	String PLAYER_PROPERTY_LASTCLEARTOTALCHARGE = "LastClearTotalCharge";//上次清累计充值
	String PLAYER_PROPERTY_HITBOSSSCORE = "HitBossScore";//击杀boss分数
	String PLAYER_PROPERTY_DICEENDTIME = "DiceEndTime";
	String PLAYER_PROPERTY_BATTERYINUSEBACKUP = "BatteryInUseBackup";//上一次使用的炮台编号
	String PLAYER_PROPERTY_USEDPROTECTPLAY = "UsedProtectPlay";//充值保护
	String PLAYER_PROPERTY_MAXPROTECTPLAY = "MaxProtectPlay";//最大充值保护
	String PLAYER_PROPERTY_ITEMSCORE = "ItemScore";//道具积分
	String PLAYER_PROPERTY_RECHARGESCORE = "RechargeScore";// 充值加的道具积分上限
	String PLAYER_PROPERTY_INITCOLORTICKET  = "InitColorTicket";
	String PLAYER_PROPERTY_BINDPROXY  = "bindProxy";//绑定代理标志
	String PLAYER_PROPERTY_SKILLMULTIPLE  = "SkillMultiple"; //技能倍数
	String PLAYER_PROPERTY_DAYCARDBUYLASTTIME  = "DayCardBuyLastTime";//日卡上次购买时间
	String PLAYER_PROPERTY_GMTARGET  = "GmTarget";
	String PLAYER_PROPERTY_GMMASTER  = "GmMaster";
	String PLAYER_PROPERTY_ROLEID  = "RoleId";
	String PLAYER_PROPERTY_LASTMAIL  = "LastMail";
	String PLAYER_PROPERTY_MONTHCARDEXPIREDATE  = "MonthCardExpireDate";
	String PLAYER_PROPERTY_GETMONTHCARDDATE  = "GetMonthCardDate";
	String PLAYER_PROPERTY_NOTIFYMAILDAY  = "NotifyMailDay";
	String PLAYER_PROPERTY_PATCHID  = "PatchID";
	String PLAYER_PROPERTY_STATISTICSLASTLOGINTIME  = "StatisticsLastLoginTime";
	String PLAYER_PROPERTY_LASTVERSION  = "LastVersion";
	//助力礼包
	String PLAYER_PROPERTY_ASSISTPKGVERSION  = "AssistPKGVersion";
	String PLAYER_PROPERTY_ASSISTPKGCLEARTIME  = "AssistPKGClearTime";
	//弹头礼包
	String PLAYER_PROPERTY_BOMBGIFTVERSION  = "BombGiftVersion";
	String PLAYER_PROPERTY_BOMBGIFTDAILY  = "BombGiftDaily";
	//圣诞节
	String PLAYER_PROPERTY_CHRISTMASVERSION  = "ChristmasVersion";
	String PLAYER_PROPERTY_CHRISTMASGIFTID  = "ChristmasGiftId";
	//每日抽奖
	String PLAYER_PROPERTY_DAILYDRAWTASKDATE  = "DailyDrawTaskDate";
	String PLAYER_PROPERTY_DAILYDRAWVERSION  = "DailyDrawVersion";
	String PLAYER_PROPERTY_DAILYDRAWCOUNT  = "DailyDrawCount";
	//鱼分排行
	String PLAYER_PROPERTY_FISHSCOREVER  = "FishScoreVer";
	String PLAYER_PROPERTY_HIGHFISHSCORE  = "HighFishScore";
	//登陆送好礼
	String PLAYER_PROPERTY_LOGINDATE  = "LoginDate";
	String PLAYER_PROPERTY_LOGINVERSION  = "LoginRewardVersion";
	//幸运翻牌
	String PLAYER_PROPERTY_LUCKYCARDRECHARGESCORE  = "LuckyCardRechargeScore";
	String PLAYER_PROPERTY_LUCKYCARDLUCK  = "LuckyCardLuck";
	String PLAYER_PROPERTY_LUCKYCARDINVERTSTATE  = "LuckyCardInvertState";
	String PLAYER_PROPERTY_LUCKYCARDVERSION  = "LuckyCardVersion";
	String PLAYER_PROPERTY_LUCKYCARDCLEARTIME  = "LuckyCardClearTime";
	String PLAYER_PROPERTY_LUCKYCARDREFRESHTIMES  = "LuckyCardRefreshTimes";
	//砸金蛋活动
	String PLAYER_PROPERTY_GOLDENEGGVER  = "GoldenEggVer";
	String PLAYER_PROPERTY_GOLDENEGGSCORE  = "GoldenEggScore";
	String PLAYER_PROPERTY_GOLDENEGGRECHARGEVALUE  = "GoldenEggRechargeValue";
	String PLAYER_PROPERTY_LUCKYNUMBER  = "LuckyNumber";
	String PLAYER_PROPERTY_HITEGGCOUNT  = "HitEggCount";
	//大富翁活动
	String PLAYER_PROPERTY_MONOPOLYRECHARGESCORE = "MonopolyRechargeScore";//该活动充值积分
	String PLAYER_PROPERTY_MONOPOLYCURRENTSTEP = "MonopolyCurrentStep";//当前所处格子
	String PLAYER_PROPERTY_MONOPOLYCURRENTROUND = "MonopolyCurrentRound";//当前轮次
	String PLAYER_PROPERTY_MONOPOLYLASTDICINGTIME = "MonopolyLastDicingTime";//上次掷骰子时间
	String PLAYER_PROPERTY_MONOPOLYVER = "MonopolyVer";//当前参加的活动版本
	String PLAYER_PROPERTY_MONOPOLYDICEPOINTS = "MonopolyDicePoints";//累计筛子点数
	String PLAYER_PROPERTY_MONOPOLYDICINGCOUNT = "MonopolyDicingCount";//掷骰子次数
	String PLAYER_PROPERTY_MONOPOLYMAXAWARD = "MonopolyMaxAward";//每轮可获得最大奖励
	String PLAYER_PROPERTY_MONOPOLYGETAWARD = "MonopolyGetAward";//本轮已获得奖励
	String PLAYER_PROPERTY_MONOPOLYTODAYCOLORTICKET = "MonopolyTodayColorTicket";//今天获得的话费券数量
	//神秘礼包
	String PLAYER_PROPERTY_MYSTERYVER = "MysteryVer";
	String PLAYER_PROPERTY_MYSTERYDAILYVER = "MysteryDailyVer";
	String PLAYER_PROPERTY_MYSTERYREFRESHVER = "MysteryRefreshVer";
	//高达礼包
	String PLAYER_PROPERTY_GUNDAM_PKG = "GumDamStatus"; // 是否已经购买高达礼包
	//补给礼包
	String PLAYER_PROPERTY_SUPPORTVERSION = "SupportVersion";
	String PLAYER_PROPERTY_SUPPORTGIFTID = "SupportGiftId";
	String PLAYER_PROPERTY_SUPPLYPKGCLEARTIME = "SupplyPKGClearTime";
	//任务赢话费
	String PLAYER_PROPERTY_TICKETTASKVER = "ticketTaskVer";

	//防沉迷
	String PLAYER_PROPERTY_ONLINETIMESTAMP = "OnlineTimeStamp";
	String PLAYER_PROPERTY_ALREADYPLAYTIME = "AlreadyPlayTime";
	String PLAYER_PROPERTY_ALREADYPLAYTIMETICKET = "AlreadyPlayTimeTicket";
	String PLAYER_PROPERTY_MONTHRECHARGEAMOUNT = "MonthRechargeAmount";
	String PLAYER_PROPERTY_MONTHRECHARGEAMOUNTDATE = "MonthRechargeAmountDate";
	//锁定的自动测试
	String PLAYER_PROPERTY_AUTOLOCKTESTFLAG = "autoLockTestFlag";
	//三太子
	String PLAYER_PROPERTY_STZGOLDWIN = "STZGoldWin";
	String PLAYER_PROPERTY_STZGOLDLOSE = "STZGoldLose";
	//宝藏轮盘
	String PLAYER_PROPERTY_GETFUNCFISHREWARDSTATUS = "GetFuncFishRewardStatus";
	String PLAYER_PROPERTY_FUNCFISH1REWARD = "FuncFish1Reward";
	//限购礼包
	String PLAYER_PROPERTY_LIMITVERSION = "LimitVersion";

	String PLAYER_PROPERTY_LASTLEAVETIME = "LastLeaveTime";
	String PLAYER_PROPERTY_LASTSITTIME = "LastSitTime";
	String PLAYER_PROPERTY_ONLINETIME = "OnlineTime";
	String PLAYER_PROPERTY_ONLINEGIFTID = "OnlineGiftId";
	String PLAYER_PROPERTY_LASTLOGINONLINETIME = "LastLoginOnlineTime";
	//7日礼包
	String PLAYER_PROPERTY_LASTLOGINDATE = "LastLoginDate";
	//炮台&皮肤
	String PLAYER_PROPERTY_TIMELIMIT = "TimeLimit";
	String PLAYER_PROPERTY_SKINID = "SkinID";
	String PLAYER_PROPERTY_BATTERYINUSE = "BatteryInUse";
	String PLAYER_PROPERTY_BATTERY_SPEED = "BatteryAttackSpeed";
	String PLAYER_PROPERTY_ACTIVITY_SPEED = "BatteryActivitySpeed";
	String PLAYER_PROPERTY_BATTERY_FUNC1 = "BatteryFuncFlag1";
	String PLAYER_PROPERTY_BATTERY_FUNC2 = "BatteryFuncFlag2";
	String PLAYER_PROPERTY_BATTERY_AUTO = "BatteryAutoTime";
	String PLAYER_PROPERTY_BATTERY_AUTO_COOL = "BatteryAutoCooling";
	String PLAYER_PROPERTY_BATTERY_STORE = "BatteryStore";

	String PLAYER_PROPERTY_ADDITIONKEEPTIME = "AdditionKeepTime";
	String PLAYER_PROPERTY_ADDITIONEXPIRETIME = "AdditionExpireTime";
	String PLAYER_PROPERTY_DISCOUNTSKEEPTIME = "DiscountsKeepTime";
	String PLAYER_PROPERTY_DISCOUNTSEXPIRETIME = "DiscountsExpireTime";
	String PLAYER_PROPERTY_TOTALCOLORTICKETFORAC = "TotalColorTicketForAC";
	//属性榜
	String PLAYER_PROPERTY_TOTALHITFISHSCORE = "TotalHitFishScore";
	String PLAYER_PROPERTY_SINGLEHITFISHSCORE = "SingleHitFishScore";
	String PLAYER_PROPERTY_USEGOLD = "UseGold";
	String PLAYER_PROPERTY_HITFISHPOINTS = "HitFishPoints";
	//成就
	String PLAYER_PROPERTY_ACHIEVEMENTPOINT = "AchievementPoint";

	String PLAYER_PROPERTY_RECRUITTOKEN = "RecruitToken";
	String PLAYER_PROPERTY_RECRUITER = "Recruiter";
	String PLAYER_PROPERTY_RECRUITERSHARECODE = "RecruiterShareCode";
	String PLAYER_PROPERTY_EXCHANGECARD = "ExchangeCard";
	String PLAYER_PROPERTY_GETEXCHANGECARD = "GetExchangeCard";
	String PLAYER_PROPERTY_SHARECODE = "ShareCode";
	String PLAYER_PROPERTY_OFFLINETIME = "OffLineTime";
	String PLAYER_PROPERTY_WRITETABLE = "WriteTable";

	String PLAYER_PROPERTY_MAHJONGSCORE = "MahjongScore";
	String PLAYER_PROPERTY_TINDERSCORE = "TinderScore";
	String PLAYER_PROPERTY_MAHJONGSCORE_S = "MahjongScore_s";
	String PLAYER_PROPERTY_TINDERSCORE_S = "TinderScore_s";
	String PLAYER_PROPERTY_FISH_POND_MOJING_SCORE = "FishPondGoldMojingScore";

	String PLAYER_PROPERTY_TIMEBOMBID = "TimeBombId";
	String PLAYER_PROPERTY_TIMEBOMBBV = "TimeBombBv";

	String PLAYER_PROPERTY_SPIRITSTONESCORE = "SpiritStoneScore";
	String PLAYER_PROPERTY_LASTSTONE2EXC = "LastStone2Exc";
	String PLAYER_PROPERTY_STONEEXCHANGECOUNT = "StoneExchangeCount";
	String PLAYER_PROPERTY_STONE2BRONZEEXCHANGECOUNT = "Stone2BronzeExchangeCount";
	String PLAYER_PROPERTY_STONE2SILVEREXCHANGECOUNT = "Stone2SilverExchangeCount";
	String PLAYER_PROPERTY_STONE2GOLDEXCHANGECOUNT = "Stone2GoldExchangeCount";
	String PLAYER_PROPERTY_STONE2PLATINUMEXCHANGECOUNT = "Stone2PlatinumExchangeCount";
	String PLAYER_PROPERTY_STONE2DIAMONDEXCHANGECOUNT = "Stone2DiamondExchangeCount";
	String PLAYER_PROPERTY_STONE2NBOMBAMOUNT = "Stone2NBombAmount";
	String PLAYER_PROPERTY_STONE2HBOMBAMOUNT = "Stone2HBombAmount";
	String PLAYER_PROPERTY_TODAYSTONE2NBOMBAMOUNT = "TodayStone2NBombAmount";
	String PLAYER_PROPERTY_TODAYSTONE2HBOMBAMOUNT = "TodayStone2HBombAmount";
	String PLAYER_PROPERTY_LASTSTONE2TIME = "LastStone2Time";
	String PLAYER_PROPERTY_STONECLEARTIME = "StoneClearTime";
	//商城&充值
	String PLAYER_PROPERTY_TOTALRECHARGEAMOUNT = "TotalRechargeAmount";
	String BombValueTotal = "BombValueTotal";
	String BombMiniGameValueTotal = "BombMiniGameValueTotal";
	String PLAYER_PROPERTY_TOTALRECHARGEAMOUNT_COUNPONS = "TotalRechargeAmountCoupons";
	String PLAYER_PROPERTY_ALREADYRECHARGED = "AlreadyRecharged";
	String PLAYER_PROPERTY_FIRSTTEMPCELLPHONE = "FirstTempCellphone";
	String PLAYER_PROPERTY_LASTTEMPCELLPHONE = "LastTempCellphone";
	//每日任务
	String PLAYER_PROPERTY_DAILYTASKASSIGNLASTTIME = "DailyTaskAssignLastTime";//上次任务分配时间
	String PLAYER_PROPERTY_DAILYTASKPOINTS = "DailyTaskPoints";//每日任务积分
	String PLAYER_PROPERTY_DAILYTASKHASAWARDEDPOINTS = "DailyTaskHasAwardedPoints";//已经领取额外奖励时的积分

	//周任务
	String PLAYER_PROPERTY_WEEKLYTASKASSIGNLASTTIME = "WeeklyTaskAssignLastTime";//上次任务分配时间
	String PLAYER_PROPERTY_WEEKLYTASKPOINTS = "WeeklyTaskPoints";//每周任务积分
	String PLAYER_PROPERTY_WEEKLYTASKHASAWARDEDPOINTS = "WeeklyTaskHasAwardedPoints";//已经领取额外奖励时的积分

	String PLAYER_PROPERTY_GOLD_HELICOPTERPKGID="GoldHelicopterPKGId";//金币场直升礼包

	String PLAYER_PROPERTY_MJ_HELICOPTERPKGID="MojinHelicopterPKGId";//魔晶场直升礼包


	//海底寻宝
	String PLAYER_PROPERTY_NUMBEROFTREASUREINSEA="NumberOfTreasureInSea"; //海底寻宝剩余次数
	String PLAYER_PROPERTY_NUMBEROFDONE="NumberOfDone"; //海底寻宝已抽次数
	String PLAYER_PROPERTY_TIMEOFTREASUREINSEA="TimeOfTreasureInSea";  //上次寻宝时间
	String PLAYER_PROPERTY_GETREWARDSTATUS="GetRewardStatus";  //奖励领取状态
	String PLAYER_PROPERTY_REWARDITEMID="RewardItemId";  //奖励 Id
	String PLAYER_PROPERTY_REWARDCOUNT="RewardCount";  //奖励数量

	String ITEM_PROPERTY_SKILL_HBOMB  = "item_skill_hbomb";  	// 传说三叉戟
	String ITEM_PROPERTY_SKILL_NBOMB  = "item_skill_nbomb";  	// 至尊三叉戟
	String ITEM_PROPERTY_DEBRIS_HBOMB = "item_debris_hbomb";  	// 传说三叉戟碎片
	String ITEM_PROPERTY_DEBRIS_NBOMB = "item_debris_nbomb";  	// 至尊三叉戟碎片

	// 公会&仓库
	String PLAYER_PROPERTY_GUILD_ID = "Guild_ID"; 	// 公会id
	String PLAYER_PROPERTY_LAST_QUIT_GUILD_TIME = "LastQuitGuildTime"; 	// 上次退出公会的时间
	String PLAYER_PROPERTY_STORE_GUILD_REPO_TOTAL_COUNT = "StoreGuildRepoTotalCount"; 	// 存入公会仓库的总数量(至尊)
	String PLAYER_PROPERTY_STORE_GUILD_REPO_USED_COUNT= "StoreGuildRepoUsedCount"; 	// 存入公会仓库已使用的数量(至尊)

	String PLAYER_PROPERTY_ACCUM_RECHARGE_SER = "AccumRechargeSer";  	// 累计充值送好礼活动版本
	String PLAYER_PROPERTY_DOUBLE_FESTIVAL_VER = "DoubleFestivalVer";  	// 双节活动版本
	String PLAYER_PROPERTY_DOUBLE_FESTIVAL_DAILY_VER = "DoubleFestivalDailyVer";  	// 双节活动每日版本

	String PLAYER_PROPERTY_LAST_GET_ERROR_TIME  = "LastGetErrorTime";//上次从公会仓库取道具密码错误时间
	String PLAYER_PROPERTY_LAST_GET_ERROR_COUNT = "LastGetErrorCount";//每日从公会仓库取道具已错误次数


	// 新手留存活动
	String PLAYER_GOT_IQIYI_CARD20  = "GotIQiyiCard20";// 10元话费兑换是否已给
	String PLAYER_ROOKIE_ACT_LEFT_TIME  = "RookieActLeftTime";

	String PLAYER_PROPERTY_LUCKY_PUZZLE_VER = "LuckyPuzzleVer";// 幸运拼图活动版本
	String PLAYER_PROPERTY_LUCKY_PUZZLE_DAILY_VER = "LuckyPuzzleDailyVer";// 幸运拼图活动每日版本

	String PLAYER_PROPERTY_ACCUM_RECHARGE_GOINGMERRY_VER = "AccumRechargeGoingMerryVer";// 累计充值送暗金梅丽活动版本

	String PLAYER_PROPERTY_DOUBLE_11_EXCHANGE_MALL_VER = "Double11ExchangeMallVer";// 双十一商城活动活动版本
	String PLAYER_PROPERTY_DOUBLE_11_EXCHANGE_MALL_DAILY_VER = "Double11ExchangeMallDailyVer";// 双十一商城活动每日活动版本
	String PLAYER_PROPERTY_11_COIN = "11Coin";// 十一币

	// 养鱼池
	String PLAYER_PROPERTY_SELF_FISH_ID = "SelfFishId";// 玩家自己投放鱼的id
	String PLAYER_PROPERTY_OPEN_FISH_POND_FLAG = "OpenActPageFlag";// 玩家有没有打开活动页面
	String PLAYER_PROPERTY_IS_FIRST_OPEN_POND = "IsFirstOpenFond";// 玩家第一次打开鱼池
	String PLAYER_PROPERTY_FISH_PONG_VER = "FishPondVer";// 版本

	String PLAYER_PROPERTY_POPUP_MANAGE_LAST_LOGIN_TIME = "PopupManageLastLoginTime";// 弹窗管理上次登录时间

	String PLAYER_PROPERTY_DEBRIS_PKG_DAILY_VER = "DebrisPkgDailyVer";// 三叉戟红包活动每日活动版本
	String PLAYER_PROPERTY_DEBRIS_PKG_VER = "DebrisPkgVer";// 三叉戟红包活动活动版本
	String PLAYER_PROPERTY_DEBRIS_PKG_OPENED_TIMES = "DebrisPkgOpenedTimes";// 三叉戟红包活动已开次数
	String PLAYER_PROPERTY_DEBRIS_PKG_REMAIN_TIMES_DAILY = "DebrisPkgRemainTimesDaily";// 三叉戟红包活动每日可领次数
	String PLAYER_PROPERTY_DEBRIS_PKG_DEBRIS_GOT_COUNT = "DebrisPkgDebrisGotCount";// 三叉戟红包活动传说碎片已领数量
	String PLAYER_PROPERTY_DEBRIS_PKG_REWARD_GOT_COUNT = "DebrisPkgRewardGotCount";// 三叉戟红包活动奖励领取次数
	String PLAYER_PROPERTY_DEBRIS_PKG_IS_FIRST_RECHARGE = "DebrisPkgIsFirstRecharge";// 三叉戟红包活动记录是否在活动内首冲
	String PLAYER_PROPERTY_DEBRIS_PKG_VIP_12_DEBRIS_INCREASE = "DebrisPkgVip12DebrisIncrease";// 三叉戟红包活动贵族12后每充值碎片上限增加
	String PLAYER_PROPERTY_DEBRIS_PKG_VIP_12_OPEN_TIMES_INCREASE = "DebrisPkgVip12OpenTimesIncrease";// 三叉戟红包活动贵族12后每充值可领取总次数增加
	String PLAYER_PROPERTY_DEBRIS_PKG_NEW_PLAYER_DISPLAY = "DebrisPkgNewPlayerDisplay";// 三叉戟红包新注册玩家不再展示

	String PLAYER_PROPERTY_MERMAID_TREASURE_VER = "MermaidTreasureVer";// 美人鱼秘宝活动活动版本
	String PLAYER_PROPERTY_UNLOCK_MERMAID_BOX_INDEX = "UnlockMermaidBoxIndex";// 解锁美人鱼宝箱的层数

	String PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP = "PlayerEnterNuclearRoomTimestamp";// 玩家进入魔晶场的时间戳
	String PLAYER_PROPERTY_PROPERTY_PROXY_ID = "ProxyId";//绑定代理标志

	String PLAYER_PROPERTY_REGISTER_TYPE = "RegisterType";//注册方式 2-手机号注册
	String PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME = "SysRealNameAuthEndTime";//认证中状态下 系统结束认证的时间戳

	//* 秘境传说活动 *//
	String PLAYER_PROPERTY_MYSTERY_LEGEND_VERSION = "MysteryLegendVersion";// 活动版本
	String PLAYER_PROPERTY_MYSTERY_LEGEND_DAILY_VERSION = "MysteryLegendDailyVersion";// 每日活动版本
	String PLAYER_PROPERTY_MYSTERY_LEGEND_ENERGY = "MysteryLegendEnergy";// 展示给客户端秘境值
	String PLAYER_PROPERTY_MYSTERY_LEGEND_GOLD_ENERGY = "MysteryLegendGoldEnergy";// 金币场秘境值
	String PLAYER_PROPERTY_MYSTERY_LEGEND_NUCLEAR_ENERGY = "MysteryLegendNuclearEnergy";// 海神殿秘境值
	String PLAYER_PROPERTY_MYSTERY_LEGEND_PKG_EXPIRE_DATE = "MysteryLegendPkgExpireDate";// 秘境传说礼包到期日期
	String PLAYER_PROPERTY_MYSTERY_LEGEND_DRAGON_BALL = "MysteryLegendDragonBall";// 玩家拥有秘境龙珠数量

	//奖券鱼掉落的奖券累计值
	String PLAYER_PROPERTY_TOTAL_DROP_LOTTERY_FISH = "totalDropLotteryFish";

	//* 每日超值技能大礼包 *//
	String PLAYER_PROPERTY_DAILY_VALUE_SKILL_PKG_VERSION = "DailyValueSkillPKGVersion"; // 活动版本
	String PLAYER_PROPERTY_DAILY_VALUE_SKILL_PKG_DAILY_VERSION = "DailyValueSkillPKGDailyVersion"; // 每日活动版本

	/**
	 * 新增广告渠道4个属性
	 */
	String PLAYER_PROPERTY_ADS_TIME     = "adsStartTime";
	String PLAYER_PROPERTY_ADS_WIN_GOLD = "adsWinGold";
	String PLAYER_PROPERTY_ADS_WIN_BOMB = "adsWinBomb";
	String PLAYER_PROPERTY_ADS_CHARGE_VALUE = "adsChargeValue";

	/**
	 * C++新的算法
	 */
	String PLAYER_PROPERTY_SF_COUNT     = "amendGameDfSelfCount";
	String PLAYER_PROPERTY_SF_VALUE     = "amendGameDfSelfValue";
	String ROOM_PROPERTY_SF_COUNT       = "amendGameDfRoomCount";
	String ROOM_PROPERTY_SF_VALUE       = "amendGameDfRoomValue";

	//信息框已启用
	String PLAYER_PROPERTY_USE_INFO     = "InfoBgInUse";

	//渔场鱼记录
	String DESK_FISH_LIST = "FishList";

	//渔场鱼数量
	String DESK_FISH_COUNT = "FishCountRec";

	//渔场鱼索引
	String DESK_FISH_INDEX = "LastFishIndex";

	//房间类型
	String DESK_TYPE_KEY = "Type";

	//世界boss离场时间
	String DESK_BOSS_LEAVE_TIME = "BoosLeaveTime";
    //桌子休眠时间不出鱼
    String DESK_NOT_OUT_FISH_END_TIME = "DeskNotOutFishEndTime";

	//山海异兽志 活动日志
	String PLAYER_PROPERTY_RAREBONE   = "RareBone";//宝骨

	String PLAYER_PROPERTY_TPR_REWARD_IDS ="tprRewardId";  //龟相来贺中奖编号
	String PLAYER_PROPERTY_LDK_REWARD_IDS ="ldkRewardId";  //龙王遗赠中奖编号

	String PLAYER_PROPERTY_COUPONS = "Coupons";//点券与货币1:10

	String PLAYER_PROPERTY_HAVENBOMB_SCORE="haveNbombScore";//拥有至尊三叉戟积分
	String PLAYER_PROPERTY_HAVEHBOMB_SCORE="haveHbombScore";//拥有传说三叉戟积分
	String PLAYER_PROPERTY_GOLD_BURST="goldBurst";//金币爆发
	String PLAYER_PROPERTY_BOMBCOIN_BURST="bombCoinBurst";//魔晶爆发

	String PLAYER_PROPERTY_REAL_INFO_NAME  = "RealInfoName";//实物邮寄名称
	String PLAYER_PROPERTY_REAL_INFO_PHONE = "RealInfoPhone";//实物邮寄电话
	String PLAYER_PROPERTY_REAL_INFO_ADDRESS = "RealInfoAddress";//实物邮寄地址

	String VIP_LEVEL_GIFT_PUB_DATA = "VipLevelGiftPubData";//vip成长礼包公共区
	String VIP_LEVEL_GIFT_REC_NAME = "VipLevelGiftPubRec";

	String SPIRIT_STONE_FIGHT_VALUE = "SpiritStoneFightValue";//灵石大作战
	String SPIRIT_STONE_FIGHT_PUB_DATA = "SpiritStoneFightPubData";//灵石大作战公共区
	String SPIRIT_STONE_FIGHT_REC_NAME = "SpiritStoneFightRankRec";//灵石大作战排行榜




	String PLAYER_REDIS_PUB_INFO="PlayerRedisPubInfo";
    String PLAYER_HIT_FISH_LAST_TIME = "PlayerHitFishLastTime";//发射子弹的最终时间
    String PLAYER_SKILL_SPEED_END_TIME = "PlayerSkillSpeedEndTime";//释放加速的最终时间
    String PLAYER_ON_ENTER_ROOM_END_TIME = "PlayerOnEnterRoomEndTime";//进入房间的最后时间
    String PLAYER_CURRENT_PLACE = "PlayerCurrentPlace";//当前所在位置，防止进入多个游戏
    //水浒传
    String PLAYER_SHZ_SINGLE_SCORE = "PlayerShzSingleScore";
    String PLAYER_SHZ_BET_NUM = "PlayerShzBetNum";
    String PLAYER_SHZ_BONUS_NUM = "PlayerShzBonusNum";
    String PLAYER_SHZ_MARRY_NUM = "PlayerShzMarryNum";
    String PLAYER_SHZ_LAST_TIME = "PlayerShzLastTime";
    String PLAYER_SHZ_OBJECT = "PlayerShzObject";
    String BombShzGameValueTotal = "BombShzGameValueTotal";

    //邀请
    String PLAYER_INVITER_VIP_INFO_CACHE = "PlayerInviterVipInfoCache";//vip所有信息缓存
    String PLAYER_INVITER_VIP_INFO_CACHE_TIME = "PlayerInviterVipInfoCacheTime";//vip所有信息缓存time
    String PLAYER_INVITER_VIP_STATUS = "PlayerInviterVipStatus";//vip状态
    String PLAYER_INVITER_BIND_ID = "PlayerInviterBindId";//绑定的id

    //炮台
    String PLAYER_GUN_EQUIP = "PlayerGunEquip";//当前装备的炮台
    String PLAYER_GUN_OWNED = "PlayerGunOwned";//拥有的

    //玩法鱼
    String ROOM_PROPERTY_FUNCTION_FISH_HIT_NUM = "RoomFunctionFishHitNum";


    String PLAYER_LAST_PW_DESKID = "PlayerLastPwDeskId";


    String PLAYER_LAST_TIME_GOT_PRIZE = "PlayerLastTimeGotPrize";//上次领取奖励时间
    String PLAYER_PRIZE_LEVEL = "PlayerPrizeLevel";//奖励等级或奖励进度等级
    String PLAYER_MAX_LEVEL_INCOME = "PlayerMaxLevelIncome";//单关最高收益
    String PLAYER_DOUBLE_COINS_PURCHASED = "PlayerDoubleCoinsPurchased";//是否购买双倍金币
    String PLAYER_LEVELS_COMPLETED = "PlayerLevelsCompleted";//已完成关卡数量
    String PLAYER_MONEY = "PlayerMoney";//金币
    String PLAYER_WEAPONS_LEVELS = "PlayerWeaponsLevels";//各武器等级数组
    String PLAYER_TANK_SPEED_LEVEL = "PlayerTankSpeedLevel";//坦克速度等级
    String PLAYER_TANK_ARMOR_LEVEL = "PlayerTankArmorLevel";//坦克护甲等级
    String PLAYER_IS_PAYING = "PlayerIsPaying";//是否付费玩家标记

    // 成就累计数量（key-value 字符串：achievementId=count;...）
    String PLAYER_PROPERTY_ACHIEVEMENT_TOTAL_COUNTS = "AchievementTotalCounts";
    // 成就奖励领取状态（key-value 字符串：achievementId=mask;...，mask 的 bit0~bit4 分别表示 lv1~lv5 已领取）
    String PLAYER_PROPERTY_ACHIEVEMENT_REWARD_CLAIMED_MASKS = "AchievementRewardClaimedMasks";




//    REQ 10000 +
//    C2S 20000 +
//    S2C 30000 +
}
