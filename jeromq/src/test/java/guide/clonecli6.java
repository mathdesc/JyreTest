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

import java.util.Random;

/**
 * Clone client model 6
 */
public class clonecli6
{
    private final static String SUBTREE = "/client/";
	public void run() {

        //  Create distributed hash instance
        clone clone = new clone();
        Random rand = new Random(System.nanoTime());

        //  Specify configuration
        clone.subtree(SUBTREE);
        clone.connect("tcp://localhost", "5556");
        clone.connect("tcp://localhost", "5566");

        //  Set random tuples into the distributed hash
        while (!Thread.currentThread().isInterrupted()) {
            //  Set random value, check it was stored
            String key = String.format("%s%d", SUBTREE, rand.nextInt(10000));
            String value= String.format("%d", rand.nextInt(1000000));
            clone.set(key, value, rand.nextInt(30));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        clone.destroy();
	}

	public static void main(String[] args) {
		new clonecli6().run();
	}
}
