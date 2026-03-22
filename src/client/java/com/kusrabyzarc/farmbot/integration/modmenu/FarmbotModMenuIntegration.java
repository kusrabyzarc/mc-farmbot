package com.kusrabyzarc.farmbot.integration.modmenu;

import com.kusrabyzarc.farmbot.FarmbotConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class FarmbotModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return FarmbotConfigScreen::new;
	}
}
