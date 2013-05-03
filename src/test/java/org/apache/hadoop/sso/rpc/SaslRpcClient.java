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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

public class SaslRpcClient extends RpcClient {

  //private static final String MECH = "DIGEST-MD5";
  private static final String MECH = "SIMPLE";
  private static final byte[] EMPTY = new byte[ 0 ];
  private static String PROTOCOL = "test-protocol";
  private static String HOST = "localhost";

  public SaslRpcClient( InetSocketAddress address ) {
    super( address );
  }

  public Socket connect() throws IOException {
    Socket socket = super.connect();

    SaslProtocol conn = new SaslProtocol( socket );
    HashMap<String, Object> props = new HashMap<String, Object>();

    // Request confidentiality
    // props.put( Sasl.QOP, "auth-conf" );

    // Create SaslClient to perform authentication
    SaslClient clnt = Sasl.createSaslClient( new String[]{ MECH }, null, PROTOCOL, HOST, props, new TestCallbackHandler() );

    if( clnt == null ) {
      throw new IOException( "Unable to find client implementation for " + MECH );
    }

    byte[] response;
    byte[] challenge;

    // Get initial response for authentication
    response = clnt.hasInitialResponse() ? clnt.evaluateChallenge( EMPTY ) : EMPTY;

    // Send initial response to server
    SaslProtocol.Message reply = conn.send( SaslProtocol.COMMAND_AUTHENTICATE, response );

    // Repeat until authentication terminates
    while( !clnt.isComplete() &&
        ( reply.getStatus() == SaslProtocol.STATUS_AUTHENTICATING || reply.getStatus() == SaslProtocol.STATUS_SUCCESS ) ) {

      // Evaluate challenge to generate response
      challenge = reply.getBytes();
      response = clnt.evaluateChallenge( challenge );

      if( reply.getStatus() == SaslProtocol.STATUS_SUCCESS ) {
        if( response != null ) {
          throw new IOException( "Protocol error interacting with SASL" );
        }
        break;
      }

      // Send response to server and read server's next challenge
      reply = conn.send( SaslProtocol.COMMAND_AUTHENTICATE, response );
    }

    // Check status of authentication
    if( clnt.isComplete() && reply.getStatus() == SaslProtocol.STATUS_SUCCESS ) {
      System.out.println( "Client authenticated." );
    } else {
      throw new IOException( "Authentication failed: " +
          " connection status? " + reply.getStatus() );
    }

    String qop = (String)clnt.getNegotiatedProperty( Sasl.QOP );
    System.out.println( "Negotiated QOP: " + qop );

//    // Try out security layer
//    boolean sl = ( qop.equals( "auth-conf" ) || qop.equals( "auth-int" ) );
//
//    byte[] msg = "Hello There!".getBytes( "UTF-8" );
//    System.out.println( "Sending: " + new String( msg, "UTF-8" ) );
//
//    byte[] encrypted = ( sl ? clnt.wrap( msg, 0, msg.length ) : msg );
//
//    reply = conn.send( SaslProto.DATA_CMD, encrypted );
//
//    if( reply.getStatus() == SaslProto.SUCCESS ) {
//      byte[] encryptedReply = reply.getBytes();
//
//      byte[] clearReply = ( sl ? clnt.unwrap( encryptedReply,
//          0, encryptedReply.length ) : encryptedReply );
//
//      System.out.println( "Received: " + new String( clearReply, "UTF-8" ) );
//    } else {
//      System.out.println( "Failed exchange: " + reply.getStatus() );
//    }

    return socket;
  }

  static class TestCallbackHandler implements CallbackHandler {

    @Override
    public void handle( Callback[] callbacks ) throws UnsupportedCallbackException {
      for( Callback callback : callbacks ) {
        if( callback instanceof NameCallback ) {
          System.out.println( "Client - NameCallback" );
          NameCallback nc = (NameCallback)callback;
          nc.setName( "test-username" );
        } else if( callback instanceof PasswordCallback ) {
          System.out.println( "Client - PasswordCallback" );
          PasswordCallback pc = (PasswordCallback)callback;
          pc.setPassword( "test-password".toCharArray() );
        } else if( callback instanceof  javax.security.sasl.RealmCallback ) {
          System.out.println( "Client - RealmCallback" );
          RealmCallback rc = (RealmCallback)callback;
          rc.setText( "test-realm" );
        } else {
          System.out.println( "Client - Unknown callback: " + callback.getClass().getName() );
        }
      }
    }
  }

}
