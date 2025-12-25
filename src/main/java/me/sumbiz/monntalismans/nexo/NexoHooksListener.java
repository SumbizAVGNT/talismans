package me.sumbiz.monntalismans.nexo;

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent;
import me.sumbiz.monntalismans.MonnTalismansPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class NexoHooksListener implements Listener {

    private final MonnTalismansPlugin plugin;

    public NexoHooksListener(MonnTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMechanicsRegistered(NexoMechanicsRegisteredEvent e) {
        // Nexo перерегистрировал механики (обычно после /nexo reload)
        plugin.registerOurMechanics();
    }

    @EventHandler
    public void onItemsLoaded(NexoItemsLoadedEvent e) {
        // Nexo заново загрузил предметы -> factories пересобраны
        plugin.onNexoItemsLoaded();
    }
}
