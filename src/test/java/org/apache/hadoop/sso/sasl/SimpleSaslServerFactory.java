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

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.Map;

public class SimpleSaslServerFactory implements SaslServerFactory {

  private static final String[] MECHS = new String[]{ "SIMPLE" };

  @Override
  public SaslServer createSaslServer( String mechanism, String protocol, String serverName, Map<String, ?> props, CallbackHandler callbackHandler ) throws SaslException {
    return new SimpleSaslServer( callbackHandler );
  }

  @Override
  public String[] getMechanismNames( Map<String, ?> properties ) {
    return MECHS;
  }

}
