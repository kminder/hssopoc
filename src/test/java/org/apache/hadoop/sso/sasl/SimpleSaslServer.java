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
package org.apache.hadoop.sso.sasl;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public class SimpleSaslServer implements SaslServer {

  private static final String MECHANISM = "SIMPLE";

  private CallbackHandler callbackHandler;
  private boolean authenticated = false;
  private String authorizationId;

  public SimpleSaslServer( CallbackHandler cbh ) {
    callbackHandler = cbh;
  }

  public String getMechanismName() {
    return MECHANISM;
  }

  public byte[] evaluateResponse( byte[] response ) throws SaslException {
    try {
      DataInputStream input = new DataInputStream( new ByteArrayInputStream( response ) );
      String username = input.readUTF();
      int length = input.readInt();
      byte[] providedDigest = new byte[ length ];
      input.read( providedDigest );

      NameCallback nameCallback = new NameCallback( "username", username );
      PasswordCallback passwordCallback = new PasswordCallback( "password", false );
      AuthorizeCallback authorizeCallback = new AuthorizeCallback( username, username );

      Callback[] callbacks = new Callback[]{ nameCallback, passwordCallback, authorizeCallback };
      callbackHandler.handle( callbacks );

      byte[] requiredDigest = DigestUtils.sha( new String( passwordCallback.getPassword() ) );

      if( authorizeCallback.isAuthorized() ) {
        if( Arrays.equals( requiredDigest, providedDigest ) ) {
          authenticated = true;
          authorizationId = authorizeCallback.getAuthenticationID();
        }
      }
      if( !authenticated ) {
        throw new SaslException( MECHANISM + ": Authentication failed" );
      }
    } catch( SaslException e ) {
      throw e;
    } catch( Exception e ) {
      throw new SaslException( MECHANISM + ": Error processing data: " + e, e );
    }
    return null;
  }

  public boolean isComplete() {
    return authenticated;
  }

  public String getAuthorizationID() {
    return authorizationId;
  }

  public byte[] unwrap( byte[] incoming, int offset, int len ) throws SaslException {
    throw new SaslException( MECHANISM + ": Encryption unsupported." );
  }

  public byte[] wrap( byte[] outgoing, int offset, int len ) throws SaslException {
    throw new SaslException( MECHANISM + ": Encryption unsupported." );
  }

  public Object getNegotiatedProperty( String propName ) {
    if( authenticated ) {
      if( propName.equals( Sasl.QOP ) ) {
        return "auth";
      } else {
        return null;
      }
    } else {
      throw new IllegalStateException( MECHANISM + ": Authentication incomplete" );
    }
  }

  public void dispose() throws SaslException {
    callbackHandler = null;
    authorizationId = null;
  }

}
