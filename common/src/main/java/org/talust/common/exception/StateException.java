package org.talust.common.exception;

public class StateException extends RuntimeException{
    StateException(String msg){
        super(msg);
    }

    StateException(){
        super("状态错误");
    }
}
