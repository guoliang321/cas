package org.jasig.cas.ticket.registry;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.ticket.ExpirationPolicy;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;

import java.util.List;
import java.util.Map;

/**
 * Abstract Implementation that handles some of the commonalities between
 * distributed ticket registries.
 *
 * @author Scott Battaglia
 * @since 3.1
 */
public abstract class AbstractDistributedTicketRegistry extends AbstractTicketRegistry {

    /**
     * Update the received ticket.
     *
     * @param ticket the ticket
     */
    protected abstract void updateTicket(Ticket ticket);

    /**
     * Whether or not a callback to the TGT is required when checking for expiration.
     *
     * @return true, if successful
     */
    protected abstract boolean needsCallback();

    /**
     * Gets the proxied ticket instance.
     *
     * @param ticket the ticket
     * @return the proxied ticket instance
     */
    protected final Ticket getProxiedTicketInstance(final Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        if (ticket instanceof TicketGrantingTicket) {
            return new TicketGrantingTicketDelegator(this, (TicketGrantingTicket) ticket, needsCallback());
        }

        return new ServiceTicketDelegator(this, (ServiceTicket) ticket, needsCallback());
    }

    private static class TicketDelegator<T extends Ticket> implements Ticket {

        private static final long serialVersionUID = 1780193477774123440L;

        private final AbstractDistributedTicketRegistry ticketRegistry;

        private final T ticket;

        private final boolean callback;

        /**
         * Instantiates a new ticket delegator.
         *
         * @param ticketRegistry the ticket registry
         * @param ticket the ticket
         * @param callback the callback
         */
        protected TicketDelegator(final AbstractDistributedTicketRegistry ticketRegistry,
                final T ticket, final boolean callback) {
            this.ticketRegistry = ticketRegistry;
            this.ticket = ticket;
            this.callback = callback;
        }
        
        
        /**
         * Update ticket by the delegated registry.
         */
        protected void updateTicket() {
            this.ticketRegistry.updateTicket(this.ticket);
        }

        protected T getTicket() {
            return this.ticket;
        }

        @Override
        public final String getId() {
            return this.ticket.getId();
        }

        @Override
        public final boolean isExpired() {
            if (!callback) {
                return this.ticket.isExpired();
            }

            final TicketGrantingTicket t = getGrantingTicket();

            return this.ticket.isExpired() || (t != null && t.isExpired());
        }

        @Override
        public final TicketGrantingTicket getGrantingTicket() {
            final TicketGrantingTicket old = this.ticket.getGrantingTicket();

            if (old == null || !callback) {
                return old;
            }

            return this.ticketRegistry.getTicket(old.getId(), Ticket.class);
        }

        @Override
        public final long getCreationTime() {
            return this.ticket.getCreationTime();
        }

        @Override
        public final int getCountOfUses() {
            return this.ticket.getCountOfUses();
        }

        @Override
        public int hashCode() {
            return this.ticket.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            return this.ticket.equals(o);
        }
    }

    private static final class ServiceTicketDelegator extends TicketDelegator<ServiceTicket>
                           implements ServiceTicket {

        private static final long serialVersionUID = 8160636219307822967L;

        /**
         * Instantiates a new service ticket delegator.
         *
         * @param ticketRegistry the ticket registry
         * @param serviceTicket the service ticket
         * @param callback the callback
         */
        protected ServiceTicketDelegator(final AbstractDistributedTicketRegistry ticketRegistry,
                final ServiceTicket serviceTicket, final boolean callback) {
            super(ticketRegistry, serviceTicket, callback);
        }

        @Override
        public Service getService() {
            return getTicket().getService();
        }

        @Override
        public boolean isFromNewLogin() {
            return getTicket().isFromNewLogin();
        }

        @Override
        public boolean isValidFor(final Service service) {
            final boolean b = this.getTicket().isValidFor(service);
            updateTicket();
            return b;
        }

        @Override
        public TicketGrantingTicket grantTicketGrantingTicket(final String id,
                final Authentication authentication, final ExpirationPolicy expirationPolicy) {
            final TicketGrantingTicket t = this.getTicket().grantTicketGrantingTicket(id,
                    authentication, expirationPolicy);
            updateTicket();
            return t;
        }
    }

    private static final class TicketGrantingTicketDelegator extends TicketDelegator<TicketGrantingTicket>
            implements TicketGrantingTicket {

        private static final long serialVersionUID = 5312560061970601497L;

        /**
         * Instantiates a new ticket granting ticket delegator.
         *
         * @param ticketRegistry the ticket registry
         * @param ticketGrantingTicket the ticket granting ticket
         * @param callback the callback
         */
        protected TicketGrantingTicketDelegator(final AbstractDistributedTicketRegistry ticketRegistry,
                final TicketGrantingTicket ticketGrantingTicket, final boolean callback) {
            super(ticketRegistry, ticketGrantingTicket, callback);
        }

        @Override
        public Authentication getAuthentication() {
            return getTicket().getAuthentication();
        }

        @Override
        public Service getProxiedBy() {
            return getTicket().getProxiedBy();
        }

        @Override
        public List<Authentication> getSupplementalAuthentications() {
            return getTicket().getSupplementalAuthentications();
        }

        @Override
        public ServiceTicket grantServiceTicket(final String id, final Service service,
                final ExpirationPolicy expirationPolicy, final boolean credentialsProvided,
                final boolean onlyTrackMostRecentSession) {
            final ServiceTicket t = this.getTicket().grantServiceTicket(id, service,
                    expirationPolicy, credentialsProvided, onlyTrackMostRecentSession);
            updateTicket();
            return t;
        }

        @Override
        public void markTicketExpired() {
            this.getTicket().markTicketExpired();
            updateTicket();
        }

        @Override
        public boolean isRoot() {
            return getTicket().isRoot();
        }

        @Override
        public TicketGrantingTicket getRoot() {
            return getTicket().getRoot();
        }

        @Override
        public List<Authentication> getChainedAuthentications() {
            return getTicket().getChainedAuthentications();
        }

        @Override
        public Map<String, Service> getServices() {
            return this.getTicket().getServices();
        }

        @Override
        public void removeAllServices() {
            this.getTicket().removeAllServices();
        }
    }
}
