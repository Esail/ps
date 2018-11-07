import context.Context;
import net.PServer;
import update.AdamUpdater;
import update.FtrlUpdater;
import update.Updater;

public class PS {
    public static void main(String args[]) {

        Context.init();
        Context.thread = 4;
        Context.mode = Context.Mode.DISTRIBUTED;

        Updater updater = new AdamUpdater(0.005, 0.9, 0.999, Math.pow(10, -8));
        Updater ftrl = new FtrlUpdater(0.005f, 1f, 0.001f, 0.001f);

        PServer server= new PServer(Context.psPort, Context.workerNum);
        server.getUpdaterMap().put(updater.getName(), updater);
        server.getUpdaterMap().put(ftrl.getName(), ftrl);
        System.out.println("PS addr is: " + Context.psHost + "using port:" + Context.psPort);
        server.start();

        System.exit(0);

    }
}