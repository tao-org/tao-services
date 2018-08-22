/*
 * Copyright (C) 2017 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services.config;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Properties;

public class TaoConfigurationFileProcessor implements ConfigurationFileProcessor {

    @Override
    public String getConfigFileName() { return "tao.properties"; }

    @Override
    public String getConfigFileResourceLocation() { return "/ro/cs/tao/configuration/tao.properties"; }

    @Override
    public void performAdditionalConfiguration(Path configDirectory, Properties properties) {
        try {
            Field field = ConfigurationManager.class.getDeclaredField("configFolder");
            field.setAccessible(true);
            field.set(null, configDirectory);
            field = ConfigurationManager.class.getDeclaredField("settings");
            field.setAccessible(true);
            ((Properties) field.get(ConfigurationManager.getInstance())).putAll(properties);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
