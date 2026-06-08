package framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MathUtils {
	static final Random RND = new Random(System.currentTimeMillis());

	/**
	 * @param reqPage
	 * @param showNum
	 * @param ts
	 * @return
	 */
	public static <T> List<T> getListOfThePage(int reqPage, byte showNum, List<T> ts) {
		int size = ts.size();
		int totalPage = (size - 1) / showNum + 1;
		if (reqPage > totalPage || reqPage < 1) {
			return Collections.emptyList();
		}
		return ts.subList((reqPage - 1) * showNum, Math.min(reqPage * showNum, size));
	}

	/**
	 * 获取指定LIST总页数,按照指定个数分页
	 * 
	 * @param <T>
	 * @param showNum
	 * @param ts
	 * @return
	 */
	public static <T> int getCountPage(byte showNum, List<T> ts) {
		int countPage = 0;
		int size = ts.size();
		countPage = size / showNum;
		if (size % showNum != 0) {
			countPage++;
		}
		return Math.max(1, countPage);
	}

	/**
	 * @param size
	 * @return
	 */
	public static int random(int size) {
		return RND.nextInt(size);
	}

	/**
	 * @param i
	 * @param j
	 * @return
	 */
	public static int random(int i, int j) {
		return RND.nextInt(j + 1 - i) + i;
	}

	/**
	 * @param x
	 * @param y
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static int getRadius(int x, int y, int x2, int y2) {
		return (int) Math.sqrt(getRadiusSquare(x, y, x2, y2));
	}

	public static int getRadiusSquare(int x, int y, int x2, int y2) {
		int tx = x2 - x;
		int ty = y2 - y;
		return tx * tx + ty * ty;
	}

	/**
	 * #{@link Random#nextDouble()}
	 * 
	 * @return
	 */
	public static double random() {
		return RND.nextDouble();
	}

	/**
	 * @return a double >= 0 and < 1
	 */
	public static double nextDouble() {
		return RND.nextDouble();
	}

	public static int nextInt(int len) {
		return RND.nextInt(len);
	}

	/**
	 * fit the chance, normally used for ratio judge
	 * 
	 * @param chn
	 * @return
	 */
	public static boolean randomLessThan(int chn) {
		if (chn < 0)
			return false;
		return RND.nextInt(100) < chn;
	}

	public static boolean randomLessThan(int chn, int percent) {
		return RND.nextInt(1000) < chn;
	}

	/**
	 * @return a random element of ts
	 */
	public static <T> T randomOne(List<T> ts) {
		if (ts.size() > 0)
			return ts.get(random(ts.size()));
		return null;
	}

	/**
	 * 从0-(size-1)之间（包括0,size-1)选取count个不重复的数 <br />
	 * 这算是最优化算法了吧。 哼哼哈哈 <br />
	 * 除非再加上如果随机数目是原来的一半以上就反向随机(size-count)个 ，麻烦，没写了<br />
	 * modify by DreamX 加上吧。。。懒惰vs强迫症没法了
	 * 
	 * @param size
	 * @param count
	 * @return 排序过的
	 */
	public static int[] randomSortedUnsameNums(int size, int count) {
		if (size <= count) {
			int[] numbers = new int[size];
			for (int i = 0; i < size; i++) {
				numbers[i] = i;
			}
			return numbers;
		}
		int[] numbers = new int[count];
		// 随机数目是原来的2/3以上就反向随机(size-count)个
		if (count > size * 3 / 5) {
			int[] onumbers = randomSortedUnsameNums(size, size - count);
			int onumberLen = onumbers.length;
			int oidx = 0;
			count = 0;
			for (int i = 0; i < size; i++) {
				if (oidx < onumberLen && i == onumbers[oidx]) {
					oidx++;
					continue;
				}
				numbers[count++] = i;
			}
			return numbers;
		}
		int gainNumCounts = 0;
		while (count > 0) {
			// 1， 随机一个数 n
			int num = random(size - gainNumCounts);
			// 2， 循环 已取的数字， 如果比某个数大或者等于 则 +1， 直到比某个数小
			int i = 0;
			// [1,3,0,0]
			for (i = 0; i < gainNumCounts; i++) {
				if (num < numbers[i]) {
					break;
				} else {
					num++;
				}
			}
			int temp = numbers[i];
			// 保存当前位置的数字，替换最新数字
			numbers[i] = num;
			// 把从当前位置开始，往后移动一格，直到达到有效数字+1
			gainNumCounts++;

			// 2 -> [1,3,0,0], i = 1;
			for (; i < gainNumCounts - 1; i++) {
				int temp2 = numbers[i + 1];
				numbers[i + 1] = temp;
				temp = temp2;
			}
			// 3，加入数字到数组，排序
			count--;
		}
		return numbers;
	}

	/**
	 * @param scenes
	 * @param maxSceneCount
	 * @return
	 */
	public static <T> List<T> randoms(Collection<T> scenes, int maxSceneCount) {
		// size[0, size-1]
		// 循环 1， 2， 知道数目达到 maxCount
		if (scenes.size() <= maxSceneCount)
			return new ArrayList<T>(scenes);
		if (maxSceneCount == 0)
			return Collections.emptyList();

		List<T> result = new ArrayList<T>(maxSceneCount);
		int[] idxs = randomSortedUnsameNums(scenes.size(), maxSceneCount);
		Iterator<T> itr = scenes.iterator();
		int idx = 0;
		int nextIdx = idxs[idx];
		int count = 0;
		while (itr.hasNext()) {
			T t = itr.next();
			if (count++ == nextIdx) {
				result.add(t);
				if (result.size() < maxSceneCount) {
					idx++;
					nextIdx = idxs[idx];
				} else {
					break;
				}
			}
		}
		return result;

	}

	/**
	 * @return a random element of ts
	 */
	public static <T> T randomOne(T[] ts) {
		return ts[random(ts.length)];
	}

	/**
	 * @return a random element of ts
	 */
	public static int randomOne(int[] ts) {
		return ts[random(ts.length)];
	}

	public static int getRandomIdx(int[] data, int i, int length, int jump) {
		int rnd = random(100);
		for (; i < length; i += jump) {
			if (rnd < data[i])
				return i;
			else
				rnd -= data[i];
		}
		return 0;
	}

	public static int[] getUnsameRandoms(int[] percents, int selectCount) {
		int[] seleted = new int[selectCount];
		Arrays.fill(seleted, -1);
		// count maxpercent
		int sum = 0;
		for (int i = 0; i < percents.length; ++i) {
			sum += percents[i];
		}
		for (int si = 0; si < selectCount; ++si) {
			int rnd = random(sum);
			f: for (int i = 0; i < percents.length; ++i) {
				for (int selectedIdx : seleted) {
					if (selectedIdx == i) {
						continue f;
					}
				}
				if (rnd < percents[i]) {
					seleted[si] = i;
					sum -= percents[i];
					break;
				} else {
					rnd -= percents[i];
				}
			}
		}
		return seleted;
	}

	/**
	 * 两线段相交的点
	 * 
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @param cx
	 * @param cy
	 * @param dx
	 * @param dy
	 * @return null 如果不相交
	 */
	public final static int[] linesInst(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy)
	// {
	// // 三角形abc 面积的2倍
	// int area_abc = (ax - cx) * (by - cy) - (ay - cy) * (bx - cx);
	// if (area_abc == 0) return new int[]{cx, cy};
	// // 三角形abd 面积的2倍
	// int area_abd = (ax - dx) * (by - dy) - (ay - dy) * (bx - dx);
	// if (area_abd == 0) return new int[]{dx, dy};
	// // 面积符号相同则两点在线段同侧,不相交 ;
	// if ( area_abc*area_abd>0 ) {
	// return null;
	// }
	//
	// // 三角形cda 面积的2倍
	// int area_cda = (cx - ax) * (dy - ay) - (cy - ay) * (dx - ax);
	// if (area_cda == 0) return new int[]{ax, ay};
	// // 三角形cdb 面积的2倍
	// // 注意: 这里有一个小优化不需要再用公式计算面积,而是通过已知的三个面积加减得出
	// int area_cdb = area_cda + area_abc - area_abd ;
	// if (area_cdb == 0) return new int[]{bx, by};
	// if ( area_cda * area_cdb > 0 ) {
	// return null;
	// }
	//
	// //计算交点坐标
	// int x= ax + area_cda*(bx - ax) / ( area_abd- area_abc ) ;
	// int y= ay + area_cda*(by - ay) / ( area_abd- area_abc ) ;
	// return new int[]{x, y};
	// }
	{

		/** 1 解线性方程组, 求线段交点. **/
		// 如果分母为0 则平行或共线, 不相交
		int d = (by - ay) * (dx - cx) - (ax - bx) * (cy - dy);
		if (d == 0) {
			return null;
		}

		// 线段所在直线的交点坐标 (x , y)
		int x = ((bx - ax) * (dx - cx) * (cy - ay) + (by - ay) * (dx - cx) * ax - (dy - cy) * (bx - ax) * cx) / d;
		int y = -((by - ay) * (dy - cy) * (cx - ax) + (bx - ax) * (dy - cy) * ay - (dx - cx) * (by - ay) * cy) / d;

		/** 2 判断交点是否在两条线段上 **/
		if (// 交点在线段1上
		(x - ax) * (x - bx) <= 0 && (y - ay) * (y - by) <= 0
		// 且交点也在线段2上
				&& (x - cx) * (x - dx) <= 0 && (y - cy) * (y - dy) <= 0) {
			// 返回交点p
			return new int[] { x, y, 0 };
		}
		// 否则不相交
		return null;

	}

	/**
	 * 从指定MAP中随机取出指定个数的Value值
	 * 
	 * @param maleNameMap
	 * @return
	 */
	public static <K, V> List<V> randomMapValues(Map<K, V> map, int num) {

		int size = map.size();
		if (size <= num)
			return new ArrayList<V>(map.values());
		// 随机IDX
		List<K> randkeys = randoms(map.keySet(), num);
		List<V> vals = new ArrayList<V>(num);
		for (K k : randkeys) {
			vals.add(map.get(k));
		}
		return vals;
	}

	/**
	 * 得到游戏坐标点(x,y)与坐标原点的角度(笛卡尔)，x或y不得大于或小于 1 << 14 = 16384；
	 * 
	 * @param x
	 * @param y
	 * @return 正常角度*2的
	 */
	public static int getDegreeOfPoint(int x, int y) {
		if (x == 0 && y != 0) {
			return y > 0 ? 270 * 2 : 90 * 2;
		} else if (x != 0 && y == 0) {
			return x > 0 ? 0 : 180 * 2;
		}
		if (y > 1 << 14)
			return 0;
		int val = (Math.abs(y) << 14) / Math.abs(x);
		int idx = Arrays.binarySearch(ANGLE_TANS, val);
		idx = Math.abs(idx);
		// idx >>= 1;
		// 根据四个象限判断具体角度，游戏坐标方式
		if (x > 0 && y > 0)
			return 270 * 2 + idx;
		if (x < 0 && y > 0)
			return 180 * 2 + idx;
		if (x < 0 && y < 0)
			return 90 * 2 + idx;
		if (x > 0 && y < 0)
			return idx;
		return idx;
	}

	/**
	 * 三点形成的角的平分线上长度为len的点的点对称的点
	 * 
	 * @param xy
	 * @param xy1
	 * @param xy2
	 * @param len
	 * @return
	 */
	public static int[] getPointAgainst(int[] xy, int[] xy1, int[] xy2, int len) {
		int degree1 = getDegreeOfPoint(xy1[0] - xy[0], xy1[1] - xy[1]);
		int degree2 = getDegreeOfPoint(xy2[0] - xy[0], xy2[1] - xy[1]);
		int degree = (degree1 + degree2) / 2;
		int[] txy = getPointOfLenAndAngle(len, degree);
		txy[0] += xy[0];
		txy[1] += xy[1];
		return txy;
	}

	/**
	 * 根据线段长和角度，得到对应点的坐标
	 * 
	 * @param len
	 * @param angle
	 *            正常角度*2的
	 * @return
	 */
	public static int[] getPointOfLenAndAngle(int len, int angle) {
		// x*x + y*y = len*len;
		// x = ANGLE_TANS[angle%180]*y/;
		// (1+ANGLE_TANS[angle%180]*ANGLE_TANS[angle%180])*y*y = len*len;
		// y*y = len*len/(1+ANGLE_TANS[angle%180]*ANGLE_TANS[angle%180]);
		int anglex = angle / 2 % 90;
		if (anglex == 0)
			return new int[] { len, 0 };
		else if (anglex == 90)
			return new int[] { 0, -len };
		else if (anglex == 180)
			return new int[] { -len, 0 };
		else if (anglex == 270)
			return new int[] { 0, len };

		int y = (int) (len * 16384
				/ Math.sqrt(1.0 + 1.0 * ANGLE_TANS[angle % 180] * ANGLE_TANS[angle % 180] / 16384 / 16384));
		int x = ANGLE_TANS[angle % 180] * y / 16384;
		y >>= 14;
		x >>= 14;
		anglex = angle / 2;
		if (anglex > 0 && anglex < 90)
			return new int[] { x, -y };
		else if (anglex > 90 && anglex < 180)
			return new int[] { -x, -y };
		else if (anglex > 180 && anglex < 270)
			return new int[] { -x, y };
		else if (anglex > 270 && anglex < 360)
			return new int[] { x, -y };
		return new int[] { x, y };
	}

	// a/b
	public static int[] ANGLE_TANS = new int[180];
	static {
		int count = 0;
		for (double angle = 0; angle < 90; angle += 0.5) {
			double a = Math.tan(Math.toRadians(angle));
			ANGLE_TANS[count++] = (int) (a * (1 << 14));
		}
	}

	public static final int[][] SCREW_IDX = { { 0, 0 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, -1 }, { 0, 1 }, { -1, -1 },
			{ -1, 0 }, { -1, 1 }, { 2, -2 }, { 2, -1 }, { 2, 0 }, { 2, 1 }, { 2, 2 }, { 1, -2 }, { 1, 2 }, { 0, -2 },
			{ 0, 2 }, { -1, -2 }, { -1, 2 }, { -2, -2 }, { -2, -1 }, { -2, 0 }, { -2, 1 }, { -2, 2 }, { 3, -3 },
			{ 3, -2 }, { 3, -1 }, { 3, 0 }, { 3, 1 }, { 3, 2 }, { 3, 3 }, { 2, -3 }, { 2, 3 }, { 1, -3 }, { 1, 3 },
			{ 0, -3 }, { 0, 3 }, { -1, -3 }, { -1, 3 }, { -2, -3 }, { -2, 3 }, { -3, -3 }, { -3, -2 }, { -3, -1 },
			{ -3, 0 }, { -3, 1 }, { -3, 2 }, };

	/**
	 * @param x
	 * @param y
	 * @param i
	 * @param j
	 * @return
	 */
	public static int[] createScrewPosition(int x, int y, int dis, int count) {
		int[] fl = SCREW_IDX[count - 1];
		return new int[] { x + dis * fl[0], y + dis * fl[1] };
	}

	public static final long TIME_IN_MILLIS_OF_DAY = 24 * 60 * 60 * 1000;
	/**
	 * 1970-01-01 00:00:00.000
	 */
	public static final long TIME_IN_MILLIS_OF_BEGIN = Timestamp.valueOf("2000-01-01 00:00:00.0").getTime();

	public static boolean isSameDay(long millistime1, long millistime2) {
		return (millistime1 - TIME_IN_MILLIS_OF_BEGIN)
				/ TIME_IN_MILLIS_OF_DAY == (millistime2 - TIME_IN_MILLIS_OF_BEGIN) / TIME_IN_MILLIS_OF_DAY;
	}

	// 是否连续的两天
	public static boolean isContinueDay(long millistime1, long millistime2) {
		return (millistime1 - TIME_IN_MILLIS_OF_BEGIN) / TIME_IN_MILLIS_OF_DAY
				- (millistime2 - TIME_IN_MILLIS_OF_BEGIN) / TIME_IN_MILLIS_OF_DAY == 1;
	}

	public static long getDayBegin(long now) {
		return (now - TIME_IN_MILLIS_OF_BEGIN) / TIME_IN_MILLIS_OF_DAY * TIME_IN_MILLIS_OF_DAY
				+ TIME_IN_MILLIS_OF_BEGIN;

	}

	// list中取最小值
	public static int getMinIntFromList(List<Integer> lvls) {
		int minLvl = lvls.get(0);
		for (int i = 0; i < lvls.size(); i++) {
			if (minLvl > lvls.get(i)) {
				minLvl = lvls.get(i);
			}
		}
		return minLvl;
	}

	/**
	 * @return
	 */
	public static Random getRandomObject() {
		return RND;
	}

	/**
	 * 0x01 0x02 0x03 0x04 -> 0x00001234
	 * 
	 * @param b1
	 *            不得大于0x0F
	 * @param b2
	 *            不得大于0x0F
	 * @param b3
	 *            不得大于0x0F
	 * @param b4
	 *            不得大于0x0F
	 * @return
	 */
	public static int bytesToShort(byte b1, byte b2, byte b3, byte b4) {
		int v0 = (b1 & 0xff) << 12;// &0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
		int v1 = (b2 & 0xff) << 8;
		int v2 = (b3 & 0xff) << 4;
		int v3 = (b4 & 0xff);
		return v0 + v1 + v2 + v3;
	}

	public static String toCustomHex(String customHex, long val) {
		long i = val;
		String charstr = customHex;
		char[] chars = charstr.toCharArray();
		StringBuffer buff = new StringBuffer();
		while (true) {
			buff.append(chars[(int) (i % chars.length)]);
			i = i / chars.length;
			if (i > chars.length) {
				continue;
			} else {
				buff.append(chars[(int) (i % chars.length)]);
				break;
			}
		}
		buff.reverse();
		return buff.toString();
	}

	public static long customHexToValue(String customHex, String hexStr) {
		long x = 0;
		for (int z = 0; z < hexStr.length(); ++z) {
			x += customHex.indexOf(customHex.charAt(z)) * Math.pow(customHex.length(), z);
		}
		return x;
	}

	public static boolean checkBit(int state, int bit) {
		return (state & bit) == bit;
	}

	public static int setBit(int state, int bit) {
		return state ^ bit;
	}

	/**
	 * 根据num 获取上下波动rate的值
	 * 
	 * @param i
	 * @param rate
	 * @return
	 */
	public static int getFluctuateValue(int num, double rate) {
		if (num == 0) {
			return 0;
		}
		int a = (int) (num * rate);
		int rand = random(0, 2 * a);
		return (num + (rand - a));
	}

	public static byte[] zip(byte[] datas) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(datas, 0, datas.length);
			gzip.close();
			return out.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] uzip(byte[] datas) {
		if (datas == null || datas.length == 0) {
			return datas;
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayInputStream in = new ByteArrayInputStream(datas);
			GZIPInputStream ugzip = new GZIPInputStream(in);
			byte[] buffer = new byte[128];
			int len = -1;
			do {
				len = ugzip.read(buffer);
				if (len < 0) {
					break;
				}
				out.write(buffer, 0, len);
			} while (true);
			return out.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 更加概率随机多个数据
	 * 
	 * @param src
	 *            倒叙排列
	 * @param max
	 * @return
	 */
	public static List<RateData> randomMore(List<RateData> src, int max) {
		List<RateData> result = new ArrayList<RateData>();
		if (src == null || src.size() == 0) {
			return result;
		}
		int count = max * 3;// 最大尝试次数
		int sum = src.stream().mapToInt(RateData::getRate).sum();
		while (result.size() < max && count > 0) {
			int rand = RND.nextInt(sum) + 1;
			for (int i = 0; i < src.size(); i++) {
				RateData rate = src.get(i);
				rand -= rate.getRate();
				if (rand <= 0) {
					result.add(rate);
					break;
				}
			}
			count--;
		}
		return result;
	}
	
	public static <T> List<T> SplitByFlag(String str,String flag,Class<T> clazz){
		if (str == null || str.length() == 0){
			return null;
		}
		String[] ss = str.split(flag);
		List<T> result = new ArrayList<>();
		for (int i = 0 ; i < ss.length ; i++){
			T t = JsonUtil.decodeToObj(ss[i],clazz);
			result.add(t);
		}
		return result;
	}
}
