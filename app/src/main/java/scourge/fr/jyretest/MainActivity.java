package scourge.fr.jyretest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActionBarActivity {

    private int NUMBER_PUB = 1;
    private int NUMBER_SUB = 5;

    private ScheduledExecutorService execpub = Executors.newScheduledThreadPool(NUMBER_PUB);
    private ScheduledExecutorService execsub = Executors.newScheduledThreadPool(NUMBER_SUB);

    private ScheduledExecutorService exexZre = Executors.newScheduledThreadPool(10);

    public int getNUMBER_PUB() {
        return NUMBER_PUB;
    }
    public int getNUMBER_SUB() {
        return NUMBER_SUB;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Runnable pub = new syncpub(this);
        ArrayList<Runnable> sub = new ArrayList<>();


        execpub.scheduleAtFixedRate(pub, 0, 10, TimeUnit.SECONDS);
        for (int i=0; i<5 ; i++) {
             sub.add(i, new syncsub(this));
            execsub.scheduleAtFixedRate(sub.get(i), 1, 10, TimeUnit.SECONDS);
        }
        */
        Runnable ZreTester = new ZreTester();
        exexZre.scheduleAtFixedRate(ZreTester, 0, 10, TimeUnit.SECONDS);

    }
}
