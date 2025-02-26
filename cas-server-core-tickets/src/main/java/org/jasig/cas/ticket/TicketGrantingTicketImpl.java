package org.jasig.cas.ticket;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.principal.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of a TicketGrantingTicket. A TicketGrantingTicket is
 * the global identifier of a principal into the system. It grants the Principal
 * single-sign on access to any service that opts into single-sign on.
 * Expiration of a TicketGrantingTicket is controlled by the ExpirationPolicy
 * specified as object creation.
 *
 * @author Scott Battaglia
 * @since 3.0.0
 */
@Entity
@Table(name="TICKETGRANTINGTICKET")
public final class TicketGrantingTicketImpl extends AbstractTicket implements TicketGrantingTicket {

    /** Unique Id for serialization. */
    private static final long serialVersionUID = -8608149809180911599L;

    /** Logger instance. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketGrantingTicketImpl.class);

    /** The authenticated object for which this ticket was generated for. */
    @Lob
    @Column(name="AUTHENTICATION", nullable=false, length = 1000000)
    private Authentication authentication;

    /** Flag to enforce manual expiration. */
    @Column(name="EXPIRED", nullable=false)
    private Boolean expired = Boolean.FALSE;

    /** Service that produced a proxy-granting ticket. */
    @Column(name="PROXIED_BY", nullable=true)
    private Service proxiedBy;

    /** The services associated to this ticket. */
    @Lob
    @Column(name="SERVICES_GRANTED_ACCESS_TO", nullable=false, length = 1000000)
    private final HashMap<String, Service> services = new HashMap<>();

    @Lob
    @Column(name="SUPPLEMENTAL_AUTHENTICATIONS", nullable=false, length = 1000000)
    private final ArrayList<Authentication> supplementalAuthentications = new ArrayList<>();

    /**
     * Instantiates a new ticket granting ticket impl.
     */
    public TicketGrantingTicketImpl() {
        // nothing to do
    }

    /**
     * Constructs a new TicketGrantingTicket.
     * May throw an {@link IllegalArgumentException} if the Authentication object is null.
     *
     * @param id the id of the Ticket
     * @param proxiedBy Service that produced this proxy ticket.
     * @param parentTicketGrantingTicket the parent ticket
     * @param authentication the Authentication request for this ticket
     * @param policy the expiration policy for this ticket.
     */
    public TicketGrantingTicketImpl(final String id,
        final Service proxiedBy,
        final TicketGrantingTicket parentTicketGrantingTicket,
        @NotNull final Authentication authentication, final ExpirationPolicy policy) {

        super(id, parentTicketGrantingTicket, policy);

        if (parentTicketGrantingTicket != null && proxiedBy == null) {
            throw new IllegalArgumentException("Must specify proxiedBy when providing parent TGT");
        }
        Assert.notNull(authentication, "authentication cannot be null");
        this.authentication = authentication;
        this.proxiedBy = proxiedBy;
    }

    /**
     * Constructs a new TicketGrantingTicket without a parent
     * TicketGrantingTicket.
     *
     * @param id the id of the Ticket
     * @param authentication the Authentication request for this ticket
     * @param policy the expiration policy for this ticket.
     */
    public TicketGrantingTicketImpl(final String id,
        final Authentication authentication, final ExpirationPolicy policy) {
        this(id, null, null, authentication, policy);
    }


    @Override
    public Authentication getAuthentication() {
        return this.authentication;
    }

    /**
     * {@inheritDoc}
     * <p>The state of the ticket is affected by this operation and the
     * ticket will be considered used. The state update subsequently may
     * impact the ticket expiration policy in that, depending on the policy
     * configuration, the ticket may be considered expired.
     */
    @Override
    public synchronized ServiceTicket grantServiceTicket(final String id,
        final Service service, final ExpirationPolicy expirationPolicy,
        final boolean credentialsProvided, final boolean onlyTrackMostRecentSession) {
        final ServiceTicket serviceTicket = new ServiceTicketImpl(id, this,
            service, this.getCountOfUses() == 0 || credentialsProvided,
            expirationPolicy);

        updateState();

        final List<Authentication> authentications = getChainedAuthentications();
        service.setPrincipal(authentications.get(authentications.size()-1).getPrincipal());

        if (onlyTrackMostRecentSession) {
            final String path = normalizePath(service);
            final Collection<Service> existingServices = services.values();
            // loop on existing services
            for (final Service existingService : existingServices) {
                final String existingPath = normalizePath(existingService);
                // if an existing service has the same normalized path, remove it
                // and its service ticket to keep the latest one
                if (StringUtils.equals(path, existingPath)) {
                    existingServices.remove(existingService);
                    LOGGER.trace("removed previous ST for service: {}", existingService);
                    break;
                }
            }
        }
        this.services.put(id, service);

        return serviceTicket;
    }

    /**
     * Normalize the path of a service by removing the query string and everything after a semi-colon.
     *
     * @param service the service to normalize
     * @return the normalized path
     */
    private String normalizePath(final Service service) {
        String path = service.getId();
        path = StringUtils.substringBefore(path, "?");
        path = StringUtils.substringBefore(path, ";");
        path = StringUtils.substringBefore(path, "#");
        return path;
    }

    /**
     * Gets an immutable map of service ticket and services accessed by this ticket-granting ticket.
     * Unlike {@link java.util.Collections#unmodifiableMap(java.util.Map)},
     * which is a view of a separate map which can still change, an instance of {@link ImmutableMap}
     * contains its own data and will never change.
     *
     * @return an immutable map of service ticket and services accessed by this ticket-granting ticket.
    */
    @Override
    public synchronized Map<String, Service> getServices() {
        return ImmutableMap.copyOf(this.services);
    }

    /**
     * Remove all services of the TGT (at logout).
     */
    @Override
    public void removeAllServices() {
        services.clear();
    }

    /**
     * Return if the TGT has no parent.
     *
     * @return if the TGT has no parent.
     */
    @Override
    public boolean isRoot() {
        return this.getGrantingTicket() == null;
    }


    @Override
    public void markTicketExpired() {
        this.expired = Boolean.TRUE;
    }


    @Override
    public TicketGrantingTicket getRoot() {
        TicketGrantingTicket current = this;
        TicketGrantingTicket parent = current.getGrantingTicket();
        while (parent != null) {
            current = parent;
            parent = current.getGrantingTicket();
        }
        return current;
    }

    /**
     * Return if the TGT is expired.
     *
     * @return if the TGT is expired.
     */
    @Override
    public boolean isExpiredInternal() {
        return this.expired;
    }


    @Override
    public List<Authentication> getSupplementalAuthentications() {
        return this.supplementalAuthentications;
    }


    @Override
    public List<Authentication> getChainedAuthentications() {
        final List<Authentication> list = new ArrayList<>();

        list.add(getAuthentication());

        if (getGrantingTicket() == null) {
            return Collections.unmodifiableList(list);
        }

        list.addAll(getGrantingTicket().getChainedAuthentications());
        return Collections.unmodifiableList(list);
    }

    @Override
    public Service getProxiedBy() {
        return this.proxiedBy;
    }


    @Override
    public boolean equals(final Object object) {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (!(object instanceof TicketGrantingTicket)) {
            return false;
        }

        final Ticket ticket = (Ticket) object;

        return new EqualsBuilder()
                .append(ticket.getId(), this.getId())
                .isEquals();
    }


}
