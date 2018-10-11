package carpet.commands;

import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class TestCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("test").
                then(argument("first",word()).
                        executes( (c)-> test(c, getString(c, "first")+" 1"))).
                then(argument("second", word()).
                        executes( (c)-> test(c, getString(c, "second")+" 2"))));
    }

    private static int test(CommandContext<CommandSource> c, String term)
    {
        Messenger.m(c.getSource(),"w term is: ",term.substring(0,1)+"b "+term);
        return 1;
    }
}
