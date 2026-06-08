/**   
*    
* 描述：   
* 文件：BaseSkill.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 下午4:27:27 
*    
*/
package game.modules.skills;

import framework.PropertyKey;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.modules.fishgame.FishModule;

/**   
*    
* 描述：   
*    
*/
public class BaseSkill implements PropertyKey {
	protected int m_type;
    protected FishModule m_FishModule = null;

    public boolean OnInit(IKernel kernel) {
        m_FishModule = (FishModule) kernel.getModule("FishModule");
        return true;
    }
	
	public void SetType(int type)
	{
		m_type = type;
	}

	BaseSkillData LoadConfig(ICfgReader cfg, int index)
	{
		BaseSkillData data = new BaseSkillData();
		data.type = m_type;
		//data.time = cfg.GetInt(index, "Time");
		return data;
	}
	
	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData data)
	{
		
	}
	
	public void OnReady(IKernel kernel, IGameObject player, BaseSkillData data, Object object) {
	
	}
}
