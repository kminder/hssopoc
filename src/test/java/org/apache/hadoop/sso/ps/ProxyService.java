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
package org.apache.hadoop.sso.ps;

import org.apache.hadoop.sso.http.AbstractHttpServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path( "/ps" )
public class ProxyService extends AbstractHttpServer {

  private static final int PORT = 9096;
  private static final String PACKAGE = ProxyService.class.getPackage().getName();

  public static void main( String[] args ) {
    main( args, PORT, PACKAGE, new ProxyService() );
  }

  public static void start( int httpPort, int rpcPort ) throws Exception {
    start( httpPort, PACKAGE, rpcPort, new ProxyService() );
  }

  @Path( "/hello" )
  @GET
  @Produces( "text/plain" )
  public String hello() {
    return "ProxyService";
  }

}
