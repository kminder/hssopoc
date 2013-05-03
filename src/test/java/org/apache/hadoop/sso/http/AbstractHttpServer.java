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
package org.apache.hadoop.sso.http;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.hadoop.sso.rpc.RpcServer;
import org.apache.hadoop.sso.rpc.SaslRpcServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public abstract class AbstractHttpServer {

  public static Server httpServer;
  public static RpcServer rpcServer;

  protected static void start( int httpPort, String packages, int rpcPort, Object rpcTarget ) throws Exception {
//    SslContextFactory sslContextFactory = new SslContextFactory( "/etc/mykeystore" );
//    sslContextFactory.setKeyStorePassword( "yourpassword" );
//    SslSelectChannelConnector selectChannelConnector = new SslSelectChannelConnector( sslContextFactory );
//    selectChannelConnector.setPort( 4567 );

    httpServer = new Server( httpPort );
    ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
    context.setContextPath( "/" );
    httpServer.setHandler( context );
    ServletHolder holder = new ServletHolder( new ServletContainer() );
    holder.setInitParameter( "com.sun.jersey.config.property.packages", packages );
    holder.setInitParameter( "com.sun.jersey.api.json.POJOMappingFeature", "true" );
    holder.setInitParameter( "com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter" );
    holder.setInitParameter( "com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter" );
    context.addServlet( holder, "/*" );
    httpServer.start();

    rpcServer = new SaslRpcServer( rpcTarget, rpcPort );
    rpcServer.start();
  }

  public static void stop() throws Exception {
    rpcServer.stop();
    httpServer.stop();
    httpServer.join();
  }

  public static void main( String[] args, int port, String packages, Object target ) {
    try {
      start( port, packages, port+1, target );
      httpServer.join();
    } catch( Exception e ) {
      e.printStackTrace();
    }
  }

}
