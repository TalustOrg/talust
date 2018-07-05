/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.chain;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.talust.chain.common.tools.DateTimeUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Controller层返回UI层类型定义
 * @date 2017/2/10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMessage implements Serializable {
    /**
     * 正常
     */
    private static final int OK=1;
    /**
     * 正常错误
     */
    private static final int ERROR=0;
    /**
     * 异常错误 try 捕获到的Exception
     */
    private static final int TOTAL_DEFAULT=-1;
    /**
     * 默认数据条数
     */
    private static final int TOTAL=0;

    private static final long serialVersionUID = 8992436576262574064L;
    /**
     * 响应码
     */
    private int code;
    /**
     * 反馈信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private transient String message;
    /**
     * 反馈数据
     */
    private Object data;
    /**
     * 列表总行数
     */
    private long total=TOTAL_DEFAULT;
    /**
     * 过滤字段：指定需要序列化的字段
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private transient Map<Class<?>, Set<String>> includes;
    /**
     * 过滤字段：指定不需要序列化的字段
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private transient Map<Class<?>, Set<String>> excludes;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private transient String callback;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (data != null) {
            map.put("data", this.getData());
        }
        if (message != null) {
            map.put("message", this.getMessage());
        }
        map.put("code", this.getCode());
        map.put("total", this.getTotal());
        return map;
    }

    public ResponseMessage() {
    }

    protected ResponseMessage(String message) {
        this(0,message,TOTAL_DEFAULT,"");
    }
    protected ResponseMessage(int code, String message) {
        this(code,message,TOTAL_DEFAULT,"");
    }
    protected ResponseMessage(int code, String message, long total, Object data) {
        this.code = code;
        this.message=message;
        this.data=data;
        this.total=total;
    }

    public ResponseMessage include(Class<?> type, String... fields) {
        return include(type, Arrays.asList(fields));
    }

    public ResponseMessage include(Class<?> type, Collection<String> fields) {
        if (includes == null) {
            includes = new HashMap<>();
        }
        if (fields == null || fields.isEmpty()) {return this;}
        fields.forEach(field -> {
            if (field.contains(".")) {
                String tmp[] = field.split("[.]", 2);
                try {
                    Field field1 = type.getDeclaredField(tmp[0]);
                    if (field1 != null) {
                        include(field1.getType(), tmp[1]);
                    }
                } catch (Throwable e) {
                }
            } else {
                getStringListFormMap(includes, type).add(field);
            }
        });
        return this;
    }

    public ResponseMessage exclude(Class type, Collection<String> fields) {
        if (excludes == null) {
            excludes = new HashMap<>();
        }
        if (fields == null || fields.isEmpty()){
            return this;
        }
        fields.forEach(field -> {
            if (field.contains(".")) {
                String tmp[] = field.split("[.]", 2);
                try {
                    Field field1 = type.getDeclaredField(tmp[0]);
                    if (field1 != null) {
                        exclude(field1.getType(), tmp[1]);
                    }
                } catch (Throwable e) {
                }
            } else {
                getStringListFormMap(excludes, type).add(field);
            }
        });
        return this;
    }

    public ResponseMessage exclude(Collection<String> fields) {
        if (excludes == null) {
            excludes = new HashMap<>();
        }
        if (fields == null || fields.isEmpty()){
            return this;
        }
        Class type;
        if (data != null) {
            type = data.getClass();
        } else {
            return this;
        }
        exclude(type, fields);
        return this;
    }

    public ResponseMessage include(Collection<String> fields) {
        if (includes == null) {
            includes = new HashMap<>();
        }
        if (fields == null || fields.isEmpty()){ return this;}
        Class type;
        if (data != null){
            type = data.getClass();
        } else {
            return this;
        }
        include(type, fields);
        return this;
    }

    public ResponseMessage exclude(Class type, String... fields) {
        return exclude(type, Arrays.asList(fields));
    }

    public ResponseMessage exclude(String... fields) {
        return exclude(Arrays.asList(fields));
    }

    public ResponseMessage include(String... fields) {
        return include(Arrays.asList(fields));
    }

    protected Set<String> getStringListFormMap(Map<Class<?>, Set<String>> map, Class type) {
        Set<String> list = map.get(type);
        if (list == null) {
            list = new HashSet<>();
            map.put(type, list);
        }
        return list;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
//        String msg = "";
//        try{
//            msg = new String(message.getBytes(), "UTF-8");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return msg;
        return message;
    }

    public Object getData() {
        return data;
    }

    public long getTotal() {
        return total;
    }
    @Override
    public String toString() {
        return JSON.toJSONStringWithDateFormat(this, DateTimeUtils.YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);
    }

    public static ResponseMessage fromJson(String json) {
        return JSON.parseObject(json, ResponseMessage.class);
    }

    public Map<Class<?>, Set<String>> getExcludes() {
        return excludes;
    }

    public Map<Class<?>, Set<String>> getIncludes() {
        return includes;
    }

    public ResponseMessage callback(String callback) {
        this.callback = callback;
        return this;
    }

    public String getCallback() {
        return callback;
    }

    public static ResponseMessage ok() {
        return new ResponseMessage(OK,"success");
    }

    public static ResponseMessage ok(Object data) {
        if(data==null){
            return error("查询对象为空");
        }else{
            if(data instanceof List<?>){
                List _obj = (List)data;
                if(_obj.size()<=0){
                    return new ResponseMessage(OK,"success",TOTAL, new ArrayList<>());
                }
            }
            return new ResponseMessage(OK,"success",1, data);
        }
    }

    public static ResponseMessage ok(long total,Object data) {
        if(data==null){
            return error("查询对象为空");
        }else{
            if(data instanceof List<?>){
                List _obj = (List)data;
                if(_obj.size()<=0){
                    return new ResponseMessage(OK,"success",0, new ArrayList<>());
                }
            }
            return new ResponseMessage(OK,"success",total, data);
        }
    }

    public static ResponseMessage error(String message) {
        return new ResponseMessage(message);
    }

    public static ResponseMessage error(int code,String message) {
        return new ResponseMessage(code,message);
    }

    public static ResponseMessage error(int code,String message, Object object) {
        return new ResponseMessage(code,message,TOTAL_DEFAULT,object);
    }

    public static int getOK() {
        return OK;
    }

    public static int getERROR() {
        return ERROR;
    }

    public static int getTotalDefault() {
        return TOTAL_DEFAULT;
    }

    public static int getTOTAL() {
        return TOTAL;
    }
}
