package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

/**
 * Created by wizardofmath on 7/9/15.
 */
public class SettingsUtils {

    @Inject
    ObjectManager objectManager;

    public void changeSetting(String name, Object value) {
        if (name == null) {
            return;
        }
        Setting setting = objectManager.findOne(Setting.class, "name", name);
        if (value == null) {
            if (setting != null) {
                objectManager.delete(setting);
            } else {
                return;
            }
        } else {
            if (null == setting) {
                objectManager.create(Setting.class, "name", name, "value", value);
            } else {
                objectManager.setFields(setting, "value", value);
            }
        }
        DeferredUtils.defer(new Runnable() {

            @Override
            public void run() {
                ArchaiusUtil.refresh();
            }
        });
    }
}
