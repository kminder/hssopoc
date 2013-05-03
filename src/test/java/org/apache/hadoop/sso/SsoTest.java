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
package org.apache.hadoop.sso;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.hadoop.sso.cr.ClusterInfo;
import org.apache.hadoop.sso.cr.ClusterLookup;
import org.apache.hadoop.sso.cr.ClusterRegistry;
import org.apache.hadoop.sso.ms.MasterService;
import org.apache.hadoop.sso.ps.ProxyService;
import org.apache.hadoop.sso.rp.ReverseProxy;
import org.apache.hadoop.sso.rpc.RpcClient;
import org.apache.hadoop.sso.rpc.SaslRpcClient;
import org.apache.hadoop.sso.sasl.SimpleSaslProvider;
import org.apache.hadoop.sso.sr.ServiceRegistry;
import org.apache.hadoop.sso.ss.SlaveService;
import org.apache.hadoop.sso.ta.TokenAuthority;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SsoTest {

  public static ObjectMapper mapper = new ObjectMapper();

  static {
    SimpleSaslProvider.register();
    mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_TARGET, false );
    mapper.configure( JsonParser.Feature.AUTO_CLOSE_SOURCE, false );
  }

  @BeforeClass
  public static void setupSuite() throws Exception {
    ClusterRegistry.start( 9000, 9001 );
    ServiceRegistry.start( 9002, 9003 );
    TokenAuthority.start( 9004, 9005 );
    ReverseProxy.start( 9006, 9007 );
    MasterService.start( 9008, 9009 );
    ProxyService.start( 9010, 9011 );
    SlaveService.start( 9012, 9013 );
  }

  @AfterClass
  public static void cleanupSuite() throws Exception {
    ClusterRegistry.stop();
    ServiceRegistry.stop();
    TokenAuthority.stop();
    ReverseProxy.stop();
    MasterService.stop();
    ProxyService.stop();
    SlaveService.stop();
  }

  @Test
  public void testHttp() throws IOException {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
    Client httpClient = Client.create( clientConfig );
    //httpClient.addFilter( new LoggingFilter( System.out ) );

    WebResource resource = httpClient.resource( "http://localhost:9000/cr/lookup" );
    ClusterInfo info = resource.accept( "application/json" ).put( ClusterInfo.class, new ClusterLookup( "my-cluster-1" ) );
    assertThat( info.name, is( "my-cluster-1" ) );

    httpClient.destroy();
  }

  @Test
  public void testRpc() throws IOException {
    RpcClient rpc = new SaslRpcClient( new InetSocketAddress( "localhost", 9001 ) );

    ClusterInfo info = rpc.invoke( "lookup", new ClusterLookup( "my-cluster-2" ), ClusterInfo.class );
    assertThat( info.name, is( "my-cluster-2" ) );

    rpc.destroy();
  }

}
