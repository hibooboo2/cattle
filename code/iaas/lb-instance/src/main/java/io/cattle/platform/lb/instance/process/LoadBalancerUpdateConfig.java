package io.cattle.platform.lb.instance.process;

import static io.cattle.platform.core.model.tables.PortTable.PORT;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lb.instance.process.lock.LoadBalancerInstancePortLock;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.lb.instance.service.impl.LoadBalancerLookup;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringListProperty;

@Named
public class LoadBalancerUpdateConfig extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    private static final DynamicStringListProperty ITEMS = ArchaiusUtil.getList("lb.config.items");
    private static final DynamicStringListProperty PROCESS_NAMES = ArchaiusUtil.getList("lb.instance.update.config.item.processes");

    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

    @Inject
    LoadBalancerDao lbDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ConfigItemStatusManager statusManager;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    List<LoadBalancerLookup> lbLookups;

    @Inject
    LoadBalancerTargetDao targetDao;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        List<String> result = PROCESS_NAMES.get();
        return result.toArray(new String[result.size()]);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Set<Long> lbIds = new HashSet<>();

        for (LoadBalancerLookup lookup : lbLookups) {
            lbIds = lookup.getLoadBalancerIds(state.getResource());
            if (!lbIds.isEmpty()) {
                break;
            }
        }

        // update config only for active load balancers
        Set<Long> activeLbIds = new HashSet<>();
        for (Long lbId : lbIds) {
            LoadBalancer lb = objectManager.loadResource(LoadBalancer.class, lbId);
            List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE);
            if (activeStates.contains(lb.getState())) {
                activeLbIds.add(lbId);
            }
        }

        createLoadBalancerInstances(state, process, lbIds);

        updateLoadBalancerConfigs(state, lbIds);

        updatePublicPorts(state, lbIds);

        return null;
    }

    private void updateLoadBalancerConfigs(ProcessState state, Set<Long> lbIds) {
        List<Agent> agents = new ArrayList<>();
        for (Long lbId : lbIds) {
            agents.addAll(lbInstanceManager.getLoadBalancerAgents(objectManager.loadResource(LoadBalancer.class, lbId)));
        }

        for (Agent agent : agents) {
            ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, getContext(agent));
            request = before(request, agent);
            ConfigUpdateRequestUtils.setRequest(request, state, getContext(agent));
        }

        for (Agent agent : agents) {
            ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, getContext(agent));
            after(request);
            ConfigUpdateRequestUtils.setRequest(request, state, getContext(agent));
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected ConfigUpdateRequest before(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = new ConfigUpdateRequest(agent.getId());
            for (String item : ITEMS.get()) {
                request.addItem(item).withApply(true).withIncrement(true).setCheckInSyncOnly(true);
            }
        }

        statusManager.updateConfig(request);

        return request;
    }

    protected void after(ConfigUpdateRequest request) {
        if (request == null) {
            return;
        }

        statusManager.waitFor(request);
    }

    public String getContext(Agent agent) {
        return String.format("AgentUpdateConfig:%s", agent.getId());
    }

    private void createLoadBalancerInstances(ProcessState state, ProcessInstance process, Set<Long> lbIds) {
        List<Instance> lbInstances = new ArrayList<>();
        // create lb instances and open the ports for them
        for (Long lbId : lbIds) {
            LoadBalancer lb = loadResource(LoadBalancer.class, lbId);
            lbInstances.addAll(lbInstanceManager.createLoadBalancerInstances(lb));
        }
        for (Instance lbInstance : lbInstances) {
            lbInstance = resourceMonitor.waitFor(lbInstance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return InstanceConstants.STATE_RUNNING.equals(obj.getState());
                }
            });
        }
    }

    private void updatePublicPorts(ProcessState state, Set<Long> lbIds) {
        for (Long lbId : lbIds) {
            final LoadBalancer lb = loadResource(LoadBalancer.class, lbId);
            List<? extends LoadBalancerListener> listeners = lbDao.listActiveListenersForConfig(lb
                    .getLoadBalancerConfigId());
            final Set<Integer> listenerPorts = new HashSet<>();
            for (LoadBalancerListener listener : listeners) {
                listenerPorts.add(listener.getSourcePort());
            }
            List<? extends Instance> lbInstances = lbInstanceManager.getLoadBalancerInstances(lb);
            // surround by lock
            for (final Instance lbInstance : lbInstances) {
                final boolean hasActiveInstances = (targetDao.getLoadBalancerActiveInstanceTargets(lb.getId()).size() + targetDao
                        .getLoadBalancerActiveIpTargets(lb.getId()).size()) > 0;
                final Map<Integer, Port> portsToCreate = new HashMap<Integer, Port>();
                final Map<Integer, Port> portsToRemove = new HashMap<Integer, Port>();
                lockManager.lock(new LoadBalancerInstancePortLock(lbInstance), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        Map<Integer, Port> existingPorts = new HashMap<Integer, Port>();
                        for (Port port : objectManager.children(lbInstance, Port.class)) {
                            if (port.getRemoved() == null && !port.getState().equals(CommonStatesConstants.REMOVED)) {
                                existingPorts.put(port.getPublicPort(), port);
                            }
                        }
                        for (Port port : existingPorts.values()) {
                            if (listenerPorts.contains(port.getPublicPort()) && hasActiveInstances) {
                                portsToCreate.put(port.getPublicPort(), port);
                            } else {
                                portsToRemove.put(port.getPublicPort(), port);
                            }
                        }

                        if (hasActiveInstances) {
                            for (Integer listenerPort : listenerPorts) {
                                if (existingPorts.containsKey(listenerPort)) {
                                    continue;
                                }

                                Nic primaryNic = networkDao.getPrimaryNic(lbInstance.getId());
                                if (primaryNic == null) {
                                    continue;
                                }
                                IpAddress ipAddress = ipAddressDao.getPrimaryIpAddress(primaryNic);
                                if (ipAddress == null) {
                                    continue;
                                }

                                Port portObj = objectManager.create(Port.class, PORT.KIND, PortConstants.KIND_USER,

                                        PORT.ACCOUNT_ID,
                                        lbInstance.getAccountId(),
                                        PORT.INSTANCE_ID, lbInstance.getId(), PORT.PUBLIC_PORT, listenerPort,
                                        PORT.PRIVATE_PORT, listenerPort, PORT.PROTOCOL, "tcp",
                                        PORT.PRIVATE_IP_ADDRESS_ID, ipAddress.getId());
                                portsToCreate.put(portObj.getPublicPort(), portObj);
                            }
                        }
                    }
                });

                if (hasActiveInstances) {
                    for (Port port : portsToCreate.values()) {
                        createThenActivate(port, state.getData());
                    }
                }

                for (Port port : portsToRemove.values()) {
                    deactivateThenRemove(port, state.getData());
                }
            }
        }
    }
}
