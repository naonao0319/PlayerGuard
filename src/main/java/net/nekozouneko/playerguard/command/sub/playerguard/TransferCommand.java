package net.nekozouneko.playerguard.command.sub.playerguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TransferCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PGMessages.error("このコマンドはプレイヤーのみ実行できます。"));
            return true;
        }

        if (args.isEmpty()) {
            sender.sendMessage(PGMessages.warn("譲渡先のプレイヤー名を指定してください。"));
            return true;
        }

        Player player = (Player) sender;
        Player transferTo = Bukkit.getPlayer(args.get(0));
        ProtectedRegion region;

        if (args.size() < 2) {
            region = PGUtil.getCurrentPositionRegion(player);
        }
        else {
            Map.Entry<ProtectedRegion, World> result = PGUtil.findPlayerGuardRegions(args.get(1));
            if (result == null) {
                region = null;
            }
            else {
                region = result.getKey();
            }
        }

        if (region == null || !region.getOwners().contains(player.getUniqueId())) {
            sender.sendMessage(PGMessages.error("対象の保護領域が見つかりません。"));
            return true;
        }

        if (!canTransfer(region, player.getUniqueId())) {
            sender.sendMessage(PGMessages.error(
                    (PGConfig.allowSubownerTransfer()
                    ? "領域を譲渡できるのは主オーナーまたはsubownerのみです。"
                    : "領域を譲渡できるのは主オーナーのみです。")
            ));
            return true;
        }

        if (transferTo == null || transferTo.equals(player)) {
            sender.sendMessage(PGMessages.error("譲渡先プレイヤーが見つからないか、オンラインではありません。"));
            return true;
        }

        requestTransfer(sender, region, transferTo);

        return true;
    }

    /**
     * region を transferTo へ譲渡するリクエストを発行する。
     * 上限超過時は false を返す。受理は ConfirmCommand 経由。
     */
    public static boolean requestTransfer(CommandSender requester, ProtectedRegion region, Player transferTo) {
        PlayerGuard inst = PlayerGuard.getInstance();

        if (inst.getProtectionUsed(transferTo) + region.volume() > inst.getProtectLimit(transferTo)) {
            requester.sendMessage(PGMessages.error("%s にはこの領域を譲渡できません。保護上限を超えます。", PGMessages.highlight(transferTo.getName())));
            return false;
        }

        ConfirmCommand.addConfirm(transferTo.getUniqueId(), () -> {
            if (inst.getProtectionUsed(transferTo) + region.volume() > inst.getProtectLimit(transferTo)) {
                requester.sendMessage(PGMessages.error("譲渡受理時点で保護上限を超えるため、移管できませんでした。"));
                return;
            }
            region.getOwners().clear();
            region.getMembers().clear();
            region.setFlag(PGCustomFlags.RENTALS, null);
            if (PlayerGuard.getInstance().getVisitorLogService() != null)
                PlayerGuard.getInstance().getVisitorLogService().clearByRegionId(region.getId());
            region.getOwners().addPlayer(transferTo.getUniqueId());
            RegionRoles.setPrimaryOwner(region, transferTo.getUniqueId());
            transferTo.sendMessage(PGMessages.success("領域 %s の譲渡が完了しました。", PGMessages.highlight(region.getId())));
        });

        transferTo.sendMessage(PGMessages.warn(
                "%s から領域 %s の譲渡リクエストが届いています。受け取るなら %s を実行してください。",
                PGMessages.highlight(requester.getName()),
                PGMessages.highlight(region.getId()),
                PGMessages.highlight("/pg confirm")
        ));
        requester.sendMessage(PGMessages.success(
                "領域 %s を %s に譲渡申請しました。",
                PGMessages.highlight(region.getId()),
                PGMessages.highlight(transferTo.getName())
        ));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() == 1) {
            return TabCompletes.players(args.get(0), Bukkit.getOnlinePlayers());
        }
        else if (args.size() == 2) {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            Set<String> regions = new HashSet<>();
            rc.getLoaded().forEach(rm ->
                    rm.getRegions().values().stream()
                            .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                            .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                            .filter(pr -> {
                                if (sender instanceof Player) {
                                    return pr.getOwners().contains(((Player) sender).getUniqueId());
                                }
                                else return true;
                            })
                            .map(ProtectedRegion::getId)
                            .forEach(regions::add)
            );

            return TabCompletes.sorted(args.get(1), regions);
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.playerguard";
    }

    private static boolean canTransfer(ProtectedRegion region, UUID uuid) {
        if (RegionRoles.isPrimaryOwner(region, uuid)) return true;
        return RegionRoles.roleOf(region, uuid) == RegionRoles.Role.SUB_OWNER && PGConfig.allowSubownerTransfer();
    }
}
