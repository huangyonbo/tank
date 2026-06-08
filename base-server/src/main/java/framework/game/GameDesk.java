package framework.game;

import framework.game.Kernel.PlayerListCols;
import lombok.Data;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * 描述： 桌子对象
 *
 */
public class GameDesk extends GameObject {
    public GameDesk(Kernel kernel) {
        super(kernel);
        m_Type = GameObjectType.GOTYPE_DESK;
    }

    private Queue<Boss> _boss = new PriorityQueue<>();

    @Data
    public class Boss implements Comparable {
        String fishId;
        long timeOut;
        int order;

        @Override
        public int compareTo(Object o) {
            Boss o1 = (Boss) o;
            if (order == o1.order) {
                return 0;
            } else if (order < o1.order) {
                return 1;
            } else {
                return -1;
            }

        }
    }

    @Override
    public void initInnerData() {
        super.initInnerData();

        declareProperty("SeatCount", ValueType.INT, true, false, false);
        declareProperty("IsPwd", ValueType.INT, true, false, false);
        declareProperty("Pwd", ValueType.STRING, true, false, false);

        IRecord rec = declareRecord("PlayerList", 1, 0, false, false, false);
        rec.setColType(PlayerListCols.COL_OBJECTID.ordinal(), ValueType.LONG);
    }

    public boolean addBoss(String fishId, int order, long timeOut) {
        Boss boss = new Boss();
        boss.timeOut = timeOut;
        boss.order = order;
        boss.fishId = fishId;
        return _boss.offer(boss);
    }

    public Boss getBoss() {
        return _boss.poll();
    }

    public Queue getQueue() {
        return _boss;
    }

    @Override
    public void onLoad() {
        int seatCount = (int) getProperty("SeatCount");
        Record rec = (Record) getRecord("PlayerList");
        rec.setMaxRow(seatCount);

        for (int i = 0; i < seatCount; ++i) {
            rec.addRow(0L);
        }

        super.onLoad();
    }

    @Override
    public void onDestroy() {
        IRecord rec = getRecord("PlayerList");
        int maxRow = rec.getMaxRow();
        for (int i = 0; i < maxRow; ++i) {
            long objid = rec.getLong(i, PlayerListCols.COL_OBJECTID.ordinal());
            GameObject player = m_kernel.getGameObject(objid);
            if (player != null) {
                m_kernel.standUp(player);
            }
        }
        super.onDestroy();
    }

    @Override
    public int getSeatCount() {
        IRecord rec = getRecord("PlayerList");
        return rec.getMaxRow();
    }

    @Override
    public int getFreeSeatCount() {
        IRecord rec = getRecord("PlayerList");
        int count = 0;
        int maxRow = rec.getMaxRow();
        for (int i = 0; i < maxRow; i++) {
            if (rec.getLong(i, PlayerListCols.COL_OBJECTID.ordinal()) == 0L) {
                ++count;
            }
        }
        return count;
    }

    @Override
    public GameObject getSeatObject(int seatid) {
        IRecord rec = getRecord("PlayerList");
        int maxRow = rec.getMaxRow();
        if (seatid < 0 || seatid >= maxRow) {
            return null;
        }
        long objid = (long) rec.getValue(seatid, PlayerListCols.COL_OBJECTID.ordinal());
        return m_kernel.getGameObject(objid);
    }
}
