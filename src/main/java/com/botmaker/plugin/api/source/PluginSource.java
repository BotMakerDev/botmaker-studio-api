package com.botmaker.plugin.api.source;

/**
 * A Java file a plugin gives a bot to hold the plugin's own values.
 *
 * <p>The host copies it into {@code src/main/java/<bot package>/plugins/<last id segment>/} the first time
 * the plugin is added to a project, substitutes {@link #PACKAGE} for the package that lands at, and never
 * touches the class again: not to add a method, not to delete one, not to reformat it. What the host does
 * afterwards is rewrite the expression a {@code @Managed} method returns, one node at a time.
 *
 * <h2>Why the plugin writes the file and the host does not</h2>
 *
 * <p>Four earlier designs had the host generate this compilation unit, and each one had to answer the same
 * list: which package, which class name, which imports, which order, and how to read the whole thing back.
 * Handing the file over as text answers all five at once — the plugin's author already knows them — and
 * shrinks what the host must parse from <em>a class</em> to <em>one expression</em>, which is the domain
 * {@code ValueCatalog.valueOf} already covers.
 *
 * <p>It also gives the bot something that works before any editor has opened: the defaults the plugin wrote
 * compile and run, so a project that adds a plugin and draws nothing still builds. That is the same promise
 * a fresh {@code @Param} field makes.
 *
 * <h2>What belongs in it</h2>
 *
 * <p>An {@code install()} the bot's {@code main} calls, one {@code @Managed} method per value with a working
 * default, and whatever javadoc tells the reader which window keeps each one. Nothing here is locked to the
 * user: it is their file from the moment it lands, and a developer with no BotMaker installed edits it like
 * any other. Only the canvas refuses a {@code @Managed} body, because another window owns it.
 *
 * <p><b>Text, not a template engine.</b> {@link #PACKAGE} is the only substitution there is and the only one
 * there will be. A placeholder the host expands is a grammar, and a grammar on this interface would be the
 * host learning the plugin's vocabulary through the back door.
 *
 * @param simpleName the class's name with no package and no extension — {@code Sdk}, never {@code Sdk.java}
 * @param source     the file's whole text, with {@link #PACKAGE} where its package declaration goes
 */
public record PluginSource(String simpleName, String source) {

    /** What the host replaces with the package the file lands in. Write it in the {@code package} line. */
    public static final String PACKAGE = "${package}";

    public PluginSource {
        simpleName = simpleName == null ? "" : simpleName.strip();
        source = source == null ? "" : source;
    }

    /** Whether this is worth copying: a class name and something to put in the file. */
    public boolean isPresent() {
        return !simpleName.isEmpty() && !source.isBlank();
    }

    /** The file name the host writes — {@code Sdk.java}. */
    public String fileName() {
        return simpleName + ".java";
    }

    /** {@link #source} with {@link #PACKAGE} resolved to {@code packageName}. */
    public String sourceIn(String packageName) {
        return source.replace(PACKAGE, packageName == null ? "" : packageName);
    }
}
