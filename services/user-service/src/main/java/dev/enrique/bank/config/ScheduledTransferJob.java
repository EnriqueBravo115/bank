package dev.enrique.bank.config;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import dev.enrique.bank.service.impl.TransactionServiceImpl;

public class ScheduledTransferJob implements Job {
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long transferId = jobDataMap.getLong("transferId");

        TransactionServiceImpl transferService = applicationContext.getBean(TransactionServiceImpl.class);
        //transferService.executeScheduledTransfer(transferId);
    }
}
