/**   
*    
* 描述：   局部炸弹
* 文件：SmallBomb.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 下午4:31:40 
*    
*/
package game.modules.skills;

import com.google.protobuf.InvalidProtocolBufferException;

import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.modules.fishgame.FishModule;

/**
 * 
 * 描述：
 * 
 */
public class LocalBomb extends BaseSkill {
	FishModule m_FishModule;

	public boolean OnInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regClientMessage(C2SMsgDef.C2S_LOCAL_BOMB_KILLFISH.ordinal(), this, "OnRecvLocalBomb");
		m_FishModule = (FishModule) kernel.getModule("FishModule");
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		IRecord rec = kernel.declareRecord(script, "LocalBombRec", 1, 10, false, false, false);
		rec.setColType(0, ValueType.INT);
	}

	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
		// record tag
		int fishIndex = (int) target;
		IRecord rec = player.getRecord("LocalBombRec");
		rec.addRow(fishIndex);
	}

	public void OnRecvLocalBomb(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {
	}
}
