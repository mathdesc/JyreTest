/*  =========================================================================
    ZreLogMsg.java
    
    Generated codec class for ZreLogMsg
    -------------------------------------------------------------------------
    Copyright (c) 1991-2012 iMatix Corporation -- http://www.imatix.com     
    Copyright other contributors as noted in the AUTHORS file.              
                                                                            
    This file is part of Zyre, an open-source framework for proximity-based 
    peer-to-peer applications -- See http://zyre.org.                       
                                                                            
    This is free software; you can redistribute it and/or modify it under   
    the terms of the GNU Lesser General Public License as published by the  
    Free Software Foundation; either version 3 of the License, or (at your  
    option) any later version.                                              
                                                                            
    This software is distributed in the hope that it will be useful, but    
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTA-   
    BILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General  
    Public License for more details.                                        
                                                                            
    You should have received a copy of the GNU Lesser General Public License
    along with this program. If not, see http://www.gnu.org/licenses/.      
    =========================================================================
*/

/*  These are the zre_log_msg messages
    LOG - Log an event
        level         number 1
        event         number 1
        node          number 2
        peer          number 2
        time          number 8
        data          string
*/

package org.jyre;

import android.util.Log;

import java.nio.ByteBuffer;

import org.jeromq.ZFrame;
import org.jeromq.ZMQ;
import org.jeromq.ZMQ.Socket;

//  Opaque class structure
public class ZreLogMsg 
{
    public static final int ZRE_LOG_MSG_VERSION             = 1;
    public static final int ZRE_LOG_MSG_LEVEL_ERROR         = 1;
    public static final int ZRE_LOG_MSG_LEVEL_WARNING       = 2;
    public static final int ZRE_LOG_MSG_LEVEL_INFO          = 3;
    public static final int ZRE_LOG_MSG_EVENT_JOIN          = 1;
    public static final int ZRE_LOG_MSG_EVENT_LEAVE         = 2;
    public static final int ZRE_LOG_MSG_EVENT_ENTER         = 3;
    public static final int ZRE_LOG_MSG_EVENT_EXIT          = 4;

    public static final int LOG                   = 1;

    //  Structure of our class
    private ZFrame address;             //  Address of peer if any
    private int id;                     //  ZreLogMsg message ID
    private ByteBuffer needle;          //  Read/write pointer for serialization
    private int level;
    private int event;
    private int node;
    private int peer;
    private long time;
    private String data;


    //  --------------------------------------------------------------------------
    //  Create a new ZreLogMsg

    public ZreLogMsg (int id)
    {
        this.id = id;
    }


    //  --------------------------------------------------------------------------
    //  Destroy the zre_log_msg

    public void destroy ()
    {
        //  Free class properties
        if (address != null)
            address.destroy ();
        address = null;

        //  Destroy frame fields
    }


    //  --------------------------------------------------------------------------
    //  Network data encoding macros


    //  Put a 1-byte number to the frame
    private final void putNumber1 (int value) 
    {
        needle.put ((byte) value);
    }

    //  Get a 1-byte number to the frame
    //  then make it unsigned
    private int getNumber1 () 
    { 
        int value = needle.get (); 
        if (value < 0)
            value = (0xff) & value;
        return value;
    }

    //  Put a 2-byte number to the frame
    private final void putNumber2 (int value) 
    {
        needle.putShort ((short) value);
    }

    //  Get a 2-byte number to the frame
    private int getNumber2 () 
    { 
        int value = needle.getShort (); 
        if (value < 0)
            value = (0xffff) & value;
        return value;
    }

    //  Put a 4-byte number to the frame
    private final void putNumber4 (long value) 
    {
        needle.putInt ((int) value);
    }

    //  Get a 4-byte number to the frame
    //  then make it unsigned
    private long getNumber4 () 
    { 
        long value = needle.getInt (); 
        if (value < 0)
            value = (0xffffffff) & value;
        return value;
    }

    //  Put a 8-byte number to the frame
    public void putNumber8 (long value) 
    {
        needle.putLong (value);
    }

    //  Get a 8-byte number to the frame
    public long getNumber8 () 
    {
        return needle.getLong ();
    }


    //  Put a block to the frame
    private void putBlock (byte [] value, int size) 
    {
        needle.put (value, 0, size);
    }

    private byte [] getBlock (int size) 
    {
        byte [] value = new byte [size]; 
        needle.get (value);

        return value;
    }

    //  Put a string to the frame
    public void putString (String value) 
    {
        needle.put ((byte) value.length ());
        needle.put (value.getBytes());
    }

    //  Get a string from the frame
    public String getString () 
    {
        int size = getNumber1 ();
        byte [] value = new byte [size];
        needle.get (value);

        return new String (value);
    }

    //  --------------------------------------------------------------------------
    //  Receive and parse a ZreLogMsg from the socket. Returns new object or
    //  null if error. Will block if there's no message waiting.

    public static ZreLogMsg recv (Socket input)
    {
        assert (input != null);
        ZreLogMsg self = new ZreLogMsg (0);
        ZFrame frame = null;

        try {
            //  Read valid message frame from socket; we loop over any
            //  garbage data we might receive from badly-connected peers
            while (true) {
                //  If we're reading from a ROUTER socket, get address
                if (input.getType () == ZMQ.ROUTER) {
                    self.address = ZFrame.recvFrame (input);
                    if (self.address == null)
                        return null;         //  Interrupted
                    if (!input.hasReceiveMore ())
                        throw new IllegalArgumentException ();
                }
                //  Read and parse command in frame
                frame = ZFrame.recvFrame (input);
                if (frame == null)
                    return null;             //  Interrupted

                //  Get and check protocol signature
                self.needle = ByteBuffer.wrap (frame.getData ()); 
                int signature = self.getNumber2 ();
                if (signature == (0xAAA0 | 2))
                    break;                  //  Valid signature

                //  Protocol assertion, drop message
                while (input.hasReceiveMore ()) {
                    frame.destroy ();
                    frame = ZFrame.recvFrame (input);
                }
                frame.destroy ();
            }

            //  Get message id, which is first byte in frame
            self.id = self.getNumber1 ();
            int listSize;
            int hashSize;

            switch (self.id) {
            case LOG:
                self.level = self.getNumber1 ();
                self.event = self.getNumber1 ();
                self.node = self.getNumber2 ();
                self.peer = self.getNumber2 ();
                self.time = self.getNumber8 ();
                self.data = self.getString ();
                break;

            default:
                throw new IllegalArgumentException ();
            }

            return self;

        } catch (Exception e) {
            //  Error returns
            Log.e("ZreLogMsg", "malformed messages " + self.id);
            self.destroy ();
            return null;
        } finally {
            if (frame != null)
                frame.destroy ();
        }
    }



    //  --------------------------------------------------------------------------
    //  Send the ZreLogMsg to the socket, and destroy it

    public boolean send (Socket socket)
    {
        assert (socket != null);

        //  Calculate size of serialized data
        int frameSize = 2 + 1;          //  Signature and message ID
        switch (id) {
        case LOG:
            //  level is a 1-byte integer
            frameSize += 1;
            //  event is a 1-byte integer
            frameSize += 1;
            //  node is a 2-byte integer
            frameSize += 2;
            //  peer is a 2-byte integer
            frameSize += 2;
            //  time is a 8-byte integer
            frameSize += 8;
            //  data is a string with 1-byte length
            frameSize++;       //  Size is one octet
            if (data != null)
                frameSize += data.length ();
            break;
            
        default:
            Log.e("ZreLogMsg", "bad message type '"+id +"', not sent\n");
            assert (false);
        }
        //  Now serialize message into the frame
        ZFrame frame = new ZFrame (new byte [frameSize]);
        needle = ByteBuffer.wrap (frame.getData ()); 
        int frameFlags = 0;
        putNumber2 (0xAAA0 | 2);
        putNumber1 ((byte) id);

        switch (id) {
        case LOG:
            putNumber1 (level);
            putNumber1 (event);
            putNumber2 (node);
            putNumber2 (peer);
            putNumber8 (time);
            if (data != null)
                putString (data);
            else
                putNumber1 ((byte) 0);      //  Empty string
            break;
            
        }
        //  If we're sending to a ROUTER, we send the address first
        if (socket.getType () == ZMQ.ROUTER) {
            assert (address != null);
            if (!address.send (socket, ZMQ.SNDMORE)) {
                destroy ();
                return false;
            }
        }
        //  Now send the data frame
        if (!frame.send (socket, frameFlags)) {
            frame.destroy ();
            destroy ();
            return false;
        }
        
        //  Now send any frame fields, in order
        switch (id) {
        }
        //  Destroy ZreLogMsg object
        destroy ();
        return true;
    }


//  --------------------------------------------------------------------------
//  Send the LOG to the socket in one step

    public static void sendLog (
        Socket output,
        int level,
        int event,
        int node,
        int peer,
        long time,
        String data) 
    {
        ZreLogMsg self = new ZreLogMsg (ZreLogMsg.LOG);
        self.setLevel (level);
        self.setEvent (event);
        self.setNode (node);
        self.setPeer (peer);
        self.setTime (time);
        self.setData (data);
        self.send (output); 
    }


    //  --------------------------------------------------------------------------
    //  Duplicate the ZreLogMsg message

    public ZreLogMsg dup ()
    {
        ZreLogMsg copy = new ZreLogMsg (this.id);
        if (this.address != null)
            copy.address = this.address.duplicate ();
        switch (this.id) {
        case LOG:
            copy.level = this.level;
            copy.event = this.event;
            copy.node = this.node;
            copy.peer = this.peer;
            copy.time = this.time;
            copy.data = this.data;
        break;
        }
        return copy;
    }


    //  --------------------------------------------------------------------------
    //  Print contents of message to stdout

    public void dump ()
    {
        switch (id) {
        case LOG:
            Log.d("ZreLogMsg", "    level=" + level + "    event=" + event + "    node=" + node + "    peer=" + peer + "    time=" + time);
            if (data != null)
                Log.d("ZreLogMsg", "    data='"+data+"'");
            else
                Log.d("ZreLogMsg", "    data=''");
            break;
            
        }
    }


    //  --------------------------------------------------------------------------
    //  Get/set the message address

    public ZFrame address ()
    {
        return address;
    }

    public void setAddress (ZFrame address)
    {
        if (this.address != null)
            this.address.destroy ();
        this.address = address.duplicate ();
    }


    //  --------------------------------------------------------------------------
    //  Get/set the zre_log_msg id

    public int id ()
    {
        return id;
    }

    public void setId (int id)
    {
        this.id = id;
    }

    //  --------------------------------------------------------------------------
    //  Get/set the level field

    public int level ()
    {
        return level;
    }

    public void setLevel (int level)
    {
        this.level = level;
    }


    //  --------------------------------------------------------------------------
    //  Get/set the event field

    public int event ()
    {
        return event;
    }

    public void setEvent (int event)
    {
        this.event = event;
    }


    //  --------------------------------------------------------------------------
    //  Get/set the node field

    public int node ()
    {
        return node;
    }

    public void setNode (int node)
    {
        this.node = node;
    }


    //  --------------------------------------------------------------------------
    //  Get/set the peer field

    public int peer ()
    {
        return peer;
    }

    public void setPeer (int peer)
    {
        this.peer = peer;
    }


    //  --------------------------------------------------------------------------
    //  Get/set the time field

    public long time ()
    {
        return time;
    }

    public void setTime (long time)
    {
        this.time = time;
    }


    //  --------------------------------------------------------------------------
    //  Get/set the data field

    public String data ()
    {
        return data;
    }

    public void setData (String format, Object ... args)
    {
        //  Format into newly allocated string
        data = String.format (format, args);
    }


}

