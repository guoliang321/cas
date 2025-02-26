package org.jasig.cas.web.flow;

import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.authentication.Credential;
import org.jasig.cas.authentication.principal.PrincipalFactory;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.ticket.AbstractTicketException;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.web.support.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.validation.constraints.NotNull;

/**
 * Abstract class to handle the retrieval and authentication of non-interactive
 * credential such as client certificates, NTLM, etc.
 *
 * @author Scott Battaglia

 * @since 3.0.0.4
 */
@Component
public abstract class AbstractNonInteractiveCredentialsAction extends AbstractAction {

    /** The logger instance. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The Principal factory.
     */
    @Autowired
    @Qualifier("principalFactory")
    protected PrincipalFactory principalFactory;

    /** Instance of CentralAuthenticationService. */
    @NotNull
    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;

    /**
     * Checks if is renew present.
     *
     * @param context the context
     * @return true, if  renew present
     */
    protected final boolean isRenewPresent(final RequestContext context) {
        return StringUtils.hasText(context.getRequestParameters().get("renew"));
    }

    @Override
    protected final Event doExecute(final RequestContext context) {
        final Credential credential = constructCredentialsFromRequest(context);

        if (credential == null) {
            return error();
        }

        final String ticketGrantingTicketId = WebUtils.getTicketGrantingTicketId(context);
        final Service service = WebUtils.getService(context);

        if (isRenewPresent(context)
            && ticketGrantingTicketId != null
            && service != null) {

            try {
                final ServiceTicket serviceTicketId = this.centralAuthenticationService
                    .grantServiceTicket(ticketGrantingTicketId,
                        service,
                            credential);
                WebUtils.putServiceTicketInRequestScope(context, serviceTicketId);
                return result("warn");
            } catch (final AuthenticationException e) {
                onError(context, credential);
                return error();
            } catch (final AbstractTicketException e) {
                this.centralAuthenticationService.destroyTicketGrantingTicket(ticketGrantingTicketId);
                logger.debug("Attempted to generate a ServiceTicket using renew=true with different credential", e);
            }
        }

        try {
            WebUtils.putTicketGrantingTicketInScopes(
                context,
                this.centralAuthenticationService
                    .createTicketGrantingTicket(credential));
            onSuccess(context, credential);
            return success();
        } catch (final Exception e) {
            onError(context, credential);
            return error();
        }
    }

    public CentralAuthenticationService getCentralAuthenticationService() {
        return centralAuthenticationService;
    }

    public final void setCentralAuthenticationService(
        final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    /**
     * Sets principal factory to create principal objects.
     *
     * @param principalFactory the principal factory
     */
    public void setPrincipalFactory(final PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    /**
     * Hook method to allow for additional processing of the response before
     * returning an error event.
     *
     * @param context the context for this specific request.
     * @param credential the credential for this request.
     */
    protected void onError(final RequestContext context,
        final Credential credential) {
        // default implementation does nothing
    }

    /**
     * Hook method to allow for additional processing of the response before
     * returning a success event.
     *
     * @param context the context for this specific request.
     * @param credential the credential for this request.
     */
    protected void onSuccess(final RequestContext context,
        final Credential credential) {
        // default implementation does nothing
    }

    /**
     * Abstract method to implement to construct the credential from the
     * request object.
     *
     * @param context the context for this request.
     * @return the constructed credential or null if none could be constructed
     * from the request.
     */
    protected abstract Credential constructCredentialsFromRequest(
        RequestContext context);
}
