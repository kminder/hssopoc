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
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class SaslRpcServer extends RpcServer {

  //private static final String MECH = "DIGEST-MD5";
  private static final String MECH = "SIMPLE";
  private static String PROTOCOL = "test-protocol";
  private static String HOST = "localhost";

  public SaslRpcServer( Object target, int port ) {
    super( target, port );
  }

  public Socket accept( Socket socket ) throws IOException {
    // Create application-level connection to handle request
    SaslProtocol conn = new SaslProtocol( socket );

    HashMap<String, Object> props = new HashMap<String, Object>();
    //props.put( Sasl.QOP, "auth-conf" ); //,auth-int,auth" );
    props.put( "com.sun.security.sasl.digest.realm", "test-realm" );

    // Create SaslServer to perform authentication
    SaslServer srv = Sasl.createSaslServer( MECH, PROTOCOL, HOST, props, new TestCallbackHandler() );

    if( srv == null ) {
      throw new IOException( "Unable to find server implementation for " + MECH );
    }

    boolean auth = false;

    // Read initial response from client
    byte[] response = conn.receive( SaslProtocol.COMMAND_AUTHENTICATE );
    SaslProtocol.Message clientMsg;

    while( !srv.isComplete() ) {
      try {
        // Generate challenge based on response
        byte[] challenge = srv.evaluateResponse( response );

        if( srv.isComplete() ) {
          conn.send( SaslProtocol.STATUS_SUCCESS, challenge );
          auth = true;
        } else {
          clientMsg = conn.send( SaslProtocol.STATUS_AUTHENTICATING, challenge );
          response = clientMsg.getBytes();
        }
      } catch( SaslException e ) {
        e.printStackTrace();
        // Send failure notification to client
        conn.send( SaslProtocol.STATUS_FAILURE, null );
        break;
      }
    }

    // Check status of authentication
    if( srv.isComplete() && auth ) {
      System.out.print( "Client authenticated; authorized client is: " + srv.getAuthorizationID() );
    } else {
      // Go get another client
      System.out.println( "Authentication failed. " );
      throw new IOException( "Authentication failed." );
    }

    String qop = (String)srv.getNegotiatedProperty( Sasl.QOP );
    System.out.println( "Negotiated QOP: " + qop );

//    // Now try to use security layer
//    boolean sl = ( qop.equals( "auth-conf" ) || qop.equals( "auth-int" ) );
//
//    byte[] msg = conn.receive( SaslProto.DATA_CMD );
//    byte[] realMsg = ( sl ? srv.unwrap( msg, 0, msg.length ) : msg );
//
//    System.out.println( "Received: " + new String( realMsg, "UTF-8" ) );
//
//    // Construct reply to send to client
//    String now = new Date().toString();
//    byte[] nowBytes = now.getBytes( "UTF-8" );
//    int len = realMsg.length + 1 + nowBytes.length;
//    byte[] reply = new byte[ len ];
//    System.arraycopy( realMsg, 0, reply, 0, realMsg.length );
//    reply[ realMsg.length ] = ' ';
//    System.arraycopy( nowBytes, 0, reply, realMsg.length + 1,
//        nowBytes.length );
//
//    System.out.println( "Sending: " + new String( reply, "UTF-8" ) );
//
//    byte[] realReply = ( sl ? srv.wrap( reply, 0, reply.length ) : reply );
//
//    conn.send( SaslProto.SUCCESS, realReply );

    return socket;
  }

  static class TestCallbackHandler implements CallbackHandler {
    @Override
    public void handle( Callback[] cbs ) throws UnsupportedCallbackException {
      for( Callback cb : cbs ) {
        if( cb instanceof AuthorizeCallback ) {
          AuthorizeCallback ac = (AuthorizeCallback)cb;
          System.out.println( "Server - AuthorizeCallback: authnId=" + ac.getAuthenticationID() + ", authzId=" + ac.getAuthorizationID() + ", id=" + ac.getAuthorizedID() );
          ac.setAuthorized( true );
        } else if( cb instanceof NameCallback ) {
          NameCallback nc = (NameCallback)cb;
          System.out.println( "Server - NameCallback: name=" + nc.getName() );
          nc.setName( "test-username" );
        } else if( cb instanceof PasswordCallback ) {
          PasswordCallback pc = (PasswordCallback)cb;
          char[] password = pc.getPassword();
          System.out.println( "Server - PasswordCallback: password=" + ( password == null ? "null" : String.copyValueOf(password) ) );
          pc.setPassword( "test-password".toCharArray() );
        } else if( cb instanceof  javax.security.sasl.RealmCallback ) {
          RealmCallback rc = (RealmCallback)cb;
          System.out.println( "Server - RealmCallback: realm=" + rc.getText() );
          rc.setText( "test-realm" );
        } else {
          System.out.println( "Server - Unknown callback: " + cb.getClass().getName() );
        }
      }
    }
  }

}
