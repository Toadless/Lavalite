package uk.toadl3ss.lavalite.events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import uk.toadl3ss.lavalite.commandmeta.CommandManager;
import uk.toadl3ss.lavalite.data.Constants;

public class EventListenerLite extends ListenerAdapter {
    // ################################################################################
    // ##                     Message Received Event
    // ################################################################################
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ");
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            return;
        }
        if (event.getMessage().getContentRaw().startsWith(Constants.GUILD_PREFIX)) {
            CommandManager.executeCommand(args, event, Constants.GUILD_PREFIX);
            return;
        }
        // If the prefix is @<bot> <command>.
        String mention = "<@!" + event.getJDA().getSelfUser().getId() + ">";
        if (event.getMessage().getContentRaw().startsWith(mention)) {
            CommandManager.executeCommand(args, event, mention);
            return;
        }
        return;
    }
}