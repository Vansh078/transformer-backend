package com.smart.transformer.config;

import com.smart.transformer.job.DailyReportJob;
import com.smart.transformer.job.ReportCleanupJob;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {

    /**
     * Quartz instantiates Job classes via reflection using a no-arg constructor,
     * which bypasses Spring DI entirely. This factory autowires each Job instance
     * through the Spring context after Quartz creates it, so @RequiredArgsConstructor
     * dependencies (like those in DailyReportJob) actually get populated.
     */
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
        private final ApplicationContext applicationContext;

        public AutowiringSpringBeanJobFactory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);
            applicationContext.getAutowireCapableBeanFactory()
                    .autowireBeanProperties(job, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            return job;
        }
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext,
                                                       JobDetail dailyReportJobDetail,
                                                       Trigger dailyReportJobTrigger,
                                                       JobDetail reportCleanupJobDetail,
                                                       Trigger reportCleanupJobTrigger) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext));
        factory.setJobDetails(dailyReportJobDetail, reportCleanupJobDetail);
        factory.setTriggers(dailyReportJobTrigger, reportCleanupJobTrigger);
        return factory;
    }

    @Bean
    public JobDetail dailyReportJobDetail() {
        return JobBuilder.newJob(DailyReportJob.class)
                .withIdentity("dailyReportJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyReportJobTrigger(JobDetail dailyReportJobDetail) {
        // Runs every day at midnight server time — generates a report per transformer
        // and emails the link to maintenance engineers (Report Management task 5).
        return TriggerBuilder.newTrigger()
                .forJob(dailyReportJobDetail)
                .withIdentity("dailyReportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
    }

    @Bean
    public JobDetail reportCleanupJobDetail() {
        return JobBuilder.newJob(ReportCleanupJob.class)
                .withIdentity("reportCleanupJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportCleanupJobTrigger(JobDetail reportCleanupJobDetail) {
        // Runs weekly (Sunday 1 AM) — archives/deletes reports past the configured retention period.
        return TriggerBuilder.newTrigger()
                .forJob(reportCleanupJobDetail)
                .withIdentity("reportCleanupTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 ? * SUN"))
                .build();
    }
}
