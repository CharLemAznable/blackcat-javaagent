package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMethodRuntime;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.github.bingoohuang.blackcat.sdk.utils.StrBuilder;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fastjson.JSON.toJSONString;

public class BlackcatMethodRuntimeProducer {
    Logger log = LoggerFactory.getLogger(BlackcatMethodRuntimeProducer.class);
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public BlackcatMethodRuntimeProducer(RingBuffer<BlackcatReq.Builder> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void send(BlackcatMethodRt blackcatMethodRt) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            BlackcatReq.Builder builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            BlackcatReqHead head = Blackcats.buildHead(ReqType.BlackcatMethodRuntime);
            BlackcatMethodRuntime.Builder runtimeBuilder = BlackcatMethodRuntime.newBuilder()
                    .setPid(blackcatMethodRt.pid)
                    .setExecutionId(blackcatMethodRt.executionId)
                    .setStartMillis(blackcatMethodRt.startMillis)
                    .setEndMillis(blackcatMethodRt.endMillis)
                    .setCostMillis(blackcatMethodRt.costMillis)

                    .setClassName(blackcatMethodRt.className)
                    .setMethodName(blackcatMethodRt.methodName)
                    .setMethodDesc(blackcatMethodRt.methodDesc)
                    .setArgs(toJSON(blackcatMethodRt.args))
                    .setResult(toJSON(blackcatMethodRt.result))
                    .setThrowableCaught(toJSON(blackcatMethodRt.throwableCaught))
                    .setSameThrowable(blackcatMethodRt.sameThrowable);
            if (!blackcatMethodRt.sameThrowable) {
                runtimeBuilder.setThrowableUncaught(
                        toJSON(blackcatMethodRt.throwableUncaught));
            }

            BlackcatMethodRuntime methodRuntime = runtimeBuilder.build();
            builder.setBlackcatReqHead(head)
                    .setBlackcatMethodRuntime(methodRuntime);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public String toJSON(Object object) {
        if (object == null) return "<null>";

        if (object.getClass().isArray()) {
            StrBuilder strBuilder = new StrBuilder("[");

            Object[] arr = (Object[]) object;
            boolean first = true;
            for (Object obj : arr) {
                if (!first) strBuilder.p(", ");
                strBuilder.p(toJSON(obj));
                first = false;
            }

            strBuilder.p(']');

            return strBuilder.toString().replaceAll("\"", "");
        }

        try {
            return toJSONString(object);
        } catch (Throwable e) {
            log.warn("failed to JSONize:{} because of {}", object, e.toString());

            return object.toString();
        }
    }
}
