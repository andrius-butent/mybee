package com.butent.bee.server.authentication;

import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 * The Server Authentication Context is an extra (required) indirection between the Application
 * Server and the actual Server Authentication Module (SAM). This can be used to encapsulate any
 * number of SAMs and either select one at run-time, invoke them all in order, etc.
 * <p>
 * Since this simple example only has a single SAM, we delegate directly to that one. Note that this
 * {@link ServerAuthContext} and the {@link ServerAuthModule} (SAM) share a common base interface:
 * {ServerAuth}.
 */
public class BeeServerAuthContext implements ServerAuthContext {

  private final ServerAuthModule serverAuthModule;

  public BeeServerAuthContext(CallbackHandler handler, ServerAuthModule serverAuthModule)
      throws AuthException {

    this.serverAuthModule = serverAuthModule;
    Map<String, String> options = Collections.emptyMap();
    serverAuthModule.initialize(null, null, handler, options);
  }

  @Override
  public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    serverAuthModule.cleanSubject(messageInfo, subject);
  }

  @Override
  public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
      throws AuthException {

    return serverAuthModule.secureResponse(messageInfo, serviceSubject);
  }

  @Override
  public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject,
      Subject serviceSubject) throws AuthException {

    return serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
  }
}
