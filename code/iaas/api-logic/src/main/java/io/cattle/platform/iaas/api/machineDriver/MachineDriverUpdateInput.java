package io.cattle.platform.iaas.api.machineDriver;

public interface MachineDriverUpdateInput {

    String getUri();

    String getName();

    String getmd5checksum();
}
