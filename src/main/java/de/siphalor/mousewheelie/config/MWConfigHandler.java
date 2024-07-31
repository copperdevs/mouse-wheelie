package de.siphalor.mousewheelie.config;

import de.siphalor.mousewheelie.config.MWConfig;
import de.siphalor.mousewheelie.client.MWClient;
import de.siphalor.mousewheelie.client.inventory.SlotRefiller;
import de.siphalor.mousewheelie.client.network.InteractionManager;
import de.siphalor.mousewheelie.client.util.CreativeSearchOrder;

public class MWConfigHandler {
    private static MWConfig CONFIG;
    private static boolean created;

    public static MWConfig getConfig() {
        return CONFIG;
    }

    public void load() {
        if (created)
            return;

        CONFIG = MWConfig.createAndLoad();
        created = true;

        CONFIG.general.subscribeToInteractionRate(this::onReloadInteractionRate);
        CONFIG.general.subscribeToIntegratedInteractionRate(this::onReloadIntegratedInteractionRate);
        CONFIG.sort.subscribeToOptimizeCreativeSearchSort(this::onReloadOptimizeCreativeSearchSort);
        CONFIG.refill.subscribeToMaxRefillsMillis(this::onMaxRefillsMillis);
    }

    private void onReloadInteractionRate(int value) {
        if (!MWClient.isOnLocalServer()) {
            InteractionManager.setTickRate(CONFIG.general.interactionRate());
        }
    }

    private void onReloadIntegratedInteractionRate(int value) {
        if (!MWClient.isOnLocalServer()) {
            InteractionManager.setTickRate(CONFIG.general.integratedInteractionRate());
        }
    }

    private void onReloadOptimizeCreativeSearchSort(boolean value) {
        CreativeSearchOrder.refreshItemSearchPositionLookup();
    }

    private void onMaxRefillsMillis(int value) {
        SlotRefiller.ChangeMaxRefillTime(value);
    }
}
