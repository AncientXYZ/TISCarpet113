package carpet.commands;

import carpet.CarpetSettings;
import carpet.CarpetSettings.CarpetSettingEntry;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarpetCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        LiteralArgumentBuilder<CommandSource> literalargumentbuilder = Commands.literal("carpet").requires((player) ->
                player.hasPermissionLevel(2) && !CarpetSettings.locked);

        literalargumentbuilder.executes((context)->listAllSettings(context.getSource())).
                then(Commands.literal("list").
                        executes( (c) -> listSettings(c.getSource(),
                                "All CarpetMod Settings",
                                CarpetSettings.findAll(null))).
                        then(Commands.literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        "Current CarpetMod Startup Settings from carpet.conf",
                                        CarpetSettings.findStartupOverrides(c.getSource().getServer())))).
                        then(Commands.argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->ISuggestionProvider.suggest(CarpetSettings.default_tags, b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format("CarpetMod Settings matching \"%s\"", StringArgumentType.getString(c, "tag")),
                                        CarpetSettings.findAll(StringArgumentType.getString(c, "tag"))))));

        for (CarpetSettingEntry rule: CarpetSettings.getAllCarpetSettings())
        {
            literalargumentbuilder.then(Commands.literal(rule.getName()).executes( (context) ->
                    displayRuleMenu(context.getSource(),rule)));
            literalargumentbuilder.then(Commands.literal("removeDefault").
                    then(Commands.literal(rule.getName()).executes((context) ->
                            removeDefault(context.getSource(), rule))));
            literalargumentbuilder.then(Commands.literal(rule.getName()).
                    then(Commands.argument("value", StringArgumentType.word()).
                            suggests((c, b)-> ISuggestionProvider.suggest(rule.getOptions(),b)).
                            executes((context) ->
                            setRule(context.getSource(), rule, StringArgumentType.getString(context, "value")))));
            literalargumentbuilder.then(Commands.literal("setDefault").
                    then(Commands.literal(rule.getName()).
                    then(Commands.argument("value", StringArgumentType.word()).
                            suggests((c, b)-> ISuggestionProvider.suggest(rule.getOptions(),b)).
                            executes((context) ->
                            setDefault(context.getSource(), rule, StringArgumentType.getString(context, "value"))))));
        }
        dispatcher.register(literalargumentbuilder);
    }

    private static int displayRuleMenu(CommandSource source, CarpetSettingEntry rule)
    {
        EntityPlayer player;
        try
        {
            player = source.asPlayer();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w "+rule.getName() +" is set to: ","wb "+rule.getStringValue());
            return 1;
        }

        Messenger.m(player, "");
        Messenger.m(player, "wb "+rule.getName(),"!/carpet "+rule.getName(),"^g refresh");
        Messenger.m(player, "w "+rule.getToast());

        Arrays.stream(rule.getInfo()).forEach(s -> Messenger.m(player, "g  "+s));

        List<ITextComponent> tags = new ArrayList<>();
        tags.add(Messenger.c("w Tags: "));
        for (String t: rule.getTags())
        {
            tags.add(Messenger.c("c ["+t+"]", "^g list all "+t+" settings","!/carpet list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(player, tags.toArray(new Object[0]));

        Messenger.m(player, "w Current value: ",String.format("%s %s (%s value)",rule.getBoolValue()?"lb":"nb", rule.getStringValue(),rule.isDefault()?"default":"modified"));
        List<ITextComponent> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: rule.getOptions())
        {
            options.add(Messenger.c(
                    String.format("%s%s %s",(o.equals(rule.getDefault()))?"u":"", (o.equals(rule.getStringValue()))?"bl":"y", o ),
                    "^g switch to "+o,
                    String.format("?/carpet %s %s",rule.getName(),o)));
            options.add(Messenger.c("w  "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(player, options.toArray(new Object[0]));

        return 1;
    }

    private static int setRule(CommandSource source, CarpetSettingEntry rule, String newValue)
    {
        CarpetSettings.set(source, rule.getName(), newValue);
        Messenger.m(source, "w "+rule.toString()+", ", "c [change permanently?]",
                "^w Click to keep the settings in carpet.conf to save across restarts",
                "?/carpet setDefault "+rule.getName()+" "+rule.getStringValue());
        return 1;
    }
    private static int setDefault(CommandSource source, CarpetSettingEntry rule, String defaultValue)
    {
        CarpetSettings.setDefaultRule(source, rule.getName(), defaultValue);
        Messenger.m(source ,"gi rule "+ rule.getName()+" will now default to "+ defaultValue);
        return 1;
    }
    private static int removeDefault(CommandSource source, CarpetSettingEntry rule)
    {
        CarpetSettings.removeDefaultRule(source, rule.getName());
        Messenger.m(source ,"gi rule "+ rule.getName()+" defaults to Vanilla");
        return 1;
    }


    private static ITextComponent displayInteractiveSetting(CarpetSettingEntry e)
    {
        List<Object> args = new ArrayList<>();
        args.add("w - "+e.getName()+" ");
        args.add("!/carpet "+e.getName());
        args.add("^y "+e.getToast());
        for (String option: e.getOptions())
        {
            String style = e.isDefault()?"g":(option.equalsIgnoreCase(e.getDefault())?"y":"e");
            if (option.equalsIgnoreCase(e.getDefault()))
                style = style+"b";
            else if (option.equalsIgnoreCase(e.getStringValue()))
                style = style+"u";
            args.add(style+" ["+option+"]");
            if (!option.equalsIgnoreCase(e.getStringValue()))
            {
                args.add("!/carpet " + e.getName() + " " + option);
                args.add("^w switch to " + option);
            }
            args.add("w  ");
        }
        args.remove(args.size()-1);
        return Messenger.c(args.toArray(new Object[0]));
    }

    private static int listSettings(CommandSource source, String title, CarpetSettingEntry[] settings_list)
    {
        try
        {
            EntityPlayer player = source.asPlayer();
            Messenger.m(player,String.format("wb %s:",title));
            Arrays.stream(settings_list).forEach(e -> Messenger.m(player,displayInteractiveSetting(e)));

        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w s:"+title);
            Arrays.stream(settings_list).forEach(r -> Messenger.m(source, "w  - "+ r.toString()));
        }
        return 1;
    }
    private static int listAllSettings(CommandSource source)
    {
        listSettings(source, "Current CarpetMod Settings", CarpetSettings.find_nondefault(source.getServer()));

        Messenger.m(source, "Carpet Mod version: "+CarpetSettings.carpetVersion);
        try
        {
            EntityPlayer player = source.asPlayer();
            List<Object> tags = new ArrayList<>();
            tags.add("w Browse Categories:\n");
            for (String t : CarpetSettings.default_tags)
            {
                tags.add("c [" + t+"]");
                tags.add("^g list all " + t + " settings");
                tags.add("!/carpet list " + t);
                tags.add("w  ");
            }
            tags.remove(tags.size() - 1);
            Messenger.m(player, tags.toArray(new Object[0]));
        }
        catch (CommandSyntaxException e)
        {
        }
        return 1;
    }
}
