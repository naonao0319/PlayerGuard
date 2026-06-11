package net.nekozouneko.playerguard.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.playerguard.ConfirmCommand;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DisclaimCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PGMessages.error("このコマンドはプレイヤーのみ実行できます。"));
            return true;
        }
        Player p = (Player) sender;
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();

        RegionManager rm;
        ProtectedRegion pr;

        if (args.length == 0) {
            rm = rc.get(BukkitAdapter.adapt(p.getWorld()));

            pr = PGUtil.getCurrentPositionRegion(p);
        }
        else {
            String id = args[0];

            rm = null;
            pr = null;

            for (RegionManager rem : rc.getLoaded()) {
                if (rem.getRegion(id) != null) {
                    rm = rem;
                    pr = rem.getRegion(id);
                    break;
                }
            }
        }

        if (pr == null || !pr.getOwners().contains(p.getUniqueId())) {
            sender.sendMessage(PGMessages.error("ここには削除できる保護領域がありません。"));
            return true;
        }

        if (!RegionRoles.isPrimaryOwner(pr, p.getUniqueId())) {
            if (!(RegionRoles.roleOf(pr, p.getUniqueId()) == RegionRoles.Role.SUB_OWNER && PGConfig.allowSubownerDisclaim())) {
                sender.sendMessage(PGMessages.error(
                        (PGConfig.allowSubownerDisclaim()
                        ? "領域を削除できるのは主オーナーまたはsubownerのみです。"
                        : "領域を削除できるのは主オーナーのみです。")
                ));
                return true;
            }
        }

        final RegionManager manager = rm;
        final ProtectedRegion region = pr;
        ConfirmCommand.addConfirm(p.getUniqueId(), () ->
            PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
                if (PlayerGuard.getInstance().getVisitorLogService() != null)
                    PlayerGuard.getInstance().getVisitorLogService().clearByRegionId(region.getId());
                manager.removeRegion(region.getId());

                sender.sendMessage(PGMessages.success("保護領域 %s を削除しました。", PGMessages.highlight(region.getId())));
            })
        );
        sender.sendMessage(PGMessages.warn("削除を確定するには %s を実行してください。", PGMessages.highlight("/pg confirm")));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
