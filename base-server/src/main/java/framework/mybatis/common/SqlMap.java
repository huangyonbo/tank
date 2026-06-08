package framework.mybatis.common;

import framework.mybatis.utils.DateUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

public class SqlMap extends HashMap<String,Object> {


    public Integer getInt(String key){
        Object value = this.get(key);
        if (value != null){
            return Integer.parseInt(value.toString());
        }
        return 0;
    }

    public Long getLong(String key){
        Object value = this.get(key);
        if (value != null){
            return Long.parseLong(value.toString());
        }
        return 0L;
    }

    public Float getFloat(String key){
        Object value = this.get(key);
        if (value != null){
            return Float.parseFloat(value.toString());
        }
        return 0.0f;
    }

    public String getString(String key){
        Object value = this.get(key);
        if (value != null){
            return value.toString();
        }
        return null;
    }

    public Date getDate(String key){
        Object value = this.get(key);
        if (value != null){
            try {
                String str = value.toString().substring(0,19);
                return DateUtils.timeFormat.parse(str);
            }catch (Exception e){

            }
        }
        return null;
    }

    public Timestamp getTime(String key){
        Object value = this.get(key);
        if (value != null){
            return (Timestamp)value;
        }
        return null;
    }

    public byte[] getBytes(String key){
        Object value = this.get(key);
        if (value != null){
            return (byte[])value;
        }
        return null;
    }
}
