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
package org.apache.hadoop.sso.rp;

import org.apache.hadoop.sso.http.AbstractHttpServer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

@Path( "/rp" )
public class ReverseProxy extends AbstractHttpServer {

  private static final int PORT = 9091;
  private static final String PACKAGE = ReverseProxy.class.getPackage().getName();

  public static void main( String[] args ) {
    main( args, PORT, PACKAGE, new ReverseProxy() );
  }

  public static void start( int httpPort, int rpcPort ) throws Exception {
    start( httpPort, PACKAGE, rpcPort, new ReverseProxy() );
  }

  @Path( "/hello" )
  @GET
  @Produces( "text/plain" )
  public String hello() {
    return "ReverseProxy";
  }

  public Response proxy( @Context HttpServletRequest servletRequest, @Context Request jerseyRequest ) {
    return Response.ok().build();
  }

}
