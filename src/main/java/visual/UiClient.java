package visual;

import context.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class UiClient {

    private ManagedChannel channel;
    private UiServerGrpc.UiServerFutureStub stub;

    private static UiClient client = new UiClient();

    public static UiClient ins() {
        return client;
    }

    private UiClient() {
        this(Context.uiHost, Context.uiPort);
    }

    private UiClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        stub = UiServerGrpc.newFutureStub(channel).withCompression("gzip");
    }

    public void close() {
        channel.shutdown();
    }

    public void plot(String id, float x, float y) {
        stub.plot(PlotMessage.newBuilder().setId(id).setData(Plot.newBuilder().addX(x).addY(y)).build());
    }
}