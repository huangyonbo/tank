package framework;

public class FlagUtil {

    public static int setFlag(int num,int flag){
        num |= flag;
        return num;
    }

    public static int clearFlag(int num,int flag){
        num &= ~flag;
        return num;
    }

    public static boolean checkFlag(int num,int flag){
        int a = num & flag;
        return a == flag;
    }

    /**
     * 0000 0000
     * 0000 0010
     * 0000 0010
     */

    public static void main(String[] args) {
        int num = 0;
//        for (int i = 1; i <= 32; i++) {
//
//            num = setFlag(num,i);
//        }

        int i = 0;
        num = setFlag(num, 1 << i);
        num = setFlag(num,1 << 1);
        num = setFlag(num,1 << 2);
        System.out.println(Integer.toBinaryString(num));
        num = clearFlag(num, 1 << 1);
        System.out.println(Integer.toBinaryString(num));
//        num = setFlag(num,4);
//        System.out.println(checkFlag(num,8));
        System.out.println(checkFlag(num,2));
//        System.out.println(checkFlag(num,4));
//        System.out.println(checkFlag(num,1));
    }
}
