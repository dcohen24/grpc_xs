import  grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import io.grpc.stub.StreamObserver;

public class HealthCheckImpl extends HealthGrpc.HealthImplBase {

    XsServiceImpl serviceImplementation;
    HealthOuterClass.HealthCheckResponse okResponse;
    HealthOuterClass.HealthCheckResponse failedResponse;
    public HealthCheckImpl(XsServiceImpl serviceImplementation)
    {
        this.serviceImplementation = serviceImplementation;
        okResponse = HealthOuterClass.HealthCheckResponse
                .newBuilder()
                .setStatus(HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING)
                .build();
        failedResponse = HealthOuterClass.HealthCheckResponse
                .newBuilder()
                .setStatus(HealthOuterClass.HealthCheckResponse.ServingStatus.NOT_SERVING)
                .build();
    }
    @Override
    public void check(HealthOuterClass.HealthCheckRequest request
            , StreamObserver<HealthOuterClass.HealthCheckResponse> responseObserver) {
        try {
            serviceImplementation.fetchQueryResults();
            responseObserver.onNext(this.okResponse);
            return;
        }
        catch (Exception E)
        {
            responseObserver.onNext(this.failedResponse);
            return;
        }
    }
}
