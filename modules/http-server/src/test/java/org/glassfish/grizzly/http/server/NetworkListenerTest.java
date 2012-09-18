/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly.http.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.grizzly.PortRange;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * {@link NetworkListener} tests.
 * 
 * @author Alexey Stashok
 */
public class NetworkListenerTest {
    public static final int PORT = 18897;

    @Test
    public void testSetPort() throws IOException {
        NetworkListener listener = new NetworkListener("set-port", "0.0.0.0", PORT);
        HttpServer httpServer = new HttpServer();
        httpServer.addListener(listener);

        try {
            assertEquals(PORT, listener.getPort());
            httpServer.start();
            assertEquals(PORT, listener.getPort());
        } finally {
            httpServer.stop();
        }
    }

    @Test
    public void testAutoPort() throws IOException {
        NetworkListener listener = new NetworkListener("auto-port", "0.0.0.0", 0);
        HttpServer httpServer = new HttpServer();
        httpServer.addListener(listener);

        try {
            assertEquals(0, listener.getPort());
            httpServer.start();
            assertNotSame(0, listener.getPort());
        } finally {
            httpServer.stop();
        }
    }

    @Test
    public void testPortRange() throws IOException {
        final int RANGE = 10;
        final PortRange portRange = new PortRange(PORT, PORT + RANGE);
        NetworkListener listener = new NetworkListener("set-port", "0.0.0.0",
                portRange);
        HttpServer httpServer = new HttpServer();
        httpServer.addListener(listener);

        try {
            assertEquals(-1, listener.getPort());
            httpServer.start();
            assertTrue(listener.getPort() >= PORT);
            assertTrue(listener.getPort() <= PORT + RANGE);
        } finally {
            httpServer.stop();
        }
    }

    @Test
    public void testTransactionTimeoutGetSet() throws IOException {
        NetworkListener l = new NetworkListener("test");
        assertEquals(NetworkListener.DEFAULT_TRANSACTION_TIMEOUT, l.getTransactionTimeout());
        l.setTransactionTimeout(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, l.getTransactionTimeout());
        l.setTransactionTimeout(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, l.getTransactionTimeout());
    }

    @Test
    public void testTransactionTimeout() throws IOException {
        final HttpServer server = HttpServer.createSimpleServer("/tmp", PORT);
        final NetworkListener listener = server.getListener("grizzly");
        listener.setTransactionTimeout(5);
        final AtomicReference<Exception> timeoutFailed = new AtomicReference<Exception>();
        server.getServerConfiguration().addHttpHandler(
                new HttpHandler() {
                    @Override
                    public void service(Request request, Response response) throws Exception {
                        Thread.sleep(15000);
                        timeoutFailed.compareAndSet(null, new IllegalStateException());
                    }
                }, "/test"
        );
        try {
            server.start();
            URL url = new URL("http://localhost:" + PORT + "/test");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            final long start = System.currentTimeMillis();
            c.connect();
            c.getResponseCode(); // cause the client to block
            final long stop = System.currentTimeMillis();
            assertNull(timeoutFailed.get());
            assertTrue((stop - start) < 15000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
