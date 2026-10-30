package com.zabyt.lib;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.zabyt.lib.screen.ZabytLibConfigScreen;

public class ZabytLibModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ZabytLibConfigScreen::new;
    }
}
