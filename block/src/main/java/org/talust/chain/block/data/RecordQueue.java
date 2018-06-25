package org.talust.chain.block.data;

//业务记录队列,用于节点接收数据放于队列中
public class RecordQueue {
    private static RecordQueue instance = new RecordQueue();
    private RecordQueue(){}
    public static RecordQueue getInstance(){
        return instance;
    }
//    private LinkedTransferQueue<Record> queue = new LinkedTransferQueue<>();
//
//    public void addRecord(Record record){
//        if(record.get()!=null){
//            this.queue.add(record);
//        }
//    }
//    public Record takeRecord() throws Exception{
//        return this.queue.take();
//    }
}
