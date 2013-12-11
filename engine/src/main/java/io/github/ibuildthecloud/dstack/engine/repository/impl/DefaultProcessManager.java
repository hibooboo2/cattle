package io.github.ibuildthecloud.dstack.engine.repository.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.context.EngineContext;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.engine.process.impl.DefaultProcessInstanceImpl;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessManager;
import io.github.ibuildthecloud.dstack.engine.repository.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.concurrent.DelayedObject;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.netflix.config.DynamicLongProperty;

public class DefaultProcessManager implements ProcessManager, InitializationTask {

    private static final DynamicLongProperty EXECUTION_DELAY = ArchaiusUtil.getLongProperty("process.log.save.interval.ms");

    ProcessRecordDao processRecordDao;
    List<ProcessDefinition> definitionList;
    Map<String, ProcessDefinition> definitions;
    LockManager lockManager;
    DelayQueue<DelayedObject<DefaultProcessInstanceImpl>> toPersist = new DelayQueue<DelayedObject<DefaultProcessInstanceImpl>>();
    ScheduledExecutorService executor;
    EventService eventService;

    @Override
    public ProcessInstance createProcessInstance(LaunchConfiguration config) {
        return createProcessInstance(new ProcessRecord(config, null, null));
    }

    protected ProcessInstance createProcessInstance(ProcessRecord record) {
        if ( record == null )
            return null;

        ProcessDefinition processDef = definitions.get(record.getProcessName());

        if ( processDef == null )
            throw new ProcessNotFoundException("Failed to find ProcessDefinition for [" + record.getProcessName() + "]");

        ProcessState state = processDef.constructProcessState(record);
        if ( state == null )
            throw new ProcessNotFoundException("Failed to construct ProcessState for [" + record.getProcessName() + "]");

        if ( record.getId() == null && ! EngineContext.hasParentProcess() )
            record = processRecordDao.insert(record);

        DefaultProcessInstanceImpl process = new DefaultProcessInstanceImpl(this, lockManager, eventService, record, processDef, state);
        toPersist.put(new DelayedObject<DefaultProcessInstanceImpl>(System.currentTimeMillis() + EXECUTION_DELAY.get(), process));

        return process;
    }

    @Override
    public void persistState(ProcessInstance process) {
        if ( ! ( process instanceof DefaultProcessInstanceImpl ) ) {
            throw new IllegalArgumentException("Can only persist ProcessInstances that are created by this repository");
        }

        DefaultProcessInstanceImpl processImpl = (DefaultProcessInstanceImpl)process;

        synchronized (processImpl) {
            ProcessRecord record = processImpl.getProcessRecord();
            if ( record.getId() != null )
                processRecordDao.update(record);
        }
    }

    @Override
    public List<Long> pendingTasks() {
        return processRecordDao.pendingTasks();
    }

    @Override
    public ProcessInstance loadProcess(Long id) {
        ProcessRecord record = processRecordDao.getRecord(id);
        return createProcessInstance(record);
    }

    protected void persistInProgress() throws InterruptedException {
        while ( true ) {
            ProcessInstance process = toPersist.take().getObject();
            synchronized (process) {
                if ( process.isRunningLogic() ) {
                    persistState(process);
                }
            }
        }
    }

    @Override
    public void start() {
        definitions = NamedUtils.createMapByName(definitionList);

        executor.scheduleAtFixedRate(new NoExceptionRunnable() {
            @Override
            public void doRun() throws Exception {
                /* This really blocks forever, but just in case it fails we restart */
                persistInProgress();
            }
        }, EXECUTION_DELAY.get(), EXECUTION_DELAY.get(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
    }

    public ProcessRecordDao getProcessRecordDao() {
        return processRecordDao;
    }

    @Inject
    public void setProcessRecordDao(ProcessRecordDao processRecordDao) {
        this.processRecordDao = processRecordDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Inject
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public List<ProcessDefinition> getDefinitionList() {
        return definitionList;
    }

    @Inject
    public void setDefinitionList(List<ProcessDefinition> definitionList) {
        this.definitionList = definitionList;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
