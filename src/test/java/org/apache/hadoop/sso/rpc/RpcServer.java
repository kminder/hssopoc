/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.sso.rpc;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class RpcServer implements Runnable {

  private static ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_TARGET, false );
    mapper.configure( JsonParser.Feature.AUTO_CLOSE_SOURCE, false );
  }
  private int port;
  private Object target;
  private Thread thread;
  private volatile boolean running = false;

  public RpcServer( Object target, int port ) {
    this.target = target;
    this.port = port;
  }

  public void start() throws InterruptedException {
    thread = new Thread( this );
    thread.start();
    while( !running ) {
      Thread.sleep( 100 );
    }
  }

  public void stop() throws InterruptedException {
    running = false;
    thread.join();
    //System.out.println( "RPC server thread joined." );
  }

  public Class<?> findInputClass( RpcRequest request ) throws ClassNotFoundException {
    return Class.forName( request.inputClass );
  }

  public Object invoke( RpcRequest request, Class<?> inputClass, Object inputObject ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = target.getClass().getMethod( request.methodName, inputClass );
    return method.invoke( target, inputObject );
  }

  public void process( Socket socket ) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    RpcRequest request = mapper.readValue( socket.getInputStream(), RpcRequest.class );
    Class<?> inputClass = findInputClass( request );
    Object input = mapper.readValue( socket.getInputStream(), inputClass );
    Object output = invoke( request, inputClass, input );
    RpcResponse response = new RpcResponse();
    mapper.writeValue( socket.getOutputStream(), response );
    mapper.writeValue( socket.getOutputStream(), output );
  }

  protected Socket accept( Socket socket ) throws IOException {
    return socket;
  }

  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket( port );
      serverSocket.setSoTimeout( 200 );
      running = true;
      while( running ) {
        Socket clientSocket;
        try {
          clientSocket = serverSocket.accept();
        } catch ( SocketTimeoutException ste ) {
          continue;
        }
        try {
          process( accept( clientSocket ) );
          clientSocket.close();
        } catch( Exception e ) {
          e.printStackTrace();
        }
      }
      serverSocket.close();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

}
