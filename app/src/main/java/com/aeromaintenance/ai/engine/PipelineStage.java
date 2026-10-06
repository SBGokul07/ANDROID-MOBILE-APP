package com.aeromaintenance.ai.engine;

/** One stage of the AI pipeline and what it found (shown in the pipeline trace). */
public final class PipelineStage {
    public final String title;
    public final String detail;

    public PipelineStage(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }
}
