package me.lukiiy.wildTag

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.Collections


object Cmd {
    private fun tag(): WildTag = WildTag.getInstance()
    private val consoleUse = SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("This command can only be used by in-game players").color(NamedTextColor.RED)))
    private val worldNoMatch = SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("There isn't a match in this world").color(NamedTextColor.RED)))

    private val main = Commands.literal("wildtag")
        .requires { it.sender.hasPermission("wildtag.cmd") }
        .executes {
            it.source.sender.sendMessage(Component.text("Available subcommands: start, reload, stop, timer").color(NamedTextColor.GRAY))
            Command.SINGLE_SUCCESS
        }

    private val reload = Commands.literal("reload")
        .executes {
            tag().setupConfig()
            it.source.sender.sendMessage(Component.text("WildTag Reload complete!").color(NamedTextColor.GREEN))

            Command.SINGLE_SUCCESS
        }

    private val start = Commands.literal("start")
        .then(Commands.argument("hunters", ArgumentTypes.player())
            .executes {
                val hunters = it.getArgument("hunters", PlayerSelectorArgumentResolver::class.java).resolve(it.source).stream().toList()
                handleStart(it, hunters, null, null)
            }
            .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                .executes {
                    val hunters = it.getArgument("hunters", PlayerSelectorArgumentResolver::class.java).resolve(it.source).stream().toList()
                    val seconds = IntegerArgumentType.getInteger(it, "seconds")
                    handleStart(it, hunters, seconds, null)
                }
                .then(Commands.argument("center", ArgumentTypes.blockPosition())
                    .executes {
                        val hunters = it.getArgument("hunters", PlayerSelectorArgumentResolver::class.java).resolve(it.source).stream().toList()
                        val seconds = IntegerArgumentType.getInteger(it, "seconds")
                        val center = it.getArgument("center", BlockPositionResolver::class.java).resolve(it.source)
                        handleStart(it, hunters, seconds, center)
                    }
                )
            )
        )
        .executes { handleStart(it, null, null, null) }

    private val stop = Commands.literal("stop")
        .then(Commands.argument("world", ArgumentTypes.world())
            .executes {
                val world = it.getArgument("world", World::class.java)
                val match = tag().getMatch(world) ?: throw worldNoMatch.create()

                match.end()
                it.source.sender.sendMessage(Component.text("Ending ${world.name}'s tag match").color(NamedTextColor.RED))
                Command.SINGLE_SUCCESS
            }
        )
        .executes {
            tag().endAll()
            it.source.sender.sendMessage(Component.text("Ending all active matches.").color(NamedTextColor.RED))
            Command.SINGLE_SUCCESS
        }

    private fun handleStart(ctx: CommandContext<CommandSourceStack>, hunters: List<Player>?, seconds: Int?, center: BlockPosition?): Int {
        val sender = ctx.source.sender as? Player ?: throw consoleUse.create()
        val world = sender.world

        if (world.players.size < 2) throw SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("Not enough players to start a tag match").color(NamedTextColor.RED))).create()
        val finalTime = seconds ?: tag().config.getInt("defaultTimer", 120)
        val finalHunters = hunters ?: Collections.emptyList()

        try {
            Match(world.players, world, center?.toLocation(world), tag().config.getInt("mapArea", 128).toDouble(), finalTime.toLong(), finalHunters)
        } catch (e: IllegalArgumentException) {
            throw SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("Could not start a match: ${e.message}").color(NamedTextColor.RED))).create()
        }

        sender.sendMessage(Component.text("Starting a new match on ${world.name}").color(NamedTextColor.YELLOW))
        return Command.SINGLE_SUCCESS
    }

    private val timer = Commands.literal("timer")
        .then(Commands.argument("operation", StringArgumentType.word())
            .suggests { _, builder -> builder.suggest("increment").suggest("decrement").buildFuture() }
            .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                .executes {
                    val sender = it.source.sender as? Player ?: throw consoleUse.create()
                    val match: Match = tag().getMatch(sender) ?: throw worldNoMatch.create()

                    val add = StringArgumentType.getString(it, "operation") == "increment"
                    val seconds = IntegerArgumentType.getInteger(it, "seconds").toLong()

                    match.timer += seconds
                    sender.sendMessage(Component.text("${if (add) "Incremented" else "Decremented"} $seconds ${if (add) "to" else "from"} the match's timer.").color(NamedTextColor.YELLOW))
                    Command.SINGLE_SUCCESS
                }
            )
        )

    fun register(): LiteralCommandNode<CommandSourceStack> = main.then(reload).then(start).then(stop).then(timer).build()
}