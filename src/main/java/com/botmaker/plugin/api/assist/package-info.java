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
 * {@link com.botmaker.plugin.api.assist.AgentReply}: text, an image, or a refusal.
 *
 * <p><b>What a tool may change is what the plugin already may.</b> It writes the plugin's own values through
 * {@code services().pluginValues()}, which the host gates exactly as it gates the plugin's own windows. It is
 * given no way to write Java, click or type: the assistant acts on the bot's target only by running the bot.
 *
 * <p>{@link com.botmaker.plugin.api.assist.TrialEntry} is the other half: the static method Studio's ▶ Try
 * calls with one statement as its body, after the setup a run does.
 */
package com.botmaker.plugin.api.assist;
