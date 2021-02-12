/*
 *  Copyright (c) 2021 Toadless @ toaddev.net
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package net.toaddev.snowball.command.admin;

import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.toaddev.snowball.entities.command.CommandFlag;
import net.toaddev.snowball.entities.command.Command;
import net.toaddev.snowball.main.BotController;
import net.toaddev.snowball.util.DiscordUtil;
import org.jetbrains.annotations.NotNull;
import net.toaddev.snowball.entities.command.CommandContext;

import java.util.concurrent.TimeUnit;

@net.toaddev.snowball.annotation.Command
public class ExitCommand extends Command
{
    public ExitCommand()
    {
        super("exit", null);
        addFlags(CommandFlag.DEVELOPER_ONLY);
        addAlias("end", "shutdown");
    }

    @Override
    public void run(@NotNull CommandContext ctx)
    {
        ctx.getChannel().sendMessage("This will **shut down the whole bot**.")
                .queue(message -> {
                    message.addReaction("\u2705").queue();
                    message.addReaction("\u274c").queue();

                    BotController.getEventWaiter().waitForEvent(
                            GuildMessageReactionAddEvent.class,
                            (e) -> e.getMessageIdLong() == message.getIdLong() && !e.getUser().isBot() && DiscordUtil.isOwner(e.getUser()),
                            (e) ->
                            {
                                if (e.getReaction().toString().contains("U+274c"))
                                {
                                    message.delete().queue();
                                }
                                else if (e.getReaction().toString().contains("U+2705"))
                                {
                                    message.clearReactions().queue();
                                    message.editMessage("Shutting down").queue();
                                    BotController.shutdown(0);
                                }
                            },
                            5L, TimeUnit.SECONDS,
                            () -> ctx.getChannel().sendMessage("You took too long.").queue()
                    );
                });
    }
}