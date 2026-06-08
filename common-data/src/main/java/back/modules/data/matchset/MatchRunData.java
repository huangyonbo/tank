package back.modules.data.matchset;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/10/29.
 */
public class MatchRunData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //比赛id
    private int roomId = 7; //房间id
    private int apply;  //报名人数
    private int robot;  //植入机器人数量
    private int playPop;    //实际参赛人数
    private int playCount;  //实际参赛次数
    private int turn;   //已开启次数
    private int online; //在线人数

    public MatchRunData(int id, int roomId, int apply, int robot, int playPop, int playCount, int turn, int online) {
        this.id = id;
        this.roomId = roomId;
        this.apply = apply;
        this.robot = robot;
        this.playPop = playPop;
        this.playCount = playCount;
        this.turn = turn;
        this.online = online;
    }

    public MatchRunData() {

    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getApply() {
        return apply;
    }

    public void setApply(int apply) {
        this.apply = apply;
    }

    public int getRobot() {
        return robot;
    }

    public void setRobot(int robot) {
        this.robot = robot;
    }

    public int getPlayPop() {
        return playPop;
    }

    public void setPlayPop(int playPop) {
        this.playPop = playPop;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }
}
