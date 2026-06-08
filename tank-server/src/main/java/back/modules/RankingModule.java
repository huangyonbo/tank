package back.modules;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.ranking.RankList;
import back.modules.data.ranking.Ranking;
import back.modules.data.ranking.RankingData;
import back.modules.dataenum.RankingType;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：排行榜
 * 
 */
public class RankingModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(RankingModule.class);

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(BacKernel kernel) {
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void getRankingData(BacKernel kernel, Ranking ranking, IDataCallBack cb) {
		List<RankingData> list = new ArrayList<RankingData>();

		int type = ranking.getType();
		int limit = ranking.getLimit();
		int start = ranking.getStart();
		logger.info("ranking request, type={}, limit={}, start={}, total={}", type, limit, start, ranking.getTotal());
		ServerMsg.IntSingle.Builder builder = ServerMsg.IntSingle.newBuilder();
		builder.setIntMember(type);
		if (type == RankingType.TREASURE.getId()) {

			kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_RANKING_DATA.ordinal(), builder.build().toByteArray(),
					(byte[] data) -> {
						logger.info("pub server return ranking data!!");
						ServerMsg.RankingDatas res;
						try {
							res = ServerMsg.RankingDatas.parseFrom(data);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
						List<ServerMsg.RankingData> rankingDataList = res.getRankingDatasList();
						if (rankingDataList.size() > start) {
							int max = (start + limit) > rankingDataList.size() ? rankingDataList.size()
									: (start + limit);
							logger.info("get {} - {} ranking data!", start, max - 1);
							for (int i = start; i < max; i++) {
								ServerMsg.RankingData info = rankingDataList.get(i);
								RankingData rdata = new RankingData();
								rdata.setRank(info.getRank());
								rdata.setName(info.getName());
								rdata.setUid(info.getUid());
								rdata.setValue(info.getValue());
								list.add(rdata);
							}
						}
						ranking.setRoot(list);
						ranking.setTotal(rankingDataList.size());
						cb.push(ranking);
					});
		} else if (type == RankingType.PAY.getId()) {
			cb.push(ranking);
		} else {
			logger.error("Ranking type err:{}", type);
		}
	}

	public void getRankListData(BacKernel kernel, RankList rankList, IDataCallBack cb) {
		List<RankingData> list = new ArrayList<RankingData>();
		logger.info("getRankListData request, type={}", rankList.getKey());
		ServerMsg.RankListType.Builder builder = ServerMsg.RankListType.newBuilder();
		builder.setType(rankList.getKey());

		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_RANK_LIST_DATA.ordinal(), builder.build().toByteArray(),
			(byte[] data) -> {
				logger.info("pub server return ranking data!!");
				ServerMsg.RankingDatas res;
				try {
					res = ServerMsg.RankingDatas.parseFrom(data);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				List<ServerMsg.RankingData> rankingDataList = res.getRankingDatasList();
				for (int i = 0; i < rankingDataList.size(); i++) {
					ServerMsg.RankingData info = rankingDataList.get(i);
					RankingData rdata = new RankingData();
					rdata.setRank(info.getRank());
					rdata.setName(info.getName());
					rdata.setUid(info.getUid());
					rdata.setValue(info.getValue());
					list.add(rdata);
				}
				rankList.setRoot(list);
				rankList.setTotal(rankingDataList.size());
				cb.push(rankList);
			});
	}
}
