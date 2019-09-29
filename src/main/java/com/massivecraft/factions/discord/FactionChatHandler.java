package com.massivecraft.factions.discord;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import mkremins.fanciful.FancyMessage;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.bukkit.ChatColor;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FactionChatHandler extends ListenerAdapter {
    public static JDA jda;
    private FactionsPlugin plugin;

    public FactionChatHandler(FactionsPlugin plugin) {
        this.plugin = plugin;
        startBot();
        jda.addEventListener(this);
        jda.addEventListener(new DiscordListener(plugin));
    }

    private void startBot() {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(Conf.discordBotToken).buildBlocking();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(FactionsPlugin plugin, Faction faction, UUID uuid, String username, String message) {
        String factionsChatChannelId = faction.getFactionChatChannelId();
        if (factionsChatChannelId == null || factionsChatChannelId.isEmpty()) {
            return;
        }
        if (jda == null) {
            return;
        }
        TextChannel textChannel = jda.getTextChannelById(factionsChatChannelId);
        if (textChannel == null) {
            return;
        }
        if (!textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MANAGE_WEBHOOKS)) {
            textChannel.sendMessage("Missing `Manage Webhooks` permission in this channel").queue();
            return;
        }
        Webhook webhook = (textChannel.getWebhooks().complete()).stream().filter(w -> w.getName().equals(Conf.webhookName)).findAny().orElse(null);
        WebhookClient webhookClient;
        if (webhook != null) {
            webhookClient = webhook.newClient().build();
        } else {
            webhookClient = textChannel.createWebhook(Conf.webhookName).complete().newClient().build();
        }
        WebhookMessage webhookMessage = new WebhookMessageBuilder().setUsername(ChatColor.stripColor(username)).setAvatarUrl(Conf.avatarUrl.replace("%uuid%", uuid.toString())).setContent(ChatColor.stripColor(message)).build();
        webhookClient.send(webhookMessage).join();
        webhookClient.close();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.isWebhookMessage() || event.getAuthor().isBot()) {
            return;
        }
        Faction faction = Factions.getInstance().getAllFactions().stream().filter(f -> event.getChannel().getId().equals(f.getFactionChatChannelId())).findAny().orElse(null);
        if (faction == null) {
            return;
        }
        String content = event.getMessage().getContentDisplay();
        String message = (content.length() > 500) ? content.substring(0, 500) : content;
        FancyMessage fancyMessage = new FancyMessage();
        fancyMessage.text(ChatColor.translateAlternateColorCodes('&', Conf.fromDiscordFactionChatPrefix + String.format(Conf.factionChatFormat, event.getAuthor().getAsTag(), message)));
        List<FancyMessage> messages = new ArrayList<>();
        messages.add(fancyMessage);
        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            messages.add(new FancyMessage().text(" [Attachment]").color(ChatColor.AQUA).link(attachment.getUrl()).tooltip(attachment.getFileName()));
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> messages.forEach(msg -> faction.getOnlinePlayers().forEach(fancyMessage::send)));
    }
}
