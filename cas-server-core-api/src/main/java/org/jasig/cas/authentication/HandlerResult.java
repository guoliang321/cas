package org.jasig.cas.authentication;

import org.jasig.cas.MessageDescriptor;
import org.jasig.cas.authentication.principal.Principal;

import java.io.Serializable;
import java.util.List;

/**
 * This is {@link HandlerResult} that describes the result of an authentication attempt.
 *
 * @author Misagh Moayyed
 * @since 4.1.0
 */
public interface HandlerResult extends Serializable {

    /**
     * Gets handler name.
     *
     * @return the handler name
     */
    String getHandlerName();

    /**
     * Gets credential meta data.
     *
     * @return the credential meta data
     */
    CredentialMetaData getCredentialMetaData();

    /**
     * Gets principal.
     *
     * @return the principal
     */
    Principal getPrincipal();

    /**
     * Gets warnings.
     *
     * @return the warnings
     */
    List<MessageDescriptor> getWarnings();
}
