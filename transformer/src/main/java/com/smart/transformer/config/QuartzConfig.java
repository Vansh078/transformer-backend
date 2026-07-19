package com.smart.transformer.config;

import com.smart.transformer.job.DailyReportJob;
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
                                                       Trigger dailyReportJobTrigger) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext));
        factory.setJobDetails(dailyReportJobDetail);
        factory.setTriggers(dailyReportJobTrigger);
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
        // Runs every day at 7:00 AM server time — adjust the cron as needed
        return TriggerBuilder.newTrigger()
                .forJob(dailyReportJobDetail)
                .withIdentity("dailyReportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 7 * * ?"))
                .build();
    }
}
