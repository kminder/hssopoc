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
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RpcClient {

  private static ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_TARGET, false );
    mapper.configure( JsonParser.Feature.AUTO_CLOSE_SOURCE, false );
  }
  private InetSocketAddress address;

  public RpcClient( InetSocketAddress address ) {
    this.address = address;
  }

  public Socket connect() throws IOException {
    return new Socket( address.getAddress(), address.getPort() );
  }

  public void destroy() {
  }

  public <T> T invoke( String method, Object input, Class<T> outputClass ) throws IOException {
    Socket socket = connect();
    RpcRequest request = new RpcRequest();
    request.methodName = method;
    request.inputClass = input.getClass().getCanonicalName();
    mapper.writeValue( socket.getOutputStream(), request );
    mapper.writeValue( socket.getOutputStream(), input );

    RpcResponse response = mapper.readValue( socket.getInputStream(), RpcResponse.class );
    Object output = mapper.readValue( socket.getInputStream(), outputClass );
    socket.close();
    return (T)output;
  }

}
