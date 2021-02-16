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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.toaddev.snowball.objects.command.Command;
import net.toaddev.snowball.objects.command.CommandContext;
import net.toaddev.snowball.objects.command.CommandFlag;
import net.toaddev.snowball.objects.exception.CommandException;
import net.toaddev.snowball.util.DiscordUtil;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@net.toaddev.snowball.annotation.Command
public class EvalCommand extends Command
{
    private final ScriptEngine SCRIPT_ENGINE;
    private final String imports;

    private final ExecutorService EVAL_EXECUTOR;

    public EvalCommand()
    {
        super("eval", null, List.of("code"));
        addFlags(CommandFlag.DEVELOPER_ONLY);
        addSelfPermissions(Permission.MESSAGE_EMBED_LINKS);
        this.EVAL_EXECUTOR = Executors.newFixedThreadPool(4);
        this.SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("groovy");
        this.imports = "import java.io.*\n" +
                "import java.lang.*\n" +
                "import java.util.*\n" +
                "import java.util.concurrent.*\n" +
                "import net.dv8tion.jda.core.*\n" +
                "import net.dv8tion.jda.core.entities.*\n" +
                "import net.dv8tion.jda.core.entities.impl.*\n" +
                "import net.dv8tion.jda.core.managers.*\n" +
                "import net.dv8tion.jda.core.managers.impl.*\n" +
                "import net.dv8tion.jda.api.*;" +
                "import net.dv8tion.jda.api.entities.Activity;;" +
                "import net.dv8tion.jda.core.utils.*\n";
    }

    @Override
    public void run(@NotNull CommandContext ctx, @NotNull Consumer<CommandException> failure)
    {
        String messageArgs = ctx.getMessage().getContentRaw().replaceFirst("^" + ctx.getPrefix() + "eval" + " ", "");
        try
        {
            EVAL_EXECUTOR.submit(() ->
            {
                Object out;
                String status = "Success";

                SCRIPT_ENGINE.put("args", messageArgs);
                SCRIPT_ENGINE.put("event", ctx.getEvent());
                SCRIPT_ENGINE.put("message", ctx.getMessage());
                SCRIPT_ENGINE.put("channel", ctx.getChannel());
                SCRIPT_ENGINE.put("jda", ctx.getJDA());
                SCRIPT_ENGINE.put("guild", ctx.getGuild());
                SCRIPT_ENGINE.put("member", ctx.getMember());

                String script = imports + messageArgs;

                long start = System.currentTimeMillis();

                try
                {
                    out = SCRIPT_ENGINE.eval(script);
                } catch (Exception exception)
                {
                    out = exception.getMessage();
                    status = "Failed";
                }

                ctx.getChannel().sendMessage(new EmbedBuilder()
                        .setTitle("Evaluated Result")
                        .addField("Status:", status, true)
                        .addField("Duration:", (System.currentTimeMillis() - start) + "ms", true)
                        .addField("Code:", "```java\n" + ctx.getMessage().getContentRaw().split("\\s+", 2)[1] + "\n```", false)
                        .addField("Result:", out == null ? "No result." : out.toString(), false)
                        .setColor(DiscordUtil.getEmbedColor())
                        .build())
                        .queue();
            });
        } catch (Exception e)
        {
            ctx.getChannel().sendMessage(e.getMessage()).queue();
        }
    }
}