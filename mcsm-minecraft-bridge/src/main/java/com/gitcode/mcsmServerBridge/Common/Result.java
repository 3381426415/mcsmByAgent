package com.gitcode.mcsmServerBridge.Common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private String msg;
    private Integer code;
    private T data;
//2000业务成功，3000业务失败，401权限不足


    public static <T> Result<T> success(String msg,  T data) {
        Result<T> result = new Result<T>();
        result.code = 2000;
        result.setMsg(msg);
        result.setData(data);
        return result;

    }

    public static <T> Result<T> successMsg(String msg) {
        return success(msg,null);
    }


    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(3000);
        result.setMsg(msg);
        return result;
    }


    public static <T> Result<T> noPermission(String msg) {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMsg(msg);
        return result;
    }


}
