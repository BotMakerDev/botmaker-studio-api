/**
 * Tools a plugin offers the AI assistant that drives Studio, and the entry Studio calls to try one statement.
 *
 * <p>Studio serves its own tools to an assistant over MCP: read the blocks, insert one, run the bot. What only a
 * plugin can do — take a screenshot of what the bot watches, cut a picture, add an activity — the plugin offers
 * as an {@link com.botmaker.plugin.api.assist.AssistantTool}:
 *
 * <pre>{@code
 * record Crop(@Describe("the picture's name") String name, int x, int y, int width, int height) {}
 *
 * static final AssistantTool<Crop> CROP = AssistantTool.named("crop_picture")
 *         .describedAs("Cuts a picture out of the watched screen and adds it to Pictures")
 *         .takes(Crop.class)
 *         .handledBy(SdkAssist::crop);
 * }</pre>
 *
 * <p><b>The parameters are a record</b>, and its components are the tool's input schema: the assistant sees
 * their names, types and {@link com.botmaker.plugin.api.assist.Describe} sentences, and the host hands the
 * handler a record built from what the assistant sent. A component of a type the schema cannot say is refused
 * when the tool is declared, with the component named. The handler answers an
 * {@link com.botmaker.plugin.api.assist.AgentReply}: text, an image, or a refusal. A component may be a nested
 * record or a list of records, so a batch is a {@code List<Box>}, never lines of text to parse.
 *
 * <p><b>One tool for one family of actions</b>, rather than a tool per verb: {@code takesOneOf} a sealed interface
 * whose records are the actions, and a handler that {@code switch}es over them.
 *
 * <pre>{@code
 * sealed interface Edit {
 *     @Describe("Renames a screen") record Rename(String screen, String to) implements Edit {}
 *     record SetStart(String screen) implements Edit {}
 * }
 * static final AssistantTool<AssistantTool.OneOf<Edit>> EDIT_MAP = AssistantTool.named("edit_map")
 *         .describedAs("Edits the screen map").takesOneOf(Edit.class).handledBy(MapTools::edit);
 * }</pre>
 *
 * <p>The assistant sends {@code action} ({@code "rename"}, {@code "set_start"}) and that action's fields flat beside
 * it ({@link com.botmaker.plugin.api.assist.ToolSchema}); the tool refuses, in a sentence, a field the action does
 * not take and a field it needs that is missing. A component may also be a {@code List} of such a sealed interface,
 * each element one action, flat the same way: one call, several edits of different kinds.
 *
 * <p>A component whose wire key cannot be its Java name ({@code class}, {@code package}, {@code wait}) takes
 * {@link com.botmaker.plugin.api.assist.Key}: {@code @Key("wait") Integer seconds}.
 *
 * <p><b>What a tool may change is what the plugin already may.</b> It writes the plugin's own values through
 * {@code services().pluginValues()}, which the host gates exactly as it gates the plugin's own windows. It is
 * given no way to write Java, click or type: the assistant acts on the bot's target only by running the bot.
 *
 * <p>{@link com.botmaker.plugin.api.assist.TrialEntry} is the other half: the static method Studio's ▶ Try
 * calls with one statement as its body, after the setup a run does.
 */
package com.botmaker.plugin.api.assist;
