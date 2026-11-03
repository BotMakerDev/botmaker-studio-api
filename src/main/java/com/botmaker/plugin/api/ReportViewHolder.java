package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.ReportView;

import java.util.List;

/**
 * A class giving the charts a plugin offers in the host's reports, named by
 * {@link PluginDeclaration#reportViews(Class[])}. See {@link TypeHolder} for how a holder is built and asked.
 */
public interface ReportViewHolder {

    /** The views, in the order the picker lists them — {@link StudioPlugin#reportViews()}. */
    List<ReportView> reportViews();
}
