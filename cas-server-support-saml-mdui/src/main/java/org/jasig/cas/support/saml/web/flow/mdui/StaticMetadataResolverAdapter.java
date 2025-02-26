package org.jasig.cas.support.saml.web.flow.mdui;

import org.opensaml.saml.metadata.resolver.filter.impl.MetadataFilterChain;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link StaticMetadataResolverAdapter} that loads metadata from static xml files
 * served by urls or locally.
 *
 * @author Misagh Moayyed
 * @since 4.1.0
 */
public final class StaticMetadataResolverAdapter extends AbstractMetadataResolverAdapter implements Job {
    private static final int DEFAULT_METADATA_REFRESH_INTERNAL_MINS = 300;

    /**
      * Refresh metadata every {@link #DEFAULT_METADATA_REFRESH_INTERNAL_MINS}
      * minutes by default.
      **/
    private int refreshIntervalInMinutes = DEFAULT_METADATA_REFRESH_INTERNAL_MINS;

    /**
     * New ctor - required for serialization and job scheduling.
     */
    public StaticMetadataResolverAdapter() {
        super();
    }

    /**
     * Instantiates a new static metadata resolver adapter.
     *
     * @param metadataResources the metadata resources
     */
    public StaticMetadataResolverAdapter(final Map<Resource, MetadataFilterChain> metadataResources) {
        super(metadataResources);
    }

    public void setRefreshIntervalInMinutes(final int refreshIntervalInMinutes) {
        this.refreshIntervalInMinutes = refreshIntervalInMinutes;
    }

    /**
     * Refresh metadata. Schedules the job to retrieve metadata.
     * @throws SchedulerException the scheduler exception
     */
    @PostConstruct
    public void refreshMetadata() throws SchedulerException {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildMetadataResolverAggregate();
            }
        });
        thread.start();

        final JobDetail job = JobBuilder.newJob(this.getClass())
                .withIdentity(this.getClass().getSimpleName().concat(UUID.randomUUID().toString())).build();
        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(this.getClass().getSimpleName().concat(UUID.randomUUID().toString()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(this.refreshIntervalInMinutes)
                        .repeatForever()).build();

        final SchedulerFactory schFactory = new StdSchedulerFactory();
        final Scheduler sch = schFactory.getScheduler();
        sch.start();
        sch.scheduleJob(job, trigger);
    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        buildMetadataResolverAggregate();
    }
}
