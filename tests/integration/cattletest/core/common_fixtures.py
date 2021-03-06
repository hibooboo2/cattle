import cattle
import os
import pytest
import random
import time
import inspect
from datetime import datetime, timedelta

NOT_NONE = object()
DEFAULT_TIMEOUT = 90
DEFAULT_AGENT_URI = 'ssh://root@localhost:22'
DEFAULT_AGENT_UUID = 'test-agent'
SLEEP_DELAY = 0.5


@pytest.fixture(scope='session')
def cattle_url():
    default_url = 'http://localhost:8080/v1/schemas'
    return os.environ.get('CATTLE_URL', default_url)


def _admin_client():
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key='admin',
                           secret_key='adminpass')


def _client_for_user(name, accounts):
    return cattle.from_env(url=cattle_url(),
                           cache=False,
                           access_key=accounts[name][0],
                           secret_key=accounts[name][1])


def create_user(admin_client, user_name, kind=None):
    if kind is None:
        kind = user_name

    password = user_name + 'pass'
    account = create_type_by_uuid(admin_client, 'account', user_name,
                                  kind=user_name,
                                  name=user_name)

    active_cred = None
    for cred in account.credentials():
        if cred.kind == 'apiKey' and cred.publicValue == user_name \
                and cred.secretValue == password:
            active_cred = cred
            break

    if active_cred is None:
        active_cred = admin_client.create_api_key({
            'accountId': account.id,
            'publicValue': user_name,
            'secretValue': password
        })

    active_cred = wait_success(admin_client, active_cred)
    if active_cred.state != 'active':
        wait_success(admin_client, active_cred.activate())

    return [user_name, password, account]


@pytest.fixture(scope='session')
def accounts():
    result = {}
    admin_client = _admin_client()
    for user_name in ['admin', 'agent', 'user', 'agentRegister', 'test',
                      'readAdmin', 'token', 'superadmin', 'service']:
        result[user_name] = create_user(admin_client,
                                        user_name,
                                        kind=user_name)

    result['admin'] = create_user(admin_client, 'admin')
    system_account = admin_client.list_account(kind='system', uuid='system')[0]
    result['system'] = [None, None, system_account]

    return result


@pytest.fixture(scope='session')
def system_account(accounts):
    return accounts['system'][2]


@pytest.fixture(scope='session')
def admin_account(accounts):
    return accounts['admin'][2]


@pytest.fixture(scope='session')
def user_account(accounts):
    return accounts['user'][2]


@pytest.fixture(scope='session')
def token_account(accounts):
    return accounts['token'][2]


@pytest.fixture(scope='session')
def super_account(accounts):
    return accounts['superadmin'][2]


@pytest.fixture(scope='session')
def client(accounts):
    return _client_for_user('user', accounts)


@pytest.fixture(scope='session')
def super_admin_client(accounts):
    return _client_for_user('superadmin', accounts)


@pytest.fixture(scope='session')
def admin_client(accounts):
    return _client_for_user('admin', accounts)


@pytest.fixture(scope='session')
def super_client(request, accounts):
    ret = _client_for_user('superadmin', accounts)
    request.addfinalizer(
        lambda: delete_sim_instances(ret))
    return ret


@pytest.fixture(scope='session')
def token_client(accounts):
    return _client_for_user('token', accounts)


@pytest.fixture(scope='session')
def agent_client(accounts):
    return _client_for_user('agent', accounts)


@pytest.fixture(scope='session')
def service_client(accounts):
    return _client_for_user('service', accounts)


def create_sim_context(super_client, uuid, ip=None, account=None,
                       public=False):
    context = kind_context(super_client,
                           'sim',
                           external_pool=True,
                           account=account,
                           uri='sim://' + uuid,
                           uuid=uuid,
                           host_public=public)
    context['imageUuid'] = 'sim:{}'.format(random_num())

    host = context['host']

    if len(host.ipAddresses()) == 0 and ip is not None:
        ip = create_and_activate(super_client, 'ipAddress',
                                 address=ip,
                                 isPublic=public)
        map = super_client.create_host_ip_address_map(hostId=host.id,
                                                      ipAddressId=ip.id)
        map = super_client.wait_success(map)
        assert map.state == 'active'

    if len(host.ipAddresses()):
        context['hostIp'] = host.ipAddresses()[0]

    return context


@pytest.fixture(scope='session')
def sim_context(request, super_client):
    context = create_sim_context(super_client, 'simagent1', ip='192.168.10.10',
                                 public=True)

    return context


@pytest.fixture(scope='session')
def sim_context2(super_client):
    return create_sim_context(super_client, 'simagent2', ip='192.168.10.11',
                              public=True)


@pytest.fixture(scope='session')
def sim_context3(super_client):
    return create_sim_context(super_client, 'simagent3', ip='192.168.10.12',
                              public=True)


@pytest.fixture
def new_sim_context(super_client):
    uri = 'sim://' + random_str()
    sim_context = kind_context(super_client, 'sim', uri=uri, uuid=uri)

    for i in ['host', 'pool', 'agent']:
        sim_context[i] = super_client.wait_success(sim_context[i])

    host = sim_context['host']
    pool = sim_context['pool']
    agent = sim_context['agent']

    assert host is not None
    assert pool is not None
    assert agent is not None

    return sim_context


@pytest.fixture(scope='session')
def user_sim_context(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent', ip='192.168.11.1',
                              account=user_account)


@pytest.fixture(scope='session')
def user_sim_context2(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent2', ip='192.168.11.2',
                              account=user_account)


@pytest.fixture(scope='session')
def user_sim_context3(super_client, user_account):
    return create_sim_context(super_client, 'usersimagent3', ip='192.168.11.3',
                              account=user_account)


def activate_resource(admin_client, obj):
    if obj.state == 'inactive':
        obj = wait_success(admin_client, obj.activate())

    return obj


def find_by_uuid(admin_client, type, uuid, activate=True, **kw):
    objs = admin_client.list(type, uuid=uuid)
    assert len(objs) == 1

    obj = wait_success(admin_client, objs[0])

    if activate:
        return activate_resource(admin_client, obj)

    return obj


def create_type_by_uuid(admin_client, type, uuid, activate=True, validate=True,
                        **kw):
    opts = dict(kw)
    opts['uuid'] = uuid

    objs = admin_client.list(type, uuid=uuid)
    obj = None
    if len(objs) == 0:
        obj = admin_client.create(type, **opts)
    else:
        obj = objs[0]

    obj = wait_success(admin_client, obj)
    if activate and obj.state == 'inactive':
        obj.activate()
        obj = wait_success(admin_client, obj)

    if validate:
        for k, v in opts.items():
            assert getattr(obj, k) == v

    return obj


def random_num():
    return random.randint(0, 1000000)


@pytest.fixture
def random_str():
    return 'random-{0}'.format(random_num())


def wait_all_success(client, objs, timeout=DEFAULT_TIMEOUT):
    ret = []
    for obj in objs:
        obj = wait_success(client, obj, timeout)
        ret.append(obj)

    return ret


def wait_success(client, obj, timeout=DEFAULT_TIMEOUT):
    return client.wait_success(obj, timeout=timeout)


def wait_transitioning(client, obj, timeout=DEFAULT_TIMEOUT):
    return client.wait_transitioning(obj, timeout=timeout)


@pytest.fixture
def wait_for_condition(client, resource, check_function, fail_handler=None,
                       timeout=DEFAULT_TIMEOUT):
    start = time.time()
    resource = client.reload(resource)
    while not check_function(resource):
        if time.time() - start > timeout:
            exceptionMsg = 'Timeout waiting for ' + resource.kind + \
                ' to satisfy condition: ' + \
                inspect.getsource(check_function)
            if (fail_handler):
                exceptionMsg = exceptionMsg + fail_handler(resource)
            raise Exception(exceptionMsg)

        time.sleep(.5)
        resource = client.reload(resource)

    return resource


def assert_fields(obj, fields):
    assert obj is not None
    for k, v in fields.items():
        assert k in obj
        if v is None:
            assert obj[k] is None
        elif v is NOT_NONE:
            assert obj[k] is not None
        else:
            assert obj[k] == v


def assert_removed_fields(obj):
    assert obj.removed is not None
    assert obj.removeTime is not None

    assert obj.removeTimeTS > obj.removedTS


def assert_restored_fields(obj):
    assert obj.removed is None
    assert obj.removeTime is None


def now():
    return datetime.utcnow()


def format_time(time):
    return (time - timedelta(microseconds=time.microsecond)).isoformat() + 'Z'


def get_agent(admin_client, name, default_uri=DEFAULT_AGENT_URI,
              default_agent_uuid=DEFAULT_AGENT_UUID, account=None):
    name = name.upper()
    uri_name = '{0}_URI'.format(name.upper())
    uuid_name = '{0}_AGENT_UUID'.format(name.upper())

    uri = os.getenv(uri_name, default_uri)
    uuid = os.getenv(uuid_name, default_agent_uuid)

    data = {}
    if account is not None:
        account_id = get_plain_id(admin_client, account)
        data['agentResourcesAccountId'] = account_id

    agent = create_type_by_uuid(admin_client, 'agent', uuid, validate=False,
                                uri=uri, data=data)

    if account is not None:
        assert agent.data.agentResourcesAccountId == account_id

    while len(agent.hosts()) == 0:
        time.sleep(SLEEP_DELAY)

    return agent


def kind_context(admin_client, kind, external_pool=False,
                 uri=DEFAULT_AGENT_URI,
                 uuid=DEFAULT_AGENT_UUID,
                 host_public=False,
                 agent=None,
                 account=None):
    if agent is None:
        kind_agent = get_agent(admin_client, kind, default_agent_uuid=uuid,
                               default_uri=uri, account=account)
    else:
        kind_agent = agent

    hosts = filter(lambda x: x.kind == kind and x.removed is None,
                   kind_agent.hosts())
    assert len(hosts) == 1
    kind_host = activate_resource(admin_client, hosts[0])

    if kind_host.isPublic != host_public:
        kind_host = admin_client.update(kind_host, isPublic=host_public)

    assert kind_host.isPublic == host_public
    assert kind_host.accountId == kind_agent.accountId or \
        get_plain_id(admin_client, kind_host.account()) == \
        str(kind_agent.data.agentResourcesAccountId)

    pools = kind_host.storagePools()
    assert len(pools) == 1
    kind_pool = activate_resource(admin_client, pools[0])

    assert kind_pool.accountId == kind_agent.accountId or \
        get_plain_id(admin_client, kind_pool.account()) == \
        str(kind_agent.data.agentResourcesAccountId)

    context = {
        'host': kind_host,
        'pool': kind_pool,
        'agent': kind_agent
    }

    if external_pool:
        pools = admin_client.list_storagePool(kind=kind, external=True)
        assert len(pools) == 1
        context['external_pool'] = activate_resource(admin_client, pools[0])

        assert pools[0].accountId is not None

    return context


def assert_required_fields(method, **kw):
    method(**kw)

    for k in kw.keys():
        args = dict(kw)
        del args[k]

        try:
            method(**args)
            # This is supposed to fail
            assert k == ''
        except cattle.ApiError as e:
            assert e.error.code == 'MissingRequired'
            assert e.error.fieldName == k


def get_plain_id(admin_client, obj):
    ret = admin_client.list(obj.type, uuid=obj.uuid, _plainId='true')
    assert len(ret) == 1
    return ret[0].id


def get_by_plain_id(super_client, type, id):
    obj = super_client.by_id(type, id, _plainId='true')
    if obj is None:
        return None
    objs = super_client.list(type, uuid=obj.uuid)
    if len(objs) == 0:
        return None
    return objs[0]


def create_and_activate(client, type, **kw):
    obj = client.create(type, **kw)
    obj = client.wait_success(obj)

    if obj.state == 'inactive':
        obj = client.wait_success(obj.activate())

    assert obj.state == 'active'
    return obj


def delete_sim_instances(admin_client):
    to_delete = []
    to_delete.extend(admin_client.list_instance(state='running', limit=1000))
    to_delete.extend(admin_client.list_instance(state='starting', limit=1000))
    to_delete.extend(admin_client.list_instance(state='stopped', limit=1000))

    for c in to_delete:
        hosts = c.hosts()
        if len(hosts) and hosts[0].kind == 'sim':
            nsps = c.networkServiceProviders()
            if len(nsps) > 0 and nsps[0].uuid == 'nsp-test-nsp':
                continue

            try:
                admin_client.delete(c)
            except:
                pass

    for state in ['active', 'reconnecting']:
        for a in admin_client.list_agent(state=state, include='instances',
                                         uri_like='delegate%'):
            if not callable(a.instances):
                for i in a.instances:
                    if i.state != 'running' and len(i.hosts()) > 0 and \
                            i.hosts()[0].agent().uri.startswith('sim://'):
                        a.deactivate()
                        break


def one(method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == 1
    return ret[0]


def process_instances(admin_client, obj, id=None, type=None):
    if id is None:
        id = get_plain_id(admin_client, obj)

    if type is None:
        type = obj.type

    return admin_client.list_process_instance(resourceType=type, resourceId=id,
                                              sort='startTime')


def auth_check(schema, id, access, props=None):
    type = schema.types[id]
    access_actual = set()

    try:
        if 'GET' in type.collectionMethods:
            access_actual.add('r')
    except AttributeError:
        pass

    try:
        if 'GET' in type.resourceMethods:
            access_actual.add('r')
    except AttributeError:
        pass

    try:
        if 'POST' in type.collectionMethods:
            access_actual.add('c')
    except AttributeError:
        pass

    try:
        if 'DELETE' in type.resourceMethods:
            access_actual.add('d')
    except AttributeError:
        pass

    try:
        if 'PUT' in type.resourceMethods:
            access_actual.add('u')
    except AttributeError:
        pass

    assert access_actual == set(access)

    if props is None:
        return 1

    for i in ['name', 'description']:
        if i not in props and i in type.resourceFields:
            acl = set('r')
            if 'c' in access_actual:
                acl.add('c')
            if 'u' in access_actual:
                acl.add('u')
            props[i] = ''.join(acl)

    for i in ['created', 'removed', 'transitioning', 'transitioningProgress',
              'removeTime', 'transitioningMessage', 'id', 'uuid', 'kind',
              'state']:
        if i not in props and i in type.resourceFields:
            props[i] = 'r'

    prop = set(props.keys())
    prop_actual = set(type.resourceFields.keys())

    assert prop_actual == prop

    for name, field in type.resourceFields.items():
        assert name in props

        prop = set(props[name])
        prop_actual = set('r')

        prop.add(name)
        prop_actual.add(name)

        if field.create and 'c' in access_actual:
            prop_actual.add('c')
        if field.update and 'u' in access_actual:
            prop_actual.add('u')

        assert prop_actual == prop

    return 1


def resource_action_check(schema, id, actions):
    action_keys = set(actions)
    keys_received = set(schema.types[id].resourceActions.keys())
    assert keys_received == action_keys


def wait_for(callback, timeout=DEFAULT_TIMEOUT):
    start = time.time()
    ret = callback()
    while ret is None or ret is False:
        time.sleep(.5)
        if time.time() - start > timeout:
            raise Exception('Timeout waiting for condition')
        ret = callback()
    return ret


def find_one(method, *args, **kw):
    return find_count(1, method, *args, **kw)[0]


def find_count(count, method, *args, **kw):
    ret = method(*args, **kw)
    assert len(ret) == count
    return ret


def create_sim_container(admin_client, sim_context, *args, **kw):
    c = admin_client.create_container(*args,
                                      imageUuid=sim_context['imageUuid'],
                                      **kw)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    return c


def create_agent_instance_nsp(admin_client, sim_context):
    accountId = sim_context['host'].accountId
    network = create_and_activate(admin_client, 'hostOnlyNetwork',
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True,
                                  accountId=accountId)

    create_and_activate(admin_client, 'subnet',
                        networkAddress='192.168.0.0',
                        networkId=network.id,
                        accountId=accountId)

    return create_and_activate(admin_client, 'agentInstanceProvider',
                               networkId=network.id,
                               agentInstanceImageUuid=sim_context['imageUuid'],
                               accountId=accountId)


@pytest.fixture(scope='session')
def test_network(super_client, sim_context):
    network = create_type_by_uuid(super_client, 'hostOnlyNetwork',
                                  'nsp-test-network',
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True)

    create_type_by_uuid(super_client, 'subnet',
                        'nsp-test-subnet',
                        networkAddress='192.168.0.0',
                        networkId=network.id)

    nsp = create_type_by_uuid(super_client, 'agentInstanceProvider',
                              'nsp-test-nsp',
                              networkId=network.id,
                              agentInstanceImageUuid='sim:test-nsp')

    create_type_by_uuid(super_client, 'portService',
                        'nsp-test-port-service',
                        networkId=network.id,
                        networkServiceProviderId=nsp.id)

    for i in nsp.instances():
        i = super_client.wait_success(i)
        if i.state != 'running':
            super_client.wait_success(i.start())

        agent = super_client.wait_success(i.agent())
        if agent.state != 'active':
            super_client.wait_success(agent.activate())

    return network


def resource_pool_items(admin_client, obj, type=None, qualifier=None):
    id = get_plain_id(admin_client, obj)

    if type is None:
        type = obj.type

    if qualifier is None:
        return admin_client.list_resource_pool(ownerType=type,
                                               ownerId=id)
    else:
        return admin_client.list_resource_pool(ownerType=type,
                                               ownerId=id,
                                               qualifier=qualifier)


@pytest.fixture(scope='session')
def network(super_client):
    network = create_type_by_uuid(super_client, 'network', 'test_vm_network',
                                  isPublic=True)

    subnet = create_type_by_uuid(super_client, 'subnet', 'test_vm_subnet',
                                 isPublic=True,
                                 networkId=network.id,
                                 networkAddress='192.168.0.0',
                                 cidrSize=24)

    vnet = create_type_by_uuid(super_client, 'vnet', 'test_vm_vnet',
                               networkId=network.id,
                               uri='fake://')

    create_type_by_uuid(super_client, 'subnetVnetMap', 'test_vm_vnet_map',
                        subnetId=subnet.id,
                        vnetId=vnet.id)

    return network


@pytest.fixture(scope='session')
def subnet(admin_client, network):
    subnets = network.subnets()
    assert len(subnets) == 1
    return subnets[0]


@pytest.fixture(scope='session')
def vnet(admin_client, subnet):
    vnets = subnet.vnets()
    assert len(vnets) == 1
    return vnets[0]


def wait_setting_active(api_client, setting, timeout=45):
    start = time.time()
    setting = api_client.by_id('setting', setting.id)
    while setting.value != setting.activeValue:
        time.sleep(.5)
        setting = api_client.by_id('setting', setting.id)
        if time.time() - start > timeout:
            msg = 'Timeout waiting for [{0}] to be done'.format(setting)
            raise Exception(msg)

    return setting
