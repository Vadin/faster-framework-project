package cn.org.faster.framework.grpc.client.proxy;

import cn.org.faster.framework.core.utils.Utils;
import cn.org.faster.framework.grpc.client.model.ChannelProperty;
import cn.org.faster.framework.grpc.core.annotation.GRpcMethod;
import cn.org.faster.framework.grpc.core.factory.MarshallerFactory;
import cn.org.faster.framework.grpc.core.model.MethodCallProperty;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.*;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangbowen
 * @since 2019/1/14
 */
public class ManageChannelProxy implements InvocationHandler {
    private final MarshallerFactory marshallerFactory;
    private ManagedChannel channel;
    private Object invoker = new Object();
    private static final Map<String, MethodCallProperty> callDefinitions = new HashMap<>();

    public ManageChannelProxy(ChannelProperty channelProperty, MarshallerFactory marshallerFactory) {
        this.marshallerFactory = marshallerFactory;
        this.channel = ManagedChannelBuilder.forAddress(channelProperty.getHost(), channelProperty.getPort())
                .usePlaintext()
                .build();
    }

    public void addCall(MethodCallProperty methodCallProperty) {
        callDefinitions.put(methodCallProperty.getMethodName(), methodCallProperty);
    }

    private ClientCall<Object, Object> buildCall(MethodCallProperty methodCallProperty) {
        MethodDescriptor.Builder<Object, Object> builder = MethodDescriptor.newBuilder(
                marshallerFactory.emptyMarshaller(),
                marshallerFactory.parseReturnMarshaller(methodCallProperty)
        ).setType(methodCallProperty.getMethodType())
                .setFullMethodName(MethodDescriptor.generateFullMethodName(methodCallProperty.getScheme(), methodCallProperty.getMethodName()));
        return channel.newCall(builder.build(), CallOptions.DEFAULT);
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();
        if ("toString".equals(methodName) && args.length == 0) {
            return className + "@" + invoker.hashCode();
        } else if ("hashCode".equals(methodName) && args.length == 0) {
            return invoker.hashCode();
        } else if ("equals".equals(methodName) && args.length == 1) {
            Object another = Utils.safeElement(args, 0);
            return proxy == another;
        }
        String annotationMethodName = method.getAnnotation(GRpcMethod.class).value();
        MethodCallProperty methodCallProperty = callDefinitions.get(StringUtils.isEmpty(annotationMethodName) ? methodName : annotationMethodName);
        ClientCall<Object, Object> clientCall = buildCall(methodCallProperty);
        switch (methodCallProperty.getMethodType()) {
            case UNARY:
                if (method.getReturnType() == ListenableFuture.class) { //等于ClientCalls.futureUnaryCall()
                    return ClientCalls.futureUnaryCall(clientCall, Utils.safeElement(args, 0));
                } else if (method.getReturnType().getName().equals("void")) { //等于ClientCalls.asyncUnaryCall();
                    if (Utils.checkMethodHasParamClass(method, StreamObserver.class)) {
                        ClientCalls.asyncUnaryCall(clientCall, Utils.safeElement(args, 0), (StreamObserver<Object>) Utils.safeElement(args, 1));
                        return null;
                    } else {
                        ClientCalls.blockingUnaryCall(clientCall, Utils.safeElement(args, 0));
                        return null;
                    }
                }
                return ClientCalls.blockingUnaryCall(clientCall, Utils.safeElement(args, 0));
            case BIDI_STREAMING://双向流，相当于asyncBidiStreamingCall
                //获取返回类型的泛型
                return ClientCalls.asyncBidiStreamingCall(clientCall, (StreamObserver<Object>) Utils.safeElement(args, 0));
            case CLIENT_STREAMING: //客户端流。等于ClientCalls.asyncClientStreamingCall()
                return ClientCalls.asyncClientStreamingCall(clientCall, (StreamObserver<Object>) Utils.safeElement(args, 0));
            case SERVER_STREAMING://等于ClientCalls.blockingServerStreamingCall
                return ClientCalls.blockingServerStreamingCall(clientCall, Utils.safeElement(args, 0));
        }
        return null;
    }

}
