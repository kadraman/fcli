package com.fortify.cli.aviator._common.config.admin.cli.cmd;

import com.fortify.cli.aviator._common.config.admin.cli.mixin.AviatorAdminConfigCreateOptions;
import com.fortify.cli.aviator._common.config.admin.cli.mixin.AviatorAdminConfigNameArgGroup;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Create.CMD_NAME, sortOptions = false)
public class AviatorAdminConfigCreateCommand extends AbstractSessionLoginCommand<AviatorAdminConfigDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Create outputHelper;
    @Getter private AviatorAdminConfigHelper configHelper = AviatorAdminConfigHelper.instance();
    @Mixin private AviatorAdminConfigCreateOptions configCreateOptions = new AviatorAdminConfigCreateOptions();
    @Getter @ArgGroup(headingKey = "aviator.admin-config.name.arggroup")
    private AviatorAdminConfigNameArgGroup configNameSupplier;

    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
        return configNameSupplier;
    }

    @Override
    protected void logoutBeforeNewLogin(String configName, AviatorAdminConfigDescriptor configDescriptor) {
    }

    @Override
    protected AviatorAdminConfigDescriptor login(String configName) {
        String privateKeyContents = configCreateOptions.getPrivateKeyResolver().getPrivateKeyContents();
        return new AviatorAdminConfigDescriptor(configCreateOptions.getAviatorUrl(), configCreateOptions.getTenant(), privateKeyContents);
    }

    @Override
    protected AviatorAdminConfigHelper getSessionHelper() {
        return configHelper;
    }
}
