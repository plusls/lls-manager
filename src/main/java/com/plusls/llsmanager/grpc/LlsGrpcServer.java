package com.plusls.llsmanager.grpc;

import io.grpc.stub.StreamObserver;

public class LlsGrpcServer {
    //    private Server server;
//    private void start() {
//        /* The port on which the server should run */
//        int port = 25564;
//        server = ServerBuilder.forPort(port)
//                .addService(new HelloIml())  //这里可以添加多个模块
//                .build()
//                .start();
//        System.out.println("Server started, listening on " + port);
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
//            System.err.println("*** shutting down gRPC server since JVM is shutting down");
//            try {
//                LlsGrpcServer.this.stop();
//            } catch (InterruptedException e) {
//                e.printStackTrace(System.err);
//            }
//            System.err.println("*** server shut down");
//        }));
//    }
//
//    private void stop() throws InterruptedException {
//        if (server != null) {
//            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
//        }
//    }
//
    private static class HelloIml extends LlsManagerGrpc.LlsManagerImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
//             super.sayHello(request, responseObserver);
            HelloReply helloResponse = HelloReply.newBuilder().setMessage("Hello " + request.getName() + ", I'm Java grpc Server").build();
            responseObserver.onNext(helloResponse);
            responseObserver.onCompleted();
        }
    }
}
