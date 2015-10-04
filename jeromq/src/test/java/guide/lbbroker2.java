/*
    Copyright (c) 2007-2014 Contributors as noted in the AUTHORS file

    This file is part of 0MQ.

    0MQ is free software; you can redistribute it and/or modify it under
    the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    0MQ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package guide;

import org.jeromq.ZContext;
import org.jeromq.ZFrame;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.Poller;
import org.jeromq.ZMQ.Socket;
import org.jeromq.ZMsg;
import org.jeromq.ZThread;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Load-balancing broker
 * Demonstrates use of the high level API
 */
public class lbbroker2
{
    private static final int NBR_CLIENTS = 10;
    private static final int NBR_WORKERS = 3;
    private static byte[] WORKER_READY = { '\001' };  //  Signals worker is ready

    /**
     * Basic request-reply client using REQ socket
     */
    private static class ClientTask implements ZThread.IDetachedRunnable
    {
        @Override
        public void run (Object ... args)
        {
            ZContext context = new ZContext();

            //  Prepare our context and sockets
            Socket client  = context.createSocket (ZMQ.REQ);
            ZHelper.setId (client);     //  Set a printable identity

            client.connect("ipc://frontend.ipc");

            //  Send request, get reply
            client.send("HELLO");
            String reply = client.recvStr ();
            System.out.println("Client: " + reply);

            context.destroy ();
        }
    }

    /**
     * Worker using REQ socket to do load-balancing
     */
    private static class WorkerTask implements ZThread.IDetachedRunnable
    {
        @Override
        public void run (Object ... args)
        {
            ZContext context = new ZContext();

            //  Prepare our context and sockets
            Socket worker  = context.createSocket (ZMQ.REQ);
            ZHelper.setId (worker);     //  Set a printable identity

            worker.connect("ipc://backend.ipc");

            //  Tell backend we're ready for work
            ZFrame frame = new ZFrame (WORKER_READY);
            frame.send (worker, 0);

            while(true)
            {
                ZMsg msg = ZMsg.recvMsg (worker);
                if (msg == null)
                    break;

                msg.getLast ().reset ("OK");
                msg.send (worker);
            }
            context.destroy ();
        }
    }

    /**
     * This is the main task. This has the identical functionality to
     * the previous lbbroker example but uses higher level classes to start child threads
     * to hold the list of workers, and to read and send messages:
     */
    public static void main (String[] args) {
        ZContext context = new ZContext();
        //  Prepare our context and sockets
        Socket frontend  = context.createSocket (ZMQ.ROUTER);
        Socket backend  = context.createSocket (ZMQ.ROUTER);
        frontend.bind("ipc://frontend.ipc");
        backend.bind("ipc://backend.ipc");

        int clientNbr;
        for (clientNbr = 0; clientNbr < NBR_CLIENTS; clientNbr++)
            ZThread.start (new ClientTask ());

        for (int workerNbr = 0; workerNbr < NBR_WORKERS; workerNbr++)
            ZThread.start (new WorkerTask ());

        //  Queue of available workers
        Queue<ZFrame> workerQueue = new LinkedList<ZFrame> ();

        //  Here is the main loop for the load-balancer. It works the same way
        //  as the previous example, but is a lot shorter because ZMsg class gives
        //  us an API that does more with fewer calls:

        while (!Thread.currentThread().isInterrupted()) {

            //  Initialize poll set
            Poller items = new Poller (2);

            //  Always poll for worker activity on backend
            items.register(backend, Poller.POLLIN);

            //  Poll front-end only if we have available workers
            if(workerQueue.size() > 0)
                items.register(frontend, Poller.POLLIN);

            if (items.poll() < 0)
                break;      //  Interrupted

            //  Handle worker activity on backend
            if (items.pollin(0)) {

                ZMsg msg = ZMsg.recvMsg (backend);
                if (msg == null)
                    break;  //  Interrupted

                ZFrame identity = msg.unwrap ();
                //  Queue worker address for LRU routing
                workerQueue.add (identity);

                //  Forward message to client if it's not a READY
                ZFrame frame = msg.getFirst ();
                if (Arrays.equals (frame.getData (), WORKER_READY))
                    msg.destroy ();
                else
                    msg.send (frontend);
            }

            if (items.pollin(1)) {
                //  Get client request, route to first available worker
                ZMsg msg = ZMsg.recvMsg (frontend);
                if (msg != null) {
                    msg.wrap (workerQueue.poll ());
                    msg.send (backend);
                }
            }
        }

        context.destroy ();
    }

}
