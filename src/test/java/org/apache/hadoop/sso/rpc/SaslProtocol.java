package org.apache.hadoop.sso.rpc;

import java.io.*;
import java.net.Socket;

class SaslProtocol {
  public static final int COMMAND_AUTHENTICATE = 100;

  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_FAILURE = 1;
  public static final int STATUS_AUTHENTICATING = 2;

  private DataInputStream inStream;
  private DataOutputStream outStream;

  SaslProtocol( Socket socket ) throws IOException {
    inStream = new DataInputStream( socket.getInputStream() );
    outStream = new DataOutputStream( socket.getOutputStream() );
  }

  byte[] receive( int expectedCommand ) throws IOException {
    if( expectedCommand != -1 ) {
      int cmd = inStream.readInt();
      if( expectedCommand != cmd ) {
        throw new IOException( "Received unexpected code: " + cmd );
      }
    }
    byte[] reply;
    int len;
    try {
      len = inStream.readInt();
    } catch( IOException e ) {
      len = 0;
    }
    if( len > 0 ) {
      reply = new byte[ len ];
      inStream.readFully( reply );
    } else {
      reply = new byte[ 0 ];
    }
    return reply;
  }

  Message send( int command, byte[] bytes ) throws IOException {
    outStream.writeInt( command );
    if( bytes != null ) {
      outStream.writeInt( bytes.length );
      if( bytes.length > 0 ) {
        outStream.write( bytes );
      }
    } else {
      outStream.writeInt( 0 );
    }
    outStream.flush();

    if( command == STATUS_SUCCESS || command == STATUS_FAILURE ) {
      return null;
    }

    int returnCode = inStream.readInt();
    byte[] response = null;
    if( returnCode != STATUS_FAILURE ) {
      response = receive( -1 );
    }
    return new Message( returnCode, response );
  }

  static class Message {
    private int code;
    private byte[] bytes;

    Message( int status, byte[] bytes ) {
      this.bytes = bytes;
      this.code = status;
    }

    int getStatus() {
      return code;
    }

    byte[] getBytes() {
      return bytes;
    }
  }

}
