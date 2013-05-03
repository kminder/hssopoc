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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Map;

public class SimpleSaslClient implements SaslClient {

  private static final String MECHANISM = "SIMPLE";

  private String username;
  private byte[] digest;
  private boolean finished;

  SimpleSaslClient( CallbackHandler callbackHandler ) throws SaslException {
    callbackForCredentials( callbackHandler );
  }

  private void callbackForCredentials( CallbackHandler callbackHandler ) throws SaslException {
    try {
      NameCallback nameCallback = new NameCallback( "username" );
      PasswordCallback passwordCallback = new PasswordCallback( "password", false );
      callbackHandler.handle( new Callback[]{ nameCallback, passwordCallback } );
      username = nameCallback.getName();
      digest = DigestUtils.sha( new String( passwordCallback.getPassword() ) );
    } catch( Exception e ) {
      throw new SaslException( MECHANISM + ": Cannot get credentials", e );
    }
  }

  @Override
  public byte[] evaluateChallenge( byte[] challenge ) throws SaslException {
    if( finished ) {
      throw new IllegalStateException( MECHANISM + ": Authentication already complete." );
    }
    finished = true;
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      DataOutputStream response = new DataOutputStream( buffer );
      response.writeUTF( username );
      response.writeInt( digest.length );
      response.write( digest );
      return buffer.toByteArray();
    } catch( Exception e ) {
      throw new SaslException( MECHANISM + ": Failed to create challenge response.", e );
    }
  }

  @Override
  public String getMechanismName() {
    return MECHANISM;
  }

  @Override
  public boolean hasInitialResponse() {
    return true;
  }

  @Override
  public boolean isComplete() {
    return finished;
  }

  @Override
  public byte[] unwrap( byte[] incoming, int offset, int len ) throws SaslException {
    if( finished ) {
      throw new IllegalStateException( MECHANISM + ": Encryption unsupported." );
    } else {
      throw new IllegalStateException( MECHANISM + ": Authentication incomplete." );
    }
  }

  @Override
  public byte[] wrap( byte[] outgoing, int offset, int len ) throws SaslException {
    if( finished ) {
      throw new IllegalStateException( MECHANISM + ": Encryption unsupported." );
    } else {
      throw new IllegalStateException( MECHANISM + ": Authentication incomplete." );
    }
  }

  @Override
  public Object getNegotiatedProperty( String propName ) {
    if( finished ) {
      if( propName.equals( Sasl.QOP ) ) {
        return "auth";
      } else {
        return null;
      }
    } else {
      throw new IllegalStateException( MECHANISM + ": Authentication incomplete." );
    }
  }

  @Override
  public void dispose() throws SaslException {
    username = null;
    digest = null;
  }

}
