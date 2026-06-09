package net.nekozouneko.playerguard.command.sub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubCommandManagerTest {

    static class DummySub extends SubCommand {
        @Override public boolean execute(CommandSender s, Command c, String l, List<String> a) { return true; }
        @Override public List<String> tabComplete(CommandSender s, Command c, String l, List<String> a) { return null; }
        @Override public String getPermission() { return "x"; }
    }

    @Test
    void resolvesByPrimaryName() {
        SubCommandManager m = new SubCommandManager();
        SubCommand sub = new DummySub();
        m.register("info", sub);
        assertSame(sub, m.getCommand("info"));
    }

    @Test
    void resolvesBySingleAlias() {
        SubCommandManager m = new SubCommandManager();
        SubCommand sub = new DummySub();
        m.register("info", sub, "i");
        assertSame(sub, m.getCommand("i"), "エイリアス 'i' で同じコマンドが引けること");
    }

    @Test
    void resolvesByMultipleAliases() {
        SubCommandManager m = new SubCommandManager();
        SubCommand sub = new DummySub();
        m.register("remove", sub, "rm", "del");
        assertSame(sub, m.getCommand("rm"));
        assertSame(sub, m.getCommand("del"), "複数エイリアスが全て有効であること");
    }

    @Test
    void unknownReturnsNull() {
        SubCommandManager m = new SubCommandManager();
        assertNull(m.getCommand("nope"));
    }

    @Test
    void namesAndAliasesIncludesBoth() {
        SubCommandManager m = new SubCommandManager();
        m.register("remove", new DummySub(), "rm", "del");
        assertTrue(m.getCommandNamesAndAliases().containsAll(List.of("remove", "rm", "del")));
    }

    @Test
    void rejectsAliasCollidingWithExistingAlias() {
        SubCommandManager m = new SubCommandManager();
        m.register("remove", new DummySub(), "rm");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> m.register("delete", new DummySub(), "rm"));
    }
}
