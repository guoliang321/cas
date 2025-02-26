package org.jasig.cas.adaptors.radius.authentication.handler.support;

import org.jasig.cas.adaptors.radius.RadiusResponse;
import org.jasig.cas.adaptors.radius.RadiusServer;
import org.jasig.cas.authentication.HandlerResult;
import org.jasig.cas.authentication.PreventedException;
import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;

import javax.security.auth.login.FailedLoginException;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Authentication Handler to authenticate a user against a RADIUS server.
 *
 * @author Scott Battaglia
 * @since 3.0.0
 */
public class RadiusAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {

    /** Array of RADIUS servers to authenticate against. */
    @NotNull
    @Size(min=1)
    private List<RadiusServer> servers;

    /**
     * Determines whether to fail over to the next configured RadiusServer if
     * there was an exception.
     */
    private boolean failoverOnException;

    /**
     * Determines whether to fail over to the next configured RadiusServer if
     * there was an authentication failure.
     */
    private boolean failoverOnAuthenticationFailure;


    /**
     * Instantiates a new Radius authentication handler.
     */
    public RadiusAuthenticationHandler() {
        super();
        logger.debug("Using {}", getClass().getSimpleName());
    }

    @Override
    protected final HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential credential)
            throws GeneralSecurityException, PreventedException {

        final String password = getPasswordEncoder().encode(credential.getPassword());
        final String username = credential.getUsername();
        
        for (final RadiusServer radiusServer : this.servers) {
            logger.debug("Attempting to authenticate {} at {}", username, radiusServer);
            try {
                final RadiusResponse response = radiusServer.authenticate(username, password);
                if (response != null) {
                     return createHandlerResult(credential, this.principalFactory.createPrincipal(username), null);
                } 
                                
                if (!this.failoverOnAuthenticationFailure) {
                    throw new FailedLoginException("Radius authentication failed for user " + username);
                }
                logger.debug("failoverOnAuthenticationFailure enabled -- trying next server");
            } catch (final PreventedException e) {
                if (!this.failoverOnException) {
                    throw e;
                }
                logger.warn("failoverOnException enabled -- trying next server.", e);
            }
        }
        throw new FailedLoginException("Radius authentication failed for user " + username);
    }

    /**
     * Determines whether to fail over to the next configured RadiusServer if
     * there was an authentication failure.
     *
     * @param failoverOnAuthenticationFailure boolean on whether to failover or
     * not.
     */
    public final void setFailoverOnAuthenticationFailure(
            final boolean failoverOnAuthenticationFailure) {
        this.failoverOnAuthenticationFailure = failoverOnAuthenticationFailure;
    }

    /**
     * Determines whether to fail over to the next configured RadiusServer if
     * there was an exception.
     *
     * @param failoverOnException boolean on whether to failover or not.
     */
    public final void setFailoverOnException(final boolean failoverOnException) {
        this.failoverOnException = failoverOnException;
    }

    public final void setServers(final List<RadiusServer> servers) {
        this.servers = servers;
    }
}
