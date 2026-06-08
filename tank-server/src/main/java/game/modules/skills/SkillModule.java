package game.modules.skills;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import framework.net.message.InnerMsg;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.ServerCodeDef;
import game.modules.GameModule;
import game.modules.items.ItemLogModule;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SkillModule implements ILogicModule
{
	public enum SkillType
	{
		TYPE_FROZEN,	// 0 冰冻
		TYPE_CLI_SHOW,	// 1 客户端效果
		TYPE_LOCAL_BOMB,// 2 局部炸弹
		TYPE_BIG_BOMB,	// 3 全屏炸弹
		TYPE_TURNTAB,	// 4 转盘
		TYPE_SAME_BOMB,	// 5 同类炸弹
		TYPE_BATTERY,	// 6 炮台技能
		TYPE_LOCK,		// 7 锁定
		TYPE_BOMB,		// 8 炸弹
		TYPE_TINDER,    // 9 火种
		TYPE_TIMEBOMB,  // 10 时限炸弹
		TYPE_SMALLBOSS, // 11 小型Boss
		TYPE_SUMMON,	// 12召唤
		TYPE_RAGE,		// 13狂暴
		TYPE_DOUBLE,	// 14翻倍

		TYPE_END        //12
	}
	public static final String BUFF_DATA = "BuffData";
	static Logger logger = LoggerFactory.getLogger(GameModule.class);

	private BaseSkill[] m_Skills = new BaseSkill[SkillType.TYPE_END.ordinal()];
	private Map<String, BaseSkillData> m_skillCfgs = new HashMap<>();
	@Override
	public boolean onInit(IKernel kernel) {
//
//		m_Skills[SkillType.TYPE_FROZEN.ordinal()] = new Frozen();        // 0 冰冻
//		m_Skills[SkillType.TYPE_CLI_SHOW.ordinal()] = new CliShow();     // 1 客户端效果
//		m_Skills[SkillType.TYPE_LOCAL_BOMB.ordinal()] = new LocalBomb(); // 2 局部炸弹
//		m_Skills[SkillType.TYPE_BIG_BOMB.ordinal()] = new BigBomb();     // 3 全屏炸弹
//		m_Skills[SkillType.TYPE_TURNTAB.ordinal()] = new TurnTable();    // 4 转盘
//		m_Skills[SkillType.TYPE_SAME_BOMB.ordinal()] = new SameBomb();   // 5 同类炸弹
//		m_Skills[SkillType.TYPE_BATTERY.ordinal()] = new Battery();      // 6 炮台技能
//		m_Skills[SkillType.TYPE_LOCK.ordinal()] = new Lock();            // 7 锁定
//		m_Skills[SkillType.TYPE_BOMB.ordinal()] = new Bomb();            // 8 炸弹
//		m_Skills[SkillType.TYPE_TINDER.ordinal()] = new Tinder();        // 9 火种
//		m_Skills[SkillType.TYPE_TIMEBOMB.ordinal()] = new TimeBomb();    // 10 时限炸弹
//		m_Skills[SkillType.TYPE_SMALLBOSS.ordinal()] = new OutSmallBoss(); // 11 小型Boss
//		m_Skills[SkillType.TYPE_SUMMON.ordinal()] = new Summon();
//		m_Skills[SkillType.TYPE_RAGE.ordinal()] = new Rage();
//		m_Skills[SkillType.TYPE_DOUBLE.ordinal()] = new Double();

//		for(int i = 0; i < SkillType.TYPE_END.ordinal(); ++i)
//		{
//			m_Skills[i].SetType(i);
//			if(!m_Skills[i].OnInit(kernel))
//			{
//				return false;
//			}
//		}
		
//		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
//		kernel.regClientMessage(C2SMsgDef.C2S_USE_SKILL_NOT_USE_ITEM.ordinal(), this, "OnUseSkillNotUseItem");
//		RefreshCfg(kernel, "res/Skill/Skill.xml");
		
		return true;
	}

	@Override
	public void onDestroy() {
		
	}
	
	void RefreshCfg(IKernel kernel, String path)
	{
		if(path.equals("res/Skill/Skill.xml"))
		{
			m_skillCfgs.clear();
			LoadConfig(kernel, path);
		}
	}
	
	boolean LoadConfig(IKernel kernel, String path)
	{
		ICfgReader cfg = kernel.loadXmlConfig(path);
		int count = cfg.getItemCount();
		for(int i = 0; i < count; ++i)
		{
			int type = cfg.getInt(i, "Type");
			if(type < 0 || type >= SkillType.TYPE_END.ordinal())
			{
				continue;
			}
			
			BaseSkillData data = m_Skills[type].LoadConfig(cfg, i);
			data.skillid = cfg.getString(i, "Id");
			if(data != null)
			{
				m_skillCfgs.put(cfg.getString(i, "Id"), data);
			}
		}
		return true;
	}

	/** 释放技能
	 * @param kernel
	 * @param player 释放者
	 * @param target 释放目标
	 * @param skillid 技能
	 */
	public void UseSkill(IKernel kernel, IGameObject player, Object target, String skillid)
	{

		if(!m_skillCfgs.containsKey(skillid))
		{
			 logger.info("UseSkill {} not found", skillid);
			return;
		}

		BaseSkillData data = m_skillCfgs.get(skillid);
		m_Skills[data.type].OnUse(kernel, player, target, data);
		kernel.command(player, CommandDef.CMD_USE_SKILL.ordinal(), skillid);
		if (skillid.equals("skill_nbomb") || skillid.equals("skill_hbomb") ) {
//			ItemLogModule.AddItemLog(kernel, player, "item_" + skillid, 1, ItemLogEnum.SKILL_ITEM_USE.ordinal());
			ItemLogModule.AddItemLog(kernel, player, "item_" + skillid, 1, "-", "渔场释放消耗");
		}
	}
	
	public BaseSkillData GetSkillData(String skillid)
	{
		if(!m_skillCfgs.containsKey(skillid))
		{
			return null;
		}
		return m_skillCfgs.get(skillid);
	}
	public void OnUseSkillNotUseItem(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
		InnerMsg.String skillId = InnerMsg.String.parseFrom(msg);
		UseSkill(kernel,player,player,skillId.getValue());
		UtilFunc.responseSerCode(kernel, player, msgid, ServerCodeDef.CODE_SUCCESS);
	}
}
