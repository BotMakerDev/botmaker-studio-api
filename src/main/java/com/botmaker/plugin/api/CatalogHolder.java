package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;

/**
 * The class giving a palette a plugin builds by hand, named by {@link PluginDeclaration#catalog(Class)}. Rarely
 * wanted: the host discovers the palette from the jar's {@code @Palette} classes. See {@link TypeHolder} for how
 * a holder is built and asked.
 */
public interface CatalogHolder {

    /** The palette, replacing the discovered one — {@link StudioPlugin#catalog()}. */
    PaletteCatalog catalog();
}
