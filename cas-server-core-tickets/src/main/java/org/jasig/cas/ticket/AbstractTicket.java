package org.jasig.cas.ticket;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * Abstract implementation of a ticket that handles all ticket state for
 * policies. Also incorporates properties common among all tickets. As this is
 * an abstract class, it cannnot be instanciated. It is recommended that
 * implementations of the Ticket interface extend the AbstractTicket as it
 * handles common functionality amongst different ticket types (such as state
 * updating).
 *
 * AbstractTicket does not provide a protected Logger instance to
 * avoid instantiating many such Loggers at runtime (there will be many instances
 * of subclasses of AbstractTicket in a typical running CAS server).  Instead
 * subclasses should use static Logger instances.
 *
 * @author Scott Battaglia
 * @since 3.0.0
 */
@MappedSuperclass
public abstract class AbstractTicket implements Ticket, TicketState {

    private static final long serialVersionUID = -8506442397878267555L;

    /**
     * The {@link ExpirationPolicy} this is associated with.
     **/
    @Lob
    @Column(name="EXPIRATION_POLICY", length = 1000000, nullable=false)
    private ExpirationPolicy expirationPolicy;

    /** The unique identifier for this ticket. */
    @Id
    @Column(name="ID", nullable=false)
    private String id;

    /**
     * The {@link TicketGrantingTicket} this is associated with.
     **/
    @ManyToOne(targetEntity=TicketGrantingTicketImpl.class)
    private TicketGrantingTicket ticketGrantingTicket;

    /** The last time this ticket was used. */
    @Column(name="LAST_TIME_USED")
    private long lastTimeUsed;

    /** The previous last time this ticket was used. */
    @Column(name="PREVIOUS_LAST_TIME_USED")
    private long previousLastTimeUsed;

    /** The time the ticket was created. */
    @Column(name="CREATION_TIME")
    private long creationTime;

    /** The number of times this was used. */
    @Column(name="NUMBER_OF_TIMES_USED")
    private int countOfUses;

    /**
     * Instantiates a new abstract ticket.
     */
    protected AbstractTicket() {
        // nothing to do
    }

    /**
     * Constructs a new Ticket with a unique id, a possible parent Ticket (can
     * be null) and a specified Expiration Policy.
     *
     * @param id the unique identifier for the ticket
     * @param ticket the parent TicketGrantingTicket
     * @param expirationPolicy the expiration policy for the ticket.
     * @throws IllegalArgumentException if the id or expiration policy is null.
     */
    public AbstractTicket(final String id, final TicketGrantingTicket ticket,
        final ExpirationPolicy expirationPolicy) {
        Assert.notNull(expirationPolicy, "expirationPolicy cannot be null");
        Assert.notNull(id, "id cannot be null");

        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastTimeUsed = System.currentTimeMillis();
        this.expirationPolicy = expirationPolicy;
        this.ticketGrantingTicket = ticket;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    /**
     * Records the <i>previous</i> last time this ticket was used as well as
     * the last usage time. The ticket usage count is also incremented.
     *
     * <p>Tickets themselves are solely responsible to maintain their state. The
     * determination of  ticket usage is left up to the implementation and
     * the specific ticket type.
     *
     * @see ExpirationPolicy
     */
    protected final void updateState() {
        this.previousLastTimeUsed = this.lastTimeUsed;
        this.lastTimeUsed = System.currentTimeMillis();
        this.countOfUses++;
    }

    @Override
    public final int getCountOfUses() {
        return this.countOfUses;
    }

    @Override
    public final long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public final TicketGrantingTicket getGrantingTicket() {
        return this.ticketGrantingTicket;
    }

    @Override
    public final long getLastTimeUsed() {
        return this.lastTimeUsed;
    }

    @Override
    public final long getPreviousTimeUsed() {
        return this.previousLastTimeUsed;
    }

    @Override
    public final boolean isExpired() {
        final TicketGrantingTicket tgt = getGrantingTicket();
        return this.expirationPolicy.isExpired(this)
                || (tgt != null && tgt.isExpired())
                || isExpiredInternal();
    }

    protected boolean isExpiredInternal() {
        return false;
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder(13, 133).append(this.getId()).toHashCode();
    }

    @Override
    public final String toString() {
        return this.getId();
    }
}
