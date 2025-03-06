package dev.anderle.attributemod.utils;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;

public class ChatUtils {
    /**
     * Get a nicely formatted error message that can be printed to the chat.
     * @param reportPlease Whether the mod should ask the user to report this error.
     */
    public static ChatComponentText errorMessage(String message, boolean reportPlease) {
        ChatComponentText text = new ChatComponentText(
                Constants.prefix + EnumChatFormatting.RED +
                "An error occured trying to do this:\n" + EnumChatFormatting.GRAY + message
        );
        if(reportPlease) {
            text.appendSibling(new ChatComponentText(
                    EnumChatFormatting.RED +
                    " If you have a free minute, please report this in "))
                .appendSibling(chatLink("Kuudra Gang", "https://discord.gg/kuudra", EnumChatFormatting.RED)
                    .appendSibling(new ChatComponentText(EnumChatFormatting.RED + ".")));
        }
        return text;
    }

    /**
     * Convert a url string to a nicely formatted chat component.
     */
    public static IChatComponent chatLink(String name, String url, EnumChatFormatting color) {
        return new ChatComponentText(name)
            .setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.OPEN_URL, url
            )).setUnderlined(true).setColor(color));
    }

    /**
     * Sends a new message but also deletes the last mod response if one was found within the last 20 messages.
     * @param chat The chat instance of the game.
     * @param comp The chat component to resend.
     */
    public static void resendChatMessage(GuiNewChat chat, IChatComponent comp) {
        try {
            // check both "field_146252_h" and "chatLines" (with or without mappings)
            Field chatLinesField = FieldUtils.getField(chat.getClass(), "field_146252_h", true);
            if(chatLinesField == null) chatLinesField = FieldUtils.getField(chat.getClass(), "chatLines", true);

            @SuppressWarnings("unchecked")
            List<ChatLine> messages = (List<ChatLine>) chatLinesField.get(chat);
            for (int i = 0; i < messages.size(); i++) {
                if(messages.get(i).getChatComponent().getFormattedText().startsWith(Constants.prefix)) {
                    messages.remove(i);
                    chat.refreshChat();
                    break;
                }
                if(i > 20) break; // don't check more than that, don't wanna make it lag :p
            }
            chat.printChatMessage(comp);
        } catch(IllegalAccessException ignored) {}
    }

    /**
     *  Convert text received from API to a nice text message.
     *  - substrings that start with "%t" will be converted to normal (colored) text
     *  - substrings that start with "%i" will add a ClickEvent to run the /viewauction command for auction id
     *  - substrings that start with "%n" will be shown as tooltip on HoverEvent
     */
    public static IChatComponent decodeToFancyChatMessage(String string) {
        IChatComponent comp = new ChatComponentText(Constants.prefix);
        for(String part : string.split("#")) {
            if(part.startsWith("t")) {
                comp.appendSibling(new ChatComponentText(part.replaceFirst("t", "")));
            } else if(part.startsWith("i")) {
                comp.getSiblings().get(comp.getSiblings().size() - 1).getChatStyle()
                .setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/viewauction " + part.replaceFirst("i", "")
                ) {
                    @Override
                    public Action getAction() {
                        return Action.RUN_COMMAND;
                    }
                });
            } else if(part.startsWith("n")) {
                comp.getSiblings().get(comp.getSiblings().size() - 1).getChatStyle()
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(
                            part.replaceFirst("n", "")
                    )));
            }
        }
        return comp;
    }

    /**
     * Send a chat message to the sender that the entered number is invalid.
     */
    public static void badNumber(ICommandSender sender, int from, int to) {
        sender.addChatMessage(new ChatComponentText(
            Constants.prefix + EnumChatFormatting.RED
            + "Please enter a valid number between " + EnumChatFormatting.DARK_RED
            + from + EnumChatFormatting.RED + " and " + EnumChatFormatting.DARK_RED
            + to + EnumChatFormatting.RED + "."
        ));
    }
}
