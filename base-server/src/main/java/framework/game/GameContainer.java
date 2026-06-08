package framework.game;

import com.google.protobuf.ByteString;
import framework.net.ClientMsgDef;
import framework.net.message.ClientMsg;
import org.apache.mina.core.buffer.IoBuffer;

import java.util.Arrays;

/**
 * 描述： 容器对象
 */
public class GameContainer extends GameObject {
    protected int m_capacity = 0;
    protected int m_viewid = -1;

    public GameContainer(Kernel kernel) {
        super(kernel);
        m_Type = GameObjectType.GOTYPE_CONTAINER;
    }

    public void initInnerData() {
        declareProperty("Capacity", ValueType.INT, false, false, false);
    }

    public void innerSetCapacity(int capacity) {
        setProperty("Capacity", capacity);

        m_capacity = capacity;
        if (m_childs.size() < capacity) {
            for (int i = m_childs.size(); i < capacity; ++i) {
                m_childs.add(0l);
            }
        }
    }

    @Override
    public int getCapacity() {
        return m_capacity;
    }

    @Override
    public boolean addChild(int pos, IGameObject child) {
        if (pos < 0 || pos >= m_capacity) {
            return false;
        }
        if (m_childs.get(pos) != 0l) {
            return false;
        }
        IGameObject oldParent = child.getParent();
        if (oldParent != null) {
            oldParent.removeChild(child);
        }
        m_childs.set(pos, child.getObjectID());
        GameObject _child = (GameObject) child;
        _child.setParent(getObjectID());
        _child.setPos(pos);
        m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_ENTER, child.getScript(), child, this);
        if (m_viewid != -1) {
            ClientMsg.LoadVpitem.Builder loadvp = ClientMsg.LoadVpitem.newBuilder();
            loadvp.setViewid(m_viewid);
            loadvp.setPos(pos);
            loadvp.setData(ByteString.copyFrom(((GameObject) child).getSyncData(true)));
            m_kernel.innerSendMessage(getParent(), ClientMsgDef.CLIENT_LOAD_VPITEM.ordinal(), loadvp.build().toByteArray());
        }
        return true;
    }

    @Override
    public int addChild(IGameObject child) {
        for (int i = 0; i < m_capacity; ++i) {
            if (m_childs.get(i) == 0l) {
                if (this.addChild(i, child)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void removeChild(IGameObject child) {
        if (m_childs.get(child.getPos()) != child.getObjectID()) {
            return;
        }
        if (m_viewid != -1) {
            ClientMsg.RemoveVpitem.Builder removevp = ClientMsg.RemoveVpitem.newBuilder();
            removevp.setViewid(m_viewid);
            removevp.setPos(child.getPos());
            m_kernel.innerSendMessage(getParent(), ClientMsgDef.CLIENT_REMOVE_VPITEM.ordinal(), removevp.build().toByteArray());
        }
        m_kernel.getClassSet().runEvent(KernelEvent.KEVENT_ON_LEAVE, child.getScript(), child, this);
        m_childs.set(child.getPos(), 0l);
        GameObject obj = (GameObject) child;
        obj.setPos(-1);
        obj.setParent(0l);
    }

    public void setViewid(int viewid) {
        m_viewid = viewid;
    }

    public void syncChilds() {
        if (m_viewid == -1) {
            return;
        }

        ClientMsg.LoadVpitem.Builder loadvp = ClientMsg.LoadVpitem.newBuilder();
        loadvp.setViewid(m_viewid);

        for (int i = 0; i < m_capacity; ++i) {
            if (m_childs.get(i) == 0l) {
                continue;
            }
            IGameObject child = m_kernel.getGameObject(m_childs.get(i));
            if (child == null) {
                continue;
            }

            loadvp.setPos(i);
            loadvp.setData(ByteString.copyFrom(((GameObject) child).getSyncData(true)));
            m_kernel.innerSendMessage(getParent(), ClientMsgDef.CLIENT_LOAD_VPITEM.ordinal(),
                    loadvp.build().toByteArray());
        }
    }

    public void syncViewportPro(GamePlayer target) {
        IoBuffer buffer = IoBuffer.allocate(10);
        buffer.setAutoExpand(true);
        int count = getSyncProperty(null, buffer);
        short priCount = (short) (count >> 16);
        if (priCount > 0) {
            byte[] proData = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
            ClientMsg.PropertySync.Builder builder = ClientMsg.PropertySync.newBuilder();
            builder.setObjectId(getObjectID());
            builder.setCount(priCount);
            builder.setData(ByteString.copyFrom(proData));
            byte[] data = builder.build().toByteArray();
            m_kernel.innerSendMessage(target, ClientMsgDef.CLIENT_SYNC_PROPERTY.ordinal(), data);
        }
        for (int i = 0; i < m_capacity; ++i) {
            if (m_childs.get(i) != 0l) {
                GameObject obj = getChild(i);
                if (obj == null) {
                    continue;
                }
                buffer = buffer.position(0);
                count = obj.getSyncProperty(null, buffer);
                priCount = (short) (count >> 16);
                if (priCount > 0) {
                    byte[] proData = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
                    ClientMsg.SyncVpitemPro.Builder builder = ClientMsg.SyncVpitemPro.newBuilder();
                    builder.setViewid(m_viewid);
                    builder.setPos(i);
                    builder.setCount(priCount);
                    builder.setData(ByteString.copyFrom(proData));
                    byte[] data = builder.build().toByteArray();
                    m_kernel.innerSendMessage(target, ClientMsgDef.CLIENT_SYNC_VPITEM_PRO.ordinal(), data);
                }
            }
        }
    }
}
