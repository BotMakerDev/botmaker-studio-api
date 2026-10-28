package com.botmaker.plugin.api;

/**
 * The class holding a palette a plugin builds by hand, named by {@link PluginDeclaration#catalog(Class)}: its one
 * {@code public static final} {@link com.botmaker.plugin.api.catalog.PaletteCatalog} field. Rarely wanted: the
 * host discovers the palette from the jar's {@code @Palette} classes. A holder with two is refused when the host
 * asks.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface CatalogHolder {
}
